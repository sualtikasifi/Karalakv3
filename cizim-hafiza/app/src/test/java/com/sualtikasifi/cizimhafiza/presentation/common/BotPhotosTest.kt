package com.sualtikasifi.cizimhafiza.presentation.common

import com.sualtikasifi.cizimhafiza.domain.model.AvatarGallery
import com.sualtikasifi.cizimhafiza.domain.model.LeagueBots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotPhotosTest {

    @Test
    fun everyLeagueBotWearsAGalleryPicture() {
        val gallery = AvatarGallery.entries.map { it.res }.toSet()
        LeagueBots.NAMES.forEach { name ->
            val res = BotPhotos.drawableFor(name)
            assertNotNull("no picture for $name", res)
            assertTrue("$name does not wear a gallery picture", res in gallery)
        }
    }

    @Test
    fun aNameThatSaysSomethingGetsTheMatchingPicture() {
        assertEquals(AvatarGallery.find("wolf")?.res, BotPhotos.drawableFor("yalnizkurt"))
        assertEquals(AvatarGallery.find("koala")?.res, BotPhotos.drawableFor("uykuluadam"))
        assertEquals(AvatarGallery.find("owl")?.res, BotPhotos.drawableFor("ogretmenim"))
    }

    @Test
    fun aNameThatIsNotALeagueBotGetsNothing() {
        assertNull(BotPhotos.drawableFor("some_real_player"))
    }

    @Test
    fun theWholeGalleryIsUsedSoNoPicturePilesUp() {
        val used = LeagueBots.NAMES.mapNotNull { BotPhotos.drawableFor(it) }
        val perPicture = used.groupingBy { it }.eachCount()
        assertEquals("every gallery picture is worn by some bot", AvatarGallery.entries.size, perPicture.size)
        assertTrue("one picture is worn by too many bots: ${perPicture.values.max()}", perPicture.values.max() <= 18)
    }
}
