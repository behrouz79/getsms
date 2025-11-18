package com.example.getsms.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Enhanced Action Execution Log
 * Tracks detailed execution information for each action
 */
@Entity(tableName = "action_logs")
public class ActionLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Link to SMS log
    @ColumnInfo(name = "sms_log_id")
    public int smsLogId;

    // Action Details
    @ColumnInfo(name = "action_type")
    public String actionType; // WEBHOOK, SMS, TELEGRAM, WHATSAPP

    @ColumnInfo(name = "action_destination")
    public String actionDestination;

    @ColumnInfo(name = "rule_name")
    public String ruleName;

    // Execution Tracking
    @ColumnInfo(name = "attempt_number")
    public int attemptNumber; // 1, 2, 3... for retries

    @ColumnInfo(name = "execution_time")
    public long executionTime; // When this attempt was made

    @ColumnInfo(name = "duration_ms")
    public long durationMs; // How long the action took

    // Result
    @ColumnInfo(name = "success")
    public boolean success;

    @ColumnInfo(name = "status_code")
    public int statusCode; // HTTP status code for webhooks

    @ColumnInfo(name = "response_body")
    public String responseBody; // Response from server

    @ColumnInfo(name = "error_message")
    public String errorMessage; // Error details if failed

    @ColumnInfo(name = "error_type")
    public String errorType; // NETWORK, TIMEOUT, AUTH, UNKNOWN

    // Retry Information
    @ColumnInfo(name = "is_retry")
    public boolean isRetry;

    @ColumnInfo(name = "retry_strategy")
    public String retryStrategy; // FIXED_DELAY, EXPONENTIAL_BACKOFF

    @ColumnInfo(name = "retry_delay_seconds")
    public int retryDelaySeconds;

    // Backup Action
    @ColumnInfo(name = "is_backup_action")
    public boolean isBackupAction;

    @ColumnInfo(name = "original_action_type")
    public String originalActionType; // If this is a backup

    // Transformation
    @ColumnInfo(name = "message_transformed")
    public boolean messageTransformed;

    @ColumnInfo(name = "original_message")
    public String originalMessage;

    @ColumnInfo(name = "transformed_message")
    public String transformedMessage;

    // Credits
    @ColumnInfo(name = "credits_used")
    public int creditsUsed;

    public ActionLog() {
        this.executionTime = System.currentTimeMillis();
    }

    /**
     * Get human-readable status
     */
    public String getStatusText() {
        if (success) {
            return "✅ SUCCESS" + (isRetry ? " (after retry)" : "");
        } else {
            return "❌ FAILED" + (isRetry ? " (retry attempt " + attemptNumber + ")" : "");
        }
    }

    /**
     * Get error type emoji
     */
    public String getErrorTypeEmoji() {
        if (errorType == null) return "";

        switch (errorType) {
            case "NETWORK": return "🌐";
            case "TIMEOUT": return "⏱️";
            case "AUTH": return "🔒";
            case "PERMISSION": return "🚫";
            case "SERVER_ERROR": return "🔥";
            case "CLIENT_ERROR": return "⚠️";
            default: return "❓";
        }
    }

    /**
     * Get detailed summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        // Status
        sb.append(getStatusText()).append("\n\n");

        // Action details
        sb.append("📋 Action: ").append(actionType).append("\n");
        sb.append("📍 Destination: ").append(actionDestination).append("\n");
        sb.append("🏷️ Rule: ").append(ruleName).append("\n\n");

        // Timing
        sb.append("⏱️ Duration: ").append(durationMs).append("ms\n");
        sb.append("🕐 Time: ").append(new java.text.SimpleDateFormat("HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date(executionTime))).append("\n\n");

        // Result
        if (success) {
            if (statusCode > 0) {
                sb.append("📊 Status Code: ").append(statusCode).append("\n");
            }
            if (responseBody != null && !responseBody.isEmpty()) {
                String truncated = responseBody.length() > 200
                        ? responseBody.substring(0, 197) + "..."
                        : responseBody;
                sb.append("📄 Response:\n").append(truncated).append("\n");
            }
        } else {
            sb.append(getErrorTypeEmoji()).append(" Error Type: ").append(errorType).append("\n");
            sb.append("💬 Error: ").append(errorMessage).append("\n");

            if (statusCode > 0) {
                sb.append("📊 Status Code: ").append(statusCode).append("\n");
            }
        }

        // Retry info
        if (isRetry) {
            sb.append("\n🔄 Retry Info:\n");
            sb.append("  Attempt: ").append(attemptNumber).append("\n");
            sb.append("  Strategy: ").append(retryStrategy).append("\n");
            sb.append("  Delay: ").append(retryDelaySeconds).append("s\n");
        }

        // Backup info
        if (isBackupAction) {
            sb.append("\n🔀 Backup Action:\n");
            sb.append("  Original: ").append(originalActionType).append("\n");
        }

        // Transformation
        if (messageTransformed) {
            sb.append("\n🔄 Message Transformed:\n");
            sb.append("  Original: ").append(originalMessage).append("\n");
            sb.append("  Transformed: ").append(transformedMessage).append("\n");
        }

        // Credits
        sb.append("\n💳 Credits Used: ").append(creditsUsed);

        return sb.toString();
    }
}