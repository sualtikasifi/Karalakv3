package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sualtikasifi.cizimhafiza.BuildConfig
import com.sualtikasifi.cizimhafiza.data.repository.EconomyConfigRepository
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.util.UpdateGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UpdateAdminState(val busy: Boolean = false, val message: String? = null)

/** Writes the lowest playable version code (`minVersionCode` on config/economy, see [UpdateGate]); the reviewer's rules allow it. */
@HiltViewModel
class UpdateAdminViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val economyConfigRepository: EconomyConfigRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateAdminState())
    val state: StateFlow<UpdateAdminState> = _state.asStateFlow()

    /** Reads the document from the server now, so the number shown is what players will get. */
    fun refresh() = economyConfigRepository.refreshNow()

    fun setMinimum(versionCode: Int) {
        if (_state.value.busy) return
        _state.update { UpdateAdminState(busy = true) }
        viewModelScope.launch {
            runCatching {
                // merge: the document also carries the economy's amounts, which must stay as they are
                firestore.document("config/economy").set(mapOf("minVersionCode" to versionCode.coerceAtLeast(0)), SetOptions.merge()).await()
            }.onSuccess {
                _state.update { UpdateAdminState(message = if (versionCode > 0) "Kilit konuldu: $versionCode altı sürümler oynayamaz. Telefonlara yarım saat içinde ulaşır." else "Kilit kaldırıldı.") }
                refresh()
            }.onFailure {
                _state.update { UpdateAdminState(message = "Yazılamadı (internet ya da yetki).") }
            }
        }
    }
}

/** The developer panel's Güncelleme tool: lock every install below a version out of the game until it updates. */
@Composable
internal fun UpdatePanel(viewModel: UpdateAdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val min by UpdateGate.minVersionCode.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }
    var input by remember { mutableStateOf(BuildConfig.VERSION_CODE.toString()) }
    var confirm by remember { mutableStateOf(false) }
    val code = input.toIntOrNull()

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
    ) {
        Text(
            "Kilitlenen sürümden eski bir uygulama, güncelleme kartını görür ve oyuna giremez; \"Güncelle\" Play Store'u açar. İnternetsiz telefon ve bu hesap kilitlenmez.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Bu uygulamanın sürümü: ${BuildConfig.VERSION_NAME} (kod ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.titleSmall)
        Text(
            if (min > 0) "Şu an kilitli: kod $min altı sürümler" else "Şu an kilit yok",
            style = MaterialTheme.typography.titleMedium
        )
        AppTextField(
            value = input,
            onValueChange = { v -> input = v.filter(Char::isDigit).take(6) },
            label = "En düşük oynanabilir sürüm kodu",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Dikkat: Bu sayıdan küçük her sürüm kilitlenir. Yeni sürüm Play'de yayınlanmadan kilit koyma, yoksa kimse güncelleyemez.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        PrimaryButton(text = "Kilidi koy", onClick = { confirm = true }, enabled = !state.busy && code != null && code > 0, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { viewModel.setMinimum(0) }, enabled = !state.busy && min > 0, modifier = Modifier.fillMaxWidth()) { Text("Kilidi kaldır") }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }

    if (confirm && code != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Kilit konsun mu?") },
            text = { Text("Kod $code altındaki tüm sürümler güncelleyene kadar oyunu açamayacak. Yeni sürümün Play'de yayında olduğundan emin ol.") },
            confirmButton = { TextButton(onClick = { confirm = false; viewModel.setMinimum(code) }) { Text("Kilidi koy", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } }
        )
    }
}
