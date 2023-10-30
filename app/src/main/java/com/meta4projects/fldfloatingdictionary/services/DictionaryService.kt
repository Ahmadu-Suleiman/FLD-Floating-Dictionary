package com.meta4projects.fldfloatingdictionary.services

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.activities.MainActivity
import com.meta4projects.fldfloatingdictionary.others.DictionaryWidget
import com.meta4projects.fldfloatingdictionary.others.Util.initialiseTTS
import com.meta4projects.fldfloatingdictionary.others.Util.isNightMode
import com.meta4projects.fldfloatingdictionary.others.Window

class DictionaryService : Service() {
    private val CODE_MAIN = 2
    private val CODE_FOREGROUND = 3
    private val DICTIONARY_CHANNEL_ID = "dictionary_channel_id"
    private var window: Window? = null

    private fun updateWidget() {
        val intent = Intent(this, DictionaryWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, DictionaryWidget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

    override fun onCreate() {
        super.onCreate()
        initialiseTTS(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        updateWidget()
        return if (intent.getBooleanExtra(START, true)) {
            if (window == null) window = Window(this)
            showNotification()
            START_STICKY
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            super.onStartCommand(intent, flags, startId)
        }
    }

    private fun showNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, CODE_MAIN, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            val notificationChannel = NotificationChannel(DICTIONARY_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN)
            notificationChannel.enableLights(false)
            notificationChannel.setShowBadge(false)
            notificationChannel.setShowBadge(false)
            notificationChannel.enableVibration(true)
            notificationChannel.setSound(null, null)
            manager.createNotificationChannel(notificationChannel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val notification = NotificationCompat.Builder(this, DICTIONARY_CHANNEL_ID).setTicker(null).setContentText("Dictionary is running").setAutoCancel(false).setSilent(true).setOngoing(true).setWhen(System.currentTimeMillis()).setSmallIcon(R.drawable.ic_notification).setPriority(NotificationCompat.PRIORITY_MIN).setVisibility(NotificationCompat.VISIBILITY_SECRET).setContentIntent(pendingIntent).build()
        startForeground(CODE_FOREGROUND, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        window?.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val START = "com.meta4projects.fldfloatingdictionary.services.Start"
        fun startDictionaryService(context: Context) {
            isNightMode(context)
            val intent = Intent(context, DictionaryService::class.java)
            context.startForegroundService(intent)
        }

        fun stopDictionaryService(context: Context) {
            context.stopService(Intent(context, DictionaryService::class.java))
        }

        fun isDictionaryRunning(dictionaryService: Class<*>, context: Context): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (serviceInfo in manager.getRunningServices(Int.MAX_VALUE)) if (dictionaryService.name == serviceInfo.service.className) return true
            return false
        }
    }
}