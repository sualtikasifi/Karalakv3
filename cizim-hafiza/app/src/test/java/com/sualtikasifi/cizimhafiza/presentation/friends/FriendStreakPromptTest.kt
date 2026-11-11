package com.sualtikasifi.cizimhafiza.presentation.friends

import com.sualtikasifi.cizimhafiza.domain.model.FriendStreak
import com.sualtikasifi.cizimhafiza.util.StreakReward
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendStreakPromptTest {
    private val me = "me"
    private val today = 20_000
    private val names = mapOf("ayse" to "Ayşe", "can" to "Can")

    private fun streak(friend: String, count: Int, lastDay: Int, run: Int = 1, repairs: Int = 0, repairedBy: String? = null, pending: Int = -1) = FriendStreak(
        pairId = "$friend-pair", friendUid = friend, count = count, best = count, run = run, lastDay = lastDay, pendingDay = pending,
        repairs = repairs, repairedBy = repairedBy, updatedAt = 0L
    )

    private fun memory(claimed: Set<String> = emptySet(), reminded: Set<String> = emptySet(), seen: Map<String, Int> = emptyMap()) = PromptMemory(
        isClaimed = { pair, run, milestone -> "$pair:$run:$milestone" in claimed },
        remindedOn = { pair, day -> "$pair:$day" in reminded },
        seenRepairs = { seen[it] ?: 0 }
    )

    private fun next(streaks: List<FriendStreak>, hour: Int = 20, ads: Boolean = true, memory: PromptMemory = memory()) =
        nextPrompt(streaks, names, me, today, hour, ads, memory)

    @Test
    fun `nothing to say when every streak is done for today and no prize waits`() {
        assertNull(next(listOf(streak("ayse", 2, today))))
    }

    @Test
    fun `a streak that ends today is mentioned in the afternoon, not before`() {
        val s = listOf(streak("ayse", 5, today - 1).copy(best = 5))
        // 5 days would also pay the 3-day prize, so it is already taken here
        val mem = memory(claimed = setOf("ayse-pair:1:3"))
        assertNull(next(s, hour = 10, memory = mem))
        val p = next(s, hour = 15, memory = mem) as StreakPrompt.AtRisk
        assertEquals("Ayşe", p.items.single().friendName)
        assertEquals(5, p.items.single().count)
    }

    @Test
    fun `a reminder already given today is not repeated`() {
        val s = listOf(streak("ayse", 2, today - 1))
        assertNull(next(s, memory = memory(reminded = setOf("ayse-pair:$today"))))
    }

    @Test
    fun `a streak of one day is not worth a reminder`() {
        assertNull(next(listOf(streak("ayse", 1, today - 1))))
    }

    @Test
    fun `a broken streak offers the ad, and only when ads are on`() {
        val s = listOf(streak("ayse", 12, today - 2))
        val mem = memory(claimed = setOf("ayse-pair:1:3", "ayse-pair:1:7"))
        val p = next(s, memory = mem) as StreakPrompt.Broken
        assertEquals("ayse", p.friendUid); assertEquals(12, p.count)
        assertNull(next(s, ads = false, memory = mem))
    }

    @Test
    fun `a streak lost for good says nothing`() {
        assertNull(next(listOf(streak("ayse", 12, today - 5))))
    }

    @Test
    fun `a milestone prize waits until it is taken, smallest first`() {
        val s = listOf(streak("ayse", 8, today))
        val first = next(s) as StreakPrompt.Reward
        assertEquals(3, first.milestone); assertEquals(StreakReward.Gold(50), first.reward)
        val second = next(s, memory = memory(claimed = setOf("ayse-pair:1:3"))) as StreakPrompt.Reward
        assertEquals(7, second.milestone)
        assertNull(next(s, memory = memory(claimed = setOf("ayse-pair:1:3", "ayse-pair:1:7"))))
    }

    @Test
    fun `a new run pays its prizes again`() {
        val s = listOf(streak("ayse", 3, today, run = 2))
        assertEquals(3, (next(s, memory = memory(claimed = setOf("ayse-pair:1:3"))) as StreakPrompt.Reward).milestone)
    }

    @Test
    fun `the friend mending the streak is told first`() {
        val s = listOf(streak("ayse", 8, today - 1, repairs = 1, repairedBy = "ayse"))
        val p = next(s, memory = memory(seen = emptyMap())) as StreakPrompt.Rescued
        assertEquals("Ayşe", p.friendName); assertEquals(1, p.repairs)
        // once told, the next window is the prize that was waiting
        assertTrue(next(s, memory = memory(seen = mapOf("ayse-pair" to 1))) is StreakPrompt.Reward)
    }

    @Test
    fun `a mend the player made themselves is not announced to them`() {
        val s = listOf(streak("ayse", 2, today, repairs = 1, repairedBy = me))
        assertNull(next(s))
    }

    @Test
    fun `friends who are no longer friends are skipped`() {
        assertNull(next(listOf(streak("stranger", 9, today - 2))))
    }
}
