// The league filler players' profile pictures (BotPhotos.kt): writes hosting/oyna/bot_photos.json (name -> file) and copies the
// pictures to hosting/oyna/img/bots/. Names come from league_names.json, which is LeagueBots.NAMES in the same order.
// Run after BotPhotos.kt or LeagueBots.NAMES change:  node scripts/gen_web_botphotos.js
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
const kt = fs.readFileSync(path.join(root, "app/src/main/java/com/sualtikasifi/cizimhafiza/presentation/common/BotPhotos.kt"), "utf8").replace(/\r/g, "");
const grab = (kind, n) => { const i = kt.indexOf("val " + n + " = " + kind + "("), j = kt.indexOf("\n    )", i); return kt.slice(i, j); };
const set = (n) => new Set([...grab("setOf", n).matchAll(/"([^"]+)"/g)].map((m) => m[1]));
const list = (n) => [...grab("listOf", n).matchAll(/bot_photo_(\w+)/g)].map((m) => "bot_photo_" + m[1]);
const FEMALE = set("FEMALE_NAMES"), NEUTRAL_N = set("NEUTRAL_NAMES"), MEN = list("MEN"), WOMEN = list("WOMEN"), NEUTRAL = list("NEUTRAL");
const over = Object.fromEntries([...kt.matchAll(/"(\w+)" to R\.drawable\.(bot_photo_\w+)/g)].map((m) => [m[1], m[2]]));
const names = JSON.parse(fs.readFileSync(path.join(root, "hosting/oyna/league_names.json"), "utf8")), out = {};
let m = 0, w = 0, n = 0;
for (const name of names) {
  // the counters advance for an overridden name too, so no other bot's picture shifts (as in the app)
  const f = FEMALE.has(name) ? WOMEN[w++ % WOMEN.length] : NEUTRAL_N.has(name) ? NEUTRAL[n++ % NEUTRAL.length] : MEN[m++ % MEN.length];
  out[name] = over[name] || f;
}
fs.writeFileSync(path.join(root, "hosting/oyna/bot_photos.json"), JSON.stringify(out));
fs.mkdirSync(path.join(root, "hosting/oyna/img/bots"), { recursive: true });
for (const f of new Set(Object.values(out))) fs.copyFileSync(path.join(root, "app/src/main/res/drawable-nodpi", f + ".jpg"), path.join(root, "hosting/oyna/img/bots", f + ".jpg"));
console.log(Object.keys(out).length + " bots, " + new Set(Object.values(out)).size + " pictures");
