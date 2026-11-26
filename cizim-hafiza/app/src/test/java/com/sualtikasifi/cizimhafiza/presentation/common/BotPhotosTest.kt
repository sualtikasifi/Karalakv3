package com.sualtikasifi.cizimhafiza.presentation.common

import com.sualtikasifi.cizimhafiza.domain.model.AvatarGallery
import com.sualtikasifi.cizimhafiza.domain.model.LeagueBots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun anyNameGetsAGalleryPictureAndAlwaysTheSameOne() {
        val gallery = AvatarGallery.entries.map { it.res }.toSet()
        listOf("some_real_player", "", "Ünal_99", "x").forEach {
            val res = BotPhotos.drawableFor(it)
            assertTrue("$it", res in gallery)
            assertEquals(res, BotPhotos.drawableFor(it))
        }
        // case does not matter: the opponents' names are the league's names with a capital letter
        assertEquals(BotPhotos.drawableFor("memetcan"), BotPhotos.drawableFor("Memetcan"))
    }

    @Test
    fun everyQuickMatchOpponentNameHasItsOwnPickedPicture() {
        // the opponents (GhostPersonas, shared with the web as ghost_names.json) are not left to the hash fallback
        val file = java.io.File("../hosting/oyna/ghost_names.json").takeIf { it.exists() } ?: return
        val names = Regex(""""([^"]+)"""").findAll(file.readText()).map { it.groupValues[1] }.filter { it !in setOf("tr", "en", "pt", "es") }.toList()
        assertTrue("only ${names.size} names read", names.size > 500)
        val known = names.filter { BotPhotos.isPicked(it) }
        assertEquals("names without a picked picture: ${names.filter { !BotPhotos.isPicked(it) }.take(10)}", names.size, known.size)
    }

    @Test
    fun aNameThatSaysSomethingGetsTheMatchingPictureInEveryLanguage() {
        assertEquals(AvatarGallery.find("wolf")?.res, BotPhotos.drawableFor("Lobo_solitario"))
        assertEquals(AvatarGallery.find("owl")?.res, BotPhotos.drawableFor("Professor_Rui"))
        assertEquals(AvatarGallery.find("koala")?.res, BotPhotos.drawableFor("uykucu"))
        assertEquals(AvatarGallery.find("lion")?.res, BotPhotos.drawableFor("Ronaldo7"))
    }

    @Test
    fun theWholeGalleryIsUsedSoNoPicturePilesUp() {
        val used = LeagueBots.NAMES.mapNotNull { BotPhotos.drawableFor(it) }
        val perPicture = used.groupingBy { it }.eachCount()
        assertEquals("every gallery picture is worn by some bot", AvatarGallery.entries.size, perPicture.size)
        assertTrue("one picture is worn by too many bots: ${perPicture.values.max()}", perPicture.values.max() <= 18)
    }

    @Test
    fun sudeInTheBotRoomWearsAGalleryPicture() {
        // her picture is a gallery avatar written the way other players' are published, so every client can draw it
        val entry = AvatarGallery.find("cat_artist")
        assertNotNull(entry)
        assertEquals(AvatarGallery.published(entry!!), com.sualtikasifi.cizimhafiza.data.bot.BotRoomEngine.BOT_AVATAR)
        assertEquals(entry, AvatarGallery.entryOfPublished(com.sualtikasifi.cizimhafiza.data.bot.BotRoomEngine.BOT_AVATAR))
    }
}
