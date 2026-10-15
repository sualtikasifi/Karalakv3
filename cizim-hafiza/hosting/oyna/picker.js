import { penList, frameList, frameRing } from "./store.js";
const T = { tr: { pen: "Kalemini seç", frame: "Çerçeveni seç", close: "Kapat", nick: "Kullanıcı adı", save: "Kaydet", locked: (n) => `Seviye ${n}` }, en: { pen: "Choose your pen", frame: "Choose your frame", close: "Close", nick: "Username", save: "Save", locked: (n) => `Level ${n}` } };
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
function sheet(c, title, build) {
  const d = c.$("pick"), x = T[c.lang()]; d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "pk-box", `<b>${title}</b>`); const grid = el("div", "pk-grid"); build(grid, () => d.classList.add("hidden")); box.append(grid);
  const close = el("button", "pill cream small", `<span>${x.close}</span>`); close.onclick = () => d.classList.add("hidden"); box.append(close);
  d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
export function openPenPicker(c) {
  const { P, lang } = c, x = T[lang()], lv = c.levelOf(P.xp).level;
  sheet(c, x.pen, (grid, done) => {
    penList().filter((p) => (p.level > 0 && !p.league) || P.owned.includes("pen:" + p.id)).forEach((p) => {
      const open = p.level === 0 || p.level <= lv || P.owned.includes("pen:" + p.id);
      const grad = p.colors.length > 1 ? `linear-gradient(90deg,${p.colors.join(",")})` : p.colors[0];
      const b = el("button", "pk-pen" + (P.pen === p.id ? " on" : "") + (open ? "" : " lock"), `<i style="background:${grad}"></i><span>${p[lang()]}</span>${open ? "" : `<small>🔒 ${x.locked(p.level)}</small>`}`);
      b.onclick = () => { if (!open) return; P.pen = p.id; c.saveP(); c.refreshHome(); done(); };
      grid.append(b);
    });
  });
}
export function openFramePicker(c) {
  const { P, lang } = c, x = T[lang()], lv = c.levelOf(P.xp).level;
  sheet(c, x.frame, (grid, done) => {
    frameList().filter((f) => (f.level > 0 && !f.league) || P.owned.includes("frame:" + f.id)).forEach((f) => {
      const open = f.level === 0 || f.level <= lv || P.owned.includes("frame:" + f.id);
      const b = el("button", "pk-fr" + (P.frame === f.id ? " on" : "") + (open ? "" : " lock"), "");
      const r = frameRing(f, "pk-ring"); b.append(r, el("span", "", f[lang()]));
      if (!open) b.append(el("small", "", "🔒 " + x.locked(f.level)));
      b.onclick = () => { if (!open) return; P.frame = f.id; c.saveP(); c.refreshHome(); done(); };
      grid.append(b);
    });
  });
}
export function openNickEdit(c) {
  const { P, lang } = c, x = T[lang()], d = c.$("pick"); d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "pk-box", `<b>${x.nick}</b>`), inp = el("input", "pk-in"); inp.maxLength = 16; inp.value = P.nick;
  const save = el("button", "pill orange small", `<span>${x.save}</span>`);
  save.onclick = () => { const v = inp.value.trim().slice(0, 16); if (v.length >= 2) { P.nick = v; c.saveP(); c.refreshHome(); } d.classList.add("hidden"); };
  const close = el("button", "pill cream small", `<span>${x.close}</span>`); close.onclick = () => d.classList.add("hidden");
  box.append(inp, save, close); d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); }; setTimeout(() => inp.focus(), 50);
}
