package com.meta4projects.fldfloatingdictionary.others;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.services.DictionaryService;

public class DictionaryWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.dictionary_widget);
            Intent intent;
            if (DictionaryService.isDictionaryRunning(DictionaryService.class, context)) {
                views.setTextViewText(R.id.text_widget, context.getText(R.string.activated_info));
                views.setTextViewText(R.id.activate_widget, context.getText(R.string.deactivate));
                intent = new Intent(context, DictionaryService.class).putExtra(DictionaryService.START, false);
            } else {
                views.setTextViewText(R.id.text_widget, context.getText(R.string.not_activated_info));
                views.setTextViewText(R.id.activate_widget, context.getText(R.string.activate));
                intent = new Intent(context, DictionaryService.class).putExtra(DictionaryService.START, true);
            }

            PendingIntent pendingIntent = PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.activate_widget, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}