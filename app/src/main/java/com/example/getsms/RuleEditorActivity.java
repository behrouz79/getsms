package com.example.getsms;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.adapter.ActionsAdapter;
import com.example.getsms.model.Action;
import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.getsms.utils.SpinnerHelper;

public class RuleEditorActivity extends BaseActivity {

    private static final int REQUEST_ADD_ACTION = 100;
    private static final int REQUEST_EDIT_ACTION = 101;

    // Rule fields
    private EditText etRuleName;
    private Spinner spinnerSim, spinnerSenderType, spinnerMessageType;
    private EditText etSenderValue, etMessageValue;
    // Actions list
    private RecyclerView recyclerActions;
    private ActionsAdapter actionsAdapter;
    private List<Action> actions = new ArrayList<>();

    // Buttons
    private Button btnSave;
    private View btnAddAction, btnCancel;
    private TextView btnBack, tvSave;

    // Data
    private DataBase db;
    private ExecutorService executorService;
    private Rule currentRule;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_editor);

        initializeDatabase();
        initViews();
        setupSpinners();
        setupListeners();
        setupActionsRecyclerView();
        loadRuleIfEditing();
    }

    private void initializeDatabase() {
        executorService = Executors.newSingleThreadExecutor();
        db = DataBase.getDbInstance(this);
    }

    private void initViews() {
        // Rule fields
        etRuleName = findViewById(R.id.etRuleName);
        spinnerSim = findViewById(R.id.spinnerSim);
        spinnerSenderType = findViewById(R.id.spinnerSenderType);
        etSenderValue = findViewById(R.id.etSenderValue);
        spinnerMessageType = findViewById(R.id.spinnerMessageType);
        etMessageValue = findViewById(R.id.etMessageValue);

        // Buttons and RecyclerView
        btnSave      = findViewById(R.id.btnSave);
        btnAddAction = findViewById(R.id.btnAddAction);
        btnCancel    = findViewById(R.id.btnCancel);
        btnBack      = findViewById(R.id.btnBack);
        tvSave       = findViewById(R.id.tvSave);
        recyclerActions = findViewById(R.id.recyclerActions);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveRule());
        tvSave.setOnClickListener(v -> saveRule());
        btnAddAction.setOnClickListener(v -> {
            Intent intent = new Intent(RuleEditorActivity.this, ActionEditorActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ACTION);
        });
    }

    private void setupSpinners() {
        SpinnerHelper.setup(this, spinnerSim, new SpinnerItem[]{
                new SpinnerItem(getString(R.string.sim_any), "ANY"),
                new SpinnerItem(getString(R.string.sim_1), "SIM1"),
                new SpinnerItem(getString(R.string.sim_2), "SIM2"),
                new SpinnerItem(getString(R.string.sim_both), "BOTH")
        });

        SpinnerHelper.setup(this, spinnerSenderType, new SpinnerItem[]{
                new SpinnerItem(getString(R.string.any), "ANY"),
                new SpinnerItem(getString(R.string.equals), "EQUALS"),
                new SpinnerItem(getString(R.string.contains), "CONTAINS"),
                new SpinnerItem(getString(R.string.starts_with), "STARTS_WITH"),
                new SpinnerItem(getString(R.string.regex), "REGEX")
        });

        SpinnerHelper.setup(this, spinnerMessageType, new SpinnerItem[]{
                new SpinnerItem(getString(R.string.any), "ANY"),
                new SpinnerItem(getString(R.string.equals), "EQUALS"),
                new SpinnerItem(getString(R.string.contains), "CONTAINS"),
                new SpinnerItem(getString(R.string.not_contains), "NOT_CONTAINS"),
                new SpinnerItem(getString(R.string.not_equals), "NOT_EQUALS"),
                new SpinnerItem(getString(R.string.regex), "REGEX")
        });
    }

    private void setupActionsRecyclerView() {
        actionsAdapter = new ActionsAdapter(this, actions, new ActionsAdapter.ActionClickListener() {
            @Override
            public void onEditClick(Action action, int position) {
                editAction(action, position);
            }

            @Override
            public void onDeleteClick(Action action, int position) {
                showDeleteActionDialog(position);
            }
        });

        recyclerActions.setLayoutManager(new LinearLayoutManager(this));
        recyclerActions.setAdapter(actionsAdapter);
    }

    private void loadRuleIfEditing() {
        int ruleId = getIntent().getIntExtra("rule_id", -1);
        if (ruleId != -1) {
            loadRule(ruleId);
        }
    }

    private void loadRule(int ruleId) {
        executorService.execute(() -> {
            currentRule = db.ruleDao().getRuleById(ruleId);
            if (currentRule != null) {
                runOnUiThread(() -> populateRuleData());
            }
        });
    }

    private void populateRuleData() {
        etRuleName.setText(currentRule.name);
        SpinnerHelper.setValue(spinnerSim, currentRule.simFilter);
        SpinnerHelper.setValue(spinnerSenderType, currentRule.senderFilterType);
        etSenderValue.setText(currentRule.senderFilterValue);
        SpinnerHelper.setValue(spinnerMessageType, currentRule.messageFilterType);
        etMessageValue.setText(currentRule.messageFilterValue);

        loadActions();
    }

    private void loadActions() {
        if (currentRule.actionsJson != null && !currentRule.actionsJson.isEmpty()) {
            try {
                List<Action> loadedActions = gson.fromJson(
                        currentRule.actionsJson,
                        new TypeToken<List<Action>>() {}.getType()
                );
                actions.clear();
                actions.addAll(loadedActions);
                actionsAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void editAction(Action action, int position) {
        Intent intent = new Intent(RuleEditorActivity.this, ActionEditorActivity.class);
        intent.putExtra("action_json", gson.toJson(action));
        intent.putExtra("action_position", position);
        startActivityForResult(intent, REQUEST_EDIT_ACTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            String actionJson = data.getStringExtra("action_json");
            int position = data.getIntExtra("action_position", -1);

            if (actionJson != null && !actionJson.isEmpty()) {
                try {
                    Action action = gson.fromJson(actionJson, Action.class);

                    if (requestCode == REQUEST_ADD_ACTION) {
                        // Adding new action
                        actions.add(action);
                        actionsAdapter.notifyDataSetChanged();
                        Toast.makeText(this, getString(R.string.action_added, action.type), Toast.LENGTH_SHORT).show();

                    } else if (requestCode == REQUEST_EDIT_ACTION && position >= 0) {
                        // Editing existing action
                        actions.set(position, action);
                        actionsAdapter.notifyDataSetChanged();
                        Toast.makeText(this, R.string.action_updated, Toast.LENGTH_SHORT).show();
                    }

                    // AUTO-UPDATE RULE IN DATABASE
                    autoSaveRuleToDatabase();

                } catch (Exception e) {
                    Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showDeleteActionDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_action)
                .setMessage(R.string.delete_action_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAction(position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAction(int position) {
        actions.remove(position);
        actionsAdapter.notifyItemRemoved(position);
        actionsAdapter.notifyItemRangeChanged(position, actions.size());
        Toast.makeText(this, R.string.action_deleted, Toast.LENGTH_SHORT).show();

        // AUTO-UPDATE RULE IN DATABASE
        autoSaveRuleToDatabase();
    }

    private void autoSaveRuleToDatabase() {
        if (currentRule == null) {
            return;
        }

        executorService.execute(() -> {
            try {
                // Update the rule's actions JSON
                currentRule.actionsJson = gson.toJson(actions);
                currentRule.updatedAt = System.currentTimeMillis();

                // Save to database
                db.ruleDao().updateRule(currentRule);

                runOnUiThread(() -> {
                    // Optional: Show subtle feedback with icon
                    // You can use a Snackbar for better UX:
                    // Snackbar.make(findViewById(android.R.id.content),
                    //     "✓ Auto-saved", Snackbar.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.error_saving_rule, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void saveRule() {
        String name = etRuleName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.enter_rule_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (actions.isEmpty()) {
            Toast.makeText(this, R.string.add_one_action, Toast.LENGTH_SHORT).show();
            return;
        }

        Rule rule = buildRule(name);
        persistRule(rule);
    }

    private Rule buildRule(String name) {
        Rule rule = currentRule != null ? currentRule : new Rule();
        rule.name = name;
        rule.simFilter = SpinnerHelper.getValue(spinnerSim);
        rule.senderFilterType = SpinnerHelper.getValue(spinnerSenderType);
        rule.senderFilterValue = etSenderValue.getText().toString().trim();
        rule.messageFilterType = SpinnerHelper.getValue(spinnerMessageType);
        rule.messageFilterValue = etMessageValue.getText().toString().trim();
        rule.actionsJson = gson.toJson(actions);
        rule.updatedAt = System.currentTimeMillis();
        return rule;
    }

    private void persistRule(Rule rule) {
        executorService.execute(() -> {
            if (currentRule == null) {
                // New rule - insert and update currentRule reference
                long ruleId = db.ruleDao().insertRule(rule);
                rule.id = (int) ruleId;
                currentRule = rule;
            } else {
                // Existing rule - update
                db.ruleDao().updateRule(rule);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, R.string.rule_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Helper Classes
    public static class SpinnerItem {
        public final String label;
        public final String value;

        public SpinnerItem(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private abstract static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            onItemSelected(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }

        public abstract void onItemSelected(int position);
    }

    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            onProgressChanged(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        public abstract void onProgressChanged(int progress);
    }
}