package com.meta4projects.fldfloatingdictionary.activities;

import static com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE;
import static com.meta4projects.fldfloatingdictionary.others.Util.WORD_EXTRA;
import static com.meta4projects.fldfloatingdictionary.others.Util.getDialogView;
import static com.meta4projects.fldfloatingdictionary.others.Util.getDictionaryFragment;
import static com.meta4projects.fldfloatingdictionary.others.Util.isNightMode;
import static com.meta4projects.fldfloatingdictionary.others.Util.showToast;
import static com.meta4projects.fldfloatingdictionary.others.WordSuggestionWork.SUGGESTION_EXTRA;
import static com.meta4projects.fldfloatingdictionary.services.DictionaryService.isDictionaryRunning;
import static com.meta4projects.fldfloatingdictionary.services.DictionaryService.startDictionaryService;
import static com.meta4projects.fldfloatingdictionary.services.DictionaryService.stopDictionaryService;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ShareCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.meta4projects.fldfloatingdictionary.R;
import com.meta4projects.fldfloatingdictionary.fragments.MainFragment;
import com.meta4projects.fldfloatingdictionary.others.Util;
import com.meta4projects.fldfloatingdictionary.room.database.DictionaryDatabase;
import com.meta4projects.fldfloatingdictionary.services.DictionaryService;

public class MainActivity extends AppCompatActivity {

    private ReviewInfo reviewInfo;
    private DrawerLayout drawerLayout;
    private LinearLayout layout;
    private ImageView toggle;
    private NavigationView navigationView;
    private SwitchCompat drawerSwitch;
    private ActivityResultLauncher<Intent> launcher;

    private InterstitialAd interstitialAdBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        launchFloatingDictionary();
        SplashScreen.installSplashScreen(this).setKeepOnScreenCondition(() -> {
            setUp();
            return false;
        });
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        layout = findViewById(R.id.layout_main);
        toggle = findViewById(R.id.nav_toggle);
        navigationView = findViewById(R.id.nav_view);
        drawerSwitch = navigationView.getMenu().findItem(R.id.nav_activate).getActionView().findViewById(R.id.switch_id);

        if (Util.isFirstTime(this)) showTutorial();
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> checkStatus());

        initialise();
        enable();
        setFragment(new MainFragment(), false);
        onNewIntent(getIntent());
        processText(getIntent());
        loadBookmarkInterstitialAd();
        updateAndReview();
    }

    private void updateAndReview() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        ReviewManager reviewManager = ReviewManagerFactory.create(this);

        //update
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE))
                appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(IMMEDIATE).setAllowAssetPackDeletion(true).build());
            else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                appUpdateManager.startUpdateFlow(appUpdateInfo, this, AppUpdateOptions.newBuilder(IMMEDIATE).setAllowAssetPackDeletion(true).build());
        });

        //review
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) reviewInfo = task.getResult();
        });
        if (reviewInfo != null) reviewManager.launchReviewFlow(this, reviewInfo);
    }

    private void launchFloatingDictionary() {
        if ("launch".equals(getIntent().getAction())) {
            if (canDrawOverApps()) startDictionaryService(MainActivity.this);
            else showToast("permission not granted", this);
            finish();
        }
    }

    private void setUp() {
        Util.initialiseTTS(this);
        AsyncTask.execute(() -> DictionaryDatabase.getINSTANCE(this).entryWordsDao().getRandomWords());
    }

    private void initialise() {
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        drawerLayout.addDrawerListener(new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);

                float scaleFactor = 6f;
                float slideX = drawerView.getWidth() * slideOffset;
                layout.setTranslationX(slideX);
                layout.setScaleX(1 - slideOffset / scaleFactor);
                layout.setScaleY(1 - slideOffset / scaleFactor);
            }
        });

        toggle.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START, true);
            else drawerLayout.openDrawer(GravityCompat.START, true);
        });

        drawerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) if (canDrawOverApps()) activate();
            else getPermission();
            else deActivate();
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_activate) {
                if (drawerSwitch.isChecked()) {
                    deActivate();
                } else {
                    if (canDrawOverApps()) activate();
                    else getPermission();
                }
            } else {
                if (id == R.id.night_mode) setMode();
                else if (id == R.id.bookmarks) showBookmark();
                else if (id == R.id.about) showAboutDialog();
                else if (id == R.id.tutorial) showTutorial();
                else if (id == R.id.apps) showApps();
                else if (id == R.id.rate) rate();
                else if (id == R.id.share) share();
                drawerLayout.closeDrawer(GravityCompat.START, true);
            }

            return true;
        });
        navigationView.getMenu().getItem(5).setActionView(R.layout.menu_image_ad);
    }

    private void setFragment(Fragment fragment, boolean animate) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (animate) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top).addToBackStack(null);
            fragmentTransaction.add(R.id.layout_container, fragment).commit();
        } else fragmentTransaction.replace(R.id.layout_container, fragment).commit();
    }

    private void loadBookmarkInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, getString(R.string.interstitial_bookmark), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                interstitialAdBookmark = interstitialAd;

                interstitialAdBookmark.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        startActivity(new Intent(MainActivity.this, BookmarkActivity.class));
                        loadBookmarkInterstitialAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        startActivity(new Intent(MainActivity.this, BookmarkActivity.class));
                        loadBookmarkInterstitialAd();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        interstitialAdBookmark = null;
                        loadBookmarkInterstitialAd();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAdBookmark = null;
            }
        });
    }

    private void showBookmark() {
        if (interstitialAdBookmark != null) interstitialAdBookmark.show(this);
        else startActivity(new Intent(MainActivity.this, BookmarkActivity.class));
    }

    public void setDictionaryFragment(String word) {
        if (word != null && !word.isBlank()) setFragment(getDictionaryFragment(word), true);
    }

    public void checkStatus() {
        drawerSwitch.setChecked(isDictionaryRunning(DictionaryService.class, MainActivity.this));
    }

    private void processText(Intent intent) {
        String word = Intent.ACTION_PROCESS_TEXT.equals(intent.getAction()) ? intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) : "";
        setDictionaryFragment(word);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setUp();

        String word = intent.getStringExtra(SUGGESTION_EXTRA) != null ? intent.getStringExtra(SUGGESTION_EXTRA) : "";
        if (intent.getStringExtra(WORD_EXTRA) != null) word = intent.getStringExtra(WORD_EXTRA);
        else if ("text/plain".equals(intent.getType()))
            word = intent.getStringExtra(Intent.EXTRA_TEXT);
        setDictionaryFragment(word);
        Util.prepareWordWorker(this);
    }

    private void enable() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            PackageManager packageManager = getPackageManager();
            if (packageManager.resolveActivity(intent, 0) == null) drawerSwitch.setEnabled(false);
        }
    }

    private void activate() {
        startDictionaryService(MainActivity.this);
        checkStatus();
    }

    private void deActivate() {
        try {
            stopDictionaryService(MainActivity.this);
            checkStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean canDrawOverApps() {
        return Settings.canDrawOverlays(this);
    }

    private void getPermission() {
        if (!Settings.canDrawOverlays(this))
            launcher.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private void setMode() {
        if (isNightMode(this))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        stopDictionaryService(this);
        checkStatus();
    }

    private void showAboutDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_about, findViewById(R.id.about_dialog), false);
        final AlertDialog dialogAbout = getDialogView(this, view);
        dialogAbout.show();
    }

    private void showTutorial() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_tutorial, findViewById(R.id.tutorial_dialog), false);
        final AlertDialog dialogAbout = getDialogView(this, view);

        view.findViewById(R.id.text_dismiss).setOnClickListener(v -> dialogAbout.dismiss());
        dialogAbout.show();
    }

    private void showApps() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=5382562347439530585")));
    }

    private void rate() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void share() {
        String message = "I'm recommending this dictionary to you. It's the most convenient dictionary i've used http://play.google.com/store/apps/details?id=" + getPackageName();
        new ShareCompat.IntentBuilder(this).setType("text/plain").setSubject("FLD Floating Dictionary").setChooserTitle("share using...").setText(message).startChooser();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START, true);
        else super.onBackPressed();
    }
}
