let list = [];
const T = {
  tr: { sClaim: "Ödülünü topla", sProg: "Devam edenler", sDone: "Kazandıkların", all: "Hepsini topla", waiting: (n) => `${n} ödül bekliyor`, title: "Başarımlar", count: "başarım açıldı", claim: "Ödülü topla", claimed: "🎁 Ödül alındı", locked: "🔒 Henüz açılmadı", unlocked: "Açıldı!", hint: "Nasıl açılır?", cond: "Koşul", close: "Kapat", xp: (n) => `+${n} XP`, gold: (n) => `+${n} altın` },
  en: { sClaim: "Ready to collect", sProg: "In progress", sDone: "Earned", all: "Collect all", waiting: (n) => `${n} rewards waiting`, title: "Achievements", count: "achievements unlocked", claim: "Claim reward", claimed: "🎁 Reward claimed", locked: "🔒 Not unlocked yet", unlocked: "Unlocked!", hint: "How to unlock", cond: "Condition", close: "Close", xp: (n) => `+${n} XP`, gold: (n) => `+${n} gold` },
  pt: { sClaim: "Prontas para resgatar", sProg: "Em andamento", sDone: "Conquistadas", all: "Resgatar tudo", waiting: (n) => `${n} recompensas esperando`, title: "Conquistas", count: "conquistas desbloqueadas", claim: "Resgatar recompensa", claimed: "🎁 Recompensa resgatada", locked: "🔒 Ainda não desbloqueada", unlocked: "Desbloqueada!", hint: "Como desbloquear", cond: "Condição", close: "Fechar", xp: (n) => `+${n} XP`, gold: (n) => `+${n} de ouro` },
  es: { sClaim: "Listas para reclamar", sProg: "En progreso", sDone: "Conseguidos", all: "Reclamar todo", waiting: (n) => `${n} recompensas esperando`, title: "Logros", count: "logros desbloqueados", claim: "Reclamar recompensa", claimed: "🎁 Recompensa reclamada", locked: "🔒 Aún no desbloqueado", unlocked: "¡Desbloqueado!", hint: "Cómo desbloquearlo", cond: "Condición", close: "Cerrar", xp: (n) => `+${n} XP`, gold: (n) => `+${n} de oro` },
};
export async function loadAchievements() { if (!list.length) list = await (await fetch("achievements.json")).json(); }
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };

function value(P, m) {
  const s = P.stats;
  return { gamesPlayed: s.games, lifetimeScore: s.score, lifetimeWordsDrawn: s.words, lifetimeXp: P.xp, perfectRounds: s.perfect, onlineWins: s.wins, currentStreak: s.streak, bestStreak: s.best }[m] || 0;
}
const fm = (n, L) => n.toLocaleString(L === "pt" ? "pt-BR" : L === "es" ? "es" : L === "tr" ? "tr" : "en");
const reward = (a) => a.gold ? Math.max(15, Math.floor((Math.floor(a.xp / 2) + 2) / 5) * 5) : a.xp;

// Called after every round so the stats the achievements read from stay current.
export function recordRound(P, { score, words, perfect }) {
  const s = P.stats, today = new Date().toISOString().slice(0, 10);
  s.games++; s.score += score; s.words += words; if (perfect) s.perfect++;
  if (s.last !== today) {
    const y = new Date(Date.now() - 864e5).toISOString().slice(0, 10);
    s.streak = s.last === y ? s.streak + 1 : 1; s.last = today; s.best = Math.max(s.best, s.streak);
  }
}
/** How many achievements are unlocked right now: the difference across a round is the new ones (gold per new one). */
export const unlockedCount = (P) => list.filter((a) => value(P, a.m) >= a.t).length;
export const unlockedIds = (P) => list.filter((a) => value(P, a.m) >= a.t).map((a) => a.id);
export function claimableCount(P) { return list.filter((a) => value(P, a.m) >= a.t && !P.claimed.includes(a.id)).length; }

export function layoutAchievements(c) {
  const { place, $ } = c, u = c.u();
  place($("ac-back"), 20, 40, 110, 106);
  place($("ac-sign"), 140, 120, 555, 234);
  place($("ac-title"), 170, 120 + 234 * 0.415, 530, 234 * 0.36);
  const l = $("ac-list"); l.style.left = 12 * u / 1 + "px"; l.style.top = 343 * u + "px"; l.style.width = 817 * u + "px"; l.style.height = c.fillH() - 343 * u + "px";
  
}

let justClaimed = new Set(), clearTimer = 0;
const frac = (a, P) => Math.min(1, value(P, a.m) / Math.max(1, a.t));
const medal = (a, done, cls = "") => `<span class="md ${done ? "on" : ""} ${cls}"><i>${a.emoji}</i></span>`;

export function renderAchievements(c) {
  const { $, P, lang } = c, x = T[lang()], L = lang();
  $("ac-title").textContent = x.title;
  const list$ = $("ac-list"), keep = list$.scrollTop; list$.replaceChildren();
  const state = (a) => { const v = value(P, a.m), done = v >= a.t, claimed = P.claimed.includes(a.id); return { a, v, done, claimed, claimable: done && !claimed }; };
  const all = list.map(state);
  const toCollect = all.filter((s) => s.done && (!s.claimed || justClaimed.has(s.a.id)));
  const earned = all.filter((s) => s.done && s.claimed && !justClaimed.has(s.a.id));
  const progress = all.filter((s) => !s.done).sort((p, q) => frac(q.a, P) - frac(p.a, P));
  const unlocked = all.filter((s) => s.done).length, f = unlocked / list.length, waiting = all.filter((s) => s.claimable).length;
  list$.append(el("div", "ac-head", `<div class="ac-ht"><b>${unlocked} / ${list.length}</b><small>${x.count}</small>${waiting ? `<em>${x.waiting(waiting)}</em>` : ""}</div><div class="ac-bar"><i style="width:${Math.max(4, f * 100)}%"></i><span>%${Math.floor(f * 100)}</span></div>`));
  const section = (title, n, accent, action, onAction) => {
    const h = el("div", "ac-sec" + (accent ? " acc" : ""), `<span class="st">${title}<b>${n}</b></span>`);
    if (action) { const b = el("button", "ac-all", `<span>${action}</span>`); b.onclick = onAction; h.append(b); }
    list$.append(h);
  };
  const reward = (a) => `<em class="rw ${a.gold ? "g" : ""}">${a.gold ? x.gold(fm(rewardOf(a), L)) : x.xp(fm(rewardOf(a), L))}</em>`;
  const row = (s) => {
    const { a, v, done, claimed, claimable } = s, celebrating = justClaimed.has(a.id) && claimed;
    const r = el("button", "ac-row" + (claimable ? " pulse" : "") + (!done ? " lock" : ""));
    const mid = done ? `<small class="${claimed ? "gr" : "cl"}">${claimed ? x.claimed : x.unlocked}</small>`
      : `<div class="pb"><div class="bar"><i style="width:${Math.max(4, frac(a, P) * 100)}%"></i></div><small>${fm(Math.min(v, a.t), L)}/${fm(a.t, L)}</small></div>`;
    const side = claimable ? `<span class="cb">${x.claim}</span>` : done ? '<i class="ck">✓</i>' : reward(a);
    r.innerHTML = `${medal(a, done)}<div class="mid"><b>${a[L][0]}</b><span class="ds">${a[L][1]}</span>${mid}</div>${side}${celebrating ? `<span class="fly ${a.gold ? "g" : ""}">+${a.gold ? fm(rewardOf(a), L) + " " + (L === "tr" ? "altın" : L === "en" ? "gold" : L === "pt" ? "ouro" : "oro") : fm(rewardOf(a), L) + " XP"}</span>` : ""}`;
    r.onclick = () => claimable ? claimOne(c, a) : detail(c, a, done, claimed, v);
    list$.append(r);
  };
  if (toCollect.length) {
    const open = toCollect.filter((s) => !s.claimed);
    section(x.sClaim, toCollect.length, true, open.length > 1 ? x.all : null, () => { open.forEach((s) => claim(c, s.a, true)); settle(c); });
    toCollect.forEach(row);
  }
  if (progress.length) { section(x.sProg, progress.length, false); progress.forEach(row); }
  if (earned.length) {
    section(x.sDone, earned.length, false);
    const shelf = el("div", "ac-shelf");
    for (const s of earned) { const m = el("button", "ac-medal", `${medal(s.a, true)}<b>${s.a[L][0]}</b>`); m.onclick = () => detail(c, s.a, true, true, s.v); shelf.append(m); }
    list$.append(shelf);
  }
  list$.scrollTop = keep;
}
const rewardOf = reward;
// A reward just collected stays in the top section for a moment so its celebration can play, then drops to the shelf.
function settle(c) { clearTimeout(clearTimer); clearTimer = setTimeout(() => { justClaimed = new Set(); renderAchievements(c); }, 1500); }

function claim(c, a, quiet) {
  const { P } = c; if (P.claimed.includes(a.id)) return;
  P.claimed.push(a.id); justClaimed.add(a.id); if (a.gold) P.gold += reward(a); else P.xp += reward(a);
  if (!quiet) { c.saveP(); c.refreshHome(); renderAchievements(c); settle(c); } else { c.saveP(); c.refreshHome(); }
}
const claimOne = (c, a) => claim(c, a);

function detail(c, a, done, claimed, v) {
  const { $, lang } = c, x = T[lang()], d = $("ac-dlg");
  // AchievementDetailDialog: the card is min(screen - 2 x 18 dp, 400 dp) wide, which is 767 of the 841 units of the scene on a phone
  const u = c.u(), W = 767 * u, k = W / 1016;
  d.classList.remove("hidden"); d.replaceChildren();
  const box = el("div", "acd"); box.style.cssText = `width:${W}px;height:${1394 * k}px;`;
  const p = (x0, y0, w, h) => `left:${x0 * k}px;top:${y0 * k}px;width:${w * k}px;height:${h * k}px;`;
  box.append(el("div", "acd-em", a.emoji)); box.lastChild.style.cssText = p(398, 160, 210, 210) + `font-size:${120 * k}px;opacity:${done ? 1 : .45}`;
  box.append(el("div", "acd-t", a[lang()][0])); box.lastChild.style.cssText = p(250, 427, 520, 100) + `font-size:${64 * k}px`;
  const body = el("div", "acd-b"); body.style.cssText = p(100, 570, 820, 740) + `font-size:${38 * k}px`;
  body.innerHTML = `<div class="tag ${done ? "ok" : ""}">${claimed ? x.claimed : done ? x.unlocked : x.locked}</div>${done ? "" : `<div class="pr">${fm(Math.min(v, a.t), lang())}/${fm(a.t, lang())}</div>`}<div class="ds"><small>${done ? x.cond : x.hint}</small>${a[lang()][1]}</div><div class="rwp">🏆 ${a.gold ? x.gold(reward(a)) : x.xp(reward(a))}</div>`;
  const btn = el("button", "pill orange small", `<span>${done && !claimed ? x.claim : x.close}</span>`); btn.style.width = "62%"; btn.style.height = 120 * k + "px";
  btn.onclick = () => { d.classList.add("hidden"); if (done && !claimed) claim(c, a); };
  body.append(btn); box.append(body); d.append(box);
  d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
