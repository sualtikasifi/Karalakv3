// LegalConsentHost.kt for the browser: once, after the tutorial, the player's date of birth (a neutral age screen — no date
// preselected, nothing hinting at an answer) and acceptance of the terms of use, both legal pages a tap away. Kept in this
// browser only (localStorage "legal"), never in the cloud save. A player who may be under 13 (LegalConsentStore.childMode:
// the exact age; an answer given as a year only takes the young end of it) keeps rooms, friends, duels, web accounts and a
// chosen name closed;
// Quick Match brings them approved recordings and the AI only, the league shows them the global table without ranking
// them (that takes a Google account, in the app), and nothing they draw is uploaded.
import { S, getLang } from "./i18n.js";

const KEY = "legal", TERMS_VERSION = 1, MIN_AGE = 13;
// today in Istanbul, as the app counts it: [year, month, day]
const today = () => new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" }).format(new Date()).split("-").map(Number);
const read = () => { try { return JSON.parse(localStorage.getItem(KEY) || "{}"); } catch { return {}; } };
/** LegalConsentStore.ageOf: the age today; a year-only answer (given before the date was asked) the youngest it can be. */
const ageOf = ({ y, m, d }) => {
  const [ty, tm, td] = today();
  if (!m || !d) return ty - y - 1;
  return ty - y - (tm < m || (tm === m && td < d) ? 1 : 0);
};

export const needsPrompt = () => { const l = read(); return !l.y || (l.t || 0) < TERMS_VERSION; };
export const childMode = () => { const l = read(); return !!l.y && ageOf(l) < MIN_AGE; };

/** [action] for a player aged 13 and up; for a younger one, the "for ages 13 and up" note instead (ChildGate). */
export const adultOnly = (say, action) => (...a) => (childMode() ? say(S("child_locked_body")) : action(...a));

export function openAgeGate(host, onDone) {
  if (document.getElementById("age-gate")) return;
  const lang = getLang(), wrap = document.createElement("div"); wrap.id = "age-gate";
  const box = document.createElement("div"); box.className = "box";
  const h = document.createElement("b"); h.className = "ag-h"; h.textContent = S("legal_title");
  const label = document.createElement("div"); label.className = "ag-l"; label.textContent = S("legal_birth_date_label");
  // day, month, year: each field hands over to the next once it is full
  const row = document.createElement("div"); row.className = "ag-row";
  const part = (name, hint, len) => {
    const col = document.createElement("label"); col.className = "ag-part" + (len === 4 ? " y" : "");
    const inp = Object.assign(document.createElement("input"), { className: "ag-in", inputMode: "numeric", maxLength: len, placeholder: S(hint), autocomplete: "off" });
    col.append(Object.assign(document.createElement("span"), { textContent: S(name) }), inp); row.append(col); return inp;
  };
  const dd = part("legal_birth_day", "legal_birth_day_hint", 2), mm = part("legal_birth_month", "legal_birth_month_hint", 2), yy = part("legal_birth_year", "legal_birth_year_hint", 4);
  const why = document.createElement("div"); why.className = "ag-why"; why.textContent = S("legal_birth_date_why");
  // "By continuing you accept the %1$s and confirm you have read the %2$s." with both names as links.
  const accept = document.createElement("p"); accept.className = "ag-acc";
  const link = (key, path) => Object.assign(document.createElement("a"), { textContent: S(key), href: `/${path}?lang=${lang}`, target: "_blank", rel: "noopener" });
  S("legal_accept_text", "\u0001", "\u0002").split(/(\u0001|\u0002)/).forEach((part) => {
    accept.append(part === "\u0001" ? link("legal_terms_link", "kosullar") : part === "\u0002" ? link("legal_privacy_link", "gizlilik") : document.createTextNode(part));
  });
  const go = document.createElement("button"); go.className = "pill orange small dis";
  go.append(Object.assign(document.createElement("span"), { textContent: S("legal_continue") }));
  const complete = () => dd.value.length > 0 && mm.value.length > 0 && yy.value.length === 4;
  [[dd, 2, mm], [mm, 2, yy], [yy, 4, null]].forEach(([inp, len, next]) => {
    inp.oninput = () => {
      inp.value = inp.value.replace(/\D/g, "").slice(0, len);
      if (next && inp.value.length === len) next.focus();
      go.classList.toggle("dis", !complete()); why.classList.remove("err"); why.textContent = S("legal_birth_date_why");
    };
    inp.onkeydown = (e) => { if (e.key === "Enter") go.click(); };
  });
  go.onclick = () => {
    const y = Number(yy.value), m = Number(mm.value), d = Number(dd.value), [ty, tm, td] = today();
    const real = m >= 1 && m <= 12 && d >= 1 && d <= new Date(Date.UTC(y, m, 0)).getUTCDate();
    const past = y < ty || (y === ty && (m < tm || (m === tm && d <= td)));
    if (!(complete() && real && past && y >= ty - 110)) { why.classList.add("err"); why.textContent = S("legal_birth_date_invalid"); return; }
    try { localStorage.setItem(KEY, JSON.stringify({ y, m, d, t: TERMS_VERSION })); } catch {}
    wrap.remove(); onDone && onDone();
  };
  box.append(h, label, row, why, accept, go); wrap.append(box); host.append(wrap);
  setTimeout(() => dd.focus(), 50);
}
