// Daily challenge: five words (2 easy, 2 medium, 1 hard) that are the same for everyone on a given Turkish calendar day.
const ISTANBUL = "Europe/Istanbul";
export function todayEpoch() {
  const p = new Intl.DateTimeFormat("en-CA", { timeZone: ISTANBUL, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date()).split("-").map(Number);
  return Math.floor(Date.UTC(p[0], p[1] - 1, p[2]) / 864e5);
}
export function msToMidnight() {
  const now = new Date(), p = new Intl.DateTimeFormat("en-GB", { timeZone: ISTANBUL, hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }).format(now).split(":").map(Number);
  return ((24 * 3600) - (p[0] * 3600 + p[1] * 60 + p[2])) * 1000;
}
function rng(seed) { let a = seed | 0; return () => { a = (a + 0x6d2b79f5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; }; }
const MIX = [["EASY", 2], ["MEDIUM", 2], ["HARD", 1]];
export function dailyWords(words, epoch, lang) {
  const lt = { tr: 7, en: 11, pt: 13, es: 17 }[lang] || 11, picked = [];
  for (const [d, per] of MIX) {
    const deck = words.filter((w) => w.difficulty === d).sort((a, b) => a.id - b.id), cycleLen = Math.floor(deck.length / per);
    if (!cycleLen) { picked.push(...deck.slice(0, per)); continue; }
    const cycle = Math.floor(epoch / cycleLen), day = ((epoch % cycleLen) + cycleLen) % cycleLen, r = rng(lt * 1000 + d.length * 7 + cycle * 104729);
    const sh = [...deck]; for (let i = sh.length - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [sh[i], sh[j]] = [sh[j], sh[i]]; }
    picked.push(...sh.slice(day * per, day * per + per));
  }
  return picked;
}
/** The Turkish calendar month as 202610 (the Seri Sigortası cover is once a month). */
export function turkeyMonth() {
  const p = new Intl.DateTimeFormat("en-CA", { timeZone: ISTANBUL, year: "numeric", month: "2-digit" }).format(new Date()).split("-").map(Number);
  return p[0] * 100 + p[1];
}
export const multiplier = (streak) => Math.min(10, Math.max(1, streak));
export function state(P) {
  const today = todayEpoch(), d = P.daily, last = d.last, live = last >= 0 && today - last <= 1;
  return { today, done: last === today, streak: live ? d.streak : 0, next: multiplier((last === today ? d.streak : live ? d.streak : 0) + (last === today ? 0 : 1)), result: d.result && d.result.day === today ? d.result : null };
}
export function record(P, flags, score) {
  const today = todayEpoch(), d = P.daily; if (d.last === today) return null;
  const gap = d.last < 0 ? 1e9 : today - d.last, streak = gap === 1 ? d.streak + 1 : 1, correct = flags.filter(Boolean).length, xp = (40 + correct * 10) * multiplier(streak);
  d.last = today; d.streak = streak; d.best = Math.max(d.best, streak); d.result = { day: today, flags, score, xp, streak };
  return { xp, streak, correct };
}
