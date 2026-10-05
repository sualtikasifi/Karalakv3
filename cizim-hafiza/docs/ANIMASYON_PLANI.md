# Karalak — Animasyon ve Görsel Cila Planı

**Tarih:** 5 Ekim 2026. Önceki plan: `UI_DENETIM_PLANI.md` (hatalar ve tutarlılık). Bu belge hareket ve cilaya odaklanır.

## 1. Tespit
Ekran başına animasyon sayısı (kodda `animate*`, `Animatable`, `tween`, `spring`, `rememberInfiniteTransition` kullanımı):
- **Hiç yok (0):** Düellolar, Düello kur, Oda Kur, Çevrimiçi sonuç, Sorun Bildir, Eğitim kartı, Kelime sayısı, Dünyalar, tüm pencereler (kasa bilgi, çıkış, silme…), çizim ekranı.
- **Az (1–4):** Arkadaşlar, Koda Katıl, Arkadaşınla Yarış, Sonuç, Ayarlar, Hesap.
- **İyi (>10):** Hızlı Eşleş, Tahmin, Kasa kazanma, Aşama haritası, Mağaza, ana sayfa kasa plakaları.

Eksikler:
1. Ekranlar ve listeler hazır gelip duruyor: giriş hareketi yok.
2. Dokunma geri bildirimi tutarsız: bazı düğmeler çöküyor, ana sayfa kutucukları ve bağlantı satırları hiçbir şey yapmıyor.
3. Ana eylem düğmeleri durağan: dikkat çeken bir hareket yok.
4. Sayılar (altın, XP) zıplıyor, sayarak değişmiyor.
5. Pencereler aniden beliriyor.
6. Kutlama yok: oyun bitince hiçbir şey olmuyor.
7. Maskot ve rozetler hareketsiz.

## 2. Çözüm: ortak hareket sözlüğü (`presentation/common/Motion.kt`)
| Parça | Ne yapar | Nerede kullanılıyor |
|---|---|---|
| `Modifier.springIn(index, stepMs, fromY, key)` | Yaylanarak yükselir, %88'den büyür, belirir. Kardeşler `index` ile art arda | pencereler, ayarlar satırları, lig satırları, başarım satırları, arkadaş satırları, lobi oyuncuları, sonuç blokları, kurulum panelleri, kasa yuvaları, çizim kelime kartı, eğitim kartı |
| `Modifier.pressable()` | Basınca küçülür, yaylanarak döner | günlük "ŞİMDİ OYNA" |
| `Modifier.pressFlash()` | Resim üstündeki dokunma alanı basılıyken beyazca parlar | ana sayfa 9 kutucuk |
| `Modifier.breathing()` | Yavaş nefes alma | ana eylem düğmeleri |
| `Modifier.glint()` | Düğmenin üstünden periyodik ışık süzülmesi | Başla, Oda Kur, Oda Oluştur, günlük oyna, pencere ana düğmesi |
| `Modifier.floating()` | Hafif süzülme ve eğilme | eğitim maskotu |
| `Modifier.popIn()` | Sıfırdan taşarak çıkar | günlük tik ve çarpılar (sırayla) |
| `Modifier.sceneIn()` / `SceneEntrance` | Ekran bir bütün olarak belirir | ana sayfa |
| `CountUpText` / `animateIntAsState` | Sayı sayarak değişir | ana sayfa altın, sonuç XP |
| `ConfettiBurst` | Bir kez düşen renkli kağıt yağmuru | sonuç ekranı (puan kazanıldığında) |

## 3. Yapılanlar (bu turda)
Ana sayfa, tüm pencereler, kurulum ekranları, ayarlar, lig, başarımlar, arkadaşlar, lobi, sonuç, çizim kelimesi, eğitim ve çevrimiçi giriş ekranları. Detay için commit mesajına bak.

## 4. Sıradaki (öncelik sırasıyla)
1. **Ekran geçişleri:** Şimdi yalnızca yatay kayma var. Önerilen: ana sayfadan alt sayfaya geçerken hedef kutucuğun içinden büyüyen "paylaşılan öğe" geçişi; geri dönüşte tersi.
2. **Tahmin ekranı:** Doğru cevapta tuvalde yeşil dalga, yanlışta kırmızı sarsıntı (sarsıntı var, dalga yok); joker kullanımında jokerin tuvale uçması.
3. **Çizim ekranı:** Süre son 3 saniyede halka kırmızıya dönüp titreşsin; çizgi çizilirken kalem ucundan ince kıvılcım.
4. **Kasa:** Kasa açılırken sallanma, kapak patlaması ve ödüllerin tek tek uçarak yerine konması (kasa kazanma penceresi iyi durumda; ana sayfadaki "Aç!" yalnızca nabız atıyor).
5. **Lig:** Kendi satırın sıra değişince yukarı/aşağı kayarak yer değiştirsin; ilk üç için taç sallanması.
6. **Mağaza:** Satın alma anında altın çipine uçan altın, kalem/çerçeve donatılınca halka parlaması.
7. **Başarımlar:** Ödül alınca kartın üzerinden XP/altın uçuşu (ana sayfadaki XP uçuşu hazır, burada kullanılmıyor).
8. **Arka plan canlılığı:** Ana sayfada lamba ışığı hafifçe sallansın, yapraklar rüzgârla titresin (katmanlı arka plan gerekir; mevcut resim tek parça).
9. **Mikro etkileşim sesleri:** Dokunma, belirme ve kutlama için kısa sesler (ses ayarına bağlı).

## 5. Kurallar
- Hiçbir hareket 450 ms'den uzun bir *giriş* süresi tutmamalı; tekrarlayanlar yalnızca dikkat çekilecek tek bir öğede olmalı.
- `graphicsLayer` kullanılır, yerleşim değiştirilmez (kayma, tekrar ölçme olmasın).
- Liste öğeleri için `springIn` yalnızca ilk 8–12 öğeye kademeli verilir; sonrakiler anında gelir.
- Sistem animasyon ölçeği kapalıysa Compose zaten süreleri sıfırlar; ek bir şey gerekmez.
