package com.meta4projects.fldfloatingdictionary.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.meta4projects.fldfloatingdictionary.DictionaryService;
import com.meta4projects.fldfloatingdictionary.R;

import java.util.Timer;
import java.util.TimerTask;

import static com.meta4projects.fldfloatingdictionary.DictionaryService.isDictionaryRunning;
import static com.meta4projects.fldfloatingdictionary.DictionaryService.startDictionaryService;
import static com.meta4projects.fldfloatingdictionary.DictionaryService.stopDictionaryService;

public class MainActivity extends AppCompatActivity {
    
    private final int PERMISSION_CODE = 1;
    
    private SwitchCompat switchView;
    private TextView textViewStatus;
    private ProgressBar progressBar;
    
    private int counter = 20;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        MobileAds.initialize(this);
        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.native_ad_main_id))
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        NativeAdView nativeAdView = (NativeAdView) getLayoutInflater().inflate(R.layout.layout_native_ad, null);
                        mapUnifiedNativeAdToLayout(nativeAd, nativeAdView);
                        
                        CardView layoutAdContainer = findViewById(R.id.ad_container);
                        layoutAdContainer.removeAllViews();
                        layoutAdContainer.addView(nativeAdView);
                    }
                })
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
        
        switchView = findViewById(R.id.switchView);
        textViewStatus = findViewById(R.id.textViewStatus);
        progressBar = findViewById(R.id.progressBar);
        
        enable();
        checkStatus();
        
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (canDrawOverApps()) {
                        textViewStatus.setText(getString(R.string.activated));
                        startProgress();
                        startDictionaryService(MainActivity.this);
                    } else {
                        getPermission();
                    }
                } else {
                    textViewStatus.setText(getString(R.string.not_activated));
                    progressBar.setVisibility(View.GONE);
                    counter = 20;
                    stopDictionaryService(MainActivity.this);
                }
            }
        });
    }
    
    private void enable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            PackageManager packageManager = getPackageManager();
            
            if (packageManager.resolveActivity(intent, 0) == null) {
                switchView.setEnabled(false);
            }
        }
    }
    
    private void checkStatus() {
        if (isDictionaryRunning(DictionaryService.class, this)) {
            switchView.setChecked(true);
            textViewStatus.setText(getString(R.string.activated));
        } else {
            switchView.setChecked(false);
            textViewStatus.setText(getString(R.string.not_activated));
        }
    }
    
    private void startProgress() {
        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                counter++;
                progressBar.setProgress(counter);
                
                if (counter == 100) {
                    timer.cancel();
                }
            }
        };
        
        progressBar.setVisibility(View.VISIBLE);
        timer.schedule(timerTask, 0, 50);
    }
    
    private void mapUnifiedNativeAdToLayout(NativeAd adFromGoogle, NativeAdView adView) {
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        adView.setMediaView(mediaView);
        
        adView.setHeadlineView(adView.findViewById(R.id.ad_head_line));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_rating));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        
        ((TextView) adView.getHeadlineView()).setText(adFromGoogle.getHeadline());
        
        if (adFromGoogle.getBody() == null) {
            adView.getBodyView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getBodyView()).setText(adFromGoogle.getBody());
        }
        
        if (adFromGoogle.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.GONE);
        } else {
            ((Button) adView.getCallToActionView()).setText(adFromGoogle.getCallToAction());
        }
        
        if (adFromGoogle.getIcon() == null) {
            adView.getIconView().setVisibility(View.INVISIBLE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(adFromGoogle.getIcon().getDrawable());
        }
        
        if (adFromGoogle.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getPriceView()).setText(adFromGoogle.getPrice());
        }
        
        if (adFromGoogle.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.GONE);
        } else {
            ((RatingBar) adView.getStarRatingView()).setRating(adFromGoogle.getStarRating().floatValue());
        }
        
        if (adFromGoogle.getStore() == null) {
            adView.getStoreView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getStoreView()).setText(adFromGoogle.getStore());
        }
        
        if (adFromGoogle.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.GONE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(adFromGoogle.getAdvertiser());
        }
        
        if (adFromGoogle.getMediaContent() == null) {
            adView.getMediaView().setVisibility(View.GONE);
        } else {
            adView.getMediaView().setMediaContent(adFromGoogle.getMediaContent());
        }
        
        adView.setNativeAd(adFromGoogle);
    }
    
    private boolean canDrawOverApps() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else {
            return Settings.canDrawOverlays(this);
        }
    }
    
    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PERMISSION_CODE);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showAboutDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_about, (ViewGroup) findViewById(R.id.about_dialog), false);
        
        final AlertDialog dialogAbout = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        
        if (dialogAbout.getWindow() != null) {
            dialogAbout.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        
        view.findViewById(R.id.textView_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogAbout.dismiss();
            }
        });
        
        dialogAbout.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_CODE) {
            checkStatus();
        }
    }
}
