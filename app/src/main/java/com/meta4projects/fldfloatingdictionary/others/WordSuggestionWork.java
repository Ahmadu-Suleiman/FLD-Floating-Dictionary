package com.meta4projects.fldfloatingdictionary.others;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.activities.MainActivity;
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase;

public class WordSuggestionWork extends Worker {

    public static final int SUGGESTION_NOTIFICATION_ID = 980;
    public static final String SUGGESTION_CHANNEL_ID = "suggestion_channel_id";
    public static final String SUGGESTION_EXTRA = "com.meta4projects.fldfloatingdictionary.others.Extra";

    public WordSuggestionWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String wordSuggestion = DictionaryDatabase.getINSTANCE(getApplicationContext()).entryWordsDao().getRandomWord();
        Intent wordIntent = new Intent(getApplicationContext(), MainActivity.class).putExtra(SUGGESTION_EXTRA, wordSuggestion).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, wordIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            NotificationChannel notificationChannel = new NotificationChannel(SUGGESTION_CHANNEL_ID, "Word Suggestion", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(notificationChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Notification wordNotification = new NotificationCompat.Builder(getApplicationContext(), SUGGESTION_CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_flat_fld_logo)).setContentTitle("Word Suggestion: ".concat(wordSuggestion)).setContentText("click to view attributes").setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setAutoCancel(true).setOnlyAlertOnce(true).setContentIntent(pendingIntent).build();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(getApplicationContext()).notify(SUGGESTION_NOTIFICATION_ID, wordNotification);
            return Result.success();
        }
        return Result.failure();
    }
}
