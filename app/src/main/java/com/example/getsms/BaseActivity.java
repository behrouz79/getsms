package com.example.getsms;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.getsms.utils.LanguageManager;

/**
 * Base Activity that applies language settings to all activities
 * ALL your activities should extend this instead of AppCompatActivity
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // CRITICAL: Apply language BEFORE activity is created
        Context context = LanguageManager.applyLanguage(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Update configuration as backup
        LanguageManager.updateConfiguration(this);
    }

    /**
     * Change language and restart activity
     */
    protected void changeLanguage(String languageCode) {
        LanguageManager.setLanguage(this, languageCode);
    }

    /**
     * Get current language
     */
    protected String getCurrentLanguage() {
        return LanguageManager.getLanguage(this);
    }

    /**
     * Check if current language is RTL
     */
    protected boolean isRTL() {
        return LanguageManager.isRTL(this);
    }
}