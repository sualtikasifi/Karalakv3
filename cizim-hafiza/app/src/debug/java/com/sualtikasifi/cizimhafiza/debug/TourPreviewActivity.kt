package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.FeatureTourDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: shows the "what is new" tour on its own so its layout can be looked at in the CI emulator. */
class TourPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CizimHafizaTheme { FeatureTourDialog(onFinish = { finish() }) } }
    }
}
