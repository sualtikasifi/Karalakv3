// Asynchronous friend duels (DuelListScreen.kt / CreateDuelScreen.kt): the challenger plays a normal round that becomes a
// challenge; the friend plays the same words later. Documents are the same duels/{id} the Android app reads.
import * as net from "./firebase.js";
import { S } from "./i18n.js";
import { el, appWindow, primary } from "./ui.js";

const HIST = "duelHistory";
export const history = () => { try { return JSON.parse(localStorage.getItem(HIST) || "[]"); } catch { return []; } };
const saveHist = (l) => { try { localStorage.setItem(HIST, JSON.stringify(l.slice(0, 60))); } catch {} };
/** Copies finished duels a snapshot carries into the on-phone history (DuelHistoryStore.archive). */
function archive(list, uid) {
  const h = history(), ids = new Set(h.map((e) => e.id)); let ch = false;
  for (const d of list) {
    if (d.status !== "COMPLETE" || d.opponentScore == null) continue;
    const mine = d.challengerUid === uid, oc = d.opponentCorrectCount ?? 0;
    const e = { id: d.id, iAmChallenger: mine, otherName: mine ? d.opponentName : d.challengerName, myScore: mine ? d.challengerScore : d.opponentScore, otherScore: mine ? d.opponentScore : d.challengerScore, myCorrect: mine ? d.challengerCorrectCount : oc, otherCorrect: mine ? oc : d.challengerCorrectCount, totalWords: d.items.length, finishedAt: d.completedAt || d.createdAt, seen: mine ? d.seenByChallenger : true };
    if (!ids.has(d.id)) { h.push(e); ch = true; }
    else { const o = h.find((x) => x.id === d.id); if (o.iAmChallenger && !o.seen && e.seen) { o.seen = true; ch = true; } }
  }
  if (ch) saveHist(h.sort((a, b) => b.finishedAt - a.finishedAt));
}
export function recordReceived(duel, myScore, myCorrect) {
  const h = history().filter((e) => e.id !== duel.id);
  h.unshift({ id: duel.id, iAmChallenger: false, otherName: duel.challengerName, myScore, otherScore: duel.challengerScore, myCorrect, otherCorrect: duel.challengerCorrectCount, totalWords: duel.items.length, finishedAt: Date.now(), seen: true });
  saveHist(h);
}

let unsub = null, state = { incoming: [], sent: [], received: [], loading: true }, reqs = [], unReq = null, myUid = "";

export async function openDuels(c) {
  stopDuels(); state = { incoming: [], sent: [], received: [], loading: true }; reqs = []; renderDuels(c);
  try {
    myUid = await net.myUid();
    unsub = await net.listenDuels((s) => { state = { ...s, loading: false }; archive([...s.sent, ...s.received], myUid); renderDuels(c); });
    unReq = await net.listen("friendRequests", (l) => { reqs = l; renderDuels(c); });
  } catch { state.loading = false; renderDuels(c); }
}
export function stopDuels() { try { if (unsub) unsub(); if (unReq) unReq(); } catch {} unsub = unReq = null; }

const empty = (icon, text) => `<div class="du-empty"><img src="img/${icon}.webp" alt=""><span>${text}</span></div>`;
const card = (kind, title, rows) => {
  const d = el("div", "du-card " + kind), b = el("div", "du-cb");
  rows.forEach((r) => b.append(r));
  d.append(el("i", "du-bgp"), el("div", "du-ct", title), b); return d;
};
const esc = (s) => String(s).replace(/[&<>"]/g, (ch) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[ch]));
const pill = (cls, text, fn) => { const b = el("button", "du-pill " + cls, `<span>${text}</span>`); b.onclick = fn; return b; };
const holder = (html) => el("div", "", html);

export function renderDuels(c) {
  if (c.current() !== "duels") return;
  const root = c.$("du-scroll"); root.replaceChildren();
  const hist = history().sort((a, b) => b.finishedAt - a.finishedAt).slice(0, 20), pending = state.sent.filter((d) => d.status === "AWAITING_OPPONENT");
  // incoming: challenges to play + friend requests to answer
  const inc = [];
  if (state.loading) inc.push(holder("<div class='du-empty'>…</div>"));
  else if (!state.incoming.length && !reqs.length) inc.push(holder(empty("du_ic_mail", S("duel_list_requests_empty"))));
  else {
    state.incoming.forEach((d) => {
      const r = el("div", "du-row", `<img src="img/du_av_other.webp" alt=""><div class="du-nm"><b>${esc(d.challengerName)}</b><small>${S("duel_list_word_count", d.items.length)}</small></div>`);
      r.append(pill("o w", S("duel_list_incoming_play"), () => c.playDuel(d))); inc.push(r);
    });
    reqs.forEach((q) => {
      const r = el("div", "du-req", `<div class="du-row"><img src="img/du_av_other.webp" alt=""><div class="du-nm"><b>${S("duel_list_friend_request", esc(q.fromNickname || "?"))}</b></div></div>`), b = el("div", "du-two");
      b.append(pill("b", S("friends_request_decline"), () => net.declineRequest(q.id)), pill("g", S("friends_request_accept"), () => net.acceptRequest(q, c.P.nick)));
      r.append(b); inc.push(r);
    });
  }
  root.append(card("a", S("duel_list_requests_title"), inc));
  // recent: finished duels, both directions
  const rec = [];
  if (!state.loading && !hist.length) rec.push(holder(empty("du_ic_out", S("duel_list_recent_empty"))));
  else hist.forEach((h) => {
    const w = h.myScore > h.otherScore ? true : h.myScore < h.otherScore ? false : null, isNew = h.iAmChallenger && !h.seen;
    const key = w === true ? "duel_list_status_won" : w === false ? "duel_list_status_lost" : "duel_list_status_tied";
    const sc = (name, s, cor, hi) => `<div class="du-sc"><span>${esc(name)}</span><b${hi ? ' class="win"' : ""}>${s}</b><small>${S("online_correct_of_total", cor, h.totalWords)}</small></div>`;
    const r = el("div", "du-rec", `<div class="du-vh"><b class="${w === true ? "win" : w === false ? "loss" : ""}">${S(key)}</b>${isNew ? '<i class="dot"></i>' : ""}<img class="x" src="img/du_x.webp" alt="${S("duel_list_delete")}"></div><div class="du-vr"><img src="img/du_av_me.webp" alt="">${sc(S("quick_match_you"), h.myScore, h.myCorrect, w === true)}<img src="img/du_vs.webp" alt="">${sc(h.otherName, h.otherScore, h.otherCorrect, w === false)}<img src="img/du_av_other.webp" alt=""></div>`);
    r.querySelector(".x").onclick = (e) => { e.stopPropagation(); saveHist(history().filter((x) => x.id !== h.id)); net.deleteDuel(h.id).catch(() => {}); renderDuels(c); };
    r.onclick = () => {
      if (isNew) { saveHist(history().map((x) => (x.id === h.id ? { ...x, seen: true } : x))); net.markDuelSeen(h.id).catch(() => {}); renderDuels(c); }
      const b = el("div", "du-dlg", `<p>${S("duel_result_score_format", S("quick_match_you"), h.myScore)}</p><p>${S("duel_result_score_format", esc(h.otherName), h.otherScore)}</p>`);
      const ov = appWindow(c.$("duels"), { title: S(key), body: b, centerTitle: true, footer: primary(S("close"), () => ov.close()) });
    };
    rec.push(r);
  });
  root.append(card("b", S("duel_list_recent_title"), rec));
  // sent and still waiting
  const snt = [];
  if (!state.loading && !pending.length) snt.push(holder(empty("du_ic_out", S("duel_list_sent_empty"))));
  else pending.forEach((d) => {
    const r = el("div", "du-row", `<img src="img/du_av_other.webp" alt=""><div class="du-nm"><b>${esc(d.opponentName)}</b><small>${S("duel_list_status_waiting")}</small></div><img class="x" src="img/du_x.webp" alt="${S("duel_list_delete")}">`);
    r.querySelector(".x").onclick = () => net.deleteDuel(d.id).catch(() => {}); snt.push(r);
  });
  root.append(card("c", S("duel_list_sent_title"), snt));
}

/** CreateDuelScreen: how many words, then the normal round starts. */
export function openCreateDuel(c, opp) {
  const COUNTS = [10, 20, 30, 40, 50]; let count = 10;
  const body = el("div", "dn-body");
  const paint = () => {
    body.innerHTML = `<div class="dn-hero"><div class="dn-ic">🥊</div><b>${S("duel_create_opponent", esc(opp.name))}</b><p>${S("duel_create_explainer", esc(opp.name))}</p></div><div class="dn-q">${S("select_word_count")}</div><div class="dn-c">${COUNTS.map((n) => `<button class="${n === count ? "on" : ""}" data-n="${n}">${n}</button>`).join("")}</div>`;
    body.querySelectorAll("[data-n]").forEach((b) => (b.onclick = () => { count = +b.dataset.n; paint(); }));
  };
  paint();
  const ov = appWindow(c.$("stage"), { title: S("duel_create_title"), body, footer: primary(S("duel_create_start"), () => { ov.close(); c.startDuelChallenge(opp, count); }), cls: "dn-win" });
}
