// Worlds list and level maps. Same rules as LevelCatalog / LevelMapViewModel: a level opens when the one before it has a
// result, a world opens when the previous world has ten results, stars come from accuracy.
let W = null;
let openWorld = 1, selected = null;
const T = {
  tr: { worlds: "Dünyalar", progress: (n) => `${n}/10 tamamlandı`, stage: (n) => `Aşama ${n}`, sub: "Çiz, tahmin et ve yıldızları topla!", play: "OYNA", locked: "Kilitli" },
  en: { worlds: "Worlds", progress: (n) => `${n}/10 completed`, stage: (n) => `Stage ${n}`, sub: "Draw, guess and collect the stars!", play: "PLAY", locked: "Locked" },
  pt: { worlds: "Mundos", progress: (n) => `${n}/10 concluídas`, stage: (n) => `Fase ${n}`, sub: "Desenhe, adivinhe e colete as estrelas!", play: "JOGAR", locked: "Bloqueado" },
  es: { worlds: "Mundos", progress: (n) => `${n}/10 completados`, stage: (n) => `Nivel ${n}`, sub: "¡Dibuja, adivina y junta las estrellas!", play: "JUGAR", locked: "Bloqueado" },
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
  const l = $("wl-list"); l.style.left = 0; l.style.top = 440 * u + "px"; l.style.width = 841 * u + "px"; l.style.height = c.fillH() - 440 * u + "px";
}
export function renderWorlds(c) {
  const { $, P, lang } = c, x = T[lang()], list = $("wl-list");
  list.replaceChildren();
  W.worlds.forEach((w) => {
    const open = worldOpen(P, w.id), b = document.createElement("button");
    b.className = "wl-banner" + (open ? "" : " lock");
    b.innerHTML = `<img src="img/world_banner_${w.id}.webp" alt=""><span class="wl-tag">${open ? `${x.progress(done(P, w.id))} · ⭐${stars(P, w.id)}` : '<svg viewBox="0 0 24 24"><path d="M7 11V8a5 5 0 0110 0v3" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round"/><rect x="4.5" y="10.5" width="15" height="11" rx="2.6" fill="#fff"/><circle cx="12" cy="15.3" r="1.6" fill="#2b323d"/><rect x="11.2" y="15.5" width="1.6" height="3.4" rx=".6" fill="#2b323d"/></svg>'}</span>`;
    b.onclick = () => { if (open) { openWorld = w.id; selected = null; tapped = null; scrollFor = null; stopCountdown(); c.show("lvmap"); } };
    list.append(b);
  });
}
const stars = (P, w) => Object.entries(P.levels).filter(([k]) => k.startsWith(w + ":")).reduce((s, [, v]) => s + v, 0);

// LevelMapScreen.kt: the artwork is drawn 1.1x taller than the window and the page scrolls to where the player left off.
const BOTTOM = { 1: "#7E7033", 2: "#677269", 3: "#64642C", 4: "#82764D", 5: "#60492B", 6: "#7A653A", 7: "#7F6F3B", 8: "#58562C", 9: "#605826" };
const ART_W = 841, ART_H = 1870, STRETCH = 1.1, DP = 2.05, COIN_BOX = 72 * DP, PANEL_MARGIN = 12 * DP;
const PLAY_SECONDS = 3;
let tapped = null, countdownTimer = null, scrollFor = null;
const stopCountdown = () => { clearInterval(countdownTimer); countdownTimer = null; };
const stateOf = (c) => {
  const { P } = c; let nextAssigned = false;
  return Array.from({ length: 10 }, (_, i) => {
    const l = i + 1, unlocked = levelOpen(P, openWorld, l), st = P.levels[`${openWorld}:${l}`], isNext = unlocked && st === undefined && !nextAssigned;
    if (isNext) nextAssigned = true;
    return { l, unlocked, stars: st || 0, isNext };
  });
};
const currentOf = (lv) => tapped ?? (lv.find((x) => x.isNext) || [...lv].reverse().find((x) => x.unlocked) || {}).l ?? null;
export function layoutMap(c) {
  const { place, $ } = c, u = c.u();
  place($("lm-back"), 20, 40, 110, 106);
  const w = $("lm-world"); w.style.top = 26 * u + "px"; w.style.right = 42 * u + "px"; w.style.maxWidth = (841 - 150 - 42) * u + "px";
  const art = ART_H * STRETCH, panelH = (841 - PANEL_MARGIN * 2) * (350 / 1080), mapH = art + panelH + 72 * DP;
  $("lm-map").style.height = mapH * u + "px"; $("lm-bg").src = `img/bg_world_${openWorld}.webp`;
  Object.assign($("lm-bg").style, { width: ART_W * STRETCH * u + "px", height: art * u + "px", left: "50%", marginLeft: -ART_W * STRETCH * u / 2 + "px" });
  const fog = $("lm-fog"), edge = BOTTOM[openWorld] || "#605826", fogH = mapH - art + 140;
  fog.style.height = fogH * u + "px"; fog.style.background = `linear-gradient(transparent 0, ${edge} ${140 / fogH * 100}%, color-mix(in srgb, ${edge} 45%, #0c1a0b) 100%)`;
  $("lm-scroll").style.background = "#2F5D2B";
  const pos = (i) => [841 / 2 + ART_W * STRETCH * (W.pos[openWorld][i][0] / 100 - 0.5), art * W.pos[openWorld][i][1] / 100];
  W.pos[openWorld].forEach((_, i) => { const n = $("lm-n" + (i + 1)); if (!n) return; const [x, y] = pos(i); place(n, x - COIN_BOX / 2, y - COIN_BOX / 2, COIN_BOX, COIN_BOX); });
  const cur = currentOf(stateOf(c)), ring = $("lm-ring");
  if (cur) { const [x, y] = pos(cur - 1), s = 80 * DP; place(ring, x - s / 2, y - s / 2 + 6 * DP, s, s); }
  const pn = $("lm-panel"); place(pn, PANEL_MARGIN, 0, 841 - PANEL_MARGIN * 2, panelH); pn.style.top = "auto"; pn.style.bottom = 10 * DP * u + "px";
  return { art, pos, cur };
}
export function renderMap(c) {
  const { $, lang } = c, x = T[lang()], w = worldInfo(openWorld), lv = stateOf(c);
  const cleared = lv.filter((n) => n.stars > 0).length;
  $("lm-world").innerHTML = `<span>${w.emoji}</span><div><b>${w[lang()]}</b><small>${x.progress(cleared)}</small></div>`;
  const nodes = $("lm-nodes"); nodes.replaceChildren(); const cur = currentOf(lv);
  const lock = '<svg viewBox="0 0 24 24"><path d="M7 11V8a5 5 0 0110 0v3" fill="none" stroke="#cbd3df" stroke-width="3"/><rect x="3" y="10.5" width="18" height="12" rx="3.2" fill="#a9b4c4" stroke="#3b4452" stroke-width="1.2"/><circle cx="12" cy="15.5" r="2" fill="#2b323d"/><rect x="11" y="16" width="2" height="4" rx=".7" fill="#2b323d"/></svg>';
  lv.forEach((n) => {
    const b = document.createElement("button"); b.id = "lm-n" + n.l; b.className = "lm-node" + (!n.unlocked ? " lock" : n.isNext ? " next" : "") + (n.l === cur ? " open" : "");
    const star = (i) => `<s class="${i < n.stars ? "on" : ""}" style="font-size:calc(var(--u)*${i === 1 ? 41 : 35})">${i < n.stars ? "★" : "☆"}</s>`;
    const stars = n.unlocked && (n.stars > 0 || n.isNext) ? `<i class="stars">${[0, 1, 2].map(star).join("")}</i>` : "";
    b.innerHTML = `${n.isNext ? '<em class="glow"></em>' : ""}${stars}<span class="coin">${n.unlocked ? `<b>${n.l}</b>` : lock}</span>${n.unlocked ? "" : `<u class="plate">${n.l}</u>`}`;
    b.onclick = () => { if (n.unlocked) { stopCountdown(); tapped = n.l; renderMap(c); } };
    nodes.append(b);
  });
  const g = layoutMap(c); renderPanel(c, lv, cur);
  // bring the open stop into the upper part of the window once per world/stop (700ms ease, like animateScrollTo)
  const sc = $("lm-scroll"), key = openWorld + ":" + cur;
  if (cur && scrollFor !== key) { scrollFor = key; smoothScroll(sc, Math.max(0, g.pos(cur - 1)[1] * c.u() - sc.clientHeight * 0.38)); }
}
function smoothScroll(el, to) {
  const from = el.scrollTop, t0 = performance.now(), D = 700, ease = (t) => 1 - Math.pow(1 - t, 3);
  const f = (now) => { const k = Math.min(1, (now - t0) / D); el.scrollTop = from + (to - from) * ease(k); if (k < 1) requestAnimationFrame(f); };
  requestAnimationFrame(f);
}
function renderPanel(c, lv, cur) {
  const { $, lang } = c, x = T[lang()], p = $("lm-panel"), n = lv.find((q) => q.l === cur);
  if (!n) { p.classList.add("hidden"); return; }
  p.classList.remove("hidden");
  p.innerHTML = `<img class="lp-ic" src="img/world_icon_${openWorld}.webp" alt=""><div class="lp-t"><b>${x.stage(n.l)}</b><em>${x.sub}</em><i>${[0, 1, 2].map((i) => `<s class="${i < n.stars ? "on" : ""}">★</s>`).join("")}</i></div>`;
  const b = document.createElement("button"); b.className = "lp-play"; b.innerHTML = `<span>${x.play}</span>`;
  b.onclick = () => {
    if (countdownTimer) return; let s = PLAY_SECONDS; b.querySelector("span").textContent = s; b.classList.add("cd");
    countdownTimer = setInterval(() => { if (c.current() !== "lvmap") return stopCountdown(); s--; if (s <= 0) { stopCountdown(); c.playLevel(openWorld, n.l); } else b.querySelector("span").textContent = s; }, 1000);
  };
  p.append(b);
}
export const currentWorld = () => openWorld;
