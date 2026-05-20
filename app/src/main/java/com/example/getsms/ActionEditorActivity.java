package com.example.getsms;

import android.content.Intent;
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
import androidx.cardview.widget.CardView;

import com.example.getsms.engine.MessageTransformer;
import com.example.getsms.model.Action;
import com.example.getsms.utils.RetryHelper;
import com.example.getsms.utils.SpinnerHelper;
import com.example.getsms.utils.TransformHelper;
import com.google.gson.Gson;

public class ActionEditorActivity extends BaseActivity {

    // Basic fields
    private Spinner spinnerActionType;
    private EditText etActionDestination, etActionTemplate;
    private EditText etBotToken, etChatId;
    private LinearLayout telegramFields;
    private CardView webhookHelpCard;

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

    // Buttons
    private Button btnSaveAction, btnCancel;

    // Data
    private Action editingAction;
    private int actionPosition = -1;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_editor);

        initViews();
        setupSpinners();
        setupListeners();
        loadActionIfEditing();
    }

    private void initViews() {
        // Basic fields
        spinnerActionType = findViewById(R.id.spinnerActionType);
        etActionDestination = findViewById(R.id.etActionDestination);
        etActionTemplate = findViewById(R.id.etActionTemplate);
        etBotToken = findViewById(R.id.etBotToken);
        etChatId = findViewById(R.id.etChatId);
        telegramFields = findViewById(R.id.telegramFields);
        webhookHelpCard = findViewById(R.id.webhookHelpCard);

        // Transform fields
        switchEnableTransform = findViewById(R.id.switchEnableTransform);
        transformSettings = findViewById(R.id.transformSettings);
        spinnerTransformType = findViewById(R.id.spinnerTransformType);
        etTransformPattern = findViewById(R.id.etTransformPattern);
        tvTransformHelp = findViewById(R.id.tvTransformHelp);
        btnTestTransform = findViewById(R.id.btnTestTransform);

        // Retry fields
        switchEnableRetry = findViewById(R.id.switchEnableRetry);
        retrySettings = findViewById(R.id.retrySettings);
        seekBarMaxRetries = findViewById(R.id.seekBarMaxRetries);
        tvMaxRetries = findViewById(R.id.tvMaxRetries);
        seekBarRetryDelay = findViewById(R.id.seekBarRetryDelay);
        tvRetryDelay = findViewById(R.id.tvRetryDelay);
        spinnerRetryStrategy = findViewById(R.id.spinnerRetryStrategy);
        tvRetryStrategyHelp = findViewById(R.id.tvRetryStrategyHelp);

        // Backup fields
        switchEnableBackup = findViewById(R.id.switchEnableBackup);
        backupSettings = findViewById(R.id.backupSettings);
        spinnerBackupType = findViewById(R.id.spinnerBackupType);
        etBackupDestination = findViewById(R.id.etBackupDestination);
        etBackupTemplate = findViewById(R.id.etBackupTemplate);
        cbBackupAfterAllRetries = findViewById(R.id.cbBackupAfterAllRetries);
        cbRetryBackup = findViewById(R.id.cbRetryBackup);

        // Error handling fields
        cbContinueOnFailure = findViewById(R.id.cbContinueOnFailure);
        cbNotifyOnFailure = findViewById(R.id.cbNotifyOnFailure);
        cbDetailedErrorLog = findViewById(R.id.cbDetailedErrorLog);

        // Buttons
        btnSaveAction = findViewById(R.id.btnSaveAction);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupSpinners() {
        SpinnerHelper.setup(this, spinnerActionType, new RuleEditorActivity.SpinnerItem[]{
                new RuleEditorActivity.SpinnerItem(getString(R.string.webhook), "WEBHOOK"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.sms), "SMS"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.telegram), "TELEGRAM")
        });

        SpinnerHelper.setup(this, spinnerTransformType, new RuleEditorActivity.SpinnerItem[]{
                new RuleEditorActivity.SpinnerItem(getString(R.string.extract_lines), "EXTRACT_LINES"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.remove_lines), "REMOVE_LINES"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.extract_pattern), "EXTRACT_PATTERN"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.remove_pattern), "REMOVE_PATTERN"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.replace_pattern), "REPLACE_PATTERN"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.keep_until), "KEEP_UNTIL"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.keep_after), "KEEP_AFTER"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.remove_after), "REMOVE_AFTER")
        });

        SpinnerHelper.setup(this, spinnerRetryStrategy, new RuleEditorActivity.SpinnerItem[]{
                new RuleEditorActivity.SpinnerItem(getString(R.string.fixed_delay), "FIXED_DELAY"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.exponential_backoff), "EXPONENTIAL_BACKOFF"),
                new RuleEditorActivity.SpinnerItem(getString(R.string.immediate), "IMMEDIATE")
        });

        SpinnerHelper.setup(this, spinnerBackupType, new RuleEditorActivity.SpinnerItem[]{
                new RuleEditorActivity.SpinnerItem(getString(R.string.sms), "SMS")
        });
    }

    private void setupListeners() {
        btnSaveAction.setOnClickListener(v -> saveAction());
        btnCancel.setOnClickListener(v -> finish());
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

    private void loadActionIfEditing() {
        String actionJson = getIntent().getStringExtra("action_json");
        actionPosition = getIntent().getIntExtra("action_position", -1);

        if (actionJson != null && !actionJson.isEmpty()) {
            try {
                editingAction = gson.fromJson(actionJson, Action.class);
                populateActionForm(editingAction);
            } catch (Exception e) {
                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
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

    private void updateActionFieldsVisibility() {
        String selectedType = SpinnerHelper.getValue(spinnerActionType);

        if ("TELEGRAM".equals(selectedType)) {
            telegramFields.setVisibility(View.VISIBLE);
            webhookHelpCard.setVisibility(View.GONE);
            etActionDestination.setHint(R.string.not_required_for_telegram);

        } else if ("WEBHOOK".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            webhookHelpCard.setVisibility(View.VISIBLE);
            etActionDestination.setHint(R.string.webhook_url_hint);

        } else if ("SMS".equals(selectedType)) {
            telegramFields.setVisibility(View.GONE);
            webhookHelpCard.setVisibility(View.GONE);
            etActionDestination.setHint(R.string.sms_phone_hint);
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
            textView.setTextColor(getResources().getColor(R.color.claude_success));
        }
        layout.addView(textView);
    }

    private void saveAction() {
        String type = SpinnerHelper.getValue(spinnerActionType);
        String destination = etActionDestination.getText().toString().trim();
        String template = etActionTemplate.getText().toString().trim();

        if (!validateBackupIfEnabled()) return;

        Action action;
        if ("TELEGRAM".equals(type)) {
            action = createTelegramAction(destination, template);
            if (action == null) return;
        } else {
            if (destination.isEmpty()) {
                Toast.makeText(this, R.string.enter_destination, Toast.LENGTH_SHORT).show();
                return;
            }
            action = createAction(type, destination, template);
        }

        applyAllSettings(action);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("action_json", gson.toJson(action));
        resultIntent.putExtra("action_position", actionPosition);
        setResult(RESULT_OK, resultIntent);
        finish();
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

        if (chat.isEmpty()) {
            Toast.makeText(this, R.string.telegram_chat_id, Toast.LENGTH_LONG).show();
            return null;
        }

        Action action = createAction("TELEGRAM", chat, template);

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

    // Helper Classes
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