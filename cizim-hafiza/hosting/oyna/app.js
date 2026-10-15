import { isCorrect } from "./matcher.js";
import * as net from "./firebase.js";

const WORD_COUNT = 10;
const DRAW_SECONDS = { EASY: 5, MEDIUM: 7, HARD: 10 };
const POINTS = 5, SPEED_BONUS = 2, SPEED_MS = 3000;
const GUESS_BASE = { EASY: 8, MEDIUM: 10, HARD: 12 };
const guessSeconds = (w) => Math.min(13, (GUESS_BASE[w.difficulty] ?? 10) + Math.max(0, [...w.text].filter((c) => /\p{L}/u.test(c)).length - 10));
const T = {
  tr: { tag: "Çiz, ezberle, tahmin et!", start: "Başla", clear: "Sil", ok: "Tamam", skip: "Geç",
        word: "Çiz:", guessN: "Tahmin", res: "Sonuç", again: "Tekrar oyna", searching: "Rakip aranıyor…", noOpp: "Şu an rakip bulunamadı.", you: "Sen", win: "Kazandın!", lose: "Kaybettin", draw: "Berabere", file: "words_tr.json" },
  en: { tag: "Draw, memorize, guess!", start: "Start", clear: "Clear", ok: "OK", skip: "Skip",
        word: "Draw:", guessN: "Guess", res: "Result", again: "Play again", searching: "Finding an opponent…", noOpp: "No opponent found right now.", you: "You", win: "You win!", lose: "You lose", draw: "Draw", file: "words_en.json" },
};
const $ = (id) => document.getElementById(id);
let lang = (navigator.language || "tr").startsWith("tr") ? "tr" : "en";
let words = [], round = [], idx = 0, timer = null;

function show(id) {
  for (const s of document.querySelectorAll(".screen")) s.classList.toggle("hidden", s.id !== id);
}
function applyLang() {
  const t = T[lang];
  document.documentElement.lang = lang;
  $("tagline").textContent = t.tag; $("start").textContent = t.start;
  $("d-clear").textContent = t.clear; $("g-ok").textContent = t.ok; $("g-skip").textContent = t.skip;
  $("again").textContent = t.again;
  $("lang-tr").classList.toggle("on", lang === "tr"); $("lang-en").classList.toggle("on", lang === "en");
}
$("lang-tr").onclick = () => { lang = "tr"; applyLang(); };
$("lang-en").onclick = () => { lang = "en"; applyLang(); };

// Strokes are lists of {x,y} in canvas pixels (same shape as Android DrawingPoint); paint() fits them to the bounding box.
function fit(canvas) {
  const r = canvas.getBoundingClientRect(), d = window.devicePixelRatio || 1;
  canvas.width = Math.round(r.width * d); canvas.height = Math.round(r.height * d);
  return { w: canvas.width, h: canvas.height, d };
}
function paint(canvas, strokes) {
  const ctx = canvas.getContext("2d"), { w, h, d } = fit(canvas);
  ctx.clearRect(0, 0, w, h);
  const pts = strokes.flat();
  if (!pts.length) return;
  const xs = pts.map((p) => p.x), ys = pts.map((p) => p.y);
  const minX = Math.min(...xs), minY = Math.min(...ys);
  const cw = Math.max(1, Math.max(...xs) - minX), ch = Math.max(1, Math.max(...ys) - minY);
  const pad = Math.min(w, h) * 0.08, sc = Math.min((w - 2 * pad) / cw, (h - 2 * pad) / ch);
  const ox = (w - cw * sc) / 2 - minX * sc, oy = (h - ch * sc) / 2 - minY * sc;
  ctx.lineCap = ctx.lineJoin = "round"; ctx.strokeStyle = "#3a2410";
  ctx.lineWidth = Math.min(13.5, Math.max(1.6, 9 * sc));
  for (const s of strokes) {
    ctx.beginPath();
    s.forEach((p, i) => (i ? ctx.lineTo(p.x * sc + ox, p.y * sc + oy) : ctx.moveTo(p.x * sc + ox, p.y * sc + oy)));
    if (s.length === 1) ctx.lineTo(s[0].x * sc + ox + 0.1, s[0].y * sc + oy);
    ctx.stroke();
  }
}

function pickWords() {
  const pool = [...words].sort(() => Math.random() - 0.5);
  return pool.slice(0, WORD_COUNT).map((w) => ({ ...w, strokes: [], guess: "", correct: false }));
}

async function start() {
  if (!words.length || words.lang !== lang) {
    words = await (await fetch(T[lang].file)).json(); words.lang = lang;
  }
  round = pickWords(); idx = 0; nextDraw();
}

function nextDraw() {
  if (idx >= round.length) { idx = 0; return nextGuess(); }
  const w = round[idx];
  show("draw");
  $("d-count").textContent = `${idx + 1}/${round.length}`;
  $("d-word").textContent = w.text;
  let left = DRAW_SECONDS[w.difficulty] ?? 7;
  $("d-timer").textContent = left;
  w.strokes = []; paint($("pad"), []);
  clearInterval(timer);
  timer = setInterval(() => {
    $("d-timer").textContent = --left;
    if (left <= 0) { clearInterval(timer); idx++; nextDraw(); }
  }, 1000);
}
$("d-clear").onclick = () => { round[idx].strokes = []; paint($("pad"), []); };

(() => {
  const c = $("pad"); let cur = null;
  const pt = (e) => { const r = c.getBoundingClientRect(); return { x: Math.round(e.clientX - r.left), y: Math.round(e.clientY - r.top) }; };
  c.addEventListener("pointerdown", (e) => { c.setPointerCapture(e.pointerId); cur = [pt(e)]; round[idx].strokes.push(cur); paint(c, round[idx].strokes); });
  c.addEventListener("pointermove", (e) => { if (cur) { cur.push(pt(e)); paint(c, round[idx].strokes); } });
  const end = () => { cur = null; };
  c.addEventListener("pointerup", end); c.addEventListener("pointercancel", end);
})();

let gTimer = null, shownAt = 0, score = 0;
function nextGuess() {
  clearInterval(gTimer);
  if (idx >= round.length) return finish();
  if (idx === 0) score = 0;
  const w = round[idx];
  show("guess");
  $("g-count").textContent = `${T[lang].guessN} ${idx + 1}/${round.length}`;
  $("g-score").textContent = `${score}`;
  $("g-input").value = "";
  paint($("view"), w.strokes);
  let left = guessSeconds(w);
  $("g-timer").textContent = left;
  shownAt = performance.now();
  gTimer = setInterval(() => { $("g-timer").textContent = --left; if (left <= 0) answer(""); }, 1000);
  $("g-input").focus();
}
function answer(text) {
  clearInterval(gTimer);
  const w = round[idx];
  w.guess = text; w.ms = Math.round(performance.now() - shownAt); w.correct = isCorrect(text, w.text);
  w.points = w.correct ? POINTS + (w.ms < SPEED_MS ? SPEED_BONUS : 0) : 0;
  score += w.points; idx++; nextGuess();
}
$("g-form").onsubmit = (e) => { e.preventDefault(); answer($("g-input").value); };
$("g-skip").onclick = () => answer("");

async function finish() {
  show("result");
  const n = round.filter((w) => w.correct).length;
  $("r-title").textContent = `${T[lang].res}: ${n}/${round.length} · ${score}`;
  $("r-vs").textContent = T[lang].searching;
  const ul = $("r-list"); ul.innerHTML = "";
  for (const w of round) addRow(ul, w.strokes, w.correct, `${w.text}${w.guess && !w.correct ? ` (${w.guess})` : ""}`);
  try {
    const me = {
      language: lang, nickname: "Web", wordIds: round.map((w) => w.id),
      perWord: round.map((w) => ({ wordId: w.id, isCorrect: w.correct, responseTimeMs: w.ms ?? 0, pointsAwarded: w.points ?? 0 })),
      items: round.map((w) => ({ word: w.text, isCorrect: w.correct, strokes: w.strokes })),
    };
    const opp = await Promise.race([net.findOpponent(lang), new Promise((_, rej) => setTimeout(() => rej(new Error("timeout")), 12000))]);
    net.recordRound(me).catch(() => {});
    if (!opp) { $("r-vs").textContent = T[lang].noOpp; return; }
    const t = T[lang];
    $("r-vs").textContent = `${t.you} ${score} – ${opp.totalScore} ${opp.nickname || ""} · ` + (score > opp.totalScore ? t.win : score < opp.totalScore ? t.lose : t.draw);
    const items = await net.loadItems(opp.id);
    const h = document.createElement("li"); h.textContent = opp.nickname; h.style.fontWeight = "700"; ul.append(h);
    for (const it of items) addRow(ul, it.strokes, it.isCorrect, it.word);
  } catch (e) { $("r-vs").textContent = T[lang].noOpp; }
}
function addRow(ul, strokes, ok, label) {
  const li = document.createElement("li"), cv = document.createElement("canvas"), sp = document.createElement("span");
  sp.className = ok ? "ok" : "bad"; sp.textContent = `${ok ? "✓" : "✗"} ${label}`;
  li.append(cv, sp); ul.append(li); paint(cv, strokes);
}
$("start").onclick = start;
$("again").onclick = () => show("menu");
applyLang(); show("menu");
