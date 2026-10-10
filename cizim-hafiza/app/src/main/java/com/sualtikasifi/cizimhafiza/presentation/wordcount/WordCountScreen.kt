package com.sualtikasifi.cizimhafiza.presentation.wordcount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Difficulty
import com.sualtikasifi.cizimhafiza.domain.model.GameMode
import com.sualtikasifi.cizimhafiza.presentation.common.StartButton
import com.sualtikasifi.cizimhafiza.presentation.common.WoodScreen
import com.sualtikasifi.cizimhafiza.presentation.common.CompactPanel
import com.sualtikasifi.cizimhafiza.presentation.common.ChoiceGrid
import com.sualtikasifi.cizimhafiza.presentation.common.PanelRow

@Composable
fun WordCountScreen(
    onStart: (count: Int, category: String?, difficulty: Difficulty?, mode: GameMode) -> Unit,
    onBack: () -> Unit,
    viewModel: WordCountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    WoodScreen(
        title = stringResource(R.string.menu_play),
        onBack = onBack,
        action = { modifier ->
            StartButton(
                text = stringResource(R.string.start_game),
                onClick = { onStart(uiState.selectedCount, uiState.selectedCategory, uiState.selectedDifficulty, uiState.selectedMode) },
                modifier = modifier
            )
        }
    ) {
        CompactPanel(paper = true) {
            PanelRow(stringResource(R.string.select_word_count)) {
                ChoiceGrid(
                    items = uiState.availableCounts,
                    columns = uiState.availableCounts.size.coerceIn(1, 6),
                    pillHeight = 38.dp,
                    textSize = 18.sp,
                    label = { it.toString() },
                    isSelected = { it == uiState.selectedCount },
                    onSelect = viewModel::selectCount
                )
            }
            PanelRow(stringResource(R.string.select_mode)) {
                ChoiceGrid(
                    items = GameMode.entries.toList(),
                    columns = GameMode.entries.size.coerceIn(1, 3),
                    pillHeight = 40.dp,
                    textSize = 14.sp,
                    label = { "${modeEmoji(it)}  ${modeLabel(it)}" },
                    isSelected = { uiState.selectedMode == it },
                    onSelect = viewModel::selectMode,
                    maxLines = 1
                )
            }
            PanelRow(stringResource(R.string.select_category)) {
                ChoiceGrid(
                    items = uiState.categories.toList<String?>() + listOf<String?>(null),
                    columns = 4,
                    pillHeight = 40.dp,
                    textSize = 12.sp,
                    label = { "${categoryEmoji(it)} ${it ?: stringResource(R.string.all_categories)}" },
                    isSelected = { uiState.selectedCategory == it },
                    onSelect = viewModel::selectCategory,
                    maxLines = 1
                )
            }
            PanelRow(stringResource(R.string.select_difficulty)) {
                ChoiceGrid(
                    items = Difficulty.entries.toList<Difficulty?>() + listOf<Difficulty?>(null),
                    columns = 4,
                    pillHeight = 38.dp,
                    textSize = 13.sp,
                    label = { if (it == null) stringResource(R.string.all_difficulties) else difficultyLabel(it) },
                    isSelected = { uiState.selectedDifficulty == it },
                    onSelect = viewModel::selectDifficulty,
                    maxLines = 1
                )
            }
        }
    }
}

// Matched on BOTH languages' category names for the same reason
// WordCategoryColors is (see Color.kt): the pool stores the display name and
// replaces it when the language changes, so an English player used to get the
// generic sparkle on every single category.
internal fun categoryEmoji(category: String?): String = when (category) {
    "Hayvanlar", "Animals" -> "🐶"
    "Eşyalar", "Objects" -> "🧺"
    "Meslekler", "Professions" -> "👮"
    "Spor", "Sports" -> "⚽"
    "Doğa", "Nature" -> "🌲"
    "Yiyecekler", "Food" -> "🍎"
    "Taşıtlar", "Vehicles" -> "🚗"
    "Duygular", "Emotions" -> "😊"
    "Giyim", "Clothing" -> "👕"
    null -> "🎨"
    else -> "✨"
}

private fun modeEmoji(mode: GameMode): String = when (mode) {
    GameMode.NORMAL -> "⏱️"
    GameMode.RELAXED -> "🧘"
}

@Composable
private fun modeLabel(mode: GameMode): String = when (mode) {
    GameMode.NORMAL -> stringResource(R.string.mode_normal)
    GameMode.RELAXED -> stringResource(R.string.mode_relaxed)
}

@Composable
private fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> stringResource(R.string.difficulty_easy)
    Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
    Difficulty.HARD -> stringResource(R.string.difficulty_hard)
}

