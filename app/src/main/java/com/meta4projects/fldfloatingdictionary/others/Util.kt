package com.meta4projects.fldfloatingdictionary.others

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.fragments.DictionaryFragment
import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.Locale
import java.util.concurrent.TimeUnit

object Util {

    const val TAG: String = "FLD TAG"
    const val WORD_EXTRA = "com.meta4projects.fldfloatingdictionary.others.Word"
    private const val IS_FIRST_TIME = "first_time_running"
    private const val NUMBER_OF_TIMES_ACTIVATED = "should_share"
    private const val SHARE_THRESHOLD = 40
    private const val SHARED_PREFERENCE = "shared_pref"
    var isNightMode = false
    var textToSpeech: TextToSpeech? = null
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    @JvmStatic
    @SuppressLint("InflateParams")
    fun showToast(text: String?, context: Context?) {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null)
        val message = view.findViewById<TextView>(R.id.textView_toast_message)
        message.text = text
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = view
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    fun copyText(text: String?, label: String, context: Context) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
        showToast("$label copied!", context)
    }

    fun hideKeyboard(context: Context, view: View?) {
        if (view != null) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun showSoftKeyboard(context: Context, view: View?) {
        if (view != null && view.requestFocus()) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    @JvmStatic
    fun isFirstTime(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val isFirstTime = sharedPreferences.getBoolean(IS_FIRST_TIME, true)
        return if (isFirstTime) {
            editor.putBoolean(IS_FIRST_TIME, false)
            editor.apply()
            true
        } else false
    }

    @JvmStatic
    fun shouldShare(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE)
        val numberOfActivated = sharedPreferences.getInt(NUMBER_OF_TIMES_ACTIVATED, 1)
        val shouldShare = numberOfActivated % SHARE_THRESHOLD == 0

        with(sharedPreferences.edit()) {
            putInt(NUMBER_OF_TIMES_ACTIVATED, numberOfActivated + 1)
            apply()
        }
        return shouldShare
    }

    @JvmStatic
    fun isNightMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        return isNightMode
    }

    @JvmStatic
    fun initialiseTTS(context: Context) {
        textToSpeech = TextToSpeech(context.applicationContext) { status: Int -> if (status == TextToSpeech.SUCCESS) textToSpeech!!.language = Locale.ENGLISH }
    }

    @JvmStatic
    fun loadNativeAd(activity: Activity, templateView: TemplateView, adUnitId: String?) {
        templateView.visibility = View.GONE
        val adLoader = AdLoader.Builder(activity, adUnitId!!).forNativeAd { nativeAd: NativeAd ->
            templateView.setNativeAd(nativeAd)
            if (activity.isDestroyed) nativeAd.destroy()
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                templateView.visibility = View.VISIBLE
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    @JvmStatic
    fun prepareWordWorker(activity: Activity?) {
        if (ContextCompat.checkSelfPermission(activity!!, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val periodicWorkRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(WordSuggestionWork::class.java, 6, TimeUnit.HOURS).build()
            WorkManager.getInstance(activity).enqueueUniquePeriodicWork("SuggestionWork", ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest)
        }
    }


    @JvmStatic
    fun getDialogView(context: Context?, view: View?): AlertDialog {
        val dialogView = AlertDialog.Builder(context!!).setView(view).create()
        if (dialogView.window != null) dialogView.window!!.setBackgroundDrawable(ColorDrawable(0))
        return dialogView
    }

    @JvmStatic
    fun getDictionaryFragment(word: String?): DictionaryFragment {
        val dictionaryFragment = DictionaryFragment()
        val bundle = Bundle()
        bundle.putString(WORD_EXTRA, word)
        dictionaryFragment.arguments = bundle
        return dictionaryFragment
    }

    @JvmStatic
    fun share(context: Context) {
        val message = "I'm recommending FLD Floating Dictionary to you. It's the most convenient dictionary i've used http://play.google.com/store/apps/details?id=$context.packageName"
        ShareCompat.IntentBuilder(context).setType("text/plain").setSubject("FLD Floating Dictionary").setChooserTitle("share using...").setText(message).startChooser()
    }
}