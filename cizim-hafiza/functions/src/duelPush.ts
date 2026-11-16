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

/** How far back a duel still counts as news. Longer than the cron interval many times over, so a run GitHub delays or drops is made up for. */
export const DUEL_PUSH_WINDOW_MS = 6 * 3_600_000;

export interface DuelPush {
  duelId: string;
  /** Who is told. */
  toUid: string;
  /** Who the notification names. */
  fromNickname: string;
  type: "duel" | "duel_result";
  /** The document field stamped once it is sent. */
  stamp: "pushedAt" | "resultPushedAt";
}

interface DuelData {
  challengerUid?: unknown; challengerName?: unknown; opponentUid?: unknown; opponentName?: unknown;
  status?: unknown; createdAt?: unknown; completedAt?: unknown; pushedAt?: unknown; resultPushedAt?: unknown;
}

const text = (v: unknown): string => (typeof v === "string" ? v.trim().slice(0, 40) : "");
const num = (v: unknown): number => (typeof v === "number" && Number.isFinite(v) ? v : 0);

/** The pushes still owed for these duels (pure: no I/O, so it can be tested). */
export function planDuelPushes(duels: { id: string; data: DuelData }[], nowMs: number): DuelPush[] {
  const cutoff = nowMs - DUEL_PUSH_WINDOW_MS;
  const out: DuelPush[] = [];
  for (const { id, data } of duels) {
    const complete = data.status === "COMPLETE";
    const challenger = text(data.challengerUid);
    const opponent = text(data.opponentUid);
    // The challenge: only while it is still waiting — if the opponent already played, the only news is the result.
    if (!complete && !data.pushedAt && opponent && num(data.createdAt) >= cutoff) {
      out.push({ duelId: id, toUid: opponent, fromNickname: text(data.challengerName), type: "duel", stamp: "pushedAt" });
    }
    if (complete && !data.resultPushedAt && challenger && num(data.completedAt) >= cutoff) {
      out.push({ duelId: id, toUid: challenger, fromNickname: text(data.opponentName), type: "duel_result", stamp: "resultPushedAt" });
    }
  }
  return out;
}

/** Sends every push still owed and stamps each one. */
export async function runNotifyDuels(): Promise<void> {
  const db = admin.firestore();
  const cutoff = Date.now() - DUEL_PUSH_WINDOW_MS;
  // Two single-field range queries (no composite index needed); select() leaves out the drawings, which are most of a duel's size.
  const fields = ["challengerUid", "challengerName", "opponentUid", "opponentName", "status", "createdAt", "completedAt", "pushedAt", "resultPushedAt"];
  const [fresh, played] = await Promise.all([
    db.collection("duels").where("createdAt", ">=", cutoff).select(...fields).get(),
    db.collection("duels").where("completedAt", ">=", cutoff).select(...fields).get(),
  ]);
  const byId = new Map<string, DuelData>();
  for (const d of [...fresh.docs, ...played.docs]) byId.set(d.id, d.data() as DuelData);
  const owed = planDuelPushes([...byId].map(([id, data]) => ({ id, data })), Date.now());
  console.log(`duels in window: ${byId.size}, pushes owed: ${owed.length}`);

  for (const push of owed) {
    // Stamped first-ish: a failed send for a missing token is not worth retrying every run, so it is stamped either way.
    const stampRef = db.collection("duels").doc(push.duelId);
    const device = await db.doc(`users/${push.toUid}/private/device`).get();
    let token = device.get("fcmToken") as string | undefined;
    if (!token) token = (await db.doc(`users/${push.toUid}`).get()).get("fcmToken") as string | undefined;
    if (token && push.fromNickname) {
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
    await stampRef.update({ [push.stamp]: Date.now() });
  }
}
