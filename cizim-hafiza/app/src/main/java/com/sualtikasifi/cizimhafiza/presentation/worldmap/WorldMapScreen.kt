package com.sualtikasifi.cizimhafiza.presentation.worldmap

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.paint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme

// The artwork carries its own "Dünyalar" sign; the list starts below it. 0.265 of the window height is where the
// sign's lower edge falls in bg_worlds (a 9:20 picture, cropped to fill), plus a little air.
private const val SignClearanceFraction = 0.265f

@Composable
fun WorldMapScreen(
    onWorldClick: (worldId: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: WorldMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val worlds = uiState.worlds
    val listState = rememberLazyListState()

    // Opens on "where you left off" rather than always at World 1.
    val currentIndex = worlds.indexOfFirst { it.isCurrent }
    LaunchedEffect(currentIndex >= 0) {
        if (currentIndex > 0) listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { _ ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .paint(painterResource(R.drawable.bg_worlds), contentScale = ContentScale.Crop)
        ) {
            // The list lives below the sign, not under it: the sign stays fully visible while the cards scroll
            // and are cut off along its lower edge.
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(this@BoxWithConstraints.maxHeight * SignClearanceFraction))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(worlds, key = { _, card -> card.world.id }) { _, card ->
                        WorldBanner(card = card, onClick = { if (card.unlocked) onWorldClick(card.world.id) })
                    }
                }
            }
            ScreenTopActions(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
        }
    }
}

/** The banner picture of each world, or null for a world whose picture has not been drawn yet. */
private fun bannerRes(worldId: Int): Int? = when (worldId) {
    1 -> R.drawable.world_banner_1
    2 -> R.drawable.world_banner_2
    3 -> R.drawable.world_banner_3
    4 -> R.drawable.world_banner_4
    5 -> R.drawable.world_banner_5
    6 -> R.drawable.world_banner_6
    7 -> R.drawable.world_banner_7
    8 -> R.drawable.world_banner_8
    9 -> R.drawable.world_banner_9
    else -> null
}

/** Greyed and darkened, alpha untouched, so a locked world keeps the banner's rounded outline. */
private val LockedFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply {
        setToSaturation(0.2f)
        timesAssign(ColorMatrix(floatArrayOf(
            0.55f, 0f, 0f, 0f, 0f,
            0f, 0.55f, 0f, 0f, 0f,
            0f, 0f, 0.55f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
    }
)

@Composable
private fun WorldBanner(card: WorldCardState, onClick: () -> Unit) {
    val name = stringResource(card.world.displayNameRes)
    val banner = bannerRes(card.world.id)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = card.unlocked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        if (banner != null) {
            Image(
                painter = painterResource(banner),
                contentDescription = name,
                contentScale = ContentScale.FillWidth,
                colorFilter = if (card.unlocked) null else LockedFilter,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // No picture yet: a plain card in the world's own colour, same size as the others.
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1080f / 288f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(card.world.accentColor).copy(alpha = if (card.unlocked) 1f else 0.5f))
                    .padding(horizontal = 22.dp)
            ) {
                Text(text = name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 12.dp)
        ) {
            if (card.unlocked) {
                Text(
                    text = "${stringResource(R.string.world_progress_format, card.completedLevels)} · ⭐${card.totalStars}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            } else {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.level_locked),
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .padding(5.dp)
                )
            }
        }
        if (card.isCurrent) {
            TintedBadge(
                text = stringResource(R.string.map_current_position),
                container = AppTheme.tokens.gold,
                content = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 22.dp)
            )
        }
    }
}
