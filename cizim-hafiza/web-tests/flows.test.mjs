// Whole game flows in WebKit on iPhone sizes: the tutorial, a quick match to its result screen and back home, and the daily challenge.
// Each flow fails on a script error, a screen that never comes, or a wrong state (e.g. the answer box losing focus between words).
import { webkit, devices } from "playwright";
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url)), root = path.join(here, "..", "hosting"), shots = path.join(here, "shots");
fs.mkdirSync(shots, { recursive: true });
const TYPES = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".json": "application/json", ".webp": "image/webp", ".png": "image/png", ".ttf": "font/ttf", ".m4a": "audio/mp4", ".mp4": "video/mp4" };
const server = http.createServer((req, res) => {
  let p = decodeURIComponent(new URL(req.url, "http://x").pathname); if (p === "/" || p === "/oyna" || p === "/oyna/") p = "/oyna/index.html";
  const f = path.join(root, p); if (!f.startsWith(root) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { "content-type": TYPES[path.extname(f)] || "application/octet-stream" }); fs.createReadStream(f).pipe(res);
}).listen(0);
const base = `http://127.0.0.1:${server.address().port}/oyna/`, fake = fs.readFileSync(path.join(here, "fakefb_mod.js"), "utf8"), problems = [];
const veteran = { nick: "Test", xp: 300, gold: 500, tutorialDone: true, login: { streak: 1, last: 99999 } };

async function session(device, seed, name, fn) {
  const browser = await webkit.launch(), ctx = await browser.newContext({ ...devices[device], locale: "tr-TR" }), page = await ctx.newPage();
  const tag = `[${device}] ${name}`; let step = "start";
  page.on("pageerror", (e) => problems.push(`${tag} (${step}): script error: ${e.message}`));
  page.on("crash", () => problems.push(`${tag} (${step}): THE PAGE CRASHED`));
  await page.route(/gstatic\.com\/firebasejs/, (r) => r.fulfill({ contentType: "text/javascript", body: fake }));
  await page.addInitScript((s) => { if (s && !localStorage.getItem("player")) localStorage.setItem("player", JSON.stringify(s)); }, seed);
  const S = (s) => { step = s; };
  const cur = () => page.evaluate(() => window.__karalak.current());
  const until = async (id, ms = 12000) => { try { await page.waitForFunction((i) => window.__karalak.current() === i, id, { timeout: ms }); } catch { problems.push(`${tag} (${step}): the "${id}" screen never came (stuck on "${await cur().catch(() => "?")}")`); throw new Error("stuck"); } };
  const snap = (n) => page.screenshot({ path: path.join(shots, `flow_${device.replace(/\W+/g, "_")}_${name}_${n}.jpg`), type: "jpeg", quality: 70 }).catch(() => {});
  try { await page.goto(base); await page.waitForFunction(() => !document.getElementById("splash"), null, { timeout: 20000 }); await page.waitForTimeout(600); await fn({ page, S, cur, until, snap, tag }); }
  catch (e) { if (e.message !== "stuck") problems.push(`${tag} (${step}): ${e.message.split("\n")[0]}`); await snap("failed"); }
  await browser.close();
}
const scribble = async (page) => { const b = await page.locator("#pad").boundingBox(); if (!b) return; await page.mouse.move(b.x + 40, b.y + 60); await page.mouse.down(); await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2, { steps: 6 }); await page.mouse.move(b.x + 60, b.y + b.height - 50, { steps: 6 }); await page.mouse.up(); };

for (const device of ["iPhone SE", "iPhone 13"]) {
  await session(device, null, "tutorial", async ({ page, S, cur, until, snap, tag }) => {
    S("welcome"); await until("welcome"); await snap("1_welcome"); await page.tap("#w-go");
    S("drawing 3 words"); await until("draw");
    for (let i = 0; i < 3; i++) {
      await scribble(page); await page.tap("#d-next"); await page.waitForTimeout(400);
      if (i < 2) { S(`coach card after word ${i + 1}`); await page.waitForSelector("#tut-go", { state: "visible", timeout: 6000 }); await snap(`2_card${i + 1}`); await page.tap("#tut-go"); await page.waitForTimeout(400); }
    }
    S("guess card"); await page.waitForSelector("#tut-go", { state: "visible", timeout: 6000 }); await page.tap("#tut-go"); await until("guess");
    S("first joker lesson"); await page.waitForTimeout(500); await snap("3_joker_lesson"); await page.tap("#jt-first", { force: true }); await page.waitForTimeout(500);
    S("typing answers");
    for (let i = 0; i < 3; i++) {
      if (i === 1) { await page.waitForTimeout(500); await page.tap("#jt-count", { force: true }).catch(() => {}); await page.waitForTimeout(400); }
      await page.fill("#g-input", "x"); await page.tap("#g-ok", { force: true });
      const focus = await page.evaluate(() => document.activeElement && document.activeElement.id);
      if (focus !== "g-input" && i < 2) problems.push(`${tag} (answers): the answer box lost the focus after word ${i + 1} (${focus}): the keyboard would close`);
      await page.waitForTimeout(1500);
      if (i === 1) { S("free joker card"); await page.waitForSelector("#tut-go", { state: "visible", timeout: 6000 }); await page.tap("#tut-go"); }
    }
    S("finale"); await page.waitForSelector("#tut-go", { state: "visible", timeout: 8000 }); await snap("4_finale"); await page.tap("#tut-go"); await until("home");
    const j = await page.evaluate(() => JSON.parse(localStorage.getItem("player")).jokers); if (!j || j.FIRST_LETTER < 3) problems.push(`${tag}: the tutorial did not give the 3 jokers (${JSON.stringify(j)})`);
  });

  await session(device, veteran, "quickmatch", async ({ page, S, cur, until, snap, tag }) => {
    S("seeding"); await page.evaluate(async () => { const w = await (await fetch("words_tr.json")).json(); const ids = w.slice(0, 30).map((x) => x.id);
      window.__store["botTrainingIndex/trained"] = { wordIds: ids, complete: false };
      for (const x of w.slice(0, 30)) window.__store["botTrainedWords/" + x.id] = { word: x.text, wordId: x.id, strokesJson: JSON.stringify([[{ x: 5, y: 5 }, { x: 90, y: 100 }, { x: 20, y: 60 }]]) }; });
    S("search"); await page.tap("#tl-quick"); await until("search"); await snap("1_search"); await until("found", 20000); await snap("2_found"); await until("draw", 15000);
    S("drawing 10 words"); for (let i = 0; i < 10; i++) { await page.tap("#d-next"); await page.waitForTimeout(150); }
    S("guessing"); await until("guess", 12000);
    for (let i = 0; i < 10; i++) {
      if (i === 1) {
        await page.tap("#jt-skip"); await page.waitForTimeout(250);
        const chip = await page.evaluate(() => { const c = document.getElementById("g-skipchip"); return c && getComputedStyle(c).display !== "none" ? c.textContent : ""; });
        if (!/(sn kaydedildi|s saved)/.test(chip)) problems.push(`${tag} (guessing): skipping showed no "time saved" note (got "${chip}")`);
        await page.waitForTimeout(1100); // the note stays up for a moment, like in the app
      }
      await page.fill("#g-input", "abc"); await page.tap("#g-ok", { force: true });
      const focus = await page.evaluate(() => document.activeElement && document.activeElement.id);
      if (focus !== "g-input") problems.push(`${tag} (guessing): the answer box lost the focus after word ${i + 1} (${focus}): the keyboard would close`);
      await page.waitForTimeout(1400);
    }
    S("result"); const t0 = Date.now(); await until("res2", 15000); if (Date.now() - t0 > 6000) problems.push(`${tag}: the result took ${Date.now() - t0} ms to show`); await page.waitForTimeout(800); await snap("3_result");
    S("opponent drawings"); const chips = await page.$$(".rs-board .chip"); if (chips[1]) { await chips[1].tap(); await page.waitForTimeout(300); await snap("4_opponent"); const sheet = await page.$(".rs-sheets:not(.hid) .sheet:not(.empty)"); if (sheet) { await sheet.tap(); await page.waitForTimeout(700); if (!(await page.$("#pick:not(.hidden)"))) problems.push(`${tag}: tapping an opponent drawing did not open it`); await snap("5_preview"); await page.tap(".pv-hit").catch(() => {}); await page.evaluate(() => document.getElementById("pick").classList.add("hidden")); } }
    else problems.push(`${tag}: no opponent chip on the result`);
    S("claim"); await page.tap(".rs-art").catch(() => {}); await until("home", 6000); await page.waitForTimeout(600); await snap("6_home_after");
  });

  await session(device, veteran, "daily", async ({ page, S, until, snap }) => {
    S("daily"); await page.tap("#h-dplay", { force: true }); await until("draw", 8000); for (let i = 0; i < 5; i++) { await scribble(page); await page.tap("#d-next"); await page.waitForTimeout(150); }
    await until("guess", 12000); for (let i = 0; i < 5; i++) { await page.fill("#g-input", "q"); await page.tap("#g-ok", { force: true }); await page.waitForTimeout(1400); }
    await until("res2", 12000); await page.waitForTimeout(500); await snap("result");
  });
}
server.close();
if (problems.length) { console.error("\nFLOW PROBLEMS:\n" + problems.map((p) => " - " + p).join("\n")); process.exit(1); }
console.log("OK: tutorial, quick match and daily challenge ran through in WebKit on iPhone SE and 13.");
