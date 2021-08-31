package com.meta4projects.fldfloatingdictionary.fragments;

import static com.meta4projects.fldfloatingdictionary.services.DictionaryService.isDictionaryRunning;
import static com.meta4projects.fldfloatingdictionary.services.DictionaryService.startDictionaryService;
import static com.meta4projects.fldfloatingdictionary.services.DictionaryService.stopDictionaryService;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.activities.MainActivity;
import com.meta4projects.fldfloatingdictionary.others.EntryUtil;
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase;
import com.meta4projects.fldfloatingdictionary.services.DictionaryService;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainFragment extends Fragment {

    private InterstitialAd interstitialAd;
    private RadioGroup switchView;
    private SwitchCompat drawerSwitch;
    private RadioButton buttonActivate;
    private RadioButton buttonDeactivate;
    private TextView textViewStatus;
    private LinearLayout layoutRandom;
    private ReviewInfo reviewInfo;
    private ReviewManager reviewManager;
    private ActivityResultLauncher<Intent> launcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        switchView = view.findViewById(R.id.switchView);
        buttonActivate = view.findViewById(R.id.activate);
        buttonDeactivate = view.findViewById(R.id.deactivate);

        textViewStatus = view.findViewById(R.id.textViewStatus);

        NavigationView navigationView = requireActivity().findViewById(R.id.nav_view);
        drawerSwitch = navigationView.getMenu().findItem(R.id.nav_activate).getActionView().findViewById(R.id.switch_id);
        layoutRandom = view.findViewById(R.id.layout_random_container);

        setRandomEntry(view);
        loadAds(view);
        enable();

        drawerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> activateOrDeactivate(isChecked));
        switchView.setOnCheckedChangeListener((group, checkedId) -> activateOrDeactivate(checkedId == R.id.activate));
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> checkStatus());

        reviewManager = ReviewManagerFactory.create(requireContext());
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) reviewInfo = task.getResult();
        });
        return view;
    }

    private void setRandomEntry(View view) {
        TextView textViewLoading = view.findViewById(R.id.textView_loading);
        LinearLayout layoutRandomEntry = view.findViewById(R.id.layout_random_entry);
        Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(requireContext()).entryWordsDao().getRandomWord()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(word -> {
            Single.fromCallable(() -> DictionaryDatabase.getINSTANCE(requireContext()).entryDao().getAllEntriesForWord(word)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(entries -> EntryUtil.setEntry(entries, layoutRandomEntry, textViewLoading, requireContext())).subscribe();
            layoutRandomEntry.setOnClickListener(v -> ((MainActivity) requireActivity()).setDictionaryFragment(word));
            view.findViewById(R.id.textViewFullscreen).setOnClickListener(v -> ((MainActivity) requireActivity()).setDictionaryFragment(word));
            view.findViewById(R.id.note_layout).setOnClickListener(v -> showTakeNoteApp());
        }).subscribe();
    }

    private void activateOrDeactivate(boolean activate) {
        if (activate) {
            if (canDrawOverApps()) activateWithAd();
            else getPermission();
        } else {
            deActivate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkStatus();
        loadInterstitial();
    }

    private void loadAds(View view) {
        loadNativeAd(view);
        loadInterstitial();
    }

    private void loadNativeAd(View view) {
        TemplateView templateView = view.findViewById(R.id.native_ad);
        templateView.setVisibility(View.GONE);

        AdLoader adLoader = new AdLoader.Builder(requireContext(), getString(R.string.native_ad_main)).forNativeAd(nativeAd -> {
            templateView.setNativeAd(nativeAd);
            if (requireActivity().isDestroyed()) nativeAd.destroy();
        }).withAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                templateView.setVisibility(View.VISIBLE);
                layoutRandom.setVisibility(View.GONE);
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(requireContext(), getString(R.string.interstitial_switch), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAd = null;
                loadInterstitial();
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitial) {
                interstitialAd = interstitial;
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        activate();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        activate();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        interstitialAd = null;
                    }
                });
            }
        });
    }

    private void enable() {
        if (!Settings.canDrawOverlays(requireContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + requireContext().getPackageName()));
            PackageManager packageManager = requireContext().getPackageManager();

            if (packageManager.resolveActivity(intent, 0) == null) {
                switchView.setEnabled(false);
                drawerSwitch.setEnabled(false);
            }
        }
    }

    private void activate() {
        startDictionaryService(requireContext());
        checkStatus();
        loadInterstitial();
    }

    private void activateWithAd() {
        if (interstitialAd != null) interstitialAd.show(requireActivity());
        else activate();
    }

    private void deActivate() {
        try {
            stopDictionaryService(requireContext());
            checkStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkStatus() {
        if (isDictionaryRunning(DictionaryService.class, requireContext())) {
            buttonActivate.setChecked(true);
            drawerSwitch.setChecked(true);
            textViewStatus.setText(getString(R.string.activated_info));
            buttonActivate.setText(R.string.activated);
            buttonDeactivate.setText(R.string.deactivate);
        } else {
            buttonDeactivate.setChecked(true);
            drawerSwitch.setChecked(false);
            textViewStatus.setText(getString(R.string.not_activated_info));
            buttonDeactivate.setText(R.string.deactivated);
            buttonActivate.setText(R.string.activate);
        }
        review();
    }

    private void showTakeNoteApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.meta4projects.noteapp")));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.meta4projects.noteapp")));
        }
    }

    private boolean canDrawOverApps() {
        return Settings.canDrawOverlays(requireContext());
    }

    private void getPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + requireContext().getPackageName()));
            launcher.launch(intent);
        }
    }

    private void review() {
        if (reviewInfo != null) reviewManager.launchReviewFlow(requireActivity(), reviewInfo);
    }
}