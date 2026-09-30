package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import com.sualtikasifi.cizimhafiza.util.UsernameClaimResult
import com.sualtikasifi.cizimhafiza.util.UsernameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsernameFinalizeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val usernameRepository: UsernameRepository
) : ViewModel() {

    private val dismissed = MutableStateFlow(false)

    /** The server has been asked whether this account already went through the offer, so a returning account never flashes it. */
    private val checked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            usernameRepository.ensureUsername()
            checked.value = true
        }
    }

    /** The signed-in account still has a changeable name: offer to keep it or change it one last time. */
    val shouldPrompt: StateFlow<Boolean> = combine(
        authRepository.authState,
        settingsRepository.nicknameRenameUsed,
        settingsRepository.nickname,
        dismissed,
        checked
    ) { auth, locked, nickname, dismissed, checked ->
        checked && auth is AuthState.Linked && !locked && nickname.isNotBlank() && !dismissed
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val currentName: StateFlow<String> = settingsRepository.nickname

    fun later() { dismissed.value = true }

    /** Keeps the name when [name] is unchanged, otherwise renames — either way the result is permanent. */
    suspend fun finalize(name: String): UsernameClaimResult {
        val trimmed = name.trim()
        return if (trimmed == settingsRepository.nickname.value.trim()) {
            usernameRepository.lockCurrent()
        } else {
            usernameRepository.change(trimmed, final = true)
        }
    }
}

/**
 * Shown once an account has been linked to Google while its username can still change. The name
 * and the profile now belong to that Google account; this is the last chance to change the name.
 */
@Composable
fun UsernameFinalizeHost(viewModel: UsernameFinalizeViewModel = hiltViewModel()) {
    val prompt by viewModel.shouldPrompt.collectAsState()
    val current by viewModel.currentName.collectAsState()
    if (!prompt) return

    var text by remember(current) { mutableStateOf(current) }
    var error by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val unchanged = text.trim() == current.trim()

    // Not dismissable: this is the one and only offer, made the first time Google is linked.
    Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        RaisedCard(corner = 28.dp, raise = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.username_finalize_title),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.username_finalize_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                AppTextField(
                    value = text,
                    onValueChange = { if (it.length <= 16 && !busy) { text = it; error = null } },
                    centered = true
                )
                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = stringResource(if (unchanged) R.string.username_finalize_keep else R.string.username_finalize_change),
                    onClick = {
                        if (busy) return@PrimaryButton
                        busy = true
                        scope.launch {
                            error = when (viewModel.finalize(text)) {
                                UsernameClaimResult.Invalid -> R.string.username_error_invalid
                                UsernameClaimResult.Taken -> R.string.username_error_taken
                                UsernameClaimResult.NetworkError -> R.string.username_error_network
                                else -> null
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && text.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
