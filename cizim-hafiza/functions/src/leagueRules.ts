/**
 * Server-side plausibility of a published league score.
 *
 * Firestore rules already bound how fast `periodXp` may grow per write (see plausibleXpGrowth in firestore.rules), but
 * rules cannot look at the whole table, and the old documents they never saw (before the rule, or written by a
 * modified client that found a gap) stay as they are. The hourly table build therefore re-checks every real row against
 * two things an honest account can never break, and leaves a row that breaks either one out of the table and out of the
 * prize — so a faked score does not decide a month.
 *
 * - A month's XP cannot exceed what the growth rule allows from the first of the month: the 30000 burst plus 3000 an hour.
 * - A month's XP cannot exceed the account's lifetime XP (they are published together; a small margin covers the two
 *   being rounded or penalised at slightly different moments).
 *
 * Kept free of Firebase imports so it can be unit-tested with plain Node (see test/leagueRules.test.mjs).
 */
export const BURST_XP = 30000;
export const XP_PER_HOUR = 3000;
const LIFETIME_MARGIN = 50;

/** The most `periodXp` an honest account can show at [nowMs], for a month that began at [monthStartMs]. */
export function maxPlausiblePeriodXp(monthStartMs: number, nowMs: number): number {
  const hours = Math.max(0, (nowMs - monthStartMs) / 3_600_000);
  return Math.floor(BURST_XP + hours * XP_PER_HOUR);
}

export interface ScoreFields {
  periodXp: number;
  /** Lifetime XP; absent on profiles written before the field existed. */
  totalXp?: number | null;
}

export function isPlausibleScore(row: ScoreFields, monthStartMs: number, nowMs: number): boolean {
  const xp = row.periodXp;
  if (typeof xp !== "number" || !Number.isFinite(xp) || xp < 0) return false;
  if (xp > maxPlausiblePeriodXp(monthStartMs, nowMs)) return false;
  if (typeof row.totalXp === "number" && row.totalXp > 0 && xp > row.totalXp + LIFETIME_MARGIN) return false;
  return true;
}

/** Start (inclusive) of a league month in Istanbul time, as epoch ms. [periodId] = year * 12 + (month - 1). */
export function monthStartMsFor(periodId: number, istanbulOffsetMs: number): number {
  const year = Math.floor(periodId / 12);
  const month = periodId % 12;
  return Date.UTC(year, month, 1) - istanbulOffsetMs;
}
