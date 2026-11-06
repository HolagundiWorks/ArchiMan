package com.holagundi.archiman.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class PracticeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        PracticeWidgetUpdater.update(context, appWidgetManager, appWidgetIds)
    }
}
