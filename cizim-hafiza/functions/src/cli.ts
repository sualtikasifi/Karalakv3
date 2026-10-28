/**
 * Runs one of the league's scheduled tasks as a plain script — no Cloud
 * Functions runtime involved — so they can run from a GitHub Actions cron
 * instead. Cloud Functions require the Blaze plan regardless of how they're
 * deployed, which this project is deliberately staying off of; these four
 * tasks are all schedule-based (nothing here reacts to a live Firestore
 * write), so a cron script is a straight substitute; only the two
 * event-triggered functions in index.ts (onInviteCreated, clampImpossibleScores)
 * genuinely need Cloud Functions and stay undeployed until that changes.
 *
 * Authenticates the same way the rules/indexes deploy does: `admin.initializeApp()`
 * reads GOOGLE_APPLICATION_CREDENTIALS, which .github/workflows/league-scheduler.yml
 * points at a file written from the FIREBASE_SERVICE_ACCOUNT secret.
 *
 * Importing "./index" both defines the (undeployed) Cloud Functions and runs
 * its top-level `admin.initializeApp()` — safe here since a plain script
 * never touches the Cloud Functions runtime those definitions belong to.
 */
import {
  runBuildGlobalLeaderboard,
  runCleanupAbandonedRooms,
  runFinalizeLeaguePeriod,
  runGrantReferralRewards,
} from "./index";
import * as admin from "firebase-admin";
import { runProcessAdminCommands, runRollupStats } from "./adminTasks";
import { runMigrateWebEmails, runProcessDeletions } from "./maintenance";

const tasks: Record<string, () => Promise<void>> = {
  "build-global-leaderboard": runBuildGlobalLeaderboard,
  "finalize-league-period": runFinalizeLeaguePeriod,
  "cleanup-abandoned-rooms": runCleanupAbandonedRooms,
  "grant-referral-rewards": runGrantReferralRewards,
  // Daily: removes the accounts that asked to be deleted (see maintenance.ts).
  "process-deletions": runProcessDeletions,
  // One-off: moves browser accounts off the old @users.karalak.app address domain (see maintenance.ts).
  "migrate-web-emails": runMigrateWebEmails,
  // Hourly: what the developer panel asked for (gifts, broadcasts) and XP events planned ahead (see adminTasks.ts).
  "process-admin-commands": runProcessAdminCommands,
  // Nightly: folds the day's statistics rows into one adminSummary document.
  "rollup-stats": runRollupStats,
};

/** Leaves the task's last run where the developer panel's "Arka plan işleri" tab can read it. */
async function recordRun(task: string, ok: boolean, error?: unknown): Promise<void> {
  try {
    await admin.firestore().doc(`adminJobs/${task}`).set({
      lastRunAt: admin.firestore.FieldValue.serverTimestamp(),
      ok,
      error: ok ? "" : String(error instanceof Error ? error.message : error).slice(0, 300),
    });
  } catch (e) {
    console.error("Could not record the run", e);
  }
}

const taskName = process.argv[2];
const run = tasks[taskName];

if (!run) {
  console.error(`Unknown task "${taskName}". Expected one of: ${Object.keys(tasks).join(", ")}`);
  process.exit(1);
}

run()
  .then(async () => {
    await recordRun(taskName, true);
    process.exit(0);
  })
  .catch(async (err) => {
    console.error(err);
    await recordRun(taskName, false, err);
    process.exit(1);
  });
