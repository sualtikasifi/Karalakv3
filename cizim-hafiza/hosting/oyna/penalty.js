// Penalties, lockouts and bans — the web side of PenaltyRepositoryImpl, MainMenuScreen's PenaltyDialog and QuickMatchScreen's lock scene.
// A reviewer (or the developer panel) writes penalties/{id}; this device acknowledges each once (appliedAt), takes the XP/gold, and
// from then on Hızlı Eşleş and the online rooms stay shut until the lock runs out (or for good, for a ban).
import { S, getLang } from "./i18n.js";
import { el, paintedCard } from "./ui.js";
import * as net from "./firebase.js";
import * as eco from "./economy.js";

const PERMANENT_LOCK_MILLIS = 4_102_444_800_000, CHECK_EVERY_MS = 30 * 60 * 1000, TEN_YEARS = 10 * 365 * 24 * 3600 * 1000;
export const isPermanent = (until) => until >= Date.now() + TEN_YEARS;
export { PERMANENT_LOCK_MILLIS };

/** "15 dakika" / "2 saat" / "3 gün": whole days only when the lock is an exact number of them (the penalty window), rounded up otherwise. */
export function lockLength(minutes, exactDays = false) {
  minutes = Math.max(1, minutes);
  if (minutes < 60) return S(minutes === 1 ? "lock_duration_minutes_one" : "lock_duration_minutes", minutes);
  if (minutes >= 1440 && (!exactDays || minutes % 1440 === 0)) { const d = exactDays ? minutes / 1440 : Math.ceil(minutes / 1440); return S(d === 1 ? "lock_duration_days_one" : "lock_duration_days", d); }
  const h = Math.ceil(minutes / 60); return S(h === 1 ? "lock_duration_hours_one" : "lock_duration_hours", h);
}

// penalty_card (1064 x 1383): the angry dog snapping a pencil; the words go over the board at fixed spots of the picture
const CARD = { w: 1064, h: 1383, title: [170, 495, 910, 605], message: [170, 615, 910, 900], warning: [170, 910, 910, 1020], button: [205, 1095, 860, 1270] };

function penaltyTexts(p) {
  const message = p.goldRevoked > 0 ? S("penalty_body_report", p.xpRevoked, p.goldRevoked) : p.breaksDailyStreak ? S("penalty_body_daily", p.xpRevoked) : S("penalty_body", p.xpRevoked);
  let warning;
  if (isPermanent(p.lockedUntil)) warning = S("penalty_banned_warning");
  else if (p.lockedUntil > 0) warning = S("penalty_locked", p.strike, lockLength(Math.max(1, Math.ceil(Math.max(0, p.lockedUntil - p.createdAt) / 60000)), true));
  else if (p.strike > 0) warning = S("penalty_strikes", p.strike);
  else warning = S("penalty_warn_next");
  return { message, warning };
}

/** Takes what the penalties took (XP, gold, daily streak, league ban). Each is marked on the server FIRST: a failed mark means "try again next time", never "take it twice". */
async function apply(c, list) {
  const P = c.P; let applied = 0;
  for (const p of list) {
    try { await net.markPenaltyApplied(p.id); } catch { continue; }
    eco.revokeXp(P, p.xpRevoked); P.gold = Math.max(0, (P.gold || 0) - p.goldRevoked);
    P.penaltiesApplied = (P.penaltiesApplied | 0) + 1;
    if (isPermanent(p.lockedUntil)) P.leagueBanned = true;
    if (p.breaksDailyStreak && P.daily) P.daily.streak = 0;
    applied++;
  }
  if (applied) { c.saveP(); c.refreshHome && c.refreshHome(); }
}

/** One painted window for the newest outstanding penalty; closing it (button or tap outside) applies every outstanding one. */
export function penaltyDialog(c, list) {
  const top = list[0], { message, warning } = penaltyTexts(top);
  let done = false; const finish = () => { if (!done) { done = true; apply(c, list); } };
  const card = paintedCard(c.$("stage"), { art: "penalty_card", w: CARD.w, h: CARD.h, cls: "pen", onClose: finish }), { put, fit } = card;
  card.card.setAttribute("role", "alertdialog"); card.card.setAttribute("aria-label", S("penalty_title"));
  const t = put("pen-t", CARD.title, "<span></span>"); t.firstChild.textContent = S("penalty_title"); fit(t, 72, 2, 0.5);
  const m = put("pen-m", CARD.message, "<span></span>"); m.firstChild.textContent = message; fit(m, 44, 6, 0.6);
  const w = put("pen-w", CARD.warning, "<span></span>"); w.firstChild.textContent = warning; fit(w, 44, 2, 0.6);
  const b = put("pc-btn pen-b", CARD.button, '<span class="pc-lt"></span>'); b.style.setProperty("--ol", "#8a3a00"); b.setAttribute("role", "button");
  b.firstChild.textContent = S("penalty_understood"); fit(b, 84, 1, 0.5); b.onclick = () => card.close();
  return card;
}

/** Looks for unacknowledged penalties at most every half hour (a penalty waits for its device) and shows them. Quiet when there is none or the read fails. */
export async function checkPenalties(c) {
  let last = 0; try { last = Number(localStorage.getItem("penaltyCheck")) || 0; } catch {}
  if (Date.now() - last < CHECK_EVERY_MS) return;
  let list; try { list = await net.outstandingPenalties(); } catch { return; }
  if (!list.length) { try { localStorage.setItem("penaltyCheck", String(Date.now())); } catch {} return; }
  penaltyDialog(c, list);
}

// bg_locked (841 x 1870, one per language): [countdown top, countdown height, reason top, reason height] on that language's picture
const LOCK_ART = { tr: [992, 92, 1088, 118], en: [1020, 72, 1096, 110], es: [1020, 72, 1096, 110], pt: [1020, 72, 1096, 110] };
const LW = 841, LH = 1870;

/**
 * The gate for Hızlı Eşleş and the online rooms: true when the way is free. Otherwise the lock scene (live countdown, then the way opens by
 * itself) or, for a ban, the permanent notice is shown and false is returned. A failed read means "no lock" — as in the app.
 */
export async function lockGate(c, onFree) {
  const until = await net.lockedUntilMillis();
  if (!until) return true;
  if (isPermanent(until)) { lockedBan(c); return false; }
  lockScene(c, until, onFree); return false;
}

function lockedBan(c) {
  import("./ui.js").then(({ paintedFrameDialog }) => paintedFrameDialog(c.$("stage"), { title: S("quick_match_banned_title"), body: S("quick_match_banned_body"), buttonText: S("penalty_understood") }));
}

export function lockScene(c, until, onFree) {
  const lang = ["tr", "es", "pt"].includes(getLang()) ? getLang() : "en", [pt, ph, bt, bh] = LOCK_ART[lang];
  const ov = el("div", "lk-ov"), box = el("div", "lk-box"); ov.append(box);
  const img = el("img"); img.src = `img/bg_locked_${lang}.webp`; img.alt = ""; box.append(img);
  const at = (cls, x, y, w, h, html) => { const e = el("div", cls, html); e.style.cssText = `left:${x / LW * 100}%;top:${y / LH * 100}%;width:${w / LW * 100}%;height:${h / LH * 100}%`; box.append(e); return e; };
  const clock = at("lk-clock", 190, pt, 460, ph, "<span></span>"); clock.style.setProperty("--ph", ph);
  const why = at("lk-why", 110, bt, 620, bh, "<span></span>");
  why.firstChild.textContent = S("lock_screen_body");
  const back = el("button", "lk-back", "‹"); back.setAttribute("aria-label", "Back"); ov.append(back);
  let timer = 0; const close = () => { clearInterval(timer); window.removeEventListener("resize", size); ov.remove(); };
  // the picture is cropped to fill the screen (centred), like the app; everything is laid out in the picture's own pixels
  const size = () => {
    const W = ov.clientWidth || innerWidth, H = ov.clientHeight || innerHeight, k = Math.max(W / LW, H / LH);
    box.style.cssText = `width:${LW * k}px;height:${LH * k}px;left:${(W - LW * k) / 2}px;top:${(H - LH * k) / 2}px`; box.style.setProperty("--k", k.toFixed(5));
  };
  const tick = () => {
    const left = Math.max(0, Math.floor((until - Date.now()) / 1000)), days = Math.floor(left / 86400);
    const hms = [Math.floor(left % 86400 / 3600), Math.floor(left % 3600 / 60), left % 60].map((n) => String(n).padStart(2, "0")).join(":");
    clock.firstChild.textContent = days > 0 ? S("lock_countdown_days", days, hms) : hms;
    if (left <= 0) { close(); onFree && onFree(); }
  };
  back.onclick = close; window.addEventListener("resize", size); c.$("stage").append(ov); size(); tick(); timer = setInterval(tick, 1000);
  return ov;
}
