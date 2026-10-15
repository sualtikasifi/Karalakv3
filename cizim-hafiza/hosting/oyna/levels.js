// Worlds list and level maps. Same rules as LevelCatalog / LevelMapViewModel: a level opens when the one before it has a
// result, a world opens when the previous world has ten results, stars come from accuracy.
let W = null;
let openWorld = 1, selected = null;
const T = {
  tr: { worlds: "Dünyalar", progress: (n) => `${n}/10 tamamlandı`, stage: (n) => `Aşama ${n}`, sub: "Çiz, tahmin et ve yıldızları topla!", play: "OYNA", locked: "Kilitli" },
  en: { worlds: "Worlds", progress: (n) => `${n}/10 completed`, stage: (n) => `Stage ${n}`, sub: "Draw, guess and collect the stars!", play: "PLAY", locked: "Locked" },
};
export async function loadWorlds() { if (!W) W = await (await fetch("worlds.json")).json(); }
export const worldInfo = (id) => W.worlds.find((w) => w.id === id);

export function mixFor(worldId, levelIndex) {
  const i = Math.min(levelIndex + (worldId - 1), 10);
  if (i <= 3) return { EASY: 6 };
  if (i <= 5) return { EASY: 3, MEDIUM: 3 };
  if (i <= 7) return { MEDIUM: 6 };
  if (i <= 9) return { MEDIUM: 3, HARD: 3 };
  return { HARD: 6 };
}
export const starsFor = (correct, total) => { if (!total) return 0; const r = correct / total; return r >= 0.85 ? 3 : r >= 0.6 ? 2 : r >= 0.3 ? 1 : 0; };
const done = (P, w) => Object.keys(P.levels).filter((k) => k.startsWith(w + ":")).length;
const worldOpen = (P, id) => id === 1 || done(P, id - 1) >= 10;
const levelOpen = (P, w, l) => l === 1 || P.levels[`${w}:${l - 1}`] !== undefined;

export function recordLevel(P, w, l, stars) { const k = `${w}:${l}`; P.levels[k] = Math.max(stars, P.levels[k] ?? 0); }

export function layoutWorlds(c) {
  const { place, $ } = c, u = c.u();
  place($("wl-back"), 20, 40, 110, 106);
  const l = $("wl-list"); l.style.left = 0; l.style.top = 440 * u + "px"; l.style.width = 841 * u + "px"; l.style.height = c.stageH() - 440 * u + "px";
}
export function renderWorlds(c) {
  const { $, P, lang } = c, x = T[lang()], list = $("wl-list");
  list.replaceChildren();
  W.worlds.forEach((w) => {
    const open = worldOpen(P, w.id), b = document.createElement("button");
    b.className = "wl-banner" + (open ? "" : " lock");
    b.innerHTML = `<img src="img/world_banner_${w.id}.webp" alt=""><span class="wl-tag">${open ? `${x.progress(done(P, w.id))} · ⭐${stars(P, w.id)}` : "🔒"}</span>`;
    b.onclick = () => { if (open) { openWorld = w.id; selected = null; c.show("lvmap"); } };
    list.append(b);
  });
}
const stars = (P, w) => Object.entries(P.levels).filter(([k]) => k.startsWith(w + ":")).reduce((s, [, v]) => s + v, 0);

export function layoutMap(c) {
  const { place, $ } = c;
  place($("lm-back"), 20, 40, 110, 106); place($("lm-world"), 160, 44, 520, 100);
  $("lm-bg").src = `img/bg_world_${openWorld}.webp`;
  const u = c.u();
  W.pos[openWorld].forEach(([px, py], i) => { const n = $("lm-n" + (i + 1)); if (!n) return; const s = 119; place(n, 841 * px / 100 - s / 2, 1870 * py / 100 - s / 2, s, s); });
  const ring = $("lm-ring"); if (selected) place(ring, 841 * W.pos[openWorld][selected - 1][0] / 100 - 80, 1870 * W.pos[openWorld][selected - 1][1] / 100 - 74, 160, 160);
  const low = selected && W.pos[openWorld][selected - 1][1] > 55;
  place($("lm-panel"), 20, low ? 170 : 1566, 801, 259);
}
export function renderMap(c) {
  const { $, P, lang } = c, x = T[lang()], w = worldInfo(openWorld);
  $("lm-world").innerHTML = `<span>${w.emoji}</span><div><b>${w[lang()]}</b><small>${x.progress(done(P, openWorld))}</small></div>`;
  const nodes = $("lm-nodes"); nodes.replaceChildren();
  let nextAssigned = false;
  for (let l = 1; l <= 10; l++) {
    const open = levelOpen(P, openWorld, l), st = P.levels[`${openWorld}:${l}`], hasRes = st !== undefined, isNext = open && !hasRes && !nextAssigned; if (isNext) nextAssigned = true;
    const n = document.createElement("button"); n.id = "lm-n" + l; n.className = "lm-node" + (!open ? " lock" : isNext ? " next" : "");
    n.innerHTML = open ? `<i class="stars">${[0, 1, 2].map((i) => `<s class="${i < (st || 0) ? "on" : ""}">★</s>`).join("")}</i><b>${l}</b>` : "🔒";
    n.onclick = () => { if (open) { selected = l; layoutMap(c); renderPanel(c); } };
    nodes.append(n);
  }
  layoutMap(c); renderPanel(c);
}
function renderPanel(c) {
  const { $, P, lang } = c, x = T[lang()], p = $("lm-panel"), ring = $("lm-ring");
  if (!selected) { p.classList.add("hidden"); ring.classList.add("hidden"); return; }
  p.classList.remove("hidden"); ring.classList.remove("hidden");
  const stage = W.stages[Math.min(selected, 10) - 1][lang()];
  p.innerHTML = `<div class="lp-t"><b>${x.stage(selected)}</b><small>${stage}</small><em>${x.sub}</em></div>`;
  const b = document.createElement("button"); b.className = "pill orange small lp-play"; b.innerHTML = `<span>${x.play}</span>`;
  b.onclick = () => c.playLevel(openWorld, selected);
  p.append(b);
}
export const currentWorld = () => openWorld;
