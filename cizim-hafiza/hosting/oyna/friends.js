import * as net from "./firebase.js";
import { S } from "./i18n.js";
import { friendInviteText, shareInvite } from "./ui.js";

// The screen's words are the app's own (strings.json), so the web never drifts from it (the invite reward was once "500 XP" here
// while the app paid gold). Only what the app does not say has its own words: it shows one "could not add" for a wrong code or
// one's own code, and shares through the share sheet rather than copying.
const OWN = {
  tr: { notfound: "Bu kodda biri yok.", self: "Bu senin kodun.", copied: "Kod kopyalandı" },
  en: { notfound: "Nobody has this code.", self: "That's your own code.", copied: "Code copied" },
  pt: { notfound: "Ninguém tem este código.", self: "Esse é o seu próprio código.", copied: "Código copiado" },
  es: { notfound: "Nadie tiene este código.", self: "Ese es tu propio código.", copied: "Código copiado" },
};
const words = (lang) => ({
  ...(OWN[lang] || OWN.en),
  title: S("online_friends_entry"), code: S("friends_my_code_label"), hint: S("friends_invite_reward_hint"), share: S("friends_share_code"),
  addL: S("friends_add_friend_label"), addH: S("friends_add_friend_hint"), add: S("friends_add_button"),
  listT: S("friends_list_title"), reqT: S("friends_requests_title"), accept: S("friends_request_accept"), decline: S("friends_request_decline"),
  invite: S("friends_invite_action"), empty: S("friends_empty"), remove: S("friends_remove_action"), block: S("friends_block_action"),
  unblock: S("friends_unblock_action"), blockedT: S("friends_blocked_section_title"),
  removeQ: (n) => S("friends_remove_confirm_message", n), blockQ: (n) => S("friends_block_confirm_message", n),
  yes: S("friends_remove_confirm_confirm"), yesBlock: S("friends_block_confirm_confirm"), no: S("friends_remove_confirm_cancel"),
  already: (n) => S("info_friend_already", n), added: (n) => S("info_friend_added", n), sent: (n) => S("info_friend_request_sent", n),
  blocked: S("error_friend_add_failed"), limit: S("error_friend_request_limit"), err: S("error_friend_add_failed"), loading: "…",
});
let myCode = null, friends = [], requests = [], blocked = [], unsubs = [], msg = "";
const k = 841 / 1080;
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };

export function layoutFriends(c) {
  const { place, $ } = c;
  const P = (id, x0, y0, x1, y1, dy = 0) => place($(id), x0 * k, (y0 + dy) * k, (x1 - x0) * k, (y1 - y0) * k);
  place($("fr-back"), 20, 40, 110, 106); place($("fr-duel"), 711, 40, 110, 106);
  P("fr-sign", 215, 110, 865, 349); P("fr-title", 290, 205, 790, 315);
  P("fr-panel", 70, 380, 1010, 958); P("fr-codel", 190, 456, 890, 528); P("fr-hint", 175, 524, 905, 612);
  P("fr-frame", 220, 612, 860, 612 + 640 * 337 / 1000); P("fr-share", 330, 846, 750, 942);
  P("fr-ribbon1", 70, 835, 470, 948, 120); P("fr-ribbon1t", 110, 850, 440, 935, 120);
  P("fr-input", 70, 957, 780, 1079, 120); P("fr-add", 790, 955, 1000, 1083, 120); P("fr-msg", 110, 1088, 970, 1142, 120);
  P("fr-ribbon2", 70, 1150, 490, 1269, 120); P("fr-ribbon2t", 105, 1168, 455, 1252, 120);
  P("fr-bigpanel", 70, 1255, 1010, 1850, 120); P("fr-list", 135, 1315, 945, 1790, 120);
  // the six digit cells sit on the painted code frame
  const fw = 640, fh = fw * 337 / 1000, cells = [401, 632, 865, 1099, 1334, 1570];
  cells.forEach((cx, i) => { const x = (1080 - fw) / 2 + cx / 1977 * fw, y = 612 + 349 / 667 * fh; P("fr-d" + i, x - 30, y - 40, x + 30, y + 40); });
}

export async function openFriends(c) {
  stop(); const { P } = c;
  try {
    myCode = await net.ensureFriendCode(P.nick); renderFriends(c);
    unsubs.push(await net.listen("friends", (l) => { friends = l; renderFriends(c); }));
    unsubs.push(await net.listen("friendRequests", (l) => { requests = l; renderFriends(c); }));
    unsubs.push(await net.listen("blockedUsers", (l) => { blocked = l; renderFriends(c); }));
  } catch { msg = words(c.lang()).err; renderFriends(c); }
}
export function stop() { unsubs.forEach((u) => { try { u(); } catch {} }); unsubs = []; }

export function renderFriends(c) {
  if (c.current() !== "friends") return;
  const { $, P, lang } = c, x = words(lang());
  $("fr-title").textContent = x.title; $("fr-duelt").textContent = S("duel_list_title"); $("fr-codel").textContent = x.code; $("fr-hint").textContent = x.hint;
  for (let i = 0; i < 6; i++) $("fr-d" + i).textContent = myCode ? myCode[i] : "";
  $("fr-sharet").innerHTML = `<svg viewBox="0 0 24 24" width="1em" height="1em"><path fill="#fff" d="M18 16.1c-.8 0-1.5.3-2 .8l-7.1-4.2c.1-.2.1-.5.1-.7s0-.5-.1-.7L16 7.2c.5.5 1.2.8 2 .8 1.7 0 3-1.3 3-3s-1.3-3-3-3-3 1.3-3 3c0 .2 0 .5.1.7L8 9.8C7.500 9.300 6.800 9 6 9c-1.700 0-3 1.300-3 3s1.300 3 3 3c.8 0 1.500-.3 2-.8l7.200 4.200c-.1.200-.1.400-.1.600 0 1.600 1.300 2.900 2.900 2.900s2.900-1.300 2.900-2.900-1.200-2.900-2.900-2.900z"/></svg> ` + x.share; $("fr-ribbon1t").textContent = x.addL; $("fr-ribbon2t").textContent = x.listT;
  $("fr-input").placeholder = x.addH; $("fr-addt").textContent = x.add; $("fr-msg").textContent = msg;
  $("fr-share").onclick = async () => {
    if (!myCode) return;
    await shareInvite(friendInviteText(myCode), myCode, () => { msg = x.copied; renderFriends(c); });
  };
  const inp = $("fr-input");
  inp.oninput = () => { inp.value = inp.value.replace(/\D/g, "").slice(0, 6); };
  $("fr-add").onclick = async () => {
    const code = inp.value; if (code.length !== 6) return; msg = x.loading; renderFriends(c);
    try {
      const r = await net.addFriendByCode(code, P.nick);
      msg = r.status === "notfound" ? x.notfound : r.status === "self" ? x.self : r.status === "already" ? x.already(r.name) : r.status === "added" ? x.added(r.name) : r.status === "sent" ? x.sent(r.name) : r.status === "limit" ? x.limit : x.blocked;
      if (r.status === "sent" || r.status === "added") inp.value = "";
    } catch { msg = x.err; }
    renderFriends(c); setTimeout(() => { if (msg !== x.err) { msg = ""; renderFriends(c); } }, 4000);
  };
  const list = $("fr-list"); list.replaceChildren();
  if (!friends.length && !requests.length && !blocked.length) {
    const e = el("div", "fr-empty", `<img src="img/friends_handshake.webp" alt="">${(() => { const i = x.empty.indexOf(". "), a = i > 0 ? x.empty.slice(0, i + 1) : x.empty, b = i > 0 ? x.empty.slice(i + 1).trim() : ""; return `<b>${a}</b>${b ? `<span>${b}</span>` : ""}`; })()}`); list.append(e); return;
  }
  if (requests.length) {
    list.append(el("div", "fr-sec", x.reqT));
    requests.forEach((r) => {
      const row = el("div", "fr-row", `<span class="nm">${esc(r.fromNickname || "?")}</span>`);
      const no = el("button", "fr-q", x.decline), yes = el("button", "fr-pill", x.accept);
      no.onclick = () => net.declineRequest(r.id); yes.onclick = () => net.acceptRequest(r, P.nick);
      row.append(no, yes); list.append(row);
    });
  }
  friends.forEach((f) => {
    const row = el("div", "fr-row", `<span class="nm">${esc(f.nickname || "?")}</span>`);
    const inv = el("button", "fr-pill", x.invite), more = el("button", "fr-more", "⋮");
    inv.onclick = () => c.inviteFriend(f);
    more.onclick = () => menu(c, f, x);
    row.append(inv, more); list.append(row);
  });
  if (blocked.length) {
    list.append(el("div", "fr-sec", x.blockedT));
    blocked.forEach((b) => { const row = el("div", "fr-row", `<span class="nm">${esc(b.nickname || "?")}</span>`); const u = el("button", "fr-pill", x.unblock); u.onclick = () => net.unblockUser(b.id); row.append(u); list.append(row); });
  }
}
const esc = (s) => String(s).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[ch]));
function menu(c, f, x) {
  const { $ } = c, d = $("fr-menu"); d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "fr-mbox", `<b>${esc(f.nickname || "?")}</b>`);
  const a = (label, q, yes, fn) => { const b = el("button", "pill cream small", `<span>${label}</span>`); b.onclick = () => { d.classList.add("hidden"); c.confirm(q, yes, fn); }; box.append(b); };
  const dl = el("button", "pill orange small", `<span>${S("duel_challenge_action")}</span>`); dl.onclick = () => { d.classList.add("hidden"); c.openDuel({ uid: f.id, name: f.nickname || "?" }); }; box.append(dl);
  a(x.remove, x.removeQ(f.nickname || "?"), x.yes, () => net.removeFriend(f.id));
  a(x.block, x.blockQ(f.nickname || "?"), x.yesBlock, () => net.blockUser(f.id, f.nickname || "?"));
  const cancel = el("button", "pill cream small", `<span>${x.no}</span>`); cancel.onclick = () => d.classList.add("hidden"); box.append(cancel);
  d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
