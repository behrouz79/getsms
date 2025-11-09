package com.example.getsms.model;

import com.example.getsms.engine.MessageTransformer;

public class Action {

    public enum ActionType {
        WEBHOOK,
        SMS,
        WHATSAPP,
        TELEGRAM
    }

    public int id;
    public int ruleId;
    public ActionType type;
    public String template; // Message template with variables
    public String destination; // Phone number, URL, chat ID, etc.
    public boolean enabled;

    // For Webhook
    public String httpMethod; // "POST", "GET"
    public String headers; // JSON string

    // For Telegram
    public String botToken;
    public String chatId;

    // For WhatsApp
    public String whatsappApiUrl;
    public String whatsappApiKey;

    // NEW: Message Transformation Settings
    public boolean enableTransform; // Enable/disable transformation
    public String transformType; // TransformType as string
    public String transformPattern; // Pattern for transformation
    public String transformChain; // JSON array of multiple transforms

    public Action() {
        this.enabled = true;
        this.httpMethod = "POST";
        this.enableTransform = false;
    }

    public Action(int ruleId, ActionType type, String template, String destination) {
        this();
        this.ruleId = ruleId;
        this.type = type;
        this.template = template;
        this.destination = destination;
    }

    /**
     * Get transform type enum
     */
    public MessageTransformer.TransformType getTransformType() {
        if (transformType == null || transformType.isEmpty()) {
            return MessageTransformer.TransformType.NONE;
        }
        try {
            return MessageTransformer.TransformType.valueOf(transformType);
        } catch (IllegalArgumentException e) {
            return MessageTransformer.TransformType.NONE;
        }
    }

    /**
     * Set transform type
     */
    public void setTransformType(MessageTransformer.TransformType type) {
        this.transformType = type != null ? type.name() : null;
    }
}