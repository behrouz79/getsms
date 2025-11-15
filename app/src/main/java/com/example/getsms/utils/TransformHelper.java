package com.example.getsms.utils;

import android.content.Context;

import com.example.getsms.R;

public class TransformHelper {

    public static String getHelpText(Context context, String type) {
        switch (type) {
            case "EXTRACT_LINES":
                return context.getString(R.string.transform_extract_lines);
            case "REMOVE_LINES":
                return context.getString(R.string.transform_remove_lines);
            case "EXTRACT_PATTERN":
                return context.getString(R.string.transform_extract_pattern);
            case "REMOVE_PATTERN":
                return context.getString(R.string.transform_remove_pattern);
            case "REPLACE_PATTERN":
                return context.getString(R.string.transform_replace_pattern);
            case "KEEP_UNTIL":
                return context.getString(R.string.transform_keep_until);
            case "KEEP_AFTER":
                return context.getString(R.string.transform_keep_after);
            case "REMOVE_AFTER":
                return context.getString(R.string.transform_remove_after);
            default:
                return context.getString(R.string.transform_default);
        }
    }
}