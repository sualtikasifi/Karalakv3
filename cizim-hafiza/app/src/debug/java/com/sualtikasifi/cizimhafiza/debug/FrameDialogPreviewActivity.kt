package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedFrameDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the vine-and-parchment window with its painted button, filled with the Runes welcome text. */
class FrameDialogPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CizimHafizaTheme {
                Image(painterResource(R.drawable.rune_screen_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                PaintedFrameDialog(
                    title = stringResource(R.string.rune_intro_title),
                    body = stringResource(R.string.rune_intro_body),
                    buttonText = stringResource(R.string.rune_intro_ok),
                    onClick = { finish() }
                )
            }
        }
    }
}
