// Karalak Runes: spend gold to open a tree of permanent bonuses. Same tree, costs and caps as the app.
let D = null, sel = null;
const SP = 116, TIERS = { 2: 15, 3: 30 }, NEED = 0.7;
const FRAME = { GOLD: "gold", ROOT: "gold", CHEST: "red", LUCK: "pink", GREEN: "green", BLUE: "blue", PURPLE: "purple", CYAN: "cyan", LEGEND: "legend" };
const CAP = { WORD: 40, WIN: 40, LOGIN: 50, DAILY: 50, CHEST: 50, CJOKER: 20, DISC: 40, XP: 30, CTIME: 40, ADCD: 30, ROOM: 40, HOST: 30 };
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
export async function loadRunes() { if (!D) D = await (await fetch("runes.json")).json(); }
const lv = (P, id) => id === "root" ? 1 : Math.min(P.runes[id] || 0, D.runes.find((r) => r.id === id).costs.length);
const unlocked = (P, r) => r.req.every((q) => lv(P, q) >= 1);
function ringProgress(P, ring) { const m = D.runes.filter((r) => r.ring === ring && r.id !== "root"); if (!m.length) return 1; return m.reduce((s, r) => s + lv(P, r.id), 0) / m.reduce((s, r) => s + r.costs.length, 0); }
const ringOpen = (P, ring, playerLevel) => ring <= 1 || (playerLevel >= TIERS[ring] && ringProgress(P, ring - 1) >= NEED);
const canBuy = (P, r, pl) => unlocked(P, r) && ringOpen(P, r.ring, pl);
const cost = (P, r) => r.costs[lv(P, r.id)] ?? null;
// ---- effects other screens read ----
export function effect(P, fx) { const t = D ? D.runes.filter((r) => r.effect === fx).reduce((s, r) => s + lv(P, r.id) * r.per, 0) : 0; return Math.min(t, CAP[fx] ?? 1e9); }
export const loaded = () => !!D;

export function layoutRunes(c) {
  const { place, $ } = c;
  place($("ru-back"), 20, 40, 110, 106); place($("ru-title"), 130, 70, 450, 120); place($("ru-gold"), 600, 70, 220, 64);
  const m = $("ru-map"), u = c.u(); m.style.left = 30 * u + "px"; m.style.top = 230 * u + "px"; m.style.width = 781 * u + "px"; m.style.height = (c.stageH() - 230 * u - 330 * u) + "px";
  const d = $("ru-detail"); d.style.left = 30 * u + "px"; d.style.width = 781 * u + "px"; d.style.bottom = 30 * u + "px";
}
export function renderRunes(c) {
  const { $, P, lang } = c, ui = (k) => D.ui[k][lang()]; P.runes = P.runes || {};
  $("ru-title").textContent = ui("rune_screen_title"); $("ru-goldn").textContent = P.gold.toLocaleString();
  const pl = c.levelOf(P.xp).level, map = $("ru-map"), u = c.u(); map.replaceChildren();
  const vis = D.runes.filter((r) => r.id === "root" || r.requires === undefined || true).filter((r) => r.id === "root" || unlockedOrNeighbor(P, r, pl));
  const reachX = Math.max(...vis.map((r) => Math.abs(r.x))), reachY = Math.max(...vis.map((r) => Math.abs(r.y)));
  const W = map.clientWidth || 781 * u, H = map.clientHeight || 900 * u;
  const spacing = Math.min(W / (2 * reachX + 1.6), H / (2 * reachY + 1.6), SP * u * 1.4);
  const cx = W / 2, cy = H / 2, size = (r) => (r.id === "root" ? 84 : r.costs.length === 1 ? 74 : 62) * spacing / SP * 1.1;
  const cv = el("canvas", "ru-cv"); cv.width = W * 2; cv.height = H * 2; cv.style.cssText = "position:absolute;inset:0;width:100%;height:100%"; map.append(cv);
  const g = cv.getContext("2d"); g.scale(2, 2); g.lineCap = "round";
  const pt = (r) => [cx + r.x * spacing, cy + r.y * spacing];
  vis.forEach((r) => r.req.forEach((q) => { const p = vis.find((x) => x.id === q); if (!p) return; const owned = lv(P, r.id) >= 1, open = canBuy(P, r, pl), [x1, y1] = pt(p), [x2, y2] = pt(r);
    g.strokeStyle = owned ? "#e59a12" : open ? "#f7d890" : "#8b7a5c"; g.globalAlpha = owned || open ? 1 : 0.7; g.lineWidth = (owned || open ? 12 : 8) * spacing / SP; g.beginPath(); g.moveTo(x1, y1); g.lineTo(x2, y2); g.stroke(); g.globalAlpha = 1; }));
  vis.forEach((r) => {
    const L = lv(P, r.id), owned = L >= 1 && r.id !== "root", open = canBuy(P, r, pl), maxed = owned && L >= r.costs.length && r.costs.length > 1;
    const sealed = !ringOpen(P, r.ring, pl) && unlocked(P, r), key = r.costs.length === 1 && r.id !== "root";
    const sz = size(r), [x, y] = pt(r), b = el("button", "ru-n" + (sel === r.id ? " sel" : "") + (open && cost(P, r) <= P.gold ? " aff" : ""));
    b.style.cssText = `left:${x - sz / 2}px;top:${y - sz / 2}px;width:${sz}px;height:${sz}px`;
    let art = r.id === "root" ? "rune_root" : sealed ? "rune_gate_seal" : key ? (owned || open ? "rune_key_" + ({ CHEST: "red", LUCK: "pink", GREEN: "green", BLUE: "blue", PURPLE: "purple", CYAN: "cyan" }[r.branch] || "gold") : "rune_key_locked") : !owned && !open ? "rune_frame_locked" : !owned ? "rune_frame_open" : maxed ? "rune_frame_max" : "rune_frame_" + (FRAME[r.branch] || "gold");
    b.innerHTML = `<img class="pl" src="img/${art}.webp" alt="">` + (sealed ? "" : open || owned || r.id === "root" ? `<img class="ic" src="img/rune_n_${r.id}.webp" onerror="this.src='img/rune_icon_${D.fx[r.effect]?.icon || "dog"}.webp';this.onerror=null" alt="">` : `<img class="ic lk" src="img/rune_icon_lock.webp" alt="">`)
      + (r.costs.length > 1 && r.id !== "root" ? `<i class="lvl">${L}/${r.costs.length}</i>` : "");
    b.onclick = (e) => { e.stopPropagation(); sel = r.id; renderRunes(c); };
    map.append(b);
  });
  map.onclick = () => { sel = null; renderRunes(c); };
  const det = $("ru-detail"); det.replaceChildren();
  if (sel) {
    const r = D.runes.find((x) => x.id === sel), fx = D.fx[r.effect], L = lv(P, r.id), cst = cost(P, r), can = canBuy(P, r, pl), max = cst === null && r.id !== "root";
    const val = fx.unit === "PERCENT" ? `%${r.per * Math.max(L, 1)}` : fx.unit === "POINTS" ? `${r.per * Math.max(L, 1)} puan` : String(r.per * Math.max(L, 1));
    const desc = (fx[lang()] || "").replace("%%", "%").replace("%1$d", r.per);
    det.append(el("b", "", `${r[lang()]}`), el("small", "", r["flavor_" + lang()] || ""), el("p", "", desc));
    if (r.id !== "root") {
      det.append(el("div", "ru-lv", ui("rune_level_of").replace("%1$d", L).replace("%2$d", r.costs.length)));
      const btn = el("button", "pill orange small", `<span>${max ? ui("rune_maxed") : !unlocked(P, r) ? ui("rune_locked") : !can ? ui("rune_gate_title") : `🪙 ${cst.toLocaleString()}`}</span>`);
      if (max || !can || cst > P.gold) btn.classList.add("dis");
      btn.onclick = () => { if (max || !can || cst > P.gold) return; P.gold -= cst; P.runes[r.id] = L + 1; c.saveP(); c.refreshHome(); renderRunes(c); };
      det.append(btn);
    }
  }
}
// A rune is shown once the one it hangs from is owned (fog of war), plus the root and its direct children.
function unlockedOrNeighbor(P, r, pl) { return r.req.some((q) => q === "root" || lv(P, q) >= 1) || lv(P, r.id) >= 1; }
