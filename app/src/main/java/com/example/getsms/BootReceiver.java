package com.example.getsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * Starts the service automatically after device reboot
 * if it was running before reboot
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        Log.d(TAG, "========================================");
        Log.d(TAG, context.getString(R.string.log_boot_receiver_triggered));
        Log.d(TAG, "Action: " + intent.getAction());
        Log.d(TAG, "========================================");

        // Check for boot completed actions
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()) ||
                "com.htc.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            // Check if service was running before reboot
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean wasEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);

            if (wasEnabled) {
                Log.d(TAG, context.getString(R.string.log_service_enabled_before_reboot));

                Intent serviceIntent = new Intent(context, EndlessService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                Log.d(TAG, context.getString(R.string.log_service_started_successfully));
            } else {
                Log.d(TAG, context.getString(R.string.log_service_disabled_before_reboot));
            }
        }
    }
}