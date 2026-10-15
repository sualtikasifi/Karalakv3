# Web (hosting/oyna) ↔ Android uyum çalışması

Amaç: iPhone kullanıcıları tarayıcıdan oynasın; web, Android uygulamasıyla görsel ve mantık olarak aynı olsun.

## Yöntem
- Aynı ekranın emülatörden (adb screencap, 1080x2400) ve masaüstü Chrome'dan (headless, 360x800 @2x) yan yana görüntüsü.
- Betikler (scratchpad): `webshot.js` (web ekran görüntüsü), `pair.ps1` (yan yana), `shotlib.js` (oyuncu durumu enjekte eder).
  Web ekranı açmak için `window.__karalak` (oyun bağlamı) üzerinden diyalog/sahneler doğrudan açılabilir.
- Veri kaynakları **Android'den üretilir**, elle kopyalanmaz:
  - `node scripts/gen_web_runes.js` → `hosting/oyna/runes.json` (rün haritası, halka kapıları, tavanlar, ikon ortalama tablosu)
  - `node scripts/gen_web_strings.js` → `hosting/oyna/strings.json` (Android metinleri; web `i18n.js` içinden `S("anahtar", ...)`)
  - Android'de rün/metin değişince bu iki betik çalıştırılıp çıktı commit'lenir.
- Mantık testi: `node --test hosting/oyna/economy.test.mjs` (RoundRewardsTest ile aynı durumlar).

## Yapıldı
- XP/altın kuralları (economy.js): kelime XP'si, bölüm/günlük XP, rün XP'si, altın kalemleri, rün katkısı, XP etkinliği (config/xpEvent), günün ilk Hızlı Eşleş'i 2x, çıkış cezası, config/economy
- Çevrimiçi oda: oda/kurucu/Dostluk altını, maç XP'si, kazanana kasa
- Çevrimdışı Oyna kurulum ekranı; Oda Kur aynı tasarım; çizimde süre hapı; tahminde "★ +N XP" ve canlı hız bonusu
- Rün haritası (runes.js): kaydırma/yakınlaştırma, sis, mühürler, bilgi kartı, durum penceresi (köpeğe dokun), config/runes, rün hazır rozeti, tüm rün etkileri (Anahtarcı, Yedek Yuva, Seri Sigortası, Şanslı Zar dahil)
- Kasalar (chests.js + chestfx.js): yuva kartları, kasa penceresi (canlı arka plan, konuşan kasa, süre plakası), açılış videosu + ödül sahnesi, kazanma sahnesi, bilgi penceresi
- Ortak: `ui.js` (AppWindowDialog, PrimaryButton), `i18n.js`
- Başarımlar: konum, yeşil "alındı" rengi, mavi/turuncu ödül hapları

## Sırada
1. Lig: ödül toplama penceresi (geçen ayın birincileri) kaldı
2. Mağaza: Kalemler / Çerçeveler sekmeleri, satın alma pencereleri, kalem/çerçeve seçici (Android'deki AppWindowDialog görünümü)
3. Seviye haritası (kaydırma, numaralı kilitli düğümler, alt panel), Dünyalar
4. Arkadaşlar: davetler (Düellolar yapıldı: duel.js)
5. Hesap: bulut yedeği, çıkış, hesap silme yapıldı (sync.js) — gerçek Google girişiyle denenmedi
6. Sonuç ekranları (Hızlı Eşleş, günlük, bölüm), çevrimiçi bekleme odası/sıralama karşılaştırması
7. Ayarlar (sesler, bildirimler, kupon, dil, eğitim, sorun bildir) ve Eğitim ekranları
8. Ana ekran: Günün Meydan Okuması kartı, ücretsiz altın/kasa (web'de reklam yok → "Uygulamada")
9. Boyut/hız: görseller 720px'e küçültülmüş durumda; ilk yüklemede lazy

## Bilerek farklı
- Reklam düğmeleri (web'de reklam yok): ücretsiz altın/kasa, kasa hızlandırma, seri kurtarma reklamı
- Çizimde "Bitir" düğmesi web'de var, Android'de yok
