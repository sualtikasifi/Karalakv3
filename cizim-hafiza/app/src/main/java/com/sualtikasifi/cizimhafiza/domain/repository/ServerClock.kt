package com.sualtikasifi.cizimhafiza.domain.repository

/** The server's idea of "today", so a phone whose clock was moved cannot collect a day's reward early. */
interface ServerClock {
    /** Today's epoch day in Türkiye according to the server; null when it cannot be reached (offline). */
    suspend fun todayEpochDay(): Long?
}
