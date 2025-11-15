package com.example.getsms.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.getsms.R;
import com.example.getsms.credit.CreditManager;
import com.example.getsms.model.Action;
import com.example.getsms.model.SmsLog;
import com.example.getsms.model.SmsMessage;
import com.example.getsms.roomDB.DataBase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles action execution with retry logic and backup actions
 */
public class ActionExecutor {

    private static final String TAG = "ActionExecutor";

    private final Context context;
    private final CreditManager creditManager;
    private final DataBase db;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ActionExecutor(Context context) {
        this.context = context;
        this.creditManager = new CreditManager(context);
        this.db = DataBase.getDbInstance(context);
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Execute action with retry and backup support
     */
    public void execute(Action action, String message, SmsMessage sms, SmsLog log, ActionCallback callback) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🚀 EXECUTING ACTION: " + action.type);
        Log.d(TAG, "Retry enabled: " + action.enableRetry);
        Log.d(TAG, "Backup enabled: " + action.enableBackup);
        Log.d(TAG, "========================================");

        executeWithRetry(action, message, sms, log, 1, callback);
    }

    /**
     * Execute action with retry logic
     */
    private void executeWithRetry(Action action, String message, SmsMessage sms,
                                  SmsLog log, int attemptNumber, ActionCallback callback) {

        Log.d(TAG, "📤 Attempt " + attemptNumber + "/" + (action.maxRetries + 1) +
                " for " + action.type);

        executorService.execute(() -> {
            boolean success = executeAction(action, message, sms, log);

            if (success) {
                Log.d(TAG, "✅ Action succeeded on attempt " + attemptNumber);

                // Update log
                updateLogSuccess(log, action);

                if (callback != null) {
                    callback.onSuccess(attemptNumber);
                }

            } else {
                Log.e(TAG, "❌ Action failed on attempt " + attemptNumber);

                // Check if we should retry
                if (action.enableRetry && attemptNumber <= action.maxRetries) {
                    long delay = action.getRetryDelay(attemptNumber);

                    Log.d(TAG, "🔄 Scheduling retry in " + (delay / 1000) + " seconds...");

                    // Schedule retry
                    mainHandler.postDelayed(() -> {
                        executeWithRetry(action, message, sms, log, attemptNumber + 1, callback);
                    }, delay);

                } else {
                    Log.e(TAG, "❌ All retry attempts exhausted for " + action.type);

                    // Update log with failure
                    updateLogFailure(log, action, "Failed after " + attemptNumber + " attempts");

                    // Execute backup action if configured
                    if (action.enableBackup && action.hasValidBackup()) {
                        if (action.backupAfterAllRetries) {
                            Log.d(TAG, "🔀 Executing backup action: " + action.backupType);
                            executeBackupAction(action, message, sms, log, callback);
                        }
                    } else {
                        if (callback != null) {
                            callback.onFailure(attemptNumber, "No backup configured");
                        }
                    }
                }
            }
        });
    }

    /**
     * Execute the actual action
     */
    private boolean executeAction(Action action, String message, SmsMessage sms, SmsLog log) {
        try {
            Log.d(TAG, "   Destination: " + action.destination);
            Log.d(TAG, "   Message: " + message);

            switch (action.type) {
                case WEBHOOK:
                    SyncExecutors.WebhookExecutor webhookExecutor = new SyncExecutors.WebhookExecutor(context);
                    return webhookExecutor.executeSync(action, message, sms);

                case SMS:
                    SyncExecutors.SmsExecutor smsExecutor = new SyncExecutors.SmsExecutor(context);
                    return smsExecutor.executeSync(action, message, sms);

                case TELEGRAM:
                    SyncExecutors.TelegramExecutor telegramExecutor = new SyncExecutors.TelegramExecutor(context);
                    return telegramExecutor.executeSync(action, message, sms);

                case WHATSAPP:
                    SyncExecutors.WhatsAppExecutor whatsappExecutor = new SyncExecutors.WhatsAppExecutor(context);
                    return whatsappExecutor.executeSync(action, message, sms);

                default:
                    Log.e(TAG, "Unknown action type: " + action.type);
                    return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception during action execution", e);
            return false;
        }
    }

    /**
     * Execute backup action
     */
    private void executeBackupAction(Action action, String message, SmsMessage sms,
                                     SmsLog log, ActionCallback callback) {

        Log.d(TAG, "========================================");
        Log.d(TAG, "🔀 BACKUP ACTION TRIGGERED");
        Log.d(TAG, "Type: " + action.backupType);
        Log.d(TAG, "Destination: " + action.backupDestination);
        Log.d(TAG, "========================================");

        // Create backup action
        Action backupAction = new Action();
        backupAction.type = action.backupType;
        backupAction.destination = action.backupDestination;
        backupAction.template = action.backupTemplate != null ?
                action.backupTemplate :
                context.getString(R.string.primary_action_failed);
        backupAction.enableRetry = action.retryBackup;
        backupAction.maxRetries = action.retryBackup ? 2 : 0;
        backupAction.retryDelaySeconds = 5;

        // Process backup template
        String backupMessage = backupAction.template
                .replace("{message}", message)
                .replace("{sender}", sms.getSender())
                .replace("{original_action}", action.type.toString())
                .replace("{sim}", sms.getSimSlot());

        // Check credits for backup
        int backupCost = getCreditCost(backupAction.type);
        if (!creditManager.hasEnoughCredits(backupCost)) {
            Log.e(TAG, "❌ Insufficient credits for backup action");
            if (callback != null) {
                callback.onFailure(0, "Insufficient credits for backup");
            }
            return;
        }

        // Deduct credits
        if (!creditManager.deductCredits(backupCost, "Backup action: " + backupAction.type)) {
            Log.e(TAG, "❌ Failed to deduct credits for backup");
            if (callback != null) {
                callback.onFailure(0, "Credit deduction failed");
            }
            return;
        }

        // Execute backup
        boolean backupSuccess = executeAction(backupAction, backupMessage, sms, log);

        if (backupSuccess) {
            Log.d(TAG, "✅ Backup action succeeded");
            log.backupActionUsed = true;
            log.backupActionType = backupAction.type.toString();
            updateLogSuccess(log, backupAction);

            if (callback != null) {
                callback.onBackupSuccess();
            }
        } else {
            Log.e(TAG, "❌ Backup action also failed");
            log.backupActionUsed = true;
            log.backupActionType = backupAction.type.toString();
            log.backupActionFailed = true;
            updateLogFailure(log, backupAction, "Backup action failed");

            if (callback != null) {
                callback.onFailure(0, "Backup action failed");
            }
        }
    }

    /**
     * Update log on success
     */
    private void updateLogSuccess(SmsLog log, Action action) {
        switch (action.type) {
            case WEBHOOK:
                log.webhookSent = true;
                log.webhookStatus = 200;
                break;
            case SMS:
                log.smsForwarded = true;
                break;
            case TELEGRAM:
                log.telegramSent = true;
                break;
            case WHATSAPP:
                log.whatsappSent = true;
                break;
        }

        // Save to database
        executorService.execute(() -> db.smsLogDao().updateLog(log));
    }

    /**
     * Update log on failure
     */
    private void updateLogFailure(SmsLog log, Action action, String errorMessage) {
        log.hasError = true;

        if (log.errorMessage == null) {
            log.errorMessage = action.type + ": " + errorMessage;
        } else {
            log.errorMessage += "\n" + action.type + ": " + errorMessage;
        }

        // Save to database
        executorService.execute(() -> db.smsLogDao().updateLog(log));
    }

    /**
     * Get credit cost for action type
     */
    private int getCreditCost(Action.ActionType type) {
        switch (type) {
            case SMS:
                return CreditManager.COST_PER_SMS;
            case WEBHOOK:
                return CreditManager.COST_PER_WEBHOOK;
            case TELEGRAM:
                return CreditManager.COST_PER_TELEGRAM;
            case WHATSAPP:
                return CreditManager.COST_PER_TELEGRAM;
            default:
                return 1;
        }
    }

    /**
     * Callback interface for action execution
     */
    public interface ActionCallback {
        void onSuccess(int attempts);
        void onFailure(int attempts, String reason);
        void onBackupSuccess();
    }

    /**
     * Shutdown executor
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}