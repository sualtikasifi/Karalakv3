package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushTokenSyncTest {
    @Test fun nothingSavedYetMeansSave() = assertTrue(PushTokenSync.needsSaving(null, "u1", "t1"))

    @Test fun theSamePairIsNotSavedAgain() = assertFalse(PushTokenSync.needsSaving(PushTokenSync.savedKey("u1", "t1"), "u1", "t1"))

    @Test fun aNewTokenForTheSameAccountIsSaved() = assertTrue(PushTokenSync.needsSaving(PushTokenSync.savedKey("u1", "t1"), "u1", "t2"))

    @Test fun aNewAccountOnTheSameTokenIsSaved() = assertTrue(PushTokenSync.needsSaving(PushTokenSync.savedKey("u1", "t1"), "u2", "t1"))
}
