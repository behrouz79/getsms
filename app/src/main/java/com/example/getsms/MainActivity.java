package com.example.getsms;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.getsms.adapter.ActionLogsAdapter;
import com.example.getsms.credit.CreditManager;
import com.example.getsms.engine.ActionLogger;
import com.example.getsms.model.ActionLog;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.utils.BatteryOptimizationHelper;
import com.example.getsms.utils.LanguageManager;
import com.example.getsms.utils.PermissionsHelper;
import com.google.android.gms.ads.MobileAds;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private ActionLogsAdapter adapter;
    private List<ActionLog> logsList = new ArrayList<>();
    private DataBase db;
    private ActionLogger actionLogger;

    private Button btnStart, btnStop, btnLanguage;
    private Button btnShowAll, btnShowSuccess, btnShowFailed;
    private TextView tvCreditsDisplay, tvStats;

    private ExecutorService executorService;
    private CreditManager creditManager;

    // Permission Launchers
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    showPermissionRationale(getString(R.string.notification_permission_required));
                }
            });

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.updateConfiguration(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus ->
                Log.d(TAG, "✅ AdMob initialized"));

        // Initialize managers
        creditManager = new CreditManager(this);
        creditManager.setBackendUrl("https://smsforwarder.amiriprog.ir/api/");
        actionLogger = new ActionLogger(this);

        db = DataBase.getDbInstance(this);

        // Setup UI
        findViews();
        setupRecyclerView();
        setupButtons();

        // Load data
        loadActionLogs();
        loadStatistics();
        updateCreditsDisplay();
        updateServiceButtonStates();

        // Check permissions
        checkAndRequestPermissions();

        Log.d(TAG, "✅ MainActivity initialization complete");
    }

    private void findViews() {
        recyclerView = findViewById(R.id.recRequ);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStart2);
        btnLanguage = findViewById(R.id.btnLanguage);
        tvCreditsDisplay = findViewById(R.id.tvCreditsDisplay);
        tvStats = findViewById(R.id.tvStats);

        btnShowAll = findViewById(R.id.btnShowAll);
        btnShowSuccess = findViewById(R.id.btnShowSuccess);
        btnShowFailed = findViewById(R.id.btnShowFailed);
    }

    private void setupRecyclerView() {
        adapter = new ActionLogsAdapter(this, logsList, this::showLogDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void setupButtons() {
        btnStart.setOnClickListener(this::startService);
        btnStop.setOnClickListener(this::stopService);
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        findViewById(R.id.btnOpenRules).setOnClickListener(v ->
                startActivity(new Intent(this, RulesActivity.class)));

        findViewById(R.id.btnCredits).setOnClickListener(v ->
                startActivity(new Intent(this, CreditsActivity.class)));

        // ADD THIS LINE
        findViewById(R.id.btnDelete).setOnClickListener(v -> showClearOldLogsDialog());

        // Filter buttons
        btnShowAll.setOnClickListener(v -> {
            loadActionLogs();
            updateFilterButtons("ALL");
        });

        btnShowSuccess.setOnClickListener(v -> {
            loadSuccessLogs();
            updateFilterButtons("SUCCESS");
        });

        btnShowFailed.setOnClickListener(v -> {
            loadFailedLogs();
            updateFilterButtons("FAILED");
        });
    }

    private void updateFilterButtons(String activeFilter) {
        // Reset all buttons
        btnShowAll.setBackgroundColor(Color.parseColor("#CCCCCC"));
        btnShowSuccess.setBackgroundColor(Color.parseColor("#CCCCCC"));
        btnShowFailed.setBackgroundColor(Color.parseColor("#CCCCCC"));

        // Highlight active button
        switch (activeFilter) {
            case "ALL":
                btnShowAll.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case "SUCCESS":
                btnShowSuccess.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case "FAILED":
                btnShowFailed.setBackgroundColor(Color.parseColor("#F44336"));
                break;
        }
    }

    private void loadActionLogs() {
        executorService.execute(() -> {
            try {
                List<ActionLog> logs = db.actionLogDao().getRecentLogs();
                runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(logs);
                    adapter.notifyDataSetChanged();

                    if (logsList.isEmpty()) {
                        Toast.makeText(this, "No action logs yet", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading action logs", e);
            }
        });
    }

    private void loadSuccessLogs() {
        executorService.execute(() -> {
            try {
                List<ActionLog> allLogs = db.actionLogDao().getRecentLogs();
                List<ActionLog> successLogs = new ArrayList<>();

                for (ActionLog log : allLogs) {
                    if (log.success) {
                        successLogs.add(log);
                    }
                }

                runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(successLogs);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading success logs", e);
            }
        });
    }

    private void loadFailedLogs() {
        executorService.execute(() -> {
            try {
                List<ActionLog> logs = db.actionLogDao().getFailedActionLogs();
                runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(logs);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading failed logs", e);
            }
        });
    }

    private void loadStatistics() {
        actionLogger.getStatistics(new ActionLogger.StatisticsCallback() {
            @Override
            public void onStatisticsLoaded(ActionLogger.ActionStatistics stats) {
                runOnUiThread(() -> {
                    tvStats.setText(String.format(Locale.getDefault(),
                            "📊 Total: %d | Success: %d (%.1f%%) | Failed: %d | Retried: %d",
                            stats.total, stats.successful, stats.successRate,
                            stats.failed, stats.retried));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvStats.setText("Error loading statistics"));
            }
        });
    }

    private void showLogDetails(ActionLog log, int position) {
        Log.d(TAG, "📋 Showing details for action log ID: " + log.id);

        StringBuilder details = new StringBuilder();

        // Status
        details.append(log.success ? "✅ SUCCESS" : "❌ FAILED");
        if (log.isRetry) {
            details.append(" (Retry #").append(log.attemptNumber).append(")");
        }
        if (log.isBackupAction) {
            details.append(" 🔀 BACKUP");
        }
        details.append("\n\n");

        // Action Details
        details.append("📋 Action Type\n");
        details.append(log.actionType).append("\n\n");

        details.append("📍 Destination\n");
        details.append(log.actionDestination).append("\n\n");

        // Message
        if (log.messageTransformed && log.transformedMessage != null) {
            details.append("💬 Transformed Message\n");
            details.append(log.transformedMessage).append("\n\n");

            if (log.originalMessage != null) {
                details.append("📝 Original Message\n");
                details.append(log.originalMessage).append("\n\n");
            }
        }

        // Rule
        if (log.ruleName != null) {
            details.append("🏷️ Rule\n");
            details.append(log.ruleName).append("\n\n");
        }

        // Timing
        details.append("⏱️ Duration\n");
        details.append(log.durationMs).append("ms\n\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        details.append("🕒 Time\n");
        details.append(sdf.format(new Date(log.executionTime))).append("\n\n");

        // Result Details
        if (log.success) {
            if (log.statusCode > 0) {
                details.append("📊 Status Code\n");
                details.append(log.statusCode).append("\n\n");
            }

            if (log.responseBody != null && !log.responseBody.isEmpty()) {
                String truncated = log.responseBody.length() > 200
                        ? log.responseBody.substring(0, 197) + "..."
                        : log.responseBody;
                details.append("📄 Response\n");
                details.append(truncated).append("\n\n");
            }
        } else {
            // Error Details
            details.append(log.getErrorTypeEmoji()).append(" Error Type\n");
            details.append(log.errorType != null ? log.errorType : "UNKNOWN").append("\n\n");

            details.append("💬 Error Message\n");
            details.append(log.errorMessage != null ? log.errorMessage : "No details").append("\n\n");

            if (log.statusCode > 0) {
                details.append("📊 Status Code\n");
                details.append(log.statusCode).append("\n\n");
            }
        }

        // Retry Information
        if (log.isRetry) {
            details.append("🔄 Retry Details\n");
            details.append("  Attempt: ").append(log.attemptNumber).append("\n");
            details.append("  Strategy: ").append(log.retryStrategy != null ? log.retryStrategy : "N/A").append("\n");
            details.append("  Delay: ").append(log.retryDelaySeconds).append("s\n\n");
        }

        // Backup Information
        if (log.isBackupAction && log.originalActionType != null) {
            details.append("🔀 Backup Action\n");
            details.append("  Original: ").append(log.originalActionType).append("\n\n");
        }

        // Credits
        details.append("💳 Credits Used\n");
        details.append(log.creditsUsed);

        new AlertDialog.Builder(this)
                .setTitle("📋 Action Log Details")
                .setMessage(details.toString())
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }
    private void deleteLog(ActionLog log) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Log")
                .setMessage("Delete this action log?")
                .setPositiveButton("Delete", (d, w) -> {
                    executorService.execute(() -> {
                        db.actionLogDao().deleteLogsForSms(log.smsLogId);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Log deleted", Toast.LENGTH_SHORT).show();
                            loadActionLogs();
                            loadStatistics();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearOldLogsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_old_logs)
                .setMessage(R.string.clear_logs_confirm)
                .setPositiveButton(R.string.clear, (d, w) -> clearOldLogs())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearOldLogs() {
        executorService.execute(() -> {
            try {
                // Calculate timestamp for 2 days ago
                long twoDaysAgo = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000);

                // Delete old logs
                int deleted = db.actionLogDao().deleteOldLogs(twoDaysAgo);

                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Deleted " + deleted + " old logs",
                            Toast.LENGTH_SHORT).show();
                    loadActionLogs();
                    loadStatistics();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error clearing old logs", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error clearing logs", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void checkAndRequestPermissions() {
        PermissionsHelper.logPermissionStates(this);
        if (PermissionsHelper.hasAllPermissions(this)) {
            return;
        }

        List<String> missing = PermissionsHelper.getMissingPermissions(this);
        StringBuilder message = new StringBuilder(getString(R.string.permissions_required_header) + "\n\n");

        for (String perm : missing) {
            message.append("• ").append(PermissionsHelper.getPermissionName(perm))
                    .append("\n  ").append(PermissionsHelper.getPermissionDescription(perm)).append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(message.toString())
                .setPositiveButton(R.string.ok, (d, w) -> PermissionsHelper.requestAllPermissions(this))
                .setNegativeButton(R.string.cancel, (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showLanguageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_language_selection, null);
        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupLanguage);
        android.widget.RadioButton radioEnglish = dialogView.findViewById(R.id.radioEnglish);
        android.widget.RadioButton radioPersian = dialogView.findViewById(R.id.radioPersian);

        String currentLang = LanguageManager.getLanguage(this);
        if (LanguageManager.ENGLISH.equals(currentLang)) radioEnglish.setChecked(true);
        else if (LanguageManager.PERSIAN.equals(currentLang)) radioPersian.setChecked(true);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    String newLang = (selectedId == R.id.radioEnglish)
                            ? LanguageManager.ENGLISH
                            : LanguageManager.PERSIAN;

                    if (!newLang.equals(currentLang)) {
                        LanguageManager.setLanguage(this, newLang);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateCreditsDisplay() {
        int credits = creditManager.getCredits();
        tvCreditsDisplay.setText(getString(R.string.credits, credits));
        int color = (credits < 10)
                ? getResources().getColor(android.R.color.holo_red_dark)
                : getResources().getColor(android.R.color.holo_green_dark);
        tvCreditsDisplay.setTextColor(color);
    }

    private void showServiceStartedDialog() {
        String status = BatteryOptimizationHelper.getBatteryOptimizationStatus(this);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.service_started_title))
                .setMessage(getString(R.string.service_started_message, status))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    public void startService(View v) {
        if (!PermissionsHelper.hasSendSmsPermission(this)) {
            PermissionsHelper.requestSendSmsPermission(this);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        if (!BatteryOptimizationHelper.isBatteryOptimizationDisabled(this)) {
            BatteryOptimizationHelper.requestDisableBatteryOptimization(this);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.battery_optimization_required)
                    .setMessage(R.string.battery_optimization_explanation)
                    .setPositiveButton(R.string.continue_text, (d, w) -> {
                        BatteryOptimizationHelper.requestDisableBatteryOptimization(this);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        if (!creditManager.hasEnoughCredits(1)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.low_credits)
                    .setMessage(R.string.need_credits_message)
                    .setPositiveButton(R.string.get_credits, (d, w) ->
                            startActivity(new Intent(this, CreditsActivity.class)))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        ReceiveSms.enableService(this);

        Intent serviceIntent = new Intent(this, EndlessService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();
        updateServiceButtonStates();
        showServiceStartedDialog();
    }

    private void cancelServiceRestartAlarm() {
        Intent restartIntent = new Intent(getApplicationContext(), EndlessService.ServiceRestartReceiver.class);
        restartIntent.setAction("RESTART_SERVICE");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                1,
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled service restart alarm");
        }
    }

    public void stopService(View v) {
        ReceiveSms.disableService(this);
        stopService(new Intent(this, EndlessService.class));
        cancelServiceRestartAlarm();
        Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show();
        updateServiceButtonStates();
    }

    private void updateServiceButtonStates() {
        boolean enabled = getSharedPreferences("sms_forwarder_prefs", MODE_PRIVATE)
                .getBoolean("service_enabled", false);
        btnStart.setEnabled(!enabled);
        btnStop.setEnabled(enabled);
        btnStart.setAlpha(enabled ? 0.5f : 1.0f);
        btnStop.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void showPermissionRationale(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void checkBatteryOptimizationStatus() {
        android.content.SharedPreferences prefs = getSharedPreferences("sms_forwarder_prefs", MODE_PRIVATE);
        boolean serviceEnabled = prefs.getBoolean("service_enabled", false);

        if (serviceEnabled && !BatteryOptimizationHelper.isBatteryOptimizationDisabled(this)) {
            Toast.makeText(this,
                    getString(R.string.battery_optimization_warning),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCreditsDisplay();
        updateServiceButtonStates();
        loadActionLogs();
        loadStatistics();
        checkBatteryOptimizationStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown())
            executorService.shutdown();
    }
}