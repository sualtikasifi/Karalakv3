// Firebase layer. Loaded lazily so the game still works offline / before config is filled in.
import { firebaseConfig, firebaseReady } from "./config.js";

const V = "10.14.1";
const CDN = `https://www.gstatic.com/firebasejs/${V}`;
const RUN_WORD_COUNT = 10, SHARD_COUNT = 1000, BAND_SIZE = 10, MIN_CORRECT = 2, MAX_BAND = 9, CANDIDATES = 5;
let api = null;

async function init() {
  if (api) return api;
  if (!firebaseReady()) throw new Error("no-config");
  const [app, auth, fs] = await Promise.all([
    import(`${CDN}/firebase-app.js`), import(`${CDN}/firebase-auth.js`), import(`${CDN}/firebase-firestore.js`),
  ]);
  const a = app.initializeApp(firebaseConfig);
  const au = auth.getAuth(a);
  if (!au.currentUser) await auth.signInAnonymously(au);
  await au.authStateReady?.();
  api = { fs, db: fs.getFirestore(a), auth: au };
  return api;
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

export async function loadItems(runId) {
  const { fs, db } = await init();
  const snap = await fs.getDoc(fs.doc(db, "ghostRunItems", runId));
  return snap.exists() ? JSON.parse(snap.data().itemsJson) : [];
}

// Files a finished round in the review queue, same document shape as Android's write().
export async function recordRound({ language, nickname, wordIds, perWord, items }) {
  const correctCount = perWord.filter((p) => p.isCorrect).length;
  if (wordIds.length < RUN_WORD_COUNT || correctCount < MIN_CORRECT) return false;
  const { fs, db, auth } = await init();
  const uid = auth.currentUser.uid;
  const ref = fs.doc(fs.collection(db, "pendingRuns"));
  const correctTimes = perWord.filter((p) => p.isCorrect).map((p) => p.responseTimeMs);
  const batch = fs.writeBatch(db);
  batch.set(ref, {
    uid, nickname, level: 1, frameId: "SCRIBBLER", levelBand: 0, shard: Math.floor(Math.random() * SHARD_COUNT),
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

export async function getGlobalLeague() {
  const { fs, db } = await init();
  const snap = await fs.getDoc(fs.doc(db, "leaderboards", "global"));
  return snap.exists() ? snap.data() : null;
}

// Same fields as FriendRepositoryImpl.publishLeagueScore. A guest's web account is its own uid, so it never
// collides with an Android profile.
export async function publishScore({ nickname, periodXp, periodId, level, frameId, totalXp }) {
  const { fs, db, auth } = await init();
  await fs.setDoc(fs.doc(db, "users", auth.currentUser.uid), { nickname, periodXp, totalXp, periodXpAt: fs.serverTimestamp(), periodId, level, frameId, avatarUrl: "" }, { merge: true });
}

// ---- Google sign-in (links the browser to the player's Google account) ----
export async function authState() {
  const { auth } = await init();
  const u = auth.currentUser;
  return u ? { uid: u.uid, anon: u.isAnonymous, name: u.displayName || "", email: u.email || "", photo: u.photoURL || "" } : null;
}
export async function signInGoogle() {
  const { auth } = await init();
  const m = await import(`${CDN}/firebase-auth.js`);
  const provider = new m.GoogleAuthProvider();
  const cur = auth.currentUser;
  try {
    // Keeps the guest's league entry when the Google account is new; falls back to a plain sign-in otherwise.
    if (cur && cur.isAnonymous) await m.linkWithPopup(cur, provider); else await m.signInWithPopup(auth, provider);
  } catch (e) {
    if (e.code === "auth/credential-already-in-use" || e.code === "auth/email-already-in-use") await m.signInWithPopup(auth, provider); else throw e;
  }
  return authState();
}
export async function signOutGoogle() {
  const { auth } = await init();
  const m = await import(`${CDN}/firebase-auth.js`);
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
export async function addFriendByCode(code, myNick) {
  const { fs, db, auth } = await init(), uid = auth.currentUser.uid;
  const c = await fs.getDoc(fs.doc(db, "friendCodes", code));
  if (!c.exists()) return { status: "notfound" };
  const fu = c.data().uid; if (fu === uid) return { status: "self" };
  const fdoc = await fs.getDoc(fs.doc(db, "users", fu)), fn = (fdoc.exists() && fdoc.data().nickname) || "?";
  if ((await fs.getDoc(fs.doc(db, "users", uid, "friends", fu))).exists()) return { status: "already", name: fn };
  if ((await fs.getDoc(fs.doc(db, "users", uid, "friendRequests", fu))).exists()) { await writeFriendship(uid, myNick, fu, fn); return { status: "added", name: fn }; }
  try { await fs.setDoc(fs.doc(db, "users", fu, "friendRequests", uid), { fromNickname: myNick, sentAt: Date.now() }); }
  catch (e) { return { status: "blocked" }; }
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
