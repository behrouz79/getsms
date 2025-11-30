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

    private void copyRule(Rule originalRule) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.copy_rule)
                .setMessage(R.string.copy_rule_confirm)
                .setPositiveButton(R.string.copy, (dialog, which) -> {
                    executorService.execute(() -> {
                        // Create a new Rule object with copied data
                        Rule copiedRule = new Rule();
                        copiedRule.name = originalRule.name + " (Copy)";
                        copiedRule.enabled = originalRule.enabled;
                        copiedRule.priority = originalRule.priority;
                        copiedRule.simFilter = originalRule.simFilter;
                        copiedRule.senderFilterType = originalRule.senderFilterType;
                        copiedRule.senderFilterValue = originalRule.senderFilterValue;
                        copiedRule.messageFilterType = originalRule.messageFilterType;
                        copiedRule.messageFilterValue = originalRule.messageFilterValue;

                        // Copy the entire actions JSON (includes all action configurations)
                        copiedRule.actionsJson = originalRule.actionsJson;

                        copiedRule.createdAt = System.currentTimeMillis();
                        copiedRule.updatedAt = System.currentTimeMillis();

                        // Insert the copied rule
                        db.ruleDao().insertRule(copiedRule);

                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.rule_copied, Toast.LENGTH_SHORT).show();
                            loadRules();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

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

            @Override
            public void onCopyClick(Rule rule) {
                copyRule(rule); // NEW
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
                .setTitle(R.string.delete_rule)
                .setMessage(R.string.delete_rule_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    executorService.execute(() -> {
                        db.ruleDao().deleteRule(rule);
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.delete_rule, Toast.LENGTH_SHORT).show();
                            loadRules();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void toggleRule(Rule rule) {
        executorService.execute(() -> {
            rule.enabled = !rule.enabled;
            rule.updatedAt = System.currentTimeMillis();
            db.ruleDao().updateRule(rule);

            runOnUiThread(() -> {
                Toast.makeText(this,
                        rule.enabled ? R.string.rule_enabled : R.string.rule_disabled,
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