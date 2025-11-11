package com.example.getsms.model;

import com.example.getsms.engine.MessageTransformer;

public class Action {

    public enum ActionType {
        WEBHOOK,
        SMS,
        WHATSAPP,
        TELEGRAM
    }

    public int id;
    public int ruleId;
    public ActionType type;
    public String template;
    public String destination;
    public boolean enabled;

    // For Webhook
    public String httpMethod;
    public String headers;

    // For Telegram
    public String botToken;
    public String chatId;

    // For WhatsApp
    public String whatsappApiUrl;
    public String whatsappApiKey;

    // Message Transformation
    public boolean enableTransform;
    public String transformType;
    public String transformPattern;
    public String transformChain;

    // ============================================
    // NEW: RETRY CONFIGURATION
    // ============================================

    /**
     * Enable automatic retry on failure
     */
    public boolean enableRetry;

    /**
     * Maximum number of retry attempts (1-5)
     */
    public int maxRetries;

    /**
     * Delay between retries in seconds (5-300)
     */
    public int retryDelaySeconds;

    /**
     * Retry strategy: "IMMEDIATE", "EXPONENTIAL_BACKOFF", "FIXED_DELAY"
     */
    public String retryStrategy;

    // ============================================
    // NEW: BACKUP ACTION CONFIGURATION
    // ============================================

    /**
     * Enable backup action on failure
     */
    public boolean enableBackup;

    /**
     * Backup action type (usually SMS as fallback)
     */
    public ActionType backupType;

    /**
     * Backup destination (phone number for SMS)
     */
    public String backupDestination;

    /**
     * Backup message template
     */
    public String backupTemplate;

    /**
     * Retry backup action if it fails
     */
    public boolean retryBackup;

    /**
     * Send backup only after all retries failed
     */
    public boolean backupAfterAllRetries;

    // ============================================
    // NEW: ERROR HANDLING
    // ============================================

    /**
     * Continue processing other actions on failure
     */
    public boolean continueOnFailure;

    /**
     * Notify user on action failure (via notification)
     */
    public boolean notifyOnFailure;

    /**
     * Log detailed error information
     */
    public boolean detailedErrorLog;

    public Action() {
        this.enabled = true;
        this.httpMethod = "POST";
        this.enableTransform = false;

        // Default retry configuration
        this.enableRetry = true;
        this.maxRetries = 3;
        this.retryDelaySeconds = 5;
        this.retryStrategy = "EXPONENTIAL_BACKOFF";

        // Default backup configuration
        this.enableBackup = true;
        this.backupType = ActionType.SMS;
        this.backupAfterAllRetries = true;
        this.retryBackup = false;

        // Default error handling
        this.continueOnFailure = true;
        this.notifyOnFailure = true;
        this.detailedErrorLog = true;
    }

    public Action(int ruleId, ActionType type, String template, String destination) {
        this();
        this.ruleId = ruleId;
        this.type = type;
        this.template = template;
        this.destination = destination;
    }

    /**
     * Get transform type enum
     */
    public MessageTransformer.TransformType getTransformType() {
        if (transformType == null || transformType.isEmpty()) {
            return MessageTransformer.TransformType.NONE;
        }
        try {
            return MessageTransformer.TransformType.valueOf(transformType);
        } catch (IllegalArgumentException e) {
            return MessageTransformer.TransformType.NONE;
        }
    }

    /**
     * Set transform type
     */
    public void setTransformType(MessageTransformer.TransformType type) {
        this.transformType = type != null ? type.name() : null;
    }

    /**
     * Calculate retry delay based on strategy
     */
    public long getRetryDelay(int attemptNumber) {
        switch (retryStrategy) {
            case "IMMEDIATE":
                return 0;

            case "EXPONENTIAL_BACKOFF":
                // 5s, 10s, 20s, 40s, 80s...
                return retryDelaySeconds * (long) Math.pow(2, attemptNumber - 1) * 1000L;

            case "FIXED_DELAY":
            default:
                return retryDelaySeconds * 1000L;
        }
    }

    /**
     * Check if action has valid backup configuration
     */
    public boolean hasValidBackup() {
        return enableBackup &&
                backupType != null &&
                backupDestination != null &&
                !backupDestination.isEmpty();
    }

    /**
     * Get action summary with retry/backup info
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(type.toString());

        if (enableRetry) {
            summary.append(" (Retry: ").append(maxRetries).append("x)");
        }

        if (enableBackup && hasValidBackup()) {
            summary.append(" → Backup: ").append(backupType);
        }

        return summary.toString();
    }
}