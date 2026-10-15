// Port of util/AnswerMatcher.kt
export function normalize(s) {
  let t = s.trim().toLocaleLowerCase("tr-TR");
  t = t.replace(/ı/g, "i").replace(/İ/g, "i").replace(/ğ/g, "g").replace(/ü/g, "u")
       .replace(/ş/g, "s").replace(/ö/g, "o").replace(/ç/g, "c");
  t = t.normalize("NFD").replace(/\p{Mn}+/gu, "");
  return t.replace(/\s+/g, " ").trim();
}
export function toleranceFor(target) {
  const n = normalize(target).length;
  return n <= 4 ? 0 : n <= 7 ? 1 : 2;
}
function lev(a, b) {
  if (a === b) return 0;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    const cur = [i];
    for (let j = 1; j <= b.length; j++) {
      cur[j] = Math.min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
    }
    prev = cur;
  }
  return prev[b.length];
}
export function isCorrect(answer, target) {
  const a = normalize(answer), b = normalize(target);
  if (!a) return false;
  return a === b || lev(a, b) <= toleranceFor(target);
}
