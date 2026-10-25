// firestore.rules on the Firestore emulator: who may write what, the cases a modified client would try first.
// Run with `npm test` in this folder (the emulator needs Java). The deploy workflow runs it before publishing rules.
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { doc, setDoc, updateDoc, deleteField, getDoc, serverTimestamp } from "firebase/firestore";
import { readFileSync } from "node:fs";

const env = await initializeTestEnvironment({
  projectId: "karalak-rules-test",
  firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8"), host: "127.0.0.1", port: 8080 },
});
const db = (uid, token) => env.authenticatedContext(uid, token).firestore();
const reviewer = () => db("rev", { email: "raunen3075@gmail.com", email_verified: true });
const player = (name) => ({ displayName: name, ready: false, totalScore: 0, left: false });
let failures = 0;
async function check(name, p) {
  try { await p; console.log("ok   ", name); } catch (e) { failures++; console.log("FAIL ", name, "—", e.message); }
}
const seed = (path, data) => env.withSecurityRulesDisabled((c) => setDoc(doc(c.firestore(), ...path), data));

// ---- rooms
async function seedRoom(code, extra = {}) {
  await seed(["rooms", code], { hostUid: "host", createdAt: 1, status: "WAITING", wordIds: [], rematchVotes: [], players: { host: player("Host"), p1: player("P1") }, ...extra });
}
await seedRoom("111111");
await check("a newcomer joins with their own row", assertSucceeds(updateDoc(doc(db("new"), "rooms", "111111"), { "players.new": player("New") })));
await seedRoom("111112");
await check("a newcomer cannot change the room while joining", assertFails(updateDoc(doc(db("new"), "rooms", "111112"), { "players.new": player("New"), status: "PLAYING" })));
await check("a newcomer cannot ban anyone while joining", assertFails(updateDoc(doc(db("new2"), "rooms", "111112"), { "players.new2": player("N2"), "kickedUsers.p1": { displayName: "P1", until: 9e12 } })));
await check("a listed player updates their own row", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111112"), { "players.p1.ready": true })));
await check("a listed player moves the room along", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111112"), { status: "PLAYING", wordIds: [1, 2, 3] })));
await check("a listed player cannot ban anyone", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { "kickedUsers.host": { displayName: "Host", until: 9e12 } })));
await check("a listed player cannot edit someone else's row", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { "players.host.totalScore": 99 })));
await check("the host kicks", assertSucceeds(updateDoc(doc(db("host"), "rooms", "111112"), { "players.p1": deleteField(), "kickedUsers.p1": { displayName: "P1", until: 9e12 } })));
await check("a kicked player cannot come back", assertFails(updateDoc(doc(db("p1"), "rooms", "111112"), { "players.p1": player("P1") })));
await check("the host unbans", assertSucceeds(updateDoc(doc(db("host"), "rooms", "111112"), { "kickedUsers.p1": deleteField() })));
await seedRoom("111113", { status: "FINISHED", rematchVotes: ["p1"] });
await check("any player resets a finished room for a rematch", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111113"), { status: "PLAYING", wordIds: [4], startedAt: 5, players: { host: player("Host"), p1: player("P1") }, rematchVotes: [] })));
await seedRoom("111114", { status: "FINISHED", kickedUsers: { x: { displayName: "X", until: 9e12 } } });
await check("a reset that leaves the ban list alone passes", assertSucceeds(updateDoc(doc(db("p1"), "rooms", "111114"), { status: "WAITING", players: { host: player("Host"), p1: player("P1") }, rematchVotes: [] })));
await check("nobody seizes the host", assertFails(updateDoc(doc(db("p1"), "rooms", "111114"), { hostUid: "p1" })));
await seedRoom("130246");
await check("the bot room stays broadly writable", assertSucceeds(updateDoc(doc(db("any"), "rooms", "130246"), { status: "PLAYING" })));

// ---- recorded rounds waiting for review
await check("a round with a normal nickname is filed", assertSucceeds(setDoc(doc(db("u1"), "pendingRuns", "r1"), { uid: "u1", nickname: "Zeynep", totalScore: 10 })));
await check("a round without a nickname is filed", assertSucceeds(setDoc(doc(db("u1"), "pendingRuns", "r2"), { uid: "u1", totalScore: 10 })));
await check("a round with a 41-character nickname is refused", assertFails(setDoc(doc(db("u1"), "pendingRuns", "r3"), { uid: "u1", nickname: "x".repeat(41) })));
await check("a round with a nickname that is not text is refused", assertFails(setDoc(doc(db("u1"), "pendingRuns", "r4"), { uid: "u1", nickname: { a: 1 } })));
await check("a round filed for someone else is refused", assertFails(setDoc(doc(db("u1"), "pendingRuns", "r5"), { uid: "u2", nickname: "a" })));
await check("a player cannot put a round in the live pool", assertFails(setDoc(doc(db("u1"), "ghostRuns", "g1"), { uid: "u1", nickname: "a" })));
await check("the reviewer can", assertSucceeds(setDoc(doc(reviewer(), "ghostRuns", "g1"), { uid: "u1", nickname: "a" })));

// ---- profiles and the league
await check("a player publishes a plausible score", assertSucceeds(setDoc(doc(db("u3"), "users", "u3"), { nickname: "A", periodXp: 1200, periodId: "2026-10", periodXpAt: serverTimestamp(), level: 5 })));
await check("a player cannot jump far past the hourly allowance", assertFails(updateDoc(doc(db("u3"), "users", "u3"), { periodXp: 250000, periodXpAt: serverTimestamp() })));
await check("nobody writes someone else's profile", assertFails(setDoc(doc(db("u4"), "users", "u3"), { nickname: "B" })));
await check("nobody writes their own league prize", assertFails(setDoc(doc(db("u3"), "users", "u3", "private", "leagueAwards"), { won: ["crown"] })));

// ---- moderation
await check("a player cannot write a penalty against another", assertFails(setDoc(doc(db("u1"), "penalties", "p1"), { uid: "u2", xp: 500, appliedAt: 0 })));
await seed(["penalties", "p2"], { uid: "u1", xp: 500, appliedAt: 0 });
await check("a player marks their own penalty applied", assertSucceeds(updateDoc(doc(db("u1"), "penalties", "p2"), { appliedAt: 5 })));
await check("but cannot soften it", assertFails(updateDoc(doc(db("u1"), "penalties", "p2"), { xp: 0 })));
await check("the bot's training drawings are sealed", assertFails(setDoc(doc(db("u1"), "botTrainedWords", "1"), { wordId: 1, strokesJson: "[]" })));

// ---- friends
await check("a friend request is sent", assertSucceeds(setDoc(doc(db("a"), "users", "b", "friendRequests", "a"), { fromNickname: "A", sentAt: 1 })));
await check("a friend request cannot be sent in someone else's name", assertFails(setDoc(doc(db("c"), "users", "b", "friendRequests", "a"), { fromNickname: "A", sentAt: 1 })));
await seed(["users", "b", "blockedUsers", "d"], { at: 1 });
await check("a blocked player cannot send a request", assertFails(setDoc(doc(db("d"), "users", "b", "friendRequests", "d"), { fromNickname: "D", sentAt: 1 })));
await check("nobody reads another player's requests", assertFails(getDoc(doc(db("c"), "users", "b", "friendRequests", "a"))));

await env.cleanup();
console.log(failures ? `\n${failures} FAILED` : "\nall passed");
process.exit(failures ? 1 : 0);
