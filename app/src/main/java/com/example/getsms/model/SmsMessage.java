package com.example.getsms.model;

public class SmsMessage {

    private String sender;
    private String body;
    private String simSlot; // "SIM1", "SIM2", "UNKNOWN"
    private long timestamp;
    private int subscriptionId; // For dual SIM identification

    public SmsMessage(String sender, String body, String simSlot, long timestamp, int subscriptionId) {
        this.sender = sender;
        this.body = body;
        this.simSlot = simSlot;
        this.timestamp = timestamp;
        this.subscriptionId = subscriptionId;
    }

    public String getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public String getSimSlot() {
        return simSlot;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    // Format date as string
    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}