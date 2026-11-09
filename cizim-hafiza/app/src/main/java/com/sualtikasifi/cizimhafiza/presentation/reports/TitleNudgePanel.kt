package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sualtikasifi.cizimhafiza.presentation.common.TitleNudge
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSwitchRow
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSurface

/** Switches the on-screen title arrows on (see [TitleNudge]); the tuning itself is done on the game screens. */
@Composable
fun TitleNudgePanel() {
    val context = LocalContext.current
    TitleNudge.load(context)
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        ConsoleSurface {
            ConsoleSwitchRow(
                title = "Başlık ayar modu",
                checked = TitleNudge.enabled,
                onChange = { TitleNudge.setEnabled(context, it) },
                subtitle = "Açıkken oyun ekranlarının altında oklar çıkar; başlığı oklarla oynat."
            )
        }
        Text(
            "Nasıl kullanılır: modu aç, panelden çık, Hesap gibi tabelalı bir ekrana git. Alttaki oklarla başlığı yerine getir " +
                "(değer başlık boyunun yüzdesi, bu yüzden her telefonda aynı kalır). Hepsi bitince \"Tüm değerleri kopyala\" ile " +
                "listeyi kopyalayıp bana yapıştır; ben kalıcı olarak koda işlerim.",
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
