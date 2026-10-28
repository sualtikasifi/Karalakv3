# Gizlilik Politikası — Karalak

Yayınlanan sürüm: **https://karalak.com/gizlilik** (Türkçe, İngilizce, Portekizce, İspanyolca; `?lang=tr|en|pt|es`).
Kaynağı: `hosting/gizlilik/index.html`. Play Console'daki gizlilik politikası adresi budur.

Aynı yerde:

- Kullanım koşulları: https://karalak.com/kosullar (`hosting/kosullar/index.html`)
- Hesap ve veri silme: https://karalak.com/hesap-silme (`hosting/hesap-silme/index.html`) — Play Console'daki
  "Hesap silme URL'si" budur.

Metni değiştirirken:

- Play, politikanın uygulama adını VE geliştirici adını açıkça içermesini şart koşuyor (bir sürüm bu yüzden reddedildi).
- Reklam sıklığı `ads/AdManager.kt` ve `GameViewModel.showResultInterstitial` ile birebir aynı olmalı.
- Toplanan her yeni veri (yeni bir koleksiyon, yeni bir SDK) hem bu sayfaya hem Play Console'daki Veri Güvenliği
  formuna eklenmeli; hesap silme akışı (`AccountDeletionRepositoryImpl`, `functions/src/maintenance.ts`) da onu silmeli.
- Eski adres (https://sualtikasifi.github.io/app-ads/, depo kökündeki `docs/index.html`) yeni sayfaya yönlendirir.
