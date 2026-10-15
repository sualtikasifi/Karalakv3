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
};
const PRIV = "https://sualtikasifi.github.io/app-ads/";
const RATE = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza";

export function layoutSettings(c) {
  const { place, $ } = c;
  place($("se-back"), 28, 95, 114, 100); place($("se-title"), 196, 186, 304, 90); place($("se-sound"), 112, 358, 262, 48);
  [467, 562, 656, 751].forEach((cy, i) => place($("se-r" + i), 88, cy - 42, 664, 84));
  place($("se-code"), 340, 905, 300, 76); place($("se-use"), 652, 905, 130, 76); place($("se-msg"), 340, 995, 440, 90); place($("se-how"), 340, 1100, 440, 44);
  [["se-lang", 1237], ["se-tut", 1347], ["se-bug", 1457], ["se-acc", 1567], ["se-rate", 1677]].forEach(([id, cy]) => place($(id), 52, cy - 47, 738, 94));
  place($("se-priv"), 190, 1752, 460, 56);
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
  $("se-code").placeholder = x.code; $("se-use-t").textContent = x.use; $("se-how").textContent = x.how;
  const flag = lang() === "tr" ? "🇹🇷 Türkçe" : "🇬🇧 English";
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
  $("se-lang").onclick = () => { c.setLang(lang() === "tr" ? "en" : "tr"); };
  $("se-tut").onclick = () => c.info(x.tutMsg);
  $("se-rate").onclick = () => window.open(RATE, "_blank", "noopener");
  $("se-bug").onclick = () => c.info(x.soon); $("se-acc").onclick = () => c.open("account");
}
