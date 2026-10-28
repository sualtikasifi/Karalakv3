// Fills the Portuguese and Spanish texts of the web game's own data files from the Android translations, so a language
// added to the app reaches the browser without a second, hand-kept copy:
//   achievements.json, pens.json, frames.json, worlds.json   each { tr, en } text is matched to the Android string that holds
//                                                            exactly that pair, and that string's pt / es join it
//   entry_quotes.json                                        online_lobby_quotes, split into the lines the lobby shows
//   ghost_names.json                                         the nickname lists in domain/model/BotGhostRun.kt
// Run: node scripts/gen_web_langs.js   (from the repo's cizim-hafiza folder). Commit the result.
const fs = require("fs");
const path = require("path");
const root = path.join(__dirname, "..");
const LANGS = ["pt", "es"];
const unxml = (s) => s.replace(/\\'/g, "'").replace(/\\"/g, '"').replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&amp;/g, "&").replace(/\\n/g, "\n").replace(/\\@/g, "@");
const res = (dir) => fs.readFileSync(path.join(root, "app/src/main/res", dir, "strings.xml"), "utf8");
function strings(dir) {
  const out = {};
  for (const m of res(dir).matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) out[m[1]] = unxml(m[2]);
  return out;
}
function arrays(dir) {
  const out = {};
  for (const m of res(dir).matchAll(/<string-array name="([^"]+)"[^>]*>([\s\S]*?)<\/string-array>/g)) out[m[1]] = [...m[2].matchAll(/<item>([\s\S]*?)<\/item>/g)].map((i) => unxml(i[1]));
  return out;
}
const S = { tr: strings("values-tr"), en: strings("values"), pt: strings("values-pt"), es: strings("values-es") };
// "tr\u0000en" -> key, for the pair lookup
const byPair = new Map();
for (const k of Object.keys(S.en)) byPair.set(`${S.tr[k]}\u0000${S.en[k]}`, k);
const missing = [];
const keyOf = (tr, en, where) => { const k = byPair.get(`${tr}\u0000${en}`); if (!k) missing.push(`${where}: "${en}"`); return k; };
/** Sets o[pre+pt] / o[pre+es] from the string holding o[pre+tr] / o[pre+en]; an empty pair stays empty. */
function fill(o, pre, where) {
  if (!o[pre + "en"]) { for (const l of LANGS) o[pre + l] = o[pre + "en"] ?? ""; return; }
  const k = keyOf(o[pre + "tr"], o[pre + "en"], where);
  for (const l of LANGS) o[pre + l] = k ? S[l][k] : o[pre + "en"];
}
const file = (n) => path.join(root, "hosting/oyna", n);
const rw = (n, fn) => { const d = JSON.parse(fs.readFileSync(file(n), "utf8")); fn(d); fs.writeFileSync(file(n), JSON.stringify(d)); };

rw("achievements.json", (list) => list.forEach((a) => {
  const t = keyOf(a.tr[0], a.en[0], `achievement ${a.id}`), d = keyOf(a.tr[1], a.en[1], `achievement ${a.id}`);
  for (const l of LANGS) a[l] = [t ? S[l][t] : a.en[0], d ? S[l][d] : a.en[1]];
}));
rw("pens.json", (list) => list.forEach((p) => { fill(p, "", `pen ${p.id}`); fill(p, "tag_", `pen ${p.id}`); }));
rw("frames.json", (list) => list.forEach((f) => fill(f, "", `frame ${f.id}`)));
rw("worlds.json", (w) => { w.worlds.forEach((x) => fill(x, "", `world ${x.id}`)); w.stages.forEach((x, i) => fill(x, "", `stage ${i + 1}`)); });

const A = { pt: arrays("values-pt"), es: arrays("values-es") };
rw("entry_quotes.json", (q) => { for (const l of LANGS) q[l] = A[l].online_lobby_quotes.map((s) => s.split("\n")); });

const kt = fs.readFileSync(path.join(root, "app/src/main/java/com/sualtikasifi/cizimhafiza/domain/model/BotGhostRun.kt"), "utf8");
const names = (v) => [...new RegExp(`val ${v} = listOf\\(([\\s\\S]*?)\\n    \\)`).exec(kt)[1].matchAll(/"([^"]+)"/g)].map((m) => m[1]);
rw("ghost_names.json", (g) => { g.pt = names("NICKNAMES_PT"); g.es = names("NICKNAMES_ES"); });

if (missing.length) { console.error("No Android string holds these texts (fix the web file or the app):\n  " + missing.join("\n  ")); process.exit(1); }
console.log("web data files: Portuguese and Spanish filled from the app");
