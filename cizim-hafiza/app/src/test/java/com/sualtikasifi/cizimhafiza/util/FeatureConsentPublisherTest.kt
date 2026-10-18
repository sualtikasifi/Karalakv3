package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureConsentPublisherTest {

    @Test
    fun aYesIsWrittenOncePerAccount() {
        assertTrue(FeatureConsentPublisher.needsWrite("a", true, null))
        assertFalse(FeatureConsentPublisher.needsWrite("a", true, "a|true"))
        // a new account needs its own yes
        assertTrue(FeatureConsentPublisher.needsWrite("b", true, "a|true"))
    }

    @Test
    fun aNoIsWrittenOnlyToTakeBackAYes() {
        // never said yes: no field already means no, so nothing is written
        assertFalse(FeatureConsentPublisher.needsWrite("a", false, null))
        assertFalse(FeatureConsentPublisher.needsWrite("b", false, "a|true"))
        assertTrue(FeatureConsentPublisher.needsWrite("a", false, "a|true"))
        assertFalse(FeatureConsentPublisher.needsWrite("a", false, "a|false"))
    }
}
