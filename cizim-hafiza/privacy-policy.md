# Gizlilik Politikası — Karalak

**Uygulama:** Karalak — Çiz, Hatırla, Yarış
**Paket adı:** com.sualtikasifi.cizimhafiza
**Geliştirici:** AVC Software
**İletişim:** sualtikasifi@gmail.com

**Son güncelleme:** 30 Eylül 2026

> Yayınlanan sürüm: https://sualtikasifi.github.io/app-ads/
> Play Console'a girilecek URL budur. Bu dosya ile `docs/index.html`
> aynı metni taşımalıdır — Play, politikanın uygulama adını VE geliştirici
> adını açıkça içermesini şart koşuyor ve ikisi eksik olduğu için sürüm bir
> kez reddedildi.

## Tek kişilik oyun modu

Oyun geçmişin (skorlar, çizim verilerin, tahminlerin, kıdem/puan durumun)
yalnızca **cihazında yerel olarak** saklanır ve hiçbir sunucuya gönderilmez.

## Arkadaşınla çevrimiçi oynama modu

Bu isteğe bağlı modu kullanmayı tercih edersen, oyunun çalışabilmesi için
aşağıdaki bilgiler Google'ın Firebase altyapısına (Firestore veritabanı)
gönderilir:

- **Cihazına özel, anonim bir kimlik** (Firebase Anonymous Authentication) —
  isim, e-posta veya şifre istenmez.
- **Kendi seçtiğin takma ad** — odadaki diğer oyunculara ve arkadaşlarına
  gösterilir.
- **Oda kodu, o oyundaki kelime listesi, skorların ve çizimlerin** — sadece
  o oyun odasındaki oyuncular tarafından görülebilir; oyun bittikten sonra
  bu veriler sunucuda kalmaya devam eder (odalar otomatik silinmez) ancak
  başka bir kullanıcı tarafından erişilemez.
- **Gönderdiğin emoji tepkileri.**

Bu veriler reklam amacıyla kullanılmaz ve satılmaz. Google'ın Firebase
altyapısı için genel gizlilik uygulamaları geçerlidir:
https://firebase.google.com/support/privacy

## Google ile giriş ve genel profil

Google ile giriş yapmayı seçersen **e-posta adresin, görünen adın ve profil
fotoğrafın** ilerlemeni buluta yedeklemek ve yeni bir cihazda geri yüklemek
için işlenir. E-posta adresin ve görünen adın diğer oyunculara gösterilmez.

Çevrimiçi modları, arkadaş listesini veya ligi kullandığında **genel
profilin** diğer oyunculara gösterilir: takma adın, seviyen, seçtiğin avatar
çerçevesi, haftalık/aylık lig puanın ve — Google ile giriş yaptıysan ve
maskot yerine profil fotoğrafını seçtiysen — **Google profil fotoğrafının
adresi**. İstersen profil fotoğrafı yerine oyunun maskotunu seçebilirsin.

## Hızlı Eşleş modu

Hızlı Eşleş modunda oynadığın turlar (kelimeler, skorlar ve çizimler) bir
havuza kaydedilir ve ileride başka oyunculara rakip turu olarak sunulabilir.
Bir tur havuza **ancak bir insan tarafından incelenip onaylandıktan sonra**
girer. Uygunsuz bulduğun bir çizimi uygulama içinden bildirebilirsin; iki
farklı oyuncunun bildirdiği tur otomatik olarak havuzdan çıkar.

Havuzda oyuncu başına en fazla on tur tutulur; daha eskiler silinir.

## Çizimlerin tanıtımda kullanılabilir

İncelemeye gelen çizimlerden bazıları, Karalak'ın **sosyal medya
hesaplarında tanıtım amacıyla** görsel veya kısa video olarak yayınlanabilir.

Yayınlanan içerikte **yalnızca çizimin kendisi ve çizilen kelime** yer alır.
Takma adın, e-posta adresin, hesap kimliğin veya seni tanımlayabilecek başka
hiçbir bilgi bu içeriğe eklenmez — çizim, kimin çizdiği belli olmadan
paylaşılır.

Çiziminin bu şekilde kullanılmasını istemiyorsan sualtikasifi@gmail.com
adresine yazman yeterlidir; ilgili içerik yayından kaldırılır.

## Çökme raporları ve kullanım istatistikleri

Uygulama Firebase Crashlytics ve Firebase Analytics kullanır: çökme dökümü,
cihaz modeli, Android ve uygulama sürümü, toplu kullanım istatistikleri ve
cihaza özel bir kurulum kimliği. IP adresinden ülke/şehir düzeyinde yaklaşık
bir konum türetilebilir. Bu veriler ad, e-posta veya çizimlerle
eşleştirilmez, reklam amacıyla kullanılmaz ve satılmaz.

## Reklamlar

Uygulama, **Google AdMob** aracılığıyla reklam gösterir:

- Sonuç ekranından sonra, ilk oyun hariç her ikinci maçta (iki reklam
  arasında en az 3 dakika olmak koşuluyla) gösterilen bir **geçiş reklamı**.
- Senin kendi isteğinle açtığın durumlarda (ek ipucu, sonuçta XP x2, seri
  kurtarma, 4 saatte bir +500 altın, günde bir bedava kasa) gösterilen
  **ödüllü reklamlar**. Ödüllü reklamlarda ödül yalnızca reklamı sonuna kadar
  izlersen verilir.

Ödüllü reklamlar hiçbir zaman kendiliğinden, sen bir işlem başlatmadan
gösterilmez. Banner, yerel veya uygulama açılış reklamı yoktur.

AdMob, reklamları göstermek ve kişiselleştirmek için **reklam kimliği
(advertising ID)** gibi cihaz tanımlayıcılarını işleyebilir. AB/EEA ve
Birleşik Krallık'taki kullanıcılara Google'ın Kullanıcı Mesaj Platformu
(UMP) üzerinden reklam kişiselleştirme rızası sorulur; bu rızanı istediğin
an geri alabilirsin. Telefonunun ayarlarından reklam kimliğini sıfırlayabilir
veya reklam kişiselleştirmeyi kapatabilirsin. Google'ın kendi gizlilik
politikası geçerlidir: https://policies.google.com/privacy

## İzinler

- **Titreşim (VIBRATE)** — çizim süresinin son saniyelerinde haptik uyarı.
- **Bildirim gönderme (POST_NOTIFICATIONS)** — günlük görev hatırlatması ve
  arkadaş davetleri; reddedebilirsin, oyun çalışmaya devam eder.
- **Açılışta başlatma (RECEIVE_BOOT_COMPLETED)** — telefonu yeniden
  başlattığında kurulu hatırlatmaların kaybolmaması için.
- **Reklam kimliği (AD_ID)** — reklamların sunulması ve dolandırıcılığın
  önlenmesi için.

## Verilerin silinmesi

Karalak uygulamasını cihazından kaldırdığında (sil/uninstall), cihazında
yerel olarak tutulan tüm oyun geçmişi, kıdem/puan durumu ve ayarlar
otomatik olarak silinir — ayrıca bir işlem yapmana gerek yoktur.

**Uygulama içinden:** Ayarlar → Hesap → *Hesabı Sil*. Bu işlem Google
bağlantını, bulut yedeğini, takma adını, arkadaş listeni, kaydedilmiş
turlarını ve çizimlerini kalıcı olarak siler. Geri alınamaz.

**E-postayla:** sualtikasifi@gmail.com adresine, kullandığın takma adı ve
(varsa) oda kodunu belirterek bir e-posta gönder. Talebin en geç 30 gün
içinde işleme alınır.

Silinen veriler: anonim kullanıcı kimliğin, takma adın, oda/oyun kayıtların
(kelime listesi, skorlar, çizimler), Google hesabına bağlı yedeğin ve
gönderdiğin emoji tepkileri. Talep edilmediği sürece bu veriler
Firebase'de saklanabilir (oyun odaları otomatik silinmez).

## Üçüncü taraflarla paylaşım

Verilerin satılmaz. Uygulama, yukarıda açıklanan Firebase ve Google AdMob
(Google) altyapısı dışında hiçbir veriyi üçüncü taraflarla paylaşmaz.

## Çocuklar

Karalak 13 yaş altındaki çocuklara yönelik değildir ve bilerek bu yaş
grubundan veri toplamaz.

## İletişim

Sorularınız için: sualtikasifi@gmail.com
