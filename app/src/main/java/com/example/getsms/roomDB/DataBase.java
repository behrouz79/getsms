package com.example.getsms.roomDB;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SmsRecord.class}, version = 2)
public abstract class DataBase extends RoomDatabase {
    public abstract SmsRecordDao smsDao();
    private static DataBase INSTANCE;
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE smsrecord ADD COLUMN body TEXT");
        }
    };


    public static DataBase getDbInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, DataBase.class, "SMS_LIST")
                    .allowMainThreadQueries().addMigrations(MIGRATION_1_2).build();
        }
        return INSTANCE;
    }
}
