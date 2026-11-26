package com.sualtikasifi.cizimhafiza.presentation.gifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sualtikasifi.cizimhafiza.domain.model.GiftBlock
import com.sualtikasifi.cizimhafiza.domain.model.GiftException
import com.sualtikasifi.cizimhafiza.domain.model.GiftOffer
import com.sualtikasifi.cizimhafiza.domain.model.GiftUsage
import com.sualtikasifi.cizimhafiza.domain.model.IncomingGift
import com.sualtikasifi.cizimhafiza.domain.repository.GiftRepository
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GiftSendState(
    val friendUid: String = "",
    val friendName: String = "",
    val loading: Boolean = true,
    val usage: GiftUsage? = null,
    val blocked: GiftBlock? = null,
    val sending: Boolean = false,
    val sent: GiftOffer? = null,
    val error: GiftBlock? = null
)

/** The "Hediye gönder" window on a friend's row. */
@HiltViewModel
class GiftSendViewModel @Inject constructor(
    private val repository: GiftRepository,
    private val settings: SettingsRepository,
    private val legal: LegalConsentStore,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _state = MutableStateFlow<GiftSendState?>(null)
    val state: StateFlow<GiftSendState?> = _state.asStateFlow()
    val gold get() = settings.goldBalance

    fun open(friendUid: String, friendName: String) {
        _state.value = GiftSendState(friendUid, friendName)
        viewModelScope.launch {
            val block = when {
                legal.childMode.value -> GiftBlock.CHILD_MODE
                auth.currentUser?.isAnonymous != false -> GiftBlock.NOT_LINKED
                settings.playerLevelNow() < com.sualtikasifi.cizimhafiza.domain.model.GiftRules.MIN_SENDER_LEVEL -> GiftBlock.LEVEL
                else -> null
            }
            if (block != null) { _state.value = _state.value?.copy(loading = false, blocked = block); return@launch }
            repository.usage(friendUid)
                .onSuccess { u -> _state.value = _state.value?.copy(loading = false, usage = u) }
                .onFailure { e -> _state.value = _state.value?.copy(loading = false, blocked = (e as? GiftException)?.block ?: GiftBlock.OFFLINE) }
        }
    }

    fun send(offer: GiftOffer, myName: String) {
        val s = _state.value ?: return
        if (s.sending) return
        _state.value = s.copy(sending = true, error = null)
        viewModelScope.launch {
            repository.send(s.friendUid, myName, offer)
                .onSuccess { _state.value = _state.value?.copy(sending = false, sent = offer) }
                .onFailure { e -> _state.value = _state.value?.copy(sending = false, error = (e as? GiftException)?.block ?: GiftBlock.OFFLINE) }
        }
    }

    fun close() { _state.value = null }
}

/** The gifts waiting for this player: read once per launch, claimed together. */
@HiltViewModel
class GiftInboxViewModel @Inject constructor(
    private val repository: GiftRepository,
    private val legal: LegalConsentStore,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _gifts = MutableStateFlow<List<IncomingGift>>(emptyList())
    val gifts: StateFlow<List<IncomingGift>> = _gifts.asStateFlow()
    private val _results = MutableStateFlow<List<Pair<IncomingGift, GiftRepository.ClaimResult>>>(emptyList())
    val results: StateFlow<List<Pair<IncomingGift, GiftRepository.ClaimResult>>> = _results.asStateFlow()
    private var checked = false

    fun check() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            val linked = auth.currentUser?.isAnonymous == false
            repository.publishProfile(giftsOk = linked && !legal.childMode.value)
            if (!linked || legal.childMode.value) return@launch
            _gifts.value = repository.fetchInbox().getOrDefault(emptyList())
        }
    }

    fun claimAll() {
        val list = _gifts.value
        _gifts.value = emptyList()
        viewModelScope.launch { _results.value = list.map { it to repository.claim(it) } }
    }

    fun dismissResults() { _results.value = emptyList() }
}
