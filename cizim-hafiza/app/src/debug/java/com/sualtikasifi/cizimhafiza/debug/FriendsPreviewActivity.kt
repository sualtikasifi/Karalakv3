package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.domain.model.Friend
import com.sualtikasifi.cizimhafiza.presentation.friends.FriendsScreen
import com.sualtikasifi.cizimhafiza.presentation.friends.FriendsUiState
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import dagger.hilt.android.AndroidEntryPoint

/** Debug builds only: the friends page with seven made-up friends, to see how a long list sits in its frame. */
@AndroidEntryPoint
class FriendsPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val names = listOf("bob ross", "liliput", "Zeynep", "Mehmet Ali", "karalamaci42", "Ece", "Can")
        val state = FriendsUiState(
            nickname = "Karalak4011",
            myLevel = 89,
            myFriendCode = "523488",
            friends = names.mapIndexed { i, n -> Friend("u$i", n) }
        )
        setContent {
            CizimHafizaTheme {
                FriendsScreen(onNavigateToWaitingRoom = {}, onBack = { finish() }, onDuel = { _, _ -> }, onDuelList = {}, previewState = state)
            }
        }
    }
}
