import { isCorrect } from "./matcher.js";
import * as net from "./firebase.js";

const WORD_COUNT = 10, POINTS = 5, SPEED_BONUS = 2, SPEED_MS = 3000;
const DRAW_SECONDS = { EASY: 5, MEDIUM: 7, HARD: 10 };
const GUESS_BASE = { EASY: 8, MEDIUM: 10, HARD: 12 };
const guessSeconds = (w) => Math.min(13, (GUESS_BASE[w.difficulty] ?? 10) + Math.max(0, [...w.text].filter((c) => /\p{L}/u.test(c)).length - 10));
const ART_W = 841, ART_H = 1870, GUESS_BOTTOM = 1398;

const T = {
  tr: { tag: "Çiz, ezberle,\ntahmin et!", start: "OYNA", note: "Android ve tarayıcı oyuncularıyla eşleş", next: "Bitir", ok: "Gönder",
        guess: "Tahmin et", result: "Sonuç", searching: "Rakip aranıyor…", searchSub: "Birisi senin gibi çizerken bulunuyor", again: "TEKRAR OYNA", noOpp: "Şu an rakip bulunamadı",
        you: "Sen", win: "Kazandın!", lose: "Kaybettin", draw: "Berabere", leave: "Oyundan çıkılsın mı?", yes: "Çık", no: "Kal",
        mine: "Çizimlerin", theirs: "Rakibin çizimleri", pts: "puan", file: "words_tr.json", ph: "Cevabın…" },
  en: { tag: "Draw, memorize,\nguess!", start: "PLAY", note: "Match with Android and browser players", next: "Done", ok: "Send",
        guess: "Guess", result: "Result", searching: "Finding an opponent…", searchSub: "Looking for someone who draws like you", again: "PLAY AGAIN", noOpp: "No opponent found right now",
        you: "You", win: "You win!", lose: "You lose", draw: "Draw", leave: "Leave the game?", yes: "Leave", no: "Stay",
        mine: "Your drawings", theirs: "Opponent's drawings", pts: "pts", file: "words_en.json", ph: "Your answer…" },
};
const $ = (id) => document.getElementById(id);
let lang = localStorage.getItem("lang") || ((navigator.language || "tr").startsWith("tr") ? "tr" : "en");
let words = [], round = [], idx = 0, current = "menu", u = 1;
let dTimer = null, gTimer = null, tool = "pen", score = 0, shownAt = 0;
const t = () => T[lang];

// ---------- layout (art units: the paintings are 841 wide) ----------
const stage = $("stage");
function place(el, x, y, w, h) {
  const s = el.style;
  s.left = x * u + "px"; s.top = y * u + "px";
  if (w != null) s.width = w * u + "px";
  if (h != null) s.height = h * u + "px";
}
function viewport() { const v = window.visualViewport; return { w: v ? v.width : innerWidth, h: v ? v.height : innerHeight }; }
function layout() {
  const { w: vw, h: vh } = viewport();
  const s = stage.style;
  let W, H;
  if (current === "guess" || current === "result") { W = Math.min(vw, 560); H = vh; }
  else { W = Math.min(vw, vh * ART_W / ART_H); H = W * ART_H / ART_W; }
  u = W / ART_W;
  s.width = W + "px"; s.height = H + "px"; s.left = (vw - W) / 2 + "px"; s.top = (vh - H) / 2 + "px";
  stage.style.setProperty("--u", u + "px");
  if (current === "menu") {
    place($("m-tag"), 130, 800, 580, 170);
    place($("m-lang"), 130, 1010, 580, 76);
    place($("start"), 125, 1176, 591, 134);
    place($("m-note"), 200, 1337, 441, 78);
  } else if (current === "draw") {
    place($("d-back"), 28, 82, 99, 96);
    place($("d-prog"), 152, 88, 296, 82);
    place($("d-ring"), 712, 82, 98, 98);
    place($("d-word"), 125, 232, 445, 112);
    place($("d-line"), 235, 343, 250, 8);
    place($("d-canvas"), 36, 438, 770, 948);
    [["t-pen", 48], ["t-erase", 165], ["t-undo", 290], ["t-clear", 418]].forEach(([id, x]) => place($(id), x, 1433, 98, 98));
    place($("d-next"), 540, 1436, 258, 92);
    fitCanvas($("pad"));
    paintLive();
  } else if (current === "guess") {
    const topFrom = 120, topEnd = 400, botStart = 985;
    const botH = (GUESS_BOTTOM - botStart) * u, topH = (topEnd - topFrom) * u;
    const midH = Math.max(0, H - topH - botH);
    const sl = (el, top, h, srcY, srcH) => {
      el.style.top = top + "px"; el.style.left = "0"; el.style.width = W + "px"; el.style.height = h + "px";
      el.style.backgroundSize = `${W}px ${srcH ? h / srcH * ART_H : u * ART_H}px`;
      el.style.backgroundPosition = `0 ${-srcY * (srcH ? h / srcH : u)}px`;
    };
    sl($("g-bgtop"), 0, topH, topFrom, topEnd - topFrom);
    sl($("g-bgmid"), topH, midH + 1, topEnd, botStart - topEnd);
    sl($("g-bgbot"), topH + midH, botH, botStart, GUESS_BOTTOM - botStart);
    const fb = (v) => H - (GUESS_BOTTOM - v) * u; // art y measured from the bottom edge
    const ty = (v) => (v - topFrom) * u;
    const P = (el, x, y, w, h) => { const s2 = el.style; s2.left = x * u + "px"; s2.top = y + "px"; s2.width = w * u + "px"; s2.height = h + "px"; };
    P($("g-back"), 22, ty(155), 84, 78 * u);
    P($("g-prog"), 118, ty(158), 114, 74 * u);
    P($("g-xp"), 334, ty(158), 188, 72 * u);
    P($("g-ring"), 700, ty(150), 100, 100 * u);
    P($("g-canvas"), 66, ty(310), 724, Math.max(60, fb(1112) - ty(310)));
    P($("g-form"), 0, fb(1265), 841, 105 * u);
    const inp = $("g-input"); inp.style.left = 58 * u + "px"; inp.style.top = 6 * u + "px"; inp.style.width = 484 * u + "px"; inp.style.height = 92 * u + "px";
    const ok = $("g-ok"); ok.style.left = 572 * u + "px"; ok.style.top = 0; ok.style.width = 225 * u + "px"; ok.style.height = 105 * u + "px";
    fitCanvas($("view"));
    paintGuess();
  } else if (current === "search") {
    place($("s-sign"), 60, 190, 720, 365);
    place($("s-text"), 150, 318, 540, 150);
    place($("s-paper"), 215, 1110, 410, 305);
    place($("s-sub"), 70, 1480, 700, 130);
    startDoodles();
  }
}
addEventListener("resize", layout);
window.visualViewport?.addEventListener("resize", layout);

function show(id) {
  current = id;
  for (const s of document.querySelectorAll(".screen")) s.classList.toggle("on", s.id === id);
  layout();
}

const DOODLES = [
  "M50 8 L61 38 L93 38 L67 57 L77 90 L50 70 L23 90 L33 57 L7 38 L39 38 Z",
  "M50 88 C10 58 12 22 32 18 C42 16 50 26 50 30 C50 26 58 16 68 18 C88 22 90 58 50 88 Z",
  "M12 50 L50 14 L88 50 M22 44 L22 86 L78 86 L78 44 M42 86 L42 62 L60 62 L60 86",
  "M50 28 A22 22 0 1 1 49.9 28 M50 4 V16 M50 84 V96 M4 50 H16 M84 50 H96 M17 17 L26 26 M74 74 L83 83 M83 17 L74 26 M26 74 L17 83",
];
let doodleTimer = null;
function startDoodles() {
  if (doodleTimer) return;
  let i = 0; const path = $("s-doodle");
  const set = () => { path.setAttribute("d", DOODLES[i++ % DOODLES.length]); path.style.animation = "none"; void path.getBoundingClientRect(); path.style.animation = ""; };
  set(); doodleTimer = setInterval(() => { if (current !== "search") { clearInterval(doodleTimer); doodleTimer = null; return; } set(); }, 2200);
}

// ---------- text ----------
function applyLang() {
  const x = t();
  document.documentElement.lang = lang;
  $("m-tag").textContent = x.tag; $("m-tag").style.whiteSpace = "pre-line";
  $("start-t").textContent = x.start; $("m-note").textContent = x.note;
  $("d-next-t").textContent = x.next; $("g-ok-t").textContent = x.ok;
  $("s-text").textContent = x.searching; $("s-sub").textContent = x.searchSub; $("again-t").textContent = x.again;
  $("dlg-text").textContent = x.leave; $("dlg-yes").textContent = x.yes; $("dlg-no").textContent = x.no;
  $("g-input").placeholder = x.ph;
  $("lang-tr").classList.toggle("on", lang === "tr"); $("lang-en").classList.toggle("on", lang === "en");
  try { localStorage.setItem("lang", lang); } catch {}
}
$("lang-tr").onclick = () => { lang = "tr"; applyLang(); };
$("lang-en").onclick = () => { lang = "en"; applyLang(); };

// ---------- drawing ----------
function fitCanvas(c) {
  const r = c.getBoundingClientRect(), d = window.devicePixelRatio || 1;
  c.width = Math.max(1, Math.round(r.width * d)); c.height = Math.max(1, Math.round(r.height * d));
}
// Strokes are lists of {x,y} in canvas pixels (the shape Android stores); every viewer fits them to the box.
function paint(canvas, strokes, live = false) {
  const ctx = canvas.getContext("2d"), w = canvas.width, h = canvas.height, d = window.devicePixelRatio || 1;
  ctx.clearRect(0, 0, w, h);
  const pts = strokes.flat();
  if (!pts.length) return;
  let sc = d, ox = 0, oy = 0;
  if (!live) {
    const xs = pts.map((p) => p.x), ys = pts.map((p) => p.y);
    const minX = Math.min(...xs), minY = Math.min(...ys);
    const cw = Math.max(1, Math.max(...xs) - minX), ch = Math.max(1, Math.max(...ys) - minY);
    const pad = Math.min(w, h) * 0.08;
    sc = Math.min((w - 2 * pad) / cw, (h - 2 * pad) / ch);
    ox = (w - cw * sc) / 2 - minX * sc; oy = (h - ch * sc) / 2 - minY * sc;
  }
  ctx.lineCap = ctx.lineJoin = "round"; ctx.strokeStyle = "#3a2a22";
  ctx.lineWidth = live ? 6 * d : Math.min(13.5 * d, Math.max(1.6 * d, 7 * sc));
  for (const s of strokes) {
    ctx.beginPath();
    s.forEach((p, i) => (i ? ctx.lineTo(p.x * sc + ox, p.y * sc + oy) : ctx.moveTo(p.x * sc + ox, p.y * sc + oy)));
    if (s.length === 1) ctx.lineTo(s[0].x * sc + ox + 0.1, s[0].y * sc + oy);
    ctx.stroke();
  }
}
const paintLive = () => current === "draw" && round[idx] && paint($("pad"), round[idx].strokes, true);
const paintGuess = () => current === "guess" && round[idx] && paint($("view"), round[idx].strokes);

(() => {
  const c = $("pad"); let cur = null;
  const pt = (e) => { const r = c.getBoundingClientRect(); return { x: Math.round(e.clientX - r.left), y: Math.round(e.clientY - r.top) }; };
  const erase = (p) => {
    const w = round[idx]; let best = -1, bd = 26;
    w.strokes.forEach((s, i) => s.forEach((q) => { const d = Math.hypot(q.x - p.x, q.y - p.y); if (d < bd) { bd = d; best = i; } }));
    if (best >= 0) { w.strokes.splice(best, 1); paintLive(); refreshTools(); }
  };
  c.addEventListener("pointerdown", (e) => {
    c.setPointerCapture(e.pointerId);
    if (tool === "erase") { cur = "erase"; erase(pt(e)); return; }
    cur = [pt(e)]; round[idx].strokes.push(cur); paintLive();
  });
  c.addEventListener("pointermove", (e) => {
    if (!cur) return;
    if (cur === "erase") erase(pt(e)); else { cur.push(pt(e)); paintLive(); }
  });
  const end = () => { cur = null; refreshTools(); };
  c.addEventListener("pointerup", end); c.addEventListener("pointercancel", end);
})();
function refreshTools() {
  const has = round[idx]?.strokes.length > 0;
  $("t-undo").disabled = !has; $("t-clear").disabled = !has;
  $("t-pen").classList.toggle("on", tool === "pen"); $("t-erase").classList.toggle("on", tool === "erase");
}
$("t-pen").onclick = () => { tool = "pen"; refreshTools(); };
$("t-erase").onclick = () => { tool = "erase"; refreshTools(); };
$("t-undo").onclick = () => { round[idx].strokes.pop(); paintLive(); refreshTools(); };
$("t-clear").onclick = () => { round[idx].strokes = []; paintLive(); refreshTools(); };

// ---------- flow ----------
function setRing(ring, arc, timeEl, left, total) {
  timeEl.textContent = left;
  arc.style.strokeDashoffset = 276.5 * (1 - Math.max(0, left) / total);
  ring.classList.toggle("warn", left <= 3);
}
async function start() {
  if (words.lang !== lang) { words = await (await fetch(t().file)).json(); words.lang = lang; }
  const pool = [...words].sort(() => Math.random() - 0.5);
  round = pool.slice(0, WORD_COUNT).map((w) => ({ ...w, strokes: [], guess: "", correct: false, points: 0, ms: 0 }));
  idx = 0; tool = "pen"; score = 0; nextDraw();
}
function nextDraw() {
  clearInterval(dTimer);
  if (idx >= round.length) { idx = 0; return nextGuess(); }
  const w = round[idx];
  show("draw");
  $("d-prog").textContent = `${idx + 1} / ${round.length}`;
  $("d-word").textContent = w.text.charAt(0).toLocaleUpperCase(lang) + w.text.slice(1);
  const wl = w.text.length; $("d-word").style.fontSize = Math.max(40, Math.min(70, 425 * 0.97 / (wl * 0.5))) * u + "px";
  $("d-line").style.animation = "none"; void $("d-line").offsetWidth; $("d-line").style.animation = "";
  const total = DRAW_SECONDS[w.difficulty] ?? 7; let left = total;
  const arc = $("d-arc"); arc.style.transition = "none"; setRing($("d-ring"), arc, $("d-time"), left, total); void arc.getBoundingClientRect(); arc.style.transition = "";
  tool = "pen"; refreshTools(); paintLive();
  dTimer = setInterval(() => {
    left--; setRing($("d-ring"), arc, $("d-time"), left, total);
    if (left <= 0) finishDraw();
  }, 1000);
}
function finishDraw() { clearInterval(dTimer); idx++; nextDraw(); }
$("d-next").onclick = finishDraw;

function nextGuess() {
  clearInterval(gTimer);
  if (idx >= round.length) return finish();
  const w = round[idx];
  show("guess");
  $("g-prog").textContent = `${idx + 1} / ${round.length}`;
  $("g-xp").textContent = `★ ${score}`;
  $("g-input").value = "";
  paintGuess();
  const total = guessSeconds(w); let left = total;
  const arc = $("g-arc"); arc.style.transition = "none"; setRing($("g-ring"), arc, $("g-time"), left, total); void arc.getBoundingClientRect(); arc.style.transition = "";
  shownAt = performance.now();
  gTimer = setInterval(() => { left--; setRing($("g-ring"), arc, $("g-time"), left, total); if (left <= 0) answer(""); }, 1000);
  $("g-input").focus({ preventScroll: true });
}
function answer(text) {
  clearInterval(gTimer);
  const w = round[idx];
  w.guess = text; w.ms = Math.round(performance.now() - shownAt); w.correct = isCorrect(text, w.text);
  w.points = w.correct ? POINTS + (w.ms < SPEED_MS ? SPEED_BONUS : 0) : 0;
  score += w.points; idx++; nextGuess();
}
$("g-form").onsubmit = (e) => { e.preventDefault(); answer($("g-input").value); };
$("g-input").addEventListener("input", (e) => { const w = round[idx]; if (w && e.target.value.trim() && e.target.value.trim().toLocaleLowerCase(lang) === w.text.toLocaleLowerCase(lang)) answer(e.target.value); });

// ---------- result ----------
function addRow(parent, strokes, ok, label, sub) {
  const row = document.createElement("div"); row.className = "row-card";
  const cv = document.createElement("canvas"), lb = document.createElement("div"), ic = document.createElement("img");
  lb.className = "lbl"; lb.textContent = label; if (sub) { const s = document.createElement("small"); s.textContent = sub; lb.append(s); }
  ic.src = ok ? "img/rs_ic_check.webp" : "img/rs_ic_x.webp"; ic.alt = "";
  row.append(cv, lb, ic); parent.append(row); fitCanvas(cv); paint(cv, strokes);
}
async function finish() {
  const x = t();
  show("search");
  const n = round.filter((w) => w.correct).length;
  const me = {
    language: lang, nickname: "Web", wordIds: round.map((w) => w.id),
    perWord: round.map((w) => ({ wordId: w.id, isCorrect: w.correct, responseTimeMs: w.ms, pointsAwarded: w.points })),
    items: round.map((w) => ({ word: w.text, isCorrect: w.correct, strokes: w.strokes })),
  };
  const minWait = new Promise((r) => setTimeout(r, 2000 + Math.random() * 2000));
  let opp = null, items = [];
  try {
    opp = await Promise.race([net.findOpponent(lang), new Promise((_, rej) => setTimeout(() => rej(new Error("timeout")), 12000))]);
    net.recordRound(me).catch(() => {});
    if (opp) items = await net.loadItems(opp.id).catch(() => []);
  } catch {}
  await minWait;
  show("result");
  $("r-title").textContent = `${x.result}: ${n}/${round.length}`;
  const vs = $("r-vs");
  vs.textContent = opp
    ? `${x.you} ${score} – ${opp.totalScore} ${opp.nickname || ""} · ${score > opp.totalScore ? x.win : score < opp.totalScore ? x.lose : x.draw}`
    : `${x.you}: ${score} ${x.pts} · ${x.noOpp}`;
  const list = $("r-list"); list.innerHTML = "";
  const h1 = document.createElement("div"); h1.className = "who"; h1.textContent = x.mine; list.append(h1);
  for (const w of round) addRow(list, w.strokes, w.correct, w.text, w.guess && !w.correct ? `“${w.guess}”` : "");
  if (items.length) {
    const h2 = document.createElement("div"); h2.className = "who"; h2.textContent = `${x.theirs} · ${opp.nickname || ""}`; list.append(h2);
    for (const it of items) addRow(list, it.strokes, it.isCorrect, it.word, "");
  }
  $("result").scrollTop = 0;
}

// ---------- navigation ----------
function confirmLeave() {
  $("dialog").classList.remove("hidden");
  $("dlg-yes").onclick = () => { $("dialog").classList.add("hidden"); clearInterval(dTimer); clearInterval(gTimer); show("menu"); };
  $("dlg-no").onclick = () => $("dialog").classList.add("hidden");
}
$("d-back").onclick = confirmLeave; $("g-back").onclick = confirmLeave;
$("start").onclick = start;
$("again").onclick = () => show("menu");
$("r-back").onclick = () => show("menu");
applyLang(); show("menu");
