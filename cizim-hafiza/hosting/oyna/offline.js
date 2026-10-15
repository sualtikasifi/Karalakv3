// Play Offline setup (port of WordCountScreen.kt on the shared WoodScreen): how many words, which mode, which categories, which
// difficulties. The same wooden look is used by the Race a Friend "create room" screen (online.js).
export const SETUP_T = {
  tr: { title: "Çevrimdışı Oyna", count: "Kaç kelimeyle oynamak istersin?", mode: "Mod seç", normal: "Normal", relaxed: "Süresiz", cat: "Kategori seç (isteğe bağlı)", all: "Tümü", diff: "Zorluk seç (isteğe bağlı)", easy: "Kolay", medium: "Orta", hard: "Zor", start: "Başla" },
  en: { title: "Play Offline", count: "How many words do you want to play with?", mode: "Choose mode", normal: "Normal", relaxed: "Untimed", cat: "Choose a category (optional)", all: "All", diff: "Choose difficulty (optional)", easy: "Easy", medium: "Medium", hard: "Hard", start: "Start" },
};
// Matched on both languages' names, as in WordCountScreen.categoryEmoji.
export const CAT_EMOJI = { Hayvanlar: "🐶", Animals: "🐶", Eşyalar: "🧺", Objects: "🧺", Meslekler: "👮", Professions: "👮", Spor: "⚽", Sports: "⚽", Doğa: "🌲", Nature: "🌲", Yiyecekler: "🍎", Food: "🍎", Taşıtlar: "🚗", Vehicles: "🚗", Duygular: "😊", Emotions: "😊", Giyim: "👕", Clothing: "👕" };
const COUNTS = [10, 20, 30, 40, 50];
const sel = { count: 10, mode: "NORMAL", cats: new Set(), diffs: new Set() };
let cats = [];

const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };

export function layoutOffline(c) {
  const { place, $ } = c;
  place($("os-back"), 20, 40, 110, 106); place($("os-title"), 160, 270, 520, 110);
  place($("os-paper"), 42, 472, 757, 925); place($("os-body"), 110, 520, 621, 840);
  place($("os-go"), 65, 1610, 710, 172);
}

/** Shrinks a pill's text until it stays on one line instead of wrapping ("🐶 Hayvanlar" used to break into emoji over word). */
function fit(b, base) {
  b.style.fontSize = base + "px";
  for (let s = base; s > base * 0.62 && b.scrollWidth > b.clientWidth + 1; s -= 1) b.style.fontSize = s + "px";
}

/**
 * One captioned row-group of choices on the wooden paper (PanelRow + ChoiceGrid): items are [text, selected, onClick];
 * `h` is the pill height and `size` the text size in art units. An item without onClick is a plain, disabled pill.
 */
/** The paper board wraps its rows like the Compose CompactPanel: 52dp above the first caption, 64dp below the last pill, rows 8dp apart (art units: 1dp = 2.05). */
export function fitPaper(c, paperId, bodyId, top = 425) {
  const u = c.u(), body = c.$(bodyId), paper = c.$(paperId), TOP = 107, BOTTOM = 131, LIMIT = 1607;
  body.style.height = "auto"; body.style.maxHeight = (LIMIT - top - TOP - BOTTOM) * u + "px";
  const h = Math.min(body.scrollHeight / u, LIMIT - top - TOP - BOTTOM);
  c.place(paper, 41, top, 759, TOP + h + BOTTOM); c.place(body, 41 + 70, top + TOP, 759 - 140, h);
}

export function setupGroup(body, u, label, items, cols, h, size) {
  const wrap = el("div", "os-g"); wrap.append(el("div", "os-l", label));
  const grid = el("div", "os-grid"); grid.style.setProperty("--cols", cols); grid.style.setProperty("--ph", h * u + "px");
  items.forEach(([txt, on, fn]) => { const b = el("button", "os-pill" + (on ? " on" : "") + (fn ? "" : " dis"), txt); if (fn) b.onclick = fn; grid.append(b); });
  wrap.append(grid); body.append(wrap);
  requestAnimationFrame(() => grid.querySelectorAll(".os-pill").forEach((b) => fit(b, size * u)));
}

export async function renderOffline(c) {
  const { $, lang } = c, x = SETUP_T[lang()], u = c.u();
  if (!cats.length || cats.lang !== lang()) {
    try { const w = await (await fetch(lang() === "tr" ? "words_tr.json" : "words_en.json")).json(); cats = [...new Set(w.map((q) => q.category))].sort((a, b) => a.localeCompare(b, lang())); cats.lang = lang(); sel.cats = new Set(); } catch { cats = []; }
  }
  $("os-title").textContent = x.title; $("os-go-t").textContent = x.start;
  const body = $("os-body"); body.replaceChildren();
  const group = (label, items, cols, h, size) => setupGroup(body, u, label, items, cols, h, size);
  const again = () => renderOffline(c);
  group(x.count, COUNTS.map((n) => [String(n), sel.count === n, () => { sel.count = n; again(); }]), 5, 78, 37);
  group(x.mode, [["⏱️  " + x.normal, sel.mode === "NORMAL", () => { sel.mode = "NORMAL"; again(); }], ["🧘  " + x.relaxed, sel.mode === "RELAXED", () => { sel.mode = "RELAXED"; again(); }]], 2, 82, 29);
  group(x.cat, [...cats.map((k) => [`${CAT_EMOJI[k] || "✨"} ${k}`, sel.cats.has(k), () => { sel.cats.has(k) ? sel.cats.delete(k) : sel.cats.add(k); again(); }]), [`🎨 ${x.all}`, sel.cats.size === 0, () => { sel.cats.clear(); again(); }]], 4, 82, 25);
  group(x.diff, [["EASY", x.easy], ["MEDIUM", x.medium], ["HARD", x.hard]].map(([k, t]) => [t, sel.diffs.has(k), () => { sel.diffs.has(k) ? sel.diffs.delete(k) : sel.diffs.add(k); again(); }]).concat([[x.all, sel.diffs.size === 0, () => { sel.diffs.clear(); again(); }]]), 4, 78, 27);
  fitPaper(c, "os-paper", "os-body");
  $("os-go").onclick = () => c.startOffline({ count: sel.count, relaxed: sel.mode === "RELAXED", cats: [...sel.cats], diffs: [...sel.diffs] });
}
