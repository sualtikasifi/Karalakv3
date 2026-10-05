package com.sualtikasifi.cizimhafiza.presentation.online

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.Difficulty
import com.sualtikasifi.cizimhafiza.presentation.common.StartButton
import com.sualtikasifi.cizimhafiza.presentation.common.WoodScreen
import com.sualtikasifi.cizimhafiza.presentation.common.CompactPanel
import com.sualtikasifi.cizimhafiza.presentation.common.ChoiceGrid
import com.sualtikasifi.cizimhafiza.presentation.common.PanelRow
import com.sualtikasifi.cizimhafiza.presentation.common.PillField
import com.sualtikasifi.cizimhafiza.util.asString

/** Opening a room: the same workshop scene as the offline setup, plus the player's name and the room's mode. */
@Composable
fun CreateRoomScreen(
    onBack: () -> Unit,
    onRoomCreated: (roomCode: String) -> Unit,
    viewModel: CreateRoomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    WoodScreen(
        title = stringResource(R.string.online_create_room),
        onBack = onBack,
        action = { modifier ->
            if (uiState.isCreating) {
                Box(modifier = modifier.aspectRatio(1015f / 246f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            } else {
                StartButton(
                    text = stringResource(R.string.online_create_room_action),
                    onClick = { viewModel.createRoom(onRoomCreated) },
                    modifier = modifier
                )
            }
        }
    ) {
        CompactPanel {
            PanelRow(stringResource(R.string.online_nickname_label)) {
                PillField(
                    value = uiState.nickname,
                    onValueChange = viewModel::setNickname,
                    enabled = viewModel.nicknameEditable,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            }
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
            PanelRow(stringResource(R.string.online_room_mode_title)) {
                ChoiceGrid(
                    items = listOf(false, true),
                    columns = 2,
                    pillHeight = 40.dp,
                    textSize = 14.sp,
                    label = { stringResource(if (it) R.string.online_room_mode_team else R.string.online_room_mode_free_for_all) },
                    isSelected = { it == uiState.teamMode },
                    onSelect = viewModel::setTeamMode,
                    maxLines = 1
                )
            }
            PanelRow(stringResource(R.string.select_category)) {
                ChoiceGrid(
                    items = uiState.categories.toList<String?>() + listOf<String?>(null),
                    columns = 4,
                    pillHeight = 40.dp,
                    textSize = 12.sp,
                    label = { "${com.sualtikasifi.cizimhafiza.presentation.wordcount.categoryEmoji(it)} ${it ?: stringResource(R.string.all_categories)}" },
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

        uiState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message.asString(),
                color = androidx.compose.ui.graphics.Color(0xFFFFD6D0),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0xCC3B1E08), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> stringResource(R.string.difficulty_easy)
    Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
    Difficulty.HARD -> stringResource(R.string.difficulty_hard)
}
