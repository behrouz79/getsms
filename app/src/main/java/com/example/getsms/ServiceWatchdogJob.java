package com.example.getsms;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

/**
 * JobScheduler-based watchdog that checks and restarts the service
 * This runs even when app is killed and survives Doze mode
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ServiceWatchdogJob extends JobService {

    private static final String TAG = "ServiceWatchdogJob";
    private static final int JOB_ID = 1001;
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";

    /**
     * Schedule the watchdog job
     */
    public static void scheduleJob(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.e(TAG, "JobScheduler not available");
                return;
            }

            ComponentName componentName = new ComponentName(context, ServiceWatchdogJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName);

            // Run every 15 minutes (minimum allowed)
            builder.setPeriodic(15 * 60 * 1000L); // 15 minutes

            // Persist across reboots
            builder.setPersisted(true);

            // Run even in Doze mode (requires RECEIVE_BOOT_COMPLETED permission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
            }

            int result = jobScheduler.schedule(builder.build());
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "✅ Watchdog job scheduled successfully");
            } else {
                Log.e(TAG, "❌ Failed to schedule watchdog job");
            }
        }
    }

    /**
     * Cancel the watchdog job
     */
    public static void cancelJob(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(JOB_ID);
                Log.d(TAG, "❌ Watchdog job cancelled");
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "🐕 Watchdog job started - checking service status");

        // Run in background thread
        new Thread(() -> {
            try {
                checkAndRestartService();
            } finally {
                // Job finished
                jobFinished(params, false);
            }
        }).start();

        // Return true because we're handling it asynchronously
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "🐕 Watchdog job stopped");
        // Return true to reschedule the job
        return true;
    }

    /**
     * Check if service is running and restart if needed
     */
    private void checkAndRestartService() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);

        if (!serviceEnabled) {
            Log.d(TAG, "Service disabled by user - no action needed");
            return;
        }

        // Check if service is actually running
        boolean isRunning = isServiceRunning();

        if (!isRunning) {
            Log.w(TAG, "⚠️ SERVICE NOT RUNNING - Restarting now!");
            restartService();
        } else {
            Log.d(TAG, "✅ Service is running correctly");
        }
    }

    /**
     * Check if EndlessService is running
     */
    private boolean isServiceRunning() {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (EndlessService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Restart the service
     */
    private void restartService() {
        try {
            Intent serviceIntent = new Intent(this, EndlessService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "✅ Service restart initiated");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error restarting service", e);
        }
    }
}