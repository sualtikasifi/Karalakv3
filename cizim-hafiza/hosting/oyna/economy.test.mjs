// Run: node --test hosting/oyna/economy.test.mjs — the same cases as RoundRewardsTest.kt / PlayerLevelTest.kt.
import test from "node:test";
import assert from "node:assert/strict";
import * as E from "./economy.js";

const none = { fx: () => 0, has: () => false };
const withFx = (m, flags = []) => ({ fx: (k) => m[k] || 0, has: (k) => flags.includes(k) });

test("a plain solo round pays ten gold per word and nothing from runes", () => {
  const g = E.soloGold({ ...none, correctInOrder: [true, false, true, true], isDaily: false, newAchievements: 0, newStars: 0, dailyStreak: null, quickWon: false });
  assert.equal(g.play, 30); assert.equal(g.total, 30); assert.equal(g.runeBonus, 0);
});

test("the word rune grows the whole round so nothing is rounded away", () => {
  // Six words of 10 at +8%: 64, where 10 per word rounded down would have paid 60.
  const g = E.soloGold({ ...withFx({ WORD: 8 }), correctInOrder: Array(6).fill(true), isDaily: false, newAchievements: 0, newStars: 0, dailyStreak: null, quickWon: false });
  assert.equal(g.play, 64); assert.equal(g.runeBonus, 4);
});

test("the chain rune pays per full three in a row", () => {
  const has = withFx({}, ["STREAK"]).has;
  assert.equal(E.chainBonus(none.has, Array(6).fill(true)), 0);
  assert.equal(E.chainBonus(has, Array(6).fill(true)), 20);
  assert.equal(E.chainBonus(has, [true, true, false, true, true, false]), 0);
  assert.equal(E.chainBonus(has, [true, true, true, false, true, true]), 10);
});

test("the daily challenge pays its own gold instead of word gold", () => {
  const g = E.soloGold({ ...none, correctInOrder: Array(5).fill(true), isDaily: true, newAchievements: 0, newStars: 0, dailyStreak: 3, quickWon: false });
  assert.equal(g.play, 0); assert.equal(g.daily, 300 + 3 * 30); assert.equal(g.total, g.daily);
});

test("stars, achievements and a quick match win add up", () => {
  const g = E.soloGold({ ...none, correctInOrder: [true], isDaily: false, newAchievements: 2, newStars: 3, dailyStreak: null, quickWon: true });
  assert.equal(g.achievements, 200); assert.equal(g.level, 150); assert.equal(g.win, 100);
  assert.equal(g.total, g.play + g.achievements + g.level + g.win);
});

test("negative stars never take gold away", () => {
  assert.equal(E.soloGold({ ...none, correctInOrder: [], isDaily: false, newAchievements: 0, newStars: -2, dailyStreak: null, quickWon: false }).total, 0);
});

test("room percentages are taken of the plain amount only", () => {
  assert.equal(E.roomRaw(4, true, 0), 140);
  const plain = E.roomGold({ ...none, correctCount: 4, won: true, newAchievements: 0, asHost: false, bestFriendBonus: false });
  assert.equal(plain.total, 140); assert.equal(plain.runeBonus, 0);
  const withRoom = E.roomGold({ ...withFx({ ROOM: 10 }), correctCount: 4, won: true, newAchievements: 0, asHost: false, bestFriendBonus: false });
  assert.equal(withRoom.room, 14); assert.equal(withRoom.runeBonus, withRoom.total - 140);
});

test("the founder rune counts only for the host and the friend bonus adds half", () => {
  const f = withFx({ HOST: 10 });
  assert.equal(E.roomGold({ ...f, correctCount: 5, won: false, newAchievements: 0, asHost: false, bestFriendBonus: false }).room, 0);
  assert.equal(E.roomGold({ ...f, correctCount: 5, won: false, newAchievements: 0, asHost: true, bestFriendBonus: false }).room, 5);
  assert.equal(E.roomGold({ ...none, correctCount: 5, won: false, newAchievements: 0, asHost: false, bestFriendBonus: true }).room, 25);
});

test("an empty room round pays nothing", () => {
  const g = E.roomGold({ ...withFx({ ROOM: 40, HOST: 30 }), correctCount: 0, won: false, newAchievements: 0, asHost: true, bestFriendBonus: true });
  assert.equal(g.total, 0); assert.equal(g.runeBonus, 0);
});

test("word XP: a base by difficulty plus a speed bonus", () => {
  assert.equal(E.wordXp("EASY", 7000), 3); assert.equal(E.wordXp("MEDIUM", 5000), 5); assert.equal(E.wordXp("HARD", 3000), 8); assert.equal(E.wordXp("EASY", 1000), 6);
});

test("level completion and daily XP", () => {
  assert.deepEqual([3, 2, 1, 0].map(E.levelCompletionBonus), [30, 15, 5, 0]);
  assert.equal(E.dailyChallengeXp(3, 1), 70); assert.equal(E.dailyChallengeXp(3, 4), 280); assert.equal(E.dailyChallengeXp(3, 99), 700);
});

test("XP runes speed up the level but never the league XP", () => {
  const P = { xp: 0, periodXp: 0 };
  assert.equal(E.addXp(P, 100, 30), 130); assert.equal(P.xp, 130); assert.equal(P.periodXp, 100);
  assert.equal(E.addXp(P, 0, 30), 0);
});

test("the quick match walk-away penalty never goes below zero", () => {
  const P = { xp: 10, periodXp: 40 };
  assert.equal(E.revokeXp(P, 25), 10); assert.equal(P.xp, 0); assert.equal(P.periodXp, 15);
});

test("config values are taken within range, otherwise the default stays", () => {
  E.applyConfig({ goldPerCorrectWord: 15, dailyGoldBase: 400 });
  assert.equal(E.CFG.goldPerCorrectWord, 15); assert.equal(E.dailyChallengeGold(3), 490);
  E.applyConfig({ goldPerCorrectWord: 0, onlineWinGold: "lots" });
  assert.equal(E.CFG.goldPerCorrectWord, 10); assert.equal(E.CFG.onlineWinGold, 100); assert.equal(E.CFG.dailyGoldBase, 300);
  E.applyConfig(null);
});
