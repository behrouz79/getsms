package com.example.getsms.engine;

import android.content.Context;
import android.os.Build;
import android.util.Log;

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

    public RuleEngine(Context context) {
        this.context = context;
        this.gson = new Gson();
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

                String processedMessage = processTemplate(action.template, sms);
                executeAction(action, processedMessage, sms);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing rule: " + rule.name, e);
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