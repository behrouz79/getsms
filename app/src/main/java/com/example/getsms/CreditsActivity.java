package com.example.getsms;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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

public class CreditsActivity extends BaseActivity {

    private TextView tvCredits;
    private TextView tvAdStats;
    private TextView tvCooldownTimer;
    private TextView tvResetTimer;
    private Button btnWatchAd;
    private Button btnRedeemToken;
    private ProgressBar progressBar;

    private CreditManager creditManager;
    private AdsManager adsManager;
    private Handler cooldownHandler;
    private Runnable cooldownRunnable;
    private Handler resetTimerHandler;
    private Runnable resetTimerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        // Initialize managers
        creditManager = new CreditManager(this);
        adsManager = new AdsManager(this, creditManager);

        // Set ad load listener
        adsManager.setAdLoadListener(new AdsManager.AdLoadListener() {
            @Override
            public void onAdLoaded() {
                runOnUiThread(() -> {
                    updateDisplay();
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    updateDisplay();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });

        // Set backend URL (only for token redemption)
        creditManager.setBackendUrl("https://smsforwarder.amiriprog.ir/api/");

        // Initialize views
        tvCredits = findViewById(R.id.tvCredits);
        tvAdStats = findViewById(R.id.tvAdStats);
        tvCooldownTimer = findViewById(R.id.tvCooldownTimer);
        tvResetTimer = findViewById(R.id.tvResetTimer);
        btnWatchAd = findViewById(R.id.btnWatchAd);
        btnRedeemToken = findViewById(R.id.btnRedeemToken);
        progressBar = findViewById(R.id.progressBar);

        // Display current info
        updateDisplay();

        // Initialize ads
        adsManager.initialize();

        // Wait a moment then preload ads
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            adsManager.preloadAds(this);
            updateDisplay();
        }, 2000); // Give 2 seconds for initialization

        // Button listeners
        btnWatchAd.setOnClickListener(v -> showRewardedAd());

        // Long press to see ad status (debug)
        btnWatchAd.setOnLongClickListener(v -> {
            Toast.makeText(this, adsManager.getAdNetworkStatus(), Toast.LENGTH_LONG).show();
            return true;
        });

        btnRedeemToken.setOnClickListener(v -> showTokenRedemptionDialog());

        // Add info button
        findViewById(R.id.btnInfo).setOnClickListener(v -> showInfoDialog());

        // Setup cooldown timer
        setupCooldownTimer();

        // Setup reset timer
        setupResetTimer();
    }

    private void updateDisplay() {
        int credits = creditManager.getCredits();
        tvCredits.setText(getString(R.string.available_credits, credits));

        // Update ad statistics
        int totalAds = creditManager.getTotalAdsWatched();
        int remainingAds = creditManager.getAdsRemainingToday();
        tvAdStats.setText(getString(R.string.ads_watched, totalAds, remainingAds));

        // Update button state
        updateWatchAdButton();
    }

    private void updateWatchAdButton() {
        boolean canWatch = creditManager.canWatchAd();
        boolean adReady = adsManager.isAnyAdReady();

        if (!adReady) {
            btnWatchAd.setEnabled(false);
            btnWatchAd.setText(getString(R.string.loading_ad));
        } else if (!canWatch) {
            btnWatchAd.setEnabled(false);
            long cooldown = creditManager.getAdCooldownRemaining();
            if (cooldown > 0) {
                btnWatchAd.setText(getString(R.string.wait_seconds, (int)(cooldown / 1000)));
            } else {
                btnWatchAd.setText(getString(R.string.daily_limit_reached));
            }
        } else {
            btnWatchAd.setEnabled(true);
            btnWatchAd.setText(getString(R.string.watch_ad));
        }
    }

    private void setupCooldownTimer() {
        cooldownHandler = new Handler(Looper.getMainLooper());
        cooldownRunnable = new Runnable() {
            @Override
            public void run() {
                long cooldown = creditManager.getAdCooldownRemaining();
                if (cooldown > 0) {
                    tvCooldownTimer.setVisibility(View.VISIBLE);
                    tvCooldownTimer.setText("Next ad in: " + (cooldown / 1000) + "s");
                    updateWatchAdButton();
                    cooldownHandler.postDelayed(this, 1000);
                } else {
                    tvCooldownTimer.setVisibility(View.GONE);
                    updateWatchAdButton();
                    // Try to preload ad
                    if (!adsManager.isAnyAdReady()) {
                        adsManager.preloadAds(CreditsActivity.this);
                    }
                }
            }
        };
    }

    private void setupResetTimer() {
        resetTimerHandler = new Handler(Looper.getMainLooper());
        resetTimerRunnable = new Runnable() {
            @Override
            public void run() {
                int remaining = creditManager.getAdsRemainingToday();
                String resetTime = creditManager.getTimeUntilReset();

                if (remaining == 0) {
                    // Show when limit is reached
                    tvResetTimer.setVisibility(View.VISIBLE);
                    tvResetTimer.setText("🌙 Daily limit reached. Resets in: " + resetTime);
                    tvResetTimer.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                } else if (remaining <= 10) {
                    // Show warning when few ads remaining
                    tvResetTimer.setVisibility(View.VISIBLE);
                    tvResetTimer.setText("⚠️ " + remaining + " ads left today. Resets in: " + resetTime);
                    tvResetTimer.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                } else {
                    // Just show reset time
                    tvResetTimer.setVisibility(View.VISIBLE);
                    tvResetTimer.setText("🔄 Counter resets in: " + resetTime);
                    tvResetTimer.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }

                // Update every minute
                resetTimerHandler.postDelayed(this, 60000);
            }
        };
    }

    private void showRewardedAd() {
        if (!creditManager.canWatchAd()) {
            int remaining = creditManager.getAdsRemainingToday();
            if (remaining == 0) {
                String resetTime = creditManager.getTimeUntilReset();
                Toast.makeText(this, "Daily limit reached. Resets in: " + resetTime,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Please wait before watching another ad",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnWatchAd.setEnabled(false);

        adsManager.showRewardedAd(this, new AdsManager.AdRewardCallback() {
            @Override
            public void onRewarded(int credits) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateDisplay();

                    // Show success message with daily stats
                    int remaining = creditManager.getAdsRemainingToday();
                    String message = getString(R.string.credits_earned, credits, creditManager.getCredits(), remaining);

                    new AlertDialog.Builder(CreditsActivity.this)
                            .setTitle(getString(R.string.reward_earned))
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();

                    // Start cooldown timer
                    cooldownHandler.post(cooldownRunnable);

                    // Update reset timer
                    resetTimerHandler.post(resetTimerRunnable);

                    // Preload next ad
                    adsManager.preloadAds(CreditsActivity.this);
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateWatchAdButton();
                    Toast.makeText(CreditsActivity.this, error, Toast.LENGTH_SHORT).show();

                    // Try to load ad again
                    adsManager.preloadAds(CreditsActivity.this);
                });
            }
        });
    }

    private void showTokenRedemptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.redeem_credit_token));
        builder.setMessage(getString(R.string.enter_token));

        final EditText input = new EditText(this);
        input.setHint("XXXXXXXXX");
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.redeem), (dialog, which) -> {
            String token = input.getText().toString().trim();
            if (token.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_valid_token), Toast.LENGTH_SHORT).show();
                return;
            }
            redeemToken(token);
        });

        builder.setNegativeButton(getString(R.string.cancel), null);

        builder.setNeutralButton(getString(R.string.need_credits_message), (dialog, which) -> {
            showContactAdminDialog();
        });

        builder.show();
    }

    private void redeemToken(String token) {
        progressBar.setVisibility(View.VISIBLE);
        btnRedeemToken.setEnabled(false);

        creditManager.redeemToken(token, new CreditManager.CreditCallback() {
            @Override
            public void onSuccess(int totalCredits) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRedeemToken.setEnabled(true);
                    updateDisplay();

                    new AlertDialog.Builder(CreditsActivity.this)
                            .setTitle(getString(R.string.token_redeemed))
                            .setMessage(getString(R.string.credits_added) + totalCredits)
                            .setPositiveButton(getString(R.string.ok), null)
                            .show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRedeemToken.setEnabled(true);

                    new AlertDialog.Builder(CreditsActivity.this)
                            .setTitle(getString(R.string.redemption_failed))
                            .setMessage(error)
                            .setPositiveButton(getString(R.string.ok), null)
                            .setNeutralButton(getString(R.string.contact_admin), (d, w) -> showContactAdminDialog())
                            .show();
                });
            }
        });
    }

    private void showContactAdminDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ok))
                .setMessage(Html.fromHtml(getString(R.string.contact_admin)))
                .setPositiveButton(getString(R.string.ok), null)
                .create();

        dialog.show();

        // Enable clickable links
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void showInfoDialog() {
        String info = "How Credits Work:\n\n" +
                "• Each SMS costs 1 credit\n" +
                "• Watch ads to earn 5 credits per ad\n" +
                "• Maximum " + creditManager.getAdsRemainingToday() + " ads remaining today\n" +
                "• 30 second cooldown between ads\n" +
                "• Daily counter resets at midnight\n\n" +
                "Need More Credits?\n" +
                "Contact admin with your Device ID to purchase token codes.\n\n" +
                "Current Stats:\n" +
                creditManager.getCreditStats();

        new AlertDialog.Builder(this)
                .setTitle("Credits Information")
                .setMessage(info)
                .setPositiveButton(getString(R.string.ok), null)
                .setNeutralButton("Copy Device ID", (d, w) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(
                            "Device ID", creditManager.getDeviceId());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Device ID copied!", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDisplay();
        adsManager.preloadAds(this);

        // Start cooldown timer if needed
        if (creditManager.getAdCooldownRemaining() > 0) {
            cooldownHandler.post(cooldownRunnable);
        }

        // Start reset timer
        resetTimerHandler.post(resetTimerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop timers
        if (cooldownHandler != null && cooldownRunnable != null) {
            cooldownHandler.removeCallbacks(cooldownRunnable);
        }
        if (resetTimerHandler != null && resetTimerRunnable != null) {
            resetTimerHandler.removeCallbacks(resetTimerRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cooldownHandler != null) {
            cooldownHandler.removeCallbacksAndMessages(null);
        }
        if (resetTimerHandler != null) {
            resetTimerHandler.removeCallbacksAndMessages(null);
        }
    }
}