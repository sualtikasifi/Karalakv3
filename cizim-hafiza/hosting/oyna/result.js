// Result screen (port of ResultScreen.kt / ResultParts.kt): painted head, stat cards, level card, versus card,
// daily card, the words board with every drawing, claim buttons, and the drawing preview scene.
let S = null;
const tr = (k, lang, ...a) => ((S && S[k] && S[k][lang]) || k).replace(/\{(\d)\}/g, (_, i) => a[+i]);
export async function loadResultStrings() { if (!S) S = await (await fetch("result_strings.json")).json(); }
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };
const cap = (w, lang) => w ? w.charAt(0).toLocaleUpperCase(lang) + w.slice(1) : "";
const ICON = { check: "rs_ic_check", x: "rs_ic_x", star: "rs_ic_star", crown: "rs_ic_crown", coin: "icon_gold_coin", trophy: "icon_league_trophy", cal: "daily_calendar_icon" };

function statList(d, lang) {
  const T = (k, ...a) => tr(k, lang, ...a), list = [
    { ic: "check", label: T("result_stat_correct"), value: String(d.n), color: "#2ea043" },
    { ic: "x", label: T("result_stat_wrong"), value: String(d.total - d.n), color: "#e23b32" }];
  const or = "#e8650f", gold = (g) => T("result_gold_amount", g);
  if (d.stars != null) list.push({ ic: "star", label: T("result_stat_stars"), value: "★".repeat(d.stars) + "☆".repeat(Math.max(0, 3 - d.stars)), color: or });
  if (d.goldPlay > 0) list.push({ ic: "coin", label: T("result_stat_play_gold"), value: gold(d.goldPlay), color: or });
  if (d.goldWin > 0) list.push({ ic: "trophy", label: T("result_stat_win_gold"), value: gold(d.goldWin), color: or });
  if (d.goldDaily > 0) list.push({ ic: "cal", label: T("result_stat_daily_gold"), value: gold(d.goldDaily), color: or });
  if (d.daily == null) list.push({ ic: "star", label: "", value: T("result_xp_won", d.xp), color: "#f2541b", headline: true });
  return list;
}

export function renderResult(c, d) {
  const { $, lang: gl } = c, lang = gl(), T = (k, ...a) => tr(k, lang, ...a);
  const root = $("rs-body"), foot = $("rs-foot"); root.replaceChildren(); foot.replaceChildren();
  const sc = $("rs-scroll"); sc.scrollTop = 0;
  // head
  const head = el("div", "rs-head", `<img src="img/rs_head.webp" alt=""><button class="rs-back" aria-label="back"></button><div class="rs-title lt">${T("game_over")}</div>`);
  head.querySelector(".rs-back").onclick = d.onHome; root.append(head);
  const inner = el("div", "rs-inner"); root.append(inner);
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
    const side = (name, score, ring, right, col) => `<div class="av ${right ? "r" : "l"}"></div><div class="nm ${right ? "r" : "l"}">${name}</div><div class="sc ${right ? "r" : "l"}" style="color:${col}">${score}</div>`;
    const v = el("div", "rs-vs", `${side(d.nick || T("quick_match_you"), me, null, false, won ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f")}${side(d.opp.nickname || "?", op, null, true, !won && !drew ? "#1f9a3a" : drew ? "#3b2314" : "#8e1f1f")}<div class="out" style="background:${drew ? "#8a6a50" : won ? "#2ea043" : "#d23b2e"}">${T(drew ? "quick_match_drew" : won ? "quick_match_won" : "quick_match_lost")}</div><div class="vs">${T("quick_match_versus")}</div>${won ? '<img class="cr" src="img/lobby_crown.webp" alt="">' : ""}`);
    const a1 = d.myRing(), a2 = d.oppRing(); v.querySelector(".av.l").append(a1); v.querySelector(".av.r").append(a2);
    inner.append(v);
  }
  // daily
  if (d.daily) {
    const D = d.daily, pips = d.flags.map((f) => `<i class="${f ? "ok" : "no"}">${f ? "✓" : "✗"}</i>`).join("");
    inner.append(el("div", "rs-daily", `<h4>${T("daily_challenge_result_title")}</h4><div class="pips">${pips}</div><div class="pl"><div><span>🔥 <b>${D.streak}</b></span><small>${T("daily_streak_label")}</small></div><div><span>⭐ <b>${T("daily_challenge_xp_earned", D.xp)}</b></span><small>${T("daily_reward_won")}</small></div></div><button class="rs-share"><svg viewBox="0 0 24 24"><path fill="#fff" d="M18 16.1c-.8 0-1.5.3-2 .8l-7.1-4.2a3 3 0 000-1.4L16 7.2A3 3 0 1015 5c0 .2 0 .4.1.7L8 9.9a3 3 0 100 4.2l7.1 4.2c-.1.2-.1.5-.1.7a3 3 0 103-2.9z"/></svg><span class="lt">${T("daily_challenge_share")}</span></button>`));
    inner.lastChild.querySelector(".rs-share").onclick = async () => { const txt = `Karalak 🎨 ${T("daily_challenge_result_title")}\n${d.flags.map((f) => (f ? "🟩" : "🟥")).join("")}\n🔥 ${D.streak}\nhttps://karalak-b6e11.web.app/oyna/`; try { if (navigator.share) await navigator.share({ text: txt }); else { await navigator.clipboard.writeText(txt); } } catch {} };
  }
  // words board
  inner.append(board(c, d, lang, T));
  // footer buttons
  if (d.next) { const b = el("button", "rs-next", `<span>⟳ ${d.next.label}</span>`); b.onclick = d.next.fn; foot.append(b); }
  const row = el("div", "rs-btns");
  const x2 = el("button", "rs-art dis", `<span class="l1 lt">${T("result_x2_amount", d.xp * 2)}</span><span class="l2">🎬 ${T("result_watch_ad")}</span>`);
  const cl = el("button", "rs-art", `<span class="l1 lt">${T("result_claim_amount", d.xp)}</span>`); cl.onclick = d.onHome;
  row.append(x2, cl); foot.append(row);
  if (d.xp > 0 && d.n > 0) confetti(c.$("res2"));
}

function board(c, d, lang, T) {
  const wrap = el("div", "rs-board"); let showOpp = false;
  const n = () => (showOpp ? d.oppItems : d.items);
  const draw = () => {
    wrap.replaceChildren();
    wrap.append(el("div", "bd-art"));
    const plank = el("div", "plank");
    if (d.opp) {
      const mk = (label, sel, en, fn) => { const b = el("button", "chip " + (sel ? "sel" : ""), `<span class="lt">${label}</span>`); if (!en && !sel) b.classList.add("dis"); b.onclick = en && !sel ? fn : null; return b; };
      const yl = d.nick || T("quick_match_gallery_yours"), ol = d.opp.nickname || "?";
      const t1 = mk(yl, !showOpp, true, () => { showOpp = false; draw(); }), t2 = mk(ol, showOpp, d.oppItems.length > 0, () => { showOpp = true; draw(); });
      t1.style.flex = Math.max(5, yl.length); t2.style.flex = Math.max(5, ol.length); plank.append(t1, t2);
    } else plank.append(el("span", "lt", T("result_words_title", d.items.length)));
    wrap.append(plank);
    const items = n(), total = items.length || d.items.length, rows = Math.ceil(total / 5), g = el("div", "rs-sheets" + (rows > 2 ? " scr" : ""));
    for (let i = 0; i < total; i++) {
      const it = items[i], sh = el("div", "sheet" + (it ? "" : " empty"));
      if (it) {
        sh.innerHTML = `<div class="pg"><i class="dots"></i><em>${i + 1}</em><canvas></canvas><b>${cap(it.word, lang)}</b></div><img class="mk" src="img/${it.isCorrect ? "rs_ic_check" : "rs_ic_x"}.webp" alt="">`;
        sh.onclick = () => preview(c, d, items, i, lang, T, showOpp);
      }
      g.append(sh);
    }
    wrap.append(g);
    requestAnimationFrame(() => g.querySelectorAll("canvas").forEach((cv, i) => { c.fitCanvas(cv); c.paint(cv, items[i].strokes, false, items[i].penSkin || d.pen || "CLASSIC"); }));
  };
  draw(); return wrap;
}

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
