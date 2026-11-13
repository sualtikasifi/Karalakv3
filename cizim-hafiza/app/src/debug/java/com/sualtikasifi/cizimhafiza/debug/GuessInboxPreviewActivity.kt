package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.domain.model.GuessInboxEntry
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessInboxDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the "they tried your drawings" window on its own. Extra "n" = how many friends (1..10). */
class GuessInboxPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val n = intent.getIntExtra("n", 3).coerceIn(1, 10)
        val names = listOf("Vortex", "Bob Ross", "Maximilian Alexander von Hohenzollern", "Ayşe", "Mehmet", "Zeynep", "Can", "Elif", "Deniz", "Ali")
        val entries = (0 until n).map { i -> GuessInboxEntry("e$i", "l", "u$i", names[i], i % 3 != 1, 1 + i % 3, 0L) }
        val gold = entries.count { it.solved } * 20
        setContent { CizimHafizaTheme { GuessInboxDialog(entries = entries, rewardGold = gold, onClose = { finish() }) } }
    }
}
