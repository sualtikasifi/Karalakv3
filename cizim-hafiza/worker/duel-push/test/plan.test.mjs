import test from "node:test";
import assert from "node:assert/strict";
import { planDuelPushes, plain, describeDuel, owedQuery, DUEL_PUSH_WINDOW_MS } from "../src/index.js";

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

import { planBroadcast, weekTopic } from "../src/admin.js";
import { decode, encode } from "../src/google.js";

test("a plain broadcast goes to the news topic", () => {
  const sends = planBroadcast({ title: "Merhaba", body: "Yeni bir güncelleme var" }, new Date("2026-11-16T12:00:00Z"));
  assert.deepEqual(sends.map((s) => [s.topic, s.title]), [["news", "Merhaba"]]);
});
test("per-language texts go to their own topic, with the Turkish one as the fallback", () => {
  const sends = planBroadcast({ texts: { tr: { title: "T", body: "b" }, en: { title: "E", body: "e" } } }, new Date("2026-11-16T12:00:00Z"));
  assert.deepEqual(sends.map((s) => s.label), ["tr", "en", "fallback"]);
  assert.match(sends[2].condition, /!\('news_tr' in topics\)/);
});
test("a lapsed audience has no fallback", () => {
  const sends = planBroadcast({ title: "T", body: "b", audience: "lapsed" }, new Date("2026-11-16T12:00:00Z"));
  assert.deepEqual(sends.map((s) => s.label), ["tr"]);
  assert.match(sends[0].condition, /seen_v1/);
});
test("a broadcast without text sends nothing", () => assert.deepEqual(planBroadcast({}, new Date()), []));
test("the ISO week topic", () => assert.equal(weekTopic(new Date("2026-11-16T12:00:00Z")), "seen_2026w47"));
test("values survive an encode and decode", () => {
  const x = { a: 5, b: "s", c: true, d: null, e: [1, "z"], f: { g: 2.5 } };
  assert.deepEqual(decode(encode(x)), x);
});

test("a duel is described with how its markers look", () => {
  const doc = { name: "projects/p/databases/(default)/documents/duels/abcdef123", fields: {
    status: { stringValue: "AWAITING_OPPONENT" }, createdAt: { integerValue: String(now - 5 * 60_000) }, pushedAt: { nullValue: null },
  } };
  assert.equal(describeDuel(doc, now), "recent duel abcdef: AWAITING_OPPONENT, 5 min ago, pushedAt=null, resultPushedAt=missing");
  const done = { name: "x/duels/zz", fields: { status: { stringValue: "COMPLETE" }, pushedAt: { integerValue: "1" }, resultPushedAt: { nullValue: null } } };
  assert.equal(describeDuel(done, now), "recent duel zz: COMPLETE, age unknown, pushedAt=set, resultPushedAt=null");
});

test("the owed-duels query tests the marker with IS_NULL, not with an EQUAL on a null value (which matches nothing in Firestore REST)", () => {
  const q = owedQuery("AWAITING_OPPONENT", "pushedAt");
  const [status, marker] = q.where.compositeFilter.filters;
  assert.equal(status.fieldFilter.value.stringValue, "AWAITING_OPPONENT");
  assert.deepEqual(marker, { unaryFilter: { op: "IS_NULL", field: { fieldPath: "pushedAt" } } });
  assert.ok(!JSON.stringify(q.where).includes("nullValue"));
});
