package com.example.getsms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.SmsLog;

import java.util.List;

public class SmsLogAdapter extends RecyclerView.Adapter<SmsLogAdapter.LogViewHolder> {

    private final Context context;
    private final List<SmsLog> logs;
    private final OnLogClickListener listener;

    public interface OnLogClickListener {
        void onLogClick(SmsLog log, int position);
    }

    public SmsLogAdapter(Context context, List<SmsLog> logs, OnLogClickListener listener) {
        this.context = context;
        this.logs = logs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sms_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        SmsLog log = logs.get(position);

        holder.tvSender.setText(log.sender);
        holder.tvSimSlot.setText("SIM: " + log.simSlot);

        String message = log.messageBody;
        if (message != null && message.length() > 100) {
            message = message.substring(0, 97) + "...";
        }
        holder.tvMessage.setText(message);

        holder.tvDate.setText(log.formattedDate);

        if (log.matchedRuleName != null) {
            holder.tvRule.setVisibility(View.VISIBLE);
            holder.tvRule.setText("Rule: " + log.matchedRuleName);
        } else {
            holder.tvRule.setVisibility(View.GONE);
        }

        String actions = log.getActionsSummary();
        if (!"No actions".equals(actions)) {
            holder.tvActions.setVisibility(View.VISIBLE);
            holder.tvActions.setText("Actions: " + actions);
        } else {
            holder.tvActions.setVisibility(View.GONE);
        }

        if (log.wasTransformed) {
            holder.tvTransform.setVisibility(View.VISIBLE);
            holder.tvTransform.setText("Transformed (" + log.transformType + ")");
        } else {
            holder.tvTransform.setVisibility(View.GONE);
        }

        holder.tvCredits.setText("Credits: " + log.creditsUsed);

        int success = context.getResources().getColor(R.color.claude_success);
        int error = context.getResources().getColor(R.color.claude_error);

        if (log.hasError) {
            holder.tvStatus.setText("Error");
            holder.tvStatus.setTextColor(error);
            holder.itemView.setBackgroundResource(R.drawable.strip_error);
        } else {
            holder.tvStatus.setText("Success");
            holder.tvStatus.setTextColor(success);
            holder.itemView.setBackgroundResource(R.drawable.strip_success);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLogClick(log, position);
        });
    }

    @Override
    public int getItemCount() {
        return logs != null ? logs.size() : 0;
    }

    public void updateLogs(List<SmsLog> newLogs) {
        this.logs.clear();
        this.logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender;
        TextView tvSimSlot;
        TextView tvMessage;
        TextView tvDate;
        TextView tvRule;
        TextView tvActions;
        TextView tvTransform;
        TextView tvCredits;
        TextView tvStatus;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvSimSlot = itemView.findViewById(R.id.tvSimSlot);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRule = itemView.findViewById(R.id.tvRule);
            tvActions = itemView.findViewById(R.id.tvActions);
            tvTransform = itemView.findViewById(R.id.tvTransform);
            tvCredits = itemView.findViewById(R.id.tvCredits);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
