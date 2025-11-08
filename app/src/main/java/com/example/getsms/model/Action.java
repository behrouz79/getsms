package com.example.getsms.model;

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

    public Action() {
        this.enabled = true;
        this.httpMethod = "POST";
    }

    public Action(int ruleId, ActionType type, String template, String destination) {
        this();
        this.ruleId = ruleId;
        this.type = type;
        this.template = template;
        this.destination = destination;
    }
}