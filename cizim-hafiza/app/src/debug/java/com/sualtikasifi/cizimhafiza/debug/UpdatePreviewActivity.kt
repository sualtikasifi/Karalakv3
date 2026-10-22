package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.presentation.common.ForcedUpdateScreen
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: shows the forced-update card on its own, so its layout can be looked at in the CI emulator. */
class UpdatePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CizimHafizaTheme {
                ForcedUpdateScreen(
                    title = "Yeni sürüm hazır",
                    message = "Oynamaya devam etmek için Karalak'ı güncelle.",
                    buttonText = "Güncelle",
                    onUpdate = {},
                    onExit = { finish() }
                )
            }
        }
    }
}
