package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.breathing
import com.sualtikasifi.cizimhafiza.presentation.game.ArtButton
import com.sualtikasifi.cizimhafiza.presentation.game.DoubleXpFace
import com.sualtikasifi.cizimhafiza.presentation.game.ResArtW
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the result screen's bottom row, the "x2 XP" ad button beside "Ödülü Al" (`--ei xp 171`, default 171). */
class DoubleXpPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val xp = intent.getIntExtra("xp", 171)
        setContent {
            CizimHafizaTheme {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val u = maxWidth / ResArtW
                    val fontScale = LocalDensity.current.fontScale
                    fun fs(art: Float) = (art * u.value / fontScale).sp
                    Image(painterResource(R.drawable.du_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().padding(horizontal = u * 40f), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(u * 14f), modifier = Modifier.fillMaxWidth()) {
                            ArtButton(res = R.drawable.rs_btn_claim, u = u, iconRoom = 0f, onClick = {}, modifier = Modifier.weight(1f).breathing(0.022f, 1400)) {
                                DoubleXpFace(amount = xp * 2, u = u, fs = ::fs)
                            }
                            ArtButton(res = R.drawable.rs_btn_claim, u = u, iconRoom = 0f, onClick = {}, modifier = Modifier.weight(1f)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    LetteredText("Ödülü Al · $xp XP", fs(38f), outline = Color(0xFF8A3A00), maxLines = 1, minScale = 0.5f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
