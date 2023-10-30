package com.meta4projects.fldfloatingdictionary.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ShareCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.fragments.MainFragment
import com.meta4projects.fldfloatingdictionary.others.Util
import com.meta4projects.fldfloatingdictionary.others.Util.WORD_EXTRA
import com.meta4projects.fldfloatingdictionary.others.Util.getDialogView
import com.meta4projects.fldfloatingdictionary.others.Util.getDictionaryFragment
import com.meta4projects.fldfloatingdictionary.others.Util.initialiseTTS
import com.meta4projects.fldfloatingdictionary.others.Util.isFirstTime
import com.meta4projects.fldfloatingdictionary.others.Util.isNightMode
import com.meta4projects.fldfloatingdictionary.others.Util.prepareWordWorker
import com.meta4projects.fldfloatingdictionary.others.Util.showToast
import com.meta4projects.fldfloatingdictionary.others.WordSuggestionWork
import com.meta4projects.fldfloatingdictionary.services.DictionaryService
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private var reviewInfo: ReviewInfo? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var layout: LinearLayout
    private lateinit var toggle: ImageView
    private lateinit var navigationView: NavigationView
    private lateinit var drawerSwitch: SwitchCompat
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var dialogAbout: AlertDialog? = null
    private var interstitialAdBookmark: InterstitialAd? = null
    private lateinit var consentInformation: ConsentInformation
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        launchFloatingDictionary()
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)
        consentForAds()
        setUp()
        drawerLayout = findViewById(R.id.drawer_layout)
        layout = findViewById(R.id.layout_main)
        toggle = findViewById(R.id.nav_toggle)
        navigationView = findViewById(R.id.nav_view)
        drawerSwitch = navigationView.menu.findItem(R.id.nav_activate).actionView!!.findViewById(R.id.switch_id)
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { checkStatus() }
        initialise()
        enable()
        setFragment(MainFragment(), false)
        onNewIntent(intent)
        processText(intent)
        loadBookmarkInterstitialAd()
        updateAndReview()
        if (isFirstTime(this)) showTutorial()
    }

    private fun consentForAds() {
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(this, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                if (loadAndShowError != null) {
                    Log.w(Util.TAG, String.format("%s: %s", loadAndShowError.errorCode, loadAndShowError.message))
                }

                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk()
                }
            }
        }, { requestConsentError ->
            Log.w(Util.TAG, String.format("%s: %s", requestConsentError.errorCode, requestConsentError.message))
        })

        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.get()) {
            return
        }
        isMobileAdsInitializeCalled.set(true)

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(this)
    }

    private fun updateAndReview() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val reviewManager = ReviewManagerFactory.create(this)

        //update
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo: AppUpdateInfo -> if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).setAllowAssetPackDeletion(true).build()) else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).setAllowAssetPackDeletion(true).build()) }

        //review
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo?> -> if (task.isSuccessful) reviewInfo = task.result }
        if (reviewInfo != null) reviewManager.launchReviewFlow(this, reviewInfo!!)
    }

    private fun launchFloatingDictionary() {
        if ("launch" == intent.action) {
            if (canDrawOverApps()) DictionaryService.startDictionaryService(this@MainActivity) else showToast("permission not granted", this)
            finish()
        }
    }

    private fun setUp() {
        initialiseTTS(this)
    }

    private fun initialise() {
        drawerLayout.setScrimColor(Color.TRANSPARENT)
        drawerLayout.addDrawerListener(object :
                                           ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                val scaleFactor = 6f
                val slideX = drawerView.width * slideOffset
                layout.translationX = slideX
                layout.scaleX = 1 - slideOffset / scaleFactor
                layout.scaleY = 1 - slideOffset / scaleFactor
            }
        })
        toggle.setOnClickListener { if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START, true) else drawerLayout.openDrawer(GravityCompat.START, true) }
        drawerSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> if (isChecked) if (canDrawOverApps()) activate() else getPermission() else deActivate() }
        navigationView.setNavigationItemSelectedListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.nav_activate) {
                if (drawerSwitch.isChecked) {
                    deActivate()
                } else {
                    if (canDrawOverApps()) activate() else getPermission()
                }
            } else {
                if (id == R.id.night_mode) setMode() else if (id == R.id.bookmarks) showBookmark() else if (id == R.id.about) showAboutDialog() else if (id == R.id.tutorial) showTutorial() else if (id == R.id.apps) showApps() else if (id == R.id.rate) rate() else if (id == R.id.share) share()
                drawerLayout.closeDrawer(GravityCompat.START, true)
            }
            true
        }
        navigationView.menu.getItem(5).setActionView(R.layout.menu_image_ad)
    }

    private fun setFragment(fragment: Fragment, animate: Boolean) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top).addToBackStack(null).setReorderingAllowed(true)
            fragmentTransaction.add(R.id.layout_container, fragment).commit()
        } else fragmentTransaction.replace(R.id.layout_container, fragment).commit()
    }

    private fun loadBookmarkInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.interstitial_bookmark), adRequest, object :
            InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialAdBookmark = interstitialAd
                interstitialAdBookmark!!.fullScreenContentCallback = object :
                    FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        startActivity(Intent(this@MainActivity, BookmarkActivity::class.java))
                        loadBookmarkInterstitialAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        startActivity(Intent(this@MainActivity, BookmarkActivity::class.java))
                        loadBookmarkInterstitialAd()
                    }

                    override fun onAdShowedFullScreenContent() {
                        interstitialAdBookmark = null
                        loadBookmarkInterstitialAd()
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                interstitialAdBookmark = null
            }
        })
    }

    private fun showBookmark() {
        if (interstitialAdBookmark != null) interstitialAdBookmark!!.show(this) else startActivity(Intent(this@MainActivity, BookmarkActivity::class.java))
    }

    fun setDictionaryFragment(word: String?) {
        if (!word.isNullOrBlank()) setFragment(getDictionaryFragment(word), true)
    }

    private fun checkStatus() {
        drawerSwitch.isChecked = DictionaryService.isDictionaryRunning(DictionaryService::class.java, this@MainActivity)
    }

    private fun processText(intent: Intent) {
        val word = if (intent.action == Intent.ACTION_PROCESS_TEXT) intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) else ""
        setDictionaryFragment(word)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setUp()
        var word = if (intent.getStringExtra(WordSuggestionWork.SUGGESTION_EXTRA) != null) intent.getStringExtra(WordSuggestionWork.SUGGESTION_EXTRA) else ""
        if (intent.getStringExtra(WORD_EXTRA) != null) word = intent.getStringExtra(WORD_EXTRA) else if ("text/plain" == intent.type) word = intent.getStringExtra(Intent.EXTRA_TEXT)
        setDictionaryFragment(word)
        prepareWordWorker(this)
    }

    private fun enable() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            val packageManager = packageManager
            if (packageManager.resolveActivity(intent, 0) == null) drawerSwitch.isEnabled = false
        }
    }

    private fun activate() {
        DictionaryService.startDictionaryService(this)
        checkStatus()
    }

    private fun deActivate() {
        try {
            DictionaryService.stopDictionaryService(this@MainActivity)
            checkStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun canDrawOverApps(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun getPermission() {
        if (!Settings.canDrawOverlays(this)) launcher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun setMode() {
        if (isNightMode(this)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        DictionaryService.stopDictionaryService(this)
        checkStatus()
    }

    private fun showAboutDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_about, findViewById(R.id.about_dialog), false)
        val dialogAbout = getDialogView(this, view)
        dialogAbout.show()
    }

    private fun showTutorial() {
        val view = LayoutInflater.from(this).inflate(R.layout.layout_tutorial, findViewById(R.id.tutorial_dialog), false)
        dialogAbout = getDialogView(this, view)
        view.findViewById<View>(R.id.text_dismiss).setOnClickListener { dialogAbout!!.dismiss() }
        dialogAbout!!.show()
    }

    private fun showApps() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=5382562347439530585")))
    }

    private fun rate() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun share() {
        val message = "I'm recommending this dictionary to you. It's the most convenient dictionary i've used http://play.google.com/store/apps/details?id=$packageName"
        ShareCompat.IntentBuilder(this).setType("text/plain").setSubject("FLD Floating Dictionary").setChooserTitle("share using...").setText(message).startChooser()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (dialogAbout != null && dialogAbout!!.isShowing) {
            dialogAbout!!.dismiss()
        }
    }
}