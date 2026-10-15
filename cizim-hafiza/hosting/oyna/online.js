// Race a Friend: entry, create/join, live lobby, synchronized game start and the ranking screen.
import * as net from "./firebase.js";
import { frameRing, frameById } from "./store.js";
import { loadChat, buildDock, watch as watchChat, stop as stopChat, onReactions, activeBubbles, messageOf, reactions as chatNow, chatRow, chatText } from "./chat.js";
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
    place($("on-back"), 20, 40, 110, 106); place($("on-sign"), 100, 110, 640, 536); place($("on-title"), 190, 480, 460, 120);
    place($("on-paper"), 70, 700, 700, 334); place($("on-text"), 110, 735, 620, 270);
    place($("on-create"), 70, 1160, 700, 164); place($("on-join"), 70, 1380, 700, 164);
  } else if (cur === "onj") {
    place($("oj-back"), 20, 40, 110, 106); place($("oj-title"), 160, 310, 520, 100);
    place($("oj-panel"), 60, 560, 720, 497);
    place($("oj-nick"), 162, 683, 506, 80); place($("oj-code"), 162, 820, 506, 80); place($("oj-msg"), 60, 960, 720, 70);
    place($("oj-go"), 120, 1110, 600, 190);
  } else if (cur === "onc") {
    place($("oc-back"), 20, 40, 110, 106); place($("oc-title"), 160, 130, 520, 110);
    place($("oc-paper"), 50, 270, 740, 1080 * 0 + 960); place($("oc-body"), 110, 340, 620, 820);
    place($("oc-make"), 110, 1330, 620, 150); place($("oc-msg"), 60, 1500, 720, 70);
  } else if (cur === "onw") {
    place($("ow-back"), 20, 40, 110, 106); place($("ow-title"), 160, 60, 520, 90);
    place($("ow-codep"), 40, 190, 760, 370); place($("ow-code"), 40, 262, 760, 100); place($("ow-codeh"), 100, 200, 640, 56);
    place($("ow-occ"), 60, 395, 720, 50);
    const l = $("ow-list"); l.style.left = 60 * c.u() + "px"; l.style.top = 505 * c.u() + "px"; l.style.width = 720 * c.u() + "px"; l.style.height = 670 * c.u() + "px";
    place($("ow-info"), 60, 1195, 720, 50);
    place($("ow-ready"), 70, 1240, 330, 125); place($("ow-start"), 430, 1240, 340, 125); place($("ow-invite"), 70, 1375, 700, 100);
    place($("ow-dock"), 60, 1490, 720, 374);
    place($("ow-cd"), 220, 700, 400, 400);
  }
}

// ---------- entry / create / join ----------
export function renderEntry(c) {
  const { $, lang } = c, x = T[lang()];
  $("on-title").textContent = x.title; $("on-create-t").textContent = x.create; $("on-join-t").textContent = x.join;
  $("on-text").innerHTML = `<span>${x.l1}</span><span>${x.l2}</span><b>${x.l3} ${x.l4}</b>`;
}
export async function renderCreate(c) {
  const { $, lang } = c, x = T[lang()], C = (k) => chatText(k, lang());
  await loadChat();
  if (!catList.length || catList.lang !== lang()) { try { const w = await (await fetch(lang() === "tr" ? "words_tr.json" : "words_en.json")).json(); catList = [...new Set(w.map((q) => q.category))].sort(); catList.lang = lang(); opts.cats = new Set(); } catch { catList = []; } }
  $("oc-title").textContent = x.cTitle; $("oc-make-t").textContent = x.make; $("oc-msg").textContent = "";
  const body = $("oc-body"); body.replaceChildren();
  const group = (label, items) => { body.append(el("div", "oc-l", label)); const r = el("div", "oc-row"); items.forEach((i) => r.append(i)); body.append(r); };
  const pill = (txt, on, fn) => { const b = el("button", "oc-pill" + (on ? " on" : ""), txt); b.onclick = fn; return b; };
  group(x.words, [10, 20, 30, 40, 50].map((n) => pill(n, opts.wordCount === n, () => { opts.wordCount = n; renderCreate(c); })));
  group(C("online_room_mode_title"), [[false, C("online_room_mode_free_for_all")], [true, C("online_room_mode_team")]].map(([k, t]) => pill(t, opts.team === k, () => { opts.team = k; renderCreate(c); })));
  group(C("select_category"), [...catList.map((k) => pill(`${CAT_EMOJI[k] || "✨"} ${k}`, opts.cats.has(k), () => { opts.cats.has(k) ? opts.cats.delete(k) : opts.cats.add(k); renderCreate(c); })), pill(`🎨 ${C("all_categories")}`, opts.cats.size === 0, () => { opts.cats = new Set(); renderCreate(c); })]);
  group(x.diff, [["EASY", x.easy], ["MEDIUM", x.med], ["HARD", x.hard]].map(([k, t]) => pill(t, opts.diffs.has(k), () => { opts.diffs.has(k) ? (opts.diffs.size > 1 && opts.diffs.delete(k)) : opts.diffs.add(k); renderCreate(c); })).concat([pill(C("all_difficulties"), opts.diffs.size === 3, () => { opts.diffs = new Set(["EASY", "MEDIUM", "HARD"]); renderCreate(c); })]));
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
  $("oj-title").textContent = x.jTitle; $("oj-go-t").textContent = x.go; $("oj-msg").textContent = "";
  $("oj-nick").placeholder = x.nick; $("oj-nick").value = P.nick; $("oj-code").placeholder = x.code;
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
  } else if (c.current() === "res2" && c.kind() === "online" && r.status === "FINISHED") renderOnlineResult(c, true);
  else if (c.current() === "search" && c.kind() === "online" && r.status === "FINISHED") renderOnlineResult(c);
  else if (r.status === "WAITING" && started && c.kind() === "online" && c.current() === "res2") { /* host sent everyone back */ started = false; c.show("onw"); }
}
let myId = null, chatUnsub = null, bubbleT = null;
const chatCtx = (c) => Object.assign(Object.create(c), { myUid: () => myId, ring: (f) => { const r = frameRing(frameById(f), "ow-av"); r.style.cssText += ";position:relative;width:100%;height:100%"; return r; }, chatText });
net.myUid().then((u) => { myId = u; }).catch(() => {});

function renderLobby(c) {
  const { $, lang } = c, x = T[lang()];
  if (!room) return;
  net.myUid().then((u) => { myId = u; });
  $("ow-title").textContent = x.lobby; $("ow-code").textContent = code.split("").join(" "); $("ow-codeh").textContent = x.roomCode; $("ow-info").textContent = "";
  const now = Date.now(), players = Object.entries(room.players || {}).map(([uid, p]) => ({ uid, ...p })).filter((p) => !p.left).sort((a, b) => (a.joinedAt || 0) - (b.joinedAt || 0));
  const presentPlayers = players.filter((p) => present(p, now));
  const team = !!room.teamMode; $("ow-occ").textContent = x.occ(presentPlayers.length, team ? 4 : 8);
  const list = $("ow-list"); list.replaceChildren();
  const bubbles = activeBubbles(chatNow());
  if (!$("ow-dock").firstChild) $("ow-dock").append(buildDock(chatCtx(c), code, () => room));
  players.forEach((p) => {
    const isMe = p.uid === myId, isHost = p.uid === room.hostUid;
    const row = el("div", "ow-row" + (present(p, now) ? "" : " away") + (p.ready || isHost ? " ready" : ""));
    const av = frameRing(frameById(p.frameId), "ow-av"); av.style.position = "relative";
    const bub = bubbles[p.uid]; if (bub) row.append(el("div", "bubble", esc(messageOf(bub, lang()))));
    row.append(av, el("div", "ow-nm", `${isHost ? "👑 " : ""}${esc(isMe ? x.you(p.displayName) : p.displayName)}<small>Lv ${p.level || 1}${p.pendingNextRound ? " · " + x.pending : ""}</small>`));
    if (team) { const tb = el("button", "tm-badge " + (p.teamId === "B" ? "b" : "a"), chatText(p.teamId === "B" ? "online_team_b" : "online_team_a", lang())); if (isMe) { tb.title = chatText("online_switch_team", lang()); tb.onclick = () => net.setTeam(code, p.teamId === "B" ? "A" : "B").catch(() => {}); } else tb.disabled = true; row.append(tb); }
    row.append(el("div", "ow-st", present(p, now) ? (p.ready || isHost ? "✓" : "⏳") : "…"));
    if (room.hostUid === myId && !isMe) { const kb = el("button", "kick", "⛔"); kb.title = chatText("online_kick_player", lang()); kb.onclick = () => c.confirm(chatText("online_kick_confirm_message", lang(), p.displayName), chatText("online_kick_confirm_confirm", lang()), () => net.kickPlayer(code, p.uid, p.displayName).catch(() => {})); row.append(kb); }
    list.append(row);
  });
  const kicked = Object.entries(room.kickedUsers || {}).filter(([, k]) => k && k.until > now);
  if (room.hostUid === myId && kicked.length) {
    list.append(el("div", "ow-sec", chatText("online_kicked_users_section_title", lang())));
    kicked.forEach(([uid, k]) => { const r = el("div", "ow-row away kicked", `<div class="ow-nm">${esc(chatText("online_kicked_user_remaining_format", lang(), k.displayName || "?", Math.max(1, Math.ceil((k.until - now) / 60000))))}</div>`); const ub = el("button", "fr-pill", chatText("online_unban_player", lang())); ub.onclick = () => net.unbanPlayer(code, uid).catch(() => {}); r.append(ub); list.append(r); });
  }
  const me = room.players[myId], isHost = room.hostUid === myId;
  const active = presentPlayers.filter((p) => !p.pendingNextRound), everyoneReady = presentPlayers.filter((p) => p.uid !== room.hostUid).every((p) => p.ready);
  const teamOk = !team || (active.length === 4 && active.filter((p) => p.teamId === "A").length === 2 && active.filter((p) => p.teamId === "B").length === 2);
  $("ow-ready").style.display = isHost ? "none" : ""; $("ow-ready-t").textContent = me && me.ready ? x.notReady : x.ready;
  $("ow-ready").onclick = () => net.setReady(code, !(me && me.ready)).catch(() => {});
  $("ow-start").style.display = isHost ? "" : "none"; $("ow-start-t").textContent = x.start;
  const canStart = isHost && presentPlayers.length >= 2 && everyoneReady && teamOk && !room.countdownStartedAt;
  $("ow-start").classList.toggle("dis", !canStart);
  $("ow-start").onclick = () => { if (canStart) hostStart(c); };
  $("ow-info").textContent = team && !teamOk ? chatText(active.length < 4 ? "online_team_needs_players" : "online_team_unbalanced", lang()) : isHost ? (presentPlayers.length < 2 ? x.needTwo : "") : (everyoneReady && presentPlayers.length >= 2 ? x.waitHost : "");
  $("ow-invite-t").textContent = x.inviteF; $("ow-invite").onclick = () => inviteSheet(c);
  // 3-2-1 shown on every lobby while the host's countdown runs
  clearInterval(cdTimer);
  if (room.countdownStartedAt) {
    const tick = () => { const left = 3 - Math.floor((Date.now() - room.countdownStartedAt) / 1000); $("ow-cd").textContent = left > 0 ? left : x.starting; $("ow-cd").classList.remove("hidden"); };
    tick(); cdTimer = setInterval(tick, 250);
  } else $("ow-cd").classList.add("hidden");
}

async function hostStart(c) {
  const r = room, ids = c.pickRoomWords(r);
  if (!ids.length) return;
  try { await net.startCountdown(code); await new Promise((res) => setTimeout(res, 3000)); await net.startGame(code, ids); }
  catch { net.cancelCountdown(code).catch(() => {}); }
}

function inviteSheet(c) {
  const { $, lang, P } = c, x = T[lang()], d = $("ow-sheet"); d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "ow-sbox", `<b>${x.inviteT}</b>`);
  if (!myFriends.length) box.append(el("p", "", x.inviteE));
  myFriends.forEach((f) => {
    const row = el("div", "fr-row", `<span class="nm">${esc(f.nickname || "?")}</span>`);
    const b = el("button", "fr-pill", sent.has(f.id) ? x.invSent : x.invSend);
    b.onclick = async () => { try { await net.sendInvite(f.id, code, P.nick); sent.add(f.id); b.textContent = x.invSent; } catch { b.textContent = "✗"; } };
    row.append(b); box.append(row);
  });
  const share = el("button", "pill cream small", `<span>${x.share}</span>`);
  share.onclick = async () => { const t = `${lang() === "tr" ? "Karalak odama gel! Kod" : "Join my Karalak room! Code"}: ${code} — https://karalak-b6e11.web.app/oyna/`; try { if (navigator.share) await navigator.share({ text: t }); else await navigator.clipboard.writeText(code); } catch {} };
  const close = el("button", "pill cream small", `<span>${x.leave === "" ? "" : (lang() === "tr" ? "Kapat" : "Close")}</span>`); close.onclick = () => d.classList.add("hidden");
  box.append(share, close); d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}

// ---------- after the game ----------
export async function submitAndWait(c, round, score) {
  const x = T[c.lang()], ok = round.filter((w) => w.correct).length, times = round.filter((w) => w.correct).map((w) => w.ms);
  const items = round.map((w) => ({ word: w.text, isCorrect: w.correct, strokes: w.strokes, penSkin: c.P.pen }));
  c.show("search"); c.$("s-text").textContent = x.done(score); c.$("s-sub").textContent = x.waiting;
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
    teamMode: !!room.teamMode, onHome: () => leaveLobby(c),
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
