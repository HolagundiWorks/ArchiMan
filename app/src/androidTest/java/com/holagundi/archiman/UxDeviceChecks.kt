package com.holagundi.archiman

import android.content.Context
import com.holagundi.archiman.data.*
import com.holagundi.archiman.data.local.ArchiManDatabase
import java.io.File
import kotlinx.coroutines.flow.first

object UxDeviceChecks {
    suspend fun run(context: Context, db: ArchiManDatabase, suite: SuiteRepository, projectId: Long) {
        MeasurementEditorDeviceChecks.run(db, projectId)
        val cache = EditorDraftStore(context, "isolated-ux-draft")
        val other = EditorDraftStore(context, "isolated-other-draft")
        val secret = "Unsaved architectural agreement — confidential client terms"
        cache.write(secret); other.write("Other project")
        check(EditorDraftStore(context, "isolated-ux-draft").read() == secret)
        check(other.read() == "Other project")
        val stored = File(context.noBackupFilesDir, "editor-drafts").listFiles().orEmpty().first { it.readBytes().let(RecoveryCipher::protected) && runCatching { RecoveryCipher.decrypt(it.readBytes()).toString(Charsets.UTF_8) == secret }.getOrDefault(false) }
        check(!stored.readBytes().toString(Charsets.UTF_8).contains(secret))
        check(runCatching { cache.write("x".repeat(2_000_001)) }.isFailure)
        check(cache.read() == secret)
        val bytes = stored.readBytes(); bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); stored.writeBytes(bytes)
        check(runCatching { cache.read() }.isFailure)
        cache.delete(); other.delete(); check(cache.read() == null)

        val tasksBefore = db.suiteDao().observeTasks(projectId).first().size
        check(runCatching { suite.addTask(projectId, " ") }.isFailure)
        check(runCatching { suite.addInspection(projectId, "", "", "") }.isFailure)
        check(runCatching { PracticeRepository(db.practiceDao()).addClient("", "", "") }.isFailure)
        check(db.suiteDao().observeTasks(projectId).first().size == tasksBefore)

        // Undo accepts precisely the committed snapshot, and rejects an intervening edit.
        suite.addTask(projectId, "Isolated UX task")
        val task = db.suiteDao().observeTasks(projectId).first().first { it.title == "Isolated UX task" }
        val operations = RecordOperations(db)
        val original = operations.read(RecordKind.TASK, task.id)
        val edited = operations.saveWithSnapshot(original, mapOf("title" to "Edited UX task", "description" to original.text("description")))
        val restored = operations.saveWithSnapshot(edited, RecordKind.TASK.fields.associate { it.column to original.text(it.column) })
        check(restored.text("title") == original.text("title"))
        check(runCatching { operations.save(edited, RecordKind.TASK.fields.associate { it.column to edited.text(it.column) }) }.isFailure)
        operations.delete(restored)
        check(db.suiteDao().observeTasks(projectId).first().size == tasksBefore)
    }
}
