import { appOnlyDialog, appWindow, primary, PLAY_URL } from "./ui.js";
import { getLang, S } from "./i18n.js";
// Giriş Serisi: the 7-day login calendar (port of LoginStreak.kt / LoginStreakDialog.kt). "Today" is the server's day
// (Date header of our own hosting), never the phone's clock.
import { openNow } from "./chests.js";
import { scaled } from "./economy.js";
import { turkeyMonth, todayEpoch } from "./daily.js";
const REW = [null, { gold: 300 }, { gold: 400 }, { jokers: true }, { gold: 500 }, { chest: "GOLD" }, { gold: 800 }, { chest: "RARE" }];
const JK = ["FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"];
const T = {
  tr: { title: "Giriş Serisi", day: (n) => `${n}. gün`, claim: "Ödülü Al", claimed: "Ödül alındı!", great: "Harika!", missed: (n) => `${n} günlük serin kesildi. Seriyi kurtarıp devam edebilir ya da baştan başlayabilirsin.`, save: "Seriyi kurtar", restart: "Baştan başla", rescued: "Seri kurtarıldı! Bugünün ödülünü al.", mile: (n) => `🎉 ${n}. gün bonusu: Sürpriz kasa açılıyor!`, joker: "Joker", chest: "Kasa", streak: (n) => `${n} günlük seri` },
  en: { title: "Login Streak", day: (n) => `Day ${n}`, claim: "Claim reward", claimed: "Reward claimed!", great: "Great!", missed: (n) => `Your ${n}-day streak was cut off. Save it to carry on, or start again.`, save: "Save the streak", restart: "Start again", rescued: "Streak saved! Take today's reward.", mile: (n) => `🎉 Day ${n} bonus: a Surprise chest is opening!`, joker: "Joker", chest: "Chest", streak: (n) => `${n}-day streak` },
};
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const dayOf = (ms) => Math.floor((ms + 3 * 3600e3) / 86400e3); // Turkey (UTC+3) calendar day

async function serverDay() {
  try {
    const r = await fetch("./index.html?t=" + Date.now(), { method: "HEAD", cache: "no-store" });
    const d = Date.parse(r.headers.get("Date") || ""); if (d) return dayOf(d);
  } catch {}
  return null;
}
export function state(L, today) {
  if (L.last === today) return { k: "done" };
  if (L.last < 0 || L.streak <= 0) return { k: "claim", before: 0 };
  if (L.last === today - 1) return { k: "claim", before: L.streak };
  if (L.last > today) return { k: "done" };
  return { k: "missed", lost: L.streak };
}
let today = null, checking = false, handled = false, rescueSeen = false;
const RESCUE = { tr: "Uygulamada kurtar", en: "Save in the app" };
/**
 * MainMenuScreen's StreakRescueDialog: a daily-challenge streak that lapsed for one to three days is offered back the moment the menu opens.
 * In the app that costs an ad; the browser has none, so the button sends the player to the app. Returns true while the offer is on screen
 * (the login-streak window waits behind it, as in the app); `then` runs when it is closed.
 */
export function dailyRescue(c, then) {
  const d = c.P.daily, gap = d.last < 0 ? 1e9 : todayEpoch() - d.last;
  if (rescueSeen || d.streak <= 0 || gap <= 1 || gap > 3) return false;
  rescueSeen = true;
  const body = el("div", "ao-body", `<div class="ao-ic">🔥</div><p>${S("streak_rescue_message")}</p>`), foot = el("div", "aw-btns");
  const ov = appWindow(c.$("stage"), { title: S("streak_rescue_title", d.streak), body, footer: foot, centerTitle: true, onClose: then });
  foot.append(primary(RESCUE[getLang()] || RESCUE.tr, () => { window.open(PLAY_URL, "_blank", "noopener"); ov.close(); }), primary(S("streak_rescue_dismiss"), () => ov.close(), "alt"));
  return true;
}
export async function checkStreak(c) {
  const P = c.P; P.login = P.login || { streak: 0, last: -1 };
  if (handled || checking) return; checking = true;
  const d = await serverDay(); checking = false; if (d == null) return; today = d; handled = true;
  const st = state(P.login, d);
  if (st.k === "claim") dialog(c, { k: "claim", before: st.before });
  // The Seri Sigortası rune: once a month a single missed day is forgiven and the streak carries on; the cover is spent when the reward is taken.
  else if (st.k === "missed") { if (P.login.last === d - 2 && c.runeHas("INSURE") && P.insMonth !== turkeyMonth()) dialog(c, { k: "claim", before: st.lost, rescued: true, insured: true }); else dialog(c, st); }
}
function dialog(c, popup) {
  const { $, P, lang } = c, x = T[lang()], d = $("pick"); d.classList.remove("hidden"); d.replaceChildren(); d.onclick = null;
  const before = popup.k === "claim" ? popup.before : popup.k === "missed" ? popup.lost : popup.streak - 1;
  const todayCell = popup.k === "claimed" ? popup.day : popup.k === "missed" ? 0 : before % 7 + 1;
  const claimed = popup.k === "claimed", close = () => { d.classList.add("hidden"); if (claimed && popup.chests.length) popup.chests.forEach((t) => openNow(c, t)); };
  const wrap = el("div", "ls-wrap"), fr = el("div", "ls-frame"); wrap.append(fr);
  fr.append(el("img", "ls-bg", ""));
  fr.firstChild.src = c.lang() === "tr" ? "img/ls_frame.webp" : "img/ls_frame_en.webp";
  const hint = el("div", "ls-hint"); fr.append(hint);
  if (popup.k === "missed") hint.textContent = x.missed(popup.lost); else if (popup.rescued) { hint.textContent = x.rescued; hint.className += " ok"; } else if (claimed && popup.milestone) { hint.textContent = x.mile(popup.streak); hint.className += " mile"; } else if (claimed) { hint.textContent = x.claimed; hint.className += " big"; } else hint.textContent = x.streak(before);
  const grid = el("div", "ls-grid");
  for (let n = 1; n <= 7; n++) {
    const r = REW[n], done = claimed ? n <= todayCell : todayCell > 0 && n < todayCell, isToday = n === todayCell && !claimed, just = claimed && n === todayCell;
    const art = r.chest ? (r.chest === "RARE" ? "pink" : "blue") : r.jokers ? "joker" : "gold";
    const gold = r.gold ? scaled(r.gold, c.runeFx("LOGIN")) : 0;
    const card = el("div", "ls-card" + (isToday ? " today" : "") + (done ? " done" : "") + (just ? " just" : ""), `<img src="img/ls_card_${art}.webp" alt=""><span class="l1">${x.day(n)}</span><span class="l2">${r.chest ? x.chest : r.jokers ? x.joker : gold}</span><i class="tick">${done ? "✓" : ""}</i>`);
    grid.append(card);
  }
  fr.append(grid);
  const act = el("div", "ls-act"); fr.append(act);
  const pill = (t, cls, fn) => { const b = el("button", "pill " + cls + " small", `<span>${t}</span>`); b.onclick = fn; return b; };
  if (popup.k === "claim") {
    const b = el("button", "ls-claim", ""); b.append(Object.assign(new Image(), { src: c.lang() === "tr" ? "img/ls_btn_claim.webp" : "img/ls_btn_claim_en.webp", alt: x.claim })); b.onclick = () => claim(c, popup);
    act.append(b);
  } else if (popup.k === "missed") act.append(pill(getLang() === "tr" ? "Uygulamada kurtar" : "Save in the app", "orange", () => appOnlyDialog(c.$("stage"))), pill(x.restart, "cream", () => dialog(c, { k: "claim", before: 0 })));
  else act.append(pill(x.great, "orange", close));
  if (popup.k !== "missed") { const cx = el("button", "ls-x", ""); cx.onclick = close; fr.append(cx); d.onclick = (e) => { if (e.target === d) close(); }; }
  d.append(wrap);
}
function claim(c, popup) {
  const P = c.P; if (P.login.last >= today) { $("pick").classList.add("hidden"); return; }
  const day = popup.before % 7 + 1, r = REW[day], streak = popup.before + 1;
  if (popup.insured) P.insMonth = turkeyMonth();
  const gold = r.gold ? scaled(r.gold, c.runeFx("LOGIN")) : 0; if (gold) P.gold += gold;
  const jokers = r.jokers ? [...JK] : [];
  // The Şanslı Zar rune: a 25% chance of one more joker with the day's reward.
  if (c.runeHas("DICE") && Math.random() < 0.25) jokers.push(JK[Math.floor(Math.random() * JK.length)]);
  for (const j of jokers) P.jokers[j] = (P.jokers[j] || 0) + 1;
  P.login = { streak, last: today }; P.stats.loginBest = Math.max(P.stats.loginBest || 0, streak);
  const milestone = streak % 30 === 0, chests = [r.chest, milestone ? "RARE" : null].filter(Boolean);
  c.saveP(); c.refreshHome(); dialog(c, { k: "claimed", day, streak, milestone, chests });
}
function $(id) { return document.getElementById(id); }
