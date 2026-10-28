import { S } from "./i18n.js";
import { appWindow, penWindow } from "./ui.js";
import { penList, frameList, frameRing } from "./store.js";
import { childMode } from "./agegate.js";
const T = { tr: { pen: "Kalemini seç", frame: "Çerçeveni seç", close: "Kapat", nick: "Kullanıcı adı", save: "Kaydet", nickHint: "2-16 karakter. Adını yalnızca bir kez değiştirebilirsin.", nickLocked: "Kullanıcı adını zaten bir kez değiştirdin.", nick_taken: "Bu ad alınmış, başka bir tane dene.", nick_invalid: "Harf, rakam, boşluk, _ . - kullanabilirsin (2-16).", nick_net: "Sunucuya ulaşılamadı.", locked: (n) => `Seviye ${n}` }, en: { pen: "Choose your pen", frame: "Choose your frame", close: "Close", nick: "Username", save: "Save", nickHint: "2-16 characters. You can change your name only once.", nickLocked: "You have already changed your username once.", nick_taken: "That name is taken, try another.", nick_invalid: "Letters, numbers, spaces and _ . - only (2-16).", nick_net: "Couldn't reach the server.", locked: (n) => `Level ${n}` },
  pt: { pen: "Escolha seu lápis", frame: "Escolha sua moldura", close: "Fechar", nick: "Nome de usuário", save: "Salvar", nickHint: "2-16 caracteres. Você só pode mudar seu nome uma vez.", nickLocked: "Você já mudou seu nome de usuário uma vez.", nick_taken: "Esse nome já está em uso, tente outro.", nick_invalid: "Use letras, números, espaços e _ . - (2-16).", nick_net: "Não foi possível falar com o servidor.", locked: (n) => `Nível ${n}` },
  es: { pen: "Elige tu lápiz", frame: "Elige tu marco", close: "Cerrar", nick: "Nombre de usuario", save: "Guardar", nickHint: "2-16 caracteres. Solo puedes cambiar tu nombre una vez.", nickLocked: "Ya cambiaste tu nombre de usuario una vez.", nick_taken: "Ese nombre ya está en uso, prueba otro.", nick_invalid: "Usa letras, números, espacios y _ . - (2-16).", nick_net: "No se pudo conectar con el servidor.", locked: (n) => `Nivel ${n}` } };
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
function sheet(c, title, build) {
  const d = c.$("pick"), x = T[c.lang()]; d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "pk-box", `<b>${title}</b>`); const grid = el("div", "pk-grid"); build(grid, () => d.classList.add("hidden")); box.append(grid);
  const close = el("button", "pill cream small", `<span>${x.close}</span>`); close.onclick = () => d.classList.add("hidden"); box.append(close);
  d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
// PenSwoosh (common/PenWave.kt): one brush sweep that starts thin, swells and curls up thin again at the end, in the pen's
// colours, with a soft shadow under it and a glossy streak along its top; a premium pen has a few stars twinkling round it.
// The shape is the same for every pen, so it is worked out once.
const SW = 100, SH = 40;
let swooshShape = null;
function swooshOutline(pts, thick, shift) {
  const left = [], right = [], n = pts.length - 1;
  pts.forEach(([x, y, tx, ty], i) => {
    const t = i / n, half = (thick / 2) * Math.max(0.06, Math.sin(Math.PI * Math.pow(t, 0.8)));
    const nx = -ty, ny = tx, cx = x + nx * shift, cy = y + ny * shift;
    left.push([cx + nx * half, cy + ny * half]); right.push([cx - nx * half, cy - ny * half]);
  });
  const f = (p) => `${p[0].toFixed(2)} ${p[1].toFixed(2)}`;
  return "M" + left.map(f).join(" L") + " L" + right.reverse().map(f).join(" L") + "Z";
}
function swoosh() {
  if (swooshShape) return swooshShape;
  const segs = [[[0.07, 0.78], [0.30, 0.10], [0.48, 0.95], [0.70, 0.48]], [[0.70, 0.48], [0.80, 0.26], [0.88, 0.14], [0.95, 0.22]]]
    .map((s) => s.map(([x, y]) => [x * SW, y * SH]));
  const bez = (s, t) => { const u = 1 - t; return [0, 1].map((k) => u * u * u * s[0][k] + 3 * u * u * t * s[1][k] + 3 * u * t * t * s[2][k] + t * t * t * s[3][k]); };
  // the curve sampled densely, then 49 points at even distances along it, each with its direction (as PathMeasure does)
  const dense = []; segs.forEach((s, i) => { for (let k = i ? 1 : 0; k <= 200; k++) dense.push(bez(s, k / 200)); });
  const len = [0]; for (let i = 1; i < dense.length; i++) len.push(len[i - 1] + Math.hypot(dense[i][0] - dense[i - 1][0], dense[i][1] - dense[i - 1][1]));
  const total = len[len.length - 1], pts = [];
  for (let i = 0, j = 1; i <= 48; i++) {
    const d = (total * i) / 48; while (j < dense.length - 1 && len[j] < d) j++;
    const a = dense[j - 1], b = dense[j], f = (d - len[j - 1]) / (len[j] - len[j - 1] || 1), l = Math.hypot(b[0] - a[0], b[1] - a[1]) || 1;
    pts.push([a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, (b[0] - a[0]) / l, (b[1] - a[1]) / l]);
  }
  const thick = SH * 0.42;
  return (swooshShape = { body: swooshOutline(pts, thick, 0), shine: swooshOutline(pts, thick * 0.32, -thick * 0.18) });
}
const STAR = (x, y, r) => `M${x} ${y - r}Q${x} ${y} ${x + r} ${y}Q${x} ${y} ${x} ${y + r}Q${x} ${y} ${x - r} ${y}Q${x} ${y} ${x} ${y - r}Z`;
/** The pen as an SVG brush swash; [id] keeps its gradient's name apart from the other cards'. */
export function penSwooshSvg(pen, id) {
  const { body, shine } = swoosh(), cols = pen.colors.length > 1 ? pen.colors : [pen.colors[0], pen.colors[0]];
  const stops = cols.map((k, i) => `<stop offset="${i / (cols.length - 1)}" stop-color="${k}"/>`).join("");
  const stars = pen.effect && pen.effect !== "NONE"
    ? `<g class="pp-stars" fill="${cols[0]}">${[[20, 8, 2.2], [55, 35, 1.8], [86, 25, 2.5]].map(([x, y, r], i) => `<path style="animation-delay:${-i * 0.6}s;transform-origin:${x}px ${y}px" d="${STAR(x, y, r)}"/>`).join("")}</g>` : "";
  return `<svg viewBox="0 0 ${SW} ${SH}" aria-hidden="true"><defs><linearGradient id="pg-${id}" x1="${0.07 * SW}" x2="${0.95 * SW}" y1="0" y2="0" gradientUnits="userSpaceOnUse">${stops}</linearGradient></defs>` +
    `<path d="${body}" fill="#000" fill-opacity=".16" transform="translate(0 ${SH * 0.06})"/><path d="${body}" fill="url(#pg-${id})"/><path d="${shine}" fill="#fff" fill-opacity=".38"/>${stars}</svg>`;
}
// PenPickerSheet: every pen, locked ones dimmed with what opens them (level / store / rune map / league prize).
export function openPenPicker(c) {
  const { P } = c, lv = c.levelOf(P.xp).level, body = el("div", "pp-grid");
  const ov = penWindow(c.$("stage"), { title: S("pen_picker_title"), body });
  const owns = (p) => (P.owned || []).includes("pen:" + p.id);
  penList().filter((p) => !p.retired || owns(p)).forEach((p) => {
    const open = p.league ? owns(p) : p.price > 0 || p.rune ? owns(p) : lv >= p.level;
    const why = p.rune ? S("cosmetic_locked_rune") : p.price > 0 ? S("store_in_store") : p.league ? S("cosmetic_locked_league") : S("avatar_frame_locked_level", p.level);
    const b = el("button", "pp-card" + (P.pen === p.id ? " on" : "") + (open ? "" : " lock"), `${penSwooshSvg(p, p.id)}<b class="fitw">${p[c.lang()]}</b>${open ? "" : `<small>${why}</small>`}`);
    b.onclick = () => { if (!open) return; P.pen = p.id; c.saveP(); c.refreshHome(); ov.close(); };
    body.append(b);
  });
  // a long name shrinks to fit its card rather than being cut (FitText in the app)
  requestAnimationFrame(() => c.fit && c.fit());
}
// AvatarFramePickerSheet: "Profil görseli" (Karalak / Google photo) and the ring grid; locked rings are faded with a padlock and
// the level (or "Mağazada") they open at.
export function openFramePicker(c) {
  const { P } = c, lv = c.levelOf(P.xp).level, host = c.$("stage");
  const body = el("div", "fp-body");
  const lock = '<svg viewBox="0 0 24 24"><path d="M7 11V8a5 5 0 0110 0v3" fill="none" stroke="#2b2b2b" stroke-width="2.6" stroke-linecap="round"/><rect x="4.5" y="10.5" width="15" height="11" rx="2.6" fill="#2b2b2b"/></svg>';
  const ov = appWindow(host, { title: S("avatar_frame_picker_title"), body });
  body.append(el("div", "fp-h", S("avatar_source_title")));
  const src = el("div", "fp-src");
  const kar = el("button", "", `<span class="av"><img src="img/avatar_dino.webp" alt=""></span><b>${S("avatar_source_karalak")}</b>`);
  src.append(kar); body.append(src);
  body.append(el("div", "fp-h", S("avatar_frame_section")));
  const grid = el("div", "fp-grid");
  frameList().filter((f) => !f.league && !f.retired && (f.level > 0 || f.price > 0 || P.owned.includes("frame:" + f.id) || f.id === "SCRIBBLER")).forEach((f) => {
    const own = f.price > 0 ? P.owned.includes("frame:" + f.id) : f.level <= lv;
    const b = el("button", "fp-card" + (P.frame === f.id ? " on" : "") + (own ? "" : " lock"), "");
    const r = frameRing(f, "fp-ring"); r.querySelector("img:first-child").style.display = f.level === 0 ? "none" : "none"; b.append(r);
    if (!own) b.append(el("div", "fp-lk", `${lock}<span>${f.price > 0 ? S("store_in_store") : S("avatar_frame_locked_level", f.level)}</span>`));
    b.onclick = () => { if (!own) return; P.frame = f.id; c.saveP(); c.refreshHome(); ov.close(); };
    grid.append(b);
  });
  body.append(grid);
}
export function openNickEdit(c) {
  const { P, lang } = c, x = T[lang()], d = c.$("pick"); d.classList.remove("hidden"); d.replaceChildren();
  const locked = (P.nameChanges || 0) >= 1;
  const box = el("div", "pk-box", `<b>${x.nick}</b>`), inp = el("input", "pk-in"); inp.maxLength = 16; inp.value = P.nick; inp.disabled = locked;
  const note = el("p", "pk-note", locked ? x.nickLocked : x.nickHint);
  const save = el("button", "pill orange small", `<span>${x.save}</span>`); save.style.display = locked ? "none" : "";
  save.onclick = async () => {
    if (childMode()) { note.textContent = S("child_feature_locked"); return; } // a chosen name is published (agegate.js)
    const v = inp.value.trim().replace(/\s+/g, " "); if (v === P.nick) { d.classList.add("hidden"); return; }
    note.textContent = "…"; const r = await c.net.claimUsername(v, P.nickClaimed ? P.nick : null);
    if (r === "ok") { P.nick = v; P.nickClaimed = true; P.nameChanges = (P.nameChanges || 0) + 1; c.saveP(); c.refreshHome(); d.classList.add("hidden"); }
    else note.textContent = x["nick_" + r] || x.nick_net;
  };
  const close = el("button", "pill cream small", `<span>${x.close}</span>`); close.onclick = () => d.classList.add("hidden");
  box.append(inp, note, save, close); d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); }; if (!locked) setTimeout(() => inp.focus(), 50);
}
