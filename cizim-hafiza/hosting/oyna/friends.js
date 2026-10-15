import * as net from "./firebase.js";

const T = {
  tr: { title: "Arkadaşlarım", code: "Kodun", hint: "Arkadaşını davet et, seviye 5'e ulaşınca ikiniz de 500 XP kazanın!", share: "Kodumu Paylaş", addL: "Arkadaş ekle", addH: "6 haneli kod", add: "Ekle",
        listT: "Arkadaşların", reqT: "Arkadaşlık İstekleri", accept: "Kabul et", decline: "Reddet", invite: "Davet Et", empty: "Henüz arkadaşın yok. Kodunu paylaş ya da bir kod gir!", remove: "Kaldır", block: "Engelle", unblock: "Engeli Kaldır", blockedT: "Engellenenler",
        removeQ: (n) => `${n} adlı kişiyi arkadaşlıktan çıkarmak istediğine emin misin?`, blockQ: (n) => `${n} adlı kişiyi engellersen sana bir daha davet gönderemez.`, yes: "Evet", no: "Vazgeç",
        notfound: "Bu kodda biri yok.", self: "Bu senin kodun.", already: (n) => `${n} zaten arkadaşın.`, added: (n) => `${n} arkadaş oldunuz!`, sent: (n) => `${n} kişisine istek gönderildi.`, blocked: "İstek gönderilemedi.", copied: "Kod kopyalandı", err: "Bir sorun oldu, tekrar dene.", loading: "…" },
  en: { title: "My Friends", code: "Your code", hint: "Invite a friend — when they reach level 5 you both get 500 XP!", share: "Share my code", addL: "Add a friend", addH: "6-digit code", add: "Add",
        listT: "Your friends", reqT: "Friend requests", accept: "Accept", decline: "Decline", invite: "Invite", empty: "No friends yet. Share your code or enter one!", remove: "Remove", block: "Block", unblock: "Unblock", blockedT: "Blocked",
        removeQ: (n) => `Remove ${n} from your friends?`, blockQ: (n) => `If you block ${n}, they can't send you invites any more.`, yes: "Yes", no: "Cancel",
        notfound: "Nobody has this code.", self: "That's your own code.", already: (n) => `${n} is already your friend.`, added: (n) => `You and ${n} are friends now!`, sent: (n) => `Request sent to ${n}.`, blocked: "Couldn't send the request.", copied: "Code copied", err: "Something went wrong, try again.", loading: "…" },
};
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
  } catch { msg = T[c.lang()].err; renderFriends(c); }
}
export function stop() { unsubs.forEach((u) => { try { u(); } catch {} }); unsubs = []; }

export function renderFriends(c) {
  if (c.current() !== "friends") return;
  const { $, P, lang } = c, x = T[lang()];
  $("fr-title").textContent = x.title; $("fr-codel").textContent = x.code; $("fr-hint").textContent = x.hint;
  for (let i = 0; i < 6; i++) $("fr-d" + i).textContent = myCode ? myCode[i] : "";
  $("fr-sharet").textContent = x.share; $("fr-ribbon1t").textContent = x.addL; $("fr-ribbon2t").textContent = x.listT;
  $("fr-input").placeholder = x.addH; $("fr-addt").textContent = x.add; $("fr-msg").textContent = msg;
  $("fr-share").onclick = async () => {
    if (!myCode) return;
    const text = lang() === "tr" ? `Karalak'ta arkadaş kodum: ${myCode} — https://karalak-b6e11.web.app/oyna/` : `My Karalak friend code: ${myCode} — https://karalak-b6e11.web.app/oyna/`;
    try { if (navigator.share) await navigator.share({ text }); else { await navigator.clipboard.writeText(myCode); msg = x.copied; renderFriends(c); } } catch {}
  };
  const inp = $("fr-input");
  inp.oninput = () => { inp.value = inp.value.replace(/\D/g, "").slice(0, 6); };
  $("fr-add").onclick = async () => {
    const code = inp.value; if (code.length !== 6) return; msg = x.loading; renderFriends(c);
    try {
      const r = await net.addFriendByCode(code, P.nick);
      msg = r.status === "notfound" ? x.notfound : r.status === "self" ? x.self : r.status === "already" ? x.already(r.name) : r.status === "added" ? x.added(r.name) : r.status === "sent" ? x.sent(r.name) : x.blocked;
      if (r.status === "sent" || r.status === "added") inp.value = "";
    } catch { msg = x.err; }
    renderFriends(c); setTimeout(() => { if (msg !== x.err) { msg = ""; renderFriends(c); } }, 4000);
  };
  const list = $("fr-list"); list.replaceChildren();
  if (!friends.length && !requests.length && !blocked.length) {
    const e = el("div", "fr-empty", `<img src="img/friends_handshake.webp" alt=""><span>${x.empty}</span>`); list.append(e); return;
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
  const a = (label, q, fn) => { const b = el("button", "pill cream small", `<span>${label}</span>`); b.onclick = () => { d.classList.add("hidden"); c.confirm(q, x.yes, fn); }; box.append(b); };
  a(x.remove, x.removeQ(f.nickname || "?"), () => net.removeFriend(f.id));
  a(x.block, x.blockQ(f.nickname || "?"), () => net.blockUser(f.id, f.nickname || "?"));
  const cancel = el("button", "pill cream small", `<span>${x.no}</span>`); cancel.onclick = () => d.classList.add("hidden"); box.append(cancel);
  d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
