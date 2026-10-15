// Background music (the same five tracks as the app) and short WebAudio sound effects (the app uses ToneGenerator beeps).
let ac = null, bgm = null, idx = 0, started = false, prefs = () => ({ s0: true, s1: true, s2: true });
export const setPrefs = (fn) => { prefs = fn; };
const on = (k) => !!prefs()[k];
function ctx() { if (!ac) { const C = window.AudioContext || window.webkitAudioContext; if (C) ac = new C(); } if (ac && ac.state === "suspended") ac.resume(); return ac; }
function tone(freq, dur, type = "sine", vol = 0.12, when = 0, slide = 0) {
  if (!on("s0")) return; const a = ctx(); if (!a) return;
  const t = a.currentTime + when, o = a.createOscillator(), g = a.createGain();
  o.type = type; o.frequency.setValueAtTime(freq, t); if (slide) o.frequency.linearRampToValueAtTime(freq + slide, t + dur);
  g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(vol, t + 0.01); g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  o.connect(g).connect(a.destination); o.start(t); o.stop(t + dur + 0.02);
}
export const sfx = {
  tick: () => tone(880, 0.08, "square", 0.05),
  correct: () => { tone(660, 0.12, "triangle", 0.14); tone(990, 0.2, "triangle", 0.14, 0.1); },
  wrong: () => tone(180, 0.28, "sawtooth", 0.1, 0, -60),
  over: () => { tone(523, 0.15, "triangle", 0.14); tone(659, 0.15, "triangle", 0.14, 0.15); tone(784, 0.3, "triangle", 0.14, 0.3); },
  tap: () => tone(520, 0.05, "sine", 0.06),
  buy: () => { tone(784, 0.1, "triangle", 0.12); tone(1047, 0.18, "triangle", 0.12, 0.09); },
};
export function vibrate(ms = 30) { if (on("s2") && navigator.vibrate) try { navigator.vibrate(ms); } catch {} }
function play() {
  if (!bgm) { bgm = new Audio(); bgm.volume = 0.3; bgm.addEventListener("ended", () => { idx = (idx + 1) % 5; bgm.src = `audio/music_${idx + 1}.m4a`; if (on("s0") && on("s1")) bgm.play().catch(() => {}); }); bgm.src = `audio/music_${idx + 1}.m4a`; }
  if (on("s0") && on("s1")) bgm.play().catch(() => {}); else bgm.pause();
}
// Browsers only allow sound after a touch: start on the first one, then keep in step with the settings.
export function initAudio() {
  const go = () => { if (started) return; started = true; ctx(); play(); };
  addEventListener("pointerdown", go, { once: false, passive: true });
  document.addEventListener("visibilitychange", () => { if (document.hidden) bgm && bgm.pause(); else if (started) play(); });
}
export const syncMusic = () => { if (started) play(); };
