package com.example.cloud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SupabaseConnectionManagerTest {
    private val manager = SupabaseConnectionManager(ApplicationProvider.getApplicationContext<Context>())

    @Test fun acceptsHttpsProjectUrlAndPublishableKey() {
        assertNull(manager.validate("https://example.supabase.co", "sb_publishable_example"))
    }

    @Test fun rejectsSecretKey() {
        assertTrue(manager.validate("https://example.supabase.co", "sb_secret_example")!!.contains("not allowed"))
    }

    @Test fun rejectsNonHttpsUrlAndLegacyKey() {
        assertTrue(manager.validate("http://example.supabase.co", "sb_publishable_example")!!.contains("HTTPS"))
        assertTrue(manager.validate("https://example.supabase.co", "legacy-anon")!!.contains("sb_publishable_"))
    }
}
