package com.example.getsms;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.getsms.adapter.AdapterRequRec;
import com.example.getsms.modul.Response;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.roomDB.SmsRecord;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recRequ;
    private AdapterRequRec adapter;
    private List<Response> dataList = new ArrayList<>();
    private DataBase db;
    EditText UrlText;
    SharedPreferences sharedPref ;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UrlText = (EditText) findViewById(R.id.editTextUrl);
        sharedPref = MainActivity.this.getSharedPreferences("BaseUrl",Context.MODE_PRIVATE);

        adapter = new AdapterRequRec(getApplicationContext(), dataList);

        findView();
        getData();
        setRecRequ();
        adapter.notifyItemChanged(dataList.size());
        if (sharedPref.contains("Url")) {
            UrlText.setText(sharedPref.getString("Url", ""));
        }
        checkForSmsPermission();
    }


    private void checkForSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    private void getData() {
        db = DataBase.getDbInstance(MainActivity.this);
        List<SmsRecord> data = db.smsDao().getAllRecord();
        for(int i=0;i< data.size(); i++){
            dataList.add(new Response(data.get(i).uid, data.get(i).title, data.get(i).date, data.get(i).status, data.get(i).body));
        }
        adapter.notifyItemChanged(dataList.size());
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        startService(serviceIntent);
    }

    public void saveUrl(View v) {
        String url = UrlText.getText().toString();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("Url", url);
        editor.apply();
        Toast.makeText(this, "BaseUrl saved", Toast.LENGTH_SHORT).show();
    }

    public void refresh(View v) {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
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
                new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false)
        );

        recRequ.setAdapter(adapter);
        adapter.notifyItemChanged(dataList.size());
    }
}
