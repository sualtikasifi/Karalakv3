// The rank ladder window (RankLadderSheet in MainMenuScreen.kt): every rank, the level it opens at and where the player stands.
import { S } from "./i18n.js";
import { el, appWindow } from "./ui.js";
const TIERS = [[1, "✏️", "rank_karalamaci"], [20, "🖊️", "rank_cirak"], [40, "🖌️", "rank_ressam"], [60, "🎨", "rank_usta_ressam"], [80, "🖼️", "rank_sanatci"], [100, "👑", "rank_buyuk_usta"]];
const totalXpForLevel = (l) => { const n = Math.max(l - 1, 0); return 25 * n * n + 75 * n; };
export function openRankLadder(c) {
  const lv = c.levelOf(c.P.xp).level, cur = [...TIERS].reverse().find((t) => lv >= t[0]);
  const body = el("div", "rl-body", `<p>${S("rank_ladder_subtitle", S(cur[2]))}</p>`);
  TIERS.forEach(([min, emoji, key]) => {
    const reached = lv >= min, isCur = min === cur[0], away = Math.max(0, totalXpForLevel(min) - c.P.xp);
    body.append(el("div", "rl-row" + (isCur ? " cur" : "") + (reached ? "" : " dim"), `<span class="e">${emoji}</span><div><b>${S(key)}</b><small>${S("rank_ladder_unlock_level", min)}</small></div><em>${isCur ? S("rank_ladder_current") : reached ? S("rank_ladder_reached") : S("rank_ladder_remaining", away)}</em>`));
  });
  appWindow(c.$("stage"), { title: S("rank_ladder_title"), body });
}
