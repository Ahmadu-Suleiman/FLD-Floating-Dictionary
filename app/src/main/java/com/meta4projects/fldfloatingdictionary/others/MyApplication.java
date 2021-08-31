package com.meta4projects.fldfloatingdictionary.others;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback;
import com.meta4projects.fldfloatingdictionary.R;

import java.util.Date;

public class MyApplication extends Application implements ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private AppOpenAdManager appOpenAdManager;
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        MobileAds.initialize(this);

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdManager = new AppOpenAdManager(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        appOpenAdManager.showAdIfAvailable(currentActivity);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!appOpenAdManager.isShowingAd) currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    private static class AppOpenAdManager {
        private final String AD_UNIT_ID;

        private AppOpenAd appOpenAd = null;
        private boolean isLoadingAd = false;
        private boolean isShowingAd = false;
        private long loadTime = 0;

        public AppOpenAdManager(Context context) {
            AD_UNIT_ID = context.getResources().getString(R.string.app_open_ad);
        }

        private void loadAd(Context context) {
            if (isLoadingAd || isAdAvailable()) return;

            isLoadingAd = true;
            AdManagerAdRequest request = new AdManagerAdRequest.Builder().build();
            AppOpenAd.load(context, AD_UNIT_ID, request, new AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    appOpenAd = ad;
                    isLoadingAd = false;
                    loadTime = (new Date()).getTime();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    isLoadingAd = false;
                }
            });
        }

        private boolean wasLoadTimeLessThan4HoursAgo() {
            long dateDifference = (new Date()).getTime() - loadTime;
            long numMilliSecondsPerHour = 3600000;
            return (dateDifference < (numMilliSecondsPerHour * (long) 4));
        }

        private boolean isAdAvailable() {
            return appOpenAd != null && wasLoadTimeLessThan4HoursAgo();
        }

        private void showAdIfAvailable(@NonNull final Activity activity) {
            showAdIfAvailable(activity, () -> {
            });
        }

        private void showAdIfAvailable(@NonNull final Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
            if (isShowingAd) return;

            if (!isAdAvailable()) {
                onShowAdCompleteListener.onShowAdComplete();
                loadAd(activity);
                return;
            }

            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isShowingAd = false;

                    onShowAdCompleteListener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    appOpenAd = null;
                    isShowingAd = false;

                    onShowAdCompleteListener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                }
            });

            isShowingAd = true;
            appOpenAd.show(activity);
        }
    }
}