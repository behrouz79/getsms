package com.example.getsms.engine;

import android.content.Context;
import android.util.Log;

import com.example.getsms.model.ActionLog;
import com.example.getsms.model.Action;
import com.example.getsms.roomDB.DataBase;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Centralized Action Logging Service
 */
public class ActionLogger {

    private static final String TAG = "ActionLogger";
    private final Context context;
    private final DataBase db;

    public ActionLogger(Context context) {
        this.context = context;
        this.db = DataBase.getDbInstance(context);
    }

    /**
     * Start logging an action execution
     */
    public ActionLog startActionLog(int smsLogId, Action action, String ruleName,
                                    int attemptNumber, boolean isBackup) {
        ActionLog log = new ActionLog();
        log.smsLogId = smsLogId;
        log.actionType = action.type.toString();
        log.actionDestination = action.destination;
        log.ruleName = ruleName;
        log.attemptNumber = attemptNumber;
        log.isRetry = attemptNumber > 1;
        log.isBackupAction = isBackup;

        if (log.isRetry) {
            log.retryStrategy = action.retryStrategy;
            log.retryDelaySeconds = action.retryDelaySeconds;
        }

        return log;
    }

    /**
     * Log successful action execution
     */
    public void logSuccess(ActionLog log, int statusCode, String responseBody,
                           long startTime, int creditsUsed) {
        log.success = true;
        log.statusCode = statusCode;
        log.responseBody = responseBody;
        log.durationMs = System.currentTimeMillis() - startTime;
        log.creditsUsed = creditsUsed;

        saveLog(log);

        Log.d(TAG, String.format("✅ Action SUCCESS: %s to %s (Status: %d, Duration: %dms)",
                log.actionType, log.actionDestination, statusCode, log.durationMs));
    }

    /**
     * Log failed action execution
     */
    public void logFailure(ActionLog log, Exception exception, long startTime,
                           int creditsUsed) {
        log.success = false;
        log.durationMs = System.currentTimeMillis() - startTime;
        log.creditsUsed = creditsUsed;

        // Classify error type
        if (exception instanceof UnknownHostException) {
            log.errorType = "NETWORK";
            log.errorMessage = "Cannot resolve host - Check internet connection";
        } else if (exception instanceof SocketTimeoutException) {
            log.errorType = "TIMEOUT";
            log.errorMessage = "Connection timeout - Server not responding";
        } else if (exception instanceof SecurityException) {
            log.errorType = "PERMISSION";
            log.errorMessage = "Permission denied - " + exception.getMessage();
        } else if (exception instanceof IOException) {
            log.errorType = "NETWORK";
            log.errorMessage = "Network error - " + exception.getMessage();
        } else {
            log.errorType = "UNKNOWN";
            log.errorMessage = exception != null ? exception.getMessage() : "Unknown error";
        }

        saveLog(log);

        Log.e(TAG, String.format("❌ Action FAILED: %s to %s (%s: %s, Duration: %dms)",
                log.actionType, log.actionDestination, log.errorType, log.errorMessage, log.durationMs));
    }

    /**
     * Log failed action with HTTP status code
     */
    public void logHttpFailure(ActionLog log, int statusCode, String responseBody,
                               long startTime, int creditsUsed) {
        log.success = false;
        log.statusCode = statusCode;
        log.responseBody = responseBody;
        log.durationMs = System.currentTimeMillis() - startTime;
        log.creditsUsed = creditsUsed;

        // Classify HTTP errors
        if (statusCode >= 500) {
            log.errorType = "SERVER_ERROR";
            log.errorMessage = "Server error (5xx) - Try again later";
        } else if (statusCode >= 400) {
            log.errorType = "CLIENT_ERROR";
            if (statusCode == 401 || statusCode == 403) {
                log.errorType = "AUTH";
                log.errorMessage = "Authentication failed - Check credentials";
            } else if (statusCode == 404) {
                log.errorMessage = "Endpoint not found - Check URL";
            } else {
                log.errorMessage = "Client error (4xx) - Check request format";
            }
        } else {
            log.errorType = "UNKNOWN";
            log.errorMessage = "Unexpected status code: " + statusCode;
        }

        saveLog(log);

        Log.e(TAG, String.format("❌ Action FAILED: %s to %s (HTTP %d: %s, Duration: %dms)",
                log.actionType, log.actionDestination, statusCode, log.errorMessage, log.durationMs));
    }

    /**
     * Add transformation info to log
     */
    public void logTransformation(ActionLog log, String originalMessage, String transformedMessage) {
        log.messageTransformed = true;
        log.originalMessage = originalMessage;
        log.transformedMessage = transformedMessage;

        Log.d(TAG, String.format("🔄 Message transformed for %s", log.actionType));
    }

    /**
     * Mark log as backup action
     */
    public void markAsBackup(ActionLog log, String originalActionType) {
        log.isBackupAction = true;
        log.originalActionType = originalActionType;

        Log.d(TAG, String.format("🔀 Backup action triggered: %s → %s",
                originalActionType, log.actionType));
    }

    /**
     * Save log to database
     */
    private void saveLog(ActionLog log) {
        new Thread(() -> {
            try {
                db.actionLogDao().insertLog(log);
                Log.d(TAG, "📝 Action log saved (ID: " + log.id + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error saving action log", e);
            }
        }).start();
    }

    /**
     * Get all logs for an SMS
     */
    public void getLogsForSms(int smsLogId, LogsCallback callback) {
        new Thread(() -> {
            try {
                List<ActionLog> logs;
                if (smsLogId == -1) {
                    logs = db.actionLogDao().getRecentLogs();
                } else {
                    logs = db.actionLogDao().getLogsForSms(smsLogId);
                }
                callback.onLogsLoaded(logs);
            } catch (Exception e) {
                Log.e(TAG, "Error loading logs", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Get statistics
     */
    public void getStatistics(StatisticsCallback callback) {
        new Thread(() -> {
            try {
                int total = db.actionLogDao().getTotalLogs();
                int successful = db.actionLogDao().getSuccessfulLogsCount();
                int failed = db.actionLogDao().getFailedLogsCount();
                int retried = db.actionLogDao().getRetriedLogs();

                ActionStatistics stats = new ActionStatistics();
                stats.total = total;
                stats.successful = successful;
                stats.failed = failed;
                stats.retried = retried;
                stats.successRate = total > 0 ? (successful * 100.0f / total) : 0;

                callback.onStatisticsLoaded(stats);
            } catch (Exception e) {
                Log.e(TAG, "Error loading statistics", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Clean old logs (older than 30 days)
     */
    public void cleanOldLogs() {
        new Thread(() -> {
            try {
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                int deleted = db.actionLogDao().deleteOldLogs(thirtyDaysAgo);
                Log.d(TAG, "🗑️ Cleaned " + deleted + " old action logs");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning logs", e);
            }
        }).start();
    }

    // Callback interfaces
    public interface LogsCallback {
        void onLogsLoaded(List<ActionLog> logs);
        void onError(String error);
    }

    public interface StatisticsCallback {
        void onStatisticsLoaded(ActionStatistics stats);
        void onError(String error);
    }

    // Statistics class
    public static class ActionStatistics {
        public int total;
        public int successful;
        public int failed;
        public int retried;
        public float successRate;

        @Override
        public String toString() {
            return String.format(
                    "Total: %d\nSuccessful: %d (%.1f%%)\nFailed: %d\nRetried: %d",
                    total, successful, successRate, failed, retried
            );
        }
    }
}
