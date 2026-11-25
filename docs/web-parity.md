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

## Kasım 2026 eşitleme turu
- Süre durmaz (#316): çizim, tahmin ve "Eşleşme bulundu" sayaçları artık saat sayacı değil son-an saatiyle (`runDeadline`, `Countdown.kt`'nin karşılığı) çalışıyor; sekme arka plana alınsa ya da telefon kilitlense de süre akıyor. 2 sn'den fazla geç dönülürse kelime zaman aşımı sayılır ve geçen süre sonraki aşamadan düşülür (`carryMs`). +10 sn jokeri son-anı uzatır; atlanan kelime kalan süresiyle döner.
- Eğitim (#313): 2. ve 3. çizim 9 ve 7 sn.
- Bilerek farklı: Google hesabı/doğum tarihi, Yenilikler turu, bildirim izni, reklam ve mağaza düğmeleri, geliştirici paneli ve video stüdyosu web'de yok; boyalı pencere çerçeveleri, basma animasyonları ve yeni doğru-cevap rozeti salt görsel olduğundan web'in mevcut tasarımıyla kaldı.

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

## Kasım 2026, ikinci eşitleme turu ("birebir" kararı)
Karar: yukarıda "salt görsel olduğundan web'in mevcut tasarımıyla kaldı" denen boyalı pencere çerçeveleri artık Android'deki gibi yapılıyor.
- **Kasalar nasıl çalışır?** Yazıyla kurulan pencere kaldırıldı; Android'deki gibi dil başına bir resim (`img/chests_info_<tr|en|es|pt>.webp`, Android'in `chests_info` dosyalarının kopyası), "Anladım" düğmesi resmin içindeki boyalı düğme (koordinatlar `res/values*/chest_info_art.xml`'den, `INFO_ART`). Resimdeki sayılar (altın, joker, süre) resmin içinde olduğundan kasa değerleri değişince resim yeniden çizilmeli.
- **`paintedFrameDialog` (ui.js)** = `PaintedFrameDialog.kt`: asma yaprak + parşömen çerçeve (`dialog_frame_btn`, 966x1595), başlık ve sığana kadar küçülen gövde, boyalı düğme. Yalnız Rünler'de kullanılıyor.
- **Rünler**: ilk ziyarette tanıtım penceresi (`rune_intro_*`, bir kez okununca `P.runesIntroSeen`), oyuncunun ilk rünü alınca açıklama penceresi (`rune_first_*`), satın alma gösterisi (`purchaseShow` = `RuneBurstEffects`: halkalar, kıvılcımlar, yükselen "+%8", karo zıplaması, seviye noktası vurgusu), son seviyede (`GrandFinish`) ışın tacı, beyaz parlama, altın dalga ve "Tamamlandı" yazısı, büyük konfeti (150 parça; ara seviyede 36).
- Testler: `ios.test.mjs` tohumuna `runesIntroSeen: true` eklendi; iki yeni durum (`chest_info`, `rune_intro`) ekran görüntüsü alıp pencerenin açılıp kapandığını denetliyor.
- **Henüz yok (sırada):** Rünlerde rehberli ilk rün (el işareti + not, ana ekrandaki maskot teklifi), diğer ekranların sistemli karşılaştırması (mağaza, hesap, arkadaşlar, lig, ayarlar, başarımlar, düello). Android'deki haptik geri bildirim web'de yok (tarayıcı desteklemiyor).

## Kasım 2026 tam analiz (otomatik karşılaştırma)
İki betik her an yeniden çalıştırılabilir (`cizim-hafiza/scripts/`): `web_parity_texts.py` (Android'in Türkçe metinlerinden web kodunda ne anahtarıyla ne de aynen geçenler; ekran adı verilirse yalnız o ekran), `web_parity_images.py` (Android'in kullandığı ama webde dosyası olmayan resimler). Çıkan satırlar **aday**tır; aşağıdaki sınıflandırma elle yapıldı.

Bu turda bulunup düzeltilen: efsane halkası (Rünler) için `rune_frame_legend` / `rune_key_legend` resimleri webde yoktu, o halkanın rünleri altın renkli çıkıyordu.

### A. Bilerek webde yok (karar: web'de gerekmiyor ya da yapılamıyor)
Reklam (`home_ad_*`, `xp_offer_*`, `streak_rescue_*`, `fstreak_ad_failed`, `chest_double_*`, `chest_speedup_*`, `result_watch_ad`, `store_pen_try/trial`, `chest_lost_watch`, `btn_ad_extra_time`) · Google girişi ve hesap bağlama (`account_sign_in_*`, `sign_in_prompt_*`, `username_finalize_*`, `avatar_source_google*`, `ic_google_g`) · Yenilikler turu (`tour_*`, `tour_card`) · bildirim izni (`chest_notif_*`) · Play Store puanlama istemi · zorunlu güncelleme (`forced_update_*`, `update_card`) · erişilebilirlik açıklamaları (`a11y_*`, `cd_*`) · video olarak paylaşma (`share_video_*`).

### B. Webde var, ama metni elle yazılmış (Android'le ayrışabilir; anahtarlara bağlanmalı)
Ayarlar (kupon hata ve ödül metinleri, "Kupon kodunu nasıl kazanırım?" penceresi, sürüm satırı, `coupon_clear`) · Hesap (kullanıcı adı hataları ve uyarıları, çıkış/silme pencereleri) · Arkadaşlar ve Çevrimiçi hata metinleri (`error_*`, `info_invite_sent`) · düello reddetme onayının başlığı · Başarımlar (`achievement_condition_label`, `achievement_gold_reward`) · Mağaza kalem sloganları (`pen_*_tagline`) ve uyarıları · Günün Meydan Okuması kartı metinleri · Kasalar kart metinleri (`chest_slot_*`, `chests_hint`).

### C. Webde olmayan, oyuncuya görünen parçalar
1. **Arkadaş serisi** (`fstreak_*`): seri satırı, "serin bitmek üzere" hatırlatması, "serin koptu / kurtarıldı" pencereleri, 3'lü ödül penceresi (Android'de var, web'de hiç yok).
2. **"Nasıl oynayalım?" seçim penceresi** (`friends_pick_*`, `play_choice_window`): arkadaşa dokununca Düello / Odaya davet seçimi.
3. **Ceza, kilit ve yasak ekranları**: tur geçersiz sayıldı (`penalty_*`), süreli kilit (`lock_screen_*`, `bg_locked`), kalıcı kapatılan hesap (`quick_match_banned_*`), "Bu mod geçici olarak kapalı".
4. **Hızlı Eşleş durumları**: havuz boş, rakip getirilemedi, iptal ediliyor, "Tekrar Ara" (`quick_match_empty_*`, `_failed_*`, `_cancelling_*`, `_search_again`), eşleşme resimleri (`bg_qm_found`, `qm_pad`).
5. **Lig**: ay sonu ödül penceresi (`league_win_*`, `league_win_frame`), boş tablo metinleri, ödül açıklamaları (`league_reward_explainer_*`).
6. **Bil Bakalım (uygulama içi tahmin ekranı)**: `guess_*` metinlerinin çoğu, `guess_answer_bg`, `guess_notice_bg` (web'de yalnız `/b/KOD` sayfası var).
7. **Kasalar**: ayrı "Kasalarım" sayfası (`chests_title`, `chests_hint`), yuva durumları ("Sırada", "Açmak için dokun!").
8. **Çizim**: silgi ve geri al araçlarının adları, "Çizimi Bitir" Türkçe düğme resmi (`btn_finish_drawing_tr`), "Sonraki Kelime" resmi (`btn_next_word_tr`), paylaşma seçim penceresi (`share_drawing_choose`).
9. **Sorun Bildir**: silme onay pencereleri (`report_bug_delete*`).
10. **Boyalı pencereler** (Android'de resimle, webde düz): `duel_result_window`, `play_choice_window`, `league_win_frame`, `invite_window`, `ls_coupon_frame` (kupon ödülü), `penalty_card`, `streak_rescue_card`, `child_locked_window`, `offline_panel`, `window_frame`, `xp_offer_window*`.
11. **Mağaza**: 18 kalem resmi (`st_pen_*`) webde farklı yolla çiziliyor, birebirliği ayrıca doğrulanacak; `st_tab_*` sekme resimleri.
12. **Ana ekran**: `frame_chip_icon`, `pen_chip_icon`, `daily_streak_icon`, `home_tile_fill`, `karalak_logo_lettering` (web'de başka resimlerle çiziliyor olabilir, doğrulanacak).

### Önerilen sıra
1. B (metinleri Android anahtarlarına bağla; kayma biter) · 2. C-2, C-3, C-4, C-5 (oyuncunun sık gördüğü ekranlar) · 3. C-10 (boyalı pencereler) · 4. C-1 (arkadaş serisi: büyük) · 5. C-6, C-7, C-8, C-9, C-11, C-12.

