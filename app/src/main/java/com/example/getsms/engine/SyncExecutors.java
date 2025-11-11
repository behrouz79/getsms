package com.example.getsms.engine;

import android.content.Context;
import android.telephony.SmsManager;
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

/**
 * Enhanced executors with synchronous methods that return success/failure
 */
public class SyncExecutors {

    /**
     * Enhanced Webhook Executor
     */
    public static class WebhookExecutor {
        private static final String TAG = "WebhookExecutor";
        private final Context context;
        private final OkHttpClient client;
        private final Gson gson;

        public WebhookExecutor(Context context) {
            this.context = context;
            this.gson = new Gson();
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
        }

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            try {
                // Build payload
                JsonObject payload = new JsonObject();
                payload.addProperty("from", sms.getSender());
                payload.addProperty("message", message);
                payload.addProperty("original_message", sms.getBody());
                payload.addProperty("sim", sms.getSimSlot());
                payload.addProperty("timestamp", sms.getTimestamp());

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request.Builder requestBuilder = new Request.Builder()
                        .url(action.destination);

                // Add headers
                if (action.headers != null && !action.headers.isEmpty()) {
                    try {
                        JsonObject headers = gson.fromJson(action.headers, JsonObject.class);
                        headers.entrySet().forEach(entry ->
                                requestBuilder.addHeader(entry.getKey(), entry.getValue().getAsString())
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing headers", e);
                    }
                }

                // Set method
                if ("POST".equals(action.httpMethod)) {
                    requestBuilder.post(body);
                } else if ("GET".equals(action.httpMethod)) {
                    requestBuilder.get();
                } else {
                    requestBuilder.post(body);
                }

                Request request = requestBuilder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Webhook success: " + response.code());
                        return true;
                    } else {
                        Log.e(TAG, "❌ Webhook failed: " + response.code() + " - " + response.message());
                        return false;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Webhook exception", e);
                return false;
            }
        }

        public void execute(Action action, String message, SmsMessage sms) {
            new Thread(() -> executeSync(action, message, sms)).start();
        }
    }

    /**
     * Enhanced SMS Executor
     */
    public static class SmsExecutor {
        private static final String TAG = "SmsExecutor";
        private final Context context;

        public SmsExecutor(Context context) {
            this.context = context;
        }

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            try {
                if (action.destination == null || action.destination.isEmpty()) {
                    Log.e(TAG, "❌ No destination phone number");
                    return false;
                }

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(
                        action.destination,
                        null,
                        message,
                        null,
                        null
                );

                Log.d(TAG, "✅ SMS sent to: " + action.destination);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "❌ SMS failed", e);
                return false;
            }
        }

        public void execute(Action action, String message, SmsMessage sms) {
            new Thread(() -> executeSync(action, message, sms)).start();
        }
    }

    /**
     * Enhanced Telegram Executor
     */
    public static class TelegramExecutor {
        private static final String TAG = "TelegramExecutor";
        private static final String TELEGRAM_API = "https://api.telegram.org/bot";

        private final Context context;
        private final OkHttpClient client;
        private final Gson gson;

        public TelegramExecutor(Context context) {
            this.context = context;
            this.gson = new Gson();
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build();
        }

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            try {
                if (action.botToken == null || action.botToken.isEmpty()) {
                    Log.e(TAG, "❌ No bot token");
                    return false;
                }

                if (action.chatId == null || action.chatId.isEmpty()) {
                    Log.e(TAG, "❌ No chat ID");
                    return false;
                }

                String url = TELEGRAM_API + action.botToken + "/sendMessage";

                JsonObject payload = new JsonObject();
                payload.addProperty("chat_id", action.chatId);
                payload.addProperty("text", message);
                payload.addProperty("parse_mode", "HTML");

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Telegram sent successfully");
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown";
                        Log.e(TAG, "❌ Telegram failed: " + response.code() + " - " + errorBody);
                        return false;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Telegram exception", e);
                return false;
            }
        }

        public void execute(Action action, String message, SmsMessage sms) {
            new Thread(() -> executeSync(action, message, sms)).start();
        }
    }

    /**
     * Enhanced WhatsApp Executor
     */
    public static class WhatsAppExecutor {
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

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            try {
                if (action.whatsappApiUrl == null || action.whatsappApiUrl.isEmpty()) {
                    Log.e(TAG, "❌ No WhatsApp API URL");
                    return false;
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("phone", action.destination);
                payload.addProperty("message", message);

                JsonObject metadata = new JsonObject();
                metadata.addProperty("sender", sms.getSender());
                metadata.addProperty("sim", sms.getSimSlot());
                payload.add("metadata", metadata);

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request.Builder requestBuilder = new Request.Builder()
                        .url(action.whatsappApiUrl)
                        .post(body);

                if (action.whatsappApiKey != null && !action.whatsappApiKey.isEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer " + action.whatsappApiKey);
                }

                Request request = requestBuilder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ WhatsApp sent successfully");
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown";
                        Log.e(TAG, "❌ WhatsApp failed: " + response.code() + " - " + errorBody);
                        return false;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ WhatsApp exception", e);
                return false;
            }
        }

        public void execute(Action action, String message, SmsMessage sms) {
            new Thread(() -> executeSync(action, message, sms)).start();
        }
    }
}