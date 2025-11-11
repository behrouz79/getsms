package com.example.getsms;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.getsms.adapter.SmsLogAdapter;
import com.example.getsms.credit.CreditManager;
import com.example.getsms.model.SmsLog;
import com.example.getsms.roomDB.DataBase;
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
    private ExecutorService executorService;

    // Credit system
    private CreditManager creditManager;
    private TextView tvCreditsDisplay;

    // Permission launchers
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    checkSmsPermission();
                } else {
                    showPermissionRationale("Notification permission is required");
                }
            });

    private final ActivityResultLauncher<String> requestSmsPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    showPermissionRationale("SMS permission is required");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        Log.d(TAG, "✅ MainActivity initialization complete");
    }

    private void findViews() {
        recyclerView = findViewById(R.id.recRequ);
        btnClearLogs = findViewById(R.id.btnDelete);
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
        });
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
                                "No SMS logs yet. Logs will appear when SMS is received.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading logs", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Error loading logs: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showLogDetails(SmsLog log, int position) {
        Log.d(TAG, "📋 Showing details for log ID: " + log.id);

        StringBuilder details = new StringBuilder();
        details.append("📱 SENDER\n");
        details.append(log.sender).append("\n\n");

        details.append("📍 SIM SLOT\n");
        details.append(log.simSlot).append("\n\n");

        details.append("💬 MESSAGE\n");
        details.append(log.messageBody).append("\n\n");

        details.append("🕐 TIME\n");
        details.append(log.formattedDate).append("\n\n");

        if (log.matchedRuleName != null) {
            details.append("📋 MATCHED RULE\n");
            details.append(log.matchedRuleName).append("\n\n");
        }

        if (log.wasTransformed) {
            details.append("🔄 TRANSFORMATION\n");
            details.append("Type: ").append(log.transformType).append("\n");
            details.append("Original: ").append(log.originalMessage).append("\n");
            details.append("Transformed: ").append(log.transformedMessage).append("\n\n");
        }

        String actions = log.getActionsSummary();
        if (!"No actions".equals(actions)) {
            details.append("🚀 ACTIONS EXECUTED\n");
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

        details.append("💳 CREDITS USED\n");
        details.append(log.creditsUsed).append("\n\n");

        if (log.hasError) {
            details.append("❌ ERROR\n");
            details.append(log.errorMessage).append("\n");
        } else {
            details.append("✅ STATUS\n");
            details.append("Successfully processed");
        }

        new AlertDialog.Builder(this)
                .setTitle("SMS Log Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Delete", (dialog, which) -> deleteLog(log))
                .show();
    }

    private void deleteLog(SmsLog log) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Log")
                .setMessage("Delete this log entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executorService.execute(() -> {
                        db.smsLogDao().deleteLog(log);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Log deleted", Toast.LENGTH_SHORT).show();
                            loadLogs();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearLogsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Old Logs")
                .setMessage("Delete logs older than 30 days?")
                .setPositiveButton("Clear", (dialog, which) -> clearOldLogs())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearOldLogs() {
        executorService.execute(() -> {
            try {
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                int deleted = db.smsLogDao().deleteOldLogs(thirtyDaysAgo);

                Log.d(TAG, "🗑️ Deleted " + deleted + " old logs");

                runOnUiThread(() -> {
                    Toast.makeText(this, "Deleted " + deleted + " old logs", Toast.LENGTH_SHORT).show();
                    loadLogs();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting old logs", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateCreditsDisplay() {
        if (tvCreditsDisplay != null) {
            int credits = creditManager.getCredits();
            tvCreditsDisplay.setText("Credits: " + credits);

            if (credits < 10) {
                tvCreditsDisplay.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvCreditsDisplay.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    public void startService(View v) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show();
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        // Check credits
        if (!creditManager.hasEnoughCredits(1)) {
            new AlertDialog.Builder(this)
                    .setTitle("Low Credits")
                    .setMessage("You need credits to use the service. Watch ads or purchase credits.")
                    .setPositiveButton("Get Credits", (dialog, which) -> {
                        Intent intent = new Intent(MainActivity.this, CreditsActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        Intent serviceIntent = new Intent(this, EndlessService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "✅ Service started", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "✅ Service started");
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        stopService(serviceIntent);

        Toast.makeText(this, "⏹️ Service stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "⏹️ Service stopped");
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
                .setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 MainActivity resumed - refreshing data");
        updateCreditsDisplay();
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