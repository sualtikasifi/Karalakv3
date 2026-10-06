package com.sualtikasifi.cizimhafiza.presentation.chests

import com.sualtikasifi.cizimhafiza.presentation.common.pressable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import kotlinx.coroutines.delay

/**
 * "Kasalarım" — the 4 chest slots (see SettingsRepository.chestSlots).
 *
 * Tap a locked chest to start its countdown (only one at a time), tap a
 * ready one to open it with the full opening scene.
 */
@Composable
fun ChestsScreen(
    onBack: () -> Unit,
    viewModel: ChestsViewModel = hiltViewModel()
) {
    val slots by viewModel.chestSlots.collectAsState()
    val gold by viewModel.goldBalance.collectAsState()
    val now by viewModel.nowMillis.collectAsState()
    val lastReward by viewModel.lastReward.collectAsState()
    val speedupAvailable by viewModel.speedupAvailable.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val anyUnlocking = slots.any { it?.unlockStartedAtMillis != null && !it.isReady(now) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val appNotificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    // Re-read whenever the player comes back from the system settings page.
    var osNotificationsOn by remember { mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                osNotificationsOn = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsReachable = osNotificationsOn && appNotificationsEnabled

    var noticeRes by remember { mutableStateOf<Int?>(null) }
    var selectedChestId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(noticeRes) {
        if (noticeRes != null) {
            delay(2_600)
            noticeRes = null
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .screenBackground()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = TopActionsClearance, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2B1A12))
                                .padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.icon_gold_coin),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = stringResource(R.string.chests_gold_balance, gold),
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.chests_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                if (anyUnlocking && !notificationsReachable) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFFFE9C7))
                                .border(2.dp, Color(0xFFF0B24E), RoundedCornerShape(18.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "🔕", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = stringResource(R.string.chest_notif_off),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFFF7A21))
                                    .clickable {
                                        if (!osNotificationsOn) {
                                            context.startActivity(
                                                android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            )
                                        } else {
                                            viewModel.enableNotifications()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(stringResource(R.string.chest_notif_turn_on), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                itemsIndexed(slots) { index, chest ->
                    com.sualtikasifi.cizimhafiza.presentation.common.CappedFontScale {
                    ChestSlotTile(
                        chest = chest,
                        nowMillis = now,
                        anotherUnlocking = anyUnlocking,
                        speedupAvailable = speedupAvailable,
                        onSpeedup = { activity?.let { viewModel.speedUp(it) { res -> noticeRes = res } } },
                        onClick = { chest?.let { selectedChestId = it.id } }
                    )
                    }
                }
            }

            ScreenTopActions(
                onBack = onBack,
                title = stringResource(R.string.chests_title),
                modifier = Modifier.align(Alignment.TopStart)
            )

            noticeRes?.let { res ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xE62B1A12))
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(text = stringResource(res), color = Color.White, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }

    ChestDetailHost(selectedId = selectedChestId, onDismiss = { selectedChestId = null }, viewModel = viewModel)
}


@Composable
private fun ChestSlotTile(
    chest: Chest?,
    nowMillis: Long,
    anotherUnlocking: Boolean,
    speedupAvailable: Boolean,
    onSpeedup: () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val ready = chest?.isReady(nowMillis) == true
    val unlocking = chest != null && chest.unlockStartedAtMillis != null && !ready

    val infinite = rememberInfiniteTransition(label = "slot")
    val readyPulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "readyPulse"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    if (chest == null) {
        val dash = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(TileHeight)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                .drawBehind {
                    drawRoundRect(
                        color = dash,
                        cornerRadius = CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f)))
                    )
                }
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(dash.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontSize = 40.sp, color = dash, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.chest_slot_empty_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.chest_slot_empty_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val tier = chest.tier
    val text = tier.onBackdrop()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TileHeight)
            .graphicsLayer {
                val s = if (ready) readyPulse else 1f
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .border(
                width = if (ready) 3.5.dp else 2.5.dp,
                color = tier.borderColor().copy(alpha = if (ready) glowAlpha else 0.9f),
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        ChestBackdrop(tier = tier, modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(98.dp), contentAlignment = Alignment.Center) {
                ChestImage(tier = tier, width = 132.dp)
            }
            Text(
                text = stringResource(tier.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = text,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            when {
                ready -> StatusPill(
                    text = stringResource(R.string.chest_slot_ready),
                    container = AppTheme.tokens.success,
                    content = Color.White
                )
                unlocking -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // The live countdown, to the second.
                    Text(
                        text = formatCountdown(chest.remainingMillis(nowMillis)),
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = if (tier == ChestTier.RARE) Color(0xFFFFE9A8) else Color(0xFF7A3B00)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { chest.unlockProgress(nowMillis) },
                        color = if (tier == ChestTier.RARE) Color(0xFFFFE066) else tier.accent(),
                        trackColor = Color.Black.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    if (speedupAvailable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .pressable(pressedScale = 0.92f, onClick = onSpeedup)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF2E8B45))
                                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = stringResource(R.string.chest_speedup_button),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFFFE066))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.chest_speedup_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF3A2416),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                anotherUnlocking -> StatusPill(
                    text = "${stringResource(R.string.chest_slot_waiting)} · ${stringResource(R.string.chest_duration_hours, tier.durationHours())}",
                    container = Color.Black.copy(alpha = 0.3f),
                    content = Color.White
                )
                else -> StatusPill(
                    text = "${stringResource(R.string.chests_start_button)} · ${stringResource(R.string.chest_duration_hours, tier.durationHours())}",
                    container = if (tier == ChestTier.RARE) Color(0xFFFFC72E) else tier.accent(),
                    content = if (tier == ChestTier.RARE) Color(0xFF3A1A00) else Color.White
                )
            }
        }
    }
}

private val TileHeight = 244.dp

@Composable
private fun StatusPill(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1
        )
    }
}
