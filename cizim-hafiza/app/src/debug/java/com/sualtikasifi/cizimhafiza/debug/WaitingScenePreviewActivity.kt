package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.domain.model.OnlinePlayer
import com.sualtikasifi.cizimhafiza.presentation.online.OnlineWaitingScene
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/** Debug builds only: the "Bitirdin!" waiting scene of an online game on its own, for the CI emulator. */
class WaitingScenePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val me = OnlinePlayer(uid = "me", displayName = "Avcution", level = 76, finished = true, totalScore = 66)
        val other = OnlinePlayer(uid = "other", displayName = "liliput", level = 10)
        setContent { CizimHafizaTheme { OnlineWaitingScene(me = me, others = listOf(other), myUid = "me", onBack = { finish() }) } }
    }
}
