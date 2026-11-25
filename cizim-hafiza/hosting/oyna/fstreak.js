// Friend streaks, the reading side (FriendStreaks.view, FriendStreakViewModel.nextPrompt, FriendStreakDialog, StreakChip).
// The writing side (a finished duel of ten words or more counts as today's day) is firebase.js recordFriendDuel. What is not here
// is the ad that mends a broken streak: the web has no ads, so a broken one shows as "💔 N" and a duel starts the next run.
import { S } from "./i18n.js";
import { appWindow, primary, el } from "./ui.js";
import * as net from "./firebase.js";

export const MILESTONES = [3, 7, 14, 30, 50, 100], REMINDER_HOUR = 14;
export const milestonesReached = (count) => MILESTONES.filter((m) => count >= m);

/** The streak as it reads on [today] (FriendStreaks.view): a mended one carries on, a lost one with a kept duel is a new streak of one. */
export function view(s, today) {
  if (s.lastDay >= today) return { s, count: s.count, status: "done" };
  if (s.lastDay === today - 1) return { s, count: s.count, status: "risk" };
  if (s.lastDay === today - 2) return { s, count: s.count, status: "broken" };
  if (s.pendingDay >= today - 1) return { s, count: 1, status: s.pendingDay >= today ? "done" : "risk" };
  return { s, count: 0, status: "lost" };
}

/** What a milestone pays each of the two friends (FriendStreakRewards). */
export const rewardFor = (m) => ({ 3: { gold: 50 }, 7: { gold: 100 }, 14: { jokers: true }, 30: { frame: "FRIENDSHIP" }, 50: { gold: 500 }, 100: { gold: 1000 } }[m] || { gold: 25 });
export function applyReward(P, r) {
  if (r.gold) P.gold = (P.gold || 0) + r.gold;
  if (r.jokers) { P.jokers = P.jokers || {}; for (const k of ["FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"]) P.jokers[k] = (P.jokers[k] || 0) + 1; }
  if (r.frame) { P.owned = P.owned || []; if (!P.owned.includes("frame:" + r.frame)) P.owned.push("frame:" + r.frame); }
}

// what this browser remembers (FriendStreakStore): prizes taken (pair, run, milestone), the day a reminder was shown, mends already told
const KEY = "fstreak";
const mem = () => { try { return JSON.parse(localStorage.getItem(KEY)) || {}; } catch { return {}; } };
const save = (m) => { try { localStorage.setItem(KEY, JSON.stringify(m)); } catch {} };
const isClaimed = (pair, run, ms) => (mem().claimed || []).includes(`${pair}:${run}:${ms}`);
const markClaimed = (pair, run, ms) => { const m = mem(); m.claimed = [...new Set([...(m.claimed || []), `${pair}:${run}:${ms}`])]; save(m); };
const remindedOn = (pair, day) => (mem().remind || {})[pair] === day;
const markReminded = (pair, day) => { const m = mem(); m.remind = { ...(m.remind || {}), [pair]: day }; save(m); };
const seenRepairs = (pair) => (mem().repairs || {})[pair] || 0;
const markRepairs = (pair, n) => { const m = mem(); m.repairs = { ...(m.repairs || {}), [pair]: n }; save(m); };

/** The one window to show now, or null: a friend mended the streak, a prize is waiting, a streak ends today (nextPrompt without the ad branch). */
export function nextPrompt(streaks, names, me, today, hour) {
  const live = streaks.filter((s) => names[s.friendUid]).map((s) => ({ s, name: names[s.friendUid], v: view(s, today) })).filter((x) => x.v.status !== "lost");
  const rescued = live.find(({ s }) => s.repairs > seenRepairs(s.pairId) && s.repairedBy && s.repairedBy !== me);
  if (rescued) return { k: "rescued", pair: rescued.s.pairId, name: rescued.name, count: rescued.v.count, repairs: rescued.s.repairs };
  for (const { s, name, v } of live) {
    const due = milestonesReached(v.count).find((m) => !isClaimed(s.pairId, s.run, m));
    if (due) return { k: "reward", pair: s.pairId, run: s.run, name, milestone: due, reward: rewardFor(due) };
  }
  if (hour >= REMINDER_HOUR) {
    const risky = live.filter(({ s, v }) => v.status === "risk" && v.count >= 2 && !remindedOn(s.pairId, today));
    if (risky.length) return { k: "risk", items: risky.map(({ s, name, v }) => ({ pair: s.pairId, uid: s.friendUid, name, count: v.count })) };
  }
  return null;
}

const rewardLine = (r) => r.gold ? S("fstreak_reward_gold", r.gold) : r.jokers ? S("fstreak_reward_jokers") : S("fstreak_reward_frame", S("frame_name_friendship"));

function window_(c, { emoji, title, body, extra, buttons }) {
  const wrap = el("div", "fs-body"); wrap.append(Object.assign(el("div", "fs-emoji"), { textContent: emoji }), Object.assign(el("p", "fs-text"), { textContent: body }));
  if (extra) wrap.append(Object.assign(el("div", "fs-extra"), { textContent: extra }));
  const foot = el("div", "fs-foot"); let ov;
  buttons.forEach(([label, fn, cls]) => foot.append(primary(label, () => { ov.close(); fn && fn(); }, cls || "")));
  ov = appWindow(c.$("stage"), { title, body: wrap, footer: foot, centerTitle: true, cls: "fs-win", onClose: undefined });
  return ov;
}

/** The home screen's one friend-streak window (FriendStreakDialog); closing it any way counts as having seen it. */
export function showPrompt(c, p, today) {
  const done = () => c.refreshHome && c.refreshHome();
  if (p.k === "rescued") return window_(c, { emoji: "🔥", title: S("fstreak_rescued_title", p.name), body: S("fstreak_rescued_body", p.name, p.count), buttons: [[S("fstreak_rescued_ok"), () => markRepairs(p.pair, p.repairs)]] });
  if (p.k === "reward") return window_(c, { emoji: "🏆", title: S("fstreak_reward_title", p.name, p.milestone), body: S("fstreak_reward_body", p.name, p.milestone), extra: rewardLine(p.reward),
    buttons: [[S("fstreak_reward_claim"), () => { if (!isClaimed(p.pair, p.run, p.milestone)) { applyReward(c.P, p.reward); markClaimed(p.pair, p.run, p.milestone); c.saveP(); done(); } }]] });
  p.items.forEach((i) => markReminded(i.pair, today));
  return window_(c, { emoji: "⏳", title: S("fstreak_reminder_title"), body: p.items.map((i) => S("fstreak_reminder_body", i.name, i.count)).join("\n"), buttons: [[S("fstreak_reminder_action"), () => c.show("friends")], [S("fstreak_reminder_later"), null, "cream"]] });
}

let unsub = null, streaks = [];
/** Starts listening to this account's streaks (the friends screen and the home prompt both read them). */
export async function watchStreaks(onChange) {
  if (unsub) return;
  try { unsub = await net.listenStreaks((l) => { streaks = l; onChange && onChange(streaks); }); } catch {}
}
export const streakOf = (uid, today) => { const s = streaks.find((x) => x.friendUid === uid); const v = s && view(s, today); return v && v.status !== "lost" ? v : null; };

/** After the opening windows: the one window the streaks ask for, if any. Quiet when another window is open. */
export async function checkStreakPrompt(c) {
  if (document.querySelector(".pc-ov, .aw-ov, .lk-ov")) return;
  await watchStreaks();
  const names = {}; const friends = await net.friendsOnce().catch(() => []);
  friends.forEach((f) => { names[f.id] = f.nickname || "?"; });
  const today = net.turkeyDay(), me = await net.myUid().catch(() => null); if (!me) return;
  const hour = new Date(Date.now() + 3 * 3600e3).getUTCHours(), p = nextPrompt(streaks, names, me, today, hour);
  if (p && c.current() === "home" && !document.querySelector(".pc-ov, .aw-ov, .lk-ov")) showPrompt(c, p, today);
}

/** "🔥 5" once today's duel is played, "⏳ 5" while it is still to play, "💔 5" when it broke yesterday (StreakChip, without the ad pill). */
export function chip(v) {
  const e = document.createElement("span"); e.className = "fs-chip " + v.status;
  e.textContent = (v.status === "done" ? "🔥 " : v.status === "broken" ? "💔 " : "⏳ ") + v.count;
  e.title = S(v.status === "done" ? "fstreak_row_done" : v.status === "broken" ? "fstreak_row_broken" : "fstreak_row_risk");
  return e;
}
