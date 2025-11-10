package com.example.getsms.credit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Manages user credits for the SMS forwarding app
 * Credits are stored locally and managed client-side
 * Backend is only used for token validation and manual credit purchases
 */
public class CreditManager {

    private static final String TAG = "CreditManager";
    private static final String PREFS_NAME = "credit_prefs";
    private static final String KEY_CREDITS = "user_credits";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_TOTAL_ADS_WATCHED = "total_ads_watched";
    private static final String KEY_LAST_AD_TIME = "last_ad_time";

    // Credit costs
    public static final int COST_PER_SMS = 1;
    public static final int COST_PER_WEBHOOK = 1;
    public static final int COST_PER_TELEGRAM = 1;

    // Credit rewards
    public static final int REWARD_PER_AD_VIEW = 5;
    public static final int INITIAL_FREE_CREDITS = 20; // Free credits on first install

    // Ad watch limits (optional - prevent abuse)
    private static final int MAX_ADS_PER_DAY = 50;
    private static final long AD_COOLDOWN_MS = 30000; // 30 seconds between ads

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final OkHttpClient client;

    // Backend configuration (only for token redemption)
    private String backendUrl = "https://smsforwarder.amiriprog.ir/api/";

    public interface CreditCallback {
        void onSuccess(int credits);
        void onError(String error);
    }

    public CreditManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Give initial free credits on first launch
        initializeCredits();
    }

    /**
     * Initialize credits on first launch
     */
    private void initializeCredits() {
        if (!prefs.contains(KEY_CREDITS)) {
            prefs.edit().putInt(KEY_CREDITS, INITIAL_FREE_CREDITS).apply();
            Log.d(TAG, "First launch - Added " + INITIAL_FREE_CREDITS + " free credits");
        }
    }

    /**
     * Set backend URL
     */
    public void setBackendUrl(String url) {
        this.backendUrl = url;
    }

    /**
     * Get current credit balance
     */
    public int getCredits() {
        return prefs.getInt(KEY_CREDITS, 0);
    }

    /**
     * Add credits locally (for ad rewards)
     */
    public void addCredits(int amount, String source) {
        int currentCredits = getCredits();
        int newCredits = currentCredits + amount;

        prefs.edit().putInt(KEY_CREDITS, newCredits).apply();
        Log.d(TAG, "Credits added: +" + amount + " (Source: " + source + "), Total: " + newCredits);
    }

    /**
     * Deduct credits for action
     */
    public boolean deductCredits(int amount, String reason) {
        int currentCredits = getCredits();

        if (currentCredits < amount) {
            Log.e(TAG, "Insufficient credits. Required: " + amount + ", Available: " + currentCredits);
            return false;
        }

        int newCredits = currentCredits - amount;
        prefs.edit().putInt(KEY_CREDITS, newCredits).apply();
        Log.d(TAG, "Credits deducted: -" + amount + " (Reason: " + reason + "), Remaining: " + newCredits);

        return true;
    }

    /**
     * Check if user has enough credits
     */
    public boolean hasEnoughCredits(int required) {
        return getCredits() >= required;
    }

    /**
     * Check if user can watch another ad (rate limiting)
     */
    public boolean canWatchAd() {
        long lastAdTime = prefs.getLong(KEY_LAST_AD_TIME, 0);
        long currentTime = System.currentTimeMillis();

        // Check cooldown
        if (currentTime - lastAdTime < AD_COOLDOWN_MS) {
            long remainingSeconds = (AD_COOLDOWN_MS - (currentTime - lastAdTime)) / 1000;
            Log.d(TAG, "Ad cooldown active. Wait " + remainingSeconds + " seconds");
            return false;
        }

        // Check daily limit
        int adsToday = getAdsWatchedToday();
        if (adsToday >= MAX_ADS_PER_DAY) {
            Log.d(TAG, "Daily ad limit reached: " + adsToday);
            return false;
        }

        return true;
    }

    /**
     * Get number of ads watched today
     */
    private int getAdsWatchedToday() {
        // Simple implementation - resets at midnight would be better
        return prefs.getInt(KEY_TOTAL_ADS_WATCHED, 0) % MAX_ADS_PER_DAY;
    }

    /**
     * Reward user for watching ad
     */
    public void rewardForAdView(CreditCallback callback) {
        if (!canWatchAd()) {
            if (callback != null) {
                callback.onError("Please wait before watching another ad");
            }
            return;
        }

        // Add credits locally
        addCredits(REWARD_PER_AD_VIEW, "ad_view");

        // Update ad watch tracking
        int totalAds = prefs.getInt(KEY_TOTAL_ADS_WATCHED, 0);
        prefs.edit()
                .putInt(KEY_TOTAL_ADS_WATCHED, totalAds + 1)
                .putLong(KEY_LAST_AD_TIME, System.currentTimeMillis())
                .apply();

        Log.d(TAG, "User rewarded: +" + REWARD_PER_AD_VIEW + " credits for watching ad");

        if (callback != null) {
            callback.onSuccess(getCredits());
        }
    }

    /**
     * Get or create device ID
     */
    public String getDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    /**
     * Get time until next ad can be watched
     */
    public long getAdCooldownRemaining() {
        long lastAdTime = prefs.getLong(KEY_LAST_AD_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long remaining = AD_COOLDOWN_MS - (currentTime - lastAdTime);
        return Math.max(0, remaining);
    }

    /**
     * Redeem purchase token from backend
     * This is the ONLY backend interaction - validates token and returns credit amount
     */
    public void redeemToken(String token, CreditCallback callback) {
        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("device_id", getDeviceId());
                payload.addProperty("token", token);

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(backendUrl + "credits/redeem/")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Token redemption failed", e);
                        if (callback != null) {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();

                        if (response.isSuccessful()) {
                            try {
                                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                                if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                                    int creditsToAdd = jsonResponse.get("credits").getAsInt();

                                    // Add credits locally
                                    addCredits(creditsToAdd, "token_redemption");

                                    Log.d(TAG, "Token redeemed: +" + creditsToAdd + " credits");

                                    if (callback != null) {
                                        callback.onSuccess(getCredits());
                                    }
                                } else {
                                    String error = jsonResponse.has("error") ?
                                            jsonResponse.get("error").getAsString() : "Invalid token";
                                    if (callback != null) {
                                        callback.onError(error);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing response", e);
                                if (callback != null) {
                                    callback.onError("Invalid response from server");
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("Invalid token or server error (Code: " + response.code() + ")");
                            }
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error redeeming token", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Get total ads watched
     */
    public int getTotalAdsWatched() {
        return prefs.getInt(KEY_TOTAL_ADS_WATCHED, 0);
    }

    /**
     * Get ads remaining today
     */
    public int getAdsRemainingToday() {
        int watched = getAdsWatchedToday();
        return Math.max(0, MAX_ADS_PER_DAY - watched);
    }

    /**
     * Reset credits (for testing only)
     */
    public void resetCredits() {
        prefs.edit()
                .putInt(KEY_CREDITS, INITIAL_FREE_CREDITS)
                .putInt(KEY_TOTAL_ADS_WATCHED, 0)
                .putLong(KEY_LAST_AD_TIME, 0)
                .apply();
        Log.d(TAG, "Credits reset to " + INITIAL_FREE_CREDITS);
    }

    /**
     * Get credit statistics
     */
    public String getCreditStats() {
        return "Credits: " + getCredits() + "\n" +
                "Total Ads Watched: " + getTotalAdsWatched() + "\n" +
                "Ads Remaining Today: " + getAdsRemainingToday();
    }
}