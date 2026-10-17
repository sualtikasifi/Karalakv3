// Cloud save (BackupRepositoryImpl.kt / ProgressSnapshot.kt): the same users/{uid}/backup/state document the Android app writes,
// so one Google account carries its progress between the phone app and this browser. Written with merge, so fields the browser
// does not track (ad cooldowns, penalties…) stay as the app left them; stamped with sealedAt = server time (firestore.rules).
import * as net from "./firebase.js";
import { unlockedIds } from "./achievements.js";
import { periodId } from "./league.js";

const KEY_AT = "lastBackupAt";
export const lastBackupAt = () => { try { return +localStorage.getItem(KEY_AT) || 0; } catch { return 0; } };
const stamp = (t) => { try { localStorage.setItem(KEY_AT, String(t)); } catch {} };

const encodeChest = (ch) => (ch ? `${ch.id}|${ch.tier}|${ch.start ?? "-"}` : "");
function decodeChests(list) {
  const slots = (list || []).slice(0, 4).map((e) => {
    const p = String(e).split("|"); if (p.length !== 3 || !p[0] || !["SILVER", "GOLD", "RARE"].includes(p[1])) return null;
    const start = p[2] === "-" ? null : Number(p[2]); if (start !== null && !Number.isFinite(start)) return null;
    return { id: p[0], tier: p[1], start };
  });
  while (slots.length < 4) slots.push(null);
  return slots;
}

/** The browser's whole progress as ProgressSnapshot.toFirestoreMap fields (only what the browser itself tracks). */
export function toSnapshot(P, remote) {
  const s = P.stats || {}, d = P.daily || { last: -1, streak: 0, best: 0, result: null }, lg = P.login || { streak: 0, last: -1 };
  const levelProgress = Object.entries(P.levels || {}).map(([k, stars]) => {
    const old = ((remote && remote.levelProgress) || []).find((r) => String(r).startsWith(k + ":"));
    return `${k}:${Math.max(stars, old ? +String(old).split(":")[2] || 0 : 0)}:${old ? +String(old).split(":")[3] || 0 : 0}`;
  });
  for (const r of (remote && remote.levelProgress) || []) { const k = String(r).split(":").slice(0, 2).join(":"); if (!(P.levels || {})[k]) levelProgress.push(r); }
  const C = P.chests;
  const snap = {
    lifetimeScore: s.score || 0, lifetimeXp: P.xp || 0, lifetimeWordsDrawn: s.words || 0, lifetimeGamesPlayed: s.games || 0, lifetimePerfectRounds: s.perfect || 0,
    lifetimeOnlineWins: s.wins || 0, bestStreak: s.best || 0, nickname: P.nick, selectedAvatarFrameId: P.frame, selectedPenSkinId: P.pen,
    dailyLastCompletedEpochDay: d.last ?? -1, dailyCurrentStreak: d.streak || 0, dailyBestStreak: d.best || 0,
    unlockedAchievementIds: unlockedIds(P), levelProgress, goldBalance: P.gold || 0, ownedStoreIds: [...(P.owned || [])],
    jokerCounts: Object.entries(P.jokers || {}).map(([t, n]) => `${t}:${n}`),
    periodXp: P.periodId === periodId() ? P.periodXp || 0 : 0, periodId: periodId(), periodXpVersion: 2,
    loginStreak: lg.streak || 0, loginLastClaimDay: lg.last ?? -1, runeLevels: { ...(P.runes || {}) }, claimedAchievementIds: [...(P.claimed || [])], claimsSaved: true,
    dailyResult: d.result ? [d.result.day, (d.result.flags || []).map((f) => (f ? "1" : "0")).join(","), d.result.score, d.result.xp, d.result.streak].join("|") : "",
    backedUpAt: Date.now(),
  };
  if (C) { snap.chestSlots = (C.slots || []).map(encodeChest); snap.chestCycleSeed = C.seed || 0; snap.chestCycleIndex = C.idx || 0; }
  return snap;
}

/** Puts a cloud snapshot into the browser's player state (BackupRepositoryImpl.apply). */
export function applySnapshot(P, r) {
  const n = (v) => (Number.isFinite(+v) ? +v : 0);
  P.xp = n(r.lifetimeXp); P.gold = n(r.goldBalance);
  P.stats = { ...(P.stats || {}), score: n(r.lifetimeScore), words: n(r.lifetimeWordsDrawn), games: n(r.lifetimeGamesPlayed), perfect: n(r.lifetimePerfectRounds), wins: n(r.lifetimeOnlineWins), best: n(r.bestStreak) };
  if (r.nickname) P.nick = r.nickname;
  if (r.selectedAvatarFrameId) P.frame = r.selectedAvatarFrameId;
  if (r.selectedPenSkinId) P.pen = r.selectedPenSkinId;
  P.owned = [...(r.ownedStoreIds || [])];
  P.jokers = {}; for (const e of r.jokerCounts || []) { const [t, c] = String(e).split(":"); if (t && Number.isFinite(+c)) P.jokers[t] = +c; }
  P.levels = {}; for (const e of r.levelProgress || []) { const p = String(e).split(":"); if (p.length === 4 && Number.isFinite(+p[2])) P.levels[`${p[0]}:${p[1]}`] = +p[2]; }
  P.claimed = [...(r.claimedAchievementIds || [])];
  P.runes = { ...(r.runeLevels || {}) };
  P.daily = { last: r.dailyLastCompletedEpochDay ?? -1, streak: n(r.dailyCurrentStreak), best: n(r.dailyBestStreak), result: null };
  if (r.dailyResult) { const p = String(r.dailyResult).split("|"); if (p.length === 5) P.daily.result = { day: +p[0], flags: p[1] ? p[1].split(",").map((f) => f === "1") : [], score: +p[2], xp: +p[3], streak: +p[4] }; }
  P.login = { streak: n(r.loginStreak), last: r.loginLastClaimDay ?? -1 };
  P.periodId = r.periodId ?? -1; P.periodXp = r.periodXpVersion >= 2 ? n(r.periodXp) : 0;
  if (r.chestSlots) P.chests = { slots: decodeChests(r.chestSlots), idx: n(r.chestCycleIndex), seed: n(r.chestCycleSeed) || Math.floor(Math.random() * 2 ** 31) + 1 };
  return P;
}

let remoteXp = -1, timer = 0, busy = false, lastRemote = null;
/** Only a browser that once signed in to an account has anything to sync; everyone else must not be signed in to Firebase just by opening the page (anonymous sign-ups are rate-limited per network). */
const FLAG = "karalakAccount";
export const markAccount = (on) => { try { if (on) localStorage.setItem(FLAG, "1"); else localStorage.removeItem(FLAG); } catch {} };
const linked = async () => { let f = false; try { f = localStorage.getItem(FLAG) === "1"; } catch {} if (!f) return null; return linked0(); };
const linked0 = async () => { try { const a = await net.authState(); return a && !a.anon ? a : null; } catch { return null; } };

/** Reads the cloud save; adopts it when it holds more XP than the browser, otherwise the browser's progress goes up. Returns "restored" | "saved" | "none". */
export async function syncNow(c, { adopt = true } = {}) {
  if (busy || !(await linked())) return "none";
  busy = true;
  try {
    const remote = await net.readBackup();
    lastRemote = remote; remoteXp = remote ? remote.lifetimeXp || 0 : 0;
    if (adopt && remote && remoteXp > (c.P.xp || 0)) { applySnapshot(c.P, remote); c.saveP(); c.refreshHome(); stamp(Date.now()); return "restored"; }
    if ((c.P.xp || 0) < remoteXp) return "none";
    const snap = toSnapshot(c.P, remote);
    if (!snap.lifetimeXp && !snap.lifetimeGamesPlayed) return "none";
    await net.writeBackup(snap); remoteXp = snap.lifetimeXp; stamp(snap.backedUpAt); return "saved";
  } catch { return "error"; } finally { busy = false; }
}
/** Called after every round and when the page is hidden: one upload at most every 20 seconds. */
export function scheduleBackup(c, now = false) {
  clearTimeout(timer);
  timer = setTimeout(async () => { if (await linked()) { if (remoteXp < 0) await syncNow(c); else if ((c.P.xp || 0) >= remoteXp) { busy = true; try { const snap = toSnapshot(c.P, lastRemote); await net.writeBackup(snap); remoteXp = snap.lifetimeXp; stamp(snap.backedUpAt); } catch {} finally { busy = false; } } } }, now ? 0 : 20000);
}
export const forgetRemote = () => { remoteXp = -1; };
