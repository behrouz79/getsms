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

public class WebhookExecutor {

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

    public void execute(Action action, String processedMessage, SmsMessage sms) {
        new Thread(() -> {
            try {
                // Build JSON payload
                JsonObject payload = new JsonObject();
                payload.addProperty("from", sms.getSender());
                payload.addProperty("message", processedMessage);
                payload.addProperty("original_message", sms.getBody());
                payload.addProperty("sim", sms.getSimSlot());
                payload.addProperty("timestamp", sms.getTimestamp());
                payload.addProperty("date", sms.getFormattedDate());

                String jsonPayload = gson.toJson(payload);

                RequestBody body = RequestBody.create(
                        jsonPayload,
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request.Builder requestBuilder = new Request.Builder()
                        .url(action.destination);

                // Add custom headers if provided
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

                // Set HTTP method
                if ("POST".equals(action.httpMethod)) {
                    requestBuilder.post(body);
                } else if ("GET".equals(action.httpMethod)) {
                    requestBuilder.get();
                } else {
                    requestBuilder.post(body); // Default to POST
                }

                Request request = requestBuilder.build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Webhook sent successfully: " + action.destination);
                    } else {
                        Log.e(TAG, "Webhook failed: " + response.code() + " - " + response.message());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending webhook", e);
            }
        }).start();
    }
}