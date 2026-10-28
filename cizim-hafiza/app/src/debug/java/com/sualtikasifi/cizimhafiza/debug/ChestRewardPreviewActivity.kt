package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.domain.model.ChestReward
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.domain.model.JokerType
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestOpeningDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: the chest-opening reward board on its own, so its layout can be looked at in the CI emulator
 * (a tap skips the video). Extras: tier = silver|gold|rare, extras = none|one|many.
 */
class ChestRewardPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tier = when (intent.getStringExtra("tier")) {
            "gold" -> ChestTier.GOLD
            "rare" -> ChestTier.RARE
            else -> ChestTier.SILVER
        }
        val reward = when (intent.getStringExtra("extras")) {
            "none" -> ChestReward(tier, gold = 214)
            "many" -> ChestReward(
                tier,
                gold = 1450,
                jokers = mapOf(JokerType.FIRST_LETTER to 2, JokerType.EXTRA_TIME to 1, JokerType.LETTER_COUNT to 1),
                penDrop = PenSkin.EMBER
            )
            else -> ChestReward(tier, gold = 802, jokers = mapOf(JokerType.LETTER_COUNT to 1))
        }
        setContent { CizimHafizaTheme { ChestOpeningDialog(reward = reward, onDismiss = { finish() }) } }
    }
}
