# Web (hosting/oyna) ↔ Android uyum listesi

Karşılaştırma yöntemi: aynı ekranın emülatörden (adb) ve masaüstü Chrome'dan (1080x2400'e eşdeğer) yan yana ekran görüntüsü.
`scratchpad/webshot.js` + `pair.ps1` ile üretildi; web mantığı için `hosting/oyna/economy.test.mjs` (node --test).

## Yapıldı
- XP ve altın kuralları Android ile aynı (economy.js): kelime XP'si, bölüm/günlük XP, rün XP'si, altın kalemleri, rün katkısı, XP etkinliği, günün ilk Hızlı Eşleş'i 2x, çıkış cezası, çevrimiçi oda altını + maç XP'si + kasa
- Çevrimdışı Oyna kurulum ekranı (kelime sayısı, mod, kategori, zorluk); Oda Kur ekranı aynı tasarımda
- Çizim ekranı: süre + kelime sayısı hapı; tahmin ekranı: "★ +N XP" ve canlı hız bonusu hapı

## Sırada (öncelik sırasıyla)
1. Rün haritası: detay kartı, fog/halka kapıları Android ile aynı, rün durumu penceresi, rün etkilerinin web'de uygulanması (kasa süresi, yedek yuva, anahtarcı, seri sigortası, şanslı zar, joker indirimi, günlük ek joker)
2. Başarımlar: ödül toplama/altın akışı Android ile aynı
3. Kasalar: kasa ekranı ve detayı, hız aşımı, sıra kuralları
4. Lig: sekme sırası (Arkadaşlar önce), yenile düğmesi, ödül toplama, arkadaş tablosu
5. Düellolar (arkadaş ekranındaki "Düellolar")
6. Ana ekran: rün hazır rozeti, kasa yuvası durumları, ücretsiz altın/kasa
7. Seviye haritası: mevcut seviyeye kaydırma, numaralı kilitli düğümler, alt bilgi paneli
8. Hesap: bulut yedeği (users/{uid}/backup/state, plausibleBackup kuralına uygun), giriş sonrası ilerleme aktarımı
9. Çizimde "Bitir" düğmesi Android'de yok (bilerek korundu); reklam düğmeleri web'de yok
