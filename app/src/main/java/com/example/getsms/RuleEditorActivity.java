package com.example.getsms;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.adapter.ActionsAdapter;
import com.example.getsms.engine.MessageTransformer;
import com.example.getsms.model.Action;
import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class RuleEditorActivity extends BaseActivity {

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

    // Transform fields
    private SwitchCompat switchEnableTransform;
    private LinearLayout transformSettings;
    private Spinner spinnerTransformType;
    private EditText etTransformPattern;
    private TextView tvTransformHelp;
    private Button btnTestTransform;

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

    private int editingActionPosition = -1;

    // Add new views to class fields
    private SwitchCompat switchEnableRetry;
    private LinearLayout retrySettings;
    private SeekBar seekBarMaxRetries;
    private TextView tvMaxRetries;
    private SeekBar seekBarRetryDelay;
    private TextView tvRetryDelay;
    private Spinner spinnerRetryStrategy;
    private TextView tvRetryStrategyHelp;

    private SwitchCompat switchEnableBackup;
    private LinearLayout backupSettings;
    private Spinner spinnerBackupType;
    private EditText etBackupDestination;
    private EditText etBackupTemplate;
    private CheckBox cbBackupAfterAllRetries;
    private CheckBox cbRetryBackup;

    private CheckBox cbContinueOnFailure;
    private CheckBox cbNotifyOnFailure;
    private CheckBox cbDetailedErrorLog;

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

        // Transform switch listener
        switchEnableTransform.setOnCheckedChangeListener((buttonView, isChecked) -> {
            transformSettings.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Action type listener
        spinnerActionType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateActionFieldsVisibility();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Transform type listener - update help text
        spinnerTransformType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateTransformHelp();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Test transform button
        btnTestTransform.setOnClickListener(v -> showTransformTestDialog());

        // Setup retry listeners
        setupRetryListeners();

        // Setup backup listeners
        setupBackupListeners();
    }

    private void setupRetryListeners() {
        // Toggle retry settings visibility
        switchEnableRetry.setOnCheckedChangeListener((buttonView, isChecked) -> {
            retrySettings.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Max retries seekbar
        seekBarMaxRetries.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int retries = Math.max(1, progress);
                tvMaxRetries.setText(String.valueOf(retries));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Retry delay seekbar
        seekBarRetryDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int delay = Math.max(1, progress);
                tvRetryDelay.setText(delay + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Retry strategy listener
        spinnerRetryStrategy.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateRetryStrategyHelp();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupBackupListeners() {
        // Toggle backup settings visibility
        switchEnableBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            backupSettings.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Set default backup template
        etBackupTemplate.setHint("⚠️ Primary action failed. Original: {message}");
    }

    private void updateRetryStrategyHelp() {
        String strategy = spinnerRetryStrategy.getSelectedItem().toString();
        String helpText;

        switch (strategy) {
            case "IMMEDIATE":
                helpText = "Retry immediately without delay.\n\n" +
                        "Example: 3 retries = instant, instant, instant\n\n" +
                        "Use for: Quick operations that might fail due to temporary issues";
                break;

            case "EXPONENTIAL_BACKOFF":
                helpText = "Double the wait time between each retry.\n\n" +
                        "Example: 5s → 10s → 20s → 40s\n\n" +
                        "Use for: Network requests, API calls (recommended)";
                break;

            case "FIXED_DELAY":
            default:
                helpText = "Wait the same amount of time between each retry.\n\n" +
                        "Example: 5s → 5s → 5s\n\n" +
                        "Use for: Simple operations with predictable recovery time";
                break;
        }

        tvRetryStrategyHelp.setText(helpText);
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

        // Transform views
        switchEnableTransform = findViewById(R.id.switchEnableTransform);
        transformSettings = findViewById(R.id.transformSettings);
        spinnerTransformType = findViewById(R.id.spinnerTransformType);
        etTransformPattern = findViewById(R.id.etTransformPattern);
        tvTransformHelp = findViewById(R.id.tvTransformHelp);
        btnTestTransform = findViewById(R.id.btnTestTransform);

        btnSave = findViewById(R.id.btnSave);
        btnAddAction = findViewById(R.id.btnAddAction);
        btnCancelAction = findViewById(R.id.btnCancelAction);

        recyclerActions = findViewById(R.id.recyclerActions);

        switchEnableRetry = findViewById(R.id.switchEnableRetry);
        retrySettings = findViewById(R.id.retrySettings);
        seekBarMaxRetries = findViewById(R.id.seekBarMaxRetries);
        tvMaxRetries = findViewById(R.id.tvMaxRetries);
        seekBarRetryDelay = findViewById(R.id.seekBarRetryDelay);
        tvRetryDelay = findViewById(R.id.tvRetryDelay);
        spinnerRetryStrategy = findViewById(R.id.spinnerRetryStrategy);
        tvRetryStrategyHelp = findViewById(R.id.tvRetryStrategyHelp);

        // Backup views
        switchEnableBackup = findViewById(R.id.switchEnableBackup);
        backupSettings = findViewById(R.id.backupSettings);
        spinnerBackupType = findViewById(R.id.spinnerBackupType);
        etBackupDestination = findViewById(R.id.etBackupDestination);
        etBackupTemplate = findViewById(R.id.etBackupTemplate);
        cbBackupAfterAllRetries = findViewById(R.id.cbBackupAfterAllRetries);
        cbRetryBackup = findViewById(R.id.cbRetryBackup);

        // Error handling views
        cbContinueOnFailure = findViewById(R.id.cbContinueOnFailure);
        cbNotifyOnFailure = findViewById(R.id.cbNotifyOnFailure);
        cbDetailedErrorLog = findViewById(R.id.cbDetailedErrorLog);
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
                new String[]{"WEBHOOK", "SMS", "TELEGRAM"});
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActionType.setAdapter(actionAdapter);

        // Transform Type
        ArrayAdapter<String> transformAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"EXTRACT_LINES", "REMOVE_LINES", "EXTRACT_PATTERN",
                        "REMOVE_PATTERN", "REPLACE_PATTERN", "KEEP_UNTIL",
                        "KEEP_AFTER", "REMOVE_AFTER"});
        transformAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransformType.setAdapter(transformAdapter);

        ArrayAdapter<String> retryStrategyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"FIXED_DELAY", "EXPONENTIAL_BACKOFF", "IMMEDIATE"});
        retryStrategyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRetryStrategy.setAdapter(retryStrategyAdapter);

        // Backup Type Spinner
        ArrayAdapter<String> backupTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"SMS", "TELEGRAM", "WEBHOOK"});
        backupTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBackupType.setAdapter(backupTypeAdapter);
    }

    private void updateTransformHelp() {
        String type = spinnerTransformType.getSelectedItem().toString();
        String helpText = getTransformHelpText(type);
        tvTransformHelp.setText(helpText);
    }

    private String getTransformHelpText(String type) {
        switch (type) {
            case "EXTRACT_LINES":
                return "Extract specific lines.\nExample: 1,2,3 (extracts lines 1, 2, and 3)\n\nYour example:\nReceive: Line1\\nLine2\\nLine3\\nLine4\nPattern: 1,2\nResult: Line1\\nLine2";

            case "REMOVE_LINES":
                return "Remove specific lines.\nExample: 3,4 (removes lines 3 and 4)\n\nYour example:\nReceive: Line1\\nLine2\\nموجودی: 757\\nLine4\nPattern: 3\nResult: Line1\\nLine2\\nLine4";

            case "EXTRACT_PATTERN":
                return "Extract text matching regex.\nExample: حساب\\d+ (extracts account number)\n\nYour example:\nReceive: حساب5694541931\\nبرداشت100,000\nPattern: حساب\\d+|برداشت[\\d,]+\nResult: حساب5694541931\\nبرداشت100,000";

            case "REMOVE_PATTERN":
                return "Remove text matching regex.\nExample: موجودی:.* (removes balance line)\n\nYour example:\nReceive: بلو\\nانتقال پل\\nموجودی: 757 ریال\nPattern: موجودی:.*\nResult: بلو\\nانتقال پل";

            case "REPLACE_PATTERN":
                return "Replace text matching regex.\nFormat: pattern|replacement\nExample: موجودی:.*| (replaces balance with nothing)";

            case "KEEP_UNTIL":
                return "Keep text until pattern found.\nExample: موجودی (keeps everything before موجودی)";

            case "KEEP_AFTER":
                return "Keep text after pattern found.\nExample: حساب (keeps from حساب onwards)";

            case "REMOVE_AFTER":
                return "Remove text after pattern found.\nExample: موجودی (removes from موجودی onwards)\n\nYour example:\nReceive: بلو\\nانتقال پل\\nموجودی: 757\nPattern: موجودی\nResult: بلو\\nانتقال پل";

            default:
                return "Select a transform type to see examples.";
        }
    }

    private void showTransformTestDialog() {
        // Create dialog with input for test message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test Transformation");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView label = new TextView(this);
        label.setText("Enter test message:");
        layout.addView(label);

        final EditText input = new EditText(this);
        input.setHint("Example:\nحساب5694541931\nبرداشت100,000\nمانده7,865,949");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(5);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Test", (dialog, which) -> {
            String testMessage = input.getText().toString();
            if (testMessage.isEmpty()) {
                Toast.makeText(this, "Please enter test message", Toast.LENGTH_SHORT).show();
                return;
            }

            // Apply transformation
            String type = spinnerTransformType.getSelectedItem().toString();
            String pattern = etTransformPattern.getText().toString();

            MessageTransformer.TransformType transformType = MessageTransformer.TransformType.valueOf(type);
            String result = MessageTransformer.transform(testMessage, transformType, pattern);

            // Show result
            showTransformResult(testMessage, result);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showTransformResult(String original, String transformed) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Transformation Result");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView originalLabel = new TextView(this);
        originalLabel.setText("Original:");
        originalLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(originalLabel);

        TextView originalText = new TextView(this);
        originalText.setText(original);
        originalText.setPadding(0, 10, 0, 20);
        layout.addView(originalText);

        TextView transformedLabel = new TextView(this);
        transformedLabel.setText("Transformed:");
        transformedLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(transformedLabel);

        TextView transformedText = new TextView(this);
        transformedText.setText(transformed);
        transformedText.setPadding(0, 10, 0, 20);
        transformedText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        layout.addView(transformedText);

        builder.setView(layout);
        builder.setPositiveButton("OK", null);
        builder.show();
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
        } else if ("SMS".equals(selectedType)) {
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

        // Populate transform settings
        switchEnableTransform.setChecked(action.enableTransform);
        if (action.enableTransform) {
            setSpinnerValue(spinnerTransformType, action.transformType);
            etTransformPattern.setText(action.transformPattern);
        }

        // Change button text
        btnAddAction.setText("Update Action");
        btnCancelAction.setVisibility(View.VISIBLE);

        // Scroll to action form
        actionFormLayout.requestFocus();

        // Load retry settings
        switchEnableRetry.setChecked(action.enableRetry);
        if (action.enableRetry) {
            seekBarMaxRetries.setProgress(action.maxRetries);
            seekBarRetryDelay.setProgress(action.retryDelaySeconds);
            setSpinnerValue(spinnerRetryStrategy, action.retryStrategy);
        }

        // Load backup settings
        switchEnableBackup.setChecked(action.enableBackup);
        if (action.enableBackup) {
            setSpinnerValue(spinnerBackupType, action.backupType.toString());
            etBackupDestination.setText(action.backupDestination);
            etBackupTemplate.setText(action.backupTemplate);
            cbBackupAfterAllRetries.setChecked(action.backupAfterAllRetries);
            cbRetryBackup.setChecked(action.retryBackup);
        }

        // Load error handling
        cbContinueOnFailure.setChecked(action.continueOnFailure);
        cbNotifyOnFailure.setChecked(action.notifyOnFailure);
        cbDetailedErrorLog.setChecked(action.detailedErrorLog);
    }

    private boolean validateAndAddOrUpdateAction() {
        String type = spinnerActionType.getSelectedItem().toString();
        String destination = etActionDestination.getText().toString().trim();
        String template = etActionTemplate.getText().toString().trim();

        // Validate backup if enabled
        if (switchEnableBackup.isChecked()) {
            String backupDest = etBackupDestination.getText().toString().trim();
            String backupType = spinnerBackupType.getSelectedItem().toString();

            if ("SMS".equals(backupType) && backupDest.isEmpty()) {
                Toast.makeText(this, "Please enter backup phone number", Toast.LENGTH_SHORT).show();
                return false;
            }

            if ("TELEGRAM".equals(backupType) && backupDest.isEmpty()) {
                Toast.makeText(this, "Please enter backup Telegram chat ID", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

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
            applyTransformSettings(action);

            addOrUpdateAction(action);
            return true;
        } else {
            if (destination.isEmpty()) {
                Toast.makeText(this, "Please enter destination", Toast.LENGTH_SHORT).show();
                return false;
            }

            Action action = createAction(type, destination, template);
            applyTransformSettings(action);

            addOrUpdateAction(action);
            return true;
        }



    }

    private void applyTransformSettings(Action action) {
        action.enableTransform = switchEnableTransform.isChecked();
        if (action.enableTransform) {
            action.transformType = spinnerTransformType.getSelectedItem().toString();
            action.transformPattern = etTransformPattern.getText().toString().trim();
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
            actions.set(editingActionPosition, action);
            Toast.makeText(this, "Action updated", Toast.LENGTH_SHORT).show();
        } else {
            actions.add(action);
            Toast.makeText(this, "Action added: " + action.type, Toast.LENGTH_SHORT).show();
        }

        action.enableRetry = switchEnableRetry.isChecked();
        if (action.enableRetry) {
            action.maxRetries = Math.max(1, seekBarMaxRetries.getProgress());
            action.retryDelaySeconds = Math.max(1, seekBarRetryDelay.getProgress());
            action.retryStrategy = spinnerRetryStrategy.getSelectedItem().toString();
        }

        // Backup settings
        action.enableBackup = switchEnableBackup.isChecked();
        if (action.enableBackup) {
            action.backupType = Action.ActionType.valueOf(
                    spinnerBackupType.getSelectedItem().toString()
            );
            action.backupDestination = etBackupDestination.getText().toString().trim();
            action.backupTemplate = etBackupTemplate.getText().toString().trim();
            action.backupAfterAllRetries = cbBackupAfterAllRetries.isChecked();
            action.retryBackup = cbRetryBackup.isChecked();
        }

        // Error handling
        action.continueOnFailure = cbContinueOnFailure.isChecked();
        action.notifyOnFailure = cbNotifyOnFailure.isChecked();
        action.detailedErrorLog = cbDetailedErrorLog.isChecked();

        actionsAdapter.notifyDataSetChanged();
    }

    private void clearActionForm() {
        etActionDestination.setText("");
        etActionTemplate.setText("");
        etBotToken.setText("");
        etChatId.setText("");
        switchEnableTransform.setChecked(false);
        etTransformPattern.setText("");
        spinnerActionType.setSelection(0);
        spinnerTransformType.setSelection(0);
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