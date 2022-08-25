package com.example.getsms.roomDB;
import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SmsRecord.class}, version = 2)
public abstract class DataBase extends RoomDatabase {
    public abstract SmsRecordDao smsDao();
    private static DataBase INSTANCE;
    public static DataBase getDbInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, DataBase.class, "SMS_LIST")
                    .allowMainThreadQueries().build();
        }
        return INSTANCE;
    }
}
