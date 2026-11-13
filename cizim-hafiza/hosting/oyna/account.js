// Account screen. Play Store players use Google in the app; a browser account is a user name + password (see firebase.js), and its
// progress is saved to users/{uid}/backup/state (sync.js) so it follows the player to any browser.
import * as net from "./firebase.js";
import { S, useAppTexts, fmtNum } from "./i18n.js";
import { rankLevelHtml } from "./ranks.js";
import { syncNow, lastBackupAt, forgetRemote, markAccount } from "./sync.js";
import { frameRing, frameById } from "./store.js";
import { childMode } from "./agegate.js";
import { openNickEdit } from "./picker.js";

const T = {
  tr: { title: "Hesap", guest: "Misafir hesap", linked: "Hesapla giriş yapıldı", xp: "Toplam XP", games: "Oynanan oyun", streak: "En iyi seri (gün)", sec: "Hesap ve yedek", max: "En üst seviyedesin",
        hint: "Kullanıcı adı ve şifreyle hesap aç: ilerlemen kaydedilsin, istediğin tarayıcıdan kaldığın yerden devam et.", user: "Kullanıcı adı", pass: "Şifre", login: "Giriş Yap", create: "Hesap Oluştur",
        badName: "Kullanıcı adı 3-16 karakter olmalı: küçük harf, rakam, _ veya .", badPass: "Şifre en az 6 karakter olmalı.", taken: "Bu kullanıcı adı alınmış.", wrong: "Kullanıcı adı veya şifre yanlış.", off: "Hesap girişi şu an kapalı.", net: "Sunucuya ulaşılamadı.",
        created: "Hesabın oluşturuldu, ilerlemen kaydediliyor 🎉", welcome: "Hoş geldin — ilerlemen geri yüklendi", loggedIn: "Giriş yapıldı", progress: (a, b) => `${a} / ${b} XP` },
  en: { title: "Account", guest: "Guest account", linked: "Signed in", xp: "Total XP", games: "Games played", streak: "Best streak (days)", sec: "Account & backup", max: "You're at the top level",
        hint: "Create an account with a user name and password: your progress is saved and follows you to any browser.", user: "User name", pass: "Password", login: "Sign in", create: "Create account",
        badName: "User name must be 3-16 characters: lowercase letters, digits, _ or .", badPass: "Password must be at least 6 characters.", taken: "That user name is taken.", wrong: "Wrong user name or password.", off: "Signing in is switched off right now.", net: "Couldn't reach the server.",
        created: "Account created, saving your progress 🎉", welcome: "Welcome back — your progress was restored", loggedIn: "Signed in", progress: (a, b) => `${a} / ${b} XP` },
  pt: { title: "Conta", guest: "Conta de convidado", linked: "Conectado", xp: "XP total", games: "Partidas jogadas", streak: "Melhor sequência (dias)", sec: "Conta e backup", max: "Você está no nível máximo",
        hint: "Crie uma conta com nome de usuário e senha: seu progresso fica salvo e te acompanha em qualquer navegador.", user: "Nome de usuário", pass: "Senha", login: "Entrar", create: "Criar conta",
        badName: "O nome de usuário deve ter de 3 a 16 caracteres: letras minúsculas, números, _ ou .", badPass: "A senha deve ter pelo menos 6 caracteres.", taken: "Esse nome de usuário já está em uso.", wrong: "Nome de usuário ou senha errados.", off: "O login está desativado agora.", net: "Não foi possível falar com o servidor.",
        created: "Conta criada, salvando seu progresso 🎉", welcome: "Bem-vindo de volta — seu progresso foi restaurado", loggedIn: "Conectado", progress: (a, b) => `${a} / ${b} XP` },
  es: { title: "Cuenta", guest: "Cuenta de invitado", linked: "Sesión iniciada", xp: "XP total", games: "Partidas jugadas", streak: "Mejor racha (días)", sec: "Cuenta y respaldo", max: "Estás en el nivel más alto",
        hint: "Crea una cuenta con nombre de usuario y contraseña: tu progreso se guarda y te acompaña en cualquier navegador.", user: "Nombre de usuario", pass: "Contraseña", login: "Iniciar sesión", create: "Crear cuenta",
        badName: "El nombre de usuario debe tener de 3 a 16 caracteres: minúsculas, números, _ o .", badPass: "La contraseña debe tener al menos 6 caracteres.", taken: "Ese nombre de usuario ya está en uso.", wrong: "Nombre de usuario o contraseña incorrectos.", off: "El inicio de sesión está desactivado ahora.", net: "No se pudo conectar con el servidor.",
        created: "Cuenta creada, guardando tu progreso 🎉", welcome: "Bienvenido de nuevo — tu progreso se restauró", loggedIn: "Sesión iniciada", progress: (a, b) => `${a} / ${b} XP` },
};
// texts that are the app's own (strings.json), so they cannot drift from it
useAppTexts(T, { linked: "account_signed_in_title", max: "level_max_reached", user: "account_section_username", welcome: "account_signed_in_restored" });
let who = null, msg = "", busy = false, linkedNow = false;

// The painted scene (bg_account) is 836 x 1882; the web's art units are 841 x 1870, so every box below is converted.
const FX = 841 / 836, FY = 1870 / 1882;
export function layoutAccount(c) {
  const { place, $ } = c, pl = (el, x, y, w, h) => place(el, x * FX, y * FY, w * FX, h * FY);
  pl($("ak-back"), 14, 66, 124, 106); pl($("ak-title"), 215, 304, 410, 100);
  pl($("ak-avatar"), 78, 497, 220, 228);
  pl($("ak-name"), 335, 504, 347, 62); pl($("ak-edit"), 692, 505, 73, 73);
  pl($("ak-lvl"), 330, 563, 207, 42); pl($("ak-badge"), 330, 622, 382, 90);
  pl($("ak-bar"), 196, 777, 551, 43); pl($("ak-lv"), 98, 758, 84, 76);
  [166, 416.5, 669].forEach((cx, i) => pl($("ak-s" + i), cx - 100, 924, 200, 182));
  pl($("ak-sec"), 168, 1166, 500, 68);
  pl($("ak-cov1"), 70, 1286, 76, 78); pl($("ak-cov2"), 630, 1266, 148, 122);
  pl($("ak-strip"), 156, 1277, 472, 106); pl($("ak-form"), 62, 1285, 708, 90);
  pl($("ak-out"), 150, 1462, 535, 128); pl($("ak-login"), 150, 1462, 535, 128); pl($("ak-reg"), 250, 1722, 336, 66);
  pl($("ak-del"), 157, 1610, 516, 95); pl($("ak-msg"), 60, 1802, 716, 66);
}

function backupLine() {
  const t = lastBackupAt(); if (!t) return S("account_never_backed_up");
  const d = new Date(t), hm = d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }), day = (x) => Math.floor((x.getTime() - x.getTimezoneOffset() * 60000) / 864e5), diff = day(new Date()) - day(d);
  return S("account_last_backup_format", diff === 0 ? S("account_backup_today", hm) : diff === 1 ? S("account_backup_yesterday", hm) : d.toLocaleDateString() + " " + hm);
}
const errText = (x, e) => {
  const code = (e && e.code) || "";
  if (code === "auth/email-already-in-use" || code === "auth/credential-already-in-use") return x.taken;
  if (code === "auth/invalid-credential" || code === "auth/wrong-password" || code === "auth/user-not-found" || code === "auth/invalid-login-credentials") return x.wrong;
  if (code === "auth/operation-not-allowed") return x.off;
  if (code === "auth/weak-password") return x.badPass;
  if (code === "auth/network-request-failed") return x.net;
  return S("account_sign_in_failed");
};
/** Paints from what is known right now (the last sign-in state), so the screen is never blank while the Firebase SDK is still loading. */
/** A long name or address cut to about max characters without losing its end (the part after an @, or its last letters). */
const shorten = (t, max) => { if (t.length <= max) return t; const at = t.lastIndexOf("@"); if (at > 0) { const dom = t.slice(at), keep = Math.max(6, max - dom.length - 1); return at <= keep ? t : t.slice(0, keep) + "…" + dom; } return t.slice(0, max - 1) + "…"; };

function paint(c) {
  const { $, P, lang } = c, x = T[lang()];
  const linked = who && !who.anon;
  linkedNow = !!linked;
  $("ak-title").textContent = x.title;
  const av = frameRing(frameById(P.frame)); av.style.cssText += ";width:100%;height:100%"; $("ak-avatar").replaceChildren(av);
  $("ak-name").textContent = P.nick;
  const lv = c.levelOf(P.xp);
  $("ak-lvl").innerHTML = rankLevelHtml(lv.level);
  $("ak-badge").className = "txt badge" + (linked ? "" : " guest");
  // a linked account's pill has two lines, so a long user name has a whole line of its own: it is cut in the middle, never at its start
  { const b = document.createElement("span"); const top = document.createElement("b"); top.textContent = linked ? "✓ " + x.linked : x.guest; b.append(top);
    if (linked && who.userName) { const n = document.createElement("em"); n.textContent = shorten(who.userName, 24); b.append(n); } $("ak-badge").replaceChildren(b); }
  $("ak-lv").textContent = lv.level; $("ak-fill").style.width = lv.frac * 100 + "%"; $("ak-barl").textContent = lv.max ? x.max : x.progress(lv.into, lv.span);
  const stats = [[fmtNum(P.xp), x.xp], [fmtNum(P.stats.games), x.games], [fmtNum(P.stats.best), x.streak]];
  stats.forEach(([v, l], i) => { const s = $("ak-s" + i); s.children[1].textContent = v; s.children[2].textContent = l; });
  $("ak-sec").innerHTML = '<svg viewBox="0 0 24 24"><path fill="#fff" d="M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z"/></svg>' + `<span>${x.sec}</span>`;
  const saved = linked && !!lastBackupAt();
  $("ak-strip").innerHTML = linked ? `<b class="fitw">${S("account_sync_on")}</b><small class="fitw">${backupLine()}</small>` : x.hint;
  $("ak-strip").style.display = linked ? "" : "none"; // a guest's strip holds the sign-in fields instead
  $("ak-cov1").style.display = $("ak-cov2").style.display = saved ? "none" : "block"; // the strip's painted tick and cloud say "saved"
  const form = $("ak-form"); form.style.display = linked ? "none" : "";
  $("ak-user").placeholder = x.user; $("ak-pass").placeholder = x.pass; $("ak-login-t").textContent = x.login; $("ak-reg-t").textContent = x.create;
  $("ak-login").style.display = linked ? "none" : ""; $("ak-reg").style.display = linked ? "none" : "";
  $("ak-out").style.display = linked ? "" : "none"; $("ak-out-t").textContent = S("account_sign_out");
  $("ak-edit").onclick = () => openNickEdit(c);
  $("ak-del").textContent = S("account_delete_action"); $("ak-del").style.display = ""; // a guest has data on the server too (profile, rounds, league score), so it can be deleted as well
  const m = $("ak-msg"); m.textContent = msg; m.style.display = msg ? "" : "none";
  const done = (text) => { msg = text; busy = false; c.refreshHome(); renderAccount(c); };
  const read = () => {
    const name = $("ak-user").value.trim().toLowerCase(), pass = $("ak-pass").value;
    if (!net.validUserName(name)) { msg = x.badName; renderMsg(); return null; }
    if (pass.length < 6) { msg = x.badPass; renderMsg(); return null; }
    return { name, pass };
  };
  const renderMsg = () => { const e = $("ak-msg"); e.textContent = msg; e.style.display = msg ? "" : "none"; };
  const go = async (fn, okText) => {
    if (childMode()) { msg = S("child_feature_locked"); renderMsg(); return; } // a web account is for ages 13 and up (agegate.js)
    if (busy) return; const v = read(); if (!v) return; busy = true; msg = "…"; renderMsg();
    try {
      who = await fn(v.name, v.pass); markAccount(true); forgetRemote();
      const r = await syncNow(c); c.saveP();
      done(r === "restored" ? x.welcome : okText);
    } catch (e) { done(errText(x, e)); }
  };
  $("ak-reg").onclick = () => go(net.signUpUserName, x.created);
  $("ak-login").onclick = () => go(net.signInUserName, x.loggedIn);
  $("ak-pass").onkeydown = (e) => { if (e.key === "Enter") $("ak-login").click(); };
  $("ak-out").onclick = () => c.confirm(S("account_sign_out_message"), S("account_sign_out_confirm"), async () => {
    if ((await syncNow(c)) === "error") return done(S("account_sign_out_backup_failed"));
    try { await net.signOutAccount(); markAccount(false); c.resetProgress(); forgetRemote(); done(S("account_signed_out_done")); } catch { done(S("account_sign_out_failed")); }
  });
  $("ak-del").onclick = () => c.confirm(S("account_delete_message"), S("account_delete_continue"), () => c.confirm(S("account_delete_final_message"), S("account_delete_final_confirm"), async () => {
    try { await net.deleteAccount(); markAccount(false); c.resetProgress(); forgetRemote(); done(S("account_delete_done")); }
    catch (e) { done(e.code === "auth/requires-recent-login" ? S("account_delete_reauth_needed") : S("account_delete_failed")); }
  }));
}

export async function renderAccount(c) {
  paint(c);
  let w = null; try { w = await net.authState(); } catch {}
  if ((w && w.uid) !== (who && who.uid) || (w && w.anon) !== (who && who.anon) || (w && w.userName) !== (who && who.userName)) { who = w; paint(c); requestAnimationFrame(() => c.fit()); } // text that just changed is fitted again
}
