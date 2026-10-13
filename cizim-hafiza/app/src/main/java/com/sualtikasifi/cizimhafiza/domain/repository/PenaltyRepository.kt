package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.Penalty

/**
 * Applies the penalties a reviewer wrote against this account.
 *
 * Looked for when the app opens. A penalty is a record rather than a
 * command precisely so this can be true: a device offline for a week still
 * applies it on the day it comes back, and a device that already applied it
 * never applies it twice.
 */
interface PenaltyRepository {

    /**
     * Penalties written against this account that its device has not applied yet, newest first — read only, nothing is
     * taken here. Shown to the player first; see [apply].
     *
     * Empty on every ordinary launch — which is the common case, so this
     * must be cheap and must never block anything.
     */
    suspend fun outstanding(): List<Penalty>

    /**
     * Takes what [penalties] cost off the account — XP, gold, a daily streak where one was broken — and marks each as
     * applied. Called when the player taps "Anladım" on the penalty window, so the loss happens in front of them.
     */
    suspend fun apply(penalties: List<Penalty>)

    /**
     * When this account may play the online modes again, or null if it may
     * play now. Read from the penalty record rather than computed here: a
     * lockout counted on the device's own clock could be sat out by moving
     * it forward.
     */
    suspend fun lockedUntilMillis(): Long?
}
