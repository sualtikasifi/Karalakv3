// What the developer panel asks of the server side (gold or XP gifts to one player, a push to everyone or to one phone) and the XP
// events planned ahead, run every few minutes here instead of hourly on GitHub's cron. The same job as
// functions/src/adminTasks.ts' runProcessAdminCommands, over REST.
import { session } from "./google.js";

export const BROADCAST_TOPIC = "news";
export const BROADCAST_LANGS = ["tr", "en", "pt", "es"];
export const SEEN_MARKER = "seen_v1";
const MAX_GRANT = 100_000;

/** The week topic for [date]: seen_<ISO week-based year>w<week> (the name the app subscribes to). */
export function weekTopic(date) {
  const d = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const day = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - day);
  const yearStart = Date.UTC(d.getUTCFullYear(), 0, 1);
  const week = Math.ceil(((d.getTime() - yearStart) / 86_400_000 + 1) / 7);
  return `seen_${d.getUTCFullYear()}w${String(week).padStart(2, "0")}`;
}

function cleanText(v) {
  const title = String(v?.title ?? "").trim().slice(0, 60);
  const body = String(v?.body ?? "").trim().slice(0, 200);
  return title && body ? { title, body } : null;
}

/** One broadcast command → the sends it needs (see planBroadcast in adminTasks.ts). */
export function planBroadcast(cmd, now) {
  const base = cleanText({ title: cmd.title, body: cmd.body });
  const raw = cmd.texts ?? {};
  const texts = {};
  for (const lang of BROADCAST_LANGS) {
    const t = cleanText(raw[lang]);
    if (t) texts[lang] = t;
  }
  if (!texts.tr && base) texts.tr = base;
  const turkish = texts.tr;
  if (!turkish) return [];
  const lapsed = cmd.audience === "lapsed";
  const hasExtra = Object.keys(texts).length > 1 || Object.keys(raw).length > 0;
  if (!lapsed && !hasExtra) return [{ topic: BROADCAST_TOPIC, title: turkish.title, body: turkish.body, label: "everyone" }];
  const week0 = weekTopic(now);
  const week1 = weekTopic(new Date(now.getTime() - 7 * 86_400_000));
  const suffix = lapsed ? ` && '${SEEN_MARKER}' in topics && !('${week0}' in topics) && !('${week1}' in topics)` : "";
  const sends = [];
  const covered = [];
  for (const lang of BROADCAST_LANGS) {
    const t = texts[lang];
    if (!t) continue;
    covered.push(lang);
    sends.push({ condition: `'${BROADCAST_TOPIC}' in topics && 'news_${lang}' in topics${suffix}`, title: t.title, body: t.body, label: lang });
  }
  if (!lapsed && cmd.fallback !== false) {
    const without = covered.map((l) => ` && !('news_${l}' in topics)`).join("");
    sends.push({ condition: `'${BROADCAST_TOPIC}' in topics${without}`, title: turkish.title, body: turkish.body, label: "fallback" });
  }
  return sends;
}

const clampInt = (v, lo, hi) => Math.min(Math.max(Number(v ?? 0) | 0, lo), hi);

export async function runProcessAdminCommands(env) {
  const g = await session(env);
  let done = 0;

  // The newest 200, handled oldest first (a command that is not done has no doneAt, so a query cannot pick just those)
  const commands = (await g.query({
    from: [{ collectionId: "adminCommands" }],
    orderBy: [{ field: { fieldPath: "createdAt" }, direction: "DESCENDING" }],
    limit: 200,
  })).reverse();

  for (const cmd of commands) {
    const c = cmd.data;
    if (c.doneAt) continue;
    if (Number(c.sendAtMillis ?? 0) > Date.now()) continue;
    try {
      if (c.type === "grant") {
        const uid = String(c.uid ?? "");
        const gold = clampInt(c.gold, 0, MAX_GRANT);
        const xp = clampInt(c.xp, 0, MAX_GRANT);
        if (!uid || gold + xp <= 0) throw new Error("grant without a player or an amount");
        const now = Date.now();
        const entries = [];
        if (gold > 0) entries.push({ amount: gold, currency: "gold", reason: "admin", sourceUid: "panel", createdAt: now });
        if (xp > 0) entries.push({ amount: xp, currency: "xp", reason: "admin", sourceUid: "panel", createdAt: now });
        await g.arrayUnion(`users/${uid}/private/pendingRewards`, "pending", entries);
      } else if (c.type === "broadcast") {
        const sends = planBroadcast(c, new Date());
        if (sends.length === 0) throw new Error("broadcast without text");
        // Data-only, like the invite push: the app builds the notification itself.
        for (const send of sends) {
          const res = await g.send({
            ...(send.topic ? { topic: send.topic } : { condition: send.condition }),
            data: { type: "broadcast", title: send.title, body: send.body },
            android: { priority: "HIGH" },
          });
          if (!res.ok) throw new Error(`broadcast "${send.label}" ${res.status}: ${(await res.text()).slice(0, 120)}`);
          console.log(`Broadcast ${cmd.id}: sent "${send.label}"`);
        }
      } else if (c.type === "test") {
        const uid = String(c.uid ?? "");
        const title = String(c.title ?? "").slice(0, 60);
        const body = String(c.body ?? "").slice(0, 200);
        if (!uid || !title || !body) throw new Error("test without a device or a text");
        const token = (await g.get(`users/${uid}/private/device`))?.fcmToken;
        if (!token) throw new Error("this account has no push token (notifications off, or never registered)");
        const res = await g.send({ token, data: { type: "broadcast", title, body }, android: { priority: "HIGH" } });
        if (!res.ok) throw new Error(`test push ${res.status}: ${(await res.text()).slice(0, 120)}`);
      } else {
        throw new Error(`unknown command type ${String(c.type)}`);
      }
      await g.patch(`adminCommands/${cmd.id}`, { doneAt: { __ts: Date.now() } });
      done++;
    } catch (e) {
      console.error(`Admin command ${cmd.id} failed`, e);
      await g.patch(`adminCommands/${cmd.id}`, {
        doneAt: { __ts: Date.now() },
        error: String(e instanceof Error ? e.message : e).slice(0, 200),
      });
    }
  }

  // XP events planned ahead: switch one on when its time has come (the app's own xpEvent document, same shape).
  const now = Date.now();
  const plans = (await g.query({
    from: [{ collectionId: "xpEventPlans" }],
    orderBy: [{ field: { fieldPath: "startAtMillis" }, direction: "DESCENDING" }],
    limit: 100,
  })).reverse();
  let started = 0;
  for (const plan of plans) {
    const p = plan.data;
    if (p.startedAt) continue;
    const startAt = Number(p.startAtMillis ?? 0);
    const hours = Math.min(Math.max(Number(p.durationHours ?? 0), 1), 168);
    const multiplier = Math.min(Math.max(Number(p.multiplier ?? 2), 1), 10);
    if (!startAt || now < startAt) continue;
    const endsAt = startAt + hours * 3_600_000;
    if (now < endsAt) {
      await g.put("config/xpEvent", { active: true, multiplier, startedAtMillis: now, endsAtMillis: endsAt, label: p.label ?? null });
      started++;
    }
    await g.patch(`xpEventPlans/${plan.id}`, { startedAt: now });
  }
  console.log(`Admin commands: ${done} run, ${started} planned XP event(s) started`);
}

/** Leaves a job's last run where the developer panel's "Arka plan işleri" tab can read it (adminJobs/<task>). */
export async function recordRun(env, task, ok, error) {
  try {
    const g = await session(env);
    await g.put(`adminJobs/${task}`, {
      lastRunAt: { __ts: Date.now() }, ok, error: ok ? "" : String(error instanceof Error ? error.message : error).slice(0, 300),
    });
  } catch (e) { console.error("Could not record the run", e); }
}
