// The rules of XP and gold, ported one-to-one from the Android app so a round pays the same on the phone and in the browser:
//   XpAwards            (domain/model/PlayerLevel.kt)
//   RoundRewards        (domain/model/RoundRewards.kt)
//   RuneEffects.scaled  (domain/model/Rune.kt)
// Pure functions, no DOM and no storage: the game passes in the player's rune effects as `fx(name)` (percent, already capped)
// and `has(name)` (a one-off ability is bought). economy.test.mjs runs the same cases as RoundRewardsTest.kt.

/** The amounts config/economy can change (EconomyConfig.kt); the defaults are the designed ones. */
export const CFG = {
  goldPerCorrectWord: 10, onlineWinGold: 100, achievementGold: 100, levelGoldPerNewStar: 50,
  dailyGoldBase: 300, dailyGoldPerStreakDay: 30, chainBonusGold: 10,
};
const RANGES = { goldPerCorrectWord: [1, 1000], onlineWinGold: [0, 10000], achievementGold: [0, 10000], levelGoldPerNewStar: [0, 10000], dailyGoldBase: [0, 100000], dailyGoldPerStreakDay: [0, 10000], chainBonusGold: [0, 1000] };
const DEFAULTS = { ...CFG };

/** Takes a config/economy document; a missing, wrong-typed or out-of-range field keeps its default (EconomyConfig.apply). */
export function applyConfig(data) {
  for (const k of Object.keys(DEFAULTS)) {
    const v = data && typeof data[k] === "number" ? Math.trunc(data[k]) : null;
    const [lo, hi] = RANGES[k];
    CFG[k] = v !== null && v >= lo && v <= hi ? v : DEFAULTS[k];
  }
}

// ---------- XP ----------
export const ONLINE_MATCH_XP = 8, ONLINE_WIN_XP = 25, DAILY_COMPLETION_XP = 40, DAILY_CORRECT_WORD_XP = 10, MAX_DAILY_STREAK_MULTIPLIER = 10;
export const QUICK_MATCH_DAILY_BONUS_MULTIPLIER = 2, QUICK_MATCH_ABANDON_PENALTY_XP = 25;

/** XP for one right word: a base by difficulty plus a bonus for answering fast (XpAwards.wordXp). */
export function wordXp(difficulty, ms) {
  const base = difficulty === "HARD" ? 6 : difficulty === "MEDIUM" ? 4 : 3;
  return base + (ms < 2000 ? 3 : ms < 4000 ? 2 : ms < 6000 ? 1 : 0);
}
export const levelCompletionBonus = (stars) => (stars === 3 ? 30 : stars === 2 ? 15 : stars === 1 ? 5 : 0);
export const dailyStreakMultiplier = (streak) => Math.min(MAX_DAILY_STREAK_MULTIPLIER, Math.max(1, streak));
export const dailyChallengeXp = (correct, streak) => (DAILY_COMPLETION_XP + Math.max(0, correct) * DAILY_CORRECT_WORD_XP) * dailyStreakMultiplier(streak);

/**
 * Pays [amount] XP the way SettingsRepository.addXp does: the XP runes speed up the player's LEVEL only, the league
 * (period) XP stays exactly what was earned. Returns what the level received.
 */
export function addXp(P, amount, xpPercent) {
  if (amount <= 0) return 0;
  const gained = amount + Math.floor((amount * Math.max(0, xpPercent)) / 100);
  P.xp += gained;
  P.periodXp = (P.periodXp || 0) + amount;
  return gained;
}

/** The walk-away penalty of a Quick Match (SettingsRepository.revokeXp): never below zero, league XP too. Returns what was taken. */
export function revokeXp(P, amount) {
  const lost = Math.min(Math.max(0, amount), P.xp);
  P.xp = Math.max(0, P.xp - amount); P.periodXp = Math.max(0, (P.periodXp || 0) - amount);
  return lost;
}

// ---------- gold ----------
export const scaled = (base, percent) => (percent <= 0 ? base : Math.floor((base * (100 + percent)) / 100));
export const dailyChallengeGold = (streak) => CFG.dailyGoldBase + Math.min(Math.max(streak, 0), 10) * CFG.dailyGoldPerStreakDay;

/** Gold for runs of three right words in a row (the Zincir Ustası rune), within each run (RoundRewards.chainBonus). */
export function chainBonus(has, correctInOrder) {
  if (!has("STREAK")) return 0;
  let run = 0, bonus = 0;
  for (const ok of correctInOrder) { if (ok) { run++; if (run % 3 === 0) bonus += CFG.chainBonusGold; } else run = 0; }
  return bonus;
}

const total = (g) => g.play + g.achievements + g.level + g.daily + g.win + g.room;

/**
 * A solo round: a level, an offline game, a Quick Match or the daily challenge (RoundRewards.solo).
 * `newStars` is the stars beyond the level's best; `dailyStreak` is set only when this round completed today's challenge
 * for the first time; `quickWon` when a Quick Match was won outright.
 */
export function soloGold({ fx, has, correctInOrder, isDaily, newAchievements, newStars, dailyStreak, quickWon }) {
  const rawWords = correctInOrder.filter(Boolean).length * CFG.goldPerCorrectWord;
  const words = scaled(rawWords, fx("WORD"));
  const chain = chainBonus(has, correctInOrder);
  const play = isDaily ? 0 : words + chain;
  const rawDaily = dailyStreak == null ? 0 : dailyChallengeGold(dailyStreak);
  const daily = dailyStreak == null ? 0 : scaled(rawDaily, fx("DAILY"));
  const rawWin = quickWon ? CFG.onlineWinGold : 0;
  const win = quickWon ? scaled(rawWin, fx("WIN")) : 0;
  const g = { play, achievements: newAchievements * CFG.achievementGold, level: Math.max(0, newStars) * CFG.levelGoldPerNewStar, daily, win, room: 0 };
  g.runeBonus = (isDaily ? 0 : words - rawWords + chain) + (daily - rawDaily) + (win - rawWin);
  g.total = total(g);
  return g;
}

/** The plain gold of an online room round, before any rune: what the room percentages are taken of. */
export const roomRaw = (correctCount, won, newAchievements) =>
  correctCount * CFG.goldPerCorrectWord + (won ? CFG.onlineWinGold : 0) + newAchievements * CFG.achievementGold;

/**
 * An online room round. The word and win runes grow their own parts; the room runes (friends always, founder when `asHost`)
 * and the once-a-day Dostluk Bağı +50% are taken of the plain amount, so they do not multiply what the other runes added.
 */
export function roomGold({ fx, correctCount, won, newAchievements, asHost, bestFriendBonus }) {
  const raw = roomRaw(correctCount, won, newAchievements);
  const words = scaled(correctCount * CFG.goldPerCorrectWord, fx("WORD"));
  const win = won ? scaled(CFG.onlineWinGold, fx("WIN")) : 0;
  const achievements = newAchievements * CFG.achievementGold;
  const percent = fx("ROOM") + (asHost ? fx("HOST") : 0) + (bestFriendBonus ? 50 : 0);
  const room = raw > 0 ? Math.floor((raw * percent) / 100) : 0;
  const g = { play: words, achievements, level: 0, daily: 0, win, room };
  g.total = total(g);
  g.runeBonus = g.total - raw;
  return g;
}
