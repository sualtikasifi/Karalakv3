package com.sualtikasifi.cizimhafiza.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import com.sualtikasifi.cizimhafiza.util.AppReviewLauncher
import com.sualtikasifi.cizimhafiza.BuildConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.SupportedLanguage
import com.sualtikasifi.cizimhafiza.presentation.common.IconWell
import com.sualtikasifi.cizimhafiza.presentation.common.DEVELOPER_REVEAL_TAPS
import com.sualtikasifi.cizimhafiza.presentation.common.WarmCard
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground

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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Clears the floating back button (see ScreenTopActions).
            Spacer(modifier = Modifier.height(TopActionsClearance))

            // 2x2 rather than four stacked full-width rows: four on/off
            // toggles that each only ever say one short word took up as
            // much vertical space as everything else on this screen
            // combined.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SettingGridCell(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = stringResource(R.string.settings_sound),
                    checked = soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
                    modifier = Modifier.weight(1f)
                )
                SettingGridCell(
                    icon = Icons.Filled.MusicNote,
                    label = stringResource(R.string.settings_music),
                    checked = musicEnabled,
                    onCheckedChange = viewModel::setMusicEnabled,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SettingGridCell(
                    icon = Icons.Filled.Vibration,
                    label = stringResource(R.string.settings_vibration),
                    checked = vibrationEnabled,
                    onCheckedChange = viewModel::setVibrationEnabled,
                    modifier = Modifier.weight(1f)
                )
                SettingGridCell(
                    icon = Icons.Filled.Notifications,
                    label = stringResource(R.string.settings_notifications),
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        val needsRuntimePermission = enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        if (needsRuntimePermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setNotificationsEnabled(enabled)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (notificationsEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                BatteryOptimizationHint()
            }
            Spacer(modifier = Modifier.height(10.dp))
            LanguageRow(selectedLanguage = language, onLanguageSelected = viewModel::setLanguage)
            Spacer(modifier = Modifier.height(10.dp))
            NavRow(
                icon = Icons.Filled.School,
                label = stringResource(R.string.settings_replay_tutorial),
                onClick = onReplayTutorialClick
            )
            Spacer(modifier = Modifier.height(10.dp))
            NavRow(
                icon = Icons.Filled.BugReport,
                label = stringResource(R.string.report_bug_title),
                onClick = onReportBugClick
            )
            Spacer(modifier = Modifier.height(10.dp))
            NavRow(
                icon = Icons.Filled.AccountCircle,
                label = stringResource(R.string.account_title),
                onClick = onAccountClick,
                showBadge = showAccountNudge,
                travelingLight = !accountLinked
            )
            Spacer(modifier = Modifier.height(10.dp))
            NavRow(
                icon = Icons.Filled.StarRate,
                label = stringResource(R.string.settings_rate_app),
                // Straight to the store listing, not Play Core's in-app
                // review sheet — that API silently does nothing on a
                // sideloaded install or once its quota is spent, with no
                // failure callback to fall back from, so a tap here read as
                // a dead button. This is deterministic on every install.
                onClick = { activity?.let(AppReviewLauncher::openStoreListing) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            NavRow(
                icon = Icons.Filled.PrivacyTip,
                label = stringResource(R.string.settings_privacy_policy),
                onClick = {
                    runCatching {
                        activity?.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(PRIVACY_POLICY_URL))
                        )
                    }
                }
            )

            // The build actually running, printed where anyone can find it.
            // Without this there was no way to answer "is the APK on this
            // phone the new one?" — every build looked identical from the
            // inside, and a sideloaded install that silently did not replace
            // the old app was indistinguishable from one that did.
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(
                    R.string.settings_version_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    // No ripple and no hint that this does anything: a player
                    // who taps the version seven times should see exactly
                    // what a player who taps it once sees. Bot İsimleri used
                    // to have its own separate long-press door here — folded
                    // into a button inside the report inbox instead (see
                    // DrawingReportsScreen), since a gesture competing with
                    // this screen's own scroll turned out to be genuinely
                    // hard to land.
                    .clickable(
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
        ScreenTopActions(
            onBack = onBack,
            title = stringResource(R.string.menu_settings),
            modifier = Modifier.align(Alignment.TopStart)
        )
        }
    }
}

/**
 * "Bildirimler bazen gelmiyor, uygulamaya girince geliyor" is the exact
 * symptom of an OEM battery manager silently holding back this app's
 * AlarmManager alarms (see NotificationScheduler/ChestReadyNotifier) until
 * something else launches the app — most visible on the aggressive
 * Xiaomi/MIUI-family managers a large share of this game's players run.
 * There is no in-app fix for that; the actual fix lives in the OS's own
 * battery settings, so this just gets the player there in one tap.
 *
 * Shown only while notifications are on, and only while the OS still has
 * this app under battery restriction — rechecked every time Settings comes
 * back to the foreground (e.g. returning from the system dialog), so the
 * card disappears the moment the player grants the exemption instead of
 * still asking for something already done.
 */
@Composable
private fun BatteryOptimizationHint() {
    val context = LocalContext.current
    var ignoringOptimizations by remember {
        mutableStateOf(
            (context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager)
                ?.isIgnoringBatteryOptimizations(context.packageName) != false
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        ignoringOptimizations = (context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager)
            ?.isIgnoringBatteryOptimizations(context.packageName) != false
    }
    if (ignoringOptimizations) return

    WarmCard(corner = 22.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.settings_battery_optimization_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_battery_optimization_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton(
                text = stringResource(R.string.settings_battery_optimization_action),
                onClick = {
                    runCatching {
                        launcher.launch(
                            android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                        )
                    }.onFailure {
                        // Some OEM builds refuse the direct-request intent —
                        // the general battery-settings screen is the fallback
                        // every device actually has.
                        runCatching {
                            launcher.launch(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    showBadge: Boolean = false,
    /** A light that keeps travelling round the row's edge — draws the eye to it. */
    travelingLight: Boolean = false
) {
    WarmCard(
        corner = 22.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().then(if (travelingLight) Modifier.travelingLight(22.dp) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconWell(icon = icon)
                    // Same dot as MenuTile's unseen-achievement badge — a
                    // presence indicator, not a count, since there's nothing
                    // here to count: just "still anonymous and played enough
                    // to have something worth protecting".
                    if (showBadge) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A dropdown rather than a row of buttons — two languages fit side by side
 * as chips, but [SupportedLanguage] is meant to grow, and a chip row that
 * keeps adding entries either wraps awkwardly or shrinks each one down to
 * an initial. A dropdown stays exactly this wide no matter how many
 * languages the list eventually holds.
 */
@Composable
private fun LanguageRow(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = SupportedLanguage.resolve(selectedLanguage)
    // Selected language pinned first, the rest alphabetized by their own
    // native label (labelRes is the same string regardless of UI locale —
    // "Türkçe" reads as "Türkçe" whether the picker itself is in Turkish or
    // English — so this order doesn't reshuffle when the app's language
    // changes out from under it). Resolved to a plain map first: compareBy's
    // selector lambdas aren't inline, so a stringResource() call inside one
    // directly would not be in a composable context.
    val labels = SupportedLanguage.entries.associateWith { stringResource(it.labelRes) }
    val orderedEntries = SupportedLanguage.entries.sortedWith(
        compareBy({ it != selected }, { labels.getValue(it) })
    )
    WarmCard(corner = 22.dp, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconWell(icon = Icons.Filled.Language)
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // DropdownMenu anchors to whatever composable directly contains
            // its own call — NOT to an align() modifier passed into it (that
            // modifier only styles the floating menu's own content, once
            // already positioned; it does nothing to WHERE it's positioned).
            // Wrapping it around the earlier full-width Row made the whole
            // card the anchor, so the menu opened from that row's start
            // (the left edge) no matter what modifier was handed to
            // DropdownMenu itself. Anchoring it to just this trailing
            // flag+label+arrow Box — which SpaceBetween already pins to the
            // card's right edge — makes the menu open from there instead.
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = selected.flagEmoji, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = stringResource(selected.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    orderedEntries.forEach { language ->
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = language.flagEmoji, style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(language.labelRes))
                                }
                            },
                            onClick = {
                                expanded = false
                                onLanguageSelected(language.code)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One cell of the 2x2 Ses/Müzik/Titreşim/Bildirimler grid — icon and switch
 * share a row, the label sits below on its own so a half-width card still
 * has room for it without wrapping or shrinking the switch.
 */
@Composable
private fun SettingGridCell(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    WarmCard(corner = 20.dp, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconWell(icon = icon)
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
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
