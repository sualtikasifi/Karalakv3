package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNotice
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSection
import com.sualtikasifi.cizimhafiza.presentation.reports.console.NoticeKind
import com.sualtikasifi.cizimhafiza.presentation.reports.console.StatTile
import com.sualtikasifi.cizimhafiza.presentation.reports.console.noticeKindOf
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
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.util.AdminAudit
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

/**
 * What may be written as the lowest playable version. A version no build has reached would lock out every player for good:
 * the number may be at most the version code of this very app, which is already out when its panel is open.
 */
object UpdateLock {
    /** What is wrong with locking below [code] from an app of version [thisBuild], as a line for the panel; null when it is fine (0 removes the lock). */
    fun problem(code: Int, thisBuild: Int): String? = when {
        code < 0 -> "Sürüm kodu eksi olamaz."
        code > thisBuild ->
            "Bu uygulamanın sürüm kodu $thisBuild; daha yüksek bir sayı konamaz, kimse oyuna giremezdi. " +
                "Daha yeni bir sürümü kilitlemek için önce o sürümü bu telefona kur."
        else -> null
    }

    /** The audit-log line: what the lock was and what it is now. */
    fun detail(before: Int, after: Int): String = when {
        after <= 0 -> "kilit kaldırıldı (önceki: $before)"
        else -> "en düşük sürüm kodu $before -> $after"
    }
}

/** Writes the lowest playable version code (`minVersionCode` on config/economy, see [UpdateGate]); the reviewer's rules allow it. */
@HiltViewModel
class UpdateAdminViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val economyConfigRepository: EconomyConfigRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateAdminState())
    val state: StateFlow<UpdateAdminState> = _state.asStateFlow()

    /** Reads the document from the server now, so the number shown is what players will get. */
    fun refresh() = economyConfigRepository.refreshNow()

    fun setMinimum(versionCode: Int, thisBuild: Int = BuildConfig.VERSION_CODE) {
        if (_state.value.busy) return
        val problem = UpdateLock.problem(versionCode, thisBuild)
        if (problem != null) {
            _state.update { UpdateAdminState(message = problem) }
            return
        }
        val before = UpdateGate.minVersionCode.value
        val after = versionCode.coerceAtLeast(0)
        _state.update { UpdateAdminState(busy = true) }
        viewModelScope.launch {
            runCatching {
                // merge: the document also carries the economy's amounts, which must stay as they are; the audit line goes in the same batch
                val batch = firestore.batch()
                batch.set(firestore.document("config/economy"), mapOf("minVersionCode" to after), SetOptions.merge())
                AdminAudit.add(firestore, batch, auth.currentUser?.email, "update-lock", "config/economy", UpdateLock.detail(before, after))
                batch.commit().await()
            }.onSuccess {
                _state.update { UpdateAdminState(message = if (after > 0) "Kilit konuldu: $after altı sürümler oynayamaz. Telefonlara yarım saat içinde ulaşır." else "Kilit kaldırıldı.") }
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
    // the same limit the write enforces, shown while typing
    val tooHigh = code?.let { UpdateLock.problem(it, BuildConfig.VERSION_CODE) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Console.Gap),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Console.Gap)) {
            StatTile("Bu uygulama", "${BuildConfig.VERSION_CODE}", Modifier.weight(1f), changeCaption = "sürüm ${BuildConfig.VERSION_NAME}")
            StatTile(
                "En düşük oynanabilir", if (min > 0) "$min" else "—", Modifier.weight(1f),
                changeCaption = if (min > 0) "altı kilitli" else "kilit yok"
            )
        }
        ConsoleNotice(
            "Kilitlenen sürümden eski bir uygulama güncelleme kartını görür ve oyuna giremez; \"Güncelle\" Play Store'u açar. İnternetsiz telefon ve bu hesap kilitlenmez.",
            kind = NoticeKind.Info
        )
        ConsoleSection("Sürüm kilidi") {
            ConsoleTextField(
                value = input,
                onValueChange = { v -> input = v.filter(Char::isDigit).take(6) },
                label = "En düşük oynanabilir sürüm kodu",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            tooHigh?.let { Spacer(Modifier.height(10.dp)); ConsoleNotice(it, kind = NoticeKind.Error) }
            Spacer(Modifier.height(10.dp))
            ConsoleNotice("Bu sayıdan küçük her sürüm kilitlenir. Yeni sürüm Play'de yayınlanmadan kilit koyma, yoksa kimse güncelleyemez.", kind = NoticeKind.Warning)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleSecondaryButton("Kilidi kaldır", { viewModel.setMinimum(0) }, Modifier.weight(1f), enabled = !state.busy && min > 0, icon = Icons.Filled.LockOpen)
                ConsoleButton("Kilidi koy", { confirm = true }, Modifier.weight(1f), enabled = !state.busy && code != null && code > 0 && tooHigh == null, loading = state.busy, icon = Icons.Filled.Lock, style = ConsoleButtonStyle.Danger)
            }
        }
        state.message?.let { ConsoleNotice(it, kind = noticeKindOf(it)) }
    }

    if (confirm && code != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Kilit konsun mu?") },
            text = { Text("Kod $code altındaki tüm sürümler güncelleyene kadar oyunu açamayacak (şu an: ${if (min > 0) "$min altı kilitli" else "kilit yok"}). Yeni sürümün Play'de yayında olduğundan emin ol. İşlem denetim kaydına yazılır.") },
            confirmButton = { TextButton(onClick = { confirm = false; viewModel.setMinimum(code) }) { Text("Kilidi koy", color = Console.Critical) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } }
        )
    }
}
