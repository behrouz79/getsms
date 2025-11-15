package com.example.getsms;

import static com.example.getsms.App.CHANNEL_ID;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * FIXED: Truly persistent foreground service that survives:
 * - App being swiped from recents
 * - System killing the app due to low memory
 * - OS stopping the service
 * - Device reboot
 *
 * The service will ONLY stop when user explicitly stops it in the app
 */
public class EndlessService extends Service {

    private static final String TAG = "EndlessService";
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";

    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver screenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "========================================");
        Log.d(TAG, getString(R.string.log_service_created));
        Log.d(TAG, "========================================");

        // Acquire wake lock to keep service running
        acquireWakeLock();

        // Register screen on/off receiver to maintain service
        registerScreenReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, getString(R.string.log_on_start_command));

        // Check if service should be running
        if (!isServiceEnabled()) {
            Log.d(TAG, getString(R.string.log_service_disabled_by_user));
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start as foreground service
        startForeground(1, createNotification());

        // Schedule restart if service is killed
        scheduleServiceRestart();

        Log.d(TAG, getString(R.string.log_service_started_foreground));

        // START_STICKY: System will recreate service if killed
        // START_REDELIVER_INTENT: Will redeliver last intent when restarting
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "========================================");
        Log.d(TAG, getString(R.string.log_service_destroyed));
        Log.d(TAG, "========================================");

        // Release wake lock
        releaseWakeLock();

        // Unregister screen receiver
        unregisterScreenReceiver();

        // If service is still enabled, restart it
        if (isServiceEnabled()) {
            Log.d(TAG, getString(R.string.log_service_scheduling_restart));
            scheduleServiceRestart();

            // Immediate restart attempt
            Intent restartIntent = new Intent(getApplicationContext(), EndlessService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        } else {
            Log.d(TAG, getString(R.string.log_service_stopped_by_user));
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "========================================");
        Log.d(TAG, getString(R.string.log_task_removed));
        Log.d(TAG, "========================================");

        // This is called when user swipes app from recent apps
        // We restart the service if it should be running
        if (isServiceEnabled()) {
            Log.d(TAG, getString(R.string.log_restarting_after_task_removal));

            // Schedule restart via AlarmManager (more reliable)
            scheduleServiceRestart();

            // Immediate restart
            Intent restartIntent = new Intent(getApplicationContext(), EndlessService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        }
    }

    /**
     * Create notification for foreground service
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String title = getString(R.string.app_name);
        String content = getString(R.string.service_running);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Cannot be dismissed
                .setAutoCancel(false)
                .build();
    }

    /**
     * Schedule service restart using AlarmManager
     * This ensures service restarts even if killed by system
     */
    private void scheduleServiceRestart() {
        Intent restartIntent = new Intent(getApplicationContext(), ServiceRestartReceiver.class);
        restartIntent.setAction("RESTART_SERVICE");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                1,
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Cancel any existing alarms
            alarmManager.cancel(pendingIntent);

            // Schedule new alarm to restart service in 10 seconds
            long triggerTime = SystemClock.elapsedRealtime() + 10000; // 10 seconds

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }

            Log.d(TAG, getString(R.string.log_service_restart_scheduled));
        }
    }

    /**
     * Acquire wake lock to keep CPU running
     */
    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "SmsForwarder::ServiceWakeLock"
                );
                wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
                Log.d(TAG, getString(R.string.log_wake_lock_acquired));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring wake lock", e);
        }
    }

    /**
     * Release wake lock
     */
    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
                Log.d(TAG, getString(R.string.log_wake_lock_released));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing wake lock", e);
        }
    }

    /**
     * Register receiver for screen on/off events
     * Helps keep service alive
     */
    private void registerScreenReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);

            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (isServiceEnabled()) {
                        Log.d(TAG, "📱 Screen event: " + intent.getAction());
                        // Refresh notification to keep service alive
                        startForeground(1, createNotification());
                    }
                }
            };

            registerReceiver(screenReceiver, filter);
            Log.d(TAG, getString(R.string.log_screen_receiver_registered));
        } catch (Exception e) {
            Log.e(TAG, "Error registering screen receiver", e);
        }
    }

    /**
     * Unregister screen receiver
     */
    private void unregisterScreenReceiver() {
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
                Log.d(TAG, getString(R.string.log_screen_receiver_unregistered));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering screen receiver", e);
        }
    }

    /**
     * Check if service should be running
     */
    private boolean isServiceEnabled() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SERVICE_ENABLED, false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * BroadcastReceiver to restart service
     */
    public static class ServiceRestartReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Check if service should be running
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);

            if (enabled) {
                Intent serviceIntent = new Intent(context, EndlessService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}