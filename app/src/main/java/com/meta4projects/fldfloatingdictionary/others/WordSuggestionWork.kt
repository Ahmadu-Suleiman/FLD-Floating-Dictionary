package com.meta4projects.fldfloatingdictionary.others

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.activities.MainActivity
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase.Companion.getINSTANCE

class WordSuggestionWork(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val wordSuggestion = getINSTANCE(applicationContext).entryWordsDao().getRandomWord()
        val wordIntent = Intent(applicationContext, MainActivity::class.java).putExtra(SUGGESTION_EXTRA, wordSuggestion).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, wordIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            val notificationChannel = NotificationChannel(SUGGESTION_CHANNEL_ID, "Word Suggestion", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(notificationChannel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val wordNotification = NotificationCompat.Builder(applicationContext, SUGGESTION_CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setLargeIcon(BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_flat_fld_logo)).setContentTitle("Word Suggestion: $wordSuggestion").setContentText("click to view attributes").setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setAutoCancel(true).setOnlyAlertOnce(true).setContentIntent(pendingIntent).build()
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(SUGGESTION_NOTIFICATION_ID, wordNotification)
            return Result.success()
        }
        return Result.failure()
    }

    companion object {
        const val SUGGESTION_NOTIFICATION_ID = 980
        const val SUGGESTION_CHANNEL_ID = "suggestion_channel_id"
        const val SUGGESTION_EXTRA = "com.meta4projects.fldfloatingdictionary.others.Extra"
    }
}