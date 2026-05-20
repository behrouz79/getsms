package com.example.getsms.engine;

import android.content.Context;
import android.os.Build;
import android.util.Log;

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
    private final DataBase db;
    private final ActionExecutor actionExecutor;

    public RuleEngine(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.db = DataBase.getDbInstance(context);
        this.actionExecutor = new ActionExecutor(context);
    }

    public void processSms(SmsMessage sms, List<Rule> rules) {
        Log.d(TAG, "Processing SMS from: " + sms.getSender() + " SIM: " + sms.getSimSlot());

        SmsLog smsLog = SmsLog.fromSmsMessage(sms);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            rules.sort((r1, r2) -> Integer.compare(r1.priority, r2.priority));
        }

        boolean ruleMatched = false;

        for (Rule rule : rules) {
            if (!rule.enabled) continue;

            if (matchesRule(sms, rule)) {
                Log.d(TAG, "Rule matched: " + rule.name);
                ruleMatched = true;
                smsLog.matchedRuleId = rule.id;
                smsLog.matchedRuleName = rule.name;
                executeRule(sms, rule, smsLog);
            }
        }

        if (!ruleMatched) {
            Log.d(TAG, "No matching rules for SMS from: " + sms.getSender());
        }

        final SmsLog finalLog = smsLog;
        new Thread(() -> db.smsLogDao().insertLog(finalLog)).start();
    }

    private boolean matchesRule(SmsMessage sms, Rule rule) {
        return matchesSimFilter(sms.getSimSlot(), rule.simFilter)
                && matchesSenderFilter(sms.getSender(), rule.senderFilterType, rule.senderFilterValue)
                && matchesMessageFilter(sms.getBody(), rule.messageFilterType, rule.messageFilterValue);
    }

    private boolean matchesSimFilter(String simSlot, String filter) {
        if ("ANY".equals(filter) || "BOTH".equals(filter)) return true;
        return simSlot.equals(filter);
    }

    private boolean matchesSenderFilter(String sender, String filterType, String filterValue) {
        if ("ANY".equals(filterType) || filterValue == null || filterValue.isEmpty()) return true;
        switch (filterType) {
            case "EQUALS":     return sender.equals(filterValue);
            case "CONTAINS":   return sender.contains(filterValue);
            case "STARTS_WITH": return sender.startsWith(filterValue);
            case "REGEX":
                try { return Pattern.matches(filterValue, sender); }
                catch (Exception e) { return false; }
            default: return true;
        }
    }

    private boolean matchesMessageFilter(String message, String filterType, String filterValue) {
        if ("ANY".equals(filterType) || filterValue == null || filterValue.isEmpty()) return true;
        switch (filterType) {
            case "EQUALS":       return message.equals(filterValue);
            case "CONTAINS":     return message.toLowerCase().contains(filterValue.toLowerCase());
            case "NOT_CONTAINS": return !message.toLowerCase().contains(filterValue.toLowerCase());
            case "NOT_EQUALS":   return !message.equals(filterValue);
            case "REGEX":
                try { return Pattern.matches(filterValue, message); }
                catch (Exception e) { return false; }
            default: return true;
        }
    }

    private void executeRule(SmsMessage sms, Rule rule, SmsLog smsLog) {
        List<Action> actions = parseActions(rule.actionsJson);

        for (Action action : actions) {
            if (!action.enabled) continue;

            String transformedBody = sms.getBody();
            if (action.enableTransform) {
                transformedBody = applyTransformation(sms.getBody(), action);
                smsLog.wasTransformed = true;
                smsLog.transformedMessage = transformedBody;
                smsLog.transformType = action.transformType;
            }

            SmsMessage transformedSms = new SmsMessage(
                    sms.getSender(), transformedBody, sms.getSimSlot(),
                    sms.getTimestamp(), sms.getSubscriptionId());

            String processedMessage = processTemplate(action.template, transformedSms);

            actionExecutor.execute(action, processedMessage, sms, smsLog,
                    new ActionExecutor.ActionCallback() {
                        @Override
                        public void onSuccess(int attempts) {
                            if (attempts > 1) {
                                smsLog.retryCount += (attempts - 1);
                                smsLog.succeededAfterRetry = true;
                            }
                        }

                        @Override
                        public void onFailure(int attempts, String reason) {
                            smsLog.retryCount += attempts;
                        }

                        @Override
                        public void onBackupSuccess() {}
                    });
        }
    }

    private String applyTransformation(String message, Action action) {
        try {
            if (action.transformChain != null && !action.transformChain.isEmpty()) {
                List<MessageTransformer.Transform> transforms = gson.fromJson(
                        action.transformChain,
                        new TypeToken<List<MessageTransformer.Transform>>(){}.getType());
                return MessageTransformer.chainTransforms(message, transforms);
            }
            return MessageTransformer.transform(message, action.getTransformType(), action.transformPattern);
        } catch (Exception e) {
            Log.e(TAG, "Error applying transformation", e);
            return message;
        }
    }

    private List<Action> parseActions(String actionsJson) {
        if (actionsJson == null || actionsJson.isEmpty()) return new ArrayList<>();
        try {
            return gson.fromJson(actionsJson, new TypeToken<List<Action>>(){}.getType());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing actions JSON", e);
            return new ArrayList<>();
        }
    }

    private String processTemplate(String template, SmsMessage sms) {
        if (template == null) return sms.getBody();
        return template
                .replace("{sender}", sms.getSender())
                .replace("{message}", sms.getBody())
                .replace("{sim}", sms.getSimSlot())
                .replace("{date}", sms.getFormattedDate())
                .replace("{time}", String.valueOf(sms.getTimestamp()));
    }

    public void shutdown() {
        actionExecutor.shutdown();
    }
}
