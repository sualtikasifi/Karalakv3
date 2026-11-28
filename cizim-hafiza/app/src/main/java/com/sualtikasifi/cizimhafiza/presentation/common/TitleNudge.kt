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
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures

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
        "Arkadaşlarım" to (0 to -5),
        "Friends" to (0 to -5),
        "Amigos" to (0 to -5),
        "Arkadaşlarınla Yarış" to (0 to 12),
        "Race Your Friends" to (0 to 12),
        "Compite con tus Amigos" to (0 to 12),
        "Desafie seus Amigos" to (0 to 12),
        "Ayarlar" to (0 to 0),
        "Settings" to (0 to 0),
        "Ajustes" to (0 to 0),
        "Aylık Lig" to (0 to -15),
        "Monthly League" to (0 to -15),
        "Liga Mensual" to (0 to -15),
        "Liga Mensal" to (0 to -15),
        "Başarımlar" to (0 to 0),
        "Achievements" to (0 to 0),
        "Logros" to (0 to 0),
        "Conquistas" to (0 to 0),
        "Bildir" to (18 to -27),
        "a Bug" to (18 to -27),
        "un Problema" to (18 to -27),
        "um Problema" to (18 to -27),
        "Hazır!" to (0 to -15),
        "Ready!" to (0 to -15),
        "¡Listo!" to (0 to -15),
        "Pronto!" to (0 to -15),
        "Hesap" to (0 to -3),
        "Account" to (0 to -3),
        "Cuenta" to (0 to -3),
        "Conta" to (0 to -3),
        "Meydan Oku" to (3 to -18),
        "Challenge" to (3 to -18),
        "Desafío" to (3 to -18),
        "Desafio" to (3 to -18),
        "Karalak" to (-6 to -15),
        "GÜNÜN ÜCRETSİZ JOKERİ" to (-16 to -9),
        "FREE JOKER OF THE DAY" to (-16 to -9),
        "COMODÍN GRATIS DEL DÍA" to (-16 to -9),
        "CORINGA GRÁTIS DO DIA" to (-16 to -9),
        "Karalak Mağaza" to (0 to -40),
        "Karalak Store" to (0 to -40),
        "Karalak Tienda" to (0 to -40),
        "Karalak Loja" to (0 to -40),
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

    /** Hand-tuned curves, written back from the developer panel the same way as [DEFAULTS]. */
    private val BEND_DEFAULTS: Map<String, Int> = mapOf(
        "Arkadaşlarım" to 11,
        "Friends" to 11,
        "Amigos" to 11,
        "Arkadaşlarınla Yarış" to 30,
        "Race Your Friends" to 30,
        "Compite con tus Amigos" to 30,
        "Desafie seus Amigos" to 30,
        "Ayarlar" to 10,
        "Settings" to 10,
        "Ajustes" to 10,
        "Aylık Lig" to 13,
        "Monthly League" to 13,
        "Liga Mensual" to 13,
        "Liga Mensal" to 13,
        "Başarımlar" to 13,
        "Achievements" to 13,
        "Logros" to 13,
        "Conquistas" to 13,
        "GÜNÜN ÜCRETSİZ JOKERİ" to 18,
        "FREE JOKER OF THE DAY" to 18,
        "COMODÍN GRATIS DEL DÍA" to 18,
        "CORINGA GRÁTIS DO DIA" to 18
    )

    private const val PREFS = "title_nudge"
    private const val ON = "__on"

    var enabled by mutableStateOf(false)
        private set

    private val saved: SnapshotStateMap<String, Pair<Int, Int>> = mutableStateMapOf()
    /** How far a title is curved: hundredths of its size, positive arches it up (an oval top), negative drops the middle. */
    private val savedBend: SnapshotStateMap<String, Int> = mutableStateMapOf()
    private const val BEND = "#oval"
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
                if (k.endsWith(BEND)) { savedBend[k.removeSuffix(BEND)] = v.toIntOrNull() ?: 0; return@forEach }
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

    /** The key a title is tuned under: its words, whatever line breaks it is set with. */
    fun keyOf(text: String): String = text.replace('\n', ' ')

    fun offsetOf(key: String): Pair<Int, Int> = saved[key] ?: DEFAULTS[key] ?: (0 to 0)

    /** The curve of this title (see [savedBend]); 0 is a straight line. */
    fun bendOf(key: String): Int = savedBend[key] ?: BEND_DEFAULTS[key] ?: 0

    fun setBend(context: Context, key: String, bend: Int) {
        savedBend[key] = bend
        runCatching { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key + BEND, "$bend").apply() }
    }

    fun set(context: Context, key: String, dx: Int, dy: Int) {
        saved[key] = dx to dy
        runCatching { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, "$dx,$dy").apply() }
    }

    fun reset(context: Context, key: String) {
        saved.remove(key)
        savedBend.remove(key)
        runCatching { context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).remove(key + BEND).apply() }
    }

    internal fun show(key: String) { active.add(key) }
    internal fun hide(key: String) { active.remove(key) }

    /** Every changed title, one per line, ready to be pasted back. */
    fun report(): String =
        (saved.keys + savedBend.keys).distinct().sorted().mapNotNull { k ->
            val (x, y) = saved[k] ?: (0 to 0)
            val b = savedBend[k] ?: 0
            if (x == 0 && y == 0 && b == 0) null else "\"$k\" -> x $x, y $y, oval $b"
        }.joinToString("\n")

    internal fun visibleKeys(): List<String> = active.distinct()
}

/**
 * The tuning arrows, over the whole app; nothing at all while the mode is off. They live in a window of their own (a
 * dialog without a scrim or focus, touches outside the panel pass through) so that they are drawn above the app's own
 * windows too — a dialog is a window above the activity, and arrows drawn in the activity stayed hidden behind it. A
 * window opened later sits above an earlier one, so the arrows' window is made again whenever the set of titles on
 * screen changes: a dialog that has just opened registers its title, and the arrows come up over it.
 */
@Composable
fun TitleNudgeOverlay() {
    val context = LocalContext.current
    TitleNudge.load(context)
    if (!TitleNudge.enabled) return
    val keys = TitleNudge.visibleKeys()
    var picked by remember { mutableStateOf<String?>(null) }
    var step by remember { mutableStateOf(1) }
    var copied by remember { mutableStateOf(false) }
    var folded by remember { mutableStateOf(false) }
    var dragX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val key = picked?.takeIf { it in keys } ?: keys.firstOrNull()
    val clipboard = LocalClipboardManager.current
    androidx.compose.runtime.key(keys) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false
            )
        ) {
            val window = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            androidx.compose.runtime.SideEffect {
                window?.apply {
                    setDimAmount(0f)
                    setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL)
                    // no focus, and a touch outside the panel belongs to whatever is under it
                    addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                }
            }
            // dragging moves the panel's whole window (its own content would only be cut off at the window's edge)
            androidx.compose.runtime.SideEffect {
                window?.let { w -> w.attributes = w.attributes.also { a -> a.x = dragX.roundToInt(); a.y = (-dragY).roundToInt() } }
            }
            Box(Modifier.navigationBarsPadding().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
        // The panel can be dragged anywhere by its handle and folded down to the handle alone, so it never sits on the
        // very thing that needs tapping (a button at the foot of a page that opens the next page to tune).
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp)).background(Color(0xEE1E1E24)).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // the handle: drag it to move the panel, tap the arrow to fold it away or open it again
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF3B3B46))
                    .pointerInput(Unit) {
                        detectDragGestures { change, amount -> change.consume(); dragX += amount.x; dragY += amount.y }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("⠿ Sürükle", color = Color.White, fontSize = 12.sp)
                Text(
                    if (folded) "Aç ▲" else "Küçült ▼", color = Color(0xFFFFB347), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { folded = !folded }
                )
            }
            if (folded) return@Column
            if (key == null) {
                Text("Bu ekranda başlık yok", color = Color.White, fontSize = 13.sp)
            } else {
                val (dx, dy) = TitleNudge.offsetOf(key)
                val bend = TitleNudge.bendOf(key)
                Text("“$key”   x $dx · y $dy · oval $bend", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                // the curve: the middle of the title up (an arch, like a ribbon's top) or down
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Ovalleştir ∩ +", false) { TitleNudge.setBend(context, key, bend + step) }
                    Chip("∪ −", false) { TitleNudge.setBend(context, key, bend - step) }
                    Chip("Düzelt", bend == 0) { TitleNudge.setBend(context, key, 0) }
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
