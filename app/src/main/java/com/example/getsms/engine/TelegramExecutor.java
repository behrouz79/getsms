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

public class TelegramExecutor {

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

    public void execute(Action action, String processedMessage, SmsMessage sms) {
        new Thread(() -> {
            try {
                if (action.botToken == null || action.botToken.isEmpty()) {
                    Log.e(TAG, "Telegram bot token not configured");
                    return;
                }

                if (action.chatId == null || action.chatId.isEmpty()) {
                    Log.e(TAG, "Telegram chat ID not configured");
                    return;
                }

                // Build Telegram API URL
                String url = TELEGRAM_API + action.botToken + "/sendMessage";

                // Build payload
                JsonObject payload = new JsonObject();
                payload.addProperty("chat_id", action.chatId);
                payload.addProperty("text", processedMessage);
                payload.addProperty("parse_mode", "HTML"); // Support HTML formatting

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
                        Log.d(TAG, "Telegram message sent successfully");
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Telegram API error: " + response.code() + " - " + errorBody);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending Telegram message", e);
            }
        }).start();
    }
}