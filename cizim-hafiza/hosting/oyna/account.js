// Account screen. Play Store players use Google in the app; a browser account is a user name + password (see firebase.js), and its
// progress is saved to users/{uid}/backup/state (sync.js) so it follows the player to any browser.
import * as net from "./firebase.js";
import { S, useAppTexts, fmtNum } from "./i18n.js";
import { rankLevelHtml } from "./ranks.js";
import { syncNow, lastBackupAt, forgetRemote, markAccount } from "./sync.js";
import { frameRing, frameById } from "./store.js";

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

export function layoutAccount(c) {
  const { place, $ } = c;
  place($("ak-back"), 20, 40, 110, 106); place($("ak-sign"), 130, 132, 580, 387); place($("ak-title"), 219, 343, 420, 113);
  place($("ak-avatar"), 92, 545, 150, 150);
  place($("ak-name"), 268, 540, 502, 60); place($("ak-lvl"), 268, 604, 502, 44); place($("ak-badge"), 268, 654, 506, 58);
  place($("ak-bar"), 92, 726, 644, 36);
  [24, 294, 566].forEach((x, i) => place($("ak-s" + i), x + 8, 905, 220, 120));
  place($("ak-sec"), 166, 1066, 574, 68); place($("ak-strip"), 166, 1150, 576, 82);
  place($("ak-form"), 110, 1262, 620, 330);
  place($("ak-out"), 120, 1304, 600, 132);
  place($("ak-del"), 120, 1460, 600, 76); place($("ak-msg"), 60, linkedNow ? 1560 : 1620, 720, 80);
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
function paint(c) {
  const { $, P, lang } = c, x = T[lang()];
  const linked = who && !who.anon;
  if (!!linked !== linkedNow) { linkedNow = !!linked; layoutAccount(c); }
  $("ak-title").textContent = x.title;
  const av = frameRing(frameById(P.frame)); av.style.cssText += ";width:100%;height:100%"; $("ak-avatar").replaceChildren(av);
  $("ak-name").textContent = P.nick;
  const lv = c.levelOf(P.xp);
  $("ak-lvl").innerHTML = rankLevelHtml(lv.level);
  $("ak-badge").className = "txt left badge" + (linked ? "" : " guest"); $("ak-badge").innerHTML = `<span>${linked ? [x.linked, who.userName].filter(Boolean).join(" · ") : x.guest}</span>`;
  $("ak-fill").style.width = lv.frac * 100 + "%"; $("ak-barl").textContent = lv.max ? x.max : x.progress(lv.into, lv.span);
  const stats = [[fmtNum(P.xp), x.xp], [fmtNum(P.stats.games), x.games], [fmtNum(P.stats.best), x.streak]];
  stats.forEach(([v, l], i) => { const s = $("ak-s" + i); s.children[0].textContent = v; s.children[1].textContent = l; });
  $("ak-sec").textContent = x.sec; $("ak-strip").innerHTML = linked ? `<b class="fitw">${S("account_sync_on")}</b><small class="fitw">${backupLine()}</small>` : x.hint;
  const form = $("ak-form"); form.style.display = linked ? "none" : "";
  $("ak-user").placeholder = x.user; $("ak-pass").placeholder = x.pass; $("ak-login-t").textContent = x.login; $("ak-reg-t").textContent = x.create;
  $("ak-out").style.display = linked ? "" : "none"; $("ak-out-t").textContent = S("account_sign_out");
  $("ak-del").textContent = "🗑 " + S("account_delete_action"); $("ak-del").style.display = linked ? "" : "none";
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
