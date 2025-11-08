package com.example.getsms;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

public class RuleEditorActivity extends AppCompatActivity {

    private EditText etRuleName;
    private Spinner spinnerSim;
    private Spinner spinnerSenderType;
    private EditText etSenderValue;
    private Spinner spinnerMessageType;
    private EditText etMessageValue;

    // Action fields
    private LinearLayout actionFormLayout;
    private Spinner spinnerActionType;
    private EditText etActionDestination;
    private EditText etActionTemplate;
    private EditText etBotToken;
    private EditText etChatId;
    private LinearLayout telegramFields;

    private Button btnSave;
    private Button btnAddAction;
    private Button btnCancelAction;

    // Actions list
    private RecyclerView recyclerActions;
    private ActionsAdapter actionsAdapter;

    private DataBase db;
    private ExecutorService executorService;
    private Rule currentRule;
    private List<Action> actions = new ArrayList<>();
    private Gson gson = new Gson();

    private int editingActionPosition = -1; // -1 means adding new, >= 0 means editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_editor);

        executorService = Executors.newSingleThreadExecutor();
        db = DataBase.getDbInstance(this);

        initViews();
        setupSpinners();
        setupActionsRecyclerView();

        // Check if editing existing rule
        int ruleId = getIntent().getIntExtra("rule_id", -1);
        if (ruleId != -1) {
            loadRule(ruleId);
        }

        btnSave.setOnClickListener(v -> saveRule());
        btnAddAction.setOnClickListener(v -> {
            if (validateAndAddOrUpdateAction()) {
                clearActionForm();
            }
        });
        btnCancelAction.setOnClickListener(v -> {
            clearActionForm();
            editingActionPosition = -1;
        });

        // Listen to action type changes to show/hide fields
        spinnerActionType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateActionFieldsVisibility();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void initViews() {
        etRuleName = findViewById(R.id.etRuleName);
        spinnerSim = findViewById(R.id.spinnerSim);
        spinnerSenderType = findViewById(R.id.spinnerSenderType);
        etSenderValue = findViewById(R.id.etSenderValue);
        spinnerMessageType = findViewById(R.id.spinnerMessageType);
        etMessageValue = findViewById(R.id.etMessageValue);

        actionFormLayout = findViewById(R.id.actionFormLayout);
        spinnerActionType = findViewById(R.id.spinnerActionType);
        etActionDestination = findViewById(R.id.etActionDestination);
        etActionTemplate = findViewById(R.id.etActionTemplate);
        etBotToken = findViewById(R.id.etBotToken);
        etChatId = findViewById(R.id.etChatId);
        telegramFields = findViewById(R.id.telegramFields);

        btnSave = findViewById(R.id.btnSave);
        btnAddAction = findViewById(R.id.btnAddAction);
        btnCancelAction = findViewById(R.id.btnCancelAction);

        recyclerActions = findViewById(R.id.recyclerActions);
    }

    private void setupSpinners() {
        // SIM Filter
        ArrayAdapter<String> simAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"ANY", "SIM1", "SIM2", "BOTH"});
        simAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSim.setAdapter(simAdapter);

        // Sender Filter Type
        ArrayAdapter<String> senderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"ANY", "EQUALS", "CONTAINS", "STARTS_WITH", "REGEX"});
        senderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSenderType.setAdapter(senderAdapter);

        // Message Filter Type
        ArrayAdapter<String> messageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"ANY", "EQUALS", "CONTAINS", "REGEX"});
        messageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMessageType.setAdapter(messageAdapter);

        // Action Type
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
//                new String[]{"WEBHOOK", "SMS", "TELEGRAM", "WHATSAPP"});
                new String[]{"WEBHOOK", "SMS", "TELEGRAM"});
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActionType.setAdapter(actionAdapter);
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

    private void updateActionFieldsVisibility() {
        String selectedType = spinnerActionType.getSelectedItem().toString();

        if ("TELEGRAM".equals(selectedType)) {
            telegramFields.setVisibility(View.VISIBLE);
            etActionDestination.setHint("Not required for Telegram");
        } else if ("WEBHOOK".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            etActionDestination.setHint("e.g., https://api.example.com/webhook");
        } else if ("SMS".equals(selectedType) || "WHATSAPP".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            etActionDestination.setHint("e.g., +1234567890");
        }
    }

    private void loadRule(int ruleId) {
        executorService.execute(() -> {
            currentRule = db.ruleDao().getRuleById(ruleId);
            if (currentRule != null) {
                runOnUiThread(() -> {
                    etRuleName.setText(currentRule.name);
                    setSpinnerValue(spinnerSim, currentRule.simFilter);
                    setSpinnerValue(spinnerSenderType, currentRule.senderFilterType);
                    etSenderValue.setText(currentRule.senderFilterValue);
                    setSpinnerValue(spinnerMessageType, currentRule.messageFilterType);
                    etMessageValue.setText(currentRule.messageFilterValue);

                    // Load existing actions
                    if (currentRule.actionsJson != null && !currentRule.actionsJson.isEmpty()) {
                        try {
                            List<Action> loadedActions = gson.fromJson(
                                    currentRule.actionsJson,
                                    new TypeToken<List<Action>>(){}.getType()
                            );
                            actions.clear();
                            actions.addAll(loadedActions);
                            actionsAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            Toast.makeText(this, "Error loading actions", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void editAction(Action action, int position) {
        editingActionPosition = position;

        // Populate form with action data
        setSpinnerValue(spinnerActionType, action.type.toString());
        etActionDestination.setText(action.destination);
        etActionTemplate.setText(action.template);

        if (action.type == Action.ActionType.TELEGRAM) {
            etBotToken.setText(action.botToken);
            etChatId.setText(action.chatId);
        }

        // Change button text
        btnAddAction.setText("Update Action");
        btnCancelAction.setVisibility(View.VISIBLE);

        // Scroll to action form
        actionFormLayout.requestFocus();
    }

    private boolean validateAndAddOrUpdateAction() {
        String type = spinnerActionType.getSelectedItem().toString();
        String destination = etActionDestination.getText().toString().trim();
        String template = etActionTemplate.getText().toString().trim();

        if ("TELEGRAM".equals(type)) {
            String botToken = etBotToken.getText().toString().trim();
            String chatId = etChatId.getText().toString().trim();

            if (botToken.isEmpty() || chatId.isEmpty()) {
                Toast.makeText(this, "Please enter Bot Token and Chat ID", Toast.LENGTH_SHORT).show();
                return false;
            }

            Action action = createAction(type, chatId, template);
            action.botToken = botToken;
            action.chatId = chatId;

            addOrUpdateAction(action);
            return true;
        } else {
            if (destination.isEmpty()) {
                Toast.makeText(this, "Please enter destination", Toast.LENGTH_SHORT).show();
                return false;
            }

            Action action = createAction(type, destination, template);
            addOrUpdateAction(action);
            return true;
        }
    }

    private Action createAction(String type, String destination, String template) {
        Action action = new Action();
        action.type = Action.ActionType.valueOf(type);
        action.destination = destination;
        action.template = template.isEmpty() ? "{message}" : template;
        action.enabled = true;
        return action;
    }

    private void addOrUpdateAction(Action action) {
        if (editingActionPosition >= 0) {
            // Update existing action
            actions.set(editingActionPosition, action);
            Toast.makeText(this, "Action updated", Toast.LENGTH_SHORT).show();
        } else {
            // Add new action
            actions.add(action);
            Toast.makeText(this, "Action added: " + action.type, Toast.LENGTH_SHORT).show();
        }

        actionsAdapter.notifyDataSetChanged();
    }

    private void clearActionForm() {
        etActionDestination.setText("");
        etActionTemplate.setText("");
        etBotToken.setText("");
        etChatId.setText("");
        spinnerActionType.setSelection(0);
        btnAddAction.setText("Add Action");
        btnCancelAction.setVisibility(View.GONE);
        editingActionPosition = -1;
    }

    private void showDeleteActionDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Action")
                .setMessage("Are you sure you want to delete this action?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    actions.remove(position);
                    actionsAdapter.notifyItemRemoved(position);
                    actionsAdapter.notifyItemRangeChanged(position, actions.size());
                    Toast.makeText(this, "Action deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveRule() {
        String name = etRuleName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter rule name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (actions.isEmpty()) {
            Toast.makeText(this, "Please add at least one action", Toast.LENGTH_SHORT).show();
            return;
        }

        Rule rule = currentRule != null ? currentRule : new Rule();
        rule.name = name;
        rule.simFilter = spinnerSim.getSelectedItem().toString();
        rule.senderFilterType = spinnerSenderType.getSelectedItem().toString();
        rule.senderFilterValue = etSenderValue.getText().toString().trim();
        rule.messageFilterType = spinnerMessageType.getSelectedItem().toString();
        rule.messageFilterValue = etMessageValue.getText().toString().trim();
        rule.actionsJson = gson.toJson(actions);
        rule.updatedAt = System.currentTimeMillis();

        executorService.execute(() -> {
            if (currentRule == null) {
                db.ruleDao().insertRule(rule);
            } else {
                db.ruleDao().updateRule(rule);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Rule saved!", Toast.LENGTH_SHORT).show();
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
}