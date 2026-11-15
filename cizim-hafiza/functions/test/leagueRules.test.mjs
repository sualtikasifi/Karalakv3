import test from "node:test";
import assert from "node:assert/strict";
import { isPlausibleScore, maxPlausiblePeriodXp, monthStartMsFor } from "../lib/leagueRules.js";

const OFFSET = 3 * 3600 * 1000;
const HOUR = 3600 * 1000;
const start = monthStartMsFor(2026 * 12 + 9, OFFSET); // 1 Oct 2026, Istanbul

test("month start is midnight Istanbul on the 1st", () => {
  assert.equal(new Date(start).toISOString(), "2026-09-30T21:00:00.000Z");
  // December rolls into January of the next year
  assert.equal(new Date(monthStartMsFor(2026 * 12 + 12, OFFSET)).toISOString(), "2026-12-31T21:00:00.000Z");
});

test("the allowance is a burst plus 3000 an hour", () => {
  assert.equal(maxPlausiblePeriodXp(start, start), 30000);
  assert.equal(maxPlausiblePeriodXp(start, start + 10 * HOUR), 60000);
  // a clock before the month start never goes below the burst
  assert.equal(maxPlausiblePeriodXp(start, start - HOUR), 30000);
});

test("an ordinary score passes", () => {
  assert.ok(isPlausibleScore({ periodXp: 10282, totalXp: 11085 }, start, start + 10 * 24 * HOUR));
});

test("a score above the month's allowance is refused", () => {
  assert.equal(isPlausibleScore({ periodXp: 119132, totalXp: 5_000_000 }, start, start + 2 * HOUR), false);
});

test("a month's score above lifetime XP is refused, with a small margin", () => {
  assert.equal(isPlausibleScore({ periodXp: 5000, totalXp: 4000 }, start, start + 20 * 24 * HOUR), false);
  assert.ok(isPlausibleScore({ periodXp: 4030, totalXp: 4000 }, start, start + 20 * 24 * HOUR));
});

test("profiles without lifetime XP are judged on the allowance alone", () => {
  assert.ok(isPlausibleScore({ periodXp: 140 }, start, start + HOUR));
  assert.ok(isPlausibleScore({ periodXp: 140, totalXp: null }, start, start + HOUR));
});

test("negative or non-numeric scores are refused", () => {
  assert.equal(isPlausibleScore({ periodXp: -1 }, start, start + HOUR), false);
  assert.equal(isPlausibleScore({ periodXp: NaN }, start, start + HOUR), false);
});

import { isPrizeEligible, PRIZE_MIN_ACCOUNT_AGE_MS } from "../lib/leagueRules.js";

test("a guest account cannot take the prize", () => {
  assert.equal(isPrizeEligible({ providers: [], createdMs: start - 100 * 24 * HOUR }, start + 30 * 24 * HOUR), false);
});

test("a signed-in account of a week or more can", () => {
  const end = start + 31 * 24 * HOUR;
  assert.equal(isPrizeEligible({ providers: ["google.com"], createdMs: end - PRIZE_MIN_ACCOUNT_AGE_MS }, end), true);
  assert.equal(isPrizeEligible({ providers: ["password"], createdMs: start }, end), true);
});

test("an account made in the last week of the month cannot", () => {
  const end = start + 31 * 24 * HOUR;
  assert.equal(isPrizeEligible({ providers: ["google.com"], createdMs: end - 2 * 24 * HOUR }, end), false);
  assert.equal(isPrizeEligible(null, end), false);
});
