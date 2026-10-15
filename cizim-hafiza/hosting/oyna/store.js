// Store page: jokers, pens, frames. Prices and catalogue come straight from the Android enums.
const J = [["FIRST_LETTER", 200, "joker_first_letter", "joker_first_letter_desc", "joker_first_letter"], ["LETTER_COUNT", 150, "joker_letter_count", "joker_letter_count_desc", "joker_letter_count"], ["EXTRA_TIME", 250, "joker_extra_time", "joker_extra_time_desc", "joker_extra_time"]];
let pens = [], frames = [], S = {}, tab = 1, toastTimer = null;

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
  sx($("st-t1"), 228, 281, 400, 80); sx($("st-t2"), 228, 337, 400, 80);
  sx($("st-back"), 22, 40, 100, 96);
  sx($("st-gold"), 540, 56, 270, 70);
  sx($("st-tabs"), 36, 560, 770, 100);
  const sh = $("st-shelf"), u = ctx.u();
  sh.style.left = 36 * u + "px"; sh.style.top = 690 * u + "px"; sh.style.width = 770 * u + "px"; sh.style.height = ctx.stageH() - 690 * u + "px";
  sh.style.setProperty("--g", 20 * u + "px");
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
      const nm = el("div", "nm", `<span>${p[lang()]}</span>${p["tag_" + lang()] ? `<small>${p["tag_" + lang()]}</small>` : ""}`);
      const b = el("button", "btn" + (owned ? " green" : P.gold < p.price ? " dis" : ""), owned ? (eq ? "✓ " + tt(ctx, "store_equipped") : tt(ctx, "store_equip")) : `${coin}<span>${fmt(p.price)}</span>`);
      b.onclick = () => owned ? (P.pen = p.id, ctx.saveP(), renderStore(ctx)) : buy(ctx, p[lang()], p.price, () => { P.owned.push(id); P.pen = p.id; });
      c.append(nm, b); shelf.append(c);
    });
  } else if (tab === 2) {
    frames.filter((f) => f.price > 0 && !f.rune && !f.retired).sort((a, b) => a.price - b.price).forEach((f) => {
      const id = "frame:" + f.id, owned = P.owned.includes(id), eq = P.frame === f.id;
      const c = el("div", "fcard"); c.append(frameRing(f, "frm"));
      c.append(el("div", "nm", f[lang()]));
      const b = el("button", "btn" + (owned ? " green" : P.gold < f.price ? " dis" : ""), owned ? (eq ? "✓ " + tt(ctx, "store_equipped") : tt(ctx, "store_equip")) : `${coin}<span>${fmt(f.price)}</span>`);
      b.onclick = () => owned ? (P.frame = f.id, ctx.saveP(), renderStore(ctx)) : buy(ctx, f[lang()], f.price, () => { P.owned.push(id); P.frame = f.id; });
      c.append(b); shelf.append(c);
    });
  } else {
    J.forEach(([type, price, nameK, descK, img]) => {
      const c = el("div", "jcard"), n = P.jokers[type] || 0;
      c.append(el("img", "", ""), el("div", "jt", `<b>${tt(ctx, nameK)}</b><small>${tt(ctx, descK)}</small><small>${tt(ctx, "joker_owned", n)}</small>`));
      c.firstChild.src = `img/${img}.webp`;
      const jb = el("div", "jb");
      [1, 5].forEach((q) => {
        const base = q >= 5 ? Math.floor(price * q * 85 / 100) : price * q, cost = Math.floor(base * (100 - ctx.runeFx("DISC")) / 100);
        const b = el("button", "btn" + (P.gold < cost ? " dis" : ""), `${coin}<span>${q === 1 ? tt(ctx, "joker_buy_one") : "5×"} ${fmt(cost)}</span>`);
        b.onclick = () => buy(ctx, tt(ctx, nameK) + (q > 1 ? " ×" + q : ""), cost, () => { P.jokers[type] = n + q; });
        jb.append(b);
      });
      c.children[1].append(jb); shelf.append(c);
    });
  }
}

// The ring picture with the player's face inside it, the way Android's LevelAvatar centres it.
export function frameRing(f, cls = "") {
  const box = el("div", cls); box.style.position = "relative";
  const face = el("img"); face.src = "img/avatar_dino.webp"; face.alt = "";
  face.style.cssText = `position:absolute;border-radius:50%;width:${f.face * 100}%;height:${f.face * 100}%;left:${50 + f.ox * 100 - f.face * 50}%;top:${50 + f.oy * 100 - f.face * 50}%`;
  const ring = el("img"); ring.src = `img/fr_${f.img}.webp`; ring.alt = ""; ring.style.cssText = "position:absolute;inset:0;width:100%;height:100%";
  box.append(face, ring); return box;
}

function toast(ctx, text, err) {
  const t = ctx.$("st-toast"); t.textContent = text; t.className = err ? "err" : ""; clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.add("hidden"), 2400);
}
function buy(ctx, name, price, grant) {
  const { P } = ctx;
  if (P.gold < price) return toast(ctx, tt(ctx, "store_not_enough"), true);
  ctx.confirm(tt(ctx, "store_buy_confirm_body", name, price), tt(ctx, "store_buy"), () => {
    P.gold -= price; grant(); ctx.saveP(); renderStore(ctx); ctx.refreshHome(); toast(ctx, tt(ctx, "store_bought"));
  });
}
