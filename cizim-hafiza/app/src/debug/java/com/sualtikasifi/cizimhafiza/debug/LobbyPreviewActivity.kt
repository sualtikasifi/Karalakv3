package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource
import com.sualtikasifi.cizimhafiza.presentation.online.PlayerSlotUiState
import com.sualtikasifi.cizimhafiza.presentation.online.PlayerSlotsGrid
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: the lobby's 8 seats on the lobby picture, in the stretch of the page the real lobby gives them
 * (measured off a phone: the seats run from 378 dp down to the chat dock at 596 dp), so the fit can be looked at in the
 * CI emulator. Extras: players = how many seats are taken (1..8).
 */
class LobbyPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val players = intent.getIntExtra("players", 2).coerceIn(1, 8)
        // How tall the seats area is: 218dp is a lobby without the ready button, 188dp one with it (the grid shrinks).
        val gridDp = intent.getIntExtra("grid", 218)
        val frame = AvatarFrame.entries.first()
        val slots: List<PlayerSlotUiState?> = List(8) { i ->
            if (i < players) PlayerSlotUiState(
                uid = "u$i", name = if (i == 0) "Avcution" else "Oyuncu$i", level = 10 + i, frame = frame,
                ready = i % 2 == 1, isHost = i == 0, isYou = i == 0, pending = i == 2, onKick = null
            ) else null
        }
        setContent {
            CizimHafizaTheme {
                Box(Modifier.fillMaxSize()) {
                    Image(painter = cachedPainterResource(R.drawable.bg_lobby), contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
                    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(378.dp))
                        PlayerSlotsGrid(slots = slots, activeReactionsByUid = mapOf("u0" to com.sualtikasifi.cizimhafiza.domain.model.Reaction(uid = "u0", emoji = "", messageKey = "chat_hazir_misin", sentAtMillis = 0L)), onInvite = {}, compact = false, modifier = Modifier.height(gridDp.dp))
                    }
                }
            }
        }
    }
}
