// The rank ladder window (RankLadderSheet in MainMenuScreen.kt): every rank, the level it opens at and where the player stands.
import { S } from "./i18n.js";
import { el } from "./ui.js";
const TIERS = [[1, "✏️", "rank_karalamaci"], [20, "🖊️", "rank_cirak"], [40, "🖌️", "rank_ressam"], [60, "🎨", "rank_usta_ressam"], [80, "🖼️", "rank_sanatci"], [100, "👑", "rank_buyuk_usta"]];
const totalXpForLevel = (l) => { const n = Math.max(l - 1, 0); return 25 * n * n + 75 * n; };
/** The rank's string key for a level (LevelTier.forLevel(level).rank in the app). */
export const rankKey = (level) => [...TIERS].reverse().find((t) => level >= t[0])[2];
/**
 * RankLevelLabel: "N. Seviye" and the rank's name taking turns in the same spot every five seconds with a soft cross-fade
 * (no sliding), as in league rows, the lobby and the account page. [bullet] puts "· " in front of both.
 */
export const rankLevelHtml = (level, bullet = false) => {
  const b = bullet ? "· " : "";
  return `<span class="rkl" style="--rk:-${Date.now() % 10000}ms"><i>${b}${S("home_level_inline", level)}</i><i>${b}${S(rankKey(level))}</i></span>`;
};
/**
 * The rank ladder page (RankLadderDialog.kt): one painted scene (img/rank_ladder_bg: the workshop, the ribbon, the close button and six
 * empty rows with each rank's picture) with the words laid over it in the picture's own 841 x 1870 units. The current rank's row is
 * tinted and rimmed in orange, the ranks still ahead are veiled. The scene fits the window whole; its edges are a blurred copy.
 */
export function openRankLadder(c) {
  const lv = c.levelOf(c.P.xp).level, cur = [...TIERS].reverse().find((t) => lv >= t[0]);
  const host = c.$("stage"), ov = el("div", "rk-scene"), board = el("div", "rk-board");
  ov.append(el("div", "rk-blur"), board); host.append(ov);
  const at = (x, y, w, h) => `left:calc(var(--k)*${x});top:calc(var(--k)*${y});width:calc(var(--k)*${w});height:calc(var(--k)*${h})`;
  const txt = (cls, x, y, w, h, fs, html) => `<div class="rk-t ${cls}" data-fs="${fs}" style="${at(x, y, w, h)}"><span>${html}</span></div>`;
  const close = () => { removeEventListener("resize", fit); ov.remove(); };
  let rows = "";
  TIERS.forEach(([min, , key], i) => {
    const reached = lv >= min, isCur = min === cur[0], away = Math.max(0, totalXpForLevel(min) - c.P.xp), top = 496 + 172 * i;
    if (isCur) rows += `<div class="rk-cur" style="${at(91, top, 658, 154)}"></div><div class="rk-rim" style="${at(89, top - 2, 662, 158)}"></div>`;
    else if (!reached) rows += `<div class="rk-veil" style="${at(91, top, 658, 154)}"></div>`;
    const dim = reached ? "" : " dim";
    const pill = isCur ? ["cur", S("rank_ladder_current")] : reached ? ["ok", "✓ " + S("rank_ladder_reached")] : ["ahead", S("rank_ladder_remaining", away)];
    rows += txt("nm" + dim, 318, top + 22, 222, 62, 46, S(key)) + txt("lv" + dim, 318, top + 84, 222, 46, 30, S("rank_ladder_unlock_level", min)) +
      `<div class="rk-pill ${pill[0]}" data-fs="27" style="${at(548, top + 47, 189, 60)}"><span>${pill[1]}</span></div>`;
  });
  board.innerHTML = `<img class="rk-bg" src="img/rank_ladder_bg.webp" alt="">
    ${txt("lt", 205, 268, 430, 90, 86, S("rank_ladder_title"))}
    ${txt("sub", 182, 408, 478, 82, 31, S("rank_ladder_subtitle", S(cur[2])))}
    ${rows}
    <button class="rk-x" style="${at(728, 338, 84, 86)}" aria-label="${S("close")}"></button>`;
  board.querySelector(".rk-x").onclick = close;
  // The whole picture is shown (letterboxed), centred; a word that does not fit its box (another language, a big number) shrinks until it does.
  function fit() {
    const R = ov.getBoundingClientRect(), k = Math.min(R.width / 841, R.height / 1870);
    board.style.setProperty("--k", k + "px"); board.style.width = 841 * k + "px"; board.style.height = 1870 * k + "px";
    board.style.left = (R.width - 841 * k) / 2 + "px"; board.style.top = (R.height - 1870 * k) / 2 + "px";
    board.querySelectorAll("[data-fs]").forEach((e) => {
      const t = e.querySelector("span") || e; let f = +e.dataset.fs * k; const min = f * 0.5; e.style.fontSize = f + "px";
      while ((t.scrollHeight > e.clientHeight + 1 || t.scrollWidth > e.clientWidth + 1) && f > min) { f *= 0.94; e.style.fontSize = f + "px"; }
    });
  }
  addEventListener("resize", fit); fit(); document.fonts && document.fonts.ready.then(fit);
}
