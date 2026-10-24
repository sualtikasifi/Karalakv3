package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.game.ResultWordsBoard
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: the end-of-game drawings board with the SAME made-up drawing in different pens (the thumbnails of
 * the board, and the same drawing big underneath), so a pen that does not show in the thumbnails can be seen in the CI
 * emulator. Extras: first = index of the first pen shown (the board shows 10 of them).
 */
class PenThumbsPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val first = intent.getIntExtra("first", 0)
        val pens = PenSkin.entries.drop(first).take(10)
        val loop = List(90) { i ->
            val a = i / 90f * 6.283f * 2.2f
            DrawingPoint(200f + 150f * kotlin.math.cos(a) * (0.35f + i / 140f), 200f + 150f * kotlin.math.sin(a) * (0.35f + i / 140f))
        }
        val wave = List(60) { i -> DrawingPoint(40f + i * 5f, 200f + 70f * kotlin.math.sin(i / 6f)) }
        val items = pens.mapIndexed { i, pen -> ResultItem("Çizim ${i + 1}", true, listOf(loop, wave), pen.name) }
        setContent {
            CizimHafizaTheme {
                Column(Modifier.fillMaxSize().padding(top = 28.dp).background(Color(0xFF6B3F1E))) {
                    ResultWordsBoard(
                        title = "Pens", items = items, placeholders = 10, wordLanguage = "tr",
                        onPreview = {}, onShareAll = null, modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth().padding(8.dp)) {
                        items.take(5).forEach { item ->
                            Box(Modifier.size(70.dp).background(Color.White)) {
                                StrokeCanvas(strokes = item.strokes, modifier = Modifier.fillMaxSize(), penSkin = item.pen(), animated = false)
                            }
                        }
                    }
                }
            }
        }
    }
}
