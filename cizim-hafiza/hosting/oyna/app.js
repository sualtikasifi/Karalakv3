import { isCorrect } from "./matcher.js";
import * as net from "./firebase.js";
import { checkStreak } from "./streak.js";
import { renderResult, loadResultStrings } from "./result.js";
import { loadChat, chatText } from "./chat.js";
import { loadRunes, layoutRunes, renderRunes, effect as runeFx, has as runeHas, applyRuneConfig, anyAffordable } from "./runes.js";
import * as eco from "./economy.js";
import { layoutReport, renderReport } from "./report.js";
import { layoutChests, renderChests, award as awardChest, wonDialog } from "./chests.js";
import { todayEpoch, msToMidnight, dailyWords, state as dailyState, record as dailyRecord } from "./daily.js";
import { drawFxStroke } from "./penfx.js";
import { openPenPicker, openFramePicker, openNickEdit } from "./picker.js";
import { setPrefs, sfx, vibrate, initAudio, syncMusic } from "./audio.js";
import { layoutOnline, renderEntry, renderCreate, renderJoin, leaveLobby, submitAndWait, joinWithCode, quickInvite } from "./online.js";
import { openDuels, stopDuels, renderDuels, openCreateDuel, recordReceived as duelReceived } from "./duel.js";
import { layoutFriends, openFriends, renderFriends, stop as stopFriends } from "./friends.js";
import { loadWorlds, layoutWorlds, renderWorlds, layoutMap, renderMap, mixFor, starsFor, recordLevel, currentWorld } from "./levels.js";
import { layoutAccount, renderAccount } from "./account.js";
import { layoutOffline, renderOffline } from "./offline.js";
import { setLang, loadStrings, S } from "./i18n.js";
import { syncNow, scheduleBackup } from "./sync.js";
import { openRankLadder } from "./ranks.js";
import { startSearch, stopSearch } from "./search.js";
import { exitDialog, appOnlyDialog, PLAY_URL } from "./ui.js";
import { makeGhostRun, loadGhostNames, isGhostId } from "./ghost.js";
import { layoutBreak, startBreak, stopBreak } from "./brk.js";
import { loadLeague, layoutLeague, renderLeague, periodId } from "./league.js";
import { loadAchievements, layoutAchievements, renderAchievements, recordRound as achRound, unlockedCount as achCount } from "./achievements.js";
import { layoutSettings, renderSettings } from "./settings.js";
import { loadStoreData, layoutStore, renderStore, frameRing, frameById, penById, frameList as frameListSafe } from "./store.js";

const WORD_COUNT = 10, POINTS = 5, SPEED_BONUS = 2, SPEED_MS = 3000;
const DRAW_SECONDS = { EASY: 5, MEDIUM: 7, HARD: 10 };
const GUESS_BASE = { EASY: 8, MEDIUM: 10, HARD: 12 };
const guessSeconds = (w) => Math.min(13, (GUESS_BASE[w.difficulty] ?? 10) + Math.max(0, [...w.text].filter((c) => /\p{L}/u.test(c)).length - 10));
const HOME_SAFE_TOP = 36, HOME_SAFE_H = 1664; // art rows of the home scene that must stay on screen
const ART_W = 841, ART_H = 1870, GUESS_BOTTOM = 1595;

const T = {
  tr: { tag: "Çiz, ezberle,\ntahmin et!", start: "OYNA", note: "Android ve tarayıcı oyuncularıyla eşleş", next: "Bitir", ok: "Gönder",
        guess: "Tahmin et", result: "Sonuç", timeJ: "+Süre", hintT: "İpucu", firstT: "İlk harf", countT: "Harf sayısı", skipT: "Geç", found: "Harika!\nEşleşme bulundu.", startIn: (n) => `Maç ${n} saniye sonra başlıyor`, tip1: (n) => `İkiniz de aynı ${n} kelimeyi çizeceksiniz`, tip2: "Puanlar maç sonunda karşılaştırılır", tip3: "Rahat ol, keyfini çıkar!", wTitle: "Karalak'a hoş geldin!", wBody: "Kısa bir deneme yapalım. Sana 3 kelime vereceğim, sen çizeceksin. Sonra çizimlerini sana geri göstereceğim, sen de ne olduğunu hatırlayacaksın. Hepsi bu!", wGo: "Hadi başlayalım", wSkip: "Eğitimi atla", finT: "İşte bu kadar! 🎉", finB: "Artık çizmeyi de hatırlamayı da biliyorsun. Hediye olarak her jokerden 3'er hakkın hesabına eklendi. Kalemin hazır, bol şans!", finBtn: "Oynamaya başla", searching: "Rakip aranıyor…", searchSub: "Birisi senin gibi çizerken bulunuyor", again: "TEKRAR OYNA", nextLevel: "Sonraki Bölüm", lobbyHome: "ANA SAYFA", noOpp: "Şu an rakip bulunamadı",
        you: "Sen", win: "Kazandın!", lose: "Kaybettin", draw: "Berabere", leaveQ: "Odadan ayrılmak istiyor musun?", leave: "Oyundan çıkılsın mı?", yes: "Çık", no: "Kal",
        mine: "Çizimlerin", tiles: { online: "Arkadaşınla Yarış", quick: "Hızlı Eşleş", offline: "Çevrimdışı Oyna", levels: "Bölümler", friends: "Arkadaşlar", ach: "Başarımlar", league: "Aylık Lig", store: "Mağaza", settings: "Ayarlar" }, goldLbl: "Klasik", adGold: "+500 Altın", daily: "Günün Meydan Okuması", adChest: "Ücretsiz Kasa", dailyPlay: "ŞİMDİ OYNA", dailyDone: "Bugünkü meydan okumayı bitirdin!", lvl: "Seviye", toNext: (n, l) => `Sv. ${l} için ${n} XP`, soon: "Çok yakında", appOnly: "Uygulamada", ok: "Tamam", langTitle: "Dil",  theirs: "Rakibin çizimleri", pts: "puan", file: "words_tr.json", ph: "Cevabın…" },
  en: { tag: "Draw, memorize,\nguess!", start: "PLAY", note: "Match with Android and browser players", next: "Done", ok: "Send",
        guess: "Guess", result: "Result", timeJ: "+Time", hintT: "Hint", firstT: "First letter", countT: "Letters", skipT: "Skip", found: "Great!\nMatch found.", startIn: (n) => `Match starts in ${n} seconds`, tip1: (n) => `You'll both draw the same ${n} words`, tip2: "Scores are compared when the match ends", tip3: "Relax and enjoy the game!", wTitle: "Welcome to Karalak!", wBody: "Let's try a quick practice. I'll give you 3 words to draw. Then I'll show your drawings back and you'll remember what they were. That's it!", wGo: "Let's start", wSkip: "Skip tutorial", finT: "That's it! 🎉", finB: "You now know how to draw and remember. As a gift, 3 of each joker were added to your account. Your pen is ready, good luck!", finBtn: "Start playing", searching: "Finding an opponent…", searchSub: "Looking for someone who draws like you", again: "PLAY AGAIN", nextLevel: "Next Level", lobbyHome: "HOME", noOpp: "No opponent found right now",
        you: "You", win: "You win!", lose: "You lose", draw: "Draw", leaveQ: "Leave the room?", leave: "Leave the game?", yes: "Leave", no: "Stay",
        mine: "Your drawings", tiles: { online: "Race a Friend", quick: "Quick Match", offline: "Play Offline", levels: "Levels", friends: "Friends", ach: "Achievements", league: "Monthly League", store: "Store", settings: "Settings" }, goldLbl: "Classic", adGold: "+500 Gold", daily: "Daily Challenge", adChest: "Free Chest", dailyPlay: "PLAY NOW", dailyDone: "You finished today's challenge!", lvl: "Level", toNext: (n, l) => `${n} XP to Lv. ${l}`, soon: "Coming soon", appOnly: "In the app", ok: "OK", langTitle: "Language",  theirs: "Opponent's drawings", pts: "pts", file: "words_en.json", ph: "Your answer…" },
};
const $ = (id) => document.getElementById(id);
let lang = localStorage.getItem("lang") || ((navigator.language || "tr").startsWith("tr") ? "tr" : "en");
let words = [], round = [], idx = 0, current = "home", u = 1;
let bonusTimer = null, dTimer = null, gTimer = null, tool = "pen", score = 0, shownAt = 0;
const t = () => T[lang];

// ---------- layout (art units: the paintings are 841 wide) ----------
const stage = $("stage");
let leagueLoaded = false, reportLoaded = false;
const ctx = {
  runeFx: (fx) => runeFx(P, fx), runeHas: (fx) => runeHas(P, fx),
  /**
   * Pays an online room round once its ranking is known (OnlineResultViewModel): the room gold with the room runes, the
   * match XP (8, +25 for a win), and a chest for the winner. The per-word XP was paid when the round ended.
   */
  grantOnline: (players, hostUid, myUid) => {
    const me = players.find((p) => p.uid === myUid); if (!me) return null;
    const place = [...players].sort((a, b) => b.totalScore - a.totalScore).findIndex((p) => p.uid === myUid) + 1;
    const tied = place === 1 && players.filter((p) => p.totalScore === me.totalScore).length > 1;
    const won = place === 1 && !tied && players.length >= 2, newAch = ctx.lastAch || 0;
    const raw = eco.roomRaw(me.correctCount || 0, won, newAch);
    // Dostluk Bağı: +50% once a day, spent only on a round that pays something.
    const best = raw > 0 && runeHas(P, "BEST") && P.bestDay !== todayEpoch(); if (best) P.bestDay = todayEpoch();
    const g = eco.roomGold({ fx: (k) => runeFx(P, k), correctCount: me.correctCount || 0, won, newAchievements: newAch, asHost: hostUid === myUid, bestFriendBonus: best });
    P.gold += g.total;
    const matchXp = (eco.ONLINE_MATCH_XP + (won ? eco.ONLINE_WIN_XP : 0)) * xpEventOnly;
    ctx.lastXp = (ctx.lastXp || 0) + eco.addXp(P, matchXp, runeFx(P, "XP"));
    ctx.lastGold = g.total; ctx.lastRune = g.runeBonus;
    let chest; if (place === 1 && !tied) chest = awardChest(P, (k) => runeHas(P, k)) || null;
    saveP(); refreshHome();
    if (chest !== undefined) setTimeout(() => wonDialog(ctx, chest), 900);
    return { won, place, gold: g };
  },
  current: () => current, myPeriodXp: () => (P.periodId === periodId() ? P.periodXp || 0 : 0),
  $, get P() { return P; }, saveP: () => saveP(), lang: () => lang, place: (...x) => place(...x), u: () => u, stageH: () => stage.getBoundingClientRect().height, fillH: () => (safeRows[current] ? safeRows[current].b * u : stage.getBoundingClientRect().height),
  inviteFriend: (f) => quickInvite(ctx, f).then(() => { stopFriends(); show("onw"); }),
  sfx, paint: (...a) => paint(...a), fitCanvas: (c) => fitCanvas(c),
  kind: () => kind, addRow: (...a) => addRow(...a), startOnlineGame: (r, cd) => startOnlineGame(r, cd),
  pickRoomWords: (r) => pickRoomWords(r), startOffline: (o) => startOffline(o),
  startTutorial: () => playTutorial(), homeGain: (g, x) => homeGain(g, x), openDuel: (opp) => openCreateDuel(ctx, opp), startDuelChallenge: (opp, n) => startDuel({ mode: "new", opp }, n), playDuel: (d) => startDuel({ mode: "play", duel: d }),
  syncMusic: () => syncMusic(), refreshHome: () => refreshHome(), show: (id) => show(id),
  playLevel: (w, l) => startLevel(w, l),
  setLang: (l) => { lang = l; applyLang(); if (current === "settings") renderSettings(ctx); },
  open: (id) => { if (id === "report") reportLoaded = false; show(id); },
  levelOf: (xp) => { const lv = levelFor(xp), fl = xpFor(lv), ce = lv >= 100 ? fl : xpFor(lv + 1); return { level: lv, into: xp - fl, span: ce - fl, frac: ce > fl ? (xp - fl) / (ce - fl) : 1, max: lv >= 100 }; },
  info: (text) => msg(text),
  // Sign-out wipes what belongs to the account (SettingsRepository.clearAccountScopedState); sounds, language and the tutorial stay.
  resetProgress() { const keep = { prefs: P.prefs, tutorialDone: P.tutorialDone }; const fresh = JSON.parse(JSON.stringify(DEFAULT_P)); for (const k of Object.keys(P)) delete P[k]; Object.assign(P, fresh, keep); P.nick = lang === "tr" ? "Oyuncu" : "Player"; saveP(); refreshHome(); },
  confirm(text, yes, onYes) {
    $("dlg-text").textContent = text; $("dlg-extra").replaceChildren(); $("dlg-no").style.display = ""; $("dlg-no").textContent = t().no === "Kal" ? "Vazgeç" : "Cancel"; $("dlg-yes").textContent = yes;
    $("dialog").classList.remove("hidden");
    $("dlg-yes").onclick = () => { $("dialog").classList.add("hidden"); onYes(); };
    $("dlg-no").onclick = () => $("dialog").classList.add("hidden");
  },
};
// ---------- player (web-local progress; the account's level comes from Google sign-in later) ----------
const RANKS = [[1, "✏️", "Karalamacı", "Scribbler"], [20, "🖊️", "Çırak", "Apprentice"], [40, "🖌️", "Ressam", "Painter"], [60, "🎨", "Usta Ressam", "Master Painter"], [80, "🖼️", "Sanatçı", "Artist"], [100, "👑", "Büyük Usta", "Grand Master"]];
const xpFor = (lv) => { const n = Math.max(0, lv - 1); return 25 * n * n + 75 * n; };
const levelFor = (xp) => { let l = 1; while (l < 100 && xp >= xpFor(l + 1)) l++; return l; };
const DEFAULT_P = { nick: "", xp: 0, gold: 0, runes: {}, owned: [], pen: "CLASSIC", frame: "SCRIBBLER", jokers: {}, claimed: [], daily: { last: -1, streak: 0, best: 0, result: null }, levels: {}, stats: { games: 0, score: 0, words: 0, perfect: 0, wins: 0, streak: 0, best: 0, last: "" } };
let P = JSON.parse(JSON.stringify(DEFAULT_P));
try { P = { ...P, ...JSON.parse(localStorage.getItem("player") || "{}") }; } catch {}
P.prefs = P.prefs || { s0: true, s1: true, s2: true, s3: false }; setPrefs(() => P.prefs); initAudio();
const saveP = () => { try { localStorage.setItem("player", JSON.stringify(P)); } catch {} try { scheduleBackup(ctx); } catch {} };
if (!P.nick) { P.nick = "Karalak" + Math.floor(1000 + Math.random() * 9000); saveP(); }
function place(el, x, y, w, h) {
  const s = el.style;
  s.left = x * u + "px"; s.top = y * u + "px";
  if (w != null) s.width = w * u + "px";
  if (h != null) s.height = h * u + "px";
}
function viewport() { const v = window.visualViewport; return { w: v ? v.width : innerWidth, h: v ? v.height : innerHeight, ox: v ? v.offsetLeft : 0, oy: v ? v.offsetTop : 0 }; }
/** Art rows that must stay on screen for each scene: home is fixed by hand, the others come from where their widgets end (see layout). */
const safeRows = { home: { t: HOME_SAFE_TOP, b: HOME_SAFE_TOP + HOME_SAFE_H } };
const SAFE_TOP = 24, SAFE_BELOW = 60, SAFE_BELOW_OF = { settings: 16 };
/** Scenes whose gap beside the art is closed partly by a slight horizontal stretch (the sides carry card fragments, so a mirror would repeat them). */
const STRETCH_MAX = { settings: 1.06 };
function layoutOnce() {
  const { w: vw, h: vh, ox, oy } = viewport();
  const s = stage.style;
  let W, H;
  const sf = safeRows[current];
  if (current === "guess" || current === "result" || current === "res2" || current === "lvmap") { W = Math.min(vw, 560); H = vh; }
  else if (sf) {
    // Fill the width as far as the painted scene allows: only decor above and below the widgets may fall off the screen.
    const uFit = Math.min(vw / ART_W, vh / ART_H), uSafe = Math.min(vw / ART_W, vh / (sf.b - sf.t));
    u = Math.max(uFit, uSafe); W = u * ART_W; H = u * ART_H;
  }
  else { W = Math.min(vw, vh * ART_W / ART_H); H = W * ART_H / ART_W; }
  u = W / ART_W;
  const kx = Math.max(1, Math.min(STRETCH_MAX[current] || 1, vw / W));
  stage.style.transform = kx > 1 ? `scaleX(${kx})` : ""; W *= 1; const Wv = W * kx;
  s.width = W + "px"; s.height = H + "px"; s.left = (vw - W) / 2 + ox + "px";
  s.top = oy + (H <= vh ? (vh - H) / 2 : sf ? Math.min(0, Math.max(vh - H, vh / 2 - (sf.t + sf.b) / 2 * u)) : (vh - H) / 2) + "px";
  stage.style.setProperty("--u", u + "px");
  paintEdges(Wv, H, (vw - Wv) / 2, parseFloat(s.top));
  if (!$("tut-spot").classList.contains("coach")) place($("tut-card"), 70, 250, 700, 360);
  if (current === "home") {
    place($("h-gold"), 112, 270, 126, 44); place($("h-runes"), 520, 86, 240, 214); place($("h-rbadge"), 682, 84, 70, 70);
    place($("h-pen"), 266, 260, 243, 62);
    place($("h-avatar"), 66, 361, 138, 138);
    place($("h-name"), 224, 357, 556, 42);
    place($("h-rank"), 222, 400, 266, 47);
    place($("h-xpnext"), 496, 406, 284, 40);
    place($("h-bar"), 222, 452, 550, 40);
    place($("h-veil-gold"), 34, 538, 196, 216); place($("h-veil-chest"), 616, 538, 192, 216);
    layoutChests(ctx);
    place($("h-dcard"), 236, 538, 372, 218); place($("h-dmul"), 256, 630, 104, 46); place($("h-dpips"), 394, 628, 200, 48); place($("h-dplay"), 264, 688, 332, 56); place($("h-gain-gold"), 110, 226, 140, 46); place($("h-gain-xp"), 540, 396, 240, 54); place($("h-dcount"), 262, 692, 142, 48); place($("h-ddone"), 410, 690, 188, 50);
    place($("h-ttl-gold"), 40, 648, 186, 50); place($("h-ttl-daily"), 384, 546, 214, 80); place($("h-ttl-chest"), 634, 640, 162, 66);
    place($("h-soon-gold"), 56, 704, 154, 36); place($("h-soon-chest"), 640, 704, 150, 36);
    const T = [["online", 30, 776, 258, 236, 50, 918, 274, 1002], ["quick", 294, 776, 258, 236, 330, 940, 520, 986], ["offline", 558, 776, 258, 236, 578, 918, 798, 1002],
      ["levels", 30, 1018, 258, 186, 50, 1142, 268, 1188], ["friends", 294, 1018, 258, 186, 320, 1142, 526, 1188], ["ach", 558, 1018, 258, 186, 584, 1142, 792, 1188],
      ["league", 30, 1210, 258, 188, 50, 1332, 268, 1380], ["store", 294, 1210, 258, 188, 340, 1332, 510, 1380], ["settings", 558, 1210, 258, 188, 590, 1332, 786, 1380]];
    for (const [id, x, y, w, h, lx0, ly0, lx1, ly1] of T) { place($("tl-" + id), x, y, w, h); place($("tx-" + id), lx0, ly0, lx1 - lx0, ly1 - ly0); }
  } else if (current === "store") {
    layoutStore(ctx); renderStore(ctx);
  } else if (current === "onl" || current === "onj" || current === "onc" || current === "onw") {
    layoutOnline(ctx);
    if (current === "onl") renderEntry(ctx); else if (current === "onj") renderJoin(ctx); else if (current === "onc") renderCreate(ctx);
  } else if (current === "offs") {
    layoutOffline(ctx); renderOffline(ctx);
  } else if (current === "friends") {
    layoutFriends(ctx); renderFriends(ctx);
  } else if (current === "worlds") {
    layoutWorlds(ctx); renderWorlds(ctx);
  } else if (current === "lvmap") {
    renderMap(ctx);
  } else if (current === "account") {
    layoutAccount(ctx); renderAccount(ctx);
  } else if (current === "welcome") {
    place($("w-title"), 126, 830, 590, 68); place($("w-body"), 104, 934, 634, 200); place($("w-go"), 125, 1170, 575, 136); place($("w-skip"), 200, 1334, 442, 90);
  } else if (current === "runes") {
    layoutRunes(ctx); renderRunes(ctx, !freshScreen);
  } else if (current === "report") {
    layoutReport(ctx); renderReport(ctx, !reportLoaded); reportLoaded = true;
  } else if (current === "brk") {
    layoutBreak(ctx);
  } else if (current === "duels") {
    place($("du-back"), 20, 40, 110, 106); renderDuels(ctx);
  } else if (current === "league") {
    layoutLeague(ctx); renderLeague(ctx, !leagueLoaded); leagueLoaded = true;
  } else if (current === "ach") {
    layoutAchievements(ctx); renderAchievements(ctx);
  } else if (current === "settings") {
    layoutSettings(ctx); renderSettings(ctx);
  } else if (current === "draw") {
    place($("d-back"), 28, 82, 99, 96);
    place($("d-prog"), 152, 88, 296, 82);
    place($("d-ring"), 712, 82, 98, 98); place($("d-music"), 593, 88, 97, 82);
    place($("d-word"), 125, 232, 445, 112);
    place($("d-line"), 235, 343, 250, 8);
    place($("d-canvas"), 36, 438, 770, 948);
    [["t-pen", 48], ["t-erase", 165], ["t-undo", 290], ["t-clear", 418]].forEach(([id, x]) => place($(id), x, 1433, 98, 98));
    place($("d-next"), 540, 1436, 258, 92);
    fitCanvas($("pad"));
    paintLive();
  } else if (current === "guess") {
    const topFrom = 120, topEnd = 400, botStart = 985;
    const botH = (GUESS_BOTTOM - botStart) * u, topH = (topEnd - topFrom) * u;
    const midH = Math.max(0, H - topH - botH);
    const sl = (el, top, h, srcY, srcH) => {
      el.style.top = top + "px"; el.style.left = "0"; el.style.width = W + "px"; el.style.height = h + "px";
      el.style.backgroundSize = `${W}px ${srcH ? h / srcH * ART_H : u * ART_H}px`;
      el.style.backgroundPosition = `0 ${-srcY * (srcH ? h / srcH : u)}px`;
    };
    sl($("g-bgtop"), 0, topH, topFrom, topEnd - topFrom);
    sl($("g-bgmid"), topH, midH + 1, topEnd, botStart - topEnd);
    sl($("g-bgbot"), topH + midH, botH, botStart, GUESS_BOTTOM - botStart);
    const fb = (v) => H - (GUESS_BOTTOM - v) * u; // art y measured from the bottom edge
    const ty = (v) => (v - topFrom) * u;
    const P = (el, x, y, w, h) => { const s2 = el.style; s2.left = x * u + "px"; s2.top = y + "px"; s2.width = w * u + "px"; s2.height = h + "px"; };
    P($("g-back"), 22, ty(155), 84, 78 * u);
    P($("g-prog"), 118, ty(158), 114, 74 * u);
    P($("g-xp"), 334, ty(158), 188, 72 * u); P($("g-bonus"), 534, ty(164), 176, 60 * u); P($("g-music"), 246, ty(158), 76, 74 * u);
    [["jt-ad", 48, 172], ["jt-first", 237, 171], ["jt-count", 426, 178], ["jt-skip", 621, 176]].forEach(([id, x0, w]) => P($(id), x0, fb(1406), w, 164 * u));
    P($("g-ring"), 722, ty(150), 96, 96 * u);
    P($("g-canvas"), 56, ty(306), 738, Math.max(60, fb(1112) - ty(310)));
    P($("g-form"), 0, fb(1265), 841, 105 * u);
    const inp = $("g-input"); inp.style.left = 58 * u + "px"; inp.style.top = 6 * u + "px"; inp.style.width = 484 * u + "px"; inp.style.height = 92 * u + "px";
    const ok = $("g-ok"); ok.style.left = 572 * u + "px"; ok.style.top = 0; ok.style.width = 225 * u + "px"; ok.style.height = 105 * u + "px";
    fitCanvas($("view"));
    paintGuess();
  } else if (current === "found") {
      const s = 1.2;
      place($("f-title"), 150, 62, 450 * s, 272 * s); place($("f-tag"), (841 - 354 * s) / 2, 62 + 266 * s, 354 * s, 106 * s); place($("f-tagt"), (841 - 354 * s) / 2 + 354 * s * 0.18, 381 + 106 * s * 0.1, 354 * s * 0.64, 106 * s * 0.68);
      const card = (id, x, y, w, h, cx) => { place($(id), x, y, w * 1.18, h * 1.18); const e = $(id), k = u * 1.18, S = (v) => v * u;
        const av = e.querySelector(".mc-av"); av.style.cssText = `left:${S(cx * 1.18 - 95)}px;top:${S(190 * 1.18 - 95)}px;width:${S(190)}px;height:${S(190)}px`;
        const rb = e.querySelector(".mc-rb"); rb.style.cssText = `left:${S(cx * 1.18 - 146)}px;top:${S(290 * 1.18 - 28)}px;width:${S(292)}px;height:${S(54)}px`;
        const xp = e.querySelector(".mc-xp"); xp.style.cssText = `left:${S(cx * 1.18 - 120)}px;top:${S(337 * 1.18 - 30)}px;width:${S(240)}px;height:${S(40)}px`; };
      card("f-cl", 20, 541, 290, 384, 148); card("f-cr", 507, 534, 266, 392, 133);
      place($("f-vs"), 297, 664, 275 * 0.9, 252 * 0.9); place($("f-mas"), 157, 966, 479 * 1.1, 291 * 1.1);
      place($("f-plank"), 25, 1235, 790, 148);
      [["f-t1", 24, 225], ["f-t2", 296, 227], ["f-t3", 568, 229]].forEach(([id, x, w]) => place($(id), x, 1405, w * 1.1, 197 * 1.1));
  } else if (current === "search") {
    place($("s-sign"), 60, 190, 720, 365);
    place($("s-text"), 160, 308, 521, 184);
    place($("s-paper"), 215, 1110, 410, 305);
    place($("s-sub"), 70, 1480, 700, 130);
    if (kind === "quick") startSearch(ctx);
  }
}
/** Lays the scene out, then measures where its widgets end so the next pass can crop the decor below them. */
function layout() {
  layoutOnce();
  if (current === "home") return;
  if (["guess", "result", "res2", "lvmap"].includes(current)) return;
  const st = stage.getBoundingClientRect(); if (!st.height || !u) return;
  const { w: vw, h: vh } = viewport();
  const have = safeRows[current], edge = (have ? have.b : ART_H) - 2;
  let fixed = 0, fill = false;
  for (const el of document.querySelectorAll("#" + current + " > :not(.bg):not(.wood)")) {
    const r = el.getBoundingClientRect(); if (!r.height || !r.width) continue;
    const b = (r.bottom - st.top) / u;
    if (b >= edge) { fill = true; continue; } // a list that runs to the bottom simply gets shorter
    fixed = Math.max(fixed, b);
  }
  // Fully across the width needs this many art rows on screen; lists can shrink to it, fixed widgets cannot.
  const fullB = SAFE_TOP + vh / (vw / ART_W);
  const want = Math.round(fill ? Math.max(fixed + SAFE_BELOW / 2, fullB) : fixed + (SAFE_BELOW_OF[current] ?? SAFE_BELOW));
  if (!fixed && !fill) return;
  if (want >= ART_H - 40) { if (have) { delete safeRows[current]; layoutOnce(); } return; }
  if (have && Math.abs(have.b - want) < 6) return;
  safeRows[current] = { t: SAFE_TOP, b: want };
  layoutOnce();
}
addEventListener("resize", layout);
window.visualViewport?.addEventListener("resize", layout);
// A soft keyboard may pan the visual viewport instead of shrinking the page: follow it, and keep the page itself from scrolling away.
window.visualViewport?.addEventListener("scroll", () => { window.scrollTo(0, 0); layout(); });
// only a text field raises the keyboard: tapping a plain button must not re-lay-out (and so reset) the scene
const isTyping = (e) => /^(INPUT|TEXTAREA|SELECT)$/.test(e.target?.tagName || "") || e.target?.isContentEditable;
document.addEventListener("focusin", (e) => { if (!isTyping(e)) return; for (const ms of [120, 350, 700]) setTimeout(() => { window.scrollTo(0, 0); layout(); }, ms); });
document.addEventListener("focusout", (e) => { if (!isTyping(e)) return; for (const ms of [120, 350]) setTimeout(layout, ms); });

const SPK = '<svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 8v8a4.47 4.47 0 0 0 2.5-4zM14 3.2v2.1a7 7 0 0 1 0 13.4v2.1a9 9 0 0 0 0-17.6z"/></svg>';
function paintMusic() { for (const id of ["d-music", "g-music"]) { const b = $(id); b.innerHTML = SPK; b.classList.toggle("off", !(P.prefs.s0 && P.prefs.s1)); } }
for (const id of ["d-music", "g-music"]) $(id).onclick = () => { P.prefs.s1 = !P.prefs.s1; saveP(); paintMusic(); syncMusic(); };
// Pictures of screens that are not showing are only fetched when the screen first opens (the first load used to pull 3.7 MB).
function loadImgs(id) { const s = document.getElementById(id); if (!s) return; s.querySelectorAll("img[data-src]").forEach((i) => { i.src = i.dataset.src; i.removeAttribute("data-src"); }); }
let freshScreen = true;
function show(id) {
  freshScreen = true;
  loadImgs(id);
  current = id; if (typeof paintMusic === "function" && P.prefs) paintMusic();
  for (const s of document.querySelectorAll(".screen")) s.classList.toggle("on", s.id === id);
  paintBackdrop(id);
  layout(); freshScreen = false; paintTeamPill();
}
/** A thin gap beside the art is closed with a mirror of the scene's own edge, so vines and wood carry on seamlessly; wider gaps keep the soft backdrop. */
function paintEdges(W, H, gap, top) {
  const bgEl = document.querySelector("#" + current + " > img.bg");
  const src = bgEl && (bgEl.getAttribute("src") || bgEl.dataset.src);
  for (const side of ["l", "r"]) {
    const e = $("edge-" + side); if (!e) continue;
    const on = !!src && gap > 0.5 && gap <= W * 0.3;
    e.style.display = on ? "block" : "none";
    if (!on) continue;
    const mirror = gap <= W * 0.16 && current !== "settings"; // a mirror of the scenery edge; the settings edge carries card fragments, so it gets a soft smear instead
    const bw = mirror ? W + 6 : W * (gap + 1) / 3;
    e.style.transform = mirror ? "scaleX(-1)" : "none";
    e.style.filter = mirror ? "none" : "blur(22px) brightness(.9)";
    const px = mirror ? (side === "l" ? "-3px" : "calc(100% + 3px)") : (side === "l" ? "0" : "100%");
    e.style.cssText += `;width:${gap + 1}px;top:${top}px;height:${H}px;background-image:url("${src}");background-size:${bw}px ${H}px;background-position:${px} top;${side === "l" ? "left:0" : "right:0"}`;
  }
}
/** The art is a fixed tall shape; the strips beside it carry a soft, enlarged copy of the same scene instead of bare wood. */
function paintBackdrop(id) {
  const e = $("backdrop"); if (!e) return;
  const bg = document.querySelector("#" + id + " > img.bg");
  const src = bg && (bg.getAttribute("src") || bg.dataset.src);
  e.style.backgroundImage = src ? `url("${src}")` : "none";
}

// ---------- text ----------
function applyLang() {
  const x = t();
  document.documentElement.lang = lang; setLang(lang);
  $("d-next-t").textContent = x.next; $("g-ok-t").textContent = S("submit_guess");
  $("w-title").textContent = x.wTitle; $("w-body").textContent = x.wBody; $("w-go-t").textContent = x.wGo; $("w-skip").textContent = x.wSkip;
  $("s-text").textContent = x.searching; $("s-sub").textContent = x.searchSub; $("again-t").textContent = x.again;
  $("dlg-text").textContent = x.leave; $("dlg-yes").textContent = x.yes; $("dlg-no").textContent = x.no;
  $("g-input").placeholder = S("what_did_you_draw");
  try { localStorage.setItem("lang", lang); } catch {}
  refreshHome();
}

// ---------- drawing ----------
function fitCanvas(c) {
  const r = c.getBoundingClientRect(), d = window.devicePixelRatio || 1;
  c.width = Math.max(1, Math.round(r.width * d)); c.height = Math.max(1, Math.round(r.height * d));
}
// Strokes are lists of {x,y} in canvas pixels (the shape Android stores); every viewer fits them to the box.
function paint(canvas, strokes, live = false, pen = "CLASSIC", fitTo = null) {
  const ctx = canvas.getContext("2d"), w = canvas.width, h = canvas.height, d = window.devicePixelRatio || 1;
  ctx.clearRect(0, 0, w, h);
  const pts = (fitTo || strokes).flat();
  if (!pts.length) return;
  let sc = d, ox = 0, oy = 0;
  if (!live) {
    const xs = pts.map((p) => p.x), ys = pts.map((p) => p.y);
    const minX = Math.min(...xs), minY = Math.min(...ys);
    const cw = Math.max(1, Math.max(...xs) - minX), ch = Math.max(1, Math.max(...ys) - minY);
    const pad = Math.min(w, h) * 0.08;
    sc = Math.min((w - 2 * pad) / cw, (h - 2 * pad) / ch);
    ox = (w - cw * sc) / 2 - minX * sc; oy = (h - ch * sc) / 2 - minY * sc;
  }
  ctx.lineCap = ctx.lineJoin = "round";
  ctx.lineWidth = live ? 6 * d : Math.min(13.5 * d, Math.max(1.6 * d, 7 * sc));
  const pObj = (() => { try { return penById(pen); } catch { return null; } })();
  if (pObj && pObj.effect && pObj.effect !== "NONE") {
    // premium pen: every stroke goes through the effect painter, in device pixels
    let dist = 0; const w = live ? 6 * d : Math.min(13.5 * d, Math.max(1.6 * d, 7 * sc));
    for (const s of strokes) dist = drawFxStroke(ctx, s.map((p) => ({ x: p.x * sc + ox, y: p.y * sc + oy })), pObj, w, performance.now() / 1000, d, dist);
    return;
  }
  const cols = penColors(pen);
  let dist = 0;
  for (const s of strokes) {
    if (s.length === 1) { ctx.strokeStyle = colorAt(cols, dist); ctx.beginPath(); ctx.moveTo(s[0].x * sc + ox, s[0].y * sc + oy); ctx.lineTo(s[0].x * sc + ox + 0.1, s[0].y * sc + oy); ctx.stroke(); continue; }
    if (cols.length === 1) { ctx.strokeStyle = cols[0]; ctx.beginPath(); s.forEach((p, i) => (i ? ctx.lineTo(p.x * sc + ox, p.y * sc + oy) : ctx.moveTo(p.x * sc + ox, p.y * sc + oy))); ctx.stroke(); continue; }
    for (let i = 1; i < s.length; i++) {
      dist += Math.hypot(s[i].x - s[i - 1].x, s[i].y - s[i - 1].y);
      ctx.strokeStyle = colorAt(cols, dist); ctx.beginPath();
      ctx.moveTo(s[i - 1].x * sc + ox, s[i - 1].y * sc + oy); ctx.lineTo(s[i].x * sc + ox, s[i].y * sc + oy); ctx.stroke();
    }
  }
}
function penColors(id) { try { const p = penById(id); return p.colors.length ? p.colors : ["#2E2A26"]; } catch { return ["#2E2A26"]; } }
function colorAt(cols, dist) {
  if (cols.length === 1) return cols[0];
  const t = ((dist / 420) % 1 + 1) % 1 * cols.length, i = Math.floor(t) % cols.length, f = t - Math.floor(t), a = cols[i], b = cols[(i + 1) % cols.length];
  const h = (x) => [1, 3, 5].map((k) => parseInt(x.slice(k, k + 2), 16));
  const [r1, g1, b1] = h(a), [r2, g2, b2] = h(b);
  return `rgb(${Math.round(r1 + (r2 - r1) * f)},${Math.round(g1 + (g2 - g1) * f)},${Math.round(b1 + (b2 - b1) * f)})`;
}
// A premium pen keeps moving, so the canvas it is drawn on is redrawn every frame while visible.
function fxLoop() {
  const pObj = (() => { try { return penById(P.pen); } catch { return null; } })();
  const animated = pObj && pObj.effect && pObj.effect !== "NONE";
  // Only a premium pen on the drawing / guessing screens needs a frame loop; everywhere else just look again in half a second.
  if (animated && (current === "guess" || current === "draw")) { current === "guess" ? paintGuess() : paintLive(); requestAnimationFrame(fxLoop); } else setTimeout(fxLoop, 500);
}
fxLoop();
const paintLive = () => current === "draw" && round[idx] && paint($("pad"), round[idx].strokes, true, P.pen);
const paintGuess = () => current === "guess" && gOrder.length && gw() && paint($("view"), gw().strokes, false, P.pen);

(() => {
  const c = $("pad"); let cur = null;
  const pt = (e) => { const r = c.getBoundingClientRect(); return { x: Math.round(e.clientX - r.left), y: Math.round(e.clientY - r.top) }; };
  const erase = (p) => {
    const w = round[idx]; let best = -1, bd = 26;
    w.strokes.forEach((s, i) => s.forEach((q) => { const d = Math.hypot(q.x - p.x, q.y - p.y); if (d < bd) { bd = d; best = i; } }));
    if (best >= 0) { w.strokes.splice(best, 1); paintLive(); refreshTools(); }
  };
  c.addEventListener("pointerdown", (e) => {
    c.setPointerCapture(e.pointerId);
    if (tool === "erase") { cur = "erase"; erase(pt(e)); return; }
    cur = [pt(e)]; round[idx].strokes.push(cur); paintLive();
  });
  c.addEventListener("pointermove", (e) => {
    if (!cur) return;
    if (cur === "erase") erase(pt(e)); else { cur.push(pt(e)); paintLive(); }
  });
  const end = () => { cur = null; refreshTools(); };
  c.addEventListener("pointerup", end); c.addEventListener("pointercancel", end);
})();
function refreshTools() {
  const has = round[idx]?.strokes.length > 0;
  $("t-undo").disabled = !has; $("t-clear").disabled = !has;
  $("t-pen").classList.toggle("on", tool === "pen"); $("t-erase").classList.toggle("on", tool === "erase");
}
$("t-pen").onclick = () => { tool = "pen"; refreshTools(); };
$("t-erase").onclick = () => { tool = "erase"; refreshTools(); };
$("t-undo").onclick = () => { round[idx].strokes.pop(); paintLive(); refreshTools(); };
$("t-clear").onclick = () => { round[idx].strokes = []; paintLive(); refreshTools(); };

// ---------- flow ----------
function setRing(ring, arc, timeEl, left, total) {
  timeEl.textContent = left;
  arc.style.strokeDashoffset = 276.5 * (1 - Math.max(0, left) / total);
  ring.classList.toggle("warn", left <= 3);
}
let levelRun = null, kind = "quick", relaxed = false;
async function ensureWords() { if (words.lang !== lang) { words = await (await fetch(t().file)).json(); words.lang = lang; } }
function pickRoomWords(r) {
  const diffs = r.difficulty ? r.difficulty.split(",") : ["EASY", "MEDIUM", "HARD"], cats = r.category ? r.category.split(",") : null;
  return [...words].filter((w) => diffs.includes(w.difficulty) && (!cats || cats.includes(w.category))).sort(() => Math.random() - 0.5).slice(0, r.wordCount || 10).map((w) => w.id);
}
let teamInfo = null;
function paintTeamPill() {
  let e = document.getElementById("team-pill");
  if (!e) { e = document.createElement("div"); e.id = "team-pill"; document.getElementById("stage").append(e); }
  const on = teamInfo && kind === "online" && (current === "draw" || current === "guess");
  e.style.display = on ? "" : "none";
  if (on) e.textContent = `${chatText("online_team_yours", lang, teamInfo.teamId)} · ${teamInfo.mate ? chatText("online_team_mate", lang, teamInfo.mate) : chatText("online_team_mate_missing", lang)}`;
}
async function startOnlineGame(r) {
  teamInfo = null;
  if (r.teamMode) { await loadChat(); const uid = await net.myUid(), mine = (r.players || {})[uid]; if (mine && mine.teamId) { const mate = Object.entries(r.players).find(([u, p]) => u !== uid && p.teamId === mine.teamId && !p.left); teamInfo = { teamId: mine.teamId, mate: mate ? mate[1].displayName : null }; } }
  await ensureWords(); kind = "online"; levelRun = null; relaxed = r.mode === "RELAXED";
  const byId = new Map(words.map((w) => [w.id, w]));
  begin((r.wordIds || []).map((id) => byId.get(id)).filter(Boolean));
}
async function startLevel(w, l) {
  levelRun = { w, l }; kind = "level"; relaxed = false;
  const mix = mixFor(w, l);
  if (words.lang !== lang) { words = await (await fetch(t().file)).json(); words.lang = lang; }
  const picks = [];
  for (const [d, n] of Object.entries(mix)) picks.push(...[...words].filter((x) => x.difficulty === d).sort(() => Math.random() - 0.5).slice(0, n));
  begin(picks.sort(() => Math.random() - 0.5));
}
const TUT_WORDS = { tr: ["kitap", "köpek", "elma"], en: ["book", "dog", "apple"] };
// TutorialViewModel.kt, ported: word 1 untimed, word 2 on a 12 s clock, word 3 on 10 s; guesses on 10 s with the two jokers taught one by one
// (the clock stands still until the joker is used); coaching cards between the turns; practice jokers that never touch the real stock.
const TUT = { drawSeconds: [0, 12, 10], guessSeconds: 10, stockStart: 3 };
let tutStock = { FIRST_LETTER: 3, LETTER_COUNT: 3 }, tutSeen = {}, tutSpot = null;
const jokerN = (k) => (kind === "tutorial" ? tutStock[k] || 0 : P.jokers[k] || 0);
const jokerSpend = (k) => { if (kind === "tutorial") tutStock[k]--; else { P.jokers[k]--; saveP(); } };
/** The opening card keeps the welcome scene; [cont] runs on its button. */
function introCard(cont) {
  $("w-title").textContent = S("tutorial_intro_title"); $("w-body").textContent = S("tutorial_intro_body"); $("w-go-t").textContent = S("tutorial_intro_button");
  $("w-skip").style.display = ""; $("w-skip").textContent = S("tutorial_skip"); $("w-body").style.fontSize = "";
  const bg = document.querySelector("#welcome > img.bg"); bg.dataset.src = "img/bg_tutorial_welcome.webp"; bg.src = "img/bg_tutorial_welcome.webp";
  show("welcome"); $("w-go").onclick = cont;
}
/** Every later card is a window over the screen the player is on (dimmed behind it), not a different scene. [cont] runs on its button. */
function coachCard(title, body, button, cont) {
  const sp = $("tut-spot"); document.activeElement?.blur?.();
  $("tut-t").textContent = S(title); $("tut-b").textContent = S(body); $("tut-go-t").textContent = S(button);
  $("tut-card").style.cssText = ""; sp.classList.add("on", "coach"); $("tut-go").style.display = "";
  $("tut-go").onclick = () => { sp.classList.remove("on", "coach"); $("tut-go").style.display = "none"; cont(); };
}
function playTutorial() { introCard(startTutorial); $("w-skip").onclick = () => show("home"); }
async function startTutorial() {
  tutStock = { FIRST_LETTER: TUT.stockStart, LETTER_COUNT: TUT.stockStart }; tutSeen = {}; tutSpot = null;
  await ensureWords(); kind = "tutorial"; levelRun = null; relaxed = false;
  const byText = new Map(words.map((w) => [w.text.toLowerCase(), w]));
  const picked = TUT_WORDS[lang].map((s, i) => byText.get(s) || { id: 900000 + i, text: s, difficulty: "EASY" });
  begin(picked);
}
function endTutorial() { const first = !P.tutorialDone; P.tutorialDone = true; if (first) for (const k of ["FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"]) P.jokers[k] = (P.jokers[k] || 0) + 3; saveP(); refreshHome(); show("home"); setTimeout(() => checkStreak(ctx), 900); }
async function startDaily() {
  if (dailyState(P).done) return;
  await ensureWords(); kind = "daily"; levelRun = null; relaxed = false;
  begin(dailyWords(words, todayEpoch(), lang));
}
let quickOpp = null, quickItems = [], quickItemsP = null;
/** Hızlı Eşleş's opponent, as in the app (GhostRunRepositoryImpl.findOpponent with BOT_OPPONENT_SHARE = 1): a round built from the
 *  hand-trained drawing set, so the words the web player draws are the ones the review queue wants; the live pool is only the fallback. */
async function pickOpponent() {
  try {
    const ids = await net.trainedWordIds(), have = new Set(words.map((w) => w.id));
    const g = makeGhostRun({ trainedIds: ids, hasWord: (id) => have.has(id), language: lang, level: levelFor(P.xp), points: POINTS, speedBonus: SPEED_BONUS, frameFor: highestFrameFor });
    if (g) return g;
  } catch {}
  return net.findOpponent(lang, levelFor(P.xp));
}
// SearchingScene.kt texts: the sign reads "Rakip / aranıyor…" with its dots ticking, the plate says what is going on.
function paintSearching() {
  $("s-text").innerHTML = `<span>${S("quick_match_searching_title")}</span><span>${S("quick_match_searching_title2").replace(/[….s]+$/, "")}<span id="s-dots"><i>.</i><i>.</i><i>.</i></span></span>`;
  $("s-sub").innerHTML = `<b>${S("quick_match_searching_sub")}</b><small>${S("quick_match_dont_worry_body")}</small>`;
}
async function startQuick() {
  levelRun = null; kind = "quick"; relaxed = false; quickOpp = null; quickItems = []; quickItemsP = null;
  await ensureWords(); await loadGhostNames().catch(() => {}); paintSearching(); show("search");
  const minWait = new Promise((r) => setTimeout(r, 2000 + Math.random() * 3000));
  try { quickOpp = await Promise.race([pickOpponent(), new Promise((_, rej) => setTimeout(() => rej(new Error("timeout")), 10000))]); } catch {}
  await minWait;
  if (current !== "search") return;
  const byId = new Map(words.map((w) => [w.id, w]));
  const picked = quickOpp ? (quickOpp.wordIds || []).map((id) => byId.get(id)).filter(Boolean) : [];
  if (!quickOpp || picked.length < 10) { quickOpp = null; msgNoOpp(); return; }
  quickItemsP = net.loadItems(quickOpp.id, new Map(words.map((w) => [w.id, w.text])), POINTS, SPEED_BONUS).then((it) => (quickItems = it)).catch(() => quickItems);
  showFound(quickOpp, () => begin(picked));
}
function msgNoOpp() { show("home"); msg(t().noOpp + (lang === "tr" ? ". Bir süre sonra tekrar dene ya da çevrimdışı oyna." : ". Try again in a bit or play offline.")); }
let foundTimer = null;
function showFound(opp, go) {
  const x = t(); show("found");
  $("f-tagt").textContent = x.found; $("f-tagt").style.whiteSpace = "pre-line";
  $("f-t1").querySelector("span").textContent = x.tip1(10); $("f-t2").querySelector("span").textContent = x.tip2; $("f-t3").querySelector("span").textContent = x.tip3;
  const card = (id, nick, lvl, frame, xp) => {
    const e = $(id); e.querySelector(".mc-rb span").textContent = nick; e.querySelector(".mc-xp b").textContent = xp.toLocaleString() + " XP";
    const fr = frameRing(frameById(frame)); fr.style.cssText += ";width:100%;height:100%"; const av = e.querySelector(".mc-av"); av.replaceChildren(fr); av.append(Object.assign(document.createElement("b"), { className: "mc-lv", textContent: lvl }));
    e.classList.add(id === "f-cl" ? "in-l" : "in-r"); setTimeout(() => e.classList.remove("in-l", "in-r"), 60);
  };
  const myLv = levelFor(P.xp);
  card("f-cl", P.nick, myLv, P.frame, P.xp); card("f-cr", opp.nickname || "?", opp.level || 1, highestFrameFor(opp.level || 1), xpFor(opp.level || 1));
  $("f-vs").classList.remove("pop"); setTimeout(() => $("f-vs").classList.add("pop"), 350);
  const fill = $("f-fill"); fill.style.transition = "none"; fill.style.width = "3%"; void fill.offsetWidth; fill.style.transition = ""; fill.style.width = "100%";
  let left = 5; const tick = () => { $("f-pt").textContent = x.startIn(left); };
  tick(); clearInterval(foundTimer);
  foundTimer = setInterval(() => { left--; if (left <= 0) { clearInterval(foundTimer); if (current === "found") go(); } else tick(); }, 1000);
}
function highestFrameFor(lv) { try { const L = frameListSafe().filter((f) => f.level > 0 && !f.league && f.level <= lv); return (L.pop() || { id: "SCRIBBLER" }).id; } catch { return "SCRIBBLER"; } }
async function start(k = "quick") {
  if (k === "quick") return startQuick();
  levelRun = null; kind = k; relaxed = false;
  await ensureWords();
  begin([...words].sort(() => Math.random() - 0.5).slice(0, WORD_COUNT));
}
// Play Offline with the options chosen on the setup screen (WordCountScreen -> GameViewModel): the pool is cut by category and
// difficulty, shuffled, and the first N words are played; Untimed draws without the countdown.
async function startOffline({ count, relaxed: rx, cats, diffs }) {
  levelRun = null; kind = "offline"; relaxed = !!rx;
  await ensureWords();
  const pool = words.filter((w) => (!cats.length || cats.includes(w.category)) && (!diffs.length || diffs.includes(w.difficulty)));
  begin([...(pool.length ? pool : words)].sort(() => Math.random() - 0.5).slice(0, count));
}
// Duels (GameViewModel's duel args): "new" plays a random mix that becomes a challenge for a friend; "play" asks the same words a friend drew.
let duelRun = null;
async function startDuel(run, count = 10) {
  levelRun = null; kind = "duel"; relaxed = false; duelRun = run; quickOpp = null;
  await ensureWords();
  if (run.mode === "play") {
    const picked = run.duel.wordIds.map((id) => words.find((w) => w.id === id)).filter(Boolean);
    if (!picked.length) { ctx.confirm(S("duel_not_found"), S("close"), () => {}); $("dlg-no").style.display = "none"; return; }
    return begin(picked);
  }
  begin([...words].sort(() => Math.random() - 0.5).slice(0, count));
}
// The XP rate of the round, fixed when it starts so every word pays the same: a running XP event times the first Quick Match
// of the day (2x), as in GameViewModel.effectiveXpMultiplier.
let xpMult = 1, qmBonus = false, xpEventOnly = 1;
async function begin(picked) {
  xpEventOnly = await net.xpEventMultiplier().catch(() => 1);
  qmBonus = kind === "quick" && !!quickOpp && P.qmDay !== todayEpoch();
  xpMult = xpEventOnly * (qmBonus ? eco.QUICK_MATCH_DAILY_BONUS_MULTIPLIER : 1);
  round = picked.map((w) => ({ ...w, strokes: [], guess: "", correct: false, points: 0, ms: 0, xp: 0 }));
  idx = 0; tool = "pen"; score = 0; nextDraw();
}
function nextDraw() {
  clearInterval(dTimer);
  if (idx >= round.length) {
    idx = 0; answered = 0; locked = false; gOrder = kind === "tutorial" ? round.map((_, i) => i) : shuffledIdx(round.length);
    if (kind === "tutorial") return coachCard("tutorial_guess_title", "tutorial_guess_body", "tutorial_generic_button", nextGuess);
    return startBreak(ctx, nextGuess);
  }
  if (kind === "tutorial" && idx > 0 && !tutSeen["d" + idx]) {
    tutSeen["d" + idx] = true;
    return idx === 1 ? coachCard("tutorial_timed_title", "tutorial_timed_body", "tutorial_generic_button", nextDraw)
      : coachCard("tutorial_last_word_title", "tutorial_last_word_body", "tutorial_generic_button", nextDraw);
  }
  const w = round[idx];
  const tut = kind === "tutorial", untimed = tut && idx === 0;
  show("draw");
  $("d-ring").style.visibility = untimed ? "hidden" : "";
  // Normal mode shows the time left for the whole drawing phase beside the word number (the stopwatch pill of the app).
  const SW = '<svg class="sw" viewBox="0 0 24 24"><circle cx="12" cy="13.5" r="8" fill="#e8650f"/><path d="M12 8.5v5l3 2" stroke="#fff" stroke-width="2" fill="none" stroke-linecap="round"/><rect x="9" y="2" width="6" height="2.6" rx="1" fill="#e8650f"/></svg>';
  const restAfter = () => round.slice(idx + 1).reduce((s, q) => s + (DRAW_SECONDS[q.difficulty] ?? 7), 0);
  const paintProg = (left) => { $("d-prog").innerHTML = relaxed || tut ? `${idx + 1} / ${round.length}` : `${SW}${Math.floor((left + restAfter()) / 60)}:${String((left + restAfter()) % 60).padStart(2, "0")} · ${idx + 1}/${round.length}`; };
  $("d-word").style.animation = "none"; void $("d-word").offsetWidth; $("d-word").style.animation = "";
  $("d-word").textContent = w.text.charAt(0).toLocaleUpperCase(lang) + w.text.slice(1);
  const wl = w.text.length; $("d-word").style.fontSize = Math.max(40, Math.min(70, 425 * 0.97 / (wl * 0.5))) * u + "px";
  $("d-line").style.animation = "none"; void $("d-line").offsetWidth; $("d-line").style.animation = "";
  let total = tut ? TUT.drawSeconds[idx] : (DRAW_SECONDS[w.difficulty] ?? 7); let left = total, usedTime = false;
  const arc = $("d-arc"); arc.style.transition = "none"; setRing($("d-ring"), arc, $("d-time"), left, total); void arc.getBoundingClientRect(); arc.style.transition = "";
  paintProg(left);
  tool = "pen"; refreshTools(); paintLive();
  const tj = $("d-timej"), refreshTj = () => { const n = P.jokers.EXTRA_TIME || 0; $("d-timej-n").textContent = n; $("d-timej-t").textContent = S("drawing_time_joker"); tj.classList.toggle("dis", usedTime || n <= 0); tj.style.display = relaxed || tut ? "none" : ""; };
  refreshTj();
  tj.onclick = () => { if (usedTime || (P.jokers.EXTRA_TIME || 0) <= 0) return; usedTime = true; P.jokers.EXTRA_TIME--; saveP(); left += 10; total += 10; setRing($("d-ring"), arc, $("d-time"), left, total); sfx.buy(); refreshTj(); };
  if (!relaxed && !untimed) dTimer = setInterval(() => {
    left--; setRing($("d-ring"), arc, $("d-time"), left, total); paintProg(Math.max(0, left));
    if (left > 0 && left <= 3) sfx.tick();
    if (left <= 0) finishDraw();
  }, 1000);
}
function finishDraw() { clearInterval(dTimer); idx++; nextDraw(); }
$("d-next").onclick = finishDraw;

// The guess order is shuffled after the drawing (GameViewModel.startGuessPhase): `idx` is a position in gOrder, the drawings stay in
// the order they were made. A skipped word goes to the end of the queue with the seconds it still had.
let gOrder = [], answered = 0, locked = false, gLeft = 0;
const gw = () => round[gOrder[idx]];
const shuffledIdx = (n) => { const a = Array.from({ length: n }, (_, i) => i); for (let i = n - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };
let jk = { first: false, count: false };
function renderHint(w) {
  const box = $("g-blanks"); box.replaceChildren();
  const groups = w.text.split(" ").map((g) => [...g].filter((ch) => /\p{L}|\d/u.test(ch)));
  if (jk.count) {
    groups.forEach((g, gi) => { const e = document.createElement("div"); e.className = "grp"; g.forEach((ch, ci) => { const s = document.createElement("div"); s.className = "ch"; s.textContent = jk.first && gi === 0 && ci === 0 ? ch.toLocaleUpperCase(lang) : ""; e.append(s); }); box.append(e); });
  } else if (jk.first) {
    const e = document.createElement("div"); e.className = "txt ink"; e.style.cssText = "font-size:calc(var(--u)*40);background:#ffe9b0;border-radius:99px;padding:0 calc(var(--u)*26);height:calc(var(--u)*64)"; e.textContent = (lang === "tr" ? "İlk harf: " : "First letter: ") + groups[0][0].toLocaleUpperCase(lang); box.append(e);
  }
}
function refreshJokers(w) {
  const f = jokerN("FIRST_LETTER"), c = jokerN("LETTER_COUNT"), x = t(), tut = kind === "tutorial";
  $("jt-ad").style.display = "none"; // the "hint" (rewarded ad) tile does nothing on the web
  $("jt-first").querySelector("span").textContent = S("joker_first_letter_short"); $("jt-first").querySelector("i").textContent = f; $("jt-first").classList.toggle("dis", jk.first || f <= 0);
  $("jt-count").querySelector("span").textContent = S("joker_letter_count_short"); $("jt-count").querySelector("i").textContent = c; $("jt-count").classList.toggle("dis", jk.count || c <= 0);
  $("jt-skip").querySelector("span").textContent = S("skip_guess"); $("jt-skip").classList.toggle("dis", tut || !!w.skipped || idx >= gOrder.length - 1);
}
$("jt-first").onclick = () => { const w = gw(); if (locked) return; if (jk.first || jokerN("FIRST_LETTER") <= 0 || (tutSpot && tutSpot.joker !== "FIRST_LETTER")) return; jk.first = true; jokerSpend("FIRST_LETTER"); sfx.buy(); renderHint(w); refreshJokers(w); endSpot(); };
$("jt-count").onclick = () => { const w = gw(); if (locked) return; if (jk.count || jokerN("LETTER_COUNT") <= 0 || (tutSpot && tutSpot.joker !== "LETTER_COUNT")) return; jk.count = true; jokerSpend("LETTER_COUNT"); sfx.buy(); renderHint(w); refreshJokers(w); endSpot(); };
$("jt-skip").onclick = () => {
  const w = gw(); if (!w || locked || tutSpot || kind === "tutorial" || w.skipped || idx >= gOrder.length - 1) return;
  w.skipped = true; w.resume = gLeft; gOrder.push(gOrder[idx]); idx++; nextGuess();
};
function endSpot() {
  if (!tutSpot) return;
  const go = tutSpot.go; tutSpot = null;
  $("tut-spot").classList.remove("on"); for (const id of ["jt-first", "jt-count"]) $(id).classList.remove("spot");
  go();
}
function nextGuess() {
  clearInterval(gTimer); clearInterval(bonusTimer); locked = false;
  if (idx >= gOrder.length) return finish();
  if (kind === "tutorial" && idx === 2 && !tutSeen.free) { tutSeen.free = true; return coachCard("tutorial_joker_free_title", "tutorial_joker_free_body", "tutorial_generic_button", nextGuess); }
  const w = gw();
  show("guess");
  jk = { first: false, count: false }; renderHint(w); refreshJokers(w);
  $("g-prog").textContent = `${Math.min(answered + 1, round.length)} / ${round.length}`;
  $("g-xp").textContent = `★ +${round.reduce((s, q) => s + (q.xp || 0), 0)} XP`;
  $("g-input").value = "";
  paintGuess();
  const tut = kind === "tutorial", total = tut ? TUT.guessSeconds : guessSeconds(w); let left = w.resume ?? total; delete w.resume; gLeft = left;
  const arc = $("g-arc"); arc.style.transition = "none"; setRing($("g-ring"), arc, $("g-time"), left, total); void arc.getBoundingClientRect(); arc.style.transition = "";
  shownAt = performance.now();
  // Live preview of the speed bonus an answer right now would earn: <2s +3, <4s +2, <6s +1 (the same ladder as wordXp).
  const bonusEl = $("g-bonus"), STAGE = ["#4fd97d", "#2ea043", "#e0c22e", "#e8650f", "#e23b32"];
  const paintBonus = () => {
    const el = (performance.now() - shownAt) / 1000, b = el < 2 ? 3 : el < 4 ? 2 : el < 6 ? 1 : 0;
    bonusEl.textContent = lang === "tr" ? `+${b} XP bonus` : `+${b} XP bonus`; bonusEl.style.background = STAGE[Math.min(4, Math.floor(el / 2))];
  };
  const runClock = () => {
    shownAt = performance.now() - (total - left) * 1000; // a skipped word comes back with the time it had left; its speed bonus still counts from the first look
    clearInterval(bonusTimer); paintBonus(); bonusTimer = setInterval(paintBonus, 250);
    gTimer = setInterval(() => { left--; gLeft = left; setRing($("g-ring"), arc, $("g-time"), left, total); if (left > 0 && left <= 3) sfx.tick(); if (left <= 0) answer(""); }, 1000);
    $("g-input").focus({ preventScroll: true });
  };
  const lesson = tut && idx < 2 ? (idx === 0 ? ["FIRST_LETTER", "jt-first", "tutorial_joker_first_title", "tutorial_joker_first_body"] : ["LETTER_COUNT", "jt-count", "tutorial_joker_count_title", "tutorial_joker_count_body"]) : null;
  if (lesson) {
    // The clock stands still, the keyboard stays down, and only the joker being taught answers a tap, until it has been used.
    paintBonus(); $("g-input").blur();
    $("tut-t").textContent = S(lesson[2]); $("tut-b").textContent = S(lesson[3]); $("tut-spot").classList.add("on"); $(lesson[1]).classList.add("spot");
    tutSpot = { joker: lesson[0], go: runClock }; layout();
  } else runClock();
}
function answer(text) {
  if (locked) return; locked = true;
  clearInterval(gTimer); clearInterval(bonusTimer);
  const w = gw();
  w.guess = text; w.ms = Math.round(performance.now() - shownAt); w.correct = isCorrect(text, w.text);
  w.points = w.correct ? POINTS + (w.ms < SPEED_MS ? SPEED_BONUS : 0) : 0;
  // XP is not the score: a right word pays by difficulty and speed (XpAwards.wordXp); the daily challenge pays its own total.
  w.xp = w.correct && kind !== "daily" ? eco.wordXp(w.difficulty, w.ms) * xpMult : 0;
  score += w.points; answered++; // idx moves on only after the tick has been shown, so the drawing under it stays the one just answered
  if (w.correct) sfx.correct(); else { sfx.wrong(); vibrate(60); }
  showFeedback(w); setTimeout(() => { hideFeedback(); idx++; if (current === "guess") nextGuess(); }, 1200);
}
// GuessFeedbackOverlay: the badge over the drawing — a tick or a cross, the XP it paid, and the right word after a miss — for 1.2 s
const CHECK = '<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="#1e8a3a" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const CROSS = '<svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18" fill="none" stroke="#c0392b" stroke-width="3.2" stroke-linecap="round"/></svg>';
function showFeedback(w) {
  let f = $("g-fb"); if (!f) { f = document.createElement("div"); f.id = "g-fb"; $("g-canvas").append(f); }
  f.innerHTML = `<div class="${w.correct ? "ok" : "no"}"><div class="card"><div class="ic">${w.correct ? CHECK : CROSS}</div>${w.xp > 0 ? `<div class="xp">${S("xp_gained_format", w.xp)}</div>` : ""}${w.correct ? "" : `<div class="ans">${S("correct_answer_was", w.text.charAt(0).toLocaleUpperCase(lang) + w.text.slice(1))}</div>`}</div></div>`;
  $("g-input").readOnly = true; $("guess").classList.add("fb"); // read-only, not blurred: the keyboard stays up between words
}
function hideFeedback() { $("guess").classList.remove("fb"); const f = $("g-fb"); if (f) f.replaceChildren(); $("g-input").readOnly = false; }
function flash(ok) { const f = $("g-flash"); f.textContent = ok ? "✅" : "❌"; f.classList.remove("go"); void f.offsetWidth; f.classList.add("go"); }
// pressing a button must not take the focus from the answer field, or the keyboard drops (and comes back with the next word)
for (const id of ["g-ok", "jt-first", "jt-count", "jt-skip"]) $(id).addEventListener("pointerdown", (e) => e.preventDefault());
$("g-form").onsubmit = (e) => { e.preventDefault(); answer($("g-input").value); };
$("g-input").addEventListener("input", (e) => {
  const el = e.target; let v = el.value;
  // the first letter is always a capital
  if (v && v[0] !== v[0].toLocaleUpperCase(lang)) { const p = el.selectionStart; v = v[0].toLocaleUpperCase(lang) + v.slice(1); el.value = v; try { el.setSelectionRange(p, p); } catch {} }
  const w = gw(); if (!locked && w && v.trim() && v.trim().toLocaleLowerCase(lang) === w.text.toLocaleLowerCase(lang)) answer(v);
});

// ---------- result ----------
function addRow(parent, strokes, ok, label, sub, pen) {
  const row = document.createElement("div"); row.className = "row-card";
  const cv = document.createElement("canvas"), lb = document.createElement("div"), ic = document.createElement("img");
  lb.className = "lbl"; lb.textContent = label; if (sub) { const s = document.createElement("small"); s.textContent = sub; lb.append(s); }
  ic.src = ok ? "img/rs_ic_check.webp" : "img/rs_ic_x.webp"; ic.alt = "";
  row.append(cv, lb, ic); parent.append(row); fitCanvas(cv); paint(cv, strokes, false, pen || "CLASSIC");
}
function showWaiting() {
  const tr = lang === "tr";
  $("s-text").innerHTML = `<span>${tr ? "Sonuçlar" : "Getting your"}</span><span>${tr ? "hazırlanıyor" : "results"}<span id="s-dots"><i>.</i><i>.</i><i>.</i></span></span>`;
  $("s-sub").innerHTML = `<b>${tr ? "Puanlar hesaplanıyor…" : "Counting the points…"}</b>`;
  show("search");
}
async function finish() {
  const x = t();
  if (kind === "tutorial") return coachCard("tutorial_finale_title", "tutorial_finale_body", "tutorial_finale_button", endTutorial);
  // the last answer's tick is over and the result is being put together: show a waiting scene at once (instead of the last guess screen
  // hanging for a moment as if another word were coming) if it takes more than a blink
  const waitScene = setTimeout(() => { if (current === "guess") showWaiting(); }, 220);
  const n = round.filter((w) => w.correct).length, xp0 = P.xp, fxp = runeFx(P, "XP");
  if (P.periodId !== periodId()) { P.periodId = periodId(); P.periodXp = 0; }
  // XP is paid per right word by difficulty and speed (see answer()); the XP runes speed up the level, never the league XP.
  eco.addXp(P, round.reduce((s, w) => s + (w.xp || 0), 0), fxp);
  const ach0 = achCount(P); achRound(P, { score, words: round.length, perfect: n === round.length });
  const newAch = Math.max(0, achCount(P) - ach0);
  if (qmBonus) P.qmDay = todayEpoch();
  const fx = (k) => runeFx(P, k), has = (k) => runeHas(P, k);
  if (kind === "online") { ctx.lastXp = P.xp - xp0; ctx.lastAch = newAch; saveP(); refreshHome(); net.publishScore({ nickname: P.nick, periodXp: P.periodXp, periodId: periodId(), level: levelFor(P.xp), frameId: P.frame, totalXp: P.xp }).catch(() => {}); return submitAndWait(ctx, round, score); }
  net.publishScore({ nickname: P.nick, periodXp: P.periodXp, periodId: periodId(), level: levelFor(P.xp), frameId: P.frame, totalXp: P.xp }).catch(() => {});
  const me = {
    language: lang, nickname: "Web", wordIds: round.map((w) => w.id),
    perWord: round.map((w) => ({ wordId: w.id, isCorrect: w.correct, responseTimeMs: w.ms, pointsAwarded: w.points })),
    items: round.map((w) => ({ word: w.text, isCorrect: w.correct, strokes: w.strokes, penSkin: P.pen })),
  };
  const minWait = Promise.resolve();
  let opp = null, items = [];
  let stars = 0, dailyRes = null;
  let newStars = 0;
  if (kind === "daily") {
    dailyRes = dailyRecord(P, round.map((w) => w.correct), score);
    // The daily challenge pays its own XP total (times the streak), at the round's XP rate, instead of per-word XP.
    if (dailyRes) { dailyRes.xp *= xpMult; eco.addXp(P, dailyRes.xp, fxp); if (P.daily.result) P.daily.result.xp = dailyRes.xp; }
  }
  if (kind === "level") {
    stars = starsFor(n, round.length); const k = levelRun.w + ":" + levelRun.l, best = P.levels[k] ?? 0;
    newStars = Math.max(0, stars - best); recordLevel(P, levelRun.w, levelRun.l, stars);
    eco.addXp(P, eco.levelCompletionBonus(stars) * xpMult, fxp);
  }
  if (kind === "quick" && quickOpp) { opp = quickOpp; items = quickItems; net.recordRound({ ...me, level: levelFor(P.xp), frameId: P.frame, nickname: P.nick }).catch(() => {}); }
  let duelNote = null, duelRematch = null;
  if (kind === "duel" && duelRun) {
    const run = duelRun;
    if (run.mode === "new") { net.createDuel({ opponentUid: run.opp.uid, opponentName: run.opp.name, items: me.items, challengerScore: score, challengerCorrectCount: n, wordIds: me.wordIds, myName: P.nick }).catch(() => {}); duelNote = S("duel_challenge_sent", run.opp.name); }
    else { opp = { nickname: run.duel.challengerName, totalScore: run.duel.challengerScore, level: 1 }; items = run.duel.items; duelReceived(run.duel, score, n); net.submitDuelResult(run.duel.id, score, n).catch(() => {}); duelRematch = () => ctx.openDuel({ uid: run.duel.challengerUid, name: run.duel.challengerName }); }
  }
  await minWait;
  clearTimeout(waitScene);
  if (kind === "quick" && quickOpp && quickItemsP && !items.length) items = await Promise.race([quickItemsP, new Promise((r) => setTimeout(() => r(quickItems), 2500))]);
  const quickWon = kind === "quick" && !!opp && score > opp.totalScore;
  // What the round pays in gold — one place, ported from RoundRewards.kt (see economy.js).
  const gold = eco.soloGold({ fx, has, correctInOrder: round.map((w) => w.correct), isDaily: kind === "daily", newAchievements: newAch, newStars, dailyStreak: dailyRes ? dailyRes.streak : null, quickWon });
  P.gold += gold.total;
  let wonChest; if (quickWon) wonChest = awardChest(P, (k) => runeHas(P, k)) || null;
  saveP(); refreshHome();
  await loadResultStrings();
  show("res2"); sfx.over(); if (wonChest !== undefined) setTimeout(() => wonDialog(ctx, wonChest), 700);
  const lvl = levelFor(P.xp), into = P.xp - xpFor(lvl), need = xpFor(lvl + 1) - xpFor(lvl), l0 = levelFor(xp0);
  const ring = (fid) => () => { const fr = frameRing(frameById(fid)); fr.style.cssText += ";width:100%;height:100%"; return fr; };
  renderResult(ctx, {
    n, total: round.length, score, xp: P.xp - xp0, gold, eventMult: xpEventOnly, stars: kind === "level" ? stars : null, pen: P.pen, nick: P.nick,
    daily: dailyRes, flags: round.map((w) => w.correct),
    level: { level: lvl, before: l0, into, need, toNext: xpFor(lvl + 1) - P.xp, max: lvl >= 100, frac: lvl >= 100 ? 1 : into / need, beforeFrac: l0 === lvl ? (xp0 - xpFor(lvl)) / need : 0 },
    opp, myRing: ring(P.frame), oppRing: ring(opp ? highestFrameFor(opp.level || 1) : "SCRIBBLER"), oppItems: items,
    items: round.map((w) => ({ word: w.text, isCorrect: w.correct, strokes: w.strokes, penSkin: P.pen })),
    note: duelNote, rematch: duelRematch, rematchLabel: S("duel_rematch_action"),
    onHome: () => { show("home"); homeGain(gold.total, P.xp - xp0); },
    next: kind === "level" && levelRun && levelRun.l < 10 ? { label: t().nextLevel, fn: () => startLevel(levelRun.w, levelRun.l + 1) } : null,
  });
}

// ---------- navigation ----------
// GameViewModel.forfeitDaily: leaving a daily challenge ends today's chance — what was guessed so far counts, the rest is wrong.
function forfeitDaily() {
  const flags = round.map((w) => !!w.correct), res = dailyRecord(P, flags, score);
  if (res) { res.xp *= xpMult; eco.addXp(P, res.xp, runeFx(P, "XP")); if (P.daily.result) P.daily.result.xp = res.xp; }
  saveP(); refreshHome();
}
function confirmLeave() {
  const quickMatch = kind === "quick" && !!quickOpp, pen = eco.QUICK_MATCH_ABANDON_PENALTY_XP, daily = kind === "daily", online = kind === "online";
  const go = () => { clearInterval(dTimer); clearInterval(gTimer); if (daily) forfeitDaily(); if (quickMatch) { eco.revokeXp(P, pen); saveP(); refreshHome(); } if (online) leaveLobby(ctx); else show("home"); };
  exitDialog($("stage"), {
    title: quickMatch ? S("quick_match_abandon_title") : daily ? S("daily_exit_title") : S("exit_game_title"),
    message: quickMatch ? S("quick_match_abandon_message", pen) : daily ? S("daily_exit_message_short") : online ? S("exit_online_message") : S("exit_game_message"),
    confirm: quickMatch ? S("quick_match_abandon_confirm", pen) : S("exit_game_confirm"), dismiss: quickMatch ? S("quick_match_abandon_keep") : S("exit_game_cancel"), onConfirm: go,
  });
}
$("d-back").onclick = confirmLeave; $("g-back").onclick = confirmLeave;
$("again").onclick = () => { if (kind === "quick") startQuick(); else show(kind === "level" ? "lvmap" : "home"); };
$("found") && 0;
$("r-back").onclick = () => show("home");
let dailyTick = null;
function refreshDaily() {
  const s = dailyState(P), x = t();
  $("h-dmul").textContent = `🔥 ${s.next}x`;
  $("h-dpips").innerHTML = Array.from({ length: 5 }, (_, i) => { const f = s.result ? s.result.flags[i] : undefined; return `<i class="${f === true ? "ok" : f === false ? "no" : ""}">${f === true ? "✓" : f === false ? "✗" : ""}</i>`; }).join("");
  $("h-dcard").style.filter = s.done ? "none" : "hue-rotate(135deg)";
  $("h-dplay").style.display = s.done ? "none" : ""; $("h-dplay").textContent = x.dailyPlay + " ▸";
  $("h-dcount").style.display = s.done ? "" : "none";
  // the finished card (daily_done_badge): green pill with a tick on the right, and the title lettering outlined in dark green instead of purple
  $("h-ddone").style.display = s.done ? "flex" : "none"; $("h-ddone-t").textContent = S("daily_done_badge");
  $("h-ttl-daily").style.webkitTextStrokeColor = s.done ? "#0b4f2a" : "#2e1a66";
  const tick = () => { const ms = msToMidnight(), z = (n) => String(Math.floor(n)).padStart(2, "0"); $("h-dcount").textContent = `${z(ms / 3600000)}:${z(ms % 3600000 / 60000)}:${z(ms % 60000 / 1000)}`; };
  clearInterval(dailyTick); if (s.done) { tick(); dailyTick = setInterval(tick, 1000); }
}
/** The level line, rank and XP bar for a given XP total: the home screen's numbers, also what the claim animation climbs through. */
function paintLevel(xpVal) {
  const x = t(), lv = levelFor(xpVal), floor = xpFor(lv), ceil = lv >= 100 ? floor : xpFor(lv + 1), into = xpVal - floor, span = ceil - floor;
  const rank = [...RANKS].reverse().find((r) => lv >= r[0]);
  $("h-lvl").textContent = "• " + x.lvl + " " + lv;
  $("h-rank-t").textContent = rank[1] + " " + (lang === "tr" ? rank[2] : rank[3]);
  $("h-xpnext").textContent = lv >= 100 ? "MAX" : x.toNext(span - into, lv + 1);
  $("h-fill").style.width = (span ? into / span * 100 : 100) + "%"; $("h-barlabel").textContent = `${into} / ${span} XP`;
  return lv;
}
/** After "Ödülü Al": gold and XP climb from what the home screen showed to the real values, with a "+N" floating up beside each (HomePainted.kt). */
function homeGain(goldEarned, xpEarned) {
  goldEarned = Math.max(0, Math.round(goldEarned || 0)); xpEarned = Math.max(0, Math.round(xpEarned || 0));
  if (!goldEarned && !xpEarned) return;
  const g1 = P.gold, x1 = P.xp, g0 = Math.max(0, g1 - goldEarned), x0 = Math.max(0, x1 - xpEarned), t0 = performance.now(), D = 1500;
  const fl = (id, v, txt) => { const e = $(id); e.style.display = v ? "flex" : "none"; if (!v) return; e.textContent = txt; e.classList.remove("go"); void e.offsetWidth; e.classList.add("go"); };
  fl("h-gain-gold", goldEarned, "+" + goldEarned.toLocaleString(lang)); fl("h-gain-xp", xpEarned, "+" + xpEarned.toLocaleString(lang) + " XP");
  const step = (now) => {
    if (current !== "home") { $("h-gold").textContent = P.gold.toLocaleString(lang); paintLevel(P.xp); return; }
    const p = Math.min(1, (now - t0 - 350) / D), e = p <= 0 ? 0 : 1 - Math.pow(1 - p, 3);
    $("h-gold").textContent = Math.round(g0 + (g1 - g0) * e).toLocaleString(lang); paintLevel(Math.round(x0 + (x1 - x0) * e));
    if (p < 1) requestAnimationFrame(step); else { $("h-gold").textContent = g1.toLocaleString(lang); paintLevel(x1); }
  };
  $("h-gold").textContent = g0.toLocaleString(lang); paintLevel(x0); requestAnimationFrame(step);
}
function refreshHome() {
  const x = t(), lv = levelFor(P.xp);
  $("h-gold").textContent = P.gold.toLocaleString(lang);
  try { $("h-pen").textContent = penById(P.pen)[lang]; } catch { $("h-pen").textContent = x.goldLbl; }
  try { const fr = frameRing(frameById(P.frame)); fr.style.cssText += ";width:100%;height:100%"; $("h-avatar").replaceChildren(fr); } catch {}
  $("h-nick").textContent = P.nick; paintLevel(P.xp);
  for (const k of Object.keys(x.tiles)) $("tx-" + k).textContent = x.tiles[k];
  for (const k of ["gold", "chest"]) $("h-soon-" + k).textContent = x.appOnly;
  refreshDaily(); renderChests(ctx);
  // A rune can be bought now: the mascot shows a pulsing badge (MainMenuViewModel.runeReady).
  $("h-rbadge").style.display = anyAffordable(P, P.gold, lv) ? "block" : "none";
  $("h-ttl-gold").textContent = x.adGold; $("h-ttl-daily").textContent = x.daily; $("h-ttl-chest").textContent = x.adChest;
}
function msg(text, extra) {
  $("dlg-text").textContent = text; $("dlg-extra").replaceChildren(...(extra || []));
  $("dlg-no").style.display = "none"; $("dlg-yes").textContent = t().ok;
  $("dialog").classList.remove("hidden");
  $("dlg-yes").onclick = () => { $("dialog").classList.add("hidden"); $("dlg-no").style.display = ""; $("dlg-extra").replaceChildren(); };
}
const soon = (name) => () => msg(`${name} · ${t().soon}`);
// Online sections need the internet; offline ones (Play Offline, Levels) keep working. Same words as the app's offline screen.
const needNet = (fn) => (...a) => { if (navigator.onLine === false) return msg(lang === "tr" ? "İnternet bağlantısı gerekiyor. Bu bölümü oynamak için internete bağlanman gerekiyor. Çevrimdışı Oyna ve Seviyeler internetsiz de açık." : "An internet connection is required. Play Offline and Levels work without it."); return fn(...a); };
$("tl-online").onclick = needNet(async () => { await ensureWords(); show("onl"); });
$("on-back").onclick = () => show("home"); $("oj-back").onclick = () => show("onl"); $("oc-back").onclick = () => show("onl");
$("on-create").onclick = () => show("onc"); $("on-join").onclick = () => show("onj");
$("ow-back").onclick = () => { ctx.confirm(t().leaveQ || "?", t().yes, () => leaveLobby(ctx)); };
// the ad-reward tiles live only in the phone app: a tap goes straight to the store page
$("h-veil-gold").onclick = $("h-veil-chest").onclick = () => window.open(PLAY_URL, "_blank", "noopener"); $("h-rank").onclick = () => openRankLadder(ctx); $("h-avatar").onclick = () => openFramePicker(ctx); $("h-pen").onclick = () => openPenPicker(ctx); $("h-name").onclick = () => openNickEdit(ctx); $("h-rank").onclick = () => openRankLadder(ctx);
$("h-dplay").onclick = startDaily; $("h-dcard").onclick = () => { if (!dailyState(P).done) startDaily(); };
$("h-runes").onclick = async () => { await loadStoreData(); await loadRunes(); P.runes = P.runes || {}; net.runeConfig().then((d) => { applyRuneConfig(d); if (current === "runes") renderRunes(ctx, true); }).catch(() => {}); show("runes"); }; $("ru-back").onclick = () => show("home");
$("tl-quick").onclick = needNet(() => start("quick")); $("tl-offline").onclick = async () => { await ensureWords(); show("offs"); };
$("os-back").onclick = () => show("home");
$("tl-levels").onclick = async () => { await loadWorlds(); show("worlds"); };
$("wl-back").onclick = () => show("home"); $("lm-back").onclick = () => show("worlds");
$("tl-store").onclick = async () => { await loadStoreData(); show("store"); };
$("st-back").onclick = () => show("home");
$("tl-ach").onclick = async () => { await loadAchievements(); show("ach"); };
$("ac-back").onclick = () => show("home");
$("tl-league").onclick = needNet(async () => { await loadStoreData(); await loadLeague(); leagueLoaded = false; show("league"); });
$("lg-back").onclick = () => show("home");
$("lg-refresh").onclick = () => { leagueLoaded = false; layout(); };
$("tl-friends").onclick = needNet(() => { show("friends"); openFriends(ctx); });
$("fr-duel").onclick = () => { stopFriends(); show("duels"); openDuels(ctx); };
$("du-back").onclick = () => { stopDuels(); show("friends"); openFriends(ctx); };
$("fr-back").onclick = () => { stopFriends(); show("home"); };
for (const [id, k] of []) $("tl-" + id).onclick = () => soon(t().tiles[k])();
const soonMsg = () => msg(t().soon);
function rankInfo() {
  const lv = levelFor(P.xp), x = t(), rows = RANKS.map((r) => `${lv >= r[0] ? "✅" : "🔒"} ${r[1]} ${lang === "tr" ? r[2] : r[3]} — ${x.lvl} ${r[0]}`).join("\n");
  msg(rows); $("dlg-text").style.whiteSpace = "pre-line"; $("dlg-text").style.textAlign = "left"; const y = $("dlg-yes"), o = y.onclick; y.onclick = () => { $("dlg-text").style.whiteSpace = ""; $("dlg-text").style.textAlign = ""; o(); };
}
$("tl-settings").onclick = () => show("settings");
$("se-back").onclick = () => show("home");
$("ak-back").onclick = () => show("settings");
$("rp-back").onclick = () => show("settings");
const _oldSettings = () => {
  const mk = (code, label) => { const b = document.createElement("button"); b.className = "chip" + (lang === code ? " on" : ""); b.textContent = label; b.onclick = () => { lang = code; applyLang(); $("dialog").classList.add("hidden"); $("dlg-no").style.display = ""; $("dlg-extra").replaceChildren(); }; return b; };
  msg(t().langTitle, [mk("tr", "Türkçe"), mk("en", "English")]);
};
loadStoreData().then(refreshHome).catch(() => {});
// A name only this account owns (same collection the app uses), claimed once the network is reachable.
setTimeout(async () => { if (P.nickClaimed) return; const n = await net.ensureUsername(P.nick).catch(() => null); if (n) { P.nick = n; P.nickClaimed = true; saveP(); refreshHome(); } }, 2500);
setTimeout(() => net.listen("invites", (list) => {
  const inv = list[0]; if (!inv || current === "onw" || current === "draw" || current === "guess") return;
  const tx = lang === "tr" ? `${inv.fromNickname} seni davet etti!` : `${inv.fromNickname} invited you!`;
  ctx.confirm(tx, lang === "tr" ? "Katıl" : "Join", async () => { net.consumeInvite(inv.id).catch(() => {}); const r = await joinWithCode(ctx, inv.roomCode); if (r !== "ok") msg(lang === "tr" ? "Odaya katılınamadı." : "Couldn't join the room."); });
  $("dlg-no").onclick = () => { net.consumeInvite(inv.id).catch(() => {}); $("dialog").classList.add("hidden"); };
}).catch(() => {}), 4000);
applyLang();
setTimeout(() => net.economyConfig().then((d) => eco.applyConfig(d)).catch(() => {}), 1800);
window.__karalak = ctx; // handle for the visual checks in docs/web-parity.md (opens dialogs and scenes without playing a round)
loadStrings().then(() => { applyLang(); }).catch(() => {});
// A signed-in browser picks up the cloud save (when it holds more) and keeps it current; hiding the page uploads at once.
setTimeout(() => syncNow(ctx).then((r) => { if (r === "restored") { refreshHome(); if (current === "home") show("home"); } }), 2500);
document.addEventListener("visibilitychange", () => { if (document.visibilityState === "hidden") scheduleBackup(ctx, true); });
loadRunes().then(() => refreshHome()).catch(() => {});
if (!P.tutorialDone) { show("welcome"); $("w-go").onclick = startTutorial; $("w-skip").onclick = () => { P.tutorialDone = true; saveP(); show("home"); setTimeout(() => checkStreak(ctx), 900); }; } else { show("home"); setTimeout(() => checkStreak(ctx), 1800); }
if ("serviceWorker" in navigator) navigator.serviceWorker.register("sw.js").catch(() => {});
// After the home screen is up, quietly fetch the pictures of the screens people open most (skipped on data saver).
setTimeout(() => {
  if (navigator.connection && navigator.connection.saveData) return;
  const list = ["bg_draw", "bg_guess", "bg_result_wood", "bg_qm_search", "st_bg", "rs_head", "bg_league", "bg_offline", "bg_race2"];
  let i = 0; const next = () => { if (i >= list.length) return; const im = new Image(); im.onload = im.onerror = () => setTimeout(next, 400); im.src = `img/${list[i++]}.webp`; };
  (window.requestIdleCallback || ((f) => setTimeout(f, 1)))(next);
}, 3500);

// Quietly pulls in the pictures of the other scenes while the player is on the home screen (three at a time, only when the line is idle
// and "data saver" is off), so a scene opens with its background already there instead of building up piece by piece.
function warmImages() {
  if (navigator.connection && navigator.connection.saveData) return;
  const urls = [], seen = new Set(), add = (u) => { if (u && !seen.has(u)) { seen.add(u); urls.push(u); } };
  for (const id of ["draw", "guess", "search", "found", "store", "settings", "league", "friends", "onl", "onc", "onj", "offs", "onw", "ach", "worlds", "lvmap", "runes", "account", "report", "duels", "res2", "result"]) {
    document.querySelectorAll("#" + id + " img[data-src]").forEach((i) => add(i.dataset.src));
  }
  try { for (const sh of document.styleSheets) for (const r of sh.cssRules) for (const m of (r.cssText || "").matchAll(/url\(["']?(img\/[^"')]+)/g)) add(m[1]); } catch {}
  let next = 0;
  const pump = () => {
    if (next >= urls.length) { if (next++ === urls.length) warmMedia(); return; }
    const img = new Image(); img.onload = img.onerror = () => (window.requestIdleCallback ? requestIdleCallback(pump, { timeout: 800 }) : setTimeout(pump, 60));
    img.src = urls[next++];
  };
  for (let k = 0; k < 3; k++) pump();
}
addEventListener("load", () => setTimeout(() => (window.requestIdleCallback ? requestIdleCallback(warmImages, { timeout: 4000 }) : warmImages()), 1500));
// The Firebase SDK (sign-in, database: ~300 KB) is fetched only after the first screen is up.
addEventListener("load", () => { try { net.reportBoot(Math.round(performance.now())); } catch {} });
addEventListener("load", () => setTimeout(() => { for (const f of ["firebase-app", "firebase-auth", "firebase-firestore"]) { const l = document.createElement("link"); l.rel = "modulepreload"; l.href = `https://www.gstatic.com/firebasejs/10.14.1/${f}.js`; document.head.append(l); } }, 600));
// After the pictures: the first music track and the most common chest film, so neither stalls the first time it is needed (skipped on slow lines).
function warmMedia() {
  const c = navigator.connection; if (c && (c.saveData || /(^|-)(2g|3g)$/.test(c.effectiveType || ""))) return;
  for (const u of ["audio/music_1.m4a", "video/chest_open_apprentice.mp4"]) fetch(u).then((r) => r.arrayBuffer()).catch(() => {});
}

// A slim "get the app" bar on the home screen: Android gets the Play Store button, an iPhone gets the add-to-home-screen hint
// (there is no iOS app yet), a computer gets the Play link. It slides in a few seconds after the home screen is up, goes away by
// itself, and a tap on x silences it for a week.
(function appBar() {
  const KEY = "appbarHiddenUntil", ua = navigator.userAgent || "";
  const ios = /iPhone|iPad|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1), android = /Android/i.test(ua);
  const standalone = (window.matchMedia && matchMedia("(display-mode: standalone)").matches) || navigator.standalone === true;
  const T = {
    tr: { app: "Karalak uygulamasını indir, ödülleri orada kazan!", get: "İndir", ios: "Karalak'ı ana ekranına ekle: Paylaş simgesine, sonra “Ana Ekrana Ekle”ye dokun." },
    en: { app: "Get the Karalak app and claim rewards there!", get: "Get it", ios: "Add Karalak to your home screen: tap Share, then “Add to Home Screen”." },
  };
  const hiddenUntil = () => { try { return +localStorage.getItem(KEY) || 0; } catch { return 0; } };
  let shown = false, timer = 0;
  const bar = $("appbar"), hide = (forWeek) => { bar.classList.remove("on"); clearTimeout(timer); if (forWeek) { try { localStorage.setItem(KEY, String(Date.now() + 7 * 864e5)); } catch {} } };
  $("ab-x").onclick = () => hide(true);
  function maybeShow() {
    if (shown || standalone || current !== "home" || Date.now() < hiddenUntil() || P.tutorialDone !== true) return;
    shown = true;
    const x = T[lang] || T.tr;
    $("ab-t").textContent = ios ? x.ios : x.app;
    const go = $("ab-go"); go.textContent = ios ? "" : x.get; go.href = PLAY_URL;
    bar.classList.add("on"); timer = setTimeout(() => hide(false), 12000);
  }
  setTimeout(maybeShow, 5000);
  // leaving the home screen takes the bar away
  new MutationObserver(() => { if (current !== "home") hide(false); }).observe($("stage"), { subtree: true, attributes: true, attributeFilter: ["class"] });
})();
