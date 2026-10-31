package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.ActiveLock
import com.sualtikasifi.cizimhafiza.domain.repository.ModerationRepository
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSurface
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSecondaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocksState(
    val locks: List<ActiveLock> = emptyList(),
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocksViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LocksState())
    val state: StateFlow<LocksState> = _state.asStateFlow()

    fun load() {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            val result = moderationRepository.activeLocks()
            _state.value = LocksState(
                locks = result.getOrNull().orEmpty(),
                loaded = true,
                busy = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun cancel(lock: ActiveLock) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            val result = moderationRepository.cancelLock(lock)
            if (result.isSuccess) {
                _state.value = _state.value.copy(locks = _state.value.locks.filterNot { it.penaltyId == lock.penaltyId }, busy = false)
            } else {
                _state.value = _state.value.copy(busy = false, error = result.exceptionOrNull()?.message)
            }
        }
    }
}

/** The developer panel's list of accounts currently locked out of the online modes, each with a cancel button. */
@Composable
fun LocksPanel(viewModel: LocksViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var pendingCancel by remember { mutableStateOf<ActiveLock?>(null) }
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.error?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
        }
        if (state.loaded && state.locks.isEmpty() && state.error == null) {
            Text(
                text = stringResource(R.string.locks_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        val now = System.currentTimeMillis()
        state.locks.forEach { lock ->
            ConsoleSurface(corner = 20.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = lock.nickname.ifBlank { lock.uid.take(8) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.locks_line, lock.strike, remainingText(lock.lockedUntilMillis - now)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = lock.uid,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    ConsoleSecondaryButton(
                        text = stringResource(R.string.locks_cancel),
                        onClick = { pendingCancel = lock },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    pendingCancel?.let { lock ->
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = { Text(stringResource(R.string.locks_cancel_title)) },
            text = { Text(stringResource(R.string.locks_cancel_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancel(lock)
                    pendingCancel = null
                }) { Text(stringResource(R.string.locks_cancel)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) { Text(stringResource(R.string.reports_feedback_delete_confirm_cancel)) }
            }
        )
    }
}

@Composable
private fun remainingText(millis: Long): String {
    val minutes = ((millis.coerceAtLeast(0L) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    return when {
        minutes < 60 -> stringResource(if (minutes == 1) R.string.lock_duration_minutes_one else R.string.lock_duration_minutes, minutes)
        minutes >= 24 * 60 -> ((minutes + 24 * 60 - 1) / (24 * 60)).let { stringResource(if (it == 1) R.string.lock_duration_days_one else R.string.lock_duration_days, it) }
        else -> ((minutes + 59) / 60).let { stringResource(if (it == 1) R.string.lock_duration_hours_one else R.string.lock_duration_hours, it) }
    }
}
