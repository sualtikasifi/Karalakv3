import { test } from "node:test";
import assert from "node:assert/strict";
import { planDuelPushes, DUEL_PUSH_WINDOW_MS } from "../lib/duelPush.js";

const NOW = 1_800_000_000_000;
const duel = (o) => ({ id: "d1", data: { challengerUid: "a", challengerName: "Ayşe", opponentUid: "b", opponentName: "Can", status: "AWAITING_OPPONENT", createdAt: NOW - 60_000, completedAt: null, ...o } });

test("a new challenge is told to the opponent, naming the challenger", () => {
  const [p] = planDuelPushes([duel({})], [], NOW);
  assert.deepEqual([p.toUid, p.fromNickname, p.type, p.stamp, p.send], ["b", "Ayşe", "duel", "pushedAt", true]);
});

test("a duel older than the window is stamped but not announced", () => {
  const [p] = planDuelPushes([duel({ createdAt: NOW - DUEL_PUSH_WINDOW_MS - 1 })], [], NOW);
  assert.equal(p.send, false);
  assert.equal(p.stamp, "pushedAt");
});

test("a challenge that was already played in the meantime is stamped, not announced", () => {
  const [p] = planDuelPushes([duel({ status: "COMPLETE" })], [], NOW);
  assert.equal(p.send, false);
});

test("a played duel tells the challenger, naming who played it", () => {
  const [p] = planDuelPushes([], [duel({ status: "COMPLETE", completedAt: NOW - 1000 })], NOW);
  assert.deepEqual([p.toUid, p.fromNickname, p.type, p.stamp, p.send], ["a", "Can", "duel_result", "resultPushedAt", true]);
});

test("nobody to name or to tell: stamped, nothing sent", () => {
  assert.equal(planDuelPushes([duel({ opponentUid: "" })], [], NOW)[0].send, false);
  assert.equal(planDuelPushes([duel({ challengerName: "" })], [], NOW)[0].send, false);
  assert.equal(planDuelPushes([], [duel({ status: "COMPLETE", completedAt: 0 })], NOW)[0].send, false);
});
