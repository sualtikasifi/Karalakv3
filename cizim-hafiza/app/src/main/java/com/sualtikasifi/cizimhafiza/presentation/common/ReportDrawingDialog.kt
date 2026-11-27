package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.DrawingReportReason
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme

/** Where a report has got to, so the button can say so rather than looking dead. */
enum class ReportSendState { Idle, Sending, Sent, Failed }

/**
 * The in-app way to report somebody else's drawing.
 *
 * Offered wherever another player's work is shown to someone who did not
 * make it — the Hızlı Eşleş opponent gallery and an online room's results.
 * That is a Play requirement for an app that shows user-generated content to
 * strangers, and it is also the only source of real labelled examples for
 * [com.sualtikasifi.cizimhafiza.domain.model.WrittenWordDetector], which is
 * currently tuned against synthesised handwriting.
 *
 * Three reasons and no free-text box. A typed explanation would need reading
 * before it could be acted on, and nobody is reading these at this scale —
 * the whole point is that two reports retire a round by themselves. Fixed
 * reasons are also what makes the reports countable later.
 */
@Composable
fun ReportDrawingDialog(
    word: String,
    sendState: ReportSendState,
    onReport: (DrawingReportReason) -> Unit,
    onDismiss: () -> Unit
) {
    ReportReasonsWindow(
        title = stringResource(R.string.report_drawing_title),
        subtitle = stringResource(R.string.report_drawing_subtitle, word),
        sendState = sendState,
        reasons = listOf(
            stringResource(R.string.report_reason_written) to { onReport(DrawingReportReason.WRITTEN) },
            stringResource(R.string.report_reason_offensive) to { onReport(DrawingReportReason.OFFENSIVE) },
            stringResource(R.string.report_reason_meaningless) to { onReport(DrawingReportReason.MEANINGLESS) }
        ),
        onDismiss = onDismiss
    )
}

/**
 * The in-app way to report another player — their name or their behaviour — from the places strangers meet: the league
 * table and the room lobby (see util/PlayerReporter). Fixed reasons, no free text, like [ReportDrawingDialog].
 */
@Composable
fun ReportPlayerDialog(
    nickname: String,
    sendState: ReportSendState,
    onReport: (com.sualtikasifi.cizimhafiza.domain.model.PlayerReportReason) -> Unit,
    onDismiss: () -> Unit
) {
    ReportReasonsWindow(
        title = stringResource(R.string.report_player_title),
        subtitle = stringResource(R.string.report_player_ask),
        chipName = nickname,
        note = stringResource(R.string.report_player_note),
        sendState = sendState,
        reasons = listOf(
            stringResource(R.string.report_player_reason_name) to { onReport(com.sualtikasifi.cizimhafiza.domain.model.PlayerReportReason.NAME) },
            stringResource(R.string.report_player_reason_behaviour) to { onReport(com.sualtikasifi.cizimhafiza.domain.model.PlayerReportReason.BEHAVIOUR) },
            stringResource(R.string.report_player_reason_cheating) to { onReport(com.sualtikasifi.cizimhafiza.domain.model.PlayerReportReason.CHEATING) },
            stringResource(R.string.report_player_reason_other) to { onReport(com.sualtikasifi.cizimhafiza.domain.model.PlayerReportReason.OTHER) }
        ),
        onDismiss = onDismiss
    )
}

/**
 * The window both reports share: the game's painted window (wooden frame, lettered title) with the reasons as painted
 * pills and a quiet "Vazgeç" under them, instead of a white card. While sending it shows a spinner, afterwards a tick.
 */
@Composable
private fun ReportReasonsWindow(
    title: String,
    subtitle: String,
    sendState: ReportSendState,
    chipName: String? = null,
    note: String? = null,
    reasons: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit
) {
    PaintedDialog(
        title = title,
        onDismiss = onDismiss,
        buttons = {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(bottom = 10.dp), contentAlignment = Alignment.Center) {
                PaintedPillButton(
                    text = stringResource(if (sendState == ReportSendState.Sent) R.string.close else R.string.report_drawing_cancel),
                    onClick = onDismiss,
                    // orange, the game's colour for a window's own button: it stands apart from the cream reasons above it
                    primary = true,
                    height = 46.dp,
                    textSize = 17.sp,
                    modifier = Modifier.fillMaxWidth(0.58f).softShadow()
                )
            }
        }
    ) {
        if (chipName != null) {
            ReportNameTag(chipName)
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            color = Color(0xFF3A2416),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))
        when (sendState) {
            ReportSendState.Sending -> CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFFE9801D))
            ReportSendState.Sent -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = AppTheme.tokens.success)
                Text(
                    text = stringResource(R.string.report_drawing_sent),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = Color(0xFF3A2416)
                )
            }
            else -> Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                reasons.forEach { (label, onClick) ->
                    PaintedPillButton(text = label, onClick = onClick, primary = false, height = 44.dp, textSize = 16.sp, modifier = Modifier.fillMaxWidth().softShadow())
                }
                if (note != null) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A5A40),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
                if (sendState == ReportSendState.Failed) {
                    Text(
                        text = stringResource(R.string.report_drawing_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** The reported player's name on a small tag with a flag, so the window says at once who it is about. */
@Composable
private fun ReportNameTag(name: String) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .softShadow()
            .background(Color(0xFFFFE9C7), shape)
            .border(1.5.dp, Color(0xFFF0B24E), shape)
            .padding(start = 8.dp, end = 14.dp, top = 5.dp, bottom = 5.dp)
    ) {
        Icon(Icons.Filled.Flag, contentDescription = null, tint = Color(0xFFD9483B), modifier = Modifier.size(18.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            color = Color(0xFF3A2416),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
