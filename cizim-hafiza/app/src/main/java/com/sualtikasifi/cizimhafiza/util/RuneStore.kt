package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffect
import com.sualtikasifi.cizimhafiza.domain.model.RuneEffects
import com.sualtikasifi.cizimhafiza.domain.model.RuneLive
import com.sualtikasifi.cizimhafiza.domain.model.RuneRewards
import com.sualtikasifi.cizimhafiza.domain.model.RuneTree
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Karalak Rünleri on the device: the levels bought (sealed like the wallet, see [EconomyGuard]), buying them, and the
 * once-a-day / once-a-month abilities the keystones give. What the runes DO is in domain/model/Rune.kt.
 *
 * Locks on [ledger], so a rune bought with gold and the gold leaving the wallet are one step.
 */
@Singleton
class RuneStore @Inject constructor(
    @ApplicationContext context: Context,
    private val ledger: EconomyLedger
) {
    private val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _runeLevels = MutableStateFlow(readStored().also { RuneLive.levels = it })
    val runeLevels: StateFlow<Map<String, Int>> = _runeLevels.asStateFlow()

    // The seal check needs the Keystore, so it runs off the main thread; until it ends the stored levels are shown.
    init { scope.launch { verify() } }

    val levels: Map<String, Int> get() = _runeLevels.value

    private fun publish(levels: Map<String, Int>) {
        RuneLive.levels = levels
        _runeLevels.value = levels
    }

    private fun sealPayload(levels: Map<String, Int>): String =
        "runes|" + levels.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" }

    /** The levels and their seal on [this] editor. */
    fun SharedPreferences.Editor.stageRunes(levels: Map<String, Int>) {
        putString(SettingsRepository.KEY_RUNE_LEVELS, Json.encodeToString(levels))
        putString(SettingsRepository.KEY_RUNE_SEAL, EconomyGuard.sign(sealPayload(levels)))
    }

    private fun decodeStored(): Map<String, Int> = runCatching {
        prefs.getString(SettingsRepository.KEY_RUNE_LEVELS, null)?.let { Json.decodeFromString<Map<String, Int>>(it) }
    }.getOrNull().orEmpty()

    private fun clean(levels: Map<String, Int>) = levels.filter { (id, level) -> RuneTree.get(id) != null && level > 0 }

    private fun readStored(): Map<String, Int> = clean(decodeStored())

    /**
     * Checks the stored levels against their seal; levels edited from outside are dropped, like the wallet. The seal
     * covers what is stored, so it is checked BEFORE runes that left the map are dropped: removing a rune in an update
     * must not look like tampering. (A seal made over the already-cleaned levels is accepted too.)
     */
    private fun verify() = synchronized(ledger) {
        val stored = decodeStored()
        val saved = clean(stored)
        val seal = prefs.getString(SettingsRepository.KEY_RUNE_SEAL, null)
        val trusted = !EconomyGuard.hasKey() || seal == null ||
            EconomyGuard.verify(sealPayload(stored), seal) || EconomyGuard.verify(sealPayload(saved), seal)
        val checked = if (trusted) {
            if (saved != stored || seal == null) prefs.edit { stageRunes(saved) }
            saved
        } else {
            android.util.Log.w("RuneStore", "Rune seal mismatch — resetting rune levels")
            prefs.edit { stageRunes(emptyMap()) }
            emptyMap()
        }
        if (checked != _runeLevels.value) publish(checked)
    }

    /**
     * Buys the next level of [id]: false when it is not open yet (at [playerLevel]), already at its max, or the gold
     * is not there. The gold, the level and a style rune's pen or frame land in one edit.
     */
    fun buy(id: String, playerLevel: Int): Boolean = synchronized(ledger) {
        val rune = RuneTree.get(id) ?: return false
        val current = _runeLevels.value
        if (!RuneTree.canBuy(current, rune, playerLevel)) return false
        val cost = RuneTree.nextCost(current, rune) ?: return false
        val gold = ledger.goldBalance.value
        if (cost <= 0 || gold < cost) return false
        val updated = current + (id to RuneTree.levelOf(current, id) + 1)
        // A style rune hands over its pen or frame the first time it is bought.
        val reward = RuneRewards.storeItem[id]?.takeIf { RuneTree.levelOf(current, id) == 0 }
        ledger.write(
            gold = gold - cost,
            owned = ledger.ownedStoreIds.value + listOfNotNull(reward)
        ) { stageRunes(updated) }
        publish(updated)
        true
    }

    /**
     * Developer tool: takes every rune level back, pays back the gold they cost (at the designed price) and clears the
     * rune-bound daily flags so the map can be tried again from the start. Returns the gold paid back.
     */
    fun reset(): Int = synchronized(ledger) {
        val current = _runeLevels.value
        val refund = RuneTree.all.filter { it.id != RuneTree.ROOT_ID }.sumOf { r -> r.costs.take(RuneTree.levelOf(current, r.id)).sum() }
        ledger.write(gold = ledger.goldBalance.value + refund) {
            stageRunes(emptyMap())
            stageAbilityClear()
        }
        publish(emptyMap())
        refund
    }

    /** Developer tool: every rune at its top level for free, so the whole map can be looked at. Gold is not touched. */
    fun unlockAll() = synchronized(ledger) {
        val all = RuneTree.all.filter { it.id != RuneTree.ROOT_ID }.associate { it.id to it.maxLevel }
        prefs.edit { stageRunes(all) }
        publish(all)
    }

    /** Puts an account's runes back after a restore: per rune, the higher level wins, never above the rune's top. */
    fun restore(saved: Map<String, Int>) = synchronized(ledger) {
        if (saved.isEmpty()) return
        val current = _runeLevels.value
        val merged = (current.keys + saved.keys)
            .mapNotNull { id -> RuneTree.get(id)?.let { r -> id to maxOf(current[id] ?: 0, saved[id] ?: 0).coerceIn(0, r.maxLevel) } }
            .filter { it.second > 0 }.toMap()
        if (merged == current) return
        prefs.edit(commit = true) { stageRunes(merged) }
        publish(merged)
    }

    /** The account's runes and their daily flags, cleared on [this] editor (an account switch); see [publishCleared]. */
    fun SharedPreferences.Editor.stageClear() {
        stageRunes(emptyMap())
        stageAbilityClear()
    }

    /** Shows the cleared state after [stageClear] landed — or the next account would inherit these levels in memory. */
    fun publishCleared() = publish(emptyMap())

    private fun SharedPreferences.Editor.stageAbilityClear() {
        remove(SettingsRepository.KEY_INSTANT_CHEST_DAY)
        remove(SettingsRepository.KEY_BEST_FRIEND_DAY)
        remove(SettingsRepository.KEY_INSURANCE_MONTH)
    }

    // --- The keystones' abilities ---

    private fun todayEpoch(): Long = TurkeyTime.today().toEpochDay()

    private fun has(effect: RuneEffect) = RuneEffects.has(_runeLevels.value, effect)

    /** The Anahtarcı rune is bought and today's instant open is still unused. */
    fun instantChestAvailable(): Boolean =
        has(RuneEffect.INSTANT) && prefs.getLong(SettingsRepository.KEY_INSTANT_CHEST_DAY, -1L) != todayEpoch()

    /** Marks today's instant open as used, on [this] editor (the chest store writes it with the chest). */
    fun SharedPreferences.Editor.stageInstantUsed() {
        putLong(SettingsRepository.KEY_INSTANT_CHEST_DAY, todayEpoch())
    }

    /** The Dostluk Bağı rune is bought and today's +50% is still unused. */
    fun bestFriendBonusAvailable(): Boolean =
        has(RuneEffect.BEST) && prefs.getLong(SettingsRepository.KEY_BEST_FRIEND_DAY, -1L) != todayEpoch()

    /** The Dostluk Bağı +50% for a room round: true (and spent for today) when it is still available. */
    fun takeBestFriendBonus(): Boolean = synchronized(ledger) {
        if (!bestFriendBonusAvailable()) return false
        prefs.edit { putLong(SettingsRepository.KEY_BEST_FRIEND_DAY, todayEpoch()) }
        true
    }

    private fun insuranceMonth(): Int = TurkeyTime.today().let { it.year * 100 + it.monthValue }

    /** The Seri Sigortası rune is bought and this month's cover is still unused. */
    fun streakInsuranceAvailable(): Boolean =
        has(RuneEffect.INSURE) && prefs.getInt(SettingsRepository.KEY_INSURANCE_MONTH, 0) != insuranceMonth()

    /** Spends this month's Seri Sigortası cover; true when it was available. */
    fun useStreakInsurance(): Boolean = synchronized(ledger) {
        if (!streakInsuranceAvailable()) return false
        prefs.edit { putInt(SettingsRepository.KEY_INSURANCE_MONTH, insuranceMonth()) }
        true
    }

    /** The Şanslı Zar rune: a joker to add to a claimed login reward, or null (25% chance when the rune is bought). */
    fun luckyDiceJoker(): JokerType? =
        if (has(RuneEffect.DICE) && Random.nextInt(100) < 25) JokerType.entries.random() else null
}
