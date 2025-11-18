package com.example.getsms.roomDB;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.getsms.model.ActionLog;

import java.util.List;

@Dao
public interface ActionLogDao {

    /**
     * Insert a new action log
     */
    @Insert
    long insertLog(ActionLog log);

    /**
     * Get all logs for a specific SMS
     */
    @Query("SELECT * FROM action_logs WHERE sms_log_id = :smsLogId ORDER BY execution_time DESC")
    List<ActionLog> getLogsForSms(int smsLogId);

    /**
     * Get recent action logs (last 100)
     */
    @Query("SELECT * FROM action_logs ORDER BY execution_time DESC LIMIT 100")
    List<ActionLog> getRecentLogs();

    /**
     * Get failed actions only (FIXED: Renamed method to avoid conflict)
     */
    @Query("SELECT * FROM action_logs WHERE success = 0 ORDER BY execution_time DESC LIMIT 50")
    List<ActionLog> getFailedActionLogs();

    /**
     * Get logs for specific action type
     */
    @Query("SELECT * FROM action_logs WHERE action_type = :actionType ORDER BY execution_time DESC LIMIT 50")
    List<ActionLog> getLogsByType(String actionType);

    /**
     * Get retry attempts for an action
     */
    @Query("SELECT * FROM action_logs WHERE sms_log_id = :smsLogId AND action_type = :actionType ORDER BY attempt_number ASC")
    List<ActionLog> getRetryAttempts(int smsLogId, String actionType);

    /**
     * Get backup actions
     */
    @Query("SELECT * FROM action_logs WHERE is_backup_action = 1 ORDER BY execution_time DESC LIMIT 50")
    List<ActionLog> getBackupActions();

    /**
     * Statistics: Total logs
     */
    @Query("SELECT COUNT(*) FROM action_logs")
    int getTotalLogs();

    /**
     * Statistics: Successful logs count
     */
    @Query("SELECT COUNT(*) FROM action_logs WHERE success = 1")
    int getSuccessfulLogsCount();

    /**
     * Statistics: Failed logs count (FIXED: Renamed to avoid conflict)
     */
    @Query("SELECT COUNT(*) FROM action_logs WHERE success = 0")
    int getFailedLogsCount();

    /**
     * Statistics: Retried logs
     */
    @Query("SELECT COUNT(*) FROM action_logs WHERE is_retry = 1")
    int getRetriedLogs();

    /**
     * Statistics: Average duration
     */
    @Query("SELECT AVG(duration_ms) FROM action_logs WHERE success = 1")
    long getAverageDuration();

    /**
     * Delete old logs (older than specified timestamp)
     */
    @Query("DELETE FROM action_logs WHERE execution_time < :timestamp")
    int deleteOldLogs(long timestamp);

    /**
     * Delete all logs
     */
    @Query("DELETE FROM action_logs")
    void deleteAllLogs();

    /**
     * Delete logs for specific SMS
     */
    @Query("DELETE FROM action_logs WHERE sms_log_id = :smsLogId")
    void deleteLogsForSms(int smsLogId);
}