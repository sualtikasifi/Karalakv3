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
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.ChoicePill
import com.sualtikasifi.cizimhafiza.presentation.common.StartButton
import com.sualtikasifi.cizimhafiza.presentation.common.WoodScreen
import com.sualtikasifi.cizimhafiza.presentation.common.WoodSection
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
                Box(modifier = modifier.aspectRatio(509f / 123f), contentAlignment = Alignment.Center) {
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
        WoodSection(stringResource(R.string.online_nickname_label)) {
            AppTextField(
                value = uiState.nickname,
                onValueChange = viewModel::setNickname,
                enabled = viewModel.nicknameEditable,
                label = stringResource(R.string.online_nickname_label),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
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
        WoodSection(stringResource(R.string.online_room_mode_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ChoicePill(
                    label = stringResource(R.string.online_room_mode_free_for_all),
                    selected = !uiState.teamMode,
                    onClick = { viewModel.setTeamMode(false) },
                    modifier = Modifier.weight(1f),
                    height = 56.dp,
                    textSize = 15.sp,
                    maxLines = 2
                )
                ChoicePill(
                    label = stringResource(R.string.online_room_mode_team),
                    selected = uiState.teamMode,
                    onClick = { viewModel.setTeamMode(true) },
                    modifier = Modifier.weight(1f),
                    height = 56.dp,
                    textSize = 15.sp,
                    maxLines = 2
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        WoodSection(stringResource(R.string.select_category)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.categories.chunked(3).forEach { rowCategories ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowCategories.forEach { category ->
                            ChoicePill(
                                label = category,
                                selected = uiState.selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) },
                                modifier = Modifier.weight(1f),
                                height = 52.dp,
                                textSize = 13.sp,
                                maxLines = 2
                            )
                        }
                        repeat(3 - rowCategories.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                ChoicePill(
                    label = "🎨  ${stringResource(R.string.all_categories)}",
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
