package com.sualtikasifi.cizimhafiza.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.sualtikasifi.cizimhafiza.BuildConfig
import com.sualtikasifi.cizimhafiza.util.GameConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdMob infrastructure — see [GameConstants.ADMOB_ENABLED].
 *
 * Placement plan (per the product brief, to avoid accidental-click policy
 * issues): interstitial after the result screen (never mid-drawing/guessing),
 * rewarded ad offered opt-in for an extra hint / extra time, never auto-shown.
 */
@Singleton
class AdManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val interstitialUnitId = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
    private val rewardedUnitId = BuildConfig.ADMOB_REWARDED_UNIT_ID
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Preloaded ahead of time (see preloadInterstitial) so maybeShowInterstitial
    // can show instantly instead of eating a multi-second network load right
    // at the moment the player reaches the result screen. Plain var, not
    // synchronized: every AdMob SDK callback that touches this is delivered
    // on the main thread, same as every call site here.
    private var cachedInterstitial: InterstitialAd? = null
    private var cachedInterstitialAt = 0L
    private var interstitialLoading = false
    private var interstitialRetries = 0

    // Same single-threaded (main-thread callback) reasoning as
    // cachedInterstitial. rewardedLoading keeps a second preload from being
    // fired while one is already in flight, which would otherwise happen
    // every time a match ends near a hint request.
    private var cachedRewarded: RewardedAd? = null
    private var cachedRewardedAt = 0L
    private var rewardedLoading = false

    // initializeIfConsented can be reached more than once (a consent form
    // resolving after a configuration change), and MobileAds.initialize is
    // not free to repeat.
    private var initialized = false

    /**
     * Initialises the ads SDK — but only once [ConsentManager] reports that
     * ad requests are permitted for this player's jurisdiction. Calling this
     * unconditionally, as an earlier revision did, requests ads in the EEA
     * with no lawful basis.
     */
    fun initializeIfConsented(consentManager: ConsentManager) {
        if (!consentManager.canRequestAds) {
            Log.d(TAG, "Ads not initialised: consent not granted or unavailable")
            return
        }
        initializeIfEnabled()
    }

    private fun initializeIfEnabled() {
        if (!GameConstants.ADMOB_ENABLED || initialized) return
        initialized = true
        // General/mixed audience (not a children's-only app — see the
        // product decision on target audience): keeps personalized ads
        // available for a healthier eCPM, while MAX_AD_CONTENT_RATING_PG
        // still keeps a family-friendly ceiling on what can show. The Play
        // Console "target audience and content" declaration is a separate,
        // account-side step that has to be done manually — this only
        // configures the SDK's own request behavior.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                // Developer phone (local.properties) always gets test ads, so
                // playing on it never counts as invalid traffic on real units.
                .setTestDeviceIds(listOfNotNull(BuildConfig.ADMOB_TEST_DEVICE_ID.takeIf { it.isNotBlank() }))
                .build()
        )
        MobileAds.initialize(context) {
            preloadInterstitial()
            preloadRewarded()
        }
    }

    /**
     * Makes sure BOTH ad types are sitting ready. Safe to call as often as
     * wanted — it is a no-op for anything already cached and fresh. Called
     * when a game screen opens, so by the time the player taps "+10 sn" (or
     * finishes a round) the ad is already in memory instead of being fetched
     * on the spot, and so an ad that sat cached past AdMob's ~1 hour expiry
     * is replaced before it can fail to show.
     */
    fun warmUp() {
        if (!GameConstants.ADMOB_ENABLED || !initialized) return
        dropStale()
        preloadInterstitial()
        preloadRewarded()
    }

    /** Ads expire about an hour after loading — an expired one fails at show time, the worst moment to find out. */
    private fun dropStale() {
        val now = System.currentTimeMillis()
        if (cachedInterstitial != null && now - cachedInterstitialAt > AD_FRESH_MILLIS) cachedInterstitial = null
        if (cachedRewarded != null && now - cachedRewardedAt > AD_FRESH_MILLIS) cachedRewarded = null
    }

    /**
     * Fetches an interstitial in the background and holds onto it until
     * [maybeShowInterstitial] consumes it. A no-op if one is already cached
     * or ads are disabled — safe to call opportunistically (called once at
     * app start, and again every time [maybeShowInterstitial] runs, so the
     * cache is topped back up right after being spent).
     */
    private fun preloadInterstitial() {
        if (!GameConstants.ADMOB_ENABLED || cachedInterstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            context,
            interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitialRetries = 0
                    cachedInterstitial = ad
                    cachedInterstitialAt = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialLoading = false
                    Log.d(TAG, "Interstitial preload failed: ${adError.message}")
                    // Backed-off retry, same as the rewarded one: a single
                    // failed fill used to leave the cache empty until the
                    // next game happened to top it up.
                    if (interstitialRetries < PRELOAD_RETRIES) {
                        val delayMillis = RETRY_BASE_MILLIS shl interstitialRetries
                        interstitialRetries++
                        mainHandler.postDelayed({ preloadInterstitial() }, delayMillis)
                    }
                }
            }
        )
    }

    /**
     * Call after the result screen is shown, never between drawing/guessing
     * turns. Shows instantly if [preloadInterstitial] already has one ready;
     * only falls back to an on-demand load (with its multi-second delay) on
     * the rare occasion nothing was preloaded yet. A load failure or a
     * disabled flag both fall straight through to [onDismissed] so the
     * result screen is never blocked on an ad.
     *
     * Frequency — the balance between revenue and AdMob's invalid-traffic /
     * "too many interstitials" scrutiny: one ad after every
     * [INTERSTITIAL_EVERY_N_GAMES]rd finished game (Hızlı Eşleş, offline and
     * online share one counter, persisted so it survives an app restart),
     * but never closer than [MIN_INTERSTITIAL_GAP_MILLIS] to the previous ad,
     * so a run of very short games can't stack them. [force] skips both
     * checks for the one placement (the daily challenge, once a day) meant
     * to show every time.
     */
    fun maybeShowInterstitial(
        activity: Activity,
        onDismissed: () -> Unit,
        force: Boolean = false,
        /**
         * With [force]: the shortest time since the previous interstitial for
         * this one to still show. 0 (the daily challenge) shows every time;
         * the Bölümler levels use a short gap so failing and instantly
         * retrying an even level does not stack ads back to back.
         */
        forceMinGapMillis: Long = 0L
    ) {
        if (!GameConstants.ADMOB_ENABLED) {
            onDismissed()
            return
        }
        if (force && forceMinGapMillis > 0L) {
            val sinceLast = System.currentTimeMillis() - prefs.getLong(KEY_LAST_AD_AT, 0L)
            if (sinceLast < forceMinGapMillis) {
                warmUp()
                onDismissed()
                return
            }
        }
        if (!force) {
            val games = prefs.getInt(KEY_GAMES_SINCE_AD, 0) + 1
            prefs.edit().putInt(KEY_GAMES_SINCE_AD, games).apply()
            val sinceLast = System.currentTimeMillis() - prefs.getLong(KEY_LAST_AD_AT, 0L)
            if (games < INTERSTITIAL_EVERY_N_GAMES || sinceLast < MIN_INTERSTITIAL_GAP_MILLIS) {
                warmUp() // keep the cache warm for next time either way
                onDismissed()
                return
            }
        }

        dropStale()
        val preloaded = cachedInterstitial
        if (preloaded != null) {
            cachedInterstitial = null
            preloaded.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() = recordInterstitialShown()
                override fun onAdDismissedFullScreenContent() {
                    onDismissed()
                    preloadInterstitial()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onDismissed()
                    preloadInterstitial()
                }
            }
            preloaded.show(activity)
            return
        }

        // Nothing preloaded (e.g. the very first match right after a cold
        // start, before the initial preload finished) — fall back to an
        // on-demand load exactly as before.
        InterstitialAd.load(
            context,
            interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() = recordInterstitialShown()
                        override fun onAdDismissedFullScreenContent() = onDismissed()
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) = onDismissed()
                    }
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Interstitial failed to load: ${adError.message}")
                    onDismissed()
                }
            }
        )
    }

    /** Only an ad that actually reached the screen resets the counters — a failed one must not cost the player their next chance. */
    private fun recordInterstitialShown() {
        prefs.edit()
            .putInt(KEY_GAMES_SINCE_AD, 0)
            .putLong(KEY_LAST_AD_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * Fetches a rewarded ad in the background and holds it until
     * [maybeShowRewarded] consumes it. Same rationale as
     * [preloadInterstitial], but the payoff is larger: a rewarded ad is
     * always requested at a moment the game clock is deliberately stopped
     * (see GameViewModel.useHint), so every second spent loading is a second
     * the player sits looking at a frozen countdown and a "Yükleniyor…"
     * label wondering whether the button worked.
     */
    private var rewardedRetries = 0

    private fun scheduleRewardedPreloadRetry() {
        if (rewardedRetries >= PRELOAD_RETRIES) return
        val delayMillis = RETRY_BASE_MILLIS shl rewardedRetries
        rewardedRetries++
        mainHandler.postDelayed({ preloadRewarded() }, delayMillis)
    }

    private fun preloadRewarded() {
        if (!GameConstants.ADMOB_ENABLED || cachedRewarded != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(
            context,
            rewardedUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewardedRetries = 0
                    cachedRewarded = ad
                    cachedRewardedAt = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedLoading = false
                    Log.d(TAG, "Rewarded preload failed: ${adError.message}")
                    // One failed fill used to mean no rewarded ad for the
                    // rest of the session: nothing ever tried again until a
                    // player tapped, and that tap then had to wait on a
                    // fresh network load that could fail the same way. Retry
                    // on a backoff so the cache is usually warm by the time
                    // anyone asks.
                    scheduleRewardedPreloadRetry()
                }
            }
        )
    }

    /**
     * User-initiated only (e.g. "extra hint" button) — never auto-triggered.
     *
     * Reports *why* no reward was granted, not merely that none was. A
     * player who skipped the video and a player whose ad never loaded both
     * used to arrive as `false`, so the screen could only sit on
     * "Yükleniyor…" forever and say nothing — the reported bug. The two
     * cases need different answers: one is a choice, the other is worth
     * apologising for and worth offering again.
     */
    fun maybeShowRewarded(activity: Activity, onResult: (RewardedOutcome) -> Unit) {
        if (!GameConstants.ADMOB_ENABLED) {
            onResult(RewardedOutcome.UNAVAILABLE)
            return
        }

        // onReward drives resuming a paused game countdown, so it must fire
        // exactly once no matter which SDK callback path is taken — a
        // double-resume would start two competing timer coroutines, and a
        // missed one would leave the round frozen forever.
        var settled = false
        val settle: (RewardedOutcome) -> Unit = { outcome ->
            if (!settled) {
                settled = true
                onResult(outcome)
                preloadRewarded() // top the cache back up for next time
            }
        }

        fun show(ad: RewardedAd) {
            // onAdDismissedFullScreenContent always fires once the ad closes,
            // whether the viewer watched to completion or skipped early — by
            // then `earned` already reflects whether the reward callback
            // fired first, so skipping early correctly resolves as no reward.
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() =
                    settle(if (earned) RewardedOutcome.EARNED else RewardedOutcome.SKIPPED)

                // Failing to *show* an ad that loaded is the player's
                // problem to be told about, not their choice — same answer
                // as never having one to show.
                override fun onAdFailedToShowFullScreenContent(adError: AdError) =
                    settle(RewardedOutcome.UNAVAILABLE)
            }
            ad.show(activity) { earned = true }
        }

        dropStale()
        val preloaded = cachedRewarded
        if (preloaded != null) {
            cachedRewarded = null
            show(preloaded)
            return
        }

        RewardedAd.load(
            context,
            rewardedUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) = show(ad)

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Rewarded ad failed to load: ${adError.message}")
                    settle(RewardedOutcome.UNAVAILABLE)
                }
            }
        )
    }

    private companion object {
        const val TAG = "AdManager"
        const val PREFS_NAME = "ad_manager_prefs"
        const val KEY_GAMES_SINCE_AD = "games_since_interstitial"
        const val KEY_LAST_AD_AT = "last_interstitial_at"

        /** Every 3rd finished game — see [maybeShowInterstitial]. */
        const val INTERSTITIAL_EVERY_N_GAMES = 3

        /** Never two interstitials closer together than this, however short the games between them. */
        const val MIN_INTERSTITIAL_GAP_MILLIS = 3 * 60 * 1000L

        /** Cached ads are dropped after this — AdMob expires them at about an hour. */
        const val AD_FRESH_MILLIS = 55 * 60 * 1000L

        /** How many times a failed preload is retried before giving up for this session. */
        const val PRELOAD_RETRIES = 4
        const val RETRY_BASE_MILLIS = 15_000L
    }
}

/** Why a rewarded ad did or did not pay out — see AdManager.maybeShowRewarded. */
enum class RewardedOutcome {
    /** Watched to the point the SDK granted the reward. */
    EARNED,

    /** Shown, but closed early — a deliberate choice, so nothing to report. */
    SKIPPED,

    /** Never shown: no fill, a load error, or ads disabled. Worth telling the player. */
    UNAVAILABLE
}
