package com.example.getsms.roomDB;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.getsms.model.Rule;

@Database(entities = {SmsRecord.class, Rule.class}, version = 3, exportSchema = false)
public abstract class DataBase extends RoomDatabase {

    public abstract SmsRecordDao smsDao();
    public abstract RuleDao ruleDao();

    private static volatile DataBase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE smsrecord ADD COLUMN body TEXT");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create rules table
            database.execSQL("CREATE TABLE IF NOT EXISTS `rules` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, " +
                    "`enabled` INTEGER NOT NULL, " +
                    "`priority` INTEGER NOT NULL, " +
                    "`sim_filter` TEXT, " +
                    "`sender_filter_type` TEXT, " +
                    "`sender_filter_value` TEXT, " +
                    "`message_filter_type` TEXT, " +
                    "`message_filter_value` TEXT, " +
                    "`actions_json` TEXT, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL)");
        }
    };

    public static DataBase getDbInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DataBase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    DataBase.class,
                                    "SMS_LIST"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}