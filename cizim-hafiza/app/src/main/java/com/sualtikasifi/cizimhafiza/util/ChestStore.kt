package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestLoot
import com.sualtikasifi.cizimhafiza.domain.model.ChestReward
import com.sualtikasifi.cizimhafiza.domain.model.ChestSlots
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffect
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffects
import com.sualtikasifi.cizimhafiza.notifications.ChestReadyNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * The chests (kasalar) — see domain.model.Chest/ChestSlots: the four slots, the Yedek Yuva reserve slot, the tier
 * cycle, and opening them. Won from a won online-room match or Hızlı Eşleş, never from solo play or the daily challenge.
 *
 * Every payout goes through the [ledger] in the same edit as the slot it empties, and locks on it.
 */
@Singleton
class ChestStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ledger: EconomyLedger,
    private val runes: RuneStore
) {
    private val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _chestSlots = MutableStateFlow(loadSlots())
    val chestSlots: StateFlow<List<Chest?>> = _chestSlots.asStateFlow()

    private val _chestReserve = MutableStateFlow(loadReserve())

    /** The chest waiting in the reserve slot (the Yedek Yuva rune), or null. */
    val chestReserve: StateFlow<Chest?> = _chestReserve.asStateFlow()

    private val _chestSpeedupDay = MutableStateFlow(prefs.getLong(SettingsRepository.KEY_CHEST_SPEEDUP_DAY, -1L))
    val chestSpeedupDay: StateFlow<Long> = _chestSpeedupDay.asStateFlow()

    // Re-arm the "chest ready" alarm on every start — see ChestReadyNotifier. Off the main thread: AlarmManager IPC
    // at construction time is a cold-start stutter.
    init { scope.launch { ChestReadyNotifier.sync(context, _chestSlots.value) } }

    private fun loadSlots(): List<Chest?> {
        val raw = prefs.getString(SettingsRepository.KEY_CHEST_SLOTS, null) ?: return List(ChestSlots.SLOT_COUNT) { null }
        return runCatching { Json.decodeFromString<List<Chest?>>(raw) }.getOrDefault(List(ChestSlots.SLOT_COUNT) { null })
    }

    private fun loadReserve(): Chest? = runCatching {
        prefs.getString(SettingsRepository.KEY_CHEST_RESERVE, null)?.let { Json.decodeFromString<Chest>(it) }
    }.getOrNull()

    private fun publishSlots(slots: List<Chest?>) {
        _chestSlots.value = slots
        ChestReadyNotifier.sync(context, slots)
    }

    private fun saveSlots(slots: List<Chest?>, extra: SharedPreferences.Editor.() -> Unit = {}) {
        prefs.edit {
            putString(SettingsRepository.KEY_CHEST_SLOTS, Json.encodeToString(slots))
            extra()
        }
        publishSlots(slots)
    }

    /** The stored seed as-is (0 = never generated) — reading it for a backup must not create one. */
    val chestCycleSeedForBackup: Long get() = prefs.getLong(SettingsRepository.KEY_CHEST_CYCLE_SEED, 0L)
    val chestCycleIndexForBackup: Int get() = prefs.getInt(SettingsRepository.KEY_CHEST_CYCLE_INDEX, 0)

    /**
     * Generated once per account, the first time it is ever needed, and never changed again — this is what makes
     * [ChestSlots.tierAt] a reproducible per-account SHUFFLE rather than a fresh roll every call. 0L means "not yet".
     */
    private val cycleSeed: Long
        get() {
            val existing = prefs.getLong(SettingsRepository.KEY_CHEST_CYCLE_SEED, 0L)
            if (existing != 0L) return existing
            val fresh = Random.nextLong().takeIf { it != 0L } ?: 1L
            prefs.edit { putLong(SettingsRepository.KEY_CHEST_CYCLE_SEED, fresh) }
            return fresh
        }

    /**
     * Advances the draw cycle regardless of whether a slot is free — leaving every slot full costs the chest the cycle
     * would have handed out, which is what makes leaving chests unopened cost something. Returns the chest only if a
     * slot was free; with the Yedek Yuva rune a chest that found no slot waits in the reserve slot instead.
     */
    fun awardChestForWin(): Chest? = synchronized(ledger) {
        val seed = cycleSeed
        val index = prefs.getInt(SettingsRepository.KEY_CHEST_CYCLE_INDEX, 0)
        val slots = _chestSlots.value
        val freeIndex = slots.indexOfFirst { it == null }
        val chest = Chest(id = UUID.randomUUID().toString(), tier = ChestSlots.tierAt(seed, index))
        if (freeIndex < 0) {
            val reserve = RuneEffects.has(runes.levels, RuneEffect.RESERVE) && _chestReserve.value == null
            prefs.edit {
                putInt(SettingsRepository.KEY_CHEST_CYCLE_INDEX, index + 1)
                if (reserve) putString(SettingsRepository.KEY_CHEST_RESERVE, Json.encodeToString(chest))
            }
            if (reserve) _chestReserve.value = chest
            return null
        }
        saveSlots(slots.toMutableList().apply { this[freeIndex] = chest }) {
            putInt(SettingsRepository.KEY_CHEST_CYCLE_INDEX, index + 1)
        }
        chest
    }

    /** The loot of a [tier] chest with the chest runes applied. */
    private fun roll(tier: ChestTier): ChestReward = ChestLoot.roll(
        tier,
        ledger.ownedStoreIds.value,
        goldPercent = RuneEffects.chestGoldPercent(runes.levels),
        jokerBonusPercent = RuneEffects.chestJokerBonusPercent(runes.levels)
    )

    /** Puts [reward] in the wallet, with [extra] keys in the same edit. */
    private fun pay(reward: ChestReward, extra: SharedPreferences.Editor.() -> Unit) {
        val jokers = ledger.jokerCounts.value.toMutableMap().apply {
            reward.jokers.forEach { (type, n) -> this[type] = (this[type] ?: 0) + n }
        }
        val owned = reward.penDrop?.let { ledger.ownedStoreIds.value + "pen:${it.name}" } ?: ledger.ownedStoreIds.value
        ledger.write(gold = ledger.goldBalance.value + reward.gold, jokers = jokers, owned = owned, extra = extra)
    }

    /** Opens a chest of [tier] on the spot (a coupon's, a login milestone's, an ad's): paid at once, no slot needed. */
    fun payOutright(tier: ChestTier, extra: SharedPreferences.Editor.() -> Unit = {}): ChestReward = synchronized(ledger) {
        roll(tier).also { pay(it, extra) }
    }

    /** False while another chest is still counting down — one timer at a time; a chest done waiting does not count. */
    fun startUnlocking(chestId: String): Boolean = synchronized(ledger) {
        val slots = _chestSlots.value
        val now = System.currentTimeMillis()
        if (slots.any { it?.unlockStartedAtMillis != null && !it.isReady(now) }) return false
        val index = slots.indexOfFirst { it?.id == chestId }
        val chest = slots.getOrNull(index) ?: return false
        if (chest.unlockStartedAtMillis != null) return false
        // The time runes move the start back, so the chest is ready that much sooner.
        val cut = chest.tier.unlockDurationMillis * RuneEffects.chestTimeReductionPercent(runes.levels) / 100
        saveSlots(slots.toMutableList().apply { this[index] = chest.copy(unlockStartedAtMillis = now - cut) })
        true
    }

    /**
     * Grants the reward and empties the slot — null if the chest isn't ready or doesn't exist any more. The reward,
     * the emptied slot and the reserve chest moving into it are one edit: a process death in between can neither pay
     * twice nor lose the chest.
     */
    fun openIfReady(chestId: String): ChestReward? = synchronized(ledger) {
        val slots = _chestSlots.value
        val index = slots.indexOfFirst { it?.id == chestId }
        val chest = slots.getOrNull(index) ?: return null
        if (!chest.isReady(System.currentTimeMillis())) return null
        val reward = roll(chest.tier)
        // The chest waiting in reserve takes the slot that just freed up.
        val reserve = _chestReserve.value
        val updatedSlots = slots.toMutableList().apply { this[index] = reserve }
        pay(reward) {
            putString(SettingsRepository.KEY_CHEST_SLOTS, Json.encodeToString(updatedSlots))
            if (reserve != null) remove(SettingsRepository.KEY_CHEST_RESERVE)
        }
        publishSlots(updatedSlots)
        if (reserve != null) _chestReserve.value = null
        reward
    }

    /** Opens a counting-down chest at once (once a day, Anahtarcı rune): the countdown is moved to the past. */
    fun instantOpen(chestId: String): Boolean = synchronized(ledger) {
        if (!runes.instantChestAvailable()) return false
        val slots = _chestSlots.value
        val index = slots.indexOfFirst { it?.id == chestId }
        val chest = slots.getOrNull(index) ?: return false
        val now = System.currentTimeMillis()
        if (chest.unlockStartedAtMillis == null || chest.isReady(now)) return false
        saveSlots(slots.toMutableList().apply { this[index] = chest.copy(unlockStartedAtMillis = now - chest.tier.unlockDurationMillis) }) {
            with(runes) { stageInstantUsed() }
        }
        true
    }

    /** Takes [millis] off the running countdown of [chestId] (the daily ad speed-up). False if spent today or not counting. */
    fun speedUp(chestId: String, millis: Long): Boolean = synchronized(ledger) {
        val today = TurkeyTime.today().toEpochDay()
        if (_chestSpeedupDay.value == today) return false
        val slots = _chestSlots.value
        val index = slots.indexOfFirst { it?.id == chestId }
        val chest = slots.getOrNull(index) ?: return false
        val started = chest.unlockStartedAtMillis ?: return false
        if (chest.isReady(System.currentTimeMillis())) return false
        saveSlots(slots.toMutableList().apply { this[index] = chest.copy(unlockStartedAtMillis = started - millis) }) {
            putLong(SettingsRepository.KEY_CHEST_SPEEDUP_DAY, today)
        }
        _chestSpeedupDay.value = today
        true
    }

    /** The account's chests cleared on [this] editor (an account switch). */
    fun SharedPreferences.Editor.stageClear() {
        remove(SettingsRepository.KEY_CHEST_SLOTS)
        remove(SettingsRepository.KEY_CHEST_RESERVE)
        remove(SettingsRepository.KEY_CHEST_CYCLE_SEED)
        putInt(SettingsRepository.KEY_CHEST_CYCLE_INDEX, 0)
    }

    /** An account's chests (and the place in its tier cycle) put on [this] editor during a restore. */
    fun SharedPreferences.Editor.stageRestore(slots: List<Chest?>, seed: Long, index: Int) {
        putString(SettingsRepository.KEY_CHEST_SLOTS, Json.encodeToString(slots))
        if (seed != 0L) {
            putLong(SettingsRepository.KEY_CHEST_CYCLE_SEED, seed)
            putInt(SettingsRepository.KEY_CHEST_CYCLE_INDEX, index)
        }
    }

    /** Shows what a staged clear or restore wrote. */
    fun publishAccount(slots: List<Chest?>) {
        publishSlots(slots)
        _chestReserve.value = loadReserve()
    }
}
