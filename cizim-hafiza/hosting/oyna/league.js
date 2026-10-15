import * as net from "./firebase.js";
import { frameRing, frameList, frameById } from "./store.js";

// ---- Port of LeagueBots.kt: the fillers every phone adds to the published table ----
let NAMES = [];
const ISTANBUL = "Europe/Istanbul";
function seeded(seed) {
  let a = Number(BigInt.asIntN(32, BigInt(seed)));
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const totalXpForLevel = (l) => { const n = Math.max(l - 1, 0); return 25 * n * n + 75 * n; };
const levelForXp = (xp) => xp <= 0 ? 1 : Math.min(100, Math.max(1, Math.floor((-75 + Math.sqrt(75 * 75 + 100 * xp)) / 50) + 1));
const nicknames = (periodId, count = 21) => {
  const pool = [...NAMES], r = seeded(periodId);
  for (let i = pool.length - 1; i >= 1; i--) { const j = Math.floor(r() * (i + 1)); [pool[i], pool[j]] = [pool[j], pool[i]]; }
  return pool.slice(0, count);
};
function startXp(pid, idx) {
  const r = seeded(BigInt(pid) * 7919n + BigInt(idx) * 104729n + 17n);
  const level = 25 + Math.floor(r() * 31), span = totalXpForLevel(level + 1) - totalXpForLevel(level);
  return totalXpForLevel(level) + Math.floor(r() * span);
}
function botPeriodXp(pid, idx, now, monthStart) {
  const pace = 0.65 + seeded(BigInt(pid) * 131n + BigInt(idx) * 977n + 5n)() * 0.55;
  const IV = 50 * 60 * 1000;
  let xp = 0, tick = Math.floor(monthStart / IV) + 1; const last = Math.floor(now / IV);
  for (; tick <= last; tick++) xp += Math.floor((60 + seeded(BigInt(tick) * 104729n + BigInt(pid) * 97n + BigInt(idx))() * 201) * pace);
  return xp;
}
function bots(pid, now, monthStart) {
  return nicknames(pid).map((nick, i) => { const xp = botPeriodXp(pid, i, now, monthStart); return { nickname: nick, periodXp: xp, level: Math.min(70, levelForXp(startXp(pid, i) + xp)), bot: true }; });
}
// ---- Turkey time ----
function turkeyToday() {
  const p = new Intl.DateTimeFormat("en-CA", { timeZone: ISTANBUL, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date()).split("-").map(Number);
  return { y: p[0], m: p[1], d: p[2] };
}
export const periodId = () => { const t = turkeyToday(); return t.y * 12 + (t.m - 1); };
function monthStartMillis() { const t = turkeyToday(); return Date.parse(`${t.y}-${String(t.m).padStart(2, "0")}-01T00:00:00+03:00`); }
const daysRemaining = () => { const t = turkeyToday(); return new Date(t.y, t.m, 0).getDate() - t.d; };

const ladder = () => frameList().filter((f) => f.level > 0 && !f.league);
const highestFrame = (lvl) => ladder().filter((f) => f.level <= lvl).pop() || frameById("SCRIBBLER");
function botFrame(lvl, seed) {
  const choices = [...ladder().filter((f) => f.level <= lvl && f.id !== "SCRIBBLER"), ...frameList().filter((f) => f.price > 0 && !f.rune && !f.retired)];
  if (!choices.length) return highestFrame(lvl);
  let h = 7; for (const ch of seed) h = (Math.imul(h, 31) + ch.codePointAt(0)) | 0;
  return choices[(h & 0x7fffffff) % choices.length];
}

const T = {
  tr: { title: "Aylık Lig", global: "Global", friends: "Arkadaşlar", resets: (n) => `${n} gün sonra sıfırlanır`, today: "Bugün sıfırlanıyor", note: "Global sıralama saatte bir yenilenir.", failed: "Global tablo yüklenemedi.", you: (n) => `${n} (Sen)`, xp: (n) => `${n} XP`,
        monthly: "Bu ayki XP'in", rank: (n) => `Sıran #${n}`, unranked: "Henüz sırada değilsin", toTop: (a, b) => `İlk ${a}'ye girmek için ${b} XP daha`, inTop: (a) => `İlk ${a}'desin 🎉`, toPodium: (b) => `Ödül (ilk 3) için ${b} XP daha`, podium: "Ödül sırasındasın! 🏆",
        friendsSoon: "Arkadaş tablosu yakında.", total: "Arkadaşlar · Toplam XP", rewardTop3: "İlk 3'e aylık ödül verilir." },
  en: { title: "Monthly League", global: "Global", friends: "Friends", resets: (n) => `Resets in ${n} days`, today: "Resets today", note: "The global table refreshes hourly.", failed: "Couldn't load the global table.", you: (n) => `${n} (You)`, xp: (n) => `${n} XP`,
        monthly: "Your XP this month", rank: (n) => `Rank #${n}`, unranked: "You're not ranked yet", toTop: (a, b) => `${b} XP more to reach the top ${a}`, inTop: (a) => `You're in the top ${a} 🎉`, toPodium: (b) => `${b} XP more for a prize (top 3)`, podium: "You're in prize position! 🏆",
        friendsSoon: "The friends table is coming soon.", total: "Friends · Total XP", rewardTop3: "Top 3 win the monthly prize." },
};
let tab = "global", table = null, loading = false;

export async function loadLeague() { if (!NAMES.length) NAMES = await (await fetch("league_names.json")).json(); }

async function fetchTable() {
  loading = true; table = null;
  const pid = periodId();
  try {
    const doc = await net.getGlobalLeague();
    const published = ((doc?.humans || doc?.entries || []).filter((r) => typeof r.uid === "string"));
    const rows = doc && doc.periodId === pid ? published : [];
    const humans = rows.map((r) => ({ uid: r.uid, nickname: r.nickname || "?", periodXp: r.periodXp || 0, level: r.level || 1, avatarUrl: "", bot: false }));
    const list = [...humans, ...bots(pid, Date.now(), monthStartMillis())].sort((a, b) => b.periodXp - a.periodXp || (a.nickname < b.nickname ? -1 : 1)).slice(0, 21);
    table = { list, ok: true };
  } catch (e) { table = { list: bots(pid, Date.now(), monthStartMillis()).sort((a, b) => b.periodXp - a.periodXp).slice(0, 21), ok: false }; }
  loading = false;
}

export function layoutLeague(c) {
  const { place, $ } = c, u = c.u();
  place($("lg-back"), 20, 40, 110, 106); place($("lg-refresh"), 711, 40, 110, 106);
  place($("lg-sign"), 150, 96, 541, 235);
  place($("lg-title"), 160, 96 + 235 * 0.415, 530, 235 * 0.36);
  place($("lg-tabs"), 52, 430, 737, 86);
  place($("lg-timer"), 150, 556, 540, 76);
  const l = $("lg-list"); l.style.left = 12 * u + "px"; l.style.top = 655 * u + "px"; l.style.width = 817 * u + "px"; l.style.height = c.stageH() - 655 * u + "px";
}

export async function renderLeague(c, refetch = true) {
  const { $, lang } = c, x = T[lang()];
  $("lg-title").textContent = x.title;
  const tabs = $("lg-tabs"); tabs.replaceChildren();
  for (const k of ["global", "friends"]) {
    const b = document.createElement("button"); b.className = "lg-pill " + (tab === k ? "on" : "off"); b.textContent = x[k];
    b.onclick = () => { tab = k; renderLeague(c, false); }; tabs.append(b);
  }
  const d = daysRemaining(); $("lg-timer").textContent = tab === "friends" ? x.total : d <= 0 ? x.today : x.resets(d);
  $("lg-timer").className = "txt lg-timer " + (tab === "friends" ? "star" : "");
  const list = $("lg-list"); list.replaceChildren();
  if (tab === "friends") { list.append(note(x.friendsSoon)); return; }
  if (refetch || (!table && !loading)) { list.append(note("…")); await fetchTable(); if (c.current() !== "league") return; list.replaceChildren(); }
  if (!table) return;
  if (!table.ok) { list.append(note(x.failed)); }
  list.append(monthlyCard(c, x));
  table.list.forEach((e, i) => list.append(row(c, e, i + 1, x)));
  list.append(note(x.note));
}
const note = (t) => { const n = document.createElement("div"); n.className = "lg-note"; n.textContent = t; return n; };

function monthlyCard(c, x) {
  const { P } = c, my = c.myPeriodXp();
  const others = table.list, top20 = others[19]?.periodXp, podium = others[2]?.periodXp;
  const ahead = others.filter((e) => e.periodXp > my).length, rank = ahead + 1;
  const inTop = rank <= 20, inPodium = rank <= 3;
  const card = document.createElement("div"); card.className = "lg-card";
  const toTop = Math.max(0, (top20 ?? 0) - my + 1), toPod = Math.max(0, (podium ?? 0) - my + 1);
  card.innerHTML = `<div class="lc-h"><b>${x.monthly}</b><span>${x.xp(my.toLocaleString())}</span></div><div class="lc-r">${inTop ? x.rank(rank) : x.unranked}</div><div class="lc-s">${inPodium ? x.podium : inTop ? x.inTop(20) + " · " + x.toPodium(toPod.toLocaleString()) : x.toTop(20, toTop.toLocaleString())}</div>`;
  return card;
}

function row(c, e, rank, x) {
  const r = document.createElement("div"), sprite = rank <= 3;
  r.className = "lg-row" + (sprite ? " s" + rank : "") + (e.me ? " me" : "");
  const frame = e.bot ? botFrame(e.level, e.nickname) : highestFrame(e.level);
  const av = frameRing(frame, "lg-av"); av.style.position = "absolute";
  const nm = document.createElement("div"); nm.className = "lg-nm"; nm.textContent = e.me ? x.you(e.nickname) : e.nickname;
  const lv = document.createElement("small"); lv.textContent = "Lv " + e.level; nm.append(lv);
  const xp = document.createElement("div"); xp.className = "lg-xp"; xp.textContent = x.xp(e.periodXp.toLocaleString());
  if (!sprite) { const n = document.createElement("div"); n.className = "lg-rk"; n.textContent = rank + "."; r.append(n); }
  r.append(av, nm, xp); return r;
}
