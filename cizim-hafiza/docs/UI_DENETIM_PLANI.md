# Karalak — Arayüz ve Kod Denetimi + Uygulama Planı

**Tarih:** 5 Ekim 2026
**Sürüm:** 1.26.0 (73), commit `c9eced1`
**Test ortamı:** Emülatör `Karalak_Pixel6`, 1080x2400 @420dpi. Ek testler: 1080x1920 (16:9), 720x1600 @320dpi ve yazı boyutu 1.3.

Bu belge, kodlamaya geçecek kişinin (Sonnet dahil) başka bir şeye bakmadan uygulayabilmesi için yazıldı.
Her madde şu bilgileri içerir: **kimlik**, **önem**, **nerede**, **ne görülüyor**, **kök neden** ve **nasıl düzeltilecek**.

Önem dereceleri:
- **P0:** Bozuk görünüm ya da hata, mutlaka düzeltilmeli.
- **P1:** Belirgin tutarsızlık.
- **P2:** İnce ayar ya da cila.

---

## 0. Özet

- Yeni boyalı (painted) tasarım ekranların çoğuna gelmiş. Hataların büyük kısmı **4 ortak kök nedenden** çıkıyor.
  Ekranları tek tek yamamak yerine önce bu 4 altyapı düzeltilmeli (Bölüm 1).
  1. **Metin sığdırma yok.** Boyalı ekranlarda yazılar sabit kutuya sabit punto ile konuyor. Taşan yazı kesiliyor ya da hiç görünmüyor.
     Örnekler: "Günün Mevdan", "Kara", "Tamaml", "22:56:3", "Ödülü Al ·", "Günlük meydan", "Arkadaşınla Yarış" (2. satır kesik).
  2. **Ana sayfa en-boy oranına uymuyor.** `ContentScale.FillBounds` kullanılıyor ve x/y ayrı ölçekleniyor. 16:9 ekranda resim eziliyor, kasa plakaları kayboluyor.
  3. **Durum çubuğu okunmuyor.** Simgeler her ekranda koyu renk. Boyalı sahnelerin üstü koyu ve karışık olduğu için saat ve pil görünmüyor.
  4. **Tasarım dili yarım taşınmış.**
     - Geri düğmesinin 3 farklı stili var.
     - Pencereler (dialog) 3 farklı stilde: Material'in varsayılan mor-gri AlertDialog'u, eski krem pencere ve yeni ahşap.
     - 7'den fazla ekran hâlâ eski karalama-kağıt tasarımında.
     - 2 farklı maskot kullanılıyor: eski turuncu dinozor ve yeni taçlı köpek.
- Ayrıca **2 gerçek hata** var:
  - Sonuç ekranında rakibin çizim küçük resimleri simsiyah leke olarak görünüyor (B-01).
  - Rakip avatarı eşleşme ekranında ve sonuç ekranında farklı (B-02).

---

## 1. Ortak kök nedenler — önce bunlar

### S-01 (P0) Metin sığdırma: `LetteredText` ve `PaintedStyle` yazıları taşınca küçülmeli

**Nerede:** `presentation/common/WoodUi.kt:111` (`LetteredText`). Ayrıca `PaintedStyle` kullanan bütün `Text` çağrıları (`HomePainted.kt`, `HomeChests.kt:415-461`, `ResultScreen.kt:305,343` vb.).

**Kök neden:**
- Yazı sabit `sp` boyutunda, kutu ise sabit boyda. `maxLines` aşılınca Compose yazıyı keser.
- Satır yüksekliği `fontSize*1.2`. İki satırlık kutular bunu taşıyamıyor.
- Kullanıcının yazı boyutu ayarı (font_scale) bu `sp` değerlerini büyütüyor. 1.3'te "1x" çarpanı tamamen kayboluyor.

**Çözüm:**
1. `common/WoodUi.kt` içine yeni bir `AutoFitLetteredText` yaz.
   - Parametreler: `text, maxSize, minSize = maxSize*0.6f, maxLines, fill, outline, align`.
   - İçeride `BoxWithConstraints` ve `rememberTextMeasurer()` kullan. `maxSize`'dan başlayıp 0.5sp adımlarla ikili arama yaparak en büyük sığan boyutu bul.
   - Ölçerken `constraints = Constraints(maxWidth = maxWidthPx)` ve `maxLines` ver. `didOverflowHeight`, `didOverflowWidth` ve `lineCount <= maxLines` kontrol edilsin.
     Yükseklik için `size.height <= maxHeightPx` olsun.
   - Bulunan boyut hem dış çizgi (stroke) hem dolgu (fill) metnine aynen uygulansın. Bugün iki ayrı `Text` var, ikisi aynı boyutu kullanmalı.
   - `minSize`'a inildiği hâlde sığmıyorsa son çare `TextOverflow.Ellipsis` olsun.
2. `LetteredText`'in mevcut imzasını koru ve içini `AutoFitLetteredText`'e yönlendir. Böylece bütün çağrılar tek seferde düzelir.
3. `PaintedStyle` ile çizilen düz `Text`'ler için aynı mantıkla bir `AutoFitText(text, style, maxLines, minScale)` yaz.
   Boyalı ekranlarda `maxLines` verilmiş her `Text`'i buna çevir. Aramak için: `grep -rn "PaintedStyle(" presentation`.
4. Boyalı sahne yazıları ölçülerini zaten ekrana göre hesaplıyor. Bunları kullanıcı yazı ölçeğine bağlamamak için `fs()` sonucunu `fontScale`'e böl:
   `(art * us / LocalDensity.current.fontScale).sp`. Normal metinler (açıklamalar, listeler) kullanıcı ayarına uymaya devam etsin.

**Kabul ölçütü:** Şu 3 ekran boyutunda ve şu 2 yazı ölçeğinde **hiçbir** yazı kesik değil:
- Ekranlar: 1080x2400@420, 1080x1920@420, 720x1600@320
- Yazı ölçeği: font_scale 1.0 ve 1.3

### S-02 (P0) Ana sayfa sahnesinin en-boy oranı (`HomePainted.kt`)

**Nerede:** `mainmenu/HomePainted.kt:99-113`. Burada `ux = maxWidth/841` ve `uy = maxHeight/1870` ayrı hesaplanıyor ve `ContentScale.FillBounds` kullanılıyor.

**Görülen:**
- 1080x1920 ekranda logo, köpek ve kutucuklar dikeyde eziliyor.
- Kasa plakaları ("Aç!", "06:00") tamamen görünmüyor.
- 720x1600 ekranda kasa plakalarının alt kısmı kesik.

**Kök neden:** Resim ve yazılar x ve y'de farklı oranda ölçekleniyor. Sabit `dp` boyutlu alt bileşenler sığmıyor. Örneğin `HomeChestSlot` plakası 24dp yüksek ve resimdeki kutuya sığmıyor.

**Çözüm:**
1. Tek bir dönüşüm kullan: `s = maxOf(w/ArtW, h/ArtH)` (Crop).
   - Yatay hizalama ortalı olsun. Dikeyde hizalama `uzun ekran (h/w ≥ 2.2) → Center`, `kısa ekran → Bottom`.
     Böylece kısa ekranda üstteki logo kırpılır, kasalar ve kutucuklar korunur.
   - `box()` şuna dönüşsün: `Modifier.offset(offX + x0*s, offY + y0*s).size((x1-x0)*s, (y1-y0)*s)`. `offX` ve `offY` resmin kırpma kaydırmasıdır.
   - `WoodUi.kt:165`'teki `WoodScreen` zaten bu yöntemi kullanıyor. Aynı hesabı `PaintedScene(artW, artH, res, alignment)` adında ortak bir yardımcıya taşı ve iki yerde de onu kullan.
2. Güvenli alan: Kısa ekranda kırpılan bölgede tıklanabilir bir şey kalmamalı.
   - Üstteki altın ve kalem çipleri resmin y≈258'inde. 16:9'da `Bottom` hizalamayla kırpılan pay yaklaşık 150 art birimi. Bu yüzden çipler kalır, logonun üst yarısı gider. Bu kabul edilebilir.
   - Kırpma payı 258'i geçerse çipleri resimden bağımsız, ekran üstüne `statusBarsPadding` ile yerleştir.
3. `HomeChestSlot(compact = true)` içindeki sabit `dp` değerleri boyalı ölçeğe bağlansın. Plaka yüksekliği, resim genişliği ve yazı boyu `s` ile çarpılarak hesaplansın ya da S-01'deki otomatik sığdırma kullanılsın.

**Kabul ölçütü:** 3 ekran boyutunda resim oranı bozulmuyor. Kasa plakaları ve bütün kutucuk yazıları tam görünüyor.

### S-03 (P0) Durum çubuğu okunabilirliği

**Nerede:** `navigation/NavGraph.kt:96-103`. Yalnızca `Screen.Store` rotasında açık renk simge kullanılıyor, diğer her yerde koyu simge.

**Görülen:** Ana sayfa, Arkadaşınla Yarış, Lobi, Koda Katıl, Oda Kur, Çevrimdışı, Dünyalar, Arkadaşlar, Başarımlar, Lig, Tahmin ve Çizim ekranlarında saat, pil ve bildirim simgeleri arka plana karışıyor.

**Çözüm:**
1. Ortak bir `StatusBarScrim()` bileşeni yaz. Ekranın en üstünde, `WindowInsets.statusBars` yüksekliği + 12dp boyunda, `Black.copy(0.38f) → Transparent` dikey gradyan olsun.
   Bunu bütün boyalı sahne ekranlarının (`PaintedScene`, `WoodScreen`, `PaintedPage` ve özel Box'lar) en üst katmanına ekle.
2. `NavGraph`'taki `onDarkScreen` bir rota kümesine dönüşsün. Boyalı sahnesi olan bütün rotalar açık renk simge kullansın:
   MainMenu, OnlineLobby, CreateRoom, JoinRoom, WaitingRoom, Offline, Levels/WorldMap/LevelMap, Friends, Achievements, League, Store, Game, OnlineGame, QuickMatch.
   Krem ya da açık ekranlar (Ayarlar, Sorun Bildir, Sonuç, Hesap) koyu simge kullanmaya devam etsin.

### S-04 (P1) Tek tasarım dili

**a) Geri düğmesi: 3 farklı stil var**
- Turuncu ahşap ok (`join_back`): Çevrimdışı, Oda Kur, Koda Katıl, Lobi, Arkadaşlar, Başarımlar, Lig, Ayarlar, Sorun Bildir.
- Beyaz kare ve gri ok: Arkadaşınla Yarış (`OnlineLobbyScreen`), Dünyalar (`WorldMapScreen`), Aşama haritası (`LevelMapScreen`), Hesap, Düellolar.
- Koyu çerçeveli ok: Mağaza. Çizim ve Tahmin ekranlarında da ayrı krem kare stil var.

→ `common` içine `PaintedBackButton(onClick)` ekle (`join_back`, 56dp, `statusBarsPadding` + start 16dp, top 12dp). Hepsini buna çevir.
Çizim ve Tahmin ekranlarının üst çubuğu ayrı bir dil; onlar kalabilir, ama kendi içinde tutarlı olmalı.

**b) Pencereler (dialog)**
- `AlertDialog(` geçen yerler: AccountScreen (3), DuelListScreen, FriendsScreen (2), GameScreen (çıkış uyarısı), LeagueScreen, MainMenuScreen, OnlineGameScreen, QuickMatchScreen, ReportBugScreen (2), WaitingRoomScreen, DrawingReportsScreen.
  Ekranda görülen örnek: "Oyundan çık?" penceresi Material 3'ün varsayılan lavanta rengiyle çıkıyor ve markaya hiç uymuyor.
- `AppWindowDialog` (krem) ve özel pencereler: ChestInfoDialog, kasa ayrıntısı, ChestWonDialog, FeatureTour, Rating ve SignIn pencereleri. Bunlar Quicksand benzeri gövde yazısı kullanıyor, boyalı ekranlar ise Baloo 2.

→ `common/PaintedDialog.kt` adında tek bir pencere bileşeni yaz:
- Krem kağıt panel (`fr_cream_a` ya da `NinePatch` ile ahşap çerçeve).
- Başlık `AutoFitLetteredText` ile, gövde `DescriptionStyle` ile.
- İki düğme: birincil turuncu boyalı hap, ikincil beyaz hap.
- `PaintedConfirmDialog(title, message, confirm, dismiss, destructive)` kısayolu.

Bütün `AlertDialog` çağrılarını buna çevir. `AppWindowDialog`'un içini de bu stile taşı; imzası kalsın.

**c) Yazı tipleri:** Boyalı ekranlarda gövde yazısı her yerde `DescriptionStyle` (Baloo 2 SemiBold, `#5A321F`) olmalı.
Hesap, Düellolar, Eğitim ve kasa pencerelerindeki gövde yazısı farklı bir yazı tipiyle görünüyor.
→ `MaterialTheme.typography` gövde yazı tipini de Baloo 2 yap (`presentation/theme/Type.kt`), ya da bu ekranlarda `DescriptionStyle` kullan.

### S-05 (P1) Eski tasarımda kalan ekranlar

Kod taraması (`screenBackground()` kullananlar) ve ekran görüntüsüyle doğrulandı:

| Ekran | Dosya | Durum | Öneri |
|---|---|---|---|
| Hesap | `account/AccountScreen.kt` | Eski karalama arka plan, beyaz kart | `PaintedPage` + ahşap paneller (Ayarlar ile aynı dil) |
| Düellolar | `duel/DuelListScreen.kt`, `CreateDuelScreen.kt` | Eski | Arkadaşlar ekranının dili (`bg_friends`) |
| Eğitim | `tutorial/TutorialScreen.kt` | Eski arka plan ve **eski dinozor maskot** | Yeni köpek maskot, `PaintedDialog` balonları |
| Ara ekran (çizim → tahmin) | `game/BreakScreen.kt` | Eski | Çizim ekranının kağıt arka planı ve köpek |
| Çevrimiçi sonuç | `online/OnlineResultScreen.kt` | Eski (3 yerde `screenBackground`) | `ResultScreen`'in boyalı dili (ortak bileşenler) |
| Kasalarım (tam ekran) | `chests/ChestsScreen.kt` | Eski. `Screen.Chests` rotası var ama yeni ana sayfadan buraya ulaşan bir yol yok. | Ya kaldır ya da kasa başlığına bağla ve yeni dile taşı |
| Mağaza | `store/StoreScreen.kt` | Ayrı ve koyu bir sahne. Logosu ve sekmeleri farklı. | Bölüm 2.8'e bak |
| Yönetici panelleri (WordReview, DifficultyReview, BotNames, BotTraining, DrawingReports) | ilgili klasörler | Eski | P2. Oyuncu görmüyor, en sona bırak. |

### S-06 (P1) Maskot ve varsayılan avatar tutarlılığı

- Yeni marka maskotu taçlı köpek (logo, açılış, ana sayfa, sonuç, çizim).
- Varsayılan profil resmi hâlâ eski turuncu dinozor taslağı (`LevelAvatar` içindeki `dino()`). Eğitim penceresi de eski dinozor.

→ Karar gerekli (kullanıcıya sor):
- (a) Varsayılan avatar köpeğin yüz çizimi olsun.
- (b) Dinozor yalnızca bir avatar seçeneği olarak kalsın.

Eğitim penceresindeki maskot her durumda köpek olmalı.

---

## 2. Ekran ekran bulgular

### 2.1 Ana sayfa (`mainmenu/HomePainted.kt`, `HomeChests.kt`)

| Kimlik | Önem | Görülen | Neden / Çözüm |
|---|---|---|---|
| H-01 | P0 | "Günün Meydan Okuması" başlığı "Günün / Mevdan" görünüyor. 3. satır kesik, y harfinin kuyruğu da kesildiği için "v" gibi okunuyor. | Kutu `box(388,548,594,624)` ve `fs(33)` ile 2 satır sığmıyor. S-01 ile otomatik sığdırma uygula, `maxLines=2`, kutuyu yukarı doğru 532'ye kadar genişlet. |
| H-02 | P0 | Tamamlanmış günlük kartta geri sayım "22:56:3" ve rozet "✓ Tamaml" diye kesik. | `box(294..396)` ve `box(404..598)` dar. Geri sayıma 256→396 aralığını ver, rozet otomatik sığsın. Saniyeyi gösterme (HH:MM yeterli) ya da sabit genişlikli rakam kullan. |
| H-03 | P0 | Rütbe hapı "Karalamacı" yerine "Kara" (1.3 ölçekte "Kar"). | `box(222,400,380,447)` genişliği sabit. Hap `wrapContent` olsun ve sağdaki "Sv. 4 için 50 XP" yazısıyla çakışmadan 430'a kadar uzayabilsin. Yazı otomatik sığsın. |
| H-04 | P0 | "Arkadaşınla Yarış" ve "Çevrimdışı Oyna" kutucuk yazılarının 2. satırı kesik. | `Tile` etiket kutusu yüksekliği 72 art birimi ve `fs(35)`. S-01 ile 2 satıra sığdır ya da kutuyu 900→1004 aralığına genişlet (resimdeki etiket bandı). |
| H-05 | P0 | Kasa başlığı "Kasaları" (son harf kesik). | `box(168,1434,352,1494)` genişliği yetmiyor. 168→470 aralığını ver ve otomatik sığdır. |
| H-06 | P1 | "Her galibiyet bir hazine!" yazısının üstünde **hayalet ikinci bir yazı** ("Her galibiyet…" yarım görünüyor). | Muhtemelen `bg_home_scene` resminde bu yazı gömülü ve kod aynı yazıyı tekrar çiziyor. Ya da 2 satıra bölünen metnin 1. satırı kutunun üstüne taşıyor. **Doğrula:** önce resmi aç (`res/drawable-nodpi/bg_home_scene.webp`). Gömülüyse resimden temizlet ya da kod yazısını kaldır. Taşmaysa `maxLines=1` + otomatik sığdır. |
| H-07 | P1 | Reklam düğmelerinde "Reklam izle" yerine "Reklam" görünüyor (sol ve sağ kart). Arayüz ağacında metin tam. | Kesilme. Otomatik sığdırma. |
| H-08 | P1 | Seviye "• 3 Seviye" çok soluk (gri, ahşap üstünde). | `InkSoft2` (`#6B5446`) zayıf. Kontrastı en az 4.5:1 olan koyu kahve kullan (`#4A3426`) ya da hap içine al. |
| H-09 | P1 | "3 Seviye" ifadesi. Uygulamanın geri kalanı "Seviye 3" diyor (`result_level_label`, `account_level_format`). | `home_level_inline` → "Seviye %1$d". Lobi ve lig satırlarında da aynısı. |
| H-10 | P1 | 1.3 yazı ölçeğinde "🔥 1x" çarpan rozetinin içi boş kalıyor ("1x" yok). | S-01 (4. adım): sahne yazılarını yazı ölçeğinden bağımsız yap. |
| H-11 | P2 | Kasa yuvalarında `compact` plakalar sahne ölçeğine bağlı değil (S-02-3). | — |
| H-12 | P2 | Alttaki boya kalemleri ve defter resmi navigasyon çubuğunun altında kalıyor. Kasa paneli alta çok yakın. | S-02'den sonra yeniden kontrol et. |

### 2.2 Günün Meydan Okuması akışı

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| D-01 | P1 | Günlük biter bitmez **geçiş reklamı** açılıyor (önce reklam, sonra sonuç). | Ürün kararı. Öneri: günlük sonrası geçiş reklamı gösterme ya da sonuç ekranından ana sayfaya dönerken göster. `GameViewModel` / reklam tetikleyicisi. |
| D-02 | P1 | Sonuç ekranındaki günlük kartı "❌❌❌❌❌" emojileriyle gösteriliyor. Ana sayfada aynı bilgi yeşil-tikli ve kırmızı-çarpılı dairelerle gösteriliyor. | Ana sayfadaki daire bileşenini ortak bir `DailyPips(flags, size)` bileşenine çıkar ve sonuç ekranında da kullan (`ResultScreen.kt:768` civarı). |
| D-03 | P2 | Çizim üst çubuğunda 2 sayaç var: hap "0:30 · 1/5" maçın toplam kalan süresi, sağdaki halka kelime süresi. Hangisinin ne olduğu belirsiz. | Hapın içine küçük bir "toplam" ipucu ekle ya da hapta yalnızca "1/5" göster. Halka kalsın. |

### 2.3 Çizim ekranı (`game/DrawingScreen.kt`)

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| G-01 | P1 | "+ Süre 0" jokeri çizim alanının **içinde**, sağ üstte duruyor. O köşeye çizilemiyor ve çizimin üstünü kapatıyor. | Jokeri araç çubuğuna, "+10sn" düğmesinin yanına taşı. Ya da kelime kartıyla tuval arasındaki bandın sağına koy. |
| G-02 | P2 | Alt kısımda boya kalemi resmi için yaklaşık 180dp boş yer harcanıyor. | Tuval 12-15% daha uzun olabilir. Dekoru küçült. |
| G-03 | P2 | Köpek sticker'ı kelime kartının sağ kenarını örtüyor. Uzun kelimede çakışma riski var. | Kelime kartı yazısı için `end` boşluğu ve otomatik sığdırma. |

### 2.4 Tahmin ekranı (`game/GuessScreen.kt`)

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| T-01 | P1 | "+3 XP" bonus çipi turuncu üstünde yeşil çerçeveli ve neredeyse okunmuyor. | Dolu yeşil hap ve beyaz yazı, ya da koyu metin. |
| T-02 | P2 | Tuvalin sol ve sağ kenarında yarım görünen beyaz "◀ ▶" şekilleri var (resim parçası mı, kaydırma ipucu mu belirsiz). | Kaynağını bul. İşlevsizse kaldır. |
| T-03 | P2 | Pasif "Gönder" düğmesi çok soluk ve boyalı stille uyumsuz. | Pasif durum için gri boyalı hap. |

### 2.5 Sonuç ekranı (`game/ResultScreen.kt`)

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| B-01 | **P0 hata** | Rakibin çizim küçük resimleri **siyah leke** gibi görünüyor. Büyütünce çizim doğru çıkıyor. | `common/StrokeCanvas.kt:74` içinde `strokeWidthPx` sabit 9 piksel, ama çizim küçük hücreye sığdırılırken ölçekleniyor (`fit.scale`). Sık çizgiler birleşip lekeye dönüşüyor. Çözüm: `drawFittedStroke` çağrısında genişlik `strokeWidthPx * fit.scale` olsun, `coerceIn(1.2f, strokeWidthPx)`. Çizimin ilk tuval genişliği biliniyorsa onunla oranla. |
| R-01 | P0 | Üst kartta "Günlük meydan okuma ödülün" yazısı "Günlük meydan" diye kesik. | Otomatik sığdır, 2 satıra izin ver. |
| R-02 | P0 | Alt düğme "Ödülü Al · 40 XP" yazısı "Ödülü Al ·" diye kesik (`ResultScreen.kt:343`, 19.sp sabit). | Otomatik sığdır. "x2" düğmesiyle yan yanayken ikisinin genişliğini de yazıya göre dengele. |
| R-03 | P1 | 0 XP kazanılınca düğme "Ödülü Al · 0 XP" diyor. | `shownXp == 0` ise "Devam" yaz ve hediye simgesini gösterme. |
| R-04 | P1 | İki ayrı paylaş düğmesi var: kart içinde "Sonucu paylaş" ve kullanıcı adının yanında paylaş simgesi. | Günlükte yalnızca "Sonucu paylaş" kalsın. Simge, normal oyunda çizimleri paylaşmak için kalsın. |
| R-05 | P2 | Alt düğmeler boya kalemi resminin üstüne biniyor. | Alt boşluk ya da dekorun kırpılması. |
| B-02 | **P1 hata** | Rakip avatarı tutarsız: Hızlı Eşleş giriş ekranında benim varsayılan avatarım (dinozor), sonuç ekranında gri insan silüeti. | Giriş ekranı `avatarPhotoOf(avatarUrl)` (`QuickMatchScreen.kt:677`), sonuç ekranı `AvatarPhoto.Persona(ghost.nickname)` (`ResultScreen.kt:594`) kullanıyor. Ortak bir `opponentPhoto(nickname, avatarUrl)` yardımcısı yaz: url boş değilse onu, boşsa `Persona` kullansın. İki yerde de bunu çağır. Ayrıca `PersonaFace` gri silüet seçeneğini (`LevelAvatar.kt:533`) kaldır, çünkü sahte ve boş görünüyor. |

### 2.6 Hızlı Eşleş (`quickmatch/QuickMatchScreen.kt`)

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| Q-01 | P1 | Arama ekranı ("Rakip aranıyor…") çok boş. | İpucu kartları zaten var, ama yalnızca eşleşme sonrası görünüyor; arama sırasında da göster. Bir iptal düğmesi ekle. |
| Q-02 | P2 | "Maç 4 saniye sonra başlıyor" çubuğunun sağındaki "•••" anlamsız. | Kaldır ya da ilerleme çubuğuna dönüştür. |
| Q-03 | P2 | Sağdaki kartın arkasında resimdeki "Küçük Çizimler Büyük Hikayeler" yazısı görünüyor (resim metni arayüzle çakışıyor). | Kart opaklığı ya da resimde o bölgeyi temizle. |
| Q-04 | P1 | "Oyundan çık?" penceresi Material'in varsayılan penceresi. | S-04b. |

### 2.7 Arkadaşınla Yarış / Oda Kur / Koda Katıl / Lobi

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| O-01 | P1 | Arkadaşınla Yarış ekranında beyaz kare geri düğmesi var, diğer çevrimiçi ekranlarda turuncu ahşap. | S-04a. |
| O-02 | P1 | Kategori çiplerinde tutarsızlık var. Çevrimdışı'nda emoji var (🌲 Doğa…), Oda Kur'da yok. Emojili "Hayvanlar" ve "Yiyecekler" 2 satıra bölünüyor (emoji üstte, yazı altta). | Tek bir `CategoryChip` bileşeni yaz. Ya her yerde emojili ve tek satır (otomatik sığdırmalı), ya da her yerde emojisiz. **Öneri:** emojili, `maxLines=1`, otomatik sığdırma. |
| O-03 | P2 | "(opsiyonel)" ifadesi. | "(isteğe bağlı)" yap: `select_category`, `select_difficulty`. |
| O-04 | P2 | Mod isimleri tutarsız: Çevrimdışı'nda "Normal / Süresiz", Oda Kur'da "Serbest / 2v2 Takım". | "Serbest" bir oda modu, "Normal" bir zaman modu; iki farklı kavram. Panelde başlıkları netleştir: "Zaman" ve "Oda türü". |
| O-05 | P2 | Çevrimdışı ve Oda Kur panelleriyle alttaki düğme arasında büyük boşluk var (16:9 dışında). | Paneli dikeyde ortala ya da düğmeyi panelin hemen altına al. |
| O-06 | P1 | Koda Katıl ekranında oda kodu alanı boş, ipucu yazısı yok. Etiket var ama alan "6 haneli kod" demiyor. | `placeholder` ekle. |
| O-07 | P1 | Lobi oyuncu kartında "Karalak7917 (…" kesik. Taç ve "(Sen)" sığmıyor. | Ad otomatik sığsın (en düşük 0.75x). "(Sen)" alt satırda küçük etiket olsun ya da yalnızca kart rengi ile ifade edilsin. |
| O-08 | P2 | Lobide yalnızken Hazır düğmesi yok, yalnızca "Arkadaşlarını bekliyoruz…" yazıyor. | Tasarım kararı; kalabilir. |
| O-09 | — | Hazır, 3-2-1 ve kıvılcım akışı (önceki oturumda yapıldı) yeni lobi tasarımında **iki gerçek cihazla** test edilmedi. | Test listesine al. |

### 2.8 Mağaza (`store/StoreScreen.kt`)

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| M-01 | P1 | Ayrı ve koyu bir tema: kendi "Karalak Mağaza" logosu, koyu ahşap sekmeler, farklı geri düğmesi. Uygulamanın geri kalanından kopuk. | Karar: (a) Mağazayı diğer sahnelerin dili olan açık ahşap ve kağıda taşı (öneri), ya da (b) koyu temayı koru ama geri düğmesi, sekme ve kart stillerini ortak bileşenlerle eşitle. |
| M-02 | P1 | Kartlar yarı saydam ve arkadaki resim (yazılı notlar, mumlar) içinden görünüyor, okunabilirlik düşüyor. | Kart zemini opak krem olsun (`#FFF6E6`, alfa 0.96). |
| M-03 | P1 | "Çerçeveler" sekmesi resimdeki "Küçük Çizimler Büyük…" notunun üstüne biniyor. | Arka plan resminde o bölgeyi temizle ya da sekmelerin arkasına koyu bir şerit koy. |
| M-04 | P2 | "Sende: 1" mor, "Sende: 2" mavi; renk anlamsız biçimde değişiyor. | Tek renk. |
| M-05 | P2 | Giriş uyarısında ☁️ emoji kullanılıyor, diğer ekranlarda Material simge var. | Simge dili birleştirilsin. |

### 2.9 Ayarlar / Sorun Bildir / Hesap

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| A-01 | P1 | 4 anahtar kartında anahtar sağ üstte, yazı sol altta, orta kısım boş. Dengesiz duruyor. | Simge, yazı ve anahtarı tek satırda dikeyde ortala ya da kart boyunu %30 kısalt. |
| A-02 | P1 | "Bildirimler geç mi geliyor?" kartı her zaman görünüyor ve ekranın üçte birini kaplıyor. | Yalnızca pil optimizasyonu açıksa ve bildirimler açıksa göster. Düzeltildikten sonra gizle. |
| A-03 | P2 | Liste kaydırılınca kartlar başlığın altına keskin bir kesikle giriyor ("Titreşim / Bildirimler" kartlarının yalnızca yazısı görünüyor). | Kayma geçişini (fade) 28dp'den 48dp'ye çıkar. |
| A-04 | P1 | "Gizlilik Politikası" telefonun Chrome'unu açıyor. Emülatörde Chrome karşılama ekranına düştü. | Custom Tab kullan (`androidx.browser`). |
| A-05 | P1 | Sorun Bildir ekranında pasif "Gönder" düğmesinin altında koyu bir çizgi kalıntısı var (resim ve gölge artığı). Altta büyük boşluk var. | Pasif durumda gölge ve kenar çizimini kapat. |
| A-06 | P1 | Hesap ekranı tamamen eski tasarımda. | S-05. |

### 2.10 Arkadaşlar / Düellolar

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| F-01 | P1 | Sağ üstteki pano simgesi Düellolar'ı açıyor, ama simge bunu anlatmıyor. | Kılıç ya da bayrak simgesi ve "Düellolar" etiketi. Bekleyen düello varsa sayı rozeti. |
| F-02 | P1 | Davet kodu kartındaki boya lekeleri yazıların altına biniyor (pembe leke "Kodun" yazısının yanında). | Leke dekorlarını kart kenarına sınırla. |
| F-03 | P2 | Açıklama satırı kötü bölünüyor: "kazanın!" tek başına 2. satırda. | Otomatik sığdır ya da metni kısalt: "Arkadaşın seviye 5'e ulaşınca ikiniz de 500 XP kazanın!" |
| F-04 | P2 | "Arkadaş ekle" ve "Arkadaşların" küçük tabelaları sola yaslı, ana başlık ortalı. | Bilinçli bir tercih olabilir. Tutarlılık için gözden geçir. |
| F-05 | P1 | Düellolar ekranı eski tasarımda. | S-05. |

### 2.11 Aylık Lig / Başarımlar / Dünyalar

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| L-01 | P2 | "Arkadaşlar · toplam XP" hapında saat simgesi var, anlamı uymuyor. | Kişi ya da kupa simgesi. |
| L-02 | P2 | Ödül kartında "Kor" adı ve pembe bir fırça izi var, ödülün ne olduğu anlaşılmıyor. | Kalem ödülü ise kalemin gerçek önizlemesi ve "Kalem" etiketi. |
| L-03 | P2 | Arkadaşlar sekmesi tek kişiyken ekranın %60'ı boş. | Boş durum kartını büyüt ya da "Arkadaş davet et" düğmesi ekle. |
| AC-01 | P2 | Ödülü alınmış başarım kartlarında hem "🎁 Ödül alındı" hem ödül hapı gösteriliyor (bilgi tekrarı). | Alınmışsa hapı soluk göster ya da gizle. |
| W-01 | P2 | Aşama haritasının altında resim bitiyor ve düz zeytin yeşili bir bant başlıyor (sahne alt paneli doldurmuyor). | Resmi alta doğru uzat ya da panelin arkasına gradyan koy. |
| W-02 | P2 | 1. aşamanın üstündeki yıldızlar çok küçük. 8. aşama durum çubuğunun altında kalıyor. | Yıldız boyunu büyüt. Harita kaydırmasına üst boşluk ekle. |

### 2.12 Kasa pencereleri ve açılış

| Kimlik | Önem | Görülen | Çözüm |
|---|---|---|---|
| C-01 | P1 | "Kasalar nasıl çalışır?" ve kasa ayrıntı pencereleri eski krem stilde ve farklı yazı tipinde. | S-04b. |
| C-02 | P2 | Bilgi penceresinde adım 2'nin başlığı "Kilidi aç", plakalar artık süre ve "Aç!" gösteriyor. | `chest_info_step_open_title` → "Süreyi başlat". |
| SP-01 | P2 | Sistem açılış ekranı düz hardal zemin üstünde küçük logo. Hemen ardından boyalı ana sayfa geliyor; geçiş sert. | `windowSplashScreenBackground` sahnenin baskın rengine (`#C98A3E` civarı) ayarlansın. İkon boyutu `windowSplashScreenAnimatedIcon` ile büyütülsün. |

---

## 3. Kod ve teknik notlar

| Kimlik | Önem | Konu | Öneri |
|---|---|---|---|
| K-01 | P1 | Kullanılmayan büyük resimler (yaklaşık 2 MB kaynak): `bg_home` (595 KB), `store_mascot_banner`, `home_logo`, `brand_logo`, `home_desk`, `home_tile_*` (9 adet), `fr_cream_a`, `level_tier_*`, `mascot_pencil_wink`, `fr_share`, `icon_*` (eski ana sayfa). | Release'de `shrinkResources` bunları zaten atıyor. Yine de silinip depo temizlensin. **Silmeden önce** her biri için `grep -rn "<ad>"` ile tekrar doğrula. |
| K-02 | P1 | `drawable-nodpi` 24 MB; dünya arka planlarının her biri 700-780 KB. Debug APK 69 MB. | WebP kalitesini 80'e indir ve boyutu en fazla 1440 px yükseklik yap. Gerekirse Play Asset Delivery. |
| K-03 | P1 | Boyalı ekranlarda koordinatlar "art birimi" ile elle yazılmış (`box(388f,548f,…)`). Resim değişince hepsi kayıyor. | `PaintedScene` yardımcısı (S-02) ve her ekran için koordinatları tek bir `object HomeArt { val dailyTitle = ArtRect(...) }` içinde topla. |
| K-04 | P2 | `LetteredText`, dış çizgi ve dolgu için iki ayrı `Text` çiziyor. Taşma ve kesilme durumunda ikisi farklı davranabilir. | S-01'de tek ölçüm ile çöz. `drawText` ile tek geçişte de çizilebilir. |
| K-05 | P2 | `NavGraph`'taki durum çubuğu mantığı tek rota içeriyor. | S-03. |
| K-06 | P2 | `Screen.Chests` rotası yeni ana sayfadan erişilemiyor (`MainMenuScreen.kt:145`'te `onChests` var, `PaintedHome` almıyor). | Ya bağla ya da kaldır. |
| K-07 | P2 | Dosyalarda karışık satır sonu (LF/CRLF uyarıları). | `.gitattributes` içine `* text=auto eol=lf` ekle ve bir kez normalize et. |

---

## 4. Uygulama sırası (her aşamanın sonunda derle, emülatörde 3 boyutta ekran görüntüsü al, commit at)

1. **Aşama 1 — Altyapı (S-01, S-03, S-04a, S-04b bileşenleri):**
   - `AutoFitLetteredText`, `AutoFitText`, `StatusBarScrim`, `PaintedBackButton`, `PaintedDialog` ve `PaintedConfirmDialog`'u yaz.
   - `LetteredText`'i otomatik sığdırmaya yönlendir.
   - Durum çubuğu rota kümesini ekle.
   - *Beklenen etki:* H-01…H-07, R-01, R-02, O-07 ve durum çubuğu sorunlarının büyük kısmı tek seferde düzelir.
2. **Aşama 2 — Ana sayfa (S-02 + 2.1):** `PaintedScene` dönüşümü, kasa plakalarının ölçeği, H-01…H-12.
3. **Aşama 3 — Hatalar:** B-01 (küçük resim çizgi kalınlığı), B-02 (rakip avatarı), D-02 (ortak `DailyPips`), R-03, R-04.
4. **Aşama 4 — Pencereler:** Bütün `AlertDialog` ve krem pencereleri `PaintedDialog`'a çevir (S-04b listesi).
5. **Aşama 5 — Geri düğmeleri ve çipler:** S-04a, O-02, O-03, O-06.
6. **Aşama 6 — Eski ekranları yeni dile taşı (S-05):** Önce oyuncuya görünenler: BreakScreen, OnlineResult, Eğitim (maskot S-06 kararıyla), Hesap, Düellolar.
7. **Aşama 7 — Mağaza (M-01…M-05):** Kullanıcı kararı (a) veya (b) gerekli.
8. **Aşama 8 — Cila:** P2 maddeleri, metin düzeltmeleri, K-01, K-02, K-07.

**Kullanıcıya sorulması gereken kararlar** (koda başlamadan önce):
- S-06: Varsayılan avatar köpek mi olsun?
- M-01: Mağaza açık temaya mı geçsin, yoksa koyu tema mı korunsun?
- D-01: Günlük meydan okuma sonrası geçiş reklamı kalsın mı?
- H-06: "Her galibiyet bir hazine!" yazısı resme gömülü mü? (resim dosyasına bakılarak doğrulanacak)

---

## 5. Test listesi (her aşamadan sonra)

Araçlar: `D:\Temp\shot.ps1 <ad> [ölçek]` küçültülmüş ekran görüntüsü alır, `D:\Temp\ui.ps1 [-bounds]` arayüz ağacını okur.

```bash
# Ekran boyutları (yalnızca emülatör, -s emulator-5554)
adb -s emulator-5554 shell wm size 1080x1920
adb -s emulator-5554 shell wm size 720x1600 ; adb -s emulator-5554 shell wm density 320
adb -s emulator-5554 shell wm size reset ; adb -s emulator-5554 shell wm density reset
# Yazı ölçeği
adb -s emulator-5554 shell settings put system font_scale 1.3
adb -s emulator-5554 shell settings put system font_scale 1.0
```

Her boyutta bakılacak ekranlar:
1. Ana sayfa: günlük açık ve tamamlanmış hâli; kasa yuvaları boş, sayıyor ve hazır hâli.
2. Günlük oyun: çizim → ara ekran → tahmin → sonuç.
3. Hızlı Eşleş: arama → giriş → oyun → sonuç (rakip küçük resimleri!).
4. Arkadaşınla Yarış → Oda Kur → Lobi; Koda Katıl.
5. Çevrimdışı, Dünyalar, Aşama haritası.
6. Arkadaşlar, Düellolar, Lig (iki sekme), Başarımlar.
7. Mağaza (3 sekme), Ayarlar (kaydırılmış hâli dahil), Hesap, Sorun Bildir, Eğitim.
8. Bütün pencereler: oyundan çık, kasa bilgi, kasa ayrıntı, kasa kazandın / kaybettin, puanlama, giriş daveti.

Bu denetimde **test edilemeyenler:**
- İki gerçek cihazla çevrimiçi oyun (Hazır, 3-2-1, OnlineResult).
- Kasa kazanma penceresi.
- Kullanıcı adı sabitleme penceresi.
- Google ile giriş.
- Bildirim izni akışı.

Bunlar Aşama 6 sonunda iki cihazla doğrulanmalı.
