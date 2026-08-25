package com.example.getsms;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.adapter.ActionLogsAdapter;
import com.example.getsms.engine.ActionLogger;
import com.example.getsms.model.ActionLog;
import com.example.getsms.roomDB.DataBase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ActionLogsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ActionLogsAdapter adapter;
    private List<ActionLog> logsList = new ArrayList<>();
    private ActionLogger actionLogger;

    private TextView btnBack;
    private TextView tvSubtitle;
    private TextView tvStatTotal;
    private TextView tvStatSuccess;
    private TextView tvStatFailed;
    private TextView tvStatRetried;
    private TabLayout tabFilter;
    private MaterialButton btnExportLogs;
    private MaterialButton btnClearLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_logs);

        actionLogger = new ActionLogger(this);

        initViews();
        setupRecyclerView();
        loadLogs();
        loadStatistics();
    }

    private void initViews() {
        recyclerView    = findViewById(R.id.recyclerActionLogs);
        btnBack         = findViewById(R.id.btnBack);
        tvSubtitle      = findViewById(R.id.tvSubtitle);
        tvStatTotal     = findViewById(R.id.tvStatTotal);
        tvStatSuccess   = findViewById(R.id.tvStatSuccess);
        tvStatFailed    = findViewById(R.id.tvStatFailed);
        tvStatRetried   = findViewById(R.id.tvStatRetried);
        tabFilter       = findViewById(R.id.tabFilter);
        btnExportLogs   = findViewById(R.id.btnExportLogs);
        btnClearLogs    = findViewById(R.id.btnClearLogs);

        btnBack.setOnClickListener(v -> finish());

        tabFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) loadLogs();
                else loadFailedLogs();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnExportLogs.setOnClickListener(v ->
                Toast.makeText(this, "Export coming soon", Toast.LENGTH_SHORT).show());

        btnClearLogs.setOnClickListener(v -> showClearDialog());
    }

    private void setupRecyclerView() {
        adapter = new ActionLogsAdapter(this, logsList, new ActionLogsAdapter.OnLogClickListener() {
            @Override
            public void onLogClick(ActionLog log, int position) {
                showLogDetails(log);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadLogs() {
        actionLogger.getLogsForSms(-1, new ActionLogger.LogsCallback() {
            @Override
            public void onLogsLoaded(List<ActionLog> logs) {
                runOnUiThread(() -> {
                    logsList.clear();
                    logsList.addAll(logs);
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(ActionLogsActivity.this, error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadFailedLogs() {
        new Thread(() -> {
            DataBase db = DataBase.getDbInstance(ActionLogsActivity.this);
            List<ActionLog> logs = db.actionLogDao().getFailedActionLogs();
            runOnUiThread(() -> {
                logsList.clear();
                logsList.addAll(logs);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void loadStatistics() {
        actionLogger.getStatistics(new ActionLogger.StatisticsCallback() {
            @Override
            public void onStatisticsLoaded(ActionLogger.ActionStatistics stats) {
                runOnUiThread(() -> {
                    int success = stats.total - stats.failed;
                    tvStatTotal.setText(String.valueOf(stats.total));
                    tvStatSuccess.setText(String.valueOf(success));
                    tvStatFailed.setText(String.valueOf(stats.failed));
                    tvStatRetried.setText(String.valueOf(stats.retried));
                    tvSubtitle.setText(stats.total + " entries");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvSubtitle.setText("Error loading stats"));
            }
        });
    }

    private void showLogDetails(ActionLog log) {
        new AlertDialog.Builder(this)
                .setTitle("Action Log Details")
                .setMessage(log.getSummary())
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void showClearDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Logs")
                .setMessage("Delete all action logs older than 30 days?")
                .setPositiveButton("Clear", (d, w) -> {
                    actionLogger.cleanOldLogs();
                    Toast.makeText(this, "Old logs cleared", Toast.LENGTH_SHORT).show();
                    loadLogs();
                    loadStatistics();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLogs();
        loadStatistics();
    }
}
