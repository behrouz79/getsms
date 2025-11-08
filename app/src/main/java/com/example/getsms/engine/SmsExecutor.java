package com.example.getsms.engine;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.getsms.model.Action;
import com.example.getsms.model.SmsMessage;

import java.util.ArrayList;

public class SmsExecutor {

    private static final String TAG = "SmsExecutor";
    private final Context context;

    public SmsExecutor(Context context) {
        this.context = context;
    }

    public void execute(Action action, String processedMessage, SmsMessage sms) {
        // Check SEND_SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted");
            return;
        }

        if (action.destination == null || action.destination.isEmpty()) {
            Log.e(TAG, "SMS destination phone number not configured");
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();

            // Split message if too long (SMS limit is 160 characters)
            ArrayList<String> parts = smsManager.divideMessage(processedMessage);

            if (parts.size() == 1) {
                // Single SMS
                smsManager.sendTextMessage(
                        action.destination,
                        null,
                        processedMessage,
                        null,
                        null
                );
                Log.d(TAG, "SMS sent to: " + action.destination);
            } else {
                // Multiple SMS parts
                smsManager.sendMultipartTextMessage(
                        action.destination,
                        null,
                        parts,
                        null,
                        null
                );
                Log.d(TAG, "Multi-part SMS sent to: " + action.destination + " (" + parts.size() + " parts)");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS", e);
        }
    }
}