package com.sualtikasifi.cizimhafiza.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards every achievement against the ways one can silently stop working: a threshold in the condition that
 * disagrees with the number in its name, a description that promises a different number, a metric that is
 * read from the wrong counter, or one that unlocks off another metric's counter.
 */
class AchievementCatalogTest {

    private val zero = AchievementStats(
        gamesPlayed = 0, lifetimeWordsDrawn = 0, lifetimeScore = 0, currentStreak = 0,
        bestStreak = 0, perfectRounds = 0, onlineWins = 0, lifetimeXp = 0
    )

    private fun statsFor(metric: AchievementMetric, value: Int): AchievementStats = when (metric) {
        AchievementMetric.GAMES -> zero.copy(gamesPlayed = value)
        AchievementMetric.SCORE -> zero.copy(lifetimeScore = value)
        AchievementMetric.WORDS -> zero.copy(lifetimeWordsDrawn = value)
        AchievementMetric.XP -> zero.copy(lifetimeXp = value)
        AchievementMetric.PERFECT -> zero.copy(perfectRounds = value)
        AchievementMetric.STREAK -> zero.copy(bestStreak = value)
        AchievementMetric.ONLINE_WIN -> zero.copy(onlineWins = value)
    }

    @Test
    fun everyAchievementUnlocksExactlyAtItsTarget() {
        Achievement.entries.forEach { a ->
            assertTrue("${a.name} should unlock at ${a.target}", a.isUnlocked(statsFor(a.metric, a.target)))
            assertTrue("${a.name} should stay unlocked above its target", a.isUnlocked(statsFor(a.metric, a.target + 1)))
            assertFalse("${a.name} must not unlock one short of ${a.target}", a.isUnlocked(statsFor(a.metric, a.target - 1)))
            assertEquals("${a.name} progress reads the wrong counter", a.target, a.currentValue(statsFor(a.metric, a.target)))
        }
    }

    @Test
    fun noAchievementUnlocksFromAnotherMetricsCounter() {
        Achievement.entries.forEach { a ->
            AchievementMetric.entries.filter { it != a.metric }.forEach { other ->
                assertFalse(
                    "${a.name} unlocked from ${other.name}",
                    a.isUnlocked(statsFor(other, 10_000_000))
                )
            }
        }
    }

    @Test
    fun nothingIsUnlockedFromAFreshInstall() {
        Achievement.entries.forEach { assertFalse("${it.name} unlocked with no progress", it.isUnlocked(zero)) }
    }

    @Test
    fun descriptionsPromiseTheRealTarget() {
        val xml = listOf("src/main/res/values-tr/strings.xml", "src/main/res/values/strings.xml")
            .map { File(it) }.first { it.exists() }.readText()
        Achievement.entries.forEach { a ->
            val key = "achievement_${a.name.lowercase()}_desc"
            val text = Regex("""<string name="$key">(.*?)</string>""").find(xml)?.groupValues?.get(1)
            assertTrue("missing description string for ${a.name}", text != null)
            // Thousands are written with separators ("1.000" / "1,000"), and the first-game ones carry no number.
            val numbers = Regex("""\d[\d.,]*""").findAll(text!!).map { it.value.replace(".", "").replace(",", "").toInt() }.toList()
            if (a != Achievement.FIRST_GAME && a != Achievement.PERFECT_ROUND && a != Achievement.STREAK_1 && a != Achievement.ONLINE_WIN_1) {
                assertTrue("${a.name}'s description ($text) does not mention ${a.target}", a.target in numbers)
            }
        }
    }

    @Test
    fun rewardsAreSplitAndPositive() {
        Achievement.entries.forEach {
            assertTrue("${it.name} pays nothing", it.xpReward > 0 && it.goldReward > 0)
        }
        assertEquals(Achievement.entries.size, Achievement.entries.map { it.name }.toSet().size)
    }
}
