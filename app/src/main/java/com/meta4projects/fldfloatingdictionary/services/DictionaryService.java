package com.meta4projects.fldfloatingdictionary.services;

import static com.meta4projects.fldfloatingdictionary.others.Util.isNightMode;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.activities.MainActivity;
import com.meta4projects.fldfloatingdictionary.others.DictionaryWidget;
import com.meta4projects.fldfloatingdictionary.others.Util;
import com.meta4projects.fldfloatingdictionary.others.Window;

public class DictionaryService extends Service {
    public static final String START = "com.meta4projects.fldfloatingdictionary.services.Start";
    public final int CODE_MAIN = 2;
    public final int CODE_FOREGROUND = 3;
    public final String DICTIONARY_CHANNEL_ID = "dictionary_channel_id";
    private Window window;

    public static void startDictionaryService(final Context context) {
        isNightMode(context);
        Intent intent = new Intent(context, DictionaryService.class);
        context.startForegroundService(intent);
    }

    public static void stopDictionaryService(Context context) {
        context.stopService(new Intent(context, DictionaryService.class));
    }

    public static boolean isDictionaryRunning(Class<?> dictionaryService, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE))
            if (dictionaryService.getName().equals(serviceInfo.service.getClassName())) return true;
        return false;
    }

    public void updateWidget() {
        Intent intent = new Intent(this, DictionaryWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, DictionaryWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Util.initialiseTTS(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWidget();
        if (intent.getBooleanExtra(START, true)) {
            if (window == null) window = new Window(this);
            showNotification();
            return START_STICKY;
        } else {
            stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
    }

    private void showNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, CODE_MAIN, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            NotificationChannel notificationChannel = new NotificationChannel(DICTIONARY_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
            notificationChannel.enableLights(false);
            notificationChannel.setShowBadge(false);
            notificationChannel.setShowBadge(false);
            notificationChannel.enableVibration(true);
            notificationChannel.setSound(null, null);
            manager.createNotificationChannel(notificationChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Notification notification = new NotificationCompat.Builder(this, DICTIONARY_CHANNEL_ID).setTicker(null).setContentText("Dictionary is running").setAutoCancel(false).setSilent(true).setOngoing(true).setWhen(System.currentTimeMillis()).setSmallIcon(R.drawable.ic_notification).setPriority(NotificationCompat.PRIORITY_MIN).setVisibility(NotificationCompat.VISIBILITY_SECRET).setContentIntent(pendingIntent).build();
        startForeground(CODE_FOREGROUND, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        window.close();
        stopForeground(true);
        stopSelf();
    }
}
