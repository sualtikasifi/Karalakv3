# Portekizce ve İspanyolca çeviri kontrolü

Bu klasördeki iki tablo, ana dili Portekizce (Brezilya) veya İspanyolca (Latin Amerika) olan bir kişinin çevirileri kontrol etmesi için hazırlandı. Google E-Tablolar veya Excel ile açılır.

- `ui_pt_es.csv`: uygulamanın arayüz metinleri (geliştirici paneli hariç). Her satırda Türkçe, İngilizce, Portekizce ve İspanyolca metin yan yana.
- `words_pt_es.csv`: oyunun 1.141 kelimesi. `dealt = no` olan 25 kelime Türkiye'ye özgü olduğu için (lahmacun, simit…) Portekizce ve İspanyolca oyunlarda hiç çıkmaz.

Kontrol eden kişi düzeltmesini Portekizce/İspanyolca sütununa yazıp `note` sütununa kısa bir açıklama ekleyebilir.

Dikkat edilecekler:
- `%1$d`, `%1$s`, `%%` gibi yer tutucular aynen kalmalı (sayı veya isim onların yerine gelir).
- Düğme metinleri kısa olmalı; İngilizcedekinden çok uzun bir metin düğmeye sığmayabilir.
- Kelimeler çizilebilir ve tahmin edilebilir olmalı. Aynı dilde iki kelime aynı yazılamaz.
- İspanyolca nötr Latin Amerika İspanyolcası ve "tú" hitabıyla; Portekizce Brezilya Portekizcesi ve "você" hitabıyla yazıldı.

Düzeltilmiş tabloyu geliştiriciye geri vermek yeterli: kaynak dosyalar `app/src/main/res/values-pt`, `values-es` ve `app/src/main/assets/words_pt.json`, `words_es.json`; web dosyaları `node scripts/gen_web_strings.js`, `gen_web_runes.js` ve `gen_web_langs.js` ile yeniden üretilir, kelime dosyaları `hosting/oyna/` altına kopyalanır.
