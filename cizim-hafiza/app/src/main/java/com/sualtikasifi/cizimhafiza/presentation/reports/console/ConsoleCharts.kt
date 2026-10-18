package com.sualtikasifi.cizimhafiza.presentation.reports.console

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** One point on a time chart: a short x label ("17 Eki") and its value. */
data class ChartPoint(val label: String, val value: Double)

/** A clean upper bound and step for a y axis that starts at zero (1/2/5 × 10ⁿ steps, three or four ticks). */
private fun niceScale(maxValue: Double): Pair<Double, Double> {
    if (maxValue <= 0.0) return 1.0 to 1.0
    val rough = maxValue / 3.0
    val mag = 10.0.pow(floor(log10(rough)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * mag }.first { it >= rough }
    return ceil(maxValue / step) * step to step
}

/** De-emphasised trend line for stat tiles: no axes, the last point marked. */
@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxV = max(values.max(), 1e-6f)
        val minV = minOf(values.min(), 0f)
        val span = max(maxV - minV, 1e-6f)
        val stepX = size.width / (values.size - 1)
        fun y(v: Float) = size.height - 3.dp.toPx() - (v - minV) / span * (size.height - 6.dp.toPx())
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * stepX, y(v)) }
        drawPath(path, color.copy(alpha = 0.55f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = Offset(size.width, y(values.last()))
        drawCircle(Console.Surface, radius = 4.dp.toPx(), center = last)
        drawCircle(color, radius = 2.5.dp.toPx(), center = last)
    }
}

private fun DrawScope.yAxis(tm: TextMeasurer, top: Float, plotH: Float, left: Float, right: Float, maxY: Double, step: Double, format: (Double) -> String) {
    var v = 0.0
    while (v <= maxY + 1e-9) {
        val y = top + plotH - (v / maxY * plotH).toFloat()
        drawLine(Console.Grid, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
        val t = tm.measure(format(v), Console.Tick)
        drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), y - t.size.height / 2f))
        v += step
    }
}

private fun DrawScope.xLabels(tm: TextMeasurer, labels: List<String>, xFor: (Int) -> Float, baseY: Float) {
    if (labels.isEmpty()) return
    // first, middle and last only: enough to place the series in time, never a crowded row
    val idx = listOf(0, labels.size / 2, labels.size - 1).distinct()
    idx.forEach { i ->
        val t = tm.measure(labels[i], Console.Tick)
        val x = (xFor(i) - t.size.width / 2f).coerceIn(xFor(0) - 2.dp.toPx(), size.width - t.size.width.toFloat())
        drawText(t, topLeft = Offset(x, baseY + 6.dp.toPx()))
    }
}

private fun DrawScope.tooltip(tm: TextMeasurer, title: String, value: String, anchorX: Float, top: Float) {
    val a = tm.measure(title, Console.Tick)
    val b = tm.measure(value, TextStyle(fontSize = Console.Subtitle.fontSize, fontWeight = FontWeight.SemiBold, color = Color.White))
    val pad = 8.dp.toPx()
    val w = max(a.size.width, b.size.width) + pad * 2
    val h = a.size.height + b.size.height + pad * 1.6f
    val x = (anchorX - w / 2f).coerceIn(0f, size.width - w)
    drawRoundRect(Console.Dark, topLeft = Offset(x, top), size = Size(w, h), cornerRadius = CornerRadius(8.dp.toPx()))
    drawText(a.let { tm.measure(title, Console.Tick.copy(color = Color(0xFFCFC8BF))) }, topLeft = Offset(x + pad, top + pad * 0.6f))
    drawText(b, topLeft = Offset(x + pad, top + pad * 0.6f + a.size.height))
}

/**
 * A single-series line over days: 2px line, a 10% wash under it, the latest value marked and
 * labelled. Touch (and drag) to read any day; the reading stays until the next touch.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    color: Color = Console.Accent,
    height: androidx.compose.ui.unit.Dp = 180.dp,
    format: (Double) -> String = { Console.compact(it) }
) {
    val tm = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val leftPad = 34.dp
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points) {
                val left = leftPad.toPx()
                fun pick(x: Float): Int {
                    if (points.size < 2) return 0
                    val step = (size.width - left) / (points.size - 1)
                    return ((x - left) / step).roundToInt().coerceIn(0, points.size - 1)
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    selected = pick(down.position.x)
                    do {
                        val ev = awaitPointerEvent()
                        ev.changes.firstOrNull()?.let { if (it.pressed) selected = pick(it.position.x) }
                    } while (ev.changes.any { it.pressed })
                }
            }
    ) {
        if (points.isEmpty()) return@Canvas
        val left = leftPad.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val plotH = bottom - top
        val (maxY, step) = niceScale(points.maxOf { it.value })
        yAxis(tm, top, plotH, left, size.width, maxY, step) { Console.compact(it) }
        val stepX = if (points.size > 1) (size.width - left) / (points.size - 1) else 0f
        fun x(i: Int) = left + i * stepX
        fun y(v: Double) = top + plotH - (v / maxY * plotH).toFloat()
        val line = Path()
        points.forEachIndexed { i, p -> if (i == 0) line.moveTo(x(i), y(p.value)) else line.lineTo(x(i), y(p.value)) }
        val area = Path().apply {
            addPath(line)
            lineTo(x(points.size - 1), bottom)
            lineTo(x(0), bottom)
            close()
        }
        drawPath(area, color.copy(alpha = 0.10f))
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        xLabels(tm, points.map { it.label }, ::x, bottom)
        val sel = selected
        if (sel != null) {
            val p = points[sel]
            drawLine(Console.InkMuted.copy(alpha = 0.6f), Offset(x(sel), top), Offset(x(sel), bottom), strokeWidth = 1.dp.toPx())
            drawCircle(Console.Surface, 6.dp.toPx(), Offset(x(sel), y(p.value)))
            drawCircle(color, 4.dp.toPx(), Offset(x(sel), y(p.value)))
            tooltip(tm, p.label, format(p.value), x(sel), top)
        } else {
            val i = points.size - 1
            drawCircle(Console.Surface, 6.dp.toPx(), Offset(x(i), y(points[i].value)))
            drawCircle(color, 4.dp.toPx(), Offset(x(i), y(points[i].value)))
        }
    }
}

/** Daily columns from one baseline: ≤ 24dp wide, 4dp rounded top, square at the baseline. Touch to read a day. */
@Composable
fun ColumnChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    color: Color = Console.Blue,
    height: androidx.compose.ui.unit.Dp = 160.dp,
    format: (Double) -> String = { Console.compact(it) }
) {
    val tm = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val leftPad = 34.dp
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points) {
                val left = leftPad.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun pick(x: Float) = if (points.isEmpty()) null else (((x - left) / ((size.width - left) / points.size)).toInt()).coerceIn(0, points.size - 1)
                    selected = pick(down.position.x)
                    do {
                        val ev = awaitPointerEvent()
                        ev.changes.firstOrNull()?.let { if (it.pressed) selected = pick(it.position.x) }
                    } while (ev.changes.any { it.pressed })
                }
            }
    ) {
        if (points.isEmpty()) return@Canvas
        val left = leftPad.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val plotH = bottom - top
        val (maxY, step) = niceScale(points.maxOf { it.value })
        yAxis(tm, top, plotH, left, size.width, maxY, step) { Console.compact(it) }
        val slot = (size.width - left) / points.size
        val gap = 2.dp.toPx()
        val w = minOf(24.dp.toPx(), slot - gap)
        val r = minOf(4.dp.toPx(), w / 2f)
        points.forEachIndexed { i, p ->
            val h = (p.value / maxY * plotH).toFloat()
            if (h <= 0f) return@forEachIndexed
            val x0 = left + i * slot + (slot - w) / 2f
            val c = if (selected == null || selected == i) color else color.copy(alpha = 0.45f)
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = x0, top = bottom - h, right = x0 + w, bottom = bottom,
                        topLeftCornerRadius = CornerRadius(r), topRightCornerRadius = CornerRadius(r),
                        bottomLeftCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero
                    )
                )
            }
            drawPath(path, c)
        }
        xLabels(tm, points.map { it.label }, { i -> left + i * slot + slot / 2f }, bottom)
        selected?.let { i -> tooltip(tm, points[i].label, format(points[i].value), left + i * slot + slot / 2f, top) }
    }
}

/** One placement's funnel: how many asked for an ad and how many got paid out. */
data class FunnelRow(val label: String, val requested: Long, val earned: Long, val note: String? = null)

/**
 * Requested vs earned per ad button, as paired horizontal bars sharing one scale. Values sit at the
 * bar ends in text ink; the legend names the two series.
 */
@Composable
fun FunnelBars(rows: List<FunnelRow>, modifier: Modifier = Modifier, firstLabel: String, secondLabel: String) {
    val maxV = max(1L, rows.maxOfOrNull { max(it.requested, it.earned) } ?: 1L).toFloat()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendKey(Console.Blue, firstLabel)
            Spacer(Modifier.width(14.dp))
            LegendKey(Console.Accent, secondLabel)
        }
        rows.forEach { row ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.label, style = Console.Subtitle.copy(fontSize = Console.Small.fontSize), modifier = Modifier.weight(1f))
                    if (row.note != null) Text(row.note, style = Console.Small)
                }
                Spacer(Modifier.height(4.dp))
                Bar(row.requested / maxV, Console.Blue, Console.compact(row.requested.toDouble()))
                Spacer(Modifier.height(2.dp))
                Bar(row.earned / maxV, Console.Accent, Console.compact(row.earned.toDouble()))
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).height(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.004f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(value, style = Console.Small.copy(color = Console.Ink), modifier = Modifier.width(44.dp))
    }
}

@Composable
fun LegendKey(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Console.Small)
    }
}

/** A share of a whole as a slim meter (e.g. reward ads that paid out). */
@Composable
fun Meter(fraction: Float, modifier: Modifier = Modifier, color: Color = Console.Accent) {
    Box(modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.16f))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}
