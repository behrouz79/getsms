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
    private final ActionExecutor actionExecutor; // NEW: Single instance

    public RuleEngine(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.creditManager = new CreditManager(context);
        this.db = DataBase.getDbInstance(context);
        this.actionExecutor = new ActionExecutor(context); // NEW: Initialize once
    }

    public void processSms(SmsMessage sms, List<Rule> rules) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "📱 NEW SMS RECEIVED");
        Log.d(TAG, "========================================");
        Log.d(TAG, "Sender: " + sms.getSender());
        Log.d(TAG, "SIM Slot: " + sms.getSimSlot());
        Log.d(TAG, "Message: " + sms.getBody());
        Log.d(TAG, "Time: " + sms.getFormattedDate());
        Log.d(TAG, "========================================");

        SmsLog smsLog = SmsLog.fromSmsMessage(sms);

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

                smsLog.matchedRuleId = rule.id;
                smsLog.matchedRuleName = rule.name;

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

        smsLog.actionsExecuted = gson.toJson(executedActions);
        final SmsLog finalLog = smsLog;
        new Thread(() -> db.smsLogDao().insertLog(finalLog)).start();
    }

    private boolean matchesRule(SmsMessage sms, Rule rule) {
        Log.d(TAG, "  Checking conditions for: " + rule.name);

        if (!matchesSimFilter(sms.getSimSlot(), rule.simFilter)) {
            Log.d(TAG, "  ❌ SIM filter failed: " + rule.simFilter);
            return false;
        }
        Log.d(TAG, "  ✅ SIM filter passed: " + rule.simFilter);

        if (!matchesSenderFilter(sms.getSender(), rule.senderFilterType, rule.senderFilterValue)) {
            Log.d(TAG, "  ❌ Sender filter failed");
            return false;
        }
        Log.d(TAG, "  ✅ Sender filter passed");

        if (!matchesMessageFilter(sms.getBody(), rule.messageFilterType, rule.messageFilterValue)) {
            Log.d(TAG, "  ❌ Message filter failed");
            return false;
        }
        Log.d(TAG, "  ✅ Message filter passed");

        return true;
    }

    private boolean matchesSimFilter(String simSlot, String filter) {
        if ("ANY".equals(filter) || "BOTH".equals(filter)) {
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

                int requiredCredits = getCreditCostForAction(action.type);

                if (!creditManager.hasEnoughCredits(requiredCredits)) {
                    String error = "Insufficient credits for " + action.type;
                    Log.e(TAG, "    ❌ " + error);
                    result.hasError = true;
                    result.errorMessage = error;
                    continue;
                }

                // Apply transformation if enabled
                String transformedBody = sms.getBody();
                if (action.enableTransform) {
                    transformedBody = applyTransformation(sms.getBody(), action);
                    Log.d(TAG, "    🔄 Message transformed");

                    smsLog.wasTransformed = true;
                    smsLog.transformedMessage = transformedBody;
                    smsLog.transformType = action.transformType;
                }

                // Create transformed SMS
                SmsMessage transformedSms = new SmsMessage(
                        sms.getSender(),
                        transformedBody,
                        sms.getSimSlot(),
                        sms.getTimestamp(),
                        sms.getSubscriptionId()
                );

                String processedMessage = processTemplate(action.template, transformedSms);

                // Deduct credits
                if (creditManager.deductCredits(requiredCredits,
                        "Action: " + action.type + " for rule: " + rule.name)) {

                    Log.d(TAG, "    💳 Credits deducted: " + requiredCredits);

                    // FIXED: Execute with proper callback
                    final int actionIndex = result.actionsExecuted;
                    actionExecutor.execute(action, processedMessage, sms, smsLog,
                            new ActionExecutor.ActionCallback() {
                                @Override
                                public void onSuccess(int attempts) {
                                    Log.d(TAG, "    ✅ Action completed successfully after " + attempts + " attempts");
                                    if (attempts > 1) {
                                        smsLog.retryCount += (attempts - 1);
                                        smsLog.succeededAfterRetry = true;
                                    }
                                }

                                @Override
                                public void onFailure(int attempts, String reason) {
                                    Log.e(TAG, "    ❌ Action failed after " + attempts + " attempts: " + reason);
                                    smsLog.retryCount += attempts;
                                }

                                @Override
                                public void onBackupSuccess() {
                                    Log.d(TAG, "    🔀 Backup action succeeded");
                                }
                            });

                    result.actionsExecuted++;
                    result.actionTypes.add(action.type.toString());
                    result.creditsUsed += requiredCredits;

                } else {
                    Log.e(TAG, "    ❌ Failed to deduct credits");
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

    private static class ExecutionResult {
        int actionsExecuted = 0;
        int creditsUsed = 0;
        List<String> actionTypes = new ArrayList<>();
        boolean hasError = false;
        String errorMessage = null;
    }

    // NEW: Cleanup method
    public void shutdown() {
        if (actionExecutor != null) {
            actionExecutor.shutdown();
        }
    }
}