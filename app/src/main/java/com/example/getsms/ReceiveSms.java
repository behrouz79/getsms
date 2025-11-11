package com.example.getsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.example.getsms.engine.RuleEngine;
import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.roomDB.SmsRecord;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiveSms extends BroadcastReceiver {

    private static final String TAG = "ReceiveSms";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (!SMS_RECEIVED.equals(intent.getAction())) {
            return;
        }

        // Check if service is enabled
        if (!isServiceEnabled(context)) {
            Log.d(TAG, "Service is disabled. Ignoring SMS.");
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) {
                return;
            }

            String format = bundle.getString("format");
            int subscriptionId = bundle.getInt("subscription", -1);

            // Determine which SIM card received the message
            String simSlot = getSimSlot(context, subscriptionId);

            SmsMessage[] msgs = new SmsMessage[pdus.length];
            StringBuilder msgBody = new StringBuilder();
            String msgSender = "";
            long timestamp = System.currentTimeMillis();

            for (int i = 0; i < pdus.length; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }

                if (msgs[i] != null) {
                    msgBody.append(msgs[i].getMessageBody());
                    if (i == 0) {
                        msgSender = msgs[i].getOriginatingAddress();
                        timestamp = msgs[i].getTimestampMillis();
                    }
                }
            }

            if (!msgSender.isEmpty()) {
                com.example.getsms.model.SmsMessage smsMessage = new com.example.getsms.model.SmsMessage(
                        msgSender,
                        msgBody.toString(),
                        simSlot,
                        timestamp,
                        subscriptionId
                );

                // Save to database and process rules
                processIncomingSms(context, smsMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error receiving SMS", e);
        }
    }

    /**
     * Check if service is enabled
     */
    private boolean isServiceEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SERVICE_ENABLED, false);
    }

    /**
     * Enable service
     */
    public static void enableService(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, true).apply();
        Log.d(TAG, "Service enabled");
    }

    /**
     * Disable service
     */
    public static void disableService(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, false).apply();
        Log.d(TAG, "Service disabled");
    }

    /**
     * Get SIM slot information
     */
    private String getSimSlot(Context context, int subscriptionId) {
        if (subscriptionId == -1) {
            return "UNKNOWN";
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager subscriptionManager =
                        (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

                if (subscriptionManager != null) {
                    SubscriptionInfo info = subscriptionManager.getActiveSubscriptionInfo(subscriptionId);
                    if (info != null) {
                        int slotIndex = info.getSimSlotIndex();
                        return "SIM" + (slotIndex + 1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM slot info", e);
        }

        return "SIM1"; // Default to SIM1 if can't determine
    }

    /**
     * Process incoming SMS: Save to DB and execute rules
     */
    private void processIncomingSms(Context context, com.example.getsms.model.SmsMessage smsMessage) {
        executorService.execute(() -> {
            try {
                DataBase db = DataBase.getDbInstance(context);

                // Save to database
                saveToDatabase(db, smsMessage);

                // Load and execute rules
                List<Rule> rules = db.ruleDao().getEnabledRules();
                if (rules != null && !rules.isEmpty()) {
                    RuleEngine ruleEngine = new RuleEngine(context);
                    ruleEngine.processSms(smsMessage, rules);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing SMS", e);
            }
        });
    }

    /**
     * Save SMS to database
     */
    private void saveToDatabase(DataBase db, com.example.getsms.model.SmsMessage smsMessage) {
        try {
            SmsRecord record = new SmsRecord();
            record.title = smsMessage.getSender() + " (" + smsMessage.getSimSlot() + ")";
            record.body = smsMessage.getBody();
            record.date = smsMessage.getFormattedDate();
            record.status = 200; // Default status for received SMS

            db.smsDao().insertRecord(record);
            Log.d(TAG, "SMS saved: " + smsMessage.getSender() + " on " + smsMessage.getSimSlot());
        } catch (Exception e) {
            Log.e(TAG, "Error saving to database", e);
        }
    }
}