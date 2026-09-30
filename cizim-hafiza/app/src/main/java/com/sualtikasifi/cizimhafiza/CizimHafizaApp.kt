package com.sualtikasifi.cizimhafiza

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.auth.FirebaseAuth
import com.sualtikasifi.cizimhafiza.data.local.WordPoolSynchronizer
import com.sualtikasifi.cizimhafiza.data.local.dao.GameSessionDao
import com.sualtikasifi.cizimhafiza.domain.model.LeaguePeriod
import com.sualtikasifi.cizimhafiza.notifications.NotificationScheduler
import com.sualtikasifi.cizimhafiza.util.AutoBackupPublisher
import com.sualtikasifi.cizimhafiza.util.ProfileNameSynchronizer
import com.sualtikasifi.cizimhafiza.util.ReferralRewardClaimer
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import com.sualtikasifi.cizimhafiza.util.LeagueScorePublisher
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class CizimHafizaApp : Application(), Configuration.Provider {

    // Everything below except workerFactory is dagger.Lazy on purpose. A plain
    // @Inject field makes Hilt construct the object inside super.onCreate() on the
    // main thread — before the first frame — even when nothing needs it until later
    // (a Firestore-backed publisher, a Room DAO, the FirebaseAuth instance ...).
    // Lazy defers that construction to the background coroutines in onCreate below.
    // SettingsRepository stays eager: MainActivity needs it for the first frame anyway,
    // so building it here costs nothing extra.
    @Inject lateinit var wordPoolSynchronizer: Lazy<WordPoolSynchronizer>
    @Inject lateinit var gameSessionDao: Lazy<GameSessionDao>
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var firebaseAuth: Lazy<FirebaseAuth>
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var leagueScorePublisher: Lazy<LeagueScorePublisher>
    @Inject lateinit var referralRewardClaimer: Lazy<ReferralRewardClaimer>
    @Inject lateinit var autoBackupPublisher: Lazy<AutoBackupPublisher>
    @Inject lateinit var profileNameSynchronizer: Lazy<ProfileNameSynchronizer>
    @Inject lateinit var xpEventRepository: Lazy<com.sualtikasifi.cizimhafiza.domain.repository.XpEventRepository>

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Must be installed before any Firebase call. Off by default — see GameConstants.APP_CHECK_ENABLED.
        if (com.sualtikasifi.cizimhafiza.util.GameConstants.APP_CHECK_ENABLED) runCatching {
            com.google.firebase.FirebaseApp.initializeApp(this)
            com.google.firebase.appcheck.FirebaseAppCheck.getInstance().installAppCheckProviderFactory(AppCheckProvider.factory())
        }

        // Ads are NOT initialised here any more: consent has to be
        // gathered first, and Google's UMP form needs an Activity to show
        // itself. Both now happen together in MainActivity.onCreate — see
        // ads/ConsentManager.kt.

        // Friend-invite push notifications (see FriendInviteMessagingService)
        // need their channel to exist before the first FCM push can arrive,
        // which can happen before anything else in the app has run — so these
        // two stay synchronous (they are a couple of milliseconds).
        NotificationScheduler.createFriendInviteChannel(this)
        com.sualtikasifi.cizimhafiza.notifications.ChestReadyNotifier.createChannel(this)

        // Needed within the first seconds, but not on the main thread and not
        // before the first frame:
        applicationScope.launch {
            // Re-synced on every app UPDATE (not just first install) and on any
            // language change, not on every single launch's happy path — see
            // WordPoolSynchronizer for why the version/language gating exists.
            guarded("word pool") { wordPoolSynchronizer.get().syncAsync() }

            // Silent anonymous sign-in for the online (friend-vs-friend) mode —
            // gives this device a stable Firestore uid with no login screen and
            // no personal data collected. Only needed once per install; if a uid
            // already exists (app relaunch) this is a no-op. Everything that
            // needs the uid goes through AuthRepository.ensureSignedIn(), which
            // waits for it instead of assuming it is already there.
            guarded("anonymous sign-in") {
                val auth = firebaseAuth.get()
                if (auth.currentUser == null) auth.signInAnonymously()
            }

            // One-time seed for the rank/level system's lifetime score (see
            // PlayerRank): if this device already has game history from before
            // this feature existed, count it instead of starting everyone back
            // at 0 — otherwise players would feel like their past games "didn't
            // count". Only the surviving pruned rows (last RECENT_GAMES_LIMIT)
            // can be counted; that's an acceptable best-effort estimate.
            // seedLifetimeScoreIfAbsent is itself a no-op once a lifetime score
            // has ever been recorded, so this is safe to call on every launch.
            guarded("lifetime score seed") {
                settingsRepository.seedLifetimeScoreIfAbsent(gameSessionDao.get().getTotalScoreSum())
                // Progression moved from raw score to XP (see PlayerLevel); this
                // hands anyone who had already earned a rank the XP that tier is
                // now worth, so the update never demotes them. No-op once any XP
                // has been recorded.
                settingsRepository.seedLifetimeXpFromLegacyScore(settingsRepository.lifetimeScore.value)
            }

            // StatisticsScreen builds a `remember { SimpleDateFormat(...) }`
            // synchronously as part of its first composition — which, since
            // it's a screen reached by navigating in, happens WHILE the
            // enter-transition animation is playing. A locale's date/time
            // symbol tables (DateFormatSymbols) are lazily loaded on the first
            // SimpleDateFormat ever constructed for that locale in this
            // process, which can cost tens of milliseconds — enough to visibly
            // drop frames mid-transition (reported as "stutter entering
            // Statistics"). Constructing one here, off the main thread, at
            // app start (long before anyone visits that screen) pays that
            // one-time cost where nothing is animating.
            guarded("date format warm-up") { SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()) }

            // Same reasoning, same fix, a second lazily-loaded locale table:
            // java.time's DateTimeFormatter caches its own locale symbol data
            // separately from java.text.SimpleDateFormat's, so warming one does
            // not warm the other. LeagueScreen calls LeaguePeriod.monthYearLabel
            // unmemoized, directly in RewardBanner's composition.
            guarded("month label warm-up") { LeaguePeriod.monthYearLabel(java.time.YearMonth.now().toString()) }
        }

        // Background syncing that nothing on the first screen depends on. Started
        // a moment after launch so it does not compete with the first frames for
        // CPU, disk and network. Each of these already runs its work on its own
        // background scope; what used to cost startup time was constructing them
        // (and their Firestore/Room dependencies) on the main thread.
        applicationScope.launch {
            delay(DEFERRED_START_DELAY_MS)

            // Your league row lives on your own profile document, so it
            // has to be written by this device — started here so it follows the
            // XP itself even when the player never opens the standings.
            guarded("league score publisher") { leagueScorePublisher.get().start() }
            // Applies any referral XP a friend invited by this device has
            // earned since the last launch (see ReferralRewardClaimer) — the
            // reward can only ever be granted server-side into a private
            // Firestore document, never straight onto the local XP counter it
            // has to end up in.
            guarded("referral reward claimer") { referralRewardClaimer.get().start() }
            // Keeps a linked account's cloud backup current on its own — see
            // AutoBackupPublisher for why the old "only on an explicit tap"
            // behaviour left most players' backups stale.
            guarded("auto backup") { autoBackupPublisher.get().start() }
            // One document listener: a 2x XP event started from the developer panel
            // then reaches every open app at once instead of waiting for a cache to expire.
            guarded("xp event listener") { xpEventRepository.get().startListening() }
            // Gives a signed-in player their Google name when they haven't
            // picked one — app-wide rather than on the Hesap screen, since the
            // name is what every other player sees in lobbies and the league.
            guarded("profile name sync") { profileNameSynchronizer.get().start() }
            // Daily "come back and play" reminder — see notifications/. Boot and
            // package-replaced receivers re-arm it independently, so waiting a
            // moment here loses nothing.
            guarded("daily reminder") { NotificationScheduler.schedule(this@CizimHafizaApp) }
        }
    }

    /** One failing background job must never take the others (or the app) down with it. */
    private inline fun guarded(what: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "Deferred startup step failed: $what", e)
        }
    }

    private companion object {
        const val TAG = "CizimHafizaApp"

        /** Long enough for the first screen to be drawn and interactive, short enough that no visible feature waits on it. */
        const val DEFERRED_START_DELAY_MS = 1_500L
    }
}
