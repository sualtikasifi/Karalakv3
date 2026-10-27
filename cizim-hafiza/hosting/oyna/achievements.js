let list = [];
const T = {
  tr: { title: "Başarımlar", count: "başarım açıldı", claim: "Ödülü topla", claimed: "🎁 Ödül alındı", locked: "🔒 Henüz açılmadı", unlocked: "Açıldı!", hint: "Nasıl açılır?", cond: "Koşul", close: "Kapat", xp: (n) => `+${n} XP`, gold: (n) => `+${n} altın` },
  en: { title: "Achievements", count: "achievements unlocked", claim: "Claim reward", claimed: "🎁 Reward claimed", locked: "🔒 Not unlocked yet", unlocked: "Unlocked!", hint: "How to unlock", cond: "Condition", close: "Close", xp: (n) => `+${n} XP`, gold: (n) => `+${n} gold` },
  pt: { title: "Conquistas", count: "conquistas desbloqueadas", claim: "Resgatar recompensa", claimed: "🎁 Recompensa resgatada", locked: "🔒 Ainda não desbloqueada", unlocked: "Desbloqueada!", hint: "Como desbloquear", cond: "Condição", close: "Fechar", xp: (n) => `+${n} XP`, gold: (n) => `+${n} de ouro` },
  es: { title: "Logros", count: "logros desbloqueados", claim: "Reclamar recompensa", claimed: "🎁 Recompensa reclamada", locked: "🔒 Aún no desbloqueado", unlocked: "¡Desbloqueado!", hint: "Cómo desbloquearlo", cond: "Condición", close: "Cerrar", xp: (n) => `+${n} XP`, gold: (n) => `+${n} de oro` },
};
export async function loadAchievements() { if (!list.length) list = await (await fetch("achievements.json")).json(); }
const el = (tag, cls, html) => { const e = document.createElement(tag); if (cls) e.className = cls; if (html != null) e.innerHTML = html; return e; };

function value(P, m) {
  const s = P.stats;
  return { gamesPlayed: s.games, lifetimeScore: s.score, lifetimeWordsDrawn: s.words, lifetimeXp: P.xp, perfectRounds: s.perfect, onlineWins: s.wins, currentStreak: s.streak, bestStreak: s.best }[m] || 0;
}
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

export function renderAchievements(c) {
  const { $, P, lang } = c, x = T[lang()];
  $("ac-title").textContent = x.title;
  const list$ = $("ac-list"); list$.replaceChildren();
  const unlocked = list.filter((a) => value(P, a.m) >= a.t).length, frac = unlocked / list.length;
  const head = el("div", "ac-head", `<div class="ac-trophy">🏆</div><div class="ac-ht"><b>${unlocked} / ${list.length}</b><small>${x.count}</small></div><div class="ac-pct">%${Math.floor(frac * 100)}</div><div class="ac-bar"><i style="width:${frac * 100}%"></i></div>`);
  list$.append(head);
  const grid = el("div", "ac-grid"); list$.append(grid);
  list.forEach((a, i) => {
    const v = value(P, a.m), done = v >= a.t, claimed = P.claimed.includes(a.id), claimable = done && !claimed;
    const card = el("button", "ac-card" + (claimable ? " pulse" : "") + (claimed ? " got" : "") + (!done ? " lock" : ""));
    card.style.backgroundImage = `url(img/ach_card_${(i + Math.floor(i / 3)) % 4 + 1}.webp)`;
    const status = !done ? `${Math.min(v, a.t)}/${a.t}` : claimable ? `<span class="cl">${x.claim}</span>` : `<span class="gr">${x.claimed}</span>`;
    card.innerHTML = `<span class="em">${a.emoji}</span><b>${a[lang()][0]}</b><small>${status}</small><em class="rw ${a.gold ? "g" : ""}">${a.gold ? x.gold(reward(a)) : x.xp(reward(a))}</em>${claimed ? '<i class="ck">✓</i>' : ""}`;
    card.onclick = () => claimable ? claim(c, a) : detail(c, a, done, claimed, v);
    grid.append(card);
  });
}

function claim(c, a) {
  const { P } = c; if (P.claimed.includes(a.id)) return;
  P.claimed.push(a.id); if (a.gold) P.gold += reward(a); else P.xp += reward(a);
  c.saveP(); c.refreshHome(); renderAchievements(c);
}

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
  body.innerHTML = `<div class="tag ${done ? "ok" : ""}">${claimed ? x.claimed : done ? x.unlocked : x.locked}</div>${done ? "" : `<div class="pr">${Math.min(v, a.t)}/${a.t}</div>`}<div class="ds"><small>${done ? x.cond : x.hint}</small>${a[lang()][1]}</div><div class="rwp">🏆 ${a.gold ? x.gold(reward(a)) : x.xp(reward(a))}</div>`;
  const btn = el("button", "pill orange small", `<span>${done && !claimed ? x.claim : x.close}</span>`); btn.style.width = "62%"; btn.style.height = 120 * k + "px";
  btn.onclick = () => { d.classList.add("hidden"); if (done && !claimed) claim(c, a); };
  body.append(btn); box.append(body); d.append(box);
  d.onclick = (e) => { if (e.target === d) d.classList.add("hidden"); };
}
