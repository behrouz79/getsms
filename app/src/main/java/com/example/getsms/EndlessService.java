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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * ULTRA-PERSISTENT service that survives indefinitely
 * Improvements:
 * - Indefinite wake lock with periodic renewal
 * - Periodic health check every 15 minutes
 * - Multiple restart mechanisms
 * - Better handling of Doze mode
 */
public class EndlessService extends Service {

    private static final String TAG = "EndlessService";
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_SERVICE_ENABLED = "service_enabled";

    // Health check interval: 15 minutes
    private static final long HEALTH_CHECK_INTERVAL = 15 * 60 * 1000L;

    // Wake lock renewal interval: 5 minutes
    private static final long WAKE_LOCK_RENEWAL_INTERVAL = 5 * 60 * 1000L;

    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver screenReceiver;
    private Handler healthCheckHandler;
    private Handler wakeLockRenewalHandler;
    private long lastHealthCheck = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "========================================");
        Log.d(TAG, "🚀 Service created at " + System.currentTimeMillis());
        Log.d(TAG, "========================================");

        // Acquire indefinite wake lock
        acquireWakeLock();

        // Register screen on/off receiver
        registerScreenReceiver();

        // Start periodic health checks
        startHealthCheck();

        // Start wake lock renewal
        startWakeLockRenewal();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "⚡ onStartCommand called");

        // Check if service should be running
        if (!isServiceEnabled()) {
            Log.d(TAG, "❌ Service disabled by user - stopping");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start as foreground service immediately
        startForeground(1, createNotification());

        // Schedule multiple restart mechanisms
        scheduleServiceRestart();
        scheduleAlarmManagerRestart();

        Log.d(TAG, "✅ Service started successfully in foreground");

        // Return START_STICKY for automatic restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "========================================");
        Log.d(TAG, "💀 Service destroyed at " + System.currentTimeMillis());
        Log.d(TAG, "========================================");

        // Stop health checks
        stopHealthCheck();
        stopWakeLockRenewal();

        // Release wake lock
        releaseWakeLock();

        // Unregister screen receiver
        unregisterScreenReceiver();

        // If service is still enabled, restart it immediately
        if (isServiceEnabled()) {
            Log.d(TAG, "🔄 Service still enabled - triggering restart");

            // Multiple restart mechanisms for redundancy
            scheduleServiceRestart();
            scheduleAlarmManagerRestart();

            // Immediate restart attempt
            restartServiceImmediately();
        } else {
            Log.d(TAG, "⏹️ Service stopped by user - not restarting");
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "========================================");
        Log.d(TAG, "📱 Task removed - app swiped from recents");
        Log.d(TAG, "========================================");

        // This is critical - restart service when user swipes app
        if (isServiceEnabled()) {
            Log.d(TAG, "🔄 Restarting service after task removal");

            scheduleServiceRestart();
            scheduleAlarmManagerRestart();
            restartServiceImmediately();
        }
    }

    /**
     * NEW: Start periodic health checks to ensure service is alive
     */
    private void startHealthCheck() {
        healthCheckHandler = new Handler(Looper.getMainLooper());
        healthCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performHealthCheck();
                // Schedule next check
                healthCheckHandler.postDelayed(this, HEALTH_CHECK_INTERVAL);
            }
        }, HEALTH_CHECK_INTERVAL);

        Log.d(TAG, "💓 Health check started - interval: " + (HEALTH_CHECK_INTERVAL / 1000) + "s");
    }

    /**
     * NEW: Stop health checks
     */
    private void stopHealthCheck() {
        if (healthCheckHandler != null) {
            healthCheckHandler.removeCallbacksAndMessages(null);
            healthCheckHandler = null;
            Log.d(TAG, "💔 Health check stopped");
        }
    }

    /**
     * NEW: Perform health check
     */
    private void performHealthCheck() {
        lastHealthCheck = System.currentTimeMillis();
        Log.d(TAG, "💓 Health check: Service is alive");

        // Refresh notification to keep service visible
        try {
            startForeground(1, createNotification());
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }

        // Ensure restart mechanisms are in place
        scheduleAlarmManagerRestart();
    }

    /**
     * NEW: Start wake lock renewal
     */
    private void startWakeLockRenewal() {
        wakeLockRenewalHandler = new Handler(Looper.getMainLooper());
        wakeLockRenewalHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                renewWakeLock();
                // Schedule next renewal
                wakeLockRenewalHandler.postDelayed(this, WAKE_LOCK_RENEWAL_INTERVAL);
            }
        }, WAKE_LOCK_RENEWAL_INTERVAL);

        Log.d(TAG, "🔋 Wake lock renewal started");
    }

    /**
     * NEW: Stop wake lock renewal
     */
    private void stopWakeLockRenewal() {
        if (wakeLockRenewalHandler != null) {
            wakeLockRenewalHandler.removeCallbacksAndMessages(null);
            wakeLockRenewalHandler = null;
            Log.d(TAG, "🔋 Wake lock renewal stopped");
        }
    }

    /**
     * NEW: Renew wake lock
     */
    private void renewWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                // Wake lock is still held, acquire for another period
                wakeLock.acquire(10 * 60 * 1000L); // 10 minutes
                Log.d(TAG, "🔋 Wake lock renewed");
            } else {
                // Wake lock was released, re-acquire it
                Log.w(TAG, "⚠️ Wake lock was released - re-acquiring");
                acquireWakeLock();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error renewing wake lock", e);
            // Try to re-acquire
            acquireWakeLock();
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

        // Add last health check info
        if (lastHealthCheck > 0) {
            long minutesAgo = (System.currentTimeMillis() - lastHealthCheck) / 60000;
            content += R.string.last_check + minutesAgo + R.string.m_ago;
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Cannot be dismissed
                .setAutoCancel(false)
                .setShowWhen(true)
                .build();
    }

    /**
     * Immediate service restart
     */
    private void restartServiceImmediately() {
        try {
            Intent restartIntent = new Intent(getApplicationContext(), EndlessService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
            Log.d(TAG, "✅ Immediate restart triggered");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error restarting service immediately", e);
        }
    }

    /**
     * Schedule service restart using broadcast receiver (10 seconds)
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
            alarmManager.cancel(pendingIntent);

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

            Log.d(TAG, "⏰ Service restart scheduled (10s)");
        }
    }

    /**
     * NEW: Additional alarm for redundancy (30 seconds)
     */
    private void scheduleAlarmManagerRestart() {
        Intent restartIntent = new Intent(getApplicationContext(), ServiceRestartReceiver.class);
        restartIntent.setAction("RESTART_SERVICE_BACKUP");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                2, // Different request code
                restartIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);

            long triggerTime = SystemClock.elapsedRealtime() + 30000; // 30 seconds

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

            Log.d(TAG, "⏰ Backup restart scheduled (30s)");
        }
    }

    /**
     * MODIFIED: Acquire indefinite wake lock with periodic renewal
     */
    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && (wakeLock == null || !wakeLock.isHeld())) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "SmsForwarder::ServiceWakeLock"
                );
                // Acquire for 10 minutes initially (will be renewed automatically)
                wakeLock.acquire(10 * 60 * 1000L);
                Log.d(TAG, "🔋 Wake lock acquired (with auto-renewal)");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error acquiring wake lock", e);
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
                Log.d(TAG, "🔋 Wake lock released");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error releasing wake lock", e);
        }
    }

    /**
     * Register receiver for screen on/off events
     */
    private void registerScreenReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);

            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (isServiceEnabled()) {
                        String action = intent.getAction();
                        Log.d(TAG, "📱 Screen event: " + action);

                        // Refresh notification and ensure service is alive
                        startForeground(1, createNotification());

                        // On screen on, perform immediate health check
                        if (Intent.ACTION_SCREEN_ON.equals(action) ||
                                Intent.ACTION_USER_PRESENT.equals(action)) {
                            performHealthCheck();
                        }
                    }
                }
            };

            registerReceiver(screenReceiver, filter);
            Log.d(TAG, "📱 Screen receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error registering screen receiver", e);
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
                Log.d(TAG, "📱 Screen receiver unregistered");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error unregistering screen receiver", e);
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
            String action = intent.getAction();
            Log.d(TAG, "🔔 Restart receiver triggered: " + action);

            // Check if service should be running
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false);

            if (enabled) {
                Log.d(TAG, "🔄 Restarting service from receiver");
                Intent serviceIntent = new Intent(context, EndlessService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else {
                Log.d(TAG, "⏹️ Service disabled - not restarting");
            }
        }
    }
}