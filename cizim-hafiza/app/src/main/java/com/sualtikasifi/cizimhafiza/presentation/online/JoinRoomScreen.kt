package com.sualtikasifi.cizimhafiza.presentation.online

import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.util.asString

// bg_join is this size; its hanging sign is painted on it, so the title is placed by fractions of the picture.
private const val ArtW = 841f
private const val ArtH = 1870f
private val Ink = Color(0xFF5A321F)

// The form panel (join_panel) has its two input wells painted in. These are where they sit, as fractions of the picture.
private const val PanelAspect = 1936f / 1336f
private const val Well1Top = 0.2806f
private const val Well2Top = 0.5672f
private const val WellHeight = 0.172f
private const val WellLeft = 0.1269f
private const val WellWidth = 0.7327f
private const val LabelLeft = 0.215f

/** Joining a friend's room: the workshop scene, a painted form panel with the name and code typed into its wells. */
@Composable
fun JoinRoomScreen(
    onBack: () -> Unit,
    onJoined: (roomCode: String) -> Unit,
    viewModel: JoinRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val s = maxOf(widthPx / ArtW, heightPx / ArtH)
        val offX = (widthPx - ArtW * s) / 2f
        val offY = 0f
        fun yOf(fraction: Float): Dp = with(density) { (offY + ArtH * s * fraction).toDp() }
        fun len(artPx: Float): Dp = with(density) { (artPx * s).toDp() }

        Image(
            painter = cachedPainterResource(R.drawable.bg_join),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // Title on the sign.
        val signCentreX = with(density) { (offX + ArtW * s * 0.503f).toDp() }
        Box(
            modifier = Modifier.offset(x = signCentreX - len(230f), y = yOf(0.197f) - len(38f)).width(len(460f)),
            contentAlignment = Alignment.Center
        ) {
            LetteredText(stringResource(R.string.online_join_room), with(density) { (46f * s).toSp() })
        }

        // The form panel.
        val panelWidth = maxWidth * 0.9f
        val panelHeight = panelWidth / PanelAspect
        // With the keyboard up, the panel slides just far enough to keep the code well above it.
        val imeBottom = with(density) { androidx.compose.foundation.layout.WindowInsets.Companion.ime.getBottom(this).toDp() }
        val wellBottom = yOf(0.29f) + panelHeight * (Well2Top + WellHeight)
        val lift = if (imeBottom > 0.dp) (wellBottom + 14.dp - (maxHeight - imeBottom)).coerceAtLeast(0.dp) else 0.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = yOf(0.29f) - lift)
                .width(panelWidth)
                .height(panelHeight)
        ) {
            Image(
                painter = painterResource(R.drawable.join_panel),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            val label = PaintedStyle(color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = stringResource(R.string.online_nickname_label),
                style = label,
                modifier = Modifier.offset(x = panelWidth * WellLeft + 22.dp, y = panelHeight * (Well1Top - 0.115f))
            )
            Text(
                text = stringResource(R.string.online_room_code_label),
                style = label,
                modifier = Modifier.offset(x = panelWidth * WellLeft + 22.dp, y = panelHeight * (Well2Top - 0.108f))
            )
            WellField(
                value = uiState.nickname,
                onValueChange = viewModel::setNickname,
                enabled = viewModel.nicknameEditable,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .offset(x = panelWidth * WellLeft, y = panelHeight * Well1Top)
                    .width(panelWidth * WellWidth)
                    .height(panelHeight * WellHeight)
            )
            WellField(
                value = uiState.roomCode,
                onValueChange = viewModel::setRoomCode,
                enabled = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                placeholder = stringResource(R.string.friends_add_friend_hint),
                modifier = Modifier
                    .offset(x = panelWidth * WellLeft, y = panelHeight * Well2Top)
                    .width(panelWidth * WellWidth)
                    .height(panelHeight * WellHeight)
            )

            // The join button sits on the panel's lower edge.
            val joinWidth = panelWidth * 0.62f
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = joinWidth * (355f / 1115f) * 0.45f).width(joinWidth)
            ) {
                if (uiState.isJoining) {
                    Box(modifier = Modifier.aspectRatio(1115f / 355f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(34.dp))
                    }
                } else {
                    PaintedButton(
                        text = stringResource(R.string.online_join_room_action),
                        onClick = { viewModel.joinRoom(onJoined) }
                    )
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message.asString(),
                style = PaintedStyle(color = Color(0xFFFFD6D0), fontSize = 15.sp, textAlign = TextAlign.Center),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = yOf(0.29f) - lift + panelHeight + 56.dp)
                    .padding(horizontal = 24.dp)
                    .background(Color(0xCC3B1E08), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // The painted back button.
        val backInteraction = remember { MutableInteractionSource() }
        Image(
            painter = painterResource(R.drawable.join_back),
            contentDescription = stringResource(R.string.cd_back),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .size(56.dp)
                .clickable(interactionSource = backInteraction, indication = null, onClick = onBack)
        )
    }
}

/** Text typed straight into one of the panel's painted wells. */
@Composable
private fun WellField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    Box(modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else 0.85f }, contentAlignment = Alignment.CenterStart) {
        if (placeholder != null && value.isEmpty()) {
            Text(
                text = placeholder,
                style = PaintedStyle(color = Ink.copy(alpha = 0.38f), fontSize = 19.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 22.dp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            textStyle = PaintedStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
            cursorBrush = SolidColor(Ink),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)
        )
    }
}

/** The orange painted button; it dips a little while pressed. */
@Composable
private fun PaintedButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 650f), label = "join-btn")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1115f / 355f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Image(painter = painterResource(R.drawable.join_btn), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        LetteredText(text, 26.sp, outline = Color(0xFF8A3A00))
    }
}
