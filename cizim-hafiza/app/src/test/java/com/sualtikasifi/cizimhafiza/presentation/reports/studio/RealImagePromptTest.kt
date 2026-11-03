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

    @Test
    fun `the picture prompt says the real size and what shows it`() {
        val prompt = RealCopy.imagePrompt("şırınga", 5, syringe.copy(size = "yaklaşık 15 cm, avuca sığar", scaleHint = "yanında bir tükenmez kalem"))
        assertTrue(prompt, prompt.contains("gerçek boyutu yaklaşık 15 cm, avuca sığar"))
        assertTrue(prompt, prompt.contains("yanında bir tükenmez kalem"))
        assertFalse("no size given, no size line", RealCopy.imagePrompt("şırınga", 5, syringe).contains("Ölçü:"))
    }

    @Test
    fun `the video prompt keeps one shot, one place, the real size and a rigid thing`() {
        val video = RealCopy.videoPrompt(
            "şırınga",
            syringe.copy(size = "yaklaşık 15 cm", fixed = "sert plastik ve metal: bükülmez, esnemez", motion = "0–2 sn: hemşire girer.", scene = "çelik bir masa")
        )
        // the room cannot change, nobody comes through a door
        assertTrue(video, video.contains("TEK ÇEKİM"))
        assertTrue(video, video.contains("Kesme, sahne değişimi, yeni oda, kapı açılması ya da yeni bir kişinin girişi yok"))
        assertTrue(video, video.contains("en fazla bir kişi"))
        // the size does not grow
        assertTrue(video, video.contains("Gerçek boyutu yaklaşık 15 cm"))
        // a rigid needle is not bent: only the person moves it
        assertTrue(video, video.contains("sert plastik ve metal: bükülmez, esnemez"))
        assertTrue(video, video.contains("bükülmez, yaylanmaz"))
        assertTrue(video, video.contains("Olay: 0–2 sn: hemşire girer."))
        assertTrue(video, video.contains("çelik bir masa"))
    }

    @Test
    fun `the scene ideas are asked for with the same rules and read back`() {
        val prompt = RealCopy.pitchPrompt("şırınga", syringe.copy(size = "yaklaşık 15 cm"))
        assertTrue(prompt, prompt.contains("6 farklı espri fikri"))
        assertTrue(prompt, prompt.contains("en iyi 3'ünü"))
        // the six one-line jokes are written down first, so the choosing really happens
        assertTrue(prompt, prompt.contains("\"candidates\""))
        assertTrue(prompt, prompt.contains("Tek çekim, tek mekân"))
        assertTrue(prompt, prompt.contains("bükülmez, yaylanmaz, sallanmaz"))
        assertTrue(prompt, prompt.contains("Gerçek boyutu: yaklaşık 15 cm"))
        val ideas = RealCopy.parsePitches("""{"ideas":[{"joke":"j1","scene":"s1"},{"joke":"j2","scene":"s2"},{"joke":"","scene":""},{"joke":"j4","scene":"s4"},{"joke":"j5","scene":"s5"}]}""")
        assertEquals(3, ideas!!.size)
        assertEquals("j1", ideas[0].joke)
        assertEquals("s4", ideas[2].scene)
        assertEquals(null, RealCopy.parsePitches("bilmiyorum"))
    }

    @Test
    fun `the picture prompt keeps the drawn body and does not print the strokes on the surface`() {
        val creature = RealCopy.imagePrompt("at", 5, RealReveal("a", kind = "canlı"))
        assertTrue(creature, creature.contains("Vücudunu gerçek hayvana benzetip düzeltme"))
        assertTrue(creature, creature.contains("kalın çizgileri yüzeye basılmış çizgi olarak kalmasın"))
        val thing = RealCopy.imagePrompt("top", 5, RealReveal("t", kind = "nesne"))
        assertFalse("an object is not told about animal bodies", thing.contains("Vücudunu gerçek hayvana"))
        assertTrue(thing, thing.contains("düz bir disk, çıkartma ya da boyama gibi görünmesin"))
    }

    @Test
    fun `a creature's video has no person and keeps its drawn body, an object's keeps its odd look`() {
        val horse = RealCopy.videoPrompt("at", RealReveal("a", kind = "canlı"))
        assertTrue(horse, horse.contains("Kadrajda insan yok"))
        assertTrue(horse, horse.contains("yerine sıradan bir hayvan koyma"))
        assertFalse(horse, horse.contains("en fazla bir kişi"))
        val ball = RealCopy.videoPrompt("top", RealReveal("t", kind = "nesne"))
        assertTrue(ball, ball.contains("yerine sıradan, normal bir \"top\" KOYMA"))
        val pitch = RealCopy.pitchPrompt("at", RealReveal("a", kind = "canlı"))
        assertTrue(pitch, pitch.contains("Kadrajda insan yok"))
        assertTrue(pitch, pitch.contains("tuhaf özellik videonun her anında"))
    }

    @Test
    fun `an object's video keeps its odd shape, the camera, and asks nothing a normal one would do`() {
        val video = RealCopy.videoPrompt("simit", RealReveal("s", kind = "nesne", describe = "iki hilal gibi kıvrık parça"))
        assertTrue(video, video.contains("Tuhaf şekli: iki hilal gibi kıvrık parça"))
        assertTrue(video, video.contains("yalnızca elleri ile kolları görünür"))
        assertTrue(video, video.contains("Nesne yenmez, ısırılmaz"))
        assertTrue(video, video.contains("nesne yerinde SABİT durur"))
        assertTrue(video, video.contains("üstünü örtmez"))
        assertTrue(video, video.contains("yakınlaşma, kayma ve kişiye dönme yok"))
        val pitch = RealCopy.pitchPrompt("simit", RealReveal("s", kind = "nesne"))
        assertTrue(pitch, pitch.contains("(yemek, içmek, giymek, açmak, kesmek) YOK"))
    }
}
