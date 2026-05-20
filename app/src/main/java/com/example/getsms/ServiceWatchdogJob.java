package com.example.getsms;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.getsms.utils.ServicePrefs;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ServiceWatchdogJob extends JobService {

    private static final String TAG = "ServiceWatchdogJob";
    private static final int JOB_ID = 1001;

    public static void scheduleJob(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.e(TAG, "JobScheduler not available");
                return;
            }

            ComponentName componentName = new ComponentName(context, ServiceWatchdogJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName)
                    .setPeriodic(15 * 60 * 1000L)
                    .setPersisted(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
            }

            int result = jobScheduler.schedule(builder.build());
            Log.d(TAG, "Watchdog job schedule result: " + result);
        }
    }

    public static void cancelJob(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancel(JOB_ID);
                Log.d(TAG, "Watchdog job cancelled");
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Watchdog job started");
        new Thread(() -> {
            try {
                checkAndRestartService();
            } finally {
                jobFinished(params, false);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private void checkAndRestartService() {
        if (!ServicePrefs.isServiceEnabled(this)) {
            Log.d(TAG, "Service disabled - no action needed");
            return;
        }

        if (!isServiceRunning()) {
            Log.w(TAG, "Service not running - restarting");
            restartService();
        } else {
            Log.d(TAG, "Service running correctly");
        }
    }

    private boolean isServiceRunning() {
        android.app.ActivityManager manager =
                (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (android.app.ActivityManager.RunningServiceInfo service :
                    manager.getRunningServices(Integer.MAX_VALUE)) {
                if (EndlessService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void restartService() {
        try {
            Intent serviceIntent = new Intent(this, EndlessService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Service restart initiated");
        } catch (Exception e) {
            Log.e(TAG, "Error restarting service", e);
        }
    }
}
