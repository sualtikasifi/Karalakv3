import * as admin from "firebase-admin";

/**
 * Push notifications for asynchronous duels, sent from a scheduled script instead of a Cloud Function.
 *
 * A duel is one document in `duels/{id}`. The push that says "X challenged you" (to the opponent) and "X played your duel" (to the
 * challenger) used to need a live Firestore trigger, which means Cloud Functions, which means the Blaze plan. This project stays on
 * Spark, so GitHub Actions' cron runs this every few minutes instead: it looks at the duels of the last few hours, sends what has not
 * been sent, and stamps the document (`pushedAt`, `resultPushedAt`) so nothing goes out twice. A push arrives minutes late, never early.
 *
 * Data-only messages, like the invite and broadcast pushes: the app builds the notification from its own strings (right language,
 * right channel). The `type` is "duel" or "duel_result".
 */

/** A duel older than this when the job finally sees it is no longer news: it is stamped as done without a push. */
export const DUEL_PUSH_WINDOW_MS = 6 * 3_600_000;

export interface DuelPush {
  duelId: string;
  /** Who is told. */
  toUid: string;
  /** Who the notification names. */
  fromNickname: string;
  type: "duel" | "duel_result";
  /** The document field stamped once it is dealt with. */
  stamp: "pushedAt" | "resultPushedAt";
  /** False for a duel that is stamped but not announced (too old, already played, nobody to name). */
  send: boolean;
}

interface DuelData {
  challengerUid?: unknown; challengerName?: unknown; opponentUid?: unknown; opponentName?: unknown;
  status?: unknown; createdAt?: unknown; completedAt?: unknown;
}

const text = (v: unknown): string => (typeof v === "string" ? v.trim().slice(0, 40) : "");
const num = (v: unknown): number => (typeof v === "number" && Number.isFinite(v) ? v : 0);

/**
 * What to do with duels the job found unannounced: [waiting] are pending duels with no `pushedAt` yet, [played] are completed
 * ones with no `resultPushedAt` yet. Each one leaves its query the moment it is stamped, so a run reads only what is owed
 * (pure: no I/O, so it can be tested).
 */
export function planDuelPushes(
  waiting: { id: string; data: DuelData }[],
  played: { id: string; data: DuelData }[],
  nowMs: number
): DuelPush[] {
  const cutoff = nowMs - DUEL_PUSH_WINDOW_MS;
  const out: DuelPush[] = [];
  for (const { id, data } of waiting) {
    const opponent = text(data.opponentUid);
    const name = text(data.challengerName);
    out.push({
      duelId: id, toUid: opponent, fromNickname: name, type: "duel", stamp: "pushedAt",
      send: !!opponent && !!name && data.status !== "COMPLETE" && num(data.createdAt) >= cutoff,
    });
  }
  for (const { id, data } of played) {
    const challenger = text(data.challengerUid);
    const name = text(data.opponentName);
    out.push({
      duelId: id, toUid: challenger, fromNickname: name, type: "duel_result", stamp: "resultPushedAt",
      send: !!challenger && !!name && num(data.completedAt) >= cutoff,
    });
  }
  return out;
}

/** Sends every push still owed and stamps each duel. */
export async function runNotifyDuels(): Promise<void> {
  const db = admin.firestore();
  // Only duels created with the two markers set to null (the app writes them) can match: an equality on null finds exactly the
  // unannounced ones, and no composite index is needed. Equality-only filters, and select() leaves out the drawings.
  const fields = ["challengerUid", "challengerName", "opponentUid", "opponentName", "status", "createdAt", "completedAt"];
  const [waiting, played] = await Promise.all([
    db.collection("duels").where("status", "==", "AWAITING_OPPONENT").where("pushedAt", "==", null).select(...fields).get(),
    db.collection("duels").where("status", "==", "COMPLETE").where("resultPushedAt", "==", null).select(...fields).get(),
  ]);
  const owed = planDuelPushes(
    waiting.docs.map((d) => ({ id: d.id, data: d.data() as DuelData })),
    played.docs.map((d) => ({ id: d.id, data: d.data() as DuelData })),
    Date.now()
  );
  console.log(`unannounced duels: ${owed.length}, to push: ${owed.filter((p) => p.send).length}`);

  // In batches, so a burst of duels is not sent one at a time.
  const BATCH = 25;
  for (let i = 0; i < owed.length; i += BATCH) {
    await Promise.all(owed.slice(i, i + BATCH).map(async (push) => {
      if (push.send) {
        const device = await db.doc(`users/${push.toUid}/private/device`).get();
        let token = device.get("fcmToken") as string | undefined;
        if (!token) token = (await db.doc(`users/${push.toUid}`).get()).get("fcmToken") as string | undefined;
        if (token) {
          try {
            await admin.messaging().send({
              token,
              data: { type: push.type, duelId: push.duelId, fromNickname: push.fromNickname },
              android: { priority: "high" },
            });
          } catch (e) {
            // an uninstalled app's token goes stale: expected over time, not a bug
            console.warn(`push for duel ${push.duelId} failed:`, e instanceof Error ? e.message : e);
          }
        }
      }
      // Stamped either way: a missing token or a stale one is not worth retrying every ten minutes.
      await db.collection("duels").doc(push.duelId).update({ [push.stamp]: Date.now() });
    }));
  }
}
