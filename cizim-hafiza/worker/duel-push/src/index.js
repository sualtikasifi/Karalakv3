// Duel push notifications on a free Cloudflare Worker cron, replacing the GitHub Actions cron (which GitHub delays by hours).
// Same job as functions/src/duelPush.ts: find duels nobody was told about, send a data-only FCM push, stamp the document.
// Talks to Firestore and FCM over REST with the Firebase service account (secret FIREBASE_SERVICE_ACCOUNT); no dependencies.

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

const b64url = (buf) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const enc = (o) => b64url(new TextEncoder().encode(JSON.stringify(o)));

async function accessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  const head = enc({ alg: "RS256", typ: "JWT" });
  const body = enc({
    iss: sa.client_email, scope: "https://www.googleapis.com/auth/cloud-platform",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600,
  });
  const der = Uint8Array.from(atob(sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "")), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${head}.${body}`));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${head}.${body}.${b64url(sig)}` }),
  });
  if (!res.ok) throw new Error(`token ${res.status}: ${await res.text()}`);
  return (await res.json()).access_token;
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

export default {
  async scheduled(_event, env, ctx) { ctx.waitUntil(runNotifyDuels(env)); },
  // Open the worker's URL to run it by hand and read the log; only answers with a secret in the query: ?key=<DUEL_PUSH_KEY>
  async fetch(request, env) {
    if (!env.DUEL_PUSH_KEY || new URL(request.url).searchParams.get("key") !== env.DUEL_PUSH_KEY) return new Response("ok");
    await runNotifyDuels(env);
    return new Response("ran");
  },
};
