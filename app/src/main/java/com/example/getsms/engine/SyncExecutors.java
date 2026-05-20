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

public class SyncExecutors {

    static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.e("SyncExecutors", "Error checking network", e);
        }
        return false;
    }

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
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "No network connection");
                return false;
            }

            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("from", sms.getSender());
                payload.addProperty("msg", message);
                payload.addProperty("original_message", sms.getBody());
                payload.addProperty("sim", sms.getSimSlot());
                payload.addProperty("timestamp", sms.getTimestamp());

                RequestBody body = RequestBody.create(
                        gson.toJson(payload),
                        MediaType.parse("application/json; charset=utf-8"));

                Request.Builder requestBuilder = new Request.Builder().url(action.destination);

                if (action.headers != null && !action.headers.isEmpty()) {
                    try {
                        JsonObject headers = gson.fromJson(action.headers, JsonObject.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            headers.entrySet().forEach(entry ->
                                    requestBuilder.addHeader(entry.getKey(), entry.getValue().getAsString()));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing headers", e);
                    }
                }

                if ("GET".equals(action.httpMethod)) {
                    requestBuilder.get();
                } else {
                    requestBuilder.post(body);
                }

                Response response = null;
                try {
                    response = client.newCall(requestBuilder.build()).execute();
                    boolean success = response.isSuccessful();
                    Log.d(TAG, "Webhook response: " + response.code());
                    return success;
                } finally {
                    if (response != null) response.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "Cannot resolve host", e);
                return false;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Connection timeout", e);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Webhook exception: " + e.getMessage(), e);
                return false;
            }
        }
    }

    public static class SmsExecutor {
        private static final String TAG = "SmsExecutor";
        private final Context context;

        public SmsExecutor(Context context) {
            this.context = context;
        }

        public boolean executeSync(Action action, String message, SmsMessage sms) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "SEND_SMS permission not granted");
                return false;
            }

            if (action.destination == null || action.destination.isEmpty()) {
                Log.e(TAG, "No destination phone number");
                return false;
            }

            try {
                SmsManager smsManager = SmsManager.getDefault();
                if (message.length() > 160) {
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(action.destination, null, parts, null, null);
                } else {
                    smsManager.sendTextMessage(action.destination, null, message, null, null);
                }
                Log.d(TAG, "SMS sent to: " + action.destination);
                return true;

            } catch (SecurityException e) {
                Log.e(TAG, "SEND_SMS permission denied at runtime", e);
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid phone number or message", e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "SMS send failed", e);
                return false;
            }
        }

        public static boolean isValidPhoneNumber(String phoneNumber) {
            if (phoneNumber == null || phoneNumber.isEmpty()) return false;
            String cleaned = phoneNumber.replaceAll("[\\s-]", "");
            if (cleaned.startsWith("+")) return cleaned.substring(1).matches("\\d{10,15}");
            return cleaned.matches("\\d{10,15}");
        }
    }

    public static class TelegramExecutor {
        private static final String TAG = "TelegramExecutor";
        private static final String TELEGRAM_API = "https://api.telegram.org/bot";
        private static final String DEFAULT_BOT_TOKEN = "6440238125:AAEVpKE-bSYF0pxOa5NgiaUn9TVaZHMYgeI";
        private static final String DEFAULT_BOT_USERNAME = "@porjects_message_sender_bot";

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
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "No network connection");
                return false;
            }

            String botToken = (action.botToken == null || action.botToken.trim().isEmpty())
                    ? DEFAULT_BOT_TOKEN
                    : action.botToken;

            if (action.chatId == null || action.chatId.trim().isEmpty()) {
                Log.e(TAG, "No chat ID configured");
                return false;
            }

            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("chat_id", action.chatId);
                payload.addProperty("text", message);
                payload.addProperty("parse_mode", "HTML");

                RequestBody body = RequestBody.create(
                        gson.toJson(payload),
                        MediaType.parse("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(TELEGRAM_API + botToken + "/sendMessage")
                        .post(body)
                        .build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Telegram failed: " + response.code());
                        return false;
                    }
                    String responseBody = response.body() != null ? response.body().string() : "";
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    boolean ok = json.has("ok") && json.get("ok").getAsBoolean();
                    Log.d(TAG, "Telegram result: " + ok);
                    return ok;
                } finally {
                    if (response != null) response.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "Cannot resolve Telegram host", e);
                return false;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Telegram connection timeout", e);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Telegram exception: " + e.getMessage(), e);
                return false;
            }
        }

        public static String getDefaultBotInfo() {
            return "Default Bot: " + DEFAULT_BOT_USERNAME + "\n" +
                    "To get your Chat ID:\n" +
                    "1. Open Telegram\n" +
                    "2. Search for " + DEFAULT_BOT_USERNAME + "\n" +
                    "3. Send /start\n" +
                    "4. Send /getchatid\n" +
                    "5. Copy the Chat ID";
        }
    }

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
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "No network connection");
                return false;
            }

            if (action.whatsappApiUrl == null || action.whatsappApiUrl.isEmpty()) {
                Log.e(TAG, "No WhatsApp API URL");
                return false;
            }

            try {
                JsonObject metadata = new JsonObject();
                metadata.addProperty("sender", sms.getSender());
                metadata.addProperty("sim", sms.getSimSlot());

                JsonObject payload = new JsonObject();
                payload.addProperty("phone", action.destination);
                payload.addProperty("message", message);
                payload.add("metadata", metadata);

                Request.Builder requestBuilder = new Request.Builder()
                        .url(action.whatsappApiUrl)
                        .post(RequestBody.create(
                                gson.toJson(payload),
                                MediaType.parse("application/json; charset=utf-8")));

                if (action.whatsappApiKey != null && !action.whatsappApiKey.isEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer " + action.whatsappApiKey);
                }

                Response response = null;
                try {
                    response = client.newCall(requestBuilder.build()).execute();
                    boolean success = response.isSuccessful();
                    Log.d(TAG, "WhatsApp response: " + response.code());
                    return success;
                } finally {
                    if (response != null) response.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "Cannot resolve WhatsApp host", e);
                return false;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "WhatsApp connection timeout", e);
                return false;
            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "WhatsApp exception: " + e.getMessage(), e);
                return false;
            }
        }
    }
}
