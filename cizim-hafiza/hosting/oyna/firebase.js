// Firebase layer. Loaded lazily so the game still works offline / before config is filled in.
import { firebaseConfig, firebaseReady } from "./config.js";
import { parseGhostId, ghostOutcome } from "./ghost.js";

const V = "10.14.1";
const CDN = `https://www.gstatic.com/firebasejs/${V}`;
const RUN_WORD_COUNT = 10, SHARD_COUNT = 1000, BAND_SIZE = 10, MIN_CORRECT = 2, MAX_BAND = 9, CANDIDATES = 5;
let api = null;

let initP = null, perf = null, bootMs = 0;
/** How long the game took from navigation start to the first usable home screen, as a Performance trace (sent once the SDK is up). */
export function reportBoot(ms) {
  bootMs = ms; if (!perf || !ms) return;
  try { const t = perf.p.trace(perf.inst, "home_ready"); t.record(Date.now() - ms, ms); bootMs = 0; } catch {}
}
// One shared start-up: concurrent callers (the room listener, the cloud save, the friends screen…) must not each sign in anonymously.
function init() {
  if (api) return Promise.resolve(api);
  if (!initP) initP = (async () => {
    if (!firebaseReady()) throw new Error('no-config');
    const [app, auth, fs] = await Promise.all([
      import(`${CDN}/firebase-app.js`), import(`${CDN}/firebase-auth.js`), import(`${CDN}/firebase-firestore.js`),
    ]);
    const a = app.initializeApp(firebaseConfig);
    const au = auth.getAuth(a);
    await au.authStateReady?.();
    if (!au.currentUser) await auth.signInAnonymously(au);
    api = { fs, db: fs.getFirestore(a), auth: au };
    // Firebase Performance: page-load and network timings of real visitors (no personal data); a failure here never matters.
    import(`${CDN}/firebase-performance.js`).then((p) => { perf = { p, inst: p.getPerformance(a) }; if (bootMs) reportBoot(bootMs); }).catch(() => {});
    return api;
  })().catch((e) => { initP = null; throw e; });
  return initP;
}

export const levelBandFor = (level) => Math.floor((Math.max(1, level) - 1) / BAND_SIZE);

// Mirrors GhostRunRepositoryImpl.findOpponent (without the bot fallback): own band first, then outwards.
export async function findOpponent(language, level = 1) {
  const { fs, db, auth } = await init();
  const own = levelBandFor(level), uid = auth.currentUser.uid;
  const bands = Array.from({ length: MAX_BAND + 1 }, (_, i) => i).sort((a, b) => Math.abs(a - own) - Math.abs(b - own) || a - b);
  const run = async (band, pivot, above) => {
    const q = fs.query(fs.collection(db, "ghostRuns"),
      fs.where("shard", above ? ">=" : "<", pivot), fs.where("language", "==", language),
      fs.where("mode", "==", "NORMAL"), fs.where("wordCount", "==", RUN_WORD_COUNT),
      fs.where("levelBand", "==", band), fs.orderBy("shard", above ? "asc" : "desc"), fs.limit(CANDIDATES));
    const snap = await fs.getDocs(q);
    return snap.docs.map((d) => ({ id: d.id, ...d.data() })).find((r) => r.uid !== uid && !("itemsJson" in r));
  };
  for (const band of bands) {
    const pivot = Math.floor(Math.random() * SHARD_COUNT);
    const found = (await run(band, pivot, true)) || (await run(band, pivot, false));
    if (found) return found;
  }
  return null;
}

let trainedCache = null;
/** The ids of every hand-trained word (botTrainingIndex/trained), read once per visit — the pool Hızlı Eşleş opponents are built from. */
export async function trainedWordIds() {
  if (trainedCache) return trainedCache;
  const { fs, db } = await init();
  const snap = await fs.getDoc(fs.doc(db, "botTrainingIndex", "trained"));
  const ids = snap.exists() && Array.isArray(snap.data().wordIds) ? snap.data().wordIds.map(Number).filter(Number.isFinite) : [];
  return ids.length >= RUN_WORD_COUNT ? (trainedCache = ids) : [];
}
/** The opponent's side of a synthesized round: her trained drawing for each word, marked by the same seeded roll as her score. */
async function loadBotItems(runId, texts, points, speedBonus) {
  const p = parseGhostId(runId); if (!p) return [];
  const { fs, db } = await init(), correctness = ghostOutcome(p.seed, p.wordIds, points, speedBonus).correctness;
  const items = await Promise.all(p.wordIds.map(async (id, i) => {
    try {
      const d = await fs.getDoc(fs.doc(db, "botTrainedWords", String(id)));
      if (!d.exists()) return null;
      return { word: (texts && texts.get(id)) || d.data().word || "", isCorrect: !!correctness[i], strokes: JSON.parse(d.data().strokesJson || "[]") };
    } catch { return null; }
  }));
  return items.filter(Boolean);
}
export async function loadItems(runId, texts, points = 5, speedBonus = 2) {
  if (parseGhostId(runId)) return loadBotItems(runId, texts, points, speedBonus);
  const { fs, db } = await init();
  const snap = await fs.getDoc(fs.doc(db, "ghostRunItems", runId));
  return snap.exists() ? JSON.parse(snap.data().itemsJson) : [];
}

// Files a finished round in the review queue, same document shape as Android's write().
export async function recordRound({ language, nickname, wordIds, perWord, items, level = 1, frameId = "SCRIBBLER" }) {
  const correctCount = perWord.filter((p) => p.isCorrect).length;
  if (wordIds.length < RUN_WORD_COUNT || correctCount < MIN_CORRECT) return false;
  const { fs, db, auth } = await init();
  const uid = auth.currentUser.uid;
  const ref = fs.doc(fs.collection(db, "pendingRuns"));
  const correctTimes = perWord.filter((p) => p.isCorrect).map((p) => p.responseTimeMs);
  const batch = fs.writeBatch(db);
  batch.set(ref, {
    uid, nickname, level, frameId, levelBand: levelBandFor(level), shard: Math.floor(Math.random() * SHARD_COUNT),
    language, mode: "NORMAL", wordCount: wordIds.length, wordIds,
    totalScore: perWord.reduce((s, p) => s + p.pointsAwarded, 0), correctCount,
    fastestCorrectMs: correctTimes.length ? Math.min(...correctTimes) : null,
    perWord, xpEarned: 0, createdAt: Date.now(),
  });
  batch.set(fs.doc(db, "pendingRunItems", ref.id), { uid, itemsJson: JSON.stringify(items) });
  await batch.commit();
  return true;
}

// Same transaction as Android's CouponRepositoryImpl.redeem.
export async function redeemCoupon(rawCode) {
  const code = rawCode.trim().toUpperCase().replace(/ /g, "");
  if (code.length < 4 || code.length > 24 || !/^[A-Z0-9-]+$/.test(code)) return { status: "notfound" };
  const { fs, db, auth } = await init();
  const uid = auth.currentUser.uid;
  const cref = fs.doc(db, "coupons", code), rref = fs.doc(db, "coupons", code, "redemptions", uid);
  try {
    return await fs.runTransaction(db, async (tx) => {
      const snap = await tx.get(cref);
      if (!snap.exists()) return { status: "notfound" };
      const c = snap.data(), now = Date.now();
      if ((c.expiresAt || 0) > 0 && now >= c.expiresAt) return { status: "expired" };
      if ((c.usedCount || 0) >= (c.maxUses || 1)) return { status: "exhausted" };
      if ((await tx.get(rref)).exists()) return { status: "used" };
      tx.set(rref, { redeemedAt: now });
      tx.update(cref, { usedCount: (c.usedCount || 0) + 1 });
      return { status: "ok", gold: Math.min(c.gold || 0, 10000000), xp: Math.min(c.xp || 0, 100000), chests: (c.chestSilver || 0) + (c.chestGold || 0) + (c.chestRare || 0) };
    });
  } catch (e) { return { status: "network" }; }
}

// config/xpEvent: a 2x-XP event started from the developer panel (XpEventRepositoryImpl). Cached for five minutes.
let xpEv = { at: 0, mult: 1 };
export async function xpEventMultiplier() {
  if (Date.now() - xpEv.at < 300000) return xpEv.mult;
  let mult = 1;
  try {
    const { fs, db } = await init(), s = await fs.getDoc(fs.doc(db, "config", "xpEvent")), d = s.exists() ? s.data() : null;
    if (d && d.active === true && Date.now() < (d.endsAtMillis || 0)) mult = Math.max(1, Math.min(10, Math.trunc(d.multiplier || 1)));
  } catch {}
  xpEv = { at: Date.now(), mult }; return mult;
}
// config/economy: the amounts gold pays, tunable without an update (EconomyConfigRepository).
// config/runes: when each rune ring opens and what the later rings cost (RuneConfigRepository).
export async function runeConfig() {
  try { const { fs, db } = await init(), s = await fs.getDoc(fs.doc(db, "config", "runes")); return s.exists() ? s.data() : null; } catch { return null; }
}
export async function economyConfig() {
  try { const { fs, db } = await init(), s = await fs.getDoc(fs.doc(db, "config", "economy")); return s.exists() ? s.data() : null; } catch { return null; }
}
export async function getGlobalLeague() {
  const { fs, db } = await init();
  const snap = await fs.getDoc(fs.doc(db, "leaderboards", "global"));
  return snap.exists() ? snap.data() : null;
}

// FriendRepositoryImpl.observeLeagueTable: me + friends, each profile's totals; periodXp counts only for the current period.
export async function friendsLeague(pid) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid;
  const ids = (await fs.getDocs(fs.collection(db, "users", uid, "friends"))).docs.map((d) => d.id);
  const out = [];
  for (const id of [uid, ...ids]) {
    try {
      const s = await fs.getDoc(fs.doc(db, "users", id)); if (!s.exists()) continue; const d = s.data();
      out.push({ uid: id, me: id === uid, nickname: d.nickname || "?", periodXp: d.periodId === pid ? d.periodXp || 0 : 0, totalXp: d.totalXp || 0, level: d.level || 1, frameId: d.frameId });
    } catch {}
  }
  return out.sort((a, b) => b.totalXp - a.totalXp);
}

// Same fields as FriendRepositoryImpl.publishLeagueScore. A guest's web account is its own uid, so it never
// collides with an Android profile.
// True while the server refuses this account's score (permission denied: a jump bigger than the rules' growth allowance).
// Being offline is not this. The league page tells the player instead of leaving them out of the table without a word.
let scoreRefused = false;
export const isScoreRefused = () => scoreRefused;
export async function publishScore({ nickname, periodXp, periodId, level, frameId, totalXp }) {
  const { fs, db, auth } = await init(), ref = fs.doc(db, "users", auth.currentUser.uid);
  try {
    await fs.setDoc(ref, { nickname, periodXp, totalXp, periodXpAt: fs.serverTimestamp(), periodId, level, frameId, avatarUrl: "" }, { merge: true });
    scoreRefused = false;
  } catch (e) {
    if (e && e.code === "permission-denied") {
      scoreRefused = true;
      // Keep the profile (name, level, frame) current even though the score itself is refused.
      try { await fs.setDoc(ref, { nickname, totalXp, level, frameId, avatarUrl: "" }, { merge: true }); } catch {}
      // Then climb to the real score in steps the rules accept (30000 plus 3000 an hour since the last score write per step).
      // The hourly table build refuses a total no honest month can reach, which is the real guard against inflated scores.
      try {
        const d = await fs.getDoc(ref), data = d.exists() ? d.data() : {};
        let cur = data.periodId === periodId ? data.periodXp || 0 : 0;
        const stamp = data.periodXpAt && data.periodXpAt.toMillis ? data.periodXpAt.toMillis() : null;
        let allow = !d.exists() || stamp == null ? 300000 : 30000 + Math.max(0, Date.now() - stamp) / 1200;
        for (let i = 0; i < 12 && cur < periodXp; i++) {
          const next = Math.min(periodXp, Math.floor(cur + allow - 1000)); if (next <= cur) break;
          await fs.setDoc(ref, { periodXp: next, periodXpAt: fs.serverTimestamp(), periodId }, { merge: true });
          cur = next; allow = 30000;
        }
        if (cur >= periodXp) { scoreRefused = false; return; }
      } catch {}
    }
    throw e;
  }
}

// ---- Browser accounts: user name + password ----
// Play Store players sign in with Google; the browser has no Google button, so a web account is an email/password account whose
// "email" is made from the user name (name@users.karalak.app — never shown, never mailed). A guest's anonymous uid is LINKED to it,
// so the progress and league entry made before signing up stay with the account. Needs the Email/Password provider enabled in
// Firebase Authentication.
const MAIL = "@users.karalak.app";
export const validUserName = (s) => /^[a-z0-9_.]{3,16}$/.test(s);
export async function authState() {
  const { auth } = await init(), u = auth.currentUser;
  return u ? { uid: u.uid, anon: u.isAnonymous, email: u.email || "", userName: (u.email || "").endsWith(MAIL) ? u.email.slice(0, -MAIL.length) : "" } : null;
}
export async function signUpUserName(name, password) {
  const { auth } = await init(), m = await import(`${CDN}/firebase-auth.js`), cred = m.EmailAuthProvider.credential(name + MAIL, password), cur = auth.currentUser;
  if (cur && cur.isAnonymous) await m.linkWithCredential(cur, cred); else await m.createUserWithEmailAndPassword(auth, name + MAIL, password);
  return authState();
}
export async function signInUserName(name, password) {
  const { auth } = await init(), m = await import(`${CDN}/firebase-auth.js`);
  await m.signInWithEmailAndPassword(auth, name + MAIL, password);
  return authState();
}
export async function signOutAccount() {
  const { auth } = await init(), m = await import(`${CDN}/firebase-auth.js`);
  await m.signOut(auth); await m.signInAnonymously(auth);
  return authState();
}

// ================= Friends (same documents and rules as FriendRepositoryImpl) =================
export async function myUid() { return (await init()).auth.currentUser.uid; }
export async function ensureFriendCode(nickname) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid;
  const me = await fs.getDoc(fs.doc(db, "users", uid));
  if (me.exists() && me.data().friendCode) return me.data().friendCode;
  for (let i = 0; i < 5; i++) {
    const code = String(100000 + Math.floor(Math.random() * 900000)), ref = fs.doc(db, "friendCodes", code);
    const ok = await fs.runTransaction(db, async (tx) => {
      if ((await tx.get(ref)).exists()) return false;
      tx.set(ref, { uid }); tx.set(fs.doc(db, "users", uid), { nickname, friendCode: code, createdAt: Date.now() }, { merge: true }); return true;
    });
    if (ok) return code;
  }
  throw new Error("code");
}
// Live lists. Returns an unsubscribe function.
export async function listen(sub, cb) {
  const { fs, db, auth } = await init();
  return fs.onSnapshot(fs.collection(db, "users", auth.currentUser.uid, sub), (s) => cb(s.docs.map((d) => ({ id: d.id, ...d.data() }))), () => cb([]));
}
// A day's quota of new friend requests (same as the app's FriendRequestLimit): the rules cannot count, so the client does.
const REQ_MAX = 20, REQ_WINDOW = 24 * 3600e3;
const reqLog = () => { try { return JSON.parse(localStorage.getItem("friendReqLog") || "[]").filter((t) => Date.now() - t < REQ_WINDOW); } catch { return []; } };
export async function addFriendByCode(code, myNick) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid;
  const c = await fs.getDoc(fs.doc(db, "friendCodes", code));
  if (!c.exists()) return { status: "notfound" };
  const fu = c.data().uid; if (fu === uid) return { status: "self" };
  const fdoc = await fs.getDoc(fs.doc(db, "users", fu)), fn = (fdoc.exists() && fdoc.data().nickname) || "?";
  if ((await fs.getDoc(fs.doc(db, "users", uid, "friends", fu))).exists()) return { status: "already", name: fn };
  if ((await fs.getDoc(fs.doc(db, "users", uid, "friendRequests", fu))).exists()) { await writeFriendship(uid, myNick, fu, fn); return { status: "added", name: fn }; }
  const log = reqLog();
  if (log.length >= REQ_MAX) return { status: "limit" };
  try { await fs.setDoc(fs.doc(db, "users", fu, "friendRequests", uid), { fromNickname: myNick, sentAt: Date.now() }); }
  catch (e) { return { status: "blocked" }; }
  try { localStorage.setItem("friendReqLog", JSON.stringify([...log, Date.now()])); } catch {}
  return { status: "sent", name: fn };
}
async function writeFriendship(uid, myNick, fu, fn) {
  const { fs, db } = await init(), b = fs.writeBatch(db), now = Date.now();
  b.set(fs.doc(db, "users", uid, "friends", fu), { nickname: fn, addedAt: now });
  b.set(fs.doc(db, "users", fu, "friends", uid), { nickname: myNick, addedAt: now });
  b.delete(fs.doc(db, "users", uid, "friendRequests", fu));
  await b.commit();
}
export async function acceptRequest(r, myNick) { await writeFriendship(await myUid(), myNick, r.id, r.fromNickname || "?"); }
export async function declineRequest(fromUid) { const { fs, db, auth } = await init(); await fs.deleteDoc(fs.doc(db, "users", auth.currentUser.uid, "friendRequests", fromUid)); }
export async function removeFriend(fu) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid, b = fs.writeBatch(db);
  b.delete(fs.doc(db, "users", uid, "friends", fu)); b.delete(fs.doc(db, "users", fu, "friends", uid)); await b.commit();
}
export async function blockUser(uid2, nick) { const { fs, db, auth } = await init(); await fs.setDoc(fs.doc(db, "users", auth.currentUser.uid, "blockedUsers", uid2), { nickname: nick, blockedAt: Date.now() }); }
export async function unblockUser(uid2) { const { fs, db, auth } = await init(); await fs.deleteDoc(fs.doc(db, "users", auth.currentUser.uid, "blockedUsers", uid2)); }
export async function sendInvite(toUid, roomCode, myNick) {
  const { fs, db, auth } = await init();
  await fs.addDoc(fs.collection(db, "users", toUid, "invites"), { fromUid: auth.currentUser.uid, fromNickname: myNick, roomCode, sentAt: Date.now() });
}

// ================= Race a Friend: rooms/{code} (same documents as OnlineGameRepositoryImpl) =================
const playerMap = (displayName, level, frameId, pendingNextRound = false, teamId = null) => ({
  displayName, level, frameId, avatarUrl: "", joinedAt: Date.now(), lastSeenAt: Date.now(),
  ready: false, finished: false, left: false, totalScore: 0, correctCount: 0, wrongCount: 0, fastestCorrectMs: null, pendingNextRound, teamId,
});
export async function createRoom(nick, level, frameId, { wordCount, difficulty, mode, category, teamMode = false }) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid;
  for (let i = 0; i < 5; i++) {
    const code = String(100000 + Math.floor(Math.random() * 900000)), ref = fs.doc(db, "rooms", code);
    const ok = await fs.runTransaction(db, async (tx) => {
      if ((await tx.get(ref)).exists()) return false;
      tx.set(ref, { hostUid: uid, createdAt: Date.now(), status: "WAITING", wordCount, category: category || null, difficulty: difficulty || null, mode, wordIds: [],
        players: { [uid]: playerMap(nick, level, frameId, false, teamMode ? "A" : null) }, rematchVotes: [], teamMode: !!teamMode });
      return true;
    });
    if (ok) return code;
  }
  throw new Error("code");
}
export async function joinRoom(code, nick, level, frameId) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid, ref = fs.doc(db, "rooms", code);
  return fs.runTransaction(db, async (tx) => {
    const s = await tx.get(ref);
    if (!s.exists()) return "notfound";
    const d = s.data(), players = d.players || {}, now = Date.now();
    const kicked = d.kickedUsers && d.kickedUsers[uid];
    if (kicked && kicked.until > now) return "kicked";
    const present = Object.values(players).filter((p) => !p.left && now - (p.lastSeenAt || p.joinedAt || 0) <= 150000);
    const team = !!d.teamMode;
    if (!(uid in players) && present.length >= (team ? 4 : 8)) return "full";
    // a returning player keeps their team; a new one goes to the team with fewer present players (ties: A)
    const teamId = !team ? null : (players[uid] && players[uid].teamId) || (present.filter((p) => p.teamId === "A").length <= present.filter((p) => p.teamId === "B").length ? "A" : "B");
    if (d.status === "WAITING") tx.update(ref, { ["players." + uid]: playerMap(nick, level, frameId, false, teamId) });
    else tx.update(ref, { ["players." + uid]: playerMap(nick, level, frameId, true, teamId) });
    return "ok";
  });
}
export async function watchRoom(code, cb) {
  const { fs, db } = await init();
  return fs.onSnapshot(fs.doc(db, "rooms", code), (s) => cb(s.exists() ? { id: s.id, ...s.data() } : null), () => cb(null));
}
export async function watchReactions(code, cb) {
  const { fs, db } = await init(), cutoff = Date.now() - 5 * 60000;
  const q = fs.query(fs.collection(db, "rooms", code, "reactions"), fs.where("sentAt", ">", cutoff), fs.orderBy("sentAt"), fs.limitToLast(50));
  return fs.onSnapshot(q, (s) => cb(s.docs.map((d) => d.data()).filter((r) => r.uid && r.emoji != null)), () => cb([]));
}
export async function sendReaction(code, emoji, messageKey) {
  const { fs, db } = await init(); const uid = await myUid();
  await fs.addDoc(fs.collection(db, "rooms", code, "reactions"), { uid, emoji, messageKey, sentAt: Date.now() });
}
const upd = async (code, data) => { const { fs, db } = await init(); await fs.updateDoc(fs.doc(db, "rooms", code), data); };
export const setTeam = async (code, teamId) => upd(code, { ["players." + (await myUid()) + ".teamId"]: teamId });
export async function kickPlayer(code, uid, name) { await upd(code, { ["players." + uid]: (await init()).fs.deleteField(), ["kickedUsers." + uid]: { displayName: name, until: Date.now() + 30 * 60000 } }); }
export async function unbanPlayer(code, uid) { await upd(code, { ["kickedUsers." + uid]: (await init()).fs.deleteField() }); }
export const setReady = async (code, ready) => upd(code, { ["players." + (await myUid()) + ".ready"]: ready });
export const touchPresence = async (code, level, frameId) => upd(code, { ["players." + (await myUid()) + ".lastSeenAt"]: Date.now(), ["players." + (await myUid()) + ".level"]: level, ["players." + (await myUid()) + ".frameId"]: frameId });
export const leaveRoom = async (code) => upd(code, { ["players." + (await myUid()) + ".left"]: true });
export async function startCountdown(code) { await upd(code, { countdownStartedAt: Date.now() }); }
export async function cancelCountdown(code) { const { fs } = await init(); await upd(code, { countdownStartedAt: fs.deleteField() }); }
export async function startGame(code, wordIds) { const { fs } = await init(); await upd(code, { status: "PLAYING", wordIds, startedAt: Date.now(), countdownStartedAt: fs.deleteField() }); }
export async function submitRoomResult(code, totalScore, correctCount, wrongCount, fastestCorrectMs, items) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid, ref = fs.doc(db, "rooms", code);
  await fs.setDoc(fs.doc(db, "rooms", code, "results", uid), { itemsJson: JSON.stringify(items) });
  await upd(code, { [`players.${uid}.finished`]: true, [`players.${uid}.totalScore`]: totalScore, [`players.${uid}.correctCount`]: correctCount, [`players.${uid}.wrongCount`]: wrongCount, [`players.${uid}.fastestCorrectMs`]: fastestCorrectMs });
  await fs.runTransaction(db, async (tx) => {
    const s = await tx.get(ref); if (!s.exists()) return;
    const d = s.data(); if (d.status === "FINISHED") return;
    const active = Object.values(d.players || {}).filter((p) => !p.pendingNextRound && !p.left);
    if (active.length && active.every((p) => p.finished)) tx.update(ref, { status: "FINISHED" });
  });
}
export async function roomResultItems(code, uid) {
  const { fs, db } = await init();
  const s = await fs.getDoc(fs.doc(db, "rooms", code, "results", uid));
  try { return s.exists() ? JSON.parse(s.data().itemsJson) : []; } catch { return []; }
}
export async function voteRematch(code) { const { fs } = await init(); await upd(code, { rematchVotes: fs.arrayUnion(await myUid()) }); }
export async function returnToWaiting(code) {
  const { fs, db } = await init(), ref = fs.doc(db, "rooms", code);
  await fs.runTransaction(db, async (tx) => {
    const s = await tx.get(ref); if (!s.exists() || s.data().status !== "FINISHED") return;
    const players = {};
    for (const [u, p] of Object.entries(s.data().players || {})) if (!p.left) players[u] = playerMap(p.displayName || "", p.level || 1, p.frameId || "SCRIBBLER");
    tx.update(ref, { status: "WAITING", wordIds: [], players, rematchVotes: [] });
  });
}
export async function consumeInvite(id) { const { fs, db, auth } = await init(); await fs.deleteDoc(fs.doc(db, "users", auth.currentUser.uid, "invites", id)); }

// ================= Usernames: usernames/{foldedName} -> { uid, name, createdAt } (same rules as UsernameRepository) =================
export const usernameValid = (n) => n.length >= 2 && n.length <= 16 && /^[\p{L}\p{N}_.\- ]+$/u.test(n) && /[\p{L}\p{N}]/u.test(n);
export const usernameKey = (n) => n.trim().normalize("NFD").replace(/\p{M}+/gu, "").replace(/ı/g, "i").replace(/İ/g, "i").toLowerCase().replace(/ /g, "_");
// Claims `name` for this account, releasing `oldName` if it was ours. Returns "ok" | "taken" | "invalid" | "net".
export async function claimUsername(name, oldName) {
  name = name.trim().replace(/\s+/g, " "); if (!usernameValid(name)) return "invalid";
  try {
    const { fs, db, auth } = await init(), uid = auth.currentUser.uid;
    const nref = fs.doc(db, "usernames", usernameKey(name)), oref = oldName ? fs.doc(db, "usernames", usernameKey(oldName)) : null;
    const taken = await fs.runTransaction(db, async (tx) => {
      const ns = await tx.get(nref), os = oref && oref.path.join("/") !== nref.path.join("/") ? await tx.get(oref) : null;
      if (ns.exists() && ns.data().uid !== uid) return true;
      if (!ns.exists()) tx.set(nref, { uid, name, createdAt: Date.now() });
      if (os && os.exists() && os.data().uid === uid) tx.delete(oref);
      return false;
    });
    if (taken) return "taken";
    await fs.setDoc(fs.doc(db, "users", uid), { nickname: name, username: name }, { merge: true });
    return "ok";
  } catch (e) { return "net"; }
}
export async function ensureUsername(current) {
  for (let i = 0; i < 12; i++) {
    const name = i === 0 ? current : "Karalak" + (100 + Math.floor(Math.random() * 1000 * (i + 1) * 10));
    const r = await claimUsername(name, i === 0 ? null : current);
    if (r === "ok") return name; if (r === "net") return null;
  }
  return null;
}

// ================= Bug reports (same document as BugReportRepositoryImpl) =================
export async function sendReport(category, description) {
  const { fs, db, auth } = await init();
  await fs.addDoc(fs.collection(db, "bugReports"), { uid: auth.currentUser.uid, category, description: description.trim().slice(0, 2000), appVersionName: "web", appVersionCode: 0, deviceModel: navigator.userAgent.slice(0, 80), androidSdk: 0, submittedAt: Date.now() });
}
export async function myReports() {
  const { fs, db, auth } = await init();
  const s = await fs.getDocs(fs.query(fs.collection(db, "bugReports"), fs.where("uid", "==", auth.currentUser.uid)));
  return s.docs.map((d) => ({ id: d.id, ...d.data() })).sort((a, b) => b.submittedAt - a.submittedAt).slice(0, 30);
}
export async function deleteReport(id) { const { fs, db } = await init(); await fs.deleteDoc(fs.doc(db, "bugReports", id)); }

// ================= Duels: duels/{duelId} (same documents as DuelRepositoryImpl) =================
const toDuel = (d) => { const x = d.data(); if (!x.challengerUid || !x.opponentUid || typeof x.itemsJson !== "string") return null; let items = []; try { items = JSON.parse(x.itemsJson); } catch {} return { id: d.id, ...x, items, wordIds: x.wordIds || [], challengerScore: x.challengerScore || 0, challengerCorrectCount: x.challengerCorrectCount || 0, status: x.status || "AWAITING_OPPONENT", createdAt: x.createdAt || 0, seenByChallenger: x.seenByChallenger !== false }; };
export async function createDuel({ opponentUid, opponentName, items, challengerScore, challengerCorrectCount, wordIds, myName }) {
  const { fs, db, auth } = await init();
  await fs.addDoc(fs.collection(db, "duels"), { challengerUid: auth.currentUser.uid, challengerName: myName, opponentUid, opponentName, itemsJson: JSON.stringify(items), wordIds, challengerScore, challengerCorrectCount, opponentScore: null, opponentCorrectCount: null, status: "AWAITING_OPPONENT", createdAt: Date.now(), completedAt: null, seenByChallenger: true });
}
export async function getDuel(id) { const { fs, db } = await init(), s = await fs.getDoc(fs.doc(db, "duels", id)); return s.exists() ? toDuel(s) : null; }
/** Live lists: incoming (waiting for me), sent (all mine) and received-and-complete. cb gets {incoming, sent, received}. */
export async function listenDuels(cb) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid, st = { incoming: [], sent: [], received: [] };
  const c = fs.collection(db, "duels"), newest = (l) => l.filter(Boolean).sort((a, b) => b.createdAt - a.createdAt).slice(0, 30), emit = () => cb({ ...st });
  const sub = (q, key) => fs.onSnapshot(q, (s) => { st[key] = newest(s.docs.map(toDuel)); emit(); }, () => { st[key] = []; emit(); });
  const un = [sub(fs.query(c, fs.where("opponentUid", "==", uid), fs.where("status", "==", "AWAITING_OPPONENT")), "incoming"), sub(fs.query(c, fs.where("challengerUid", "==", uid)), "sent"), sub(fs.query(c, fs.where("opponentUid", "==", uid), fs.where("status", "==", "COMPLETE")), "received")];
  return () => un.forEach((u) => u());
}
export async function submitDuelResult(id, opponentScore, opponentCorrectCount) { const { fs, db } = await init(); await fs.updateDoc(fs.doc(db, "duels", id), { status: "COMPLETE", opponentScore, opponentCorrectCount, completedAt: Date.now(), seenByChallenger: false }); }
export async function markDuelSeen(id) { const { fs, db } = await init(); await fs.updateDoc(fs.doc(db, "duels", id), { seenByChallenger: true }); }
export async function deleteDuel(id) { const { fs, db } = await init(); await fs.deleteDoc(fs.doc(db, "duels", id)); }

// ================= Cloud save: users/{uid}/backup/state (BackupRepositoryImpl) =================
export async function readBackup() {
  const { fs, db, auth } = await init(), s = await fs.getDoc(fs.doc(db, "users", auth.currentUser.uid, "backup", "state"));
  return s.exists() ? s.data() : null;
}
export async function writeBackup(snap) {
  const { fs, db, auth } = await init();
  await fs.setDoc(fs.doc(db, "users", auth.currentUser.uid, "backup", "state"), { ...snap, sealedAt: fs.serverTimestamp() }, { merge: true });
}

// AccountDeletionRepositoryImpl: the user's documents, then the sign-in itself. Throws {code:"auth/requires-recent-login"} when a fresh sign-in is needed.
export async function deleteAccount() {
  const { fs, db, auth } = await init(), user = auth.currentUser, uid = user.uid, userDoc = fs.doc(db, "users", uid);
  const safe = async (f) => { try { await f(); } catch {} };
  for (const name of ["backup", "friends", "invites", "blockedUsers", "inviteCooldowns", "private"]) {
    await safe(async () => { for (const d of (await fs.getDocs(fs.collection(db, "users", uid, name))).docs) await safe(() => fs.deleteDoc(d.ref)); });
  }
  await safe(async () => { const code = (await fs.getDoc(userDoc)).data()?.friendCode; if (code) await fs.deleteDoc(fs.doc(db, "friendCodes", code)); });
  await safe(async () => { for (const d of (await fs.getDocs(fs.query(fs.collection(db, "usernames"), fs.where("uid", "==", uid)))).docs) await fs.deleteDoc(d.ref); });
  await safe(async () => { for (const d of (await fs.getDocs(fs.query(fs.collection(db, "ghostRuns"), fs.where("uid", "==", uid)))).docs) { await safe(() => fs.deleteDoc(d.ref)); await safe(() => fs.deleteDoc(fs.doc(db, "ghostRunItems", d.id))); } });
  await safe(() => fs.deleteDoc(userDoc));
  const m = await import(`${CDN}/firebase-auth.js`);
  await m.deleteUser(user);
  await m.signInAnonymously(auth);
}

// A friend request to someone met in a room (FriendRepositoryImpl.sendFriendRequest): one document in their friendRequests.
export async function sendFriendRequestTo(uid, name, myNick) {
  const { fs, db, auth } = await init();
  await fs.setDoc(fs.doc(db, "users", uid, "friendRequests", auth.currentUser.uid), { fromNickname: myNick, sentAt: Date.now() });
}
