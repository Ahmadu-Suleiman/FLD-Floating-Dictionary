package com.meta4projects.fldfloatingdictionary.others

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.appopen.AppOpenAd
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.room.database.BookmarkDatabase
import java.util.Date

class MyApplication : Application(), ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var appOpenAdManager: AppOpenAdManager? = null
    private var currentActivity: Activity? = null

    override fun onCreate() {
        BookmarkDatabase.getINSTANCE(this)
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)
        MobileAds.initialize(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        appOpenAdManager?.showAdIfAvailable(currentActivity!!)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        if (!appOpenAdManager!!.isShowingAd) currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private class AppOpenAdManager(context: Context) {
        private val adUnitId: String
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false
        private var loadTime: Long = 0

        init {
            adUnitId = context.resources.getString(R.string.app_open_ad)
        }

        private fun loadAd(context: Context) {
            if (isLoadingAd || isAdAvailable) return
            isLoadingAd = true
            val request = AdManagerAdRequest.Builder().build()
            AppOpenAd.load(context, adUnitId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                }
            })
        }

        private fun wasLoadTimeLessThan4HoursAgo(): Boolean {
            val dateDifference = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * 4L
        }

        private val isAdAvailable: Boolean
            get() = appOpenAd != null && wasLoadTimeLessThan4HoursAgo()

        fun showAdIfAvailable(activity: Activity,
                              onShowAdCompleteListener: OnShowAdCompleteListener = object :
                                  OnShowAdCompleteListener {
                                  override fun onShowAdComplete() {}
                              }) {
            if (isShowingAd) return
            if (!isAdAvailable) {
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity)
                return
            }
            appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    onShowAdCompleteListener.onShowAdComplete()
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {}
            }
            isShowingAd = true
            appOpenAd!!.show(activity)
        }
    }
}