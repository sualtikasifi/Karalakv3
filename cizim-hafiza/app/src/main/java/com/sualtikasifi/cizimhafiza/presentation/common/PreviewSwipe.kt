package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Flicking left or right across the drawing preview steps to the next or previous drawing: a drag to the right
 * goes back, a drag to the left goes forward. A null side is the end of the list and ignores that direction.
 */
fun Modifier.previewSwipe(onPrevious: (() -> Unit)?, onNext: (() -> Unit)?): Modifier =
    pointerInput(onPrevious, onNext) {
        var travelled = 0f
        detectHorizontalDragGestures(
            onDragStart = { travelled = 0f },
            onDragEnd = {
                if (travelled > SWIPE_THRESHOLD_PX) onPrevious?.invoke()
                else if (travelled < -SWIPE_THRESHOLD_PX) onNext?.invoke()
            },
            onHorizontalDrag = { _, delta -> travelled += delta }
        )
    }

private const val SWIPE_THRESHOLD_PX = 120f
