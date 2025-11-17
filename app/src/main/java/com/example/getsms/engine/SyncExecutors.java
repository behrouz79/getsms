package com.example.getsms.engine;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.getsms.model.Action;
import com.example.getsms.model.SmsMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * FIXED: Proper network error detection and status reporting
 */
public class SyncExecutors {

    /**
     * Enhanced Webhook Executor - FIXED Network Detection
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
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
        }

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            // Check network connectivity first
            if (!isNetworkAvailable()) {
                Log.e(TAG, "❌ No network connection available");
                return false;
            }

            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("from", sms.getSender());
                payload.addProperty("msg", message);
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

                if (action.headers != null && !action.headers.isEmpty()) {
                    try {
                        JsonObject headers = gson.fromJson(action.headers, JsonObject.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            headers.entrySet().forEach(entry ->
                                    requestBuilder.addHeader(entry.getKey(), entry.getValue().getAsString())
                            );
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing headers", e);
                    }
                }

                if ("POST".equals(action.httpMethod)) {
                    requestBuilder.post(body);
                } else if ("GET".equals(action.httpMethod)) {
                    requestBuilder.get();
                } else {
                    requestBuilder.post(body);
                }

                Request request = requestBuilder.build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();

                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    Log.d(TAG, "Response Code: " + statusCode);
                    Log.d(TAG, "Response Body: " + responseBody);

                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Webhook success: " + statusCode);
                        return true;
                    } else {
                        Log.e(TAG, "❌ Webhook failed: " + statusCode + " - " + response.message());
                        return false;
                    }
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "❌ Network error: Cannot resolve host (no internet or DNS issue)", e);
                return false;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "❌ Network error: Connection timeout", e);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "❌ Network error: " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "❌ Webhook exception: " + e.getMessage(), e);
                return false;
            }
        }

        private boolean isNetworkAvailable() {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking network", e);
            }
            return false;
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
            // ============================================
            // CRITICAL: CHECK SEND_SMS PERMISSION FIRST
            // ============================================
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "❌ SEND_SMS permission not granted!");
                Log.e(TAG, "   This should have been checked before execution");
                Log.e(TAG, "   Please request permission in MainActivity");
                return false;
            }

            // Check destination
            if (action.destination == null || action.destination.isEmpty()) {
                Log.e(TAG, "❌ No destination phone number");
                return false;
            }

            try {
                Log.d(TAG, "📤 Sending SMS to: " + action.destination);
                Log.d(TAG, "   Message: " + message);

                SmsManager smsManager = SmsManager.getDefault();

                // Check message length
                if (message.length() > 160) {
                    Log.d(TAG, "   Message is long (" + message.length() + " chars), splitting...");

                    ArrayList<String> parts = smsManager.divideMessage(message);
                    Log.d(TAG, "   Split into " + parts.size() + " parts");

                    smsManager.sendMultipartTextMessage(
                            action.destination,
                            null,
                            parts,
                            null,
                            null
                    );

                    Log.d(TAG, "✅ Multi-part SMS sent successfully");
                } else {
                    smsManager.sendTextMessage(
                            action.destination,
                            null,
                            message,
                            null,
                            null
                    );

                    Log.d(TAG, "✅ SMS sent successfully");
                }

                return true;

            } catch (SecurityException e) {
                Log.e(TAG, "❌ SecurityException: SEND_SMS permission denied at runtime!", e);
                Log.e(TAG, "   This should not happen if permissions were checked properly");
                return false;

            } catch (IllegalArgumentException e) {
                Log.e(TAG, "❌ IllegalArgumentException: Invalid phone number or message", e);
                Log.e(TAG, "   Destination: " + action.destination);
                return false;

            } catch (Exception e) {
                Log.e(TAG, "❌ SMS failed with exception: " + e.getClass().getSimpleName(), e);
                Log.e(TAG, "   Error: " + e.getMessage());
                return false;
            }
        }

        /**
         * Asynchronous execution
         */
        public void execute(Action action, String message, SmsMessage sms) {
            new Thread(() -> {
                boolean success = executeSync(action, message, sms);
                if (!success) {
                    Log.e(TAG, "❌ Async SMS execution failed");
                }
            }).start();
        }

        /**
         * Check if SMS can be sent
         */
        public static boolean canSendSms(Context context) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "⚠️ SEND_SMS permission not granted");
                return false;
            }

            return true;
        }

        /**
         * Validate phone number format
         */
        public static boolean isValidPhoneNumber(String phoneNumber) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return false;
            }

            // Remove spaces and dashes
            String cleaned = phoneNumber.replaceAll("[\\s-]", "");

            // Check if it starts with + and has digits
            if (cleaned.startsWith("+")) {
                return cleaned.substring(1).matches("\\d{10,15}");
            }

            // Check if it's all digits with reasonable length
            return cleaned.matches("\\d{10,15}");
        }

    }

    /**
     * Enhanced Telegram Executor - FIXED Status Detection
     */
    public static class TelegramExecutor {
        private static final String TAG = "TelegramExecutor";
        private static final String TELEGRAM_API = "https://api.telegram.org/bot";

        // Default bot for users without their own bot
        private static final String DEFAULT_BOT_TOKEN = "YOUR_DEFAULT_BOT_TOKEN_HERE";
        private static final String DEFAULT_CHAT_ID = "YOUR_DEFAULT_CHAT_ID_HERE";

        private final Context context;
        private final OkHttpClient client;
        private final Gson gson;

        public TelegramExecutor(Context context) {
            this.context = context;
            this.gson = new Gson();
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
        }

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            // Check network first
            if (!isNetworkAvailable()) {
                Log.e(TAG, "❌ No network connection available");
                return false;
            }

            String botToken = action.botToken;
            String chatId = action.chatId;
            boolean usingDefaultBot = false;

            // Use default bot if user hasn't configured their own
            if (botToken == null || botToken.isEmpty()) {
                Log.d(TAG, "📱 Using default bot");
                botToken = DEFAULT_BOT_TOKEN;
                chatId = DEFAULT_CHAT_ID;
                usingDefaultBot = true;

                // Prepend info about using default bot
                message = "🤖 [Via App Bot]\n" + message;
            }

            if (chatId == null || chatId.isEmpty()) {
                Log.e(TAG, "❌ No chat ID configured");
                return false;
            }

            try {
                String url = TELEGRAM_API + botToken + "/sendMessage";

                JsonObject payload = new JsonObject();
                payload.addProperty("chat_id", chatId);
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

                Response response = null;
                try {
                    response = client.newCall(request).execute();

                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    Log.d(TAG, "Response Code: " + statusCode);
                    Log.d(TAG, "Response Body: " + responseBody);

                    if (response.isSuccessful()) {
                        // Parse response to verify message was sent
                        try {
                            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                            boolean ok = jsonResponse.has("ok") && jsonResponse.get("ok").getAsBoolean();

                            if (ok) {
                                Log.d(TAG, "✅ Telegram sent successfully" + (usingDefaultBot ? " (default bot)" : ""));
                                return true;
                            } else {
                                Log.e(TAG, "❌ Telegram API returned ok=false");
                                return false;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing Telegram response", e);
                            return false;
                        }
                    } else {
                        Log.e(TAG, "❌ Telegram failed: " + statusCode + " - " + responseBody);
                        return false;
                    }
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "❌ Network error: Cannot resolve Telegram host (no internet)", e);
                return false;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "❌ Network error: Telegram connection timeout", e);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "❌ Network error: " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "❌ Telegram exception: " + e.getMessage(), e);
                return false;
            }
        }

        private boolean isNetworkAvailable() {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking network", e);
            }
            return false;
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
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();
        }

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            if (!isNetworkAvailable()) {
                Log.e(TAG, "❌ No network connection available");
                return false;
            }

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

                Response response = null;
                try {
                    response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ WhatsApp sent successfully");
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown";
                        Log.e(TAG, "❌ WhatsApp failed: " + response.code() + " - " + errorBody);
                        return false;
                    }
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "❌ Network error: Cannot resolve WhatsApp host", e);
                return false;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "❌ Network error: WhatsApp connection timeout", e);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "❌ Network error: " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "❌ WhatsApp exception: " + e.getMessage(), e);
                return false;
            }
        }

        private boolean isNetworkAvailable() {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking network", e);
            }
            return false;
        }

        public void execute(Action action, String message, SmsMessage sms) {
            new Thread(() -> executeSync(action, message, sms)).start();
        }
    }
}