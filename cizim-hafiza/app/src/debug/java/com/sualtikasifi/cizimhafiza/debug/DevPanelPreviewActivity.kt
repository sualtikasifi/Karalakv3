package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.common.developerBackground
import com.sualtikasifi.cizimhafiza.presentation.reports.DrawingReportsGate
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: the developer panel's background, for the CI emulator. `?screen=gate` is the real passcode screen
 * (nothing behind it needs an account); the default is a stand-in for a working screen (cards, headings and notes
 * straight on the background) built from the panel's own pieces, since the real panel needs a signed-in reviewer.
 */
class DevPanelPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gate = intent.getStringExtra("screen") == "gate"
        setContent {
            CizimHafizaTheme {
                if (gate) {
                    DrawingReportsGate(onBack = { finish() })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().developerBackground(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 72.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column {
                                Text("Video Stüdyosu", style = Console.Title)
                                Text("Karalak'lı dikey videolar · 1080×1920 · sesli", style = Console.Small)
                            }
                        }
                        listOf("1  Çizimleri seç" to "3/5 seçili", "2  Gerçeğe dönüştür" to "2/3 görsel · 1/3 video", "3  Kanca: videonun ilk saniyeleri" to "≈ 3 sn · sesli", "4  Kapanış görseli" to "≈ 4,1 sn · sesli").forEach { (title, note) ->
                            item {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(title, style = Console.Subtitle, modifier = Modifier.weight(1f))
                                    Text(note, style = Console.Small)
                                }
                            }
                            item {
                                ConsoleCard(Modifier.fillMaxWidth()) {
                                    Text("Video, çizimin oyun ekranında baştan çizilmesiyle başlar: kelime üstte, süre köşede akar.", style = Console.Small)
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Tag("Sesli", fg = Console.GoodText, bg = Console.GoodSoft)
                                        Tag("Klasik açılış", fg = Console.Accent, bg = Console.AccentSoft)
                                    }
                                }
                            }
                        }
                        item { Text("Not: bu satır kartın dışında, doğrudan arka planda duruyor; okunur olmalı.", style = Console.Small) }
                    }
                }
            }
        }
    }
}
