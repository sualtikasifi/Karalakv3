package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.domain.model.PendingRun
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.reports.SwipeAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/** A decision made on the deck but not yet sent: it waits a moment so it can be taken back. */
private data class Staged(val run: PendingRun, val action: SwipeAction)

private const val UNDO_WINDOW_MS = 2600L

/**
 * The approval queue as a deck of cards: swipe right to let a round into the pool, left to turn
 * it down without a penalty, up for the penalty (XP back + a strike). Each decision waits
 * [UNDO_WINDOW_MS] before it is written, so a wrong swipe can be undone; the next swipe sends the
 * previous one at once. Tap a drawing to watch it on its own.
 */
@Composable
fun SwipeQueue(
    runs: List<PendingRun>,
    loading: Boolean,
    endReached: Boolean,
    failures: Int,
    failedIds: Set<String>,
    done: Int,
    onNeedMore: () -> Unit,
    onDecide: (PendingRun, SwipeAction) -> Unit,
    onPreview: (ResultItem) -> Unit,
    onRename: (PendingRun) -> Unit,
    onListMode: () -> Unit
) {
    var hidden by remember { mutableStateOf(setOf<String>()) }
    var staged by remember { mutableStateOf<Staged?>(null) }
    var sessionCount by remember { mutableStateOf(0) }
    val decide by rememberUpdatedState(onDecide)

    fun commit(s: Staged) = decide(s.run, s.action)

    fun stage(run: PendingRun, action: SwipeAction) {
        staged?.let { commit(it) }
        hidden = hidden + run.id
        staged = Staged(run, action)
        sessionCount++
    }

    // The staged decision is sent when its window runs out, or when the deck goes away.
    LaunchedEffect(staged) {
        val s = staged ?: return@LaunchedEffect
        delay(UNDO_WINDOW_MS)
        if (staged == s) { commit(s); staged = null }
    }
    DisposableEffect(Unit) { onDispose { staged?.let { commit(it) } } }
    // A refused write puts its card back on the deck (the banner above says what happened). Only
    // that card: the others still hidden are waiting to be sent or already on their way.
    LaunchedEffect(failures) { if (failures > 0) hidden = hidden - (failedIds - setOfNotNull(staged?.run?.id)) }

    val visible = runs.filterNot { it.id in hidden }
    LaunchedEffect(visible.size, loading, endReached) {
        if (visible.size <= 2 && !loading && !endReached) onNeedMore()
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Onay kuyruğu", style = Console.Title)
                Text(
                    if (endReached && visible.isEmpty()) "Kuyruk bitti" else "Bu oturumda $sessionCount karar · sağa onay · sola reddet · yukarı ceza",
                    style = Console.Small
                )
            }
            Surface(shape = RoundedCornerShape(10.dp), color = Console.Surface, border = BorderStroke(1.dp, Console.Border), modifier = Modifier.clickable(onClick = onListMode)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Console.InkSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Liste", style = Console.Label)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                visible.isEmpty() && (loading || !endReached) -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Console.Accent)
                visible.isEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Console.GoodSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Console.GoodText, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Hepsi incelendi", style = Console.Title)
                    Text("Yeni turlar geldikçe burada görünecek.", style = Console.Small)
                }
                else -> {
                    // the next card peeks out behind, so the deck reads as a deck
                    visible.getOrNull(1)?.let { next ->
                        Box(Modifier.fillMaxSize().padding(top = 10.dp).graphicsLayer { scaleX = 0.95f; scaleY = 0.95f; alpha = 0.7f }) {
                            RunCard(next, interactive = false, onPreview = {}, onRename = {})
                        }
                    }
                    androidx.compose.runtime.key(visible.first().id) {
                        SwipeCard(
                            run = visible.first(),
                            onSwiped = { action -> stage(visible.first(), action) },
                            onPreview = onPreview,
                            onRename = { onRename(visible.first()) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val top = visible.firstOrNull()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            DeckButton(Icons.Filled.Close, "Reddet", Console.InkSecondary, Console.Surface, Modifier.weight(1f), enabled = top != null) { top?.let { stage(it, SwipeAction.Dismiss) } }
            DeckButton(Icons.Filled.Block, "Cezalı ret", Console.Critical, Console.CriticalSoft, Modifier.weight(1f), enabled = top != null) { top?.let { stage(it, SwipeAction.Reject) } }
            DeckButton(Icons.Filled.Check, "Onayla", Color.White, Console.Good, Modifier.weight(1.2f), enabled = top != null) { top?.let { stage(it, SwipeAction.Approve) } }
        }
        val s = staged
        if (s != null) {
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Console.Dark, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${s.run.nickname}: " + when (s.action) {
                            SwipeAction.Approve -> "onaylanıyor"
                            SwipeAction.Dismiss -> "reddediliyor"
                            SwipeAction.Reject -> "cezalı ret (−${s.run.xpEarned} XP)"
                        },
                        style = Console.Small.copy(color = Color.White),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { hidden = hidden - s.run.id; staged = null; sessionCount-- }.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Undo, contentDescription = null, tint = Console.Warning, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Geri al", style = Console.Label.copy(color = Console.Warning, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeCard(run: PendingRun, onSwiped: (SwipeAction) -> Unit, onPreview: (ResultItem) -> Unit, onRename: () -> Unit) {
    val scope = rememberCoroutineScope()
    val dx = remember { Animatable(0f) }
    val dy = remember { Animatable(0f) }
    val density = LocalDensity.current
    val thresholdX = with(density) { 110.dp.toPx() }
    val thresholdY = with(density) { 140.dp.toPx() }
    val fling = with(density) { 900.dp.toPx() }

    fun fly(action: SwipeAction) {
        scope.launch {
            when (action) {
                SwipeAction.Approve -> dx.animateTo(fling, tween(220))
                SwipeAction.Dismiss -> dx.animateTo(-fling, tween(220))
                SwipeAction.Reject -> dy.animateTo(-fling, tween(220))
            }
            onSwiped(action)
        }
    }

    val approveP = (dx.value / thresholdX).coerceIn(0f, 1f)
    val dismissP = (-dx.value / thresholdX).coerceIn(0f, 1f)
    val rejectP = if (abs(dy.value) > abs(dx.value)) (-dy.value / thresholdY).coerceIn(0f, 1f) else 0f

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = dx.value
                translationY = dy.value
                rotationZ = dx.value / 40f
            }
            .pointerInput(run.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            -dy.value > thresholdY && abs(dy.value) > abs(dx.value) -> fly(SwipeAction.Reject)
                            dx.value > thresholdX -> fly(SwipeAction.Approve)
                            dx.value < -thresholdX -> fly(SwipeAction.Dismiss)
                            else -> scope.launch {
                                launch { dx.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                dy.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        }
                    },
                    onDragCancel = { scope.launch { dx.animateTo(0f); dy.animateTo(0f) } }
                ) { change, drag ->
                    change.consume()
                    scope.launch { dx.snapTo(dx.value + drag.x); dy.snapTo(dy.value + drag.y) }
                }
            }
    ) {
        RunCard(run, interactive = true, onPreview = onPreview, onRename = onRename)
        Stamp("ONAYLA", Console.Good, approveP, Alignment.TopStart, -12f)
        Stamp("REDDET", Console.InkSecondary, dismissP, Alignment.TopEnd, 12f)
        Stamp("CEZALI RET", Console.Critical, rejectP, Alignment.Center, 0f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Stamp(text: String, color: Color, progress: Float, align: Alignment, angle: Float) {
    if (progress <= 0.02f) return
    Text(
        text,
        style = Console.Title.copy(color = color, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
        modifier = Modifier
            .align(align)
            .padding(24.dp)
            .graphicsLayer { alpha = progress; rotationZ = angle }
            .border(3.dp, color, RoundedCornerShape(8.dp))
            .background(Console.Surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/** One round on a card: who, how it went, and the ten drawings big enough to judge. */
@Composable
private fun RunCard(run: PendingRun, interactive: Boolean, onPreview: (ResultItem) -> Unit, onRename: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Console.Surface, border = BorderStroke(1.dp, Console.Border), shadowElevation = 6.dp, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = if (interactive) Modifier.clickable(onClick = onRename) else Modifier) {
                        Text(run.nickname, style = Console.Title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (interactive) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.Edit, contentDescription = "Adı değiştir", tint = Console.InkMuted, modifier = Modifier.size(15.dp))
                        }
                    }
                    Text(ago(run.createdAtMillis), style = Console.Small)
                }
                Tag("Sv. ${run.level}")
                Spacer(Modifier.width(6.dp))
                Tag("${run.correctCount}/${run.items.size} doğru", fg = Console.GoodText, bg = Console.GoodSoft)
                Spacer(Modifier.width(6.dp))
                Tag("+${run.xpEarned} XP", fg = Console.Accent, bg = Console.AccentSoft)
            }
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val cols = 3
                val rows = (run.items.size + cols - 1) / cols
                val gap = 8.dp
                val labelH = 16.dp
                val cell = min(
                    ((maxWidth - gap * (cols - 1)) / cols).value,
                    ((maxHeight - gap * (rows - 1)) / rows - labelH).value
                ).dp
                Column(verticalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.align(Alignment.TopCenter)) {
                    run.items.chunked(cols).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            line.forEach { item ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(cell)) {
                                    StrokeCanvas(
                                        strokes = item.strokes,
                                        modifier = Modifier
                                            .size(cell)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .border(1.dp, if (item.isCorrect) Console.Border else Console.CriticalSoft, RoundedCornerShape(10.dp))
                                            .then(if (interactive) Modifier.clickable { onPreview(item) } else Modifier)
                                    )
                                    Text(
                                        item.word,
                                        style = Console.Tick.copy(color = if (item.isCorrect) Console.InkSecondary else Console.Critical, fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.height(labelH)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, fg: Color, bg: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = if (bg == Console.Surface) BorderStroke(1.dp, Console.Border) else null,
        modifier = modifier.height(52.dp).graphicsLayer { alpha = if (enabled) 1f else 0.4f }.clip(RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = Console.Subtitle.copy(color = fg))
        }
    }
}

private fun ago(millis: Long): String {
    if (millis <= 0L) return ""
    val m = (System.currentTimeMillis() - millis) / 60_000
    return when {
        m < 1 -> "az önce"
        m < 60 -> "$m dk önce"
        m < 60 * 24 -> "${m / 60} sa önce"
        else -> "${m / (60 * 24)} gün önce"
    }
}
