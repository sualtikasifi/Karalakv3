package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * Small glowing dots travelling round the outline of a pill-shaped element, as if sparks were circling it.
 *
 * [progress] is a looping 0..1 (one lap); the dots are spread evenly and move together, so the loop has no seam.
 * [outset] pushes the track out from the element's edge, [wobble] lets each dot drift in and out of that track,
 * and [colors] are handed out to the dots in turn.
 */
fun DrawScope.drawOrbitSparks(
    progress: Float,
    count: Int,
    colors: List<Color>,
    radius: Float,
    outset: Float = 0f,
    wobble: Float = 0f
) {
    if (count <= 0 || colors.isEmpty()) return
    val corner = size.height / 2f + outset
    val path = Path().apply {
        addRoundRect(RoundRect(-outset, -outset, size.width + outset, size.height + outset, CornerRadius(corner)))
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val length = measure.length
    if (length <= 0f) return
    for (i in 0 until count) {
        val t = (progress + i.toFloat() / count) % 1f
        val base = measure.getPosition(t * length)
        if (base == Offset.Unspecified) continue
        // Drift sideways off the track a little, and breathe in size, each dot on its own phase.
        val phase = (t * 2f + i * 0.37f) * 2f * PI.toFloat()
        val drift = wobble * sin(phase)
        val tangent = measure.getTangent(t * length)
        val centre = if (tangent == Offset.Unspecified) base else Offset(base.x + tangent.y * drift, base.y - tangent.x * drift)
        val size = radius * (0.75f + 0.25f * sin(phase + 1.3f))
        val colour = colors[i % colors.size]
        drawCircle(colour.copy(alpha = 0.28f), radius = size * 2.8f, center = centre)
        drawCircle(colour.copy(alpha = 0.9f), radius = size, center = centre)
        drawCircle(Color.White.copy(alpha = 0.8f), radius = size * 0.45f, center = centre)
    }
}
