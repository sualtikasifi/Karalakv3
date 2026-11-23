// Duel push notifications on a free Cloudflare Worker cron, replacing the GitHub Actions cron (which GitHub delays by hours).
// Same job as functions/src/duelPush.ts: find duels nobody was told about, send a data-only FCM push, stamp the document.
// Talks to Firestore and FCM over REST with the Firebase service account (secret FIREBASE_SERVICE_ACCOUNT); no dependencies.
import { accessToken, decode } from "./google.js";
import { runProcessAdminCommands, recordRun } from "./admin.js";

/** A duel older than this when the job finally sees it is no longer news: stamped as done without a push. */
export const DUEL_PUSH_WINDOW_MS = 6 * 3_600_000;

const text = (v) => (typeof v === "string" ? v.trim().slice(0, 40) : "");
const num = (v) => (typeof v === "number" && Number.isFinite(v) ? v : 0);

/** Pure: what to do with pending duels without `pushedAt` (waiting) and completed ones without `resultPushedAt` (played). */
export function planDuelPushes(waiting, played, nowMs) {
  const cutoff = nowMs - DUEL_PUSH_WINDOW_MS;
  const out = [];
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

/** Firestore REST value → plain JS value (only the kinds a duel header holds). */
export function plain(v) {
  if (!v) return undefined;
  if ("stringValue" in v) return v.stringValue;
  if ("integerValue" in v) return Number(v.integerValue);
  if ("doubleValue" in v) return v.doubleValue;
  return undefined;
}

const FIELDS = ["challengerUid", "challengerName", "opponentUid", "opponentName", "status", "createdAt", "completedAt"];

async function owedDuels(api, auth, status, marker) {
  const res = await fetch(`${api}:runQuery`, {
    method: "POST", headers: auth,
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "duels" }],
        where: { compositeFilter: { op: "AND", filters: [
          { fieldFilter: { field: { fieldPath: "status" }, op: "EQUAL", value: { stringValue: status } } },
          { fieldFilter: { field: { fieldPath: marker }, op: "EQUAL", value: { nullValue: null } } },
        ] } },
        select: { fields: FIELDS.map((fieldPath) => ({ fieldPath })) },
        limit: 200,
      },
    }),
  });
  if (!res.ok) throw new Error(`query ${status} ${res.status}: ${await res.text()}`);
  return (await res.json()).filter((r) => r.document).map((r) => {
    const data = {};
    for (const [k, v] of Object.entries(r.document.fields ?? {})) data[k] = plain(v);
    return { id: r.document.name.split("/").pop(), data };
  });
}

/** How a marker field looks in a stored duel: "null" (not announced yet: what the job queries for), "missing" (never written), "set". */
const markerKind = (v) => (v === undefined ? "missing" : "nullValue" in v ? "null" : "set");

/** Pure: one line telling how a duel document looks to the job (for the log: why is it, or is it not, owed a push?). */
export function describeDuel(doc, nowMs) {
  const f = doc.fields ?? {};
  const created = Number(f.createdAt?.integerValue ?? NaN);
  const age = Number.isFinite(created) ? `${Math.round((nowMs - created) / 60_000)} min ago` : "age unknown";
  return `recent duel ${doc.name.split("/").pop().slice(0, 6)}: ${f.status?.stringValue ?? "no status"}, ${age}, pushedAt=${markerKind(f.pushedAt)}, resultPushedAt=${markerKind(f.resultPushedAt)}`;
}

/** The newest few duels, whatever their state, written to the log: tells a missing duel from one that was already announced. Never fails the job. */
async function logRecentDuels(api, auth) {
  try {
    const res = await fetch(`${api}:runQuery`, {
      method: "POST", headers: auth,
      body: JSON.stringify({ structuredQuery: {
        from: [{ collectionId: "duels" }],
        orderBy: [{ field: { fieldPath: "createdAt" }, direction: "DESCENDING" }],
        limit: 3,
      } }),
    });
    if (!res.ok) { console.warn(`recent duels ${res.status}: ${await res.text()}`); return; }
    const docs = (await res.json()).filter((r) => r.document).map((r) => r.document);
    if (docs.length === 0) console.log("recent duels: none in the database");
    for (const d of docs) console.log(describeDuel(d, Date.now()));
  } catch (e) { console.warn("recent duels failed", e); }
}

async function tokenOf(api, auth, uid) {
  for (const path of [`users/${uid}/private/device`, `users/${uid}`]) {
    const res = await fetch(`${api}/${path}`, { headers: auth });
    if (!res.ok) continue; // 404 = no such document
    const t = (await res.json()).fields?.fcmToken?.stringValue;
    if (t) return t;
  }
  return undefined;
}

export async function runNotifyDuels(env) {
  const sa = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
  const project = sa.project_id;
  const auth = { authorization: `Bearer ${await accessToken(sa)}`, "content-type": "application/json" };
  const api = `https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/documents`;

  const [waiting, played] = await Promise.all([
    owedDuels(api, auth, "AWAITING_OPPONENT", "pushedAt"),
    owedDuels(api, auth, "COMPLETE", "resultPushedAt"),
  ]);
  const owed = planDuelPushes(waiting, played, Date.now());
  console.log(`unannounced duels: ${owed.length}, to push: ${owed.filter((p) => p.send).length}`);
  await logRecentDuels(api, auth);

  for (const push of owed) {
    if (push.send) {
      const token = await tokenOf(api, auth, push.toUid);
      if (token) {
        const res = await fetch(`https://fcm.googleapis.com/v1/projects/${project}/messages:send`, {
          method: "POST", headers: auth,
          body: JSON.stringify({ message: {
            token, data: { type: push.type, duelId: push.duelId, fromNickname: push.fromNickname }, android: { priority: "HIGH" },
          } }),
        });
        console.log(`duel ${push.duelId} ${push.type} -> ${res.status}`);
        // an uninstalled app's token goes stale: expected over time, not a bug
        if (!res.ok) console.warn(await res.text());
      } else {
        console.log(`duel ${push.duelId}: no token for ${push.toUid}`);
      }
    }
    // Stamped either way: a missing or stale token is not worth retrying every five minutes.
    const patch = await fetch(`${api}/duels/${push.duelId}?updateMask.fieldPaths=${push.stamp}`, {
      method: "PATCH", headers: auth,
      body: JSON.stringify({ fields: { [push.stamp]: { integerValue: String(Date.now()) } } }),
    });
    if (!patch.ok) console.warn(`stamp ${push.duelId} ${patch.status}: ${await patch.text()}`);
  }
}

/** Runs one job, and leaves its result for the developer panel's "Arka plan işleri" tab. */
async function job(env, name, fn) {
  try { await fn(env); await recordRun(env, name, true); }
  catch (e) { console.error(`${name} failed`, e); await recordRun(env, name, false, e); }
}

export default {
  // Every five minutes: the duel pushes, and what the panel asked for (gifts, pushes, planned XP events).
  async scheduled(_event, env, ctx) {
    ctx.waitUntil(Promise.all([job(env, "notify-duels", runNotifyDuels), job(env, "process-admin-commands", runProcessAdminCommands)]));
  },
  // Open the worker's URL to run it by hand and read the log; only answers with a secret in the query: ?key=<DUEL_PUSH_KEY>
  async fetch(request, env) {
    if (!env.DUEL_PUSH_KEY || new URL(request.url).searchParams.get("key") !== env.DUEL_PUSH_KEY) return new Response("ok");
    await job(env, "notify-duels", runNotifyDuels);
    await job(env, "process-admin-commands", runProcessAdminCommands);
    return new Response("ran");
  },
};
