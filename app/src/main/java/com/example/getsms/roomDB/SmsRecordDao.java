package com.example.getsms.roomDB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SmsRecordDao {
    @Query("SELECT * FROM smsrecord ORDER BY uid DESC")
    List<SmsRecord> getAllRecord();

    @Query("SELECT * FROM smsrecord ORDER BY uid ASC LIMIT 2")
    List<SmsRecord> getLastOlderMonth();

    @Insert
    void insertRecord(SmsRecord...smsRecords);

    @Query("UPDATE smsrecord SET title=:title,body=:body,date=:date,status=:status WHERE uid=:uid")
    int updateRecord(String title, String body, String date, int status, int uid);

    @Delete
    void deleteRecord(SmsRecord smsRecords);
}
