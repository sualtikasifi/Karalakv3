// The web game in WebKit (Safari's engine) on iPhone-sized screens. Chromium on a desktop hides iPhone-only problems (the bottom
// toolbar, 100vh, the visual viewport, tap handling), so this runs the same screens in WebKit and fails on real errors:
//   - a script error on any screen
//   - a screen that scrolls sideways or whose game area falls outside the visible window
//   - the home screen's tiles or the Runes not answering a tap
// It also saves one screenshot per screen and device (web-tests/shots), which the workflow uploads.
// Run locally:  cd cizim-hafiza/web-tests && npm i && npx playwright install webkit && npm test
import { webkit, devices } from "playwright";
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url)), root = path.join(here, "..", "hosting"), shots = path.join(here, "shots");
fs.mkdirSync(shots, { recursive: true });
const TYPES = { ".html": "text/html", ".js": "text/javascript", ".mjs": "text/javascript", ".css": "text/css", ".json": "application/json", ".webp": "image/webp", ".png": "image/png", ".ttf": "font/ttf", ".m4a": "audio/mp4", ".mp4": "video/mp4" };
const server = http.createServer((req, res) => {
  let p = decodeURIComponent(new URL(req.url, "http://x").pathname);
  if (p === "/" || p === "/oyna" || p === "/oyna/") p = "/oyna/index.html";
  const f = path.join(root, p);
  if (!f.startsWith(root) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { "content-type": TYPES[path.extname(f)] || "application/octet-stream" }); fs.createReadStream(f).pipe(res);
}).listen(0);
const port = server.address().port, base = `http://127.0.0.1:${port}/oyna/`;
const fake = fs.readFileSync(path.join(here, "fakefb_mod.js"), "utf8");
const DEVICES = ["iPhone SE", "iPhone 13", "iPhone 15 Pro Max"];
const SCREENS = ["home", "store", "settings", "ach", "league", "runes", "friends", "account", "report", "onl", "offs", "duels", "welcome", "draw", "guess"];
const problems = [];
const seed = { nick: "Test", xp: 300, gold: 500, tutorialDone: true, login: { streak: 1, last: 99999 } };

for (const name of DEVICES) {
  const browser = await webkit.launch();
  const ctx = await browser.newContext({ ...devices[name], locale: "tr-TR" });
  const page = await ctx.newPage(); let current = "";
  page.on("pageerror", (e) => problems.push(`[${name}] ${current}: script error: ${e.message}`));
  await page.route(/gstatic\.com\/firebasejs/, (r) => r.fulfill({ contentType: "text/javascript", body: fake }));
  await page.addInitScript((s) => { localStorage.setItem("player", JSON.stringify(s)); }, seed);
  await page.goto(base); await page.waitForFunction(() => !document.getElementById("splash"), null, { timeout: 20000 }).catch(() => problems.push(`[${name}] splash never closed`));
  await page.waitForTimeout(800);
  for (const id of SCREENS) {
    current = id;
    await page.evaluate((i) => window.__karalak.show(i), id).catch((e) => problems.push(`[${name}] ${id}: cannot open: ${e.message.split("\n")[0]}`));
    await page.waitForTimeout(500);
    const m = await page.evaluate(() => { const st = document.getElementById("stage").getBoundingClientRect(), vv = window.visualViewport; return { sw: document.documentElement.scrollWidth, iw: innerWidth, l: st.left, r: st.right, w: vv ? vv.width : innerWidth }; });
    if (m.sw > m.iw + 1) problems.push(`[${name}] ${id}: the page scrolls sideways (${m.sw} > ${m.iw})`);
    if (m.l < -1 || m.r > m.w + 1) problems.push(`[${name}] ${id}: the game area sticks out of the window (${Math.round(m.l)}..${Math.round(m.r)} of ${Math.round(m.w)})`);
    await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_${id}.png`) });
  }
  // taps: a home tile and a rune must answer
  current = "tap"; await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(400);
  await page.tap("#tl-settings").catch((e) => problems.push(`[${name}] tap on Ayarlar failed: ${e.message.split("\n")[0]}`));
  await page.waitForTimeout(500);
  if ((await page.evaluate(() => window.__karalak.current())) !== "settings") problems.push(`[${name}] tapping Ayarlar did not open it`);
  await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(300);
  await page.tap("#h-runes").catch(() => {}); await page.waitForTimeout(1200);
  const rune = await page.$(".ru-n.aff"); if (rune) { await rune.tap().catch(() => {}); await page.waitForTimeout(500); if (!(await page.$(".ru-n.sel"))) problems.push(`[${name}] tapping a rune did not select it`); }
  await browser.close();
}
server.close();
if (problems.length) { console.error("\nPROBLEMS:\n" + problems.map((p) => " - " + p).join("\n")); process.exit(1); }
console.log(`OK: ${DEVICES.length} devices x ${SCREENS.length} screens, taps answered, no errors.`);
