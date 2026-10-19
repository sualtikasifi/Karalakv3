package com.sualtikasifi.cizimhafiza.presentation.settings

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.BuildConfig
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.SupportedLanguage
import com.sualtikasifi.cizimhafiza.presentation.common.BackdropCache
import com.sualtikasifi.cizimhafiza.presentation.common.DEVELOPER_REVEAL_TAPS
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.a11yButton
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.sceneIn
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sinkWith
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.sunkenArt
import com.sualtikasifi.cizimhafiza.util.AppReviewLauncher

// bg_settings is this size; every control is laid over its painted frame by the frame's own coordinates.
private const val ArtW = 841f
private const val ArtH = 1870f
private val Ink = Color(0xFF3B2314)

/**
 * Settings, on its painted scene: the picture carries the sign, the parchment card, the icons and the rows' frames; the
 * words, the switches and the buttons are live and sit exactly in the frames the picture leaves empty for them.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReportBugClick: () -> Unit,
    onReplayTutorialClick: () -> Unit,
    onAccountClick: () -> Unit,
    /**
     * Opens the report inbox, after [DEVELOPER_REVEAL_TAPS] taps on the
     * version line below. Hidden this way rather than as a menu row because
     * it is not a player-facing screen — see DeveloperAccess.
     */
    onDeveloperReveal: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var versionTaps by remember { mutableIntStateOf(0) }
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val language by viewModel.language.collectAsState()
    val showAccountNudge by viewModel.showAccountNudge.collectAsState()
    val accountLinked by viewModel.accountLinked.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setNotificationsEnabled(granted) }


    BoxWithConstraints(modifier = Modifier.fillMaxSize().sceneIn()) {
        // ONE scale in both directions, so the picture is never stretched; spare strips show a blurred copy of it.
        val unit = minOf(maxWidth / ArtW, maxHeight / ArtH)
        val offX = (maxWidth - unit * ArtW) / 2
        val offY = (maxHeight - unit * ArtH) / 2
        val us = unit.value
        // Lettering on the scene follows the picture, not the system font-size setting.
        val fontScale0 = LocalDensity.current.fontScale
        fun fs(art: Float) = (art * us / fontScale0).sp
        fun box(x0: Float, y0: Float, x1: Float, y1: Float): Modifier =
            Modifier.offset(offX + unit * x0, offY + unit * y0).size(unit * (x1 - x0), unit * (y1 - y0))

        val scene = cachedPainterResource(R.drawable.bg_settings)
        val sceneBitmap = remember {
            runCatching { BackdropCache.get(context.resources, R.drawable.bg_settings) }.getOrNull()
        }
        val pxX = (sceneBitmap?.width ?: 1) / ArtW
        val pxY = (sceneBitmap?.height ?: 1) / ArtH
        // A painted area that caves in under the finger (see sunkenArt).
        fun sunk(sink: com.sualtikasifi.cizimhafiza.presentation.mainmenu.SinkState, x0: Float, y0: Float, x1: Float, y1: Float, cornerArt: Float): Modifier =
            Modifier.sunkenArt(sink, sceneBitmap, x0 * pxX, y0 * pxY, (x1 - x0) * pxX, (y1 - y0) * pxY, (cornerArt * us).dp)

        if (offX > 1.dp || offY > 1.dp) {
            Image(scene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(20.dp))
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        }
        Image(scene, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = box(0f, 0f, ArtW, ArtH))

        // ---- Sign: the back button and the title on the plank --------------------------------------------------
        val backSink = rememberSink(0.9f)
        Box(
            box(28f, 95f, 142f, 195f)
                .then(sunk(backSink, 28f, 95f, 142f, 195f, 24f))
                .clickable(interactionSource = backSink.source, indication = null, onClick = onBack)
                .a11yButton(stringResource(R.string.cd_back))
        )
        LetteredText(
            text = stringResource(R.string.menu_settings),
            size = fs(80f),
            modifier = box(196f, 186f, 500f, 276f),
            minScale = 0.5f,
            title = true
        )

        // ---- "Ses ve Titreşim" card ----------------------------------------------------------------------------
        LetteredText(
            text = stringResource(R.string.settings_sound_section),
            size = fs(36f),
            modifier = box(112f, 358f, 374f, 406f),
            minScale = 0.5f,
            title = true
        )

        @Composable
        fun SwitchRow(cy: Float, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
            Box(
                box(88f, cy - 42f, 752f, cy + 42f)
                    .toggleable(
                        value = checked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Switch,
                        onValueChange = onChange
                    )
            )
            FitText(
                text = label,
                style = PaintedStyle(color = Ink, fontSize = fs(31f), textAlign = TextAlign.Start),
                maxLines = 1,
                minScale = 0.6f,
                contentAlignment = Alignment.CenterStart,
                modifier = box(215f, cy - 30f, 600f, cy + 30f)
            )
            SceneSwitch(checked, box(627f, cy - 29f, 733f, cy + 29f))
        }
        SwitchRow(467f, stringResource(R.string.settings_sound), soundEnabled, viewModel::setSoundEnabled)
        SwitchRow(562f, stringResource(R.string.settings_music), musicEnabled, viewModel::setMusicEnabled)
        SwitchRow(656f, stringResource(R.string.settings_vibration), vibrationEnabled, viewModel::setVibrationEnabled)
        SwitchRow(751f, stringResource(R.string.settings_notifications), notificationsEnabled) { enabled ->
            val needsRuntimePermission = enabled &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsRuntimePermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setNotificationsEnabled(enabled)
            }
        }

        // ---- Kupon kodu (the card's old title and text gave way to it; the fix button below stays) ------------------
        val couponState by viewModel.coupon.collectAsState()
        val couponKeyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val canUse = couponState.code.isNotBlank() && !couponState.busy
        val useCoupon = {
            couponKeyboard?.hide()
            viewModel.redeemCoupon()
        }
        // A wrong code shakes the field; a right one has its own confirmation below.
        val shake = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(couponState.message) {
            if (couponState.message is CouponMessage.Error) {
                for (d in listOf(14f, -14f, 10f, -10f, 5f, 0f)) shake.animateTo(d, tween(55))
            }
        }
        Row(
            box(352f, 884f, 754f, 952f).graphicsLayer { translationX = shake.value * density },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10f * us).dp)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFFFF8EA), RoundedCornerShape(50))
                    .border((2.5f * us).dp, if (couponState.message is CouponMessage.Error) Color(0xFFD9534F) else Color(0xFFC9A46A), RoundedCornerShape(50))
                    .padding(start = (16f * us).dp, end = (10f * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ConfirmationNumber,
                    contentDescription = null,
                    tint = Color(0xFFC77A14),
                    modifier = Modifier.size((38f * us).dp)
                )
                Spacer(Modifier.width((10f * us).dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = couponState.code,
                    onValueChange = viewModel::setCouponCode,
                    singleLine = true,
                    textStyle = PaintedStyle(color = Ink, fontSize = fs(30f), textAlign = TextAlign.Start),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { useCoupon() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (couponState.code.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.coupon_hint),
                                    style = PaintedStyle(color = Ink.copy(alpha = 0.45f), fontSize = fs(27f), textAlign = TextAlign.Start),
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (couponState.code.isNotEmpty() && !couponState.busy) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.coupon_clear),
                        tint = Ink.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size((40f * us).dp)
                            .clip(CircleShape)
                            .clickable { viewModel.setCouponCode("") }
                            .padding((8f * us).dp)
                    )
                }
            }
        }
            val useLabel = stringResource(R.string.coupon_use)
            // The redeem button has the row below, on the left of "how to get codes".
            Box(
                box(352f, 962f, 544f, 1030f)
                    .shadow(if (canUse) (3 * us).dp else 0.dp, RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            if (canUse) listOf(Color(0xFFFFD75E), Color(0xFFF7A81B)) else listOf(Color(0xFFEADCC0), Color(0xFFD9C7A4))
                        ),
                        RoundedCornerShape(50)
                    )
                    .border((2.5f * us).dp, if (canUse) Color(0xFFE08A1B) else Color(0xFFBFA77A), RoundedCornerShape(50))
                    .then(if (canUse) Modifier.pressable(pressedScale = 0.95f) { useCoupon() } else Modifier)
                    .a11yButton(useLabel),
                contentAlignment = Alignment.Center
            ) {
                if (couponState.busy) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size((30f * us).dp),
                        strokeWidth = (3.5f * us).dp,
                        color = Ink
                    )
                } else {
                    FitText(
                        text = useLabel,
                        style = PaintedStyle(color = if (canUse) Ink else Ink.copy(alpha = 0.45f), fontSize = fs(29f), textAlign = TextAlign.Center),
                        maxLines = 1,
                        minScale = 0.6f,
                        modifier = Modifier.padding(horizontal = (8f * us).dp)
                    )
            }
        }
        // A chest from a code opens straight away with its opening animation (several, one after another).
        couponState.chestRewards.firstOrNull()?.let { reward ->
            // Keyed on how many are left, so each chest starts its own opening from the beginning.
            androidx.compose.runtime.key(couponState.chestRewards.size) {
                com.sualtikasifi.cizimhafiza.presentation.chests.ChestOpeningDialog(reward = reward, onDismiss = viewModel::consumeCouponChest)
            }
        }
        // What came of the last try, as a pill with its own icon: what was won (green), or why not (red).
        var lastMessage by remember { mutableStateOf<CouponMessage?>(null) }
        LaunchedEffect(couponState.message) { couponState.message?.let { lastMessage = it } }
        Box(box(352f, 1042f, 754f, 1100f), contentAlignment = Alignment.CenterStart) {
            androidx.compose.animation.AnimatedVisibility(
                visible = couponState.message != null,
                enter = androidx.compose.animation.fadeIn(tween(160)) + androidx.compose.animation.slideInVertically(tween(220)) { it / 3 },
                exit = androidx.compose.animation.fadeOut(tween(160))
            ) {
                val message = lastMessage
                val ok = message as? CouponMessage.Ok
                val text = when {
                    ok != null -> buildList {
                        if (ok.gold > 0) add(stringResource(R.string.coupon_won_gold, ok.gold))
                        if (ok.xp > 0) add(stringResource(R.string.coupon_won_xp, ok.xp))
                        if (ok.chests > 0) add(stringResource(R.string.coupon_won_chest, ok.chests))
                    }.joinToString("  ·  ").ifBlank { stringResource(R.string.coupon_won_nothing) }
                    message is CouponMessage.Error -> stringResource(message.res)
                    else -> ""
                }
                val tint = if (ok != null) Color(0xFF2E7D32) else Color(0xFFB3261E)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tint.copy(alpha = 0.12f), RoundedCornerShape((22f * us).dp))
                        .border((2f * us).dp, tint.copy(alpha = 0.55f), RoundedCornerShape((22f * us).dp))
                        .padding(horizontal = (14f * us).dp)
                ) {
                    Icon(
                        if (ok != null) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size((42f * us).dp)
                    )
                    Spacer(Modifier.width((10f * us).dp))
                    FitText(
                        text = text,
                        style = DescriptionStyle(fs(24f), fs(28f)).copy(color = tint, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start),
                        maxLines = 2,
                        minScale = 0.6f,
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        // ---- How to win a code: the other half of the row under "Kullan", opening a page that points to the socials --
        var couponInfoOpen by remember { mutableStateOf(false) }
        val infoLabel = stringResource(R.string.coupon_how_button)
        Row(
            box(554f, 962f, 754f, 1030f)
                .shadow((3 * us).dp, RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3D6), Color(0xFFFFD98A))), RoundedCornerShape(50))
                .border((2.5f * us).dp, Color(0xFFE08A1B), RoundedCornerShape(50))
                .pressable(pressedScale = 0.95f) { couponInfoOpen = true }
                .a11yButton(infoLabel)
                .padding(horizontal = (10f * us).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((5f * us).dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = Ink, modifier = Modifier.size((28f * us).dp))
            FitText(
                text = infoLabel,
                style = PaintedStyle(color = Ink, fontSize = fs(24f), textAlign = TextAlign.Start).copy(
                    lineHeight = fs(26f),
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                        androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                        androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                    )
                ),
                maxLines = 2,
                minScale = 0.6f,
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f)
            )
        }
        if (couponInfoOpen) {
            val openLink = { url: String ->
                runCatching { activity?.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                Unit
            }
            if (androidx.compose.ui.res.booleanResource(R.bool.coupon_how_painted_art)) {
                // The painted window (ls_coupon_frame, 987 x 1371): its buttons are part of the picture, so live, invisible
                // buttons sit over them and cave in by redrawing their own pixels while pressed.
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { couponInfoOpen = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        val frameW = minOf(maxWidth, (maxHeight - 24.dp) * 987f / 1371f)
                        val k = frameW / 987f
                        val frameBitmap = remember { runCatching { BackdropCache.get(context.resources, R.drawable.ls_coupon_frame) }.getOrNull() }
                        val px = (frameBitmap?.width ?: 987) / 987f
                        @Composable
                        fun Hit(x0: Float, y0: Float, x1: Float, y1: Float, corner: Float, label: String, onClick: () -> Unit) {
                            val sink = com.sualtikasifi.cizimhafiza.presentation.mainmenu.rememberSink(0.94f)
                            Box(
                                Modifier
                                    .offset(k * x0, k * y0)
                                    .size(k * (x1 - x0), k * (y1 - y0))
                                    .sunkenArt(sink, frameBitmap, x0 * px, y0 * px, (x1 - x0) * px, (y1 - y0) * px, k * corner)
                                    .clickable(interactionSource = sink.source, indication = null, onClick = onClick)
                                    .a11yButton(label)
                            )
                        }
                        Box(Modifier.size(frameW, k * 1371f)) {
                            Image(androidx.compose.ui.res.painterResource(R.drawable.ls_coupon_frame), contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                            Hit(841f, 294f, 966f, 419f, 62f, stringResource(R.string.coupon_how_close)) { couponInfoOpen = false }
                            Hit(150f, 818f, 852f, 976f, 70f, stringResource(R.string.coupon_how_instagram)) { openLink("https://www.instagram.com/karalakapp") }
                            Hit(150f, 1004f, 852f, 1154f, 70f, stringResource(R.string.coupon_how_tiktok)) { openLink("https://www.tiktok.com/@karalakapp") }
                            Hit(360f, 1199f, 625f, 1270f, 30f, stringResource(R.string.coupon_how_close)) { couponInfoOpen = false }
                        }
                    }
                }
            } else {
            androidx.compose.ui.window.Dialog(onDismissRequest = { couponInfoOpen = false }) {
                Column(
                    Modifier
                        .background(Color(0xFFFFF8EA), RoundedCornerShape(28.dp))
                        .border(3.dp, Color(0xFFC9A46A), RoundedCornerShape(28.dp))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(stringResource(R.string.coupon_how_title), style = PaintedStyle(color = Ink, fontSize = 24.sp, textAlign = TextAlign.Start))
                    Text(stringResource(R.string.coupon_how_body), style = DescriptionStyle(16.sp, 22.sp).copy(color = Ink))
                    androidx.compose.material3.Button(
                        onClick = { openLink("https://www.instagram.com/karalakapp") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.coupon_how_instagram)) }
                    androidx.compose.material3.Button(
                        onClick = { openLink("https://www.tiktok.com/@karalakapp") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.coupon_how_tiktok)) }
                    androidx.compose.material3.TextButton(onClick = { couponInfoOpen = false }, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.coupon_how_close))
                    }
                }
            }
            }
        }
        // ---- Rows ----------------------------------------------------------------------------------------------
        @Composable
        fun SceneRow(cy: Float, label: String, onClick: () -> Unit, extra: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {}) {
            val sink = rememberSink(0.95f)
            Box(
                box(52f, cy - 47f, 790f, cy + 47f)
                    .then(sunk(sink, 52f, cy - 47f, 790f, cy + 47f, 40f))
                    .then(extra)
                    .clickable(interactionSource = sink.source, indication = null, onClick = onClick)
                    .a11yButton(label)
            ) {
                Box(Modifier.fillMaxSize().sinkWith(sink, 0.5f, 0.5f), contentAlignment = Alignment.CenterStart) {
                    FitText(
                        text = label,
                        style = PaintedStyle(color = Ink, fontSize = fs(31f), textAlign = TextAlign.Start),
                        maxLines = 1,
                        minScale = 0.6f,
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(start = unit * (178f - 52f)).width(unit * (470f - 178f))
                    )
                    content()
                }
            }
        }

        // Language: a dropdown, because SupportedLanguage is meant to grow and a chip row would not.
        var languageOpen by remember { mutableStateOf(false) }
        val selectedLanguage = SupportedLanguage.resolve(language)
        // Selected language pinned first, the rest alphabetized by their own native label (the same string whatever
        // the UI locale, so the order does not reshuffle when the app's language changes).
        val languageLabels = SupportedLanguage.entries.associateWith { stringResource(it.labelRes) }
        val orderedLanguages = SupportedLanguage.entries.sortedWith(compareBy({ it != selectedLanguage }, { languageLabels.getValue(it) }))
        SceneRow(1237f, stringResource(R.string.settings_language), onClick = { languageOpen = true }) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = unit * (790f - 726f))) {
                Row(horizontalArrangement = Arrangement.spacedBy((10f * us).dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = selectedLanguage.flagEmoji, fontSize = fs(34f))
                    Text(
                        text = languageLabels.getValue(selectedLanguage),
                        style = PaintedStyle(color = Ink, fontSize = fs(31f), textAlign = TextAlign.End),
                        maxLines = 1
                    )
                }
                DropdownMenu(expanded = languageOpen, onDismissRequest = { languageOpen = false }) {
                    orderedLanguages.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = entry.flagEmoji, style = MaterialTheme.typography.bodyMedium)
                                    Text(languageLabels.getValue(entry))
                                }
                            },
                            onClick = {
                                languageOpen = false
                                viewModel.setLanguage(entry.code)
                            }
                        )
                    }
                }
            }
        }
        SceneRow(1347f, stringResource(R.string.settings_replay_tutorial), onReplayTutorialClick)
        SceneRow(1457f, stringResource(R.string.report_bug_title), onReportBugClick)
        SceneRow(
            1567f, stringResource(R.string.account_title), onAccountClick,
            // A light that keeps travelling round the row's edge until a Google account is linked.
            extra = if (!accountLinked) Modifier.travelingLight((40f * us).dp) else Modifier
        )
        if (showAccountNudge) {
            Box(box(134f, 1527f, 158f, 1551f).background(Color(0xFFE53935), CircleShape).border(2.dp, Color.White, CircleShape))
        }
        SceneRow(
            1677f, stringResource(R.string.settings_rate_app),
            // Straight to the store listing, not Play Core's in-app review sheet — that API silently does nothing on a
            // sideloaded install or once its quota is spent, with no failure callback to fall back from.
            onClick = { activity?.let(AppReviewLauncher::openStoreListing) }
        )

        // ---- Privacy policy and the build number, on one small parchment strip under the rows -----------------
        val privacyLabel = stringResource(R.string.settings_privacy_policy)
        Row(
            box(190f, 1752f, 650f, 1808f)
                .shadow((3 * us).dp, RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3D6), Color(0xFFFFD98A))), RoundedCornerShape(50))
                .border((2f * us).dp, Color(0xFFE08A1B), RoundedCornerShape(50))
                .pressable(pressedScale = 0.96f) {
                    // A Custom Tab rather than handing off to the browser app: it opens over the game, in its colours,
                    // and back returns straight here (a bare ACTION_VIEW could land on the browser's first-run screen).
                    val uri = android.net.Uri.parse(PRIVACY_POLICY_URL)
                    runCatching {
                        androidx.browser.customtabs.CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .setDefaultColorSchemeParams(
                                androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                                    .setToolbarColor(android.graphics.Color.parseColor("#8A4E12"))
                                    .build()
                            )
                            .build()
                            .launchUrl(context, uri)
                    }.onFailure {
                        runCatching { activity?.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
                    }
                }
                .a11yButton(privacyLabel)
                .padding(horizontal = (22f * us).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10f * us).dp)
        ) {
            Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = Ink, modifier = Modifier.size((30f * us).dp))
            FitText(
                text = privacyLabel,
                style = PaintedStyle(color = Ink, fontSize = fs(25f), textAlign = TextAlign.Start),
                maxLines = 1,
                minScale = 0.6f,
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f)
            )
            // The build actually running, printed where anyone can find it. Seven taps on it open the developer door,
            // with no ripple and no hint that it does anything.
            Text(
                text = stringResource(R.string.settings_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = DescriptionStyle(fs(18f)).copy(color = Ink.copy(alpha = 0.75f), textAlign = TextAlign.End),
                maxLines = 1,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        versionTaps++
                        if (versionTaps >= DEVELOPER_REVEAL_TAPS) {
                            versionTaps = 0
                            onDeveloperReveal()
                        }
                    }
                )
            )
        }
    }
}

/** The painted switch of the scene: an orange track with a white knob when on, a grey one when off. Hesap uses it too. */
@Composable
internal fun SceneSwitch(checked: Boolean, modifier: Modifier) {
    val t by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "switch")
    BoxWithConstraints(
        modifier = modifier
            .background(
                if (checked) Brush.verticalGradient(listOf(Color(0xFFFF9A3C), Color(0xFFF26A1B)))
                else Brush.verticalGradient(listOf(Color(0xFFCFC6B8), Color(0xFFB4AA9B))),
                RoundedCornerShape(50)
            )
            .border(2.dp, if (checked) Color(0xFFC4500F) else Color(0xFF8F8576), RoundedCornerShape(50))
    ) {
        val knob = maxHeight * 0.8f
        val pad = maxHeight * 0.1f
        Box(
            Modifier
                .offset(x = pad + (maxWidth - knob - pad * 2) * t, y = pad)
                .size(knob)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

private const val PRIVACY_POLICY_URL = "https://sualtikasifi.github.io/app-ads/"

/**
 * A bright arc that circles the element's edge, lap after lap. Used on the account row for anyone who has not
 * linked a Google account yet, so the one thing that protects their progress is the thing the eye lands on.
 */
private fun Modifier.travelingLight(corner: androidx.compose.ui.unit.Dp): Modifier = composed {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "accountLight")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(2600, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "turn"
    )
    drawWithContent {
        drawContent()
        val stroke = 4.dp.toPx()
        val glow = Color(0xFFFF7A1A)
        val angle = turn
        val brush = object : androidx.compose.ui.graphics.ShaderBrush() {
            override fun createShader(size: androidx.compose.ui.geometry.Size): android.graphics.Shader =
                android.graphics.SweepGradient(
                    size.width / 2f,
                    size.height / 2f,
                    intArrayOf(
                        Color.Transparent.toArgb(), Color.Transparent.toArgb(),
                        glow.copy(alpha = 0.95f).toArgb(), Color(0xFFFF5A00).toArgb()
                    ),
                    floatArrayOf(0f, 0.45f, 0.85f, 1f)
                ).also { shader ->
                    shader.setLocalMatrix(android.graphics.Matrix().apply { postRotate(angle, size.width / 2f, size.height / 2f) })
                }
        }
        drawRoundRect(
            brush = brush,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
        )
    }
}
