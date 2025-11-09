package com.example.getsms.engine;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Message Transformer - Extracts, removes, or transforms SMS content
 */
public class MessageTransformer {

    private static final String TAG = "MessageTransformer";

    public enum TransformType {
        EXTRACT_LINES,      // Extract specific lines
        REMOVE_LINES,       // Remove specific lines
        EXTRACT_PATTERN,    // Extract text matching regex
        REMOVE_PATTERN,     // Remove text matching regex
        REPLACE_PATTERN,    // Replace text matching regex
        KEEP_UNTIL,         // Keep text until a pattern
        KEEP_AFTER,         // Keep text after a pattern
        REMOVE_AFTER,       // Remove text after a pattern
        NONE                // No transformation
    }

    /**
     * Transform message based on type and pattern
     */
    public static String transform(String message, TransformType type, String pattern) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (type == null || type == TransformType.NONE) {
            return message;
        }

        try {
            switch (type) {
                case EXTRACT_LINES:
                    return extractLines(message, pattern);

                case REMOVE_LINES:
                    return removeLines(message, pattern);

                case EXTRACT_PATTERN:
                    return extractPattern(message, pattern);

                case REMOVE_PATTERN:
                    return removePattern(message, pattern);

                case REPLACE_PATTERN:
                    return replacePattern(message, pattern);

                case KEEP_UNTIL:
                    return keepUntil(message, pattern);

                case KEEP_AFTER:
                    return keepAfter(message, pattern);

                case REMOVE_AFTER:
                    return removeAfter(message, pattern);

                default:
                    return message;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error transforming message", e);
            return message; // Return original on error
        }
    }

    /**
     * Extract specific lines (comma-separated line numbers: "1,2,3")
     * Example: "1,2" extracts first and second lines
     */
    private static String extractLines(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        String[] lines = message.split("\n");
        List<String> result = new ArrayList<>();

        try {
            String[] lineNumbers = pattern.split(",");
            for (String numStr : lineNumbers) {
                int lineNum = Integer.parseInt(numStr.trim()) - 1; // 0-indexed
                if (lineNum >= 0 && lineNum < lines.length) {
                    result.add(lines[lineNum]);
                }
            }
            return String.join("\n", result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid line numbers: " + pattern, e);
            return message;
        }
    }

    /**
     * Remove specific lines (comma-separated line numbers: "3,4")
     * Example: "3" removes third line
     */
    private static String removeLines(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        String[] lines = message.split("\n");
        List<Integer> linesToRemove = new ArrayList<>();

        try {
            String[] lineNumbers = pattern.split(",");
            for (String numStr : lineNumbers) {
                int lineNum = Integer.parseInt(numStr.trim()) - 1; // 0-indexed
                linesToRemove.add(lineNum);
            }

            List<String> result = new ArrayList<>();
            for (int i = 0; i < lines.length; i++) {
                if (!linesToRemove.contains(i)) {
                    result.add(lines[i]);
                }
            }
            return String.join("\n", result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid line numbers: " + pattern, e);
            return message;
        }
    }

    /**
     * Extract text matching regex pattern
     * Example: "حساب\\d+" extracts account numbers
     */
    private static String extractPattern(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        try {
            Pattern regex = Pattern.compile(pattern, Pattern.MULTILINE);
            Matcher matcher = regex.matcher(message);

            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(matcher.group());
            }

            return matches.isEmpty() ? message : String.join("\n", matches);
        } catch (Exception e) {
            Log.e(TAG, "Invalid regex pattern: " + pattern, e);
            return message;
        }
    }

    /**
     * Remove text matching regex pattern
     * Example: "موجودی:.*" removes balance line
     */
    private static String removePattern(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        try {
            return message.replaceAll(pattern, "").trim();
        } catch (Exception e) {
            Log.e(TAG, "Invalid regex pattern: " + pattern, e);
            return message;
        }
    }

    /**
     * Replace text matching regex pattern
     * Format: "pattern|replacement"
     * Example: "موجودی:.*|" removes balance completely
     */
    private static String replacePattern(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        try {
            String[] parts = pattern.split("\\|", 2);
            if (parts.length != 2) {
                Log.e(TAG, "Invalid replace pattern format. Use: pattern|replacement");
                return message;
            }

            String searchPattern = parts[0];
            String replacement = parts[1];

            return message.replaceAll(searchPattern, replacement).trim();
        } catch (Exception e) {
            Log.e(TAG, "Invalid replace pattern: " + pattern, e);
            return message;
        }
    }

    /**
     * Keep text until pattern is found (exclusive)
     * Example: "موجودی" keeps everything before "موجودی"
     */
    private static String keepUntil(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(message);

            if (matcher.find()) {
                return message.substring(0, matcher.start()).trim();
            }
            return message;
        } catch (Exception e) {
            Log.e(TAG, "Invalid pattern: " + pattern, e);
            return message;
        }
    }

    /**
     * Keep text after pattern is found (inclusive of match)
     * Example: "حساب" keeps from "حساب" onwards
     */
    private static String keepAfter(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(message);

            if (matcher.find()) {
                return message.substring(matcher.start()).trim();
            }
            return message;
        } catch (Exception e) {
            Log.e(TAG, "Invalid pattern: " + pattern, e);
            return message;
        }
    }

    /**
     * Remove text after pattern is found (inclusive of match)
     * Example: "موجودی" removes from "موجودی" onwards
     */
    private static String removeAfter(String message, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return message;
        }

        try {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(message);

            if (matcher.find()) {
                return message.substring(0, matcher.start()).trim();
            }
            return message;
        } catch (Exception e) {
            Log.e(TAG, "Invalid pattern: " + pattern, e);
            return message;
        }
    }

    /**
     * Chain multiple transformations
     */
    public static String chainTransforms(String message, List<Transform> transforms) {
        String result = message;

        for (Transform transform : transforms) {
            result = transform(result, transform.type, transform.pattern);
        }

        return result;
    }

    /**
     * Transform configuration class
     */
    public static class Transform {
        public TransformType type;
        public String pattern;

        public Transform(TransformType type, String pattern) {
            this.type = type;
            this.pattern = pattern;
        }
    }
}