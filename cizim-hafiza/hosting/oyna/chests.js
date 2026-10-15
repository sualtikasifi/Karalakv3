// Chests: won by beating a Quick Match opponent, unlocked on a timer, opened for gold, jokers and sometimes a pen.
import { penList } from "./store.js";
const TIERS = { SILVER: { ms: 2 * 3600e3, gold: [150, 250], img: "chest_apprentice", k: "silver" }, GOLD: { ms: 3 * 3600e3, gold: [450, 700], img: "chest_artist", k: "gold" }, RARE: { ms: 6 * 3600e3, gold: [1500, 2500], img: "chest_surprise", k: "rare" } };
const SLOTS = 4, JOKERS = ["FIRST_LETTER", "LETTER_COUNT", "EXTRA_TIME"];
const T = {
  tr: { title: "Kasalarım", tagline: "Her galibiyet bir hazine!", full: "Yuvalar dolu — bir kasa aç!", empty: "Kasa Yuvası", tier: { silver: "Çırak Kasası", gold: "Sanatçı Kasası", rare: "Sürpriz Kasası" }, tapStart: "Başlatmak için dokun", ready: "Açmak için dokun!", unlocking: "Açılıyor…", waiting: "Sırada",
        busy: "Başka bir kasa açılıyor — önce onun süresi dolmalı.", total: (h) => `Açılma süresi: ${h} saat`, detailStart: "Süresini başlatmak için aşağıdaki butona dokun.", start: "Süreyi Başlat", open: "Kasayı Aç", close: "Kapat", still: "Bu kasanın süresi dolmadı.", ready2: "Kasan hazır!",
        contents: "Kasadan neler çıkar?", loot: { silver: "150–250 altın · %30 ihtimalle 1 joker", gold: "450–700 altın · 1 joker (%30 ihtimalle 2)", rare: "1500–2500 altın · 2 joker (%40 ihtimalle 3) · %12 ihtimalle mağaza kalemi" },
        won: "KASA KAZANDIN!", wonHint: "Süreyi Kasalarım'dan başlat", lost: "Kasa yuvaları dolu! Bu galibiyetin kasası kayboldu. Yer açmak için kasalarını aç.", got: "Kazandın", gold: "altın", jokers: { FIRST_LETTER: "İlk Harf Jokeri", LETTER_COUNT: "Harf Sayısı Jokeri", EXTRA_TIME: "Ekstra Süre Jokeri" }, pen: "Yeni kalem!", ok: "Harika!",
        info: "Hızlı Eşleş'te rakibini yenersen bir kasa kazanırsın! 🎁 Kasalar altın, joker ve bazen nadir kalemler saklar. Kaliteye göre 2, 3 veya 6 saatte açılır; aynı anda tek kasanın sayacı işler. 4 yuvan var — hepsi doluyken yeni kasa uçup gider, o yüzden zamanında aç! 🚀", hm: (h, m) => h ? `${h} sa ${m} dk` : `${m} dk` },
  en: { title: "My Chests", tagline: "Every win is a treasure!", full: "Slots full — open a chest!", empty: "Chest Slot", tier: { silver: "Apprentice Chest", gold: "Artist Chest", rare: "Surprise Chest" }, tapStart: "Tap to start", ready: "Tap to open!", unlocking: "Opening…", waiting: "Waiting",
        busy: "Another chest is opening — wait for it first.", total: (h) => `Opens in: ${h} h`, detailStart: "Tap the button below to start the timer.", start: "Start Timer", open: "Open Chest", close: "Close", still: "This chest isn't ready yet.", ready2: "Your chest is ready!",
        contents: "What's inside?", loot: { silver: "150–250 gold · 30% chance of 1 joker", gold: "450–700 gold · 1 joker (30% chance of 2)", rare: "1500–2500 gold · 2 jokers (40% chance of 3) · 12% chance of a store pen" },
        won: "YOU WON A CHEST!", wonHint: "Start the timer from My Chests", lost: "Chest slots are full! This win's chest was lost. Open some to make room.", got: "You won", gold: "gold", jokers: { FIRST_LETTER: "First Letter Joker", LETTER_COUNT: "Letter Count Joker", EXTRA_TIME: "Extra Time Joker" }, pen: "New pen!", ok: "Awesome!",
        info: "Beat your opponent in Quick Match to win a chest! 🎁 Chests hold gold, jokers and sometimes rare pens. They open in 2, 3 or 6 hours depending on quality; only one timer runs at a time. You have 4 slots — when all are full new chests fly away, so open them in time! 🚀", hm: (h, m) => h ? `${h} h ${m} min` : `${m} min` },
};
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const rnd = (a, b) => a + Math.floor(Math.random() * (b - a + 1));

export function ensure(P) { P.chests = P.chests || { slots: [null, null, null, null], idx: 0, seed: Math.floor(Math.random() * 2 ** 31) + 1 }; return P.chests; }
function bagTier(seed, idx) {
  const bag = [...Array(144).fill("SILVER"), ...Array(72).fill("GOLD"), ...Array(24).fill("RARE")];
  let a = seed | 0; const r = () => { a = (a + 0x6d2b79f5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
  for (let i = bag.length - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [bag[i], bag[j]] = [bag[j], bag[i]]; }
  return bag[((idx % 240) + 240) % 240];
}
// Returns the new chest, or null when every slot is taken.
export function award(P) {
  const C = ensure(P), free = C.slots.findIndex((s) => !s), tier = bagTier(C.seed, C.idx++);
  if (free < 0) return null;
  const chest = { id: Math.random().toString(36).slice(2), tier, start: null }; C.slots[free] = chest; return chest;
}
const isReady = (c, now = Date.now()) => c.start != null && now >= c.start + TIERS[c.tier].ms;
const anyUnlocking = (C, now) => C.slots.some((c) => c && c.start != null && !isReady(c, now));
function roll(P, tier) {
  const g = rnd(...TIERS[tier].gold);
  const n = tier === "SILVER" ? (Math.random() < 0.3 ? 1 : 0) : tier === "GOLD" ? 1 + (Math.random() < 0.3 ? 1 : 0) : 2 + (Math.random() < 0.4 ? 1 : 0);
  const jokers = {}; for (let i = 0; i < n; i++) { const t = JOKERS[Math.floor(Math.random() * 3)]; jokers[t] = (jokers[t] || 0) + 1; }
  let pen = null;
  if (tier === "RARE" && Math.random() < 0.12) { const pool = penList().filter((p) => p.price > 0 && p.price <= 6500 && !p.rune && !p.retired && !P.owned.includes("pen:" + p.id)); if (pool.length) pen = pool[Math.floor(Math.random() * pool.length)]; }
  return { gold: g, jokers, pen };
}
const fmtLeft = (ms, x) => { const s = Math.max(0, Math.ceil(ms / 1000)), h = Math.floor(s / 3600), m = Math.floor(s % 3600 / 60), sec = s % 60; return h ? `${h}:${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")}` : `${m}:${String(sec).padStart(2, "0")}`; };

export function layoutChests(c) {
  const { place, $ } = c, k = 1;
  place($("ch-title"), 166, 1416, 400, 70); place($("ch-tag"), 380, 1426, 344, 64); place($("ch-info"), 731, 1426, 63, 64);
  [[64, 232], [247, 412], [429, 596], [610, 780]].forEach(([a, b], i) => place($("ch-s" + i), a, 1494, b - a, 180));
}
let timer = null;
export function renderChests(c) {
  const { $, P, lang } = c, x = T[lang()], C = ensure(P), now = Date.now();
  $("ch-title").textContent = x.title;
  const full = C.slots.every(Boolean);
  $("ch-tag").textContent = full ? x.full : x.tagline;
  C.slots.forEach((ch, i) => {
    const s = $("ch-s" + i); s.replaceChildren(); s.className = "ch-slot";
    if (!ch) { s.append(el("span", "ch-e", x.empty)); s.onclick = null; return; }
    const tier = TIERS[ch.tier], ready = isReady(ch, now), going = ch.start != null && !ready;
    s.classList.toggle("ready", ready);
    s.append(el("img", "", ""), el("b", "", ready ? x.ready : going ? fmtLeft(ch.start + tier.ms - now, x) : x.tapStart));
    s.firstChild.src = `img/${tier.img}.webp`; s.firstChild.alt = "";
    s.onclick = () => detail(c, ch.id);
  });
  $("ch-info").onclick = () => c.info(x.info);
  clearInterval(timer);
  if (C.slots.some((ch) => ch && ch.start != null && !isReady(ch))) timer = setInterval(() => { if (c.current() === "home") renderChests(c); else clearInterval(timer); }, 1000);
}
function detail(c, id) {
  const { $, P, lang } = c, x = T[lang()], C = ensure(P), ch = C.slots.find((s) => s && s.id === id), d = $("pick"); if (!ch) return;
  const tier = TIERS[ch.tier], now = Date.now(), ready = isReady(ch, now), going = ch.start != null && !ready;
  d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "pk-box", `<img class="ch-big" src="img/${tier.img}.webp" alt=""><b>${x.tier[tier.k]}</b><p>${ready ? x.ready2 : going ? x.unlocking : x.detailStart}</p><p>${x.total(tier.ms / 3600e3)}</p><p class="ch-loot"><u>${x.contents}</u><br>${x.loot[tier.k]}</p>`);
  const act = el("button", "pill orange small", `<span>${ready ? x.open : going ? x.unlocking : x.start}</span>`);
  if (going) act.classList.add("dis");
  act.onclick = () => {
    if (going) return;
    if (!ready) {
      if (anyUnlocking(C, Date.now())) { box.querySelector("p").textContent = x.busy; return; }
      ch.start = Date.now(); c.saveP(); d.classList.add("hidden"); renderChests(c); return;
    }
    const rw = roll(P, ch.tier); P.gold += rw.gold; for (const [t, n] of Object.entries(rw.jokers)) P.jokers[t] = (P.jokers[t] || 0) + n; if (rw.pen) P.owned.push("pen:" + rw.pen.id);
    C.slots[C.slots.indexOf(ch)] = null; c.saveP(); c.refreshHome(); renderChests(c); reward(c, ch.tier, rw);
  };
  const close = el("button", "pill cream small", `<span>${x.close}</span>`); close.onclick = () => d.classList.add("hidden");
  box.append(act, close); d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
export function reward(c, tierKey, rw) {
  const { $, lang } = c, x = T[lang()], d = $("pick"); d.classList.remove("hidden"); d.replaceChildren();
  const lines = [`<div class="rw-gold">🪙 +${rw.gold.toLocaleString()} ${x.gold}</div>`, ...Object.entries(rw.jokers).map(([t, n]) => `<div class="rw-j">🃏 ${n}× ${x.jokers[t]}</div>`)];
  if (rw.pen) lines.push(`<div class="rw-pen">🖊️ ${x.pen} <b>${rw.pen[lang()]}</b></div>`);
  const box = el("div", "pk-box rwbox", `<img class="ch-big pop" src="img/${TIERS[tierKey].img}.webp" alt=""><b>${x.got}</b>${lines.join("")}`);
  const ok = el("button", "pill orange small", `<span>${x.ok}</span>`); ok.onclick = () => d.classList.add("hidden"); box.append(ok); d.append(box);
}
export function wonDialog(c, chest) {
  const { $, lang } = c, x = T[lang()], d = $("pick"); d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "pk-box rwbox");
  if (chest) box.innerHTML = `<b>${x.won}</b><img class="ch-big pop" src="img/${TIERS[chest.tier].img}.webp" alt=""><p>${x.tier[TIERS[chest.tier].k]}</p><p>${x.total(TIERS[chest.tier].ms / 3600e3)}</p><p>${x.wonHint}</p>`;
  else box.innerHTML = `<p>${x.lost}</p>`;
  const ok = el("button", "pill orange small", `<span>${x.ok}</span>`); ok.onclick = () => d.classList.add("hidden"); box.append(ok); d.append(box);
}
