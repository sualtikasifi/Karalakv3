// Karalak Runes (port of RunesScreen.kt, RuneStatusDialog.kt and the rune rules of Rune.kt). The data — the map, the texts, the
// ring gates, the ceilings and the icon centring — is generated from the Android sources by scripts/gen_web_runes.js, so the web
// map cannot drift from the app. One art unit is 1/841 of the stage width; one Android dp is DP art units.
import { todayEpoch, turkeyMonth } from "./daily.js";
import { paintedFrameDialog } from "./ui.js";
let D = null, sel = null, statusOpen = false, introOpen = false, view = { s: 0.78, x: 0, y: 0, key: "" };
const DP = 2.05, SP = 116 * DP;
const GATES = { 2: 15, 3: 30, 4: 40, need: 70, price: 1 };
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const fmtNum = (n) => Number(n).toLocaleString("en-US").replace(/,/g, ".");
const fmt = (s, ...a) => String(s).replace(/%(\d)\$[ds]/g, (_, i) => a[+i - 1]).replace(/%%/g, "%");

export async function loadRunes() {
  if (!D) { D = await (await fetch("runes.json")).json(); GATES[2] = D.gates.ring2; GATES[3] = D.gates.ring3; GATES[4] = D.gates.ring4; GATES.need = D.gates.need; }
}
/** config/runes (RuneConfigRepository): when each ring opens and what the later rings cost; every field range-checked. */
export function applyRuneConfig(d) {
  const int = (k, lo, hi, def) => (d && typeof d[k] === "number" && Math.trunc(d[k]) >= lo && Math.trunc(d[k]) <= hi ? Math.trunc(d[k]) : def);
  GATES[2] = int("ring2Level", 1, 100, D ? D.gates.ring2 : 15); GATES[3] = int("ring3Level", 1, 100, D ? D.gates.ring3 : 30); GATES[4] = int("ring4Level", 1, 100, D ? D.gates.ring4 : 40);
  GATES.need = int("percentNeeded", 0, 100, D ? D.gates.need : 70);
  GATES.price = d && typeof d.priceFactor === "number" && d.priceFactor >= 0.25 && d.priceFactor <= 4 ? d.priceFactor : 1;
}
export const loaded = () => !!D;

// ---------- the rules (RuneTree, RuneGates, RuneEffects) ----------
const byId = (id) => D.runes.find((r) => r.id === id);
const keystone = (r) => (r.keystone === null || r.keystone === undefined ? r.costs.length === 1 : r.keystone);
export const lv = (P, id) => (id === "root" ? 1 : Math.min(Math.max((P.runes || {})[id] || 0, 0), (byId(id) || { costs: [] }).costs.length));
const unlocked = (P, r) => r.req.every((q) => lv(P, q) >= 1);
function ringProgress(P, ring) {
  const m = D.runes.filter((r) => r.ring === ring && r.id !== "root"); if (!m.length) return 1;
  return m.reduce((s, r) => s + lv(P, r.id), 0) / m.reduce((s, r) => s + r.costs.length, 0);
}
const gateLevel = (ring) => GATES[Math.min(ring, 4)];
const ringOpen = (P, ring, pl) => ring <= 1 || (pl >= gateLevel(ring) && ringProgress(P, ring - 1) >= GATES.need / 100);
const canBuy = (P, r, pl) => unlocked(P, r) && ringOpen(P, r.ring, pl);
const price = (r, level) => { const base = r.costs[level]; if (base === undefined) return null; return r.ring >= 2 && GATES.price !== 1 ? Math.max(1, Math.floor(base * GATES.price)) : base; };
const nextCost = (P, r) => (r.id === "root" ? null : price(r, lv(P, r.id)));
/** The sum of level x perLevel over every rune of [fx], held to its ceiling (RuneEffects.capped). */
export function effect(P, fx) {
  if (!D) return 0;
  const t = D.runes.filter((r) => r.effect === fx).reduce((s, r) => s + lv(P, r.id) * r.per, 0);
  return Math.min(t, D.caps[fx] ?? 1e9);
}
/** A one-off ability: its rune is bought (RuneEffects.has). */
export const has = (P, fx) => !!D && D.runes.some((r) => r.effect === fx && lv(P, r.id) >= 1);
export const anyAffordable = (P, gold, pl) => !!D && D.runes.some((r) => canBuy(P, r, pl) && (nextCost(P, r) ?? Infinity) <= gold);

/** The runes the player gets to see: owned or buyable, the foggy next step after the latest rune, seals for closed rings (visibleRunes). */
function visibleRunes(P, pl) {
  const open = new Set(D.runes.filter((r) => lv(P, r.id) >= 1 || canBuy(P, r, pl)).map((r) => r.id));
  const sealed = new Set(D.runes.filter((r) => !open.has(r.id) && unlocked(P, r) && !ringOpen(P, r.ring, pl)).map((r) => r.id));
  let latest = null;
  for (const r of D.runes) if (r.id !== "root" && lv(P, r.id) >= 1 && (!latest || r.costs[0] > latest.costs[0])) latest = r;
  const front = new Set();
  if (latest) { front.add(latest.id); D.runes.filter((r) => r.req.includes(latest.id)).forEach((r) => front.add(r.id)); }
  const fog = new Set(D.runes.filter((r) => !open.has(r.id) && !sealed.has(r.id) && ringOpen(P, r.ring, pl) && r.req.some((q) => front.has(q))).map((r) => r.id));
  return D.runes.filter((r) => open.has(r.id) || sealed.has(r.id) || fog.has(r.id)).map((r) => ({ r, sealed: sealed.has(r.id) }));
}

// ---------- plates, medallions, icon centring (tileArt, medallionShift, medallionIcon, RuneIconFix) ----------
const FRAME = { CHEST: "red", LUCK: "pink", GREEN: "green", BLUE: "blue", PURPLE: "purple", CYAN: "cyan", LEGEND: "legend" };
function tileArt(r, owned, unl, maxed) {
  if (r.id === "root") return "rune_root";
  if (keystone(r)) return owned ? "rune_key_" + (FRAME[r.branch] || "gold") : unl ? "rune_key_open" : "rune_key_locked";
  if (!owned && !unl) return "rune_frame_locked";
  if (!owned) return "rune_frame_open";
  if (maxed) return "rune_frame_max";
  return "rune_frame_" + (FRAME[r.branch] || "gold");
}
function medallionShift(r, owned, unl, maxed) {
  if (r.id === "root") return [0, -0.01];
  if (keystone(r)) return [0, owned ? ({ GREEN: 0.075, BLUE: 0.052, PURPLE: 0.053, CYAN: 0.053, LEGEND: 0.041 }[r.branch] ?? 0.06) : unl ? 0.042 : 0.046];
  if (maxed) return [-0.046, 0.023];
  if (owned && r.branch === "LUCK") return [0, -0.022];
  return [0, 0];
}
const medallionIcon = (r, owned) => (r.id === "root" ? 0.56 : keystone(r) && owned && !["GOLD", "CHEST", "LUCK"].includes(r.branch) ? 0.5 : keystone(r) ? 0.56 : 0.6);
const iconFix = (r, state) => D.fix[`${r.id}:${state}`] || [0, 0];
const lockFix = (r) => (r.id === "root" ? [-0.0023, -0.0091] : keystone(r) ? [-0.0031, -0.0335] : [-0.0031, -0.0123]);
const tileSize = (r, sealed) => (sealed ? 62 : r.id === "root" ? 84 : keystone(r) ? 74 : 62) * DP;

/** The picture of a rune: rune_n_<id>, then rune_icon_<key>, otherwise its emoji. */
function iconImg(r, size, cls) {
  const im = el("img", cls || ""); im.alt = ""; im.style.width = im.style.height = size + "px";
  im.src = `img/rune_n_${r.id}.webp`;
  im.onerror = () => { im.onerror = () => { const s = el("span", cls || "", D.fx[r.effect].emoji); s.style.fontSize = size * 0.78 + "px"; s.style.cssText += ";display:block;line-height:1;text-align:center"; im.replaceWith(s); }; im.src = `img/rune_icon_${D.fx[r.effect].icon}.webp`; };
  return im;
}

// ---------- layout (the screen, in art units measured off the app) ----------
// The map takes whatever the info card below it leaves (the card hangs off the bottom edge, like the app's column layout).
const MAP = { x: 20, y: 269, w: 801, h: 1206 }, CARD = { x: 26, w: 787, bottom: 80 };
export function layoutRunes(c) {
  const { place, $ } = c;
  place($("ru-back"), 37, 135, 103, 99); place($("ru-title"), 215, 130, 420, 96); place($("ru-gold"), 655, 150, 158, 60);
  const d = $("ru-detail"), u = c.u(); d.style.left = CARD.x * u + "px"; d.style.width = CARD.w * u + "px"; d.style.top = "auto"; d.style.height = "auto"; d.style.bottom = CARD.bottom * u + "px"; d.style.minHeight = 150 * DP * u + "px";
}

// ---------- the map ----------
function clampView(map, reach) {
  const lx = reach.x * SP * view.s, ly = reach.y * SP * view.s;
  view.x = Math.max(-lx, Math.min(lx, view.x)); view.y = Math.max(-ly, Math.min(ly, view.y));
}
function applyView(layer) { layer.style.transform = `translate(${view.x}px,${view.y}px) scale(${view.s})`; }

function buildMap(c) {
  const { $, P } = c, u = c.u(), pl = c.levelOf(P.xp).level, map = $("ru-map"), shown = visibleRunes(P, pl);
  map.replaceChildren();
  const W = MAP.w * u, H = MAP.h * u, sp = SP * u, cx = W / 2, cy = H / 2;
  const reach = { x: Math.max(...shown.map((s) => Math.abs(s.r.x))), y: Math.max(...shown.map((s) => Math.abs(s.r.y))) };
  const minScale = Math.min(0.6, Math.max(0.12, Math.min(W / ((2 * reach.x + 1.4) * sp), H / ((2 * reach.y + 1.4) * sp))));
  const key = shown.map((s) => s.r.id + (s.sealed ? "s" : "")).join(",") + "|" + u.toFixed(3);
  if (view.key !== key) { if (!view.key || view.key.split("|")[1] !== key.split("|")[1]) { view.s = 0.78; view.x = 0; view.y = 0; } view.key = key; }
  view.s = Math.max(minScale, Math.min(1.5, view.s)); clampView(map, { x: reach.x * u, y: reach.y * u });
  const layer = el("div", "ru-layer"); layer.style.cssText = `width:${W}px;height:${H}px`; map.append(layer);
  const visIds = new Set(shown.map((s) => s.r.id)), pt = (r) => [cx + r.x * sp, cy + r.y * sp];
  // the ropes: dashed trail while locked, beige when open, gold once owned (a painted tile repeated along each link)
  for (const { r } of shown) for (const q of r.req) {
    if (!visIds.has(q)) continue;
    const p = byId(q), [x1, y1] = pt(p), [x2, y2] = pt(r), owned = lv(P, r.id) >= 1, open = canBuy(P, r, pl);
    const len = Math.hypot(x2 - x1, y2 - y1), ang = Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI, th = (owned || open ? 20 : 14) * DP * u;
    const rope = el("i", "ru-rope " + (owned ? "own" : open ? "open" : "lock"));
    rope.style.cssText = `left:${x1}px;top:${y1 - th / 2}px;width:${len}px;height:${th}px;transform:rotate(${ang}deg)`;
    layer.append(rope);
  }
  for (const { r, sealed } of shown) layer.append(tile(c, r, sealed, pl, u, cx, cy, sp));
  // one-finger drag, two-finger pinch and the wheel; a tap on the bare map clears the selection
  const ptr = new Map(); let dragged = false, startDist = 0, startScale = 1;
  const lim = { x: reach.x * u, y: reach.y * u };
  map.onpointerdown = (e) => { ptr.set(e.pointerId, [e.clientX, e.clientY]); dragged = false; if (ptr.size === 2) { const [a, b] = [...ptr.values()]; startDist = Math.hypot(a[0] - b[0], a[1] - b[1]); startScale = view.s; } };
  map.onpointermove = (e) => {
    if (!ptr.has(e.pointerId)) return;
    const prev = ptr.get(e.pointerId), cur = [e.clientX, e.clientY]; ptr.set(e.pointerId, cur);
    if (ptr.size === 1) { const dx = cur[0] - prev[0], dy = cur[1] - prev[1]; if (Math.abs(dx) + Math.abs(dy) > 0) { if (!dragged && Math.hypot(cur[0] - e.clientX, 0) >= 0) if (!dragged && Math.abs(dx) + Math.abs(dy) > 1.5) { dragged = true; try { map.setPointerCapture(e.pointerId); } catch {} } view.x += dx; view.y += dy; clampView(map, lim); applyView(layer); } }
    else if (ptr.size === 2) { dragged = true; const [a, b] = [...ptr.values()], d = Math.hypot(a[0] - b[0], a[1] - b[1]); zoomAt(map, layer, minScale, lim, startScale * d / (startDist || d), (a[0] + b[0]) / 2, (a[1] + b[1]) / 2); }
  };
  const up = (e) => { ptr.delete(e.pointerId); };
  map.onpointerup = up; map.onpointercancel = up;
  map.onwheel = (e) => { e.preventDefault(); zoomAt(map, layer, minScale, lim, view.s * (e.deltaY < 0 ? 1.12 : 1 / 1.12), e.clientX, e.clientY); };
  map.onclick = (e) => { if (dragged) { dragged = false; return; } if (e.target === map || e.target === layer) { sel = null; renderRunes(c, true); } };
  applyView(layer);
}
/** Zooms so the point under the fingers stays put (the layer scales around the middle of the map). */
function zoomAt(map, layer, minScale, lim, ns, px, py) {
  const r = map.getBoundingClientRect(), fx = px - r.left - r.width / 2, fy = py - r.top - r.height / 2, old = view.s;
  ns = Math.max(minScale, Math.min(1.5, ns)); const ratio = ns / old; view.s = ns;
  view.x = fx - (fx - view.x) * ratio; view.y = fy - (fy - view.y) * ratio; clampView(map, lim); applyView(layer);
}

/** One rune on the map (RuneTile): the halo, the painted plate, the picture centred in its medallion, the upgrade badge, the level pips. */
function tile(c, r, sealed, pl, u, cx, cy, sp) {
  const { P } = c, level = lv(P, r.id), owned = level >= 1, root = r.id === "root", key = keystone(r);
  const unl = canBuy(P, r, pl), maxed = owned && level >= r.costs.length && !key && !root;
  const cost = nextCost(P, r), afford = unl && cost !== null && P.gold >= cost, size = tileSize(r, sealed) * u;
  const b = el("button", "ru-n" + (afford ? " aff" : "") + (sel === r.id ? " sel" : ""));
  b.dataset.rune = r.id;
  b.style.cssText = `left:${cx + r.x * sp - size / 2}px;top:${cy + r.y * sp - size / 2}px;width:${size}px;height:${size}px`;
  if (sel === r.id || afford) { const h = el("img", "halo"); h.src = "img/rune_halo.webp"; h.alt = ""; h.style.cssText = `width:${size * 1.45}px;height:${size * 1.45}px;left:${-size * 0.225}px;top:${-size * 0.225}px;${sel === r.id ? "opacity:1;animation:none" : ""}`; b.append(h); }
  const pl_ = el("img", "pl"); pl_.alt = ""; pl_.src = "img/" + (sealed ? "rune_gate_seal" : tileArt(r, owned, unl, maxed)) + ".webp";
  const ps = size * (maxed ? 1.16 : 1); pl_.style.cssText = `width:${ps}px;height:${ps}px;left:${(size - ps) / 2}px;top:${(size - ps) / 2}px`; b.append(pl_);
  b.onclick = (e) => { e.stopPropagation(); if (r.id === "root") { openStatus(c); } else { sel = sel === r.id ? null : r.id; } renderRunes(c, true); };
  if (sealed) return b;
  const [sx, sy] = medallionShift(r, owned, unl, maxed), [fx, fy] = unl || owned ? iconFix(r, owned ? (maxed ? 1 : 0) : 2) : lockFix(r);
  const holder = el("div", "ic"); holder.style.cssText = `left:${size * (sx + fx)}px;top:${size * (sy + fy)}px;width:${size}px;height:${size}px`;
  if (unl || owned) { const isz = size * medallionIcon(r, owned), im = iconImg(r, isz); im.style.opacity = owned || root ? 1 : 0.7; holder.append(im); }
  else { const lk = el("img"); lk.src = "img/rune_icon_lock.webp"; lk.alt = ""; lk.style.cssText = `width:${size * 0.42}px;height:${size * 0.42}px;opacity:.85`; holder.append(lk); }
  b.append(holder);
  if (afford) { const bd = el("img", "bdg"); bd.src = "img/badge_upgrade.webp"; bd.alt = ""; bd.style.cssText = `right:${-5 * DP * u}px;top:${-5 * DP * u}px;width:${20 * DP * u}px;height:${20 * DP * u}px`; b.append(bd); }
  if (r.costs.length > 5) b.append(el("i", "lvl", `${level}/${r.costs.length}`));
  else if (r.costs.length > 1 && !root) {
    const row = el("div", "pips"); row.style.cssText = `bottom:${-9 * DP * u}px;gap:${DP * u}px`;
    for (let i = 0; i < r.costs.length; i++) { const p = el("img"); p.src = i < level ? "img/rune_pip_full.webp" : "img/rune_pip_empty.webp"; p.alt = ""; p.style.cssText = `width:${14 * DP * u}px;height:${14 * DP * u}px`; row.append(p); }
    b.append(row);
  }
  return b;
}

// ---------- the info card (InfoCard / RuneDetail) ----------
const unitText = (ui, unit, v) => (unit === "PERCENT" ? fmt(ui("rune_tile_pct"), v) : unit === "POINTS" ? fmt(ui("rune_tile_pts"), v) : unit === "COUNT" ? fmt(ui(v === 1 ? "rune_tile_cnt_one" : "rune_tile_cnt"), v) : ui("rune_tile_on"));
function drawCard(c) {
  const { $, P, lang } = c, ui = (k) => D.ui[k][lang()], pl = c.levelOf(P.xp).level, det = $("ru-detail"), u = c.u();
  det.replaceChildren();
  const r = sel ? byId(sel) : null;
  if (!r) {
    const next = D.runes.filter((q) => canBuy(P, q, pl) && nextCost(P, q) !== null).sort((a, b) => nextCost(P, a) - nextCost(P, b))[0];
    det.append(el("div", "ru-hint", next ? fmt(ui("rune_hint_next"), next[lang()], fmtNum(nextCost(P, next))) : ui("rune_hint_all")), el("div", "ru-hint2", ui("rune_status_hint")));
    return;
  }
  const level = lv(P, r.id), fx = D.fx[r.effect], unl = unlocked(P, r), cost = nextCost(P, r);
  const head = (img, title, body, now) => { const row = el("div", "ru-row"); row.append(img, (() => { const col = el("div", "ru-col"); col.append(el("div", "ru-t", title), el("div", "ru-b", body)); if (now) col.append(el("div", "ru-n2", now)); return col; })()); det.append(row); };
  if (unl && level < 1 && !ringOpen(P, r.ring, pl)) {
    // a sealed ring: what opens it and where the player stands
    const seal = el("img", "ru-ico"); seal.src = "img/rune_gate_seal.webp"; seal.alt = "";
    head(seal, ui("rune_gate_title"), fmt(ui("rune_gate_body"), gateLevel(r.ring), GATES.need), fmt(ui("rune_gate_now"), pl, Math.floor(ringProgress(P, r.ring - 1) * 100)));
    return;
  }
  if (!unl && level < 1) {
    // a rune whose neighbours are not bought yet is still a secret
    const q = el("img", "ru-ico"); q.src = "img/rune_soon.webp"; q.alt = ""; q.style.opacity = 0.85;
    head(q, ui("rune_fog_title"), ui("rune_fog_body"));
    return;
  }
  const owned = level >= 1, maxed = owned && level >= r.costs.length && !keystone(r) && r.id !== "root";
  const box = el("div", "ru-ico plate"); const art = el("img"); art.src = "img/" + tileArt(r, owned, true, maxed) + ".webp"; art.alt = ""; art.style.cssText = "position:absolute;inset:0;width:100%;height:100%";
  const [sx, sy] = medallionShift(r, owned, true, maxed), [fx2, fy2] = iconFix(r, owned ? (maxed ? 1 : 0) : 2), S = 66 * DP * u, isz = S * medallionIcon(r, owned);
  const im = iconImg(r, isz); im.style.cssText += `;position:absolute;left:${(S - isz) / 2 + S * (sx + fx2)}px;top:${(S - isz) / 2 + S * (sy + fy2)}px`;
  box.append(art, im);
  const col = el("div", "ru-col"), nameRow = el("div", "ru-namerow", `<span class="ru-t">${r[lang()]}</span>`);
  if (r.id !== "root") nameRow.append(el("span", "ru-lvpill", fmt(ui("rune_level_of"), level, r.costs.length)));
  col.append(nameRow);
  const fl = r["flavor_" + lang()]; if (fl) col.append(el("div", "ru-fl", fl));
  col.append(el("div", "ru-b", r.id === "root" ? ui("rune_root_desc") : fmt(fx[lang()], r.per)));
  const row = el("div", "ru-row"); row.append(box, col); det.append(row);
  if (r.id === "root") return;
  // one box per level: what that level brings in total, the reached ones lit
  if (!(fx.unit === "FLAG" && r.costs.length === 1)) {
    const long = r.costs.length > 5, nums = long ? [...new Set([Math.max(level, 1), Math.min(level + 1, r.costs.length)])] : r.costs.map((_, i) => i + 1);
    const boxes = el("div", "ru-boxes");
    nums.forEach((n) => {
      const reached = level >= n, isNext = level + 1 === n, bx = el("div", "ru-box " + (reached ? "gold" : isNext ? "empty" : "gray"));
      const val = unitText(ui, fx.unit, r.per * n);
      bx.innerHTML = `<img src="img/${reached ? "rune_pip_full" : "rune_pip_empty"}.webp" alt=""><span class="n">${fmt(ui("rune_level_n"), n)}</span><b class="${reached ? "r" : isNext ? "x" : "g"}">${val}</b>`;
      boxes.append(bx);
    });
    det.append(boxes);
  }
  if (cost === null) det.append(el("div", "ru-state ok", ui("rune_maxed")));
  else if (!unl) det.append(el("div", "ru-state bad", ui("rune_locked")));
  else if (!ringOpen(P, r.ring, pl)) det.append(el("div", "ru-state bad", fmt(ui("rune_status_ring_locked"), gateLevel(r.ring))));
  else {
    const can = P.gold >= cost, btn = el("button", "ru-buy " + (can ? "on" : "off"), `<img src="img/icon_gold_coin.webp" alt=""><span>${fmt(ui("rune_buy"), fmtNum(cost))}</span>`);
    btn.onclick = () => {
      if (!can) return;
      // the player's first rune ever gets a window of its own (RunesScreen.kt: firstRune)
      const first = D.runes.every((q) => q.id === "root" || lv(P, q.id) === 0), reached = level + 1, grand = reached >= r.costs.length;
      P.gold -= cost; P.runes[r.id] = reached; c.saveP(); c.refreshHome(); c.sfx && c.sfx.buy && c.sfx.buy();
      c.analytics && c.analytics("rune_bought", { rune_id: r.id, rune_level: reached, ring: r.ring, player_level: pl, gold_left: P.gold });
      renderRunes(c, true); celebrate(c, grand); purchaseShow(c, r, reached, grand);
      if (first) firstRuneDialog(c, r);
    };
    det.append(btn);
  }
}
function celebrate(c, grand) {
  const host = c.$("runes"), f = el("div", "rs-conf"), n = grand ? 150 : 36, span = grand ? 2.3 : 1.2; host.append(f);
  for (let i = 0; i < n; i++) { const e = el("i", ""); e.style.cssText = `left:${Math.random() * 100}%;background:hsl(${Math.random() * 360},85%,60%);animation-delay:${Math.random() * span * 0.35}s;animation-duration:${span * (0.55 + Math.random() * 0.45)}s`; f.append(e); }
  setTimeout(() => f.remove(), (span + 0.6) * 1000);
}

// ---------- the windows of a first visit and of a first rune (RunesScreen.kt: showIntro, firstRune) ----------
function introDialog(c) {
  const { lang } = c, ui = (k) => D.ui[k][lang()];
  introOpen = true;
  paintedFrameDialog(c.$("stage"), { title: ui("rune_intro_title"), body: ui("rune_intro_body"), buttonText: ui("rune_intro_ok"), onClick: () => { introOpen = false; c.P.runesIntroSeen = true; c.saveP(); } });
}
function firstRuneDialog(c, r) {
  const { lang } = c, ui = (k) => D.ui[k][lang()];
  paintedFrameDialog(c.$("stage"), { title: ui("rune_first_title"), body: fmt(ui("rune_first_body"), r[lang()]) + "\n\n" + fmt(D.fx[r.effect][lang()], r.per), buttonText: ui("rune_first_ok"), onClick: () => {} });
}

// ---------- the show round a rune just bought (RuneTile / RuneBurstEffects / GrandFinish) ----------
const BRANCH = { ROOT: ["#FFD35E", "#E08A1B"], GOLD: ["#FFD35E", "#E59A12"], CHEST: ["#FF8F80", "#D42A1E"], LUCK: ["#FF9BD0", "#D43A8E"], GREEN: ["#8BE08B", "#2E9B3E"], BLUE: ["#8CC0FF", "#2A62C8"], PURPLE: ["#C79BFF", "#7A3FC8"], CYAN: ["#8FE6EA", "#1FA6B0"], LEGEND: ["#B69AE0", "#3C2A5E"] };
const hexA = (hex, a) => `rgba(${parseInt(hex.slice(1, 3), 16)},${parseInt(hex.slice(3, 5), 16)},${parseInt(hex.slice(5, 7), 16)},${Math.max(0, Math.min(1, a))})`;
const easeOutSlow = (t) => 1 - Math.pow(1 - t, 3); // LinearOutSlowInEasing
/** One frame of the rings, sparks and flash that play in front of the tile, [t] 0..1 through the show, [s] the tile's size in canvas pixels. */
function frontFx(g, B, s, t, grand, light, deep) {
  g.clearRect(0, 0, B, B); const cx = B / 2, cy = B / 2;
  const ring = (tt, from, to, thick) => { if (tt <= 0 || tt >= 1) return; g.beginPath(); g.arc(cx, cy, s * (from + (to - from) * tt), 0, Math.PI * 2); g.lineWidth = s * thick * (1 - tt) + 1; g.strokeStyle = hexA(light, (1 - tt) * 0.95); g.stroke(); };
  ring(t, 0.45, 1.3, 0.14);
  if (grand) { ring((t - 0.12) / 0.88, 0.4, 1.9, 0.16); ring((t - 0.28) / 0.72, 0.4, 2.3, 0.10); }
  const n = grand ? 24 : 10, e = 1 - (1 - t) * (1 - t);
  for (let i = 0; i < n; i++) {
    const a = ((i * 360) / n + (grand ? 7 : 0) + (i % 2) * 9) * Math.PI / 180, dist = s * (0.55 + (0.75 + (i % 3) * 0.28) * e * (grand ? 1.6 : 1)), r = s * (i % 3 === 0 ? 0.085 : 0.055) * (1 - t);
    const x = cx + dist * Math.cos(a), y = cy + dist * Math.sin(a);
    g.beginPath(); g.moveTo(x, y - r * 2); g.lineTo(x + r * 0.7, y - r * 0.7); g.lineTo(x + r * 2, y); g.lineTo(x + r * 0.7, y + r * 0.7); g.lineTo(x, y + r * 2); g.lineTo(x - r * 0.7, y + r * 0.7); g.lineTo(x - r * 2, y); g.lineTo(x - r * 0.7, y - r * 0.7); g.closePath();
    g.fillStyle = i % 2 === 0 ? `rgba(255,255,255,${1 - t})` : hexA(deep, 1 - t); g.fill();
  }
  if (grand) { const f = Math.max(0, Math.min(1, 1 - Math.abs(t - 0.2) / 0.2)); if (f > 0) { g.beginPath(); g.arc(cx, cy, s * 0.95, 0, Math.PI * 2); g.fillStyle = `rgba(255,255,255,${0.9 * f})`; g.fill(); } }
}
/** The turning crown of rays behind the tile (the last level only). */
function behindFx(g, B, t, light) {
  g.clearRect(0, 0, B, B); const cx = B / 2, cy = B / 2, reach = B / 2, fade = Math.sin(Math.PI * Math.max(0, Math.min(1, t))), spin = t * 70;
  for (let i = 0; i < 14; i++) {
    const a = (spin + (i * 360) / 14) * Math.PI / 180, w = 5.5 * Math.PI / 180, gr = g.createRadialGradient(cx, cy, 0, cx, cy, reach);
    gr.addColorStop(0, `rgba(255,255,255,${0.85 * fade})`); gr.addColorStop(0.5, hexA(light, 0.55 * fade)); gr.addColorStop(1, hexA(light, 0));
    g.beginPath(); g.moveTo(cx, cy); g.lineTo(cx + reach * Math.cos(a - w), cy + reach * Math.sin(a - w)); g.lineTo(cx + reach * Math.cos(a + w), cy + reach * Math.sin(a + w)); g.closePath(); g.fillStyle = gr; g.fill();
  }
}
function purchaseShow(c, r, level, grand) {
  const { $, lang } = c, tileEl = $("ru-map").querySelector(`[data-rune="${r.id}"]`); if (!tileEl) return;
  const ui = (k) => D.ui[k][lang()], u = c.u(), size = tileSize(r, false) * u, [light, deep] = BRANCH[r.branch] || BRANCH.GOLD;
  const dur = grand ? 1700 : 900, box = size * (grand ? 4.2 : 3.2), dpr = Math.min(2, window.devicePixelRatio || 1) * Math.max(1, view.s), cv = (cls) => { const k = el("canvas", "ru-fx " + cls); k.width = k.height = Math.round(box * dpr); k.style.width = k.style.height = box + "px"; return k; };
  tileEl.style.isolation = "isolate";
  const front = cv(""), fg = front.getContext("2d"), back = grand ? cv("behind") : null, bg = back && back.getContext("2d");
  if (back) tileEl.append(back); tileEl.append(front);
  // the tile jumps (the last level crouches first and leaps higher), the dot of the level reached pops in
  tileEl.animate(grand ? [{ transform: "scale(1)" }, { transform: "scale(0.8)", offset: 0.08 }, { transform: "scale(1.5)", offset: 0.22 }, { transform: "scale(1)", easing: "cubic-bezier(.3,1.6,.5,1)" }] : [{ transform: "scale(1)" }, { transform: "scale(1.32)", offset: 0.18 }, { transform: "scale(1)", easing: "cubic-bezier(.3,1.6,.5,1)" }], { duration: grand ? 900 : 520 });
  const pip = tileEl.querySelector(".pips")?.children[level - 1]; if (pip) pip.animate([{ transform: "scale(2.6)" }, { transform: "scale(1)" }], { duration: 480, easing: "cubic-bezier(.3,1.6,.5,1)" });
  // the plate a last level had before dissolves over the new one
  if (grand && tileEl.querySelector("img.pl")) { const old = el("img", "ru-old"); old.src = "img/" + tileArt(r, true, true, false) + ".webp"; old.alt = ""; old.style.cssText = `left:0;top:0;width:${size}px;height:${size}px`; tileEl.append(old); old.animate([{ opacity: 1 }, { opacity: 0 }], { duration: dur * 0.45, fill: "forwards" }).onfinish = () => old.remove(); }
  // the gain floats up above the tile
  const fx = D.fx[r.effect];
  if (fx.unit !== "FLAG" && r.per > 0) {
    const g = el("div", "ru-gain", unitText(ui, fx.unit, r.per)); tileEl.append(g);
    g.animate([{ transform: `translateY(${-size * 0.55}px)`, opacity: 1, offset: 0 }, { opacity: 1, offset: 0.55 }, { transform: `translateY(${-size * 1.45}px)`, opacity: 0 }], { duration: dur, easing: "ease-out" }).onfinish = () => g.remove();
  }
  const t0 = performance.now(), step = (now) => {
    const t = Math.min(1, (now - t0) / dur), k = easeOutSlow(t);
    if (bg) behindFx(bg, back.width, k, light); frontFx(fg, front.width, size * dpr, k, grand, light, deep);
    if (t < 1) requestAnimationFrame(step); else { front.remove(); back && back.remove(); tileEl.style.isolation = ""; }
  };
  requestAnimationFrame(step);
  if (grand) grandFinish(c);
}
/** The screen's part of a rune's last level: a gold wash that clears, and the banner that says it is complete. */
function grandFinish(c) {
  const host = c.$("runes"), wash = el("div", "rs-wash"), banner = el("div", "rs-banner", D.ui.rune_complete_banner[c.lang()]); host.append(wash, banner);
  wash.animate([{ opacity: 0 }, { opacity: 1, offset: 0.17 }, { opacity: 0 }], { duration: 1080 }).onfinish = () => wash.remove();
  banner.animate([{ opacity: 0, transform: "scale(.4)" }, { opacity: 1, transform: "scale(1)", offset: 0.22, easing: "cubic-bezier(.3,1.5,.5,1)" }, { opacity: 1, transform: "scale(1)", offset: 0.77 }, { opacity: 0, transform: "scale(.4)" }], { duration: 1900 }).onfinish = () => banner.remove();
}

// ---------- the status window (RuneStatusDialog) ----------
const STAT_ROWS = [["WORD", "rune_stat_word"], ["WIN", "rune_stat_win"], ["LOGIN", "rune_stat_login"], ["DAILY", "rune_stat_daily"], ["CHEST", "rune_stat_chest"], ["CJOKER", "rune_stat_cjoker"], ["DISC", "rune_stat_disc"], ["DJ", "rune_stat_dj"], ["XP", "rune_stat_xp"], ["CTIME", "rune_stat_ctime"], ["ADCD", "rune_stat_adcd"], ["ROOM", "rune_stat_room"], ["HOST", "rune_stat_host"]];
const REDUCTIONS = new Set(["DISC", "CTIME", "ADCD"]);
const todayKey = () => todayEpoch();
export const abilityState = (P) => ({ instantReady: has(P, "INSTANT") && P.instantDay !== todayKey(), bestReady: has(P, "BEST") && P.bestDay !== todayKey(), insuranceReady: has(P, "INSURE") && P.insMonth !== monthKey(), reserveWaiting: !!(P.chests && P.chests.reserve) });
export const monthKey = () => turkeyMonth();
function openStatus(c) { statusOpen = true; c.analytics && c.analytics("rune_status_opened", {}); }
function drawStatus(c) {
  const { $, P, lang } = c, host = $("ru-status"); host.replaceChildren();
  if (!statusOpen) { host.classList.add("hidden"); return; }
  host.classList.remove("hidden");
  const ui = (k) => D.ui[k][lang()], pl = c.levelOf(P.xp).level, runes = D.runes.filter((r) => r.id !== "root"), ab = abilityState(P);
  const sec = (t) => el("div", "st-sec", t), row = (label, value, extra, dim) => el("div", "st-row" + (dim ? " dim" : ""), `<span class="l">${label}</span><span class="v">${value}</span>${extra ? `<span class="x"> ${extra}</span>` : ""}`);
  const card = el("div", "st-card"), list = el("div", "st-list");
  card.append(el("div", "st-title lt", ui("rune_status_title")), list);
  list.append(sec(ui("rune_status_general")), row(ui("rune_status_player_level"), pl),
    row(ui("rune_status_rune_levels"), `${runes.reduce((s, r) => s + lv(P, r.id), 0)} / ${runes.reduce((s, r) => s + r.costs.length, 0)}`),
    row(ui("rune_status_spent"), fmtNum(runes.reduce((s, r) => { let t = 0; for (let i = 0; i < lv(P, r.id); i++) t += price(r, i) || 0; return s + t; }, 0))));
  for (let ring = 1; ring <= 4; ring++) {
    const label = ring === 4 ? ui("rune_status_ring_legend") : fmt(ui("rune_status_ring"), ring);
    list.append(ringOpen(P, ring, pl) ? row(label, fmt(ui("rune_status_percent"), Math.floor(ringProgress(P, ring) * 100))) : row(label, fmt(ui("rune_status_ring_locked"), gateLevel(ring)), null, true));
  }
  list.append(sec(ui("rune_status_bonuses")));
  for (const [fx, key] of STAT_ROWS) {
    const now = effect(P, fx), top = D.caps[fx] ?? D.runes.filter((r) => r.effect === fx).reduce((s, r) => s + r.costs.length * r.per, 0), unit = D.fx[fx].unit;
    const t = (v) => (unit === "COUNT" ? fmt(ui("rune_status_count"), v) : REDUCTIONS.has(fx) ? fmt(ui("rune_status_minus"), v) : fmt(ui("rune_tile_pct"), v));
    list.append(row(ui(key), t(now), "/ " + t(top), now === 0));
  }
  list.append(sec(ui("rune_status_abilities")));
  for (const r of runes.filter((q) => D.fx[q.effect].unit === "FLAG" && q.effect !== "STYLE")) {
    const owned = lv(P, r.id) >= 1;
    const st = !owned ? "rune_status_locked" : r.effect === "INSTANT" ? (ab.instantReady ? "rune_status_ready_today" : "rune_status_used_today") : r.effect === "BEST" ? (ab.bestReady ? "rune_status_ready_today" : "rune_status_used_today")
      : r.effect === "INSURE" ? (ab.insuranceReady ? "rune_status_ready_month" : "rune_status_used_month") : r.effect === "RESERVE" ? (ab.reserveWaiting ? "rune_status_reserve_full" : "rune_status_reserve_empty") : "rune_status_on";
    list.append(row(r[lang()], ui(st), null, !owned));
  }
  const style = runes.filter((r) => r.effect === "STYLE");
  if (style.length) {
    list.append(sec(ui("rune_status_collection")), row(ui("rune_status_rewards"), `${style.filter((r) => lv(P, r.id) >= 1).length} / ${style.length}`));
    style.forEach((r) => list.append(row(r[lang()], ui(lv(P, r.id) >= 1 ? "rune_status_owned" : "rune_status_locked"), null, lv(P, r.id) < 1)));
  }
  const close = el("button", "st-close", `<span>${ui("close")}</span>`); close.onclick = () => { statusOpen = false; drawStatus(c); };
  card.append(close); host.append(card); host.onclick = (e) => { if (e.target === host) { statusOpen = false; drawStatus(c); } };
}

export function renderRunes(c, keepView) {
  const { $, P, lang } = c; P.runes = P.runes || {};
  const ui = (k) => D.ui[k][lang()];
  $("ru-title").textContent = ui("rune_screen_title"); $("ru-goldn").textContent = fmtNum(P.gold);
  if (!keepView) { sel = null; statusOpen = false; view = { s: 0.78, x: 0, y: 0, key: "" }; }
  drawCard(c);
  // the card decides how tall the map is: its top edge, less the app's 8 dp gap
  const u = c.u(), cardTop = ($("ru-detail").offsetTop) / u; MAP.h = Math.max(300, cardTop - 16 - MAP.y);
  c.place($("ru-map"), MAP.x, MAP.y, MAP.w, MAP.h);
  buildMap(c); drawStatus(c);
  // the first visit: what the runes are (the guide, when one brings the player here, explains itself)
  if (!keepView && !P.runesIntroSeen && !introOpen) introDialog(c);
}
