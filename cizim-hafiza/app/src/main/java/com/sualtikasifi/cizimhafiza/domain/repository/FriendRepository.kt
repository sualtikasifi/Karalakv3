package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.AddFriendOutcome
import com.sualtikasifi.cizimhafiza.domain.model.BlockedUser
import com.sualtikasifi.cizimhafiza.domain.model.Friend
import com.sualtikasifi.cizimhafiza.domain.model.FriendMigration
import com.sualtikasifi.cizimhafiza.domain.model.FriendRequest
import com.sualtikasifi.cizimhafiza.domain.model.InviteEligibility
import com.sualtikasifi.cizimhafiza.domain.model.MatchInvite
import com.sualtikasifi.cizimhafiza.domain.model.LeagueTable
import kotlinx.coroutines.flow.Flow

/** Firestore-backed friends list + match invites (see users/{uid}, friendCodes/{code}). */
interface FriendRepository {

    /** Returns this device's permanent friend code, generating one on first call. */
    suspend fun ensureFriendCode(nickname: String): String

    fun observeFriends(): Flow<List<Friend>>

    /**
     * Looks up [code] and asks that player to be friends — it does NOT make
     * them one. See [AddFriendOutcome] for the three ways that can land.
     */
    suspend fun addFriendByCode(code: String, myNickname: String): Result<AddFriendOutcome>

    /**
     * The same as [addFriendByCode] for somebody already identified by uid (a player met in a lobby), so no code is
     * needed. Same outcomes: a request sent, a friendship closed because they had asked first, or already friends.
     */
    suspend fun sendFriendRequestToUid(friendUid: String, friendNickname: String, myNickname: String): Result<AddFriendOutcome>

    /** Requests waiting on this player's answer — see [acceptFriendRequest]. */
    fun observeFriendRequests(): Flow<List<FriendRequest>>

    /** Writes the friendship onto both sides and clears the request. */
    suspend fun acceptFriendRequest(request: FriendRequest, myNickname: String): Result<Unit>

    /** Drops the request without a trace. No cooldown, no notification — see declineInvite for the noisier case. */
    suspend fun declineFriendRequest(fromUid: String): Result<Unit>

    /** Removes the friendship on both sides. */
    suspend fun removeFriend(friendUid: String): Result<Unit>

    /**
     * Reads this uid's current friends and leaves a one-time secret in its own private/migration document — called
     * right before this identity is abandoned in favour of a different, pre-existing account (see
     * AuthRepositoryImpl.signInWithGoogle). Must run WHILE STILL SIGNED IN as this uid: only this uid can write that
     * secret, and the secret is what later lets the new account prove to firestore.rules that it is this one's
     * successor. Nothing is deleted here, so a sign-in that fails afterwards leaves every friendship as it was.
     *
     * The returned ticket is what [adoptMigratedFriends] re-establishes on the account being switched to.
     */
    suspend fun prepareFriendMigration(): FriendMigration

    /**
     * The other half of a migration started by [prepareFriendMigration] —
     * called once already signed in as the NEW uid. Records where this
     * account came from (with the secret) in its own private/migratedFrom,
     * then, per friend, adds the friend onto this uid's own list, puts this
     * uid onto theirs and removes the old uid from it. Best-effort: one
     * friend that fails to migrate (blocked in the meantime, deleted their
     * account) must not undo the ones that succeeded, and must never block
     * the account switch itself.
     */
    suspend fun adoptMigratedFriends(migration: FriendMigration)

    fun observeIncomingInvites(): Flow<List<MatchInvite>>

    /** Pre-send check (UX only — the real enforcement is in firestore.rules' invites create rule). */
    suspend fun canInvite(toUid: String): InviteEligibility

    suspend fun sendMatchInvite(toUid: String, roomCode: String, myNickname: String): Result<Unit>

    /** Removes an invite once it's been accepted — no cooldown consequence (see declineInvite for that). */
    suspend fun consumeInvite(inviteId: String)

    /** Removes the invite and starts a cooldown so [invite]'s sender can't re-invite for a while. */
    suspend fun declineInvite(invite: MatchInvite): Result<Unit>

    suspend fun blockUser(uid: String, nickname: String): Result<Unit>
    suspend fun unblockUser(uid: String): Result<Unit>
    fun observeBlockedUsers(): Flow<List<BlockedUser>>

    /** Persists this device's current FCM token so the invite Cloud Function can push to it. */
    suspend fun updateFcmToken(token: String)

    /**
     * Publishes a renamed player onto their own public profile document —
     * the one a friend list and the league table read names from.
     *
     * Separate from [publishLeagueScore] because a rename must not wait for
     * the next weekly publish to be seen by anybody else: until this ran,
     * changing your name changed it only on your own phone.
     */
    suspend fun updatePublicNickname(nickname: String)

    /**
     * Marks this player's public profile as agreeing — or no longer agreeing — to have their drawings and name shown
     * in Karalak's social media videos (the developer panel's video studio offers only those who agreed).
     */
    suspend fun publishFeatureConsent(consent: Boolean)

    /**
     * Publishes this device's weekly-league standing onto its own public
     * profile document, so friends can read it without a per-player
     * subcollection. Safe to call often — one read of the profile and one
     * merged write. Returns false when the server's league limits only let
     * part of [periodXp] through for now (see LeagueScoreCap); the rest is
     * published by a later call.
     */
    suspend fun publishLeagueScore(nickname: String, periodXp: Int, periodId: Long, level: Int, frameId: String, avatarUrl: String, totalXp: Int): Boolean

    /**
     * The player's own row plus every friend's, already ranked — see
     * domain.model.LeagueTable. Rows from a previous week read as zero
     * rather than being hidden, so the table is complete on a Monday
     * morning instead of empty.
     */
    fun observeLeagueTable(): Flow<LeagueTable>

    /**
     * Attributes this device's account to whoever owns [inviterFriendCode] —
     * called once, when the app is opened via a friend-invite deep link (see
     * karalak://friend/482913 and FriendsViewModel). A no-op if this account
     * already has an inviter, if the code doesn't resolve, or if it resolves
     * to this same account — the reward this unlocks (see
     * [claimPendingRewards]) is for bringing in someone new, not for
     * self-dealing or overwriting an earlier invite.
     */
    suspend fun recordReferralIfEligible(inviterFriendCode: String)

    /**
     * Applies and clears any referral (or future) XP rewards a Blaze-free
     * cron script has left on this account's private/pendingRewards
     * document (see functions/src/index.ts's runGrantReferralRewards) — the
     * amount actually applied, for a one-time "kazandın!" toast, or 0 if
     * nothing was pending.
     */
    suspend fun claimPendingRewards(): PendingRewardTotals
}

class FriendCodeNotFoundException : Exception("Bu kodla bir kullanıcı bulunamadı")
class CannotAddSelfException : Exception("Kendini arkadaş olarak ekleyemezsin")

/** More new friend requests than [com.sualtikasifi.cizimhafiza.domain.model.FriendRequestLimit] allows in a day. */
class FriendRequestLimitException : Exception("Bugün çok fazla arkadaşlık isteği gönderdin")

/**
 * Thrown by [FriendRepository.addFriendByCode] for the bot's fixed code
 * (see BotRoomEngine.ROOM_CODE) — never actually adds a friendship, always
 * looks like a request that was sent and is still awaiting a response, so
 * the bot reads as a cautious real person who hasn't answered yet, not an
 * outright rejection.
 */
class BotFriendRequestPendingException : Exception("İstek gönderildi, henüz cevap yok")

/** What [FriendRepository.claimPendingRewards] brought in: entries tagged `currency: "gold"` count as gold, older untagged ones as XP. */
data class PendingRewardTotals(val xp: Int = 0, val gold: Int = 0) {
    val isEmpty get() = xp <= 0 && gold <= 0
}
