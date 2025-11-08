package com.example.getsms.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "rules")
public class Rule {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "enabled")
    public boolean enabled;

    @ColumnInfo(name = "priority")
    public int priority; // Lower number = higher priority

    // Conditions
    @ColumnInfo(name = "sim_filter")
    public String simFilter; // "SIM1", "SIM2", "BOTH", "ANY"

    @ColumnInfo(name = "sender_filter_type")
    public String senderFilterType; // "CONTAINS", "EQUALS", "STARTS_WITH", "REGEX", "ANY"

    @ColumnInfo(name = "sender_filter_value")
    public String senderFilterValue;

    @ColumnInfo(name = "message_filter_type")
    public String messageFilterType; // "CONTAINS", "EQUALS", "REGEX", "ANY"

    @ColumnInfo(name = "message_filter_value")
    public String messageFilterValue;

    // Actions (stored as JSON or comma-separated)
    @ColumnInfo(name = "actions_json")
    public String actionsJson; // JSON array of actions

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public Rule() {
        this.enabled = true;
        this.priority = 0;
        this.simFilter = "ANY";
        this.senderFilterType = "ANY";
        this.messageFilterType = "ANY";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}