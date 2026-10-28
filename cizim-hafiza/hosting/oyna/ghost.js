// Hızlı Eşleş's opponent, ported from Android (BotGhostRun.kt + GhostRunRepositoryImpl.botOpponent): the match is built out of the
// hand-trained drawing set (botTrainingIndex / botTrainedWords), the same way the app does it, so the web player draws the SAME
// trained words and the rounds that come back are the ones the review queue needs. Everything about the opponent (name, level, score,
// which words she "forgets") is derived from the run id, so the offer and the result screen always agree.
const PREFIX = "ghost:", NAME_SALT = 0x9e3779b9, LEVEL_SALT = 0x7f4a7c15, LEVEL_SPREAD = 6, SPEED_BONUS_PERCENT = 40, WORD_WINDOW = 40, RUN_WORDS = 10;
export const GHOST_UID = "karalak-ghost";
let names = null;
export async function loadGhostNames() { if (!names) names = await (await fetch("ghost_names.json")).json(); }

/** Small seeded generator (mulberry32): the same seed always rolls the same numbers. */
function rng(seed) { let a = seed >>> 0; return () => { a = (a + 0x6d2b79f5) >>> 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; }; }
const shuffled = (n, r) => { const a = Array.from({ length: n }, (_, i) => i); for (let i = n - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };

export const isGhostId = (id) => typeof id === "string" && id.startsWith(PREFIX);
export const ghostId = (seed, wordIds) => PREFIX + seed + ":" + wordIds.join(",");
export function parseGhostId(id) {
  if (!isGhostId(id)) return null;
  const body = id.slice(PREFIX.length), seed = Number(body.split(":")[0]), ids = body.slice(body.indexOf(":") + 1).split(",").map(Number);
  return Number.isFinite(seed) && ids.length && ids.every(Number.isFinite) ? { seed, wordIds: ids } : null;
}
/** Per word, in the run's own order: did she recall her own drawing? Plus the score that follows from it. */
export function ghostOutcome(seed, wordIds, points, speedBonus) {
  const r = rng(seed), roll = Math.floor(r() * 100), target = roll < 15 ? 0 : roll < 50 ? 1 : roll < 80 ? 2 : 3, wrongCount = Math.min(target, wordIds.length);
  const wrong = new Set(shuffled(wordIds.length, r).slice(0, wrongCount)), correctness = wordIds.map((_, i) => !wrong.has(i));
  const correctCount = correctness.filter(Boolean).length;
  let bonuses = 0; for (let i = 0; i < correctCount; i++) if (Math.floor(r() * 100) < SPEED_BONUS_PERCENT) bonuses++;
  return { correctness, correctCount, totalScore: correctCount * points + bonuses * speedBonus, fastestCorrectMs: correctCount ? 1200 + Math.floor(r() * 2301) : null };
}
const levelBonus = (f) => (f >= 1 ? 30 : f >= 0.9 ? 10 : f >= 0.8 ? 5 : f >= 0.6 ? 0 : f >= 0.4 ? -4 : -8);
/** One synthesized round for a challenger of [level], or null when the trained set cannot fill ten words of this language's pool. */
export function makeGhostRun({ trainedIds, hasWord, language, level, points, speedBonus, frameFor }) {
  const r0 = rng((Math.random() * 4294967296) >>> 0), pool = shuffled(trainedIds.length, r0).slice(0, WORD_WINDOW).map((i) => trainedIds[i]);
  const wordIds = pool.filter(hasWord).slice(0, RUN_WORDS);
  if (wordIds.length < RUN_WORDS) return null;
  const seed = Math.floor(Math.random() * 2147483647), o = ghostOutcome(seed, wordIds, points, speedBonus);
  const list = (names && (names[language] || names.en)) || ["Player"];
  const lvl = Math.min(100, Math.max(1, level + levelBonus(o.correctCount / wordIds.length) + (Math.floor(rng(seed + LEVEL_SALT)() * (2 * LEVEL_SPREAD + 1)) - LEVEL_SPREAD)));
  return { id: ghostId(seed, wordIds), uid: GHOST_UID, nickname: list[Math.floor(rng(seed + NAME_SALT)() * list.length)], level: lvl, frameId: frameFor ? frameFor(lvl) : "SCRIBBLER", wordIds, totalScore: o.totalScore, correctCount: o.correctCount, fastestCorrectMs: o.fastestCorrectMs };
}
