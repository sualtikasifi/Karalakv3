package com.sualtikasifi.cizimhafiza.presentation.common

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hand-tuning of page titles: how far the lettering of each sign is moved from where the layout puts it, in hundredths
 * of the title's own size (so it holds on every phone). The values live on the device; once they are right they are copied
 * (see [report]) and written into [DEFAULTS], which every install then uses.
 *
 * The tuning mode itself is switched on from the developer panel (Araçlar > Başlık ayarı); while it is on, [TitleNudgeOverlay]
 * shows arrows over the app for whichever titles are on the screen.
 */
object TitleNudge {
    /** dx, dy in hundredths of the title size; positive is right / down. Filled in from the hand-tuned values. */
    private val DEFAULTS: Map<String, Pair<Int, Int>> = mapOf(
        // Hand-tuned in the developer panel (Başlık ayarı); each title in every language it is written in.
        "Arkadaşlarım" to (0 to -9),
        "Friends" to (0 to -9),
        "Amigos" to (0 to -9),
        "Arkadaşlarınla Yarış" to (0 to 12),
        "Race Your Friends" to (0 to 12),
        "Compite con tus Amigos" to (0 to 12),
        "Desafie seus Amigos" to (0 to 12),
        "Ayarlar" to (-25 to -18),
        "Settings" to (-25 to -18),
        "Ajustes" to (-25 to -18),
        "Aylık Lig" to (0 to -18),
        "Monthly League" to (0 to -18),
        "Liga Mensual" to (0 to -18),
        "Liga Mensal" to (0 to -18),
        "Başarımlar" to (12 to -15),
        "Achievements" to (12 to -15),
        "Logros" to (12 to -15),
        "Conquistas" to (12 to -15),
        "Bildir" to (18 to -27),
        "a Bug" to (18 to -27),
        "un Problema" to (18 to -27),
        "um Problema" to (18 to -27),
        "Hazır!" to (0 to -15),
        "Ready!" to (0 to -15),
        "¡Listo!" to (0 to -15),
        "Pronto!" to (0 to -15),
        "Hesap" to (0 to -18),
        "Account" to (0 to -18),
        "Cuenta" to (0 to -18),
        "Conta" to (0 to -18),
        "Karalak" to (-6 to -15),
        "Karalak Rünleri" to (21 to 0),
        "Karalak Runes" to (21 to 0),
        "Runas de Karalak" to (21 to 0),
        "Runas do Karalak" to (21 to 0),
        "Koda Katıl" to (0 to -21),
        "Join with Code" to (0 to -21),
        "Unirse con Código" to (0 to -21),
        "Entrar com Código" to (0 to -21),
        "Mağaza" to (-6 to -21),
        "Store" to (-6 to -21),
        "Tienda" to (-6 to -21),
        "Loja" to (-6 to -21),
        "Rakibin" to (0 to 9),
        "Opponent" to (0 to 9),
        "Rival" to (0 to 9),
        "Rakip aranıyor…" to (3 to -9),
        "Finding an opponent…" to (3 to -9),
        "Buscando un rival…" to (3 to -9),
        "Procurando um rival…" to (3 to -9),
        "Sorun" to (18 to -3),
        "Report" to (18 to -3),
        "Reportar" to (18 to -3),
        "Relatar" to (18 to -3),
        "Çevrimdışı Oyna" to (-9 to -3),
        "Play Offline" to (-9 to -3),
        "Jugar sin Conexión" to (-9 to -3),
        "Jogar Offline" to (-9 to -3),
        "Arkadaş ekle" to (16 to -8),
        "Add a friend" to (16 to -8),
        "Agregar amigo" to (16 to -8),
        "Adicionar amigo" to (16 to -8),
        "Arkadaşların" to (23 to -6),
        "Your Friends" to (23 to -6),
        "Tus Amigos" to (23 to -6),
        "Seus Amigos" to (23 to -6),
        "Gelen İstekler" to (0 to -13),
        "Incoming Requests" to (0 to -13),
        "Solicitudes Recibidas" to (0 to -13),
        "Pedidos Recebidos" to (0 to -13),
        "Gönderdiklerim" to (0 to -8),
        "Sent By You" to (0 to -8),
        "Enviados por Ti" to (0 to -8),
        "Enviados por Você" to (0 to -8),
        "Oyun Bitti!" to (7 to -8),
        "Game Over!" to (7 to -8),
        "¡Fin del Juego!" to (7 to -8),
        "Fim de Jogo!" to (7 to -8),
        "Ses ve Titreşim" to (0 to -3),
        "Sound & Vibration" to (0 to -3),
        "Sonido y Vibración" to (0 to -3),
        "Som e Vibração" to (0 to -3),
        "Son Meydan Okumalar" to (0 to -14),
        "Recent Challenges" to (0 to -14),
        "Desafíos Recientes" to (0 to -14),
        "Desafios Recentes" to (0 to -14),
        "Tahmin aşamasına" to (0 to -18),
        "Get ready for" to (0 to -18),
        "¡Prepárate para" to (0 to -18),
        "Prepare-se para" to (0 to -18),
        "Lobi" to (0 to -9),
        "Lobby" to (0 to -9),
        "Sala de Espera" to (0 to -9),
        "hazırlan" to (0 to -30),
        "guessing!" to (0 to -30),
        "adivinar!" to (0 to -30),
        "adivinhar!" to (0 to -30),
        "Sohbet" to (0 to -23),
        "Chat" to (0 to -23)
    )

    private const val PREFS = "title_nudge"
    private const val ON = "__on"

    var enabled by mutableStateOf(false)
        private set

    private val saved: SnapshotStateMap<String, Pair<Int, Int>> = mutableStateMapOf()
    private val active: SnapshotStateList<String> = mutableStateListOf()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            enabled = prefs.getBoolean(ON, false)
            prefs.all.forEach { (k, v) ->
                if (k == ON || v !is String) return@forEach
                val p = v.split(",")
                if (p.size == 2) saved[k] = (p[0].toIntOrNull() ?: 0) to (p[1].toIntOrNull() ?: 0)
            }
        }
    }

    fun setEnabled(context: Context, on: Boolean) {
        load(context)
        enabled = on
        runCatching { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ON, on).apply() }
    }

    fun offsetOf(key: String): Pair<Int, Int> = saved[key] ?: DEFAULTS[key] ?: (0 to 0)

    fun set(context: Context, key: String, dx: Int, dy: Int) {
        saved[key] = dx to dy
        runCatching { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, "$dx,$dy").apply() }
    }

    fun reset(context: Context, key: String) {
        saved.remove(key)
        runCatching { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).apply() }
    }

    internal fun show(key: String) { active.add(key) }
    internal fun hide(key: String) { active.remove(key) }

    /** Every changed title, one per line, ready to be pasted back. */
    fun report(): String =
        saved.entries.filter { it.value != (0 to 0) }.sortedBy { it.key }.joinToString("\n") { "\"${it.key}\" -> x ${it.value.first}, y ${it.value.second}" }

    internal fun visibleKeys(): List<String> = active.distinct()
}

/** The tuning arrows, over the whole app; nothing at all while the mode is off. */
@Composable
fun TitleNudgeOverlay() {
    val context = LocalContext.current
    TitleNudge.load(context)
    if (!TitleNudge.enabled) return
    val keys = TitleNudge.visibleKeys()
    var picked by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(1) }
    val key = picked?.takeIf { it in keys } ?: keys.firstOrNull()
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xEE1E1E24)).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (key == null) {
                Text("Bu ekranda başlık yok", color = Color.White, fontSize = 13.sp)
            } else {
                val (dx, dy) = TitleNudge.offsetOf(key)
                Text("“$key”   x $dx · y $dy", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (keys.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        keys.take(4).forEach { k ->
                            Chip(k.take(14), selected = k == key) { picked = k }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Arrow(Icons.Filled.KeyboardArrowLeft) { TitleNudge.set(context, key, dx - step, dy) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Arrow(Icons.Filled.KeyboardArrowUp) { TitleNudge.set(context, key, dx, dy - step) }
                        Arrow(Icons.Filled.KeyboardArrowDown) { TitleNudge.set(context, key, dx, dy + step) }
                    }
                    Arrow(Icons.Filled.KeyboardArrowRight) { TitleNudge.set(context, key, dx + step, dy) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Adım 1", step == 1) { step = 1 }
                    Chip("Adım 3", step == 3) { step = 3 }
                    Chip("Sıfırla", false) { TitleNudge.reset(context, key) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(if (copied) "Kopyalandı ✓" else "Tüm değerleri kopyala", false) {
                    clipboard.setText(AnnotatedString(TitleNudge.report().ifBlank { "(değişen başlık yok)" }))
                    copied = true
                }
                Chip("Modu kapat", false) { TitleNudge.setEnabled(context, false) }
            }
        }
    }
}

@Composable
private fun Arrow(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF3B3B46)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp)) }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFE8731A) else Color(0xFF3B3B46))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}
