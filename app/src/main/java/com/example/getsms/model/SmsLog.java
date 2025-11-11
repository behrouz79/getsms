package com.example.getsms.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Enhanced SMS Log with retry and backup tracking
 */
@Entity(tableName = "sms_logs")
public class SmsLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // SMS Details
    @ColumnInfo(name = "sender")
    public String sender;

    @ColumnInfo(name = "message_body")
    public String messageBody;

    @ColumnInfo(name = "sim_slot")
    public String simSlot;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "formatted_date")
    public String formattedDate;

    // Processing Details
    @ColumnInfo(name = "matched_rule_id")
    public int matchedRuleId;

    @ColumnInfo(name = "matched_rule_name")
    public String matchedRuleName;

    @ColumnInfo(name = "actions_executed")
    public String actionsExecuted;

    // Action Results
    @ColumnInfo(name = "webhook_sent")
    public boolean webhookSent;

    @ColumnInfo(name = "webhook_status")
    public int webhookStatus;

    @ColumnInfo(name = "telegram_sent")
    public boolean telegramSent;

    @ColumnInfo(name = "sms_forwarded")
    public boolean smsForwarded;

    @ColumnInfo(name = "whatsapp_sent")
    public boolean whatsappSent;

    // Transformation Details
    @ColumnInfo(name = "was_transformed")
    public boolean wasTransformed;

    @ColumnInfo(name = "original_message")
    public String originalMessage;

    @ColumnInfo(name = "transformed_message")
    public String transformedMessage;

    @ColumnInfo(name = "transform_type")
    public String transformType;

    // Error Tracking
    @ColumnInfo(name = "has_error")
    public boolean hasError;

    @ColumnInfo(name = "error_message")
    public String errorMessage;

    // Credits
    @ColumnInfo(name = "credits_used")
    public int creditsUsed;

    // ============================================
    // NEW: RETRY TRACKING
    // ============================================

    @ColumnInfo(name = "retry_count")
    public int retryCount;

    @ColumnInfo(name = "retry_attempts")
    public String retryAttempts; // JSON: [{action, attempt, success, timestamp}]

    @ColumnInfo(name = "succeeded_after_retry")
    public boolean succeededAfterRetry;

    @ColumnInfo(name = "retry_strategy_used")
    public String retryStrategyUsed;

    // ============================================
    // NEW: BACKUP ACTION TRACKING
    // ============================================

    @ColumnInfo(name = "backup_action_used")
    public boolean backupActionUsed;

    @ColumnInfo(name = "backup_action_type")
    public String backupActionType;

    @ColumnInfo(name = "backup_action_destination")
    public String backupActionDestination;

    @ColumnInfo(name = "backup_action_success")
    public boolean backupActionSuccess;

    @ColumnInfo(name = "backup_action_failed")
    public boolean backupActionFailed;

    @ColumnInfo(name = "backup_triggered_at")
    public long backupTriggeredAt;

    public SmsLog() {
        this.timestamp = System.currentTimeMillis();
        this.hasError = false;
        this.creditsUsed = 0;
        this.retryCount = 0;
        this.backupActionUsed = false;
        this.succeededAfterRetry = false;
    }

    /**
     * Create from SmsMessage
     */
    public static SmsLog fromSmsMessage(com.example.getsms.model.SmsMessage sms) {
        SmsLog log = new SmsLog();
        log.sender = sms.getSender();
        log.messageBody = sms.getBody();
        log.originalMessage = sms.getBody();
        log.simSlot = sms.getSimSlot();
        log.timestamp = sms.getTimestamp();
        log.formattedDate = sms.getFormattedDate();
        return log;
    }

    /**
     * Get comprehensive summary
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("From: ").append(sender).append("\n");
        summary.append("SIM: ").append(simSlot).append("\n");
        summary.append("Time: ").append(formattedDate).append("\n");

        if (matchedRuleName != null) {
            summary.append("Rule: ").append(matchedRuleName).append("\n");
        }

        if (wasTransformed) {
            summary.append("Transformed: Yes\n");
        }

        // Retry information
        if (retryCount > 0) {
            summary.append("Retries: ").append(retryCount);
            if (succeededAfterRetry) {
                summary.append(" (Succeeded)\n");
            } else {
                summary.append(" (Failed)\n");
            }
        }

        // Backup information
        if (backupActionUsed) {
            summary.append("Backup: ").append(backupActionType);
            if (backupActionSuccess) {
                summary.append(" ✅\n");
            } else {
                summary.append(" ❌\n");
            }
        }

        summary.append("Credits: ").append(creditsUsed).append("\n");

        if (hasError) {
            summary.append("Error: ").append(errorMessage);
        } else {
            summary.append("Status: Success");
        }

        return summary.toString();
    }

    /**
     * Get actions summary with retry/backup info
     */
    public String getActionsSummary() {
        StringBuilder actions = new StringBuilder();

        if (webhookSent) {
            actions.append("Webhook (").append(webhookStatus).append(")");
            if (retryCount > 0 && succeededAfterRetry) {
                actions.append(" [Retry]");
            }
            actions.append(", ");
        }
        if (telegramSent) {
            actions.append("Telegram");
            if (retryCount > 0 && succeededAfterRetry) {
                actions.append(" [Retry]");
            }
            actions.append(", ");
        }
        if (smsForwarded) {
            actions.append("SMS, ");
        }
        if (whatsappSent) {
            actions.append("WhatsApp, ");
        }

        // Add backup info
        if (backupActionUsed) {
            actions.append("🔀 Backup: ").append(backupActionType);
            if (backupActionSuccess) {
                actions.append(" ✅, ");
            } else {
                actions.append(" ❌, ");
            }
        }

        if (actions.length() > 0) {
            actions.setLength(actions.length() - 2);
            return actions.toString();
        }

        return "No actions";
    }

    /**
     * Get detailed retry information
     */
    public String getRetryInfo() {
        if (retryCount == 0) {
            return "No retries needed";
        }

        StringBuilder info = new StringBuilder();
        info.append("Retry attempts: ").append(retryCount).append("\n");
        info.append("Strategy: ").append(retryStrategyUsed != null ? retryStrategyUsed : "Unknown").append("\n");

        if (succeededAfterRetry) {
            info.append("Result: ✅ Succeeded after retry");
        } else {
            info.append("Result: ❌ Failed after all retries");
        }

        return info.toString();
    }

    /**
     * Get backup action information
     */
    public String getBackupInfo() {
        if (!backupActionUsed) {
            return "No backup action used";
        }

        StringBuilder info = new StringBuilder();
        info.append("Backup triggered: Yes\n");
        info.append("Type: ").append(backupActionType).append("\n");
        info.append("Destination: ").append(backupActionDestination != null ? backupActionDestination : "Unknown").append("\n");

        if (backupActionSuccess) {
            info.append("Result: ✅ Backup succeeded");
        } else if (backupActionFailed) {
            info.append("Result: ❌ Backup failed");
        } else {
            info.append("Result: ⏳ Pending");
        }

        return info.toString();
    }
}