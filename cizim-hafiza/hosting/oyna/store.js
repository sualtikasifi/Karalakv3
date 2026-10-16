import { appOnlyDialog } from "./ui.js";
// Store page: jokers, pens, frames. Prices and catalogue come straight from the Android enums.
const J = [["FIRST_LETTER", 200, "joker_first_letter", "joker_first_letter_desc", "joker_first_letter"], ["LETTER_COUNT", 150, "joker_letter_count", "joker_letter_count_desc", "joker_letter_count"], ["EXTRA_TIME", 250, "joker_extra_time", "joker_extra_time_desc", "joker_extra_time"]];
let pens = [], frames = [], S = {}, tab = 0, toastTimer = null;

export async function loadStoreData() {
  if (pens.length) return;
  [pens, frames, S] = await Promise.all(["pens.json", "frames.json", "store_strings.json"].map((f) => fetch(f).then((r) => r.json())));
}
export const penList = () => pens;
export const frameList = () => frames;
export const penById = (id) => pens.find((p) => p.id === id) || pens[0];
export const frameById = (id) => frames.find((f) => f.id === id) || frames.find((f) => f.id === "SCRIBBLER");

const tt = (ctx, k, ...a) => { const v = (S[k] || {})[ctx.lang()] || k; return v.replace(/%(\d)\$d/g, (_, i) => a[i - 1]).replace(/%%/g, "%"); };
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const coin = '<img src="img/icon_gold_coin.webp" alt="">';
const fmt = (n) => n.toLocaleString();

export function layoutStore(ctx) {
  const { place, $ } = ctx;
  const sx = (e, x, y, w, h) => place(e, x, y, w, h);
  sx($("st-t1"), 228, 297, 400, 80); sx($("st-t2"), 228, 353, 400, 80);
  sx($("st-back"), 22, 40, 100, 96);
  sx($("st-gold"), 547, 131, 252, 98);
  sx($("st-tabs"), 34, 547, 769, 93);
  const sh = $("st-shelf"), u = ctx.u();
  sh.style.left = 34 * u + "px"; sh.style.top = 673 * u + "px"; sh.style.width = 771 * u + "px"; sh.style.height = ctx.fillH() - 673 * u + "px";
  sh.style.setProperty("--g", 28 * u + "px");
}

export function renderStore(ctx) {
  const { $, P, lang } = ctx;
  $("st-t1").textContent = "Karalak"; $("st-t2").textContent = tt(ctx, "store_title");
  $("st-goldn").textContent = fmt(P.gold);
  const tabs = $("st-tabs"); tabs.replaceChildren();
  [[0, "st_ic_crown", "store_tab_jokers"], [1, "st_ic_pencil", "store_tab_pens"], [2, "st_ic_image", "store_tab_frames"]].forEach(([i, ic, k]) => {
    const b = el("button", "tab " + (tab === i ? "on" : "off"), `<img src="img/${ic}.webp" alt=""><span>${tt(ctx, k)}</span>`);
    b.onclick = () => { tab = i; renderStore(ctx); $("st-shelf").scrollTop = 0; }; tabs.append(b);
  });
  const shelf = $("st-shelf"); shelf.replaceChildren();
  if (tab === 1) {
    pens.filter((p) => p.price > 0 && !p.rune && !p.retired && p.card).sort((a, b) => a.price - b.price).forEach((p) => {
      const id = "pen:" + p.id, owned = P.owned.includes(id), eq = P.pen === p.id;
      const c = el("div", "pcard"); c.style.backgroundImage = `url(img/pen_${p.id.toLowerCase()}.webp)`;
      const nm = el("div", "nm", `<span>${p[lang()]}</span>`);
      const b = el("button", "btn" + (owned ? " green" : P.gold < p.price ? " dis" : ""), owned ? (eq ? "✓ " + tt(ctx, "store_equipped") : tt(ctx, "store_equip")) : `${coin}<span>${fmt(p.price)}</span>`);
      b.onclick = () => owned ? (P.pen = p.id, ctx.saveP(), renderStore(ctx)) : buy(ctx, { art: "buy_frame", name: p[lang()], price: p.price, preview: Object.assign(new Image(), { src: `img/pen_${p.id.toLowerCase()}.webp` }), grant: () => finish(ctx, p.price, () => { P.owned.push(id); P.pen = p.id; }) });
      c.append(nm, b); shelf.append(c);
    });
  } else if (tab === 2) {
    frames.filter((f) => f.price > 0 && !f.rune && !f.retired).sort((a, b) => a.price - b.price).forEach((f) => {
      const id = "frame:" + f.id, owned = P.owned.includes(id), eq = P.frame === f.id;
      const c = el("div", "fcard"); c.append(frameRing(f, "frm"));
      c.append(el("div", "nm", f[lang()]));
      const b = el("button", "btn" + (owned ? " green" : P.gold < f.price ? " dis" : ""), owned ? (eq ? "✓ " + tt(ctx, "store_equipped") : tt(ctx, "store_equip")) : `${coin}<span>${fmt(f.price)}</span>`);
      b.onclick = () => owned ? (P.frame = f.id, ctx.saveP(), renderStore(ctx)) : buy(ctx, { art: "buy_frame", name: f[lang()], price: f.price, preview: frameRing(f, "bd-frm"), grant: () => finish(ctx, f.price, () => { P.owned.push(id); P.frame = f.id; }) });
      c.append(b); shelf.append(c);
    });
  } else {
    const today = Math.floor((Date.now() + 3 * 3600e3) / 86400e3);
    if (P.dailyJokerDay !== today) {
      const [type, , nameK, descK, img] = J[today % 3];
      const c = el("div", "djoker", `<img class="bg" src="img/st_daily_full.webp" alt=""><span class="dj-tag">${tt(ctx, "store_daily_joker_tag")}</span><div class="dj-art"><img src="img/${img}.webp" alt=""></div><div class="dj-txt"><b>${tt(ctx, nameK)}</b><small>${tt(ctx, descK)}</small></div>`);
      const b = el("button", "dj-btn", `<svg viewBox="0 0 24 24"><path fill="#fff" d="M12 2a10 10 0 100 20 10 10 0 000-20zm-2 14.5v-9l7 4.5z"/></svg><span>${ctx.lang() === "tr" ? "Uygulamada ücretsiz al" : "Get it free in the app"}</span>`);
      b.onclick = () => appOnlyDialog(ctx.$("stage"));
      c.append(b); shelf.append(c);
    }
    J.forEach(([type, price, nameK, descK, img]) => {
      const n = P.jokers[type] || 0;
      const c = el("div", "jcard", `<img class="bg" src="img/st_card_joker.webp" alt=""><div class="ja"><img src="img/${img}.webp" alt=""></div><b class="jn">${tt(ctx, nameK)}</b><span class="jo">${tt(ctx, "joker_owned", n)}</span><small class="jd">${tt(ctx, descK)}</small>`);
      const jb = el("div", "jb");
      [1, 5].forEach((q) => {
        const base = q >= 5 ? Math.floor(price * q * 85 / 100) : price * q, cost = Math.floor(base * (100 - ctx.runeFx("DISC")) / 100);
        const b = el("button", "pill" + (q === 1 ? "o" : "b") + (P.gold < cost ? " dis" : ""), q === 1 ? `<span class="pr">${coin}${fmt(cost)}</span>` : `<span class="bk">${tt(ctx, "joker_buy_bulk", q, 15)}</span><span class="pr s">${coin}${fmt(cost)}</span>`);
        b.onclick = () => P.gold < cost ? toast(ctx, tt(ctx, "store_not_enough"), true) : buy(ctx, { joker: true, art: "joker_buy_" + ({ FIRST_LETTER: "first", LETTER_COUNT: "count", EXTRA_TIME: "time" })[type], qty: q, price: cost, grant: () => { finish(ctx, cost, () => { P.jokers[type] = (P.jokers[type] || 0) + q; }, true); bought(ctx, type, nameK, q); } });
        jb.append(b);
      });
      c.append(jb); shelf.append(c);
    });
  }
}

// The ring picture with the player's face inside it, the way Android's LevelAvatar centres it.
export function frameRing(f, cls = "", photo = null) {
  const box = el("div", cls); box.style.position = "relative";
  // `photo`: another picture for the hole (a league filler's face); by default the Karalak mascot
  const face = photo || el("img"); if (!photo) { face.src = "img/avatar_dino.webp"; face.alt = ""; }
  face.style.cssText += `;position:absolute;border-radius:50%;overflow:hidden;width:${f.face * 100}%;height:${f.face * 100}%;left:${50 + f.ox * 100 - f.face * 50}%;top:${50 + f.oy * 100 - f.face * 50}%`;
  const ring = el("img"); ring.src = `img/fr_${f.img}.webp`; ring.alt = ""; ring.style.cssText = "position:absolute;inset:0;width:100%;height:100%";
  box.append(face, ring); return box;
}

function toast(ctx, text, err) {
  const t = ctx.$("st-toast"); t.textContent = text; t.className = err ? "err" : ""; clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.add("hidden"), 2400);
}
// Painted purchase window (Android PaintedPurchaseDialog): the picture carries title, plank and buttons; price,
// "missing gold" note and the preview are written over it. Hit areas sit on the painted buttons.
const JOKER_SPEC = { close: [865, 150, 1020, 310], cancel: [118, 1072, 532, 1224], confirm: [552, 1072, 982, 1226], price: [485, 938, 260, 104], coin: null, warn: [300, 868, 490, 56] };
const FRAME_SPEC = { close: [895, 150, 1060, 320], cancel: [95, 1060, 535, 1225], confirm: [550, 1060, 995, 1225], price: [465, 912, 290, 108], coin: [335, 912, 108], warn: [300, 838, 490, 50] };
const pc = (v, t) => (v / t * 100).toFixed(3) + "%";
const box = (r, abs) => { const [x, y, a, b] = r; const w = abs ? a - x : a, h = abs ? b - y : b; return `left:${pc(x, 1086)};top:${pc(y, 1448)};width:${pc(w, 1086)};height:${pc(h, 1448)}`; };
function buy(ctx, o) {
  const { P, $ } = ctx, d = $("pick"), spec = o.joker ? JOKER_SPEC : FRAME_SPEC, afford = P.gold >= o.price;
  d.classList.remove("hidden"); d.replaceChildren(); d.onclick = null;
  const wrap = el("div", "bd-wrap"), fr = el("div", "bd-frame"); wrap.append(fr);
  const close = () => d.classList.add("hidden");
  const art = el("img", "bd-art", ""); art.src = `img/${o.art}.webp`; fr.append(art);
  if (o.joker) { if (o.qty > 1) fr.append(el("div", "bd-qty", `<span>×${o.qty}</span>`)); }
  else {
    const pv = el("div", "bd-prev"); pv.style.cssText = box([150, 330, 790, 400]); pv.append(o.preview); fr.append(pv);
    const nm = el("div", "bd-name", o.name); nm.style.cssText = box([150, 745, 790, 90]); fr.append(nm);
  }
  if (spec.coin) { const c = el("img", "bd-coin", ""); c.src = "img/icon_gold_coin.webp"; c.style.cssText = `left:${pc(spec.coin[0], 1086)};top:${pc(spec.coin[1], 1448)};width:${pc(spec.coin[2], 1086)}`; fr.append(c); }
  const pr = el("div", "bd-price", fmt(o.price)); pr.style.cssText = box(spec.price); fr.append(pr);
  if (!afford) { const w = el("div", "bd-warn", "⚠ " + tt(ctx, "store_missing_gold", o.price - P.gold)); w.style.cssText = box(spec.warn); fr.append(w); }
  [["close", spec.close, true, close], ["cancel", spec.cancel, true, close], ["confirm", spec.confirm, afford, () => { close(); o.grant(); }]].forEach(([k, r, en, fn]) => {
    const b = el("button", "bd-hit" + (en ? "" : " dis"), ""); b.style.cssText = box(r, true); b.onclick = en ? fn : null; fr.append(b);
  });
  d.onclick = (e) => { if (e.target === d) close(); };
  d.append(wrap);
}
function bought(ctx, type, nameK, qty) {
  const d = ctx.$("pick"); d.classList.remove("hidden"); d.replaceChildren(); d.onclick = null;
  const img = J.find((j) => j[0] === type)[4];
  const o = el("div", "bc", `<div class="bc-art"><i class="bc-rays"></i><div class="bc-j"><img src="img/${img}.webp" alt=""></div></div><h2>${tt(ctx, "joker_bought_title")}</h2><h3>${tt(ctx, nameK)}  +${qty}</h3>`);
  for (let i = 0; i < 60; i++) { const f = el("i", "cf"); f.style.cssText = `left:${40 + Math.random() * 20}%;--dx:${(Math.random() - .5) * 120}vw;--dy:${-20 + Math.random() * 90}vh;background:hsl(${Math.random() * 360},90%,60%);animation-delay:${Math.random() * .25}s`; o.append(f); }
  d.append(o); const done = () => { d.classList.add("hidden"); d.onclick = null; }; d.onclick = done; setTimeout(() => { if (!d.classList.contains("hidden") && d.querySelector(".bc")) done(); }, 2600);
}

function finish(ctx, price, grant, quiet) {
  const { P } = ctx; if (P.gold < price) return toast(ctx, tt(ctx, "store_not_enough"), true);
  P.gold -= price; grant(); ctx.saveP(); renderStore(ctx); ctx.refreshHome(); if (!quiet) toast(ctx, tt(ctx, "store_bought"));
}
