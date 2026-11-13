package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle

/** Where the double-XP question is: asking, or told that the 30 minutes have started. */
enum class XpOfferStep { Ask, Started }

private val OfferInk = Color(0xFF3B2314)

/**
 * The question asked on a tap on "Hızlı Eşleş": watch an ad for 30 minutes of double XP, or not now (the match then simply
 * starts). Once the ad is watched the same window says the 30 minutes have begun, with one button into the match.
 */
@Composable
fun XpBoostOfferDialog(step: XpOfferStep, onWatch: () -> Unit, onSkip: () -> Unit, onPlay: () -> Unit) {
    PaintedDialog(
        title = stringResource(if (step == XpOfferStep.Ask) R.string.xp_offer_title else R.string.xp_offer_started_title),
        onDismiss = if (step == XpOfferStep.Ask) onSkip else onPlay,
        buttons = {
            if (step == XpOfferStep.Ask) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    PaintedPillButton(
                        text = stringResource(R.string.xp_offer_watch),
                        onClick = onWatch,
                        height = 56.dp,
                        textSize = 22.sp,
                        attention = true,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                    Spacer(Modifier.height(8.dp))
                    PaintedPillButton(
                        text = stringResource(R.string.xp_offer_skip),
                        onClick = onSkip,
                        primary = false,
                        height = 44.dp,
                        textSize = 17.sp,
                        modifier = Modifier.fillMaxWidth(0.62f)
                    )
                }
            } else {
                PaintedPillButton(
                    text = stringResource(R.string.xp_offer_play),
                    onClick = onPlay,
                    height = 56.dp,
                    textSize = 22.sp,
                    attention = true,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            }
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            val shape = RoundedCornerShape(50)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE566), Color(0xFFFFB300))), shape)
                    .border(3.dp, Color(0xFFB36B00), shape)
                    .padding(horizontal = 26.dp, vertical = 6.dp)
            ) {
                LetteredText(
                    text = stringResource(R.string.xp_offer_badge),
                    size = 34.sp,
                    outline = Color(0xFF8A3A00),
                    modifier = Modifier.height(44.dp),
                    minScale = 0.6f
                )
            }
            Spacer(Modifier.height(12.dp))
            FitText(
                text = stringResource(if (step == XpOfferStep.Ask) R.string.xp_offer_body else R.string.xp_offer_started_body),
                style = PaintedStyle(color = OfferInk, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 24.sp),
                maxLines = 4,
                minScale = 0.7f,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        }
    }
}
