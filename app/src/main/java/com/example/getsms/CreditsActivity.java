package com.example.getsms;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.getsms.credit.AdsManager;
import com.example.getsms.credit.CreditManager;

public class CreditsActivity extends AppCompatActivity {

    private TextView tvCredits;
    private TextView tvUserId;
    private Button btnWatchAd;
    private Button btnPurchaseKey;
    private Button btnRefreshCredits;
    private ProgressBar progressBar;

    private CreditManager creditManager;
    private AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        // Initialize managers
        creditManager = new CreditManager(this);
        adsManager = new AdsManager(this, creditManager);

        // Set backend URL (configure this)
        creditManager.setBackendUrl("https://your-django-backend.com/api/");

        // Initialize views
        tvCredits = findViewById(R.id.tvCredits);
        tvUserId = findViewById(R.id.tvUserId);
        btnWatchAd = findViewById(R.id.btnWatchAd);
        btnPurchaseKey = findViewById(R.id.btnPurchaseKey);
        btnRefreshCredits = findViewById(R.id.btnRefreshCredits);
        progressBar = findViewById(R.id.progressBar);

        // Display current credits
        updateCreditDisplay();
        tvUserId.setText("User ID: " + creditManager.getUserId());

        // Initialize ads
        adsManager.initialize();
        adsManager.preloadAds();

        // Button listeners
        btnWatchAd.setOnClickListener(v -> showRewardedAd());
        btnPurchaseKey.setOnClickListener(v -> showPurchaseKeyDialog());
        btnRefreshCredits.setOnClickListener(v -> refreshCreditsFromServer());

        // Fetch latest credits from server
        refreshCreditsFromServer();
    }

    private void updateCreditDisplay() {
        int credits = creditManager.getCredits();
        tvCredits.setText("Available Credits: " + credits);

        // Update button states
        if (adsManager.isAdMobAdReady()) {
            btnWatchAd.setEnabled(true);
            btnWatchAd.setText("Watch Ad (+5 Credits)");
        } else {
            btnWatchAd.setEnabled(false);
            btnWatchAd.setText("Loading Ad...");
            adsManager.preloadAds();
        }
    }

    private void showRewardedAd() {
        progressBar.setVisibility(View.VISIBLE);
        btnWatchAd.setEnabled(false);

        adsManager.showRewardedAd(this, new AdsManager.AdRewardCallback() {
            @Override
            public void onRewarded(int credits) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateCreditDisplay();
                    Toast.makeText(CreditsActivity.this,
                            "+" + credits + " credits earned!",
                            Toast.LENGTH_SHORT).show();

                    // Preload next ad
                    adsManager.preloadAds();
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnWatchAd.setEnabled(true);
                    Toast.makeText(CreditsActivity.this, error, Toast.LENGTH_SHORT).show();

                    // Try to load ad again
                    adsManager.preloadAds();
                });
            }
        });
    }

    private void showPurchaseKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Purchase Key");
        builder.setMessage("Enter the purchase key you received:");

        final EditText input = new EditText(this);
        input.setHint("XXXX-XXXX-XXXX-XXXX");
        builder.setView(input);

        builder.setPositiveButton("Redeem", (dialog, which) -> {
            String key = input.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter a valid key", Toast.LENGTH_SHORT).show();
                return;
            }
            redeemPurchaseKey(key);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void redeemPurchaseKey(String key) {
        progressBar.setVisibility(View.VISIBLE);
        btnPurchaseKey.setEnabled(false);

        creditManager.purchaseWithKey(key, new CreditManager.CreditCallback() {
            @Override
            public void onSuccess(int credits) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnPurchaseKey.setEnabled(true);
                    updateCreditDisplay();

                    new AlertDialog.Builder(CreditsActivity.this)
                            .setTitle("Success!")
                            .setMessage("Credits added successfully!\nYour new balance: " + credits)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnPurchaseKey.setEnabled(true);

                    new AlertDialog.Builder(CreditsActivity.this)
                            .setTitle("Purchase Failed")
                            .setMessage(error)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    private void refreshCreditsFromServer() {
        progressBar.setVisibility(View.VISIBLE);

        creditManager.fetchCreditsFromBackend(new CreditManager.CreditCallback() {
            @Override
            public void onSuccess(int credits) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateCreditDisplay();
                    Toast.makeText(CreditsActivity.this,
                            "Credits synced: " + credits,
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreditsActivity.this,
                            "Sync failed: " + error,
                            Toast.LENGTH_SHORT).show();
                    updateCreditDisplay(); // Show local credits
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCreditDisplay();
        adsManager.preloadAds();
    }
}