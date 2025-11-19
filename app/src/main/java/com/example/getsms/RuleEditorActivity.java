package com.example.getsms;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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
import com.example.getsms.engine.SyncExecutors;
import com.example.getsms.model.Action;
import com.example.getsms.model.Rule;
import com.example.getsms.roomDB.DataBase;
import com.example.getsms.utils.RetryHelper;
import com.example.getsms.utils.TransformHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.getsms.utils.SpinnerHelper;

public class RuleEditorActivity extends BaseActivity {

    // Rule fields
    private EditText etRuleName;
    private Spinner spinnerSim, spinnerSenderType, spinnerMessageType;
    private EditText etSenderValue, etMessageValue;

    // Action fields
    private LinearLayout actionFormLayout;
    private Spinner spinnerActionType;
    private EditText etActionDestination, etActionTemplate;
    private EditText etBotToken, etChatId;
    private LinearLayout telegramFields;

    // Transform fields
    private SwitchCompat switchEnableTransform;
    private LinearLayout transformSettings;
    private Spinner spinnerTransformType;
    private EditText etTransformPattern;
    private TextView tvTransformHelp;
    private Button btnTestTransform;

    // Retry fields
    private SwitchCompat switchEnableRetry;
    private LinearLayout retrySettings;
    private SeekBar seekBarMaxRetries, seekBarRetryDelay;
    private TextView tvMaxRetries, tvRetryDelay;
    private Spinner spinnerRetryStrategy;
    private TextView tvRetryStrategyHelp;

    // Backup fields
    private SwitchCompat switchEnableBackup;
    private LinearLayout backupSettings;
    private Spinner spinnerBackupType;
    private EditText etBackupDestination, etBackupTemplate;
    private CheckBox cbBackupAfterAllRetries, cbRetryBackup;

    // Error handling fields
    private CheckBox cbContinueOnFailure, cbNotifyOnFailure, cbDetailedErrorLog;

    // Actions list
    private RecyclerView recyclerActions;
    private ActionsAdapter actionsAdapter;
    private List<Action> actions = new ArrayList<>();

    // Buttons
    private Button btnSave, btnAddAction, btnCancelAction;

    // Data
    private DataBase db;
    private ExecutorService executorService;
    private Rule currentRule;
    private Gson gson = new Gson();
    private int editingActionPosition = -1;
    // ADD THIS LINE
    private androidx.cardview.widget.CardView webhookHelpCard;


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

        // Action fields
        actionFormLayout = findViewById(R.id.actionFormLayout);
        spinnerActionType = findViewById(R.id.spinnerActionType);
        etActionDestination = findViewById(R.id.etActionDestination);
        etActionTemplate = findViewById(R.id.etActionTemplate);
        etBotToken = findViewById(R.id.etBotToken);
        etChatId = findViewById(R.id.etChatId);
        telegramFields = findViewById(R.id.telegramFields);

        // ADD THIS LINE
        webhookHelpCard = findViewById(R.id.webhookHelpCard);

        // Transform views
        switchEnableTransform = findViewById(R.id.switchEnableTransform);
        transformSettings = findViewById(R.id.transformSettings);
        spinnerTransformType = findViewById(R.id.spinnerTransformType);
        etTransformPattern = findViewById(R.id.etTransformPattern);
        tvTransformHelp = findViewById(R.id.tvTransformHelp);
        btnTestTransform = findViewById(R.id.btnTestTransform);

        // Retry views
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

        // Buttons and RecyclerView
        btnSave = findViewById(R.id.btnSave);
        btnAddAction = findViewById(R.id.btnAddAction);
        btnCancelAction = findViewById(R.id.btnCancelAction);
        recyclerActions = findViewById(R.id.recyclerActions);
    }

    private void setupListeners() {
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
        btnTestTransform.setOnClickListener(v -> showTransformTestDialog());

        switchEnableTransform.setOnCheckedChangeListener((buttonView, isChecked) ->
                transformSettings.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        switchEnableRetry.setOnCheckedChangeListener((buttonView, isChecked) ->
                retrySettings.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        switchEnableBackup.setOnCheckedChangeListener((buttonView, isChecked) ->
                backupSettings.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        spinnerActionType.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                updateActionFieldsVisibility();
            }
        });

        spinnerTransformType.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                updateTransformHelp();
            }
        });

        spinnerRetryStrategy.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                updateRetryStrategyHelp();
            }
        });

        setupSeekBars();
        etBackupTemplate.setHint(R.string.primary_action_failed);
    }

    private void setupSeekBars() {
        seekBarMaxRetries.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(int progress) {
                int retries = Math.max(1, progress);
                tvMaxRetries.setText(String.valueOf(retries));
            }
        });

        seekBarRetryDelay.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(int progress) {
                int delay = Math.max(1, progress);
                tvRetryDelay.setText(delay + "s");
            }
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
                new SpinnerItem(getString(R.string.regex), "REGEX")
        });

        SpinnerHelper.setup(this, spinnerActionType, new SpinnerItem[]{
                new SpinnerItem(getString(R.string.webhook), "WEBHOOK"),
                new SpinnerItem(getString(R.string.sms), "SMS"),
                new SpinnerItem(getString(R.string.telegram), "TELEGRAM")
        });

        SpinnerHelper.setup(this, spinnerTransformType, new SpinnerItem[]{
                new SpinnerItem(getString(R.string.extract_lines), "EXTRACT_LINES"),
                new SpinnerItem(getString(R.string.remove_lines), "REMOVE_LINES"),
                new SpinnerItem(getString(R.string.extract_pattern), "EXTRACT_PATTERN"),
                new SpinnerItem(getString(R.string.remove_pattern), "REMOVE_PATTERN"),
                new SpinnerItem(getString(R.string.replace_pattern), "REPLACE_PATTERN"),
                new SpinnerItem(getString(R.string.keep_until), "KEEP_UNTIL"),
                new SpinnerItem(getString(R.string.keep_after), "KEEP_AFTER"),
                new SpinnerItem(getString(R.string.remove_after), "REMOVE_AFTER")
        });

        SpinnerHelper.setup(this, spinnerRetryStrategy, new SpinnerItem[]{
                new SpinnerItem(getString(R.string.fixed_delay), "FIXED_DELAY"),
                new SpinnerItem(getString(R.string.exponential_backoff), "EXPONENTIAL_BACKOFF"),
                new SpinnerItem(getString(R.string.immediate), "IMMEDIATE")
        });

        SpinnerHelper.setup(this, spinnerBackupType, new SpinnerItem[]{
                new SpinnerItem(getString(R.string.sms), "SMS")
        });
    }

    private void updateTelegramFieldsVisibility() {
        String selectedType = SpinnerHelper.getValue(spinnerActionType);

        if ("TELEGRAM".equals(selectedType)) {
            telegramFields.setVisibility(View.VISIBLE);
            etActionDestination.setHint(R.string.not_required_for_telegram);
        } else if ("WEBHOOK".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            etActionDestination.setHint("e.g., https://api.example.com/webhook");
        } else if ("SMS".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            etActionDestination.setHint("e.g., +1234567890");
        }
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
        editingActionPosition = position;
        populateActionForm(action);
        btnAddAction.setText(R.string.update_action);
        btnCancelAction.setVisibility(View.VISIBLE);
        actionFormLayout.requestFocus();
    }

    private void populateActionForm(Action action) {
        SpinnerHelper.setValue(spinnerActionType, action.type.toString());
        etActionDestination.setText(action.destination);
        etActionTemplate.setText(action.template);

        if (action.type == Action.ActionType.TELEGRAM) {
            etBotToken.setText(action.botToken);
            etChatId.setText(action.chatId);
        }

        populateTransformSettings(action);
        populateRetrySettings(action);
        populateBackupSettings(action);
        populateErrorHandling(action);
    }

    private void populateTransformSettings(Action action) {
        switchEnableTransform.setChecked(action.enableTransform);
        if (action.enableTransform) {
            SpinnerHelper.setValue(spinnerTransformType, action.transformType);
            etTransformPattern.setText(action.transformPattern);
        }
    }

    private void populateRetrySettings(Action action) {
        switchEnableRetry.setChecked(action.enableRetry);
        if (action.enableRetry) {
            seekBarMaxRetries.setProgress(action.maxRetries);
            seekBarRetryDelay.setProgress(action.retryDelaySeconds);
            SpinnerHelper.setValue(spinnerRetryStrategy, action.retryStrategy);
        }
    }

    private void populateBackupSettings(Action action) {
        switchEnableBackup.setChecked(action.enableBackup);
        if (action.enableBackup) {
            SpinnerHelper.setValue(spinnerBackupType, action.backupType.toString());
            etBackupDestination.setText(action.backupDestination);
            etBackupTemplate.setText(action.backupTemplate);
            cbBackupAfterAllRetries.setChecked(action.backupAfterAllRetries);
            cbRetryBackup.setChecked(action.retryBackup);
        }
    }

    private void populateErrorHandling(Action action) {
        cbContinueOnFailure.setChecked(action.continueOnFailure);
        cbNotifyOnFailure.setChecked(action.notifyOnFailure);
        cbDetailedErrorLog.setChecked(action.detailedErrorLog);
    }

    private boolean validateAndAddOrUpdateAction() {
        String type = SpinnerHelper.getValue(spinnerActionType);
        String destination = etActionDestination.getText().toString().trim();
        String template = etActionTemplate.getText().toString().trim();

        if (!validateBackupIfEnabled()) return false;

        Action action;
        if ("TELEGRAM".equals(type)) {
            action = createTelegramAction(destination, template);
            if (action == null) return false;
        } else {
            if (destination.isEmpty()) {
                Toast.makeText(this, R.string.enter_destination, Toast.LENGTH_SHORT).show();
                return false;
            }
            action = createAction(type, destination, template);
        }

        applyAllSettings(action);
        addOrUpdateAction(action);
        return true;
    }

    private boolean validateBackupIfEnabled() {
        if (!switchEnableBackup.isChecked()) return true;

        String backupDest = etBackupDestination.getText().toString().trim();
        String backupType = SpinnerHelper.getValue(spinnerBackupType);

        if ("SMS".equals(backupType) && backupDest.isEmpty()) {
            Toast.makeText(this, R.string.enter_backup_phone_number, Toast.LENGTH_SHORT).show();
            return false;
        }

        if ("TELEGRAM".equals(backupType) && backupDest.isEmpty()) {
            Toast.makeText(this, R.string.enter_backup_telegram_chat_id, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private Action createTelegramAction(String chatId, String template) {
        String botToken = etBotToken.getText().toString().trim();
        String chat = etChatId.getText().toString().trim();

        // Chat ID is required, but bot token is optional (will use default)
        if (chat.isEmpty()) {
            Toast.makeText(this, "Chat ID is required. Send /getchatid to bot to get it.", Toast.LENGTH_LONG).show();
            return null;
        }

        Action action = createAction("TELEGRAM", chat, template);

        // Set bot token only if provided (empty means use default bot)
        if (!botToken.isEmpty()) {
            action.botToken = botToken;
        }

        action.chatId = chat;
        return action;
    }

    private Action createAction(String type, String destination, String template) {
        Action action = new Action();
        action.type = Action.ActionType.valueOf(type);
        action.destination = destination;
        action.template = template.isEmpty() ? "{message}" : template;
        action.enabled = true;
        return action;
    }

    private void applyAllSettings(Action action) {
        applyTransformSettings(action);
        applyRetrySettings(action);
        applyBackupSettings(action);
        applyErrorHandlingSettings(action);
    }

    private void applyTransformSettings(Action action) {
        action.enableTransform = switchEnableTransform.isChecked();
        if (action.enableTransform) {
            action.transformType = SpinnerHelper.getValue(spinnerTransformType);
            action.transformPattern = etTransformPattern.getText().toString().trim();
        }
    }

    private void applyRetrySettings(Action action) {
        action.enableRetry = switchEnableRetry.isChecked();
        if (action.enableRetry) {
            action.maxRetries = Math.max(1, seekBarMaxRetries.getProgress());
            action.retryDelaySeconds = Math.max(1, seekBarRetryDelay.getProgress());
            action.retryStrategy = SpinnerHelper.getValue(spinnerRetryStrategy);
        }
    }

    private void applyBackupSettings(Action action) {
        action.enableBackup = switchEnableBackup.isChecked();
        if (action.enableBackup) {
            action.backupType = Action.ActionType.valueOf(SpinnerHelper.getValue(spinnerBackupType));
            action.backupDestination = etBackupDestination.getText().toString().trim();
            action.backupTemplate = etBackupTemplate.getText().toString().trim();
            action.backupAfterAllRetries = cbBackupAfterAllRetries.isChecked();
            action.retryBackup = cbRetryBackup.isChecked();
        }
    }

    private void applyErrorHandlingSettings(Action action) {
        action.continueOnFailure = cbContinueOnFailure.isChecked();
        action.notifyOnFailure = cbNotifyOnFailure.isChecked();
        action.detailedErrorLog = cbDetailedErrorLog.isChecked();
    }

    private void addOrUpdateAction(Action action) {
        if (editingActionPosition >= 0) {
            actions.set(editingActionPosition, action);
            Toast.makeText(this, R.string.action_updated, Toast.LENGTH_SHORT).show();
        } else {
            actions.add(action);
            Toast.makeText(this, getString(R.string.action_added, action.type), Toast.LENGTH_SHORT).show();
        }
        actionsAdapter.notifyDataSetChanged();
    }

    private void clearActionForm() {
        etActionDestination.setText("");
        etActionTemplate.setText("");
        etBotToken.setText("");
        etChatId.setText("");
        switchEnableTransform.setChecked(false);
        switchEnableRetry.setChecked(false);
        switchEnableBackup.setChecked(false);
        etTransformPattern.setText("");
        spinnerActionType.setSelection(0);
        spinnerTransformType.setSelection(0);
        btnAddAction.setText(R.string.add_action);
        btnCancelAction.setVisibility(View.GONE);
        editingActionPosition = -1;
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
    }

    private void updateActionFieldsVisibility() {
        String selectedType = SpinnerHelper.getValue(spinnerActionType);

        if ("TELEGRAM".equals(selectedType)) {
            telegramFields.setVisibility(View.VISIBLE);
            webhookHelpCard.setVisibility(View.GONE);
            etActionDestination.setHint(R.string.not_required_for_telegram);

        } else if ("WEBHOOK".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            webhookHelpCard.setVisibility(View.VISIBLE);
            etActionDestination.setHint("e.g., https://api.example.com/webhook");

        } else if ("SMS".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            webhookHelpCard.setVisibility(View.GONE);
            etActionDestination.setHint("e.g., +1234567890");
        }
    }

    private void updateTransformHelp() {
        String type = SpinnerHelper.getValue(spinnerTransformType);
        tvTransformHelp.setText(TransformHelper.getHelpText(this, type));
    }

    private void updateRetryStrategyHelp() {
        String strategy = SpinnerHelper.getValue(spinnerRetryStrategy);
        tvRetryStrategyHelp.setText(RetryHelper.getStrategyHelp(this, strategy));
    }

    private void showTransformTestDialog() {
        LinearLayout layout = createTestDialogLayout();
        final EditText input = (EditText) layout.getChildAt(1);

        new AlertDialog.Builder(this)
                .setTitle(R.string.test_transformation)
                .setView(layout)
                .setPositiveButton(R.string.test, (dialog, which) -> testTransformation(input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private LinearLayout createTestDialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView label = new TextView(this);
        label.setText(R.string.enter_test_message);
        layout.addView(label);

        EditText input = new EditText(this);
        input.setHint(R.string.test_message_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(5);
        layout.addView(input);

        return layout;
    }

    private void testTransformation(String testMessage) {
        if (testMessage.isEmpty()) {
            Toast.makeText(this, R.string.enter_test_message, Toast.LENGTH_SHORT).show();
            return;
        }

        String type = SpinnerHelper.getValue(spinnerTransformType);
        String pattern = etTransformPattern.getText().toString();

        MessageTransformer.TransformType transformType = MessageTransformer.TransformType.valueOf(type);
        String result = MessageTransformer.transform(testMessage, transformType, pattern);

        showTransformResult(testMessage, result);
    }

    private void showTransformResult(String original, String transformed) {
        LinearLayout layout = createResultLayout(original, transformed);

        new AlertDialog.Builder(this)
                .setTitle(R.string.transformation_result)
                .setView(layout)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private LinearLayout createResultLayout(String original, String transformed) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        addLabeledText(layout, getString(R.string.original), original, false);
        addLabeledText(layout, getString(R.string.transformed), transformed, true);

        return layout;
    }

    private void addLabeledText(LinearLayout layout, String label, String text, boolean highlight) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(labelView);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(0, 10, 0, 20);
        if (highlight) {
            textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        layout.addView(textView);
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
                db.ruleDao().insertRule(rule);
            } else {
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