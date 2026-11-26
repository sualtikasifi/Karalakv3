// The league filler players' profile pictures (BotPhotos.kt): writes hosting/oyna/bot_photos.json (name -> avatar id) and copies the
// gallery pictures they wear to hosting/oyna/img/avatars/<id>.webp. The table is BotPhotos.BY_NAME; the names are LeagueBots.NAMES.
// Run after BotPhotos.kt or the avatar gallery change:  node scripts/gen_web_botphotos.js
const fs = require("fs"), path = require("path");
const root = path.join(__dirname, "..");
const kt = fs.readFileSync(path.join(root, "app/src/main/java/com/sualtikasifi/cizimhafiza/presentation/common/BotPhotos.kt"), "utf8").replace(/\r/g, "");
const i = kt.indexOf("BY_NAME"), j = kt.indexOf("\n    )", i);
const out = Object.fromEntries([...kt.slice(i, j).matchAll(/"([^"]+)" to "([^"]+)"/g)].map((m) => [m[1], m[2]]));
const names = JSON.parse(fs.readFileSync(path.join(root, "hosting/oyna/league_names.json"), "utf8"));
const missing = names.filter((n) => !out[n]); if (missing.length) throw new Error("no picture for: " + missing.join(", "));
fs.writeFileSync(path.join(root, "hosting/oyna/bot_photos.json"), JSON.stringify(out));
const dir = path.join(root, "hosting/oyna/img/avatars"); fs.mkdirSync(dir, { recursive: true });
for (const id of new Set(Object.values(out))) fs.copyFileSync(path.join(root, "app/src/main/res/drawable-nodpi", "avatar_gallery_" + id + ".webp"), path.join(dir, id + ".webp"));
console.log(Object.keys(out).length + " bots, " + new Set(Object.values(out)).size + " pictures");
