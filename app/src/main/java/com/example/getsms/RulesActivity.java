package com.example.getsms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.adapter.RulesAdapter;
import com.example.getsms.backup.BackupManager;
import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RulesActivity extends BaseActivity {

    private RecyclerView recyclerRules;
    private RulesAdapter rulesAdapter;
    private List<Rule> rulesList = new ArrayList<>();
    private Button btnBackup, btnRestore, fabAddRule;

    private DataBase db;
    private ExecutorService executorService;
    private BackupManager backupManager;

    // Activity result launchers for file operations
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        initializeComponents();
        setupRecyclerView();
        setupButtons();
        setupFileOperations();
        loadRules();
    }

    private void initializeComponents() {
        executorService = Executors.newSingleThreadExecutor();
        db = DataBase.getDbInstance(this);
        backupManager = new BackupManager(this);

        recyclerRules = findViewById(R.id.recyclerRules);
        fabAddRule = findViewById(R.id.fabAddRule);
        btnBackup = findViewById(R.id.btnBackup);
        btnRestore = findViewById(R.id.btnRestore);
    }

    private void setupFileOperations() {
        // Export launcher - Create file
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            exportRules(uri);
                        }
                    }
                }
        );

        // Import launcher - Pick file
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            previewAndImportRules(uri);
                        }
                    }
                }
        );
    }

    private void setupRecyclerView() {
        rulesAdapter = new RulesAdapter(this, rulesList, new RulesAdapter.RuleClickListener() {
            @Override
            public void onEditClick(Rule rule) {
                editRule(rule);
            }

            @Override
            public void onDeleteClick(Rule rule) {
                showDeleteDialog(rule);
            }

            @Override
            public void onToggleClick(Rule rule) {
                toggleRule(rule);
            }

            @Override
            public void onCopyClick(Rule rule) {
                copyRule(rule);
            }
        });

        recyclerRules.setLayoutManager(new LinearLayoutManager(this));
        recyclerRules.setAdapter(rulesAdapter);

        // Enable drag and drop for reordering
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                Collections.swap(rulesList, fromPosition, toPosition);
                rulesAdapter.notifyItemMoved(fromPosition, toPosition);
                updatePriorities();
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerRules);
    }

    private void setupButtons() {
        // FAB - Add new rule
        fabAddRule.setOnClickListener(v -> {
            Intent intent = new Intent(RulesActivity.this, RuleEditorActivity.class);
            startActivity(intent);
        });

        // Backup button
        btnBackup.setOnClickListener(v -> startBackup());

        // Restore button
        btnRestore.setOnClickListener(v -> startRestore());

        // Long press on backup button to show delete all option
        btnBackup.setOnLongClickListener(v -> {
            showDeleteAllDialog();
            return true;
        });
    }

    private void startBackup() {
        // Check if there are rules to backup
        if (rulesList.isEmpty()) {
            Toast.makeText(this, R.string.no_rules_to_backup, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, BackupManager.getDefaultBackupFileName());
        exportLauncher.launch(intent);
    }

    private void exportRules(Uri uri) {
        // Show progress
        Toast.makeText(this, R.string.backing_up, Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            boolean success = backupManager.exportRules(uri);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, R.string.backup_success, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.backup_failed, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void startRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importLauncher.launch(intent);
    }

    private void previewAndImportRules(Uri uri) {
        // Show progress
        Toast.makeText(this, R.string.loading_backup, Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            BackupManager.BackupData preview = backupManager.previewBackup(uri);

            runOnUiThread(() -> {
                if (preview != null && preview.rules != null) {
                    showImportDialog(uri, preview);
                } else {
                    Toast.makeText(this, R.string.invalid_backup_file, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showImportDialog(Uri uri, BackupManager.BackupData preview) {
        String message = getString(R.string.restore_preview,
                preview.ruleCount,
                preview.getFormattedDate(),
                preview.appVersion);

        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_rules)
                .setMessage(message)
                .setPositiveButton(R.string.replace_all, (dialog, which) -> {
                    confirmReplaceAll(uri);
                })
                .setNeutralButton(R.string.merge, (dialog, which) -> {
                    importRules(uri, false);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmReplaceAll(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.replace_all_warning)
                .setPositiveButton(R.string.yes_replace, (dialog, which) -> {
                    importRules(uri, true);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void importRules(Uri uri, boolean replaceExisting) {
        // Show progress
        Toast.makeText(this, R.string.restoring, Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            BackupManager.ImportResult result = backupManager.importRules(uri, replaceExisting);

            runOnUiThread(() -> {
                if (result.success) {
                    Toast.makeText(this, result.getSummary(), Toast.LENGTH_LONG).show();
                    loadRules();
                } else {
                    Toast.makeText(this, getString(R.string.restore_failed, result.errorMessage),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadRules() {
        executorService.execute(() -> {
            List<Rule> rules = db.ruleDao().getAllRules();
            runOnUiThread(() -> {
                rulesList.clear();
                rulesList.addAll(rules);
                rulesAdapter.notifyDataSetChanged();
            });
        });
    }

    private void editRule(Rule rule) {
        Intent intent = new Intent(this, RuleEditorActivity.class);
        intent.putExtra("rule_id", rule.id);
        startActivity(intent);
    }

    private void showDeleteDialog(Rule rule) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_rule)
                .setMessage(getString(R.string.delete_rule_confirm, rule.name))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteRule(rule))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteRule(Rule rule) {
        executorService.execute(() -> {
            db.ruleDao().deleteRule(rule);
            runOnUiThread(() -> {
                loadRules();
                Toast.makeText(this, R.string.rule_deleted, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void toggleRule(Rule rule) {
        executorService.execute(() -> {
            rule.enabled = !rule.enabled;
            db.ruleDao().updateRule(rule);
            runOnUiThread(() -> {
                loadRules();
                String message = rule.enabled ? getString(R.string.rule_enabled) :
                        getString(R.string.rule_disabled);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void copyRule(Rule rule) {
        executorService.execute(() -> {
            Rule newRule = new Rule();
            newRule.name = rule.name + " (Copy)";
            newRule.enabled = rule.enabled;
            newRule.priority = rule.priority;
            newRule.simFilter = rule.simFilter;
            newRule.senderFilterType = rule.senderFilterType;
            newRule.senderFilterValue = rule.senderFilterValue;
            newRule.messageFilterType = rule.messageFilterType;
            newRule.messageFilterValue = rule.messageFilterValue;
            newRule.actionsJson = rule.actionsJson;
            newRule.createdAt = System.currentTimeMillis();
            newRule.updatedAt = System.currentTimeMillis();

            db.ruleDao().insertRule(newRule);

            runOnUiThread(() -> {
                loadRules();
                Toast.makeText(this, R.string.rule_copied, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updatePriorities() {
        executorService.execute(() -> {
            for (int i = 0; i < rulesList.size(); i++) {
                Rule rule = rulesList.get(i);
                rule.priority = i;
                db.ruleDao().updateRule(rule);
            }
        });
    }

    private void showDeleteAllDialog() {
        if (rulesList.isEmpty()) {
            Toast.makeText(this, R.string.no_rules_to_delete, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_all_rules)
                .setMessage(R.string.delete_all_rules_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAllRules())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAllRules() {
        executorService.execute(() -> {
            db.ruleDao().deleteAllRules();
            runOnUiThread(() -> {
                loadRules();
                Toast.makeText(this, R.string.all_rules_deleted, Toast.LENGTH_SHORT).show();
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