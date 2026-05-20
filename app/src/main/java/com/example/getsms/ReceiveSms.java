package com.example.getsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.utils.ServicePrefs;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiveSms extends BroadcastReceiver {

    private static final String TAG = "ReceiveSms";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !SMS_RECEIVED.equals(intent.getAction())) return;
        if (!ServicePrefs.isServiceEnabled(context)) {
            Log.d(TAG, "Service disabled. Ignoring SMS.");
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) return;

            String format = bundle.getString("format");
            int subscriptionId = bundle.getInt("subscription", -1);
            String simSlot = getSimSlot(context, subscriptionId);

            SmsMessage[] msgs = new SmsMessage[pdus.length];
            StringBuilder msgBody = new StringBuilder();
            String msgSender = "";
            long timestamp = System.currentTimeMillis();

            for (int i = 0; i < pdus.length; i++) {
                msgs[i] = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? SmsMessage.createFromPdu((byte[]) pdus[i], format)
                        : SmsMessage.createFromPdu((byte[]) pdus[i]);

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
                        msgSender, msgBody.toString(), simSlot, timestamp, subscriptionId);
                processIncomingSms(context, smsMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error receiving SMS", e);
        }
    }

    public static void enableService(Context context) {
        ServicePrefs.setServiceEnabled(context, true);
    }

    public static void disableService(Context context) {
        ServicePrefs.setServiceEnabled(context, false);
    }

    private String getSimSlot(Context context, int subscriptionId) {
        if (subscriptionId == -1) return "UNKNOWN";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager sm = (SubscriptionManager)
                        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    SubscriptionInfo info = sm.getActiveSubscriptionInfo(subscriptionId);
                    if (info != null) return "SIM" + (info.getSimSlotIndex() + 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SIM slot info", e);
        }
        return "SIM1";
    }

    private void processIncomingSms(Context context, com.example.getsms.model.SmsMessage smsMessage) {
        executorService.execute(() -> {
            try {
                List<Rule> rules = DataBase.getDbInstance(context).ruleDao().getEnabledRules();
                if (rules != null && !rules.isEmpty()) {
                    ((App) context.getApplicationContext()).getRuleEngine().processSms(smsMessage, rules);
                } else {
                    Log.d(TAG, "No enabled rules found");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing SMS", e);
            }
        });
    }
}
