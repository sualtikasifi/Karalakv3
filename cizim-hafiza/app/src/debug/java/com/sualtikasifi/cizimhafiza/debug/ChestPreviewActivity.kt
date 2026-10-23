package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestDetailDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: shows the chest window on its own so its layout can be looked at in the CI emulator.
 * Extras: tier = silver|gold|rare, state = start|unlocking|ready|busy|used.
 */
class ChestPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tier = when (intent.getStringExtra("tier")) {
            "gold" -> ChestTier.GOLD
            "rare" -> ChestTier.RARE
            else -> ChestTier.SILVER
        }
        val state = intent.getStringExtra("state") ?: "start"
        val now = System.currentTimeMillis()
        val chest = when (state) {
            "unlocking", "used" -> Chest("x", tier, unlockStartedAtMillis = now - tier.unlockDurationMillis * 4 / 10)
            "ready" -> Chest("x", tier, unlockStartedAtMillis = now - tier.unlockDurationMillis - 1000)
            else -> Chest("x", tier)
        }
        setContent {
            CizimHafizaTheme {
                ChestDetailDialog(
                    chest = chest,
                    nowMillis = now,
                    anotherUnlocking = state == "busy",
                    speedupAvailable = state == "unlocking",
                    instantAvailable = state == "unlocking",
                    bubble = null,
                    onBubbleDone = {},
                    onTapChest = {},
                    onStart = {},
                    onOpen = {},
                    onSpeedup = {},
                    onInstant = {},
                    onDismiss = { finish() }
                )
            }
        }
    }
}
