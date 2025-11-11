package com.example.getsms.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * Language Manager - Handles app language switching
 */
public class LanguageManager {
    private static final String PREFS_NAME = "language_prefs";
    private static final String KEY_LANGUAGE = "selected_language";

    // Supported languages
    public static final String ENGLISH = "en";
    public static final String PERSIAN = "fa";

    private final Context context;
    private final SharedPreferences prefs;

    public LanguageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Set app language
     */
    public void setLanguage(String languageCode) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        updateResources(languageCode);
    }

    /**
     * Get current language
     */
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, ENGLISH);
    }

    /**
     * Update resources with selected language
     */
    public void updateResources(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }

    /**
     * Apply language on activity start
     */
    public static void applyLanguage(Activity activity) {
        LanguageManager manager = new LanguageManager(activity);
        String language = manager.getLanguage();
        manager.updateResources(language);
    }

    /**
     * Get language display name
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case ENGLISH:
                return "English";
            case PERSIAN:
                return "فارسی";
            default:
                return "English";
        }
    }

    /**
     * Restart activity to apply language change
     */
    public static void restartActivity(Activity activity) {
        activity.recreate();
    }
}