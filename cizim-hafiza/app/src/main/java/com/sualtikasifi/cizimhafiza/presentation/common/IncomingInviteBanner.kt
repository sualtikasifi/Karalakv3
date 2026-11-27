package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.MatchInvite

/**
 * App-wide "X invited you!" banner — shown floating above whatever screen is
 * currently on top (see its usage in CizimHafizaNavGraph), so an invite is
 * never missed just because the player isn't on the Friends screen.
 *
 * It is one painted picture (invite_banner, 1100 x 507 px: the mascot with a brush, a parchment for the words and two
 * empty pills, green and red); the invite's words and the two labels are lettered live on it, in the picture's own
 * proportions, and the two pills are the buttons.
 */
@Composable
fun IncomingInviteBanner(
    invite: MatchInvite?,
    isResponding: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = invite != null,
        enter = slideInVertically(animationSpec = tween(280)) { -it } + fadeIn(tween(280)),
        exit = slideOutVertically(animationSpec = tween(220)) { -it } + fadeOut(tween(220)),
        modifier = modifier.statusBarsPadding().padding(top = 8.dp)
    ) {
        if (invite != null) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 380.dp)
                    .aspectRatio(BannerW / BannerH)
                    .softShadowBanner()
            ) {
                val w = maxWidth
                val h = maxHeight
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.invite_banner),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                // the words, on the parchment to the right of the brush
                FitText(
                    text = stringResource(R.string.friends_incoming_invite, invite.fromNickname),
                    style = PaintedStyle(
                        color = Color(0xFF3B2314), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center, lineHeight = 22.sp
                    ),
                    maxLines = 3,
                    minScale = 0.6f,
                    modifier = Modifier.offset(w * 0.50f, h * 0.15f).size(w * 0.41f, h * 0.36f)
                )
                if (isResponding) {
                    // the two pills give way to a spinner while the answer is on its way
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.offset(w * 0.28f, h * 0.63f).size(w * 0.62f, h * 0.25f)
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
                    }
                } else {
                    BannerButton(stringResource(R.string.friends_accept_invite), Color(0xFF0F5B07), onAccept, Modifier.offset(w * 0.285f, h * 0.645f).size(w * 0.285f, h * 0.225f))
                    BannerButton(stringResource(R.string.friends_decline_invite), Color(0xFF7A1410), onDecline, Modifier.offset(w * 0.595f, h * 0.645f).size(w * 0.30f, h * 0.225f))
                }
            }
        }
    }
}

/** One of the picture's empty pills, with its label lettered on it. */
@Composable
private fun BannerButton(text: String, outline: Color, onClick: () -> Unit, modifier: Modifier) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.pressable(pressedScale = 0.93f, onClick = onClick)
    ) {
        LetteredText(
            text = text,
            size = 19.sp,
            fill = Color.White,
            outline = outline,
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            minScale = 0.6f
        )
    }
}

private const val BannerW = 1100f
private const val BannerH = 507f

/** A faint shadow under the banner so it lifts off the screen behind it. */
private fun Modifier.softShadowBanner(): Modifier = this.shadow(
    elevation = 8.dp,
    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    clip = false,
    ambientColor = Color(0x33000000),
    spotColor = Color(0x59000000)
)
