// Race a Friend: entry, create/join, live lobby, synchronized game start and the ranking screen.
import * as net from "./firebase.js";
import { S } from "./i18n.js";
import { frameRing, frameById } from "./store.js";
import { loadChat, buildDock, watch as watchChat, stop as stopChat, onReactions, activeBubbles, messageOf, reactions as chatNow, chatRow, chatText } from "./chat.js";
import { setupGroup, fitPaper, SETUP_T } from "./offline.js";
import { renderOnlineResult as drawOnlineResult, loadResultStrings } from "./result.js";

const T = {
  tr: { title: "Arkadaşlarınla Yarış", l1: "Ne çizdiğini hatırlamak, bazen", l2: "kuantum fiziğinden bile daha zor olabilir.", l3: "Karalama Savaşlarına", l4: "Hoşgeldin!", create: "Oda Kur", join: "Koda Katıl",
        cTitle: "Oda Kur", words: "Kelime sayısı", diff: "Zorluk", easy: "Kolay", med: "Orta", hard: "Zor", mode: "Mod", normal: "Süreli", relaxed: "Rahat", make: "Oda Oluştur",
        jTitle: "Koda Katıl", nick: "Takma adın", code: "6 haneli oda kodu", go: "Katıl", notfound: "Böyle bir oda yok.", full: "Oda dolu.", kicked: "Bu odadan atıldın.", err: "Bir sorun oldu, tekrar dene.",
        lobby: "Lobi", roomCode: "Oda kodu", send: "Bu kodu arkadaşına gönder", occ: (a, b) => `${a}/${b} kişi`, ready: "Hazır", notReady: "Hazır değilim", start: "Oyunu Başlat", waitHost: "Herkes hazır! Oda sahibinin başlatmasını bekliyoruz…",
        you: (n) => `${n} (Sen)`, host: "Oda sahibi", inviteF: "Arkadaş çağır", inviteT: "Arkadaşını odaya çağır", inviteE: "Henüz arkadaşın yok. Oda kodunu paylaşarak davet edebilirsin.", invSend: "Çağır", invSent: "Davet gönderildi ✓", share: "Paylaş",
        starting: "Oyun başlıyor!", needTwo: "Başlamak için en az 2 oyuncu gerekir.", pending: "Mevcut tur bitince katılacaksın", leave: "Odadan ayrıl",
        done: (n) => `Bitirdin! ${n} Puan`, waiting: "Diğer oyuncuların bitirmesini bekliyoruz…", place: (n) => `${n}. oldun!`, drawings: "Kimin çizimleri?", ofTotal: (a, b) => `${a}/${b} doğru`, rematch: "Tekrar Oyna", lobbyBack: "Lobiye dön", pts: "puan", leaveQ: "Odadan ayrılmak istiyor musun?" },
  en: { title: "Race Your Friends", l1: "Remembering what you drew can be", l2: "harder than quantum physics.", l3: "Welcome to the", l4: "Doodle Wars!", create: "Create Room", join: "Join with Code",
        cTitle: "Create Room", words: "Number of words", diff: "Difficulty", easy: "Easy", med: "Medium", hard: "Hard", mode: "Mode", normal: "Timed", relaxed: "Relaxed", make: "Create Room",
        jTitle: "Join with Code", nick: "Your nickname", code: "6-digit room code", go: "Join", notfound: "No such room.", full: "The room is full.", kicked: "You were removed from this room.", err: "Something went wrong, try again.",
        lobby: "Lobby", roomCode: "Room code", send: "Send this code to your friend", occ: (a, b) => `${a}/${b} players`, ready: "Ready", notReady: "Not ready", start: "Start Game", waitHost: "Everyone's ready! Waiting for the host to start…",
        you: (n) => `${n} (You)`, host: "Host", inviteF: "Invite a friend", inviteT: "Invite a friend to the room", inviteE: "No friends yet. Share the room code to invite.", invSend: "Invite", invSent: "Invite sent ✓", share: "Share",
        starting: "Game starting!", needTwo: "At least 2 players are needed to start.", pending: "You'll join when the current round ends", leave: "Leave room",
        done: (n) => `Finished! ${n} points`, waiting: "Waiting for the other players to finish…", place: (n) => `You placed #${n}!`, drawings: "Whose drawings?", ofTotal: (a, b) => `${a}/${b} correct`, rematch: "Play Again", lobbyBack: "Back to lobby", pts: "pts", leaveQ: "Leave the room?" },
};
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const esc = (s) => String(s).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[ch]));
const PRESENCE = 150000;
let opts = { wordCount: 10, diffs: new Set(["EASY", "MEDIUM", "HARD"]), mode: "NORMAL", cats: new Set(), team: false };
const CAT_EMOJI = { Hayvanlar: "🐶", Animals: "🐶", Eşyalar: "🧺", Objects: "🧺", Meslekler: "👮", Professions: "👮", Spor: "⚽", Sports: "⚽", Doğa: "🌲", Nature: "🌲", Yiyecekler: "🍎", Food: "🍎", Taşıtlar: "🚗", Vehicles: "🚗", Duygular: "😊", Emotions: "😊", Giyim: "👕", Clothing: "👕" };
let catList = [];
let code = null, room = null, unsub = null, hb = null, cdTimer = null, started = false, myFriends = [], sent = new Set(), finishedLocal = false;

// ---------- layout ----------
export function layoutOnline(c) {
  const { place, $ } = c, cur = c.current();
  if (cur === "onl") {
    place($("on-back"), 20, 40, 110, 106); place($("on-sign"), 70.5, 300, 700, 586); place($("on-title"), 130.5, 626, 580, 222);
    place($("on-paper"), 80.5, 884, 680, 324); place($("on-text"), 140.5, 965, 560, 195);
    place($("on-create"), 100.5, 1236, 640, 150); place($("on-join"), 100.5, 1424, 640, 158);
  } else if (cur === "onj") {
    place($("oj-back"), 20, 40, 110, 106); place($("oj-title"), 423 - 230, 368 - 26, 460, 76);
    place($("oj-panel"), 42.05, 542.3, 756.9, 522.3);
    place($("oj-nick"), 138.1, 688.8, 554.6, 89.8); place($("oj-code"), 138.1, 838.5, 554.6, 89.8); place($("oj-l1"), 183, 628.8, 480, 50); place($("oj-l2"), 183, 782, 480, 50); place($("oj-msg"), 42, 1190, 757, 80);
    place($("oj-go"), 185.9, 982.4, 469.3, 149.4);
  } else if (cur === "onc") {
    place($("oc-back"), 20, 40, 110, 106); place($("oc-title"), 160, 258, 520, 110);
    place($("oc-paper"), 42, 472, 757, 925); place($("oc-body"), 110, 520, 621, 840);
    place($("oc-make"), 65, 1610, 710, 172); place($("oc-msg"), 60, 1790, 720, 60);
  } else if (cur === "onfin") {
    // as the app's waiting scene: the title is written on the painted sign, the score card is centred in what is left below it
    place($("of-title"), 0, 216, 841, 123);
    const u = c.u(), col = $("of-col");
    col.style.left = 56 * u + "px"; col.style.width = 729 * u + "px"; col.style.top = 350 * u + "px"; col.style.bottom = (1870 - 1800) * u + "px";
  } else if (cur === "onw") {
    place($("ow-back"), 20, 40, 110, 106);
    place($("ow-title"), 0, 216, 841, 123);
    const u = c.u(), col = $("ow-col");
    col.style.left = 41 * u + "px"; col.style.width = 759 * u + "px"; col.style.top = 362 * u + "px"; col.style.bottom = (1870 - 1775) * u + "px";

  }
}

// ---------- entry / create / join ----------
let quotesP;
const loadQuotes = () => (quotesP ||= fetch("entry_quotes.json").then((r) => r.json()).catch(() => ({ tr: [["Karalama Savaşlarına", "Hoşgeldin!"]], en: [["Welcome to the", "Doodle Wars!"]] })));
let entryQuote = null;
export function renderEntry(c, fresh) {
  const { $, lang } = c, x = T[lang()];
  $("on-title").textContent = x.title; $("on-create-t").textContent = x.create; $("on-join-t").textContent = x.join;
  const tx = $("on-text");
  // a different note every time the screen opens (never the same one twice in a row); entry_quotes.json mirrors the app's online_lobby_quotes
  loadQuotes().then((q) => {
    const list = q[lang()] || q.tr; let i = 0;
    if (!fresh && entryQuote && entryQuote.list === list) i = entryQuote.i; // a re-layout (keyboard, rotation) keeps the note
    else if (list.length > 1) { let last = -1; try { last = +localStorage.getItem("entryQuote"); } catch {} do { i = Math.floor(Math.random() * list.length); } while (i === last); try { localStorage.setItem("entryQuote", i); } catch {} }
    entryQuote = { list, i };
    tx.replaceChildren(...list[i].map((t) => el("span", "", t)));
    // one size for the lines: the longest decides how far they shrink (minScale 0.6, like the app)
    requestAnimationFrame(() => { const base = 41 * c.u(); let k = 1; tx.style.fontSize = base + "px"; while (k > 0.6 && [...tx.children].some((e) => e.scrollWidth > tx.clientWidth + 1)) { k -= 0.02; tx.style.fontSize = base * k + "px"; } });
  });
}
export async function renderCreate(c) {
  const { $, lang } = c, x = T[lang()], C = (k) => chatText(k, lang());
  await loadChat();
  if (!catList.length || catList.lang !== lang()) { try { const w = await (await fetch(lang() === "tr" ? "words_tr.json" : "words_en.json")).json(); catList = [...new Set(w.map((q) => q.category))].sort(); catList.lang = lang(); opts.cats = new Set(); } catch { catList = []; } }
  $("oc-title").textContent = x.cTitle; $("oc-make-t").textContent = x.make; $("oc-msg").textContent = "";
  const body = $("oc-body"); body.replaceChildren();
  // The same wooden setup rows as Play Offline (CreateRoomScreen on the shared WoodScreen).
  const u = c.u(), group = (label, items, cols, h, size) => setupGroup(body, u, label, items, cols, h, size), again = () => renderCreate(c);
  const S = SETUP_T[lang()];
  group(x.nick, [[c.P.nick, false, null]], 1, 78, 30);
  group(S.count, [10, 20, 30, 40, 50].map((n) => [String(n), opts.wordCount === n, () => { opts.wordCount = n; again(); }]), 5, 78, 37);
  group(C("online_room_mode_title"), [[false, C("online_room_mode_free_for_all")], [true, C("online_room_mode_team")]].map(([k, t]) => [t, opts.team === k, () => { opts.team = k; again(); }]), 2, 82, 29);
  group(C("select_category"), [...catList.map((k) => [`${CAT_EMOJI[k] || "✨"} ${k}`, opts.cats.has(k), () => { opts.cats.has(k) ? opts.cats.delete(k) : opts.cats.add(k); again(); }]), [`🎨 ${C("all_categories")}`, opts.cats.size === 0, () => { opts.cats = new Set(); again(); }]], 4, 82, 25);
  group(S.diff, [["EASY", x.easy], ["MEDIUM", x.med], ["HARD", x.hard]].map(([k, t]) => [t, opts.diffs.has(k) && opts.diffs.size < 3, () => { opts.diffs.size === 3 ? (opts.diffs = new Set([k])) : opts.diffs.has(k) ? (opts.diffs.size > 1 && opts.diffs.delete(k)) : opts.diffs.add(k); again(); }]).concat([[C("all_difficulties"), opts.diffs.size === 3, () => { opts.diffs = new Set(["EASY", "MEDIUM", "HARD"]); again(); }]]), 4, 78, 27);
  fitPaper(c, "oc-paper", "oc-body");
  $("oc-make").onclick = async () => {
    $("oc-msg").textContent = "…";
    try {
      const lv = c.levelOf(c.P.xp).level;
      const all = opts.diffs.size === 3 ? null : [...opts.diffs].sort((a, b) => ["EASY", "MEDIUM", "HARD"].indexOf(a) - ["EASY", "MEDIUM", "HARD"].indexOf(b)).join(",");
      const cat = opts.cats.size ? [...opts.cats].sort().join(",") : null;
      const cd = await net.createRoom(c.P.nick, lv, c.P.frame, { wordCount: opts.wordCount, difficulty: all, mode: opts.mode, category: cat, teamMode: opts.team });
      enterLobby(c, cd);
    } catch { $("oc-msg").textContent = x.err; }
  };
}
export function renderJoin(c) {
  const { $, P, lang } = c, x = T[lang()];
  $("oj-title").textContent = S("online_join_room"); $("oj-go-t").textContent = S("online_join_room_action"); $("oj-l1").textContent = S("online_nickname_label"); $("oj-l2").textContent = S("online_room_code_label"); $("oj-msg").textContent = "";
  $("oj-nick").value = P.nick; $("oj-code").placeholder = S("friends_add_friend_hint");
  $("oj-code").oninput = () => { $("oj-code").value = $("oj-code").value.replace(/\D/g, "").slice(0, 6); };
  $("oj-go").onclick = async () => {
    const cd = $("oj-code").value; if (cd.length !== 6) return;
    const nick = ($("oj-nick").value || P.nick).trim().slice(0, 20) || P.nick; $("oj-msg").textContent = "…";
    try {
      const r = await net.joinRoom(cd, nick, c.levelOf(P.xp).level, P.frame);
      if (r === "ok") enterLobby(c, cd); else $("oj-msg").textContent = x[r] || x.err;
    } catch { $("oj-msg").textContent = x.err; }
  };
}
export const joinWithCode = async (c, cd) => {
  try { const r = await net.joinRoom(cd, c.P.nick, c.levelOf(c.P.xp).level, c.P.frame); if (r === "ok") enterLobby(c, cd); return r; } catch { return "err"; }
};

// ---------- lobby ----------
async function enterLobby(c, cd) {
  code = cd; started = false; finishedLocal = false; sent = new Set(); room = null;
  c.show("onw");
  if (unsub) try { unsub(); } catch {}
  unsub = await net.watchRoom(cd, (r) => onRoom(c, r));
  await loadChat(); c.$("ow-dock").replaceChildren(); watchChat(cd).catch(() => {});
  chatUnsub && chatUnsub(); chatUnsub = onReactions(() => { if (c.current() === "onw") { renderLobby(c); clearTimeout(bubbleT); bubbleT = setTimeout(() => c.current() === "onw" && renderLobby(c), 3300); } });
  clearInterval(hb); const beat = () => net.touchPresence(cd, c.levelOf(c.P.xp).level, c.P.frame).catch(() => {}); beat(); hb = setInterval(beat, 60000);
  net.listen("friends", (l) => { myFriends = l; }).catch(() => {});
}
export function leaveLobby(c, goHome = true) {
  clearInterval(hb); clearInterval(cdTimer); stopChat(); if (chatUnsub) { chatUnsub(); chatUnsub = null; } if (unsub) { try { unsub(); } catch {} unsub = null; }
  if (code) net.leaveRoom(code).catch(() => {});
  code = null; room = null; if (goHome) c.show("home");
}
const present = (p, now = Date.now()) => !p.left && now - (p.lastSeenAt || p.joinedAt || 0) <= PRESENCE;
function onRoom(c, r) {
  room = r;
  if (!r) return;
  if (myId && r.players && !r.players[myId] && c.current() === "onw") { c.info(T[c.lang()].kicked); leaveLobby(c); return; }
  if (c.current() === "onw") {
    if (r.status === "PLAYING" && !started && !(r.players[myId]?.pendingNextRound)) { started = true; clearInterval(cdTimer); c.startOnlineGame(r, code); return; }
    renderLobby(c);
  } else if (c.current() === "onfin") { if (r.status === "FINISHED") renderOnlineResult(c); else renderFinished(c); }
  else if (c.current() === "res2" && c.kind() === "online" && r.status === "FINISHED") renderOnlineResult(c, true);
  else if (c.current() === "search" && c.kind() === "online" && r.status === "FINISHED") renderOnlineResult(c);
  else if (r.status === "WAITING" && started && c.kind() === "online" && c.current() === "res2") { /* host sent everyone back */ started = false; c.show("onw"); }
}
let myId = null, chatUnsub = null, bubbleT = null;
const chatCtx = (c) => Object.assign(Object.create(c), { myUid: () => myId, ring: (f) => { const r = frameRing(frameById(f), "ow-av"); r.style.cssText += ";position:relative;width:100%;height:100%"; return r; }, chatText });
net.myUid().then((u) => { myId = u; }).catch(() => {});

// The waiting room (WaitingRoomScreen.kt): the code card, "Oyuncular" band with the occupancy pill, the 2-column grid of eight seats
// (or two team columns), the waiting pill, the chat dock and ONE ready button that turns into "Oyunu Başlat" for the host.
const RANK_KEYS = ["rank_karalamaci", "rank_cirak", "rank_ressam", "rank_usta_ressam", "rank_sanatci", "rank_buyuk_usta"], RANK_AT = [1, 20, 40, 60, 80, 100];
const rankKey = (l) => RANK_KEYS[RANK_AT.filter((t) => l >= t).length - 1];
let readyOverride = null, starting = false, notice = "", noticeT = 0;
const say = (c, text) => { notice = text; clearTimeout(noticeT); noticeT = setTimeout(() => { notice = ""; if (c.current() === "onw") renderLobby(c); }, 2500); };
const PERSON = '<svg viewBox="0 0 24 24" width="1em" height="1em"><circle cx="12" cy="8" r="4.2" fill="#e9801d"/><path d="M3.8 21c0-4.4 3.7-7 8.2-7s8.2 2.6 8.2 7z" fill="#e9801d"/></svg>';
const KICK = '<svg viewBox="0 0 24 24" width="1em" height="1em"><path fill="#c0392b" d="M14 8c0-2.2-1.8-4-4-4S6 5.8 6 8s1.8 4 4 4 4-1.8 4-4zm-4 5c-2.7 0-8 1.3-8 4v3h9.3a6.5 6.5 0 01-.3-2c0-1.2.3-2.300.9-3.200C11 14.300 10.500 13 10 13zm8.500-2L17 12.500 15.500 11 14 12.500l1.500 1.500L14 15.500 15.500 17 17 15.500 18.500 17 20 15.500 18.500 14 20 12.500z"/></svg>';
const SWAP = '<svg viewBox="0 0 24 24" width="1em" height="1em"><path fill="#e9801d" d="M6.990 11L3 15l3.990 4v-3H14v-2H6.990v-3zM21 9l-3.990-4v3H10v2h7.010v3L21 9z"/></svg>';

function renderLobby(c) {
  const { $, lang } = c, lg = lang();
  if (!room) return;
  net.myUid().then((u) => { myId = u; });
  $("ow-title").textContent = S("online_waiting_room_title");
  const now = Date.now(), team = !!room.teamMode, isHost = room.hostUid === myId;
  const all = Object.entries(room.players || {}).map(([uid, p]) => ({ uid, ...p })).filter((p) => !p.left);
  const meP = all.find((p) => p.uid === myId), others = all.filter((p) => p.uid !== myId && present(p, now)).sort((a, b) => (a.joinedAt || 0) - (b.joinedAt || 0));
  const presentCount = (meP ? 1 : 0) + others.length, amPending = !!(meP && meP.pendingNextRound);
  const active = [...(meP ? [meP] : []), ...others].filter((p) => !p.pendingNextRound);
  const amReady = readyOverride ?? !!(meP && meP.ready);
  const rdy = (p) => (p.uid === myId ? amReady : !!p.ready);
  const allReady = team ? active.length === 4 && active.filter((p) => p.teamId === "A").length === 2 && active.filter((p) => p.teamId === "B").length === 2 && active.every(rdy) : active.length >= 2 && active.every(rdy);
  const teamHint = !team ? null : active.length < 4 ? "online_team_needs_players" : active.filter((p) => p.teamId === "A").length !== 2 || active.filter((p) => p.teamId === "B").length !== 2 ? "online_team_unbalanced" : null;
  const bubbles = activeBubbles(chatNow());
  if (!$("ow-dock").firstChild) $("ow-dock").append(buildDock(chatCtx(c), code, () => room));

  // ---- seats ----
  const seat = (p) => {
    const isMe = p.uid === myId, host = p.uid === room.hostUid, bub = bubbles[p.uid], ready = rdy(p);
    const card = el("div", "ow-card" + (ready ? " rdy" : "") + (bub || p.pendingNextRound ? " st" : ""));
    const av = frameRing(frameById(p.frameId), "ow-av"); av.style.position = "relative";
    const nm = isMe ? S("online_you_label", p.displayName) : p.displayName, lv = p.level || 1;
    const status = bub ? esc(messageOf(bub, lg)) : p.pendingNextRound ? S("online_pending_badge") : "";
    const txt = el("div", "ow-tx", `<div class="ow-n"><span class="fitw">${esc(nm)}</span>${host ? '<img src="img/lobby_crown.webp" alt="">' : ""}</div><div class="ow-rk"><i>${S(rankKey(lv))}</i><i>${S("home_level_inline", lv)}</i></div><div class="ow-s">${status}</div>`);
    card.append(av, txt);
    if (team && isMe) { const sw = el("button", "ow-ico", SWAP); sw.title = S("online_switch_team"); sw.onclick = (e) => { e.stopPropagation(); net.setTeam(code, p.teamId === "B" ? "A" : "B").catch(() => {}); }; card.append(sw); }
    if (isHost && !isMe && p.uid !== "karalak-bot") {
      const kb = el("button", "ow-ico", KICK); kb.title = S("online_kick_player");
      kb.onclick = (e) => { e.stopPropagation(); c.confirm(S("online_kick_confirm_message", p.displayName), S("online_kick_confirm_confirm"), () => net.kickPlayer(code, p.uid, p.displayName).catch(() => {})); };
      card.append(kb);
    }
    if (!isMe && !myFriends.some((f) => f.id === p.uid)) card.onclick = () => c.confirm(S("online_friend_add_message", p.displayName), S("online_friend_add_confirm"), () => net.sendFriendRequestTo(p.uid, p.displayName, c.P.nick).catch(() => {}));
    return card;
  };
  const empty = () => { const e = el("div", "ow-slot", `<img src="img/lobby_adduser.webp" alt=""><span>${S("online_invite_friend_slot")}</span>`); e.onclick = () => inviteSheet(c); return e; };
  const main = $("ow-main"); main.replaceChildren();
  // code card
  const cp = el("div", "ow-cp");
  cp.innerHTML = `<div class="h">${S("online_room_code_hint")}</div><div class="n">${code}</div><div class="b"><span>${S("online_invite_friend")}</span></div>`;
  cp.querySelector(".b").onclick = async () => {
    const t = `${S("share_room_invite")} ${S("share_room_code", code)} — https://karalak-b6e11.web.app/oyna/`;
    try { if (navigator.share) await navigator.share({ text: t }); else { await navigator.clipboard.writeText(code); say(c, lg === "tr" ? "Kod kopyalandı" : "Code copied"); } } catch {}
  };
  main.append(cp);
  if (amPending) main.append(el("div", "ow-note", S("online_pending_next_round_message")));
  main.append(el("div", "ow-head", `<div class="band"><span>${S("online_players_section_title")}</span></div><div class="owp">${PERSON}<span>${S("online_room_occupancy", presentCount, team ? 4 : 8)}</span></div>`));
  const mine = meP ? [seat(meP)] : [], theirs = others.map(seat);
  if (team) {
    const cols = el("div", "ow-teams");
    for (const id of ["A", "B"]) {
      const col = el("div", "ow-col", `<div class="owp t"><span>${S(id === "A" ? "online_team_a" : "online_team_b")}</span></div>`), mem = [...(meP ? [meP] : []), ...others].filter((p) => p.teamId === id);
      mem.forEach((p) => col.append(p.uid === myId ? mine[0] : theirs[others.indexOf(p)]));
      for (let i = mem.length; i < 2; i++) col.append(empty());
      cols.append(col);
    }
    main.append(cols);
  } else {
    // the room's founder always takes the first seat (top left), whoever is looking
    const order = [...(meP ? [meP] : []), ...others], seats = [...mine, ...theirs], cells = order.map((p, i) => ({ p, e: seats[i] })).sort((a, b) => (b.p.uid === room.hostUid) - (a.p.uid === room.hostUid)).map((x) => x.e); while (cells.length < 8) cells.push(empty());
    const grid = el("div", "ow-grid"); cells.forEach((x) => grid.append(x)); main.append(grid);
  }
  const kicked = Object.entries(room.kickedUsers || {}).filter(([, k]) => k && k.until > now);
  if (isHost && kicked.length) {
    main.append(el("div", "ow-sec", S("online_kicked_users_section_title")));
    kicked.forEach(([uid, k]) => { const r = el("div", "ow-kk", `<span>${esc(S("online_kicked_user_remaining_format", k.displayName || "?", Math.max(1, Math.ceil((k.until - now) / 60000))))}</span>`), u = el("button", "fr-pill", S("online_unban_player")); u.onclick = () => net.unbanPlayer(code, uid).catch(() => {}); r.append(u); main.append(r); });
  }
  // ---- waiting pill, hints and the one button ----
  $("ow-wait").style.display = others.length ? "none" : "";
  $("ow-wait").innerHTML = `<img src="img/lobby_hourglass.webp" alt=""><span>${S("online_waiting_for_friend")}</span>`;
  const counting = !!room.countdownStartedAt, left = counting ? 3 - Math.floor((now - room.countdownStartedAt) / 1000) : 0;
  const hostCanStart = allReady && isHost && !counting && !starting;
  const hints = $("ow-hints"); hints.replaceChildren();
  if (notice) hints.append(el("div", "ow-hint", esc(notice)));
  if (!amPending && others.length) {
    if (!allReady && teamHint) hints.append(el("div", "ow-hint", S(teamHint)));
    if (allReady && !isHost && !counting && !starting) hints.append(el("div", "ow-hint", `${S("online_everyone_ready")} ${S("online_waiting_for_host_sub")}`));
  }
  const btn = $("ow-ready"); btn.style.display = !amPending && others.length ? "" : "none";
  btn.className = "ow-rb" + (amReady || counting ? " g" : "") + (counting ? " cd" : "") + (!(amReady || counting) ? " blink" : "");
  $("ow-ready-t").textContent = counting ? (left > 0 ? left : S("online_game_starting").replace(/!$/, "")) : hostCanStart ? S("online_start_game") : S("online_ready");
  btn.onclick = () => {
    if (counting || starting) return;
    if (hostCanStart) { starting = true; renderLobby(c); hostStart(c).finally(() => { starting = false; }); return; }
    readyOverride = !amReady; renderLobby(c); net.setReady(code, readyOverride).catch(() => {}).finally(() => { setTimeout(() => { readyOverride = null; }, 1500); });
  };
  requestAnimationFrame(() => c.fit && c.fit()); clearInterval(cdTimer);
  if (counting) cdTimer = setInterval(() => { if (c.current() !== "onw") return clearInterval(cdTimer); renderLobby(c); }, 400);
}

async function hostStart(c) {
  const r = room, ids = c.pickRoomWords(r);
  if (!ids.length) return;
  try { await net.startCountdown(code); await new Promise((res) => setTimeout(res, 3000)); await net.startGame(code, ids); }
  catch { net.cancelCountdown(code).catch(() => {}); }
}

function inviteSheet(c) {
  const { $, lang, P } = c, x = T[lang()], d = $("ow-sheet"); d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "ow-sbox", `<b>${S("online_invite_sheet_title")}</b>`);
  if (!myFriends.length) box.append(el("p", "", S("online_invite_sheet_empty")));
  myFriends.forEach((f) => {
    const row = el("div", "fr-row", `<span class="nm">${esc(f.nickname || "?")}</span>`);
    const b = el("button", "fr-pill", sent.has(f.id) ? S("online_invite_sent_short") : S("online_invite_send"));
    b.onclick = async () => { try { await net.sendInvite(f.id, code, P.nick); sent.add(f.id); b.textContent = S("online_invite_sent_short"); } catch { b.textContent = "✗"; } };
    row.append(b); box.append(row);
  });
  const share = el("button", "pill cream small", `<span>${x.share}</span>`);
  share.onclick = async () => { const t = `${lang() === "tr" ? "Karalak odama gel! Kod" : "Join my Karalak room! Code"}: ${code} — https://karalak-b6e11.web.app/oyna/`; try { if (navigator.share) await navigator.share({ text: t }); else await navigator.clipboard.writeText(code); } catch {} };
  const close = el("button", "pill cream small", `<span>${S("close")}</span>`); close.onclick = () => d.classList.add("hidden");
  box.append(share, close); d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}

// ---------- after the game ----------
let myScore = 0;
/** The "Bitirdin!" scene while the others are still drawing: your score, everyone's tick or hourglass, and how many are done. */
export function renderFinished(c) {
  const { $ } = c; $("of-title").textContent = S("online_finished_title");
  const card = $("of-card"), all = Object.entries((room && room.players) || {}).map(([uid, p]) => ({ uid, ...p })).filter((p) => !p.left && !p.pendingNextRound);
  all.sort((a, b) => (b.uid === myId) - (a.uid === myId));
  const me = all.find((p) => p.uid === myId);
  const done = all.filter((p) => p.uid === myId || p.finished).length;
  card.replaceChildren();
  const rows = all.map((p) => {
    const row = el("div", "of-row"), av = frameRing(frameById(p.frameId), "ow-av");
    row.append(av, el("span", "of-nm fitw", esc(p.uid === myId ? S("online_you_label", p.displayName) : p.displayName)));
    const ok = p.uid === myId || p.finished;
    row.append(el("i", "of-st" + (ok ? " ok" : ""), ok ? "✓" : "⏳"));
    return row;
  });
  card.append(el("div", "of-score", String(me && me.totalScore != null ? me.totalScore : myScore)), el("div", "of-lbl", S("online_score_label")), ...rows,
    el("div", "of-wait", S("online_waiting_for_opponent_result")), el("div", "of-cnt", S("online_players_done", done, all.length)));
}

export async function submitAndWait(c, round, score) {
  const x = T[c.lang()], ok = round.filter((w) => w.correct).length, times = round.filter((w) => w.correct).map((w) => w.ms);
  const items = round.map((w) => ({ word: w.text, isCorrect: w.correct, strokes: w.strokes, penSkin: c.P.pen }));
  myScore = score; c.show("onfin");
  try { await net.submitRoomResult(code, score, ok, round.length - ok, times.length ? Math.min(...times) : null, items); }
  catch { /* the room listener below still ends the wait if others finish */ }
  finishedLocal = true;
  if (room && room.status === "FINISHED") renderOnlineResult(c);
}

let olSel = null, olItems = {}, olRequested = false, olFirst = true;
async function renderOnlineResult(c, again) {
  const { $, lang } = c;
  if (!room) return;
  if (!again) { if (!finishedLocal) return; finishedLocal = false; olSel = null; olItems = {}; olRequested = false; olFirst = true; }
  await Promise.all([loadResultStrings(), loadChat()]);
  const cc = chatCtx(c);
  const roundPlayers = Object.entries(room.players || {}).map(([uid, p]) => ({ uid, ...p })).filter((p) => !p.left && !p.pendingNextRound).sort((a, b) => (a.uid === myId ? -1 : 0) - (b.uid === myId ? -1 : 0));
  const left = Object.entries(room.players || {}).map(([uid, p]) => ({ uid, ...p })).filter((p) => p.left && !p.pendingNextRound && p.uid !== myId);
  if (!roundPlayers.find((p) => p.uid === myId)) return;
  // everyone's drawings load in the background and fill the board as they arrive
  for (const p of roundPlayers) if (!olItems[p.uid]) { olItems[p.uid] = []; net.roomResultItems(code, p.uid).then((it) => { olItems[p.uid] = it || []; if (c.current() === "res2" && c.kind() === "online") renderOnlineResult(c, true); }); }
  const votes = room.rematchVotes || [];
  if (olFirst) { olFirst = false; c.show("res2"); c.sfx && c.sfx.over(); c.grantOnline && c.grantOnline(roundPlayers, room.hostUid, myId); }
  const mine = roundPlayers.find((p) => p.uid === myId);
  // everybody voted: the room goes back to its lobby
  if (olRequested && roundPlayers.every((p) => votes.includes(p.uid))) { net.returnToWaiting(code).catch(() => {}); }
  drawOnlineResult(cc, {
    players: roundPlayers, left, myUid: myId, xp: c.lastXp || 0, gold: c.lastGold || 0, pen: c.P.pen, wordCount: room.wordCount, sel: olSel,
    votes, requested: olRequested, blocked: false, ring: cc.ring, itemsFor: (u) => olItems[u] || [], select: (u) => { olSel = u; },
    dock: () => { const w = document.createElement("div"); w.className = "cd-wrap1"; w.append(buildDock(cc, code, () => room)); return w; },
    teamMode: !!room.teamMode, onHome: () => { leaveLobby(c); const g = c.lastGold || 0, x = c.lastXp || 0; c.lastGold = 0; c.lastXp = 0; setTimeout(() => c.homeGain && c.homeGain(g, x), 60); },
    onRematch: async () => { olRequested = true; try { await net.voteRematch(code); } catch {} renderOnlineResult(c, true); },
  });
}
export const roomCode = () => code;
export const roomNow = () => room;

// Invite a friend straight from the friends list: opens a room if there is none, then sends the invite.
export async function quickInvite(c, f) {
  try {
    if (!code) { const cd = await net.createRoom(c.P.nick, c.levelOf(c.P.xp).level, c.P.frame, { wordCount: 10, difficulty: null, mode: "NORMAL", category: null }); await enterLobby(c, cd); }
    await net.sendInvite(f.id, code, c.P.nick); sent.add(f.id);
  } catch {}
}
