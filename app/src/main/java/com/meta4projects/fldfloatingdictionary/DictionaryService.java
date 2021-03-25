package com.meta4projects.fldfloatingdictionary;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.meta4projects.fldfloatingdictionary.activities.MainActivity;
import com.meta4projects.fldfloatingdictionary.others.Window;

import java.util.ArrayList;

public class DictionaryService extends Service {
    public static final int CODE_MAIN = 2;
    public static final int CODE_FOREGROUND = 3;
    
    Window window;
    private final ArrayList<String> entryWords = new ArrayList<>();
    
    public static void startDictionaryService(final Context context) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(context, DictionaryService.class);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
                
            }
        });
    }
    
    public static void stopDictionaryService(Context context) {
        context.stopService(new Intent(context, DictionaryService.class));
    }
    
    public static boolean isDictionaryRunning(Class<?> dictionaryService, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (dictionaryService.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        
        return false;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (window == null) {
            window = new Window(this, entryWords);
        }
        
        window.open();
        showNotification();
        
        return START_STICKY;
    }
    
    private void showNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Intent mainIntent = new Intent(this, MainActivity.class);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, CODE_MAIN, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel notificationChannel = new NotificationChannel("notify", "notification channel general", NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.enableLights(false);
                notificationChannel.setShowBadge(false);
                notificationChannel.enableVibration(false);
                notificationChannel.setSound(null, null);
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(notificationChannel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "notify");
        builder.setTicker(null)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Dictionary is running")
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);
        startForeground(CODE_FOREGROUND, builder.build());
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        window.close();
        stopForeground(true);
    }
}
