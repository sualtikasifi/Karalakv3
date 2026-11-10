package com.sualtikasifi.cizimhafiza.presentation.reports

import com.sualtikasifi.cizimhafiza.domain.model.BugReport
import com.sualtikasifi.cizimhafiza.domain.model.BugReportCategory
import com.sualtikasifi.cizimhafiza.domain.model.BugReportEntry
import com.sualtikasifi.cizimhafiza.domain.model.DetectorEvent
import com.sualtikasifi.cizimhafiza.domain.model.DrawingReport
import com.sualtikasifi.cizimhafiza.domain.model.PendingDailyChallenge
import com.sualtikasifi.cizimhafiza.domain.model.PendingRun
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import com.sualtikasifi.cizimhafiza.domain.model.ReviewerIdentity
import com.sualtikasifi.cizimhafiza.domain.model.RunPage
import com.sualtikasifi.cizimhafiza.domain.model.WrittenWordDetector
import com.sualtikasifi.cizimhafiza.domain.repository.BugReportRepository
import com.sualtikasifi.cizimhafiza.domain.repository.DetectorEventRepository
import com.sualtikasifi.cizimhafiza.domain.repository.DrawingReportRepository
import com.sualtikasifi.cizimhafiza.domain.repository.GlobalLeagueRepository
import com.sualtikasifi.cizimhafiza.domain.repository.ModerationRepository
import com.sualtikasifi.cizimhafiza.domain.repository.XpEvent
import com.sualtikasifi.cizimhafiza.domain.repository.XpEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The review screen's decisions: a round leaves the queue only when its write went through, the badge follows the real
 * count, a refused write raises the banner and changes nothing, bulk actions take only what is picked, and a list that
 * could not be read is a failure, never "nothing here".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DrawingReportsViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun run(id: String) = PendingRun(
        id, "uid-$id", "Oyuncu $id", 5, emptyList<ResultItem>(), totalScore = 10, correctCount = 5, xpEarned = 40, goldEarned = 20, createdAtMillis = 1L
    )

    // ---- fakes: only what the screen calls does anything

    private class FakeModeration(
        var queue: List<PendingRun> = emptyList(), var total: Long = 0, var fail: Boolean = false
    ) : ModerationRepository {
        val calls = mutableListOf<String>()
        private fun result(name: String): Result<Unit> { calls += name; return if (fail) Result.failure(IllegalStateException("denied")) else Result.success(Unit) }
        override fun identity() = ReviewerIdentity("u", "r@x", true)
        override suspend fun pendingRuns(limit: Int, after: Long?) = Result.success(RunPage(queue, null, true))
        override suspend fun pendingRunCount() = Result.success(total)
        override suspend fun poolRuns(limit: Int, after: Long?) = Result.success(RunPage(emptyList(), null, true))
        override suspend fun approve(runId: String) = result("approve $runId")
        override suspend fun rename(runId: String, nickname: String, inPool: Boolean) = result("rename $runId")
        override suspend fun updateIdentity(runId: String, nickname: String, level: Int, frameId: String, photoKey: String?, inPool: Boolean) = result("identity $runId")
        override suspend fun sendBackToQueue(runId: String) = result("sendBack $runId")
        override suspend fun dismiss(runId: String) = result("dismiss $runId")
        override suspend fun reject(runId: String, xpToRevoke: Int, goldToRevoke: Int) = result("reject $runId $xpToRevoke $goldToRevoke")
        override suspend fun submitDailyChallengeForReview(items: List<ResultItem>, score: Int, correctCount: Int, xpEarned: Int, goldEarned: Int) = result("submit")
        override suspend fun pendingDailyChallenges(limit: Int): Result<List<PendingDailyChallenge>> = Result.success(emptyList())
        override suspend fun approveDailyChallenge(id: String) = result("daily-approve $id")
        override suspend fun rejectDailyChallenge(id: String, xpToRevoke: Int, goldToRevoke: Int) = result("daily-reject $id")
        override suspend fun upholdReport(report: DrawingReport) = result("uphold ${report.id}")
        override suspend fun activeLocks() = Result.success(emptyList<com.sualtikasifi.cizimhafiza.domain.model.ActiveLock>())
        override suspend fun cancelLock(lock: com.sualtikasifi.cizimhafiza.domain.model.ActiveLock) = result("cancelLock")
        override suspend fun dismissReport(reportId: String) = result("dismissReport $reportId")
        override suspend fun dismissReports(reportIds: List<String>) = result("dismissReports ${reportIds.sorted()}")
    }

    private class FakeBugReports(var entries: List<BugReportEntry> = emptyList(), var fail: Boolean = false) : BugReportRepository {
        val calls = mutableListOf<String>()
        private fun result(name: String): Result<Unit> { calls += name; return if (fail) Result.failure(IllegalStateException("denied")) else Result.success(Unit) }
        override suspend fun submitReport(description: String, category: BugReportCategory) = result("submit")
        override fun observeMyReports(): Flow<List<BugReport>> = emptyFlow()
        override suspend fun allReports(limit: Int) = Result.success(entries)
        override suspend fun markSeen(reportId: String) = result("seen $reportId")
        override suspend fun deleteReports(reportIds: List<String>) = result("delete")
        override suspend fun reportCount() = Result.success(entries.size.toLong())
        override suspend fun unseenCount() = Result.success(entries.count { !it.report.isSeen }.toLong())
        override suspend fun markSeenMany(reportIds: List<String>) = result("seenMany ${reportIds.sorted()}")
        override suspend fun deleteReportsAudited(reportIds: List<String>) = result("deleteAudited ${reportIds.sorted()}")
        override suspend fun deleteAllReports(): Result<Int> { calls += "deleteAll"; return if (fail) Result.failure(IllegalStateException("denied")) else Result.success(entries.size) }
    }

    private class FakeDrawingReports(var fail: Boolean = false) : DrawingReportRepository {
        override suspend fun reportRunDrawing(runId: String, reportedUid: String, word: String, strokes: List<List<com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint>>, reason: com.sualtikasifi.cizimhafiza.domain.model.DrawingReportReason) = Result.success(Unit)
        override suspend fun reportRoomDrawing(roomCode: String, reportedUid: String, word: String, strokes: List<List<com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint>>, reason: com.sualtikasifi.cizimhafiza.domain.model.DrawingReportReason) = Result.success(Unit)
        override suspend fun isRetired(runId: String) = false
        override suspend fun recentReports(limit: Int): Result<List<DrawingReport>> =
            if (fail) Result.failure(IllegalStateException("denied")) else Result.success(emptyList())
    }

    private class FakeDetector(var fail: Boolean = false) : DetectorEventRepository {
        override fun recordRefusal(verdict: WrittenWordDetector.RoundVerdict, items: List<ResultItem>) = Unit
        override suspend fun recentEvents(limit: Int): Result<List<DetectorEvent>> =
            if (fail) Result.failure(IllegalStateException("denied")) else Result.success(emptyList())
    }

    private class FakeLeague : GlobalLeagueRepository {
        override suspend fun table(forceRefresh: Boolean) = Result.failure<com.sualtikasifi.cizimhafiza.domain.model.GlobalLeagueTable>(IllegalStateException())
        override suspend fun myRank(periodId: Long, myPeriodXp: Int, botsAbove: Int) = Result.success(1)
        override suspend fun setWeekReward(rewardId: String?) = Result.success(Unit)
    }

    private class FakeXp : XpEventRepository {
        override val live: kotlinx.coroutines.flow.StateFlow<XpEvent?> = kotlinx.coroutines.flow.MutableStateFlow(null)
        override fun startListening() = Unit
        override suspend fun currentMultiplier() = 1.0
        override suspend fun current(): Result<XpEvent?> = Result.success(null)
        override suspend fun startEvent(multiplier: Double, durationMillis: Long, label: String?) = Result.success(Unit)
        override suspend fun stopEvent() = Result.success(Unit)
    }

    private fun vm(
        moderation: FakeModeration = FakeModeration(), bugs: FakeBugReports = FakeBugReports(),
        reports: FakeDrawingReports = FakeDrawingReports(), detector: FakeDetector = FakeDetector()
    ) = DrawingReportsViewModel(reports, detector, moderation, bugs, FakeLeague(), FakeXp())

    private fun feedback(id: String, seen: Boolean = false) = BugReportEntry(
        BugReport(id, BugReportCategory.SUGGESTION, "d$id", 1L, if (seen) 5L else null), "u", null, null
    )

    // ---- the queue and its badge

    @Test
    fun theQueueOpensWithItsFirstPageAndTheRealTotal() {
        val m = FakeModeration(queue = listOf(run("a"), run("b"), run("c")), total = 42)
        val v = vm(m)
        assertEquals(3, v.uiState.value.queue.runs.size)
        // the badge is the total, not the three rows read so far
        assertEquals(42L, v.uiState.value.queueTotal)
    }

    @Test
    fun anApprovalMovesTheRoundToThePoolAndLowersTheBadge() {
        val m = FakeModeration(queue = listOf(run("a"), run("b")), total = 10)
        val v = vm(m)
        v.approve(v.uiState.value.queue.runs.first())
        assertEquals(listOf("approve a"), m.calls)
        assertEquals(listOf("b"), v.uiState.value.queue.runs.map { it.id })
        assertEquals(listOf("a"), v.uiState.value.pool.runs.map { it.id })
        assertEquals(9L, v.uiState.value.queueTotal)
        assertFalse(v.uiState.value.decisionFailed)
    }

    @Test
    fun aRejectionTakesBackWhatTheRoundPaidAndKeepsItOutOfThePool() {
        val m = FakeModeration(queue = listOf(run("a")), total = 1)
        val v = vm(m)
        v.reject(v.uiState.value.queue.runs.first())
        assertEquals(listOf("reject a 40 20"), m.calls)
        assertTrue(v.uiState.value.queue.runs.isEmpty())
        assertTrue(v.uiState.value.pool.runs.isEmpty())
        assertEquals(0L, v.uiState.value.queueTotal)
    }

    @Test
    fun aRefusedDecisionChangesNothingAndRaisesTheBanner() {
        val m = FakeModeration(queue = listOf(run("a"), run("b")), total = 2, fail = true)
        val v = vm(m)
        v.approve(v.uiState.value.queue.runs.first())
        val s = v.uiState.value
        assertEquals(listOf("a", "b"), s.queue.runs.map { it.id })
        assertEquals(2L, s.queueTotal)
        assertTrue(s.decisionFailed)
        assertTrue(s.decisionError!!.contains("denied"))
        assertNull(s.decidingId)
    }

    @Test
    fun sendingARoundBackRaisesTheBadgeAgain() {
        val m = FakeModeration(queue = listOf(run("a")), total = 5)
        val v = vm(m)
        v.approve(v.uiState.value.queue.runs.first())
        assertEquals(4L, v.uiState.value.queueTotal)
        v.sendBackToQueue(v.uiState.value.pool.runs.first())
        assertEquals(5L, v.uiState.value.queueTotal)
        assertEquals(listOf("a"), v.uiState.value.queue.runs.map { it.id })
    }

    @Test
    fun theSwipeDeckKeepsAFailedRoundAndCountsADoneOne() {
        val m = FakeModeration(queue = listOf(run("a"), run("b")), total = 2)
        val v = vm(m)
        v.swipeDecide(v.uiState.value.queue.runs[0], SwipeAction.Approve)
        assertEquals(1, v.uiState.value.swipeDone)
        assertEquals(1L, v.uiState.value.queueTotal)
        m.fail = true
        v.swipeDecide(v.uiState.value.queue.runs[0], SwipeAction.Reject)
        val s = v.uiState.value
        assertEquals(listOf("b"), s.queue.runs.map { it.id })
        assertEquals(setOf("b"), s.swipeFailedIds)
        assertEquals(1, s.swipeFailures)
        assertTrue(s.decisionFailed)
        assertEquals(1L, s.queueTotal)
    }

    // ---- feedback

    @Test
    fun deletingAllFeedbackClearsTheInboxAndAFailureIsShown() {
        val bugs = FakeBugReports(entries = listOf(feedback("1"), feedback("2")))
        val v = vm(bugs = bugs)
        v.selectTab(ReportsTab.Feedback)
        assertEquals(2L, v.uiState.value.feedbackTotal)
        v.deleteAllFeedback()
        assertTrue(bugs.calls.contains("deleteAll"))
        assertTrue(v.uiState.value.feedback.isEmpty())
        assertEquals(0L, v.uiState.value.feedbackTotal)

        val failing = FakeBugReports(entries = listOf(feedback("1")), fail = true)
        val v2 = vm(bugs = failing)
        v2.selectTab(ReportsTab.Feedback)
        v2.deleteAllFeedback()
        assertEquals(1, v2.uiState.value.feedback.size)
        assertTrue(v2.uiState.value.decisionFailed)
    }

    @Test
    fun aBulkActionTakesOnlyWhatIsPicked() {
        val bugs = FakeBugReports(entries = listOf(feedback("1"), feedback("2"), feedback("3")))
        val v = vm(bugs = bugs)
        v.selectTab(ReportsTab.Feedback)
        v.toggleFeedbackSelect("1")
        v.toggleFeedbackSelect("3")
        v.toggleFeedbackSelect("3")
        assertEquals(setOf("1"), v.uiState.value.feedbackSelection)
        v.toggleFeedbackSelect("2")
        v.markSelectedFeedbackSeen()
        assertTrue(bugs.calls.contains("seenMany [1, 2]"))
        assertEquals(listOf("3"), v.uiState.value.feedback.filter { !it.report.isSeen }.map { it.report.id })
        assertTrue(v.uiState.value.feedbackSelection.isEmpty())
        v.toggleFeedbackSelect("1")
        v.deleteSelectedFeedback()
        assertTrue(bugs.calls.contains("deleteAudited [1]"))
        assertEquals(listOf("2", "3"), v.uiState.value.feedback.map { it.report.id })
        assertEquals(2L, v.uiState.value.feedbackTotal)
    }

    @Test
    fun aSingleFeedbackDeleteFailureIsNotSilent() {
        val bugs = FakeBugReports(entries = listOf(feedback("1")), fail = true)
        val v = vm(bugs = bugs)
        v.selectTab(ReportsTab.Feedback)
        v.deleteReport("1")
        assertEquals(1, v.uiState.value.feedback.size)
        assertTrue(v.uiState.value.decisionFailed)
        v.markSeen("1")
        assertFalse(v.uiState.value.feedback.first().report.isSeen)
    }

    // ---- reports and the detector

    @Test
    fun aListThatCouldNotBeReadIsAFailureNotAnEmptyList() {
        val v = vm(reports = FakeDrawingReports(fail = true), detector = FakeDetector(fail = false))
        v.selectTab(ReportsTab.Reports)
        val s = v.uiState.value
        assertTrue(s.reportsFailed)
        assertFalse(s.detectorFailed)
        assertTrue(s.evidenceLoaded)

        val v2 = vm(reports = FakeDrawingReports(), detector = FakeDetector(fail = true))
        v2.selectTab(ReportsTab.Detector)
        assertFalse(v2.uiState.value.reportsFailed)
        assertTrue(v2.uiState.value.detectorFailed)
    }

    @Test
    fun theReviewerIsToldApartFromAnyoneElse() {
        val v = vm()
        assertTrue(v.uiState.value.identity!!.isReviewer)
    }
}
