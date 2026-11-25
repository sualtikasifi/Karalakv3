package com.sualtikasifi.cizimhafiza.domain.model

import androidx.annotation.DrawableRes
import com.sualtikasifi.cizimhafiza.R

/**
 * The pictures a player can wear inside their avatar frame. These and the Karalak mascot are the only choices: a player cannot
 * bring their own photo (a Google account picture used to be offered; it is not any more), so nothing a person uploads or
 * has uploaded elsewhere ever appears on another player's screen.
 *
 * What other players are told is the short string [published] ("avatar:fox"); the picture itself is drawn from the app's own
 * resources, so nothing is downloaded and an id this build does not know (a newer app's picture) simply shows the mascot.
 *
 * Adding a picture: put `avatar_gallery_<id>.webp` in res/drawable-nodpi (scripts/make_avatar_gallery.py makes it from the
 * artwork), then list it here. The order below is the order on screen.
 */
object AvatarGallery {
    class Entry(val id: String, @DrawableRes val res: Int)

    /** The prefix of the value published to other players (also checked by firestore.rules' avatarUrl rule). */
    const val PUBLISHED_PREFIX = "avatar:"

    /** The prefix of the value kept in this phone's settings ([com.sualtikasifi.cizimhafiza.util.SettingsRepository.avatarSource]). */
    const val SOURCE_PREFIX = "AVATAR:"

    val entries: List<Entry> = listOf(
        Entry("puppy_girl", R.drawable.avatar_gallery_puppy_girl),
        Entry("cat_artist", R.drawable.avatar_gallery_cat_artist),
        Entry("panda", R.drawable.avatar_gallery_panda),
        Entry("fox", R.drawable.avatar_gallery_fox),
        Entry("bunny_astronaut", R.drawable.avatar_gallery_bunny_astronaut),
        Entry("penguin", R.drawable.avatar_gallery_penguin),
        Entry("lion", R.drawable.avatar_gallery_lion),
        Entry("owl", R.drawable.avatar_gallery_owl),
        Entry("dino", R.drawable.avatar_gallery_dino),
        Entry("robot", R.drawable.avatar_gallery_robot),
        Entry("octopus", R.drawable.avatar_gallery_octopus),
        Entry("koala", R.drawable.avatar_gallery_koala),
        Entry("tiger", R.drawable.avatar_gallery_tiger),
        Entry("frog", R.drawable.avatar_gallery_frog),
        Entry("unicorn", R.drawable.avatar_gallery_unicorn),
        Entry("bear", R.drawable.avatar_gallery_bear)
    )

    fun find(id: String?): Entry? = if (id == null) null else entries.firstOrNull { it.id == id }

    /** The setting that picks [id]. */
    fun source(id: String): String = SOURCE_PREFIX + id

    /** The gallery picture a setting value picks, or null (the mascot: "DINO", "" or anything else, including the retired "GOOGLE"). */
    fun entryOfSource(source: String?): Entry? =
        if (source != null && source.startsWith(SOURCE_PREFIX)) find(source.removePrefix(SOURCE_PREFIX)) else null

    /** What other players are told about [entry]. */
    fun published(entry: Entry): String = PUBLISHED_PREFIX + entry.id

    /** The gallery picture a published value names, or null (the mascot): "", a retired Google address, an unknown id, anything else. */
    fun entryOfPublished(value: String?): Entry? =
        if (value != null && value.length <= MAX_PUBLISHED_LENGTH && value.startsWith(PUBLISHED_PREFIX)) find(value.removePrefix(PUBLISHED_PREFIX)) else null

    private const val MAX_PUBLISHED_LENGTH = 64
}
