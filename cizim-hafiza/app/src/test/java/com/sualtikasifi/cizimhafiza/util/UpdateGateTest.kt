package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateGateTest {
    @Test
    fun anInstallBelowTheMinimumIsBlockedAndOneAtOrAboveIsNot() {
        assertTrue(UpdateGate.blocks(currentVersionCode = 98, minVersionCode = 99))
        assertFalse(UpdateGate.blocks(currentVersionCode = 99, minVersionCode = 99))
        assertFalse(UpdateGate.blocks(currentVersionCode = 120, minVersionCode = 99))
    }

    @Test
    fun noLockMeansNobodyIsBlocked() {
        assertFalse(UpdateGate.blocks(currentVersionCode = 1, minVersionCode = 0))
    }

    @Test
    fun theDevelopersOwnAccountIsNeverBlocked() {
        assertFalse(UpdateGate.blocks(currentVersionCode = 1, minVersionCode = 999, isReviewer = true))
    }

    @Test
    fun theMinimumIsReadFromTheDocumentAndAMissingOrWrongFieldMeansNoLock() {
        UpdateGate.apply(mapOf("minVersionCode" to 105L, "goldPerWord" to 5L))
        assertEquals(105, UpdateGate.minVersionCode.value)
        UpdateGate.apply(mapOf("minVersionCode" to 105.0))
        assertEquals(105, UpdateGate.minVersionCode.value)
        UpdateGate.apply(mapOf("minVersionCode" to "abc"))
        assertEquals(0, UpdateGate.minVersionCode.value)
        UpdateGate.apply(mapOf("minVersionCode" to -4L))
        assertEquals(0, UpdateGate.minVersionCode.value)
        UpdateGate.apply(null)
        assertEquals(0, UpdateGate.minVersionCode.value)
    }
}
