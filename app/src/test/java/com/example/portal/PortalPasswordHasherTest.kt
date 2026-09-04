package com.example.portal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PortalPasswordHasherTest {
    @Test fun acceptsOnlyTheOriginalPassword() {
        val digest = PortalPasswordHasher.hash("correct-password".toCharArray())
        assertTrue(PortalPasswordHasher.verify("correct-password".toCharArray(), digest.hash, digest.salt, digest.iterations))
        assertFalse(PortalPasswordHasher.verify("wrong-password".toCharArray(), digest.hash, digest.salt, digest.iterations))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShortPasswords() {
        PortalPasswordHasher.hash("short".toCharArray())
    }
}
