package com.sualtikasifi.cizimhafiza.presentation.reports

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.BugReportCategory
import com.sualtikasifi.cizimhafiza.domain.model.BugReportEntry
import com.sualtikasifi.cizimhafiza.domain.model.DrawingReportReason
import com.sualtikasifi.cizimhafiza.domain.model.LeagueReward
import com.sualtikasifi.cizimhafiza.domain.model.PendingRun
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.domain.repository.XpEvent
import com.sualtikasifi.cizimhafiza.presentation.common.ReplayableDrawing
import com.sualtikasifi.cizimhafiza.presentation.common.StrokeCanvas
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Console
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleBackdrop
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleButtonStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleCard
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleChip
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleEmpty
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNavBar
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNavItem
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleNotice
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSection
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSkeleton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleSurface
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTextField
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTheme
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleTopBar
import com.sualtikasifi.cizimhafiza.presentation.reports.console.HeaderIconButton
import com.sualtikasifi.cizimhafiza.presentation.reports.console.IdentityChip
import com.sualtikasifi.cizimhafiza.presentation.reports.console.NoticeKind
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Overline
import com.sualtikasifi.cizimhafiza.presentation.reports.console.Tag
import com.sualtikasifi.cizimhafiza.presentation.reports.console.ToolTile
import com.sualtikasifi.cizimhafiza.presentation.reports.console.pressScale
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.util.DrawingVideoExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The developer panel: what players have reported, with the drawings actually drawn, and every tool for running the
 * live game.
 *
 * This is the whole reason the reports live somewhere the app can read rather than only in Firestore: `strokesJson`
 * is an unreadable wall of numbers in a console, and a report you cannot look at is a report you cannot judge. The
 * app already knows how to render strokes, so the cheapest possible review surface is the app itself.
 *
 * The frame is the console's (see console/ConsoleShell): a dark band with where you are and whether this phone is the
 * reviewer, a light sheet with the work, and a bottom bar for the four main places plus the tools drawer. Moving into
 * a tool slides sideways, moving between main places fades, so the panel always says which way you went.
 */
@Composable
fun DrawingReportsScreen(
    onBack: () -> Unit,
    /**
     * Bot İsimleri lives behind the same passcode gate as this screen (see DrawingReportsGate) rather than a second
     * gate of its own; it is one more tile in the tools drawer.
     */
    onBotNames: () -> Unit = {},
    viewModel: DrawingReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // A drawing opened on its own, to be watched and possibly kept. Held here rather than in the ViewModel: it is a
    // local view of data the screen already has, and it must not survive the screen.
    var previewItem by remember { mutableStateOf<ResultItem?>(null) }

    // Delete is irreversible, so both the single-row and "delete all" taps stage a confirmation rather than firing.
    var pendingDeleteFeedbackId by remember { mutableStateOf<String?>(null) }
    var pendingUpholdReport by remember { mutableStateOf<ReportedDrawing?>(null) }
    var deleteAllFeedbackRequested by remember { mutableStateOf(false) }
    // The queue opens as a swipe deck; the old list stays one tap away.
    var queueListMode by rememberSaveable { mutableStateOf(false) }
    // A special day tapped on the dashboard, waiting for the assistant to plan it.
    var pendingPlan by remember { mutableStateOf<com.sualtikasifi.cizimhafiza.presentation.reports.console.SpecialDay?>(null) }
    var identityShown by remember { mutableStateOf(false) }
    // The system back key and the arrow at the top are one and the same: inside a tool they go up to the tool list,
    // and from anywhere else they ask before leaving, because leaving locks the panel again and the passcode has to
    // be typed anew.
    var exitAsk by remember { mutableStateOf(false) }
    val place = Place(uiState.tab, uiState.tab == ReportsTab.Queue && queueListMode)
    val goBack: () -> Unit = {
        if (place.depth > 0) {
            if (uiState.tab == ReportsTab.Queue) queueListMode = false
            viewModel.selectTab(ReportsTab.Tools)
        } else {
            exitAsk = true
        }
    }
    BackHandler(onBack = goBack)

    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    ConsoleTheme {
        Box(modifier = Modifier.fillMaxSize().background(Console.HeaderBottom)) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Box {
                    ConsoleBackdrop(Modifier.matchParentSize())
                    ConsoleTopBar(
                        title = place.title,
                        overline = if (place.depth > 0) "Araçlar" else "Geliştirici paneli",
                        onBack = goBack,
                        backDescription = if (place.depth > 0) "Araçlara dön" else "Panelden çık"
                    ) {
                        // Only where there is a whole inbox to clear at once.
                        if (uiState.tab == ReportsTab.Feedback && uiState.feedback.isNotEmpty()) {
                            HeaderIconButton(Icons.Filled.DeleteSweep, stringResource(R.string.reports_feedback_delete_all), { deleteAllFeedbackRequested = true })
                        }
                        if (place.legacyList) {
                            HeaderIconButton(Icons.Filled.Refresh, stringResource(R.string.reports_refresh), viewModel::refresh)
                        }
                        uiState.identity?.let { who -> IdentityChip(ok = who.isReviewer, onClick = { identityShown = true }) }
                    }
                }

                // the sheet: light, rounded at the top, over the dark band
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = Console.SheetCorner, topEnd = Console.SheetCorner))
                        .background(Console.Bg)
                ) {
                    // A refused decision is a line above the list, never a replacement for it. Blanking the screen on
                    // a failed write made a tap that did nothing look like the rounds had been deleted.
                    AnimatedVisibility(
                        visible = uiState.decisionFailed,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        ConsoleNotice(
                            uiState.decisionError ?: stringResource(R.string.reports_decision_failed),
                            kind = NoticeKind.Error,
                            modifier = Modifier.padding(start = Console.Gutter, end = Console.Gutter, top = 12.dp)
                        )
                    }
                    AnimatedContent(
                        targetState = place,
                        transitionSpec = {
                            val deeper = targetState.depth > initialState.depth
                            val shallower = targetState.depth < initialState.depth
                            val move = tween<androidx.compose.ui.unit.IntOffset>(300, easing = FastOutSlowInEasing)
                            when {
                                deeper -> (fadeIn(tween(220, 40)) + slideInHorizontally(move) { it / 4 })
                                    .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(move) { -it / 10 })
                                shallower -> (fadeIn(tween(220, 40)) + slideInHorizontally(move) { -it / 10 })
                                    .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(move) { it / 4 })
                                else -> (fadeIn(tween(240, 60)) + scaleIn(tween(280, 40), initialScale = 0.985f) + slideInVertically(move) { it / 40 })
                                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(move) { -it / 60 })
                            }.using(SizeTransform(clip = false))
                        },
                        label = "panelPlace",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { at ->
                        Box(Modifier.fillMaxSize().padding(start = Console.Gutter, end = Console.Gutter, top = 14.dp)) {
                            PlaceContent(
                                at = at,
                                uiState = uiState,
                                viewModel = viewModel,
                                onPreview = { previewItem = it },
                                onQueueList = { queueListMode = it },
                                onPlan = { day -> pendingPlan = day; viewModel.selectTab(ReportsTab.Assistant) },
                                pendingPlan = pendingPlan,
                                onPlanConsumed = { pendingPlan = null },
                                onBotNames = onBotNames,
                                onDeleteFeedback = { pendingDeleteFeedbackId = it },
                                onUphold = { pendingUpholdReport = it }
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = !imeOpen, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    ConsoleNav(
                        tab = uiState.tab,
                        queueBadge = uiState.queue.runs.size,
                        onSelect = { t -> if (t == ReportsTab.Queue) queueListMode = false; viewModel.selectTab(t) }
                    )
                }
            }

            uiState.renaming?.let { run ->
                RenameDialog(run = run, onDismiss = viewModel::cancelRename, onConfirm = { viewModel.rename(run, it) })
            }

            previewItem?.let { item -> DrawingPreviewDialog(item = item, onDismiss = { previewItem = null }) }

            pendingUpholdReport?.let { entry ->
                val (xp, gold) = com.sualtikasifi.cizimhafiza.domain.model.Moderation.reportPenalty(entry.report.reason, 1)
                AlertDialog(
                    onDismissRequest = { pendingUpholdReport = null },
                    title = { Text(stringResource(R.string.reports_uphold_title)) },
                    text = { Text(stringResource(R.string.reports_uphold_body, xp, gold)) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.upholdReport(entry)
                            pendingUpholdReport = null
                        }) { Text(stringResource(R.string.reports_uphold_confirm), color = Console.Critical) }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingUpholdReport = null }) { Text(stringResource(R.string.reports_feedback_delete_confirm_cancel)) }
                    }
                )
            }
            pendingDeleteFeedbackId?.let { id ->
                ConfirmDeleteDialog(
                    title = stringResource(R.string.reports_feedback_delete_confirm_title),
                    message = stringResource(R.string.reports_feedback_delete_confirm_message),
                    onDismiss = { pendingDeleteFeedbackId = null },
                    onConfirm = {
                        viewModel.deleteReport(id)
                        pendingDeleteFeedbackId = null
                    }
                )
            }
            if (deleteAllFeedbackRequested) {
                ConfirmDeleteDialog(
                    title = stringResource(R.string.reports_feedback_delete_confirm_title),
                    message = stringResource(R.string.reports_feedback_delete_all_confirm_message),
                    onDismiss = { deleteAllFeedbackRequested = false },
                    onConfirm = {
                        viewModel.deleteAllFeedback()
                        deleteAllFeedbackRequested = false
                    }
                )
            }
            if (identityShown) {
                uiState.identity?.let { who ->
                    AlertDialog(
                        onDismissRequest = { identityShown = false },
                        title = { Text(if (who.isReviewer) "Yetkili hesap" else "Yetkisiz hesap") },
                        text = {
                            Text(
                                if (who.isReviewer) stringResource(R.string.reports_identity_ok, who.email.orEmpty())
                                else stringResource(R.string.reports_identity_wrong, who.uid.orEmpty())
                            )
                        },
                        confirmButton = { TextButton(onClick = { identityShown = false }) { Text("Tamam") } }
                    )
                }
            }
            if (exitAsk) {
                AlertDialog(
                    onDismissRequest = { exitAsk = false },
                    title = { Text("Geliştirici panelinden çıkılsın mı?") },
                    text = { Text("Çıkarsan panel yeniden kilitlenir, şifreyi tekrar girmen gerekir.") },
                    confirmButton = { TextButton(onClick = { exitAsk = false; onBack() }) { Text("Çık", color = Console.Critical) } },
                    dismissButton = { TextButton(onClick = { exitAsk = false }) { Text("Kal") } }
                )
            }
        }
    }
}

/**
 * Where the panel is: a tab, and for the queue whether it is the deck or the classic list. [depth] is 0 for the four
 * main places and the tools drawer, 1 for anything opened from the drawer — what decides which way a change slides.
 */
private data class Place(val tab: ReportsTab, val queueList: Boolean) {
    val legacyList: Boolean get() = tab.isLegacyTool() || (tab == ReportsTab.Queue && queueList)
    val depth: Int get() = if (legacyList || tab == ReportsTab.Studio) 1 else 0
    val title: String get() = when (tab) {
        ReportsTab.Dashboard -> "Genel bakış"
        ReportsTab.Queue -> if (queueList) "Kuyruk (liste)" else "Onay kuyruğu"
        ReportsTab.Stats -> "İstatistik"
        ReportsTab.Assistant -> "Asistan"
        ReportsTab.Tools -> "Araçlar"
        ReportsTab.Studio -> "Video Stüdyosu"
        else -> TOOLS.firstOrNull { it.tab == tab }?.title ?: "Geliştirici paneli"
    }
}

/** One place's content, drawn from [at] rather than the live tab, so a place sliding out keeps showing itself. */
@Composable
private fun PlaceContent(
    at: Place,
    uiState: DrawingReportsUiState,
    viewModel: DrawingReportsViewModel,
    onPreview: (ResultItem) -> Unit,
    onQueueList: (Boolean) -> Unit,
    onPlan: (com.sualtikasifi.cizimhafiza.presentation.reports.console.SpecialDay) -> Unit,
    pendingPlan: com.sualtikasifi.cizimhafiza.presentation.reports.console.SpecialDay?,
    onPlanConsumed: () -> Unit,
    onBotNames: () -> Unit,
    onDeleteFeedback: (String) -> Unit,
    onUphold: (ReportedDrawing) -> Unit
) {
    val tab = at.tab
    when {
        tab == ReportsTab.Dashboard -> com.sualtikasifi.cizimhafiza.presentation.reports.console.DashboardPanel(
            onOpenQueue = { onQueueList(false); viewModel.selectTab(ReportsTab.Queue) },
            onOpenTool = { key ->
                viewModel.selectTab(
                    when (key) {
                        "daily" -> ReportsTab.DailyChallenge
                        "reports" -> ReportsTab.Reports
                        else -> ReportsTab.Feedback
                    }
                )
            },
            onOpenStats = { viewModel.selectTab(ReportsTab.Stats) },
            onPlanEvent = onPlan
        )

        tab == ReportsTab.Assistant -> com.sualtikasifi.cizimhafiza.presentation.reports.console.AssistantPanel(
            pendingPlan = pendingPlan,
            onPlanConsumed = onPlanConsumed
        )

        tab == ReportsTab.Tools -> ToolsGrid(
            onOpen = { viewModel.selectTab(it) },
            onQueueList = { onQueueList(true); viewModel.selectTab(ReportsTab.Queue) },
            onBotNames = onBotNames,
            queueCount = uiState.queue.runs.size
        )

        tab == ReportsTab.Queue && !at.queueList -> com.sualtikasifi.cizimhafiza.presentation.reports.console.SwipeQueue(
            runs = uiState.queue.runs,
            loading = uiState.queue.loading,
            endReached = uiState.queue.endReached,
            failures = uiState.swipeFailures,
            failedIds = uiState.swipeFailedIds,
            done = uiState.swipeDone,
            onNeedMore = { viewModel.loadMore(ReportsTab.Queue) },
            onDecide = { run, action -> viewModel.swipeDecide(run, action) },
            onPreview = onPreview,
            onRename = { viewModel.startRename(it) },
            onListMode = { onQueueList(true) }
        )

        // A picker, not a list: it has no rows to count, no pages to load and no empty state.
        tab == ReportsTab.League -> LeaguePrizePicker(
            selectedRewardId = uiState.weekRewardId,
            busy = uiState.leagueLoading,
            failed = uiState.leagueFailed,
            onSelect = viewModel::setWeekReward
        )

        tab == ReportsTab.Studio -> com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioPanel()
        tab == ReportsTab.Stats -> StatsPanel()
        tab == ReportsTab.Coupons -> CouponPanel()
        tab == ReportsTab.Locks -> LocksPanel()
        tab == ReportsTab.Runes -> RunesDevPanel()
        tab == ReportsTab.Update -> UpdatePanel()
        tab == ReportsTab.PlayerReports -> PlayerReportsPanel()
        tab == ReportsTab.Players -> PlayersPanel()
        tab == ReportsTab.Economy -> EconomyPanel()
        tab == ReportsTab.Announce -> AnnouncePanel()
        tab == ReportsTab.Push -> PushPanel()
        tab == ReportsTab.Jobs -> JobsPanel()
        tab == ReportsTab.Audit -> AuditPanel()

        // Also not a list: a status card plus start/stop, same shape as the League branch above.
        tab == ReportsTab.XpEvent -> XpEventPanel(
            event = uiState.xpEvent,
            busy = uiState.xpEventLoading,
            failed = uiState.xpEventFailed,
            onStart = viewModel::startXpEvent,
            onStop = viewModel::stopXpEvent
        )

        else -> RunLists(tab, uiState, viewModel, onPreview, onDeleteFeedback, onUphold)
    }
}

/** The tabs that are lists of rows: the queue as a list, the pool, feedback, reports, the detector, daily attempts. */
@Composable
private fun RunLists(
    tab: ReportsTab,
    uiState: DrawingReportsUiState,
    viewModel: DrawingReportsViewModel,
    onPreview: (ResultItem) -> Unit,
    onDeleteFeedback: (String) -> Unit,
    onUphold: (ReportedDrawing) -> Unit
) {
    val list = uiState.listFor(tab)
    val rowCount = when (tab) {
        ReportsTab.Queue, ReportsTab.Pool -> list?.runs?.size ?: 0
        ReportsTab.Feedback -> uiState.feedback.size
        ReportsTab.Reports -> uiState.reports.size
        ReportsTab.Detector -> uiState.refusals.size
        ReportsTab.DailyChallenge -> uiState.dailyChallengeQueue.size
        else -> 0
    }
    val firstLoad = when (tab) {
        ReportsTab.Queue, ReportsTab.Pool -> list?.neverLoaded == true && list.loading
        ReportsTab.Feedback -> uiState.feedbackLoading && !uiState.feedbackLoaded
        ReportsTab.DailyChallenge -> uiState.dailyChallengeLoading && !uiState.dailyChallengeLoaded
        else -> uiState.evidenceLoading && !uiState.evidenceLoaded
    }
    val loadFailed = when (tab) {
        ReportsTab.Queue, ReportsTab.Pool -> list?.failed == true && rowCount == 0
        ReportsTab.Feedback -> uiState.feedbackFailed
        ReportsTab.DailyChallenge -> uiState.dailyChallengeFailed
        else -> uiState.evidenceFailed
    }
    when {
        firstLoad -> ConsoleSkeleton(rows = 4, rowHeight = if (tab == ReportsTab.Feedback) 84.dp else 132.dp)

        loadFailed -> Centered {
            ConsoleEmpty(
                icon = Icons.Filled.CloudOff,
                title = stringResource(R.string.reports_load_failed),
                body = "İnternet bağlantısını ve bu cihazın geliştirici hesabıyla giriş yaptığını kontrol et.",
                action = { ConsoleButton("Tekrar dene", viewModel::refresh, icon = Icons.Filled.Refresh, style = ConsoleButtonStyle.Secondary) }
            )
        }

        rowCount == 0 -> Centered {
            ConsoleEmpty(
                icon = when (tab) {
                    ReportsTab.Feedback -> Icons.Filled.Feedback
                    ReportsTab.Reports -> Icons.Filled.Flag
                    ReportsTab.Detector -> Icons.Filled.Visibility
                    ReportsTab.DailyChallenge -> Icons.Filled.Today
                    ReportsTab.Pool -> Icons.Filled.Pool
                    else -> Icons.Filled.Inbox
                },
                title = stringResource(
                    when (tab) {
                        ReportsTab.Queue -> R.string.reports_queue_empty
                        ReportsTab.Pool -> R.string.reports_pool_empty
                        ReportsTab.Feedback -> R.string.reports_feedback_empty
                        ReportsTab.Reports -> R.string.reports_empty
                        // Not the same "nothing here" at all: an empty detector tab means no round has been refused,
                        // which is either good news or a broken detector.
                        ReportsTab.Detector -> R.string.reports_detector_empty
                        else -> R.string.reports_daily_challenge_empty
                    }
                )
            )
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Console.Gap),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            when (tab) {
                ReportsTab.Queue -> items(uiState.queue.runs, key = { it.id }) { run ->
                    PendingRunRow(
                        run = run,
                        busy = uiState.decidingId != null,
                        onPreview = onPreview,
                        onApprove = { viewModel.approve(run) },
                        onReject = { viewModel.reject(run) },
                        onDismiss = { viewModel.dismiss(run) },
                        onRename = { viewModel.startRename(run) },
                        modifier = Modifier.animateItem()
                    )
                }
                ReportsTab.Pool -> items(uiState.pool.runs, key = { it.id }) { run ->
                    PendingRunRow(
                        run = run,
                        busy = uiState.decidingId != null,
                        onPreview = onPreview,
                        // A round that is already live has one move left. Rejecting it straight from here would take
                        // XP for a round this screen had already passed, so it goes back to the queue and is decided
                        // there.
                        onSendBack = { viewModel.sendBackToQueue(run) },
                        onRename = { viewModel.startRename(run) },
                        modifier = Modifier.animateItem()
                    )
                }
                ReportsTab.Feedback -> items(uiState.feedback, key = { it.report.id }) { entry ->
                    FeedbackRow(
                        entry,
                        onMarkSeen = { viewModel.markSeen(entry.report.id) },
                        onDelete = { onDeleteFeedback(entry.report.id) },
                        modifier = Modifier.animateItem()
                    )
                }
                ReportsTab.Reports -> items(uiState.reports, key = { it.report.id }) { entry ->
                    ReportRow(
                        entry,
                        busy = uiState.decidingReportId != null,
                        onDismiss = { viewModel.dismissReport(entry) },
                        onUphold = { onUphold(entry) },
                        modifier = Modifier.animateItem()
                    )
                }
                ReportsTab.Detector -> items(uiState.refusals, key = { it.event.id }) { RefusalRow(it, Modifier.animateItem()) }
                ReportsTab.DailyChallenge -> items(uiState.dailyChallengeQueue, key = { it.id }) { pending ->
                    PendingDailyChallengeRow(
                        pending = pending,
                        busy = uiState.decidingDailyChallengeId != null,
                        onPreview = onPreview,
                        onApprove = { viewModel.approveDailyChallenge(pending) },
                        onReject = { viewModel.rejectDailyChallenge(pending) },
                        modifier = Modifier.animateItem()
                    )
                }
                else -> Unit
            }

            // The bottom of a run list is what pays for the next three rows: composing this item IS the trigger, so
            // nothing is fetched until somebody scrolls to it.
            if (list != null && !list.endReached) {
                item(key = "load-more") {
                    LaunchedEffect(list.runs.size, list.failed) {
                        if (!list.failed) viewModel.loadMore(tab)
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        if (list.failed) {
                            ConsoleButton(
                                text = stringResource(R.string.reports_load_more),
                                onClick = { viewModel.loadMore(tab) },
                                icon = Icons.Filled.Refresh,
                                style = ConsoleButtonStyle.Secondary
                            )
                        } else {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Console.Accent)
                        }
                    }
                }
            }
        }
    }
}

/** Shared confirm/cancel dialog for an irreversible delete — one report, or a whole inbox. */
@Composable
private fun ConfirmDeleteDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Console.Critical) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.reports_feedback_delete_confirm_action), color = Console.Critical)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reports_feedback_delete_confirm_cancel)) }
        }
    )
}

/**
 * Which cosmetic the global league's top three win this month.
 *
 * The value is read back from the PUBLISHED table rather than the config document, because that is the one players
 * see. A change made here reaches them at the next scheduled rebuild — up to an hour — which the note says out loud
 * rather than leaving the picker looking stuck.
 */
@Composable
private fun LeaguePrizePicker(
    selectedRewardId: String?,
    busy: Boolean,
    failed: Boolean,
    onSelect: (String) -> Unit
) {
    val rewards = LeagueReward.all
    Column(modifier = Modifier.fillMaxSize()) {
        ConsoleNotice(stringResource(R.string.reports_league_hint), kind = NoticeKind.Info, modifier = Modifier.padding(bottom = 10.dp))
        if (failed) ConsoleNotice(stringResource(R.string.reports_load_failed), kind = NoticeKind.Error, modifier = Modifier.padding(bottom = 10.dp))
        if (rewards.isEmpty()) {
            // Reachable today for frames: the plumbing ships before the artwork does (see AvatarFrame.isLeagueReward).
            Centered { ConsoleEmpty(Icons.Filled.EmojiEvents, stringResource(R.string.reports_league_no_rewards)) }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(rewards, key = { it.id }) { reward ->
                val selected = reward.id == selectedRewardId
                ConsoleSurface(
                    border = if (selected) Console.Accent else Console.Border,
                    face = if (selected) Console.AccentSoft else Console.Surface,
                    onClick = if (busy) null else ({ onSelect(reward.id) }),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = if (selected) Console.Accent else Console.InkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = when (reward) {
                                is LeagueReward.Pen -> stringResource(reward.skin.labelRes)
                                // Without the month every frame row reads the same, and there is one per month.
                                is LeagueReward.Frame -> listOfNotNull(
                                    stringResource(R.string.league_reward_kind_frame),
                                    reward.periodLabel
                                ).joinToString(" · ")
                            },
                            style = Console.Subtitle,
                            modifier = Modifier.weight(1f)
                        )
                        RadioDot(selected)
                    }
                }
            }
        }
    }
}

/** A round radio mark: an empty ring, or the accent with a check. */
@Composable
private fun RadioDot(on: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (on) Console.Accent else Console.Surface)
            .then(if (on) Modifier else Modifier.background(Console.Grid).padding(2.dp).clip(CircleShape).background(Console.Surface)),
        contentAlignment = Alignment.Center
    ) {
        if (on) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
    }
}

/**
 * The Developer Panel's control for the app-wide "2x XP etkinliği" (see XpEventRepository): a status card, then a
 * name, a multiplier and a fixed duration, then go. No free-form date/time picker: every duration the reviewer would
 * realistically want (a few hours, a day) is one of these.
 */
@Composable
private fun XpEventPanel(
    event: XpEvent?,
    busy: Boolean,
    failed: Boolean,
    onStart: (multiplier: Double, durationMillis: Long, label: String?) -> Unit,
    onStop: () -> Unit
) {
    val isRunning = event != null && event.active && System.currentTimeMillis() < event.endsAtMillis
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Console.Gap)
    ) {
        if (failed) ConsoleNotice(stringResource(R.string.reports_load_failed), kind = NoticeKind.Error)
        // the state of things, first
        ConsoleCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(isRunning)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Overline(if (isRunning) "Etkinlik sürüyor" else "Durum")
                    Text(
                        text = if (isRunning) {
                            val until = remember(event.endsAtMillis) {
                                SimpleDateFormat("d MMMM, HH:mm", Locale.getDefault()).format(Date(event.endsAtMillis))
                            }
                            (event.label?.takeIf { it.isNotBlank() }?.let { "$it · " } ?: "") +
                                stringResource(R.string.reports_xp_event_active, com.sualtikasifi.cizimhafiza.domain.model.XpAwards.multiplierLabel(event.multiplier), until)
                        } else {
                            stringResource(R.string.reports_xp_event_inactive)
                        },
                        style = Console.Subtitle
                    )
                }
            }
            if (isRunning) {
                Spacer(Modifier.height(12.dp))
                ConsoleButton(
                    stringResource(R.string.reports_xp_event_stop), onStop, Modifier.fillMaxWidth(),
                    enabled = !busy, icon = Icons.Filled.Close, style = ConsoleButtonStyle.DangerSoft, height = 40.dp
                )
            }
        }

        // The event's name, shown to every player on the home screen, and how long it runs. One start button.
        var eventName by remember { mutableStateOf("") }
        val durations = listOf(
            15 * 60 * 1000L to R.string.reports_xp_event_d15,
            30 * 60 * 1000L to R.string.reports_xp_event_d30,
            60 * 60 * 1000L to R.string.reports_xp_event_d60,
            3 * 60 * 60 * 1000L to R.string.reports_xp_event_d180,
            24 * 60 * 60 * 1000L to R.string.reports_xp_event_d1440
        )
        var selectedDuration by remember { mutableStateOf(durations[2].first) }
        // 1.3 and 1.5 are "30 percent more" and "50 percent more"; 2 and 3 are double and triple.
        val multipliers = listOf(1.3, 1.5, 2.0, 3.0)
        var selectedMultiplier by remember { mutableStateOf(2.0) }
        ConsoleSection("Yeni etkinlik", subtitle = stringResource(R.string.reports_xp_event_hint)) {
            ConsoleTextField(
                value = eventName,
                onValueChange = { if (it.length <= 32) eventName = it },
                label = stringResource(R.string.reports_xp_event_name_label),
                placeholder = stringResource(R.string.reports_xp_event_name_hint),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Overline(stringResource(R.string.reports_xp_event_multiplier_label))
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                multipliers.forEach { m ->
                    ConsoleChip(
                        label = com.sualtikasifi.cizimhafiza.domain.model.XpAwards.multiplierLabel(m),
                        selected = selectedMultiplier == m,
                        onClick = { selectedMultiplier = m }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Overline(stringResource(R.string.reports_xp_event_duration_label))
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                durations.forEach { (millis, labelRes) ->
                    ConsoleChip(label = stringResource(labelRes), selected = selectedDuration == millis, onClick = { selectedDuration = millis })
                }
            }
            Spacer(Modifier.height(14.dp))
            ConsoleButton(
                text = stringResource(R.string.reports_xp_event_start),
                onClick = { onStart(selectedMultiplier, selectedDuration, eventName.trim().ifBlank { null }) },
                enabled = !busy,
                loading = busy,
                icon = Icons.Filled.Bolt,
                modifier = Modifier.fillMaxWidth()
            )
        }
        XpPlanSection()
    }
}

/** A status light: green and breathing while something is live, grey when not. */
@Composable
private fun LiveDot(live: Boolean) {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(live) {
        if (!live) { pulse.snapTo(0f); return@LaunchedEffect }
        while (true) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(1400))
        }
    }
    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        if (live) {
            Box(
                Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = 0.4f + 0.6f * pulse.value; scaleY = scaleX; alpha = 1f - pulse.value }
                    .clip(CircleShape)
                    .background(Console.Good)
            )
        }
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (live) Console.Good else Console.InkMuted))
    }
}

@Composable
private fun DrawingPreviewDialog(item: ResultItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler { onDismiss() }
        Box(modifier = Modifier.fillMaxSize().background(Console.HeaderTop.copy(alpha = 0.96f))) {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReplayableDrawing(
                    strokes = item.strokes,
                    penSkin = item.pen(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppTheme.tokens.canvasPaper)
                )
                Text(
                    text = item.word,
                    style = Console.Hero.copy(color = Console.HeaderInk),
                    modifier = Modifier.padding(top = 18.dp, bottom = 18.dp)
                )
                ConsoleButton(
                    text = stringResource(if (exporting) R.string.reports_video_exporting else R.string.reports_video_save),
                    onClick = {
                        exporting = true
                        failed = false
                        scope.launch {
                            DrawingVideoExporter.export(context, item.word, item.strokes)
                                .onSuccess { DrawingVideoExporter.share(context, it) }
                                .onFailure { failed = true }
                            exporting = false
                        }
                    },
                    enabled = !exporting,
                    loading = exporting,
                    icon = Icons.Filled.Videocam,
                    height = 48.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                if (failed) {
                    Text(
                        text = stringResource(R.string.reports_video_failed),
                        style = Console.Small.copy(color = Color(0xFFFF8A80)),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            HeaderIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp)
            )
        }
    }
}

/** A drawing as a small tile on paper, tappable to open it on its own. */
@Composable
private fun DrawingThumb(strokes: List<List<com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint>>, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    StrokeCanvas(
        strokes = strokes,
        modifier = modifier
            .aspectRatio(1f)
            .then(if (onClick != null) Modifier.pressScale(interaction, 0.92f) else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(AppTheme.tokens.canvasPaper)
            .then(if (onClick != null) Modifier.clickable(interaction, ripple(color = Console.Accent), onClick = onClick) else Modifier)
    )
}

@Composable
private fun ReportRow(entry: ReportedDrawing, busy: Boolean, onDismiss: () -> Unit, onUphold: () -> Unit, modifier: Modifier = Modifier) {
    ConsoleCard(modifier = modifier.fillMaxWidth(), padding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DrawingThumb(entry.strokes, Modifier.width(88.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.report.word, style = Console.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Tag(stringResource(entry.report.reason.labelRes()), fg = Console.Critical, bg = Console.CriticalSoft)
                }
                Spacer(modifier = Modifier.height(4.dp))
                // The detector's own verdict, beside the human's. A WRITTEN report scoring low here is a miss worth
                // understanding.
                Text(
                    stringResource(R.string.reports_detector_score, (entry.writingScore * 100).toInt()),
                    style = Console.Label
                )
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.report.submittedAtMillis)) +
                        " · " + entry.report.reportedUid.take(UID_PREVIEW_LENGTH),
                    style = Console.Small,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConsoleButton(
                stringResource(R.string.reports_dismiss), onDismiss, Modifier.weight(1f),
                enabled = !busy, icon = Icons.Filled.Check, height = 38.dp, style = ConsoleButtonStyle.Secondary
            )
            ConsoleButton(
                stringResource(R.string.reports_uphold), onUphold, Modifier.weight(1f),
                enabled = !busy, icon = Icons.Filled.Block, height = 38.dp, style = ConsoleButtonStyle.Danger
            )
        }
    }
}

/**
 * One round waiting to be let into the pool: all ten drawings, then the buttons.
 *
 * The drawings are the whole row. A five-across grid fits ten thumbnails in two lines without scrolling, which is
 * what makes a queue clearable in a sitting — writing is obvious at a glance, so the decision rarely needs a closer
 * look.
 */
@Composable
private fun PendingRunRow(
    run: PendingRun,
    busy: Boolean,
    onPreview: (ResultItem) -> Unit,
    onApprove: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onSendBack: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ConsoleCard(modifier = modifier.fillMaxWidth(), padding = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .then(if (onRename != null) Modifier.clickable(onClick = onRename) else Modifier)
            ) {
                Text(
                    text = run.nickname,
                    style = Console.Title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onRename != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.reports_rename), tint = Console.InkMuted, modifier = Modifier.size(15.dp))
                }
            }
            Tag(stringResource(R.string.reports_queue_summary, run.level, run.correctCount, run.items.size, run.xpEarned))
        }
        Spacer(modifier = Modifier.height(10.dp))
        ThumbGrid(run.items, onPreview)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onSendBack != null) {
                ConsoleButton(stringResource(R.string.reports_pool_send_back), onSendBack, Modifier.weight(1f), enabled = !busy, icon = Icons.Filled.Undo, height = 38.dp, style = ConsoleButtonStyle.Secondary)
            }
            if (onReject != null) {
                ConsoleButton(stringResource(R.string.reports_queue_reject), onReject, Modifier.weight(1f), enabled = !busy, icon = Icons.Filled.Block, height = 38.dp, style = ConsoleButtonStyle.DangerSoft)
            }
            // Reddet: out of the queue with no penalty and no pool entry.
            if (onDismiss != null) {
                ConsoleButton(stringResource(R.string.reports_queue_dismiss), onDismiss, Modifier.weight(1f), enabled = !busy, icon = Icons.Filled.Close, height = 38.dp, style = ConsoleButtonStyle.Secondary)
            }
            if (onApprove != null) {
                ConsoleButton(stringResource(R.string.reports_queue_approve), onApprove, Modifier.weight(1f), enabled = !busy, icon = Icons.Filled.Check, height = 38.dp)
            }
        }
    }
}

/** Ten drawings in two lines of five; a last line that is short keeps the first line's tile size. */
@Composable
private fun ThumbGrid(items: List<ResultItem>, onPreview: (ResultItem) -> Unit) {
    items.chunked(QUEUE_THUMBS_PER_ROW).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { item -> DrawingThumb(item.strokes, Modifier.weight(1f)) { onPreview(item) } }
            repeat(QUEUE_THUMBS_PER_ROW - row.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

/** Ten drawings in two lines of five — see [PendingRunRow]. */
private const val QUEUE_THUMBS_PER_ROW = 5

/**
 * One finished Günlük Meydan Okuma attempt waiting for review — same shape as [PendingRunRow], simplified: no rename,
 * no pool (see ModerationRepository's daily-challenge doc for why approve is just "leave it" and reject is "take the
 * XP and the streak back").
 */
@Composable
private fun PendingDailyChallengeRow(
    pending: com.sualtikasifi.cizimhafiza.domain.model.PendingDailyChallenge,
    busy: Boolean,
    onPreview: (ResultItem) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConsoleCard(modifier = modifier.fillMaxWidth(), padding = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pending.nickname.ifBlank { pending.uid.take(8) },
                style = Console.Title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Tag(stringResource(R.string.reports_daily_challenge_summary, pending.correctCount, pending.items.size, pending.xpEarned))
        }
        Spacer(modifier = Modifier.height(10.dp))
        ThumbGrid(pending.items, onPreview)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConsoleButton(stringResource(R.string.reports_queue_reject), onReject, Modifier.weight(1f), enabled = !busy, icon = Icons.Filled.Block, height = 38.dp, style = ConsoleButtonStyle.DangerSoft)
            ConsoleButton(stringResource(R.string.reports_queue_approve), onApprove, Modifier.weight(1f), enabled = !busy, icon = Icons.Filled.Check, height = 38.dp)
        }
    }
}

/**
 * One round the detector refused.
 *
 * The score list is the useful part — it shows the SHAPE of the round, which is what the decision was actually made
 * on: four or more high scores is a cheat, and a row of low ones with three high would be a threshold sitting too
 * close to the edge.
 */
@Composable
private fun RefusalRow(entry: RefusedRound, modifier: Modifier = Modifier) {
    ConsoleCard(modifier = modifier.fillMaxWidth(), padding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DrawingThumb(entry.strokes, Modifier.width(88.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.event.sampleWord, style = Console.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Tag(
                        stringResource(R.string.reports_detector_flagged, entry.event.flaggedCount, entry.event.wordCount),
                        fg = Console.Critical, bg = Console.CriticalSoft
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(entry.event.scores.joinToString("  "), style = Console.Label.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                if (entry.event.outcomeLooksRead) {
                    Text(stringResource(R.string.reports_detector_fast), style = Console.Small)
                }
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.event.createdAtMillis)),
                    style = Console.Small.copy(color = Console.InkMuted)
                )
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 40.dp), contentAlignment = Alignment.Center) { content() }
}

private fun DrawingReportReason.labelRes(): Int = when (this) {
    DrawingReportReason.WRITTEN -> R.string.report_reason_written
    DrawingReportReason.OFFENSIVE -> R.string.report_reason_offensive
    DrawingReportReason.MEANINGLESS -> R.string.report_reason_meaningless
}

/** Enough of a uid to tell two reported players apart at a glance. */
private const val UID_PREVIEW_LENGTH = 10

/** The tabs that still render the original panel content (reached from the tools grid). */
private fun ReportsTab.isLegacyTool(): Boolean = this !in setOf(ReportsTab.Dashboard, ReportsTab.Assistant, ReportsTab.Tools, ReportsTab.Studio, ReportsTab.Queue, ReportsTab.Stats)

/** The bottom bar: overview, deck, numbers, assistant, and the drawer of every other tool. */
@Composable
private fun ConsoleNav(tab: ReportsTab, queueBadge: Int, onSelect: (ReportsTab) -> Unit) {
    val key = when (tab) {
        ReportsTab.Dashboard -> "dash"
        ReportsTab.Queue -> "queue"
        ReportsTab.Stats -> "stats"
        ReportsTab.Assistant -> "ai"
        else -> "tools"
    }
    ConsoleNavBar(
        items = listOf(
            ConsoleNavItem("dash", "Pano", Icons.Filled.Dashboard),
            ConsoleNavItem("queue", "Kuyruk", Icons.Filled.Style, badge = queueBadge),
            ConsoleNavItem("stats", "İstatistik", Icons.Filled.Insights),
            ConsoleNavItem("ai", "Asistan", Icons.Filled.AutoAwesome),
            ConsoleNavItem("tools", "Araçlar", Icons.Filled.Apps)
        ),
        selected = key,
        onSelect = { k ->
            onSelect(
                when (k) {
                    "dash" -> ReportsTab.Dashboard
                    "queue" -> ReportsTab.Queue
                    "stats" -> ReportsTab.Stats
                    "ai" -> ReportsTab.Assistant
                    else -> ReportsTab.Tools
                }
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** What a tool is about; each group has its own tint, so the drawer reads as a few families rather than 20 tiles. */
private enum class ToolGroup(val title: String, val tint: Color, val soft: Color) {
    Moderation("İçerik ve moderasyon", Console.Accent, Console.AccentSoft),
    People("Oyuncular", Console.Blue, Console.BlueSoft),
    Live("Canlı operasyon", Console.Violet, Console.VioletSoft),
    Game("Ekonomi ve oyun", Console.Teal, Console.TealSoft),
    System("Sistem", Console.InkSecondary, Console.Grid)
}

/** One tool of the drawer. [tab] is null for one that opens elsewhere (Bot İsimleri), [key] tells the list view of the queue apart. */
private data class ToolSpec(val key: String, val tab: ReportsTab?, val icon: ImageVector, val title: String, val sub: String, val group: ToolGroup)

private val TOOLS = listOf(
    ToolSpec("queueList", null, Icons.AutoMirrored.Filled.List, "Kuyruk (liste)", "Onay kuyruğunun klasik görünümü", ToolGroup.Moderation),
    ToolSpec("pool", ReportsTab.Pool, Icons.Filled.Pool, "Havuz", "Hızlı Eşleş'teki onaylı turlar", ToolGroup.Moderation),
    ToolSpec("daily", ReportsTab.DailyChallenge, Icons.Filled.Today, "Günlük meydan okuma", "Günlük denemelerin incelemesi", ToolGroup.Moderation),
    ToolSpec("reports", ReportsTab.Reports, Icons.Filled.Flag, "Çizim şikayetleri", "Bildirilen çizimler ve karar", ToolGroup.Moderation),
    ToolSpec("detector", ReportsTab.Detector, Icons.Filled.Visibility, "Dedektör", "Otomatik reddedilen turlar", ToolGroup.Moderation),
    ToolSpec("feedback", ReportsTab.Feedback, Icons.Filled.Feedback, "Geri bildirim", "Oyuncu önerileri ve şikayetleri", ToolGroup.Moderation),
    ToolSpec("players", ReportsTab.Players, Icons.Filled.Search, "Oyuncu ara", "Profil, ceza, ad sıfırlama, telafi", ToolGroup.People),
    ToolSpec("playerReports", ReportsTab.PlayerReports, Icons.Filled.Report, "Oyuncu raporları", "Bildirilen oyuncular ve işlem", ToolGroup.People),
    ToolSpec("locks", ReportsTab.Locks, Icons.Filled.Lock, "Kilitler", "Oyuncu kilitleri ve cezalar", ToolGroup.People),
    ToolSpec("botNames", null, Icons.Filled.Badge, "Bot isimleri", "Rakip isim havuzu", ToolGroup.People),
    ToolSpec("announce", ReportsTab.Announce, Icons.Filled.Campaign, "Duyuru", "Ana sayfa duyuru şeridi", ToolGroup.Live),
    ToolSpec("push", ReportsTab.Push, Icons.Filled.Notifications, "Bildirim gönder", "Tüm oyunculara push", ToolGroup.Live),
    ToolSpec("xp", ReportsTab.XpEvent, Icons.Filled.Bolt, "XP etkinliği", "x1.5 / x2 XP kampanyası", ToolGroup.Live),
    ToolSpec("league", ReportsTab.League, Icons.Filled.EmojiEvents, "Lig ödülü", "Bu ayın ilk 3 ödülü", ToolGroup.Live),
    ToolSpec("coupons", ReportsTab.Coupons, Icons.Filled.ConfirmationNumber, "Kuponlar", "Altın / XP / kasa kodları", ToolGroup.Live),
    ToolSpec("economy", ReportsTab.Economy, Icons.Filled.Tune, "Ekonomi", "Altın ve ödül ayarları", ToolGroup.Game),
    ToolSpec("runes", ReportsTab.Runes, Icons.Filled.Diamond, "Rünler", "Rün ayarları", ToolGroup.Game),
    ToolSpec("update", ReportsTab.Update, Icons.Filled.SystemUpdate, "Güncelleme", "Eski sürümlerde oyunu kilitle", ToolGroup.System),
    ToolSpec("jobs", ReportsTab.Jobs, Icons.Filled.Schedule, "Arka plan işleri", "Zamanlanmış işlerin durumu", ToolGroup.System),
    ToolSpec("audit", ReportsTab.Audit, Icons.Filled.History, "Denetim kaydı", "Panelde yapılan işlemler", ToolGroup.System)
)

/**
 * Every tool that is not one of the four main places: the video studio up top (the one used most), then the rest in
 * families, two to a row, with a search box that narrows them as you type. Tiles come in one after another the first
 * time the drawer opens.
 */
@Composable
private fun ToolsGrid(onOpen: (ReportsTab) -> Unit, onQueueList: () -> Unit, onBotNames: () -> Unit, queueCount: Int) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase(Locale("tr"))
    val shown = TOOLS.filter { q.isEmpty() || it.title.lowercase(Locale("tr")).contains(q) || it.sub.lowercase(Locale("tr")).contains(q) }
    val open: (ToolSpec) -> Unit = { t ->
        when {
            t.key == "queueList" -> onQueueList()
            t.key == "botNames" -> onBotNames()
            t.tab != null -> onOpen(t.tab)
        }
    }
    val studioShown = q.isEmpty() || "video stüdyosu reels tiktok".contains(q)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ConsoleTextField(
            value = query,
            onValueChange = { query = it.take(30) },
            placeholder = "Araç ara",
            leading = { Icon(Icons.Filled.Search, contentDescription = null, tint = Console.InkMuted, modifier = Modifier.size(18.dp)) },
            trailing = if (query.isNotEmpty()) ({
                Icon(
                    Icons.Filled.Close, contentDescription = "Temizle", tint = Console.InkMuted,
                    modifier = Modifier.size(18.dp).clip(CircleShape).clickable { query = "" }
                )
            }) else null,
            modifier = Modifier.fillMaxWidth()
        )
        var order = 0
        if (studioShown) Stagger(order++) { StudioFeature { onOpen(ReportsTab.Studio) } }
        ToolGroup.entries.forEach { group ->
            val tools = shown.filter { it.group == group }
            if (tools.isEmpty()) return@forEach
            Overline(group.title, modifier = Modifier.padding(start = 2.dp, top = 8.dp))
            tools.chunked(2).forEach { pair ->
                // both tiles of a row as tall as the taller one, so a two-line name never leaves a ragged row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    pair.forEach { t ->
                        Stagger(order++, Modifier.weight(1f).fillMaxHeight()) {
                            ToolTile(
                                t.icon, t.title, t.sub, Modifier.fillMaxSize(),
                                badge = if (t.key == "queueList" && queueCount > 0) queueCount.toString() else null,
                                tint = group.tint, tintSoft = group.soft,
                                onClick = { open(t) }
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (shown.isEmpty() && !studioShown) {
            ConsoleEmpty(Icons.Filled.Search, "“$query” ile eşleşen araç yok", body = "Başka bir kelime dene.")
        }
    }
}

/** The studio's own wide tile at the top of the drawer, on the same dark as the header. */
@Composable
private fun StudioFeature(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f)
            .clip(RoundedCornerShape(Console.CardCorner))
            .background(Brush.linearGradient(listOf(Console.HeaderTop, Color(0xFF3A2A20))))
            .clickable(interaction, ripple(color = Color.White), onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Console.Accent),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Video Stüdyosu", style = Console.Title.copy(color = Console.HeaderInk))
                Text("Çizimlerden Reels / TikTok videosu üret", style = Console.Small.copy(color = Console.HeaderMuted))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Console.HeaderMuted)
        }
    }
}

/** Fades and lifts [content] in, [index] steps after the drawer opened, so a grid arrives as a short cascade. */
@Composable
private fun Stagger(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(14) * 22L)
        progress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
    }
    Box(modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 10.dp.toPx()
    }) { content() }
}

/**
 * Renames one run's author.
 *
 * Opens on the current name rather than empty: the common edit is a small change to something already there, and an
 * empty box would make every rename a retype.
 */
@Composable
private fun RenameDialog(
    run: PendingRun,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(run.id) { mutableStateOf(run.nickname) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reports_rename)) },
        text = {
            ConsoleTextField(
                value = name,
                onValueChange = { name = it.take(RENAME_MAX_LENGTH) },
                label = stringResource(R.string.reports_rename_label),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name.trim() != run.nickname
            ) { Text(stringResource(R.string.reports_rename_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reports_rename_cancel)) }
        }
    )
}

/** Kept in step with the 40-character cap in firestore.rules. */
private const val RENAME_MAX_LENGTH = 40

/**
 * One "Sorun Bildir" submission.
 *
 * The build and the phone are shown beside the text rather than left in Firestore: they are the first two questions
 * any report raises, and asking them back means a round trip through somebody who has already moved on.
 */
@Composable
private fun FeedbackRow(entry: BugReportEntry, onMarkSeen: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val suggestion = entry.report.category == BugReportCategory.SUGGESTION
    ConsoleCard(modifier = modifier.fillMaxWidth(), padding = 12.dp) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Tag(
                stringResource(if (suggestion) R.string.reports_feedback_suggestion else R.string.reports_feedback_complaint),
                fg = if (suggestion) Console.Blue else Console.Critical,
                bg = if (suggestion) Console.BlueSoft else Console.CriticalSoft
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = listOfNotNull(entry.appVersionName, entry.deviceModel).joinToString(" · "),
                style = Console.Small.copy(color = Console.InkMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!entry.report.isSeen) Box(Modifier.size(8.dp).clip(CircleShape).background(Console.Accent))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = entry.report.description, style = Console.Body)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (entry.report.isSeen) {
                Tag("✓ " + stringResource(R.string.reports_feedback_seen), fg = Console.GoodText, bg = Console.GoodSoft)
                Spacer(Modifier.weight(1f))
            } else {
                ConsoleButton(
                    stringResource(R.string.reports_feedback_mark_seen), onMarkSeen,
                    icon = Icons.Filled.Check, height = 34.dp, style = ConsoleButtonStyle.Tonal
                )
                Spacer(Modifier.weight(1f))
            }
            com.sualtikasifi.cizimhafiza.presentation.reports.console.ConsoleIconButton(
                icon = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.reports_feedback_delete),
                onClick = onDelete,
                size = 34.dp,
                tint = Console.Critical
            )
        }
    }
}
