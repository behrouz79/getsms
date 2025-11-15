package com.example.getsms.utils;

import android.content.Context;

import com.example.getsms.R;

public class RetryHelper {

    public static String getStrategyHelp(Context context, String strategy) {
        switch (strategy) {
            case "IMMEDIATE":
                return context.getString(R.string.retry_immediate);
            case "EXPONENTIAL_BACKOFF":
                return context.getString(R.string.retry_exponential_backoff);
            case "FIXED_DELAY":
            default:
                return context.getString(R.string.retry_fixed_delay);
        }
    }
}