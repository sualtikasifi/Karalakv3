package com.sualtikasifi.cizimhafiza.domain.repository

/**
 * Erases this player's account: everything stored about them in Firestore,
 * everything stored on this device, and the Firebase user itself.
 *
 * Required by Google Play for any app that lets people create an account —
 * an in-app route to delete the account and the data attached to it, not
 * merely to sign out. Karalak links a Google account (see
 * [AuthRepository.linkWithGoogle]) and keeps a profile, a friends list, a
 * cloud backup and a weekly-league entry under `users/{uid}`, so signing
 * out alone left all of that behind with no way for the player to reach it.
 *
 * The one remote step that must succeed is filing the deletion request
 * (deletionRequests/{uid}); the server's daily maintenance run then removes
 * everything the account left behind, including the sign-in. What the device
 * removes itself on top of that is best-effort, and local data is wiped
 * unconditionally.
 */
interface AccountDeletionRepository {
    /**
     * @return failure only when the deletion request could not be recorded (offline, typically).
     *   A partial client-side cleanup still counts as success — the server finishes it.
     */
    suspend fun deleteAccountAndData(): Result<Unit>
}

/** The Firebase user was signed in too long ago for a delete to be allowed without signing in again. */
class ReauthenticationRequiredException : Exception()
