package com.meta4projects.fldfloatingdictionary.others

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.services.DictionaryService

class DictionaryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context,
                          appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.dictionary_widget)
            val intent: Intent = if (DictionaryService.isDictionaryRunning(DictionaryService::class.java, context)) {
                views.setTextViewText(R.id.text_widget, context.getText(R.string.activated_info))
                views.setTextViewText(R.id.activate_widget, context.getText(R.string.deactivate))
                Intent(context, DictionaryService::class.java).putExtra(DictionaryService.START, false)
            } else {
                views.setTextViewText(R.id.text_widget, context.getText(R.string.not_activated_info))
                views.setTextViewText(R.id.activate_widget, context.getText(R.string.activate))
                Intent(context, DictionaryService::class.java).putExtra(DictionaryService.START, true)
            }
            val pendingIntent = PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.activate_widget, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}