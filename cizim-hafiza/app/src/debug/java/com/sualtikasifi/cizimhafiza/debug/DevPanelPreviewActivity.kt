package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.DrawingReportsGate
import com.sualtikasifi.cizimhafiza.presentation.reports.DrawingReportsScreen
import com.sualtikasifi.cizimhafiza.presentation.reports.DrawingReportsViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.ReportsTab
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug builds only: the developer panel as it really is, for the CI emulator. `screen=gate` is the passcode screen;
 * any other value names a [ReportsTab] (Dashboard, Tools, Studio, Coupons…) and opens the real panel straight on it,
 * past the passcode (a debug build has no code to type). Without a signed-in reviewer the panels show their empty and
 * "could not read" states, which is part of what there is to look at.
 */
@AndroidEntryPoint
class DevPanelPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        setContent {
            CizimHafizaTheme {
                if (screen == "gate") {
                    DrawingReportsGate(onBack = { finish() })
                } else {
                    val tab = ReportsTab.entries.find { it.name.equals(screen, ignoreCase = true) } ?: ReportsTab.Dashboard
                    val vm: DrawingReportsViewModel = hiltViewModel()
                    LaunchedEffect(tab) { vm.selectTab(tab) }
                    DrawingReportsScreen(onBack = { finish() }, viewModel = vm)
                }
            }
        }
    }
}
