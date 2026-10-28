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

## Sırada (web-parity-2 dalında: mağaza, Dünyalar kilidi, rütbe penceresi, Hızlı Eşleş arama/hazır ekranları, performans yapıldı)
1. Lig: ödül toplama penceresi (geçen ayın birincileri) kaldı
2. Mağaza: Kalemler / Çerçeveler sekmeleri, satın alma pencereleri, kalem/çerçeve seçici (Android'deki AppWindowDialog görünümü)
3. Seviye haritası (kaydırma, numaralı kilitli düğümler, alt panel), Dünyalar
4. Arkadaşlar: davetler (Düellolar yapıldı: duel.js)
5. Hesap: bulut yedeği, çıkış, hesap silme yapıldı (sync.js) — gerçek Google girişiyle denenmedi
6. Sonuç ekranları (Hızlı Eşleş, günlük, bölüm); çevrimiçi sıralama karşılaştırması (bekleme odası yapıldı)
7. Ayarlar (sesler, bildirimler, kupon, dil, eğitim, sorun bildir) ve Eğitim ekranları
8. Ana ekran: Günün Meydan Okuması kartı, ücretsiz altın/kasa (web'de reklam yok → "Uygulamada")
9. Boyut/hız: görseller 720px'e küçültülmüş durumda; ilk yüklemede lazy

## Bilerek farklı
- Reklam düğmeleri (web'de reklam yok): ücretsiz altın/kasa, kasa hızlandırma, seri kurtarma reklamı
- Çizimde "Bitir" düğmesi web'de var, Android'de yok

## Güncel durum (web-parity-8 sonrası)
Yukarıdaki "Sırada" listesindeki her şey yapıldı (Lig, mağaza, harita, düellolar, hesap, sonuçlar, ayarlar, eğitim, günlük kart). Eklenenler: kullanıcı adı+şifre hesabı, günlük seri kurtarma teklifi (web'de "Uygulamada kurtar"), pencerelerin görünen ekranın ortasına konması, WebKit'e özel düzeltmeler.

## Test yöntemi (web)
- `web-tests` içinde Playwright WebKit (Safari motoru) + Chromium; iPhone SE / 13 / 15 Pro Max, TR + EN.
- 54 durumluk tarama: her ekran + pencere için ekran görüntüsü; yazı kesilmesi, üst üste binme, ekran dışına taşma, görünen ekrandan uzanan kaydırma alanı ve pencere kutusu taşması otomatik denetlenir.
- Firebase tarama sırasında sahte (bellekte) sürümle değiştirilir: gerçek anonim kayıt sınırı (IP başına) dolmasın diye.
- Bilinen kasıtlı fark: web'de reklam yok, Google girişi yok, çizimde "Bitir" düğmesi var.
- Açık: İngilizce'de boyalı Türkçe yazılar (Düellolar başlığı, Dünyalar tabelası ve dünya afişleri).

## Ekim 2026 eşitleme turu
- Android'den taşındı: seri kurtarma teklifi aynı kopma için 3 kez kapatılınca bir daha gösterilmiyor; açılış sahnesi kalkmadan hiçbir pencere açılmıyor (`afterSplash`); kalem seçicide mağazadaki gibi kıvrımlı kalem çizgileri (`penSwooshSvg`, boyalı kalem penceresi); sayfa geçişleri tek tip; oda kur / çevrimdışı kurulumda 3 sütun ve sığmazsa küçülen düzen; "Hazır" düğmesinin iki satırlı hali; rütbe seviyesi rozetleri (`rankLevelHtml`); davet ve günlük paylaşım metinleri Android'deki metinlerle aynı.
- Metinler: `scripts/gen_web_strings.js` artık `result_strings.json`, `chat_strings.json`, `store_strings.json` dosyalarını da Android'den üretiyor; web'deki elle yazılmış metin tabloları `useAppTexts(...)` ile Android anahtarlarına bağlandı. CI'daki "Web texts match the app" adımı üretilen dosyalar elle değişirse kırmızı yanar.
- Tekil/çoğul: Android'de `_one` anahtarları (`lock_duration_*_one`, `chest_duration_hours_one`, `rune_tile_cnt_one`) eklendi; web aynı anahtarları kullanıyor.

## Web sitesi (karalak.com)
- Arama ve paylaşım: açıklama, `canonical`, Open Graph + Twitter kartı (`oyna/img/og_share.jpg`, 1200x630), `VideoGame` yapılandırılmış verisi, İngilizce ziyaretçiye İngilizce sekme başlığı; `/indir` aynı paylaşım görselini kullanıyor.
- `robots.txt` (yalnız `/studio/` kapalı; davet linkleri önizleme için açık) ve `sitemap.xml` (`/`, `/indir`).
- `404.html`: TR/EN, oyuna dön / uygulamayı indir.
- `favicon.ico` (16/32/48), manifest'te `categories` ve yükleme penceresi için ekran görüntüleri (`img/shot_*.webp`).
- Güvenlik başlıkları (firebase.json): `Permissions-Policy`, sayfalar için `X-Frame-Options: SAMEORIGIN` + `frame-ancestors 'self'` (Firebase'in `/__/` giriş sayfalarına dokunulmuyor). HSTS'i Firebase zaten gönderiyor.
- Servis çalışanı: oyun `/` adresinde açıldığı hâlde çalışan `/oyna/` kapsamındaydı, yani hiçbir sayfayı denetlemiyor, görseller hiç önbelleğe alınmıyordu. Artık `Service-Worker-Allowed: /` başlığıyla tüm siteyi kapsıyor; video parça (Range) istekleri ağa bırakılıyor (Safari).
- `color-scheme: only light` (tarayıcıların zorunlu karanlık modu boyalı ekranları bozmasın) ve `format-detection: telephone=no` (oda kodları telefon linkine dönmesin).

## Diller: Türkçe, İngilizce, Portekizce (Brezilya), İspanyolca (Latin Amerika)
- Android: `values-pt`, `values-es` (anahtarlar ve sıra `values` ile aynı), `assets/words_pt.json`, `words_es.json` (ID'ler ve zorluklar Türkçe havuzla birebir; `WordPoolParityTest` bunu ve tekrar eden kelime olmamasını denetler). `SupportedLanguage` dört dili listeler; `WordSeeder.assetFileFor` dili havuz dosyasına bağlar; bot isimleri dile göre (`BotGhostRun`).
- Çevrimiçi oda dil karışık olabilir: kurucu kendi havuzundan kelime ID'si seçer, her oyuncu aynı ID'yi kendi dilinde görür. Türkiye'ye özgü 25 kelime İngilizce, Portekizce ve İspanyolca havuzda `approved: false` (dağıtılmaz ama ID ile bulunur).
- Web: `strings.json`, `runes.json`, `result/chat/store_strings.json` dört dili taşır (`gen_web_strings.js`, `gen_web_runes.js`); başarımlar, kalemler, çerçeveler, dünyalar, lobi alıntıları ve bot isimleri `gen_web_langs.js` ile Android çevirilerinden doldurulur; `hosting/oyna/words_*.json` Android havuzlarının kopyasıdır. CI hepsini denetler.
- Web modüllerindeki kendi metin tablolarına (`T`) `pt` ve `es` eklendi; dil yoksa İngilizceye düşer. Sayılar dile göre gruplanır (`fmtNum`: 6.245 / 6,245).
- Görseller henüz çevrilmedi: boyalı yazılı görseller (Düellolar, Dünyalar, giriş serisi çerçevesi, eşleşme başlığı, indirme sayfası) Portekizce ve İspanyolcada İngilizce/Türkçe sürümüyle görünür.
- Emülatör kontrolü: `smoke-test.yml` → `langs.sh` ana ekranları iki dilde çeker.
