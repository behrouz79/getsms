package com.example.getsms;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        startService(serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        stopService(serviceIntent);
    }

}