// Shared pieces of the app's look: the popup window (AppWindowDialog.kt) and the orange action button (PrimaryButton).
import { S, getLang } from "./i18n.js";
export const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };

/** The orange pill button of the app (PrimaryButton). */
export function primary(text, onClick, cls = "") { const b = el("button", "prim " + cls, `<span>${text}</span>`); if (onClick) b.onclick = onClick; return b; }

/**
 * The game's popup window: a centred parchment panel in a wooden frame with a title and a close button over a dimmed screen.
 * `body` and `footer` are elements; the body scrolls and the footer stays pinned. Returns the overlay (call .close() to remove).
 */
export function appWindow(host, { title, body, footer, centerTitle = false, onClose, cls = "" }) {
  const ov = el("div", "aw-ov " + cls), card = el("div", "aw-card"), inner = el("div", "aw-in");
  const head = el("div", "aw-head" + (centerTitle ? " ctr" : ""));
  const t = el("div", "aw-title", title); head.append(t);
  const close = () => { ov.remove(); onClose && onClose(); };
  if (!centerTitle) { const x = el("button", "aw-x", "✕"); x.setAttribute("aria-label", S("close")); x.onclick = close; head.append(x); }
  inner.append(head);
  const bd = el("div", "aw-body"); if (body) bd.append(body); inner.append(bd);
  if (footer) { const f = el("div", "aw-foot"); f.append(footer); inner.append(f); }
  card.append(inner); ov.append(card);
  ov.onclick = (e) => { if (e.target === ov) close(); };
  host.append(ov); ov.close = close;
  return ov;
}

/**
 * The painted "leave?" window (PaintedExitDialog.kt): the stern crowned dog on exit_game_dialog (1100x795) or the sad one on
 * exit_dialog (1100x823, the menu's "Karalak'tan çık"). The picture carries the sign and both buttons; the words are laid over it.
 */
const EXIT = {
  game: { art: "exit_game_dialog", w: 1100, h: 795, title: [264, 353, 850, 440], msg: [285, 468, 821, 566], left: [149, 572, 545, 716], right: [557, 572, 949, 716], out: "#7a0e0e" },
  menu: { art: "exit_dialog", w: 1100, h: 823, title: [230, 357, 882, 447], msg: [300, 455, 812, 531], left: [163, 566, 543, 731], right: [560, 566, 940, 731], out: "#7a2a08" },
};
export function exitDialog(host, { style = "game", title, message, confirm, dismiss, onConfirm, onDismiss }) {
  const s = EXIT[style], pc = (v, t) => (v / t * 100).toFixed(3) + "%";
  const box = (r) => `left:${pc(r[0], s.w)};top:${pc(r[1], s.h)};width:${pc(r[2] - r[0], s.w)};height:${pc(r[3] - r[1], s.h)}`;
  const ov = el("div", "ex-ov"), card = el("div", "ex-card"); card.style.aspectRatio = `${s.w}/${s.h}`;
  card.innerHTML = `<img src="img/${s.art}.webp" alt="">`;
  const put = (cls, r, html) => { const e = el("div", cls, html); e.style.cssText = box(r); card.append(e); return e; };
  put("ex-t", s.title, `<span>${title}</span>`); put("ex-m", s.msg, `<span>${message}</span>`);
  const close = () => ov.remove();
  const left = put("ex-b l", s.left, `<span>${dismiss}</span>`), right = put("ex-b r", s.right, `<span style="-webkit-text-stroke-color:${s.out}">${confirm}</span>`);
  left.onclick = () => { close(); onDismiss && onDismiss(); }; right.onclick = () => { close(); onConfirm && onConfirm(); };
  ov.onclick = (e) => { if (e.target === ov) { close(); onDismiss && onDismiss(); } };
  ov.append(card); host.append(ov);
  // the painted window shows once its picture is ready (first open on a slow line), never as bare words on the dimmed screen
  const img = card.firstChild, show = () => ov.classList.remove("wait"); ov.classList.add("wait");
  if (img.complete && img.naturalWidth) show(); else { img.addEventListener("load", show, { once: true }); img.addEventListener("error", show, { once: true }); setTimeout(show, 1500); }
  // FitText: the title stays on one line (shrinking to half), the message may take three
  const fit = (e, base, lines, min) => { const sp = e.firstChild; let k = 1; sp.style.fontSize = base + "px"; while (k > min && (e.scrollHeight > e.clientHeight + 1 || sp.scrollWidth > e.clientWidth + 1 || (lines === 1 && sp.offsetHeight > base * 1.4))) { k -= 0.04; sp.style.fontSize = base * k + "px"; } };
  requestAnimationFrame(() => { const w = card.getBoundingClientRect().width, k = w / s.w; const t = card.querySelector(".ex-t"), m = card.querySelector(".ex-m"); t.style.whiteSpace = "nowrap"; fit(t, 80 * k, 1, 0.62); if (t.firstChild.scrollWidth > t.clientWidth + 1) { t.style.whiteSpace = "normal"; fit(t, 66 * k, 2, 0.4); } fit(m, 46 * k, 3, 0.5); card.querySelectorAll(".ex-b").forEach((b) => { const sp = b.firstChild, room = b.clientWidth - 2 * 40 * k; let q = 1; sp.style.fontSize = 68 * k + "px"; while (q > 0.4 && sp.scrollWidth > room) { q -= 0.03; sp.style.fontSize = 68 * k * q + "px"; } }); });
  return ov;
}

/**
 * The app's painted windows (the chest window, the "what is new" tour): one picture carries the sign, the parchment, the
 * plate and the button, and the words are laid over it at fixed spots of the picture. `w`/`h` are the picture's own size, so a
 * spot is given in its pixels; `--k` (card width / picture width) turns a font size in picture pixels into screen pixels.
 * Returns { ov, card, put, fit, close }; `below` is an optional element shown under the card (a second button).
 */
export function paintedCard(host, { art, w, h, cls = "", onClose, below }) {
  const pc = (v, t) => (v / t * 100).toFixed(3) + "%";
  const ov = el("div", "pc-ov wait " + cls), col = el("div", "pc-col"), card = el("div", "pc-card");
  card.style.setProperty("--ar", (w / h).toFixed(4)); card.style.aspectRatio = `${w}/${h}`;
  const img = el("img"); img.src = `img/${art}.webp`; img.alt = ""; card.append(img); col.append(card); if (below) col.append(below); ov.append(col);
  const close = () => { window.removeEventListener("resize", size); ov.remove(); onClose && onClose(); };
  const put = (c, r, html) => { const e = el("div", c, html); e.style.cssText = `left:${pc(r[0], w)};top:${pc(r[1], h)};width:${pc(r[2] - r[0], w)};height:${pc(r[3] - r[1], h)}`; card.append(e); return e; };
  const size = () => card.style.setProperty("--k", (card.getBoundingClientRect().width / w).toFixed(5));
  /** Shrinks the words in `e` (its first child) from `base` picture pixels until they fit its box on `lines` lines. */
  const fit = (e, base, lines = 1, min = 0.5) => { const sp = e.firstElementChild || e, k = card.getBoundingClientRect().width / w; let q = 1; sp.style.fontSize = base * k + "px"; while (q > min && (e.scrollHeight > e.clientHeight + 1 || sp.scrollWidth > e.clientWidth + 1 || (lines === 1 && sp.offsetHeight > base * k * 1.5))) { q -= 0.04; sp.style.fontSize = base * k * q + "px"; } };
  ov.onclick = (e) => { if (e.target === ov || e.target === col) close(); };
  host.append(ov); ov.close = close; window.addEventListener("resize", size);
  const show = () => { ov.classList.remove("wait"); size(); };
  if (img.complete && img.naturalWidth) show(); else { img.addEventListener("load", show, { once: true }); img.addEventListener("error", show, { once: true }); setTimeout(show, 1500); }
  size();
  return { ov, card, put, fit, close, size };
}

/**
 * Ads only exist in the phone app, so every "watch an ad" spot on the web says so and sends the player to the store instead
 * of pretending to show one.
 */
export const PLAY_URL = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza";
const APP_ONLY = {
  tr: { title: "Uygulamada", body: "Reklam ödülleri yalnızca Karalak uygulamasında. Google Play'den indirip bu ödülü orada alabilirsin!", go: "Uygulamayı Aç", close: "Kapat" },
  en: { title: "In the app", body: "Ad rewards are only in the Karalak app. Get it on Google Play to claim this reward there!", go: "Get the app", close: "Close" },
};
export function appOnlyDialog(host) {
  const x = APP_ONLY[getLang()] || APP_ONLY.tr;
  const body = el("div", "ao-body", `<div class="ao-ic">📱</div><p>${x.body}</p>`);
  const foot = el("div", "ao-foot"), go = primary(x.go, () => { window.open(PLAY_URL, "_blank", "noopener"); ov.close(); });
  foot.append(go);
  const ov = appWindow(host, { title: x.title, body, footer: foot, centerTitle: false });
  return ov;
}
