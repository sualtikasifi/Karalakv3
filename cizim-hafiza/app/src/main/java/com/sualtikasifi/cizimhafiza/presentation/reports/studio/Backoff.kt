package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tries a call again when the service said "too many requests" (429), was briefly down (5xx) or the network blinked,
 * waiting longer each time (or as long as the service asked). A bad key, an empty balance or a refused request are not
 * retried: another go would only be refused again, and a paid call must never be repeated on a guess.
 */
object Backoff {
    const val ATTEMPTS = 3
    private const val BASE_MS = 1_500L
    private const val MAX_WAIT_MS = 20_000L

    /** A failure that carries the HTTP status it came with, and how long the service asked to wait, if it did. */
    interface Coded {
        val code: Int
        val retryAfterMs: Long? get() = null
    }

    fun retryable(e: Throwable): Boolean = when (e) {
        is CancellationException -> false
        is Coded -> e.code == 429 || e.code in 500..599
        is java.io.IOException -> true
        else -> false
    }

    /** How long to wait before attempt number [attempt] (1 is the first retry). */
    fun waitMs(attempt: Int, e: Throwable): Long {
        val asked = (e as? Coded)?.retryAfterMs
        val own = BASE_MS shl (attempt - 1).coerceIn(0, 4)
        return (asked ?: own).coerceIn(250L, MAX_WAIT_MS)
    }

    /** [block] up to [attempts] times; the last result stands. [onWait] says what is being waited for. */
    suspend fun <T> retrying(attempts: Int = ATTEMPTS, onWait: (attempt: Int, waitMs: Long) -> Unit = { _, _ -> }, block: suspend () -> Result<T>): Result<T> {
        var last: Result<T> = block()
        var attempt = 1
        while (attempt < attempts) {
            val e = last.exceptionOrNull() ?: return last
            if (!retryable(e)) return last
            val wait = waitMs(attempt, e)
            onWait(attempt, wait)
            delay(wait)
            attempt++
            last = block()
        }
        return last
    }
}
