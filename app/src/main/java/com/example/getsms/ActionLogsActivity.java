package com.example.getsms;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.adapter.ActionLogsAdapter;
import com.example.getsms.engine.ActionLogger;
import com.example.getsms.model.ActionLog;
import com.example.getsms.roomDB.DataBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to view detailed action execution logs
 * FIXED: Removed 'var' keyword for Java 8 compatibility
 */
public class ActionLogsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ActionLogsAdapter adapter;
    private List<ActionLog> logsList = new ArrayList<>();
    private ActionLogger actionLogger;

    private TextView tvStats;
    private Button btnShowFailed;
    private Button btnShowAll;
    private Button btnClearLogs;

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
        recyclerView = findViewById(R.id.recyclerActionLogs);
        tvStats = findViewById(R.id.tvStats);
        btnShowFailed = findViewById(R.id.btnShowFailed);
        btnShowAll = findViewById(R.id.btnShowAll);
        btnClearLogs = findViewById(R.id.btnClearLogs);

        btnShowFailed.setOnClickListener(v -> {
            loadFailedLogs();
            btnShowAll.setBackgroundResource(R.drawable.btn_outline);
            btnShowFailed.setBackgroundResource(R.drawable.btn_error);
            btnShowAll.setTextColor(getResources().getColor(R.color.claude_text_primary));
            btnShowFailed.setTextColor(getResources().getColor(R.color.white));
        });
        btnShowAll.setOnClickListener(v -> {
            loadLogs();
            btnShowAll.setBackgroundResource(R.drawable.btn_primary);
            btnShowFailed.setBackgroundResource(R.drawable.btn_outline);
            btnShowAll.setTextColor(getResources().getColor(R.color.white));
            btnShowFailed.setTextColor(getResources().getColor(R.color.claude_text_primary));
        });
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logsList.clear();
                        logsList.addAll(logs);
                        adapter.notifyDataSetChanged();

                        btnShowAll.setBackgroundResource(R.drawable.btn_primary);
                        btnShowFailed.setBackgroundResource(R.drawable.btn_outline);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ActionLogsActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadFailedLogs() {
        // Load only failed actions
        // FIXED: Removed 'var' keyword, added explicit types, using correct method name
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataBase db = DataBase.getDbInstance(ActionLogsActivity.this);
                List<ActionLog> logs = db.actionLogDao().getFailedActionLogs();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logsList.clear();
                        logsList.addAll(logs);
                        adapter.notifyDataSetChanged();

                        btnShowAll.setBackgroundResource(R.drawable.btn_outline);
                        btnShowFailed.setBackgroundResource(R.drawable.btn_error);
                    }
                });
            }
        }).start();
    }

    private void loadStatistics() {
        actionLogger.getStatistics(new ActionLogger.StatisticsCallback() {
            @Override
            public void onStatisticsLoaded(ActionLogger.ActionStatistics stats) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStats.setText(
                                String.format("%d total  ·  %.1f%% success  ·  %d failed  ·  %d retried",
                                        stats.total, stats.successRate, stats.failed, stats.retried)
                        );
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStats.setText("Error loading stats");
                    }
                });
            }
        });
    }

    private void showLogDetails(ActionLog log) {
        new AlertDialog.Builder(this)
                .setTitle("📋 Action Log Details")
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