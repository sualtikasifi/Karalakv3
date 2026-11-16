package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.presentation.duel.DuelResultDialog
import com.sualtikasifi.cizimhafiza.presentation.duel.RecentDuel
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import com.sualtikasifi.cizimhafiza.util.DuelHistoryEntry

/** Debug builds only: the finished-duel result window (`--es mode won|lost|tie`, default won; `--es name` for the other player). */
class DuelResultDialogPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra("mode") ?: "won"
        val name = intent.getStringExtra("name") ?: "liliput"
        val (mine, theirs) = when (mode) { "lost" -> 41 to 64; "tie" -> 50 to 50; else -> 64 to 41 }
        val entry = DuelHistoryEntry("d1", true, name, mine, theirs, 10, 7, 10, 0L)
        setContent {
            CizimHafizaTheme { DuelResultDialog(recent = RecentDuel(entry), onDismiss = { finish() }) }
        }
    }
}
