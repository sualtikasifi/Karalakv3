package com.sualtikasifi.cizimhafiza.presentation.reports.console

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.sualtikasifi.cizimhafiza.domain.repository.BugReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/** What the assistant is asked to produce. */
enum class AiTask(val title: String, val blurb: String) {
    Event("Etkinlik planı", "Özel bir gün için oyun içi etkinlik, ödüller, kelime paketi ve duyurular"),
    Social("Sosyal medya içeriği", "Paylaşıma hazır video senaryosu, gönderi metni, etiketler"),
    Growth("Büyüme stratejisi", "Canlı verilere bakıp ücretsiz/az bütçeli büyüme hamleleri"),
    Improve("Geliştirme önerileri", "Geri bildirim ve istatistiklerden ürün iyileştirmeleri"),
    Store("Mağaza sayfası (ASO)", "Play Store başlık, kısa/uzun açıklama, anahtar kelimeler"),
    Ask("Serbest soru", "Oyun bilgisiyle donanmış danışmana istediğini sor")
}

enum class SocialPlatform(val label: String) { Reels("TikTok / Reels / Shorts"), Post("Instagram gönderi / carousel"), X("X (Twitter)"), Community("Ekşi / Reddit / forum") }

data class SavedAnswer(val title: String, val text: String, val at: Long, val model: String = "", val costUsd: Double? = null)

data class AssistantState(
    val hasKey: Boolean = false,
    val keyPreview: String = "",
    val model: String = OpenRouterClient.DEFAULT_MODEL,
    val webSearch: Boolean = false,
    /** The key's spend as OpenRouter last reported it; null until read (or when it could not be). */
    val usage: KeyUsage? = null,
    val task: AiTask = AiTask.Event,
    val eventName: String = "",
    val eventDate: String = "",
    val platform: SocialPlatform = SocialPlatform.Reels,
    val topic: String = "",
    val question: String = "",
    val running: Boolean = false,
    val answer: AiAnswer? = null,
    val answerTitle: String = "",
    val error: String? = null,
    val history: List<SavedAnswer> = emptyList(),
    val showSettings: Boolean = false
)

/**
 * The panel's AI assistant. Every request carries a brief of the game, today's date with the
 * special days ahead, two weeks of live numbers and the latest player feedback, so the answers are
 * about Karalak as it is today rather than generic advice.
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val bugReports: BugReportRepository
) : ViewModel() {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(AssistantState())
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private var liveContext: String? = null

    init {
        // the panel talked to Gemini directly before OpenRouter; that key and its settings mean nothing now
        prefs.edit().remove("gemini_key").remove("model").remove("web_search").apply()
        val key = prefs.getString(KEY, "").orEmpty()
        _state.value = _state.value.copy(
            hasKey = key.isNotBlank(),
            keyPreview = preview(key),
            model = prefs.getString(MODEL, OpenRouterClient.DEFAULT_MODEL).orEmpty().ifBlank { OpenRouterClient.DEFAULT_MODEL },
            webSearch = prefs.getBoolean(SEARCH, false),
            history = readHistory(),
            showSettings = key.isBlank()
        )
        refreshUsage()
    }

    /** Reads the key's spend for the day and the month (shown in the header and the settings card). */
    fun refreshUsage() {
        val key = prefs.getString(KEY, "").orEmpty()
        if (key.isBlank()) { _state.value = _state.value.copy(usage = null); return }
        viewModelScope.launch {
            OpenRouterClient(key).usage().onSuccess { u -> _state.value = _state.value.copy(usage = u) }
        }
    }

    private fun preview(key: String) = if (key.length > 8) key.take(4) + "…" + key.takeLast(4) else ""

    fun saveSettings(key: String, model: String, webSearch: Boolean) {
        val k = key.trim()
        prefs.edit().apply {
            if (k.isNotEmpty()) putString(KEY, k)
            putString(MODEL, model.trim().ifBlank { OpenRouterClient.DEFAULT_MODEL })
            putBoolean(SEARCH, webSearch)
        }.apply()
        val stored = prefs.getString(KEY, "").orEmpty()
        _state.value = _state.value.copy(
            hasKey = stored.isNotBlank(), keyPreview = preview(stored),
            model = model.trim().ifBlank { OpenRouterClient.DEFAULT_MODEL }, webSearch = webSearch, showSettings = stored.isBlank()
        )
        refreshUsage()
    }

    fun forgetKey() {
        prefs.edit().remove(KEY).apply()
        _state.value = _state.value.copy(hasKey = false, keyPreview = "", usage = null, showSettings = true)
    }

    fun toggleSettings() { _state.value = _state.value.copy(showSettings = !_state.value.showSettings) }
    fun selectTask(t: AiTask) { _state.value = _state.value.copy(task = t, error = null) }
    fun setEvent(name: String, date: String) { _state.value = _state.value.copy(eventName = name, eventDate = date) }
    fun setPlatform(p: SocialPlatform) { _state.value = _state.value.copy(platform = p) }
    fun setTopic(t: String) { _state.value = _state.value.copy(topic = t) }
    fun setQuestion(q: String) { _state.value = _state.value.copy(question = q) }

    /** Opened from a special day on the dashboard: straight to an event plan for it. */
    fun planFor(day: SpecialDay) {
        _state.value = _state.value.copy(task = AiTask.Event, eventName = day.name, eventDate = day.date.toString(), answer = null, error = null)
        run()
    }

    fun run() {
        val st = _state.value
        val key = prefs.getString(KEY, "").orEmpty()
        if (key.isBlank()) { _state.value = st.copy(showSettings = true, error = "Önce OpenRouter anahtarını gir."); return }
        if (st.running) return
        _state.value = st.copy(running = true, error = null)
        viewModelScope.launch {
            val ctx = liveContext ?: buildLiveContext().also { liveContext = it }
            val (title, prompt) = promptFor(_state.value)
            val result = OpenRouterClient(key).generate(SYSTEM, ctx + "\n\n" + prompt, _state.value.model, _state.value.webSearch)
            result.onSuccess { a ->
                val saved = (listOf(SavedAnswer(title, a.text, System.currentTimeMillis(), a.model, a.costUsd)) + _state.value.history).take(30)
                writeHistory(saved)
                _state.value = _state.value.copy(running = false, answer = a, answerTitle = title, history = saved)
            }.onFailure { e ->
                _state.value = _state.value.copy(running = false, error = e.message ?: e::class.simpleName)
            }
            refreshUsage()
        }
    }

    fun openSaved(s: SavedAnswer) {
        _state.value = _state.value.copy(answer = AiAnswer(s.text, model = SAVED, costUsd = s.costUsd), answerTitle = s.title)
    }
    fun closeAnswer() { _state.value = _state.value.copy(answer = null) }
    fun deleteSaved(s: SavedAnswer) {
        val list = _state.value.history.filterNot { it.at == s.at }
        writeHistory(list); _state.value = _state.value.copy(history = list)
    }
    fun refreshContext() { liveContext = null }

    private suspend fun buildLiveContext(): String {
        val sb = StringBuilder()
        val today = LocalDate.now()
        sb.append("## Bugün\n").append(today).append(" (").append(today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("tr"))).append(")\n")
        sb.append("Yaklaşan özel günler: ")
        sb.append(SpecialDays.upcoming(today, 90).joinToString("; ") { "${it.name} ${it.date} (${it.daysLeft(today)} gün)" }).append("\n")
        runCatching { loadDaySeries(firestore, 14) }.getOrNull()?.let { s ->
            sb.append("\n## Son 14 günün canlı verisi (UTC gün)\n")
            sb.append("gün | aktif oyuncu | açılış | biten oyun | ödüllü reklam istek/izlenen\n")
            s.days.indices.forEach { i ->
                sb.append("${s.days[i]} | ${s.players[i]} | ${s.opens(i)} | ${s.games(i)} | ${s.adRequested(i)}/${s.adEarned(i)}\n")
            }
            s.currency()?.let { cur ->
                val rev = s.days.indices.sumOf { s.revenueMicros(it) } / 1_000_000.0
                sb.append("14 günlük reklam geliri: ").append(String.format(Locale.US, "%.2f", rev)).append(' ').append(cur).append('\n')
            }
        } ?: sb.append("\n(Canlı veri okunamadı.)\n")
        bugReports.allReports(25).getOrNull()?.takeIf { it.isNotEmpty() }?.let { list ->
            sb.append("\n## Oyunculardan son geri bildirimler\n")
            list.forEach { e -> sb.append("- [").append(e.report.category.name).append("] ").append(e.report.description.replace('\n', ' ').take(240)).append('\n') }
        }
        return sb.toString()
    }

    private fun promptFor(st: AssistantState): Pair<String, String> = when (st.task) {
        AiTask.Event -> {
            val name = st.eventName.ifBlank { SpecialDays.upcoming().firstOrNull()?.name ?: "Sıradaki özel gün" }
            val date = st.eventDate.ifBlank { SpecialDays.upcoming().firstOrNull()?.date?.toString().orEmpty() }
            "Etkinlik: $name" to """
                # Görev: "$name" ($date) için Karalak'ta oyun içi etkinlik planı
                Elimizdeki araçlar (kod yazmadan, geliştirici panelinden açılabilenler): XP etkinliği (x1.5 / x2, başlangıç-bitiş), kupon kodları (altın / XP / kasa; sosyal medyada dağıtılır), aylık lig ödülü seçimi (kalem/çerçeve), günlük meydan okuma, oyuncuya bildirim. Yeni kelime paketi, kalem, çerçeve ya da tema görseli eklemek kod/çizim işi gerektirir — öner ama maliyetini belirt.
                İstenen bölümler:
                1. **Konsept ve başlık** (3 alternatif isim, etkinlik sloganı)
                2. **Zaman çizelgesi** (gün gün: duyuru, başlangıç, zirve, kapanış; saatleriyle)
                3. **Oyun içi mekanik** (hangi panel aracı, hangi değerlerle; neden oyuncuyu geri getirir)
                4. **Temalı kelime paketi** (30 kelime: kolay/orta/zor, çizilebilir ve eğlenceli; Türkçe)
                5. **Ödüller ve kupon kodları** (kod adları, ne verir, kaç kullanım, nerede paylaşılır)
                6. **Bildirim ve uygulama içi metinler** (3 push bildirimi, en fazla 60 karakter)
                7. **Sosyal medya duyuru paketi** (1 Reels/TikTok senaryosu sahne sahne, 1 gönderi metni, etiketler)
                8. **Ölçüm** (hangi metriğe bakılır, başarı eşiği; yukarıdaki canlı verilere göre gerçekçi hedef)
                9. **Maliyet ve risk** (0 TL ile nasıl yapılır, neye dikkat)
            """.trimIndent()
        }
        AiTask.Social -> "İçerik: ${st.platform.label}" to """
            # Görev: ${st.platform.label} için paylaşıma hazır 4 içerik
            Konu/odak: ${st.topic.ifBlank { "serbest — oyunun en paylaşılabilir anlarını kullan (kötü çizim komedisi, 'ne çizdiğini hatırlayamama', arkadaş düellosu)" }}
            Her içerik için:
            - **Kanca** (ilk 1,5 saniye, ekrana yazı + söylenen cümle)
            - **Senaryo** (sahne sahne, süreleriyle; telefonla tek kişi çekebilmeli, yüz göstermeden de olabilmeli)
            - **Ekran yazıları** ve **açıklama metni** (Türkçe, doğal, ajans dili değil)
            - **Etiketler** (en fazla 6, niş + geniş karışık) ve **ses/trend önerisi**
            - **Paylaşım zamanı** ve **CTA** (indirme bağlantısı / karalak.com / oda kodu)
            - **Neden işe yarar** (tek cümle, hangi psikolojik tetikleyici)
            Platformun kendi diline uy. Ekşi/Reddit gibi topluluklar seçildiyse reklam gibi değil, değer katan dürüst geliştirici paylaşımı yaz ve topluluk kurallarına dikkat et.
        """.trimIndent()
        AiTask.Growth -> "Büyüme stratejisi" to """
            # Görev: Karalak'ı sıfıra yakın bütçeyle büyütme planı
            Yukarıdaki canlı verileri yorumla (trend, oyun derinliği, reklam izlenme oranı) ve darboğazı söyle: edinim mi, aktivasyon mu, elde tutma mı, paylaşım mı?
            Sonra öncelik sırasıyla 10 hamle ver. Her biri için: ne yapılacak (somut adımlar), beklenen etki (yüksek/orta/düşük), efor (saat), maliyet (TL), nasıl ölçülür.
            Kurallar: genel geçer tavsiye yok ("sosyal medyayı kullanın" gibi). Türkiye'deki kanallara (TikTok, Instagram, YouTube Shorts, Ekşi Sözlük, Reddit r/Turkey ve r/AndroidGaming, Discord, okul/öğrenci toplulukları, mikro içerik üreticiler) ve oyunun viral mekaniklerine (oda kodu, düello, paylaşılabilir komik çizimler) dayan. Web sürümünün (karalak.com) kurulumsuz oynatma avantajını kullan.
        """.trimIndent()
        AiTask.Improve -> "Geliştirme önerileri" to """
            # Görev: Ürün iyileştirme önerileri
            Geri bildirimleri temalara ayır (kaç kez geçtiğiyle), sonra elde tutmayı ve paylaşımı en çok artıracak 8 geliştirme öner.
            Her biri: sorun → çözüm → beklenen etki → uygulama zorluğu (S/M/L) → riski. Mevcut özellikleri (lig, düello, kasa, rün, günlük meydan okuma) bozmadan.
            En sona "bu hafta yapılacak 3 şey" listesini koy.
        """.trimIndent()
        AiTask.Store -> "Mağaza sayfası (ASO)" to """
            # Görev: Google Play mağaza sayfası optimizasyonu (Türkçe)
            Ver: 5 başlık alternatifi (en fazla 30 karakter), 3 kısa açıklama (en fazla 80 karakter), 1 uzun açıklama (paragraflar + madde işaretleri, anahtar kelimeleri doğal yerleştir, en fazla 4000 karakter), 25 anahtar kelime (Türkçe arama niyetine göre: "çizim oyunu", "arkadaşla oynanan oyun", "kim ne çizdi" vb. — rekabet ve hacim tahminiyle), 8 ekran görüntüsü için üst başlık metni sırasıyla, tanıtım videosunun 30 saniyelik senaryosu.
            Rakip oyunlara (Gartic, Draw Something, skribbl.io, Quick Draw) karşı farkımızı (çiz + hatırla, hafıza oyunu) öne çıkar.
        """.trimIndent()
        AiTask.Ask -> "Soru: ${st.question.take(40)}" to "# Soru\n${st.question.ifBlank { "Bu hafta en çok neye odaklanmalıyım?" }}"
    }

    private fun readHistory(): List<SavedAnswer> = runCatching {
        Json.parseToJsonElement(prefs.getString(HISTORY, "[]").orEmpty()).jsonArray.mapNotNull { e ->
            val o = e.jsonObject
            SavedAnswer(
                o["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                o["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                o["at"]?.jsonPrimitive?.longOrNull ?: 0L,
                o["model"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                o["cost"]?.jsonPrimitive?.doubleOrNull
            )
        }
    }.getOrDefault(emptyList())

    private fun writeHistory(list: List<SavedAnswer>) {
        val arr: JsonArray = buildJsonArray { list.forEach { s -> addJsonObject {
            put("title", s.title); put("text", s.text); put("at", s.at); put("model", s.model)
            s.costUsd?.let { put("cost", it) }
        } } }
        prefs.edit().putString(HISTORY, arr.toString()).apply()
    }

    companion object {
        /** The model name an answer opened from the saved list carries, so the sheet knows not to offer a rerun. */
        const val SAVED = "kayıt"
        /** Shared with the video studio, which writes its captions with the same key and model. */
        internal const val PREFS = "console_ai"
        internal const val KEY = "openrouter_key"
        internal const val MODEL = "or_model"
        private const val SEARCH = "or_web_search"
        private const val HISTORY = "history"

        private val SYSTEM = """
            Sen Karalak'ın kıdemli büyüme pazarlamacısı ve oyun yapımcısısın. Mobil oyunları sıfır bütçeyle viral büyütmüş, Türkiye pazarını ve gençlerin internet kültürünü (TikTok, Instagram, Ekşi Sözlük, Reddit, Discord, okul ortamı) çok iyi bilen birisin.

            ## Oyun
            Karalak: Android (Google Play) ve web (karalak.com, kurulumsuz, iPhone'da da çalışır) çizim + hafıza oyunu. Oyuncu kısa sürede 10 kelime çizer; sonra kendi çizimlerine bakıp hangi kelime olduğunu hatırlayıp yazar. Komedi buradan doğar: kendi çizimini tanıyamamak.
            Modlar: Hızlı Eşleş (aynı kelimeler, rakiple puan yarışı), Arkadaşlarınla Yarış (6 haneli oda kodu, 8 kişiye kadar, takım modu), Günlük Meydan Okuma (herkese aynı 5 kelime, seri), Bölümler (dünya haritası), Çevrimdışı, Düellolar.
            Sistemler: jokerler (ilk harf, harf sayısı, ekstra süre), altın, mağaza (kalemler, çerçeveler), kasalar, rünler (kalıcı bonuslar), aylık global ve arkadaş ligi, başarımlar, giriş serisi.
            Gelir: AdMob ödüllü ve geçiş reklamları (uygulama içi satın alma yok). Ekip: tek geliştirici. Bütçe: neredeyse sıfır. Sosyal medya: @karalakapp (Instagram, TikTok).

            ## Yazım kuralları
            - Türkçe yaz; samimi ama profesyonel. Markdown kullan: ## başlık, - madde, **kalın**.
            - Amatör, genel geçer tavsiye verme. Her öneri somut, uygulanabilir, ölçülebilir olsun; örnek metinleri hazır yaz.
            - Verilen canlı verilere dayan; veri azsa bunu söyle ve küçük ölçekte gerçekçi hedef koy.
            - Web araması kullanabiliyorsan güncel trendleri, Türkiye takvimini ve rakipleri kontrol et; uydurma istatistik verme.
            - Sahte yorum, bot takipçi, spam, telif ihlali, yanıltıcı reklam ya da platform kurallarını çiğneyen taktik önerme.
        """.trimIndent()
    }
}
