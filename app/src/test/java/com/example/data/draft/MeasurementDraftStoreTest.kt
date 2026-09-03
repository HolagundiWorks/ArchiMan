package com.example.data.draft

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeasurementDraftStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val key = MeasurementDraftKey(1, 2, 3, 4)

    @Before fun clear() = MeasurementDraftStore(context).clear(key)

    @Test fun draftSurvivesStoreRecreationAndKeepsRowsScopedToSession() {
        val row = MeasurementDraftRow("row-1", "North wall", "4.2", "3", "0.23", "1", "0", "", "file:///photo.jpg")
        MeasurementDraftStore(context).save(key, listOf(row), "IMPERIAL")

        val restored = MeasurementDraftStore(context).load(key)
        assertEquals(row, restored?.rows?.single())
        assertEquals("IMPERIAL", restored?.unitSystem)
        assertNull(MeasurementDraftStore(context).load(key.copy(floorId = 99)))
    }

    @Test fun emptyDefaultRowDoesNotCreateDraft() {
        val empty = MeasurementDraftRow("row-1", "", "", "", "", "1", "0", "", null)
        MeasurementDraftStore(context).save(key, listOf(empty))
        assertNull(MeasurementDraftStore(context).load(key))
    }

    @Test fun legacyDraftWithoutUnitSystemDefaultsToMetric() {
        context.getSharedPreferences("measurement_drafts", Context.MODE_PRIVATE)
            .edit()
            .putString(
                key.storageKey,
                """{"rows":[{"id":"legacy","description":"Wall","lengthText":"2","heightText":"3","widthText":"","nosText":"1","deductionText":"0","remarks":"","photoUri":null}],"updatedAt":1}"""
            )
            .commit()

        val restored = MeasurementDraftStore(context).load(key)
        assertEquals("METRIC", restored?.unitSystem)
        assertEquals("2", restored?.rows?.single()?.lengthText)
    }
}
