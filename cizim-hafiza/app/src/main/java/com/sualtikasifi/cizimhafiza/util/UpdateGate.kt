package com.sualtikasifi.cizimhafiza.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The forced-update switch. The `minVersionCode` field of the `config/economy` document (set from the developer panel's
 * "Güncelleme" tool) is the lowest app version code that may still play: an install below it gets the full-screen
 * update card and cannot reach the game until it is updated. 0, a missing field or a missing document means no lock.
 *
 * It is fail-open on purpose: with no internet, or when the document cannot be read, nothing is locked, and an install
 * that has never seen the field is never blocked. The developer's own account is let through (see [blocks]) so a wrong
 * number can never lock the panel that fixes it.
 */
object UpdateGate {
    private val _minVersionCode = MutableStateFlow(0)
    val minVersionCode: StateFlow<Int> = _minVersionCode.asStateFlow()

    /** Takes the `config/economy` document (null when it does not exist). */
    fun apply(data: Map<String, Any?>?) {
        _minVersionCode.value = ((data?.get("minVersionCode") as? Number)?.toInt() ?: 0).coerceAtLeast(0)
    }

    /** True when an install of [currentVersionCode] must update before playing. The reviewer account is never blocked. */
    fun blocks(currentVersionCode: Int, minVersionCode: Int, isReviewer: Boolean = false): Boolean =
        minVersionCode > currentVersionCode && !isReviewer
}
