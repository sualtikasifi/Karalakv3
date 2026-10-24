package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.game.ResArtW
import com.sualtikasifi.cizimhafiza.presentation.game.ResultHeader
import com.sualtikasifi.cizimhafiza.presentation.game.ResultStat
import com.sualtikasifi.cizimhafiza.presentation.game.ResultStatGrid
import com.sualtikasifi.cizimhafiza.presentation.game.StatIcon
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the stat cards of a level result (hits and misses in one card, the XP card in the grid). */
class ResultStatsPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val orange = Color(0xFFE8650F)
        val stats = listOf(
            ResultStat(
                StatIcon.Art(R.drawable.rs_ic_check), "Doğru", "6", Color(0xFF2EA043),
                second = ResultStat(StatIcon.Art(R.drawable.rs_ic_x), "Yanlış", "0", Color(0xFFE23B32))
            ),
            ResultStat(StatIcon.Art(R.drawable.rs_ic_star), "Yıldızların", "★★★", orange),
            ResultStat(StatIcon.Art(R.drawable.icon_gold_coin), "Kelime altını", "+104 altın", orange),
            ResultStat(StatIcon.Art(R.drawable.rs_ic_crown), "Bölüm yıldızı", "+150 altın", orange),
            ResultStat(StatIcon.Art(R.drawable.rune_root), "Rün katkısı", "+44 altın", orange),
            ResultStat(StatIcon.Art(R.drawable.rs_ic_star), "", "91 XP Kazandın!", Color(0xFFF26A1B), headline = true)
        )
        setContent {
            CizimHafizaTheme {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val u = maxWidth / ResArtW
                    Image(painterResource(R.drawable.du_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Column(Modifier.fillMaxSize().padding(top = 24.dp)) {
                        Box(Modifier.fillMaxWidth()) { ResultHeader(title = "Oyun Bitti!") }
                        Column(Modifier.fillMaxWidth().padding(horizontal = u * 40f)) { ResultStatGrid(stats, u, Modifier.fillMaxWidth()) }
                    }
                }
            }
        }
    }
}
