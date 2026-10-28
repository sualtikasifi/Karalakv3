// Karalak Panel (web): the developer panel's player, economy and announcement tools on a big screen.
// Its own Firebase app instance ("panel"), so signing in here never replaces the anonymous session of the game on the same origin.
// Everything is enforced by firestore.rules (reviewer account only); nothing here is a secret.
import { firebaseConfig } from "../oyna/config.js";

const V = "10.14.1", CDN = `https://www.gstatic.com/firebasejs/${V}`;
const $ = (s) => document.querySelector(s);
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const when = (ms) => (ms > 0 ? new Date(ms).toLocaleString("tr", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" }) : "-");
const tsMs = (t) => (t && t.toMillis ? t.toMillis() : 0);

// The economy's fields and their ranges — keep in step with util/EconomyConfig.kt.
const ECONOMY = [
  ["goldPerCorrectWord", "Doğru kelime başı altın", 1, 1000, 10], ["onlineWinGold", "Çevrimiçi galibiyet altını", 0, 10000, 100],
  ["achievementGold", "Başarım altını", 0, 10000, 100], ["levelGoldPerNewStar", "Yeni yıldız başı altın", 0, 10000, 50],
  ["dailyGoldBase", "Günlük ödül (taban)", 0, 100000, 300], ["dailyGoldPerStreakDay", "Seri günü başı ek altın", 0, 10000, 30],
  ["chainBonusGold", "Zincir bonusu", 0, 1000, 10], ["adGoldAmount", "Reklam altını", 0, 100000, 500],
  ["adGoldCooldownHours", "Reklam altını bekleme (saat)", 1, 48, 3], ["adChestCooldownHours", "Reklam kasası bekleme (saat)", 1, 48, 4],
  ["leagueMonthlyGold", "Aylık lig altını", 0, 1000000, 10000],
];
const JOBS = [
  ["build-global-leaderboard", "Küresel lig özeti (saatlik)"], ["process-admin-commands", "Panel komutları (saatlik)"],
  ["rollup-stats", "Günlük istatistik özeti (gece)"], ["cleanup-abandoned-rooms", "Oda temizliği (gece)"],
  ["finalize-league-period", "Lig kapanışı (gece)"], ["grant-referral-rewards", "Davet ödülleri (gece)"], ["process-deletions", "Hesap silme (gece)"],
];
const LANGS = [["tr", "Türkçe"], ["en", "English"], ["pt", "Português"], ["es", "Español"]];
const LADDER_HOURS = [6, 12, 24, 72, 168];

let fb, auth, db, user;

async function boot() {
  const [app, au, fs] = await Promise.all([import(`${CDN}/firebase-app.js`), import(`${CDN}/firebase-auth.js`), import(`${CDN}/firebase-firestore.js`)]);
  const a = app.initializeApp(firebaseConfig, "panel");
  auth = au.getAuth(a); db = fs.getFirestore(a); fb = { au, fs };
  await auth.authStateReady?.();
  $("#signin").onclick = async () => {
    try { await au.signInWithPopup(auth, new au.GoogleAuthProvider()); } catch (e) { $("#gate-msg").textContent = "Giriş olmadı: " + (e.code || e.message); }
  };
  $("#signout").onclick = () => au.signOut(auth);
  au.onAuthStateChanged(auth, (u) => { user = u && !u.isAnonymous ? u : null; show(); });
}

function show() {
  $("#gate").hidden = !!user; $("#app").hidden = !user; $("#signout").hidden = !user;
  $("#who").textContent = user ? user.email : "";
  if (user) tabs();
}

const TABS = { reports: ["Oyuncu raporları", reportsView], players: ["Oyuncu ara", playersView], summary: ["Özet", summaryView], economy: ["Ekonomi", economyView],
  announce: ["Duyuru", announceView], push: ["Bildirim", pushView], jobs: ["İşler", jobsView], audit: ["Denetim", auditView] };
let current = "reports";
function tabs() {
  $("#tabs").innerHTML = Object.entries(TABS).map(([k, [t]]) => `<button data-k="${k}" class="${k === current ? "on" : ""}">${t}</button>`).join("");
  $("#tabs").querySelectorAll("button").forEach((b) => (b.onclick = () => { current = b.dataset.k; tabs(); }));
  const v = $("#view"); v.innerHTML = '<p class="mute">Yükleniyor…</p>';
  TABS[current][1](v).catch((e) => { v.innerHTML = `<p class="bad">Hata: ${esc(e.code || e.message)}</p>`; });
}

const q = () => fb.fs;

async function audit(action, target, detail) {
  try { await q().addDoc(q().collection(db, "adminAudit"), { action, target, detail: String(detail || "").slice(0, 200), by: user.email || "", at: q().serverTimestamp() }); } catch {}
}

// ---------------------------------------------------------------- player actions (mirror util/PlayerAdmin.kt)
async function resetName(uid, old) {
  const fresh = "Oyuncu" + (1000 + Math.floor(Math.random() * 9000));
  await q().updateDoc(q().doc(db, "users", uid), { nickname: fresh, username: fresh, nameResetAt: Date.now() });
  await audit("name-reset", uid, `"${old}" -> "${fresh}"`); return fresh;
}
async function lockPlayer(uid) {
  const { doc, getDoc, writeBatch, collection } = q();
  const ref = doc(db, "moderationStrikes", uid);
  const prev = (await getDoc(ref)).data()?.count || 0, strike = prev + 1, now = Date.now();
  const until = now + LADDER_HOURS[Math.min(strike, 5) - 1] * 3600000;
  const b = writeBatch(db);
  b.set(doc(collection(db, "penalties")), { uid, xpRevoked: 0, goldRevoked: 0, strike, lockedUntil: until, appliedAt: 0, createdAt: now });
  b.set(ref, { count: strike, updatedAt: now });
  await b.commit(); await audit("lock", uid, `ceza ${strike}`); return until;
}
async function unlockPlayer(uid) {
  const { collection, query, where, getDocs, doc, getDoc, writeBatch } = q();
  const now = Date.now();
  const running = (await getDocs(query(collection(db, "penalties"), where("uid", "==", uid)))).docs.filter((d) => (d.data().lockedUntil || 0) > now);
  if (!running.length) return 0;
  const ref = doc(db, "moderationStrikes", uid);
  const prev = (await getDoc(ref)).data()?.count || 0;
  const b = writeBatch(db);
  running.forEach((d) => {
    const u = { lockedUntil: 0, canceledAt: now };
    if (!(d.data().appliedAt > 0)) Object.assign(u, { xpRevoked: 0, goldRevoked: 0, appliedAt: now });
    b.update(d.ref, u);
  });
  b.set(ref, { count: Math.max(0, prev - running.length), updatedAt: now });
  await b.commit(); await audit("unlock", uid, `${running.length} kilit`); return running.length;
}
async function grant(uid, gold, xp, note) {
  await q().addDoc(q().collection(db, "adminCommands"), { type: "grant", uid, gold, xp, note: note.slice(0, 60), by: user.email || "", createdAt: q().serverTimestamp() });
  await audit("grant", uid, `altın ${gold}, XP ${xp}: ${note}`);
}

// ---------------------------------------------------------------- views
async function reportsView(v) {
  const { collection, getDocs, limit, query, writeBatch } = q();
  const docs = (await getDocs(query(collection(db, "playerReports"), limit(400)))).docs;
  const groups = new Map();
  for (const d of docs) {
    const r = d.data(); const g = groups.get(r.reportedUid) || { uid: r.reportedUid, nick: "", reasons: {}, ctx: new Set(), last: 0, ids: [] };
    g.nick ||= r.nickname || ""; g.reasons[r.reason] = (g.reasons[r.reason] || 0) + 1; if (r.context) g.ctx.add(r.context);
    g.last = Math.max(g.last, tsMs(r.createdAt)); g.ids.push(d.ref); groups.set(r.reportedUid, g);
  }
  const list = [...groups.values()].sort((a, b) => b.ids.length - a.ids.length || b.last - a.last);
  v.innerHTML = (list.length ? "" : '<p class="mute">Bekleyen oyuncu raporu yok.</p>') + list.map((g, i) => `
    <div class="card"><h3>${esc(g.nick || g.uid.slice(0, 8))} · ${g.ids.length} rapor</h3>
    <div class="mute">${Object.entries(g.reasons).map(([k, n]) => `${esc(k)} ×${n}`).join("  ")} ${g.ctx.size ? "· " + esc([...g.ctx].join(", ")) : ""} · son: ${when(g.last)}</div>
    <div class="mute">${esc(g.uid)}</div>
    <div class="row" style="margin-top:8px"><button class="ghost" data-a="name" data-i="${i}">Adı sıfırla</button><button class="ghost" data-a="lock" data-i="${i}">Kilitle</button><button class="primary" data-a="close" data-i="${i}">Kapat</button></div>
    <div class="msg" id="m${i}"></div></div>`).join("");
  v.querySelectorAll("button[data-a]").forEach((b) => (b.onclick = async () => {
    const g = list[+b.dataset.i], m = $(`#m${b.dataset.i}`); b.disabled = true;
    try {
      if (b.dataset.a === "name") m.textContent = "Yeni ad: " + (await resetName(g.uid, g.nick));
      else if (b.dataset.a === "lock") m.textContent = "Kilitli: " + when(await lockPlayer(g.uid));
      else { const w = writeBatch(db); g.ids.forEach((r) => w.delete(r)); await w.commit(); await audit("report-dismiss", g.uid, `${g.ids.length} rapor`); b.closest(".card").remove(); }
    } catch (e) { m.textContent = "Olmadı: " + (e.code || e.message); b.disabled = false; }
  }));
}

async function playersView(v) {
  v.innerHTML = `<div class="row"><input id="pq" placeholder="Ad, arkadaş kodu ya da kullanıcı kimliği" style="flex:1"><button class="primary" id="go">Ara</button></div><div id="pr"></div>`;
  const run = async () => {
    const { doc, getDoc, getDocs, collection, query, where, limit } = q();
    const text = $("#pq").value.trim(), out = $("#pr"); if (!text) return;
    out.innerHTML = '<p class="mute">Aranıyor…</p>';
    const uids = new Set();
    if (text.length >= 20 && !/\s/.test(text)) uids.add(text);
    if (/^\d{6}$/.test(text)) { const f = await getDoc(doc(db, "friendCodes", text)); if (f.exists()) uids.add(f.data().uid); }
    const key = text.trim().normalize("NFD").replace(/\p{M}+/gu, "").replace(/[ıİ]/g, "i").toLowerCase().replace(/ /g, "_");
    const un = await getDoc(doc(db, "usernames", key)); if (un.exists()) uids.add(un.data().uid);
    (await getDocs(query(collection(db, "users"), where("nickname", "==", text), limit(5)))).docs.forEach((d) => uids.add(d.id));
    const cards = [];
    for (const uid of [...uids].slice(0, 5)) {
      const u = await getDoc(doc(db, "users", uid)); if (!u.exists()) continue; const d = u.data(), now = Date.now();
      const strikes = (await getDoc(doc(db, "moderationStrikes", uid))).data()?.count || 0;
      const locked = Math.max(0, ...(await getDocs(query(collection(db, "penalties"), where("uid", "==", uid)))).docs.map((x) => x.data().lockedUntil || 0));
      const reports = (await getDocs(query(collection(db, "playerReports"), where("reportedUid", "==", uid)))).size;
      cards.push({ uid, d, strikes, locked: locked > now ? locked : 0, reports });
    }
    out.innerHTML = cards.length ? cards.map((c, i) => `
      <div class="card"><h3>${esc(c.d.nickname || "(adsız)")}</h3>
      <div class="mute">Seviye ${c.d.level ?? 0} · bu ay ${c.d.periodXp ?? 0} XP · kod ${esc(c.d.friendCode || "-")}</div>
      <div class="${c.locked ? "bad" : "mute"}">Ceza: ${c.strikes} · rapor: ${c.reports}${c.locked ? " · KİLİTLİ " + when(c.locked) + "'e kadar" : ""}</div>
      <div class="mute">${esc(c.uid)}</div>
      <div class="row" style="margin-top:8px"><button class="ghost" data-a="name" data-i="${i}">Adı sıfırla</button>
      <button class="ghost" data-a="${c.locked ? "unlock" : "lock"}" data-i="${i}">${c.locked ? "Kilidi aç" : "Kilitle"}</button></div>
      <div class="row" style="margin-top:8px"><input type="number" id="g${i}" placeholder="Altın" style="width:110px"><input type="number" id="x${i}" placeholder="XP" style="width:110px">
      <input id="n${i}" placeholder="Not" style="flex:1"><button class="primary" data-a="grant" data-i="${i}">Telafi gönder</button></div>
      <div class="msg" id="m${i}"></div></div>`).join("") : '<p class="mute">Böyle bir oyuncu bulunamadı.</p>';
    out.querySelectorAll("button[data-a]").forEach((b) => (b.onclick = async () => {
      const c = cards[+b.dataset.i], m = $(`#m${b.dataset.i}`); b.disabled = true;
      try {
        if (b.dataset.a === "name") m.textContent = "Yeni ad: " + (await resetName(c.uid, c.d.nickname || ""));
        else if (b.dataset.a === "lock") m.textContent = "Kilitli: " + when(await lockPlayer(c.uid));
        else if (b.dataset.a === "unlock") m.textContent = (await unlockPlayer(c.uid)) + " kilit kaldırıldı.";
        else {
          const gold = Math.max(0, Math.min(100000, +$(`#g${b.dataset.i}`).value | 0)), xp = Math.max(0, Math.min(100000, +$(`#x${b.dataset.i}`).value | 0));
          if (gold + xp <= 0) throw new Error("Miktar yaz");
          await grant(c.uid, gold, xp, $(`#n${b.dataset.i}`).value || "telafi"); m.textContent = "Sıraya alındı; oyuncunun uygulaması bir sonraki açılışında alır.";
        }
      } catch (e) { m.textContent = "Olmadı: " + (e.code || e.message); }
      b.disabled = false;
    }));
  };
  $("#go").onclick = run; $("#pq").onkeydown = (e) => e.key === "Enter" && run();
}

async function summaryView(v) {
  const { collection, getDocs, query, orderBy, limit } = q();
  const rows = (await getDocs(query(collection(db, "adminSummary"), orderBy("day", "desc"), limit(31)))).docs.map((d) => d.data());
  const sum = (c, p, s) => Object.entries(c || {}).filter(([k]) => k.startsWith(p) && k.endsWith(s)).reduce((a, [, n]) => a + n, 0);
  const pct = (x) => (x == null ? "-" : Math.round(x * 100) + "%");
  v.innerHTML = rows.length ? `<table><tr><th>Gün</th><th>Oyuncu</th><th>Yeni</th><th>D1</th><th>D7</th><th>Açılış</th><th>Oyun</th><th>Reklam</th></tr>${rows.map((r) =>
    `<tr><td>${esc(r.day)}</td><td>${r.players}</td><td>${r.newPlayers}</td><td>${pct(r.d1Return)}</td><td>${pct(r.d7Return)}</td><td>${r.counts?.app__open__count || 0}</td><td>${sum(r.counts, "games__finished__", "")}</td><td>${sum(r.counts, "ad__", "__reward_earned")}</td></tr>`).join("")}</table>`
    : '<p class="mute">Özet henüz yok: gece işi ("rollup-stats") ilk çalışınca dolar.</p>';
}

async function economyView(v) {
  const { doc, getDoc, setDoc } = q(); const ref = doc(db, "config", "economy"); const data = (await getDoc(ref)).data() || {};
  v.innerHTML = `<div class="grid">${ECONOMY.map(([k, label, lo, hi, def]) => `<div class="card"><label>${esc(label)} (${lo}–${hi}, varsayılan ${def})</label><input type="number" data-k="${k}" min="${lo}" max="${hi}" value="${Number.isInteger(data[k]) && data[k] >= lo && data[k] <= hi ? data[k] : def}"></div>`).join("")}</div>
    <p class="msg" id="em"></p><button class="primary" id="save">Kaydet</button>`;
  $("#save").onclick = async () => {
    const changed = {};
    for (const [k, label, lo, hi, def] of ECONOMY) {
      const n = Number(v.querySelector(`[data-k="${k}"]`).value), was = Number.isInteger(data[k]) && data[k] >= lo && data[k] <= hi ? data[k] : def;
      if (!Number.isInteger(n) || n < lo || n > hi) { $("#em").textContent = `${label}: ${lo} ile ${hi} arasında olmalı.`; return; }
      if (n !== was) changed[k] = n;
    }
    if (!Object.keys(changed).length) { $("#em").textContent = "Değişiklik yok."; return; }
    try { await setDoc(ref, changed, { merge: true }); await audit("economy", "config/economy", Object.entries(changed).map(([k, n]) => `${k}=${n}`).join(", ")); Object.assign(data, changed); $("#em").textContent = `Kaydedildi (${Object.keys(changed).length} alan). Telefonlara yarım saat içinde ulaşır.`; }
    catch (e) { $("#em").textContent = "Yazılamadı: " + (e.code || e.message); }
  };
}

async function announceView(v) {
  const { doc, getDoc, setDoc } = q(); const ref = doc(db, "config", "announcement"); const cur = (await getDoc(ref)).data() || {};
  v.innerHTML = `<p>${cur.active ? "Şu an yayında." : "Şu an yayında duyuru yok."}</p>` + LANGS.map(([l, n]) => `<div class="card"><h3>${n}</h3>
    <label>Başlık</label><input data-l="${l}" data-f="title" maxlength="60" value="${esc(cur.texts?.[l]?.title)}">
    <label>Metin</label><textarea data-l="${l}" data-f="body" rows="3" maxlength="400">${esc(cur.texts?.[l]?.body)}</textarea></div>`).join("") + `
    <label>Kaç gün yayında kalsın (0 = süresiz)</label><input id="days" type="number" value="7" style="width:120px">
    <p class="msg" id="am"></p><div class="row"><button class="primary" id="pub">Yayınla</button><button class="ghost" id="off">Kapat</button></div>`;
  const texts = () => Object.fromEntries(LANGS.map(([l]) => [l, { title: v.querySelector(`[data-l="${l}"][data-f=title]`).value.trim(), body: v.querySelector(`[data-l="${l}"][data-f=body]`).value.trim() }]).filter(([, t]) => t.title || t.body));
  const write = async (active) => {
    const t = texts(); if (active && !(t.tr?.title || t.tr?.body)) { $("#am").textContent = "Türkçe metin zorunlu."; return; }
    const days = Math.max(0, +$("#days").value | 0), now = Date.now();
    try { await setDoc(ref, { active, id: String(now), untilMillis: days > 0 ? now + days * 86400000 : 0, texts: t }); await audit(active ? "announce" : "announce-off", "config/announcement", t.tr?.title || ""); $("#am").textContent = active ? "Yayında. Telefonlara yarım saat içinde ulaşır." : "Duyuru kapatıldı."; }
    catch (e) { $("#am").textContent = "Yazılamadı: " + (e.code || e.message); }
  };
  $("#pub").onclick = () => write(true); $("#off").onclick = () => write(false);
}

async function pushView(v) {
  const { collection, addDoc, getDocs, query, where, limit, serverTimestamp } = q();
  const recent = (await getDocs(query(collection(db, "adminCommands"), where("type", "==", "broadcast"), limit(10)))).docs.map((d) => d.data()).sort((a, b) => tsMs(b.createdAt) - tsMs(a.createdAt));
  v.innerHTML = `<p class="mute">Bildirimi açık olan 13 yaş ve üstü tüm oyunculara gider. Saat başı çalışan iş gönderir.</p>
    <label>Başlık</label><input id="pt" maxlength="60"><label>Metin</label><textarea id="pb" rows="3" maxlength="200"></textarea>
    <p class="msg" id="pm"></p><button class="primary" id="send">Gönder</button>
    <h3>Son gönderilenler</h3>${recent.map((r) => `<div class="mute">${when(tsMs(r.createdAt))} · ${esc(r.title)} · ${r.doneAt ? (r.error ? "HATA: " + esc(r.error) : "gönderildi") : "sırada"}</div>`).join("")}`;
  $("#send").onclick = async () => {
    const title = $("#pt").value.trim(), body = $("#pb").value.trim(); if (!title || !body) return;
    if (!confirm(`Herkese gönderilsin mi?\n\n${title}\n${body}`)) return;
    try { await addDoc(collection(db, "adminCommands"), { type: "broadcast", title, body, by: user.email || "", createdAt: serverTimestamp() }); await audit("broadcast", "news", title); $("#pm").textContent = "Sıraya alındı."; $("#pt").value = $("#pb").value = ""; }
    catch (e) { $("#pm").textContent = "Yazılamadı: " + (e.code || e.message); }
  };
}

async function jobsView(v) {
  const { doc, getDoc, getDocs, collection, getCountFromServer, limit, query } = q();
  const rows = await Promise.all(JOBS.map(async ([k, label]) => ({ label, d: (await getDoc(doc(db, "adminJobs", k))).data() })));
  const del = await getCountFromServer(collection(db, "deletionRequests")).then((s) => s.data().count).catch(() => "?");
  const cmds = (await getDocs(query(collection(db, "adminCommands"), limit(200)))).docs.filter((d) => !d.data().doneAt).length;
  v.innerHTML = `<p>Bekleyen hesap silme talebi: <b>${del}</b> · bekleyen panel komutu: <b>${cmds}</b></p>` + rows.map(({ label, d }) => {
    const last = tsMs(d?.lastRunAt), stale = last && Date.now() - last > 36 * 3600000, bad = d && (d.ok === false || stale);
    return `<div class="card"><h3>${esc(label)}</h3><div class="${bad ? "bad" : "mute"}">${!d ? "Henüz kayıt yok" : d.ok === false ? "HATA · " + when(last) + " · " + esc(d.error) : stale ? "Uzun süredir çalışmadı · " + when(last) : "Çalıştı · " + when(last)}</div></div>`;
  }).join("");
}

async function auditView(v) {
  const { collection, getDocs, query, orderBy, limit } = q();
  const rows = (await getDocs(query(collection(db, "adminAudit"), orderBy("at", "desc"), limit(100)))).docs.map((d) => d.data());
  v.innerHTML = rows.length ? rows.map((r) => `<div class="card"><h3>${esc(r.action)} → ${esc(String(r.target).slice(0, 28))}</h3><div>${esc(r.detail)}</div><div class="mute">${when(tsMs(r.at))} · ${esc(String(r.by).split("@")[0])}</div></div>`).join("") : '<p class="mute">Kayıt yok.</p>';
}

boot().catch((e) => { $("#gate-msg").textContent = "Başlatılamadı: " + (e.message || e); });
