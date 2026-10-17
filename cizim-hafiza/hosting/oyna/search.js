// "Rakip aranıyor…" (SearchingScene.kt): a sheet on the table where a pencil keeps drawing doodles — star, heart, house, sun — in turn.
const DOODLE_MS = 2600, DRAW_SHARE = 0.72, FADE_FROM = 0.86;
const ring = (cx, cy, r, n = 40) => Array.from({ length: n + 1 }, (_, i) => { const a = (2 * Math.PI * i) / n; return [cx + r * Math.cos(a), cy + r * Math.sin(a)]; });
const D = [
  { c: "#F2A100", s: [Array.from({ length: 11 }, (_, i) => { const a = -Math.PI / 2 + (i * Math.PI) / 5, r = i % 2 === 0 ? 0.4 : 0.17; return [0.5 + r * Math.cos(a), 0.53 + r * Math.sin(a)]; })] },
  { c: "#E5483C", s: [Array.from({ length: 49 }, (_, i) => { const t = (2 * Math.PI * i) / 48, x = 16 * Math.sin(t) ** 3, y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t); return [0.5 + x / 40, 0.5 - y / 40]; })] },
  { c: "#2E86D6", s: [[[0.24, 0.46], [0.24, 0.82], [0.76, 0.82], [0.76, 0.46]], [[0.18, 0.5], [0.5, 0.18], [0.82, 0.5]], [[0.44, 0.82], [0.44, 0.62], [0.56, 0.62], [0.56, 0.82]]] },
  { c: "#F26A1B", s: [ring(0.5, 0.52, 0.17), ...Array.from({ length: 8 }, (_, k) => { const a = (k * Math.PI) / 4; return [[0.5 + 0.25 * Math.cos(a), 0.52 + 0.25 * Math.sin(a)], [0.5 + 0.36 * Math.cos(a), 0.52 + 0.36 * Math.sin(a)]]; })] },
];
D.forEach((d) => { d.len = d.s.map((p) => p.reduce((t, q, i) => (i ? t + Math.hypot(q[0] - p[i - 1][0], q[1] - p[i - 1][1]) : 0), 0)); d.total = d.len.reduce((a, b) => a + b, 0); });

function pencil(g, x, y, L, alpha) {
  const w = L * 0.16, cone = L * 0.2;
  g.save(); g.globalAlpha = alpha; g.translate(x, y); g.rotate((-38 * Math.PI) / 180);
  const top = -L, bot = -cone;
  g.fillStyle = "#FFC83D"; g.fillRect(-w / 2, top + L * 0.14, w, bot - top - L * 0.14);
  g.fillStyle = "#E59A12"; g.fillRect(w / 6, top + L * 0.14, w / 3, bot - top - L * 0.14);
  g.fillStyle = "#B9BEC6"; g.fillRect(-w / 2, top + L * 0.07, w, L * 0.07);
  g.fillStyle = "#F48FB1"; g.beginPath(); g.roundRect(-w / 2, top, w, L * 0.08, w * 0.3); g.fill();
  g.fillStyle = "#F5D3A0"; g.beginPath(); g.moveTo(-w / 2, bot); g.lineTo(w / 2, bot); g.lineTo(0, 0); g.fill();
  g.fillStyle = "#3B2314"; g.beginPath(); g.moveTo(-w / 6, -cone / 3); g.lineTo(w / 6, -cone / 3); g.lineTo(0, 0); g.fill();
  g.strokeStyle = "#5A2815"; g.globalAlpha = alpha * 0.8; g.lineWidth = w * 0.08; g.lineJoin = "round";
  g.beginPath(); g.moveTo(0, 0); g.lineTo(-w / 2, bot); g.lineTo(-w / 2, top + L * 0.04); g.lineTo(w / 2, top + L * 0.04); g.lineTo(w / 2, bot); g.closePath(); g.stroke();
  g.restore();
}
function frame(cv, t) {
  const g = cv.getContext("2d"), W = cv.width, H = cv.height; g.clearRect(0, 0, W, H);
  g.strokeStyle = "rgba(160,120,74,.13)"; g.lineWidth = H * 0.004;
  for (let y = H * 0.16; y < H * 0.95; y += H * 0.12) { g.beginPath(); g.moveTo(W * 0.05, y); g.lineTo(W * 0.95, y); g.stroke(); }
  const c = (t % (DOODLE_MS * D.length)) / DOODLE_MS, i = Math.min(D.length - 1, Math.floor(c)), local = c - i;
  const drawn = Math.min(1, local / DRAW_SHARE), fade = local > FADE_FROM ? 1 - (local - FADE_FROM) / (1 - FADE_FROM) : 1, d = D[i];
  const side = Math.min(W, H) * 0.86, left = (W - side) / 2, top = (H - side) / 2, map = (p) => [left + p[0] * side, top + p[1] * side];
  let budget = d.total * drawn, tip = map(d.s[0][0]);
  g.save(); g.globalAlpha = fade; g.strokeStyle = d.c; g.lineWidth = side * 0.035; g.lineCap = "round"; g.lineJoin = "round";
  outer: for (const pts of d.s) {
    if (budget <= 0) break;
    g.beginPath(); let m = map(pts[0]); g.moveTo(m[0], m[1]); tip = m;
    for (let k = 1; k < pts.length; k++) {
      const a = pts[k - 1], b = pts[k], seg = Math.hypot(b[0] - a[0], b[1] - a[1]);
      if (budget >= seg) { budget -= seg; m = map(b); g.lineTo(m[0], m[1]); tip = m; }
      else { const f = seg ? budget / seg : 0; m = map([a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f]); g.lineTo(m[0], m[1]); tip = m; budget = 0; g.stroke(); break outer; }
    }
    g.stroke();
  }
  g.restore();
  if (drawn < 1 || local < FADE_FROM) pencil(g, tip[0], tip[1], Math.min(W, H) * 0.3, fade);
}
let raf = 0;
export function startSearch(c) {
  stopSearch();
  const cv = c.$("s-cv"), sc = document.getElementById("s-dots"), t0 = performance.now();
  const fit = () => { const r = cv.getBoundingClientRect(), k = Math.min(2, devicePixelRatio || 1); cv.width = Math.max(1, r.width * k); cv.height = Math.max(1, r.height * k); }; fit();
  const loop = (now) => {
    if (c.current() !== "search") return;
    const t = Math.max(0, now - t0); frame(cv, t);
    (sc ? sc.querySelectorAll("i") : []).forEach((e, k) => { e.style.opacity = ((t % 1600) / 400) >= k + 1 ? 1 : 0.18; });
    raf = requestAnimationFrame(loop);
  };
  raf = requestAnimationFrame(loop);
}
export function stopSearch() { cancelAnimationFrame(raf); raf = 0; }
