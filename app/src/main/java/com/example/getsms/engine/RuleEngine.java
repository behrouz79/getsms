package com.example.getsms.engine;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.getsms.credit.CreditManager;
import com.example.getsms.model.Action;
import com.example.getsms.model.Rule;
import com.example.getsms.model.SmsMessage;
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

    public RuleEngine(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.creditManager = new CreditManager(context);
    }

    /**
     * Process incoming SMS through all rules
     */
    public void processSms(SmsMessage sms, List<Rule> rules) {
        Log.d(TAG, "Processing SMS from: " + sms.getSender() + " on " + sms.getSimSlot());

        // Sort rules by priority (lower number = higher priority)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            rules.sort((r1, r2) -> Integer.compare(r1.priority, r2.priority));
        }

        for (Rule rule : rules) {
            if (!rule.enabled) {
                continue;
            }

            if (matchesRule(sms, rule)) {
                Log.d(TAG, "Rule matched: " + rule.name);
                executeRule(sms, rule);
            }
        }
    }

    /**
     * Check if SMS matches rule conditions
     */
    private boolean matchesRule(SmsMessage sms, Rule rule) {
        // Check SIM filter
        if (!matchesSimFilter(sms.getSimSlot(), rule.simFilter)) {
            return false;
        }

        // Check sender filter
        if (!matchesSenderFilter(sms.getSender(), rule.senderFilterType, rule.senderFilterValue)) {
            return false;
        }

        // Check message filter
        if (!matchesMessageFilter(sms.getBody(), rule.messageFilterType, rule.messageFilterValue)) {
            return false;
        }

        return true;
    }

    private boolean matchesSimFilter(String simSlot, String filter) {
        if ("ANY".equals(filter)) {
            return true;
        }
        if ("BOTH".equals(filter)) {
            return true; // Both means any SIM
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
    private void executeRule(SmsMessage sms, Rule rule) {
        try {
            List<Action> actions = parseActions(rule.actionsJson);

            for (Action action : actions) {
                if (!action.enabled) {
                    continue;
                }

                // CHECK CREDITS BEFORE EXECUTING ACTION
                int requiredCredits = getCreditCostForAction(action.type);

                if (!creditManager.hasEnoughCredits(requiredCredits)) {
                    Log.e(TAG, "Insufficient credits for action: " + action.type +
                            ". Required: " + requiredCredits +
                            ", Available: " + creditManager.getCredits());

                    // Optionally send a notification to user about low credits
                    notifyLowCredits();
                    continue; // Skip this action
                }

                // Apply transformation FIRST if enabled (on original SMS body)
                String transformedBody = sms.getBody();
                if (action.enableTransform) {
                    transformedBody = applyTransformation(sms.getBody(), action);
                    Log.d(TAG, "Message transformed for action: " + action.type);
                    Log.d(TAG, "Original: " + sms.getBody());
                    Log.d(TAG, "Transformed: " + transformedBody);
                }

                // THEN process template with variables (using transformed body)
                SmsMessage transformedSms = new SmsMessage(
                        sms.getSender(),
                        transformedBody,
                        sms.getSimSlot(),
                        sms.getTimestamp(),
                        sms.getSubscriptionId()
                );

                String processedMessage = processTemplate(action.template, transformedSms);

                // DEDUCT CREDITS BEFORE EXECUTING
                if (creditManager.deductCredits(requiredCredits,
                        "Action: " + action.type + " for rule: " + rule.name)) {

                    Log.d(TAG, "Credits deducted: " + requiredCredits +
                            ", Remaining: " + creditManager.getCredits());

                    // Execute the action
                    executeAction(action, processedMessage, sms);
                } else {
                    Log.e(TAG, "Failed to deduct credits for action: " + action.type);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing rule: " + rule.name, e);
        }
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
                return CreditManager.COST_PER_TELEGRAM; // Same as Telegram
            default:
                return 1;
        }
    }

    /**
     * Notify user about low credits
     */
    private void notifyLowCredits() {
        // TODO: Implement notification to user
        // You can use Android Notifications or save a flag to show dialog in MainActivity
        Log.w(TAG, "Low credits warning - user should be notified");
    }

    /**
     * Apply message transformation based on action settings
     */
    private String applyTransformation(String message, Action action) {
        try {
            // Check if using chain transforms
            if (action.transformChain != null && !action.transformChain.isEmpty()) {
                List<MessageTransformer.Transform> transforms = gson.fromJson(
                        action.transformChain,
                        new TypeToken<List<MessageTransformer.Transform>>(){}.getType()
                );
                return MessageTransformer.chainTransforms(message, transforms);
            }

            // Single transformation
            MessageTransformer.TransformType type = action.getTransformType();
            return MessageTransformer.transform(message, type, action.transformPattern);

        } catch (Exception e) {
            Log.e(TAG, "Error applying transformation", e);
            return message; // Return original on error
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
     * Variables: {sender}, {message}, {sim}, {date}, {time}
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
     * Execute specific action
     */
    private void executeAction(Action action, String message, SmsMessage sms) {
        Log.d(TAG, "Executing action: " + action.type);

        switch (action.type) {
            case WEBHOOK:
                new WebhookExecutor(context).execute(action, message, sms);
                break;
            case SMS:
                new SmsExecutor(context).execute(action, message, sms);
                break;
            case TELEGRAM:
                new TelegramExecutor(context).execute(action, message, sms);
                break;
            case WHATSAPP:
                new WhatsAppExecutor(context).execute(action, message, sms);
                break;
        }
    }
}