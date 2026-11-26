package com.sualtikasifi.cizimhafiza.domain.model

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * The filler players of the global monthly league, worked out on the phone.
 *
 * They used to be published by the scheduled job together with the real players, which tied what a player
 * saw to how often (and by which version of the code) that job ran: late runs froze the bots, and a second,
 * older writer swapped the table back to old names and old scores. Here a bot's name, level and score are a
 * pure function of the month and the clock — the same on every phone, always current, nothing to publish.
 *
 * Names and the clock-driven growth follow functions/src/index.ts (botNicknamesForPeriod / botPeriodXp), which is
 * still what an older app version reads: the league scores are as high as ever (a bot earns 3,000–5,500 XP a day), so
 * there is something to chase. The levels are spread out here (see [startXp] and [shownLevel]): every bot used to start
 * between levels 25 and 55, and the month's league XP was added in full, so by mid-month they all sat at the cap.
 */
object LeagueBots {
    const val COUNT = 21

    private const val GROWTH_INTERVAL_MS = 50L * 60 * 1000
    private const val GROWTH_MIN = 60
    private const val GROWTH_MAX = 260
    // The pace is what it always was (0.65–1.2: about 3,000–5,500 XP a day); half of it follows the level, so higher levels tend to lead.
    private const val PACE_MIN = 0.65
    private const val PACE_MAX = 1.2
    private const val PACE_FROM_LEVEL = 0.5
    // The level a bot shows moves only with a fifth of its league XP, so the table keeps rookies and veterans all month.
    private const val LEVEL_FROM_LEAGUE = 0.2
    private const val MIN_START_LEVEL = 8
    private const val MAX_START_LEVEL = 60
    private const val MAX_SHOWN_LEVEL = 70
    private const val MAX_LEVEL = 100
    private val ISTANBUL = ZoneId.of("Europe/Istanbul")

    /** The same 32-bit generator the server uses, so a seed means the same sequence on both sides. */
    private class Seeded(seed: Long) {
        private var a: Int = seed.toInt()
        fun next(): Double {
            a += 0x6d2b79f5
            var t = (a xor (a ushr 15)) * (1 or a)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL) / 4294967296.0
        }
    }

    fun nicknames(periodId: Long, count: Int = COUNT): List<String> {
        val pool = NAMES.toMutableList()
        val random = Seeded(periodId)
        for (i in pool.size - 1 downTo 1) {
            val j = floor(random.next() * (i + 1)).toInt()
            val tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp
        }
        return pool.take(count)
    }

    private fun totalXpForLevel(level: Int): Int {
        val n = maxOf(level - 1, 0)
        return 25 * n * n + 75 * n
    }

    private fun levelForXp(xp: Int): Int {
        if (xp <= 0) return 1
        val n = floor((-75 + sqrt(75.0 * 75.0 + 100.0 * xp)) / 50).toInt()
        return minOf(MAX_LEVEL, maxOf(1, n + 1))
    }

    /** The two draws that make bot [index]'s starting point: its level, and how far into that level it stands. */
    private fun startDraws(periodId: Long, index: Int): Pair<Double, Double> {
        val rnd = Seeded(periodId * 7_919 + index * 104_729L + 17)
        return rnd.next() to rnd.next()
    }

    /** Lifetime XP at the start of the month: the level is spread evenly over 8..60, so the table shows every rank from rookies to veterans. */
    private fun startXp(periodId: Long, index: Int): Int {
        val (u1, u2) = startDraws(periodId, index)
        val level = MIN_START_LEVEL + floor(u1 * (MAX_START_LEVEL - MIN_START_LEVEL + 1)).toInt()
        val span = totalXpForLevel(level + 1) - totalXpForLevel(level)
        return totalXpForLevel(level) + floor(u2 * span).toInt()
    }

    /** How fast bot [index] earns XP: half of its own luck, half of its starting level, so the high levels tend to be up the table. */
    private fun pace(periodId: Long, index: Int): Double {
        val own = Seeded(periodId * 131 + index * 977L + 5).next()
        val x = (1 - PACE_FROM_LEVEL) * own + PACE_FROM_LEVEL * startDraws(periodId, index).first
        return PACE_MIN + (PACE_MAX - PACE_MIN) * x
    }

    /** The level a bot wears: its starting XP plus a fifth of this month's league XP, up to the shown maximum. */
    private fun shownLevel(periodId: Long, index: Int, periodXp: Int): Int =
        minOf(MAX_SHOWN_LEVEL, levelForXp(startXp(periodId, index) + (periodXp * LEVEL_FROM_LEAGUE).toInt()))

    /** What bot [index] has earned by [nowMillis], counting growth ticks since [monthStartMillis]. */
    fun periodXp(periodId: Long, index: Int, nowMillis: Long, monthStartMillis: Long): Int {
        val pace = pace(periodId, index)
        var xp = 0
        var tick = monthStartMillis / GROWTH_INTERVAL_MS + 1
        val last = nowMillis / GROWTH_INTERVAL_MS
        while (tick <= last) {
            val draw = Seeded(tick * 104_729L + periodId * 97 + index).next()
            xp += floor((GROWTH_MIN + draw * (GROWTH_MAX - GROWTH_MIN + 1)) * pace).toInt()
            tick++
        }
        return xp
    }

    /** Midnight at the start of [today]'s month in Istanbul — the league's own clock. */
    fun monthStartMillis(today: LocalDate): Long =
        today.withDayOfMonth(1).atStartOfDay(ISTANBUL).toInstant().toEpochMilli()

    class Bot(val nickname: String, val periodXp: Int, val level: Int)

    fun bots(periodId: Long, nowMillis: Long, monthStartMillis: Long): List<Bot> =
        nicknames(periodId).mapIndexed { index, name ->
            val xp = periodXp(periodId, index, nowMillis, monthStartMillis)
            Bot(name, xp, shownLevel(periodId, index, xp))
        }

    val NAMES = listOf(
        "memetcan", "ahmet734", "fthylmz", "uykuluadam", "kraduman", "fistikezmesi",
        "kadir007", "ceyda8821", "cananabaci", "yussuf", "deliomer", "ruzgargibi",
        "brkydmr", "burakreis", "gozluklucocuk", "mustfcn", "sagocu99", "yalnizkurt",
        "ahmmet", "asabiadam", "simitcay", "kaptanali", "karabela", "gecebekcisi",
        "demirhan", "hknkrks", "yorgunsavasci", "zynpcetn", "karakoc", "aysenur11",
        "alican1903", "siyahinci", "ssknr", "mertcn", "iremsu", "bsgul",
        "aleyna34", "gorkem543", "cnsyksl", "bthnky", "yusufinho", "polatalmdr",
        "minikkus", "gamsizbaykus", "mimarmerve", "muhendisbey", "soforkemal", "issizgucsuz",
        "mezunadam", "caykolik", "kemalkaya", "gizemlikiz", "kafkef88", "poyrazkarayel",
        "ucanbalik", "isimsizkahraman", "kacakyolcu", "delidolu", "yalnizim", "firtinakemal",
        "gocebe", "krmzblt", "karadenizli", "vethasan", "volkan00", "keloglan",
        "gulyabani", "tosuncuk", "karaeylem", "ogretmenim", "hemsiremelisa", "avukatbey",
        "ogrenciyiz", "tekbasina", "krdsler", "sariyildiz", "merve742", "farukeczanesi",
        "cemal33", "komsukizi", "bakkalamca", "uykucu", "sessizkalan", "gokhantepe",
        "ahemt98", "yanlizadam", "herkezgitsin", "orjinall", "suprizci", "yalnizdegil",
        "mnyk", "fth123", "qweasd", "tofask", "passatci", "hondacivic",
        "cbf150", "broadwayci", "doganslx", "izmir35", "bursa1616", "kordonboyu",
        "kemalpasali", "mudanyali", "adana01", "cikkofteci", "caykasigi", "sekersiz",
        "bolacili", "sarmisakli", "uykumvar", "nebilimben", "bosver", "falanfilan",
        "ivirzivir", "baksanabana", "belkide", "veterinerbey", "yirmi8", "hekimsami",
        "98tayfa", "mormadenci", "ustaeller", "kafkef", "pesimist", "cimbom1905",
        "fenerli1907", "bjk1903", "ronaldo7", "ts61", "messi10", "spinci",
        "lufersesi", "amatorbalikci", "sahteyem", "yagmurlu", "lodos", "ametist",
        "hsncn", "brk98", "glsh", "mstyfa", "ahmet8520", "cufcuf",
        "wqewqe", "bumbum", "laylaylom", "laylon", "soley", "hicbiri",
        "sonsoz", "oburki", "isimsiz", "siyahgiyen", "heryeryesil", "kdr",
        "gokhn", "voldemort", "padisah", "vezir", "kayiboyu", "ineksaban",
    )
}
