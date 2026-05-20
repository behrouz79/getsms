package com.example.getsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.getsms.utils.ServicePrefs;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)
                && !"com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            return;
        }

        Log.d(TAG, "Boot completed: " + action);

        if (ServicePrefs.isServiceEnabled(context)) {
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
