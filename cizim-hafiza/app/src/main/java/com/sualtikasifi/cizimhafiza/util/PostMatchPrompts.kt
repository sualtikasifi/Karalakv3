package com.sualtikasifi.cizimhafiza.util

import com.sualtikasifi.cizimhafiza.domain.repository.AuthState

/**
 * Decides whether a just-finished match should interrupt the result screen
 * with a one-time onboarding nudge: a Google sign-in prompt after the very
 * first match ever played on this device, a Play Store rating prompt after
 * the third. Both are evaluated right after [SettingsRepository.lifetimeGamesPlayed]
 * has already been bumped for THIS match (see GameRepositoryImpl.finishSaving,
 * called before either result screen reads it), so the count read here is
 * this match's own, not the one before it.
 *
 * Each prompt is truly one-time: calling either function flips its "shown"
 * flag the moment it decides to show the prompt, not when the player acts
 * on it — so a dismissed or ignored prompt never comes back on a later
 * match either. That is a deliberate choice: this is a first-impressions
 * nudge, not a recurring ask.
 */
object PostMatchPrompts {
    const val SIGN_IN_PROMPT_AT_GAME = 1
    /** Finished Quick Match rounds (not games in general) after which the rating ask may appear. */
    const val RATING_PROMPT_AT_GAME = 3

    /** Paid once, via SettingsRepository.grantRatingBonusXpOnce, when the rating prompt's "Puanla" is tapped. */
    const val RATING_BONUS_XP = 500

    /**
     * Later, softer reminders for a player who is still a guest: by the 10th and the 30th finished game there is real
     * progress to lose, which is exactly when "do not lose it" is persuasive. Each is shown once.
     */
    val SIGN_IN_REMINDER_AT = listOf(10, 30)

    fun shouldShowSignIn(settingsRepository: SettingsRepository, authState: AuthState): Boolean {
        if (authState is AuthState.Linked) return false
        val games = settingsRepository.lifetimeGamesPlayed
        if (!settingsRepository.signInPromptShown && games == SIGN_IN_PROMPT_AT_GAME) {
            settingsRepository.signInPromptShown = true
            return true
        }
        val next = nextReminderStage(games, settingsRepository.signInReminderStage) ?: return false
        settingsRepository.signInReminderStage = next
        return true
    }

    /**
     * The reminder stage to move to when a later sign-in reminder is due, or null when none is. Moves past every
     * threshold already reached, so a restored high game count asks once, not once per threshold.
     */
    fun nextReminderStage(games: Int, stage: Int): Int? {
        val due = SIGN_IN_REMINDER_AT.getOrNull(stage) ?: return null
        if (games < due) return null
        return SIGN_IN_REMINDER_AT.count { games >= it }
    }

    fun shouldShowRating(settingsRepository: SettingsRepository): Boolean {
        if (settingsRepository.ratingPromptShown) return false
        // Due only once a Quick Match round has finished with the count at three or more — see
        // SettingsRepository.markRatingDueIfEligible. Counters restored from a backup alone never qualify.
        if (!settingsRepository.ratingPromptPending) return false
        settingsRepository.ratingPromptPending = false
        settingsRepository.ratingPromptShown = true
        return true
    }
}
