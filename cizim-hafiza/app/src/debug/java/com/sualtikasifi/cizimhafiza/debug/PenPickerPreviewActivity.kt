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
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.PenPickerSheet
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.PenSkinUiItem
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the pen picker window with every pen, the first one worn and every fourth one still locked. */
class PenPickerPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val items = PenSkin.entries.mapIndexed { i, s -> PenSkinUiItem(s, unlocked = i % 4 != 3, selected = i == 0) }
        setContent {
            CizimHafizaTheme {
                Image(painterResource(R.drawable.du_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                PenPickerSheet(items = items, onSelect = {}, onDismiss = { finish() })
            }
        }
    }
}
