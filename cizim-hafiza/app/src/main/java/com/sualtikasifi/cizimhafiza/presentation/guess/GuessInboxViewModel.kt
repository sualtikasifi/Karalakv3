package com.sualtikasifi.cizimhafiza.presentation.guess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.domain.model.GuessInboxEntry
import com.sualtikasifi.cizimhafiza.domain.repository.GuessRepository
import com.sualtikasifi.cizimhafiza.util.GuessStore
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What came of the player's "Bil Bakalım" links, shown once when the home screen opens (there are no push messages
 * to tell them: this window is how they find out). A friend who guessed one pays [GuessStore.REWARD_GOLD] gold, up
 * to [GuessStore.MAX_PAID_SOLVES_PER_DAY] a day.
 */
@HiltViewModel
class GuessInboxViewModel @Inject constructor(
    private val repository: GuessRepository,
    private val store: GuessStore,
    private val settingsRepository: SettingsRepository,
    private val legalConsentStore: LegalConsentStore
) : ViewModel() {

    private val _entries = MutableStateFlow<List<GuessInboxEntry>>(emptyList())
    val entries: StateFlow<List<GuessInboxEntry>> = _entries.asStateFlow()

    private var checked = false

    /** Reads the inbox, once per launch (and tidies up expired links while at it). Does nothing for a player who may be under 13. */
    fun check() {
        if (checked || legalConsentStore.childMode.value) return
        checked = true
        viewModelScope.launch {
            repository.deleteExpiredLinks()
            _entries.value = repository.fetchInbox().getOrDefault(emptyList())
        }
    }

    /** The gold the entries on screen are worth right now. */
    val rewardGold: Int
        get() = paidSolves() * GuessStore.REWARD_GOLD

    private fun paidSolves(): Int = minOf(_entries.value.count { it.solved }, store.paidSolvesLeftToday)

    /** Pays what is due, clears the entries from the inbox and closes the window. */
    fun claim() {
        val list = _entries.value
        if (list.isEmpty()) return
        val paid = paidSolves()
        if (paid > 0) {
            settingsRepository.earnGold(paid * GuessStore.REWARD_GOLD)
            store.recordPaidSolves(paid)
        }
        _entries.value = emptyList()
        viewModelScope.launch { repository.clearInbox(list.map { it.id }) }
    }
}
