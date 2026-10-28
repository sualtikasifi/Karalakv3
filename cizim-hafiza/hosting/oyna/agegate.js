// LegalConsentHost.kt for the browser: once, after the tutorial, the player's year of birth (a neutral age screen — no year
// preselected, nothing hinting at an answer) and acceptance of the terms of use, both legal pages a tap away. Kept in this
// browser only (localStorage "legal"), never in the cloud save. A player who may be under 13 (LegalConsentStore.childMode:
// only a year is asked, so the young end of it counts) keeps online play, friends, the league, web accounts and a chosen
// name closed, and nothing they draw is uploaded.
import { S, getLang } from "./i18n.js";

const KEY = "legal", TERMS_VERSION = 1, MIN_AGE = 13;
const year = () => Number(new Intl.DateTimeFormat("en", { timeZone: "Europe/Istanbul", year: "numeric" }).format(new Date()));
const read = () => { try { return JSON.parse(localStorage.getItem(KEY) || "{}"); } catch { return {}; } };

export const needsPrompt = () => { const l = read(); return !l.y || (l.t || 0) < TERMS_VERSION; };
export const childMode = () => { const y = read().y; return !!y && year() - y - 1 < MIN_AGE; };

/** [action] for a player aged 13 and up; for a younger one, the "for ages 13 and up" note instead (ChildGate). */
export const adultOnly = (say, action) => (...a) => (childMode() ? say(S("child_locked_body")) : action(...a));

export function openAgeGate(host, onDone) {
  if (document.getElementById("age-gate")) return;
  const lang = getLang(), wrap = document.createElement("div"); wrap.id = "age-gate";
  const box = document.createElement("div"); box.className = "box";
  const h = document.createElement("b"); h.className = "ag-h"; h.textContent = S("legal_title");
  const label = document.createElement("div"); label.className = "ag-l"; label.textContent = S("legal_birth_year_label");
  const inp = document.createElement("input"); inp.className = "ag-in"; inp.inputMode = "numeric"; inp.maxLength = 4; inp.placeholder = S("legal_birth_year_placeholder"); inp.autocomplete = "off";
  const why = document.createElement("div"); why.className = "ag-why"; why.textContent = S("legal_birth_year_why");
  // "By continuing you accept the %1$s and confirm you have read the %2$s." with both names as links.
  const accept = document.createElement("p"); accept.className = "ag-acc";
  const link = (key, path) => Object.assign(document.createElement("a"), { textContent: S(key), href: `/${path}?lang=${lang}`, target: "_blank", rel: "noopener" });
  S("legal_accept_text", "\u0001", "\u0002").split(/(\u0001|\u0002)/).forEach((part) => {
    accept.append(part === "\u0001" ? link("legal_terms_link", "kosullar") : part === "\u0002" ? link("legal_privacy_link", "gizlilik") : document.createTextNode(part));
  });
  const go = document.createElement("button"); go.className = "pill orange small dis";
  go.append(Object.assign(document.createElement("span"), { textContent: S("legal_continue") }));
  inp.oninput = () => { inp.value = inp.value.replace(/\D/g, "").slice(0, 4); go.classList.toggle("dis", inp.value.length !== 4); why.classList.remove("err"); why.textContent = S("legal_birth_year_why"); };
  go.onclick = () => {
    const y = Number(inp.value);
    if (!(inp.value.length === 4 && y >= year() - 110 && y <= year())) { why.classList.add("err"); why.textContent = S("legal_birth_year_invalid"); return; }
    try { localStorage.setItem(KEY, JSON.stringify({ y, t: TERMS_VERSION })); } catch {}
    wrap.remove(); onDone && onDone();
  };
  inp.onkeydown = (e) => { if (e.key === "Enter") go.click(); };
  box.append(h, label, inp, why, accept, go); wrap.append(box); host.append(wrap);
  setTimeout(() => inp.focus(), 50);
}
