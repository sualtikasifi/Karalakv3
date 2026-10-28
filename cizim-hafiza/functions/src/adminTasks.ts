/**
 * What the developer panel asks of the server side, run from functions/src/cli.ts on a GitHub Actions runner:
 *  - process-admin-commands: the requests the panel writes to adminCommands (a gift of gold or XP to one player, a push
 *    to everyone) and the XP events planned ahead (xpEventPlans);
 *  - rollup-stats: folds each day's adminStats rows into one small adminSummary document, so the panel reads 14
 *    documents instead of thousands of device rows (Firestore's free tier allows 50,000 reads a day).
 */
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";

const MAX_DEVICE_ROWS = 5000;
const MAX_DEVICE_EVENTS = 2000;
/** 20 units of the account currency in one day from one device. */
const MAX_DEVICE_REVENUE_MICROS = 20_000_000;
/** The push topic every app with notifications on (and not a child account) subscribes to — see BroadcastTopic.kt. */
export const BROADCAST_TOPIC = "news";
const MAX_GRANT = 100_000;

/** What one device can plausibly add to a counter in a day (the same limits as the panel's own ConsoleData.kt had). */
export function capPerDevice(key: string, value: unknown): number {
  const n = typeof value === "number" && Number.isFinite(value) ? value : 0;
  return Math.min(Math.max(n, 0), key.endsWith("__micros") ? MAX_DEVICE_REVENUE_MICROS : MAX_DEVICE_EVENTS);
}

/** Share of [before] that is also in [after], 0..1, or null when nobody was there to come back. */
export function returnRate(before: ReadonlySet<string>, after: ReadonlySet<string>): number | null {
  if (before.size === 0) return null;
  let back = 0;
  for (const uid of before) if (after.has(uid)) back++;
  return Math.round((back / before.size) * 1000) / 1000;
}

function isoDay(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function shiftDay(day: string, delta: number): string {
  const d = new Date(`${day}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + delta);
  return isoDay(d);
}

// ------------------------------------------------------------------ commands

export async function runProcessAdminCommands(): Promise<void> {
  const db = admin.firestore();
  let done = 0;

  const commands = await db.collection("adminCommands").limit(200).get();
  for (const doc of commands.docs) {
    if (doc.get("doneAt")) continue;
    const type = doc.get("type");
    try {
      if (type === "grant") {
        const uid = String(doc.get("uid") ?? "");
        const gold = Math.min(Math.max(Number(doc.get("gold") ?? 0) | 0, 0), MAX_GRANT);
        const xp = Math.min(Math.max(Number(doc.get("xp") ?? 0) | 0, 0), MAX_GRANT);
        if (!uid || gold + xp <= 0) throw new Error("grant without a player or an amount");
        const entries: object[] = [];
        const now = Date.now();
        if (gold > 0) entries.push({ amount: gold, currency: "gold", reason: "admin", sourceUid: "panel", createdAt: now });
        if (xp > 0) entries.push({ amount: xp, currency: "xp", reason: "admin", sourceUid: "panel", createdAt: now });
        await db
          .doc(`users/${uid}/private/pendingRewards`)
          .set({ pending: admin.firestore.FieldValue.arrayUnion(...entries) }, { merge: true });
      } else if (type === "broadcast") {
        const title = String(doc.get("title") ?? "").slice(0, 60);
        const body = String(doc.get("body") ?? "").slice(0, 200);
        if (!title || !body) throw new Error("broadcast without text");
        // Data-only, like the invite push: the app builds the notification itself (right channel, own icon).
        await admin.messaging().send({
          topic: BROADCAST_TOPIC,
          data: { type: "broadcast", title, body },
          android: { priority: "high" },
        });
      } else {
        throw new Error(`unknown command type ${String(type)}`);
      }
      await doc.ref.update({ doneAt: admin.firestore.FieldValue.serverTimestamp() });
      done++;
    } catch (e) {
      logger.error(`Admin command ${doc.id} failed`, e);
      await doc.ref.update({
        doneAt: admin.firestore.FieldValue.serverTimestamp(),
        error: String(e instanceof Error ? e.message : e).slice(0, 200),
      });
    }
  }

  // XP events planned ahead: switch one on when its time has come (the app's own xpEvent document, same shape).
  const now = Date.now();
  const plans = await db.collection("xpEventPlans").limit(100).get();
  let started = 0;
  for (const plan of plans.docs) {
    if (plan.get("startedAt")) continue;
    const startAt = Number(plan.get("startAtMillis") ?? 0);
    const hours = Math.min(Math.max(Number(plan.get("durationHours") ?? 0), 1), 168);
    const multiplier = Math.min(Math.max(Number(plan.get("multiplier") ?? 2), 1), 10);
    if (!startAt || now < startAt) continue;
    const endsAt = startAt + hours * 3_600_000;
    if (now < endsAt) {
      await db.doc("config/xpEvent").set({
        active: true,
        multiplier,
        startedAtMillis: now,
        endsAtMillis: endsAt,
        label: plan.get("label") ?? null,
      });
      started++;
    }
    await plan.ref.update({ startedAt: now });
  }
  logger.info(`Admin commands: ${done} run, ${started} planned XP event(s) started`);
}

// ------------------------------------------------------------------ stats roll-up

export interface DaySummary {
  day: string;
  players: number;
  devices: number;
  counts: Record<string, number>;
  newPlayers: number;
  d1Return: number | null;
  d7Return: number | null;
  final: boolean;
}

/** Reads one day's rows and folds them (and the day's retention against earlier days) into adminSummary/{day}. */
export async function rollupDay(db: admin.firestore.Firestore, day: string): Promise<DaySummary> {
  const counts: Record<string, number> = {};
  const rows = await db.collection(`adminStats/${day}/devices`).limit(MAX_DEVICE_ROWS).get();
  for (const row of rows.docs) {
    const c = row.get("counts") as Record<string, unknown> | undefined;
    if (!c) continue;
    for (const [key, value] of Object.entries(c)) counts[key] = (counts[key] ?? 0) + capPerDevice(key, value);
  }
  // Totals older app versions kept on the day document itself.
  const legacy = (await db.doc(`adminStats/${day}`).get()).get("counts") as Record<string, unknown> | undefined;
  if (legacy) for (const [key, value] of Object.entries(legacy)) counts[key] = (counts[key] ?? 0) + capPerDevice(key, value);

  const markers = await db.collection(`adminStats/${day}/users`).get();
  const today = new Set(markers.docs.map((m) => (m.get("uid") as string | undefined) ?? m.id));

  const uidsOf = async (d: string): Promise<Set<string>> => {
    const snap = await db.doc(`adminSummaryUids/${d}`).get();
    return new Set<string>((snap.get("uids") as string[] | undefined) ?? []);
  };
  const yesterday = await uidsOf(shiftDay(day, -1));
  const weekAgo = await uidsOf(shiftDay(day, -7));
  // "New" = not seen in any of the 30 days before: a first visit, or someone who has been away a month.
  const seenBefore = new Set<string>();
  for (let i = 1; i <= 30; i++) for (const uid of await uidsOf(shiftDay(day, -i))) seenBefore.add(uid);
  let newPlayers = 0;
  for (const uid of today) if (!seenBefore.has(uid)) newPlayers++;

  await db.doc(`adminSummaryUids/${day}`).set({ uids: [...today].slice(0, 20000) });
  const summary: DaySummary = {
    day,
    players: today.size,
    devices: rows.size,
    counts,
    newPlayers,
    d1Return: returnRate(yesterday, today),
    d7Return: returnRate(weekAgo, today),
    final: true,
  };
  await db.doc(`adminSummary/${day}`).set({ ...summary, rolledAt: admin.firestore.FieldValue.serverTimestamp() });
  return summary;
}

export async function runRollupStats(): Promise<void> {
  const db = admin.firestore();
  const today = isoDay(new Date());
  // The last two days are redone every night (a phone can flush a day late); anything missing before that is filled in.
  const days = Array.from({ length: 31 }, (_, i) => shiftDay(today, -(31 - i)));
  const have = new Set((await db.collection("adminSummary").where("day", ">=", days[0]).get()).docs.map((d) => d.id));
  const todo = days.filter((d, i) => !have.has(d) || i >= days.length - 2);
  for (const day of todo) {
    const s = await rollupDay(db, day);
    logger.info(`Rolled up ${day}: ${s.players} player(s), ${s.devices} device row(s), ${s.newPlayers} new`);
  }
}
