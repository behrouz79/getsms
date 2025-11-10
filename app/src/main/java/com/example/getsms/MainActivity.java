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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.getsms.adapter.AdapterRequRec;
import com.example.getsms.credit.CreditManager;
import com.example.getsms.modul.Response;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.roomDB.SmsRecord;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recRequ;
    private AdapterRequRec adapter;
    private List<Response> dataList = new ArrayList<>();
    private DataBase db;
    private Button btnDelete;
    private EditText UrlText;
    private SharedPreferences sharedPref;
    private ExecutorService executorService;

    // Credit system
    private CreditManager creditManager;
    private TextView tvCreditsDisplay;

    // Permission launchers for Android 13+
    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    checkSmsPermission();
                } else {
                    showPermissionRationale("Notification permission is required for foreground service");
                }
            });

    private final ActivityResultLauncher<String> requestSmsPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    showPermissionRationale("SMS permission is required to receive messages");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});

        // Initialize credit manager
        creditManager = new CreditManager(this);
        creditManager.setBackendUrl("https://smsforwarder.amiriprog.ir/api/");

        adapter = new AdapterRequRec(getApplicationContext(), dataList);

        findView();
        setRecRequ();

        // Load data in background thread
        loadDataAsync();

        // Check permissions on startup
        checkPermissions();

        btnDelete.setOnClickListener(view -> deleteOldRecords());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE
            }, 100);
        }

        Button btnOpenRules = findViewById(R.id.btnOpenRules);
        btnOpenRules.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RulesActivity.class);
            startActivity(intent);
        });

        // NEW: Credits button
        Button btnCredits = findViewById(R.id.btnCredits);
        btnCredits.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreditsActivity.class);
            startActivity(intent);
        });

        requestPermissions();

        // Update credits display
        updateCreditsDisplay();
    }

    private void updateCreditsDisplay() {
        if (tvCreditsDisplay != null) {
            int credits = creditManager.getCredits();
            tvCreditsDisplay.setText("Credits: " + credits);

            // Show warning if low credits
            if (credits < 10) {
                tvCreditsDisplay.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvCreditsDisplay.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                requestPermissions(
                        permissionsToRequest.toArray(new String[0]),
                        100
                );
            }
        }
    }

    private void checkPermissions() {
        // For Android 13+ (API 33+), check notification permission first
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
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadDataAsync() {
        executorService.execute(() -> {
            db = DataBase.getDbInstance(MainActivity.this);
            List<SmsRecord> data = db.smsDao().getAllRecord();

            List<Response> tempList = new ArrayList<>();
            for (SmsRecord record : data) {
                tempList.add(new Response(
                        record.uid,
                        record.title,
                        record.date,
                        record.status,
                        record.body
                ));
            }

            runOnUiThread(() -> {
                dataList.clear();
                dataList.addAll(tempList);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void deleteOldRecords() {
        executorService.execute(() -> {
            db = DataBase.getDbInstance(MainActivity.this);
            List<SmsRecord> data = db.smsDao().getLastOlderMonth();
            for (SmsRecord record : data) {
                db.smsDao().deleteRecord(record);
            }

            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Old records deleted", Toast.LENGTH_SHORT).show();
                loadDataAsync();
            });
        });
    }

    public void startService(View v) {
        // Check notification permission before starting service (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show();
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        // Check if user has credits
        if (!creditManager.hasEnoughCredits(1)) {
            new AlertDialog.Builder(this)
                    .setTitle("Low Credits")
                    .setMessage("You don't have enough credits to use the service. Please watch ads or purchase credits.")
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
        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
    }

    public void saveUrl(View v) {
        String url = UrlText.getText().toString();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("Url", url);
        editor.apply();
        Toast.makeText(this, "BaseUrl saved", Toast.LENGTH_SHORT).show();
    }

    public void refresh(View v) {
        loadDataAsync();
        updateCreditsDisplay();
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
    }

    private void findView() {
        recRequ = findViewById(R.id.recRequ);
        btnDelete = findViewById(R.id.btnDelete);
        tvCreditsDisplay = findViewById(R.id.tvCreditsDisplay);
    }

    private void setRecRequ() {
        recRequ.setLayoutManager(
                new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false)
        );
        recRequ.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCreditsDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}