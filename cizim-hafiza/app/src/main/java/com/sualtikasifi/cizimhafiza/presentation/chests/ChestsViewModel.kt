package com.sualtikasifi.cizimhafiza.presentation.chests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestReward
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The "Kasalarım" screen: the 4 chest slots (see SettingsRepository.
 * chestSlots), earned only from a won Arkadaşla Yarış match, and the gold
 * they pay out.
 */
@HiltViewModel
class ChestsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val adManager: com.sualtikasifi.cizimhafiza.ads.AdManager
) : ViewModel() {

    val chestSlots: StateFlow<List<Chest?>> = settingsRepository.chestSlots
    val goldBalance: StateFlow<Int> = settingsRepository.goldBalance
    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
    fun enableNotifications() = settingsRepository.setNotificationsEnabled(true)

    // Every countdown on screen reads against this rather than calling
    // System.currentTimeMillis() directly in the composable — one ticking
    // clock the whole slot row recomposes from together, instead of each
    // card free-running its own.
    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    /** Non-null exactly once, right after a chest is opened — the reveal dialog reads and clears it. */
    private val _lastReward = MutableStateFlow<ChestReward?>(null)
    val lastReward: StateFlow<ChestReward?> = _lastReward.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _nowMillis.value = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }

    fun startUnlocking(chestId: String) {
        settingsRepository.startUnlockingChest(chestId)
    }

    fun open(chestId: String) {
        settingsRepository.openChestIfReady(chestId)?.let { _lastReward.value = it }
    }

    fun consumeLastReward() {
        _lastReward.value = null
    }

    /** True while today's rewarded-ad speed-up has not been used yet. */
    val speedupAvailable: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        settingsRepository.chestSpeedupDay,
        _nowMillis
    ) { day, _ -> day != com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay() }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, settingsRepository.chestSpeedupDay.value != com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay())

    /**
     * Watches a rewarded ad and, if it is earned, takes [ChestSlots.SPEEDUP_MILLIS]
     * off the chest that is currently unlocking. [onNotice] gets a string
     * resource to show as a short message.
     */
    fun speedUp(activity: android.app.Activity, onNotice: (Int) -> Unit) {
        val chest = settingsRepository.chestSlots.value.firstOrNull { it?.unlockStartedAtMillis != null && !it.isReady(System.currentTimeMillis()) } ?: return
        adManager.maybeShowRewarded(activity, "chest_speedup") { outcome ->
            when (outcome) {
                com.sualtikasifi.cizimhafiza.ads.RewardedOutcome.EARNED -> {
                    if (settingsRepository.speedUpChest(chest.id, com.sualtikasifi.cizimhafiza.domain.model.ChestSlots.SPEEDUP_MILLIS)) {
                        onNotice(com.sualtikasifi.cizimhafiza.R.string.chest_speedup_done)
                    }
                }
                com.sualtikasifi.cizimhafiza.ads.RewardedOutcome.SKIPPED -> Unit
                else -> onNotice(com.sualtikasifi.cizimhafiza.R.string.chest_speedup_unavailable)
            }
        }
    }
}
