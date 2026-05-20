package com.example.getsms;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.getsms.engine.RuleEngine;

public class App extends Application {
    public static final String CHANNEL_ID = "exampleServiceChannel";

    private RuleEngine ruleEngine;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        ruleEngine = new RuleEngine(this);
    }

    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ghasedak Sms Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
