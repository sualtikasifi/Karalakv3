package com.sualtikasifi.cizimhafiza.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChestConsolationTest {
    @Test fun two_a_day() {
        assertEquals(2, ChestConsolation.offersLeft(storedDay = -1, storedCount = 0, today = 10))
        assertEquals(1, ChestConsolation.offersLeft(10, 1, 10))
        assertEquals(0, ChestConsolation.offersLeft(10, 2, 10))
        assertEquals(0, ChestConsolation.offersLeft(10, 5, 10))
    }

    @Test fun a_new_day_starts_again() = assertEquals(2, ChestConsolation.offersLeft(10, 2, 11))
}
