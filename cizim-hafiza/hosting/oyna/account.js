import * as net from "./firebase.js";
import { S } from "./i18n.js";
import { syncNow, lastBackupAt, forgetRemote } from "./sync.js";
import { frameRing, frameById } from "./store.js";

const T = {
  tr: { title: "Hesap", guest: "Misafir hesap", linked: "Google ile bağlı", xp: "Toplam XP", games: "Oynanan oyun", streak: "En iyi seri (gün)", sec: "Hesap ve yedek", lvl: (n) => `Seviye ${n}`, max: "En üst seviyedesin",
        hint: "Google hesabını bağla: kullanıcı adın ve ilerlemen bu tarayıcıda hesabınla görünsün.", linkedNote: "Giriş yapıldı. Altın ve XP bu tarayıcıda tutulur.", signIn: "Google ile Giriş Yap", signOut: "Çıkış Yap", del: "Hesabı Sil", delMsg: "Hesap silme işlemini uygulamadan yapabilirsin.",
        failed: "Giriş yapılamadı. Tekrar dene.", noNet: "Sunucuya ulaşılamadı.", out: "Çıkış yapıldı.", progress: (a, b) => `${a} / ${b} XP` },
  en: { title: "Account", guest: "Guest account", linked: "Linked with Google", xp: "Total XP", games: "Games played", streak: "Best streak (days)", sec: "Account & backup", lvl: (n) => `Level ${n}`, max: "You're at the top level",
        hint: "Link your Google account so your name and progress show under your account in this browser.", linkedNote: "Signed in. Gold and XP stay in this browser.", signIn: "Sign in with Google", signOut: "Sign out", del: "Delete account", delMsg: "You can delete your account from the app.",
        failed: "Couldn't sign in. Try again.", noNet: "Couldn't reach the server.", out: "Signed out.", progress: (a, b) => `${a} / ${b} XP` },
};
let who = null, msg = "";
const G = '<svg viewBox="0 0 48 48"><path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9.1 3.6l6.8-6.8C35.8 2.4 30.3 0 24 0 14.6 0 6.5 5.4 2.6 13.2l7.9 6.1C12.4 13.6 17.7 9.5 24 9.5z"/><path fill="#4285F4" d="M46.5 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.7c-.6 3-2.3 5.5-4.8 7.2l7.5 5.8c4.4-4.1 7.1-10.1 7.1-17.5z"/><path fill="#FBBC05" d="M10.5 28.7a14.5 14.5 0 0 1 0-9.4l-7.9-6.1a24 24 0 0 0 0 21.6l7.9-6.1z"/><path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.5-5.8c-2.1 1.4-4.9 2.3-8.4 2.3-6.3 0-11.6-4.1-13.5-9.8l-7.9 6.1C6.5 42.6 14.6 48 24 48z"/></svg>';

export function layoutAccount(c) {
  const { place, $ } = c;
  place($("ak-back"), 20, 40, 110, 106); place($("ak-sign"), 130, 132, 580, 387); place($("ak-title"), 219, 325, 420, 113);
  place($("ak-avatar"), 92, 545, 150, 150);
  place($("ak-name"), 268, 540, 502, 60); place($("ak-lvl"), 268, 604, 502, 44); place($("ak-badge"), 268, 654, 506, 58);
  place($("ak-bar"), 92, 726, 644, 36);
  [24, 294, 566].forEach((x, i) => place($("ak-s" + i), x + 8, 905, 220, 120));
  place($("ak-sec"), 166, 1066, 574, 68); place($("ak-strip"), 166, 1158, 576, 64);
  place($("ak-google"), 130, 1318, 580, 122); place($("ak-out"), 120, 1292, 600, 172);
  place($("ak-del"), 120, 1490, 600, 80); place($("ak-msg"), 60, 1590, 720, 80);
}

function backupLine() {
  const t = lastBackupAt(); if (!t) return S("account_never_backed_up");
  const d = new Date(t), hm = d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }), day = (x) => Math.floor((x.getTime() - x.getTimezoneOffset() * 60000) / 864e5), diff = day(new Date()) - day(d);
  return S("account_last_backup_format", diff === 0 ? S("account_backup_today", hm) : diff === 1 ? S("account_backup_yesterday", hm) : d.toLocaleDateString() + " " + hm);
}
export async function renderAccount(c) {
  const { $, P, lang } = c, x = T[lang()];
  try { who = await net.authState(); } catch { who = null; }
  const linked = who && !who.anon;
  $("ak-title").textContent = x.title;
  const av = frameRing(frameById(P.frame)); av.style.cssText += ";width:100%;height:100%"; $("ak-avatar").replaceChildren(av);
  $("ak-name").textContent = linked && who.name ? who.name : P.nick;
  const lv = c.levelOf(P.xp);
  $("ak-lvl").textContent = x.lvl(lv.level);
  $("ak-badge").className = "txt left badge" + (linked ? "" : " guest"); $("ak-badge").innerHTML = `<span>${linked ? [x.linked, who.email].filter(Boolean).join(" · ") : x.guest}</span>`;
  $("ak-fill").style.width = lv.frac * 100 + "%"; $("ak-barl").textContent = lv.max ? x.max : x.progress(lv.into, lv.span);
  const stats = [[P.xp.toLocaleString(), x.xp], [P.stats.games.toLocaleString(), x.games], [P.stats.best.toLocaleString(), x.streak]];
  stats.forEach(([v, l], i) => { const s = $("ak-s" + i); s.children[0].textContent = v; s.children[1].textContent = l; });
  $("ak-sec").textContent = x.sec; $("ak-strip").innerHTML = linked ? `<b>${S("account_sync_on")}</b><small>${backupLine()}</small>` : S("account_guest_hint");
  $("ak-google").innerHTML = G + `<span>${x.signIn}</span>`; $("ak-google").style.display = linked ? "none" : "";
  $("ak-out").style.display = linked ? "" : "none"; $("ak-out-t").textContent = S("account_sign_out");
  $("ak-del").textContent = "🗑 " + S("account_delete_action");
  const m = $("ak-msg"); m.textContent = msg; m.style.display = msg ? "" : "none";
  const done = (text) => { msg = text; c.refreshHome(); renderAccount(c); };
  $("ak-google").onclick = async () => {
    try {
      who = await net.signInGoogle(); forgetRemote();
      const r = await syncNow(c); c.saveP();
      done(r === "restored" ? S("account_signed_in_restored") : r === "saved" ? S("account_signed_in_progress_kept") : S("account_signed_in_title"));
    } catch (e) { done(e.code === "auth/popup-closed-by-user" || e.code === "auth/cancelled-popup-request" ? "" : S("account_sign_in_failed")); }
  };
  $("ak-out").onclick = () => c.confirm(S("account_sign_out_message"), S("account_sign_out_confirm"), async () => {
    if ((await syncNow(c)) === "error") return done(S("account_sign_out_backup_failed"));
    try { await net.signOutGoogle(); c.resetProgress(); forgetRemote(); done(S("account_signed_out_done")); } catch { done(S("account_sign_out_failed")); }
  });
  $("ak-del").onclick = () => c.confirm(S("account_delete_message"), S("account_delete_continue"), () => c.confirm(S("account_delete_final_message"), S("account_delete_final_confirm"), async () => {
    try { await net.deleteAccount(); c.resetProgress(); forgetRemote(); done(S("account_delete_done")); }
    catch (e) { done(e.code === "auth/requires-recent-login" ? S("account_delete_reauth_needed") : S("account_delete_failed")); }
  }));
}
