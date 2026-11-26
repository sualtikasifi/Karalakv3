import * as net from "./firebase.js";
import { S, fmtNum } from "./i18n.js";
import { reportPlayerDialog } from "./ui.js";
import { childMode } from "./agegate.js";
import { rankLevelHtml } from "./ranks.js";
import { frameRing, frameList, frameById, penList } from "./store.js";
import * as eco from "./economy.js";
import { paintedCard } from "./ui.js";

// ---- Port of LeagueBots.kt: the fillers every phone adds to the published table ----
let NAMES = [], BOTPHOTO = {};
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
// Levels and pace are spread out (the app's LeagueBots.kt does the same): a bot starts anywhere from level 4 to 58 and its pace follows its level
const startDraws = (pid, idx) => { const r = seeded(BigInt(pid) * 7919n + BigInt(idx) * 104729n + 17n); return [r(), r()]; };
function startXp(pid, idx) {
  const [u1, u2] = startDraws(pid, idx), level = 4 + Math.floor(u1 * 55), span = totalXpForLevel(level + 1) - totalXpForLevel(level);
  return totalXpForLevel(level) + Math.floor(u2 * span);
}
const botPace = (pid, idx) => 0.05 + 0.95 * Math.pow(0.3 * seeded(BigInt(pid) * 131n + BigInt(idx) * 977n + 5n)() + 0.7 * startDraws(pid, idx)[0], 2.2);
function botPeriodXp(pid, idx, now, monthStart) {
  const pace = botPace(pid, idx);
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

let tab = "friends", table = null, loading = false, fr = null;

export async function loadLeague() { if (!NAMES.length) NAMES = await (await fetch("league_names.json")).json(); if (!Object.keys(BOTPHOTO).length) BOTPHOTO = await (await fetch("bot_photos.json")).json().catch(() => ({})); }

async function fetchTable() {
  loading = true; table = null;
  const pid = periodId();
  try {
    const doc = await net.getGlobalLeague(); rewardId = doc?.rewardId || null; const mine = await net.myUid().catch(() => ""); lastPeriod = doc?.lastPeriod || null; myId = mine;
    const published = ((doc?.humans || doc?.entries || []).filter((r) => typeof r.uid === "string"));
    const rows = doc && doc.periodId === pid ? published : [];
    const humans = rows.map((r) => ({ uid: r.uid, nickname: r.nickname || "?", periodXp: r.periodXp || 0, level: r.level || 1, avatarUrl: "", bot: false, me: r.uid === mine, frameId: r.frameId }));
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
  place($("lg-timer"), 0, 556, 841, 78);
  const l = $("lg-list"); l.style.left = 12 * u + "px"; l.style.top = 655 * u + "px"; l.style.width = 817 * u + "px"; l.style.height = c.fillH() - 655 * u + "px";
}

export async function renderLeague(c, refetch = true) {
  const { $, lang } = c;
  $("lg-title").textContent = S("league_title");
  const tabs = $("lg-tabs"); tabs.replaceChildren();
  // A player who may be under 13 (agegate.js) has no friends list and meets no other player: the global table only, no
  // report, and a word on how they join it (a Google account, which only the app has; the web never ranks them).
  if (childMode()) tab = "global";
  for (const k of childMode() ? ["global"] : ["friends", "global"]) {
    const b = document.createElement("button"); b.className = "lg-pill " + (tab === k ? "on" : "off"); b.textContent = S("league_tab_" + k);
    b.onclick = () => { tab = k; renderLeague(c, false); }; tabs.append(b);
  }
  $("lg-refresh").style.display = tab === "global" ? "" : "none";
  const t = timerText(); $("lg-timer").innerHTML = `<span class="chip">${tab === "friends" ? S("league_friends_total_caption") : t}</span>`;
  $("lg-timer").className = "txt lg-timer " + (tab === "friends" ? "star" : "");
  clearInterval(tick); if (tab === "global" && daysRemaining() <= 0) tick = setInterval(() => { if (c.current() !== "league") return clearInterval(tick); $("lg-timer").firstChild.textContent = timerText(); }, 1000);
  const list = $("lg-list"); list.replaceChildren();
  if (net.isScoreRefused()) list.append(note("⚠️ " + ({ tr: "Puanın şu an sunucuya yazılamıyor, bu yüzden diğer oyuncular seni bu tabloda göremeyebilir. Sayfayı yenile ya da biraz sonra tekrar dene.", en: "Your score can't be saved to the server right now, so other players may not see you in this table. Reload or try again later.", pt: "Sua pontuação não pode ser salva no servidor agora, então outros jogadores podem não te ver nesta tabela. Recarregue a página ou tente mais tarde.", es: "Tu puntuación no se puede guardar en el servidor ahora, así que otros jugadores quizá no te vean en esta tabla. Recarga la página o inténtalo más tarde." })[c.lang()]));
  if (tab === "friends") {
    if (refetch || !fr) { list.append(note("…")); try { fr = await net.friendsLeague(periodId()); } catch { fr = []; } if (c.current() !== "league" || tab !== "friends") return; list.replaceChildren(); }
    // my own row is always there, even before this browser has published a profile
    if (!fr.some((e) => e.me)) fr = [...fr, { me: true, nickname: c.P.nick, periodXp: 0, totalXp: c.P.xp, level: levelForXp(c.P.xp) }].sort((a, b) => b.totalXp - a.totalXp);
    const hintAt = list.children.length;
    if (fr.length <= 1) {
      const h = document.createElement("div"); h.className = "lg-card lg-hint"; h.innerHTML = `<img src="img/friends_handshake.webp" alt=""><div class="lh-t"><span>${S("league_friends_hint")}</span><b>${S("league_friends_hint_action")}</b></div>`;
      h.onclick = () => c.show("friends"); list.append(h);
    }
    fr.forEach((e, i) => list.append(row(c, { ...e, nickname: e.me ? c.P.nick : e.nickname, level: e.me ? levelForXp(c.P.xp) : e.level, periodXp: e.totalXp }, i + 1)));
    return;
  }
  if (refetch || (!table && !loading)) { list.append(note("…")); await fetchTable(); collectPrize(c); if (c.current() !== "league" || tab !== "global") return; list.replaceChildren(); }
  if (!table) return;
  if (!table.ok) list.append(note(S("league_global_failed")));
  if (childMode()) list.append(note("🏆 " + ({ tr: "Aylık sıralamaya Android uygulamasında Google hesabınla katılabilirsin.", en: "You can join the monthly ranking with your Google account in the Android app.", pt: "Você pode entrar no ranking mensal com sua conta Google no app Android.", es: "Puedes entrar en la clasificación mensual con tu cuenta de Google en la app de Android." })[c.lang()]));
  const bn = banner(c.lang()); if (bn) list.append(bn);
  list.append(monthlyCard(c));
  table.list.forEach((e, i) => list.append(row(c, e, i + 1)));
  list.append(note(S("league_global_refresh_note")));
}
let tick = 0, rewardId = null, lastPeriod = null, myId = "";
// PenStrokePreview: a wide curve drawn on in the pen's own gradient (65% drawing, then a hold, then a fade), looping every 4.2 s
let penRaf = 0;
function penPreview(cv, colors) {
  cancelAnimationFrame(penRaf);
  const t0 = performance.now(), ease = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
  const loop = (now) => {
    if (!cv.isConnected) return;
    if (!cv.width) { const r = cv.getBoundingClientRect(), k = Math.min(2, devicePixelRatio || 1); cv.width = Math.max(1, r.width * k); cv.height = Math.max(1, r.height * k); }
    const W = cv.width, H = cv.height, g = cv.getContext("2d"), cycle = ((now - t0) % 4200) / 4200, draw = Math.min(1, cycle / 0.65), fade = Math.max(0, (cycle - 0.8) / 0.2);
    g.clearRect(0, 0, W, H); g.globalAlpha = 1 - fade;
    const pts = []; for (let i = 0; i <= 60; i++) { const t = i / 60, m = 1 - t; pts.push([m * m * m * W * 0.04 + 3 * m * m * t * W * 0.28 + 3 * m * t * t * W * 0.6 + t * t * t * W * 0.96, m * m * m * H * 0.75 + 3 * m * m * t * H * 0.05 + 3 * m * t * t * H * 1.05 + t * t * t * H * 0.25]); }
    const gr = g.createLinearGradient(0, 0, W, 0); colors.forEach((k, i) => gr.addColorStop(colors.length > 1 ? i / (colors.length - 1) : 0, k));
    g.strokeStyle = gr; g.lineWidth = H * 0.3; g.lineCap = "round"; g.lineJoin = "round"; g.beginPath();
    const n = Math.max(1, Math.floor(pts.length * ease(draw))); pts.slice(0, n + 1).forEach((p, i) => (i ? g.lineTo(p[0], p[1]) : g.moveTo(p[0], p[1]))); g.stroke();
    penRaf = requestAnimationFrame(loop);
  };
  penRaf = requestAnimationFrame(loop);
}
function banner(lang) {
  const id = rewardId || "", isPen = id.startsWith("PEN:"), key = id.replace(/^(PEN|FRAME):/, "");
  const pen = isPen ? penList().find((p) => p.id === key) : null, frame = !isPen && id ? frameList().find((f) => f.id === key) : null;
  if (!pen && !frame) return null;
  const b = document.createElement("div"); b.className = "lg-banner";
  const icon = document.createElement("div"); icon.className = "lb-i";
  if (frame) icon.append(frameRing(frame, "lb-fr")); else icon.textContent = "🏆";
  const label = pen ? pen[lang] || pen.en : S("league_reward_kind_frame") + (key.startsWith("LEAGUE_CHAMPION_") ? " · " + key.replace("LEAGUE_CHAMPION_", "").replace(/_/g, "-") : "");
  const mid = document.createElement("div"); mid.className = "lb-m"; mid.innerHTML = `<small>${S("league_reward_title")}</small><b>${label}</b>`;
  if (pen) { const cv = document.createElement("canvas"); cv.className = "lb-pen"; mid.append(cv); penPreview(cv, pen.colors); }
  const r = document.createElement("div"); r.className = "lb-r"; r.innerHTML = `<span>${S("league_reward_bonus_gold", fmtNum(eco.CFG.leagueMonthlyGold))}</span><span>${S("league_reward_bonus_xp", fmtNum(10000))}</span><small>${S("league_reward_top3")}</small>`;
  b.append(icon, mid, r); return b;
}
function timerText() {
  const d = daysRemaining();
  if (d > 0) return S(d === 1 ? "league_resets_in_one" : "league_resets_in", d);
  const t = turkeyToday(), end = Date.parse(`${t.m === 12 ? t.y + 1 : t.y}-${String(t.m === 12 ? 1 : t.m + 1).padStart(2, "0")}-01T00:00:00+03:00`);
  const left = Math.max(0, end - Date.now()), p = (n) => String(Math.floor(n)).padStart(2, "0");
  return S("league_resets_countdown", `${p(left / 3600000)}:${p(left / 60000 % 60)}:${p(left / 1000 % 60)}`);
}
const note = (t) => { const n = document.createElement("div"); n.className = "lg-note"; n.textContent = t; return n; };

function monthlyCard(c) {
  const my = c.myPeriodXp(), others = table.list, top20 = others[19]?.periodXp, podium = others[2]?.periodXp;
  const rank = others.filter((e) => e.periodXp > my).length + 1, inTop = rank <= 20, inPodium = rank <= 3;
  const card = document.createElement("div"); card.className = "lg-card";
  const toTop = Math.max(0, (top20 ?? 0) - my + 1), toPod = Math.max(0, (podium ?? 0) - my + 1), n = (v) => fmtNum(v);
  const pct = Math.min(100, Math.round(100 * my / Math.max(1, top20 ?? 1)));
  card.innerHTML = `<div class="lc-h"><div><small>${S("league_monthly_title")}</small><b>${S("league_xp_format", my)}</b></div><span>${inTop ? S("league_monthly_rank", rank) : S("league_monthly_unranked")}</span></div>${top20 != null && !inTop ? `<div class="lc-bar"><i style="width:${pct}%"></i></div>` : ""}<div class="lc-s">${inPodium ? S("league_monthly_in_podium") : inTop ? S("league_monthly_in_top", 20) + " · " + S("league_monthly_to_podium", n(toPod)) : S("league_monthly_to_top", 20, n(toTop))}</div>${!inTop && podium != null ? `<div class="lc-s">${S("league_monthly_to_podium", n(toPod))}</div>` : ""}`;
  return card;
}

// LevelAvatar's PersonaFace: a filler wears the photo its name picks (BotPhotos); any other name gets a generated face
const LETTER = ["#1A73E8", "#D93025", "#188038", "#E37400", "#9334E6", "#12B5CB", "#E52592", "#5F6368"];
const GRAD = [["#FFE0B2", "#FFB74D"], ["#C8E6C9", "#81C784"], ["#BBDEFB", "#64B5F6"], ["#F8BBD0", "#F06292"], ["#D1C4E9", "#9575CD"], ["#B2EBF2", "#4DD0E1"], ["#FFF59D", "#FFD54F"]];
const EMOJI = ["🦊", "🐼", "🎨", "⚽", "🎸", "🌸", "🚀", "🐱", "🎮", "🌵", "🍕", "🐶", "🦁", "📚", "🏀", "🐧"];
function personaFace(seed) {
  const d = document.createElement("div");
  if (BOTPHOTO[seed]) { const i = document.createElement("img"); i.src = `img/avatars/${BOTPHOTO[seed]}.webp`; i.alt = ""; i.style.cssText = "width:100%;height:100%;object-fit:cover"; d.append(i); return d; }
  let h = 11; for (const ch of seed) h = (Math.imul(h, 31) + ch.codePointAt(0)) | 0; h &= 0x7fffffff;
  const b = h % 20; d.style.cssText = "display:flex;align-items:center;justify-content:center;color:#fff;font-weight:500;line-height:1";
  if (b <= 2) { const i = document.createElement("img"); i.src = "img/avatar_dino.webp"; i.alt = ""; i.style.cssText = "width:100%;height:100%"; d.append(i); }
  else if (b <= 9) { d.style.background = LETTER[Math.floor(h / 20) % LETTER.length]; d.style.fontSize = "calc(var(--u)*40)"; d.textContent = ([...seed].find((x) => /[A-Za-zÇĞİÖŞÜçğıöşü]/.test(x)) || "K").toUpperCase(); }
  else { const g = GRAD[Math.floor(h / 20) % GRAD.length]; d.style.background = `linear-gradient(${g[0]},${g[1]})`; d.style.fontSize = "calc(var(--u)*44)"; d.textContent = EMOJI[Math.floor(h / 140) % EMOJI.length]; }
  return d;
}
function row(c, e, rank) {
  const r = document.createElement("div"), sprite = rank <= 3;
  r.className = "lg-row" + (sprite ? " s" + rank : "") + (e.me ? " me" : "");
  const frame = e.bot ? botFrame(e.level, e.nickname) : (e.frameId && frameById(e.frameId)) || highestFrame(e.level);
  const av = frameRing(frame, "lg-av", e.bot ? personaFace(e.nickname) : null); av.style.position = "absolute";
  const nm = document.createElement("div"); nm.className = "lg-nm"; nm.textContent = e.me ? S("quick_match_you_named", e.nickname) : e.nickname;
  const lv = document.createElement("small"); lv.className = "rl"; lv.innerHTML = rankLevelHtml(e.level, true); nm.append(lv);
  const xp = document.createElement("div"); xp.className = "lg-xp"; xp.textContent = S("league_xp_format", e.periodXp);
  if (!sprite) { const n = document.createElement("div"); n.className = "lg-rk"; n.textContent = rank + "."; r.append(n); }
  r.append(av, nm, xp);
  // a real player other than you can be reported from their row (LeagueScreen); an AI player's row is nobody to report
  if (!childMode() && !e.me && !e.bot && e.uid) { r.style.cursor = "pointer"; r.title = S("report_player_action"); r.onclick = () => reportPlayerDialog(net, e.uid, e.nickname, "league"); }
  return r;
}

// ---------- last month's prize (LeagueViewModel.collectPrize + LeaguePrizeDialog) ----------
/**
 * Hands over last month's prize once, if this account was among the winners: the monthly gold and XP and the cosmetic itself.
 * ONE gate per MONTH ("PERIOD:<id>"), not per prize id: the id can be pinned from the review panel and repeat month after month.
 */
function collectPrize(c) {
  const P = c.P, lp = lastPeriod; if (!lp || !Array.isArray(lp.winners) || !myId) return;
  const win = lp.winners.find((w) => w && w.uid === myId); if (!win) return;
  const earned = (P.leagueEarned = P.leagueEarned || []), gate = "PERIOD:" + lp.periodId;
  if (earned.includes(gate)) return;
  earned.push(gate);
  const rid = typeof lp.rewardId === "string" ? lp.rewardId : "";
  if (rid) { if (!earned.includes(rid)) earned.push(rid); const own = rid.replace(/^PEN:/, "pen:").replace(/^FRAME:/, "frame:"); if (/^(pen|frame):/.test(own) && !(P.owned = P.owned || []).includes(own)) P.owned.push(own); }
  P.gold = (P.gold || 0) + eco.CFG.leagueMonthlyGold; eco.addXp(P, 10000, 0);
  c.saveP(); c.refreshHome && c.refreshHome();
  prizeDialog(c, rid, win.rank | 0);
}
const WIN = { w: 1229, h: 1280, title: [150, 300, 1080, 395], prize: [200, 410, 1030, 585], body: [150, 598, 1080, 715], gold: [350, 748, 560, 842], xp: [770, 748, 1000, 842], button: [262, 895, 940, 1085] };
function prizeDialog(c, rid, rank) {
  const card = paintedCard(c.$("stage"), { art: "league_win_frame", w: WIN.w, h: WIN.h, cls: "lw" }), { put, fit } = card;
  card.card.setAttribute("role", "dialog");
  const t = put("pc-t", WIN.title, "<span></span>"); t.firstChild.textContent = S("league_win_title", rank); fit(t, 92, 1, 0.5);
  const isPen = rid.startsWith("PEN:"), key = rid.replace(/^(PEN|FRAME):/, "");
  const pen = isPen ? penList().find((p) => p.id === key) : null, frame = !isPen && rid ? frameList().find((f) => f.id === key) : null;
  if (pen || frame) {
    const box = put("lw-prize", WIN.prize, ""), lang = c.lang();
    if (pen) { const cv = document.createElement("canvas"); cv.className = "lw-pen"; box.append(cv); penPreview(cv, pen.colors); }
    else box.append(frameRing(frame, "lw-fr"));
    const l = document.createElement("div"); l.className = "lw-l"; const sp = document.createElement("span");
    sp.textContent = pen ? pen[lang] || pen.en : S("league_reward_kind_frame") + (key.startsWith("LEAGUE_CHAMPION_") ? " · " + key.replace("LEAGUE_CHAMPION_", "").replace(/_/g, "-") : ""); l.append(sp); box.append(l);
    fit(l, 84, 1, 0.5);
  }
  const b = put("lw-b", WIN.body, "<span></span>"); b.firstChild.textContent = S("league_win_body"); fit(b, 46, 2, 0.6);
  const g = put("pc-t", WIN.gold, "<span></span>"); g.firstChild.textContent = "+" + fmtNum(eco.CFG.leagueMonthlyGold); fit(g, 46, 1, 0.5);
  const x = put("pc-t", WIN.xp, "<span></span>"); x.firstChild.textContent = S("league_reward_bonus_xp", fmtNum(10000)).replace(/^\+?/, "+"); fit(x, 46, 1, 0.5);
  const btn = put("pc-btn lw-btn", WIN.button, '<span class="pc-lt"></span>'); btn.style.setProperty("--ol", "#8a3a00"); btn.setAttribute("role", "button");
  btn.firstChild.textContent = S("close"); fit(btn, 88, 1, 0.5); btn.onclick = () => card.close();
  return card;
}
export { prizeDialog };
