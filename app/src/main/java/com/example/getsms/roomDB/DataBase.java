package com.example.getsms.roomDB;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.getsms.model.Rule;
import com.example.getsms.model.SmsLog;

@Database(entities = {SmsRecord.class, Rule.class, SmsLog.class}, version = 4, exportSchema = false)
public abstract class DataBase extends RoomDatabase {

    public abstract SmsRecordDao smsDao();
    public abstract RuleDao ruleDao();
    public abstract SmsLogDao smsLogDao(); // NEW

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

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create new SMS logs table with comprehensive tracking
            database.execSQL("CREATE TABLE IF NOT EXISTS `sms_logs` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sender` TEXT, " +
                    "`message_body` TEXT, " +
                    "`sim_slot` TEXT, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`formatted_date` TEXT, " +
                    "`matched_rule_id` INTEGER NOT NULL, " +
                    "`matched_rule_name` TEXT, " +
                    "`actions_executed` TEXT, " +
                    "`webhook_sent` INTEGER NOT NULL, " +
                    "`webhook_status` INTEGER NOT NULL, " +
                    "`telegram_sent` INTEGER NOT NULL, " +
                    "`sms_forwarded` INTEGER NOT NULL, " +
                    "`whatsapp_sent` INTEGER NOT NULL, " +
                    "`was_transformed` INTEGER NOT NULL, " +
                    "`original_message` TEXT, " +
                    "`transformed_message` TEXT, " +
                    "`transform_type` TEXT, " +
                    "`has_error` INTEGER NOT NULL, " +
                    "`error_message` TEXT, " +
                    "`credits_used` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add retry tracking columns
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN retry_attempts TEXT");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN succeeded_after_retry INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN retry_strategy_used TEXT");

            // Add backup tracking columns
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN backup_action_used INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN backup_action_type TEXT");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN backup_action_destination TEXT");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN backup_action_success INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN backup_action_failed INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE sms_logs ADD COLUMN backup_triggered_at INTEGER NOT NULL DEFAULT 0");
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}