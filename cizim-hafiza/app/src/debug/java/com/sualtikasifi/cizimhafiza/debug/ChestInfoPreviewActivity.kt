package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.ChestInfoDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the painted "How do chests work?" guide over the home background. */
class ChestInfoPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CizimHafizaTheme {
                Image(painterResource(R.drawable.du_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                ChestInfoDialog(onDismiss = { finish() })
            }
        }
    }
}
