// Chests (kasalar): won by beating a Quick Match opponent or a room, unlocked on a timer, opened for gold, jokers and sometimes a pen.
// A port of ChestStore.kt (the rules), ChestDetail.kt (the window), ChestWonDialog.kt (the win scene), ChestComponents.kt (the opening
// scene) and ChestInfoDialog.kt. The texts are the app's own (i18n.js / strings.json).
import { penList } from "./store.js";
import { S, fmtNum, ready as strReady } from "./i18n.js";
import { el, primary, appWindow, paintedCard, appOnlyDialog } from "./ui.js";
import { TIER, backdrop, wonScene, rewardScene, rgba, glowOf } from "./chestfx.js";
import { todayEpoch } from "./daily.js";

const SLOTS = 4, JOKERS = ["FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"], GOLD = { SILVER: [150, 250], GOLD: [450, 700], RARE: [1500, 2500] };
const JOKER_TINT = { FIRST_LETTER: "#8E4FE0", LETTER_COUNT: "#1E8FD6", EXTRA_TIME: "#E8672A" }, JOKER_ART = { FIRST_LETTER: "joker_first_letter", LETTER_COUNT: "joker_letter_count", EXTRA_TIME: "joker_extra_time" }, JOKER_SHORT = { FIRST_LETTER: "joker_first_letter_short", LETTER_COUNT: "joker_letter_count_short", EXTRA_TIME: "joker_extra_time_short" };
const rnd = (a, b) => a + Math.floor(Math.random() * (b - a + 1));

export function ensure(P) { P.chests = P.chests || { slots: [null, null, null, null], idx: 0, seed: Math.floor(Math.random() * 2 ** 31) + 1 }; return P.chests; }
function bagTier(seed, idx) {
  const bag = [...Array(144).fill("SILVER"), ...Array(72).fill("GOLD"), ...Array(24).fill("RARE")];
  let a = seed | 0; const r = () => { a = (a + 0x6d2b79f5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
  for (let i = bag.length - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [bag[i], bag[j]] = [bag[j], bag[i]]; }
  return bag[((idx % 240) + 240) % 240];
}
/**
 * A won match's chest. The tier cycle advances every time; with every slot taken the chest is lost — unless the Yedek Yuva rune
 * is bought and its reserve slot is free, then it waits there (ChestStore.awardChestForWin). Returns the chest or null.
 */
export function award(P, hasRune) {
  const C = ensure(P), free = C.slots.findIndex((s) => !s), tier = bagTier(C.seed, C.idx++);
  const chest = { id: Math.random().toString(36).slice(2), tier, start: null };
  if (free < 0) { if (hasRune && hasRune("RESERVE") && !C.reserve) C.reserve = chest; return null; }
  C.slots[free] = chest; return chest;
}
const rawMs = (tier) => TIER[tier].ms;
/** How long a chest takes once started, with the chest-time rune taken off (RuneLive.chestMillis). */
const effMs = (c, tier) => Math.floor(rawMs(tier) * (100 - c.runeFx("CTIME")) / 100);
const isReady = (ch, now = Date.now()) => ch.start != null && now >= ch.start + rawMs(ch.tier);
const remaining = (c, ch, now) => (ch.start == null ? effMs(c, ch.tier) : Math.max(0, ch.start + rawMs(ch.tier) - now));
const anyUnlocking = (C, now) => C.slots.some((ch) => ch && ch.start != null && !isReady(ch, now));
const fmtLeft = (ms) => { const s = Math.max(0, Math.floor(ms / 1000)), h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60; return h > 0 ? `${h}:${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")}` : `${m}:${String(sec).padStart(2, "0")}`; };
/** "2 saat", "1 sa 48 dk" or "45 dk": the wait as it really is, runes included (ChestTier.durationText). */
const durText = (c, tier) => { const min = Math.floor(effMs(c, tier) / 60000), h = Math.floor(min / 60), m = min % 60; return m === 0 ? S(h === 1 ? "chest_duration_hours_one" : "chest_duration_hours", h) : h === 0 ? S("chest_duration_minutes", m) : S("chest_duration_hm", h, m); };

/** The loot of a chest, with the chest runes applied (ChestLoot.roll). The percent is taken with integer division, like the app. */
function roll(P, tier) {
  const g0 = rnd(...GOLD[tier]), g = g0 + Math.floor(g0 * P.__fx("CHEST") / 100), jb = P.__fx("CJOKER") / 100;
  const n = tier === "SILVER" ? (Math.random() < 0.3 + jb ? 1 : 0) : tier === "GOLD" ? 1 + (Math.random() < 0.3 + jb ? 1 : 0) : 2 + (Math.random() < 0.4 + jb ? 1 : 0);
  const jokers = {}; for (let i = 0; i < n; i++) { const t = JOKERS[Math.floor(Math.random() * 3)]; jokers[t] = (jokers[t] || 0) + 1; }
  let pen = null;
  if (tier === "RARE" && Math.random() < 0.12) { const pool = penList().filter((p) => p.price > 0 && p.price <= 6500 && !p.rune && !p.retired && !P.owned.includes("pen:" + p.id)); if (pool.length) pen = pool[Math.floor(Math.random() * pool.length)]; }
  return { tier, gold: g, jokers, pen };
}
function pay(P, rw) {
  P.gold += rw.gold; for (const [t, n] of Object.entries(rw.jokers)) P.jokers[t] = (P.jokers[t] || 0) + n; if (rw.pen) P.owned.push("pen:" + rw.pen.id);
}

// ---------- the home section: the four slots ----------
export function layoutChests(c) {
  const { place, $ } = c;
  place($("ch-title"), 166, 1416, 400, 70); place($("ch-tag"), 380, 1426, 344, 64); place($("ch-info"), 731, 1426, 63, 64);
  [[64, 232], [247, 412], [429, 596], [610, 780]].forEach(([a, b], i) => place($("ch-s" + i), a, 1494, b - a, 180));
}
let timer = null; const slotFx = [];
const hm = (ms) => { const m = Math.floor(ms / 60000); return String(Math.floor(m / 60)).padStart(2, "0") + ":" + String(m % 60).padStart(2, "0"); };
export function renderChests(c) {
  const { $, P } = c, C = ensure(P), now = Date.now(), lang0 = c.lang() + (strReady() ? "" : "!");
  $("ch-title").textContent = S("menu_chests");
  const full = C.slots.every(Boolean);
  $("ch-tag").textContent = C.reserve ? S("home_chests_reserve") : full ? S("home_chests_full") : S("home_chests_tagline");
  C.slots.forEach((ch, i) => {
    const s = $("ch-s" + i);
    // a slot is only rebuilt (picture, animated backdrop) when what it shows changes; the ticking clock just rewrites the plate text
    const ready0 = ch && isReady(ch, now), going0 = ch && ch.start != null && !ready0, sig = ch ? `${ch.id}|${ch.tier}|${ready0 ? "r" : going0 ? "g" : "i"}|${lang0}` : "empty|" + lang0;
    if (s.dataset.sig === sig) { const sp = s.querySelector(".sl-plate span"); if (sp) sp.textContent = ready0 ? S("chests_open_button") + "!" : going0 ? fmtLeft(remaining(c, ch, now)) : hm(effMs(c, ch.tier)); return; }
    s.dataset.sig = sig; s.replaceChildren(); s.className = "ch-slot";
    if (!ch) { s.append(el("span", "ch-e", S("chest_slot_empty_title"))); s.onclick = null; if (slotFx[i] && slotFx[i].stop) { slotFx[i].stop(); slotFx[i].stop = null; } return; }
    const ready = ready0, going = going0, T = TIER[ch.tier];
    s.classList.add("filled"); s.classList.toggle("ready", ready); s.style.setProperty("--bc", T.border);
    const cv = el("canvas", "sl-cv"), img = el("img", "sl-img"); img.src = `img/${T.art}.webp`; img.alt = "";
    const plate = el("div", "sl-plate " + (ready ? "ready" : going ? "going" : "idle"), `<span>${ready ? S("chests_open_button") + "!" : going ? fmtLeft(remaining(c, ch, now)) : hm(effMs(c, ch.tier))}</span>`);
    plate.style.setProperty("--ac", T.accent);
    s.append(cv, img, el("span", "sl-n fitw", S(T.label)), plate);
    // the tier's backdrop moves behind every filled slot, like the app's slot cards
    (slotFx[i] = slotFx[i] || {}).stop && slotFx[i].stop(); requestAnimationFrame(() => { slotFx[i].stop = backdrop(cv, ch.tier, 24); });
    s.onclick = () => detail(c, ch.id);
  });
  requestAnimationFrame(() => c.fit && c.fit());
  $("ch-info").onclick = () => infoDialog(c);
  clearInterval(timer);
  if (C.slots.some((ch) => ch && ch.start != null && !isReady(ch))) timer = setInterval(() => { if (c.current() === "home") renderChests(c); else clearInterval(timer); }, 1000);
}

// ---------- the chest window (ChestDetailHost) ----------
/** Shrinks the prize lines of the parchment, one by one, until each fits the width it has (FitText in the app). */
function fitRows(box, card) { const k = card.card.getBoundingClientRect().width / 793; box.querySelectorAll(".cd2-r span").forEach((sp) => { let q = 1; sp.style.fontSize = 30 * k + "px"; while (q > 0.55 && sp.scrollWidth > sp.clientWidth + 1) { q -= 0.04; sp.style.fontSize = 30 * k * q + "px"; } }); }
// One painted card (chest_card, 793x1390): the title sign, the lit stage, the parchment, the timer plate and the orange button
// are in the picture; the words, the chest and the clock are laid over it at the same spots as ChestDetail.kt.
const CD = { title: [150, 108, 620, 228], stage: [92, 258, 702, 704], contents: [96, 772, 698, 940], timer: [108, 1012, 688, 1174], button: [104, 1224, 690, 1346], close: [662, 28, 786, 142] };
function detail(c, id) {
  const { $, P } = c, C = ensure(P), ch = C.slots.find((s) => s && s.id === id); if (!ch) return;
  const tier = ch.tier, T = TIER[tier], host = $("stage");
  const below = el("div", "pc-b2 hidden"); // the second button (the ad speed-up) under the card, as in the app
  below.style.cssText = "width:88%;aspect-ratio:689/173"; below.style.display = "none";
  const card = paintedCard(host, { art: "chest_card", w: 793, h: 1390, cls: "cd2", below, onClose: () => { clearInterval(tick); clearTimeout(bubbleT); } });
  const { put, fit } = card; let bubbleT = null;
  const title = put("pc-t", CD.title, `<span>${S(T.label)}</span>`);
  // the stage: the chest (tappable, it answers in a speech bubble)
  const stage = put("cd2-stage", CD.stage, ""), img = el("img", "cd2-chest"), hov = el("div", "cd2-hover"); img.src = `img/${T.art}.webp`; img.alt = "";
  hov.style.setProperty("--fp", { SILVER: 0.148, GOLD: 0.019, RARE: 0.0375 }[tier]); hov.style.setProperty("--cw", { SILVER: 500, GOLD: 515, RARE: 551 }[tier]); /* same visible width for every tier */
  hov.append(img); stage.append(el("i", "cd2-shadow"), hov);
  const bubble = el("div", "cd-bubble hidden"); stage.append(bubble);
  const say = (key) => { bubble.innerHTML = `<span>${S(key)}</span><i></i>`; bubble.classList.remove("hidden"); img.classList.remove("wig"); void img.offsetWidth; img.classList.add("wig"); clearTimeout(bubbleT); bubbleT = setTimeout(() => bubble.classList.add("hidden"), 1900 + 500); };
  // what a chest of this tier can hold
  const stars = "★".repeat(T.stars) + "☆".repeat(3 - T.stars);
  const jokersKey = { SILVER: ["chest_detail_jokers_silver", 30], GOLD: ["chest_detail_jokers_gold", 30], RARE: ["chest_detail_jokers_rare", 40] }[tier];
  const contents = put("cd2-contents" + (tier === "RARE" ? " tight" : ""), CD.contents, `<div class="cd2-h"><b>${S("chest_detail_contents_title")}</b><span style="color:${T.accent};background:${rgba(T.accent, 0.16)}">${stars} ${S(T.rarity)}</span></div>
    <div class="cd2-r"><i>🪙</i><span>${S("chest_detail_gold", GOLD[tier][0], GOLD[tier][1])}</span></div><div class="cd2-r"><i>🎲</i><span>${S(jokersKey[0], jokersKey[1])}</span></div>${tier === "RARE" ? `<div class="cd2-r"><i>✏️</i><span>${S("chest_detail_pen", 12)}</span></div>` : ""}`);
  const timer = put("cd2-timer", CD.timer, "");
  const btn = put("pc-btn cd2-btn", CD.button, "");
  const x = put("pc-x", CD.close, ""); x.setAttribute("aria-label", S("close")); x.onclick = () => card.close();
  stage.onclick = () => { const cur = C.slots.find((s) => s && s.id === id); if (!cur || isReady(cur)) return; say(cur.start != null ? "chest_slot_still_unlocking" : anyUnlocking(C, Date.now()) ? "chest_slot_busy" : "chest_detail_tap_to_start"); };
  const instantOk = () => c.runeHas("INSTANT") && P.instantDay !== todayEpoch();
  const refresh = () => { renderChests(c); paint(); };
  const label = (html, base = 58, lines = 1) => { btn.innerHTML = `<span class="pc-lt">${html}</span>`; btn.classList.remove("dis"); fit(btn, base, lines, 0.5); };
  const paint = () => {
    const now = Date.now(), cur = C.slots.find((s) => s && s.id === id); if (!cur) { card.close(); return; }
    const ready = isReady(cur, now), unlocking = cur.start != null && !ready, another = anyUnlocking(C, now) && cur.start == null;
    // the clock plate: label, big digits and a bar (or the "starts when you tap" hint); a ready chest just says so
    if (ready) timer.innerHTML = `<span class="pc-lt cd2-ready" style="--ol:#2a1608;color:#ffe08a">${S("chest_detail_ready")}</span>`;
    else timer.innerHTML = `<div class="cd2-tl"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="#FFC94D" stroke-width="2"/><path d="M12 7v5l3 2" stroke="#FFC94D" stroke-width="2" fill="none" stroke-linecap="round"/></svg><span>${S(unlocking ? "chest_timer_remaining" : "chest_timer_total").toLocaleUpperCase(c.lang())}</span></div>
      <div class="cd2-td">${fmtLeft(remaining(c, cur, now))}</div>${unlocking ? `<div class="cd2-bar"><i style="width:${Math.max(0, Math.min(1, 1 - remaining(c, cur, now) / effMs(c, cur.tier))) * 100}%"></i></div>` : `<div class="cd2-th">${S("chest_timer_hint")}</div>`}`;
    // the buttons only change when the state does
    const state = ready ? "ready" : unlocking ? "go" + instantOk() : another ? "busy" : "start" + c.lang();
    if (btn.dataset.s === state) return; btn.dataset.s = state; below.style.display = "none"; below.replaceChildren();
    if (ready) { label(S("chests_open_button")); btn.onclick = () => openChest(c, cur, card); }
    else if (unlocking) {
      const adBtn = (host, main) => { host.innerHTML = `<span class="pc-lt cd2-ad"><svg viewBox="0 0 24 24" width="1em" height="1em"><circle cx="12" cy="12" r="11" fill="#fff"/><path d="M9.5 7.5v9l7-4.5z" fill="#f26a1b"/></svg> ${S("chest_speedup_button")} <em>${S("chest_speedup_badge")}</em></span>`; host.onclick = () => appOnlyDialog(host.closest(".pc-ov") ? $("stage") : $("stage")); };
      if (instantOk()) { label(S("chest_instant_button")); btn.onclick = () => { cur.start = Date.now() - rawMs(cur.tier); P.instantDay = todayEpoch(); c.saveP(); btn.dataset.s = ""; refresh(); }; below.style.display = "flex"; adBtn(below); fit(below, 48, 1, 0.5); }
      else { adBtn(btn, true); fit(btn, 58, 1, 0.5); }
    } else if (another) { label(S("chest_slot_busy"), 44, 2); btn.firstElementChild.classList.add("wrap"); fit(btn, 44, 2, 0.5); btn.classList.add("dis"); btn.onclick = null; }
    else { label(S("chests_start_button") + " · " + durText(c, cur.tier)); btn.onclick = () => { cur.start = Date.now() - Math.floor(rawMs(cur.tier) * c.runeFx("CTIME") / 100); c.saveP(); btn.dataset.s = ""; refresh(); }; }
    // the title, the contents and the labels all shrink to their boxes
    fit(title, 68, 1, 0.5);
  };
  const tick = setInterval(paint, 1000);
  requestAnimationFrame(() => { fit(title, 68, 1, 0.5); paint(); fitRows(contents, card); });
  card.size();
}

/** Opens a ready chest: the reward is paid at once (and the reserve chest slides into the freed slot), then the opening scene plays. */
function openChest(c, ch, win) {
  const { P } = c, C = ensure(P); P.__fx = c.runeFx;
  const rw = roll(P, ch.tier); pay(P, rw);
  const i = C.slots.indexOf(ch); C.slots[i] = C.reserve || null; C.reserve = null;
  c.saveP(); c.refreshHome(); win && win.close(); renderChests(c);
  openingScene(c, rw);
}
/** A chest opened on the spot (login-streak days, coupons): the same loot and the same opening scene. */
export function openNow(c, tier) {
  const P = c.P; P.__fx = c.runeFx; const rw = roll(P, tier); pay(P, rw); c.saveP(); c.refreshHome(); openingScene(c, rw);
}
export const reward = (c, tier, rw) => openingScene(c, { tier, ...rw });

// ---------- the opening scene: the tier's video, then the reward (ChestOpeningScene) ----------
const TAIL_TRIM = 0.15; // the clips now end as the screen turns white
function openingScene(c, rw) {
  const { $ } = c, T = TIER[rw.tier], host = $("stage"), ov = el("div", "os2"); host.append(ov);
  let stage = 0, stopFx = null, done = false; const timers = [];
  const vid = el("video", "os2-v"); vid.src = `video/${T.video}.mp4`; vid.playsInline = true; vid.setAttribute("playsinline", ""); vid.autoplay = true; vid.muted = !(c.P.prefs && c.P.prefs.s0);
  ov.append(vid);
  const toReward = () => { if (stage !== 0) return; stage = 1; vid.pause(); vid.remove(); ov.classList.add("white"); rewardUi(); };
  vid.addEventListener("ended", toReward); vid.addEventListener("error", toReward);
  vid.addEventListener("timeupdate", () => { if (vid.duration && vid.duration > TAIL_TRIM * 2 && vid.currentTime >= vid.duration - TAIL_TRIM) toReward(); });
  ov.onclick = () => toReward();
  vid.play().catch(() => { vid.muted = true; vid.play().catch(toReward); });
  const hasExtras = Object.keys(rw.jokers).length > 0 || !!rw.pen;
  function rewardUi() {
    const cv = el("canvas", "os2-cv"); ov.append(cv); stopFx = rewardScene(cv, rw.tier, rw.gold);
    const col = el("div", "os2-col");
    col.innerHTML = `<div class="os2-title">${S("chests_reward_title")}</div><div class="os2-tier" style="color:${T.accent};background:${rgba(T.accent, 0.14)}">${S(T.label)}</div>
      <div class="os2-card" style="border-color:${rgba(T.accent, 0.55)}"><img class="os2-coin${hasExtras ? " sm" : ""}" src="img/icon_gold_coin.webp" alt=""><div class="os2-gold${hasExtras ? " sm" : ""}">${S("chests_reward_gold", 0)}</div><div class="os2-extras"></div></div>`;
    ov.append(col); const goldEl = col.querySelector(".os2-gold"), extras = col.querySelector(".os2-extras");
    c.sfx && c.sfx.over && c.sfx.over();
    const t0 = performance.now(), count = (now) => { const p = Math.min(1, Math.max(0, (now - t0 - 250) / 900)), e = 1 - Math.pow(1 - p, 3); goldEl.textContent = S("chests_reward_gold", Math.round(rw.gold * e)); if (p < 1) timers.push(requestAnimationFrame(count)); else settled(); };
    timers.push(requestAnimationFrame(count));
    function settled() {
      if (hasExtras) {
        for (const [t, n] of Object.entries(rw.jokers)) { const d = el("div", "os2-j"); d.style.background = JOKER_TINT[t]; d.innerHTML = `<span class="ja"><img src="img/${JOKER_ART[t]}.webp" alt=""></span><b>×${n}</b><small>${S(JOKER_SHORT[t])}</small>`; extras.append(d); }
        if (rw.pen) { const colors = rw.pen.colors.length > 1 ? rw.pen.colors : [rw.pen.colors[0], rw.pen.colors[0]]; extras.append(el("div", "os2-pen", `<i style="background:linear-gradient(90deg,${colors.join(",")})"></i><span>${S("joker_pen_won")} ${rw.pen[c.lang()]}</span>`)); }
      }
      const b = primary(S("chests_reward_button"), () => { timers.forEach(cancelAnimationFrame); stopFx && stopFx(); ov.remove(); }); b.classList.add("os2-btn"); col.append(b);
    }
  }
}

// ---------- the win scene (ChestWonScene) ----------
export function wonDialog(c, chest) {
  const { $ } = c, host = $("stage");
  if (!chest) { // every slot was taken: the chest is lost — the app simply drops it, with a note
    const body = el("div", "cd-lost", S("chest_lost_body")); const ov = appWindow(host, { title: S("chest_lost_title"), body, footer: primary(S("chest_won_button"), () => ov.close()) }); return;
  }
  const T = TIER[chest.tier], ov = el("div", "won"); host.append(ov);
  const cv = el("canvas", "won-cv"); ov.append(cv);
  const img = new Image(); img.src = `img/${T.art}.webp`;
  const col = el("div", "won-col"), stars = "★".repeat(T.stars) + "☆".repeat(3 - T.stars);
  col.innerHTML = `<div class="won-title">${S("chest_win_title")}</div><div class="won-tier" style="background:linear-gradient(90deg,${rgba(T.accent, 1)},${T.accent})"><span class="st">${stars}</span><b>${S(T.label)}</b><small>· ${S(T.rarity)}</small></div>
    <div class="won-info"><div class="won-t"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" fill="none" stroke="#FFE066" stroke-width="2"/><path d="M12 7v5l3 2" stroke="#FFE066" stroke-width="2" fill="none" stroke-linecap="round"/></svg>${S("chest_win_unlock_time_text", durText(c, chest.tier))}</div><small>${S("chest_win_hint")}</small></div>`;
  const btn = primary(S("chest_won_button"), () => { stopFx(); ov.remove(); }); btn.classList.add("won-btn"); col.append(btn); ov.append(col);
  const stopFx = wonScene(cv, img, chest.tier, () => { col.classList.add("in"); c.sfx && c.sfx.over && c.sfx.over(); });
}

// ---------- the info window (ChestInfoDialog) ----------
function infoDialog(c) {
  const body = el("div", "ci-body");
  const step = (n, emoji, title, text, first, last) => `<div class="ci-step ${first ? "first" : ""} ${last ? "last" : ""}"><div class="ci-rail"><i>${n}</i></div><div class="ci-tx"><b>${emoji}  ${title}</b><span>${text}</span></div></div>`;
  body.innerHTML = step(1, "🏆", S("chest_info_step_win_title"), S("chest_info_step_win_body"), true, false) + step(2, "⏳", S("chest_info_step_open_title"), S("chest_info_step_open_body"), false, false) + step(3, "🔓", S("chest_info_step_slots_title"), S("chest_info_step_slots_body", SLOTS), false, true)
    + `<div class="ci-h">${S("chest_info_contents_title")}</div>`
    + ["SILVER", "GOLD", "RARE"].map((t) => { const T = TIER[t], j = { SILVER: ["chest_detail_jokers_silver", 30], GOLD: ["chest_detail_jokers_gold", 30], RARE: ["chest_detail_jokers_rare", 40] }[t];
      return `<div class="ci-card" style="background:${rgba(T.accent, 0.1)};border-color:${rgba(T.accent, 0.5)}"><img src="img/${T.art}.webp" alt=""><div><div class="ci-n"><b>${S(T.label)}</b><span style="background:${T.accent}">⏱ ${durText(c, t)}</span></div><div class="ci-l">🪙 ${S("chest_detail_gold", GOLD[t][0], GOLD[t][1])}<br>🎲 ${S(j[0], j[1])}${t === "RARE" ? `<br>✏️ ${S("chest_detail_pen", 12)}` : ""}</div></div></div>`; }).join("");
  const ov = appWindow(c.$("stage"), { title: S("home_chests_info_title"), body, footer: primary(S("home_chests_info_ok"), () => ov.close()) });
}
