package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.*
import com.example.data.repository.SiteRepository
import com.example.domain.MeasurementSheetStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkItemMergeTest {
    private lateinit var database: AppDatabase

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TRIGGER lock_approved_measurement_update BEFORE UPDATE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
                    db.execSQL("CREATE TRIGGER lock_approved_measurement_delete BEFORE DELETE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
                    db.execSQL("CREATE TRIGGER immutable_review_event_update BEFORE UPDATE ON measurement_review_events BEGIN SELECT RAISE(ABORT, 'Review events are immutable'); END")
                    db.execSQL("CREATE TRIGGER immutable_review_event_delete BEFORE DELETE ON measurement_review_events BEGIN SELECT RAISE(ABORT, 'Review events are immutable'); END")
                }
            }).build()
    }

    @After fun close() = database.close()

    @Test fun mergePreservesMeasurementsAndRepointsCatalogReferences() = runBlocking {
        database.projectDao().insertProject(ProjectEntity(id = 1, name = "Project"))
        database.contractorDao().insertContractor(ContractorEntity(id = 2, name = "Contractor"))
        val canonicalId = database.itemMasterDao().insertItem(
            ItemMasterEntity(itemCode = "FIN-001", workType = "Finishes", name = "Internal wall cement plaster", unit = "sqm", calculationType = CalculationType.AREA)
        )
        val sourceId = database.itemMasterDao().insertItem(
            ItemMasterEntity(itemCode = "FIN-002", workType = "Finishes", name = "Cement plaster internal walls", unit = "sqm", calculationType = CalculationType.AREA)
        )
        database.componentWorkItemDao().insertWorkItem(ComponentWorkItemEntity(componentId = 8, itemId = sourceId, itemName = "Cement plaster internal walls", unit = "sqm", calculationType = CalculationType.AREA))
        SiteRepository(database).insertMeasurement(MeasurementEntity(projectId = 1, contractorId = 2, contractorName = "Contractor", itemId = sourceId, itemName = "Cement plaster internal walls", unit = "sqm", calculationType = CalculationType.AREA, quantity = 12.5))
        database.workItemAliasDao().insert(WorkItemAliasEntity(workItemId = sourceId, alias = "Internal plaster"))

        database.itemMasterDao().mergeIntoCanonical(sourceId, canonicalId)

        assertEquals(0, scalar("SELECT isActive FROM item_master WHERE id = $sourceId"))
        assertEquals(canonicalId, scalarLong("SELECT itemId FROM component_work_items LIMIT 1"))
        assertEquals(canonicalId, scalarLong("SELECT itemId FROM measurements LIMIT 1"))
        assertEquals("Cement plaster internal walls", scalarText("SELECT itemName FROM measurements LIMIT 1"))
        assertEquals(2, scalar("SELECT COUNT(*) FROM work_item_aliases WHERE workItemId = $canonicalId"))
        assertTrue(scalar("SELECT COUNT(*) FROM measurements") == 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun mergeRejectsDifferentFormulaTypes() = runBlocking {
        val area = database.itemMasterDao().insertItem(ItemMasterEntity(itemCode = "A", workType = "Finishes", name = "Area item", unit = "sqm", calculationType = CalculationType.AREA))
        val volume = database.itemMasterDao().insertItem(ItemMasterEntity(itemCode = "B", workType = "Finishes", name = "Volume item", unit = "cum", calculationType = CalculationType.VOLUME))
        database.itemMasterDao().mergeIntoCanonical(volume, area)
    }

    @Test fun batchCreatesOneSheetWithVersionedFormulaSnapshots() = runBlocking {
        database.projectDao().insertProject(ProjectEntity(id = 1, name = "Project"))
        database.contractorDao().insertContractor(ContractorEntity(id = 2, name = "Contractor"))
        val itemId = database.itemMasterDao().insertItem(ItemMasterEntity(itemCode = "AREA-1", workType = "Finishes", name = "Painting", unit = "sqm", calculationType = CalculationType.AREA))
        val base = MeasurementEntity(projectId = 1, contractorId = 2, contractorName = "Contractor", itemId = itemId, itemName = "Painting", unit = "sqm", calculationType = CalculationType.AREA, description = "Wall", length = 2.0, width = 3.0, quantity = 6.0)

        assertEquals(2, SiteRepository(database).insertMeasurementBatch(listOf(base, base.copy(description = "Ceiling"))))
        assertEquals(1, scalar("SELECT COUNT(*) FROM measurement_sheets"))
        assertEquals(1, scalar("SELECT COUNT(DISTINCT sheetId) FROM measurements"))
        assertEquals("AREA", scalarText("SELECT formulaCode FROM measurements LIMIT 1"))
        assertEquals(1, scalar("SELECT formulaVersion FROM measurements LIMIT 1"))
    }

    @Test fun approvedSheetIsAuditedAndRowsAreDatabaseLocked() = runBlocking {
        database.projectDao().insertProject(ProjectEntity(id = 1, name = "Project"))
        database.contractorDao().insertContractor(ContractorEntity(id = 2, name = "Contractor"))
        val itemId = database.itemMasterDao().insertItem(ItemMasterEntity(itemCode = "LOCK-1", workType = "Finishes", name = "Lock Test", unit = "sqm", calculationType = CalculationType.AREA))
        val row = MeasurementEntity(projectId = 1, contractorId = 2, contractorName = "Contractor", itemId = itemId, itemName = "Lock Test", unit = "sqm", calculationType = CalculationType.AREA, description = "Wall", length = 2.0, width = 3.0, quantity = 6.0)
        val repository = SiteRepository(database)
        repository.insertMeasurementBatch(listOf(row))
        val sheetId = scalarLong("SELECT id FROM measurement_sheets LIMIT 1")

        repository.transitionMeasurementSheet(sheetId, MeasurementSheetStatus.SUBMITTED, "Engineer")
        repository.transitionMeasurementSheet(sheetId, MeasurementSheetStatus.CHECKED, "Checker", "Dimensions checked")
        repository.transitionMeasurementSheet(sheetId, MeasurementSheetStatus.APPROVED, "Approver")

        assertEquals("APPROVED", scalarText("SELECT status FROM measurement_sheets WHERE id=$sheetId"))
        assertEquals(3, scalar("SELECT COUNT(*) FROM measurement_review_events WHERE sheetId=$sheetId"))
        val saved = database.measurementDao().getMeasurementById(scalarLong("SELECT id FROM measurements LIMIT 1"))!!
        assertTrue(runCatching { database.measurementDao().updateMeasurement(saved.copy(description = "Changed")) }.isFailure)
        assertTrue(runCatching { database.measurementDao().deleteMeasurement(saved) }.isFailure)
        assertTrue(runCatching { database.openHelper.writableDatabase.execSQL("UPDATE measurement_review_events SET comment='tampered'") }.isFailure)
        assertEquals("Wall", scalarText("SELECT description FROM measurements WHERE id=${saved.id}"))
    }

    @Test fun archiveRetainsRowsAndWritesAuditEvent() = runBlocking {
        database.projectDao().insertProject(ProjectEntity(id = 1, name = "Project"))
        database.contractorDao().insertContractor(ContractorEntity(id = 2, name = "Contractor"))
        val itemId = database.itemMasterDao().insertItem(ItemMasterEntity(itemCode = "ARC-1", workType = "Finishes", name = "Archive Test", unit = "sqm", calculationType = CalculationType.AREA))
        val repository = SiteRepository(database)
        repository.insertMeasurement(MeasurementEntity(projectId = 1, contractorId = 2, contractorName = "Contractor", itemId = itemId, itemName = "Archive Test", unit = "sqm", calculationType = CalculationType.AREA, description = "Wall", quantity = 1.0))
        val sheetId = scalarLong("SELECT id FROM measurement_sheets LIMIT 1")

        repository.archiveMeasurementSheet(sheetId, "Engineer")

        assertEquals(1, scalar("SELECT COUNT(*) FROM measurements"))
        assertTrue(scalarLong("SELECT archivedAt FROM measurement_sheets WHERE id=$sheetId") > 0)
        assertEquals("ARCHIVED", scalarText("SELECT toStatus FROM measurement_review_events WHERE sheetId=$sheetId"))
    }

    private fun scalar(sql: String): Int = database.openHelper.readableDatabase.query(sql).use { it.moveToFirst(); it.getInt(0) }
    private fun scalarLong(sql: String): Long = database.openHelper.readableDatabase.query(sql).use { it.moveToFirst(); it.getLong(0) }
    private fun scalarText(sql: String): String = database.openHelper.readableDatabase.query(sql).use { it.moveToFirst(); it.getString(0) }
}
