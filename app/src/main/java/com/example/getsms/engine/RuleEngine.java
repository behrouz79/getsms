package com.example.getsms.engine;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.getsms.credit.CreditManager;
import com.example.getsms.model.Action;
import com.example.getsms.model.Rule;
import com.example.getsms.model.SmsLog;
import com.example.getsms.model.SmsMessage;
import com.example.getsms.roomDB.DataBase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RuleEngine {

    private static final String TAG = "RuleEngine";
    private final Context context;
    private final Gson gson;
    private final CreditManager creditManager;
    private final DataBase db;

    public RuleEngine(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.creditManager = new CreditManager(context);
        this.db = DataBase.getDbInstance(context);
    }

    /**
     * Process incoming SMS through all rules with detailed logging
     */
    public void processSms(SmsMessage sms, List<Rule> rules) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "📱 NEW SMS RECEIVED");
        Log.d(TAG, "========================================");
        Log.d(TAG, "Sender: " + sms.getSender());
        Log.d(TAG, "SIM Slot: " + sms.getSimSlot());
        Log.d(TAG, "Message: " + sms.getBody());
        Log.d(TAG, "Time: " + sms.getFormattedDate());
        Log.d(TAG, "========================================");

        // Create log entry
        SmsLog smsLog = SmsLog.fromSmsMessage(sms);

        // Sort rules by priority (lower number = higher priority)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            rules.sort((r1, r2) -> Integer.compare(r1.priority, r2.priority));
        }

        boolean ruleMatched = false;
        int actionsExecuted = 0;
        List<String> executedActions = new ArrayList<>();

        for (Rule rule : rules) {
            if (!rule.enabled) {
                Log.d(TAG, "⏭️ Skipping disabled rule: " + rule.name);
                continue;
            }

            Log.d(TAG, "🔍 Checking rule: " + rule.name);

            if (matchesRule(sms, rule)) {
                Log.d(TAG, "✅ RULE MATCHED: " + rule.name);
                ruleMatched = true;

                // Update log with matched rule
                smsLog.matchedRuleId = rule.id;
                smsLog.matchedRuleName = rule.name;

                // Execute rule and track results
                ExecutionResult result = executeRule(sms, rule, smsLog);
                actionsExecuted += result.actionsExecuted;
                executedActions.addAll(result.actionTypes);

                if (result.hasError) {
                    smsLog.hasError = true;
                    smsLog.errorMessage = result.errorMessage;
                }

                smsLog.creditsUsed += result.creditsUsed;
            } else {
                Log.d(TAG, "❌ Rule not matched: " + rule.name);
            }
        }

        // Log summary
        Log.d(TAG, "========================================");
        Log.d(TAG, "📊 PROCESSING SUMMARY");
        Log.d(TAG, "========================================");
        Log.d(TAG, "Rule Matched: " + (ruleMatched ? "YES" : "NO"));
        if (ruleMatched) {
            Log.d(TAG, "Matched Rule: " + smsLog.matchedRuleName);
            Log.d(TAG, "Actions Executed: " + actionsExecuted);
            Log.d(TAG, "Action Types: " + String.join(", ", executedActions));
            Log.d(TAG, "Credits Used: " + smsLog.creditsUsed);
            Log.d(TAG, "Available Credits: " + creditManager.getCredits());
        } else {
            Log.d(TAG, "No matching rules found");
        }
        Log.d(TAG, "========================================\n");

        // Save log to database
        smsLog.actionsExecuted = gson.toJson(executedActions);
        new Thread(() -> db.smsLogDao().insertLog(smsLog)).start();
    }

    /**
     * Check if SMS matches rule conditions
     */
    private boolean matchesRule(SmsMessage sms, Rule rule) {
        Log.d(TAG, "  Checking conditions for: " + rule.name);

        // Check SIM filter
        if (!matchesSimFilter(sms.getSimSlot(), rule.simFilter)) {
            Log.d(TAG, "  ❌ SIM filter failed: " + rule.simFilter);
            return false;
        }
        Log.d(TAG, "  ✅ SIM filter passed: " + rule.simFilter);

        // Check sender filter
        if (!matchesSenderFilter(sms.getSender(), rule.senderFilterType, rule.senderFilterValue)) {
            Log.d(TAG, "  ❌ Sender filter failed: " + rule.senderFilterType + " " + rule.senderFilterValue);
            return false;
        }
        Log.d(TAG, "  ✅ Sender filter passed: " + rule.senderFilterType);

        // Check message filter
        if (!matchesMessageFilter(sms.getBody(), rule.messageFilterType, rule.messageFilterValue)) {
            Log.d(TAG, "  ❌ Message filter failed: " + rule.messageFilterType);
            return false;
        }
        Log.d(TAG, "  ✅ Message filter passed: " + rule.messageFilterType);

        return true;
    }

    private boolean matchesSimFilter(String simSlot, String filter) {
        if ("ANY".equals(filter)) {
            return true;
        }
        if ("BOTH".equals(filter)) {
            return true;
        }
        return simSlot.equals(filter);
    }

    private boolean matchesSenderFilter(String sender, String filterType, String filterValue) {
        if ("ANY".equals(filterType) || filterValue == null || filterValue.isEmpty()) {
            return true;
        }

        switch (filterType) {
            case "EQUALS":
                return sender.equals(filterValue);
            case "CONTAINS":
                return sender.contains(filterValue);
            case "STARTS_WITH":
                return sender.startsWith(filterValue);
            case "REGEX":
                try {
                    return Pattern.matches(filterValue, sender);
                } catch (Exception e) {
                    Log.e(TAG, "Invalid regex: " + filterValue, e);
                    return false;
                }
            default:
                return true;
        }
    }

    private boolean matchesMessageFilter(String message, String filterType, String filterValue) {
        if ("ANY".equals(filterType) || filterValue == null || filterValue.isEmpty()) {
            return true;
        }

        switch (filterType) {
            case "EQUALS":
                return message.equals(filterValue);
            case "CONTAINS":
                return message.toLowerCase().contains(filterValue.toLowerCase());
            case "REGEX":
                try {
                    return Pattern.matches(filterValue, message);
                } catch (Exception e) {
                    Log.e(TAG, "Invalid regex: " + filterValue, e);
                    return false;
                }
            default:
                return true;
        }
    }

    /**
     * Execute all actions for a matched rule
     */
    private ExecutionResult executeRule(SmsMessage sms, Rule rule, SmsLog smsLog) {
        ExecutionResult result = new ExecutionResult();

        try {
            List<Action> actions = parseActions(rule.actionsJson);
            Log.d(TAG, "  📋 Executing " + actions.size() + " action(s)");

            for (Action action : actions) {
                if (!action.enabled) {
                    Log.d(TAG, "    ⏭️ Skipping disabled action: " + action.type);
                    continue;
                }

                Log.d(TAG, "    🚀 Executing action: " + action.type);

                // CHECK CREDITS BEFORE EXECUTING ACTION
                int requiredCredits = getCreditCostForAction(action.type);

                if (!creditManager.hasEnoughCredits(requiredCredits)) {
                    String error = "Insufficient credits for " + action.type +
                            ". Required: " + requiredCredits +
                            ", Available: " + creditManager.getCredits();
                    Log.e(TAG, "    ❌ " + error);
                    result.hasError = true;
                    result.errorMessage = error;
                    notifyLowCredits();
                    continue;
                }

                // Apply transformation FIRST if enabled
                String transformedBody = sms.getBody();
                if (action.enableTransform) {
                    transformedBody = applyTransformation(sms.getBody(), action);
                    Log.d(TAG, "    🔄 Message transformed");
                    Log.d(TAG, "       Type: " + action.transformType);
                    Log.d(TAG, "       Original: " + sms.getBody());
                    Log.d(TAG, "       Transformed: " + transformedBody);

                    // Update log
                    smsLog.wasTransformed = true;
                    smsLog.transformedMessage = transformedBody;
                    smsLog.transformType = action.transformType;
                }

                // Process template with variables
                SmsMessage transformedSms = new SmsMessage(
                        sms.getSender(),
                        transformedBody,
                        sms.getSimSlot(),
                        sms.getTimestamp(),
                        sms.getSubscriptionId()
                );

                String processedMessage = processTemplate(action.template, transformedSms);
                Log.d(TAG, "    📝 Processed message: " + processedMessage);

                // DEDUCT CREDITS BEFORE EXECUTING
                if (creditManager.deductCredits(requiredCredits,
                        "Action: " + action.type + " for rule: " + rule.name)) {

                    Log.d(TAG, "    💳 Credits deducted: " + requiredCredits +
                            ", Remaining: " + creditManager.getCredits());

                    // Execute the action
                    boolean success = executeAction(action, processedMessage, sms, smsLog);

                    if (success) {
                        result.actionsExecuted++;
                        result.actionTypes.add(action.type.toString());
                        result.creditsUsed += requiredCredits;
                        Log.d(TAG, "    ✅ Action executed successfully");
                    } else {
                        Log.e(TAG, "    ❌ Action execution failed");
                        result.hasError = true;
                    }
                } else {
                    Log.e(TAG, "    ❌ Failed to deduct credits for action: " + action.type);
                    result.hasError = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error executing rule: " + rule.name, e);
            result.hasError = true;
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    /**
     * Get credit cost based on action type
     */
    private int getCreditCostForAction(Action.ActionType type) {
        switch (type) {
            case SMS:
                return CreditManager.COST_PER_SMS;
            case WEBHOOK:
                return CreditManager.COST_PER_WEBHOOK;
            case TELEGRAM:
                return CreditManager.COST_PER_TELEGRAM;
            case WHATSAPP:
                return CreditManager.COST_PER_TELEGRAM;
            default:
                return 1;
        }
    }

    /**
     * Notify user about low credits
     */
    private void notifyLowCredits() {
        Log.w(TAG, "⚠️ LOW CREDITS WARNING - User should be notified");
    }

    /**
     * Apply message transformation
     */
    private String applyTransformation(String message, Action action) {
        try {
            if (action.transformChain != null && !action.transformChain.isEmpty()) {
                List<MessageTransformer.Transform> transforms = gson.fromJson(
                        action.transformChain,
                        new TypeToken<List<MessageTransformer.Transform>>(){}.getType()
                );
                return MessageTransformer.chainTransforms(message, transforms);
            }

            MessageTransformer.TransformType type = action.getTransformType();
            return MessageTransformer.transform(message, type, action.transformPattern);

        } catch (Exception e) {
            Log.e(TAG, "Error applying transformation", e);
            return message;
        }
    }

    /**
     * Parse actions JSON
     */
    private List<Action> parseActions(String actionsJson) {
        if (actionsJson == null || actionsJson.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return gson.fromJson(actionsJson, new TypeToken<List<Action>>(){}.getType());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing actions JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * Process template with variables
     */
    private String processTemplate(String template, SmsMessage sms) {
        if (template == null) {
            return sms.getBody();
        }

        return template
                .replace("{sender}", sms.getSender())
                .replace("{message}", sms.getBody())
                .replace("{sim}", sms.getSimSlot())
                .replace("{date}", sms.getFormattedDate())
                .replace("{time}", String.valueOf(sms.getTimestamp()));
    }

    /**
     * Execute specific action and update log
     */
    private boolean executeAction(Action action, String message, SmsMessage sms, SmsLog log) {
        Log.d(TAG, "    📤 Sending via: " + action.type);
        Log.d(TAG, "       Destination: " + action.destination);

        try {
            switch (action.type) {
                case WEBHOOK:
                    new WebhookExecutor(context).execute(action, message, sms);
                    log.webhookSent = true;
                    log.webhookStatus = 200; // This should be updated by callback
                    return true;

                case SMS:
                    new SmsExecutor(context).execute(action, message, sms);
                    log.smsForwarded = true;
                    return true;

                case TELEGRAM:
                    new TelegramExecutor(context).execute(action, message, sms);
                    log.telegramSent = true;
                    return true;

                case WHATSAPP:
                    new WhatsAppExecutor(context).execute(action, message, sms);
                    log.whatsappSent = true;
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "    ❌ Error executing action", e);
            return false;
        }
    }

    /**
     * Execution result container
     */
    private static class ExecutionResult {
        int actionsExecuted = 0;
        int creditsUsed = 0;
        List<String> actionTypes = new ArrayList<>();
        boolean hasError = false;
        String errorMessage = null;
    }
}