package com.example.getsms.roomDB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.getsms.model.Rule;

import java.util.List;

@Dao
public interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY priority ASC, id DESC")
    List<Rule> getAllRules();

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority ASC")
    List<Rule> getEnabledRules();

    @Query("SELECT * FROM rules WHERE id = :ruleId")
    Rule getRuleById(int ruleId);

    @Insert
    long insertRule(Rule rule);

    @Update
    void updateRule(Rule rule);

    @Delete
    void deleteRule(Rule rule);

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :ruleId")
    void toggleRule(int ruleId, boolean enabled);

    @Query("DELETE FROM rules")
    void deleteAllRules();

    @Query("SELECT COUNT(*) FROM rules")
    int getRuleCount();
}