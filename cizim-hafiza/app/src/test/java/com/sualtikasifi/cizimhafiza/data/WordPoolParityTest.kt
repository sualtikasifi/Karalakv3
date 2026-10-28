package com.sualtikasifi.cizimhafiza.data

import com.sualtikasifi.cizimhafiza.data.local.WordSeeder
import com.sualtikasifi.cizimhafiza.domain.model.SupportedLanguage
import com.sualtikasifi.cizimhafiza.util.AnswerMatcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every language's word pool (assets/words*.json) must hold the same ids in the same order, with the same difficulty:
 * an online room is dealt by its host from THEIR pool and each player then looks those ids up in their own, so a pool
 * that lacks an id hands one player fewer words, and one that rates it differently changes the timer between them.
 */
class WordPoolParityTest {

    @Serializable
    private data class Entry(val id: Int, val text: String, val category: String, val difficulty: String, val approved: Boolean? = null)

    private val json = Json { ignoreUnknownKeys = true }
    private fun pool(file: String): List<Entry> = json.decodeFromString(File("src/main/assets/$file").readText())

    private val files = SupportedLanguage.entries.map { WordSeeder.assetFileFor(it.code) }

    @Test
    fun `every supported language has its own pool file`() {
        assertEquals(SupportedLanguage.entries.size, files.toSet().size)
        files.forEach { assertTrue("missing $it", File("src/main/assets/$it").exists()) }
        assertEquals(WordSeeder.ENGLISH_ASSET_FILE, WordSeeder.assetFileFor("hi"))
    }

    @Test
    fun `pools match the Turkish one id for id`() {
        val reference = pool(WordSeeder.TURKISH_ASSET_FILE)
        for (file in files) {
            val other = pool(file)
            assertEquals(file, reference.map { it.id }, other.map { it.id })
            assertEquals(file, reference.map { it.difficulty }, other.map { it.difficulty })
            assertEquals(file, reference.map { it.category }.toSet().size, other.map { it.category }.toSet().size)
        }
    }

    @Test
    fun `the Turkish-only words stay out of every other pool's draw`() {
        val english = pool(WordSeeder.ENGLISH_ASSET_FILE).filter { it.approved == false }.map { it.id }.toSet()
        for (file in files - WordSeeder.TURKISH_ASSET_FILE) {
            assertEquals(file, english, pool(file).filter { it.approved == false }.map { it.id }.toSet())
        }
    }

    @Test
    fun `no two dealt words read the same`() {
        for (file in files) {
            val dealt = pool(file).filter { it.approved != false }
            dealt.forEach { assertTrue("$file ${it.id} is blank or padded", it.text.isNotBlank() && it.text == it.text.trim()) }
            // Folded the way answers are compared, except in Turkish, whose folding merges real pairs on purpose
            // (masa/maşa, gol/göl, süt/şut are separate words there).
            val key: (Entry) -> String =
                if (file == WordSeeder.TURKISH_ASSET_FILE) { e -> e.text.lowercase() } else { e -> AnswerMatcher.normalize(e.text) }
            val clashes = dealt.groupBy(key).filterValues { it.size > 1 }.keys
            assertTrue("$file repeats $clashes", clashes.isEmpty())
        }
    }
}
