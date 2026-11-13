package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.XpBoostOfferDialog
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.XpOfferStep
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the double-XP question on its own. Extra "step" is Ask or Started. */
class XpOfferPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val step = runCatching { XpOfferStep.valueOf(intent.getStringExtra("step") ?: "Ask") }.getOrDefault(XpOfferStep.Ask)
        setContent { CizimHafizaTheme { XpBoostOfferDialog(step = step, onWatch = {}, onSkip = { finish() }, onPlay = { finish() }) } }
    }
}
