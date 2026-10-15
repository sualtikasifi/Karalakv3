// Shared pieces of the app's look: the popup window (AppWindowDialog.kt) and the orange action button (PrimaryButton).
import { S } from "./i18n.js";
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
