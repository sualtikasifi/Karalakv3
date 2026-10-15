# Rün ikonları: yeni görsel üretim promptu

## 1. Ortak stil promptu (her ikonun başına ekle)

> Cute glossy mobile-game rune icon, a single centered object, chibi cartoon style, thick dark-brown outline, rich warm saturated colors, soft top-left highlight, subtle inner shading, clean vector-like painting like a casual game (Brawl Stars / Clash Royale item art). The object fills about 75% of the square, perfectly centered both horizontally and vertically, balanced margins on all four sides. Transparent background. No frame, no border, no plate, no text, no numbers, no cast shadow on the ground, no extra sparkles floating outside the object. 1024x1024 PNG with alpha.

Not: Görseller oyunda 256x256'ya küçülüp altın/kırmızı/yeşil/mavi plakaların içindeki yuvarlak boşluğa oturuyor. Plakayı **çizme**, sadece nesneyi çiz. Nesne tek parça, kare içinde ortada ve çok geniş/uzun olmasın (köşelere taşan kılıç, uzun asa vb. olmasın).

## 2. Tekrar eden / birbirine benzeyen ikonlar için yeni konular

Aşağıdaki rünlerin ikonları birbirine çok benziyor (kupalar, madalyalar, sandıklar). Her birine **farklı bir silüet** veriyoruz. Her satırın sonuna "Ortak stil promptu"nu ekle.

| Rün (id) | Ad | Yeni konu |
|---|---|---|
| win_2 | Şampiyon Kupası | Altın kupa, iki yanında defne dalları, önünde küçük kalem |
| win_3 | On İkiden Kupa | Tam ortasına isabet etmiş ok saplı hedef tahtası, üstünde minik taç |
| win_4 | Zafer Parıltısı | Yıldızlı zafer bayrağı (kısa direkli, dalgalanan kırmızı flama) |
| ach_3 | Başarı Kupası | Küçük podyum (1-2-3), en üst basamakta altın yıldız |
| ach_4 | Altın Madalya | Kalkan şeklinde altın madalya, üstünde taç, kısa mavi kurdele |
| ach_7 | Başarı Parıltısı | Yıldız patlaması şeklinde yuvarlak rozet (rosette), pembe-mor kurdeleli |
| eg_3 | Sonsuz Madalya | Sonsuzluk (∞) işaretli madalya, mor-altın parıltılı, kurdeleli |
| chest_2 | Mücevher Sandığı | Ağzına kadar renkli elmas ve mücevherle dolu açık sandık |
| chest_3 | Kraliyet Sandığı | Kırmızı kadife kaplı, altın tacı kapağında olan sandık |
| chest_5 | Işıltılı Sandık | Kapağı aralık, içinden altın ışık huzmesi çıkan sandık |
| chest_7 | Kasa Parıltısı | Büyük altın anahtar deliği ve etrafında ışık halkası olan kasa kapısı |
| eg_2 | Sonsuz Kasa | Üstünde ∞ işareti olan mor-altın sandık, açık kapaktan mavi ışık |
| login_2 | Gün Doğumu Sandığı | Küçük sandığın arkasından doğan turuncu güneş |
| cjoker_2 | Yonca Sandığı | Dört yapraklı yonca kabartmalı yeşil sandık |
| disc_4 | Kuruş Hesabı | Pembe kumbara domuz, sırtında bozuk para |
| word_gold | Kalem Parası | Altın sikke üstünde kalem silueti (basit, tek nesne) |

## 3. Teslim şekli

- Her ikon ayrı dosya: `rune_n_<id>.png` (örn. `rune_n_win_3.png`), 1024x1024, şeffaf arka plan.
- Dosya adı olarak yukarıdaki **id** sütununu kullan; ben webp'e çevirip 256'ya küçültüp ekleyeceğim.
- Aynı konuyu iki kez üretme: her satırın silüeti farklı olsun (yuvarlak / kare / uzun / yıldız gibi çeşitlilik).
