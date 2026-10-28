// NameFilter.kt for the browser: profanity, slurs and impersonation kept out of the names other players see. Same lists
// (namefilter_data.js, generated from the app), same folding: case and accents dropped, look-alike digits and symbols read
// as letters, runs of three or more of one letter cut to one; every check once with digits as letters, once as separators.
import { SUBSTRINGS, TOKENS } from "./namefilter_data.js";

const TOKEN_SET = new Set(TOKENS);
const LEET = { 0: "o", 1: "i", 3: "e", 4: "a", 5: "s", 7: "t", 8: "b", "@": "a", $: "s", "!": "i", "ß": "s" };
const plain = (name) => name.replace(/İ/g, "i").replace(/I/g, "i").replace(/ı/g, "i").toLowerCase().normalize("NFD").replace(/\p{M}+/gu, "");
const leet = (s) => [...s].map((c) => LEET[c] ?? c).join("");
const collapse = (s) => s.replace(/([a-z])\1{2,}/g, "$1");

export function nameAllowed(name) {
  const p = plain(name);
  for (const v of [leet(p), p]) {
    const letters = collapse(v.replace(/[^a-z]/g, ""));
    if (SUBSTRINGS.some((w) => letters.includes(w)) || TOKEN_SET.has(letters)) return false;
    if (v.split(/[^a-z]+/).filter(Boolean).some((t) => TOKEN_SET.has(t) || TOKEN_SET.has(collapse(t)))) return false;
  }
  return true;
}
