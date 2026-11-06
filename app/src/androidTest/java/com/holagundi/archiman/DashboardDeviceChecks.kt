package com.holagundi.archiman

import com.holagundi.archiman.data.ProjectRepository
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.data.RecordOperations
import com.holagundi.archiman.data.SuiteRepository
import com.holagundi.archiman.data.local.ArchiManDatabase
import kotlinx.coroutines.flow.first

object DashboardDeviceChecks {
    suspend fun run(db: ArchiManDatabase, suite: SuiteRepository) {
        val sql = db.openHelper.writableDatabase
        val before = db.suiteDao().observeDashboardMetrics().first()
        val projectId = ProjectRepository(db, db.projectDao(), db.practiceDao()).createProject("Dashboard check", "DASH-DEVICE", "Dashboard client", "Residential", "Site", "Architect")
        suite.addTask(projectId, "Dashboard open task")
        suite.addTask(projectId, "Dashboard completed task")
        sql.execSQL("UPDATE project_tasks SET status = 'DONE' WHERE projectId = ? AND title = 'Dashboard completed task'", arrayOf(projectId))
        sql.execSQL("UPDATE project_tasks SET dueDate = 1 WHERE projectId = ? AND status != 'DONE'", arrayOf(projectId))
        val sampleId = sql.query("SELECT id FROM measurement_sheets LIMIT 1").use { it.moveToFirst(); it.getLong(0) }
        val sample = db.measurementDao().getSheet(sampleId)!!
        db.measurementDao().insertSheet(sample.copy(id = 0, projectId = projectId, sheetCode = "DASH-SHEET", floorId = null, status = "SUBMITTED", archivedAt = null, updatedAt = 1))
        val populated = db.suiteDao().observeDashboardMetrics().first()
        check(populated.openTasks == before.openTasks + 1)
        check(populated.pendingReviews == before.pendingReviews + 1)
        check(db.suiteDao().observeDashboardTasks().first().first().projectId == projectId)
        check(db.suiteDao().observeDashboardReviews().first().any { it.projectId == projectId })
        val operations = RecordOperations(db)
        operations.archiveProject(operations.read(RecordKind.PROJECT, projectId))
        check(db.suiteDao().observeDashboardMetrics().first() == before)
        check(db.suiteDao().observeDashboardTasks().first().none { it.projectId == projectId })
        check(db.suiteDao().observeDashboardReviews().first().none { it.projectId == projectId })
    }
}
