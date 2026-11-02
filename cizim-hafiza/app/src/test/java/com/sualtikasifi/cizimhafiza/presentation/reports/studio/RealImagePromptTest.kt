package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The picture prompt: what a thing IS decides what it may have (an object has no eyes), and the real parts come from the assistant. */
class RealImagePromptTest {

    private val syringe = RealReveal(
        sampleId = "s",
        kind = "nesne",
        describe = "uzun dikey dikdörtgen gövde, üstünden çıkan ince çizgi, altta T",
        parts = "uzun dikey dikdörtgen gövde → şeffaf plastik silindir\n- üstten çıkan ince çizgi → pistonun kolu",
        material = "şeffaf plastik, siyah kauçuk, ince metal uç",
        scene = "bir muayene masasının paslanmaz çelik yüzeyi"
    )

    @Test
    fun `an object may not have eyes, a face or limbs`() {
        val prompt = RealCopy.imagePrompt("şırınga", 5, syringe)
        assertTrue(prompt, prompt.contains("NESNE"))
        assertTrue(prompt, prompt.contains("gözü, yüzü, ağzı, kolu, bacağı"))
        assertFalse("an object is not asked for fur or skin", prompt.contains("gerçek tüyü, derisi"))
    }

    @Test
    fun `a creature gets fur or skin but the drawn body`() {
        val prompt = RealCopy.imagePrompt("kedi", 5, RealReveal("k", kind = "canlı"))
        assertTrue(prompt, prompt.contains("CANLI"))
        assertTrue(prompt, prompt.contains("tüyü, derisi"))
        assertFalse(prompt, prompt.contains("NESNE"))
    }

    @Test
    fun `the drawn parts, the material and the scene go into the prompt`() {
        val prompt = RealCopy.imagePrompt("şırınga", 5, syringe)
        assertTrue(prompt, prompt.contains("- uzun dikey dikdörtgen gövde → şeffaf plastik silindir"))
        assertTrue(prompt, prompt.contains("- üstten çıkan ince çizgi → pistonun kolu"))
        assertTrue(prompt, prompt.contains("Malzeme: şeffaf plastik, siyah kauçuk, ince metal uç"))
        assertTrue(prompt, prompt.contains("Sahne: bir muayene masasının paslanmaz çelik yüzeyi"))
    }

    @Test
    fun `a photograph is asked for, not an illustration, and nothing frightening`() {
        val prompt = RealCopy.imagePrompt("şırınga", 5, syringe)
        assertTrue(prompt, prompt.contains("telefon fotoğrafı"))
        assertTrue(prompt, prompt.contains("İllüstrasyon, çizgi film, anime"))
        assertFalse("the old invitation to be scary made models invent creatures", prompt.contains("ürkütücü olması tam istediğimiz"))
    }

    @Test
    fun `without the assistant's notes the prompt still works and asks the model to decide what the thing is`() {
        val prompt = RealCopy.imagePrompt("nar", 7, null)
        assertTrue(prompt, prompt.contains("karar ver"))
        assertTrue(prompt, prompt.contains("\"nar\""))
        assertTrue(prompt, prompt.contains("7 saniyede"))
    }

    @Test
    fun `the assistant's answer carries the new fields, the parts as a list or as text`() {
        val json = """{"reveals":[
            {"index":1,"kind":"nesne","describe":"d","parts":["a → b","c → d"],"material":"m","scene":"s","motion":"x"},
            {"index":2,"kind":"canlı","describe":"d2","parts":"p → q","material":"m2","scene":"s2","motion":"x2"}
        ],"caption":"c","hashtags":"#h"}"""
        val picks = RealCopy.parsePicks(json, 2)
        assertNotNull(picks)
        assertEquals("a → b\nc → d", picks!!.reveals[0].parts)
        assertEquals("nesne", picks.reveals[0].kind)
        assertEquals("p → q", picks.reveals[1].parts)
        assertEquals("s2", picks.reveals[1].scene)
    }
}
