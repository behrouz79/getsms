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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.getsms.adapter.SmsLogAdapter;
import com.example.getsms.credit.CreditManager;
import com.example.getsms.model.SmsLog;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.utils.BatteryOptimizationHelper;
import com.example.getsms.utils.LanguageManager;
import com.example.getsms.utils.PermissionsHelper;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private SmsLogAdapter adapter;
    private List<SmsLog> logsList = new ArrayList<>();
    private DataBase db;
    private Button btnClearLogs, btnStart, btnStop, btnLanguage;
    private TextView tvCreditsDisplay;

    private ExecutorService executorService;
    private CreditManager creditManager;

    // --- Permission Launchers ---
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    showPermissionRationale(getString(R.string.notification_permission_required));
                }
            });

    // --- Attach language before activity is created ---
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ensure language applied before UI inflation
        LanguageManager.updateConfiguration(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> Log.d(TAG, "✅ AdMob initialized"));

        // Initialize Credit Manager
        creditManager = new CreditManager(this);
        creditManager.setBackendUrl("https://smsforwarder.amiriprog.ir/api/");
        Log.d(TAG, "💳 Credits: " + creditManager.getCredits());

        // Initialize database
        db = DataBase.getDbInstance(this);

        // Setup UI
        findViews();
        setupRecyclerView();
        setupButtons();

        // Load logs and update UI
        loadLogs();
        updateCreditsDisplay();
        updateServiceButtonStates();

        // Check permissions
        checkAndRequestPermissions();

        Log.d(TAG, "✅ MainActivity initialization complete");
    }

    private void findViews() {
        recyclerView = findViewById(R.id.recRequ);
        btnClearLogs = findViewById(R.id.btnDelete);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStart2);
        btnLanguage = findViewById(R.id.btnLanguage);
        tvCreditsDisplay = findViewById(R.id.tvCreditsDisplay);
    }

    private void setupRecyclerView() {
        adapter = new SmsLogAdapter(this, logsList, this::showLogDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void setupButtons() {
        btnStart.setOnClickListener(this::startService);
        btnStop.setOnClickListener(this::stopService);
        btnClearLogs.setOnClickListener(v -> showClearLogsDialog());

        findViewById(R.id.btnOpenRules).setOnClickListener(v ->
                startActivity(new Intent(this, RulesActivity.class)));

        findViewById(R.id.btnCredits).setOnClickListener(v ->
                startActivity(new Intent(this, CreditsActivity.class)));

//        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
//            loadLogs();
//            updateCreditsDisplay();
//            updateServiceButtonStates();
//        });

        btnLanguage.setOnClickListener(v -> showLanguageDialog());
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_language_selection, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupLanguage);
        RadioButton radioEnglish = dialogView.findViewById(R.id.radioEnglish);
        RadioButton radioPersian = dialogView.findViewById(R.id.radioPersian);

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

    private void loadLogs() {
        executorService.execute(() -> {
            try {
                List<SmsLog> logs = db.smsLogDao().getRecentLogs();
                runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(logs);
                    adapter.notifyDataSetChanged();
                    if (logsList.isEmpty()) {
                        Toast.makeText(this, R.string.no_logs_yet, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading logs", e);
            }
        });
    }

    private void showLogDetails(SmsLog log, int position) {
        Log.d(TAG, "📋 Showing details for log ID: " + log.id);

        StringBuilder details = new StringBuilder();
        details.append("📱 ").append(getString(R.string.sender)).append("\n");
        details.append(log.sender).append("\n\n");

        details.append("📍 ").append(getString(R.string.sim_slot)).append("\n");
        details.append(log.simSlot).append("\n\n");

        details.append("💬 ").append(getString(R.string.message)).append("\n");
        details.append(log.messageBody).append("\n\n");

        details.append("🕒 ").append(getString(R.string.time)).append("\n");
        details.append(log.formattedDate).append("\n\n");

        if (log.matchedRuleName != null) {
            details.append("📋 ").append(getString(R.string.matched_rule)).append("\n");
            details.append(log.matchedRuleName).append("\n\n");
        }

        if (log.wasTransformed) {
            details.append("🔄 ").append(getString(R.string.transformation)).append("\n");
            details.append("Type: ").append(log.transformType).append("\n");
            details.append("Original: ").append(log.originalMessage).append("\n");
            details.append("Transformed: ").append(log.transformedMessage).append("\n\n");
        }

        String actions = log.getActionsSummary();
        if (!"No actions".equals(actions)) {
            details.append("🚀 ").append(getString(R.string.actions_executed)).append("\n");
            details.append(actions).append("\n\n");

            if (log.webhookSent) details.append("  • Webhook: Status ").append(log.webhookStatus).append("\n");
            if (log.telegramSent) details.append("  • Telegram: Sent\n");
            if (log.smsForwarded) details.append("  • SMS: Forwarded\n");
            if (log.whatsappSent) details.append("  • WhatsApp: Sent\n");
            details.append("\n");
        }

        details.append("💳 ").append(getString(R.string.credits_used)).append("\n");
        details.append(log.creditsUsed).append("\n\n");

        if (log.hasError) {
            details.append("❌ ").append(getString(R.string.error)).append("\n");
            details.append(log.errorMessage).append("\n");
        } else {
            details.append("✅ ").append(getString(R.string.status)).append("\n");
            details.append(getString(R.string.successfully_processed));
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sms_log_details))
                .setMessage(details.toString())
                .setPositiveButton(getString(R.string.ok), null)
                .setNeutralButton(getString(R.string.delete), (dialog, which) -> deleteLog(log))
                .show();
    }

    private void deleteLog(SmsLog log) {
        executorService.execute(() -> {
            db.smsLogDao().deleteLog(log);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.log_deleted, Toast.LENGTH_SHORT).show();
                loadLogs();
            });
        });
    }

    private void showClearLogsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_old_logs)
                .setMessage(R.string.clear_logs_confirm)
                .setPositiveButton(R.string.clear, (d, w) -> clearOldLogs())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearOldLogs() {
        executorService.execute(() -> {
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            int deleted = db.smsLogDao().deleteOldLogs(thirtyDaysAgo);
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.logs_deleted, deleted), Toast.LENGTH_SHORT).show();
                loadLogs();
            });
        });
    }

    private void updateCreditsDisplay() {
        int credits = creditManager.getCredits();
        tvCreditsDisplay.setText(getString(R.string.credits, credits));
        int color = (credits < 10)
                ? getResources().getColor(android.R.color.holo_red_dark)
                : getResources().getColor(android.R.color.holo_green_dark);
        tvCreditsDisplay.setTextColor(color);
    }

    /**
     * Show dialog confirming service is running persistently
     */
    private void showServiceStartedDialog() {
        String status = BatteryOptimizationHelper.getBatteryOptimizationStatus(this);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.service_started_title))
                .setMessage(getString(R.string.service_started_message, status))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }


    public void startService(View v) {
        // 1. Check SMS permissions first
        if (!PermissionsHelper.hasSendSmsPermission(this)) {
            PermissionsHelper.requestSendSmsPermission(this);
            return;
        }

        // 2. Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }

        // 3. CRITICAL: Check battery optimization
        if (!BatteryOptimizationHelper.isBatteryOptimizationDisabled(this)) {
            BatteryOptimizationHelper.requestDisableBatteryOptimization(this);
            // Show info dialog explaining why this is needed
            new AlertDialog.Builder(this)
                    .setTitle(R.string.battery_optimization_required)
                    .setMessage(R.string.battery_optimization_explanation)
                    .setPositiveButton(R.string.continue_text, (d, w) -> {
                        // After user understands, request battery optimization
                        BatteryOptimizationHelper.requestDisableBatteryOptimization(this);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        // 4. Check credits
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

        // 5. Enable service in preferences
        ReceiveSms.enableService(this);

        // 6. Start foreground service
        Intent serviceIntent = new Intent(this, EndlessService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 7. Update UI
        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();
        updateServiceButtonStates();

        // 8. Show status dialog
        showServiceStartedDialog();
    }


    /**
     * Cancel scheduled service restart alarm
     */
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
    /**
     * Stop the service (updated method)
     */
    public void stopService(View v) {
        // 1. Disable service in preferences (CRITICAL - prevents auto-restart)
        ReceiveSms.disableService(this);

        // 2. Stop the foreground service
        stopService(new Intent(this, EndlessService.class));

        // 3. Cancel any pending restart alarms
        cancelServiceRestartAlarm();

        // 4. Update UI
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

    /**
     * Check and warn about battery optimization
     */
    private void checkBatteryOptimizationStatus() {
        SharedPreferences prefs = getSharedPreferences("sms_forwarder_prefs", MODE_PRIVATE);
        boolean serviceEnabled = prefs.getBoolean("service_enabled", false);

        // Only check if service is running
        if (serviceEnabled && !BatteryOptimizationHelper.isBatteryOptimizationDisabled(this)) {
            // Show warning that service might be killed
            Toast.makeText(this,
                    getString(R.string.battery_optimization_warning),
                    Toast.LENGTH_LONG).show();
        }
    }
    /**
     * Add this to onResume to check service status
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateCreditsDisplay();
        updateServiceButtonStates();
        loadLogs();

        // Show battery optimization warning if not disabled
        checkBatteryOptimizationStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown())
            executorService.shutdown();
    }
}
