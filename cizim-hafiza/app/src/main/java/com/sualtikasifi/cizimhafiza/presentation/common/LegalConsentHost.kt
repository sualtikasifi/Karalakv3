package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.LegalLinks
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/** Whether the player may be under 13 (see LegalConsentStore.childMode), provided once at the app's root (MainActivity). */
val LocalChildMode = staticCompositionLocalOf { false }

@HiltViewModel
class LegalConsentViewModel @Inject constructor(
    private val store: LegalConsentStore,
    settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Asked once the first-run tutorial is behind the player (the tutorial is offline and uploads nothing), and before
     * the ad consent form and the notification permission, which both wait for it (see MainActivity).
     */
    val shouldShow: StateFlow<Boolean> = combine(
        flow {
            while (!settingsRepository.tutorialCompleted) delay(1_000)
            emit(true)
        },
        store.needsPromptFlow
    ) { tutorialDone, needed -> tutorialDone && needed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun accept(date: com.sualtikasifi.cizimhafiza.util.BirthDate) = store.accept(date)
}

/**
 * The first-run legal prompt: a neutral date-of-birth question (nothing hints at an "expected" answer, no date is
 * preselected) and acceptance of the terms of use, with both legal pages a tap away. Not dismissable: the answers decide
 * how the online features, ads and Analytics behave (LegalConsentStore), and an existing player sees it once after
 * updating as well, since the terms are new to them too.
 */
@Composable
fun LegalConsentHost(viewModel: LegalConsentViewModel = hiltViewModel()) {
    val show by viewModel.shouldShow.collectAsState()
    if (!show) return
    val context = LocalContext.current
    var dayText by rememberSaveable { mutableStateOf("") }
    var monthText by rememberSaveable { mutableStateOf("") }
    var yearText by rememberSaveable { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val monthFocus = remember { FocusRequester() }
    val yearFocus = remember { FocusRequester() }
    val date = com.sualtikasifi.cizimhafiza.util.BirthDate(yearText.toIntOrNull() ?: 0, monthText.toIntOrNull() ?: 0, dayText.toIntOrNull() ?: 0)
    val complete = dayText.isNotEmpty() && monthText.isNotEmpty() && yearText.length == 4
    val valid = complete && date.month > 0 && date.day > 0 && LegalConsentStore.isValidBirthDate(date)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        RaisedCard(corner = 28.dp, raise = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.legal_title),
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.legal_birth_date_label),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Day, month, year: each field hands over to the next once it is full.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePart(
                        label = stringResource(R.string.legal_birth_day),
                        hint = stringResource(R.string.legal_birth_day_hint),
                        value = dayText,
                        maxLength = 2,
                        onValue = { dayText = it; showError = false; if (it.length == 2) monthFocus.requestFocus() },
                        modifier = Modifier.weight(1f)
                    )
                    DatePart(
                        label = stringResource(R.string.legal_birth_month),
                        hint = stringResource(R.string.legal_birth_month_hint),
                        value = monthText,
                        maxLength = 2,
                        onValue = { monthText = it; showError = false; if (it.length == 2) yearFocus.requestFocus() },
                        modifier = Modifier.weight(1f),
                        fieldModifier = Modifier.focusRequester(monthFocus)
                    )
                    DatePart(
                        label = stringResource(R.string.legal_birth_year),
                        hint = stringResource(R.string.legal_birth_year_hint),
                        value = yearText,
                        maxLength = 4,
                        onValue = { yearText = it; showError = false },
                        modifier = Modifier.weight(1.5f),
                        fieldModifier = Modifier.focusRequester(yearFocus)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(if (showError) R.string.legal_birth_date_invalid else R.string.legal_birth_date_why),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                val terms = stringResource(R.string.legal_terms_link)
                val privacy = stringResource(R.string.legal_privacy_link)
                val template = stringResource(R.string.legal_accept_text, "\u0001", "\u0002")
                val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
                val text = buildAnnotatedString {
                    template.forEach { c ->
                        when (c) {
                            '\u0001' -> withLink(LinkAnnotation.Clickable("terms", linkStyle) { LegalLinks.open(context, LegalLinks.termsOfUse(context)) }) { append(terms) }
                            '\u0002' -> withLink(LinkAnnotation.Clickable("privacy", linkStyle) { LegalLinks.open(context, LegalLinks.privacyPolicy(context)) }) { append(privacy) }
                            else -> append(c)
                        }
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryButton(
                    text = stringResource(R.string.legal_continue),
                    onClick = { if (valid) viewModel.accept(date) else showError = true },
                    enabled = complete,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Wraps a screen that is for ages 13 and up (rooms, friends, duels): for a player who may be younger,
 * a panel stands in for it, with a way back. Unlike OfflineGate the screen behind is never composed, so nothing it would
 * load or send happens at all.
 */
@Composable
fun ChildGate(onBack: () -> Unit, content: @Composable () -> Unit) {
    if (!LocalChildMode.current) {
        content()
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2A1708))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        NinePatch(
            res = R.drawable.qm_pad,
            slicePx = 120,
            edge = 26.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 34.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.child_locked_title),
                    style = PaintedStyle(color = Color(0xFF2B1A10), fontSize = 24.sp, textAlign = TextAlign.Center)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.child_locked_body),
                    style = DescriptionStyle(15.sp, 20.sp)
                )
                Spacer(Modifier.height(16.dp))
                PaintedPillButton(
                    text = stringResource(R.string.cd_back),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            }
        }
    }
}

/** One part of the date of birth: its name above a short numbers-only field. */
@Composable
private fun DatePart(
    label: String,
    hint: String,
    value: String,
    maxLength: Int,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        AppTextField(
            value = value,
            onValueChange = { v -> if (v.length <= maxLength && v.all(Char::isDigit)) onValue(v) },
            placeholder = hint,
            centered = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = fieldModifier.fillMaxWidth()
        )
    }
}
