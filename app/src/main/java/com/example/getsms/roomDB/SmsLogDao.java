package com.example.getsms.roomDB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.getsms.model.SmsLog;

import java.util.List;

@Dao
public interface SmsLogDao {

    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC LIMIT 100")
    List<SmsLog> getRecentLogs();

    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    List<SmsLog> getAllLogs();

    @Query("SELECT * FROM sms_logs WHERE sender = :sender ORDER BY timestamp DESC")
    List<SmsLog> getLogsBySender(String sender);

    @Query("SELECT * FROM sms_logs WHERE matched_rule_id = :ruleId ORDER BY timestamp DESC")
    List<SmsLog> getLogsByRule(int ruleId);

    @Query("SELECT * FROM sms_logs WHERE sim_slot = :simSlot ORDER BY timestamp DESC")
    List<SmsLog> getLogsBySimSlot(String simSlot);

    @Query("SELECT * FROM sms_logs WHERE has_error = 1 ORDER BY timestamp DESC")
    List<SmsLog> getErrorLogs();

    @Query("SELECT * FROM sms_logs WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    List<SmsLog> getLogsSince(long startTime);

    @Query("SELECT * FROM sms_logs WHERE id = :logId")
    SmsLog getLogById(int logId);

    @Insert
    long insertLog(SmsLog log);

    @Update
    void updateLog(SmsLog log);

    @Delete
    void deleteLog(SmsLog log);

    @Query("DELETE FROM sms_logs WHERE timestamp < :timestamp")
    int deleteOldLogs(long timestamp);

    @Query("DELETE FROM sms_logs")
    void deleteAllLogs();

    @Query("SELECT COUNT(*) FROM sms_logs")
    int getLogCount();

    @Query("SELECT COUNT(*) FROM sms_logs WHERE has_error = 1")
    int getErrorCount();

    @Query("SELECT SUM(credits_used) FROM sms_logs")
    int getTotalCreditsUsed();

    // Statistics
    @Query("SELECT sender, COUNT(*) as count FROM sms_logs GROUP BY sender ORDER BY count DESC LIMIT 10")
    List<SenderStats> getTopSenders();

    @Query("SELECT matched_rule_name, COUNT(*) as count FROM sms_logs WHERE matched_rule_name IS NOT NULL GROUP BY matched_rule_name ORDER BY count DESC")
    List<RuleStats> getRuleStatistics();

    // Inner classes for statistics
    class SenderStats {
        public String sender;
        public int count;
    }

    class RuleStats {
        public String matched_rule_name;
        public int count;
    }
}