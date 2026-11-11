// "Bil Bakalım" on the web (hosting/bil): an iPhone watches a friend's drawing, guesses it, draws the same word and gets a link of
// its own; an Android phone is NOT let play in the browser and is handed to the app. Firebase is the in-memory fake the other
// web tests use, seeded with a link whose hash is computed here the way the app computes it (GuessCodec.kt).
import { webkit, chromium, devices } from "playwright";
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import { normalize } from "../hosting/oyna/matcher.js";

const here = path.dirname(fileURLToPath(import.meta.url)), root = path.join(here, "..", "hosting");
const TYPES = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".json": "application/json", ".webp": "image/webp", ".png": "image/png", ".woff2": "font/woff2" };
const server = http.createServer((req, res) => {
  let p = decodeURIComponent(new URL(req.url, "http://x").pathname);
  if (/^\/b\//.test(p)) p = "/bil/index.html"; // firebase.json's rewrite
  const f = path.join(root, p);
  if (!f.startsWith(root) || !fs.existsSync(f) || fs.statSync(f).isDirectory()) { res.writeHead(404); return res.end(); }
  res.writeHead(200, { "content-type": TYPES[path.extname(f)] || "application/octet-stream" }); fs.createReadStream(f).pipe(res);
}).listen(0);
const origin = `http://127.0.0.1:${server.address().port}`, fake = fs.readFileSync(path.join(here, "fakefb_mod.js"), "utf8"), problems = [];
const fail = (m) => problems.push(m);

const words = JSON.parse(fs.readFileSync(path.join(root, "oyna", "words_tr.json"), "utf8")).map((w) => w.text);
const WORD = words.find((w) => w.includes(" ") && w.length < 14) || words[5], SALT = "saltsalt0123", ID = "AbCdEf1234";
const hash = crypto.createHash("sha256").update(`${SALT}:${normalize(WORD)}`).digest("hex");
const strokes = JSON.stringify({ v: 1, s: [[100, 100, 900, 100, 900, 900], [100, 900, 500, 500]] });
const link = { ownerUid: "owner", ownerName: "Ayşe", lang: "tr", salt: SALT, hash, pattern: normalize(WORD).split(" ").map((w) => w.length).join(" "), first: WORD[0].toLocaleUpperCase("tr"), strokesJson: strokes, seconds: 7, createdAt: Date.now(), expiresAt: Date.now() + 864e5 };
const seed = { [`guessLinks/${ID}`]: link };

// WebKit is what an iPhone runs (the other web tests use it); where it is not installed Chromium stands in for it.
const iphone = await (async () => { try { await (await webkit.launch()).close(); return webkit; } catch { return chromium; } })();
// BROWSER_PATH points Chromium at an installed build when the pinned Playwright's own download is not there.
const launch = (type) => type.launch(type === chromium && process.env.BROWSER_PATH ? { executablePath: process.env.BROWSER_PATH, args: ["--no-sandbox"] } : {});

async function open(browserType, ctxOpts, url) {
  const browser = await launch(browserType), ctx = await browser.newContext(ctxOpts), page = await ctx.newPage();
  page.on("pageerror", (e) => fail(`script error: ${e.message}`));
  await page.route(/gstatic\.com\/firebasejs/, (r) => r.fulfill({ contentType: "text/javascript", body: fake }));
  await page.addInitScript((s) => { window.__store = s; }, seed);
  await page.goto(origin + url);
  return { browser, page };
}
const store = (page) => page.evaluate(() => window.__store);

// ---- an iPhone plays the whole thing
{
  const { browser, page } = await open(iphone, { ...devices["iPhone 13"], locale: "tr-TR" }, `/b/${ID}`);
  try {
    await page.waitForSelector(".paper", { timeout: 8000 });
    await page.waitForFunction(() => { const i = document.querySelector('input[type=text]:not([maxlength])'); return i && !i.disabled; }, null, { timeout: 12000 });
    if (!(await page.textContent(".sub")).includes("Ayşe")) fail("the sender's name is not shown");
    // a wrong guess costs a try and shows the shape of the word
    await page.fill('input[type=text]:not([maxlength])', "yanlis cevap");
    await page.click("button.primary");
    await page.waitForFunction(() => document.querySelector(".hint").textContent.includes("_"), null, { timeout: 6000 });
    // the right one (with the accents dropped) solves it
    await page.waitForFunction(() => !document.querySelector('input[type=text]:not([maxlength])').disabled, null, { timeout: 6000 });
    await page.fill('input[type=text]:not([maxlength])', normalize(WORD));
    await page.click("button.primary");
    await page.waitForSelector(".big", { timeout: 6000 });
    if (!(await page.textContent(".big")).includes("Bildin")) fail("the right guess was not accepted");
    const s1 = await store(page);
    const entry = s1[`users/owner/guessInbox/${ID}_me`];
    if (!entry || entry.solved !== true || entry.attempts !== 2 || entry.linkId !== ID) fail(`the owner's inbox entry is wrong: ${JSON.stringify(entry)}`);
    // sıra sende: the same word, drawn on the page
    await page.click("button.primary");
    await page.waitForSelector(".timer", { timeout: 4000 });
    if (!(await page.textContent(".sub")).toLocaleLowerCase("tr").includes(WORD.toLocaleLowerCase("tr"))) fail("the word to draw is not shown");
    const b = await page.locator(".paper canvas").boundingBox();
    await page.mouse.move(b.x + 40, b.y + 60); await page.mouse.down(); await page.mouse.move(b.x + 200, b.y + 120, { steps: 8 }); await page.mouse.move(b.x + 120, b.y + 260, { steps: 8 }); await page.mouse.up();
    await page.click("button.primary");
    await page.waitForSelector(".box", { timeout: 6000 });
    const mine = Object.entries(await store(page)).filter(([k, v]) => k.startsWith("guessLinks/") && k !== `guessLinks/${ID}`);
    if (mine.length !== 1) fail(`expected one new link, found ${mine.length}`);
    else {
      const d = mine[0][1];
      if (d.ownerUid !== "me" || d.lang !== "tr" || d.hash?.length !== 64 || "word" in d) fail(`the new link is malformed: ${JSON.stringify(d).slice(0, 200)}`);
      if (d.hash !== crypto.createHash("sha256").update(`${d.salt}:${normalize(WORD)}`).digest("hex")) fail("the new link's hash does not match the word");
      if (!/^\/?b\//.test(new URL(await page.textContent(".box")).pathname.slice(1)) && !(await page.textContent(".box")).includes("/b/")) fail("the link shown is not a /b/ link");
    }
  } catch (e) { fail(`iPhone flow: ${e.message.split("\n")[0]}`); }
  await browser.close();
}

// ---- an Android phone is handed to the app and never gets the drawing
{
  const ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";
  const { browser, page } = await open(chromium, { userAgent: ua, viewport: { width: 400, height: 800 }, locale: "tr-TR" }, `/b/${ID}`);
  try {
    await page.waitForSelector("a.btn", { timeout: 6000 });
    const hrefs = await page.$$eval("a.btn", (a) => a.map((x) => x.getAttribute("href")));
    if (!hrefs.some((h) => h.startsWith(`intent://guess/${ID}#Intent;scheme=karalak;package=com.sualtikasifi.cizimhafiza`))) fail(`Android: no app link in ${JSON.stringify(hrefs)}`);
    if (!hrefs.some((h) => h.includes("play.google.com") && h.includes("karalak_guess%3D" + ID))) fail("Android: the Play Store link does not carry the link as referrer");
    if (await page.$(".paper, canvas")) fail("Android: the drawing is playable in the browser");
    if (await page.$('input[type=text]')) fail("Android: a guess box is offered in the browser");
  } catch (e) { fail(`Android flow: ${e.message.split("\n")[0]}`); }
  await browser.close();
}

// ---- a link that is not there
{
  const { browser, page } = await open(iphone, { ...devices["iPhone 13"], locale: "en-US" }, `/b/Zzzzzzzzzz`);
  try {
    await page.waitForFunction(() => document.querySelector(".sub")?.textContent.includes("no longer works"), null, { timeout: 6000 });
    if (await page.$(".paper")) fail("a missing link shows a drawing");
  } catch (e) { fail(`missing link: ${e.message.split("\n")[0]}`); }
  await browser.close();
}

server.close();
if (problems.length) { console.log("FAIL\n" + problems.map((p) => " - " + p).join("\n")); process.exit(1); }
console.log("bil: all passed");
process.exit(0);
