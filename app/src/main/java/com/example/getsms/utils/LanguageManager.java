package com.example.getsms.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

/**
 * FIXED Language Manager - Properly handles app language switching
 */
public class LanguageManager {
    private static final String TAG = "LanguageManager";
    private static final String PREFS_NAME = "language_prefs";
    private static final String KEY_LANGUAGE = "selected_language";

    // Supported languages
    public static final String ENGLISH = "en";
    public static final String PERSIAN = "fa";
    public static final String ARABIC = "ar";

    /**
     * Set app language and restart activity
     * CRITICAL: This must be called BEFORE setContentView()
     */
    public static void setLanguage(Activity activity, String languageCode) {
        Log.d(TAG, "Setting language to: " + languageCode);

        // Save preference
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        // Apply language
        applyLanguage(activity, languageCode);

        // Restart activity to apply changes
        Intent intent = activity.getIntent();
        activity.finish();
        activity.startActivity(intent);

        Log.d(TAG, "Activity restarted with new language");
    }

    /**
     * Get saved language preference
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String language = prefs.getString(KEY_LANGUAGE, getSystemLanguage());
        Log.d(TAG, "Current language: " + language);
        return language;
    }

    /**
     * Apply language to context
     * MUST be called in attachBaseContext() or onCreate() BEFORE setContentView()
     */
    public static Context applyLanguage(Context context) {
        String languageCode = getLanguage(context);
        return applyLanguage(context, languageCode);
    }

    /**
     * Apply specific language to context
     */
    public static Context applyLanguage(Context context, String languageCode) {
        Log.d(TAG, "Applying language: " + languageCode);

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLayoutDirection(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLayoutDirection(locale);
            }
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }

    /**
     * Check if language is RTL (Right-to-Left)
     */
    public static boolean isRTL(String languageCode) {
        return PERSIAN.equals(languageCode) || ARABIC.equals(languageCode);
    }

    /**
     * Check if current language is RTL
     */
    public static boolean isRTL(Context context) {
        String language = getLanguage(context);
        return isRTL(language);
    }

    /**
     * Get system language
     */
    private static String getSystemLanguage() {
        String systemLang = Locale.getDefault().getLanguage();
        // Return supported language or default to English
        if (PERSIAN.equals(systemLang) || ARABIC.equals(systemLang)) {
            return systemLang;
        }
        return ENGLISH;
    }

    /**
     * Get language display name
     */
    public static String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case ENGLISH:
                return "English";
            case PERSIAN:
                return "فارسی (Persian)";
            case ARABIC:
                return "العربية (Arabic)";
            default:
                return "English";
        }
    }

    /**
     * Get all available languages
     */
    public static String[] getAvailableLanguages() {
        return new String[]{ENGLISH, PERSIAN};
    }

    /**
     * Get display names for all languages
     */
    public static String[] getAvailableLanguageNames() {
        return new String[]{
                getLanguageDisplayName(ENGLISH),
                getLanguageDisplayName(PERSIAN)
        };
    }

    /**
     * Update configuration for activity
     * Call this in onCreate() BEFORE setContentView()
     */
    public static void updateConfiguration(Activity activity) {
        String language = getLanguage(activity);
        Log.d(TAG, "Updating configuration with language: " + language);

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = activity.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            configuration.setLayoutDirection(locale);
            activity.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLayoutDirection(locale);
            }
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }
}