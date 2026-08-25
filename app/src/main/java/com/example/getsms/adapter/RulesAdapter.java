package com.example.getsms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.Rule;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

public class RulesAdapter extends RecyclerView.Adapter<RulesAdapter.RuleViewHolder> {

    private final Context context;
    private final List<Rule> rules;
    private final RuleClickListener listener;
    private ItemTouchHelper itemTouchHelper;

    public interface RuleClickListener {
        void onEditClick(Rule rule);
        void onDeleteClick(Rule rule);
        void onToggleClick(Rule rule);
        void onCopyClick(Rule rule);
    }

    public RulesAdapter(Context context, List<Rule> rules, RuleClickListener listener) {
        this.context = context;
        this.rules = rules;
        this.listener = listener;
    }

    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.itemTouchHelper = helper;
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

        // Whole card fades when disabled
        holder.itemView.setAlpha(rule.enabled ? 1.0f : 0.65f);

        holder.tvRuleName.setText(rule.name);
        holder.switchEnabled.setChecked(rule.enabled);

        // Conditions summary
        StringBuilder conditions = new StringBuilder();
        if (!"ANY".equals(rule.simFilter)) {
            conditions.append("SIM: ").append(rule.simFilter).append(" • ");
        }
        if (!"ANY".equals(rule.senderFilterType) && rule.senderFilterValue != null && !rule.senderFilterValue.isEmpty()) {
            conditions.append("Sender: ").append(rule.senderFilterValue).append(" • ");
        }
        if (!"ANY".equals(rule.messageFilterType) && rule.messageFilterValue != null && !rule.messageFilterValue.isEmpty()) {
            String prefix = "Message: ";
            if ("NOT_CONTAINS".equals(rule.messageFilterType)) prefix = "Message NOT contains: ";
            else if ("NOT_EQUALS".equals(rule.messageFilterType)) prefix = "Message NOT equals: ";
            conditions.append(prefix).append(rule.messageFilterValue).append(" • ");
        }
        if (conditions.length() > 0) {
            conditions.setLength(conditions.length() - 3);
            holder.tvRuleConditions.setText(conditions.toString());
        } else {
            holder.tvRuleConditions.setText("Match all messages");
        }

        // Action count badge — primary tone if >0, neutral if 0
        int count = getActionCount(rule.actionsJson);
        String badgeText = count + (count == 1 ? " action" : " actions");
        holder.tvActionsBadge.setText(badgeText);
        if (count > 0) {
            holder.tvActionsBadge.setBackgroundResource(R.drawable.bg_badge_primary);
            holder.tvActionsBadge.setTextColor(context.getResources().getColor(R.color.primary));
        } else {
            holder.tvActionsBadge.setBackgroundResource(R.drawable.bg_badge_neutral);
            holder.tvActionsBadge.setTextColor(context.getResources().getColor(R.color.text_secondary));
        }

        // Drag handle — start drag on touch down
        holder.tvDragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && itemTouchHelper != null) {
                itemTouchHelper.startDrag(holder);
            }
            return false;
        });

        holder.tvEdit.setOnClickListener(v -> listener.onEditClick(rule));
        holder.tvCopy.setOnClickListener(v -> listener.onCopyClick(rule));
        holder.tvDelete.setOnClickListener(v -> listener.onDeleteClick(rule));
        holder.switchEnabled.setOnClickListener(v -> listener.onToggleClick(rule));
        holder.itemView.setOnClickListener(v -> listener.onEditClick(rule));
    }

    private static int getActionCount(String actionsJson) {
        if (actionsJson == null || actionsJson.isEmpty()) return 0;
        try {
            return new JSONArray(actionsJson).length();
        } catch (JSONException e) {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    static class RuleViewHolder extends RecyclerView.ViewHolder {
        TextView tvDragHandle;
        TextView tvRuleName;
        TextView tvRuleConditions;
        SwitchCompat switchEnabled;
        TextView tvActionsBadge;
        TextView tvEdit;
        TextView tvCopy;
        TextView tvDelete;

        RuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDragHandle     = itemView.findViewById(R.id.tvDragHandle);
            tvRuleName       = itemView.findViewById(R.id.tvRuleName);
            tvRuleConditions = itemView.findViewById(R.id.tvRuleConditions);
            switchEnabled    = itemView.findViewById(R.id.switchEnabled);
            tvActionsBadge   = itemView.findViewById(R.id.tvActionsBadge);
            tvEdit           = itemView.findViewById(R.id.tvEdit);
            tvCopy           = itemView.findViewById(R.id.tvCopy);
            tvDelete         = itemView.findViewById(R.id.tvDelete);
        }
    }
}
