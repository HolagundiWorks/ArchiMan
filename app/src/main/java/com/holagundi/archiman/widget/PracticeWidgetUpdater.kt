package com.holagundi.archiman.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.holagundi.archiman.MainActivity
import com.holagundi.archiman.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

object PracticeWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun requestUpdate(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val ids = manager.getAppWidgetIds(ComponentName(app, PracticeWidgetProvider::class.java))
        if (ids.isEmpty()) return
        update(app, manager, ids)
    }

    fun update(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val app = context.applicationContext
        scope.launch {
            val views = try {
                withTimeout(8_000) { buildViews(app) }
            } catch (_: Exception) {
                errorViews(app)
            }
            appWidgetIds.forEach { id -> manager.updateAppWidget(id, views) }
        }
    }

    private suspend fun buildViews(context: Context): RemoteViews {
        val entry = EntryPointAccessors.fromApplication(context, PracticeWidgetEntryPoint::class.java)
        val metrics = entry.suiteDao().observeDashboardMetrics().first()
        val openTaskCount = metrics.openTasks
        val projects = entry.projectDao().projects().count { it.status != "ARCHIVED" }
        return RemoteViews(context.packageName, R.layout.widget_practice).apply {
            setTextViewText(R.id.widget_kpi_projects, projects.toString())
            setTextViewText(R.id.widget_kpi_tasks, openTaskCount.toString())
            setTextViewText(R.id.widget_kpi_reviews, metrics.pendingReviews.toString())
            setTextViewText(R.id.widget_kpi_issues, metrics.openIssues.toString())
            // Privacy: aggregates only — no project or task titles on the launcher.
            setViewVisibility(R.id.widget_task_1, View.GONE)
            setViewVisibility(R.id.widget_task_2, View.GONE)
            setViewVisibility(R.id.widget_task_3, View.GONE)
            setViewVisibility(R.id.widget_task_4, View.GONE)
            setViewVisibility(R.id.widget_tasks_empty, View.VISIBLE)
            setTextViewText(
                R.id.widget_tasks_empty,
                if (openTaskCount == 0) context.getString(R.string.widget_tasks_empty)
                else context.resources.getQuantityString(R.plurals.widget_open_tasks_summary, openTaskCount, openTaskCount),
            )
            setOnClickPendingIntent(R.id.widget_root, openApp(context))
        }
    }

    private fun errorViews(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_practice).apply {
            setTextViewText(R.id.widget_kpi_projects, "—")
            setTextViewText(R.id.widget_kpi_tasks, "—")
            setTextViewText(R.id.widget_kpi_reviews, "—")
            setTextViewText(R.id.widget_kpi_issues, "—")
            setViewVisibility(R.id.widget_task_1, View.GONE)
            setViewVisibility(R.id.widget_task_2, View.GONE)
            setViewVisibility(R.id.widget_task_3, View.GONE)
            setViewVisibility(R.id.widget_task_4, View.GONE)
            setViewVisibility(R.id.widget_tasks_empty, View.VISIBLE)
            setTextViewText(R.id.widget_tasks_empty, context.getString(R.string.widget_tasks_error))
            setOnClickPendingIntent(R.id.widget_root, openApp(context))
        }
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
