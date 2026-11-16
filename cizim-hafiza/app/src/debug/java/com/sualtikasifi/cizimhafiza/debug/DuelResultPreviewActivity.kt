package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.presentation.game.DuelChallengerSummary
import com.sualtikasifi.cizimhafiza.presentation.game.GamePhase
import com.sualtikasifi.cizimhafiza.presentation.game.ResultScreen
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import dagger.hilt.android.AndroidEntryPoint

/** Debug builds only: the result screen of an answered duel, with the challenger's ten made-up drawings ("Geri Meydan Oku" + their gallery). */
@AndroidEntryPoint
class DuelResultPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val words = listOf("Çakal", "Jokey", "Bankacı", "Soba", "Buzdolabı", "Çorbacı", "Bayrak", "Minnettar", "Boya paleti", "Marshmallow")
        fun scribble(seed: Int) = listOf(
            List(14) { DrawingPoint(0.15f + it * 0.05f, 0.5f + 0.3f * kotlin.math.sin((it + seed) * 0.7f)) },
            List(10) { DrawingPoint(0.2f + 0.04f * it * ((seed % 3) + 1) / 2f, 0.2f + it * 0.06f) }
        )
        val mine = words.mapIndexed { i, w -> ResultItem(w, true, scribble(i)) }
        val theirs = words.mapIndexed { i, w -> ResultItem(w, i % 4 != 3, scribble(i + 5)) }
        val state = GamePhase.Result(
            totalScore = 64, correctCount = 10, wrongCount = 0, fastestCorrectSeconds = 2.1,
            items = mine,
            duelChallenger = DuelChallengerSummary("d1", "u1", "liliput", 41, 7),
            xpEarned = 83, goldEarned = 170
        )
        setContent {
            CizimHafizaTheme {
                ResultScreen(
                    state = state,
                    ghostItems = theirs,
                    onPlayAgain = {}, onMainMenu = { finish() },
                    onRematchDuel = { _, _ -> },
                    levelProgress = LevelProgressState.forXp(300_000),
                    myName = "Sen"
                )
            }
        }
    }
}
