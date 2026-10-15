// Ready-made chat + emoji reactions (port of ReactionBar.kt): the painted dock, the phrase / emoji sheets, the chat
// history window and the short-lived bubbles. Messages are rooms/{code}/reactions documents, exactly like Android.
import * as net from "./firebase.js";
let S = null;
export async function loadChat() { if (!S) S = await (await fetch("chat_strings.json")).json(); }
const tx = (k, lang, ...a) => ((S && S[k] && S[k][lang]) || k).replace(/\{(\d)\}/g, (_, i) => a[+i]);
export const chatText = tx;
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
export const EMOJIS = [["😂", "funny"], ["👏", "nice"], ["😅", "hard"], ["🔥", "fire"], ["😱", "shock"], ["👋", "hi"], ["😢", "cry"], ["😡", "angry"], ["🤔", "think"], ["😴", "sleepy"], ["🥳", "party"], ["😎", "cool"], ["🙌", "raise_hands"], ["💪", "strong"], ["👍", "thumbs_up"], ["❤️", "heart"], ["🎉", "tada"], ["🏆", "trophy"]];
export const PHRASES = ["selam", "hazir_misin", "bol_sans", "iyi_eglenceler", "hos_geldin", "harikasin", "super_cizim", "bildin", "aynen_oyle", "vay_canina", "cok_iyiydi", "tam_isabet", "ne_ciziyorsun", "az_kaldi", "elinden_geliyor", "detaylara_bak_sen", "bilecegim", "zor_bir_tane", "aklima_geldi", "ipucu_ver", "ne_bu_oyle", "anlamadim_hic", "sanat_bu", "bir_daha_dene", "yakaladim_seni", "bu_turu_kazanacagim", "rovans_istiyorum", "kafa_kafaya", "gorusuruz", "tekrar_oynayalim_mi"].map((k) => "chat_" + k);

// per-device usage, so the quick rows float what the player really sends (Android: emojiUsageCounts / phraseUsageCounts)
const useKey = "chatUse";
const usage = () => { try { return JSON.parse(localStorage.getItem(useKey) || "{}"); } catch { return {}; } };
const bump = (k) => { try { const u = usage(); u[k] = (u[k] || 0) + 1; localStorage.setItem(useKey, JSON.stringify(u)); } catch {} };
const byUse = (list, key) => { const u = usage(); return [...list].sort((a, b) => (u[key(b)] || 0) - (u[key(a)] || 0)); };

export function send(code, emoji, key) { bump(key); return net.sendReaction(code, emoji, key).catch(() => {}); }
export const messageOf = (r, lang) => (r.messageKey && S && S[r.messageKey] ? tx(r.messageKey, lang) : r.emoji);

// ---------------- the dock ----------------
export function buildDock(c, code, getRoom) {
  const lang = c.lang(), dock = el("div", "cd-dock"), px = (v) => (v / 1740 * 100).toFixed(3) + "%", py = (v) => (v / 904 * 100).toFixed(3) + "%";
  const img = el("img", "cd-bg"); img.src = "img/ls_dock.webp"; dock.append(img);
  const abs = (cls, x, y, w, h, html) => { const e = el("div", cls, html || ""); e.style.cssText = `left:${px(x)};top:${py(y)};width:${px(w)};height:${py(h)}`; dock.append(e); return e; };
  const chatBtn = abs("cd-circ", 166, 394, 132, 132, `<svg viewBox="0 0 24 24"><path fill="#fff" d="M20 2H4a2 2 0 00-2 2v18l4-4h14a2 2 0 002-2V4a2 2 0 00-2-2z"/></svg>`);
  chatBtn.onclick = () => history(c, code, getRoom);
  const row = abs("cd-row phr", 318, 394, 1090, 132);
  byUse(PHRASES, (k) => k).forEach((k) => { const b = el("button", "cd-chip", tx(k, lang)); b.onclick = () => send(code, "", k); row.append(b); });
  const all = abs("cd-circ cd-sq", 1428, 394, 132, 132, `<svg viewBox="0 0 24 24"><path fill="#fff" d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>`);
  all.onclick = () => sheet(c, code, "phr");
  const erow = abs("cd-row emo", 130, 636, 1290, 128);
  byUse(EMOJIS, (e) => e[1]).forEach(([e, k]) => { const b = el("button", "cd-emo", e); b.onclick = () => send(code, e, k); erow.append(b); });
  const pick = abs("cd-circ cd-sq brown", 1448, 642, 116, 116, `<svg viewBox="0 0 24 24"><path fill="#fff" d="M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z"/></svg>`);
  pick.onclick = () => sheet(c, code, "emo");
  return dock;
}

// the phrase / emoji sheets (PaintedDialog lists)
function sheet(c, code, kind) {
  const lang = c.lang(), pk = c.$("pick"); pk.classList.remove("hidden"); pk.replaceChildren(); pk.onclick = (e) => { if (e.target === pk) pk.classList.add("hidden"); };
  const box = el("div", "pk-box cd-sheet", `<b>${tx(kind === "phr" ? "reaction_chat_sheet_title" : "reaction_emoji_sheet_title", lang)}</b>`);
  const list = el("div", kind === "phr" ? "cd-list" : "cd-grid");
  const done = () => pk.classList.add("hidden");
  if (kind === "phr") byUse(PHRASES, (k) => k).forEach((k) => { const b = el("button", "cd-chip big", tx(k, lang)); b.onclick = () => { done(); send(code, "", k); }; list.append(b); });
  else byUse(EMOJIS, (e) => e[1]).forEach(([e, k]) => { const b = el("button", "cd-emo2", e); b.onclick = () => { done(); send(code, e, k); }; list.append(b); });
  box.append(list); const x = el("button", "pill orange small", `<span>${tx("close", lang)}</span>`); x.onclick = done; box.append(x); pk.append(box);
}

// ---------------- chat history window (ls_chat_frame 941 x 1672) ----------------
let histRefresh = null;
export function history(c, code, getRoom) {
  const lang = c.lang(), pk = c.$("pick"); pk.classList.remove("hidden"); pk.replaceChildren(); pk.onclick = (e) => { if (e.target === pk) { pk.classList.add("hidden"); histRefresh = null; } };
  const wrap = el("div", "ch-wrap"), fr = el("div", "ch-frame"); wrap.append(fr);
  const img = el("img", "ch-bg"); img.src = "img/ls_chat_frame.webp"; fr.append(img);
  const pu = (v) => (v / 941 * 100).toFixed(3) + "%", pv = (v) => (v / 1672 * 100).toFixed(3) + "%", box = (x, y, w, h) => `left:${pu(x)};top:${pv(y)};width:${pu(w)};height:${pv(h)}`;
  const title = el("div", "ch-title lt", tx("online_chat_history_title", lang)); title.style.cssText = box(230, 66, 480, 112); fr.append(title);
  const x = el("button", "ch-x", "✕"); x.style.cssText = box(770, 30, 96, 96); x.onclick = () => { pk.classList.add("hidden"); histRefresh = null; }; fr.append(x);
  const list = el("div", "ch-list"); list.style.cssText = box(140, 262, 662, 1150); fr.append(list);
  const bar = el("div", "ch-bar"); bar.style.cssText = box(215, 1496, 512, 90); fr.append(bar);
  PHRASES.forEach((k) => { const b = el("button", "cd-chip", tx(k, lang)); b.onclick = () => send(code, "", k); bar.append(b); });
  EMOJIS.forEach(([e, k]) => { const b = el("button", "cd-chip", e); b.onclick = () => send(code, e, k); bar.append(b); });
  const fill = () => {
    const room = getRoom() || { players: {} }, my = c.myUid && c.myUid(), rs = reactionsNow;
    list.replaceChildren();
    if (!rs.length) { list.append(el("div", "ch-empty", tx("online_chat_empty_window", lang))); return; }
    rs.forEach((r) => {
      const p = room.players && room.players[r.uid], mine = r.uid === my, name = mine ? tx("online_you_label", lang, "").trim() : (p && p.displayName) || "";
      const t = new Date(r.sentAt || 0), hh = String(t.getHours()).padStart(2, "0") + ":" + String(t.getMinutes()).padStart(2, "0");
      const row = el("div", "ch-msg", `<span class="av"></span><div class="bub ${mine ? "me" : ""}">${name ? `<small>${esc(name)}</small>` : ""}<span>${esc(messageOf(r, lang))}</span></div><time>${hh}</time>`);
      if (p && c.ring) row.querySelector(".av").append(c.ring(p.frameId));
      list.append(row);
    });
    list.scrollTop = list.scrollHeight;
  };
  histRefresh = fill; fill(); pk.append(wrap);
}
const esc = (s) => String(s).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[ch]);

// ---------------- the live stream ----------------
let reactionsNow = [], unsub = null, curCode = null, listeners = new Set();
export const reactions = () => reactionsNow;
export function onReactions(fn) { listeners.add(fn); return () => listeners.delete(fn); }
export async function watch(code) {
  if (curCode === code && unsub) return; stop(); curCode = code;
  unsub = await net.watchReactions(code, (list) => { reactionsNow = list; listeners.forEach((f) => f(list)); if (histRefresh) histRefresh(); });
}
export function stop() { if (unsub) { try { unsub(); } catch {} } unsub = null; curCode = null; reactionsNow = []; }

// Per-sender bubbles: each reaction stays for 3.2 s and only fresh ones (≤ 10 s old) show (Android rememberActiveReactionsByUid).
export function activeBubbles(list, now = Date.now()) {
  const m = {}; for (const r of list) if (now - r.sentAt <= 3200 && now - r.sentAt >= 0) m[r.uid] = r; return m;
}
// A compact one-line chat row for the result screen (Android ChatRow): latest message, tap = history.
export function chatRow(c, code, getRoom) {
  const lang = c.lang(), row = el("button", "cd-row1", ""), draw = () => {
    const l = reactionsNow[reactionsNow.length - 1], room = getRoom() || { players: {} };
    const sender = l ? (l.uid === (c.myUid && c.myUid()) ? tx("online_you_label", lang, "").trim() : ((room.players && room.players[l.uid]) || {}).displayName || "") : "";
    row.innerHTML = `<svg viewBox="0 0 24 24"><path fill="#e8650f" d="M20 2H4a2 2 0 00-2 2v18l4-4h14a2 2 0 002-2V4a2 2 0 00-2-2z"/></svg><span>${l ? esc((sender ? sender + ": " : "") + messageOf(l, lang)) : tx("online_chat_row_empty", lang)}</span>${reactionsNow.length ? `<em>${reactionsNow.length}</em>` : ""}`;
  };
  draw(); onReactions(draw); row.onclick = () => history(c, code, getRoom); return row;
}
