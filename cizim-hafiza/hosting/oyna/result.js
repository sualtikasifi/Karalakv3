// Result screen (port of ResultScreen.kt / ResultParts.kt): painted head, stat cards, level card, versus card,
// daily card, the words board with every drawing, claim buttons, and the drawing preview scene.
import { S as appS, locale } from "./i18n.js";
let S = null;
const tr = (k, lang, ...a) => ((S && S[k] && S[k][lang]) || k).replace(/\{(\d)\}/g, (_, i) => a[+i]);
export async function loadResultStrings() { if (!S) S = await (await fetch("result_strings.json")).json(); }
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
export const cap = (w, lang) => w ? w.charAt(0).toLocaleUpperCase(lang) + w.slice(1) : "";
const ICON = { check: "rs_ic_check", x: "rs_ic_x", star: "rs_ic_star", crown: "rs_ic_crown", coin: "icon_gold_coin", trophy: "icon_league_trophy", cal: "daily_calendar_icon", rune: "rune_root", event: "rs_ic_star" };

function statList(d, lang) {
  const T = (k, ...a) => tr(k, lang, ...a), list = [
    { ic: "check", label: T("result_stat_correct"), value: String(d.n), color: "#2ea043" },
    { ic: "x", label: T("result_stat_wrong"), value: String(d.total - d.n), color: "#e23b32" }];
  const or = "#e8650f", gold = (g) => T("result_gold_amount", g);
  if (d.stars != null) list.push({ ic: "star", label: T("result_stat_stars"), value: "★".repeat(d.stars) + "☆".repeat(Math.max(0, 3 - d.stars)), color: or });
  // The same rows, in the same order, as the Android result screen (ResultParts.kt).
  const g = d.gold || {};
  if (g.play > 0) list.push({ ic: "coin", label: T("result_stat_play_gold"), value: gold(g.play), color: or });
  if (g.achievements > 0) list.push({ ic: "trophy", label: T("result_stat_achievement"), value: gold(g.achievements), color: or });
  if (g.level > 0) list.push({ ic: "crown", label: T("result_stat_level_gold"), value: gold(g.level), color: or });
  if (g.daily > 0) list.push({ ic: "cal", label: T("result_stat_daily_gold"), value: gold(g.daily), color: or });
  if (g.win > 0) list.push({ ic: "trophy", label: T("result_stat_win_gold"), value: gold(g.win), color: or });
  if (g.runeBonus > 0) list.push({ ic: "rune", label: T("result_stat_rune_bonus"), value: gold(g.runeBonus), color: or });
  if (d.eventMult > 1) list.push({ ic: "event", label: T("result_stat_event"), value: d.eventMult >= 2 - 1e-6 ? T("result_stat_multiplier", Math.round(d.eventMult)) : T("result_stat_bonus", Math.round((d.eventMult - 1) * 100)), color: or });
  if (d.daily == null) list.push({ ic: "star", label: "", value: T("result_xp_won", d.xp), color: "#f2541b", headline: true });
  return list;
}

export function renderResult(c, d) {
  const { $, lang: gl } = c, lang = gl(), T = (k, ...a) => tr(k, lang, ...a);
  const root = $("rs-body"), foot = $("rs-foot"); root.replaceChildren(); foot.replaceChildren();
  const sc = $("rs-scroll"); sc.scrollTop = 0;
  // head
  // the back button is not in the head (which scrolls with the page) but laid over the screen, so it stays in its corner
  const head = el("div", "rs-head", `<img src="img/rs_head_plain.webp" alt=""><div class="rs-title lt">${T("game_over")}</div>`);
  $("rs-fback").onclick = d.onHome; root.append(head);
  const inner = el("div", "rs-inner"); root.append(inner);
  // Plain text: the note can carry another player's name (the duel opponent's), which must never be read as markup.
  if (d.note) { const note = el("div", "rs-note"); note.textContent = d.note; inner.append(note); }
  // stats
  const grid = el("div", "rs-grid"); const sl = statList(d, lang);
  for (let i = 0; i < sl.length; i += 2) {
    const pair = sl.slice(i, i + 2), row = el("div", "rs-row" + (pair.length === 1 ? " one" : ""));
    pair.forEach((s, j) => {
      const right = pair.length === 1 || j === 1, card = el("div", "rs-card " + (right ? "ra" : "rb") + (s.headline ? " hl" : ""));
      card.innerHTML = s.headline ? `<b style="color:${s.color}">${s.value}</b>` : `<img src="img/${ICON[s.ic]}.webp" alt=""><div class="tx"><span>${s.label}</span><b style="color:${s.color}">${s.value}</b></div>`;
      row.append(card);
    });
    grid.append(row);
  }
  inner.append(grid);
  // level card
  if (d.level) {
    const L = d.level, up = L.before < L.level, card = el("div", "rs-level", `<img class="bgi" src="img/rs_level_blank.webp" alt=""><img class="crown" src="img/lobby_crown.webp" alt=""><div class="lv">${T("result_level_label", L.level)}</div><div class="nx">${up ? `<i>${T("result_level_up")}</i>` : L.max ? "" : T("home_xp_to_next", L.toNext, L.level + 1)}</div><div class="bar"><i class="fill"></i><em></em><span>${T("home_xp_format", L.into, L.need)}</span></div>`);
    const fill = card.querySelector(".fill"); fill.style.width = (up ? 4 : Math.max(4, L.beforeFrac * 100)) + "%"; inner.append(card);
    requestAnimationFrame(() => requestAnimationFrame(() => { fill.style.width = Math.max(4, L.frac * 100) + "%"; }));
  }
  // versus card
  if (d.opp) {
    const me = d.score, op = d.opp.totalScore, won = me > op, drew = me === op;
    // The opponent's name is another player's (a recorded round's nickname), so it is escaped like every other name.
    const side = (name, score, ring, right, col) => `<div class="av ${right ? "r" : "l"}"></div><div class="nm ${right ? "r" : "l"}">${esc(name)}</div><div class="sc ${right ? "r" : "l"}" style="color:${col}">${esc(score)}</div>`;
    const v = el("div", "rs-vs", `${side(d.nick || T("quick_match_you"), me, null, false, won ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f")}${side(d.opp.nickname || "?", op, null, true, !won && !drew ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f")}<div class="out" style="background:${drew ? "#8a6a50" : won ? "#2ea043" : "#d23b2e"}">${T(drew ? "quick_match_drew" : won ? "quick_match_won" : "quick_match_lost")}</div><div class="vs">${T("quick_match_versus")}</div>${won ? '<img class="cr" src="img/lobby_crown.webp" alt="">' : ""}`);
    const a1 = d.myRing(), a2 = d.oppRing(); v.querySelector(".av.l").append(a1); v.querySelector(".av.r").append(a2);
    inner.append(v);
    if (d.rematch) { const b = el("button", "rs-next", `<span>${d.rematchLabel}</span>`); b.onclick = d.rematch; inner.append(b); }
  }
  // daily
  if (d.daily) {
    const D = d.daily, pips = d.flags.map((f) => `<i class="${f ? "ok" : "no"}">${f ? "✓" : "✗"}</i>`).join("");
    inner.append(el("div", "rs-daily", `<h4>${T("daily_challenge_result_title")}</h4><div class="pips">${pips}</div><div class="pl"><div><span>🔥 <b>${D.streak}</b></span><small>${T("daily_streak_label")}</small></div><div><span>⭐ <b>${T("daily_challenge_xp_earned", D.xp)}</b></span><small>${T("daily_reward_won")}</small></div></div><button class="rs-share"><svg viewBox="0 0 24 24"><path fill="#fff" d="M18 16.1c-.8 0-1.5.3-2 .8l-7.1-4.2a3 3 0 000-1.4L16 7.2A3 3 0 1015 5c0 .2 0 .4.1.7L8 9.9a3 3 0 100 4.2l7.1 4.2c-.1.2-.1.5-.1.7a3 3 0 103-2.9z"/></svg><span class="lt">${T("daily_challenge_share")}</span></button>`));
    // DailyChallengeShareUtil: spoiler-free, a ✅/❌ row and the app's own words, never the words of the day or the drawings
    inner.lastChild.querySelector(".rs-share").onclick = async () => {
      const date = new Date().toLocaleDateString(lang === "en" ? "en-GB" : locale(), { day: "numeric", month: "long", year: "numeric" });
      const txt = d.flags.map((f) => (f ? "✅" : "❌")).join("") + "\n" + appS("daily_challenge_share_text", date, d.flags.filter(Boolean).length, d.flags.length, D.streak, "https://karalak.com/indir");
      try { if (navigator.share) await navigator.share({ text: txt }); else await navigator.clipboard.writeText(txt); } catch {}
    };
  }
  // words board
  const cfg = { title: T("result_words_title", d.items.length), sel: "me", placeholders: d.items.length, pen: d.pen, items: (sel) => (sel === "opp" ? d.oppItems : d.items), theirs: (sel) => sel === "opp",
    chips: d.opp ? [{ id: "me", label: d.nick || T("quick_match_gallery_yours"), enabled: true }, { id: "opp", label: d.opp.nickname || "?", enabled: d.oppItems.length > 0 }] : [] };
  inner.append(board(c, cfg, lang, T));
  // footer buttons
  if (d.next) { const b = el("button", "rs-next", `<span>⟳ ${d.next.label}</span>`); b.onclick = d.next.fn; foot.append(b); }
  const row = el("div", "rs-btns");
  const cl = el("button", "rs-art", `<span class="l1 lt">${T("result_claim_amount", d.xp)}</span>`); cl.onclick = d.onHome;
  row.append(cl); row.classList.add("one"); foot.append(row);
  if (d.xp > 0 && d.n > 0) confetti(c.$("res2"));
}

// cfg: { title, chips:[{id,label,enabled}] (empty = plain title), sel, items(sel) -> [], placeholders, onSelect(id), theirs(sel) -> bool, pen }
function board(c, cfg, lang, T) {
  const wrap = el("div", "rs-board");
  // Every side's sheet grid (yours AND the opponent's) is built and painted up front and stacked in the same place: choosing a name
  // only shows the other grid, so the opponent's drawings are there the instant a name is tapped.
  const grids = {};
  const sheetsFor = (id) => {
    const items = cfg.items(id), total = items.length || cfg.placeholders, rows = Math.ceil(total / 5), g = el("div", "rs-sheets" + (rows > 2 ? " scr" : ""));
    for (let i = 0; i < total; i++) {
      const it = items[i], sh = el("div", "sheet" + (it ? "" : " empty"));
      if (it) {
        sh.innerHTML = `<div class="pg"><i class="dots"></i><em>${i + 1}</em><canvas></canvas><b>${esc(cap(it.word, lang))}</b></div><img class="mk" src="img/${it.isCorrect ? "rs_ic_check" : "rs_ic_x"}.webp" alt="">`;
        sh.onclick = () => preview(c, cfg, items, i, lang, T, cfg.theirs ? cfg.theirs(id) : false);
      }
      g.append(sh);
    }
    return g;
  };
  const show = () => { for (const id of Object.keys(grids)) grids[id].classList.toggle("hid", id !== cfg.sel); };
  const draw = () => {
    wrap.replaceChildren(); wrap.append(el("div", "bd-art"));
    const plank = el("div", "plank"), chipEls = {};
    if (cfg.chips && cfg.chips.length) {
      cfg.chips.forEach((ch) => {
        const b = el("button", "chip", `<span class="lt">${esc(ch.label)}</span>`); chipEls[ch.id] = b;
        b.style.flex = Math.max(5, ch.label.length); if (cfg.chips.length > 2) { b.style.flex = "none"; b.style.width = "calc(var(--k)*110)"; }
        b.onclick = () => { if (!ch.enabled || cfg.sel === ch.id) return; cfg.sel = ch.id; cfg.onSelect && cfg.onSelect(ch.id); paintChips(); show(); };
        plank.append(b);
      });
      if (cfg.chips.length > 2) plank.classList.add("scrl");
    } else plank.append(el("span", "lt", cfg.title));
    const paintChips = () => cfg.chips.forEach((ch) => { const b = chipEls[ch.id]; b.classList.toggle("sel", ch.id === cfg.sel); b.classList.toggle("dis", !ch.enabled && ch.id !== cfg.sel); });
    wrap.append(plank);
    const ids = cfg.chips && cfg.chips.length ? cfg.chips.filter((ch) => ch.enabled || ch.id === cfg.sel).map((ch) => ch.id) : [cfg.sel];
    for (const id of ids) { grids[id] = sheetsFor(id); wrap.append(grids[id]); }
    if (cfg.chips && cfg.chips.length) paintChips();
    show();
    requestAnimationFrame(() => { for (const id of Object.keys(grids)) { const items = cfg.items(id); grids[id].querySelectorAll("canvas").forEach((cv, i) => { c.fitCanvas(cv); c.paint(cv, items[i].strokes, false, items[i].penSkin || cfg.pen || "CLASSIC"); }); } });
  };
  cfg.redraw = draw; draw(); return wrap;
}
const esc = (s) => String(s).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[ch]);

function preview(c, d, items, idx, lang, T, theirs) {
  const { $ } = c, pk = $("pick"); pk.classList.remove("hidden"); pk.replaceChildren(); pk.onclick = null;
  const st = $("stage") || pk.parentElement, W = st.clientWidth, H = st.clientHeight, unit = Math.max(W / 841, H / 1870), offX = (W - unit * 841) / 2, offY = (H - unit * 1870) / 2;
  const scene = el("div", "pv"); pk.append(scene);
  const box = (x0, y0, x1, y1) => `left:${offX + unit * x0}px;top:${offY + unit * y0}px;width:${unit * (x1 - x0)}px;height:${unit * (y1 - y0)}px`;
  const cur = { i: idx }, close = () => { pk.classList.add("hidden"); cancelAnimationFrame(cur.raf); };
  const show = () => {
    scene.replaceChildren(); const it = items[cur.i];
    const bg = el("img", "pv-bg"); bg.src = "img/rs_dialog_bg.webp"; bg.style.cssText = box(0, 0, 841, 1870); scene.append(bg);
    const word = el("div", "pv-word lt", cap(it.word, lang)); word.style.cssText = box(190, 462, 660, 532) + `;font-size:${54 * unit}px`; scene.append(word);
    const cv = el("canvas", "pv-cv"); cv.style.cssText = box(188, 590, 648, 1050) + `;border-radius:${26 * unit}px`; scene.append(cv); c.fitCanvas(cv);
    const pen = it.penSkin || d.pen || "CLASSIC", all = it.strokes, total = all.reduce((a, s) => a + s.length, 0);
    const play = () => {
      cancelAnimationFrame(cur.raf); const t0 = performance.now(), dur = Math.min(4000, Math.max(1500, total * 18));
      const step = (now) => {
        const p = Math.min(1, (now - t0) / dur); let k = Math.floor(total * p), part = [];
        for (const s of all) { if (k <= 0) break; part.push(s.slice(0, k)); k -= s.length; }
        c.paint(cv, part, false, pen, all); if (p < 1) cur.raf = requestAnimationFrame(step);
      };
      cur.raf = requestAnimationFrame(step);
    };
    play();
    const mkBtn = (r, fn, label) => { const b = el("button", "pv-hit", ""); b.style.cssText = box(...r); b.setAttribute("aria-label", label); b.onclick = fn; scene.append(b); return b; };
    mkBtn([704, 188, 828, 316], close, T("close")); mkBtn([650, 1000, 800, 1140], play, T("replay_drawing"));
    const act = mkBtn([150, 1205, 692, 1345], () => { if (!theirs) shareDrawing(c, it, cv); }, ""); act.className += " pv-act lt"; act.style.fontSize = 54 * unit + "px"; act.textContent = T(theirs ? "report_drawing_action" : "share_drawing");
    if (items.length > 1) {
      const arrow = (r, left, en) => { const b = mkBtn(r, () => { cur.i += left ? -1 : 1; show(); }, ""); b.className += " pv-ar" + (en ? "" : " dis"); b.textContent = left ? "‹" : "›"; b.style.fontSize = 64 * unit + "px"; if (!en) b.onclick = null; };
      arrow([94, 780, 174, 860], true, cur.i > 0); arrow([666, 780, 746, 860], false, cur.i < items.length - 1);
      const cnt = el("div", "pv-cnt lt", `${cur.i + 1} / ${items.length}`); cnt.style.cssText = box(300, 1052, 540, 1110) + `;font-size:${40 * unit}px`; scene.append(cnt);
    }
  };
  show();
  let sx = null; scene.addEventListener("touchstart", (e) => { sx = e.touches[0].clientX; }, { passive: true });
  scene.addEventListener("touchend", (e) => { if (sx == null) return; const dx = e.changedTouches[0].clientX - sx; sx = null; if (Math.abs(dx) > 60) { const n = cur.i + (dx < 0 ? 1 : -1); if (n >= 0 && n < items.length) { cur.i = n; show(); } } });
}

// Share a drawing as a picture (Web Share API when the browser has it, a download otherwise).
async function shareDrawing(c, it, cv) {
  try {
    const out = document.createElement("canvas"); out.width = 720; out.height = 860; const g = out.getContext("2d");
    g.fillStyle = "#fff6e2"; g.fillRect(0, 0, 720, 860); g.fillStyle = "#3b2314"; g.font = "800 56px sans-serif"; g.textAlign = "center"; g.fillText(cap(it.word, c.lang()), 360, 90);
    g.drawImage(cv, 40, 130, 640, 640 * cv.height / cv.width); g.font = "700 32px sans-serif"; g.fillStyle = "#b5510f"; g.fillText("Karalak", 360, 830);
    const blob = await new Promise((r) => out.toBlob(r, "image/png")), file = new File([blob], "karalak.png", { type: "image/png" });
    if (navigator.canShare && navigator.canShare({ files: [file] })) await navigator.share({ files: [file] });
    else { const a = document.createElement("a"); a.href = URL.createObjectURL(blob); a.download = "karalak.png"; a.click(); }
  } catch {}
}

function confetti(host) {
  const f = el("div", "rs-conf"); host.append(f);
  for (let i = 0; i < 46; i++) { const e = el("i", ""); e.style.cssText = `left:${Math.random() * 100}%;background:hsl(${Math.random() * 360},85%,60%);animation-delay:${Math.random() * .6}s;animation-duration:${2 + Math.random() * 1.4}s;transform:rotate(${Math.random() * 360}deg)`; f.append(e); }
  setTimeout(() => f.remove(), 4200);
}

// ---------------- online result (OnlineResultScreen.kt) ----------------
// d: { players:[{uid,displayName,level,frameId,totalScore,correctCount,wrongCount}], left:[...], myUid, xp, gold, pen, code,
//      votes:[uid], requested, blocked, itemsFor(uid)->[], select(uid), sel, getRoom, ring(frameId), dock(), chatRow(), onHome, onRematch }
export function renderOnlineResult(c, d) {
  const { $, lang: gl } = c, lang = gl(), T = (k, ...a) => tr(k, lang, ...a), C = (k, ...a) => c.chatText(k, lang, ...a);
  const root = $("rs-body"), foot = $("rs-foot"), sc = $("rs-scroll"), keep = sc.scrollTop; root.replaceChildren(); foot.replaceChildren();
  // the back button is not in the head (which scrolls with the page) but laid over the screen, so it stays in its corner
  const head = el("div", "rs-head", `<img src="img/rs_head_plain.webp" alt=""><div class="rs-title lt">${T("game_over")}</div>`);
  $("rs-fback").onclick = d.onHome; root.append(head);
  const inner = el("div", "rs-inner"); root.append(inner);
  const me = d.players.find((p) => p.uid === d.myUid), ranked = [...d.players].sort((a, b) => b.totalScore - a.totalScore), place = ranked.findIndex((p) => p.uid === d.myUid) + 1;
  const ringBox = (fid) => { const w = el("div", "rsv-ring"); w.append(d.ring(fid)); return w; };
  if (d.teamMode) {
    const myT = me.teamId === "B" ? "B" : "A", tot = (t) => d.players.filter((p) => p.teamId === t).reduce((a, p) => a + p.totalScore, 0), mine = tot(myT), theirs = tot(myT === "A" ? "B" : "A"), won = mine > theirs, drew = mine === theirs;
    const col = (m) => (m ? (won ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f") : (!won && !drew ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f"));
    const nm = (t) => C(t === "A" ? "online_team_a" : "online_team_b");
    inner.append(el("div", "rs-vs", `<div class="nm l tn">${nm(myT)}</div><div class="sc l tn" style="color:${col(true)}">${mine}</div><div class="nm r tn">${nm(myT === "A" ? "B" : "A")}</div><div class="sc r tn" style="color:${col(false)}">${theirs}</div><div class="out" style="background:${drew ? "#8a6a50" : won ? "#2ea043" : "#d23b2e"}">${T(drew ? "quick_match_drew" : won ? "quick_match_won" : "quick_match_lost")}</div><div class="vs">${T("quick_match_versus")}</div>${won ? '<img class="cr" src="img/lobby_crown.webp" alt="">' : ""}`));
  } else if (d.players.length === 2) {
    const other = d.players.find((p) => p.uid !== d.myUid), won = me.totalScore > other.totalScore, drew = me.totalScore === other.totalScore;
    const col = (mine) => (mine ? (won ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f") : (!won && !drew ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f"));
    const v = el("div", "rs-vs", `<div class="av l"></div><div class="nm l">${esc(me.displayName)}</div><div class="sc l" style="color:${col(true)}">${me.totalScore}</div><div class="av r"></div><div class="nm r">${esc(other.displayName)}</div><div class="sc r" style="color:${col(false)}">${other.totalScore}</div><div class="out" style="background:${drew ? "#8a6a50" : won ? "#2ea043" : "#d23b2e"}">${T(drew ? "quick_match_drew" : won ? "quick_match_won" : "quick_match_lost")}</div><div class="vs">${T("quick_match_versus")}</div>${won ? '<img class="cr" src="img/lobby_crown.webp" alt="">' : ""}`);
    v.querySelector(".av.l").append(d.ring(me.frameId)); v.querySelector(".av.r").append(d.ring(other.frameId)); inner.append(v);
  } else {
    const medal = (r) => ["🥇", "🥈", "🥉"][r - 1] || r + ".";
    const rows = ranked.map((p, i) => `<div class="st-row"><span class="rk">${medal(i + 1)}</span><i class="av" data-f="${p.frameId}"></i><div class="who"><b>${esc(p.uid === d.myUid ? C("online_you_label", p.displayName) : p.displayName)}</b><small>${p.correctCount}/${p.correctCount + p.wrongCount}</small></div>${d.votes.includes(p.uid) ? '<u class="ok">✓</u>' : ""}<span class="pt">${p.totalScore}</span></div>`).join("")
      + d.left.map((p) => `<div class="st-row gone"><span class="rk">•</span><i class="av" data-f="${p.frameId}"></i><div class="who"><b>${esc(p.displayName)}</b><small>…</small></div></div>`).join("");
    const card = el("div", "rs-stand", `<h4 style="color:${place === 1 ? "#1f9a3a" : "#3b2314"}">${C("online_result_placement", place)}</h4>${rows}`);
    card.querySelectorAll(".av").forEach((a) => a.append(d.ring(a.dataset.f))); inner.append(card);
  }
  // compact stats
  const mini = (icon, val, col) => `<div class="mn"><img src="img/${icon}.webp" alt=""><b style="color:${col}">${val}</b></div>`;
  const right = [d.gold > 0 ? mini("icon_gold_coin", "+" + d.gold, "#e8650f") : "", d.xp > 0 ? mini("rs_ic_star", d.xp + " XP", "#f2541b") : ""].join("");
  inner.append(el("div", "rs-pair" + (right ? "" : " one"), `<div class="rs-card rb"><div class="mrow">${mini("rs_ic_check", me.correctCount, "#2ea043")}${mini("rs_ic_x", me.wrongCount, "#e23b32")}</div></div>${right ? `<div class="rs-card ra"><div class="mrow">${right}</div></div>` : ""}`));
  // board
  const sel = d.sel || d.myUid, cfg = { title: T("result_words_title", me.correctCount + me.wrongCount), sel, placeholders: Math.max(me.correctCount + me.wrongCount, d.wordCount || 10), pen: d.pen,
    items: (u) => d.itemsFor(u), theirs: (u) => u !== d.myUid, onSelect: (u) => d.select(u),
    chips: d.players.map((p) => ({ id: p.uid, label: p.displayName, enabled: p.uid === d.myUid || d.itemsFor(p.uid).length > 0 })) };
  inner.append(board(c, cfg, lang, T));
  // chat + rematch note
  inner.append(d.dock());
  if (d.blocked) inner.append(el("div", "rs-note", C("online_rematch_blocked_message")));
  else if (d.votes.length) inner.append(el("div", "rs-note ok", C("online_rematch_ready_count", d.votes.filter((u) => d.players.some((p) => p.uid === u)).length, d.players.length)));
  const row = el("div", "rs-btns");
  const home = el("button", "rs-art", `<span class="l1 lt">${C("main_menu")}</span>`); home.onclick = d.onHome;
  const again = el("button", "rs-art" + (d.requested || d.blocked ? " dis" : ""), `<span class="l1 lt">${C(d.requested ? "online_rematch_waiting" : "play_again")}</span>`); if (!d.requested && !d.blocked) again.onclick = d.onRematch;
  row.append(home, again); foot.append(row);
  sc.scrollTop = keep;
}
