package com.sualtikasifi.cizimhafiza.presentation.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The gold store: pens and frames that are sold rather than unlocked by
 * level. What is for sale is simply every [PenSkin]/[AvatarFrame] with a
 * [PenSkin.storePrice] — adding a frame to the store is one enum entry.
 */
@HiltViewModel
class StoreViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository,
    private val adManager: com.sualtikasifi.cizimhafiza.ads.AdManager
) : ViewModel() {

    val gold: StateFlow<Int> = settingsRepository.goldBalance

    /** True while the player has no linked (Google) account, so nothing they buy is backed up. */
    val isGuest: StateFlow<Boolean> = authRepository.authState
        .map { it !is AuthState.Linked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.authState.value !is AuthState.Linked)
    val owned: StateFlow<Set<String>> = settingsRepository.ownedStoreIds
    val selectedPenId: StateFlow<String> = settingsRepository.selectedPenSkinId
    val selectedFrameId: StateFlow<String> = settingsRepository.selectedAvatarFrameId

    val pens: List<PenSkin> = PenSkin.entries.filter { it.isStoreItem }.sortedBy { it.storePrice }
    val frames: List<AvatarFrame> = AvatarFrame.entries.filter { it.isStoreItem }.sortedBy { it.storePrice }

    fun buyPen(skin: PenSkin): Boolean = settingsRepository.purchaseStoreItem(penId(skin), skin.storePrice)
    fun buyFrame(frame: AvatarFrame): Boolean = settingsRepository.purchaseStoreItem(frameId(frame), frame.storePrice)

    val jokerCounts: StateFlow<Map<com.sualtikasifi.cizimhafiza.domain.model.JokerType, Int>> = settingsRepository.jokerCounts
    fun buyJoker(type: com.sualtikasifi.cizimhafiza.domain.model.JokerType, quantity: Int): Boolean = settingsRepository.purchaseJoker(type, quantity)

    /**
     * Today's free joker while it is still unclaimed (and ads exist at all),
     * null once it has been taken — which is what removes its card from the
     * store. Re-evaluated every minute so it reappears at midnight without the
     * screen being reopened.
     */
    val dailyJoker: StateFlow<com.sualtikasifi.cizimhafiza.domain.model.JokerType?> = kotlinx.coroutines.flow.combine(
        settingsRepository.dailyJokerDay,
        kotlinx.coroutines.flow.flow { while (true) { emit(Unit); kotlinx.coroutines.delay(60_000) } }
    ) { claimedDay, _ ->
        val today = java.time.LocalDate.now().toEpochDay()
        if (!com.sualtikasifi.cizimhafiza.util.GameConstants.ADMOB_ENABLED || claimedDay == today) null
        else com.sualtikasifi.cizimhafiza.domain.model.DailyJoker.typeFor(today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Shows a rewarded ad and, only if it is earned, adds today's joker. [onNotice] gets a string resource for a short message. */
    fun claimDailyJoker(activity: android.app.Activity, onNotice: (Int, Boolean) -> Unit) {
        adManager.maybeShowRewarded(activity, "store_daily_joker") { outcome ->
            when (outcome) {
                com.sualtikasifi.cizimhafiza.ads.RewardedOutcome.EARNED -> {
                    if (settingsRepository.claimDailyJoker() != null) {
                        onNotice(com.sualtikasifi.cizimhafiza.R.string.store_daily_joker_claimed, false)
                    }
                }
                com.sualtikasifi.cizimhafiza.ads.RewardedOutcome.SKIPPED -> Unit
                else -> onNotice(com.sualtikasifi.cizimhafiza.R.string.store_daily_joker_unavailable, true)
            }
        }
    }

    fun equipPen(skin: PenSkin) = settingsRepository.setSelectedPenSkin(skin)
    fun equipFrame(frame: AvatarFrame) = settingsRepository.setSelectedAvatarFrame(frame)

    companion object {
        fun penId(skin: PenSkin) = "pen:${skin.name}"
        fun frameId(frame: AvatarFrame) = "frame:${frame.name}"
    }
}
