package com.example.getsms.engine;

import android.content.Context;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActionExecutor {

    private static final String TAG = "ActionExecutor";

    private final Context context;
    private final CreditManager creditManager;
    private final DataBase db;
    private final ActionLogger actionLogger;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;

    public ActionExecutor(Context context) {
        this.context = context;
        this.creditManager = new CreditManager(context);
        this.db = DataBase.getDbInstance(context);
        this.actionLogger = new ActionLogger(context);
        this.executorService = Executors.newCachedThreadPool();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void execute(Action action, String message, SmsMessage sms, SmsLog smsLog, ActionCallback callback) {
        int cost = getCreditCost(action.type);
        if (!creditManager.deductCredits(cost, "Action: " + action.type)) {
            Log.e(TAG, "Insufficient credits for " + action.type);
            if (callback != null) callback.onFailure(0, "Insufficient credits");
            return;
        }
        smsLog.creditsUsed += cost;
        executeWithRetry(action, message, sms, smsLog, 1, false, null, callback);
    }

    private void executeWithRetry(Action action, String message, SmsMessage sms,
                                  SmsLog smsLog, int attemptNumber, boolean isBackup,
                                  String originalActionType, ActionCallback callback) {

        ActionLog actionLog = actionLogger.startActionLog(
                smsLog.id, action, smsLog.matchedRuleName, attemptNumber, isBackup);

        if (isBackup && originalActionType != null) {
            actionLogger.markAsBackup(actionLog, originalActionType);
        }

        executorService.execute(() -> {
            long startTime = System.currentTimeMillis();
            int creditsUsed = getCreditCost(action.type);
            boolean success = executeAction(action, message, sms);

            if (success) {
                actionLogger.logSuccess(actionLog, 200, "OK", startTime, creditsUsed);
                updateLogSuccess(smsLog, action);
                if (callback != null) callback.onSuccess(attemptNumber);

            } else {
                actionLogger.logFailure(actionLog,
                        new Exception("Action execution failed"), startTime, creditsUsed);

                if (action.enableRetry && attemptNumber <= action.maxRetries) {
                    long delayMs = action.getRetryDelay(attemptNumber);
                    Log.d(TAG, "Retry " + attemptNumber + "/" + action.maxRetries
                            + " in " + (delayMs / 1000) + "s for " + action.type);
                    scheduler.schedule(
                            () -> executeWithRetry(action, message, sms, smsLog,
                                    attemptNumber + 1, isBackup, originalActionType, callback),
                            delayMs, TimeUnit.MILLISECONDS);
                } else {
                    updateLogFailure(smsLog, action, "Failed after " + attemptNumber + " attempts");
                    if (!isBackup && action.enableBackup && action.hasValidBackup()
                            && action.backupAfterAllRetries) {
                        executeBackupAction(action, message, sms, smsLog, callback);
                    } else if (callback != null) {
                        callback.onFailure(attemptNumber, "No backup configured");
                    }
                }
            }
        });
    }

    private boolean executeAction(Action action, String message, SmsMessage sms) {
        try {
            switch (action.type) {
                case WEBHOOK:
                    return new SyncExecutors.WebhookExecutor(context).executeSync(action, message, sms);
                case SMS:
                    return new SyncExecutors.SmsExecutor(context).executeSync(action, message, sms);
                case TELEGRAM:
                    return new SyncExecutors.TelegramExecutor(context).executeSync(action, message, sms);
                case WHATSAPP:
                    return new SyncExecutors.WhatsAppExecutor(context).executeSync(action, message, sms);
                default:
                    Log.e(TAG, "Unknown action type: " + action.type);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during action execution", e);
            return false;
        }
    }

    private void executeBackupAction(Action action, String message, SmsMessage sms,
                                     SmsLog smsLog, ActionCallback callback) {
        Action backupAction = new Action();
        backupAction.type = action.backupType;
        backupAction.destination = action.backupDestination;
        backupAction.template = action.backupTemplate != null
                ? action.backupTemplate
                : context.getString(R.string.primary_action_failed);
        backupAction.enableRetry = action.retryBackup;
        backupAction.maxRetries = action.retryBackup ? 2 : 0;
        backupAction.retryDelaySeconds = 5;

        String backupMessage = backupAction.template
                .replace("{message}", message)
                .replace("{sender}", sms.getSender())
                .replace("{original_action}", action.type.toString())
                .replace("{sim}", sms.getSimSlot());

        int backupCost = getCreditCost(backupAction.type);
        if (!creditManager.deductCredits(backupCost, "Backup action: " + backupAction.type)) {
            Log.e(TAG, "Insufficient credits for backup action");
            if (callback != null) callback.onFailure(0, "Insufficient credits for backup");
            return;
        }

        executeWithRetry(backupAction, backupMessage, sms, smsLog, 1, true,
                action.type.toString(), new ActionCallback() {
                    @Override
                    public void onSuccess(int attempts) {
                        smsLog.backupActionUsed = true;
                        smsLog.backupActionType = backupAction.type.toString();
                        updateLogSuccess(smsLog, backupAction);
                        if (callback != null) callback.onBackupSuccess();
                    }

                    @Override
                    public void onFailure(int attempts, String reason) {
                        smsLog.backupActionUsed = true;
                        smsLog.backupActionType = backupAction.type.toString();
                        smsLog.backupActionFailed = true;
                        updateLogFailure(smsLog, backupAction, "Backup action failed");
                        if (callback != null) callback.onFailure(attempts, reason);
                    }

                    @Override
                    public void onBackupSuccess() {}
                });
    }

    private void updateLogSuccess(SmsLog log, Action action) {
        switch (action.type) {
            case WEBHOOK:  log.webhookSent = true; log.webhookStatus = 200; break;
            case SMS:      log.smsForwarded = true; break;
            case TELEGRAM: log.telegramSent = true; break;
            case WHATSAPP: log.whatsappSent = true; break;
        }
        executorService.execute(() -> db.smsLogDao().updateLog(log));
    }

    private void updateLogFailure(SmsLog log, Action action, String errorMessage) {
        log.hasError = true;
        if (log.errorMessage == null) {
            log.errorMessage = action.type + ": " + errorMessage;
        } else {
            log.errorMessage += "\n" + action.type + ": " + errorMessage;
        }
        executorService.execute(() -> db.smsLogDao().updateLog(log));
    }

    static int getCreditCost(Action.ActionType type) {
        switch (type) {
            case SMS:     return CreditManager.COST_PER_SMS;
            case WEBHOOK: return CreditManager.COST_PER_WEBHOOK;
            case TELEGRAM:
            case WHATSAPP: return CreditManager.COST_PER_TELEGRAM;
            default: return 1;
        }
    }

    public interface ActionCallback {
        void onSuccess(int attempts);
        void onFailure(int attempts, String reason);
        void onBackupSuccess();
    }

    public void shutdown() {
        if (!executorService.isShutdown()) executorService.shutdown();
        if (!scheduler.isShutdown()) scheduler.shutdown();
    }
}
