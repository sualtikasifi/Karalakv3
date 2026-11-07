package com.sualtikasifi.cizimhafiza.domain.repository

import com.sualtikasifi.cizimhafiza.domain.model.BugReport
import com.sualtikasifi.cizimhafiza.domain.model.BugReportCategory
import com.sualtikasifi.cizimhafiza.domain.model.BugReportEntry
import kotlinx.coroutines.flow.Flow

/**
 * Firestore-backed inbox for in-app "Sorun Bildir" (report a bug)
 * submissions — see presentation/reportbug/.
 *
 * Reports are stamped with their author's uid so [observeMyReports] can read
 * them back. There is no reply text — the only status a report carries is
 * whether the reviewer has marked it seen (see [markSeen]), which the
 * reporter then sees reflected on their own copy.
 */
interface BugReportRepository {
    suspend fun submitReport(description: String, category: BugReportCategory): Result<Unit>

    /** This device's own past reports, newest first, with their seen status. */
    fun observeMyReports(): Flow<List<BugReport>>

    /**
     * Everyone's reports, newest first — the developer panel's inbox.
     *
     * Readable only by the reviewer (see firestore.rules); for anybody else
     * the query comes back empty rather than failing, which is what the
     * panel's own identity line is there to explain.
     */
    suspend fun allReports(limit: Int): Result<List<BugReportEntry>>

    /**
     * Marks a report seen from the developer panel. Reviewer-only (see
     * firestore.rules) and restricted there to touching just this one field —
     * the panel has no way to edit or answer a report, only to acknowledge it.
     */
    suspend fun markSeen(reportId: String): Result<Unit>

    /**
     * Deletes one or more reports — a reporter clearing their own history, or
     * the reviewer clearing (or bulk-clearing) the panel's inbox. Both sides
     * call the same method; firestore.rules is what actually limits a
     * non-reviewer to deleting only their own uid's documents.
     */
    suspend fun deleteReports(reportIds: List<String>): Result<Unit>

    // --- the developer panel's own operations: reviewer only, and the deletions go into the audit log in the same write ---

    /** How many reports there are in all (one count query). */
    suspend fun reportCount(): Result<Long>

    /** How many of them the reviewer has not marked seen yet (two count queries: all, minus the seen ones). */
    suspend fun unseenCount(): Result<Long>

    /** Marks several reports seen in one write. */
    suspend fun markSeenMany(reportIds: List<String>): Result<Unit>

    /** The reviewer deleting reports from the panel's inbox; the audit-log line is written in the same batch. */
    suspend fun deleteReportsAudited(reportIds: List<String>): Result<Unit>

    /** Deletes every report there is, not just the ones the panel has loaded; returns how many went. */
    suspend fun deleteAllReports(): Result<Int>
}
