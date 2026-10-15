import { isCorrect } from "./matcher.js";

const WORD_COUNT = 10;
const DRAW_SECONDS = { EASY: 5, MEDIUM: 7, HARD: 10 };
const T = {
  tr: { tag: "Çiz, ezberle, tahmin et!", start: "Başla", clear: "Sil", ok: "Tamam", skip: "Geç",
        word: "Çiz:", guessN: "Tahmin", res: "Sonuç", again: "Tekrar oyna", file: "words_tr.json" },
  en: { tag: "Draw, memorize, guess!", start: "Start", clear: "Clear", ok: "OK", skip: "Skip",
        word: "Draw:", guessN: "Guess", res: "Result", again: "Play again", file: "words_en.json" },
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

// Strokes are stored as normalized (0..1) point lists, same shape as Android DrawingPoint x/y.
function fit(canvas) {
  const r = canvas.getBoundingClientRect(), d = window.devicePixelRatio || 1;
  canvas.width = Math.round(r.width * d); canvas.height = Math.round(r.height * d);
  return { w: canvas.width, h: canvas.height, d };
}
function paint(canvas, strokes) {
  const ctx = canvas.getContext("2d"), { w, h, d } = fit(canvas);
  ctx.clearRect(0, 0, w, h);
  ctx.lineCap = ctx.lineJoin = "round"; ctx.lineWidth = 5 * d; ctx.strokeStyle = "#3a2410";
  for (const s of strokes) {
    ctx.beginPath();
    s.forEach((p, i) => (i ? ctx.lineTo(p.x * w, p.y * h) : ctx.moveTo(p.x * w, p.y * h)));
    if (s.length === 1) ctx.lineTo(s[0].x * w + 0.1, s[0].y * h);
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
  const pt = (e) => { const r = c.getBoundingClientRect(); return { x: (e.clientX - r.left) / r.width, y: (e.clientY - r.top) / r.height }; };
  c.addEventListener("pointerdown", (e) => { c.setPointerCapture(e.pointerId); cur = [pt(e)]; round[idx].strokes.push(cur); paint(c, round[idx].strokes); });
  c.addEventListener("pointermove", (e) => { if (cur) { cur.push(pt(e)); paint(c, round[idx].strokes); } });
  const end = () => { cur = null; };
  c.addEventListener("pointerup", end); c.addEventListener("pointercancel", end);
})();

function nextGuess() {
  if (idx >= round.length) return finish();
  show("guess");
  $("g-count").textContent = `${T[lang].guessN} ${idx + 1}/${round.length}`;
  $("g-input").value = "";
  paint($("view"), round[idx].strokes);
  $("g-input").focus();
}
function answer(text) {
  const w = round[idx]; w.guess = text; w.correct = isCorrect(text, w.text); idx++; nextGuess();
}
$("g-form").onsubmit = (e) => { e.preventDefault(); answer($("g-input").value); };
$("g-skip").onclick = () => answer("");

function finish() {
  show("result");
  const n = round.filter((w) => w.correct).length;
  $("r-title").textContent = `${T[lang].res}: ${n}/${round.length}`;
  const ul = $("r-list"); ul.innerHTML = "";
  for (const w of round) {
    const li = document.createElement("li"), cv = document.createElement("canvas"), sp = document.createElement("span");
    sp.className = w.correct ? "ok" : "bad";
    sp.textContent = `${w.correct ? "✓" : "✗"} ${w.text}${w.guess && !w.correct ? ` (${w.guess})` : ""}`;
    li.append(cv, sp); ul.append(li); paint(cv, w.strokes);
  }
}
$("start").onclick = start;
$("again").onclick = () => show("menu");
applyLang(); show("menu");
