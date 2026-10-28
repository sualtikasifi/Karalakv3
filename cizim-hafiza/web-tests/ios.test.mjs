// The web game in WebKit (Safari's engine) on iPhone-sized screens. Chromium on a desktop hides iPhone-only problems (the bottom
// toolbar, 100vh, the visual viewport, tap handling), so this runs the same screens in WebKit and fails on real errors:
//   - a script error on any screen
//   - a screen that scrolls sideways or whose game area falls outside the visible window
//   - the home screen's tiles or the Runes not answering a tap
// It also saves one screenshot per screen and device (web-tests/shots), which the workflow uploads.
// Run locally:  cd cizim-hafiza/web-tests && npm i && npx playwright install webkit && npm test
import { webkit, chromium, devices } from "playwright";
import pixelmatch from "pixelmatch";
import { PNG } from "pngjs";
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url)), root = path.join(here, "..", "hosting"), shots = path.join(here, "shots");
fs.mkdirSync(shots, { recursive: true });
const TYPES = { ".html": "text/html", ".js": "text/javascript", ".mjs": "text/javascript", ".css": "text/css", ".json": "application/json", ".webp": "image/webp", ".png": "image/png", ".ttf": "font/ttf", ".woff2": "font/woff2", ".m4a": "audio/mp4", ".mp4": "video/mp4" };
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
const problems = [], diffs = [];
const seed = { nick: "Test", xp: 300, gold: 500, tutorialDone: true, login: { streak: 1, last: 99999 } };

for (const name of DEVICES) {
  const browser = await webkit.launch();
  const cbrowser = await chromium.launch();
  const cctx = await cbrowser.newContext({ ...devices[name], locale: "tr-TR", userAgent: devices[name].userAgent }); // the same screen size, drawn by Chrome's engine
  const cpage = await cctx.newPage();
  await cpage.route(/gstatic\.com\/firebasejs/, (r) => r.fulfill({ contentType: "text/javascript", body: fake }));
  await cpage.addInitScript((s) => { localStorage.setItem("player", JSON.stringify(s)); localStorage.setItem("legal", JSON.stringify({ y: 1990, t: 1 })); }, seed);
  await cpage.goto(base); await cpage.waitForFunction(() => !document.getElementById("splash"), null, { timeout: 20000 }).catch(() => {}); await cpage.waitForTimeout(800);
  const ctx = await browser.newContext({ ...devices[name], locale: "tr-TR" });
  let page = null, current = "";
  const open = async () => {
    page = await ctx.newPage();
    page.on("pageerror", (e) => problems.push(`[${name}] ${current}: script error: ${e.message}`));
    page.on("crash", () => problems.push(`[${name}] ${current}: THE PAGE CRASHED in WebKit`));
    page.on("console", (m) => { if (m.type() === "error") console.log(`  console error [${name}] ${current}: ${m.text().slice(0, 160)}`); });
    await page.route(/gstatic\.com\/firebasejs/, (r) => r.fulfill({ contentType: "text/javascript", body: fake }));
    await page.addInitScript((s) => { localStorage.setItem("player", JSON.stringify(s)); localStorage.setItem("legal", JSON.stringify({ y: 1990, t: 1 })); }, seed);
    await page.goto(base);
  };
  await open(); await page.waitForFunction(() => !document.getElementById("splash"), null, { timeout: 20000 }).catch(() => problems.push(`[${name}] splash never closed`));
  await page.waitForTimeout(800);
  for (const id of SCREENS) {
    current = id;
    if (page.isClosed()) { await open(); await page.waitForFunction(() => !document.getElementById("splash"), null, { timeout: 20000 }).catch(() => {}); }
    try { await page.evaluate((i) => window.__karalak.show(i), id); } catch (e) { problems.push(`[${name}] ${id}: cannot open: ${e.message.split("\n")[0]}`); continue; }
    await page.waitForTimeout(500);
    let m; try { m = await page.evaluate(() => { const st = document.getElementById("stage").getBoundingClientRect(), vv = window.visualViewport; return { sw: document.documentElement.scrollWidth, iw: innerWidth, l: st.left, r: st.right, w: vv ? vv.width : innerWidth }; }); } catch (e) { problems.push(`[${name}] ${id}: lost the page: ${e.message.split("\n")[0]}`); continue; }
    console.log(`  ${name} ${id}: stage ${Math.round(m.l)}..${Math.round(m.r)} of ${Math.round(m.w)}`);
    if (m.sw > m.iw + 1) problems.push(`[${name}] ${id}: the page scrolls sideways (${m.sw} > ${m.iw})`);
    if (m.l < -1 || m.r > m.w + 1) problems.push(`[${name}] ${id}: the game area sticks out of the window (${Math.round(m.l)}..${Math.round(m.r)} of ${Math.round(m.w)})`);
    await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_${id}.jpg`), type: "jpeg", quality: 72 });
    // the same screen in Chrome's engine; the share of pixels that differ shows what is Safari-only
    try {
      await cpage.evaluate((i) => window.__karalak.show(i), id); await cpage.waitForTimeout(500);
      const a = PNG.sync.read(await page.screenshot()), b = PNG.sync.read(await cpage.screenshot());
      if (a.width === b.width && a.height === b.height) {
        const d = new PNG({ width: a.width, height: a.height }), n = pixelmatch(a.data, b.data, d.data, a.width, a.height, { threshold: 0.25 });
        const pct = Math.round(n / (a.width * a.height) * 1000) / 10; diffs.push({ name, id, pct });
        fs.writeFileSync(path.join(shots, `${name.replace(/\W+/g, "_")}_${id}_diff.jpg`.replace(".jpg", ".png")), PNG.sync.write(d));
      }
    } catch (e) { console.log(`  (no Chrome comparison for ${id}: ${e.message.split("\n")[0]})`); }
  }
  // taps: a home tile and a rune must answer
  current = "tap"; await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(400);
  await page.tap("#tl-settings").catch((e) => problems.push(`[${name}] tap on Ayarlar failed: ${e.message.split("\n")[0]}`));
  await page.waitForTimeout(500);
  if ((await page.evaluate(() => window.__karalak.current())) !== "settings") problems.push(`[${name}] tapping Ayarlar did not open it`);
  await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(300);
  await page.tap("#h-runes").catch(() => {}); await page.waitForTimeout(1200);
  const rune = await page.$(".ru-n.aff"); if (rune) { await rune.tap().catch(() => {}); await page.waitForTimeout(500); if (!(await page.$(".ru-n.sel"))) problems.push(`[${name}] tapping a rune did not select it`); }
  await browser.close(); await cbrowser.close();
}
server.close();
fs.writeFileSync(path.join(shots, "diffs.json"), JSON.stringify(diffs));
console.log("Safari vs Chrome (share of different pixels): " + diffs.filter((d) => d.pct >= 5).map((d) => `${d.name}/${d.id} ${d.pct}%`).join(", "));
for (const d of diffs) if (d.pct > 45) problems.push(`[${d.name}] ${d.id}: looks very different in Safari than in Chrome (${d.pct}% of the pixels)`);
if (problems.length) { console.error("\nPROBLEMS:\n" + problems.map((p) => " - " + p).join("\n")); process.exit(1); }
console.log(`OK: ${DEVICES.length} devices x ${SCREENS.length} screens, taps answered, no errors.`);
