package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
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

/**
 * The player's wallet: gold, jokers and owned store items, and the tamper seal over all three (see [EconomyGuard]).
 *
 * Every change goes through [write], which puts the three values, the seal and any keys of the feature that caused the
 * change (a chest slot, a rune level) into ONE edit — so a reward and what paid for it can never land apart. The rune
 * and chest stores lock on this object too, so a purchase and a payout never interleave.
 */
@Singleton
class EconomyLedger @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _goldBalance = MutableStateFlow(prefs.getInt(SettingsRepository.KEY_GOLD_BALANCE, 0))
    val goldBalance: StateFlow<Int> = _goldBalance.asStateFlow()

    private val _jokerCounts = MutableStateFlow(loadJokers())
    val jokerCounts: StateFlow<Map<JokerType, Int>> = _jokerCounts.asStateFlow()

    private val _ownedStoreIds = MutableStateFlow(prefs.getStringSet(SettingsRepository.KEY_OWNED_STORE_IDS, emptySet())?.toSet() ?: emptySet())
    val ownedStoreIds: StateFlow<Set<String>> = _ownedStoreIds.asStateFlow()

    // Off the main thread: EconomyGuard's first call generates (or reads) an AndroidKeyStore key, which can cost
    // 50-200ms on some devices — enough to visibly drop frames right at cold start otherwise.
    init { scope.launch { verify() } }

    private fun loadJokers(): Map<JokerType, Int> =
        prefs.getString(SettingsRepository.KEY_JOKERS, null)
            ?.let { runCatching { Json.decodeFromString<Map<String, Int>>(it) }.getOrNull() }
            ?.mapNotNull { (name, n) -> runCatching { JokerType.valueOf(name) }.getOrNull()?.let { it to n } }
            ?.toMap()
            ?: emptyMap()

    private fun payload(gold: Int, jokers: Map<JokerType, Int>, owned: Set<String>): String =
        "$gold|" + jokers.entries.filter { it.value != 0 }.sortedBy { it.key.name }.joinToString(",") { "${it.key.name}=${it.value}" } +
            "|" + owned.sorted().joinToString(",")

    /** Puts the three values and their seal on [this] editor — for an edit the caller owns (an account restore). */
    fun SharedPreferences.Editor.stageEconomy(gold: Int, jokers: Map<JokerType, Int>, owned: Set<String>) {
        putInt(SettingsRepository.KEY_GOLD_BALANCE, gold)
        putString(SettingsRepository.KEY_JOKERS, Json.encodeToString(jokers.mapKeys { it.key.name }))
        putStringSet(SettingsRepository.KEY_OWNED_STORE_IDS, owned)
        putString(SettingsRepository.KEY_ECONOMY_SEAL, EconomyGuard.sign(payload(gold, jokers, owned)))
    }

    /** Shows values a staged edit (see [stageEconomy]) has written. */
    fun publish(gold: Int, jokers: Map<JokerType, Int>, owned: Set<String>) {
        _goldBalance.value = gold
        _jokerCounts.value = jokers
        _ownedStoreIds.value = owned
    }

    /**
     * The one way the wallet changes: new values, sealed, together with [extra] keys of the same change, in a single
     * edit ([commit] when it must be on disk before the call returns).
     */
    @Synchronized
    fun write(
        gold: Int = _goldBalance.value,
        jokers: Map<JokerType, Int> = _jokerCounts.value,
        owned: Set<String> = _ownedStoreIds.value,
        commit: Boolean = false,
        extra: SharedPreferences.Editor.() -> Unit = {}
    ) {
        prefs.edit(commit = commit) {
            stageEconomy(gold, jokers, owned)
            extra()
        }
        publish(gold, jokers, owned)
    }

    /**
     * Checks the stored wallet against its seal. No key yet (fresh install, new phone) means nothing can be compared,
     * so the current numbers are trusted and sealed; a missing or wrong seal means they were edited from outside, and
     * the wallet is emptied.
     */
    @Synchronized
    private fun verify() {
        val seal = prefs.getString(SettingsRepository.KEY_ECONOMY_SEAL, null)
        when {
            !EconomyGuard.hasKey() -> write()
            seal != null && EconomyGuard.verify(payload(_goldBalance.value, _jokerCounts.value, _ownedStoreIds.value), seal) -> Unit
            else -> {
                android.util.Log.w("EconomyLedger", "Economy seal mismatch — resetting gold, jokers and store items")
                write(0, emptyMap(), emptySet())
            }
        }
    }

    /** Gold earned by playing (rounds, achievements, ads). */
    @Synchronized
    fun addGold(amount: Int) {
        if (amount <= 0) return
        write(gold = _goldBalance.value + amount)
        HomeShownStats.pendingGold += amount
    }

    /** Takes gold back after a moderation penalty, never below zero. Returns what was actually taken. */
    @Synchronized
    fun revokeGold(amount: Int): Int {
        if (amount <= 0) return 0
        val lost = minOf(amount, _goldBalance.value)
        write(gold = _goldBalance.value - lost, commit = true)
        return lost
    }

    /** Buys [id] for [price] gold. False if already owned or the balance is too low. */
    @Synchronized
    fun purchaseStoreItem(id: String, price: Int): Boolean {
        if (price <= 0 || id in _ownedStoreIds.value || _goldBalance.value < price) return false
        write(gold = _goldBalance.value - price, owned = _ownedStoreIds.value + id)
        return true
    }

    /** Puts a store item straight into the collection (chest drop, rune reward) — no gold changes hands. */
    @Synchronized
    fun grantStoreItem(id: String) {
        if (id in _ownedStoreIds.value) return
        write(owned = _ownedStoreIds.value + id)
    }

    @Synchronized
    fun addJoker(type: JokerType, quantity: Int) {
        if (quantity <= 0) return
        write(jokers = _jokerCounts.value + (type to ((_jokerCounts.value[type] ?: 0) + quantity)))
    }

    /** Spends one joker; false if none are left. */
    @Synchronized
    fun useJoker(type: JokerType): Boolean {
        val have = _jokerCounts.value[type] ?: 0
        if (have <= 0) return false
        write(jokers = _jokerCounts.value + (type to (have - 1)))
        return true
    }

    /** Buys [quantity] jokers for [price] gold; false if the balance can't cover it. */
    @Synchronized
    fun purchaseJoker(type: JokerType, quantity: Int, price: Int): Boolean {
        if (quantity <= 0 || _goldBalance.value < price) return false
        write(
            gold = _goldBalance.value - price,
            jokers = _jokerCounts.value + (type to ((_jokerCounts.value[type] ?: 0) + quantity))
        )
        return true
    }
}
