// Premium pen effects (Android's PenBrush.kt, simplified): a glow under the stroke and animated sparkles along it.
const hash = (n) => { const x = Math.sin(n * 127.1 + 311.7) * 43758.5453; return x - Math.floor(x); };
const hex = (s) => [1, 3, 5].map((k) => parseInt(s.slice(k, k + 2), 16));
function mix(cols, pos) {
  const n = cols.length, t = ((pos % 1) + 1) % 1 * n, i = Math.floor(t) % n, f = t - Math.floor(t), a = hex(cols[i]), b = hex(cols[(i + 1) % n]);
  return [0, 1, 2].map((k) => Math.round(a[k] + (b[k] - a[k]) * f));
}
const rgb = (c, a = 1) => `rgba(${c[0]},${c[1]},${c[2]},${a})`;
function star(ctx, x, y, r, color) {
  ctx.fillStyle = color; ctx.beginPath();
  for (let i = 0; i < 8; i++) { const rr = i % 2 ? r * 0.28 : r, ang = i * Math.PI / 4; ctx.lineTo(x + Math.cos(ang) * rr, y + Math.sin(ang) * rr); }
  ctx.closePath(); ctx.fill();
}
// pts: one stroke as device-pixel points [{x,y}]. dist0 continues colour travel across strokes.
export function drawFxStroke(ctx, pts, pen, w, t, d, dist0 = 0) {
  const cols = pen.colors, fx = pen.effect, n = pts.length;
  if (!n) return dist0;
  // resample every ~10px
  const S = 10 * d, samples = []; let acc = 0, dist = dist0;
  samples.push({ ...pts[0], dist });
  for (let i = 1; i < n; i++) {
    const dx = pts[i].x - pts[i - 1].x, dy = pts[i].y - pts[i - 1].y, seg = Math.hypot(dx, dy); dist += seg; acc += seg;
    if (acc >= S) { samples.push({ x: pts[i].x, y: pts[i].y, dist }); acc = 0; }
  }
  const last = pts[n - 1]; if (samples[samples.length - 1].x !== last.x || samples[samples.length - 1].y !== last.y) samples.push({ x: last.x, y: last.y, dist });
  const flowing = fx === "FLOW" || fx === "AURORA";
  const colorAt = (dd) => { const shift = flowing ? t * 90 : 0; return mix(cols, (dd - shift) / (cols.length > 3 ? 700 : 450)); };
  ctx.lineCap = ctx.lineJoin = "round";
  // glow, rim, core
  for (const [mul, alpha, dark] of [[2.2, 0.2, false], [1.15, 1, true], [1, 1, false]]) {
    ctx.lineWidth = w * mul;
    for (let i = 1; i < samples.length; i++) {
      const c = colorAt(samples[i].dist), col = dark ? c.map((v) => Math.round(v * 0.55)) : c;
      ctx.strokeStyle = rgb(col, alpha); ctx.beginPath(); ctx.moveTo(samples[i - 1].x, samples[i - 1].y); ctx.lineTo(samples[i].x, samples[i].y); ctx.stroke();
    }
    if (samples.length === 1) { ctx.fillStyle = rgb(colorAt(samples[0].dist), alpha); ctx.beginPath(); ctx.arc(samples[0].x, samples[0].y, w * mul / 2, 0, 7); ctx.fill(); }
  }
  // shine
  ctx.lineWidth = w * 0.28; ctx.strokeStyle = "rgba(255,255,255,.35)";
  for (let i = 1; i < samples.length; i++) { ctx.beginPath(); ctx.moveTo(samples[i - 1].x, samples[i - 1].y - w * 0.18); ctx.lineTo(samples[i].x, samples[i].y - w * 0.18); ctx.stroke(); }
  // effect overlays
  samples.forEach((s, i) => {
    const h = hash(Math.round(s.dist / 10) * 7 + 3), h2 = hash(Math.round(s.dist / 10) * 13 + 5), c = colorAt(s.dist);
    const tw = Math.sin(t * 3 + h * 6.28) * 0.5 + 0.5;
    if (fx === "GLINT" || fx === "ROYAL") { if (tw > 0.8) star(ctx, s.x + (h - 0.5) * w, s.y + (h2 - 0.5) * w, w * (0.5 + tw * 0.5), "rgba(255,250,210,.95)"); }
    else if (fx === "EMBERS") {
      if (h > 0.35) { const rise = ((t * 38 + h * 30) % 26) * d, a = 1 - rise / (26 * d); ctx.strokeStyle = h2 > 0.5 ? `rgba(255,190,60,${a})` : `rgba(255,110,30,${a})`; ctx.lineWidth = w * 0.16; ctx.beginPath(); ctx.moveTo(s.x + (h2 - 0.5) * w, s.y - rise - w * 0.4); ctx.lineTo(s.x + (h2 - 0.5) * w, s.y - rise - w * 0.4 - w * 0.5); ctx.stroke(); }
    } else if (fx === "BOLTS") {
      if (h > 0.75 && Math.sin(t * 5 + h * 40) > -0.2) { const len = w * (1.15 + 0.75 * h2), bx = s.x, by = s.y - w * 0.4; ctx.strokeStyle = "rgba(220,240,255,.95)"; ctx.lineWidth = Math.max(1.5 * d, w * 0.11); ctx.beginPath(); ctx.moveTo(bx, by); ctx.lineTo(bx + len * 0.3, by - len * 0.4); ctx.lineTo(bx - len * 0.1, by - len * 0.5); ctx.lineTo(bx + len * 0.35, by - len); ctx.stroke(); }
    } else if (fx === "STARS") { if (tw > 0.55) star(ctx, s.x + (h - 0.5) * w * 1.4, s.y + (h2 - 0.5) * w * 1.4, w * 0.45 * tw, i % 2 ? "rgba(255,255,255,.95)" : rgb(mix(cols.slice(1), h), 0.95)); }
    else if (fx === "SMOKE") { const rise = ((t * 18 + h * 40) % 40) * d, a = 0.14 * (1 - rise / (40 * d)); ctx.fillStyle = rgb(c, a); ctx.beginPath(); ctx.arc(s.x + (h2 - 0.5) * w, s.y - rise, w * (0.8 + h * 0.8), 0, 7); ctx.fill(); }
    else if (fx === "AURORA") { const wave = Math.sin(s.dist / 60 - t * 2) * 0.5 + 0.5; ctx.fillStyle = `rgba(255,255,255,${0.18 * wave})`; ctx.beginPath(); ctx.arc(s.x, s.y, w * 0.7, 0, 7); ctx.fill(); }
  });
  return samples[samples.length - 1].dist;
}
