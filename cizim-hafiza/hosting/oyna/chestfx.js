// The chest scenes drawn on canvas, ported from the app: ChestBackdrop.kt (the stage behind a chest in its window),
// ChestWonDialog.kt (the full-screen "you won a chest" scene) and the reward half of ChestComponents.kt's opening scene.
// Every scene is one requestAnimationFrame loop; call the returned function to stop it.
export const TIER = {
  SILVER: { n: 0, accent: "#E08A2E", border: "#D7B48A", on: "#3A2416", art: "chest_apprentice", video: "chest_open_apprentice", stars: 1, rarity: "rarity_common", label: "chest_tier_silver", ms: 2 * 3600e3 },
  GOLD: { n: 1, accent: "#E5A800", border: "#E5A800", on: "#4A2F00", art: "chest_artist", video: "chest_open_artist", stars: 2, rarity: "rarity_rare", label: "chest_tier_gold", ms: 3 * 3600e3 },
  RARE: { n: 2, accent: "#8E4FE0", border: "#E070FF", on: "#FFFFFF", art: "chest_surprise", video: "chest_open_surprise", stars: 3, rarity: "rarity_legendary", label: "chest_tier_rare", ms: 6 * 3600e3 },
};
const hex = (c) => [parseInt(c.slice(1, 3), 16), parseInt(c.slice(3, 5), 16), parseInt(c.slice(5, 7), 16)];
export const rgba = (c, a) => { const [r, g, b] = hex(c); return `rgba(${r},${g},${b},${Math.max(0, Math.min(1, a))})`; };
const lerpC = (a, b, t) => { const A = hex(a), B = hex(b); return "#" + A.map((x, i) => Math.round(x + (B[i] - x) * t).toString(16).padStart(2, "0")).join(""); };
/** A much lighter partner of the accent: the glow behind the chest (ChestTier.glow). */
export const glowOf = (tier) => lerpC(TIER[tier].accent, "#FFFFFF", 0.72);
const ease = (x) => x * x * (3 - 2 * x);
const rng = (seed) => { let a = seed | 0; return () => { a = (a + 0x6d2b79f5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; }; };

function sparkle(g, x, y, r, color) {
  g.fillStyle = color; g.beginPath(); g.moveTo(x, y - r);
  g.quadraticCurveTo(x, y, x + r, y); g.quadraticCurveTo(x, y, x, y + r); g.quadraticCurveTo(x, y, x - r, y); g.quadraticCurveTo(x, y, x, y - r); g.fill();
}
function rays(g, cx, cy, count, half, len, from, to, spin) {
  g.save(); g.translate(cx, cy); g.rotate(spin * Math.PI / 180);
  const grad = g.createRadialGradient(0, 0, 0, 0, 0, len); grad.addColorStop(0, from); grad.addColorStop(1, to);
  for (let i = 0; i < count; i++) {
    const a = i * 2 * Math.PI / count;
    g.beginPath(); g.moveTo(0, 0); g.lineTo(Math.cos(a - half) * len, Math.sin(a - half) * len); g.lineTo(Math.cos(a + half) * len, Math.sin(a + half) * len); g.closePath(); g.fillStyle = grad; g.fill();
  }
  g.restore();
}
function disc(g, cx, cy, r, stops) {
  const grad = g.createRadialGradient(cx, cy, 0, cx, cy, r); stops.forEach(([o, c]) => grad.addColorStop(o, c)); g.fillStyle = grad; g.beginPath(); g.arc(cx, cy, r, 0, 7); g.fill();
}
const sizeCanvas = (cv) => { const r = cv.getBoundingClientRect(), d = Math.min(2, window.devicePixelRatio || 1); cv.width = Math.max(2, Math.round(r.width * d)); cv.height = Math.max(2, Math.round(r.height * d)); return { w: cv.width, h: cv.height, d }; };
// One frame loop per scene. `fps` caps the rate (the small slot backdrops run at 24 — plenty for a soft glow and much lighter on a phone);
// a loop also ends by itself when its canvas leaves the page and sleeps while the tab is hidden.
function loop(draw, fps = 60, cv = null) {
  let on = true, t0 = performance.now(), last = 0; const gap = 1000 / fps - 3;
  const f = (now) => { if (!on) return; if (cv && !cv.isConnected) { on = false; return; } if (!document.hidden && now - last >= gap) { last = now; draw(now - t0); } requestAnimationFrame(f); };
  requestAnimationFrame(f); return () => { on = false; };
}

/** The window stage's backdrop for a tier (ChestBackdrop). */
export function backdrop(cv, tier, fps = 60) {
  const g = cv.getContext("2d"), { w, h } = sizeCanvas(cv), n = TIER[tier].n, r = rng(n * 97 + 3), stars = Array.from({ length: tier === "RARE" ? 16 : 7 }, () => ({ x: r(), y: r(), s: 0.5 + r() * 0.9, o: r() }));
  const gc = [w / 2, h * 0.38];
  const vg = (stops) => { const gr = g.createLinearGradient(0, 0, 0, h); stops.forEach(([o, c]) => gr.addColorStop(o, c)); g.fillStyle = gr; g.fillRect(0, 0, w, h); };
  return loop((t) => {
    const sweep = (t / 5000) % 1, spin = ((t / 16000) % 1) * 360, p = (t / 1500) % 2, pulse = ease(p < 1 ? p : 2 - p);
    g.clearRect(0, 0, w, h);
    if (tier === "SILVER") {
      vg([[0, "#FFE9C4"], [1, "#E9B97A"]]);
      rays(g, gc[0], gc[1], 8, Math.PI / 8 * 0.4, h * 1.2, "rgba(255,255,255,.34)", "rgba(255,255,255,0)", spin);
      disc(g, w / 2, h / 2, Math.max(w, h) * 0.75, [[0, "rgba(122,74,18,0)"], [1, "rgba(122,74,18,.18)"]]);
      const drift = spin / 360;
      stars.slice(0, 6).forEach((s) => { const ph = (drift + s.o) % 1, env = Math.sin(ph * Math.PI); g.fillStyle = `rgba(255,246,221,${0.85 * env})`; g.beginPath(); g.arc(s.x * w, h * (1.02 - 1.04 * ph), w * 0.016 * s.s, 0, 7); g.fill(); });
      const breathe = 0.4 + 0.25 * pulse;
      disc(g, gc[0], gc[1], w * 0.62, [[0, `rgba(255,255,255,${Math.min(1, breathe + 0.15)})`], [1, "rgba(255,255,255,0)"]]);
      stars.slice(0, 4).forEach((s) => sparkle(g, s.x * w, s.y * h * 0.9, w * 0.04 * s.s, `rgba(255,255,255,${Math.min(1, 0.55 * Math.abs(Math.sin((s.o + sweep) * Math.PI * 2)))})`));
    } else if (tier === "GOLD") {
      vg([[0, "#FFF5C9"], [0.5, "#FFD25E"], [1, "#FFB92E"]]);
      const glow = 0.85 + 0.15 * pulse;
      disc(g, gc[0], gc[1], w * (0.8 + 0.08 * pulse), [[0, `rgba(255,255,255,${glow})`], [0.5, "rgba(255,243,176,.8)"], [1, "rgba(255,243,176,0)"]]);
      for (const shift of [0, 0.5]) {
        const ph = (sweep + shift) % 1, env = Math.sin(ph * Math.PI), bx = (-0.2 + 1.4 * ph) * w;
        const gr = g.createLinearGradient(bx - w * 0.22, 0, bx + w * 0.22, h * 0.5); gr.addColorStop(0, "rgba(255,255,255,0)"); gr.addColorStop(0.5, `rgba(255,255,255,${0.7 * env})`); gr.addColorStop(1, "rgba(255,255,255,0)"); g.fillStyle = gr; g.fillRect(0, 0, w, h);
      }
      stars.forEach((s) => sparkle(g, s.x * w, s.y * h, w * 0.05 * s.s, `rgba(255,255,255,${Math.min(1, 0.35 + 0.65 * Math.abs(Math.sin((s.o + sweep) * Math.PI * 2)))})`));
    } else {
      vg([[0, "#240B4A"], [0.5, "#5B27B8"], [1, "#B245D6"]]);
      rays(g, gc[0], gc[1], 10, Math.PI / 10 * 0.32, h * 1.3, "rgba(255,214,255,.32)", "rgba(255,214,255,0)", spin);
      disc(g, gc[0], gc[1], w * (0.6 + 0.1 * pulse), [[0, `rgba(255,232,255,${0.55 + 0.3 * pulse})`], [1, "rgba(192,107,255,0)"]]);
      stars.forEach((s) => sparkle(g, s.x * w, s.y * h, w * 0.05 * s.s, `rgba(255,255,255,${Math.min(1, 0.25 + 0.75 * Math.abs(Math.sin((s.o + sweep * 2) * Math.PI)))})`));
    }
  }, fps, cv);
}

/** The full-screen "you won a chest" scene (ChestWonScene): the chest drops, squashes, floats; rays, confetti and twinkles. `host` is the overlay. */
export function wonScene(cv, chestImg, tier, onLanded) {
  const g = cv.getContext("2d"), { w, h } = sizeCanvas(cv), A = TIER[tier].accent, light = glowOf(tier), legendary = tier === "RARE";
  const palette = [A, light, "#FFD84D", "#FFFFFF", "#FF7A59", "#5BD6FF", "#B388FF"], r = rng(TIER[tier].n * 31 + 7);
  const conf = Array.from({ length: legendary ? 90 : 64 }, () => ({ x: r(), start: r(), speed: 0.22 + r() * 0.45, sway: 0.015 + r() * 0.03, rate: 1.2 + r() * 2.2, spin: 120 + r() * 420, size: 6 + r() * 8, color: Math.floor(r() * palette.length) }));
  const r2 = rng(TIER[tier].n * 53 + 11), tw = Array.from({ length: 28 }, () => ({ x: r2(), y: r2(), s: 0.5 + r2(), o: r2() }));
  const U = w / 411; // canvas pixels per dp of the app
  const cy = h * 0.34, t0 = performance.now(); let landedAt = null, fired = false;
  const stop = loop((now) => {
    const t = (now) / 1000, fall = Math.min(1, now / 650), fe = fall * fall;
    if (fall >= 1 && landedAt === null) { landedAt = now; if (!fired) { fired = true; onLanded && onLanded(); } }
    const landed = landedAt === null ? 0 : Math.min(1, (now - landedAt) / 1100), L = 1 - Math.pow(1 - landed, 2);
    const settle = landedAt === null ? 1 : Math.min(1, (now - landedAt) / 900), sq = landedAt === null ? 1 : 1 - Math.exp(-5 * settle) * Math.cos(settle * 11);
    g.clearRect(0, 0, w, h);
    const bg = g.createRadialGradient(w / 2, cy, 0, w / 2, cy, h * 0.9); bg.addColorStop(0, rgba(A, 0.55)); bg.addColorStop(0.5, "#241046"); bg.addColorStop(1, "#0B0616"); g.fillStyle = bg; g.fillRect(0, 0, w, h);
    const rayAlpha = (0.16 + 0.2 * (legendary ? 1 : 0.7)) * L;
    rays(g, w / 2, cy, legendary ? 20 : 14, Math.PI / (legendary ? 20 : 14) * 0.36, h * 1.2, rgba(light, rayAlpha), rgba(light, 0), t * 14);
    const gp = 0.85 + 0.25 * ease((t / 1.1) % 2 < 1 ? (t / 1.1) % 2 : 2 - (t / 1.1) % 2), gr = (150 + 90 * L) * U * gp;
    disc(g, w / 2, cy, gr, [[0, rgba(light, 0.95 * L)], [0.5, rgba(A, 0.35 * L)], [1, rgba(A, 0)]]);
    if (landed > 0 && landed < 1) for (let k = 0; k < 2; k++) { const p = Math.max(0, Math.min(1, (landed - k * 0.18) / (1 - k * 0.18))); if (p > 0) { g.strokeStyle = `rgba(255,255,255,${(1 - p) * 0.7})`; g.lineWidth = ((1 - p) * 14 + 2) * U; g.beginPath(); g.arc(w / 2, cy + 90 * U, (40 + 520 * p) * U, 0, 7); g.stroke(); } }
    conf.forEach((c) => { const prog = ((c.start + t * c.speed) % 1.2) - 0.1, x = (c.x + Math.sin(t * c.rate + c.start * 6) * c.sway) * w, y = prog * h, s = c.size * U; g.save(); g.translate(x, y); g.rotate((t * c.spin + c.start * 360) * Math.PI / 180); g.fillStyle = rgba(palette[c.color], L * 0.95); g.fillRect(-s / 2, -s * 0.85, s, s * 1.7); g.restore(); });
    tw.forEach((q) => sparkle(g, q.x * w, q.y * h, 9 * U * q.s, `rgba(255,255,255,${(0.2 + 0.8 * Math.abs(Math.sin((q.o * 3 + t * 0.9) * Math.PI))) * L})`));
    const flash = Math.max(0, 1 - landed / 0.16); if (landed > 0 && flash > 0) { g.fillStyle = `rgba(255,255,255,${flash * 0.85})`; g.fillRect(0, 0, w, h); }
    // the chest itself
    const size = 390 * U, drop = 1 - fe, bob = Math.sin(t * 2.1) * 6 * U * sq; // the app's chest scene: a bigger chest, 30 dp higher
    g.save(); g.translate(w / 2, cy - 30 * U + (-drop * (cy + 260 * U)) + bob); g.rotate(drop * -14 * Math.PI / 180);
    const sx = 1.22 - 0.22 * sq, sy = 0.72 + 0.28 * sq; g.translate(0, size * 0.35); g.scale(sx, sy); g.translate(0, -size * 0.35);
    if (chestImg.complete && chestImg.naturalWidth) { const iw = size, ih = size * chestImg.naturalHeight / chestImg.naturalWidth; g.drawImage(chestImg, -iw / 2, -ih / 2, iw, ih); }
    g.restore();
  });
  return stop;
}

/** The light rays, rings and confetti of the reward half of the opening scene (ChestOpeningScene, stage >= 1). */
export function rewardScene(cv, tier, gold) {
  const g = cv.getContext("2d"), { w, h } = sizeCanvas(cv), A = TIER[tier].accent, light = glowOf(tier), U = w / 411;
  const palette = [A, light, "#FFC928", "#FF7A59", "#5BD6FF", "#7BD88F"], r = rng(gold * 31 + TIER[tier].n);
  const ps = Array.from({ length: 56 }, (_, i) => ({ a: r() * Math.PI * 2, sp: 260 + r() * 620, size: 6 + r() * 10, spin: (r() - 0.5) * 900, c: Math.floor(r() * palette.length), coin: i % 4 === 0 }));
  const c0 = [w / 2, h * 0.36];
  return loop((now) => {
    const burst = Math.min(1, now / 1300), b = 1 - Math.pow(1 - burst, 3), spin = ((now / 16000) % 1) * 360;
    g.clearRect(0, 0, w, h);
    disc(g, c0[0], c0[1], 340 * U, [[0, rgba(light, 0.75)], [0.5, rgba(light, 0.22)], [1, rgba(light, 0)]]);
    rays(g, c0[0], c0[1], 16, Math.PI / 16 * 0.28, Math.max(w, h), rgba(A, 0.16), rgba(A, 0), spin);
    g.strokeStyle = rgba(A, (1 - b) * 0.55); g.lineWidth = (14 * (1 - b) + 2) * U; g.beginPath(); g.arc(c0[0], c0[1], (50 + 560 * b) * U, 0, 7); g.stroke();
    const tt = b * 1.5;
    ps.forEach((p) => {
      const x = c0[0] + Math.cos(p.a) * p.sp * tt * U, y = c0[1] + Math.sin(p.a) * p.sp * tt * U + 0.5 * 900 * tt * tt * U, al = Math.max(0, 1 - b * b), s = p.size * U;
      if (p.coin) { g.fillStyle = `rgba(255,201,40,${al})`; g.beginPath(); g.arc(x, y, s, 0, 7); g.fill(); g.fillStyle = `rgba(255,240,160,${al})`; g.beginPath(); g.arc(x, y, s * 0.6, 0, 7); g.fill(); }
      else { g.save(); g.translate(x, y); g.rotate(p.spin * tt * Math.PI / 180); g.fillStyle = rgba(palette[p.c], al); g.fillRect(-s / 2, -s / 2, s, s * 1.7); g.restore(); }
    });
  });
}
