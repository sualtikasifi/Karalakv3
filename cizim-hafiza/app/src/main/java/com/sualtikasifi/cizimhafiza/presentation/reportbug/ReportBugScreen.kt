package com.sualtikasifi.cizimhafiza.presentation.reportbug

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

    // Scrolls only when it has to: the form fits a normal phone, the history of earlier reports grows below it.
    val cream = Color(0xFFFFEBC8)
    PaintedPage(title = stringResource(R.string.report_bug_title), onBack = onBack) {
        // A short intro card: gives the form a proper "what is this for" framing.
        NinePatch(res = R.drawable.league_card, slicePx = 100, edge = 22.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(42.dp).background(Color(0xFFFFE2B5), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Feedback, contentDescription = null, tint = Color(0xFFF26A1B), modifier = Modifier.size(24.dp)) }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.report_bug_hint),
                    style = DescriptionStyle(14.sp, 19.sp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        FormLabel(stringResource(R.string.report_bug_category_label), cream)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ChoicePill(
                label = stringResource(R.string.report_bug_category_suggestion),
                selected = uiState.category == BugReportCategory.SUGGESTION,
                onClick = { viewModel.onCategorySelected(BugReportCategory.SUGGESTION) },
                modifier = Modifier.weight(1f),
                height = 48.dp,
                textSize = 17.sp
            )
            ChoicePill(
                label = stringResource(R.string.report_bug_category_complaint),
                selected = uiState.category == BugReportCategory.COMPLAINT,
                onClick = { viewModel.onCategorySelected(BugReportCategory.COMPLAINT) },
                modifier = Modifier.weight(1f),
                height = 48.dp,
                textSize = 17.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        FormLabel(stringResource(R.string.report_bug_description_label), cream)
        Spacer(modifier = Modifier.height(6.dp))
        NinePatch(res = R.drawable.league_card, slicePx = 100, edge = 20.dp, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)) {
            BasicTextField(
                value = uiState.description,
                onValueChange = { if (it.length <= MAX_DESCRIPTION_LENGTH) viewModel.onDescriptionChanged(it) },
                textStyle = PaintedStyle(color = InkBrown, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start),
                cursorBrush = SolidColor(ButtonOrange),
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(horizontal = 18.dp, vertical = 16.dp),
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
            style = PaintedStyle(color = cream, fontSize = 12.sp, textAlign = TextAlign.End),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 6.dp)
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
        Spacer(modifier = Modifier.height(10.dp))
        val canSend = uiState.description.isNotBlank() && !uiState.isSubmitting
        NinePatch(
            res = R.drawable.res_btn_claim,
            slicePx = 64,
            sliceYPx = 46,
            edge = 28.dp,
            edgeY = 21.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = if (canSend) 1f else 0.55f }
                .clickable(enabled = canSend, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = viewModel::submit)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                LetteredText(
                    stringResource(if (uiState.isSubmitting) R.string.report_bug_sending else R.string.report_bug_submit),
                    20.sp, outline = Color(0xFF8A3A00)
                )
            }
        }

        if (myReports.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LetteredText(stringResource(R.string.report_bug_history_title), 18.sp)
                Text(
                    text = stringResource(R.string.report_bug_delete_all),
                    style = PaintedStyle(color = Color(0xFFFFC2BA), fontSize = 14.sp, textAlign = TextAlign.End),
                    modifier = Modifier.clickable { deleteAllRequested = true }.padding(8.dp)
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
    if (uiState.isSubmitted) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSuccess,
            confirmButton = {
                PrimaryButton(text = stringResource(R.string.close), onClick = viewModel::dismissSuccess)
            },
            icon = { IconWell(icon = Icons.Filled.CheckCircle, tint = AppTheme.tokens.success) },
            text = {
                Text(
                    text = stringResource(R.string.report_bug_success),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_bug_delete_confirm_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.report_bug_delete_confirm_action),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.report_bug_delete_confirm_cancel)) }
        }
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

/** A section label on the wooden wall: cream lettering with a soft shadow, left aligned. */
@Composable
private fun FormLabel(text: String, color: Color) {
    Text(
        text = text,
        style = PaintedStyle(
            color = color, fontSize = 16.sp, textAlign = TextAlign.Start,
            shadow = androidx.compose.ui.graphics.Shadow(Color(0xAA2A1005), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)
        ),
        modifier = Modifier.padding(start = 4.dp)
    )
}
