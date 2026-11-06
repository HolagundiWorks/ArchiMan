package com.holagundi.archiman

import androidx.lifecycle.SavedStateHandle
import com.holagundi.archiman.data.*
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.ui.RecordMeasurementViewModel
import java.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull

object MeasurementEditorDeviceChecks {
    suspend fun run(db: ArchiManDatabase, projectId: Long) {
        val dao = db.measurementDao()
        val original = dao.measurementsNow(projectId).first()
        val originalSheet = requireNotNull(dao.getSheet(original.sheetId))
        val count = dao.measurementsNow(projectId).size
        val sheetId = dao.insertSheet(originalSheet.copy(id = 0, sheetCode = "UX-EDITOR-${System.nanoTime()}", status = "DRAFT", lockedAt = null, archivedAt = null))
        val id = dao.insertMeasurement(original.copy(id = 0, sheetId = sheetId))
        val operations = RecordOperations(db)
        val repository = MeasurementRepository(db, dao, db.catalogDao(), db.projectDao())
        val handle = SavedStateHandle()
        val editor = RecordMeasurementViewModel(repository, operations, handle)
        suspend fun select(vm: RecordMeasurementViewModel): RecordSnapshot {
            val done = CompletableDeferred<RecordSnapshot>()
            withContext(Dispatchers.Main) { vm.selectEntry(id) { done.complete(it) } }
            return withTimeout(10000) { done.await() }.also { vm.busy.first { busy -> !busy } }
        }
        try {
            val selected = select(editor)
            check(!editor.editing.value)
            val editReady = CompletableDeferred<Unit>()
            withContext(Dispatchers.Main) { editor.editSelected { editReady.complete(Unit) } }
            withTimeout(10000) { editReady.await() }; editor.busy.first { !it }

            // Recreate the editor from the same Bundle-compatible serializable state.
            val serialized = ByteArrayOutputStream().also { output -> ObjectOutputStream(output).use { it.writeObject(handle.get<HashMap<String, Any?>>("entryValues")) } }.toByteArray()
            @Suppress("UNCHECKED_CAST")
            val values = ObjectInputStream(serialized.inputStream()).use { it.readObject() as HashMap<String, Any?> }
            val restored = RecordMeasurementViewModel(repository, operations, SavedStateHandle(mapOf("entryId" to id, "entryValues" to values, "entryEditing" to true)))
            check(restored.selected.value?.id == id && restored.editing.value)
            val fields = RecordKind.MEASUREMENT.fields.associate { it.column to selected.text(it.column) }.toMutableMap()
            fields["nos"] = (selected.text("nos").toDouble() + 1).toString()
            val committed = CompletableDeferred<RecordSnapshot>()
            withContext(Dispatchers.Main) { restored.saveSelected(fields) { committed.complete(it) } }
            val updated = withTimeout(10000) { committed.await() }; restored.busy.first { !it }
            check(updated.id == id && !restored.editing.value)
            check(dao.measurementsNow(projectId).size == count + 1)
            check(updated.text("boqRevision") == selected.text("boqRevision") && updated.values["rateSnapshot"] == selected.values["rateSnapshot"])

            var staleAccepted = false
            withContext(Dispatchers.Main) { editor.saveSelected(fields) { staleAccepted = true } }
            val staleError = withTimeout(10000) { editor.error.filterNotNull().first() }; editor.busy.first { !it }
            check(!staleAccepted && staleError.contains("changed"))
            check(dao.measurementsNow(projectId).size == count + 1)

            dao.updateSheet(requireNotNull(dao.getSheet(sheetId)).copy(status = "APPROVED"))
            select(restored)
            withContext(Dispatchers.Main) { restored.editSelected { error("Approved entries are read-only") } }
            withTimeout(10000) { restored.error.filterNotNull().first() }; restored.busy.first { !it }
            check(!restored.editing.value)
        } finally {
            dao.updateSheet(requireNotNull(dao.getSheet(sheetId)).copy(status = "DRAFT", lockedAt = null))
            operations.delete(operations.read(RecordKind.MEASUREMENT, id))
            operations.delete(operations.read(RecordKind.SHEET, sheetId))
        }
        check(dao.measurementsNow(projectId).size == count)
    }
}
