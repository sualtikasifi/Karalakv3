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
const seed = { nick: "Test", xp: 300, gold: 500, tutorialDone: true, runesIntroSeen: true, login: { streak: 1, last: 99999 } };

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
    // a label wider than its own button is cut off (or spills): list them so a wrong-sized pill is seen in the log, not only by a player
    const clipped = await page.evaluate(() => [...document.querySelectorAll(".screen.on button, .screen.on .pill, .screen.on .tab")].filter((b) => b.offsetParent && b.firstElementChild && b.firstElementChild.tagName === "SPAN").map((b) => { const sp = b.firstElementChild, cs = getComputedStyle(b), room = b.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight); return { t: sp.textContent.trim().slice(0, 30), w: Math.round(sp.scrollWidth), room: Math.round(room) }; }).filter((x) => x.t && x.room > 0 && x.w > x.room + 3));
    if (clipped.length) console.log(`  CLIPPED [${name}] ${id}: ` + JSON.stringify(clipped));
    // the lettered labels that fit their box by shrinking (fitw): the letters plus their outline must stay inside it (an outline on the edge reads as text spilling over)
    const tight = await page.evaluate(() => [...document.querySelectorAll(".screen.on .fitw")].filter((e) => e.offsetParent && e.clientWidth > 4).map((e) => { const r = document.createRange(); r.selectNodeContents(e); const w = r.getBoundingClientRect().width + (parseFloat(getComputedStyle(e).webkitTextStrokeWidth) || 0); return { t: e.textContent.trim().slice(0, 30), w: Math.round(w), box: Math.round(e.getBoundingClientRect().width) }; }).filter((x) => x.t && x.w > x.box + 1));
    if (tight.length) console.log(`  TIGHT [${name}] ${id}: ` + JSON.stringify(tight));
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
  // two painted windows taken from the app: the chest guide, and the Runes' first-visit window (it opens only until it has been read)
  current = "chest_info";
  try {
    await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(400);
    await page.evaluate(() => document.getElementById("ch-info").click()); await page.waitForTimeout(900);
    if (!(await page.$(".pc-ov.ci2"))) problems.push(`[${name}] the chest guide did not open`);
    else {
      await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_chest_info.jpg`), type: "jpeg", quality: 72 });
      await page.evaluate(() => document.querySelector(".pc-ov.ci2 .pc-btn").click()); await page.waitForTimeout(300);
      if (await page.$(".pc-ov.ci2")) problems.push(`[${name}] the chest guide's button did not close it`);
    }
  } catch (e) { problems.push(`[${name}] chest guide: ${e.message.split("\n")[0]}`); }
  current = "rune_intro";
  try {
    await page.evaluate(() => { window.__karalak.P.runesIntroSeen = false; window.__karalak.show("runes"); }); await page.waitForTimeout(1200);
    if (!(await page.$(".pc-ov.pf"))) problems.push(`[${name}] the Runes' first-visit window did not open`);
    else {
      await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_rune_intro.jpg`), type: "jpeg", quality: 72 });
      await page.evaluate(() => document.querySelector(".pc-ov.pf .pc-btn").click()); await page.waitForTimeout(300);
      if (await page.$(".pc-ov.pf") || !(await page.evaluate(() => window.__karalak.P.runesIntroSeen))) problems.push(`[${name}] the Runes' window did not close and stay read`);
    }
  } catch (e) { problems.push(`[${name}] rune intro: ${e.message.split("\n")[0]}`); }
  // the "how do you want to play?" window over a friend (opened straight from its module: the test has no friends)
  current = "play_choice";
  try {
    await page.evaluate(() => window.__karalak.show("friends")); await page.waitForTimeout(500);
    await page.evaluate(async () => { const m = await import("./ui.js"); window.__picked = []; m.playChoiceDialog(document.getElementById("stage"), { onRoom: () => window.__picked.push("room"), onDuel: () => window.__picked.push("duel") }); });
    await page.waitForTimeout(900);
    if (!(await page.$(".pc-ov.pcw"))) problems.push(`[${name}] the play choice window did not open`);
    else {
      await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_play_choice.jpg`), type: "jpeg", quality: 72 });
      const bars = await page.$$(".pc-ov.pcw .pc-btn");
      if (bars.length !== 2) problems.push(`[${name}] the play choice window has ${bars.length} buttons, not 2`);
      else {
        await bars[1].tap(); await page.waitForTimeout(300);
        if ((await page.evaluate(() => window.__picked.join())) !== "duel" || (await page.$(".pc-ov.pcw"))) problems.push(`[${name}] choosing a duel did not answer and close the window`);
      }
    }
  } catch (e) { problems.push(`[${name}] play choice: ${e.message.split("\n")[0]}`); }
  // the penalty window (taken from the app) and the lockout scene, both opened straight from their module: the test account has no penalties
  current = "penalty";
  try {
    await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(400);
    await page.evaluate(async () => { const m = await import("./penalty.js"); m.penaltyDialog(window.__karalak, [{ id: "t", xpRevoked: 120, goldRevoked: 0, strike: 2, lockedUntil: Date.now() + 2 * 3600 * 1000 + 5000, createdAt: Date.now(), breaksDailyStreak: false }]); });
    await page.waitForTimeout(900);
    if (!(await page.$(".pc-ov.pen"))) problems.push(`[${name}] the penalty window did not open`);
    else {
      await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_penalty.jpg`), type: "jpeg", quality: 72 });
      await page.evaluate(() => document.querySelector(".pc-ov.pen .pen-b").click()); await page.waitForTimeout(300);
      if (await page.$(".pc-ov.pen")) problems.push(`[${name}] the penalty window's button did not close it`);
    }
    await page.evaluate(async () => { const m = await import("./penalty.js"); window.__freed = false; m.lockScene(window.__karalak, Date.now() + 26 * 3600 * 1000 + 3723000, () => { window.__freed = true; }); });
    await page.waitForTimeout(900);
    if (!(await page.$(".lk-ov"))) problems.push(`[${name}] the lockout scene did not open`);
    else {
      await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_locked.jpg`), type: "jpeg", quality: 72 });
      const txt = await page.evaluate(() => document.querySelector(".lk-clock").textContent);
      if (!/\d/.test(txt) || !/:/.test(txt)) problems.push(`[${name}] the lockout countdown shows "${txt}"`);
      await page.evaluate(() => document.querySelector(".lk-back").click()); await page.waitForTimeout(200);
      if (await page.$(".lk-ov")) problems.push(`[${name}] the lockout scene's back button did not close it`);
    }
  } catch (e) { problems.push(`[${name}] penalty/lock: ${e.message.split("\n")[0]}`); }
  // last month's league prize window (taken from the app), opened straight from its module
  current = "league_win";
  try {
    await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(400);
    await page.evaluate(async () => { const m = await import("./league.js"); m.prizeDialog(window.__karalak, "PEN:LEAGUE_AURORA", 2); }); await page.waitForTimeout(900);
    if (!(await page.$(".pc-ov.lw"))) problems.push(`[${name}] the league prize window did not open`);
    else {
      await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_league_win.jpg`), type: "jpeg", quality: 72 });
      await page.evaluate(() => document.querySelector(".pc-ov.lw .lw-btn").click()); await page.waitForTimeout(300);
      if (await page.$(".pc-ov.lw")) problems.push(`[${name}] the league prize window's button did not close it`);
    }
  } catch (e) { problems.push(`[${name}] league win: ${e.message.split("\n")[0]}`); }
  // "Kupon kodunu nasıl kazanırım?" (the painted window in Turkish, as in the app)
  current = "coupon_how";
  try {
    await page.evaluate(() => window.__karalak.show("settings")); await page.waitForTimeout(500);
    await page.evaluate(() => document.getElementById("se-how").click()); await page.waitForTimeout(900);
    if (!(await page.$(".pc-ov.cph"))) problems.push(`[${name}] the coupon window did not open`);
    else {
      await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_coupon_how.jpg`), type: "jpeg", quality: 72 });
      await page.evaluate(() => document.querySelector(".pc-ov.cph .pc-btn").click()); await page.waitForTimeout(300);
      if (await page.$(".pc-ov.cph")) problems.push(`[${name}] the coupon window's close button did not close it`);
    }
  } catch (e) { problems.push(`[${name}] coupon how: ${e.message.split("\n")[0]}`); }
  // the store's three tabs: each must sit inside the row (not spill past the screen) and show its label (a percentage padding once made them twice as wide, with no room for the words)
  current = "store_tabs";
  try {
    await page.evaluate(() => window.__karalak.show("store")); await page.waitForTimeout(700);
    const t = await page.evaluate(() => { const row = document.getElementById("st-tabs").getBoundingClientRect(), vw = innerWidth; return { row: [row.left, row.right], vw, tabs: [...document.querySelectorAll("#st-tabs .tab")].map((b) => { const r = b.getBoundingClientRect(), s = b.firstElementChild.getBoundingClientRect(); return { l: r.left, r: r.right, label: b.textContent.trim(), lw: s.width, fs: parseFloat(getComputedStyle(b).fontSize) }; }) }; });
    console.log(`  ${name} store tabs: ` + JSON.stringify(t));
    if (t.tabs.length !== 3) problems.push(`[${name}] the store shows ${t.tabs.length} tabs, not 3`);
    for (const b of t.tabs) {
      if (b.r > t.vw + 8 || b.l < -8) problems.push(`[${name}] store tab "${b.label}" sticks out of the screen (${Math.round(b.l)}..${Math.round(b.r)} of ${t.vw})`);
      if (b.r - b.l > (t.row[1] - t.row[0]) * 0.4) problems.push(`[${name}] store tab "${b.label}" is wider than a third of the row (${Math.round(b.r - b.l)} of ${Math.round(t.row[1] - t.row[0])})`);
      if (!b.label || b.lw < 20 || b.fs < 7) problems.push(`[${name}] store tab "${b.label}" has no room for its label (width ${Math.round(b.lw)}, font ${b.fs})`);
    }
    await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_store_tabs.jpg`), type: "jpeg", quality: 72 });
  } catch (e) { problems.push(`[${name}] store tabs: ${e.message.split("\n")[0]}`); }
  // the friend-streak windows (a prize, a streak that ends today), opened straight from their module
  for (const [which, prompt] of [["reward", { k: "reward", pair: "a_b", run: 1, name: "Ece", milestone: 7, reward: { gold: 100 } }], ["risk", { k: "risk", items: [{ pair: "a_b", uid: "b", name: "Ece", count: 4 }] }]]) {
    current = "fstreak_" + which;
    try {
      await page.evaluate(() => window.__karalak.show("home")); await page.waitForTimeout(400);
      await page.evaluate(async (p) => { const m = await import("./fstreak.js"); m.showPrompt(window.__karalak, p, 1); }, prompt); await page.waitForTimeout(700);
      if (!(await page.$(".aw-ov.fs-win"))) problems.push(`[${name}] the ${which} streak window did not open`);
      else {
        await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_fstreak_${which}.jpg`), type: "jpeg", quality: 72 });
        await page.evaluate(() => document.querySelector(".aw-ov.fs-win .prim").click()); await page.waitForTimeout(300);
        if (await page.$(".aw-ov.fs-win")) problems.push(`[${name}] the ${which} streak window's button did not close it`);
      }
    } catch (e) { problems.push(`[${name}] streak ${which}: ${e.message.split("\n")[0]}`); }
  }
  // Hızlı Eşleş's two message states (pool empty / opponent not fetched), with "Tekrar Ara"
  for (const which of ["empty", "failed"]) {
    current = "qmsg_" + which;
    try {
      await page.evaluate((w) => window.__karalak.quickMessage(w), which); await page.waitForTimeout(700);
      const t = await page.evaluate(() => document.getElementById("qm-t").textContent.trim());
      if (!t || (await page.evaluate(() => window.__karalak.current())) !== "qmsg") problems.push(`[${name}] the ${which} message did not open (title "${t}")`);
      else await page.screenshot({ path: path.join(shots, `${name.replace(/\W+/g, "_")}_qm_${which}.jpg`), type: "jpeg", quality: 72 });
    } catch (e) { problems.push(`[${name}] qm ${which}: ${e.message.split("\n")[0]}`); }
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
