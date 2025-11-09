package com.example.getsms.credit;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusInitListener;
import ir.tapsell.plus.model.AdNetworkError;
import ir.tapsell.plus.model.AdNetworks;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

/**
 * Manages ad integration for earning credits
 * Supports both Google AdMob and Iranian ad networks (Tapsell, AdNegah)
 */
public class AdsManager {

    private static final String TAG = "AdsManager";

    // Google AdMob IDs (Replace with your actual IDs)
    private static final String ADMOB_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"; // Test ID

    // Iranian Ad Networks (Tapsell example)
    // Add Tapsell SDK to your build.gradle first
    // implementation 'ir.tapsell.plus:tapsell-plus-sdk-android:2.1.8'

    private final Context context;
    private final CreditManager creditManager;

    // AdMob
    private RewardedAd rewardedAd;
    private boolean isAdLoading = false;

    // Callbacks
    public interface AdRewardCallback {
        void onRewarded(int credits);
        void onAdFailed(String error);
    }

    public AdsManager(Context context, CreditManager creditManager) {
        this.context = context.getApplicationContext();
        this.creditManager = creditManager;
    }

    /**
     * Initialize ad networks
     */
    public void initialize() {
        // Initialize Google AdMob
//        initializeAdMob();

        // Initialize Iranian ad networks (example: Tapsell)
         initializeTapsell();
    }

    /**
     * Initialize Google AdMob
     */
    private void initializeAdMob() {
        // AdMob is initialized automatically when you add the dependency
        // Just load an ad
        loadAdMobRewardedAd();
    }

    /**
     * Load AdMob rewarded ad
     */
    public void loadAdMobRewardedAd() {
        if (isAdLoading) {
            Log.d(TAG, "Ad is already loading");
            return;
        }

        isAdLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, ADMOB_REWARDED_AD_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "AdMob ad failed to load: " + loadAdError.getMessage());
                rewardedAd = null;
                isAdLoading = false;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                Log.d(TAG, "AdMob ad loaded successfully");
                rewardedAd = ad;
                isAdLoading = false;
            }
        });
    }

    /**
     * Show AdMob rewarded ad
     */
    public void showAdMobRewardedAd(Activity activity, AdRewardCallback callback) {
        if (rewardedAd == null) {
            Log.e(TAG, "AdMob ad not ready");
            callback.onAdFailed("Ad not ready. Please wait...");
            loadAdMobRewardedAd(); // Load for next time
            return;
        }

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed");
                rewardedAd = null;
                loadAdMobRewardedAd(); // Load next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                rewardedAd = null;
                callback.onAdFailed("Failed to show ad");
                loadAdMobRewardedAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content");
            }
        });

        rewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                Log.d(TAG, "User earned reward: " + rewardItem.getAmount());

                // Reward user with credits
                creditManager.rewardForAdView(new CreditManager.CreditCallback() {
                    @Override
                    public void onSuccess(int credits) {
                        callback.onRewarded(CreditManager.REWARD_PER_AD_VIEW);
                    }

                    @Override
                    public void onError(String error) {
                        // Still give local credits even if sync fails
                        callback.onRewarded(CreditManager.REWARD_PER_AD_VIEW);
                    }
                });
            }
        });
    }

    /**
     * Check if AdMob ad is ready
     */
    public boolean isAdMobAdReady() {
        return rewardedAd != null;
    }

    /**
     * Initialize Tapsell (Iranian Ad Network)
     * Uncomment and configure if you want to use Tapsell
     */
    private void initializeTapsell() {
        String TAPSELL_KEY = "btnpladcfjlcbrtonjjdrrrhmrgjbithlbctopljntoneqssspfsbenjlrjbbcoeihkshd";
        TapsellPlus.initialize(context, TAPSELL_KEY,
                new TapsellPlusInitListener() {
                    @Override
                    public void onInitializeSuccess(AdNetworks adNetworks) {
                        Log.d("onInitializeSuccess", adNetworks.name());
                    }

                    @Override
                    public void onInitializeFailed(AdNetworks adNetworks,
                                                   AdNetworkError adNetworkError) {
                        Log.e("onInitializeFailed", "ad network: " + adNetworks.name() + ", error: " +	adNetworkError.getErrorMessage());
                    }
                });
    }


    /**
     * Show any available ad (tries AdMob first, then fallback to Iranian networks)
     */
    public void showRewardedAd(Activity activity, AdRewardCallback callback) {
        if (isAdMobAdReady()) {
            showAdMobRewardedAd(activity, callback);
        } else {
            // Fallback to Iranian ad networks if needed
            // showTapsellRewardedAd(activity, callback);
            callback.onAdFailed("No ads available. Please try again later.");
            loadAdMobRewardedAd(); // Load for next time
        }
    }

    /**
     * Preload ads for faster display
     */
    public void preloadAds() {
        if (!isAdMobAdReady() && !isAdLoading) {
            loadAdMobRewardedAd();
        }
        // Preload other ad networks if configured
    }
}