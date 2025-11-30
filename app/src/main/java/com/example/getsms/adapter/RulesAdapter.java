package com.example.getsms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.Rule;

import java.util.List;

public class RulesAdapter extends RecyclerView.Adapter<RulesAdapter.RuleViewHolder> {

    private final Context context;
    private final List<Rule> rules;
    private final RuleClickListener listener;

    public interface RuleClickListener {
        void onEditClick(Rule rule);
        void onDeleteClick(Rule rule);
        void onToggleClick(Rule rule);
        void onCopyClick(Rule rule); // NEW
    }

    public RulesAdapter(Context context, List<Rule> rules, RuleClickListener listener) {
        this.context = context;
        this.rules = rules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_rule, parent, false);
        return new RuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        Rule rule = rules.get(position);

        holder.tvRuleName.setText(rule.name);
        holder.switchEnabled.setChecked(rule.enabled);

        // Build conditions summary
        StringBuilder conditions = new StringBuilder();

        if (!"ANY".equals(rule.simFilter)) {
            conditions.append("SIM: ").append(rule.simFilter).append(" • ");
        }

        if (!"ANY".equals(rule.senderFilterType) && rule.senderFilterValue != null && !rule.senderFilterValue.isEmpty()) {
            conditions.append("Sender: ").append(rule.senderFilterValue).append(" • ");
        }

        if (!"ANY".equals(rule.messageFilterType) && rule.messageFilterValue != null && !rule.messageFilterValue.isEmpty()) {
            conditions.append("Message: ").append(rule.messageFilterValue).append(" • ");
        }

        if (conditions.length() > 0) {
            // Remove last " • "
            conditions.setLength(conditions.length() - 3);
            holder.tvRuleConditions.setText(conditions.toString());
        } else {
            holder.tvRuleConditions.setText("Match all messages");
        }

        // Set enabled/disabled appearance
        float alpha = rule.enabled ? 1.0f : 0.5f;
        holder.tvRuleName.setAlpha(alpha);
        holder.tvRuleConditions.setAlpha(alpha);

        // Click listeners
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(rule));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(rule));
        holder.btnCopy.setOnClickListener(v -> listener.onCopyClick(rule)); // NEW
        holder.switchEnabled.setOnClickListener(v -> listener.onToggleClick(rule));

        // Prevent switch from toggling when clicking the item
        holder.itemView.setOnClickListener(v -> listener.onEditClick(rule));
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    static class RuleViewHolder extends RecyclerView.ViewHolder {
        TextView tvRuleName;
        TextView tvRuleConditions;
        SwitchCompat switchEnabled;
        Button btnEdit;
        Button btnDelete;
        Button btnCopy; // NEW

        RuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRuleName = itemView.findViewById(R.id.tvRuleName);
            tvRuleConditions = itemView.findViewById(R.id.tvRuleConditions);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnCopy = itemView.findViewById(R.id.btnCopy); // NEW
        }
    }
}