package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.content.SharedPreferences
import com.sualtikasifi.cizimhafiza.R
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestReward
import com.sualtikasifi.cizimhafiza.domain.model.ChestLoot
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.ChestSlots
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffects
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffect
import com.sualtikasifi.cizimhafiza.domain.model.RuneLive
import com.sualtikasifi.cizimhafiza.domain.model.RuneRewards
import com.sualtikasifi.cizimhafiza.domain.model.RuneTree
import com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel
import com.sualtikasifi.cizimhafiza.domain.model.LeaguePeriod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Synchronized
import kotlin.random.Random

/**
 * Sound/vibration on-off toggles from the Settings screen, backed by SharedPreferences.
 *
 * Every gold/XP/joker/store/chest mutator below is `@Synchronized`: each one
 * reads a StateFlow's current value, computes an updated value, and writes
 * both the pref and the StateFlow back — a classic read-modify-write with no
 * protection between the read and the write. Two of these racing on
 * different threads (a chest finishing opening while a store purchase is in
 * flight, two rapid joker taps) could otherwise silently lose one of the two
 * updates. `@Synchronized` locks on `this`, which is safe here specifically
 * because this class is a single `@Singleton` instance nothing else holds a
 * lock on, and every one of these calls is a fast, synchronous
 * SharedPreferences write — never a suspend function, so there is no risk of
 * blocking the lock across a coroutine suspension point.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ledger: EconomyLedger,
    private val runes: RuneStore,
    private val chests: ChestStore
) {

    // Gameplay writes (gold, chests, purchases, runes) are ONE edit each, applied asynchronously: a single apply() lands
    // whole or not at all, Android flushes it before the app is stopped, and the disk write stays off the main thread.
    // Only the paths that must be on disk before something else reads it back (penalties, restore, account switch)
    // still commit synchronously.
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * The few "has this device already been asked" facts that must outlive a reinstall — kept in their
     * own small file because only this file is included in Android's cloud backup (see
     * res/xml/backup_rules.xml); the main settings file is account data and deliberately is not.
     */
    private val flagPrefs = context.getSharedPreferences(FLAGS_PREFS_NAME, Context.MODE_PRIVATE)

    // For work this class needs to do off the main thread at construction
    // time — this repository is a Hilt singleton, so it is built eagerly
    // during Activity/Application injection, ON the main thread. Anything
    // slow in an init block here stutters the very first frame (reported as
    // "kasma" on the splash screen). See the two init blocks below.
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * What to call a player who never typed a nickname — a localised string,
     * not the hard-coded "Oyuncu" it used to be in ten separate call sites.
     * That name is shown to OTHER players (lobby, result table, league), so
     * an English player with no nickname was appearing to everyone, in every
     * language, under a Turkish word.
     */
    val nicknameOrDefault: String
        get() = nickname.value.trim().ifBlank { context.getString(R.string.default_nickname) }

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    /**
     * The background music, separately from [soundEnabled].
     *
     * Two switches rather than one because they answer different questions.
     [soundEnabled] is the master — turning it off silences the app
     * completely, music included — while this one exists for the far more
     * common case of wanting the game's own feedback sounds but not a
     * soundtrack, and it is what the in-game speaker button toggles (see
     * DrawingScreen/GuessScreen's top bar) without touching the master.
     */
    private val _musicEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUSIC, true))
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    fun setMusicEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MUSIC, enabled) }
        _musicEnabled.value = enabled
    }

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION, true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // Display name shown to the opponent in online (friend-vs-friend) rooms.
    // Empty until the player sets one on their first visit to online mode.
    private val _nickname = MutableStateFlow(prefs.getString(KEY_NICKNAME, "") ?: "")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    // Cumulative points across every finished game, solo or online, ever
    // played on this device — never decreases. A pure statistic now that
    // progression runs on XP (see lifetimeXp below). Kept in SharedPreferences
    // rather than Room on purpose: it survives a Room schema migration
    // untouched, unlike a value stored in a table that
    // fallbackToDestructiveMigration() wipes.
    private val _lifetimeScore = MutableStateFlow(prefs.getInt(KEY_LIFETIME_SCORE, 0))
    val lifetimeScore: StateFlow<Int> = _lifetimeScore.asStateFlow()

    // The single progression currency (see domain.model.PlayerLevel). Unlike
    // lifetimeScore this also pays out for turning up — daily challenges and
    // streaks — so the level badge reflects commitment, not just skill.
    private val _lifetimeXp = MutableStateFlow(prefs.getInt(KEY_LIFETIME_XP, 0))
    val lifetimeXp: StateFlow<Int> = _lifetimeXp.asStateFlow()

    // Same idea as lifetimeScore, for the achievement system's "N kelime
    // çizdin" milestones (see domain.model.Achievement) — game_sessions is
    // pruned (see GameSessionDao.pruneOlderThan) so it can't answer "how
    // many words ever", this never-shrinking counter can.
    private val _lifetimeWordsDrawn = MutableStateFlow(prefs.getInt(KEY_LIFETIME_WORDS_DRAWN, 0))
    val lifetimeWordsDrawn: StateFlow<Int> = _lifetimeWordsDrawn.asStateFlow()

    // Further never-shrinking counters behind the longer-horizon achievements
    // (see domain.model.Achievement) — same rationale as lifetimeWordsDrawn:
    // game_sessions is pruned, so it can't answer "ever" questions.
    val lifetimeGamesPlayed: Int get() = prefs.getInt(KEY_LIFETIME_GAMES_PLAYED, 0)

    // True once a game has been finished since the app was started (not remembered across launches). The daily login
    // reward window waits for it: it opens after the first game of a visit, not when the app is opened.
    private val _gamePlayedThisRun = MutableStateFlow(false)
    val gamePlayedThisRun: StateFlow<Boolean> = _gamePlayedThisRun.asStateFlow()
    val lifetimePerfectRounds: Int get() = prefs.getInt(KEY_LIFETIME_PERFECT_ROUNDS, 0)
    val lifetimeOnlineWins: Int get() = prefs.getInt(KEY_LIFETIME_ONLINE_WINS, 0)
    val bestStreak: Int get() = prefs.getInt(KEY_BEST_STREAK, 0)

    // Which AvatarFrame ring the player has chosen to wear (see
    // domain.model.AvatarFrame.resolve) — persisted by the enum constant's
    // own name, same convention as UnlockedAchievementEntity, so renaming a
    // constant would strand this pref on a frame that no longer resolves.
    // Starts on AvatarFrame.DEFAULT (the level-1 frame) for every install.
    private val _selectedAvatarFrameId = MutableStateFlow(prefs.getString(KEY_SELECTED_AVATAR_FRAME, AvatarFrame.DEFAULT.name) ?: AvatarFrame.DEFAULT.name)
    val selectedAvatarFrameId: StateFlow<String> = _selectedAvatarFrameId.asStateFlow()

    // The cosmetic pen the player draws with (see domain.model.PenSkin) —
    // same persist-by-enum-name convention, and the same caveat: renaming a
    // constant strands this pref on a pen that no longer resolves.
    private val _selectedPenSkinId = MutableStateFlow(prefs.getString(KEY_SELECTED_PEN_SKIN, PenSkin.DEFAULT.name) ?: PenSkin.DEFAULT.name)
    val selectedPenSkinId: StateFlow<String> = _selectedPenSkinId.asStateFlow()

    // League prizes this account has actually won (see
    // domain.model.LeagueReward). Stored by reward id, the same
    // persist-by-stable-identifier convention as the two selections above.
    //
    // This is the ONLY record that a league cosmetic was earned, so it has
    // to survive a reinstall — it is carried in the cloud backup
    // (ProgressSnapshot.earnedLeagueRewardIds) for exactly that reason. A
    // prize that vanished with the app would be worse than no prize.
    private val _earnedLeagueRewardIds = MutableStateFlow(loadEarnedLeagueRewardIds())
    val earnedLeagueRewardIds: StateFlow<Set<String>> = _earnedLeagueRewardIds.asStateFlow()

    // How many times each online-lobby chat phrase (see
    // presentation.online.PRESET_PHRASES) has actually been sent from this
    // device — lets the "Bir şey söyle" sheet float a player's own most-used
    // phrases to the top instead of showing the same fixed catalog order to
    // everyone. Keyed by the phrase's own stable key, same convention as
    // KEY_SELECTED_AVATAR_FRAME storing AvatarFrame by name.
    private val _phraseUsageCounts = MutableStateFlow(loadPhraseUsageCounts())
    val phraseUsageCounts: StateFlow<Map<String, Int>> = _phraseUsageCounts.asStateFlow()

    // Same idea as [phraseUsageCounts], for the quick-send emoji row (see
    // presentation.online.EMOJI_CATALOG) — keyed by each PresetReaction's
    // own stable key, not the emoji glyph itself, so the map stays plain
    // ASCII regardless of which emoji it's counting.
    private val _emojiUsageCounts = MutableStateFlow(loadEmojiUsageCounts())
    val emojiUsageCounts: StateFlow<Map<String, Int>> = _emojiUsageCounts.asStateFlow()

    // Daily "come back and play" reminder (see notifications/DailyEngagementWorker.kt).
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // "Videolarda öne çık": the player agrees that their drawings and name may appear in Karalak's social media videos.
    // Off until they turn it on; belongs to the account (cleared with it), see FeatureConsentPublisher.
    private val _featureConsent = MutableStateFlow(prefs.getBoolean(KEY_FEATURE_CONSENT, false))
    val featureConsent: StateFlow<Boolean> = _featureConsent.asStateFlow()

    // Streak bookkeeping for the reminder worker. Not exposed as StateFlow —
    // only ever read by the background worker, never observed by the UI.
    val lastPlayedEpochDay: Long get() = prefs.getLong(KEY_LAST_PLAYED_EPOCH_DAY, -1L)
    val currentStreak: Int get() = prefs.getInt(KEY_CURRENT_STREAK, 0)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND, enabled) }
        _soundEnabled.value = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VIBRATION, enabled) }
        _vibrationEnabled.value = enabled
    }

    /**
     * Whether this account's name was ever chosen by the player, as opposed
     * to being filled in from their Google account.
     *
     * Read by [ProfileNameSynchronizer], which fills a blank name from
     * Google. Without this it re-filled ANY blank, at any moment — so
     * clearing the field to type a new name put the old one back before the
     * first new character arrived, on every screen with a nickname field.
     * Once a player has named themselves, a name they then delete is a
     * deliberately empty field, not one waiting to be helped.
     */
    var hasChosenNickname: Boolean
        get() = prefs.getBoolean(KEY_NICKNAME_CHOSEN, false)
        private set(value) = prefs.edit { putBoolean(KEY_NICKNAME_CHOSEN, value) }

    /** True once the player has spent their single allowed rename from the home screen. */
    private val _nicknameRenameUsed = MutableStateFlow(prefs.getBoolean(KEY_NICKNAME_RENAME_USED, false))
    val nicknameRenameUsed: StateFlow<Boolean> = _nicknameRenameUsed.asStateFlow()

    /** Which picture sits inside the avatar frame: "DINO", "GOOGLE", or "" (automatic: the Google photo when signed in). */
    private val _avatarSource = MutableStateFlow(prefs.getString(KEY_AVATAR_SOURCE, "") ?: "")
    val avatarSource: StateFlow<String> = _avatarSource.asStateFlow()

    fun setAvatarSource(value: String) {
        prefs.edit { putString(KEY_AVATAR_SOURCE, value) }
        _avatarSource.value = value
    }

    /** How many times this account has already changed its auto-assigned username before the final lock. */
    var usernameChanges: Int
        get() = prefs.getInt(KEY_USERNAME_CHANGES, 0)
        set(value) = prefs.edit { putInt(KEY_USERNAME_CHANGES, value) }

    /**
     * True once this device has confirmed with the server that the account owns its username.
     * Until then every app start re-checks it (two server reads); afterwards it is skipped.
     * Cleared with the rest of the account-scoped state, so a different account re-verifies.
     */
    var usernameVerified: Boolean
        get() = prefs.getBoolean(KEY_USERNAME_VERIFIED, false)
        set(value) = prefs.edit { putBoolean(KEY_USERNAME_VERIFIED, value) }

    /** Last time this device looked for unapplied moderation penalties (see PenaltyRepositoryImpl). */
    var lastPenaltyCheckMillis: Long
        get() = prefs.getLong(KEY_LAST_PENALTY_CHECK, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_PENALTY_CHECK, value) }

    /** Last time the pending-reward document was checked on this device (see ReferralRewardClaimer). */
    var lastRewardCheckMillis: Long
        get() = prefs.getLong(KEY_LAST_REWARD_CHECK, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_REWARD_CHECK, value) }

    /** True once the Play install referrer has been looked at (it only exists for the first launch after an install). */
    var installReferrerHandled: Boolean
        get() = prefs.getBoolean(KEY_INSTALL_REFERRER_HANDLED, false)
        set(value) = prefs.edit { putBoolean(KEY_INSTALL_REFERRER_HANDLED, value) }

    /** The friend code an install link carried, kept until it has been attributed (see ReferralRewardClaimer). */
    var pendingInviterCode: String?
        get() = prefs.getString(KEY_PENDING_INVITER_CODE, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_PENDING_INVITER_CODE) else putString(KEY_PENDING_INVITER_CODE, value) }

    /** Stores the server-verified username and locks it for good. */
    fun lockUsername(name: String) {
        setNickname(name)
        prefs.edit { putBoolean(KEY_NICKNAME_RENAME_USED, true) }
        _nicknameRenameUsed.value = true
    }

    fun setNickname(name: String) {
        val trimmed = name.trim()
        prefs.edit { putString(KEY_NICKNAME, trimmed) }
        _nickname.value = trimmed
        // Typing one character counts: from that keystroke on, this field
        // belongs to the player.
        if (trimmed.isNotEmpty()) hasChosenNickname = true
    }

    fun setSelectedAvatarFrame(frame: AvatarFrame) {
        prefs.edit { putString(KEY_SELECTED_AVATAR_FRAME, frame.name) }
        _selectedAvatarFrameId.value = frame.name
    }

    fun setSelectedPenSkin(skin: PenSkin) {
        prefs.edit { putString(KEY_SELECTED_PEN_SKIN, skin.name) }
        _selectedPenSkinId.value = skin.name
    }

    /**
     * Records a league prize as won. Idempotent — the same week's award is
     * read from the published table on every league open, so this is called
     * again and again for a prize already held.
     *
     * Returns true only the first time, which is what lets the caller show
     * the "you won" card exactly once instead of on every visit.
     */
    fun grantLeagueReward(rewardId: String): Boolean {
        if (rewardId.isBlank() || rewardId in _earnedLeagueRewardIds.value) return false
        val updated = _earnedLeagueRewardIds.value + rewardId
        prefs.edit { putString(KEY_EARNED_LEAGUE_REWARDS, Json.encodeToString(updated)) }
        _earnedLeagueRewardIds.value = updated
        return true
    }

    private fun loadEarnedLeagueRewardIds(): Set<String> {
        val stored = prefs.getString(KEY_EARNED_LEAGUE_REWARDS, null) ?: return emptySet()
        // A prefs value this device cannot parse is not worth crashing over,
        // and there is nothing to recover from it either.
        return runCatching { Json.decodeFromString<Set<String>>(stored) }.getOrDefault(emptySet())
    }

    /** Bumps [phraseUsageCounts] for one chat phrase — called every time it's actually sent (see OnlineGameRepositoryImpl.sendReaction). */
    fun recordPhraseUsed(key: String) {
        val updated = _phraseUsageCounts.value + (key to (_phraseUsageCounts.value[key] ?: 0) + 1)
        prefs.edit { putString(KEY_PHRASE_USAGE_COUNTS, Json.encodeToString(updated)) }
        _phraseUsageCounts.value = updated
    }

    private fun loadPhraseUsageCounts(): Map<String, Int> {
        val raw = prefs.getString(KEY_PHRASE_USAGE_COUNTS, null) ?: return emptyMap()
        return runCatching { Json.decodeFromString<Map<String, Int>>(raw) }.getOrDefault(emptyMap())
    }

    /** Bumps [emojiUsageCounts] for one quick-send emoji — called every time it's actually sent (see OnlineGameRepositoryImpl.sendReaction). */
    fun recordEmojiUsed(key: String) {
        val updated = _emojiUsageCounts.value + (key to (_emojiUsageCounts.value[key] ?: 0) + 1)
        prefs.edit { putString(KEY_EMOJI_USAGE_COUNTS, Json.encodeToString(updated)) }
        _emojiUsageCounts.value = updated
    }

    private fun loadEmojiUsageCounts(): Map<String, Int> {
        val raw = prefs.getString(KEY_EMOJI_USAGE_COUNTS, null) ?: return emptyMap()
        return runCatching { Json.decodeFromString<Map<String, Int>>(raw) }.getOrDefault(emptyMap())
    }

    // --- The economy lives in three stores of its own: the wallet (EconomyLedger), the rune map (RuneStore) and the
    // chests (ChestStore). The members below hand straight through to them, so the screens keep one place to ask. ---

    val goldBalance: StateFlow<Int> get() = ledger.goldBalance
    val jokerCounts: StateFlow<Map<JokerType, Int>> get() = ledger.jokerCounts
    val ownedStoreIds: StateFlow<Set<String>> get() = ledger.ownedStoreIds

    /** Gold earned by playing (rounds, achievements, daily challenge). Sealed like every other economy write. */
    fun earnGold(amount: Int) = ledger.addGold(amount)

    /** Takes gold back after a moderation penalty. Never below zero; sealed like every other economy write. */
    fun revokeGold(amount: Int): Int = ledger.revokeGold(amount)

    /** Buys [id] for [price] gold. False if already owned or the balance is too low. */
    fun purchaseStoreItem(id: String, price: Int): Boolean = ledger.purchaseStoreItem(id, price)

    /** Puts a store item straight into the collection (chest drop) — no gold changes hands. */
    fun grantStoreItem(id: String) = ledger.grantStoreItem(id)

    fun addJoker(type: JokerType, quantity: Int) = ledger.addJoker(type, quantity)

    /** Spends one joker; false if none are left. */
    fun useJoker(type: JokerType): Boolean = ledger.useJoker(type)

    /** Buys [quantity] jokers at [JokerType.priceFor] with the discount runes; false if the balance can't cover it. */
    fun purchaseJoker(type: JokerType, quantity: Int): Boolean =
        ledger.purchaseJoker(type, quantity, type.priceFor(quantity, RuneEffects.jokerDiscountPercent(runes.levels)))

    /** The one-time "what is new" tour (chests, jokers, store) has been shown. */
    var featureTourSeen: Boolean
        get() = prefs.getBoolean(KEY_FEATURE_TOUR_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_FEATURE_TOUR_SEEN, value) }

    /** XP the online round just finished paid out per word — handed from the match screen to its result screen (memory only). */
    @Volatile var lastOnlineRoundXp: Int = 0

    // --- Hızlı Eşleş word variety: the last words this player was dealt. ---

    /** Word ids from the player's most recent quick matches (newest last, capped). */
    fun recentQuickMatchWords(): Set<Int> =
        prefs.getString(KEY_QM_RECENT_WORDS, null)
            ?.let { runCatching { Json.decodeFromString<List<Int>>(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()

    fun rememberQuickMatchWords(ids: List<Int>) {
        val current = prefs.getString(KEY_QM_RECENT_WORDS, null)
            ?.let { runCatching { Json.decodeFromString<List<Int>>(it) }.getOrNull() }
            ?: emptyList()
        val updated = (current.filterNot { it in ids } + ids).takeLast(QM_RECENT_WORDS_CAP)
        prefs.edit { putString(KEY_QM_RECENT_WORDS, Json.encodeToString(updated)) }
    }

    // --- The daily rewarded-ad joker: one per local calendar day. Device-scoped
    // (deliberately absent from the account backup and its clear) so signing out
    // and back in cannot claim the same day's joker twice. ---
    private val _dailyJokerDay = MutableStateFlow(prefs.getLong(KEY_DAILY_JOKER_DAY, -1L))
    val dailyJokerDay: StateFlow<Long> = _dailyJokerDay.asStateFlow()

    /** Hands over today's [DailyJoker] (plus the Sabah Jokeri runes' extra) and records the day; null if already claimed. */
    @Synchronized
    fun claimDailyJoker(): JokerType? {
        val today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()
        if (_dailyJokerDay.value == today) return null
        val type = com.sualtikasifi.cizimhafiza.domain.model.DailyJoker.typeFor(today)
        prefs.edit { putLong(KEY_DAILY_JOKER_DAY, today) }
        _dailyJokerDay.value = today
        ledger.addJoker(type, 1 + RuneEffects.extraDailyJokers(runes.levels))
        return type
    }

    // --- Chests (see ChestStore) ---

    val chestSlots: StateFlow<List<Chest?>> get() = chests.chestSlots
    val chestReserve: StateFlow<Chest?> get() = chests.chestReserve
    val chestSpeedupDay: StateFlow<Long> get() = chests.chestSpeedupDay
    val chestCycleSeedForBackup: Long get() = chests.chestCycleSeedForBackup
    val chestCycleIndexForBackup: Int get() = chests.chestCycleIndexForBackup

    /** A won match's chest award: [chest] is null when every slot was full, in which case nothing drops. */
    data class ChestAward(val chest: Chest?, val instantReward: ChestReward?)

    fun awardChestForWin(): Chest? = chests.awardChestForWin()
    fun awardChestForWinOrPay(): ChestAward = ChestAward(chests.awardChestForWin(), null)

    /** Opens a chest of [tier] on the spot (a coupon's chest): its reward is paid at once and no slot is needed. */
    fun openChestNow(tier: ChestTier): ChestReward = chests.payOutright(tier)
    fun startUnlockingChest(chestId: String): Boolean = chests.startUnlocking(chestId)
    fun openChestIfReady(chestId: String): ChestReward? = chests.openIfReady(chestId)
    fun instantOpenChest(chestId: String): Boolean = chests.instantOpen(chestId)
    fun speedUpChest(chestId: String, millis: Long): Boolean = chests.speedUp(chestId, millis)

    // --- Karalak Rünleri (see RuneStore) ---

    val runeLevels: StateFlow<Map<String, Int>> get() = runes.runeLevels

    /** The player's level, from the lifetime XP. */
    fun playerLevelNow(): Int = PlayerLevel.levelForXp(_lifetimeXp.value)

    fun buyRune(id: String): Boolean = runes.buy(id, playerLevelNow())
    fun resetRunes(): Int = runes.reset()
    fun unlockAllRunes() = runes.unlockAll()
    fun restoreRuneLevels(saved: Map<String, Int>) = runes.restore(saved)
    fun instantChestAvailable(): Boolean = runes.instantChestAvailable()
    fun bestFriendBonusAvailable(): Boolean = runes.bestFriendBonusAvailable()
    fun takeBestFriendBonus(): Boolean = runes.takeBestFriendBonus()
    fun streakInsuranceAvailable(): Boolean = runes.streakInsuranceAvailable()
    fun useStreakInsurance(): Boolean = runes.useStreakInsurance()
    fun luckyDiceJoker(): JokerType? = runes.luckyDiceJoker()

    /** Percent off joker prices from the runes. */
    fun jokerDiscountPercent(): Int = RuneEffects.jokerDiscountPercent(runes.levels)

    /** True the first time it is asked for [ring] on this device: the "ring opened" Analytics event is sent once. */
    @Synchronized
    fun firstRingOpening(ring: Int): Boolean {
        val key = "$KEY_RING_OPEN_LOGGED_PREFIX$ring"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit { putBoolean(key, true) }
        return true
    }

    // --- Giriş Serisi (login streak) ---

    /** Consecutive days whose login reward was taken. */
    var loginStreak: Int
        get() = prefs.getInt(KEY_LOGIN_STREAK, 0)
        private set(value) = prefs.edit { putInt(KEY_LOGIN_STREAK, value) }

    /** Epoch day (Türkiye) of the last login reward taken, -1 if none. */
    var loginLastClaimDay: Long
        get() = prefs.getLong(KEY_LOGIN_LAST_CLAIM_DAY, -1L)
        private set(value) = prefs.edit { putLong(KEY_LOGIN_LAST_CLAIM_DAY, value) }

    /**
     * Records that [today]'s login reward is taken and moves the streak on: [continuing] false starts it over at one
     * (the player chose to start again, or never had a streak), true carries [streakBefore] on. One commit, so a
     * process that dies in the middle cannot leave the day marked without the streak having moved.
     */
    @Synchronized
    fun recordLoginClaim(today: Long, streakBefore: Int, continuing: Boolean): Int {
        val newStreak = (if (continuing) streakBefore else 0) + 1
        prefs.edit {
            putInt(KEY_LOGIN_STREAK, newStreak)
            putLong(KEY_LOGIN_LAST_CLAIM_DAY, today)
        }
        return newStreak
    }

    /** Puts an account's streak back after a restore: whichever claim is more recent wins. */
    fun restoreLoginStreak(streak: Int, lastClaimDay: Long) {
        if (lastClaimDay > loginLastClaimDay) {
            prefs.edit(commit = true) {
                putInt(KEY_LOGIN_STREAK, streak)
                putLong(KEY_LOGIN_LAST_CLAIM_DAY, lastClaimDay)
            }
        }
    }

    private val _adGoldNextAtMillis = MutableStateFlow(prefs.getLong(KEY_AD_GOLD_NEXT_AT, 0L))
    val adGoldNextAtMillis: StateFlow<Long> = _adGoldNextAtMillis.asStateFlow()

    private val _adChestNextAt = MutableStateFlow(prefs.getLong(KEY_AD_CHEST_NEXT_AT, 0L))
    /** When the free-chest ad can be taken again (epoch millis); 0 while one is ready. */
    val adChestNextAtMillis: StateFlow<Long> = _adChestNextAt.asStateFlow()

    @Synchronized
    fun isAdGoldReady(now: Long = System.currentTimeMillis()): Boolean = now >= _adGoldNextAtMillis.value

    /** Pays [AD_GOLD_AMOUNT] gold and starts the 3-hour cooldown; false while the cooldown is still running. */
    @Synchronized
    fun claimAdGold(): Boolean {
        val now = System.currentTimeMillis()
        if (now < _adGoldNextAtMillis.value) return false
        val next = now + AD_GOLD_COOLDOWN_MILLIS * (100 - RuneEffects.adCooldownReductionPercent(runes.levels)) / 100
        prefs.edit { putLong(KEY_AD_GOLD_NEXT_AT, next) }
        _adGoldNextAtMillis.value = next
        ledger.addGold(AD_GOLD_AMOUNT)
        return true
    }

    @Synchronized
    fun isAdChestReady(now: Long = System.currentTimeMillis()): Boolean = now >= _adChestNextAt.value

    /** Rolls and pays a free mid-tier chest without needing a slot and starts the 4-hour wait; null while that wait is still running. */
    @Synchronized
    fun claimAdChest(): ChestReward? {
        val now = System.currentTimeMillis()
        if (now < _adChestNextAt.value) return null
        val next = now + AD_CHEST_COOLDOWN_MILLIS * (100 - RuneEffects.adCooldownReductionPercent(runes.levels)) / 100
        val reward = chests.payOutright(ChestTier.GOLD) { putLong(KEY_AD_CHEST_NEXT_AT, next) }
        _adChestNextAt.value = next
        return reward
    }
    @Synchronized
    fun addScore(points: Int) {
        val updated = _lifetimeScore.value + points
        prefs.edit { putInt(KEY_LIFETIME_SCORE, updated) }
        _lifetimeScore.value = updated
    }

    fun addWordsDrawn(count: Int) {
        val updated = _lifetimeWordsDrawn.value + count
        prefs.edit { putInt(KEY_LIFETIME_WORDS_DRAWN, updated) }
        _lifetimeWordsDrawn.value = updated
    }

    /** Adds to the progression currency. See domain.model.XpAwards for what each action is worth. */
    /**
     * Takes XP back after a rejected round — the ONLY path in the app that
     * lowers it.
     *
     * The level is not stored, it is derived from this number
     * (PlayerLevel.levelForXp), so it follows on its own and every screen
     * reading the flow updates with it. The period total comes down too:
     * leaving it would let a rejected round keep winning the league.
     *
     * Floored at zero and committed durably rather than with apply(): the
     * record of having applied a penalty is written separately, and a
     * half-written pair would either lose the penalty or repeat it.
     */
    @Synchronized
    fun revokeXp(amount: Int): Int {
        if (amount <= 0) return 0
        val lost = minOf(amount, _lifetimeXp.value)
        val updated = (_lifetimeXp.value - amount).coerceAtLeast(0)
        val period = (_periodXp.value - amount).coerceAtLeast(0)
        prefs.edit(commit = true) {
            putInt(KEY_LIFETIME_XP, updated)
            putInt(KEY_PERIOD_XP, period)
        }
        _lifetimeXp.value = updated
        _periodXp.value = period
        return lost
    }

    /**
     * How many moderation penalties this device has applied.
     *
     * Account-scoped and carried into the backup snapshot, because it is what
     * lets the restore guards tell a penalty apart from data loss — see
     * ProgressSnapshot.penaltiesApplied.
     */
    var penaltiesApplied: Int
        get() = prefs.getInt(KEY_PENALTIES_APPLIED, 0)
        set(value) {
            prefs.edit(commit = true) { putInt(KEY_PENALTIES_APPLIED, value) }
        }

    /**
     * The Hızlı Eşleş walk-away penalty: takes [GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP] XP and counts as an applied
     * penalty, so the backup guards treat the lower total as legitimate rather than as data loss (same as PenaltyRepositoryImpl).
     */
    fun applyQuickMatchAbandonPenalty() {
        revokeXp(GameConstants.QUICK_MATCH_ABANDON_PENALTY_XP)
        penaltiesApplied = penaltiesApplied + 1
    }

    @Synchronized
    fun addXp(amount: Int) {
        if (amount <= 0) return
        // The XP runes speed up the player's LEVEL only; the league (period) XP stays exactly what was earned, so a rune
        // never buys a better league place.
        val gained = amount + amount * RuneEffects.xpPercent(runes.levels) / 100
        val updated = _lifetimeXp.value + gained
        prefs.edit { putInt(KEY_LIFETIME_XP, updated) }
        _lifetimeXp.value = updated
        addPeriodXp(amount)
        HomeShownStats.pendingXp += gained
    }

    /**
     * Last calendar day a Hızlı Eşleş (Quick Match) round's daily 2x-XP
     * bonus was actually claimed (see GameConstants.
     * QUICK_MATCH_DAILY_BONUS_MULTIPLIER) — read-only here; only
     * [claimQuickMatchDailyBonus] advances it.
     */
    val lastQuickMatchEpochDay: Long get() = prefs.getLong(KEY_LAST_QUICK_MATCH_EPOCH_DAY, -1L)

    /**
     * Marks today as having paid the Quick Match daily bonus. Idempotent
     * within a day: returns false (and writes nothing) if today was already
     * claimed. Called once, from GameViewModel.finishGame(), only for a
     * quick match round that actually finished — a round started and
     * abandoned never spends the day's bonus.
     */
    fun claimQuickMatchDailyBonus(): Boolean {
        val today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()
        if (lastQuickMatchEpochDay == today) return false
        prefs.edit { putLong(KEY_LAST_QUICK_MATCH_EPOCH_DAY, today) }
        return true
    }

    // --- League period (see domain.model.LeaguePeriod) ---

    /**
     * XP earned since the first of the month. Rolls over lazily on read and
     * write rather than by a scheduled job: a worker that failed to fire
     * would carry last month's total into the new table, which is far worse
     * than computing the boundary on demand from the date.
     */
    /**
     * One-time clean-up: until this version, a score that had been counted in one month could be copied
     * into the next (an app left open across midnight, or a backup written just after it, kept last month's
     * number and stamped it with the new month's id), so some accounts started a month with last month's XP.
     * The month's XP is reset once; from here on it is always read against the current month.
     */
    private fun periodXpAfterFix(): Int {
        if (!prefs.getBoolean(KEY_PERIOD_XP_FIX_V2, false)) {
            prefs.edit {
                putInt(KEY_PERIOD_XP, 0)
                putBoolean(KEY_PERIOD_XP_FIX_V2, true)
            }
        }
        return readPeriodXp()
    }

    private val _periodXp = MutableStateFlow(periodXpAfterFix())
    val periodXp: StateFlow<Int> = _periodXp.asStateFlow()

    private fun readPeriodXp(): Int {
        val currentPeriod = LeaguePeriod.periodIdFor(com.sualtikasifi.cizimhafiza.util.TurkeyTime.today())
        if (prefs.getLong(KEY_PERIOD_XP_PERIOD, -1L) != currentPeriod) return 0
        return prefs.getInt(KEY_PERIOD_XP, 0)
    }

    /**
     * Brings back what a reinstall would otherwise zero: this month's league XP (only if the backup
     * is from the same month) and the ad-reward cooldowns (the later of local and backed-up, so a
     * reinstall is never a way to collect a reward twice).
     */
    @Synchronized
    fun restoreEngagement(periodXp: Int, periodId: Long, adGoldNextAt: Long, adChestNextAt: Long) {
        val currentPeriod = LeaguePeriod.periodIdFor(com.sualtikasifi.cizimhafiza.util.TurkeyTime.today())
        if (periodId == currentPeriod && periodXp > _periodXp.value) {
            prefs.edit {
                putLong(KEY_PERIOD_XP_PERIOD, currentPeriod)
                putInt(KEY_PERIOD_XP, periodXp)
            }
            _periodXp.value = periodXp
        }
        if (adGoldNextAt > _adGoldNextAtMillis.value) {
            prefs.edit { putLong(KEY_AD_GOLD_NEXT_AT, adGoldNextAt) }
            _adGoldNextAtMillis.value = adGoldNextAt
        }
        if (adChestNextAt > _adChestNextAt.value) {
            prefs.edit { putLong(KEY_AD_CHEST_NEXT_AT, adChestNextAt) }
            _adChestNextAt.value = adChestNextAt
        }
    }

    private fun addPeriodXp(amount: Int) {
        val currentPeriod = LeaguePeriod.periodIdFor(com.sualtikasifi.cizimhafiza.util.TurkeyTime.today())
        val storedPeriod = prefs.getLong(KEY_PERIOD_XP_PERIOD, -1L)
        val base = if (storedPeriod == currentPeriod) prefs.getInt(KEY_PERIOD_XP, 0) else 0
        val updated = base + amount
        prefs.edit {
            putLong(KEY_PERIOD_XP_PERIOD, currentPeriod)
            putInt(KEY_PERIOD_XP, updated)
        }
        _periodXp.value = updated
    }

    /**
     * What LeagueScorePublisher last successfully wrote onto the public
     * profile. Persisted rather than held in memory so relaunching the app
     * with nothing new to say costs no Firestore write at all.
     */
    var publishedLeagueScoreSignature: String?
        get() = prefs.getString(KEY_PUBLISHED_LEAGUE_SIGNATURE, null)
        set(value) = prefs.edit { putString(KEY_PUBLISHED_LEAGUE_SIGNATURE, value) }

    /** Re-reads the period total; call on resume in case the month rolled over while the app sat open. */
    fun refreshPeriodXp() {
        _periodXp.value = readPeriodXp()
    }

    /**
     * This month's XP, read against the current month right now (and the observable value brought up to
     * date). Everything that WRITES the number somewhere — the league row, the cloud backup — uses this and
     * not [periodXp]'s last value, which can still be last month's if the app sat open past midnight.
     */
    fun currentPeriodXp(): Int {
        val value = readPeriodXp()
        if (_periodXp.value != value) _periodXp.value = value
        return value
    }

    /**
     * Bumps the per-finished-game lifetime tallies the achievement catalog
     * reads. Called once per saved game (solo or online) alongside
     * [addScore]/[addWordsDrawn] — see GameRepositoryImpl.finishSaving.
     */
    fun recordFinishedGame(wasPerfectRound: Boolean, wasOnlineWin: Boolean) {
        _gamePlayedThisRun.value = true
        prefs.edit {
            putInt(KEY_LIFETIME_GAMES_PLAYED, lifetimeGamesPlayed + 1)
            if (wasPerfectRound) putInt(KEY_LIFETIME_PERFECT_ROUNDS, lifetimePerfectRounds + 1)
            if (wasOnlineWin) putInt(KEY_LIFETIME_ONLINE_WINS, lifetimeOnlineWins + 1)
        }
    }

    /**
     * One-time migration for devices that earned a rank before progression
     * moved from raw score to XP. Grants exactly enough XP to land at the
     * floor of the tier that score had already unlocked, so nobody opens the
     * update to find themselves demoted to Karalamacı.
     */
    @Synchronized
    fun seedLifetimeXpFromLegacyScore(legacyScore: Int) {
        if (prefs.contains(KEY_LIFETIME_XP)) return
        // The old score thresholds, paired with the level each tier now starts at.
        val legacyTiers = listOf(0 to 1, 1000 to 20, 3000 to 40, 5000 to 60, 10000 to 80, 25000 to PlayerLevel.MAX_LEVEL)
        val earnedLevel = legacyTiers.last { legacyScore >= it.first }.second
        val seeded = PlayerLevel.totalXpForLevel(earnedLevel)
        prefs.edit { putInt(KEY_LIFETIME_XP, seeded) }
        _lifetimeXp.value = seeded
    }

    /** One-time seed from surviving local game history, only if no lifetime score has been recorded yet. */
    @Synchronized
    fun seedLifetimeScoreIfAbsent(fallbackScore: Int) {
        if (prefs.contains(KEY_LIFETIME_SCORE)) return
        prefs.edit { putInt(KEY_LIFETIME_SCORE, fallbackScore) }
        _lifetimeScore.value = fallbackScore
    }

    /**
     * Erases everything that belongs to the PLAYER rather than to the phone,
     * so the next account starts from a genuinely clean slate.
     *
     * The split is the whole point. Sound/music/vibration, the notification
     * toggle, whether the tutorial has been seen and the bot-training gate
     * are properties of this device and its owner's preferences — they
     * survive. Every counter, streak, cosmetic choice and name below is
     * part of a player's progress and MUST NOT be visible under somebody
     * else's account: leaving any one of them behind is exactly how a
     * level 4 profile kept showing up on a brand-new account.
     *
     * Adding a new progress-bearing preference means adding it here too —
     * a key left out of this list is a key that leaks across accounts.
     *
     * The counters are written as explicit ZEROES rather than removed, and
     * that difference matters: [seedLifetimeScoreIfAbsent] and
     * [seedLifetimeXpFromLegacyScore] run on every launch and both key off
     * `prefs.contains(...)`, so a removed key is an invitation for them to
     * reconstruct a level from whatever local game history survived. A key
     * that is present and zero is a key those migrations leave alone —
     * there is no path back to the old number.
     *
     * Uses commit() rather than apply(): the caller wipes and then restarts
     * the process (see util.AppRestarter), and apply()'s write is
     * asynchronous — a restart racing it could come back up with some keys
     * still holding the previous account's values.
     */
    fun clearAccountScopedState() {
        prefs.edit(commit = true) { stageAccountScopedClear() }
        _lifetimeScore.value = 0
        _lifetimeXp.value = 0
        _lifetimeWordsDrawn.value = 0
        _nickname.value = ""
        _nicknameRenameUsed.value = false
        _usernameOfferPending.value = false
        _avatarSource.value = ""
        _featureConsent.value = false
        _selectedAvatarFrameId.value = AvatarFrame.DEFAULT.name
        _selectedPenSkinId.value = PenSkin.DEFAULT.name
        _periodXp.value = 0
        _phraseUsageCounts.value = emptyMap()
        _emojiUsageCounts.value = emptyMap()
        _earnedLeagueRewardIds.value = emptySet()
        ledger.publish(0, emptyMap(), emptySet())
        chests.publishAccount(List(ChestSlots.SLOT_COUNT) { null })
        runes.publishCleared()
    }

    /**
     * Stages the clear onto an editor the CALLER commits, rather than
     * committing one of its own.
     *
     * That is the whole point of it being separate. [replaceWithAccount]
     * needs the clear and the restore to reach the disk as one write; when
     * it could only get the clear by calling something that committed on its
     * own, the disk went through a state where the account was wiped and the
     * new values had not arrived yet. See [replaceWithAccount] for what that
     * cost.
     */
    private fun SharedPreferences.Editor.stageAccountScopedClear() {
        putInt(KEY_LIFETIME_SCORE, 0)
        putInt(KEY_LIFETIME_XP, 0)
        putInt(KEY_PENALTIES_APPLIED, 0)
        putInt(KEY_LIFETIME_WORDS_DRAWN, 0)
        putInt(KEY_LIFETIME_GAMES_PLAYED, 0)
        putInt(KEY_LIFETIME_PERFECT_ROUNDS, 0)
        putInt(KEY_LIFETIME_ONLINE_WINS, 0)
        putInt(KEY_BEST_STREAK, 0)
        putString(KEY_NICKNAME, "")
        // The incoming account has not named itself on this device, so
        // it should get its own Google name rather than inheriting the
        // previous player's "leave it blank" decision.
        putBoolean(KEY_NICKNAME_CHOSEN, false)
        remove(KEY_NICKNAME_RENAME_USED)
        remove(KEY_USERNAME_CHANGES)
        remove(KEY_USERNAME_OFFER_SHOWN)
        remove(KEY_USERNAME_OFFER_PENDING)
        remove(KEY_RATING_PROMPT_PENDING)
        remove(KEY_USERNAME_VERIFIED)
        remove(KEY_LAST_REWARD_CHECK)
        remove(KEY_LAST_PENALTY_CHECK)
        remove(KEY_AVATAR_SOURCE)
        // Agreeing to appear in videos was this person's decision, not the next account's.
        remove(KEY_FEATURE_CONSENT)
        putString(KEY_SELECTED_AVATAR_FRAME, AvatarFrame.DEFAULT.name)
        putString(KEY_SELECTED_PEN_SKIN, PenSkin.DEFAULT.name)
        // The league standing is this player's, not the phone's —
        // left behind, the new account would open the league table
        // already holding somebody else's XP for the week.
        putInt(KEY_PERIOD_XP, 0)
        remove(KEY_PERIOD_XP_PERIOD)
        // Prizes belong to the account that won them, not to the phone.
        remove(KEY_EARNED_LEAGUE_REWARDS)
        // Same for the play streak the reminder worker tracks.
        remove(KEY_LAST_PLAYED_EPOCH_DAY)
        putInt(KEY_CURRENT_STREAK, 0)
        // The Quick Match daily bonus belongs to the account, not the phone.
        remove(KEY_LAST_QUICK_MATCH_EPOCH_DAY)
        // Gold and chests are this account's economy, not the phone's — left
        // behind, a new account would inherit somebody else's gold and
        // half-opened chests.
        // The login streak is this account's too.
        remove(KEY_LOGIN_STREAK)
        remove(KEY_LOGIN_LAST_CLAIM_DAY)
        // The runes are cleared AND resealed, and shown cleared in memory by the caller: before, the in-memory levels of
        // the previous account survived and were merged into the next one's on restore.
        with(runes) { stageClear() }
        with(ledger) { stageEconomy(0, emptyMap(), emptySet()) }
        with(chests) { stageClear() }
        // LeagueScorePublisher skips the write when the signature it
        // last published still matches. Carried over, the new account
        // would look like it had already published — and would never
        // appear in its own friends' league table at all.
        remove(KEY_PUBLISHED_LEAGUE_SIGNATURE)
        remove(KEY_PHRASE_USAGE_COUNTS)
        remove(KEY_EMOJI_USAGE_COUNTS)
    }

    /**
     * Adopts an account's cloud backup outright, replacing whatever this
     * device held — used only when the signed-in uid itself changed, never
     * for an ordinary restore.
     *
     * Replaces rather than merges, and that distinction is the whole fix:
     * a merge assumes this device's numbers and the backup describe the
     * SAME player at two points in time, but across an account switch they
     * describe two DIFFERENT players — so a level 4 profile must not
     * survive a max() against a level 1 account it has nothing to do with.
     * Every account-scoped key is staged to zero first, so a field the
     * backup happens not to carry is left at zero rather than at the
     * previous account's value.
     *
     * ### One commit, and why this is the bug that ate an account
     *
     * This used to clear by calling [clearAccountScopedState] — which
     * commits — and then write the restored values with `prefs.edit { }`,
     * which is `apply()` and therefore ASYNCHRONOUS. Immediately afterwards
     * the caller restarts the process (util.AppRestarter →
     * `Runtime.getRuntime().exit(0)`), and `exit()` does not flush pending
     * `apply()` writes: the framework only waits for them at Activity
     * lifecycle transitions, never at an arbitrary process exit.
     *
     * So the disk got the zeroes, durably, and then the process died before
     * the level-5 profile that was supposed to replace them ever left
     * memory. The app came back up, read the zeroes, and the account was
     * gone — a signed-out-and-back-in player put at level 1. Being a race,
     * it survived every reasoned walk through the code and only ever showed
     * up on a real device.
     *
     * The fix is not a bigger `commit`: it is that there must be no moment,
     * on disk, where this account is cleared but not yet restored. Both
     * halves go into one editor and land together or not at all.
     */
    @Synchronized
    fun replaceWithAccount(
        lifetimeScore: Int,
        lifetimeXp: Int,
        lifetimeWordsDrawn: Int,
        lifetimeGamesPlayed: Int,
        lifetimePerfectRounds: Int,
        lifetimeOnlineWins: Int,
        bestStreak: Int,
        nickname: String,
        selectedAvatarFrameId: String,
        selectedPenSkinId: String,
        earnedLeagueRewardIds: Set<String>,
        goldBalance: Int = 0,
        ownedStoreIds: Set<String> = emptySet(),
        jokerCounts: Map<JokerType, Int> = emptyMap(),
        chestSlots: List<Chest?> = List(ChestSlots.SLOT_COUNT) { null },
        restoredChestCycleSeed: Long = 0L,
        restoredChestCycleIndex: Int = 0
    ) {
        val frame = selectedAvatarFrameId.ifBlank { AvatarFrame.DEFAULT.name }
        val pen = selectedPenSkinId.ifBlank { PenSkin.DEFAULT.name }
        prefs.edit(commit = true) {
            stageAccountScopedClear()
            putInt(KEY_LIFETIME_SCORE, lifetimeScore)
            putInt(KEY_LIFETIME_XP, lifetimeXp)
            putInt(KEY_LIFETIME_WORDS_DRAWN, lifetimeWordsDrawn)
            putInt(KEY_LIFETIME_GAMES_PLAYED, lifetimeGamesPlayed)
            putInt(KEY_LIFETIME_PERFECT_ROUNDS, lifetimePerfectRounds)
            putInt(KEY_LIFETIME_ONLINE_WINS, lifetimeOnlineWins)
            putInt(KEY_BEST_STREAK, bestStreak)
            putString(KEY_NICKNAME, nickname)
            // A restored account that already had a name had chosen one; an
            // account whose backup carries no name has not, and should still
            // be offered its Google one.
            putBoolean(KEY_NICKNAME_CHOSEN, nickname.isNotBlank())
            putString(KEY_SELECTED_AVATAR_FRAME, frame)
            putString(KEY_SELECTED_PEN_SKIN, pen)
            putString(KEY_EARNED_LEAGUE_REWARDS, Json.encodeToString(earnedLeagueRewardIds))
            // Chest slots travel with the account (see ChestBackupCodec), along
            // with the seed/position of the tier cycle so the next chests
            // keep following the same shuffle after a reinstall.
            with(chests) { stageRestore(chestSlots, restoredChestCycleSeed, restoredChestCycleIndex) }
            with(ledger) { stageEconomy(goldBalance, jokerCounts, ownedStoreIds) }
        }
        _periodXp.value = 0
        _phraseUsageCounts.value = emptyMap()
        _emojiUsageCounts.value = emptyMap()
        _earnedLeagueRewardIds.value = earnedLeagueRewardIds
        _lifetimeScore.value = lifetimeScore
        _lifetimeXp.value = lifetimeXp
        _lifetimeWordsDrawn.value = lifetimeWordsDrawn
        _nickname.value = nickname
        _usernameOfferPending.value = false
        _selectedAvatarFrameId.value = frame
        _selectedPenSkinId.value = pen
        ledger.publish(goldBalance, jokerCounts, ownedStoreIds)
        chests.publishAccount(chestSlots)
        // The account's runes come back through restoreRuneLevels, merged into an empty map — not into the last account's.
        runes.publishCleared()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
        _notificationsEnabled.value = enabled
    }

    fun setFeatureConsent(on: Boolean) {
        prefs.edit { putBoolean(KEY_FEATURE_CONSENT, on) }
        _featureConsent.value = on
    }

    /** "uid|true" or "uid|false": what FeatureConsentPublisher last wrote to which profile. */
    var publishedFeatureConsent: String?
        get() = prefs.getString(KEY_PUBLISHED_FEATURE_CONSENT, null)
        set(value) = prefs.edit { putString(KEY_PUBLISHED_FEATURE_CONSENT, value) }

    /**
     * The last day the daily reminder actually posted a notification.
     *
     * Two independent schedulers now drive that reminder — an alarm and a
     * WorkManager backstop, see NotificationScheduler — precisely because
     * either one alone can be silently dropped by the OS. That redundancy is
     * the point, and this is what keeps it from being felt: whichever fires
     * first claims the day, and the other finds it taken and does nothing.
     *
     * Device-scoped, NOT account-scoped: it describes what this phone's
     * status bar has already shown today, which has nothing to do with who
     * is signed in — so it is deliberately absent from
     * [clearAccountScopedState], where clearing it would let a sign-out
     * produce a second reminder on the same day.
     */
    var lastReminderEpochDay: Long
        get() = prefs.getLong(KEY_LAST_REMINDER_EPOCH_DAY, -1L)
        set(value) = prefs.edit { putLong(KEY_LAST_REMINDER_EPOCH_DAY, value) }

    // False until the first-run tutorial (see presentation/tutorial/) has been
    // played or skipped — decides the app's start destination on launch.
    var tutorialCompleted: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)
        set(value) = prefs.edit { putBoolean(KEY_TUTORIAL_COMPLETED, value) }

    /**
     * Whether this device has ever entered the Bot Eğitim passcode (see
     * BotTrainingGate). Remembered rather than asked every time: the gate is
     * there to keep the tile from being wandered into by players, not to
     * defend the screen from the person holding the phone — and the handful
     * of people actually training the bot would otherwise retype the code on
     * every cold start.
     *
     * The whole feature, gate included, comes out once training is done.
     */
    var botTrainingUnlocked: Boolean
        get() = prefs.getBoolean(KEY_BOT_TRAINING_UNLOCKED, false)
        set(value) = prefs.edit { putBoolean(KEY_BOT_TRAINING_UNLOCKED, value) }

    // Guards the one-time automatic permission prompt on the home screen
    // (MainMenuScreen.NotificationPermissionWhenQuiet) so it is only ever asked once.
    var notificationPermissionRequested: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, value) }

    // Guards the one-time post-first-match Google sign-in nudge and the
    // one-time post-third-match Play Store rating nudge (see
    // util/PostMatchPrompts.kt) — each flips true the moment its dialog is
    // shown, not when the player acts on it, so a dismissed prompt never
    // comes back either. Device-scoped like tutorialCompleted: what this
    // phone has already interrupted the player with has nothing to do with
    // which account is signed in.
    var signInPromptShown: Boolean
        get() = prefs.getBoolean(KEY_SIGN_IN_PROMPT_SHOWN, false)
        set(value) = prefs.edit { putBoolean(KEY_SIGN_IN_PROMPT_SHOWN, value) }

    /** How many of the later "don't lose your progress" reminders (see PostMatchPrompts.SIGN_IN_REMINDER_AT) were already shown. */
    /** The Runes screen's one-time explanation has been read. */
    var runesIntroSeen: Boolean
        get() = prefs.getBoolean(KEY_RUNES_INTRO_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_RUNES_INTRO_SEEN, value) }

    /** Until when (epoch ms) the mascot keeps its "your gold buys a rune" offer to itself, after the player closed it. */
    var runePromptSnoozedUntil: Long
        get() = prefs.getLong(KEY_RUNE_PROMPT_SNOOZED_UNTIL, 0L)
        set(value) = prefs.edit { putLong(KEY_RUNE_PROMPT_SNOOZED_UNTIL, value) }

    /** The "Yenilikler" tour's rune page has been shown (players who saw the tour before it had one get just that page). */
    var runesTourSeen: Boolean
        get() = prefs.getBoolean(KEY_RUNES_TOUR_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_RUNES_TOUR_SEEN, value) }

    var signInReminderStage: Int
        get() = prefs.getInt(KEY_SIGN_IN_REMINDER_STAGE, 0)
        set(value) = prefs.edit { putInt(KEY_SIGN_IN_REMINDER_STAGE, value) }

    // Read from both files so a device that already answered before the flag moved to flagPrefs is not asked again.
    var ratingPromptShown: Boolean
        get() = flagPrefs.getBoolean(KEY_RATING_PROMPT_SHOWN, false) || prefs.getBoolean(KEY_RATING_PROMPT_SHOWN, false)
        set(value) = flagPrefs.edit { putBoolean(KEY_RATING_PROMPT_SHOWN, value) }

    /** Quick Match rounds finished to the end. The rating ask comes after the third. Survives a reinstall like [ratingPromptShown]. */
    /**
     * The tutorial's gift: three First Letter and three Letter Count jokers, once per install. Returns whether
     * it was granted now (false when it already had been, e.g. the tutorial replayed from Settings).
     */
    @Synchronized
    fun grantTutorialJokersOnce(): Boolean {
        if (flagPrefs.getBoolean(KEY_TUTORIAL_JOKERS_GRANTED, false)) return false
        flagPrefs.edit { putBoolean(KEY_TUTORIAL_JOKERS_GRANTED, true) }
        addJoker(JokerType.FIRST_LETTER, TUTORIAL_JOKER_GIFT)
        addJoker(JokerType.LETTER_COUNT, TUTORIAL_JOKER_GIFT)
        return true
    }

    val quickMatchGamesFinished: Int get() = flagPrefs.getInt(KEY_QUICK_MATCH_GAMES_FINISHED, 0)

    fun recordQuickMatchFinished() {
        flagPrefs.edit { putInt(KEY_QUICK_MATCH_GAMES_FINISHED, quickMatchGamesFinished + 1) }
    }

    /**
     * The rating ask is due: set at the moment a Quick Match round FINISHES with at least three on
     * the count, never from restored data. That is what keeps it from appearing the instant a
     * reinstalled account's counters come back — it waits for a round actually played. Account-scoped.
     */
    var ratingPromptPending: Boolean
        get() = prefs.getBoolean(KEY_RATING_PROMPT_PENDING, false)
        set(value) = prefs.edit { putBoolean(KEY_RATING_PROMPT_PENDING, value) }

    fun markRatingDueIfEligible() {
        if (!ratingPromptShown && quickMatchGamesFinished >= com.sualtikasifi.cizimhafiza.util.PostMatchPrompts.RATING_PROMPT_AT_GAME) {
            ratingPromptPending = true
        }
    }

    /** Restores the prompts' state from a cloud backup: never un-shows a prompt, never lowers the count. */
    fun restoreOneTimePrompts(ratingShown: Boolean, quickMatchGames: Int) {
        flagPrefs.edit {
            if (ratingShown) putBoolean(KEY_RATING_PROMPT_SHOWN, true)
            if (quickMatchGames > quickMatchGamesFinished) putInt(KEY_QUICK_MATCH_GAMES_FINISHED, quickMatchGames)
        }
    }

    private val _usernameOfferPending = MutableStateFlow(prefs.getBoolean(KEY_USERNAME_OFFER_PENDING, false))

    /**
     * True only after a guest account was linked to Google for the first time on this device (see
     * AccountViewModel.signIn, LinkedToDevice). Signing in to an account that already exists —
     * a reinstall, a second phone — never sets it, so a returning account is never offered the
     * name again. Dropped with the rest of the account-scoped state on an account switch.
     */
    val usernameOfferPending: StateFlow<Boolean> = _usernameOfferPending.asStateFlow()

    fun requestUsernameOffer() {
        prefs.edit { putBoolean(KEY_USERNAME_OFFER_PENDING, true) }
        _usernameOfferPending.value = true
    }

    /** The one-time "keep your name or change it" offer was shown to this account. Account-scoped: cleared with the rest on an account switch. */
    var usernameOfferShown: Boolean
        get() = prefs.getBoolean(KEY_USERNAME_OFFER_SHOWN, false)
        set(value) = prefs.edit { putBoolean(KEY_USERNAME_OFFER_SHOWN, value) }


    /**
     * Called whenever a game (solo or online) finishes. Extends the streak by
     * one if the player last played yesterday, leaves it alone if they've
     * already played today, and otherwise resets it to a fresh streak of 1.
     */
    fun updateStreakOnPlay() {
        val today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()
        val newStreak = when (today - lastPlayedEpochDay) {
            0L -> currentStreak
            1L -> currentStreak + 1
            else -> 1
        }
        prefs.edit {
            putLong(KEY_LAST_PLAYED_EPOCH_DAY, today)
            putInt(KEY_CURRENT_STREAK, newStreak)
            // High-water mark, so a "longest streak" achievement stays earned
            // after the active streak resets (currentStreak drops back to 1).
            if (newStreak > bestStreak) putInt(KEY_BEST_STREAK, newStreak)
        }
    }

    internal companion object {
        const val PREFS_NAME = "cizim_hafiza_settings"
        const val KEY_MUSIC = "music_enabled"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_NICKNAME = "online_nickname"
        const val KEY_SELECTED_AVATAR_FRAME = "selected_avatar_frame"
        const val KEY_SELECTED_PEN_SKIN = "selected_pen_skin"
        // Renamed from the weekly keys rather than reused: the value means a
        // month now, and an upgrading device must start the new period at zero
        // instead of inheriting a part-week total as its monthly one.
        const val KEY_PERIOD_XP = "period_xp"
        const val KEY_PERIOD_XP_PERIOD = "period_xp_period_id"
        const val KEY_PHRASE_USAGE_COUNTS = "chat_phrase_usage_counts"
        const val KEY_EMOJI_USAGE_COUNTS = "chat_emoji_usage_counts"
        const val KEY_EARNED_LEAGUE_REWARDS = "earned_league_rewards"
        const val KEY_LIFETIME_SCORE = "lifetime_score"
        const val KEY_LIFETIME_XP = "lifetime_xp"
        const val KEY_PENALTIES_APPLIED = "penalties_applied"
        const val KEY_LIFETIME_WORDS_DRAWN = "lifetime_words_drawn"
        const val KEY_LIFETIME_GAMES_PLAYED = "lifetime_games_played"
        const val KEY_LIFETIME_PERFECT_ROUNDS = "lifetime_perfect_rounds"
        const val KEY_LIFETIME_ONLINE_WINS = "lifetime_online_wins"
        const val KEY_BEST_STREAK = "best_streak"
        const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_FEATURE_CONSENT = "feature_consent"
        const val KEY_PUBLISHED_FEATURE_CONSENT = "published_feature_consent"
        const val KEY_LAST_PLAYED_EPOCH_DAY = "last_played_epoch_day"
        const val KEY_LAST_QUICK_MATCH_EPOCH_DAY = "last_quick_match_epoch_day"
        const val KEY_GOLD_BALANCE = "gold_balance"
        const val KEY_OWNED_STORE_IDS = "owned_store_ids"
        const val KEY_JOKERS = "joker_counts"
        const val KEY_FEATURE_TOUR_SEEN = "feature_tour_seen"
        const val KEY_ECONOMY_SEAL = "economy_seal"
        const val KEY_RUNE_SEAL = "rune_seal"
        const val KEY_RING_OPEN_LOGGED_PREFIX = "rune_ring_open_logged_"
        const val KEY_QM_RECENT_WORDS = "qm_recent_words"
        const val QM_RECENT_WORDS_CAP = 300
        const val KEY_CHEST_SPEEDUP_DAY = "chest_speedup_day"
        const val KEY_DAILY_JOKER_DAY = "daily_joker_day"
        const val KEY_CHEST_SLOTS = "chest_slots"
        const val KEY_CHEST_CYCLE_SEED = "chest_cycle_seed"
        const val KEY_CHEST_CYCLE_INDEX = "chest_cycle_index"
        const val KEY_RUNE_LEVELS = "rune_levels"
        const val KEY_CHEST_RESERVE = "chest_reserve"
        const val KEY_INSTANT_CHEST_DAY = "instant_chest_day"
        const val KEY_BEST_FRIEND_DAY = "best_friend_day"
        const val KEY_INSURANCE_MONTH = "streak_insurance_month"
        const val KEY_LOGIN_STREAK = "login_streak"
        const val KEY_LOGIN_LAST_CLAIM_DAY = "login_last_claim_day"
        const val KEY_AD_GOLD_NEXT_AT = "ad_gold_next_at"
        const val KEY_AD_CHEST_NEXT_AT = "ad_chest_next_at"
        // From config/economy (see EconomyConfig).
        val AD_CHEST_COOLDOWN_MILLIS: Long get() = EconomyConfig.adChestCooldownHours * 60 * 60 * 1000L
        val AD_GOLD_AMOUNT: Int get() = EconomyConfig.adGoldAmount
        val AD_GOLD_COOLDOWN_MILLIS: Long get() = EconomyConfig.adGoldCooldownHours * 60 * 60 * 1000L
        const val KEY_CURRENT_STREAK = "current_streak"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        const val KEY_LAST_REMINDER_EPOCH_DAY = "last_reminder_epoch_day"
        const val KEY_RUNES_INTRO_SEEN = "runes_intro_seen"
        const val KEY_RUNE_PROMPT_SNOOZED_UNTIL = "rune_prompt_snoozed_until"
        const val KEY_RUNES_TOUR_SEEN = "runes_tour_seen"
        const val KEY_SIGN_IN_REMINDER_STAGE = "sign_in_reminder_stage"
        const val KEY_SIGN_IN_PROMPT_SHOWN = "sign_in_prompt_shown"
        const val KEY_RATING_PROMPT_SHOWN = "rating_prompt_shown"
        const val KEY_QUICK_MATCH_GAMES_FINISHED = "quick_match_games_finished"
        const val KEY_TUTORIAL_JOKERS_GRANTED = "tutorial_jokers_granted"
        const val TUTORIAL_JOKER_GIFT = 3
        const val KEY_USERNAME_OFFER_SHOWN = "username_offer_shown"
        const val KEY_PERIOD_XP_FIX_V2 = "period_xp_fix_v2"
        const val KEY_USERNAME_OFFER_PENDING = "username_offer_pending"
        const val KEY_RATING_PROMPT_PENDING = "rating_prompt_pending"
        const val FLAGS_PREFS_NAME = "one_time_flags"
        const val KEY_NICKNAME_CHOSEN = "nickname_chosen_by_player"
        const val KEY_NICKNAME_RENAME_USED = "nickname_rename_used"
        const val KEY_USERNAME_CHANGES = "username_changes"
        const val KEY_USERNAME_VERIFIED = "username_verified"
        const val KEY_LAST_REWARD_CHECK = "last_reward_check_millis"
        const val KEY_INSTALL_REFERRER_HANDLED = "install_referrer_handled"
        const val KEY_PENDING_INVITER_CODE = "pending_inviter_code"
        const val KEY_LAST_PENALTY_CHECK = "last_penalty_check_millis"
        const val KEY_AVATAR_SOURCE = "avatar_source"
        const val NICKNAME_MIN = 2
        const val NICKNAME_MAX = 16
        const val KEY_BOT_TRAINING_UNLOCKED = "bot_training_unlocked"
        const val KEY_PUBLISHED_LEAGUE_SIGNATURE = "published_league_score_signature"
    }
}
