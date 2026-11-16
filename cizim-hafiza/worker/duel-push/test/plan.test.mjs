import test from "node:test";
import assert from "node:assert/strict";
import { planDuelPushes, plain, DUEL_PUSH_WINDOW_MS } from "../src/index.js";

const now = 1_000_000_000_000;
test("a fresh pending duel is announced to the opponent", () => {
  const [p] = planDuelPushes([{ id: "a", data: { opponentUid: "u2", challengerName: "Ayşe", createdAt: now - 60_000, status: "AWAITING_OPPONENT" } }], [], now);
  assert.deepEqual([p.toUid, p.fromNickname, p.type, p.stamp, p.send], ["u2", "Ayşe", "duel", "pushedAt", true]);
});
test("an old duel is stamped without a push", () => {
  const [p] = planDuelPushes([{ id: "a", data: { opponentUid: "u2", challengerName: "Ayşe", createdAt: now - DUEL_PUSH_WINDOW_MS - 1 } }], [], now);
  assert.equal(p.send, false);
});
test("a played duel tells the challenger", () => {
  const [p] = planDuelPushes([], [{ id: "b", data: { challengerUid: "u1", opponentName: "Can", completedAt: now - 1000 } }], now);
  assert.deepEqual([p.toUid, p.fromNickname, p.type, p.stamp, p.send], ["u1", "Can", "duel_result", "resultPushedAt", true]);
});
test("firestore values become plain numbers and strings", () => {
  assert.equal(plain({ integerValue: "42" }), 42);
  assert.equal(plain({ stringValue: "x" }), "x");
  assert.equal(plain({ nullValue: null }), undefined);
});
