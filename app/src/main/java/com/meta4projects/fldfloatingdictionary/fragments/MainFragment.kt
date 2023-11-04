package com.meta4projects.fldfloatingdictionary.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.meta4projects.fldfloatingdictionary.R
import com.meta4projects.fldfloatingdictionary.activities.MainActivity
import com.meta4projects.fldfloatingdictionary.others.EntryUtil
import com.meta4projects.fldfloatingdictionary.others.Util
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase
import com.meta4projects.fldfloatingdictionary.room.entities.Entry
import com.meta4projects.fldfloatingdictionary.services.DictionaryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment(), CoroutineScope by MainScope() {

    private var interstitialAdSwitch: InterstitialAd? = null
    private var interstitialAdDictionary: InterstitialAd? = null
    private var reviewInfo: ReviewInfo? = null
    private lateinit var switchView: RadioGroup
    private lateinit var drawerSwitch: SwitchCompat
    private lateinit var buttonActivate: RadioButton
    private lateinit var buttonDeactivate: RadioButton
    private lateinit var textViewStatus: TextView
    private lateinit var layoutRandom: LinearLayout
    private lateinit var reviewManager: ReviewManager
    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        switchView = view.findViewById(R.id.switchView)
        buttonActivate = view.findViewById(R.id.activate)
        buttonDeactivate = view.findViewById(R.id.deactivate)
        textViewStatus = view.findViewById(R.id.textViewStatus)
        val navigationView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        drawerSwitch = navigationView.menu.findItem(R.id.nav_activate).actionView!!.findViewById(R.id.switch_id)
        layoutRandom = view.findViewById(R.id.layout_random_container)
        setRandomEntry(view)
        loadAds(view)
        enable()
        drawerSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            activateOrDeactivate(isChecked)
        }
        switchView.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            activateOrDeactivate(checkedId == R.id.activate)
        }
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { checkStatus() }
        reviewManager = ReviewManagerFactory.create(requireContext())
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo?> ->
            if (task.isSuccessful) reviewInfo = task.result
        }
        return view
    }

    private fun setRandomEntry(view: View) {
        val textViewLoading = view.findViewById<TextView>(R.id.textView_loading)
        val layoutRandomEntry = view.findViewById<LinearLayout>(R.id.layout_random_entry)
        val buttonFullScreen = view.findViewById<Button>(R.id.buttonFullscreen)
        val layoutNote = view.findViewById<LinearLayout>(R.id.note_layout)


        CoroutineScope(Dispatchers.IO + Util.coroutineExceptionHandler).launch {
            val randomWord = DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getRandomWord()
            val randomEntries = DictionaryDatabase.getINSTANCE(requireContext()).entryDao().getAllEntriesForWord(randomWord) as ArrayList<Entry>
            withContext(Dispatchers.Main + Util.coroutineExceptionHandler) {
                EntryUtil.setEntry(randomEntries, layoutRandomEntry, textViewLoading, requireContext())
                layoutRandomEntry.setOnClickListener { (requireActivity() as MainActivity).setDictionaryFragment(randomWord) }
                buttonFullScreen.setOnClickListener { if (interstitialAdDictionary != null) interstitialAdDictionary!!.show(requireActivity()) else launchFullscreenDictionary() }
            }
        }
        layoutNote.setOnClickListener { showTakeNoteApp() }
    }

    private fun launchFullscreenDictionary() =
        (requireActivity() as MainActivity).setDictionaryFragment("A")

    private fun activateOrDeactivate(activate: Boolean) {
        if (activate) {
            if (canDrawOverApps()) activateWithAd() else getPermission()
        } else {
            deActivate()
        }
    }

    override fun onResume() {
        super.onResume()
        checkStatus()
        loadInterstitialSwitch()
        loadInterstitialDictionary()
    }

    private fun loadAds(view: View) {
        loadNativeAd(view)
        loadInterstitialSwitch()
        loadInterstitialDictionary()
    }

    private fun loadNativeAd(view: View) {
        val templateView = view.findViewById<TemplateView>(R.id.native_ad)
        templateView.visibility = View.GONE
        val adLoader = AdLoader.Builder(requireContext(), getString(R.string.native_ad_main)).forNativeAd { nativeAd: NativeAd ->
            templateView.setNativeAd(nativeAd)
            if (isAdded && requireActivity().isDestroyed) nativeAd.destroy()
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                templateView.visibility = View.VISIBLE
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun loadInterstitialSwitch() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), getString(R.string.interstitial_switch), adRequest, object :
            InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                interstitialAdSwitch = null
                loadInterstitialSwitch()
            }

            override fun onAdLoaded(interstitial: InterstitialAd) {
                interstitialAdSwitch = interstitial
                interstitialAdSwitch!!.fullScreenContentCallback = object :
                    FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        activate()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        activate()
                    }

                    override fun onAdShowedFullScreenContent() {
                        interstitialAdSwitch = null
                    }
                }
            }
        })
    }

    private fun loadInterstitialDictionary() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), getString(R.string.interstitial_dictionary), adRequest, object :
            InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                interstitialAdDictionary = null
                loadInterstitialDictionary()
            }

            override fun onAdLoaded(interstitial: InterstitialAd) {
                interstitialAdDictionary = interstitial
                interstitialAdDictionary!!.fullScreenContentCallback = object :
                    FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        launchFullscreenDictionary()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        launchFullscreenDictionary()
                    }

                    override fun onAdShowedFullScreenContent() {
                        interstitialAdDictionary = null
                    }
                }
            }
        })
    }

    private fun enable() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + requireContext().packageName))
            val packageManager = requireContext().packageManager
            if (packageManager.resolveActivity(intent, 0) == null) {
                switchView.isEnabled = false
                drawerSwitch.isEnabled = false
            }
        }
    }

    private fun activate() {
        DictionaryService.startDictionaryService(requireContext())
        checkStatus()
        loadInterstitialSwitch()
        loadInterstitialDictionary()
    }

    private fun activateWithAd() {
        if (interstitialAdSwitch != null) interstitialAdSwitch!!.show(requireActivity()) else activate()
    }

    private fun deActivate() {
        try {
            DictionaryService.stopDictionaryService(requireContext())
            checkStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkStatus() {
        if (DictionaryService.isDictionaryRunning(DictionaryService::class.java, requireContext())) {
            buttonActivate.isChecked = true
            drawerSwitch.isChecked = true
            textViewStatus.text = getString(R.string.activated_info)
            buttonActivate.setText(R.string.activated)
            buttonDeactivate.setText(R.string.deactivate)
        } else {
            buttonDeactivate.isChecked = true
            drawerSwitch.isChecked = false
            textViewStatus.text = getString(R.string.not_activated_info)
            buttonDeactivate.setText(R.string.deactivated)
            buttonActivate.setText(R.string.activate)
        }
        review()
    }

    private fun showTakeNoteApp() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.meta4projects.noteapp")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.meta4projects.noteapp")))
        }
    }

    private fun canDrawOverApps(): Boolean {
        return Settings.canDrawOverlays(requireContext())
    }

    private fun getPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + requireContext().packageName))
            launcher.launch(intent)
        }
    }

    private fun review() {
        reviewInfo?.let { reviewManager.launchReviewFlow(requireActivity(), it) }
    }
}