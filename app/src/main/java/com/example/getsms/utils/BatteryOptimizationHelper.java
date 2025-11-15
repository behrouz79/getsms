package com.example.getsms.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.example.getsms.R;

/**
 * Helper to disable battery optimization for the app
 * This is CRITICAL for keeping the service running
 */
public class BatteryOptimizationHelper {

    private static final String TAG = "BatteryOptimization";

    /**
     * Check if battery optimization is disabled for this app
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                String packageName = context.getPackageName();
                boolean isIgnoring = pm.isIgnoringBatteryOptimizations(packageName);
                Log.d(TAG, "Battery optimization disabled: " + isIgnoring);
                return isIgnoring;
            }
        }
        return true; // Not applicable for older versions
    }

    /**
     * Request to disable battery optimization
     * CRITICAL: Without this, Android will kill your service
     */
    @SuppressLint("BatteryLife")
    public static void requestDisableBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                String packageName = context.getPackageName();

                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Log.d(TAG, "Requesting battery optimization exemption");

                    // Show explanation dialog first
                    new AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.battery_optimization))
                            .setMessage(context.getString(R.string.battery_optimization_message))
                            .setPositiveButton(context.getString(R.string.disable_optimization), (dialog, which) -> {
                                try {
                                    @SuppressLint("BatteryLife")
                                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                    intent.setData(Uri.parse("package:" + packageName));
                                    context.startActivity(intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error opening battery optimization settings", e);
                                    // Fallback to general battery settings
                                    openBatteryOptimizationSettings(context);
                                }
                            })
                            .setNegativeButton(context.getString(R.string.skip), (dialog, which) -> {
                                Log.w(TAG, "User declined battery optimization exemption");
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        }
    }

    /**
     * Open battery optimization settings page
     */
    public static void openBatteryOptimizationSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening battery settings", e);
        }
    }

    /**
     * Show battery optimization status
     */
    public static String getBatteryOptimizationStatus(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                String packageName = context.getPackageName();
                boolean isIgnoring = pm.isIgnoringBatteryOptimizations(packageName);

                if (isIgnoring) {
                    return context.getString(R.string.battery_optimization_disabled_good);
                } else {
                    return context.getString(R.string.battery_optimization_enabled_warning);
                }
            }
        }
        return context.getString(R.string.battery_optimization_not_applicable);
    }
}