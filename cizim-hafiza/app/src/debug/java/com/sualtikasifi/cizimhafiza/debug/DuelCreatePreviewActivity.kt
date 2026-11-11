package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.presentation.duel.CreateDuelScreen
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: the challenge page (CreateDuelScreen) on its own, so its layout can be looked at in the CI
 * emulator without a friend to challenge. The opponent's name is the "name" extra (a long one tests the fit).
 */
class DuelCreatePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("name") ?: "Bob Ross"
        setContent { CizimHafizaTheme { CreateDuelScreen(opponentName = name, onBack = { finish() }, onChallengeStarted = { finish() }) } }
    }
}
