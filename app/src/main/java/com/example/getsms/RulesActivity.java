package com.example.getsms;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.adapter.RulesAdapter;
import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RulesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private RulesAdapter adapter;
    private List<Rule> rulesList = new ArrayList<>();
    private DataBase db;
    private ExecutorService executorService;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        executorService = Executors.newSingleThreadExecutor();
        db = DataBase.getDbInstance(this);

        recyclerView = findViewById(R.id.recyclerRules);
        fabAdd = findViewById(R.id.fabAddRule);

        adapter = new RulesAdapter(this, rulesList, new RulesAdapter.RuleClickListener() {
            @Override
            public void onEditClick(Rule rule) {
                openRuleEditor(rule);
            }

            @Override
            public void onDeleteClick(Rule rule) {
                deleteRule(rule);
            }

            @Override
            public void onToggleClick(Rule rule) {
                toggleRule(rule);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> openRuleEditor(null));

        loadRules();
    }

    private void loadRules() {
        executorService.execute(() -> {
            List<Rule> rules = db.ruleDao().getAllRules();
            runOnUiThread(() -> {
                rulesList.clear();
                if (rules != null) {
                    rulesList.addAll(rules);
                }
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void openRuleEditor(Rule rule) {
        Intent intent = new Intent(this, RuleEditorActivity.class);
        if (rule != null) {
            intent.putExtra("rule_id", rule.id);
        }
        startActivity(intent);
    }

    private void deleteRule(Rule rule) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Rule")
                .setMessage("Are you sure you want to delete this rule?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executorService.execute(() -> {
                        db.ruleDao().deleteRule(rule);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Rule deleted", Toast.LENGTH_SHORT).show();
                            loadRules();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleRule(Rule rule) {
        executorService.execute(() -> {
            rule.enabled = !rule.enabled;
            rule.updatedAt = System.currentTimeMillis();
            db.ruleDao().updateRule(rule);

            runOnUiThread(() -> {
                Toast.makeText(this,
                        rule.enabled ? "Rule enabled" : "Rule disabled",
                        Toast.LENGTH_SHORT).show();
                loadRules();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRules();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}