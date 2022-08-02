package com.example.getsms;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new AdapterRequRec(getApplicationContext(), dataList);

        findView();
        getData();
        setRecRequ();
        adapter.notifyItemChanged(dataList.size());


    }

    private void getData() {
        db = DataBase.getDbInstance(MainActivity.this);
        List<SmsRecord> data = db.smsDao().getAllRecord();
        for(int i=0;i< data.size(); i++){
            dataList.add(new Response(data.get(i).uid, data.get(i).title, data.get(i).date, data.get(i).status));
        }
        adapter.notifyItemChanged(dataList.size());
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, EndlessService.class);
        startService(serviceIntent);
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
