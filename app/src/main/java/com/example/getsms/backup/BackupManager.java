package com.example.getsms.backup;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupManager {

    private static final String TAG = "BackupManager";
    private static final int BACKUP_VERSION = 1;

    private final Context context;
    private final DataBase db;
    private final Gson gson;

    public BackupManager(Context context) {
        this.context = context;
        this.db = DataBase.getDbInstance(context);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Backup data class
     */
    public static class BackupData {
        public int version;
        public long timestamp;
        public String appVersion;
        public int ruleCount;
        public List<Rule> rules;

        public BackupData() {
            this.version = BACKUP_VERSION;
            this.timestamp = System.currentTimeMillis();
        }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    /**
     * Export rules to JSON file
     */
    public boolean exportRules(Uri destinationUri) {
        try {
            List<Rule> rules = db.ruleDao().getAllRules();

            BackupData backupData = new BackupData();
            backupData.appVersion = getAppVersion();
            backupData.ruleCount = rules.size();
            backupData.rules = rules;

            String json = gson.toJson(backupData);

            OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri);
            if (outputStream != null) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                writer.write(json);
                writer.flush();
                writer.close();
                outputStream.close();

                Log.i(TAG, "Backup successful: " + rules.size() + " rules exported");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error exporting rules", e);
        }
        return false;
    }

    /**
     * Import rules from JSON file
     */
    public ImportResult importRules(Uri sourceUri, boolean replaceExisting) {
        ImportResult result = new ImportResult();

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }

                reader.close();
                inputStream.close();

                String json = jsonBuilder.toString();
                BackupData backupData = gson.fromJson(json, BackupData.class);

                if (backupData == null || backupData.rules == null) {
                    result.success = false;
                    result.errorMessage = "Invalid backup file format";
                    return result;
                }

                result.backupInfo = backupData;

                // Replace existing rules if requested
                if (replaceExisting) {
                    db.ruleDao().deleteAllRules();
                    Log.i(TAG, "Existing rules deleted");
                }

                // Import rules
                int imported = 0;
                for (Rule rule : backupData.rules) {
                    // Reset IDs to let Room auto-generate new ones
                    rule.id = 0;
                    rule.updatedAt = System.currentTimeMillis();

                    long newId = db.ruleDao().insertRule(rule);
                    if (newId > 0) {
                        imported++;
                    }
                }

                result.success = true;
                result.importedCount = imported;
                result.skippedCount = backupData.rules.size() - imported;

                Log.i(TAG, "Import successful: " + imported + " rules imported");
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error importing rules", e);
            result.success = false;
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    /**
     * Preview backup file without importing
     */
    public BackupData previewBackup(Uri sourceUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }

                reader.close();
                inputStream.close();

                String json = jsonBuilder.toString();
                return gson.fromJson(json, BackupData.class);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error previewing backup", e);
        }
        return null;
    }

    /**
     * Get app version
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Generate default backup filename
     */
    public static String getDefaultBackupFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "GetSMS_Backup_" + sdf.format(new Date()) + ".json";
    }

    /**
     * Import result class
     */
    public static class ImportResult {
        public boolean success;
        public int importedCount;
        public int skippedCount;
        public String errorMessage;
        public BackupData backupInfo;

        public String getSummary() {
            if (!success) {
                return "Import failed: " + errorMessage;
            }
            return "Successfully imported " + importedCount + " rules" +
                    (skippedCount > 0 ? " (" + skippedCount + " skipped)" : "");
        }
    }
}