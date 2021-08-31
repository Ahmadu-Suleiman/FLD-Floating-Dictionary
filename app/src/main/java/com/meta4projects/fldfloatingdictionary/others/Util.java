package com.meta4projects.fldfloatingdictionary.others;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.fragments.DictionaryFragment;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Util {
    public static final String IS_FIRST_TIME = "first_time_running";
    public static final String SHARED_PREFERENCE = "shared_pref";
    public static final String WORD_EXTRA = "com.meta4projects.fldfloatingdictionary.others.Word";
    public static boolean isNightMode;
    public static TextToSpeech textToSpeech;

    @SuppressLint("InflateParams")
    public static void showToast(String text, Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null);

        TextView message = view.findViewById(R.id.textView_toast_message);
        message.setText(text);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void copyText(String text, String label, Context context) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(label, text);
        clipboardManager.setPrimaryClip(clipData);
        showToast(label.concat(" copied!"), context);
    }

    public static void hideKeyboard(Context context, View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showSoftKeyboard(Context context, View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static boolean isFirstTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean isFirstTime = sharedPreferences.getBoolean(IS_FIRST_TIME, true);

        if (isFirstTime) {
            editor.putBoolean(IS_FIRST_TIME, false);
            editor.apply();
            return true;
        } else return false;
    }

    public static boolean isNightMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        return isNightMode;
    }

    public static void initialiseTTS(Context context) {
        textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) textToSpeech.setLanguage(Locale.ENGLISH);
        });
    }

    public static void loadNativeAd(Activity activity, TemplateView templateView, String adUnitId) {
        templateView.setVisibility(View.GONE);
        AdLoader adLoader = new AdLoader.Builder(activity, adUnitId).forNativeAd(nativeAd -> {
            templateView.setNativeAd(nativeAd);
            if (activity.isDestroyed()) nativeAd.destroy();
        }).withAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                templateView.setVisibility(View.VISIBLE);
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void prepareWordWorker(Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
            PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(WordSuggestionWork.class, 6, TimeUnit.HOURS).build();
            WorkManager.getInstance(activity).enqueueUniquePeriodicWork("SuggestionWork", ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
        }
    }

    public static AlertDialog getDialogView(Context context, View view) {
        AlertDialog dialogView = new AlertDialog.Builder(context).setView(view).create();
        if (dialogView.getWindow() != null)
            dialogView.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        return dialogView;
    }

    public static DictionaryFragment getDictionaryFragment(String word) {
        DictionaryFragment dictionaryFragment = new DictionaryFragment();
        Bundle bundle = new Bundle();
        bundle.putString(WORD_EXTRA, word);
        dictionaryFragment.setArguments(bundle);
        return dictionaryFragment;
    }
}
