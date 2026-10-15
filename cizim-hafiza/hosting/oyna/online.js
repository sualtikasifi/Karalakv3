// Race a Friend: entry, create/join, live lobby, synchronized game start and the ranking screen.
import * as net from "./firebase.js";
import { frameRing, frameById } from "./store.js";

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
let opts = { wordCount: 10, diffs: new Set(["EASY", "MEDIUM", "HARD"]), mode: "NORMAL" };
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
    const l = $("ow-list"); l.style.left = 60 * c.u() + "px"; l.style.top = 505 * c.u() + "px"; l.style.width = 720 * c.u() + "px"; l.style.height = 700 * c.u() + "px";
    place($("ow-info"), 60, 1220, 720, 70);
    place($("ow-ready"), 70, 1300, 330, 150); place($("ow-start"), 430, 1300, 340, 150); place($("ow-invite"), 70, 1480, 700, 120);
    place($("ow-cd"), 220, 700, 400, 400);
  }
}

// ---------- entry / create / join ----------
export function renderEntry(c) {
  const { $, lang } = c, x = T[lang()];
  $("on-title").textContent = x.title; $("on-create-t").textContent = x.create; $("on-join-t").textContent = x.join;
  $("on-text").innerHTML = `<span>${x.l1}</span><span>${x.l2}</span><b>${x.l3} ${x.l4}</b>`;
}
export function renderCreate(c) {
  const { $, lang } = c, x = T[lang()];
  $("oc-title").textContent = x.cTitle; $("oc-make-t").textContent = x.make; $("oc-msg").textContent = "";
  const body = $("oc-body"); body.replaceChildren();
  const group = (label, items) => { body.append(el("div", "oc-l", label)); const r = el("div", "oc-row"); items.forEach((i) => r.append(i)); body.append(r); };
  const pill = (txt, on, fn) => { const b = el("button", "oc-pill" + (on ? " on" : ""), txt); b.onclick = fn; return b; };
  group(x.words, [10, 20, 30, 40, 50].map((n) => pill(n, opts.wordCount === n, () => { opts.wordCount = n; renderCreate(c); })));
  group(x.diff, [["EASY", x.easy], ["MEDIUM", x.med], ["HARD", x.hard]].map(([k, t]) => pill(t, opts.diffs.has(k), () => { opts.diffs.has(k) ? (opts.diffs.size > 1 && opts.diffs.delete(k)) : opts.diffs.add(k); renderCreate(c); })));
  group(x.mode, [["NORMAL", x.normal], ["RELAXED", x.relaxed]].map(([k, t]) => pill(t, opts.mode === k, () => { opts.mode = k; renderCreate(c); })));
  $("oc-make").onclick = async () => {
    $("oc-msg").textContent = "…";
    try {
      const lv = c.levelOf(c.P.xp).level;
      const all = opts.diffs.size === 3 ? null : [...opts.diffs].sort((a, b) => ["EASY", "MEDIUM", "HARD"].indexOf(a) - ["EASY", "MEDIUM", "HARD"].indexOf(b)).join(",");
      const cd = await net.createRoom(c.P.nick, lv, c.P.frame, { wordCount: opts.wordCount, difficulty: all, mode: opts.mode, category: null });
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
  clearInterval(hb); const beat = () => net.touchPresence(cd, c.levelOf(c.P.xp).level, c.P.frame).catch(() => {}); beat(); hb = setInterval(beat, 60000);
  net.listen("friends", (l) => { myFriends = l; }).catch(() => {});
}
export function leaveLobby(c, goHome = true) {
  clearInterval(hb); clearInterval(cdTimer); if (unsub) { try { unsub(); } catch {} unsub = null; }
  if (code) net.leaveRoom(code).catch(() => {});
  code = null; room = null; if (goHome) c.show("home");
}
const present = (p, now = Date.now()) => !p.left && now - (p.lastSeenAt || p.joinedAt || 0) <= PRESENCE;
function onRoom(c, r) {
  room = r;
  if (!r) return;
  if (c.current() === "onw") {
    if (r.status === "PLAYING" && !started && !(r.players[myId]?.pendingNextRound)) { started = true; clearInterval(cdTimer); c.startOnlineGame(r, code); return; }
    renderLobby(c);
  } else if (c.current() === "result" && c.kind() === "online" && r.status === "FINISHED") renderOnlineResult(c);
  else if (c.current() === "search" && c.kind() === "online" && r.status === "FINISHED") renderOnlineResult(c);
  else if (r.status === "WAITING" && started && c.kind() === "online" && c.current() === "result") { /* host sent everyone back */ started = false; c.show("onw"); }
}
let myId = null;
net.myUid().then((u) => { myId = u; }).catch(() => {});

function renderLobby(c) {
  const { $, lang } = c, x = T[lang()];
  if (!room) return;
  net.myUid().then((u) => { myId = u; });
  $("ow-title").textContent = x.lobby; $("ow-code").textContent = code.split("").join(" "); $("ow-codeh").textContent = x.roomCode; $("ow-info").textContent = "";
  const now = Date.now(), players = Object.entries(room.players || {}).map(([uid, p]) => ({ uid, ...p })).filter((p) => !p.left).sort((a, b) => (a.joinedAt || 0) - (b.joinedAt || 0));
  const presentPlayers = players.filter((p) => present(p, now));
  $("ow-occ").textContent = x.occ(presentPlayers.length, 8);
  const list = $("ow-list"); list.replaceChildren();
  players.forEach((p) => {
    const isMe = p.uid === myId, isHost = p.uid === room.hostUid;
    const row = el("div", "ow-row" + (present(p, now) ? "" : " away") + (p.ready || isHost ? " ready" : ""));
    const av = frameRing(frameById(p.frameId), "ow-av"); av.style.position = "relative";
    row.append(av, el("div", "ow-nm", `${isHost ? "👑 " : ""}${esc(isMe ? x.you(p.displayName) : p.displayName)}<small>Lv ${p.level || 1}${p.pendingNextRound ? " · " + x.pending : ""}</small>`), el("div", "ow-st", present(p, now) ? (p.ready || isHost ? "✓" : "⏳") : "…"));
    list.append(row);
  });
  const me = room.players[myId], isHost = room.hostUid === myId;
  const everyoneReady = presentPlayers.filter((p) => p.uid !== room.hostUid).every((p) => p.ready);
  $("ow-ready").style.display = isHost ? "none" : ""; $("ow-ready-t").textContent = me && me.ready ? x.notReady : x.ready;
  $("ow-ready").onclick = () => net.setReady(code, !(me && me.ready)).catch(() => {});
  $("ow-start").style.display = isHost ? "" : "none"; $("ow-start-t").textContent = x.start;
  const canStart = isHost && presentPlayers.length >= 2 && everyoneReady && !room.countdownStartedAt;
  $("ow-start").classList.toggle("dis", !canStart);
  $("ow-start").onclick = () => { if (canStart) hostStart(c); };
  $("ow-info").textContent = isHost ? (presentPlayers.length < 2 ? x.needTwo : "") : (everyoneReady && presentPlayers.length >= 2 ? x.waitHost : "");
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

async function renderOnlineResult(c) {
  const { $, lang } = c, x = T[lang()];
  if (!room || !finishedLocal) return;
  finishedLocal = false;
  c.show("result");
  const ps = Object.entries(room.players || {}).map(([uid, p]) => ({ uid, ...p })).filter((p) => !p.left && !p.pendingNextRound).sort((a, b) => b.totalScore - a.totalScore || (a.fastestCorrectMs ?? 1e9) - (b.fastestCorrectMs ?? 1e9));
  const place = ps.findIndex((p) => p.uid === myId) + 1;
  $("r-title").textContent = place ? x.place(place) : x.lobby;
  $("r-vs").textContent = "";
  const list = $("r-list"); list.replaceChildren();
  const h = el("div", "who", x.drawings); list.append(h);
  const detail = el("div", "", ""); 
  ps.forEach((p, i) => {
    const row = el("button", "row-card ro", ""); row.style.width = "100%";
    const av = frameRing(frameById(p.frameId), "ow-av"); av.style.cssText += ";position:relative;width:calc(var(--u)*110);height:calc(var(--u)*110);flex:none";
    row.append(el("span", "ro-rk", i + 1 + "."), av, el("div", "lbl", `${esc(p.uid === myId ? x.you(p.displayName) : p.displayName)}<small>${x.ofTotal(p.correctCount, p.correctCount + p.wrongCount)}</small>`), el("b", "ro-sc", `${p.totalScore}`));
    row.onclick = async () => { detail.replaceChildren(); const items = await net.roomResultItems(code, p.uid); items.forEach((it) => c.addRow(detail, it.strokes, it.isCorrect, it.word, "", it.penSkin)); };
    list.append(row);
  });
  list.append(detail);
  const mine = ps.find((p) => p.uid === myId);
  if (mine) { const items = await net.roomResultItems(code, myId); items.forEach((it) => c.addRow(detail, it.strokes, it.isCorrect, it.word, "", it.penSkin)); }
  c.$("again-t").textContent = x.lobbyBack;
  c.$("again").onclick = async () => { try { await net.returnToWaiting(code); } catch {} started = false; c.show("onw"); renderLobby(c); };
  c.$("r-back").onclick = () => leaveLobby(c);
  if (c.winRewards) c.winRewards(place === 1);
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
