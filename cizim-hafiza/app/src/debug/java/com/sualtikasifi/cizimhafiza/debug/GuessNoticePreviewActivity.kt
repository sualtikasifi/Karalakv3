package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessNoticeScreen
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessProblem
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: the Guess It notice page (own drawing, dead link, offline, turned off) on its own, so its layout can
 * be looked at in the CI emulator without a link. The "problem" extra is one of NotFound, Offline, Child, Own.
 */
class GuessNoticePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val problem = runCatching { GuessProblem.valueOf(intent.getStringExtra("problem") ?: "Own") }.getOrDefault(GuessProblem.Own)
        setContent { CizimHafizaTheme { GuessNoticeScreen(problem = problem, onRetry = {}, onClose = { finish() }, onBack = { finish() }) } }
    }
}
