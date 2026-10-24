// The running XP event on the home screen, as in the app: a painted "2x" sign on the Hızlı Eşleş card, and a banner along the top that
// blinks for 7 seconds (once per event for the whole visit) and then fades out for good. Other multipliers get a plain badge and strip.
import * as net from "./firebase.js";
import { S, loadStrings } from "./i18n.js";

const SHOW_MS = 7000;
let key = 0, startedAt = 0, tick = null;
const two = (n) => String(n).padStart(2, "0");
const countdown = (ms) => { const s = Math.max(0, Math.floor(ms / 1000)), h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60); return h > 0 ? `${h}:${two(m)}:${two(s % 60)}` : `${m}:${two(s % 60)}`; };
/** "2x" for a double, "%30" for a 1.3 (the same label the app writes). */
export const multiplierLabel = (m) => (m >= 2 - 1e-6 ? Math.round(m) + "x" : "%" + Math.round((m - 1) * 100));

export async function paintXpEvent(c) {
  const { $ } = c, sign = $("h-xpsign"), badge = $("h-xpbadge"), banner = $("h-xpbanner"); if (!sign || !banner) return;
  const hide = () => { sign.classList.add("hidden"); badge.classList.add("hidden"); banner.classList.add("hidden"); clearInterval(tick); tick = null; };
  let ev; try { [ev] = await Promise.all([net.xpEventInfo(), loadStrings().catch(() => {})]); } catch { return hide(); }
  if (!ev || ev.mult <= 1 || Date.now() >= ev.endsAt) return hide();
  const dbl = Math.abs(ev.mult - 2) < 1e-6;
  sign.classList.toggle("hidden", !dbl); badge.classList.toggle("hidden", dbl);
  if (!dbl) badge.textContent = `✦ ${multiplierLabel(ev.mult)} XP`;
  if (key !== ev.endsAt) { key = ev.endsAt; startedAt = Date.now(); }
  const elapsed = Date.now() - startedAt;
  if (elapsed < SHOW_MS) {
    banner.classList.remove("hidden"); banner.classList.toggle("strip", !dbl);
    banner.querySelector(".xb-name").textContent = ev.label || S("xp_event_default_name");
    banner.querySelector(".xb-sub").textContent = S(dbl ? "xp_event_banner_sub" : "xp_event_banner_sub_percent", multiplierLabel(ev.mult));
    banner.style.animation = "none"; void banner.offsetWidth;
    banner.style.animation = `xpb ${SHOW_MS}ms linear ${-elapsed}ms forwards`;
    banner.onanimationend = () => banner.classList.add("hidden");
  } else banner.classList.add("hidden");
  clearInterval(tick);
  const upd = () => { const left = ev.endsAt - Date.now(); if (left <= 0) return hide(); banner.querySelector(".xb-time").textContent = countdown(left); };
  upd(); tick = setInterval(upd, 1000);
}
