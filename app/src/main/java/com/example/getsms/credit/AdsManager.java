package com.example.getsms.credit;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import ir.tapsell.plus.AdRequestCallback;
import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusInitListener;
import ir.tapsell.plus.model.AdNetworkError;
import ir.tapsell.plus.model.AdNetworks;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

/**
 * Manages ad integration for earning credits
 * Supports both Google AdMob and Tapsell Plus (Iranian ad network)
 */
public class AdsManager {

    private static final String TAG = "AdsManager";

    // Google AdMob IDs (Replace with your actual IDs)
    private static final String ADMOB_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"; // Test ID

    // Tapsell Plus Configuration (Replace with your actual keys)
    private static final String TAPSELL_APP_KEY = "btnpladcfjlcbrtonjjdrrrhmrgjbithlbctopljntoneqssspfsbenjlrjbbcoeihkshd";
    private static final String TAPSELL_REWARDED_ZONE_ID = "69109980c424f72658802fe4";

    private final Context context;
    private final CreditManager creditManager;

    // AdMob
    private RewardedAd rewardedAd;
    private boolean isAdMobLoading = false;

    // Tapsell
    private String tapsellResponseId;
    private boolean isTapsellLoading = false;
    private boolean isTapsellInitialized = false;

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
        initializeAdMob();

        // Initialize Tapsell Plus
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
     * Initialize Tapsell Plus
     */
    private void initializeTapsell() {
        TapsellPlus.initialize(context, TAPSELL_APP_KEY, new TapsellPlusInitListener() {
            @Override
            public void onInitializeSuccess(AdNetworks adNetworks) {
                Log.d(TAG, "Tapsell initialized successfully with: " + adNetworks.name());
                isTapsellInitialized = true;
                // Preload Tapsell ad
                loadTapsellRewardedAd();
            }

            @Override
            public void onInitializeFailed(AdNetworks adNetworks, AdNetworkError adNetworkError) {
                Log.e(TAG, "Tapsell initialization failed: " + adNetworkError.getErrorMessage());
                isTapsellInitialized = false;
            }
        });
    }

    /**
     * Load AdMob rewarded ad
     */
    public void loadAdMobRewardedAd() {
        if (isAdMobLoading) {
            Log.d(TAG, "AdMob ad is already loading");
            return;
        }

        isAdMobLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, ADMOB_REWARDED_AD_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "AdMob ad failed to load: " + loadAdError.getMessage());
                rewardedAd = null;
                isAdMobLoading = false;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                Log.d(TAG, "AdMob ad loaded successfully");
                rewardedAd = ad;
                isAdMobLoading = false;
            }
        });
    }

    /**
     * Load Tapsell rewarded ad
     */
    public void loadTapsellRewardedAd() {
        if (!isTapsellInitialized) {
            Log.e(TAG, "Tapsell not initialized yet");
            return;
        }

        if (isTapsellLoading) {
            Log.d(TAG, "Tapsell ad is already loading");
            return;
        }

        isTapsellLoading = true;

        TapsellPlus.requestRewardedVideoAd((Activity) context, TAPSELL_REWARDED_ZONE_ID, new AdRequestCallback() {
            @Override
            public void response(TapsellPlusAdModel tapsellPlusAdModel) {
                Log.d(TAG, "Tapsell ad loaded successfully");
                tapsellResponseId = tapsellPlusAdModel.getResponseId();
                isTapsellLoading = false;
            }

            @Override
            public void error(String errorMessage) {
                Log.e(TAG, "Tapsell ad failed to load: " + errorMessage);
                tapsellResponseId = null;
                isTapsellLoading = false;
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
                Log.d(TAG, "AdMob ad was dismissed");
                rewardedAd = null;
                loadAdMobRewardedAd(); // Load next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                Log.e(TAG, "AdMob ad failed to show: " + adError.getMessage());
                rewardedAd = null;
                callback.onAdFailed("Failed to show ad");
                loadAdMobRewardedAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "AdMob ad showed fullscreen content");
            }
        });

        rewardedAd.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull com.google.android.gms.ads.rewarded.RewardItem rewardItem) {
                Log.d(TAG, "AdMob user earned reward: " + rewardItem.getAmount());

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
     * Show Tapsell rewarded ad
     */
    public void showTapsellRewardedAd(Activity activity, AdRewardCallback callback) {
        if (tapsellResponseId == null) {
            Log.e(TAG, "Tapsell ad not ready");
            callback.onAdFailed("Ad not ready. Please wait...");
            loadTapsellRewardedAd(); // Load for next time
            return;
        }

        TapsellPlus.showRewardedVideoAd(activity, tapsellResponseId, new AdShowListener() {
            @Override
            public void onOpened(TapsellPlusAdModel tapsellPlusAdModel) {
                Log.d(TAG, "Tapsell ad opened");
            }

            @Override
            public void onClosed(TapsellPlusAdModel tapsellPlusAdModel) {
                Log.d(TAG, "Tapsell ad closed");
                tapsellResponseId = null;
                loadTapsellRewardedAd(); // Load next ad
            }

            @Override
            public void onRewarded(TapsellPlusAdModel tapsellPlusAdModel) {
                Log.d(TAG, "Tapsell user earned reward");

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

            @Override
            public void onError(TapsellPlusErrorModel tapsellPlusErrorModel) {
                Log.e(TAG, "Tapsell ad error: " + tapsellPlusErrorModel.getErrorMessage());
                callback.onAdFailed("Failed to show ad: " + tapsellPlusErrorModel.getErrorMessage());
                tapsellResponseId = null;
                loadTapsellRewardedAd();
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
     * Check if Tapsell ad is ready
     */
    public boolean isTapsellAdReady() {
        return tapsellResponseId != null;
    }

    /**
     * Check if any ad is ready
     */
    public boolean isAnyAdReady() {
        return isAdMobAdReady() || isTapsellAdReady();
    }

    /**
     * Show any available ad (tries AdMob first, then fallback to Tapsell)
     */
    public void showRewardedAd(Activity activity, AdRewardCallback callback) {
        if (isAdMobAdReady()) {
            Log.d(TAG, "Showing AdMob ad");
            showAdMobRewardedAd(activity, callback);
        } else if (isTapsellAdReady()) {
            Log.d(TAG, "Showing Tapsell ad");
            showTapsellRewardedAd(activity, callback);
        } else {
            Log.e(TAG, "No ads available");
            callback.onAdFailed("No ads available. Please try again later.");
            // Try to load both
            loadAdMobRewardedAd();
            loadTapsellRewardedAd();
        }
    }

    /**
     * Preload ads for faster display
     */
    public void preloadAds() {
        // Load AdMob ad if not ready and not loading
        if (!isAdMobAdReady() && !isAdMobLoading) {
            loadAdMobRewardedAd();
        }

        // Load Tapsell ad if not ready and not loading
        if (!isTapsellAdReady() && !isTapsellLoading && isTapsellInitialized) {
            loadTapsellRewardedAd();
        }
    }

    /**
     * Get ad network status for debugging
     */
    public String getAdNetworkStatus() {
        StringBuilder status = new StringBuilder();
        status.append("AdMob: ");
        if (isAdMobAdReady()) {
            status.append("Ready");
        } else if (isAdMobLoading) {
            status.append("Loading...");
        } else {
            status.append("Not Ready");
        }

        status.append(" | Tapsell: ");
        if (isTapsellAdReady()) {
            status.append("Ready");
        } else if (isTapsellLoading) {
            status.append("Loading...");
        } else if (!isTapsellInitialized) {
            status.append("Not Initialized");
        } else {
            status.append("Not Ready");
        }

        return status.toString();
    }
}