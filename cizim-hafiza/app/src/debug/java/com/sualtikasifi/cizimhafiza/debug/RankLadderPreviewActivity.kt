package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.RankLadderDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the painted rank ladder at a given player level (`--ei level 79`; default 79). */
class RankLadderPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val level = intent.getIntExtra("level", 79)
        val xp = PlayerLevel.totalXpForLevel(level) + 40
        setContent {
            CizimHafizaTheme {
                Image(painterResource(R.drawable.du_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                RankLadderDialog(progress = LevelProgressState.forXp(xp), onDismiss = { finish() })
            }
        }
    }
}
