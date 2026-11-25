import * as net from "./firebase.js";
import { S, useAppTexts, getLang } from "./i18n.js";
import * as eco from "./economy.js";
import { openNow } from "./chests.js";
import { paintedCard, appWindow, primary } from "./ui.js";

const L = {
  tr: { title: "Ayarlar", sound: "Ses ve Titreşim", rows: ["Tüm Sesler", "Müzik", "Titreşim", "Bildirimler"], code: "Kupon kodu", use: "Kullan", how: "Kodlar nasıl kazanılır?",
        lang: "Dil", tut: "Eğitimi Tekrar İzle", bug: "Sorun Bildir", acc: "Hesap", rate: "Uygulamayı Puanla", priv: "Gizlilik Politikası", ver: "Web sürümü",
        won: (g, x, c) => [g && `+${g} altın`, x && `+${x} XP`, c && `Kasalar uygulamada geçerli`].filter(Boolean).join(" · ") || "Kod kabul edildi",
        err: { notfound: "Kod bulunamadı. Kontrol edip tekrar dene.", expired: "Bu kodun süresi dolmuş.", exhausted: "Bu kodun hakkı bitmiş.", used: "Bu kodu zaten kullandın.", network: "Sunucuya ulaşılamadı. Bağlantını kontrol et." },
        howBody: "Kupon kodlarını @karalakapp sosyal medya hesaplarımızdan paylaşıyoruz. Kaçırmamak için bizi Instagram ve TikTok'ta takip et!", close: "Kapat", soon: "Çok yakında", tutMsg: "Eğitim uygulamada var." },
  en: { title: "Settings", sound: "Sound & Vibration", rows: ["All Sounds", "Music", "Vibration", "Notifications"], code: "Coupon code", use: "Redeem", how: "How to get codes",
        lang: "Language", tut: "Replay Tutorial", bug: "Report a Bug", acc: "Account", rate: "Rate the App", priv: "Privacy Policy", ver: "Web version",
        won: (g, x, c) => [g && `+${g} gold`, x && `+${x} XP`, c && `Chests apply in the app`].filter(Boolean).join(" · ") || "Code accepted",
        err: { notfound: "Code not found. Check it and try again.", expired: "This code has expired.", exhausted: "This code has been used up.", used: "You have already used this code.", network: "Could not reach the server. Check your connection." },
        howBody: "We share coupon codes on our @karalakapp social accounts. Follow us on Instagram and TikTok so you don't miss one!", close: "Close", soon: "Coming soon", tutMsg: "The tutorial lives in the app." },
  pt: { title: "Ajustes", sound: "Som e Vibração", rows: ["Todos os Sons", "Música", "Vibração", "Notificações"], code: "Código de cupom", use: "Resgatar", how: "Como conseguir códigos",
        lang: "Idioma", tut: "Repetir Tutorial", bug: "Relatar um Problema", acc: "Conta", rate: "Avaliar o App", priv: "Política de Privacidade", ver: "Versão web",
        won: (g, x, c) => [g && `+${g} de ouro`, x && `+${x} XP`, c && `Os baús valem no app`].filter(Boolean).join(" · ") || "Código aceito",
        err: { notfound: "Código não encontrado. Confira e tente de novo.", expired: "Este código expirou.", exhausted: "Este código já se esgotou.", used: "Você já usou este código.", network: "Não foi possível falar com o servidor. Verifique sua conexão." },
        howBody: "Compartilhamos códigos de cupom nas nossas redes @karalakapp. Siga a gente no Instagram e no TikTok para não perder nenhum!", close: "Fechar", soon: "Em breve", tutMsg: "O tutorial fica no app." },
  es: { title: "Ajustes", sound: "Sonido y Vibración", rows: ["Todos los Sonidos", "Música", "Vibración", "Notificaciones"], code: "Código de cupón", use: "Canjear", how: "Cómo conseguir códigos",
        lang: "Idioma", tut: "Repetir Tutorial", bug: "Reportar un Problema", acc: "Cuenta", rate: "Calificar la App", priv: "Política de Privacidad", ver: "Versión web",
        won: (g, x, c) => [g && `+${g} de oro`, x && `+${x} XP`, c && `Los cofres valen en la app`].filter(Boolean).join(" · ") || "Código aceptado",
        err: { notfound: "Código no encontrado. Revísalo e inténtalo de nuevo.", expired: "Este código expiró.", exhausted: "Este código ya se agotó.", used: "Ya usaste este código.", network: "No se pudo conectar con el servidor. Revisa tu conexión." },
        howBody: "Compartimos códigos de cupón en nuestras redes @karalakapp. ¡Síguenos en Instagram y TikTok para no perderte ninguno!", close: "Cerrar", soon: "Próximamente", tutMsg: "El tutorial está en la app." },
};
const PRIV = "/gizlilik"; // the privacy policy (hosting/gizlilik), which links on to the terms of use
const RATE = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza";

/** Supported languages: add a line here (and its strings) when a new one ships, as in SupportedLanguage.kt. */
export const LANGS = [{ code: "en", label: "English", flag: "🇺🇸" }, { code: "tr", label: "Türkçe", flag: "🇹🇷" }, { code: "pt", label: "Português (Brasil)", flag: "🇧🇷" }, { code: "es", label: "Español", flag: "🇪🇸" }];
/** The app's language menu: a small list that drops under the row, the current language first, the rest in alphabetical order of their own names. */
function openLangMenu(c) {
  const row = c.$("se-lang"), cur = c.lang(), stage = c.$("stage");
  document.getElementById("lang-menu")?.remove();
  const veil = document.createElement("div"); veil.id = "lang-menu";
  const box = document.createElement("div"); box.className = "lm-box";
  [...LANGS].sort((a, b) => (b.code === cur) - (a.code === cur) || a.label.localeCompare(b.label)).forEach((l) => {
    const b = document.createElement("button"); b.className = "lm-item" + (l.code === cur ? " on" : ""); b.innerHTML = `<span>${l.flag}</span><span>${l.label}</span>`;
    b.onclick = () => { veil.remove(); if (l.code !== cur) c.setLang(l.code); }; box.append(b);
  });
  veil.append(box); veil.onclick = (e) => { if (e.target === veil) veil.remove(); }; stage.append(veil);
  const r = row.getBoundingClientRect(), s = stage.getBoundingClientRect();
  box.style.right = Math.max(8, s.right - r.right + 12) + "px"; box.style.top = Math.min(r.bottom - s.top - 6, s.height - box.offsetHeight - 12) + "px";
}

export function layoutSettings(c) {
  const { place, $ } = c;
  place($("se-back"), 28, 95, 114, 100); place($("se-title"), 196, 186, 304, 90); place($("se-sound"), 112, 358, 262, 48);
  [467, 562, 656, 751].forEach((cy, i) => place($("se-r" + i), 88, cy - 42, 664, 84));
  place($("se-code"), 353, 888, 399, 62); place($("se-use"), 353, 964, 190, 64); place($("se-how"), 556, 964, 196, 64); place($("se-msg"), 353, 1040, 399, 90);
  [["se-lang", 1237], ["se-tut", 1347], ["se-bug", 1457], ["se-acc", 1567], ["se-rate", 1677]].forEach(([id, cy]) => place($(id), 52, cy - 47, 738, 94));
  place($("se-priv"), 190, 1735, 460, 50);
}

export function renderSettings(c) {
  const { $, P, lang } = c, x = L[lang()];
  $("se-title").textContent = x.title; $("se-sound").textContent = x.sound;
  P.prefs = P.prefs || { s0: true, s1: true, s2: true, s3: false };
  x.rows.forEach((label, i) => {
    const r = $("se-r" + i), on = !!P.prefs["s" + i];
    r.innerHTML = `<span>${label}</span><i class="sw${on ? " on" : ""}"></i>`;
    r.onclick = () => { P.prefs["s" + i] = !P.prefs["s" + i]; if (i === 0) for (const k of [1, 2]) P.prefs["s" + k] = P.prefs.s0; c.saveP(); c.syncMusic(); renderSettings(c); };
  });
  $("se-code").placeholder = x.code; $("se-use-t").textContent = x.use; $("se-how").innerHTML = `<i>i</i><span>${x.how}</span>`; { const inp = $("se-code"), use = $("se-use"), sync = () => use.classList.toggle("dis", !inp.value.trim()); inp.addEventListener("input", sync); sync(); }
  const cl = LANGS.find((l) => l.code === lang()) || LANGS[0], flag = `${cl.flag} ${cl.label}`;
  const nav = (id, label, right) => { $(id).innerHTML = `<span>${label}</span><span>${right || ""}</span>`; };
  nav("se-lang", x.lang, flag); nav("se-tut", x.tut); nav("se-bug", x.bug); nav("se-acc", x.acc); nav("se-rate", x.rate);
  $("se-priv-t").textContent = "🛡 " + x.priv; $("se-ver").textContent = x.ver; $("se-priv").href = PRIV + "?lang=" + lang();
  // Wrong codes in a row (SettingsViewModel.miss): five, and the box rests for thirty seconds
  $("se-use").onclick = async () => {
    const code = $("se-code").value; if (!code.trim()) return;
    if (Date.now() < couponLockedUntil) { $("se-msg").textContent = S("coupon_error_wait"); return; }
    $("se-msg").textContent = "…";
    const r = await net.redeemCoupon(code).catch(() => ({ status: "network" }));
    if (r.status === "ok") {
      couponFails = 0; P.gold += r.gold; eco.addXp(P, r.xp, 0); c.saveP(); c.refreshHome();
      $("se-msg").textContent = [r.gold > 0 && S("coupon_won_gold", r.gold), r.xp > 0 && S("coupon_won_xp", r.xp), r.chests.length > 0 && S("coupon_won_chest")].filter(Boolean).join("  ·  ") || S("coupon_won_nothing");
      $("se-code").value = "";
      // each chest is opened right now, one opening scene after another
      const queue = [...r.chests], next = () => { const t = queue.shift(); if (t) openNow(c, t, next); }; next();
    } else {
      if (["notfound", "expired", "exhausted"].includes(r.status) && ++couponFails >= 5) { couponFails = 0; couponLockedUntil = Date.now() + 30000; }
      $("se-msg").textContent = S("coupon_error_" + ({ notfound: "not_found" }[r.status] || r.status));
    }
  };
  $("se-how").onclick = () => couponHow(c);
  $("se-lang").onclick = () => openLangMenu(c);
  $("se-tut").onclick = () => c.startTutorial();
  $("se-rate").onclick = () => window.open(RATE, "_blank", "noopener");
  $("se-bug").onclick = () => c.open("report"); $("se-acc").onclick = () => c.open("account");
}

let couponFails = 0, couponLockedUntil = 0;
useAppTexts(L, { code: "coupon_hint", use: "coupon_use", how: "coupon_how_button", howBody: "coupon_how_body", close: "coupon_how_close" });
const SOCIAL = { instagram: "https://www.instagram.com/karalakapp", tiktok: "https://www.tiktok.com/@karalakapp" };
// ls_coupon_frame (one per language: the buttons are painted in): [w, h], then left/top/right/bottom of close, Instagram, TikTok and the "close" word (coupon_art.xml)
const COUPON_ART = { tr: [987, 1371, 841, 294, 966, 419, 150, 818, 852, 976, 150, 1004, 852, 1154, 360, 1199, 625, 1270], es: [1064, 1478, 887, 302, 1027, 442, 165, 854, 885, 1028, 165, 1046, 885, 1218, 395, 1255, 650, 1315], pt: [1064, 1478, 887, 302, 1027, 442, 165, 856, 885, 1030, 165, 1052, 885, 1225, 395, 1262, 650, 1322] };
/** "Kupon kodunu nasıl kazanırım?": the painted window in Turkish, Spanish and Portuguese, the plain one in English (as in the app). */
export function couponHow(c) {
  const lang = getLang(), art = COUPON_ART[lang], open = (u) => window.open(u, "_blank", "noopener");
  if (art) {
    const card = paintedCard(c.$("stage"), { art: "ls_coupon_frame_" + lang, w: art[0], h: art[1], cls: "cph" });
    const hit = (i, label, fn) => { const e = card.put("pc-btn", art.slice(i, i + 4), ""); e.setAttribute("role", "button"); e.setAttribute("aria-label", label); e.onclick = fn; };
    hit(2, S("coupon_how_close"), () => card.close()); hit(6, S("coupon_how_instagram"), () => open(SOCIAL.instagram));
    hit(10, S("coupon_how_tiktok"), () => open(SOCIAL.tiktok)); hit(14, S("coupon_how_close"), () => card.close());
    return card;
  }
  const body = document.createElement("div"); body.className = "cph-plain"; body.textContent = S("coupon_how_body");
  const foot = document.createElement("div"); foot.className = "cph-foot";
  let ov; foot.append(primary(S("coupon_how_instagram"), () => open(SOCIAL.instagram)), primary(S("coupon_how_tiktok"), () => open(SOCIAL.tiktok)), primary(S("coupon_how_close"), () => ov.close(), "cream"));
  ov = appWindow(c.$("stage"), { title: S("coupon_how_title"), body, footer: foot });
  return ov;
}
