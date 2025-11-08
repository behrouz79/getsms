package com.example.getsms.engine;

import android.content.Context;
import android.util.Log;

import com.example.getsms.model.Action;
import com.example.getsms.model.SmsMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WhatsAppExecutor {

    private static final String TAG = "WhatsAppExecutor";

    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;

    public WhatsAppExecutor(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Send WhatsApp message using WhatsApp Business API or third-party API
     * You can use services like:
     * - WhatsApp Business API (official)
     * - Twilio WhatsApp API
     * - 360Dialog
     * - Gupshup
     * - Or any custom WhatsApp gateway
     */
    public void execute(Action action, String processedMessage, SmsMessage sms) {
        new Thread(() -> {
            try {
                if (action.whatsappApiUrl == null || action.whatsappApiUrl.isEmpty()) {
                    Log.e(TAG, "WhatsApp API URL not configured");
                    return;
                }

                // Generic WhatsApp API format (adjust based on your provider)
                JsonObject payload = new JsonObject();
                payload.addProperty("phone", action.destination); // Phone number with country code
                payload.addProperty("message", processedMessage);

                // Add metadata
                JsonObject metadata = new JsonObject();
                metadata.addProperty("sender", sms.getSender());
                metadata.addProperty("sim", sms.getSimSlot());
                metadata.addProperty("timestamp", sms.getTimestamp());
                payload.add("metadata", metadata);

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request.Builder requestBuilder = new Request.Builder()
                        .url(action.whatsappApiUrl)
                        .post(body);

                // Add API key if configured
                if (action.whatsappApiKey != null && !action.whatsappApiKey.isEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer " + action.whatsappApiKey);
                }

                Request request = requestBuilder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "WhatsApp message sent successfully");
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "WhatsApp API error: " + response.code() + " - " + errorBody);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending WhatsApp message", e);
            }
        }).start();
    }
}