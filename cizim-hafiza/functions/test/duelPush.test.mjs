import { test } from "node:test";
import assert from "node:assert/strict";
import { planDuelPushes, DUEL_PUSH_WINDOW_MS } from "../lib/duelPush.js";

const NOW = 1_800_000_000_000;
const duel = (o) => ({ id: "d1", data: { challengerUid: "a", challengerName: "Ayşe", opponentUid: "b", opponentName: "Can", status: "AWAITING_OPPONENT", createdAt: NOW - 60_000, completedAt: null, ...o } });

test("a new challenge is told to the opponent, naming the challenger", () => {
  const [p] = planDuelPushes([duel({})], NOW);
  assert.deepEqual([p.toUid, p.fromNickname, p.type, p.stamp], ["b", "Ayşe", "duel", "pushedAt"]);
});

test("a challenge already pushed is never sent twice", () => {
  assert.equal(planDuelPushes([duel({ pushedAt: NOW - 1000 })], NOW).length, 0);
});

test("a duel older than the window is left alone", () => {
  assert.equal(planDuelPushes([duel({ createdAt: NOW - DUEL_PUSH_WINDOW_MS - 1 })], NOW).length, 0);
});

test("a played duel tells the challenger, not the opponent again", () => {
  const out = planDuelPushes([duel({ status: "COMPLETE", completedAt: NOW - 1000 })], NOW);
  assert.equal(out.length, 1);
  assert.deepEqual([out[0].toUid, out[0].fromNickname, out[0].type, out[0].stamp], ["a", "Can", "duel_result", "resultPushedAt"]);
});

test("the result is sent once, and a duel with no name sends nothing to name", () => {
  assert.equal(planDuelPushes([duel({ status: "COMPLETE", completedAt: NOW - 1000, resultPushedAt: 5 })], NOW).length, 0);
  assert.equal(planDuelPushes([duel({ opponentUid: "" })], NOW).length, 0);
});
