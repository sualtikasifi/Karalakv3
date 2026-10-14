package com.sualtikasifi.cizimhafiza.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * A failure the game survives but someone should hear about: logged, and sent to Crashlytics as a non-fatal so a
 * broken backup, sign-in or listener shows up in the console instead of vanishing.
 *
 * The everyday ones are left out — no connection, a timeout, a cancelled coroutine — they are not bugs, and reporting
 * every offline moment would bury the real ones.
 */
object NonFatal {

    fun report(tag: String, message: String, error: Throwable) {
        Log.w(tag, message, error)
        if (isExpected(error)) return
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                log("$tag: $message")
                recordException(error)
            }
        }
    }

    private fun isExpected(error: Throwable): Boolean = when (error) {
        is CancellationException, is IOException -> true
        is FirebaseFirestoreException -> error.code in setOf(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.CANCELLED,
            FirebaseFirestoreException.Code.ABORTED
        )
        else -> error.cause?.let { it !== error && isExpected(it) } ?: false
    }
}
