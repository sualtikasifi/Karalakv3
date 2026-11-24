package com.sualtikasifi.cizimhafiza.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class DuelNotificationTextTest {
    @Test fun firstTimeUsesTheFirstPick() = assertEquals(3, DuelNotificationText.nextIndex(-1, 7, 3))

    @Test fun firstPickStaysInsideTheList() = assertEquals(6, DuelNotificationText.nextIndex(-1, 7, 99))

    @Test fun nextOneFollows() = assertEquals(4, DuelNotificationText.nextIndex(3, 7, 0))

    @Test fun wrapsAroundToTheStart() = assertEquals(0, DuelNotificationText.nextIndex(6, 7, 5))

    @Test fun neverTheSameTwiceInARow() {
        var last = -1
        repeat(30) {
            val next = DuelNotificationText.nextIndex(last, 7, 2)
            if (last >= 0) assert(next != last)
            last = next
        }
    }

    @Test fun aListThatShrankStartsOverInsteadOfCrashing() = assertEquals(2, DuelNotificationText.nextIndex(9, 3, 2))

    @Test fun anEmptyListGivesZero() = assertEquals(0, DuelNotificationText.nextIndex(0, 0, 0))
}
