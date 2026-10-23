package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sualtikasifi.cizimhafiza.presentation.account.AccountScreen
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme
import dagger.hilt.android.AndroidEntryPoint

/** Debug builds only: opens the account page on its own, so its layout can be looked at in the CI emulator. */
@AndroidEntryPoint
class AccountPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CizimHafizaTheme { AccountScreen(onBack = { finish() }) } }
    }
}
