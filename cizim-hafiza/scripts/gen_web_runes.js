// Regenerates hosting/oyna/runes.json from the Android sources, so the web rune map can never drift from the app:
//   domain/model/Rune.kt           the map (ids, branches, effects, per-level values, costs, positions, rings) and the effect list
//   res/values{,-tr,-pt,-es}/strings.xml   every rune_* text (names, flavours, effect descriptions, window texts)
// Run: node scripts/gen_web_runes.js   (from the repo's cizim-hafiza folder). Commit the result.
const fs = require("fs");
const path = require("path");
const root = path.join(__dirname, "..");
const kt = fs.readFileSync(path.join(root, "app/src/main/java/com/sualtikasifi/cizimhafiza/domain/model/Rune.kt"), "utf8").replace(/\r\n/g, "\n");

const unxml = (s) => s.replace(/\\'/g, "'").replace(/\\"/g, '"').replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&amp;/g, "&").replace(/\\n/g, "\n");
function strings(file) {
  const out = {}, s = fs.readFileSync(path.join(root, "app/src/main/res", file), "utf8");
  for (const m of s.matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) out[m[1]] = unxml(m[2]);
  return out;
}
const tr = strings("values-tr/strings.xml"), en = strings("values/strings.xml"), pt = strings("values-pt/strings.xml"), es = strings("values-es/strings.xml");

// effects
const fx = {};
const effBlock = kt.slice(kt.indexOf("enum class RuneEffect"), kt.indexOf("/**\n * One node"));
for (const m of effBlock.matchAll(/^\s+(\w+)\("(\w+)", "([^"]+)", RuneUnit\.(\w+), R\.string\.(\w+)\)/gm)) {
  fx[m[1]] = { icon: m[2], emoji: m[3], unit: m[4], tr: tr[m[5]] || "", en: en[m[5]] || "", pt: pt[m[5]] || "", es: es[m[5]] || "" };
}

// runes
const consts = {};
for (const m of kt.matchAll(/const val (\w+) = "([^"]+)"/g)) consts[m[1]] = m[2];
const id = (t) => (t.startsWith('"') ? t.slice(1, -1) : consts[t] || t.toLowerCase());
const runes = [];
const re = /Rune\(("?[\w]+"?), RuneBranch\.(\w+), RuneEffect\.(\w+), R\.string\.(\w+), (\d+), listOf\(([\d_, ]+)\), (?:listOf\(([^)]*)\)|emptyList\(\)), ([-\d.]+)f, ([-\d.]+)f(?:, (\d))?(?:, (true|false))?\)/g;
for (const m of kt.matchAll(re)) {
  const rid = m[1] === "ROOT_ID" ? "root" : id(m[1]), nameKey = m[4], flavorKey = nameKey.replace(/_name$/, "_flavor");
  runes.push({
    id: rid, branch: m[2], effect: m[3], tr: tr[nameKey] || rid, en: en[nameKey] || rid, pt: pt[nameKey] || rid, es: es[nameKey] || rid,
    flavor_tr: tr[flavorKey] || "", flavor_en: en[flavorKey] || "", flavor_pt: pt[flavorKey] || "", flavor_es: es[flavorKey] || "",
    per: +m[5], costs: m[6].split(",").map((x) => +x.replace(/_/g, "")),
    req: m[7] ? m[7].split(",").map((x) => x.trim()).map((x) => (x === "ROOT_ID" ? "root" : id(x))) : [],
    x: +m[8], y: +m[9], ring: +(m[10] || 1), keystone: m[11] === undefined ? null : m[11] === "true",
  });
}

// window and card texts: every rune_* string that is not a name, a flavour or an effect description
const ui = {};
for (const k of Object.keys(en)) {
  if (!/^rune_/.test(k) || /_(name|flavor)$/.test(k) || /^rune_fx_/.test(k)) continue;
  ui[k] = { tr: tr[k] ?? en[k], en: en[k], pt: pt[k] ?? en[k], es: es[k] ?? en[k] };
}
for (const k of Object.keys(en)) if (/^runes_dev_/.test(k)) delete ui[k];
// a few shared texts
for (const k of ["close"]) ui[k] = { tr: tr[k], en: en[k], pt: pt[k], es: es[k] };

// the ring gates and ceilings, read from Rune.kt so the web cannot disagree with the app
const gates = { ring2: +/ring2Level: Int = (\d+)/.exec(kt)[1], ring3: +/ring3Level: Int = (\d+)/.exec(kt)[1], ring4: +/ring4Level: Int = (\d+)/.exec(kt)[1], need: +/percentNeeded: Int = (\d+)/.exec(kt)[1] };
const caps = {};
for (const m of kt.matchAll(/RuneEffect\.(\w+)(?:, RuneEffect\.\w+)* -> (\d+)/g)) caps[m[1]] = +m[2];

// the measured per-rune, per-plate icon centring (RuneIconFix.kt): "id:state" -> [dx, dy] in tile sizes
const fix = {};
const fixSrc = fs.readFileSync(path.join(root, "app/src/main/java/com/sualtikasifi/cizimhafiza/presentation/runes/RuneIconFix.kt"), "utf8");
for (const m of fixSrc.matchAll(/"(\w+:\d)" to \((-?[\d.]+)f to (-?[\d.]+)f\)/g)) fix[m[1]] = [+m[2], +m[3]];

const out = { runes, fx, ui, gates, caps, fix };
fs.writeFileSync(path.join(root, "hosting/oyna/runes.json"), JSON.stringify(out));
console.log(`runes.json: ${runes.length} runes, ${Object.keys(fx).length} effects, ${Object.keys(ui).length} texts, gates ${JSON.stringify(gates)}, caps ${Object.keys(caps).length}`);
