package com.sualtikasifi.cizimhafiza.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.PenSkin
import com.sualtikasifi.cizimhafiza.domain.model.RuneHint
import com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.FeatureTourDialog
import com.sualtikasifi.cizimhafiza.presentation.mainmenu.PaintedHome
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTheme
import com.sualtikasifi.cizimhafiza.presentation.reports.console.DaySeries
import com.sualtikasifi.cizimhafiza.presentation.reports.console.RuneFunnelCard
import com.sualtikasifi.cizimhafiza.presentation.runes.RunesScreen
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import com.sualtikasifi.cizimhafiza.util.DailyChallengeState
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Debug builds only: the pieces that bring the runes forward, for the CI emulator (see runepreview.sh).
 * `screen` = home_ready (the Rünler sign with runes to buy, and the mascot's first-rune offer), home_saving (the sign
 * saving up for the next one), runes_guide (the rune map opened by the offer, pointing at the first rune, on a phone
 * reset to no runes and enough gold), tour_runes (the tour's rune page alone) or funnel (the developer panel's rune
 * funnel card with made-up numbers).
 */
@AndroidEntryPoint
class RunePromoPreviewActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        if (screen == "runes_guide") {
            // a phone that has never bought a rune and holds enough gold for the first one
            settings.resetRunes()
            settings.runesIntroSeen = false
            val gold = settings.goldBalance.value
            if (gold < 800) settings.earnGold(800 - gold)
        }
        setContent {
            CizimHafizaTheme {
                when (screen) {
                    "home_ready", "home_saving" -> PaintedHome(
                        nickname = "Barış",
                        progress = LevelProgressState.forXp(4_200),
                        frame = AvatarFrame.entries.first(),
                        photo = AvatarPhoto.Dino,
                        pen = PenSkin.CLASSIC,
                        gold = if (screen == "home_ready") 820 else 180,
                        xpEvent = null,
                        dailyState = DailyChallengeState(todayEpochDay = 20_000, lastCompletedEpochDay = 0, currentStreak = 3, bestStreak = 5, lastResult = null),
                        adGoldNextAt = 0L,
                        adChestNextAt = 0L,
                        pendingFriendRequests = 0,
                        hasUnseenAchievement = false,
                        accountNotLinked = false,
                        onFrameClick = {}, onPenClick = {}, onRankClick = {}, onGoldClick = {}, onWatchGold = {}, onWatchChest = {},
                        onDaily = {}, onPlayOnline = {}, onQuickMatch = {}, onPlay = {}, onLevels = {}, onFriends = {},
                        onAchievements = {}, onLeague = {}, onStore = {}, onSettings = {},
                        runeHint = if (screen == "home_ready") RuneHint.Affordable(2) else RuneHint.Saving(120),
                        runePrompt = screen == "home_ready",
                        onRunes = { source ->
                            startActivity(Intent(this, RunePromoPreviewActivity::class.java).putExtra("screen", if (source == "prompt") "runes_guide" else "runes"))
                        }
                    )
                    "runes_guide", "runes" -> RunesScreen(onBack = { finish() }, guide = screen == "runes_guide")
                    "tour_runes" -> FeatureTourDialog(onFinish = { finish() }, runesOnly = true, onOpenRunes = {})
                    else -> ConsoleTheme {
                        Box(Modifier.fillMaxSize().background(Console.Bg).statusBarsPadding().padding(16.dp)) {
                            RuneFunnelCard(sampleSeries())
                        }
                    }
                }
            }
        }
    }

    /** Seven days of made-up funnel counters: map opens by source, the offer, first runes, purchases. */
    private fun sampleSeries(): DaySeries {
        val days = (1..7).map { "2026-10-${24 + it}" }
        val day = mapOf(
            "rune__open__chip" to 42L, "rune__open__mascot" to 9L, "rune__open__prompt" to 14L, "rune__open__tour" to 6L,
            "rune__prompt__shown" to 51L, "rune__prompt__tapped" to 14L, "rune__prompt__dismissed" to 11L,
            "rune__first__guided" to 9L, "rune__first__self" to 4L, "rune__bought__count" to 37L
        )
        return DaySeries(days = days, players = days.map { 120L }, counts = days.map { day })
    }
}
