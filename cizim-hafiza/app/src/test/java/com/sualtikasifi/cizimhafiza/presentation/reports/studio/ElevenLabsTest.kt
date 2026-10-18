package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceModel
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceSpec
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevenLabsTest {

    @Test
    fun v3SaysTheLineWithItsMoodOthersSayItPlain() {
        val line = SpokenLine(" Bu kedi mi, patates mi? ", MascotEmotion.Laughing)
        assertEquals("[laughs] Bu kedi mi, patates mi?", ElevenLabs.textFor(line, VoiceModel.V3, tags = true))
        assertEquals("Bu kedi mi, patates mi?", ElevenLabs.textFor(line, VoiceModel.V3, tags = false))
        assertEquals("Bu kedi mi, patates mi?", ElevenLabs.textFor(line, VoiceModel.V2, tags = true))
        // a calm line carries no tag at all
        assertEquals("Merhaba", ElevenLabs.textFor(SpokenLine("Merhaba", MascotEmotion.Neutral), VoiceModel.V3, tags = true))
    }

    @Test
    fun everyMoodButCalmHasADelivery() {
        MascotEmotion.entries.filter { it != MascotEmotion.Neutral }.forEach { e ->
            val tag = ElevenLabs.tagFor(e)
            assertTrue("$e", tag != null && tag.startsWith("[") && tag.endsWith("]"))
        }
    }

    @Test
    fun v3StabilitySnapsToItsThreeSettings() {
        assertEquals(0.0, ElevenLabs.stabilityFor(VoiceModel.V3, 0.2), 0.0)
        assertEquals(0.5, ElevenLabs.stabilityFor(VoiceModel.V3, 0.4), 0.0)
        assertEquals(1.0, ElevenLabs.stabilityFor(VoiceModel.V3, 0.9), 0.0)
        assertEquals(0.35, ElevenLabs.stabilityFor(VoiceModel.V2, 0.35), 1e-9)
    }

    @Test
    fun aLineIsPaidForOnceButAnyChangeAsksAgain() {
        val a = ElevenLabs.cacheName("Merhaba", "v1", VoiceModel.V3, 0.5)
        assertEquals(a, ElevenLabs.cacheName("Merhaba", "v1", VoiceModel.V3, 0.5))
        assertNotEquals(a, ElevenLabs.cacheName("Merhaba!", "v1", VoiceModel.V3, 0.5))
        assertNotEquals(a, ElevenLabs.cacheName("Merhaba", "v2", VoiceModel.V3, 0.5))
        assertNotEquals(a, ElevenLabs.cacheName("Merhaba", "v1", VoiceModel.V2, 0.5))
        assertNotEquals(a, ElevenLabs.cacheName("Merhaba", "v1", VoiceModel.V3, 1.0))
        assertTrue(a.endsWith(".mp3"))
    }

    @Test
    fun errorsSayWhatToDo() {
        val quota = ElevenLabs.errorMessage(401, """{"detail":{"status":"quota_exceeded","message":"This request exceeds your quota."}}""")
        assertTrue(quota.contains("kredisi bitti"))
        val perm = ElevenLabs.errorMessage(401, """{"detail":{"status":"missing_permissions","message":"missing voices_read"}}""")
        assertTrue(perm.contains("izni yok") && perm.contains("voices_read"))
        assertTrue(ElevenLabs.errorMessage(401, """{"detail":"Invalid API key"}""").contains("geçersiz"))
        assertTrue(ElevenLabs.errorMessage(500, "oops").contains("oops"))
    }

    @Test
    fun modelsAreFoundByTheirIds() {
        assertEquals(VoiceModel.V2, VoiceModel.of("eleven_multilingual_v2"))
        assertEquals(VoiceModel.V3, VoiceModel.of(null))
        assertEquals(VoiceModel.V3, VoiceModel.of("unknown"))
    }

    @Test
    fun theCostShownIsWhatIsSent() {
        val line = SpokenLine("Bu kedi mi?", MascotEmotion.Laughing)  // "[laughs] Bu kedi mi?" = 20 characters
        assertEquals(20, VoiceSpec("v", VoiceModel.V3, 0.5, tags = true).credits(line))
        assertEquals(11, VoiceSpec("v", VoiceModel.V2, 0.5, tags = true).credits(line))
        assertEquals(6, VoiceSpec("v", VoiceModel.Flash, 0.5, tags = true).credits(line))
    }
}
