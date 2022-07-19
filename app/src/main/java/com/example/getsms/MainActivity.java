package com.example.getsms;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.getsms.adapter.AdapterRequRec;
import com.example.getsms.modul.Response;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recRequ;
    private List<Response> dataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findView();
        dataList.add(new Response("Ali", "",0));
        dataList.add(new Response("erf", "",2500));
        dataList.add(new Response("erg", "",500));
        //setRecRequ();
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        startService(serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        stopService(serviceIntent);
    }


    private void findView() {
        recRequ = findViewById(R.id.recRequ);
    }

    private void setRecRequ() {
        recRequ.setLayoutManager(
                new LinearLayoutManager(MainActivity.this, dataList.size(), false)
        );
        recRequ.setAdapter(
                new AdapterRequRec(getApplicationContext(), dataList)
        );
    }
}
