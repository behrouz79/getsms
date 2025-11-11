package com.example.getsms;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
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
import com.example.getsms.utils.LanguageManager;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private SmsLogAdapter adapter;
    private List<SmsLog> logsList = new ArrayList<>();
    private DataBase db;
    private Button btnClearLogs;
    private Button btnStart;
    private Button btnStop;
    private Button btnLanguage;
    private ExecutorService executorService;

    // Credit system
    private CreditManager creditManager;
    private TextView tvCreditsDisplay;

    // Language manager
    private LanguageManager languageManager;

    // Permission launchers
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    checkSmsPermission();
                } else {
                    showPermissionRationale(getString(R.string.notification_permission_required));
                }
            });

    private final ActivityResultLauncher<String> requestSmsPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    showPermissionRationale(getString(R.string.sms_permission_required));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply language before setContentView
        languageManager = new LanguageManager(this);
        languageManager.updateResources(languageManager.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "========================================");
        Log.d(TAG, "📱 SMS FORWARDER STARTED");
        Log.d(TAG, "========================================");

        executorService = Executors.newSingleThreadExecutor();

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "✅ AdMob initialized");
        });

        // Initialize credit manager
        creditManager = new CreditManager(this);
        creditManager.setBackendUrl("https://smsforwarder.amiriprog.ir/api/");

        Log.d(TAG, "💳 Available credits: " + creditManager.getCredits());

        // Initialize database
        db = DataBase.getDbInstance(this);

        // Initialize views
        findViews();

        // Setup adapter with click listener
        adapter = new SmsLogAdapter(this, logsList, this::showLogDetails);

        // Setup RecyclerView
        setupRecyclerView();

        // Load data
        loadLogs();

        // Check permissions
        checkPermissions();

        // Setup buttons
        setupButtons();

        // Update credits display
        updateCreditsDisplay();

        // Update button states
        updateServiceButtonStates();

        Log.d(TAG, "✅ MainActivity initialization complete");
    }

    private void findViews() {
        recyclerView = findViewById(R.id.recRequ);
        btnClearLogs = findViewById(R.id.btnDelete);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStart2);
        btnLanguage = findViewById(R.id.btnLanguage);
        tvCreditsDisplay = findViewById(R.id.tvCreditsDisplay);

        Log.d(TAG, "Views initialized");
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        Log.d(TAG, "RecyclerView configured");
    }

    private void setupButtons() {
        // Start service button
        btnStart.setOnClickListener(v -> startService(v));

        // Stop service button
        btnStop.setOnClickListener(v -> stopService(v));

        // Clear logs button
        btnClearLogs.setOnClickListener(v -> showClearLogsDialog());

        // Rules button
        Button btnOpenRules = findViewById(R.id.btnOpenRules);
        btnOpenRules.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RulesActivity.class);
            startActivity(intent);
        });

        // Credits button
        Button btnCredits = findViewById(R.id.btnCredits);
        btnCredits.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreditsActivity.class);
            startActivity(intent);
        });

        // Refresh button
        Button btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            Log.d(TAG, "🔄 Refreshing logs...");
            loadLogs();
            updateCreditsDisplay();
            updateServiceButtonStates();
        });

        // Language button
        btnLanguage.setOnClickListener(v -> showLanguageDialog());
    }

    private void showLanguageDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_language_selection, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupLanguage);
        RadioButton radioEnglish = dialogView.findViewById(R.id.radioEnglish);
        RadioButton radioPersian = dialogView.findViewById(R.id.radioPersian);

        // Set current selection
        String currentLanguage = languageManager.getLanguage();
        if (LanguageManager.ENGLISH.equals(currentLanguage)) {
            radioEnglish.setChecked(true);
        } else if (LanguageManager.PERSIAN.equals(currentLanguage)) {
            radioPersian.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (d, which) -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    String newLanguage = currentLanguage;

                    if (selectedId == R.id.radioEnglish) {
                        newLanguage = LanguageManager.ENGLISH;
                    } else if (selectedId == R.id.radioPersian) {
                        newLanguage = LanguageManager.PERSIAN;
                    }

                    if (!newLanguage.equals(currentLanguage)) {
                        languageManager.setLanguage(newLanguage);
                        Toast.makeText(this, R.string.language_changed, Toast.LENGTH_SHORT).show();

                        // Restart activity to apply language
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();
    }

    private void updateServiceButtonStates() {
        boolean isEnabled = isServiceEnabled();

        if (isEnabled) {
            btnStart.setEnabled(false);
            btnStart.setAlpha(0.5f);
            btnStop.setEnabled(true);
            btnStop.setAlpha(1.0f);
        } else {
            btnStart.setEnabled(true);
            btnStart.setAlpha(1.0f);
            btnStop.setEnabled(false);
            btnStop.setAlpha(0.5f);
        }
    }

    private boolean isServiceEnabled() {
        return getSharedPreferences("sms_forwarder_prefs", MODE_PRIVATE)
                .getBoolean("service_enabled", false);
    }

    private void loadLogs() {
        Log.d(TAG, "📊 Loading SMS logs from database...");

        executorService.execute(() -> {
            try {
                List<SmsLog> logs = db.smsLogDao().getRecentLogs();

                Log.d(TAG, "✅ Loaded " + logs.size() + " log entries");

                // Log summary
                int withErrors = 0;
                int totalCredits = 0;
                for (SmsLog log : logs) {
                    if (log.hasError) withErrors++;
                    totalCredits += log.creditsUsed;
                }

                Log.d(TAG, "📊 Log Statistics:");
                Log.d(TAG, "   Total logs: " + logs.size());
                Log.d(TAG, "   With errors: " + withErrors);
                Log.d(TAG, "   Total credits used: " + totalCredits);

                runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(logs);
                    adapter.notifyDataSetChanged();

                    if (logsList.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                R.string.no_logs_yet,
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading logs", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.error_occurred, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
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

            // Detailed action status
            if (log.webhookSent) {
                details.append("  • Webhook: Status ").append(log.webhookStatus).append("\n");
            }
            if (log.telegramSent) {
                details.append("  • Telegram: Sent\n");
            }
            if (log.smsForwarded) {
                details.append("  • SMS: Forwarded\n");
            }
            if (log.whatsappSent) {
                details.append("  • WhatsApp: Sent\n");
            }
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
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_log))
                .setMessage(getString(R.string.delete_log_confirm))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    executorService.execute(() -> {
                        db.smsLogDao().deleteLog(log);
                        runOnUiThread(() -> {
                            Toast.makeText(this, getString(R.string.log_deleted), Toast.LENGTH_SHORT).show();
                            loadLogs();
                        });
                    });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showClearLogsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_old_logs))
                .setMessage(getString(R.string.clear_logs_confirm))
                .setPositiveButton(getString(R.string.clear), (dialog, which) -> clearOldLogs())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void clearOldLogs() {
        executorService.execute(() -> {
            try {
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                int deleted = db.smsLogDao().deleteOldLogs(thirtyDaysAgo);

                Log.d(TAG, "🗑️ Deleted " + deleted + " old logs");

                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.logs_deleted, deleted), Toast.LENGTH_SHORT).show();
                    loadLogs();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting old logs", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_occurred, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateCreditsDisplay() {
        if (tvCreditsDisplay != null) {
            int credits = creditManager.getCredits();
            tvCreditsDisplay.setText(getString(R.string.credits, credits));

            if (credits < 10) {
                tvCreditsDisplay.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvCreditsDisplay.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    public void startService(android.view.View v) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.notification_permission_required), Toast.LENGTH_SHORT).show();
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        // Check credits
        if (!creditManager.hasEnoughCredits(1)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.low_credits))
                    .setMessage(getString(R.string.need_credits_message))
                    .setPositiveButton(getString(R.string.get_credits), (dialog, which) -> {
                        Intent intent = new Intent(MainActivity.this, CreditsActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            return;
        }

        // Enable service
        ReceiveSms.enableService(this);

        // Start foreground service
        Intent serviceIntent = new Intent(this, EndlessService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, getString(R.string.service_started), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "✅ Service started and enabled");

        updateServiceButtonStates();
    }

    public void stopService(android.view.View v) {
        // Disable service
        ReceiveSms.disableService(this);

        // Stop foreground service
        Intent serviceIntent = new Intent(this, EndlessService.class);
        stopService(serviceIntent);

        Toast.makeText(this, getString(R.string.service_stopped), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "⏹️ Service stopped and disabled");

        updateServiceButtonStates();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        checkSmsPermission();
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestSmsPermission.launch(Manifest.permission.RECEIVE_SMS);
        }
    }

    private void showPermissionRationale(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_required))
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 MainActivity resumed - refreshing data");
        updateCreditsDisplay();
        updateServiceButtonStates();
        loadLogs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d(TAG, "👋 MainActivity destroyed");
    }
}