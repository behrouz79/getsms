package com.example.getsms;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
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
import com.example.getsms.utils.ServicePrefs;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

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

    private MaterialButton btnService;
    private TabLayout tabLayout;
    private TextView tvSubtitle;
    private TextView tvStatForwarded;
    private TextView tvStatFailed;
    private TextView tvStatCredits;
    private TextView tvClear;

    private ExecutorService executorService;
    private CreditManager creditManager;

    // Tracks which tab filter is active: 0=All, 1=Success, 2=Failed
    private int activeTab = 0;

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

        MobileAds.initialize(this, initializationStatus ->
                Log.d(TAG, "AdMob initialized"));

        creditManager = new CreditManager(this);
        creditManager.setBackendUrl("https://smsforwarder.amiriprog.ir/api/");
        actionLogger = new ActionLogger(this);
        db = DataBase.getDbInstance(this);

        findViews();
        setupRecyclerView();
        setupServiceButton();
        setupTabs();
        setupOverflowMenu();

        loadActionLogs();
        loadStatistics();
        updateCreditsDisplay();
        updateServiceButtonState();

        checkAndRequestPermissions();
    }

    // ── View wiring ────────────────────────────────────────────

    private void findViews() {
        recyclerView     = findViewById(R.id.recRequ);
        btnService       = findViewById(R.id.btnService);
        tabLayout        = findViewById(R.id.tabLayout);
        tvSubtitle       = findViewById(R.id.tvSubtitle);
        tvStatForwarded  = findViewById(R.id.tvStatForwarded);
        tvStatFailed     = findViewById(R.id.tvStatFailed);
        tvStatCredits    = findViewById(R.id.tvStatCredits);
        tvClear          = findViewById(R.id.tvClear);
    }

    private void setupRecyclerView() {
        adapter = new ActionLogsAdapter(this, logsList, this::showLogDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
    }

    private void setupServiceButton() {
        btnService.setOnClickListener(v -> {
            if (ServicePrefs.isServiceEnabled(this)) {
                stopService(v);
            } else {
                startService(v);
            }
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                activeTab = tab.getPosition();
                reloadCurrentFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        tvClear.setOnClickListener(v -> showClearOldLogsDialog());
    }

    private void setupOverflowMenu() {
        TextView btnOverflow = findViewById(R.id.btnOverflow);
        btnOverflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 1, 0, R.string.menu_rules);
            popup.getMenu().add(0, 2, 1, R.string.menu_credits);
            popup.getMenu().add(0, 3, 2, R.string.menu_language);
            popup.getMenu().add(0, 4, 3, R.string.menu_clear_logs);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1: startActivity(new Intent(this, RulesActivity.class)); return true;
                    case 2: startActivity(new Intent(this, CreditsActivity.class)); return true;
                    case 3: showLanguageDialog(); return true;
                    case 4: showClearOldLogsDialog(); return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // ── Service control ────────────────────────────────────────

    public void startService(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                requestDozeExemption();
                return;
            }
        }

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
            new AlertDialog.Builder(this)
                    .setTitle(R.string.battery_optimization_required)
                    .setMessage(R.string.battery_optimization_explanation)
                    .setPositiveButton(R.string.continue_text, (d, w) ->
                            BatteryOptimizationHelper.requestDisableBatteryOptimization(this))
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ServiceWatchdogJob.scheduleJob(this);
        }

        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();
        updateServiceButtonState();
        showServiceStartedDialog();
    }

    public void stopService(View v) {
        ReceiveSms.disableService(this);
        stopService(new Intent(this, EndlessService.class));
        cancelServiceRestartAlarm();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ServiceWatchdogJob.cancelJob(this);
        }

        Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show();
        updateServiceButtonState();
    }

    private void updateServiceButtonState() {
        boolean running = ServicePrefs.isServiceEnabled(this);

        if (running) {
            btnService.setBackgroundTintList(
                    getResources().getColorStateList(R.color.error));
            btnService.setText(R.string.stop_service);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tvSubtitle.setText(getString(R.string.service_running_since,
                    sdf.format(new Date())));
        } else {
            btnService.setBackgroundTintList(
                    getResources().getColorStateList(R.color.success));
            btnService.setText(R.string.start_service);
            tvSubtitle.setText(R.string.service_idle);
        }
    }

    // ── Data loading ───────────────────────────────────────────

    private void reloadCurrentFilter() {
        switch (activeTab) {
            case 1: loadSuccessLogs(); break;
            case 2: loadFailedLogs();  break;
            default: loadActionLogs(); break;
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
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading action logs", e);
            }
        });
    }

    private void loadSuccessLogs() {
        executorService.execute(() -> {
            try {
                List<ActionLog> all = db.actionLogDao().getRecentLogs();
                List<ActionLog> filtered = new ArrayList<>();
                for (ActionLog log : all) {
                    if (log.success) filtered.add(log);
                }
                runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(filtered);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading success logs", e);
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
                Log.e(TAG, "Error loading failed logs", e);
            }
        });
    }

    private void loadStatistics() {
        actionLogger.getStatistics(new ActionLogger.StatisticsCallback() {
            @Override
            public void onStatisticsLoaded(ActionLogger.ActionStatistics stats) {
                runOnUiThread(() -> {
                    tvStatForwarded.setText(String.valueOf(stats.successful));
                    tvStatFailed.setText(String.valueOf(stats.failed));
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading statistics: " + error);
            }
        });
    }

    private void updateCreditsDisplay() {
        int credits = creditManager.getCredits();
        tvStatCredits.setText(String.valueOf(credits));
    }

    // ── Log detail dialog (unchanged) ──────────────────────────

    private void showLogDetails(ActionLog log, int position) {
        Log.d(TAG, "Showing details for action log ID: " + log.id);

        StringBuilder details = new StringBuilder();
        details.append(log.success ? "SUCCESS" : "FAILED");
        if (log.isRetry) details.append(" (Retry #").append(log.attemptNumber).append(")");
        if (log.isBackupAction) details.append(" BACKUP");
        details.append("\n\n");

        details.append("Action Type\n").append(log.actionType).append("\n\n");
        details.append("Destination\n").append(log.actionDestination).append("\n\n");

        if (log.messageTransformed && log.transformedMessage != null) {
            details.append("Transformed Message\n").append(log.transformedMessage).append("\n\n");
            if (log.originalMessage != null)
                details.append("Original Message\n").append(log.originalMessage).append("\n\n");
        }

        if (log.ruleName != null)
            details.append("Rule\n").append(log.ruleName).append("\n\n");

        details.append("Duration\n").append(log.durationMs).append("ms\n\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        details.append("Time\n").append(sdf.format(new Date(log.executionTime))).append("\n\n");

        if (log.success) {
            if (log.statusCode > 0) details.append("Status\n").append(log.statusCode).append("\n\n");
            if (log.responseBody != null && !log.responseBody.isEmpty()) {
                String truncated = log.responseBody.length() > 200
                        ? log.responseBody.substring(0, 197) + "..."
                        : log.responseBody;
                details.append("Response\n").append(truncated).append("\n\n");
            }
        } else {
            details.append("Error Type\n")
                    .append(log.errorType != null ? log.errorType : "UNKNOWN").append("\n\n");
            details.append("Error\n")
                    .append(log.errorMessage != null ? log.errorMessage : "No details").append("\n\n");
            if (log.statusCode > 0) details.append("Status\n").append(log.statusCode).append("\n\n");
        }

        if (log.isRetry) {
            details.append("Retry\n");
            details.append("  Attempt: ").append(log.attemptNumber).append("\n");
            details.append("  Strategy: ").append(log.retryStrategy != null ? log.retryStrategy : "N/A").append("\n");
            details.append("  Delay: ").append(log.retryDelaySeconds).append("s\n\n");
        }

        if (log.isBackupAction && log.originalActionType != null)
            details.append("Backup — Original: ").append(log.originalActionType).append("\n\n");

        details.append("Credits Used\n").append(log.creditsUsed);

        new AlertDialog.Builder(this)
                .setTitle("Action Log Details")
                .setMessage(details.toString())
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    // ── Clear logs ─────────────────────────────────────────────

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
                long twoDaysAgo = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000);
                int deleted = db.actionLogDao().deleteOldLogs(twoDaysAgo);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Deleted " + deleted + " old logs", Toast.LENGTH_SHORT).show();
                    loadActionLogs();
                    loadStatistics();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error clearing old logs", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error clearing logs", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ── Permissions ────────────────────────────────────────────

    private void checkAndRequestPermissions() {
        PermissionsHelper.logPermissionStates(this);
        if (PermissionsHelper.hasAllPermissions(this)) return;

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

    private void showPermissionRationale(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    // ── Language dialog ────────────────────────────────────────

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
                    if (!newLang.equals(currentLang)) LanguageManager.setLanguage(this, newLang);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Battery / Doze ─────────────────────────────────────────

    private void showServiceStartedDialog() {
        String status = BatteryOptimizationHelper.getBatteryOptimizationStatus(this);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.service_started_title))
                .setMessage(getString(R.string.service_started_message, status))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void cancelServiceRestartAlarm() {
        Intent restartIntent = new Intent(getApplicationContext(), EndlessService.ServiceRestartReceiver.class);
        restartIntent.setAction("RESTART_SERVICE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 1, restartIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
    }

    private void checkBatteryOptimizationStatus() {
        if (ServicePrefs.isServiceEnabled(this) && !BatteryOptimizationHelper.isBatteryOptimizationDisabled(this)) {
            Toast.makeText(this, getString(R.string.battery_optimization_warning), Toast.LENGTH_LONG).show();
        }
    }

    private void checkDozeExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (ServicePrefs.isServiceEnabled(this) && pm != null
                    && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Toast.makeText(this,
                        "WARNING: Doze mode will kill service! Grant battery exemption.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestDozeExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("Doze Mode Exemption Required")
                        .setMessage("For 24/7 operation, disable battery optimization for this app.")
                        .setPositiveButton("Grant Now", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e) {
                                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        updateCreditsDisplay();
        updateServiceButtonState();
        loadActionLogs();
        loadStatistics();
        checkBatteryOptimizationStatus();
        checkDozeExemption();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown())
            executorService.shutdown();
    }
}
