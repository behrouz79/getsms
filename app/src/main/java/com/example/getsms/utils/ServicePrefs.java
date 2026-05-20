package com.example.getsms.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ServicePrefs {
    public static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";

    public static boolean isServiceEnabled(Context context) {
        return prefs(context).getBoolean(KEY_SERVICE_ENABLED, false);
    }

    public static void setServiceEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
