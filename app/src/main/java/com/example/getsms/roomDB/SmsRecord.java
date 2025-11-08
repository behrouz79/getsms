package com.example.getsms.roomDB;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SmsRecord {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "body")
    public String body;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "status")
    public int status;

    @ColumnInfo(name = "delivery_status")
    public String deliveryStatus; // "PENDING", "SENT", "FAILED"

    @ColumnInfo(name = "retry_count")
    public int retryCount;

    @ColumnInfo(name = "error_message")
    public String errorMessage;
}
