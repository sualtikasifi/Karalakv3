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
    private val authRepository: com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
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

    fun equipPen(skin: PenSkin) = settingsRepository.setSelectedPenSkin(skin)
    fun equipFrame(frame: AvatarFrame) = settingsRepository.setSelectedAvatarFrame(frame)

    companion object {
        fun penId(skin: PenSkin) = "pen:${skin.name}"
        fun frameId(frame: AvatarFrame) = "frame:${frame.name}"
    }
}
