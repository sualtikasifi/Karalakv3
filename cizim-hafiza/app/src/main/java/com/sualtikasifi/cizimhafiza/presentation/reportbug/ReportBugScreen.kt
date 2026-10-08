package com.sualtikasifi.cizimhafiza.presentation.reportbug

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.BugReport

import androidx.compose.foundation.layout.wrapContentWidth

import androidx.compose.ui.draw.drawBehind

import com.sualtikasifi.cizimhafiza.presentation.common.pressable

import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.presentation.common.ButtonOrange
import com.sualtikasifi.cizimhafiza.presentation.common.ChoicePill
import com.sualtikasifi.cizimhafiza.presentation.common.DescriptionStyle
import com.sualtikasifi.cizimhafiza.presentation.common.InkBrown
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.NinePatch
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPage
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.BugReport
import com.sualtikasifi.cizimhafiza.domain.model.BugReportCategory
import com.sualtikasifi.cizimhafiza.presentation.common.IconWell
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedIconButton
import com.sualtikasifi.cizimhafiza.presentation.common.ScreenTopActions
import com.sualtikasifi.cizimhafiza.presentation.common.TopActionsClearance
import com.sualtikasifi.cizimhafiza.presentation.common.SectionLabel
import com.sualtikasifi.cizimhafiza.presentation.common.SelectableChip
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.presentation.common.AppTextField
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.util.asString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_DESCRIPTION_LENGTH = 2000

@Composable
fun ReportBugScreen(
    onBack: () -> Unit,
    viewModel: ReportBugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val myReports by viewModel.myReports.collectAsState()

    // Delete is irreversible, so both the single-row and "delete all" taps
    // stage a confirmation rather than firing immediately.
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var deleteAllRequested by remember { mutableStateOf(false) }

    // The page is the workshop room with the cards, buttons and labels of the design laid over it as drawn pieces
    // (rp_* in drawable-nodpi, cut from the design sheet). It scrolls when it has to: the form fits a normal phone, the
    // history of earlier reports grows below it.
    val ink = Color(0xFF3B2314)
    val canSend = uiState.description.isNotBlank() && !uiState.isSubmitting
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = com.sualtikasifi.cizimhafiza.presentation.common.cachedPainterResource(R.drawable.bg_report),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The sign: both dogs leaning on the plank, the title lettered across it in two lines.
            BoxWithConstraints(Modifier.fillMaxWidth(0.86f).padding(top = 6.dp)) {
                val signW = maxWidth
                Image(
                    painter = painterResource(R.drawable.rp_sign),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                val words = stringResource(R.string.report_bug_title).split(' ')
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = signW * 0.085f)
                        .fillMaxWidth(0.72f)
                        .height(signW * 0.31f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LetteredText(
                        text = words.first(),
                        size = (signW.value * 0.17f).sp,
                        fill = Color.White,
                        outline = Color(0xFF5A2815),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        minScale = 0.45f
                    )
                    if (words.size > 1) LetteredText(
                        text = words.drop(1).joinToString(" "),
                        size = (signW.value * 0.17f).sp,
                        outline = Color(0xFF5A2815),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        minScale = 0.45f
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // What this form is for.
            NinePatch(res = R.drawable.rp_card_big, slicePx = 110, sliceYPx = 110, edge = 34.dp, edgeY = 34.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.report_bug_intro_title),
                        style = PaintedStyle(color = ink, fontSize = 19.sp, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.report_bug_intro_body),
                        style = DescriptionStyle(14.sp, 20.sp).copy(color = ink, textAlign = TextAlign.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            FormLabel(stringResource(R.string.report_bug_category_label), Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                CategoryPill(
                    label = stringResource(R.string.report_bug_category_suggestion),
                    res = R.drawable.rp_pill_orange,
                    selected = uiState.category == BugReportCategory.SUGGESTION,
                    onClick = { viewModel.onCategorySelected(BugReportCategory.SUGGESTION) },
                    modifier = Modifier.weight(1f)
                )
                CategoryPill(
                    label = stringResource(R.string.report_bug_category_complaint),
                    res = R.drawable.rp_pill_blue,
                    selected = uiState.category == BugReportCategory.COMPLAINT,
                    onClick = { viewModel.onCategorySelected(BugReportCategory.COMPLAINT) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            FormLabel(stringResource(R.string.report_bug_description_label), Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(4.dp))
            NinePatch(
                res = R.drawable.rp_card_msg, slicePx = 120, sliceYPx = 96, edge = 44.dp, edgeY = 36.dp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp)
            ) {
                BasicTextField(
                    value = uiState.description,
                    onValueChange = { if (it.length <= MAX_DESCRIPTION_LENGTH) viewModel.onDescriptionChanged(it) },
                    textStyle = PaintedStyle(color = InkBrown, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                    cursorBrush = SolidColor(ButtonOrange),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(horizontal = 30.dp, vertical = 26.dp),
                    decorationBox = { inner ->
                        Box {
                            if (uiState.description.isEmpty()) {
                                Text(
                                    stringResource(R.string.report_bug_placeholder),
                                    style = PaintedStyle(color = Color(0xFF9C8F82), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start)
                                )
                            }
                            inner()
                        }
                    }
                )
            }
            Text(
                text = stringResource(R.string.report_bug_char_count_format, uiState.description.length, MAX_DESCRIPTION_LENGTH),
                style = PaintedStyle(color = Color(0xFFFFEBC8), fontSize = 13.sp, textAlign = TextAlign.End, shadow = androidx.compose.ui.graphics.Shadow(Color(0xCC2A1005), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)),
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, end = 10.dp)
            )
            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.asString(),
                    style = PaintedStyle(color = Color(0xFFFFD6D0), fontSize = 14.sp, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC3B1E08), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            SendButton(
                text = stringResource(if (uiState.isSubmitting) R.string.report_bug_sending else R.string.report_bug_submit),
                enabled = canSend,
                busy = uiState.isSubmitting,
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            if (myReports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormLabel(stringResource(R.string.report_bug_history_title), Modifier)
                    Text(
                        text = stringResource(R.string.report_bug_delete_all),
                        style = PaintedStyle(color = Color(0xFFD63A2E), fontSize = 14.sp, textAlign = TextAlign.End),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xF2FFF6EE))
                            .pressable(pressedScale = 0.92f) { deleteAllRequested = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                myReports.forEach { report ->
                    ReportHistoryCard(report, onDelete = { pendingDeleteId = report.id })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedBackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
    if (uiState.isSubmitted) {
        com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog(
            title = stringResource(R.string.report_bug_success),
            onDismiss = viewModel::dismissSuccess,
            buttons = { com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton(text = stringResource(R.string.close), onClick = viewModel::dismissSuccess, modifier = Modifier.fillMaxWidth()) }
        ) {
            IconWell(icon = Icons.Filled.CheckCircle, tint = AppTheme.tokens.success)
        }
    }
    pendingDeleteId?.let { id ->
        ConfirmReportDeleteDialog(
            message = stringResource(R.string.report_bug_delete_confirm_message),
            onDismiss = { pendingDeleteId = null },
            onConfirm = {
                viewModel.deleteReport(id)
                pendingDeleteId = null
            }
        )
    }
    if (deleteAllRequested) {
        ConfirmReportDeleteDialog(
            message = stringResource(R.string.report_bug_delete_all_confirm_message),
            onDismiss = { deleteAllRequested = false },
            onConfirm = {
                viewModel.deleteAllReports()
                deleteAllRequested = false
            }
        )
    }
}

/** Confirm/cancel dialog for an irreversible delete — one report, or the whole history. */
@Composable
private fun ConfirmReportDeleteDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    com.sualtikasifi.cizimhafiza.presentation.common.PaintedConfirmDialog(
        title = stringResource(R.string.report_bug_delete_confirm_title),
        message = message,
        confirmText = stringResource(R.string.report_bug_delete_confirm_action),
        dismissText = stringResource(R.string.report_bug_delete_confirm_cancel),
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun ReportHistoryCard(report: BugReport, onDelete: () -> Unit) {
    val dateFormat = remember(report.submittedAtMillis) { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    NinePatch(res = R.drawable.league_card, slicePx = 100, edge = 20.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TintedBadge(
                        text = stringResource(
                            if (report.category == BugReportCategory.SUGGESTION) {
                                R.string.report_bug_category_suggestion
                            } else {
                                R.string.report_bug_category_complaint
                            }
                        )
                    )
                    Text(
                        text = dateFormat.format(Date(report.submittedAtMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RaisedIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.report_bug_delete),
                    onClick = onDelete,
                    size = 32.dp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (report.isSeen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = AppTheme.tokens.success,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.report_bug_seen),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.tokens.success
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.report_bug_not_seen_yet),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The send button: the design's orange pill in its leaf wreath. With nothing written yet it goes grey and still —
 * solid and legible rather than see-through, so it reads as "not yet", not as broken.
 */
@Composable
private fun SendButton(text: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    NinePatch(
        res = R.drawable.rp_pill_orange,
        slicePx = 120,
        sliceYPx = 90,
        edge = 44.dp,
        edgeY = 32.dp,
        tint = if (enabled) null else androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.12f) }
        ),
        modifier = modifier
            .height(82.dp)
            .pressable(enabled = enabled, pressedScale = 0.95f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (busy) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            LetteredText(text, 25.sp, outline = if (enabled) Color(0xFF8A3A00) else Color(0xFF6E6A66), modifier = Modifier.wrapContentWidth())
        }
    }
}

/** One of the two category pills: the design's orange (Öneri) or blue (Şikayet); the one not chosen is greyed and a touch smaller. */
@Composable
private fun CategoryPill(label: String, res: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by androidx.compose.animation.core.animateFloatAsState(if (selected) 1f else 0.94f, label = "pillSel")
    NinePatch(
        res = res,
        slicePx = 120,
        sliceYPx = 90,
        edge = 40.dp,
        edgeY = 28.dp,
        tint = if (selected) null else androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.2f) }
        ),
        modifier = modifier
            .height(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (selected) 1f else 0.85f }
            .pressable(pressedScale = 0.94f, onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
            LetteredText(label, 21.sp, outline = Color(0xFF3A1A06), modifier = Modifier.fillMaxWidth(), minScale = 0.6f)
        }
    }
}

/** A section label on the design's little wooden plank ("Bildirim Türü", "Mesajın"). */
@Composable
private fun FormLabel(text: String, modifier: Modifier = Modifier) {
    NinePatch(res = R.drawable.rp_label, slicePx = 36, sliceYPx = 36, edge = 12.dp, edgeY = 12.dp, modifier = modifier.wrapContentWidth()) {
        LetteredText(text, 17.sp, outline = Color(0xFF4A2410), modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp).wrapContentWidth())
    }
}
