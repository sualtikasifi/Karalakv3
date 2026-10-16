import { S } from "./i18n.js";
import { appWindow } from "./ui.js";
import { penList, frameList, frameRing } from "./store.js";
const T = { tr: { pen: "Kalemini seç", frame: "Çerçeveni seç", close: "Kapat", nick: "Kullanıcı adı", save: "Kaydet", nickHint: "2-16 karakter. Adını yalnızca bir kez değiştirebilirsin.", nickLocked: "Kullanıcı adını zaten bir kez değiştirdin.", nick_taken: "Bu ad alınmış, başka bir tane dene.", nick_invalid: "Harf, rakam, boşluk, _ . - kullanabilirsin (2-16).", nick_net: "Sunucuya ulaşılamadı.", locked: (n) => `Seviye ${n}` }, en: { pen: "Choose your pen", frame: "Choose your frame", close: "Close", nick: "Username", save: "Save", nickHint: "2-16 characters. You can change your name only once.", nickLocked: "You have already changed your username once.", nick_taken: "That name is taken, try another.", nick_invalid: "Letters, numbers, spaces and _ . - only (2-16).", nick_net: "Couldn't reach the server.", locked: (n) => `Level ${n}` } };
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
function sheet(c, title, build) {
  const d = c.$("pick"), x = T[c.lang()]; d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "pk-box", `<b>${title}</b>`); const grid = el("div", "pk-grid"); build(grid, () => d.classList.add("hidden")); box.append(grid);
  const close = el("button", "pill cream small", `<span>${x.close}</span>`); close.onclick = () => d.classList.add("hidden"); box.append(close);
  d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
// PenPickerSheet: every pen, locked ones dimmed with what opens them (level / store / rune map / league prize).
export function openPenPicker(c) {
  const { P } = c, lv = c.levelOf(P.xp).level, body = el("div", "pp-grid");
  const ov = appWindow(c.$("stage"), { title: S("pen_picker_title"), body });
  const owns = (p) => (P.owned || []).includes("pen:" + p.id);
  penList().filter((p) => !p.retired || owns(p)).forEach((p) => {
    const open = p.league ? owns(p) : p.price > 0 || p.rune ? owns(p) : lv >= p.level;
    const grad = p.colors.length > 1 ? `linear-gradient(90deg,${p.colors.join(",")})` : p.colors[0];
    const why = p.rune ? S("cosmetic_locked_rune") : p.price > 0 ? S("store_in_store") : p.league ? S("cosmetic_locked_league") : S("avatar_frame_locked_level", p.level);
    const b = el("button", "pp-card" + (P.pen === p.id ? " on" : "") + (open ? "" : " lock"), `<svg viewBox="0 0 100 34"><defs><linearGradient id="pg-${p.id}" x1="12" x2="88" y1="0" y2="0" gradientUnits="userSpaceOnUse">${p.colors.map((k, i) => `<stop offset="${p.colors.length > 1 ? i / (p.colors.length - 1) : 0}" stop-color="${k}"/>`).join("")}</linearGradient></defs><line x1="12" y1="23" x2="88" y2="11" stroke="url(#pg-${p.id})" stroke-width="12" stroke-linecap="round"/></svg><b>${p[c.lang()]}</b>${open ? "" : `<small>${why}</small>`}`);
    b.onclick = () => { if (!open) return; P.pen = p.id; c.saveP(); c.refreshHome(); ov.close(); };
    body.append(b);
  });
}
// AvatarFramePickerSheet: "Profil görseli" (Karalak / Google photo) and the ring grid; locked rings are faded with a padlock and
// the level (or "Mağazada") they open at.
export function openFramePicker(c) {
  const { P } = c, lv = c.levelOf(P.xp).level, host = c.$("stage");
  const body = el("div", "fp-body");
  const lock = '<svg viewBox="0 0 24 24"><path d="M7 11V8a5 5 0 0110 0v3" fill="none" stroke="#2b2b2b" stroke-width="2.6" stroke-linecap="round"/><rect x="4.5" y="10.5" width="15" height="11" rx="2.6" fill="#2b2b2b"/></svg>';
  const ov = appWindow(host, { title: S("avatar_frame_picker_title"), body });
  body.append(el("div", "fp-h", S("avatar_source_title")));
  const src = el("div", "fp-src");
  const kar = el("button", "", `<span class="av"><img src="img/avatar_dino.webp" alt=""></span><b>${S("avatar_source_karalak")}</b>`);
  src.append(kar); body.append(src);
  body.append(el("div", "fp-h", S("avatar_frame_section")));
  const grid = el("div", "fp-grid");
  frameList().filter((f) => !f.league && !f.retired && (f.level > 0 || f.price > 0 || P.owned.includes("frame:" + f.id) || f.id === "SCRIBBLER")).forEach((f) => {
    const own = f.price > 0 ? P.owned.includes("frame:" + f.id) : f.level <= lv;
    const b = el("button", "fp-card" + (P.frame === f.id ? " on" : "") + (own ? "" : " lock"), "");
    const r = frameRing(f, "fp-ring"); r.querySelector("img:first-child").style.display = f.level === 0 ? "none" : "none"; b.append(r);
    if (!own) b.append(el("div", "fp-lk", `${lock}<span>${f.price > 0 ? S("store_in_store") : S("avatar_frame_locked_level", f.level)}</span>`));
    b.onclick = () => { if (!own) return; P.frame = f.id; c.saveP(); c.refreshHome(); ov.close(); };
    grid.append(b);
  });
  body.append(grid);
}
export function openNickEdit(c) {
  const { P, lang } = c, x = T[lang()], d = c.$("pick"); d.classList.remove("hidden"); d.replaceChildren();
  const locked = (P.nameChanges || 0) >= 1;
  const box = el("div", "pk-box", `<b>${x.nick}</b>`), inp = el("input", "pk-in"); inp.maxLength = 16; inp.value = P.nick; inp.disabled = locked;
  const note = el("p", "pk-note", locked ? x.nickLocked : x.nickHint);
  const save = el("button", "pill orange small", `<span>${x.save}</span>`); save.style.display = locked ? "none" : "";
  save.onclick = async () => {
    const v = inp.value.trim().replace(/\s+/g, " "); if (v === P.nick) { d.classList.add("hidden"); return; }
    note.textContent = "…"; const r = await c.net.claimUsername(v, P.nickClaimed ? P.nick : null);
    if (r === "ok") { P.nick = v; P.nickClaimed = true; P.nameChanges = (P.nameChanges || 0) + 1; c.saveP(); c.refreshHome(); d.classList.add("hidden"); }
    else note.textContent = x["nick_" + r] || x.nick_net;
  };
  const close = el("button", "pill cream small", `<span>${x.close}</span>`); close.onclick = () => d.classList.add("hidden");
  box.append(inp, note, save, close); d.append(box); d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); }; if (!locked) setTimeout(() => inp.focus(), 50);
}
