package com.example.getsms.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.getsms.R;
import com.example.getsms.credit.CreditManager;
import com.example.getsms.model.Action;
import com.example.getsms.model.ActionLog;
import com.example.getsms.model.SmsLog;
import com.example.getsms.model.SmsMessage;
import com.example.getsms.roomDB.DataBase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced Action Executor with comprehensive logging
 */
public class ActionExecutor {

    private static final String TAG = "ActionExecutor";

    private final Context context;
    private final CreditManager creditManager;
    private final DataBase db;
    private final ActionLogger actionLogger;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ActionExecutor(Context context) {
        this.context = context;
        this.creditManager = new CreditManager(context);
        this.db = DataBase.getDbInstance(context);
        this.actionLogger = new ActionLogger(context);
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Execute action with comprehensive logging
     */
    public void execute(Action action, String message, SmsMessage sms, SmsLog smsLog, ActionCallback callback) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🚀 EXECUTING ACTION: " + action.type);
        Log.d(TAG, "Retry enabled: " + action.enableRetry);
        Log.d(TAG, "Backup enabled: " + action.enableBackup);
        Log.d(TAG, "========================================");

        executeWithRetry(action, message, sms, smsLog, 1, false, null, callback);
    }

    /**
     * Execute action with retry logic and detailed logging
     */
    private void executeWithRetry(Action action, String message, SmsMessage sms,
                                  SmsLog smsLog, int attemptNumber, boolean isBackup,
                                  String originalActionType, ActionCallback callback) {

        Log.d(TAG, "📤 Attempt " + attemptNumber + "/" + (action.maxRetries + 1) + " for " + action.type);

        // Create action log for this attempt
        ActionLog actionLog = actionLogger.startActionLog(
                smsLog.id,
                action,
                smsLog.matchedRuleName,
                attemptNumber,
                isBackup
        );

        if (isBackup && originalActionType != null) {
            actionLogger.markAsBackup(actionLog, originalActionType);
        }

        executorService.execute(() -> {
            long startTime = System.currentTimeMillis();
            int creditsUsed = getCreditCost(action.type);

            boolean success = executeAction(action, message, sms, smsLog);

            if (success) {
                Log.d(TAG, "✅ Action succeeded on attempt " + attemptNumber);

                // Log success
                actionLogger.logSuccess(actionLog, 200, "OK", startTime, creditsUsed);

                // Update SMS log
                updateLogSuccess(smsLog, action);

                if (callback != null) {
                    callback.onSuccess(attemptNumber);
                }

            } else {
                Log.e(TAG, "❌ Action failed on attempt " + attemptNumber);

                // Log failure
                actionLogger.logFailure(actionLog, new Exception("Action execution failed"),
                        startTime, creditsUsed);

                // Check if we should retry
                if (action.enableRetry && attemptNumber <= action.maxRetries) {
                    long delay = action.getRetryDelay(attemptNumber);

                    Log.d(TAG, "🔄 Scheduling retry in " + (delay / 1000) + " seconds...");

                    // Schedule retry
                    mainHandler.postDelayed(() -> {
                        executeWithRetry(action, message, sms, smsLog, attemptNumber + 1,
                                isBackup, originalActionType, callback);
                    }, delay);

                } else {
                    Log.e(TAG, "❌ All retry attempts exhausted for " + action.type);

                    // Update SMS log with failure
                    updateLogFailure(smsLog, action, "Failed after " + attemptNumber + " attempts");

                    // Execute backup action if configured
                    if (!isBackup && action.enableBackup && action.hasValidBackup()) {
                        if (action.backupAfterAllRetries) {
                            Log.d(TAG, "🔀 Executing backup action: " + action.backupType);
                            executeBackupAction(action, message, sms, smsLog, callback);
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
    private boolean executeAction(Action action, String message, SmsMessage sms, SmsLog smsLog) {
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
                                     SmsLog smsLog, ActionCallback callback) {

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

        // Execute backup with logging
        executeWithRetry(backupAction, backupMessage, sms, smsLog, 1, true,
                action.type.toString(), new ActionCallback() {
                    @Override
                    public void onSuccess(int attempts) {
                        Log.d(TAG, "✅ Backup action succeeded");
                        smsLog.backupActionUsed = true;
                        smsLog.backupActionType = backupAction.type.toString();
                        updateLogSuccess(smsLog, backupAction);

                        if (callback != null) {
                            callback.onBackupSuccess();
                        }
                    }

                    @Override
                    public void onFailure(int attempts, String reason) {
                        Log.e(TAG, "❌ Backup action also failed");
                        smsLog.backupActionUsed = true;
                        smsLog.backupActionType = backupAction.type.toString();
                        smsLog.backupActionFailed = true;
                        updateLogFailure(smsLog, backupAction, "Backup action failed");

                        if (callback != null) {
                            callback.onFailure(attempts, reason);
                        }
                    }

                    @Override
                    public void onBackupSuccess() {
                        // Not used
                    }
                });
    }

    /**
     * Update SMS log on success
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

        executorService.execute(() -> db.smsLogDao().updateLog(log));
    }

    /**
     * Update SMS log on failure
     */
    private void updateLogFailure(SmsLog log, Action action, String errorMessage) {
        log.hasError = true;

        if (log.errorMessage == null) {
            log.errorMessage = action.type + ": " + errorMessage;
        } else {
            log.errorMessage += "\n" + action.type + ": " + errorMessage;
        }

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
            case WHATSAPP:
                return CreditManager.COST_PER_TELEGRAM;
            default:
                return 1;
        }
    }

    /**
     * Callback interface
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