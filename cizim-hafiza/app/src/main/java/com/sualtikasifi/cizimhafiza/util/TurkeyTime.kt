package com.sualtikasifi.cizimhafiza.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The one clock every "new day" in the game runs on: Türkiye time (Europe/Istanbul, UTC+3 all year).
 * The daily challenge, the free chest, the daily joker and the league all roll over at 00:00 here,
 * whatever the phone's own time zone says — so the daily words are the same round for every player
 * and everyone's countdown ends at the same moment.
 */
object TurkeyTime {
    val zone: ZoneId = ZoneId.of("Europe/Istanbul")

    fun today(): LocalDate = LocalDate.now(zone)

    fun now(): LocalDateTime = LocalDateTime.now(zone)

    /** Wall-clock millis of the next 00:00 in Türkiye. */
    fun nextMidnightMillis(): Long =
        ZonedDateTime.now(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}
