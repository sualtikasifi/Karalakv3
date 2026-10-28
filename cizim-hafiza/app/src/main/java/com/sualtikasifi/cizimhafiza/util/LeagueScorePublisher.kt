package com.sualtikasifi.cizimhafiza.util

import android.util.Log
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel
import com.sualtikasifi.cizimhafiza.domain.model.LeaguePeriod
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps this device's league standing published on its public profile.
 *
 * The standing used to be written in exactly one place: the moment the player
 * opened the league screen. That is backwards. Your row in a friend's
 * table comes from *your* profile document, so a friend who plays all week
 * and never opens the standings has nothing to read — everyone else sees them
 * sitting on zero, no matter how much XP they earned. Which is precisely the
 * bug this exists to fix, and it hit the most casual players hardest, because
 * they are the least likely to open a leaderboard.
 *
 * Publishing follows the XP itself instead. [SettingsRepository.periodXp] is
 * a StateFlow, so this also fires once at app start with whatever the current
 * total is, which is what makes it self-healing: a write lost to a dead
 * connection is simply retried the next time the app opens.
 *
 * The two safeguards on cost:
 *  - [PUBLISH_DEBOUNCE_MS] collapses a whole match's worth of per-word XP into
 *    one write, landing a few seconds after the player stops earning.
 *  - a stored signature skips a write that would say exactly what the profile
 *    already says, so relaunching the app repeatedly costs nothing.
 */
@Singleton
class LeagueScorePublisher @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val friendRepository: FriendRepository,
    private val avatarPhotoResolver: AvatarPhotoResolver,
    private val legalConsentStore: LegalConsentStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    private val _scoreRefused = kotlinx.coroutines.flow.MutableStateFlow(false)

    /**
     * True while the server is refusing this account's score (permission denied — typically a jump bigger than the
     * rules' growth allowance). Being offline is NOT this: it just retries. The league screen tells the player, who
     * would otherwise see a table without themselves and no reason why.
     */
    val scoreRefused: kotlinx.coroutines.flow.StateFlow<Boolean> = _scoreRefused

    /** Safe to call repeatedly; only the first call subscribes. */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        scope.launch {
            kotlinx.coroutines.flow.combine(settingsRepository.periodXp.debounce(PUBLISH_DEBOUNCE_MS), avatarPhotoResolver.url) { xp, avatarUrl -> snapshotFor(xp, avatarUrl) }
                .distinctUntilChanged()
                .collect { snapshot -> publish(snapshot) }
        }
    }

    /**
     * Called when the standings are actually being looked at, so a table
     * opened seconds after a match does not show a stale row for yourself
     * while the debounce above is still counting down.
     */
    fun publishNow() {
        scope.launch { publish(snapshotFor(settingsRepository.periodXp.value, avatarPhotoResolver.currentUrl())) }
    }

    private fun snapshotFor(periodXp: Int, avatarUrl: String): Snapshot {
        val level = PlayerLevel.levelForXp(settingsRepository.lifetimeXp.value)
        return Snapshot(
            nickname = settingsRepository.nicknameOrDefault,
            // Read against the current month, not the value the flow last carried (see currentPeriodXp).
            periodXp = settingsRepository.currentPeriodXp(),
            periodId = LeaguePeriod.periodIdFor(com.sualtikasifi.cizimhafiza.util.TurkeyTime.today()),
            level = level,
            frameId = AvatarFrame.resolve(settingsRepository.selectedAvatarFrameId.value, level).name,
            avatarUrl = avatarUrl,
            totalXp = settingsRepository.lifetimeXp.value
        )
    }

    private suspend fun publish(snapshot: Snapshot) {
        // A player who may be under 13 is kept out of the public table (see LegalConsentStore).
        if (legalConsentStore.childMode.value) return
        if (settingsRepository.publishedLeagueScoreSignature == snapshot.signature) return
        runCatching {
            friendRepository.publishLeagueScore(
                nickname = snapshot.nickname,
                periodXp = snapshot.periodXp,
                periodId = snapshot.periodId,
                level = snapshot.level,
                frameId = snapshot.frameId,
                avatarUrl = snapshot.avatarUrl,
                totalXp = snapshot.totalXp
            )
        }.onSuccess { complete ->
            // Only remembered once the whole score actually landed — a signature
            // stored on a failed or partial publish would suppress the retry that
            // is the whole point of re-publishing at app start.
            if (complete) settingsRepository.publishedLeagueScoreSignature = snapshot.signature
            _scoreRefused.value = !complete
        }.onFailure {
            if ((it as? com.google.firebase.firestore.FirebaseFirestoreException)?.code ==
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) _scoreRefused.value = true
            NonFatal.report(TAG, "League score not published", it)
        }
    }

    private data class Snapshot(
        val nickname: String,
        val periodXp: Int,
        val periodId: Long,
        val level: Int,
        val frameId: String,
        val avatarUrl: String,
        val totalXp: Int
    ) {
        val signature: String get() = "$periodId|$periodXp|$level|$frameId|$nickname|$avatarUrl|$totalXp"
    }

    private companion object {
        const val TAG = "LeagueScorePublisher"

        /**
         * Long enough to sit out a whole match's per-word XP (one write per match instead of one per word). The write
         * that would be lost if the app were closed inside this window is flushed when the screen goes away
         * (see CizimHafizaApp's activity callback), so a long wait costs nothing in freshness.
         */
        const val PUBLISH_DEBOUNCE_MS = 45_000L
    }
}
