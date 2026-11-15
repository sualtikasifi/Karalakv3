package com.sualtikasifi.cizimhafiza.presentation.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperGateTest {
    @Test fun `the maintainer's verified account opens it`() {
        assertTrue(DeveloperGate.allows("raunen3075@gmail.com", emailVerified = true, debugBuild = false))
    }

    @Test fun `the same address unverified does not`() {
        assertFalse(DeveloperGate.allows("raunen3075@gmail.com", emailVerified = false, debugBuild = false))
    }

    @Test fun `any other account does not, verified or not`() {
        assertFalse(DeveloperGate.allows("someone@gmail.com", emailVerified = true, debugBuild = false))
        assertFalse(DeveloperGate.allows("someone@gmail.com", emailVerified = false, debugBuild = false))
    }

    @Test fun `a lookalike address does not`() {
        assertFalse(DeveloperGate.allows("raunen3075@gmail.com.evil.com", emailVerified = true, debugBuild = false))
        assertFalse(DeveloperGate.allows("Raunen3075@gmail.com", emailVerified = true, debugBuild = false))
    }

    @Test fun `a phone signed in to nothing does not`() {
        assertFalse(DeveloperGate.allows(null, emailVerified = false, debugBuild = false))
    }

    @Test fun `a debug build is open so the previews work`() {
        assertTrue(DeveloperGate.allows(null, emailVerified = false, debugBuild = true))
    }
}
