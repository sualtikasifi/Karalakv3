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
import com.sualtikasifi.cizimhafiza.presentation.common.ChoicePill
import com.sualtikasifi.cizimhafiza.presentation.common.StartButton
import com.sualtikasifi.cizimhafiza.presentation.common.WoodScreen
import com.sualtikasifi.cizimhafiza.presentation.common.WoodSection

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
        WoodSection(stringResource(R.string.select_word_count)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                uiState.availableCounts.forEach { count ->
                    ChoicePill(
                        label = count.toString(),
                        selected = count == uiState.selectedCount,
                        onClick = { viewModel.selectCount(count) },
                        modifier = Modifier.weight(1f),
                        textSize = 20.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        WoodSection(stringResource(R.string.select_mode)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GameMode.entries.forEach { mode ->
                    ChoicePill(
                        label = "${modeEmoji(mode)}  ${modeLabel(mode)}",
                        selected = uiState.selectedMode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        modifier = Modifier.weight(1f),
                        textSize = 16.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        WoodSection(stringResource(R.string.select_category)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.categories.chunked(3).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowCategories.forEach { category ->
                            ChoicePill(
                                label = "${categoryEmoji(category)}\n$category",
                                selected = uiState.selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) },
                                modifier = Modifier.weight(1f),
                                height = 64.dp,
                                textSize = 13.sp,
                                maxLines = 2
                            )
                        }
                        repeat(3 - rowCategories.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                ChoicePill(
                    label = "${categoryEmoji(null)}  ${stringResource(R.string.all_categories)}",
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    modifier = Modifier.fillMaxWidth(),
                    textSize = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        WoodSection(stringResource(R.string.select_difficulty)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEach { difficulty ->
                        ChoicePill(
                            label = difficultyLabel(difficulty),
                            selected = uiState.selectedDifficulty == difficulty,
                            onClick = { viewModel.selectDifficulty(difficulty) },
                            modifier = Modifier.weight(1f),
                            textSize = 16.sp
                        )
                    }
                }
                ChoicePill(
                    label = stringResource(R.string.all_difficulties),
                    selected = uiState.selectedDifficulty == null,
                    onClick = { viewModel.selectDifficulty(null) },
                    modifier = Modifier.fillMaxWidth(),
                    textSize = 16.sp
                )
            }
        }
    }
}

// Matched on BOTH languages' category names for the same reason
// WordCategoryColors is (see Color.kt): the pool stores the display name and
// replaces it when the language changes, so an English player used to get the
// generic sparkle on every single category.
private fun categoryEmoji(category: String?): String = when (category) {
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

