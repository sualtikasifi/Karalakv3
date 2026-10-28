// Chests (kasalar): won by beating a Quick Match opponent or a room, unlocked on a timer, opened for gold, jokers and sometimes a pen.
// A port of ChestStore.kt (the rules), ChestDetail.kt (the window), ChestWonDialog.kt (the win scene), ChestComponents.kt (the opening
// scene) and ChestInfoDialog.kt. The texts are the app's own (i18n.js / strings.json).
import { penList } from "./store.js";
import { S, fmtNum, ready as strReady } from "./i18n.js";
import { el, primary, appWindow, paintedCard, appOnlyDialog } from "./ui.js";
import { TIER, backdrop, wonScene, rewardScene, rgba, glowOf, lerpC } from "./chestfx.js";
import { todayEpoch } from "./daily.js";

const SLOTS = 4, JOKERS = ["FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"], GOLD = { SILVER: [150, 250], GOLD: [450, 700], RARE: [1500, 2500] };
const JOKER_ORDER = ["FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"];
const JOKER_ART = { FIRST_LETTER: "joker_first_letter", LETTER_COUNT: "joker_letter_count", EXTRA_TIME: "joker_extra_time" }, JOKER_SHORT = { FIRST_LETTER: "joker_first_letter_short", LETTER_COUNT: "joker_letter_count_short", EXTRA_TIME: "joker_extra_time_short" };
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
  ov.onclick = () => (stage === 0 ? toReward() : showAll());
  vid.play().catch(() => { vid.muted = true; vid.play().catch(toReward); });
  let showAll = () => {};
  // The reward board (ChestComponents.kt's ChestOpeningScene): laid out in the app's 1080 × 2000 design units, scaled to fit.
  function rewardUi() {
    const A = T.accent, light = lerpC(A, "#ffffff", 0.25), dark = lerpC(A, "#000000", 0.22);
    const extras = [...Object.entries(rw.jokers).filter(([, n]) => n > 0).sort(([a], [b]) => JOKER_ORDER.indexOf(a) - JOKER_ORDER.indexOf(b)).map(([t, n]) => ({ t, n })), ...(rw.pen ? [{ pen: rw.pen }] : [])];
    const compact = !extras.length, heroY = compact ? 1000 : 915, amountY = compact ? 1235 : 1110;
    const at = (x, y, w, h) => `left:${x / 10.8}%;top:${y / 20}%;width:${w / 10.8}%;height:${h / 20}%`;
    const k = (n) => `calc(var(--k)*${n})`;
    const board = el("div", "rb");
    const cv = el("canvas", "os2-cv"); ov.append(cv, board);
    const fitBoard = () => { const R = ov.getBoundingClientRect(), wpx = Math.min(R.width, R.height * 1080 / 2000); board.style.width = wpx + "px"; board.style.height = wpx * 2000 / 1080 + "px"; board.style.setProperty("--k", wpx / 1080 + "px"); board.querySelectorAll(".lt").forEach(fitLt); };
    fitBoard(); addEventListener("resize", fitBoard);
    stopFx = rewardScene(cv, rw.tier, rw.gold, (d) => { const R = ov.getBoundingClientRect(), B = board.getBoundingClientRect(), kk = B.width / 1080; return { x: (B.left - R.left + 540 * kk) * d, y: (B.top - R.top + heroY * kk) * d, k: kk * d }; });
    const title = S("chests_reward_title").toLocaleUpperCase(c.lang());
    const delays = { sign: 0, frame: 140, rib: 340, hero: 460, count: 640 }, countMs = 900, cardAt = (i) => delays.count + countMs + i * 140, btnAt = cardAt(extras.length) + 100;
    let html = `<img class="rb-frame" style="${at(60, 510, 960, 1180)}" src="img/tut_frame_mid.webp" alt="">
      <div class="rb-sign" style="${at(90, 20, 900, 563)}"><img src="img/rs_head_plain.webp" alt=""><div class="rb-t">${lt(title, 92, "#FFFAD7", "#FFC032", "#552305")}</div></div>
      <div class="rb-rib" style="${at(191, 666, 698, 124)}">${ribbonSvg(light, dark)}<div class="rb-rt">${[0, 1, 2].map((i) => `<img src="img/mf_star.webp" alt=""${i < T.stars ? "" : ' class="off"'}>`).join("")}${lt(S(T.label), 46, "#ffffff", "#FFF0DC", lerpC(dark, "#000000", 0.5))}</div></div>
      <div class="rb-hero" style="${at(310, heroY - 175, 460, 350)}"><img src="img/rune_n_chest_gold.webp" alt="">${[[0.13, 0.18, 24], [0.88, 0.27, 20], [0.80, 0.82, 14], [0.17, 0.76, 16], [0.54, 0.04, 18]].map(([x, y, r], i) => `<i class="rb-spk" style="left:${x * 100}%;top:${y * 100}%;width:${k(r * 2)};height:${k(r * 2)};animation-delay:${i * 0.34}s"></i>`).join("")}</div>
      <div class="rb-amt" style="${at(0, amountY - 70, 1080, 140)}"><img src="img/icon_gold_coin.webp" alt="">${lt("+0", 120, "#FFF6AA", "#FFA514", "#5A2805", "rb-gold")}</div>
      <div class="rb-lbl" style="${at(0, amountY + 52, 1080, 56)}">${S("chests_reward_gold_label").toLocaleUpperCase(c.lang())}</div>`;
    if (!compact) {
      const tile = extras.length > 3 ? 168 : 190, gap = 28, x0 = (1080 - extras.length * tile - (extras.length - 1) * gap) / 2;
      html += `<div class="rb-div" style="${at(220, 1232, 640, 20)}"><i class="rb-spk on"></i></div>`;
      extras.forEach((e, i) => {
        const art = e.pen ? penSvg(e.pen.colors) : `<img src="img/${JOKER_ART[e.t]}.webp" alt="">`, name = e.pen ? e.pen[c.lang()] : S(JOKER_SHORT[e.t]), tw = tile * 0.56, th = tile * 0.30;
        html += `<div class="rb-card" style="${at(x0 + i * (tile + gap), 1272, tile, tile + 72)};animation-delay:${cardAt(i)}ms"><div class="tl" style="width:${k(tile)};height:${k(tile)}">${art}</div>
          <div class="tag" style="left:${k(tile - tw + 16)};top:${k(tile - th + 14)};width:${k(tw)};height:${k(th)}">${lt("×" + (e.n || 1), th * 0.72, "#ffffff", "#FFEBC8", "#6E2800")}</div>
          <div class="nm" style="top:${k(tile + 16)};font-size:${k(tile * 0.18)}">${esc(name)}</div></div>`;
      });
    }
    html += `<div class="rb-btn" style="${at(210, 1712, 660, 275)};animation-delay:${btnAt}ms"><img src="img/rp_pill_orange.webp" alt="">${lt(S("chests_reward_button"), 78, "#ffffff", "#FFEBC8", "#823200")}</div>`;
    board.innerHTML = html;
    board.querySelector(".rb-sign").style.animationDelay = delays.sign + "ms";
    board.querySelector(".rb-frame").style.animationDelay = delays.frame + "ms";
    board.querySelector(".rb-rib").style.animationDelay = delays.rib + "ms";
    board.querySelector(".rb-hero").style.animationDelay = board.querySelector(".rb-amt").style.animationDelay = board.querySelector(".rb-lbl").style.animationDelay = delays.hero + "ms";
    const fitAllLt = () => board.querySelectorAll(".lt").forEach(fitLt);
    requestAnimationFrame(fitAllLt); document.fonts && document.fonts.ready.then(fitAllLt);
    c.sfx && c.sfx.over && c.sfx.over();
    const goldEl = board.querySelector(".rb-gold"), setGold = (n) => { goldEl.querySelector("text").textContent = "+" + fmtNum(n); sizeLt(goldEl); };
    let skip = false;
    const t0 = performance.now(), count = (now) => { const p = skip ? 1 : Math.min(1, Math.max(0, (now - t0 - delays.count) / countMs)), e = 1 - Math.pow(1 - p, 3); setGold(Math.round(rw.gold * e)); if (p < 1) timers.push(requestAnimationFrame(count)); };
    timers.push(requestAnimationFrame(count));
    const close = () => { if (done) return; done = true; timers.forEach(cancelAnimationFrame); removeEventListener("resize", fitBoard); stopFx && stopFx(); ov.remove(); };
    const btn = board.querySelector(".rb-btn");
    btn.onclick = (ev) => { ev.stopPropagation(); if (stage === 2) close(); };
    timers.push(setTimeout(() => { stage = 2; }, btnAt + 150));
    // a tap while the board is still coming in shows it all at once (the reward is already paid either way)
    showAll = () => { if (stage !== 1) return; skip = true; board.classList.add("now"); stage = 2; };
  }
}

/** The tier's ribbon, drawn as ChestComponents.kt's TierRibbon: a band with folded tails behind it and stitched edges. */
function ribbonSvg(light, dark) {
  const back = lerpC(dark, "#000000", 0.25), edge = lerpC(dark, "#000000", 0.55), fold = lerpC(dark, "#000000", 0.5), stitch = lerpC(light, "#ffffff", 0.5), id = "rbg" + Math.random().toString(36).slice(2, 8);
  const L = 79, R = 619, T0 = 8, B0 = 96, mid = (T0 + B0) / 2;
  const tail = (sd) => { const inner = sd < 0 ? L + 28 : R - 28, outer = sd < 0 ? 0 : 698, notch = sd < 0 ? 26 : 672, x = sd < 0 ? L : R;
    return `<polygon points="${inner},${T0 + 18} ${outer},${T0 + 18} ${notch},${mid + 18} ${outer},${B0 + 18} ${inner},${B0 + 18}" fill="${back}" stroke="${edge}" stroke-width="5" stroke-linejoin="round"/><polygon points="${inner},${B0} ${x},${B0} ${inner},${B0 + 18}" fill="${fold}"/>`; };
  return `<svg viewBox="0 0 698 124" preserveAspectRatio="none"><defs><linearGradient id="${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${light}"/><stop offset="1" stop-color="${dark}"/></linearGradient></defs>${tail(-1)}${tail(1)}
    <rect x="${L}" y="${T0}" width="${R - L}" height="${B0 - T0}" rx="18" fill="url(#${id})" stroke="${edge}" stroke-width="6"/>
    <line x1="${L + 22}" y1="${T0 + 14}" x2="${R - 22}" y2="${T0 + 14}" stroke="rgba(255,255,255,.45)" stroke-width="6" stroke-linecap="round"/>
    <line x1="${L + 26}" y1="${T0 + 26}" x2="${R - 26}" y2="${T0 + 26}" stroke="${stitch}" stroke-width="3" stroke-dasharray="12 14"/>
    <line x1="${L + 26}" y1="${B0 - 16}" x2="${R - 26}" y2="${B0 - 16}" stroke="${stitch}" stroke-width="3" stroke-dasharray="12 14"/></svg>`;
}
/** A won pen as PenWave.kt's PenSwoosh: one brush sweep in the pen's colours. */
function penSvg(colors) {
  const cs = colors.length > 1 ? colors : [colors[0], colors[0]], id = "pg" + Math.random().toString(36).slice(2, 8);
  return `<svg class="pen" viewBox="0 0 100 60"><defs><linearGradient id="${id}">${cs.map((col, i) => `<stop offset="${i / (cs.length - 1)}" stop-color="${col}"/>`).join("")}</linearGradient></defs>
    <path d="M7 47 C30 6 48 57 70 29 C80 16 88 8 95 13" fill="none" stroke="rgba(0,0,0,.16)" stroke-width="13" stroke-linecap="round" transform="translate(0 4)"/>
    <path d="M7 47 C30 6 48 57 70 29 C80 16 88 8 95 13" fill="none" stroke="url(#${id})" stroke-width="13" stroke-linecap="round"/>
    <path d="M9 43 C30 4 48 53 70 25" fill="none" stroke="rgba(255,255,255,.4)" stroke-width="3.5" stroke-linecap="round"/></svg>`;
}
const esc = (t) => String(t).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[ch]));
/** Game lettering (RewardLettering): an outline with a soft drop under it and a top-to-bottom gradient fill. SVG text, so a
 *  glyph's overlapping outlines merge cleanly (CSS text-stroke and background-clip:text show them as lines). */
const lt = (text, size, top, bot, outline, cls = "") => {
  const id = "lg" + Math.random().toString(36).slice(2, 8);
  return `<svg class="lt ${cls}" style="--fs:${size}"><defs><linearGradient id="${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0.2" stop-color="${top}"/><stop offset="0.85" stop-color="${bot}"/></linearGradient></defs><text x="50%" y="50%" text-anchor="middle" dominant-baseline="central" fill="url(#${id})" stroke="${outline}">${esc(text)}</text></svg>`;
};
/** Sizes a lettering to its text (outline included). */
function sizeLt(svg) {
  const t = svg.querySelector("text"), sw = parseFloat(getComputedStyle(t).strokeWidth) || 0;
  svg.style.width = Math.ceil(t.getComputedTextLength() + sw * 1.2) + "px";
}
/** Shrinks a lettering until it fits the box it sits in (never wraps), as FitText does in the app. */
function fitLt(svg) {
  const box = svg.parentElement; if (!box || !svg.querySelector("text")) return;
  let fs = parseFloat(svg.dataset.fs0 || svg.style.getPropertyValue("--fs")); svg.dataset.fs0 = fs; const min = fs * 0.5;
  const room = () => box.clientWidth * 0.98 - [...box.children].filter((x) => x !== svg && getComputedStyle(x).position !== "absolute").reduce((w, x) => w + x.getBoundingClientRect().width, 0);
  svg.style.setProperty("--fs", fs); sizeLt(svg);
  while (svg.getBoundingClientRect().width > room() && fs > min) { fs *= 0.94; svg.style.setProperty("--fs", fs); sizeLt(svg); }
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
