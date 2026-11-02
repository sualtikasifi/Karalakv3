package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sualtikasifi.cizimhafiza.presentation.achievements.AchievementsScreen
import com.sualtikasifi.cizimhafiza.presentation.league.LeagueScreen
import com.sualtikasifi.cizimhafiza.presentation.online.JoinRoomScreen
import com.sualtikasifi.cizimhafiza.presentation.online.OnlineLobbyScreen
import com.sualtikasifi.cizimhafiza.presentation.quickmatch.SearchingScene
import com.sualtikasifi.cizimhafiza.presentation.quickmatch.FoundScene
import com.sualtikasifi.cizimhafiza.presentation.quickmatch.QuickMatchPlayerSnapshot
import com.sualtikasifi.cizimhafiza.domain.model.GhostRun
import com.sualtikasifi.cizimhafiza.presentation.reportbug.ReportBugScreen
import com.sualtikasifi.cizimhafiza.presentation.settings.SettingsScreen
import com.sualtikasifi.cizimhafiza.presentation.store.StoreScreen
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import com.sualtikasifi.cizimhafiza.presentation.worldmap.WorldMapScreen
import com.sualtikasifi.cizimhafiza.presentation.wordcount.WordCountScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug builds only: one page whose title sits on a painted sign, drawn on its own for the CI emulator, so the lettering
 * can be measured against the plank (see titles_preview.sh). `screen` = settings, store, achievements, league, join,
 * lobby, report, worlds or wordcount.
 */
@AndroidEntryPoint
class TitlesPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        setContent {
            CizimHafizaTheme {
                when (screen) {
                    "settings" -> SettingsScreen(onBack = {}, onReportBugClick = {}, onReplayTutorialClick = {}, onAccountClick = {})
                    "store" -> StoreScreen(onBack = {})
                    "achievements" -> AchievementsScreen(onBack = {})
                    "league" -> LeagueScreen(onBack = {})
                    "join" -> JoinRoomScreen(onBack = {}, onJoined = {})
                    "lobby" -> OnlineLobbyScreen(onBack = {}, onCreateRoom = {}, onJoinRoom = {})
                    "report" -> ReportBugScreen(onBack = {})
                    "worlds" -> WorldMapScreen(onWorldClick = {}, onBack = {})
                    "searching" -> SearchingScene()
                    "found" -> FoundScene(
                        opponent = GhostRun("preview", "x", "mormadenci", 83, "ROYAL_CROWN", listOf(1), 40, 8, 2000L),
                        me = QuickMatchPlayerSnapshot("Avcution", 79, "ROYAL_CROWN", 160_452),
                        onStart = {}
                    )
                    "wordcount" -> WordCountScreen(onStart = { _, _, _, _ -> }, onBack = {})
                }
            }
        }
    }
}
