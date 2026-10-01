package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.LevelTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * One clock for every rank/level label on screen, so a whole table (or a lobby, or the account page)
 * turns over together. Runs only while at least one label is being drawn.
 */
object RankLabelClock {
    private const val PERIOD_MILLIS = 5_000L

    val showRank: StateFlow<Boolean> = flow {
        var rank = false
        while (true) {
            delay(PERIOD_MILLIS)
            rank = !rank
            emit(rank)
        }
    }.stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Default), SharingStarted.WhileSubscribed(), false)
}

/**
 * "33 Seviye" and the rank title ("Çırak") taking turns in the same spot every five seconds, in the same
 * type style everywhere it appears (league rows, the lobby, the account page), with a soft cross-fade.
 * [bullet] puts the "• " in front, for a label that sits beside a name rather than under it.
 */
@Composable
fun RankLevelLabel(level: Int, modifier: Modifier = Modifier, bullet: Boolean = true) {
    val showRank by RankLabelClock.showRank.collectAsState()
    val rankName = stringResource(LevelTier.forLevel(level).rank.nameRes)
    val levelText = stringResource(R.string.home_level_inline, level)
    AnimatedContent(
        targetState = showRank,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 })
                .togetherWith(fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 3 })
        },
        label = "rank-level"
    ) { rank ->
        Text(
            text = (if (bullet) "• " else "") + if (rank) rankName else levelText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}
