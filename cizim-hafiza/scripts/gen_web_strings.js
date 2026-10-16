// Exports the Android string resources (Turkish and English) to hosting/oyna/strings.json, so the web game reads the SAME texts as
// the app and can never drift from them: the web asks i18n.js for `S("chest_detail_gold", 150, 250)` instead of holding its own copy.
// Run: node scripts/gen_web_strings.js   (from the repo's cizim-hafiza folder). Commit the result.
const fs = require("fs");
const path = require("path");
const root = path.join(__dirname, "..");
const unxml = (s) => s.replace(/\\'/g, "'").replace(/\\"/g, '"').replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&amp;/g, "&").replace(/\\n/g, "\n").replace(/\\@/g, "@").replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16)));
function strings(file) {
  const out = {}, s = fs.readFileSync(path.join(root, "app/src/main/res", file), "utf8");
  for (const m of s.matchAll(/<string name="([^"]+)"([^>]*)>([\s\S]*?)<\/string>/g)) { if (/translatable="false"/.test(m[2])) continue; out[m[1]] = unxml(m[3]); }
  return out;
}
const tr = strings("values-tr/strings.xml"), en = strings("values/strings.xml");
// the rune, developer-panel and bug-report texts are not needed by the web game; runes have their own generated file (gen_web_runes.js)
const skip = /^(runes_dev_|reports_|bot_|word_review|difficulty_review|coupon_panel|admin_|stats_|locks_|dev_)/;
const out = {};
for (const k of Object.keys(en)) { if (skip.test(k)) continue; out[k] = { tr: tr[k] ?? en[k], en: en[k] }; }
fs.writeFileSync(path.join(root, "hosting/oyna/strings.json"), JSON.stringify(out));
console.log(`strings.json: ${Object.keys(out).length} texts, ${(fs.statSync(path.join(root, "hosting/oyna/strings.json")).size / 1024).toFixed(0)} KB`);
