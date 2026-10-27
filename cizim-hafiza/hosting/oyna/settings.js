import * as net from "./firebase.js";

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
const PRIV = "https://sualtikasifi.github.io/app-ads/";
const RATE = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza";

/** Supported languages: add a line here (and its strings) when a new one ships, as in SupportedLanguage.kt. */
export const LANGS = [{ code: "en", label: "English", flag: "🇺🇸" }, { code: "tr", label: "Türkçe", flag: "🇹🇷" }, { code: "pt", label: "Português", flag: "🇧🇷" }, { code: "es", label: "Español", flag: "🇪🇸" }];
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
  $("se-priv-t").textContent = "🛡 " + x.priv; $("se-ver").textContent = x.ver; $("se-priv").href = PRIV;
  $("se-use").onclick = async () => {
    const code = $("se-code").value; if (!code.trim()) return;
    $("se-msg").textContent = "…";
    const r = await net.redeemCoupon(code).catch(() => ({ status: "network" }));
    if (r.status === "ok") { P.gold += r.gold; P.xp += r.xp; c.saveP(); c.refreshHome(); $("se-msg").textContent = x.won(r.gold, r.xp, r.chests); $("se-code").value = ""; }
    else $("se-msg").textContent = x.err[r.status] || x.err.network;
  };
  $("se-how").onclick = () => c.info(x.howBody);
  $("se-lang").onclick = () => openLangMenu(c);
  $("se-tut").onclick = () => c.startTutorial();
  $("se-rate").onclick = () => window.open(RATE, "_blank", "noopener");
  $("se-bug").onclick = () => c.open("report"); $("se-acc").onclick = () => c.open("account");
}
