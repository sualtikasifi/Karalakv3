# Karalak

Kotlin + Jetpack Compose ile geliştirilmiş çizim/hafıza oyunu. MVVM + Clean
Architecture (`data` / `domain` / `presentation`), Room, Hilt, Navigation-Compose.

## Kurulum

1. Android Studio ile `cizim-hafiza/` klasörünü aç.
2. `local.properties.example` dosyasını `local.properties` olarak kopyala,
   `sdk.dir` değerini kendi Android SDK yoluna göre düzenle.
3. Gerçek AdMob ID'lerin varsa aynı dosyaya `ADMOB_*` anahtarlarını ekle
   (boş bırakırsan Google'ın public test ID'leri kullanılır — bkz. altta).

## Mimari

```
data/          Room entity/DAO/Database, repository implementasyonu
domain/        Modeller, repository arayüzü, use case'ler
presentation/  Compose ekranları, ViewModel'ler, navigation
di/            Hilt modülleri
ads/           AdManager (geçiş + ödüllü reklamlar, UMP onayı)
util/          Constants, AnswerMatcher (Levenshtein), VibratorHelper, SettingsRepository
```

## Kelime havuzu

`app/src/main/assets/words.json` — her uygulama açılışında Room ile
senkronize edilir (`WordSeeder` + `CizimHafizaApp.onCreate`): veritabanındaki
kelime sayısı dosyadakiyle eşleşmiyorsa eksikler otomatik eklenir. Şu an 1169
kelime var (8 kategori × 83–165). Daha da eklemek için:

1. `WORDS_SCHEMA.md` dosyasındaki şemayı ve id/kategori kurallarını oku.
2. Aynı formatta yeni kayıtları `words.json`'a ekle (id'ler unique olmalı,
   en yüksek id'den devam et — mevcut kayıtları değiştirme).
3. Uygulamayı yeniden derleyip kur; ilk açılışta yeni kelimeler otomatik
   eklenir, mevcut oyun geçmişi/istatistikler silinmez.

## AdMob

`GameConstants.ADMOB_ENABLED = BuildConfig.DEBUG || BuildConfig.ADMOB_REAL_IDS` —
reklamlar debug build'lerde (Google'ın herkese açık TEST kimlikleriyle, serbestçe
tıklanabilir) ve gerçek kimlikler `local.properties`'te varsa release build'lerde
açıktır. Kimlikler yoksa release reklamsız çıkar ve gelir gelmez; bunu CI'daki
"release reklam kimlikleri" kontrolü yakalar. Kendi *gerçek* biriminize tıklamak
AdMob hesabını askıya aldırır.

Geçiş reklamı: ilk oyun hariç her ikinci maçta. Ödüllü reklamlar: ipucu, sonuçta
XP x2, seri kurtarma, 4 saatte bir +500 altın, günde bir bedava kasa.

## Hız bonusu

`GameConstants.SPEED_BONUS_ENABLED = true` — 3 saniye altı doğru cevaba +2
puan. Tek satırdan kapatılabilir.

## Play Store

`PLAY_STORE.md` dosyasında Türkçe mağaza metni taslağı, `privacy-policy.md`
dosyasında gizlilik politikası taslağı var.
