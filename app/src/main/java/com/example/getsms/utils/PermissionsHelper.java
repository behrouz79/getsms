package com.example.getsms.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to handle all runtime permissions
 */
public class PermissionsHelper {

    private static final String TAG = "PermissionsHelper";

    // Permission request code
    public static final int PERMISSION_REQUEST_CODE = 1001;

    // All required permissions
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    // Android 13+ specific permissions
    private static final String[] ANDROID_13_PERMISSIONS = {
            Manifest.permission.POST_NOTIFICATIONS
    };

    /**
     * Check if all required permissions are granted
     */
    public static boolean hasAllPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Missing permission: " + permission);
                return false;
            }
        }

        // Check Android 13+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (String permission : ANDROID_13_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Missing permission: " + permission);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if SEND_SMS permission is granted
     */
    public static boolean hasSendSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if RECEIVE_SMS permission is granted
     */
    public static boolean hasReceiveSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get list of permissions that need to be requested
     */
    public static List<String> getMissingPermissions(Context context) {
        List<String> missing = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        // Add Android 13+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (String permission : ANDROID_13_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    missing.add(permission);
                }
            }
        }

        return missing;
    }

    /**
     * Request all missing permissions
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> missing = getMissingPermissions(activity);

        if (!missing.isEmpty()) {
            Log.d(TAG, "Requesting " + missing.size() + " permissions");
            ActivityCompat.requestPermissions(
                    activity,
                    missing.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        } else {
            Log.d(TAG, "All permissions already granted");
        }
    }

    /**
     * Request SEND_SMS permission specifically
     */
    public static void requestSendSmsPermission(Activity activity) {
        if (!hasSendSmsPermission(activity)) {
            Log.d(TAG, "Requesting SEND_SMS permission");
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Handle permission result
     */
    public static boolean handlePermissionResult(int requestCode,
                                                 String[] permissions,
                                                 int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return false;
        }

        boolean allGranted = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Permission granted: " + permissions[i]);
            } else {
                Log.e(TAG, "❌ Permission denied: " + permissions[i]);
                allGranted = false;
            }
        }

        return allGranted;
    }

    /**
     * Check if should show permission rationale
     */
    public static boolean shouldShowRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * Get user-friendly permission name
     */
    public static String getPermissionName(String permission) {
        switch (permission) {
            case Manifest.permission.SEND_SMS:
                return "Send SMS";
            case Manifest.permission.RECEIVE_SMS:
                return "Receive SMS";
            case Manifest.permission.READ_SMS:
                return "Read SMS";
            case Manifest.permission.READ_PHONE_STATE:
                return "Read Phone State";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Show Notifications";
            case Manifest.permission.INTERNET:
                return "Internet Access";
            case Manifest.permission.ACCESS_NETWORK_STATE:
                return "Network State";
            default:
                return permission;
        }
    }

    /**
     * Get permission description
     */
    public static String getPermissionDescription(String permission) {
        switch (permission) {
            case Manifest.permission.SEND_SMS:
                return "Required to forward SMS messages and send backup notifications";
            case Manifest.permission.RECEIVE_SMS:
                return "Required to receive incoming SMS messages";
            case Manifest.permission.READ_SMS:
                return "Required to read SMS content for processing";
            case Manifest.permission.READ_PHONE_STATE:
                return "Required to detect which SIM card received the message";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "Required to show service status and alerts";
            case Manifest.permission.INTERNET:
                return "Required to send webhooks and Telegram messages";
            case Manifest.permission.ACCESS_NETWORK_STATE:
                return "Required to check network connectivity";
            default:
                return "Required for app functionality";
        }
    }

    /**
     * Log all permission states
     */
    public static void logPermissionStates(Context context) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "PERMISSION STATUS");
        Log.d(TAG, "========================================");

        for (String permission : REQUIRED_PERMISSIONS) {
            boolean granted = ContextCompat.checkSelfPermission(context, permission)
                    == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, (granted ? "✅" : "❌") + " " + getPermissionName(permission));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (String permission : ANDROID_13_PERMISSIONS) {
                boolean granted = ContextCompat.checkSelfPermission(context, permission)
                        == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, (granted ? "✅" : "❌") + " " + getPermissionName(permission));
            }
        }

        Log.d(TAG, "========================================");
    }
}