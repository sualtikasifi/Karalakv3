// firestore.rules on the Firestore emulator: who may write what, the cases a modified client would try first.
// Run with `npm test` in this folder (the emulator needs Java). The deploy workflow runs it before publishing rules.
// Every honest flow the apps perform is checked to still pass, and every attack found in the October 2026 audit is
// checked to stay closed.
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import {
  doc, setDoc, updateDoc, deleteDoc, deleteField, getDoc, getDocs, collection, query, where, addDoc,
  serverTimestamp, writeBatch, arrayUnion, runTransaction,
} from "firebase/firestore";
import { readFileSync } from "node:fs";

const env = await initializeTestEnvironment({
  projectId: "karalak-rules-test",
  firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8"), host: "127.0.0.1", port: 8080 },
});
// A clean database every run: the emulator keeps what earlier runs wrote, and a "create" against a leftover document
// is an update to the rules.
await env.clearFirestore();
const db = (uid, token) => env.authenticatedContext(uid, token).firestore();
const linked = (uid) => db(uid, { firebase: { sign_in_provider: "google.com" } });
const guest = (uid) => db(uid, { firebase: { sign_in_provider: "anonymous" } });
const reviewer = () => db("rev", { email: "raunen3075@gmail.com", email_verified: true });
const player = (name) => ({ displayName: name, ready: false, totalScore: 0, left: false, finished: false });
let failures = 0;
async function check(name, p) {
  try { await p; console.log("ok   ", name); } catch (e) { failures++; console.log("FAIL ", name, "—", e.message); }
}
const seed = (path, data) => env.withSecurityRulesDisabled((c) => setDoc(doc(c.firestore(), ...path), data));

// The league month as the apps stamp it (Istanbul calendar).
const ist = new Date(Date.now() + 3 * 3600e3);
const PERIOD = ist.getUTCFullYear() * 12 + ist.getUTCMonth();

// ---- rooms
async function seedRoom(code, extra = {}) {
  await seed(["rooms", code], { hostUid: "host", createdAt: 1, status: "WAITING", wordIds: [], rematchVotes: [], players: { host: player("Host"), p1: player("P1") }, ...extra });
}
await seedRoom("111111");
await check("a newcomer joins with their own row", assertSucceeds(updateDoc(doc(db("new"), "rooms", "111111"), { "players.new": player("New") })));
await check("a room is looked up by its code", assertSucceeds(getDoc(doc(db("stranger"), "rooms", "111111"))));
await check("nobody lists every room", assertFails(getDocs(collection(db("stranger"), "rooms"))));
await seedRoom("111112");
await check("a newcomer cannot change the room while joining", assertFails(updateDoc(doc(db("new"), "rooms", "111112"), { "players.new": player("New"), status: "PLAYING" })));
await check("a newcomer cannot ban anyone while joining", assertFails(updateDoc(doc(db("new2"), "rooms", "111112"), { "players.new2": player("N2"), "kickedUsers.p1": { displayName: "P1", until: 9e12 } })));
await check("a listed player updates their own row", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111112"), { "players.p1.ready": true })));
await check("a listed player cannot start the round (host only)", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { status: "PLAYING", wordIds: [1, 2, 3] })));
await check("a listed player cannot change the words", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { wordIds: [9] })));
await check("the host starts the countdown", assertSucceeds(updateDoc(doc(db("host"), "rooms", "111112"), { countdownStartedAt: 5 })));
await check("the host starts the round", assertSucceeds(updateDoc(doc(db("host"), "rooms", "111112"), { status: "PLAYING", wordIds: [1, 2, 3], startedAt: 6, countdownStartedAt: deleteField() })));
await check("a listed player finishes their own round", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111112"), { "players.p1.finished": true, "players.p1.totalScore": 10 })));
await check("a listed player ends the round once everyone is done", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111112"), { status: "FINISHED" })));
await check("a listed player votes for a rematch", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111112"), { rematchVotes: arrayUnion("p1") })));
await check("a listed player cannot vote for someone else", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { rematchVotes: arrayUnion("host") })));
await check("a listed player cannot ban anyone", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { "kickedUsers.host": { displayName: "Host", until: 9e12 } })));
await check("a listed player cannot edit someone else's row", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { "players.host.totalScore": 99 })));
await check("the host kicks", assertSucceeds(updateDoc(doc(db("host"), "rooms", "111112"), { "players.p1": deleteField(), "kickedUsers.p1": { displayName: "P1", until: 9e12 } })));
await check("a kicked player cannot come back", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { "players.p1": player("P1") })));
await check("the host unbans", assertSucceeds(updateDoc(doc(db("host"), "rooms", "111112"), { "kickedUsers.p1": deleteField() })));
await seedRoom("111113", { status: "FINISHED", rematchVotes: ["p1"] });
await check("any player resets a finished room for a rematch", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111113"), { status: "PLAYING", wordIds: [4], startedAt: 5, players: { host: player("Host"), p1: player("P1") }, rematchVotes: [] })));
await seedRoom("111114", { status: "FINISHED", kickedUsers: { x: { displayName: "X", until: 9e12 } } });
await check("a reset that leaves the ban list alone passes", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111114"), { status: "WAITING", wordIds: [], players: { host: player("Host"), p1: player("P1") }, rematchVotes: [] })));
await seedRoom("111115", { status: "FINISHED" });
await check("a reset cannot change the room's settings", assertFails(updateDoc(doc(db("p1"), "rooms", "111115"), { status: "WAITING", players: { host: player("Host"), p1: player("P1") }, rematchVotes: [], wordCount: 50 })));
await check("nobody seizes the host", assertFails(updateDoc(doc(db("p1"), "rooms", "111114"), { hostUid: "p1" })));
await seedRoom("130246", { hostUid: "", createdAt: 0 });
await check("the bot room stays broadly writable", assertSucceeds(updateDoc(doc(db("any"), "rooms", "130246"), { status: "PLAYING" })));
await check("nobody bans players from the bot room", assertFails(updateDoc(doc(db("any"), "rooms", "130246"), { "kickedUsers.p1": { displayName: "P1", until: 9e12 } })));
// drawings and chat are for the room's own players
await seedRoom("222222", { status: "PLAYING" });
await seed(["rooms", "222222", "results", "host"], { itemsJson: "[]" });
await check("a player reads the room's drawings", assertSucceeds(getDoc(doc(db("p1"), "rooms", "222222", "results", "host"))));
await check("a stranger cannot read a room's drawings", assertFails(getDoc(doc(db("stranger"), "rooms", "222222", "results", "host"))));
await check("a player writes their own drawings", assertSucceeds(setDoc(doc(db("p1"), "rooms", "222222", "results", "p1"), { itemsJson: "[]" })));
await check("a player posts a reaction", assertSucceeds(addDoc(collection(db("p1"), "rooms", "222222", "reactions"), { uid: "p1", emoji: "😂", messageKey: "funny", sentAt: 1 })));
await check("a stranger cannot post a reaction", assertFails(addDoc(collection(db("stranger"), "rooms", "222222", "reactions"), { uid: "stranger", emoji: "😂", messageKey: "funny", sentAt: 1 })));
await check("a reaction cannot carry extra data", assertFails(addDoc(collection(db("p1"), "rooms", "222222", "reactions"), { uid: "p1", emoji: "x", messageKey: "k", sentAt: 1, junk: "x".repeat(1000) })));
await check("a stranger cannot read the chat", assertFails(getDocs(collection(db("stranger"), "rooms", "222222", "reactions"))));

// ---- recorded rounds waiting for review
await check("a round with a normal nickname is filed", assertSucceeds(setDoc(doc(db("u1"), "pendingRuns", "r1"), { uid: "u1", nickname: "Zeynep", totalScore: 10 })));
await check("a round without a nickname is filed", assertSucceeds(setDoc(doc(db("u1"), "pendingRuns", "r2"), { uid: "u1", totalScore: 10 })));
await check("a round with a 41-character nickname is refused", assertFails(setDoc(doc(db("u1"), "pendingRuns", "r3"), { uid: "u1", nickname: "x".repeat(41) })));
await check("a round with a nickname that is not text is refused", assertFails(setDoc(doc(db("u1"), "pendingRuns", "r4"), { uid: "u1", nickname: { a: 1 } })));
await check("a round filed for someone else is refused", assertFails(setDoc(doc(db("u1"), "pendingRuns", "r5"), { uid: "u2", nickname: "a" })));
await check("a stranger cannot read the review queue", assertFails(getDocs(collection(db("u2"), "pendingRuns"))));
await check("the author reads their own queued round", assertSucceeds(getDoc(doc(db("u1"), "pendingRuns", "r1"))));
await check("the reviewer reads the queue", assertSucceeds(getDocs(collection(reviewer(), "pendingRuns"))));
await check("the author withdraws their own round", assertSucceeds(deleteDoc(doc(db("u1"), "pendingRuns", "r2"))));
await check("a player cannot put a round in the live pool", assertFails(setDoc(doc(db("u1"), "ghostRuns", "g1"), { uid: "u1", nickname: "a" })));
await check("the reviewer can", assertSucceeds(setDoc(doc(reviewer(), "ghostRuns", "g1"), { uid: "u1", nickname: "a" })));
await seed(["pendingDailyChallenges", "d1"], { uid: "u1", itemsJson: "[]" });
await check("a stranger cannot read a daily attempt", assertFails(getDoc(doc(db("u2"), "pendingDailyChallenges", "d1"))));
await seed(["detectorEvents", "e1"], { uid: "u1", sampleStrokesJson: "[]" });
await check("a stranger cannot read handwriting samples", assertFails(getDocs(collection(db("u2"), "detectorEvents"))));

// ---- profiles and the league
await check("a new account publishes a modest score", assertSucceeds(setDoc(doc(db("u3"), "users", "u3"), { nickname: "A", periodXp: 1200, periodId: PERIOD, periodXpAt: serverTimestamp(), periodStartAt: serverTimestamp(), level: 5 })));
await check("a further score keeps the month's start", assertSucceeds(updateDoc(doc(db("u3"), "users", "u3"), { periodXp: 2400, periodXpAt: serverTimestamp(), periodId: PERIOD })));
await check("the month's start cannot be moved back", assertFails(updateDoc(doc(db("u3"), "users", "u3"), { periodXp: 2500, periodXpAt: serverTimestamp(), periodId: PERIOD, periodStartAt: new Date(Date.now() - 20 * 864e5) })));
await check("a new account cannot open the month at the ceiling", assertFails(setDoc(doc(db("n1"), "users", "n1"), { nickname: "cheat", periodXp: 300000, totalXp: 5000000, level: 100, periodId: PERIOD, periodXpAt: serverTimestamp(), periodStartAt: serverTimestamp() })));
await check("a score without the month's start stamp is refused", assertFails(setDoc(doc(db("n2"), "users", "n2"), { nickname: "x", periodXp: 100, periodId: PERIOD, periodXpAt: serverTimestamp() })));
let chain = 1;
let chainOk = true;
for (let i = 0; i < 4 && chainOk; i++) {
  chain += 29000;
  try { await updateDoc(doc(db("u3"), "users", "u3"), { periodXp: chain, periodXpAt: serverTimestamp(), periodId: PERIOD }); } catch { chainOk = false; }
}
await check("back-to-back writes cannot climb past the burst", chainOk ? Promise.reject(new Error(`reached ${chain}`)) : Promise.resolve());
await check("a score for a month that is over is refused", assertFails(setDoc(doc(db("n3"), "users", "n3"), { periodXp: 100, periodId: PERIOD - 2, periodXpAt: serverTimestamp(), periodStartAt: serverTimestamp() })));
await check("nobody writes someone else's profile", assertFails(setDoc(doc(db("u4"), "users", "u3"), { nickname: "B" })));
await check("nobody writes their own league prize", assertFails(setDoc(doc(db("u3"), "users", "u3", "private", "leagueAwards"), { won: ["crown"] })));
await check("a profile name of 5000 characters is refused", assertFails(setDoc(doc(db("u5"), "users", "u5"), { nickname: "x".repeat(5000) }, { merge: true })));
await check("a Google photo is accepted as the picture", assertSucceeds(setDoc(doc(db("u5"), "users", "u5"), { nickname: "Ece", avatarUrl: "https://lh3.googleusercontent.com/a/abc=s96-c" }, { merge: true })));
await check("any other picture address is refused", assertFails(setDoc(doc(db("u5"), "users", "u5"), { avatarUrl: "https://evil.example/pixel.png" }, { merge: true })));
await check("the owner deletes their own profile", assertSucceeds(deleteDoc(doc(db("u5"), "users", "u5"))));
// the global table counts the players ahead of you
await check("the players ahead of you can be counted", assertSucceeds(getDocs(query(collection(db("u3"), "users"), where("periodId", "==", PERIOD), where("periodXp", ">", 10)))));

// ---- referrals
await check("an invitee records who invited them", assertSucceeds(setDoc(doc(db("i1"), "users", "i1"), { invitedByUid: "b", invitedByAtMillis: 1, referralRewardGranted: false }, { merge: true })));
await check("the inviter cannot be swapped afterwards", assertFails(updateDoc(doc(db("i1"), "users", "i1"), { invitedByUid: "z" })));
await seed(["users", "i2"], { nickname: "inv", invitedByUid: "b", referralRewardGranted: true, level: 5 });
await check("a paid referral cannot be reset to unpaid", assertFails(updateDoc(doc(db("i2"), "users", "i2"), { referralRewardGranted: false })));
await check("nobody invites themselves", assertFails(setDoc(doc(db("i3"), "users", "i3"), { invitedByUid: "i3", referralRewardGranted: false }, { merge: true })));

// ---- the cloud save
await env.withSecurityRulesDisabled((c) => setDoc(doc(c.firestore(), "users", "g", "backup", "state"), { goldBalance: 100, lifetimeXp: 100, sealedAt: new Date(Date.now() - 1000) }));
await check("a save grows within the hourly allowance", assertSucceeds(setDoc(doc(db("g"), "users", "g", "backup", "state"), { goldBalance: 5000, lifetimeXp: 3000, sealedAt: serverTimestamp() })));
await check("a first save is held to the first-save ceiling", assertFails(setDoc(doc(db("g2"), "users", "g2", "backup", "state"), { goldBalance: 1500000, lifetimeXp: 5000000, sealedAt: serverTimestamp() })));
await check("an ordinary first save goes through", assertSucceeds(setDoc(doc(db("g3"), "users", "g3", "backup", "state"), { goldBalance: 20000, lifetimeXp: 40000, sealedAt: serverTimestamp() })));
await check("a save cannot be deleted from a client", assertFails(deleteDoc(doc(db("g"), "users", "g", "backup", "state"))));

// ---- moderation
await check("a player cannot write a penalty against another", assertFails(setDoc(doc(db("u1"), "penalties", "p1"), { uid: "u2", xp: 500, appliedAt: 0 })));
await seed(["penalties", "p2"], { uid: "u1", xp: 500, appliedAt: 0 });
await check("a player marks their own penalty applied", assertSucceeds(updateDoc(doc(db("u1"), "penalties", "p2"), { appliedAt: 5 })));
await check("but cannot soften it", assertFails(updateDoc(doc(db("u1"), "penalties", "p2"), { xp: 0 })));
await check("the bot's training drawings are sealed", assertFails(setDoc(doc(db("u1"), "botTrainedWords", "1"), { wordId: 1, strokesJson: "[]" })));
const report = (reporter, extra = {}) => ({ reporterUid: reporter, reason: "OFFENSIVE", reportedUid: "victim", runId: "run9", strokesJson: "[]", ...extra });
await check("a signed-in player reports a drawing", assertSucceeds(setDoc(doc(linked("s1"), "drawingReports", "run9__s1"), report("s1", { reporterLinked: true }))));
await check("a guest cannot claim to be a signed-in reporter", assertFails(setDoc(doc(guest("s2"), "drawingReports", "run9__s2"), report("s2", { reporterLinked: true }))));
await check("a guest's report is filed as a guest's", assertSucceeds(setDoc(doc(guest("s3"), "drawingReports", "run9__s3"), report("s3", { reporterLinked: false }))));
await check("a player reports another player's name", assertSucceeds(setDoc(doc(db("r1"), "playerReports", "bad__r1"), { reporterUid: "r1", reportedUid: "bad", nickname: "BadName", reason: "NAME", context: "league", createdAt: serverTimestamp() })));
await check("a player report cannot be filed for someone else", assertFails(setDoc(doc(db("r1"), "playerReports", "bad__r2"), { reporterUid: "r2", reportedUid: "bad", nickname: "B", reason: "NAME", createdAt: serverTimestamp() })));
await check("players cannot read reports", assertFails(getDoc(doc(db("bad"), "playerReports", "bad__r1"))));

// ---- statistics
await check("a device writes its own day's counters", assertSucceeds(setDoc(doc(db("d1"), "adminStats", "2026-10-27", "devices", "d1"), { day: "2026-10-27", counts: { app__open__count: 1 } })));
await check("a device cannot write another device's counters", assertFails(setDoc(doc(db("d1"), "adminStats", "2026-10-27", "devices", "d2"), { day: "2026-10-27", counts: {} })));
await check("nobody overwrites the shared day document", assertFails(setDoc(doc(db("d1"), "adminStats", "2026-10-27"), { day: "2026-10-27", counts: { ads__revenue: 999999 } })));

// ---- friends
await seed(["users", "b"], { nickname: "Bob" });
await check("a friend request is sent", assertSucceeds(setDoc(doc(db("a"), "users", "b", "friendRequests", "a"), { fromNickname: "A", sentAt: 1, fromUid: "a" })));
await check("a friend request cannot be sent in someone else's name", assertFails(setDoc(doc(db("c"), "users", "b", "friendRequests", "a"), { fromNickname: "A", sentAt: 1 })));
await seed(["users", "b", "blockedUsers", "d"], { at: 1 });
await check("a blocked player cannot send a request", assertFails(setDoc(doc(db("d"), "users", "b", "friendRequests", "d"), { fromNickname: "D", sentAt: 1 })));
await check("nobody reads another player's requests", assertFails(getDoc(doc(db("c"), "users", "b", "friendRequests", "a"))));
await check("nobody writes themselves into a stranger's friends list", assertFails(setDoc(doc(db("x"), "users", "b", "friends", "x"), { nickname: "X", addedAt: 1 })));
await check("a blocked player cannot write themselves into the list", assertFails(setDoc(doc(db("d"), "users", "b", "friends", "d"), { nickname: "D", addedAt: 1 })));
{
  // b accepts a's request (users/b/friendRequests/a): both entries and the request in one batch
  const bb = db("b"), batch = writeBatch(bb);
  batch.set(doc(bb, "users", "b", "friends", "a"), { nickname: "A", addedAt: 1 });
  batch.set(doc(bb, "users", "a", "friends", "b"), { nickname: "Bob", addedAt: 1 });
  batch.delete(doc(bb, "users", "b", "friendRequests", "a"));
  await check("accepting a request writes both halves", assertSucceeds(batch.commit()));
}
await check("a friend entry cannot carry a 5000-character name", assertFails(setDoc(doc(db("a"), "users", "a", "friends", "q"), { nickname: "x".repeat(5000), addedAt: 1 })));
{
  // guest g1 (friends with f1) signs in to an existing account acct1: the old uid leaves a secret, the new one presents it
  await seed(["users", "f1", "friends", "g1"], { nickname: "Guest", addedAt: 1 });
  await seed(["users", "g1", "friends", "f1"], { nickname: "F1", addedAt: 1 });
  await check("the old account leaves its moving secret", assertSucceeds(setDoc(doc(db("g1"), "users", "g1", "private", "migration"), { token: "s3cret-token-0123456789", at: 1 })));
  await check("the new account records where it came from", assertSucceeds(setDoc(doc(db("acct1"), "users", "acct1", "private", "migratedFrom"), { uid: "g1", token: "s3cret-token-0123456789" })));
  const na = db("acct1"), batch = writeBatch(na);
  batch.set(doc(na, "users", "acct1", "friends", "f1"), { nickname: "F1", addedAt: 2 });
  batch.set(doc(na, "users", "f1", "friends", "acct1"), { nickname: "Me", addedAt: 2 });
  batch.delete(doc(na, "users", "f1", "friends", "g1"));
  await check("the new account takes over the old one's friendships", assertSucceeds(batch.commit()));
  await check("a wrong secret takes over nothing", (async () => {
    await seed(["users", "f2", "friends", "g2"], { nickname: "Guest", addedAt: 1 });
    await seed(["users", "g2", "private", "migration"], { token: "real-secret-0123456789", at: 1 });
    await setDoc(doc(db("evil"), "users", "evil", "private", "migratedFrom"), { uid: "g2", token: "guessed-secret-000000" });
    await assertFails(setDoc(doc(db("evil"), "users", "f2", "friends", "evil"), { nickname: "Evil", addedAt: 1 }));
  })());
}

// ---- duels, invites, usernames, friend codes
await seed(["users", "v", "blockedUsers", "w"], { at: 1 });
const duel = (from, to) => ({ challengerUid: from, challengerName: "W", opponentUid: to, opponentName: "V", itemsJson: "[]", wordIds: [1, 2], challengerScore: 1, challengerCorrectCount: 1, opponentScore: null, opponentCorrectCount: null, status: "AWAITING_OPPONENT", createdAt: 1, completedAt: null, seenByChallenger: true });
await check("a duel is created", assertSucceeds(setDoc(doc(db("w"), "duels", "dd1"), duel("w", "z"))));
await check("a blocked player cannot challenge the blocker", assertFails(setDoc(doc(db("w"), "duels", "dd2"), duel("w", "v"))));
await seed(["users", "v", "friends", "fr"], { nickname: "Fr", addedAt: 1 });
const invite = (from) => ({ fromUid: from, fromNickname: "Fr", roomCode: "123456", sentAt: 1 });
await check("a friend sends a match invite", assertSucceeds(addDoc(collection(db("fr"), "users", "v", "invites"), invite("fr"))));
await check("a stranger cannot send a match invite", assertFails(addDoc(collection(db("st"), "users", "v", "invites"), invite("st"))));
await check("an invite cannot carry a huge name", assertFails(addDoc(collection(db("fr"), "users", "v", "invites"), { ...invite("fr"), fromNickname: "x".repeat(100000) })));
{
  const q = db("q");
  await check("a name is claimed together with the account's marker", assertSucceeds(runTransaction(q, async (tx) => {
    tx.set(doc(q, "usernames", "ayse_34"), { uid: "q", name: "Ayşe 34", createdAt: 1 });
    tx.set(doc(q, "usernameOwners", "q"), { key: "ayse_34" });
  })));
  await check("a second name cannot be claimed on the side", assertFails(setDoc(doc(q, "usernames", "admin"), { uid: "q", name: "admin", createdAt: 1 })));
  await check("a rename moves the marker and releases the old name", assertSucceeds(runTransaction(q, async (tx) => {
    tx.set(doc(q, "usernames", "ayse_35"), { uid: "q", name: "Ayşe 35", createdAt: 2 });
    tx.delete(doc(q, "usernames", "ayse_34"));
    tx.set(doc(q, "usernameOwners", "q"), { key: "ayse_35" });
  })));
  await check("the marker cannot move while the old name is kept", assertFails(runTransaction(q, async (tx) => {
    tx.set(doc(q, "usernames", "ayse_36"), { uid: "q", name: "Ayşe 36", createdAt: 3 });
    tx.set(doc(q, "usernameOwners", "q"), { key: "ayse_36" });
  })));
  await check("deleting the account releases the name and the marker together", assertSucceeds(runTransaction(q, async (tx) => {
    tx.delete(doc(q, "usernames", "ayse_35"));
    tx.delete(doc(q, "usernameOwners", "q"));
  })));
}
{
  const fc = db("fc");
  await check("a friend code is created with the profile", assertSucceeds(runTransaction(fc, async (tx) => {
    tx.set(doc(fc, "friendCodes", "555555"), { uid: "fc" });
    tx.set(doc(fc, "users", "fc"), { nickname: "Fc", friendCode: "555555", createdAt: 1 }, { merge: true });
  })));
  await check("a second friend code cannot be claimed", assertFails(setDoc(doc(fc, "friendCodes", "666666"), { uid: "fc" })));
  await check("the friend code on the profile cannot be changed", assertFails(updateDoc(doc(fc, "users", "fc"), { friendCode: "777777" })));
  await check("the owner deletes their friend code", assertSucceeds(deleteDoc(doc(fc, "friendCodes", "555555"))));
}

// ---- account deletion
await check("an account files its deletion request", assertSucceeds(setDoc(doc(db("del"), "deletionRequests", "del"), { requestedAt: serverTimestamp(), platform: "android" })));
await check("nobody files a deletion request for someone else", assertFails(setDoc(doc(db("del"), "deletionRequests", "other"), { requestedAt: serverTimestamp() })));
await seed(["users", "del", "private", "pendingRewards"], { pending: [] });
await check("the owner deletes their reward queue", assertSucceeds(deleteDoc(doc(db("del"), "users", "del", "private", "pendingRewards"))));

// The date of birth bound to a Google account (AccountAgeSync): saved once, locked until the player is 18.
{
  const Y = new Date().getUTCFullYear();
  const ageDoc = (u) => doc(linked(u), "users", u, "private", "age");
  await check("the owner saves their date of birth", assertSucceeds(setDoc(ageDoc("age1"), { birthYear: Y - 10, birthMonth: 6, birthDay: 15, savedAt: serverTimestamp() })));
  await check("a year-only answer is saved too", assertSucceeds(setDoc(ageDoc("age6"), { birthYear: Y - 10, savedAt: serverTimestamp() })));
  await check("a month without a day is refused", assertFails(setDoc(ageDoc("age7"), { birthYear: Y - 10, birthMonth: 6, savedAt: serverTimestamp() })));
  await check("an impossible month is refused", assertFails(setDoc(ageDoc("age8"), { birthYear: Y - 10, birthMonth: 13, birthDay: 1, savedAt: serverTimestamp() })));
  await check("the owner reads their date of birth", assertSucceeds(getDoc(ageDoc("age1"))));
  await check("nobody else reads a date of birth", assertFails(getDoc(doc(linked("age2"), "users", "age1", "private", "age"))));
  await check("a child's year cannot be rewritten older", assertFails(setDoc(ageDoc("age1"), { birthYear: Y - 30, savedAt: serverTimestamp() })));
  await check("nothing but the year is kept", assertFails(setDoc(ageDoc("age3"), { birthYear: Y - 20, savedAt: serverTimestamp(), email: "x@y.z" })));
  await check("an impossible year is refused", assertFails(setDoc(ageDoc("age4"), { birthYear: Y + 1, savedAt: serverTimestamp() })));
  await seed(["users", "age5", "private", "age"], { birthYear: Y - 25, savedAt: 1 });
  await check("an adult's year can be corrected", assertSucceeds(setDoc(ageDoc("age5"), { birthYear: Y - 26, savedAt: serverTimestamp() })));
  await check("the owner deletes their date of birth with the account", assertSucceeds(deleteDoc(ageDoc("age1"))));
}

// Reporting a player (PlayerReporter): one report per pair, read only by the reviewer, never rewritten.
const preport = (by, about, extra = {}) => ({ reporterUid: by, reportedUid: about, nickname: "Kaba", reason: "NAME", context: "league", createdAt: serverTimestamp(), ...extra });
await check("a player reports another player", assertSucceeds(setDoc(doc(db("pr1"), "playerReports", "bad1__pr1"), preport("pr1", "bad1"))));
await check("a report cannot be filed under someone else's name", assertFails(setDoc(doc(db("pr2"), "playerReports", "bad1__pr3"), preport("pr3", "bad1"))));
await check("nobody reports themselves", assertFails(setDoc(doc(db("pr4"), "playerReports", "pr4__pr4"), preport("pr4", "pr4"))));
await check("a report's reason is one of the four", assertFails(setDoc(doc(db("pr5"), "playerReports", "bad1__pr5"), preport("pr5", "bad1", { reason: "SPAM" }))));
await check("a filed report is not rewritten", assertFails(setDoc(doc(db("pr1"), "playerReports", "bad1__pr1"), preport("pr1", "bad1", { reason: "OTHER" }))));
await check("the reviewer reads reports", assertSucceeds(getDoc(doc(reviewer(), "playerReports", "bad1__pr1"))));

// The developer panel's own collections: commands, audit log, roll-ups, announcement, planned XP events, and a reviewer's name reset.
await seed(["users", "nm1"], { nickname: "Kaba", level: 3 });
await check("the reviewer puts a neutral name on an account", assertSucceeds(updateDoc(doc(reviewer(), "users", "nm1"), { nickname: "Oyuncu1234", username: "Oyuncu1234", nameResetAt: 5 })));
await check("the reviewer cannot touch anything else on an account", assertFails(updateDoc(doc(reviewer(), "users", "nm1"), { level: 99 })));
await check("a stranger cannot rename an account", assertFails(updateDoc(doc(db("nm2"), "users", "nm1"), { nickname: "x" })));
await check("the reviewer asks for a gift", assertSucceeds(setDoc(doc(reviewer(), "adminCommands", "c1"), { type: "grant", uid: "nm1", gold: 100, xp: 0, note: "n", by: "r", createdAt: serverTimestamp() })));
await check("an unknown command type is refused", assertFails(setDoc(doc(reviewer(), "adminCommands", "c2"), { type: "wipe", createdAt: serverTimestamp() })));
await check("a player cannot queue a command", assertFails(setDoc(doc(db("nm2"), "adminCommands", "c3"), { type: "grant", uid: "nm2", gold: 9999, xp: 0, note: "n", by: "x", createdAt: serverTimestamp() })));
await check("a player cannot read the commands", assertFails(getDoc(doc(db("nm2"), "adminCommands", "c1"))));
await check("the reviewer writes an audit line", assertSucceeds(setDoc(doc(reviewer(), "adminAudit", "a1"), { action: "lock", target: "nm1", detail: "", by: "r", at: serverTimestamp() })));
await check("the audit log is not rewritten", assertFails(setDoc(doc(reviewer(), "adminAudit", "a1"), { action: "lock", target: "nm1", detail: "x", by: "r", at: serverTimestamp() })));
await check("the audit log is not erased", assertFails(deleteDoc(doc(reviewer(), "adminAudit", "a1"))));
await seed(["adminSummary", "2026-10-27"], { day: "2026-10-27", players: 5, final: true });
await check("the reviewer reads a roll-up", assertSucceeds(getDoc(doc(reviewer(), "adminSummary", "2026-10-27"))));
await check("a player cannot read a roll-up", assertFails(getDoc(doc(db("nm2"), "adminSummary", "2026-10-27"))));
await check("nobody writes a roll-up from the app", assertFails(setDoc(doc(reviewer(), "adminSummary", "2026-10-28"), { day: "2026-10-28", players: 1, final: true })));
await check("the reviewer sets the announcement", assertSucceeds(setDoc(doc(reviewer(), "config", "announcement"), { active: true, id: "1", texts: { tr: { title: "a", body: "b" } } })));
await check("a player reads the announcement", assertSucceeds(getDoc(doc(db("nm2"), "config", "announcement"))));
await check("a player cannot set the announcement", assertFails(setDoc(doc(db("nm2"), "config", "announcement"), { active: true })));
await check("the reviewer plans an XP event", assertSucceeds(setDoc(doc(reviewer(), "xpEventPlans", "p1"), { startAtMillis: 1, durationHours: 24, multiplier: 2 })));
await check("a player cannot plan an XP event", assertFails(setDoc(doc(db("nm2"), "xpEventPlans", "p2"), { startAtMillis: 1, durationHours: 24, multiplier: 10 })));

await env.cleanup();
console.log(failures ? `\n${failures} FAILED` : "\nall passed");
process.exit(failures ? 1 : 0);
