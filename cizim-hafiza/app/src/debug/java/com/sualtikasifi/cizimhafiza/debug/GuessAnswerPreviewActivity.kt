package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.domain.model.GuessLink
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessAnswerScreen
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessPhase
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessUiState
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: the Guess It answer page on its own, with a sample drawing, so its layout can be looked at in the CI
 * emulator without a link. Extras: "attempts" (0..2, how many guesses have missed) and "typed" (text already in the field).
 */
class GuessAnswerPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attempts = intent.getIntExtra("attempts", 0)
        val typed = intent.getStringExtra("typed").orEmpty()
        val sun = (0..40).map { i ->
            val a = i / 40f * 6.2832f
            DrawingPoint(0.5f + 0.28f * kotlin.math.cos(a), 0.5f + 0.28f * kotlin.math.sin(a))
        }
        val strokes = listOf(sun, listOf(DrawingPoint(0.5f, 0.1f), DrawingPoint(0.5f, 0.9f)), listOf(DrawingPoint(0.1f, 0.5f), DrawingPoint(0.9f, 0.5f)))
        val link = GuessLink("preview", "u", "Bob Ross", "tr", "s", "h", "3 4", "a", strokes, 10, 0L, 0L)
        setContent {
            CizimHafizaTheme {
                var state by remember {
                    mutableStateOf(
                        GuessUiState(
                            phase = GuessPhase.Guessing, link = link, replayDone = true, input = typed, attempts = attempts,
                            wrong = List(attempts) { "yanlış${it + 1}" }
                        )
                    )
                }
                GuessAnswerScreen(
                    state = state,
                    onReplay = {}, onReplayFinished = {},
                    onInput = { state = state.copy(input = it) },
                    onSubmit = {}, onGiveUp = {}, onBack = { finish() }
                )
            }
        }
    }
}
