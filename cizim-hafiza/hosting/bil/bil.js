// "Bil Bakalım" on the web (karalak.com/b/ID): a friend's drawing, watched being drawn and guessed in three tries, then "sıra sende".
//
// Who may play here: iPhones and iPads only. Everybody else is NOT let play in the browser, on purpose: the game's income is
// the ads inside the app, so an Android visitor gets the app (it opens straight on this drawing) or the Play Store (the link
// rides along as the install referrer, so the app opens on the drawing after the install), and a computer gets the Play Store.
//
// The answer is never in the link. The page checks a guess by hashing it (GuessCodec.kt does the same on the phone) and, when
// the round is over, finds the word by hashing the language's word list.
import * as net from "../oyna/firebase.js";
import { isCorrect, normalize } from "../oyna/matcher.js";
import { nameAllowed } from "../oyna/namefilter.js";

const PKG = "com.sualtikasifi.cizimhafiza";
const MAX_ATTEMPTS = 3, DRAW_SECONDS = 10, SPACE = 1000, MAX_POINTS = 2500, MAX_LINKS_PER_DAY = 20;
const $ = (id) => document.getElementById(id);

const T = {
  tr: {
    title: "Bil Bakalım", loading: "Çizim yükleniyor…", gone: "Bu bağlantı artık geçerli değil. Süresi dolmuş ya da silinmiş olabilir.",
    offline: "Çizim yüklenemedi. Bağlantını kontrol edip tekrar dene.", retry: "Tekrar dene", own: "Bu çizimi sen gönderdin. Arkadaşların bilsin diye bağlantıyı paylaşmaya devam et!",
    someone: "Bir arkadaşın", drew: (n) => `${n} bir şey çizdi`, watch: "İzle ve 3 denemede bil!", replay: "Tekrar izle", you: "Adın:", guest: "Misafir",
    placeholder: "Sence ne çizildi?", guess: "Tahmin et", skip: "Pas geç", solved: "Bildin! 🎉", first: "İlk denemede!", tries: (n) => `${n}. denemede bildin`,
    failed: "Bilemedin 😅", answer: (w) => `Cevap: ${w}`, turn: "Sıra sende!", turnHint: "Şimdi aynı kelimeyi çiz ve arkadaşına gönder.", draw: (w) => `${w.toLocaleUpperCase("tr")} çiz!`,
    undo: "Geri al", done: "Bitti", ready: "Hazır!", readySub: "Çizimin bağlantıyla yola çıktı.", send: "Arkadaşına gönder", copied: "Bağlantı kopyalandı",
    shareText: (u) => `Bu çizim ne? Bil bakalım! 🎨\n${u}`, limit: "Bugün çok fazla bağlantı oluşturdun, yarın tekrar dene.", failedLink: "Bağlantı oluşturulamadı. Tekrar dene.",
    play: "Karalak'ı tarayıcıda oyna", report: "Bildir", reported: "Teşekkürler, inceleyeceğiz.",
    aTitle: "Bu çizimi uygulamada bil!", aText: "Bir arkadaşın sana bir çizim gönderdi. Karalak uygulamasında izleyip bilebilirsin.", aOpen: "Karalak'ta aç", aStore: "Google Play'den indir",
  },
  en: {
    title: "Guess It", loading: "Loading the drawing…", gone: "This link no longer works. It may have expired or been deleted.",
    offline: "The drawing could not be loaded. Check your connection and try again.", retry: "Try again", own: "You sent this drawing. Keep sharing the link so your friends can guess it!",
    someone: "A friend", drew: (n) => `${n} drew something`, watch: "Watch it and guess in 3 tries!", replay: "Watch again", you: "Your name:", guest: "Guest",
    placeholder: "What do you think it is?", guess: "Guess", skip: "Skip", solved: "You got it! 🎉", first: "First try!", tries: (n) => `Guessed on try ${n}`,
    failed: "Not this time 😅", answer: (w) => `Answer: ${w}`, turn: "Your turn!", turnHint: "Now draw the same word and send it to your friend.", draw: (w) => `Draw ${w.toLocaleUpperCase("en")}!`,
    undo: "Undo", done: "Done", ready: "Ready!", readySub: "Your drawing is on its way, link and all.", send: "Send to a friend", copied: "Link copied",
    shareText: (u) => `What is this drawing? Guess it! 🎨\n${u}`, limit: "You made a lot of links today, try again tomorrow.", failedLink: "The link could not be created. Try again.",
    play: "Play Karalak in your browser", report: "Report", reported: "Thank you, we will look into it.",
    aTitle: "Guess this drawing in the app!", aText: "A friend sent you a drawing. Watch it and guess it in the Karalak app.", aOpen: "Open in Karalak", aStore: "Get it on Google Play",
  },
  es: {
    title: "Adivina", loading: "Cargando el dibujo…", gone: "Este enlace ya no funciona. Puede haber caducado o haberse borrado.",
    offline: "No se pudo cargar el dibujo. Revisa tu conexión e inténtalo de nuevo.", retry: "Reintentar", own: "Enviaste este dibujo. ¡Sigue compartiendo el enlace para que tus amigos lo adivinen!",
    someone: "Un amigo", drew: (n) => `${n} dibujó algo`, watch: "¡Míralo y adivina en 3 intentos!", replay: "Ver otra vez", you: "Tu nombre:", guest: "Invitado",
    placeholder: "¿Qué crees que es?", guess: "Adivinar", skip: "Pasar", solved: "¡Lo adivinaste! 🎉", first: "¡A la primera!", tries: (n) => `Acertaste en el intento ${n}`,
    failed: "Esta vez no 😅", answer: (w) => `Respuesta: ${w}`, turn: "¡Te toca!", turnHint: "Ahora dibuja la misma palabra y envíasela a tu amigo.", draw: (w) => `¡Dibuja ${w.toLocaleUpperCase("es")}!`,
    undo: "Deshacer", done: "Listo", ready: "¡Listo!", readySub: "Tu dibujo va en camino, enlace incluido.", send: "Enviar a un amigo", copied: "Enlace copiado",
    shareText: (u) => `¿Qué dibujo es este? ¡Adivina! 🎨\n${u}`, limit: "Hoy creaste muchos enlaces, vuelve a intentarlo mañana.", failedLink: "No se pudo crear el enlace. Inténtalo de nuevo.",
    play: "Juega Karalak en el navegador", report: "Reportar", reported: "Gracias, lo revisaremos.",
    aTitle: "¡Adivina este dibujo en la app!", aText: "Un amigo te envió un dibujo. Míralo y adivínalo en la app de Karalak.", aOpen: "Abrir en Karalak", aStore: "Descargar en Google Play",
  },
  pt: {
    title: "Adivinha", loading: "Carregando o desenho…", gone: "Este link não funciona mais. Pode ter expirado ou sido apagado.",
    offline: "Não foi possível carregar o desenho. Verifique a conexão e tente de novo.", retry: "Tentar de novo", own: "Você enviou este desenho. Continue compartilhando o link para os amigos adivinharem!",
    someone: "Um amigo", drew: (n) => `${n} desenhou algo`, watch: "Assista e adivinhe em 3 tentativas!", replay: "Assistir de novo", you: "Seu nome:", guest: "Visitante",
    placeholder: "O que você acha que é?", guess: "Adivinhar", skip: "Pular", solved: "Você acertou! 🎉", first: "De primeira!", tries: (n) => `Acertou na tentativa ${n}`,
    failed: "Dessa vez não 😅", answer: (w) => `Resposta: ${w}`, turn: "Sua vez!", turnHint: "Agora desenhe a mesma palavra e envie para o seu amigo.", draw: (w) => `Desenhe ${w.toLocaleUpperCase("pt")}!`,
    undo: "Desfazer", done: "Pronto", ready: "Pronto!", readySub: "Seu desenho já está a caminho, com o link.", send: "Enviar a um amigo", copied: "Link copiado",
    shareText: (u) => `Que desenho é este? Adivinhe! 🎨\n${u}`, limit: "Você criou muitos links hoje, tente de novo amanhã.", failedLink: "Não foi possível criar o link. Tente de novo.",
    play: "Jogue Karalak no navegador", report: "Denunciar", reported: "Obrigado, vamos analisar.",
    aTitle: "Adivinhe este desenho no app!", aText: "Um amigo enviou um desenho. Assista e adivinhe no app do Karalak.", aOpen: "Abrir no Karalak", aStore: "Baixar na Google Play",
  },
};
const code2 = (navigator.language || "tr").slice(0, 2).toLowerCase();
const lang = /^(tr|es|pt)$/.test(code2) ? code2 : "en";
const x = T[lang];
document.documentElement.lang = lang;
document.title = "Karalak – " + x.title;
$("title").textContent = x.title;

const view = $("view");
const m = location.pathname.match(/^\/b\/([A-Za-z0-9]{10})\/?$/);
const linkId = m ? m[1] : "";
const android = /Android/i.test(navigator.userAgent) || navigator.userAgentData?.platform === "Android";
const ios = /iPhone|iPad|iPod/i.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);

const h = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
const show = (...nodes) => view.replaceChildren(...nodes);
const button = (cls, text, fn) => { const b = h("button", "btn " + cls, esc(text)); b.type = "button"; b.onclick = fn; return b; };
const safe = (fn) => { try { return fn(); } catch { return null; } };

// ---- everybody but iOS: the app, not the browser --------------------------------------------------------------------------------
if (!ios) {
  const store = `https://play.google.com/store/apps/details?id=${PKG}${linkId ? "&referrer=" + encodeURIComponent("karalak_guess=" + linkId) : ""}`;
  $("title").textContent = x.aTitle;
  const open = h("a", "btn primary", esc(x.aOpen));
  const play = h("a", "btn second", esc(x.aStore));
  play.href = store;
  if (linkId && android) {
    // karalak://guess/ID; if the app is not installed the browser falls back to the Play Store with the link as referrer
    const intent = `intent://guess/${linkId}#Intent;scheme=karalak;package=${PKG};S.browser_fallback_url=${encodeURIComponent(store)};end`;
    open.href = intent;
    setTimeout(() => { location.href = intent; }, 300);
  } else open.href = store;
  // a computer has no app to open: only the store
  show(h("p", "sub", esc(x.aText)), ...(android ? [open, play] : [play]));
} else {
  start().catch(() => fail(x.offline, true));
}

function fail(text, retry) {
  const nodes = [h("p", "sub", esc(text))];
  if (retry) nodes.push(button("primary", x.retry, () => { show(h("p", "sub", esc(x.loading))); start().catch(() => fail(x.offline, true)); }));
  nodes.push(playLink());
  show(...nodes);
}

function playLink() {
  const a = h("a", "btn second", esc(x.play));
  a.href = "/oyna/";
  return a;
}

// ---- the drawing -------------------------------------------------------------------------------------------------------
/** {"v":1,"s":[[x,y,x,y,…],…]} in a 1000 x 1000 square (GuessCodec.encodeStrokes). */
function decode(text) {
  try {
    return JSON.parse(text).s.map((s) => { const p = []; for (let i = 0; i + 1 < s.length; i += 2) p.push([+s[i], +s[i + 1]]); return p; }).filter((s) => s.length);
  } catch { return []; }
}

function encode(strokes) {
  const drawn = strokes.filter((s) => s.length);
  const all = drawn.flat();
  if (!all.length) return '{"v":1,"s":[]}';
  const minX = Math.min(...all.map((p) => p[0])), minY = Math.min(...all.map((p) => p[1]));
  const span = Math.max(Math.max(...all.map((p) => p[0])) - minX, Math.max(...all.map((p) => p[1])) - minY, 1), scale = SPACE / span;
  const keep = Math.max(1, Math.ceil(all.length / MAX_POINTS));
  const clamp = (v) => Math.min(SPACE, Math.max(0, Math.round(v)));
  const s = drawn.map((stroke) => stroke.filter((_, i) => i % keep === 0 || i === stroke.length - 1).flatMap((p) => [clamp((p[0] - minX) * scale), clamp((p[1] - minY) * scale)]));
  return JSON.stringify({ v: 1, s });
}

function paper(withTimer) {
  const box = h("div", "paper"), canvas = document.createElement("canvas");
  const size = 420, dpr = Math.min(2, window.devicePixelRatio || 1);
  canvas.width = canvas.height = Math.round(size * dpr);
  box.append(canvas);
  if (withTimer) box.append(h("div", "timer", ""));
  const ctx = canvas.getContext("2d");
  const k = canvas.width / SPACE;
  const clear = () => { ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.clearRect(0, 0, canvas.width, canvas.height); ctx.setTransform(k, 0, 0, k, 0, 0); ctx.lineCap = "round"; ctx.lineJoin = "round"; ctx.strokeStyle = "#2e2a26"; ctx.lineWidth = 11; };
  const line = (pts) => {
    if (!pts.length) return;
    ctx.beginPath(); ctx.moveTo(pts[0][0], pts[0][1]);
    if (pts.length === 1) ctx.lineTo(pts[0][0] + 0.01, pts[0][1]);
    for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i][0], pts[i][1]);
    ctx.stroke();
  };
  clear();
  return { box, canvas, clear, line, draw: (strokes) => { clear(); strokes.forEach(line); } };
}

/** Plays the drawing stroke by stroke (about as long as it is, 1.2 to 6 seconds), then calls [done]. Returns a function that stops it. */
function replay(pad, strokes, done) {
  const total = strokes.reduce((n, s) => n + s.length, 0), ms = Math.min(6000, Math.max(1200, total * 14));
  let start = 0, raf = 0, stopped = false;
  const frame = (t) => {
    if (stopped) return;
    if (!start) start = t;
    const p = Math.min(1, (t - start) / ms);
    let left = Math.floor(p * total) + 1;
    const part = [];
    for (const s of strokes) { if (left <= 0) break; part.push(s.slice(0, left)); left -= s.length; }
    pad.draw(part);
    if (p < 1) raf = requestAnimationFrame(frame); else done();
  };
  raf = requestAnimationFrame(frame);
  return () => { stopped = true; cancelAnimationFrame(raf); };
}

// ---- words ---------------------------------------------------------------------------------------------------------------
const wordCache = {};
async function wordsOf(language) {
  if (!wordCache[language]) {
    const r = await fetch(`/oyna/words_${language}.json`);
    if (!r.ok) throw new Error("words");
    wordCache[language] = (await r.json()).map((w) => w.text);
  }
  return wordCache[language];
}

async function isRight(guess, words, link) {
  for (const w of words) if (isCorrect(guess, w) && (await net.guessHash(link.salt, w)) === link.hash) return true;
  return false;
}

async function reveal(words, link) {
  for (const w of words) if ((await net.guessHash(link.salt, w)) === link.hash) return w;
  return null;
}

// ---- the flow ------------------------------------------------------------------------------------------------------------
let nameValue = safe(() => localStorage.getItem("bilName")) || `${x.guest} ${1000 + Math.floor(Math.random() * 9000)}`;
const myName = () => {
  const n = nameValue.trim().slice(0, 24);
  return n && nameAllowed(n) ? n : x.guest;
};

async function start() {
  if (!linkId) return fail(x.gone, false);
  show(h("p", "sub", esc(x.loading)));
  const link = await net.getGuessLink(linkId);
  if (!link) return fail(x.gone, false);
  if (link.ownerUid === (await net.myUid())) return fail(x.own, false);
  const strokes = decode(link.strokesJson);
  if (!strokes.length) return fail(x.gone, false);
  const words = await wordsOf(link.lang);
  guessing(link, strokes, words);
}

function guessing(link, strokes, words) {
  let attempts = 0, ready = false, stop = () => {};
  const wrong = [];
  const pad = paper(false);
  const dots = h("div", "dots"), hint = h("p", "hint"), miss = h("p", "wrong");
  const input = h("input"); input.type = "text"; input.placeholder = x.placeholder; input.disabled = true; input.autocomplete = "off"; input.autocapitalize = "none"; input.enterKeyHint = "done";
  const go = button("primary", x.guess, () => submit()), skip = button("second", x.skip, () => finish(false, Math.max(1, Math.min(MAX_ATTEMPTS, attempts + 1))));
  const again = button("link", x.replay, () => { stop(); play(); });
  const nameBox = h("div", "name", `<span>${esc(x.you)}</span>`), nameIn = h("input"); nameIn.type = "text"; nameIn.maxLength = 24; nameIn.value = nameValue;
  nameIn.oninput = () => { nameValue = nameIn.value; safe(() => localStorage.setItem("bilName", nameValue)); };
  nameBox.append(nameIn);
  const sync = () => {
    dots.replaceChildren(...Array.from({ length: MAX_ATTEMPTS }, (_, i) => h("i", i < attempts ? "x" : "")));
    const pattern = link.pattern.split(" ").map(Number).filter((n) => n > 0);
    hint.textContent = attempts < 1 ? "" : pattern.map((n, wi) => Array.from({ length: n }, (_, i) => (wi === 0 && i === 0 && attempts >= 2 && link.first ? link.first : "_")).join(" ")).join("   ");
    miss.textContent = wrong.join("  ·  ");
    go.disabled = skip.disabled = input.disabled = !ready;
  };
  const play = () => { ready = false; sync(); stop = replay(pad, strokes, () => { ready = true; sync(); input.focus({ preventScroll: true }); }); };
  const submit = async () => {
    const g = input.value.trim();
    if (!ready || !g) return;
    ready = false; sync();
    if (await isRight(g, words, link)) return finish(true, attempts + 1);
    attempts++; wrong.push(g); input.value = "";
    if (attempts >= MAX_ATTEMPTS) return finish(false, attempts);
    ready = true; sync(); input.focus({ preventScroll: true });
  };
  input.onkeydown = (e) => { if (e.key === "Enter") submit(); };
  const owner = link.ownerName || x.someone;
  const sub = h("p", "sub", `<b>${esc(x.drew(owner))}</b><br>${esc(x.watch)}`);
  show(sub, pad.box, dots, hint, miss, input, go, skip, again, nameBox);
  play();

  async function finish(solved, tries) {
    stop(); ready = false;
    const word = await reveal(words, link).catch(() => null);
    net.sendGuessResult(link, solved, tries, myName()).catch(() => {});
    verdict(link, strokes, words, solved, tries, word);
  }
}

function verdict(link, strokes, words, solved, tries, word) {
  const pad = paper(false);
  pad.draw(strokes);
  const nodes = [h("p", "big", esc(solved ? x.solved : x.failed)), h("p", "word", esc(solved ? (tries === 1 ? x.first : x.tries(tries)) : (word ? x.answer(word) : ""))), pad.box];
  if (word) nodes.push(h("p", "small", esc(x.turnHint)), button("primary", x.turn, () => drawing(link, strokes, words, solved, tries, word)));
  nodes.push(playLink());
  const rep = button("link", x.report, async () => {
    rep.disabled = true;
    await net.reportPlayer(link.ownerUid, link.ownerName || "?", "OTHER", `guess ${link.id}`).catch(() => {});
    rep.textContent = x.reported;
  });
  nodes.push(rep);
  show(...nodes);
}

function linksToday() {
  const day = new Date().toISOString().slice(0, 10);
  const s = safe(() => JSON.parse(localStorage.getItem("bilLinks") || "{}")) || {};
  return s.day === day ? s.n : 0;
}
function countLink() {
  const day = new Date().toISOString().slice(0, 10);
  safe(() => localStorage.setItem("bilLinks", JSON.stringify({ day, n: linksToday() + 1 })));
}

/** Sıra sende: the same word, drawn in ten seconds; going back (nothing drawn, link refused) returns to the verdict as it was. */
function drawing(link, original, words, solved, tries, word) {
  const back = () => verdict(link, original, words, solved, tries, word);
  const pad = paper(true), timerEl = pad.box.querySelector(".timer");
  const strokes = [];
  let cur = null, left = DRAW_SECONDS, ended = false, busy = false;
  const t0 = Date.now();
  const pt = (e) => { const r = pad.canvas.getBoundingClientRect(); return [(e.clientX - r.left) * SPACE / r.width, (e.clientY - r.top) * SPACE / r.height]; };
  pad.canvas.onpointerdown = (e) => { if (ended) return; pad.canvas.setPointerCapture(e.pointerId); cur = [pt(e)]; strokes.push(cur); pad.draw(strokes); };
  pad.canvas.onpointermove = (e) => { if (!cur || ended) return; cur.push(pt(e)); pad.draw(strokes); };
  const up = () => { cur = null; };
  pad.canvas.onpointerup = up; pad.canvas.onpointercancel = up;
  const undo = button("second", x.undo, () => { strokes.pop(); pad.draw(strokes); });
  const doneBtn = button("primary", x.done, () => end());
  const tick = setInterval(() => { left--; timerEl.textContent = left; if (left <= 0) end(); }, 1000);
  timerEl.textContent = left;
  show(h("p", "sub", `<b>${esc(x.draw(word))}</b>`), pad.box, doneBtn, undo);

  async function end() {
    if (ended || busy) return;
    clearInterval(tick);
    if (!strokes.some((s) => s.length)) return back();
    if (linksToday() >= MAX_LINKS_PER_DAY) { ended = true; alert(x.limit); return back(); }
    ended = busy = true; doneBtn.disabled = undo.disabled = true;
    try {
      const id = await net.createGuessLink({ word, lang: link.lang, strokesJson: encode(strokes), seconds: Math.round((Date.now() - t0) / 1000), ownerName: myName() });
      countLink();
      shared(id, strokes);
    } catch (e) {
      alert(e && e.code === "guess-limit" ? x.limit : x.failedLink);
      back();
    }
  }
}

function shared(id, strokes) {
  const url = `${location.origin}/b/${id}`, text = x.shareText(url);
  const pad = paper(false);
  pad.draw(strokes);
  const copy = h("p", "box", esc(url));
  const send = button("primary", x.send, async () => {
    if (navigator.share) { try { await navigator.share({ text }); return; } catch { /* closed: fall through to copying */ } }
    try { await navigator.clipboard.writeText(text); send.textContent = x.copied; } catch { window.prompt("", text); }
  });
  show(h("p", "big", esc(x.ready)), h("p", "word", esc(x.readySub)), pad.box, copy, send, playLink());
}
