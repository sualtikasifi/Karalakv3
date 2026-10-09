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

    // "Bildirimler geç mi geliyor?" — the OEM battery manager holding back reminders (see the note in git history of
    // this file): the card offers the fix only while notifications are on and the OS still restricts this app. It is
    // rechecked when the system dialog returns, so the offer disappears the moment the exemption is granted.
    fun batteryOk(): Boolean =
        (context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager)
            ?.isIgnoringBatteryOptimizations(context.packageName) != false
    var ignoringOptimizations by remember { mutableStateOf(batteryOk()) }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        ignoringOptimizations = batteryOk()
    }
    val needsBatteryFix = notificationsEnabled && !ignoringOptimizations

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

        // ---- Notification-delay card ---------------------------------------------------------------------------
        FitText(
            text = stringResource(if (needsBatteryFix) R.string.settings_battery_optimization_title else R.string.settings_battery_ok_title),
            style = PaintedStyle(color = Ink, fontSize = fs(30f), textAlign = TextAlign.Start),
            maxLines = 1,
            minScale = 0.6f,
            contentAlignment = Alignment.CenterStart,
            modifier = box(352f, 886f, 788f, 930f)
        )
        FitText(
            text = stringResource(if (needsBatteryFix) R.string.settings_battery_optimization_body else R.string.settings_battery_ok_body),
            style = DescriptionStyle(fs(22f), fs(29f)).copy(color = Ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
            maxLines = 4,
            minScale = 0.6f,
            contentAlignment = Alignment.TopStart,
            modifier = box(376f, 938f, 790f, 1044f)
        )
        if (needsBatteryFix) {
            val fixLabel = stringResource(R.string.settings_battery_optimization_action)
            Row(
                box(355f, 1054f, 792f, 1136f)
                    .shadow((3 * us).dp, RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFD75E), Color(0xFFF7A81B))), RoundedCornerShape(50))
                    .border((2.5f * us).dp, Color(0xFFE08A1B), RoundedCornerShape(50))
                    .pressable(pressedScale = 0.95f) {
                        runCatching {
                            batteryLauncher.launch(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                            )
                        }.onFailure {
                            // Some OEM builds refuse the direct-request intent — the general battery settings
                            // screen is the fallback every device actually has.
                            runCatching {
                                batteryLauncher.launch(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        }
                    }
                    .a11yButton(fixLabel)
                    .padding(horizontal = (26f * us).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((14f * us).dp)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Ink, modifier = Modifier.size((40f * us).dp))
                FitText(
                    text = fixLabel,
                    style = PaintedStyle(color = Ink, fontSize = fs(29f), textAlign = TextAlign.Start),
                    maxLines = 1,
                    minScale = 0.6f,
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Ink, modifier = Modifier.size((38f * us).dp))
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

/** The painted switch of the scene: an orange track with a white knob when on, a grey one when off. */
@Composable
private fun SceneSwitch(checked: Boolean, modifier: Modifier) {
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
