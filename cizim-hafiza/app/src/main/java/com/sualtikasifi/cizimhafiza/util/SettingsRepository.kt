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
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    // --- Gold (spent, eventually, in a cosmetics shop — not built yet).
    // Earned ONLY from opening a chest, never directly from a match. ---

    private val _goldBalance = MutableStateFlow(prefs.getInt(KEY_GOLD_BALANCE, 0))
    val goldBalance: StateFlow<Int> = _goldBalance.asStateFlow()

    @Synchronized
    private fun addGold(amount: Int) {
        if (amount <= 0) return
        val updated = _goldBalance.value + amount
        prefs.edit { putInt(KEY_GOLD_BALANCE, updated); sealEconomy(gold = updated) }
        _goldBalance.value = updated
    }

    /** The one-time "what is new" tour (chests, jokers, store) has been shown. */
    var featureTourSeen: Boolean
        get() = prefs.getBoolean(KEY_FEATURE_TOUR_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_FEATURE_TOUR_SEEN, value) }

    /** Gold earned by playing (achievements, level stars, daily challenge). Sealed like every other economy write. */
    fun earnGold(amount: Int) = addGold(amount)

    // --- Economy tamper seal (see EconomyGuard). Every write of gold / jokers /
    // store items also writes the seal in the SAME edit, so the two can never
    // disagree after a crash. ---

    private fun economyPayload(gold: Int, jokers: Map<JokerType, Int>, owned: Set<String>): String =
        "$gold|" + jokers.entries.filter { it.value != 0 }.sortedBy { it.key.name }.joinToString(",") { "${it.key.name}=${it.value}" } +
            "|" + owned.sorted().joinToString(",")

    private fun android.content.SharedPreferences.Editor.sealEconomy(
        gold: Int = _goldBalance.value,
        jokers: Map<JokerType, Int> = _jokerCounts.value,
        owned: Set<String> = _ownedStoreIds.value
    ) {
        putString(KEY_ECONOMY_SEAL, EconomyGuard.sign(economyPayload(gold, jokers, owned)))
    }

    // --- Store (mağaza). Ids are "pen:<PenSkin.name>" / "frame:<AvatarFrame.name>". ---

    private val _ownedStoreIds = MutableStateFlow(prefs.getStringSet(KEY_OWNED_STORE_IDS, emptySet())?.toSet() ?: emptySet())
    val ownedStoreIds: StateFlow<Set<String>> = _ownedStoreIds.asStateFlow()

    /** Buys [id] for [price] gold. False if already owned or the balance is too low. */
    @Synchronized
    fun purchaseStoreItem(id: String, price: Int): Boolean {
        if (price <= 0 || id in _ownedStoreIds.value || _goldBalance.value < price) return false
        val updatedGold = _goldBalance.value - price
        val updatedOwned = _ownedStoreIds.value + id
        // One commit for the deduction AND the grant — spendGold()+a second
        // apply() used to split these; a crash in between lost the player's
        // gold with nothing granted for it.
        prefs.edit(commit = true) {
            putInt(KEY_GOLD_BALANCE, updatedGold)
            putStringSet(KEY_OWNED_STORE_IDS, updatedOwned)
            sealEconomy(gold = updatedGold, owned = updatedOwned)
        }
        _goldBalance.value = updatedGold
        _ownedStoreIds.value = updatedOwned
        return true
    }

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

    // --- Jokers (consumables): bought in the store or dropped from chests. ---

    private fun loadJokers(): Map<JokerType, Int> =
        prefs.getString(KEY_JOKERS, null)
            ?.let { runCatching { Json.decodeFromString<Map<String, Int>>(it) }.getOrNull() }
            ?.mapNotNull { (name, n) -> runCatching { JokerType.valueOf(name) }.getOrNull()?.let { it to n } }
            ?.toMap()
            ?: emptyMap()

    private val _jokerCounts = MutableStateFlow(loadJokers())
    val jokerCounts: StateFlow<Map<JokerType, Int>> = _jokerCounts.asStateFlow()

    // Off the main thread: EconomyGuard's first call generates (or reads) an
    // AndroidKeyStore key, which can synchronously cost 50-200ms on some
    // devices — enough to visibly drop frames right at cold start otherwise.
    init { repoScope.launch { verifyEconomy() } }

    /**
     * Checks the stored economy against its seal on every start. No key yet
     * (fresh install, new phone, first launch after this feature) means
     * nothing can be compared, so the current numbers are trusted and
     * sealed. A key with a missing or wrong seal means the numbers were
     * edited from outside: the economy is reset to zero.
     */
    @Synchronized
    private fun verifyEconomy() {
        val payload = economyPayload(_goldBalance.value, _jokerCounts.value, _ownedStoreIds.value)
        val seal = prefs.getString(KEY_ECONOMY_SEAL, null)
        when {
            !EconomyGuard.hasKey() -> prefs.edit { sealEconomy() }
            seal != null && EconomyGuard.verify(payload, seal) -> Unit
            else -> {
                android.util.Log.w("SettingsRepository", "Economy seal mismatch — resetting gold, jokers and store items")
                prefs.edit {
                    putInt(KEY_GOLD_BALANCE, 0)
                    remove(KEY_OWNED_STORE_IDS)
                    remove(KEY_JOKERS)
                    sealEconomy(0, emptyMap(), emptySet())
                }
                _goldBalance.value = 0
                _ownedStoreIds.value = emptySet()
                _jokerCounts.value = emptyMap()
            }
        }
    }

    private fun saveJokers(counts: Map<JokerType, Int>) {
        prefs.edit { putString(KEY_JOKERS, Json.encodeToString(counts.mapKeys { it.key.name })); sealEconomy(jokers = counts) }
        _jokerCounts.value = counts
    }

    @Synchronized
    fun addJoker(type: JokerType, quantity: Int) {
        if (quantity <= 0) return
        saveJokers(_jokerCounts.value + (type to ((_jokerCounts.value[type] ?: 0) + quantity)))
    }

    /** Spends one joker; false if none are left. */
    @Synchronized
    fun useJoker(type: JokerType): Boolean {
        val have = _jokerCounts.value[type] ?: 0
        if (have <= 0) return false
        saveJokers(_jokerCounts.value + (type to (have - 1)))
        return true
    }

    /** Buys [quantity] jokers at [JokerType.priceFor]; false if the balance can't cover it. */
    @Synchronized
    fun purchaseJoker(type: JokerType, quantity: Int): Boolean {
        val price = type.priceFor(quantity)
        if (quantity <= 0 || _goldBalance.value < price) return false
        val updatedGold = _goldBalance.value - price
        val updatedJokers = _jokerCounts.value + (type to ((_jokerCounts.value[type] ?: 0) + quantity))
        // One commit for the deduction AND the grant — see purchaseStoreItem's
        // comment for why splitting these across two apply() calls is unsafe.
        prefs.edit(commit = true) {
            putInt(KEY_GOLD_BALANCE, updatedGold)
            putString(KEY_JOKERS, Json.encodeToString(updatedJokers.mapKeys { it.key.name }))
            sealEconomy(gold = updatedGold, jokers = updatedJokers)
        }
        _goldBalance.value = updatedGold
        _jokerCounts.value = updatedJokers
        return true
    }

    // --- The daily rewarded-ad joker: one per local calendar day. Device-scoped
    // (deliberately absent from the account backup and its clear) so signing out
    // and back in cannot claim the same day's joker twice. ---
    private val _dailyJokerDay = MutableStateFlow(prefs.getLong(KEY_DAILY_JOKER_DAY, -1L))
    val dailyJokerDay: StateFlow<Long> = _dailyJokerDay.asStateFlow()

    /** Hands over today's [DailyJoker] and records the day; null if it was already claimed. */
    @Synchronized
    fun claimDailyJoker(): JokerType? {
        val today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()
        if (_dailyJokerDay.value == today) return null
        val type = com.sualtikasifi.cizimhafiza.domain.model.DailyJoker.typeFor(today)
        prefs.edit { putLong(KEY_DAILY_JOKER_DAY, today) }
        _dailyJokerDay.value = today
        addJoker(type, 1)
        return type
    }

    /** Puts a store item straight into the collection (chest drop) — no gold changes hands. */
    @Synchronized
    fun grantStoreItem(id: String) {
        if (id in _ownedStoreIds.value) return
        val updated = _ownedStoreIds.value + id
        prefs.edit { putStringSet(KEY_OWNED_STORE_IDS, updated); sealEconomy(owned = updated) }
        _ownedStoreIds.value = updated
    }

    // --- Rewarded-ad chest speed-up: once per local calendar day. ---

    private val _chestSpeedupDay = MutableStateFlow(prefs.getLong(KEY_CHEST_SPEEDUP_DAY, -1L))
    val chestSpeedupDay: StateFlow<Long> = _chestSpeedupDay.asStateFlow()

    /** Takes [millis] off the running countdown of [chestId]. False if today's speed-up is spent or that chest is not counting down. */
    @Synchronized
    fun speedUpChest(chestId: String, millis: Long): Boolean {
        val today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()
        if (_chestSpeedupDay.value == today) return false
        val slots = _chestSlots.value
        val index = slots.indexOfFirst { it?.id == chestId }
        val chest = slots.getOrNull(index) ?: return false
        val started = chest.unlockStartedAtMillis ?: return false
        if (chest.isReady(System.currentTimeMillis())) return false
        saveChestSlots(slots.toMutableList().apply { this[index] = chest.copy(unlockStartedAtMillis = started - millis) })
        prefs.edit { putLong(KEY_CHEST_SPEEDUP_DAY, today) }
        _chestSpeedupDay.value = today
        return true
    }

    // --- Chests (kasalar) — see domain.model.Chest/ChestSlots. Won only
    // from a WON real online-room match (see OnlineResultViewModel), never
    // from solo play, Hızlı Eşleş or the daily challenge. ---

    private val _chestSlots = MutableStateFlow(loadChestSlots())
    val chestSlots: StateFlow<List<Chest?>> = _chestSlots.asStateFlow()

    // Re-arm the "chest ready" alarm on every start — see ChestReadyNotifier.
    // Off the main thread too: AlarmManager IPC at construction time is the
    // same class of cold-start stutter as the economy check above.
    init { repoScope.launch { com.sualtikasifi.cizimhafiza.notifications.ChestReadyNotifier.sync(context, _chestSlots.value) } }

    private fun loadChestSlots(): List<Chest?> {
        val raw = prefs.getString(KEY_CHEST_SLOTS, null)
            ?: return List(ChestSlots.SLOT_COUNT) { null }
        return runCatching { Json.decodeFromString<List<Chest?>>(raw) }
            .getOrDefault(List(ChestSlots.SLOT_COUNT) { null })
    }

    private fun saveChestSlots(slots: List<Chest?>) {
        prefs.edit { putString(KEY_CHEST_SLOTS, Json.encodeToString(slots)) }
        com.sualtikasifi.cizimhafiza.notifications.ChestReadyNotifier.sync(context, slots)
        _chestSlots.value = slots
    }

    /** The stored seed as-is (0 = never generated) — reading it for a backup must not create one. */
    val chestCycleSeedForBackup: Long get() = prefs.getLong(KEY_CHEST_CYCLE_SEED, 0L)
    val chestCycleIndexForBackup: Int get() = prefs.getInt(KEY_CHEST_CYCLE_INDEX, 0)

    /**
     * Generated once per account, the first time it is ever needed, and
     * never changed again — this is what makes [ChestSlots.tierAt] a
     * reproducible per-account SHUFFLE rather than a fresh roll every call.
     * 0L is treated as "not yet generated" rather than a legal seed (a
     * one-in-2^64 chance of actually rolling zero from Random.nextLong() is
     * an acceptable cost for not needing a separate "has a seed" flag).
     */
    private var chestCycleSeed: Long
        get() {
            val existing = prefs.getLong(KEY_CHEST_CYCLE_SEED, 0L)
            if (existing != 0L) return existing
            val fresh = Random.nextLong().takeIf { it != 0L } ?: 1L
            prefs.edit { putLong(KEY_CHEST_CYCLE_SEED, fresh) }
            return fresh
        }
        set(value) = prefs.edit { putLong(KEY_CHEST_CYCLE_SEED, value) }

    /** How many chests this account has ever been AWARDED (not opened) — see [ChestSlots.tierAt]. */
    private var chestCycleIndex: Int
        get() = prefs.getInt(KEY_CHEST_CYCLE_INDEX, 0)
        set(value) = prefs.edit { putInt(KEY_CHEST_CYCLE_INDEX, value) }

    /**
     * Advances the draw cycle regardless of whether a slot is free —
     * leaving all [ChestSlots.SLOT_COUNT] slots full for too long costs the
     * chest the cycle would otherwise have handed out. That is deliberate:
     * it is what makes leaving chests unopened actually cost something,
     * instead of a queue nothing is ever lost from. Returns the awarded
     * chest only if a slot was free to put it in; null still means the win
     * counted (the cycle moved on), just that nothing appeared on screen.
     */
    @Synchronized
    fun awardChestForWin(): Chest? {
        val seed = chestCycleSeed
        val index = chestCycleIndex
        chestCycleIndex = index + 1
        val slots = _chestSlots.value
        val freeIndex = slots.indexOfFirst { it == null }
        if (freeIndex < 0) return null
        val chest = Chest(id = UUID.randomUUID().toString(), tier = ChestSlots.tierAt(seed, index))
        saveChestSlots(slots.toMutableList().apply { this[freeIndex] = chest })
        return chest
    }

    /** A won match's chest award: [chest] is null when every slot was full, in which case nothing drops. */
    data class ChestAward(val chest: Chest?, val instantReward: ChestReward?)

    /**
     * A win's chest goes into a free slot; with every slot taken nothing drops (the win still pays
     * its XP and gold) and [ChestAward.chest] is null. The tier cycle still advances, so leaving
     * chests unopened costs the chest that would have come.
     */
    @Synchronized
    fun awardChestForWinOrPay(): ChestAward = ChestAward(awardChestForWin(), null)

    private fun payChestOutright(tier: ChestTier, extraEdit: android.content.SharedPreferences.Editor.() -> Unit = {}): ChestReward {
        val reward = ChestLoot.roll(tier, _ownedStoreIds.value)
        val updatedGold = _goldBalance.value + reward.gold
        val updatedJokers = _jokerCounts.value.toMutableMap().apply {
            reward.jokers.forEach { (type, n) -> this[type] = (this[type] ?: 0) + n }
        }
        val updatedOwned = reward.penDrop?.let { _ownedStoreIds.value + "pen:${it.name}" } ?: _ownedStoreIds.value
        prefs.edit(commit = true) {
            putInt(KEY_GOLD_BALANCE, updatedGold)
            putString(KEY_JOKERS, Json.encodeToString(updatedJokers.mapKeys { it.key.name }))
            putStringSet(KEY_OWNED_STORE_IDS, updatedOwned)
            extraEdit()
            sealEconomy(gold = updatedGold, jokers = updatedJokers, owned = updatedOwned)
        }
        _goldBalance.value = updatedGold
        _jokerCounts.value = updatedJokers
        _ownedStoreIds.value = updatedOwned
        return reward
    }

    /** False if another slot is already counting down — only one chest unlocks at a time. */
    @Synchronized
    fun startUnlockingChest(chestId: String): Boolean {
        val slots = _chestSlots.value
        if (slots.any { it?.unlockStartedAtMillis != null }) return false
        val index = slots.indexOfFirst { it?.id == chestId }
        val chest = slots.getOrNull(index) ?: return false
        if (chest.unlockStartedAtMillis != null) return false
        saveChestSlots(
            slots.toMutableList().apply { this[index] = chest.copy(unlockStartedAtMillis = System.currentTimeMillis()) }
        )
        return true
    }

    /** Grants the reward and empties the slot — null if the chest isn't ready yet or doesn't exist any more. */
    @Synchronized
    fun openChestIfReady(chestId: String): ChestReward? {
        val slots = _chestSlots.value
        val index = slots.indexOfFirst { it?.id == chestId }
        val chest = slots.getOrNull(index) ?: return null
        if (!chest.isReady(System.currentTimeMillis())) return null
        val reward = ChestLoot.roll(chest.tier, _ownedStoreIds.value)

        val updatedGold = _goldBalance.value + reward.gold
        val updatedJokers = _jokerCounts.value.toMutableMap().apply {
            reward.jokers.forEach { (type, n) -> this[type] = (this[type] ?: 0) + n }
        }
        val updatedOwned = reward.penDrop?.let { _ownedStoreIds.value + "pen:${it.name}" } ?: _ownedStoreIds.value
        val updatedSlots = slots.toMutableList().apply { this[index] = null }

        // One commit for the whole payout, not the four-plus separate
        // apply() calls (addGold/addJoker per type/grantStoreItem/saveChestSlots)
        // this used to be. Those are async and independently ordered on
        // disk, so a process death between "reward granted" and "slot
        // cleared" could leave the reward paid but the chest still sitting
        // there ready to open again — a real double-grant path. Landing the
        // reward and the slot-clear in the same commit makes that
        // impossible: either both happened or neither did.
        prefs.edit(commit = true) {
            putInt(KEY_GOLD_BALANCE, updatedGold)
            putString(KEY_JOKERS, Json.encodeToString(updatedJokers.mapKeys { it.key.name }))
            putStringSet(KEY_OWNED_STORE_IDS, updatedOwned)
            putString(KEY_CHEST_SLOTS, Json.encodeToString(updatedSlots))
            sealEconomy(gold = updatedGold, jokers = updatedJokers, owned = updatedOwned)
        }
        _goldBalance.value = updatedGold
        _jokerCounts.value = updatedJokers
        _ownedStoreIds.value = updatedOwned
        _chestSlots.value = updatedSlots
        com.sualtikasifi.cizimhafiza.notifications.ChestReadyNotifier.sync(context, updatedSlots)
        return reward
    }


    // --- Home-screen ad rewards: 500 gold every 4 hours, and one free
    // mid-tier chest per calendar day (resets at local midnight). ---

    private val _adGoldNextAtMillis = MutableStateFlow(prefs.getLong(KEY_AD_GOLD_NEXT_AT, 0L))
    val adGoldNextAtMillis: StateFlow<Long> = _adGoldNextAtMillis.asStateFlow()

    private val _adChestDay = MutableStateFlow(prefs.getLong(KEY_AD_CHEST_DAY, -1L))
    val adChestDay: StateFlow<Long> = _adChestDay.asStateFlow()

    @Synchronized
    fun isAdGoldReady(now: Long = System.currentTimeMillis()): Boolean = now >= _adGoldNextAtMillis.value

    /** Pays [AD_GOLD_AMOUNT] gold and starts the 4-hour cooldown; false while the cooldown is still running. */
    @Synchronized
    fun claimAdGold(): Boolean {
        val now = System.currentTimeMillis()
        if (now < _adGoldNextAtMillis.value) return false
        val next = now + AD_GOLD_COOLDOWN_MILLIS
        prefs.edit(commit = true) { putLong(KEY_AD_GOLD_NEXT_AT, next) }
        _adGoldNextAtMillis.value = next
        addGold(AD_GOLD_AMOUNT)
        return true
    }

    @Synchronized
    fun isAdChestReady(): Boolean = _adChestDay.value != com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()

    /** Rolls and pays a free mid-tier chest without needing a slot; null if today's was already taken. */
    @Synchronized
    fun claimAdChest(): ChestReward? {
        val today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()
        if (_adChestDay.value == today) return null
        val reward = payChestOutright(ChestTier.GOLD) { putLong(KEY_AD_CHEST_DAY, today) }
        _adChestDay.value = today
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
    fun revokeXp(amount: Int) {
        if (amount <= 0) return
        val updated = (_lifetimeXp.value - amount).coerceAtLeast(0)
        val period = (_periodXp.value - amount).coerceAtLeast(0)
        prefs.edit(commit = true) {
            putInt(KEY_LIFETIME_XP, updated)
            putInt(KEY_PERIOD_XP, period)
        }
        _lifetimeXp.value = updated
        _periodXp.value = period
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
        val updated = _lifetimeXp.value + amount
        prefs.edit { putInt(KEY_LIFETIME_XP, updated) }
        _lifetimeXp.value = updated
        addPeriodXp(amount)
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
    private val _periodXp = MutableStateFlow(readPeriodXp())
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
    fun restoreEngagement(periodXp: Int, periodId: Long, adGoldNextAt: Long, adChestDay: Long) {
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
        if (adChestDay > _adChestDay.value) {
            prefs.edit { putLong(KEY_AD_CHEST_DAY, adChestDay) }
            _adChestDay.value = adChestDay
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
     * Bumps the per-finished-game lifetime tallies the achievement catalog
     * reads. Called once per saved game (solo or online) alongside
     * [addScore]/[addWordsDrawn] — see GameRepositoryImpl.finishSaving.
     */
    fun recordFinishedGame(wasPerfectRound: Boolean, wasOnlineWin: Boolean) {
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
        _avatarSource.value = ""
        _selectedAvatarFrameId.value = AvatarFrame.DEFAULT.name
        _selectedPenSkinId.value = PenSkin.DEFAULT.name
        _periodXp.value = 0
        _phraseUsageCounts.value = emptyMap()
        _emojiUsageCounts.value = emptyMap()
        _earnedLeagueRewardIds.value = emptySet()
        _goldBalance.value = 0
        _ownedStoreIds.value = emptySet()
        _jokerCounts.value = emptyMap()
        _chestSlots.value = List(ChestSlots.SLOT_COUNT) { null }
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
        remove(KEY_USERNAME_VERIFIED)
        remove(KEY_LAST_REWARD_CHECK)
        remove(KEY_LAST_PENALTY_CHECK)
        remove(KEY_AVATAR_SOURCE)
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
        putInt(KEY_GOLD_BALANCE, 0)
        remove(KEY_OWNED_STORE_IDS)
        remove(KEY_JOKERS)
        sealEconomy(0, emptyMap(), emptySet())
        remove(KEY_CHEST_SLOTS)
        remove(KEY_CHEST_CYCLE_SEED)
        putInt(KEY_CHEST_CYCLE_INDEX, 0)
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
            putString(KEY_CHEST_SLOTS, Json.encodeToString(chestSlots))
            if (restoredChestCycleSeed != 0L) {
                putLong(KEY_CHEST_CYCLE_SEED, restoredChestCycleSeed)
                putInt(KEY_CHEST_CYCLE_INDEX, restoredChestCycleIndex)
            }
            putInt(KEY_GOLD_BALANCE, goldBalance)
            putStringSet(KEY_OWNED_STORE_IDS, ownedStoreIds)
            putString(KEY_JOKERS, Json.encodeToString(jokerCounts.mapKeys { it.key.name }))
            sealEconomy(goldBalance, jokerCounts, ownedStoreIds)
        }
        _periodXp.value = 0
        _phraseUsageCounts.value = emptyMap()
        _emojiUsageCounts.value = emptyMap()
        _earnedLeagueRewardIds.value = earnedLeagueRewardIds
        _lifetimeScore.value = lifetimeScore
        _lifetimeXp.value = lifetimeXp
        _lifetimeWordsDrawn.value = lifetimeWordsDrawn
        _nickname.value = nickname
        _selectedAvatarFrameId.value = frame
        _selectedPenSkinId.value = pen
        _goldBalance.value = goldBalance
        _ownedStoreIds.value = ownedStoreIds
        _jokerCounts.value = jokerCounts
        _chestSlots.value = chestSlots
        com.sualtikasifi.cizimhafiza.notifications.ChestReadyNotifier.sync(context, chestSlots)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
        _notificationsEnabled.value = enabled
    }

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

    // Guards the one-time automatic permission prompt in MainActivity so it
    // only ever fires on a device's very first launch, not every cold start.
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

    var ratingPromptShown: Boolean
        get() = prefs.getBoolean(KEY_RATING_PROMPT_SHOWN, false)
        set(value) = prefs.edit { putBoolean(KEY_RATING_PROMPT_SHOWN, value) }

    /**
     * Pays the rating-prompt's 500 XP bonus exactly once, ever — a second
     * call (a retried dialog action, a process death replaying the tap)
     * returns false and grants nothing instead of paying out again. Separate
     * from [ratingPromptShown]: that flag only guards the DIALOG appearing,
     * this one guards the XP itself, the same split addScore/addXp keep
     * from every other reward path.
     */
    @Synchronized
    fun grantRatingBonusXpOnce(amount: Int): Boolean {
        if (prefs.getBoolean(KEY_RATING_BONUS_XP_GRANTED, false)) return false
        prefs.edit { putBoolean(KEY_RATING_BONUS_XP_GRANTED, true) }
        addXp(amount)
        return true
    }

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

    private companion object {
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
        const val KEY_LAST_PLAYED_EPOCH_DAY = "last_played_epoch_day"
        const val KEY_LAST_QUICK_MATCH_EPOCH_DAY = "last_quick_match_epoch_day"
        const val KEY_GOLD_BALANCE = "gold_balance"
        const val KEY_OWNED_STORE_IDS = "owned_store_ids"
        const val KEY_JOKERS = "joker_counts"
        const val KEY_FEATURE_TOUR_SEEN = "feature_tour_seen"
        const val KEY_ECONOMY_SEAL = "economy_seal"
        const val KEY_QM_RECENT_WORDS = "qm_recent_words"
        const val QM_RECENT_WORDS_CAP = 300
        const val KEY_CHEST_SPEEDUP_DAY = "chest_speedup_day"
        const val KEY_DAILY_JOKER_DAY = "daily_joker_day"
        const val KEY_CHEST_SLOTS = "chest_slots"
        const val KEY_CHEST_CYCLE_SEED = "chest_cycle_seed"
        const val KEY_CHEST_CYCLE_INDEX = "chest_cycle_index"
        const val KEY_AD_GOLD_NEXT_AT = "ad_gold_next_at"
        const val KEY_AD_CHEST_DAY = "ad_chest_day"
        const val AD_GOLD_AMOUNT = 500
        const val AD_GOLD_COOLDOWN_MILLIS = 4 * 60 * 60 * 1000L
        const val KEY_CURRENT_STREAK = "current_streak"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        const val KEY_LAST_REMINDER_EPOCH_DAY = "last_reminder_epoch_day"
        const val KEY_SIGN_IN_PROMPT_SHOWN = "sign_in_prompt_shown"
        const val KEY_RATING_PROMPT_SHOWN = "rating_prompt_shown"
        const val KEY_RATING_BONUS_XP_GRANTED = "rating_bonus_xp_granted"
        const val KEY_NICKNAME_CHOSEN = "nickname_chosen_by_player"
        const val KEY_NICKNAME_RENAME_USED = "nickname_rename_used"
        const val KEY_USERNAME_CHANGES = "username_changes"
        const val KEY_USERNAME_VERIFIED = "username_verified"
        const val KEY_LAST_REWARD_CHECK = "last_reward_check_millis"
        const val KEY_LAST_PENALTY_CHECK = "last_penalty_check_millis"
        const val KEY_AVATAR_SOURCE = "avatar_source"
        const val NICKNAME_MIN = 2
        const val NICKNAME_MAX = 16
        const val KEY_BOT_TRAINING_UNLOCKED = "bot_training_unlocked"
        const val KEY_PUBLISHED_LEAGUE_SIGNATURE = "published_league_score_signature"
    }
}
