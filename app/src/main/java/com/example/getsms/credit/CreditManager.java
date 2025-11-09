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
 * Credits are required to send SMS/webhooks
 */
public class CreditManager {

    private static final String TAG = "CreditManager";
    private static final String PREFS_NAME = "credit_prefs";
    private static final String KEY_CREDITS = "user_credits";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LAST_SYNC = "last_sync_time";

    // Credit costs
    public static final int COST_PER_SMS = 1;
    public static final int COST_PER_WEBHOOK = 1;
    public static final int COST_PER_TELEGRAM = 1;

    // Credit rewards
    public static final int REWARD_PER_AD_VIEW = 5;
    public static final int REWARD_PER_AD_CLICK = 10;

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final OkHttpClient client;

    // Backend configuration
    private String backendUrl = "https://your-django-backend.com/api/";

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
     * Add credits locally and sync with backend
     */
    public void addCredits(int amount, String source, CreditCallback callback) {
        int currentCredits = getCredits();
        int newCredits = currentCredits + amount;

        prefs.edit().putInt(KEY_CREDITS, newCredits).apply();
        Log.d(TAG, "Credits added: " + amount + " (Source: " + source + "), Total: " + newCredits);

        // Sync with backend
        syncCreditsWithBackend(amount, source, callback);
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
        Log.d(TAG, "Credits deducted: " + amount + " (Reason: " + reason + "), Remaining: " + newCredits);

        // Log transaction to backend
        logCreditTransaction(amount, reason, "DEBIT");

        return true;
    }

    /**
     * Check if user has enough credits
     */
    public boolean hasEnoughCredits(int required) {
        return getCredits() >= required;
    }

    /**
     * Reward user for watching ad
     */
    public void rewardForAdView(CreditCallback callback) {
        addCredits(REWARD_PER_AD_VIEW, "ad_view", callback);
    }

    /**
     * Reward user for clicking ad
     */
    public void rewardForAdClick(CreditCallback callback) {
        addCredits(REWARD_PER_AD_CLICK, "ad_click", callback);
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
     * Set user ID after authentication
     */
    public void setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, getDeviceId());
    }

    /**
     * Sync credits with backend
     */
    public void syncCreditsWithBackend(int amount, String source, CreditCallback callback) {
        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("user_id", getUserId());
                payload.addProperty("device_id", getDeviceId());
                payload.addProperty("amount", amount);
                payload.addProperty("source", source);
                payload.addProperty("timestamp", System.currentTimeMillis());

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(backendUrl + "credits/add/")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Failed to sync credits", e);
                        if (callback != null) {
                            callback.onError("Failed to sync: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                            if (jsonResponse.has("total_credits")) {
                                int serverCredits = jsonResponse.get("total_credits").getAsInt();
                                prefs.edit()
                                        .putInt(KEY_CREDITS, serverCredits)
                                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                                        .apply();

                                Log.d(TAG, "Credits synced with server: " + serverCredits);

                                if (callback != null) {
                                    callback.onSuccess(serverCredits);
                                }
                            }
                        } else {
                            Log.e(TAG, "Backend sync failed: " + response.code());
                            if (callback != null) {
                                callback.onError("Sync failed: " + response.code());
                            }
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error syncing credits", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Log credit transaction to backend
     */
    private void logCreditTransaction(int amount, String reason, String type) {
        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("user_id", getUserId());
                payload.addProperty("device_id", getDeviceId());
                payload.addProperty("amount", amount);
                payload.addProperty("reason", reason);
                payload.addProperty("type", type);
                payload.addProperty("timestamp", System.currentTimeMillis());

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(backendUrl + "credits/log/")
                        .post(body)
                        .build();

                client.newCall(request).execute();

            } catch (Exception e) {
                Log.e(TAG, "Error logging transaction", e);
            }
        }).start();
    }

    /**
     * Fetch credits from backend
     */
    public void fetchCreditsFromBackend(CreditCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(backendUrl + "credits/balance/?user_id=" + getUserId())
                        .get()
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Failed to fetch credits", e);
                        if (callback != null) {
                            callback.onError("Failed to fetch: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                            if (jsonResponse.has("credits")) {
                                int serverCredits = jsonResponse.get("credits").getAsInt();
                                prefs.edit()
                                        .putInt(KEY_CREDITS, serverCredits)
                                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                                        .apply();

                                Log.d(TAG, "Credits fetched from server: " + serverCredits);

                                if (callback != null) {
                                    callback.onSuccess(serverCredits);
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to fetch credits: " + response.code());
                            if (callback != null) {
                                callback.onError("Fetch failed: " + response.code());
                            }
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching credits", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Purchase credits with key
     */
    public void purchaseWithKey(String purchaseKey, CreditCallback callback) {
        new Thread(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("user_id", getUserId());
                payload.addProperty("device_id", getDeviceId());
                payload.addProperty("purchase_key", purchaseKey);

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(backendUrl + "credits/purchase/")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Purchase failed", e);
                        if (callback != null) {
                            callback.onError("Purchase failed: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();

                        if (response.isSuccessful()) {
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                            if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                                int creditsAdded = jsonResponse.get("credits_added").getAsInt();
                                int totalCredits = jsonResponse.get("total_credits").getAsInt();

                                prefs.edit().putInt(KEY_CREDITS, totalCredits).apply();

                                Log.d(TAG, "Purchase successful: +" + creditsAdded + " credits");

                                if (callback != null) {
                                    callback.onSuccess(totalCredits);
                                }
                            } else {
                                String error = jsonResponse.has("error") ?
                                        jsonResponse.get("error").getAsString() : "Unknown error";
                                if (callback != null) {
                                    callback.onError(error);
                                }
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("Invalid purchase key or server error");
                            }
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error processing purchase", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Reset credits (for testing)
     */
    public void resetCredits() {
        prefs.edit().putInt(KEY_CREDITS, 0).apply();
        Log.d(TAG, "Credits reset to 0");
    }
}