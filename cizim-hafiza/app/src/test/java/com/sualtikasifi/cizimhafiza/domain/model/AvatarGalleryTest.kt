package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarGalleryTest {

    @Test
    fun everyIdIsUniqueAndFitsTheRulesPattern() {
        val ids = AvatarGallery.entries.map { it.id }
        assertEquals("ids must be unique", ids.size, ids.toSet().size)
        // firestore.rules accepts 'avatar:[a-z0-9_]{1,32}'
        ids.forEach { assertTrue("$it does not fit the published pattern", Regex("[a-z0-9_]{1,32}").matches(it)) }
    }

    @Test
    fun theSettingPicksOnlyAKnownGalleryPicture() {
        val first = AvatarGallery.entries.first()
        assertEquals(first, AvatarGallery.entryOfSource(AvatarGallery.source(first.id)))
        // the mascot, nothing chosen, the retired Google choice and an id this build does not have all show the mascot
        listOf("DINO", "", "GOOGLE", "AVATAR:not_a_picture", "avatar:" + first.id, null).forEach {
            assertNull("source $it", AvatarGallery.entryOfSource(it))
        }
    }

    @Test
    fun aPublishedValueNamesAGalleryPictureOrNothing() {
        val any = AvatarGallery.entries.last()
        assertEquals(any, AvatarGallery.entryOfPublished(AvatarGallery.published(any)))
        assertEquals("avatar:" + any.id, AvatarGallery.published(any))
        listOf(
            "",
            null,
            "https://lh3.googleusercontent.com/a/abc=s96-c", // a retired Google address is never drawn
            "https://evil.example/pixel.png",
            "avatar:unknown",
            "avatar:",
            "AVATAR:" + any.id,
            "avatar:" + "a".repeat(200)
        ).forEach { assertNull("published $it", AvatarGallery.entryOfPublished(it)) }
    }

    @Test
    fun theGalleryIsNotEmpty() {
        assertNotNull(AvatarGallery.find("fox"))
        assertTrue(AvatarGallery.entries.size >= 16)
    }
}
