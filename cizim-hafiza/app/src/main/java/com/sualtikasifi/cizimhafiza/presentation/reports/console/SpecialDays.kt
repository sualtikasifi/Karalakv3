package com.sualtikasifi.cizimhafiza.presentation.reports.console

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** A date worth building an in-game event or a post around. */
data class SpecialDay(
    val date: LocalDate,
    val name: String,
    /** One line on why it suits a drawing-and-memory game — given to the assistant as a starting angle. */
    val angle: String,
    /** Lunar holidays are announced officially; the date here is the expected one. */
    val estimated: Boolean = false
) {
    fun daysLeft(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, date)
}

/**
 * Turkish and global days that fit Karalak, for the next [horizonDays] days. Fixed dates are
 * computed; the two religious holidays use the expected dates for 2026-2028 (they move ~11 days a
 * year and are confirmed by Diyanet), marked as estimated.
 */
object SpecialDays {
    private val lunar = listOf(
        LocalDate.of(2026, 3, 20) to "Ramazan Bayramı",
        LocalDate.of(2026, 5, 27) to "Kurban Bayramı",
        LocalDate.of(2027, 3, 9) to "Ramazan Bayramı",
        LocalDate.of(2027, 5, 16) to "Kurban Bayramı",
        LocalDate.of(2028, 2, 26) to "Ramazan Bayramı",
        LocalDate.of(2028, 5, 5) to "Kurban Bayramı"
    )

    private fun forYear(y: Int): List<SpecialDay> = listOf(
        SpecialDay(LocalDate.of(y, 1, 1), "Yılbaşı", "Yeni yıl dilekleri, 'yılın çizimi' yarışması, kar/kutlama kelime paketi"),
        SpecialDay(LocalDate.of(y, 1, 20).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.FRIDAY)), "Yarıyıl tatili başlıyor", "Okullar tatilde: öğrenciler evde, arkadaşlarla oda kurma kampanyası"),
        SpecialDay(LocalDate.of(y, 2, 14), "Sevgililer Günü", "Çiftler/en yakın arkadaş düellosu, kalp-aşk kelime paketi, 'birbirinizi ne kadar tanıyorsunuz' çizimi"),
        SpecialDay(LocalDate.of(y, 4, 15), "Dünya Sanat Günü", "Ünlü tabloları 30 saniyede çiz, sanatçı kelime paketi"),
        SpecialDay(LocalDate.of(y, 4, 23), "23 Nisan Ulusal Egemenlik ve Çocuk Bayramı", "Çocuk dostu kelime paketi, aile oyunu, okullar için paylaşılabilir içerik"),
        SpecialDay(LocalDate.of(y, 5, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY)), "Anneler Günü", "Annene çiz: en komik anne çizimleri, paylaşılabilir kart"),
        SpecialDay(LocalDate.of(y, 5, 19), "19 Mayıs Gençlik ve Spor Bayramı", "Spor kelime paketi, gençlere yönelik turnuva"),
        SpecialDay(LocalDate.of(y, 6, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.SUNDAY)), "Babalar Günü", "Babanla düello, baba esprileri kelime paketi"),
        SpecialDay(LocalDate.of(y, 6, 20).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)), "Yaz tatili başlıyor", "Uzun yaz: günlük meydan okuma serisi, yaz kelime paketi"),
        SpecialDay(LocalDate.of(y, 7, 17), "Dünya Emoji Günü", "Emojiyi çiz-bil: emoji kelime paketi, kısa video formatı"),
        SpecialDay(LocalDate.of(y, 8, 8), "Dünya Kedi Günü", "En kötü kedi çizimi yarışması: kedi sahipleri topluluğu"),
        SpecialDay(LocalDate.of(y, 8, 26), "Dünya Köpek Günü", "Maskot köpekle özel etkinlik, köpek kelime paketi"),
        SpecialDay(LocalDate.of(y, 9, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY)), "Okullar açılıyor", "Teneffüs oyunu konumlandırması, sınıf arkadaşlarıyla oda kur"),
        SpecialDay(LocalDate.of(y, 10, 1), "Inktober başlıyor (Ekim çizim ayı)", "Dünyaca ünlü 31 günlük çizim akımı: günlük tema kelimeleri, #inktober etiketi"),
        SpecialDay(LocalDate.of(y, 10, 29), "29 Ekim Cumhuriyet Bayramı", "Cumhuriyet temalı kelime paketi, kutlama gönderisi"),
        SpecialDay(LocalDate.of(y, 10, 31), "Cadılar Bayramı (Halloween)", "Korkunç kelime paketi, kostüm çerçevesi, karanlık tema, 'en korkunç çizim' yarışması"),
        SpecialDay(LocalDate.of(y, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY)), "Kasım ara tatili", "Bir hafta okul yok: arkadaşlarla yarış haftası"),
        SpecialDay(LocalDate.of(y, 11, 24), "Öğretmenler Günü", "Öğretmenini çiz, sınıfta oynanacak oda kodu kampanyası"),
        SpecialDay(LocalDate.of(y, 12, 31), "Yılbaşı gecesi", "Yılın en çok çizilen kelimeleri, yıl özeti paylaşımı")
    ) + lunar.filter { it.first.year == y }.map { (d, n) -> SpecialDay(d, n, "Bayram tatili: aile oyunu, büyüklerle düello, bayram kelime paketi", estimated = true) }

    fun upcoming(today: LocalDate = LocalDate.now(), horizonDays: Long = 120): List<SpecialDay> =
        (forYear(today.year) + forYear(today.year + 1))
            .filter { it.daysLeft(today) in 0..horizonDays }
            .sortedBy { it.date }
}
