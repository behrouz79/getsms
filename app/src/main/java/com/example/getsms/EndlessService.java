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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.getsms.utils.ServicePrefs;

public class EndlessService extends Service {

    private static final String TAG = "EndlessService";

    private static final long HEALTH_CHECK_INTERVAL = 15 * 60 * 1000L;
    private static final long WAKE_LOCK_RENEWAL_INTERVAL = 5 * 60 * 1000L;

    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver screenReceiver;
    private Handler healthCheckHandler;
    private Handler wakeLockRenewalHandler;
    private long lastHealthCheck = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        acquireWakeLock();
        registerScreenReceiver();
        startHealthCheck();
        startWakeLockRenewal();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!ServicePrefs.isServiceEnabled(this)) {
            Log.d(TAG, "Service disabled by user - stopping");
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(1, createNotification());
        scheduleServiceRestart();
        scheduleAlarmManagerRestart();
        Log.d(TAG, "Service started in foreground");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopHealthCheck();
        stopWakeLockRenewal();
        releaseWakeLock();
        unregisterScreenReceiver();

        if (ServicePrefs.isServiceEnabled(this)) {
            Log.d(TAG, "Service still enabled - scheduling restart");
            scheduleServiceRestart();
            scheduleAlarmManagerRestart();
            restartServiceImmediately();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "Task removed");
        if (ServicePrefs.isServiceEnabled(this)) {
            scheduleServiceRestart();
            scheduleAlarmManagerRestart();
            restartServiceImmediately();
        }
    }

    private void startHealthCheck() {
        healthCheckHandler = new Handler(Looper.getMainLooper());
        healthCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performHealthCheck();
                healthCheckHandler.postDelayed(this, HEALTH_CHECK_INTERVAL);
            }
        }, HEALTH_CHECK_INTERVAL);
    }

    private void stopHealthCheck() {
        if (healthCheckHandler != null) {
            healthCheckHandler.removeCallbacksAndMessages(null);
            healthCheckHandler = null;
        }
    }

    private void performHealthCheck() {
        lastHealthCheck = System.currentTimeMillis();
        Log.d(TAG, "Health check: Service alive");
        try {
            startForeground(1, createNotification());
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }
        scheduleAlarmManagerRestart();
    }

    private void startWakeLockRenewal() {
        wakeLockRenewalHandler = new Handler(Looper.getMainLooper());
        wakeLockRenewalHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                renewWakeLock();
                wakeLockRenewalHandler.postDelayed(this, WAKE_LOCK_RENEWAL_INTERVAL);
            }
        }, WAKE_LOCK_RENEWAL_INTERVAL);
    }

    private void stopWakeLockRenewal() {
        if (wakeLockRenewalHandler != null) {
            wakeLockRenewalHandler.removeCallbacksAndMessages(null);
            wakeLockRenewalHandler = null;
        }
    }

    private void renewWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L);
            } else {
                acquireWakeLock();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error renewing wake lock", e);
            acquireWakeLock();
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String content = getString(R.string.service_running);
        if (lastHealthCheck > 0) {
            long minutesAgo = (System.currentTimeMillis() - lastHealthCheck) / 60000;
            content += R.string.last_check + minutesAgo + R.string.m_ago;
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShowWhen(true)
                .build();
    }

    private void restartServiceImmediately() {
        try {
            Intent restartIntent = new Intent(getApplicationContext(), EndlessService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restarting service immediately", e);
        }
    }

    private void scheduleServiceRestart() {
        Intent restartIntent = new Intent(getApplicationContext(), ServiceRestartReceiver.class);
        restartIntent.setAction("RESTART_SERVICE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 1, restartIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            long triggerTime = SystemClock.elapsedRealtime() + 10000;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    private void scheduleAlarmManagerRestart() {
        Intent restartIntent = new Intent(getApplicationContext(), ServiceRestartReceiver.class);
        restartIntent.setAction("RESTART_SERVICE_BACKUP");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 2, restartIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            long triggerTime = SystemClock.elapsedRealtime() + 30000;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && (wakeLock == null || !wakeLock.isHeld())) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK, "SmsForwarder::ServiceWakeLock");
                wakeLock.acquire(10 * 60 * 1000L);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring wake lock", e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing wake lock", e);
        }
    }

    private void registerScreenReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);

            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!ServicePrefs.isServiceEnabled(context)) return;
                    startForeground(1, createNotification());
                    String action = intent.getAction();
                    if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                        performHealthCheck();
                    }
                }
            };

            registerReceiver(screenReceiver, filter);
        } catch (Exception e) {
            Log.e(TAG, "Error registering screen receiver", e);
        }
    }

    private void unregisterScreenReceiver() {
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering screen receiver", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class ServiceRestartReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Restart receiver triggered: " + intent.getAction());
            if (!ServicePrefs.isServiceEnabled(context)) return;

            Intent serviceIntent = new Intent(context, EndlessService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
