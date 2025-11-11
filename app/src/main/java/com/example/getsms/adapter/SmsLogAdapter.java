package com.example.getsms.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.SmsLog;

import java.util.List;

/**
 * Adapter to display comprehensive SMS logs with all details
 */
public class SmsLogAdapter extends RecyclerView.Adapter<SmsLogAdapter.LogViewHolder> {

    private static final String TAG = "SmsLogAdapter";

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
        Log.d(TAG, "Adapter created with " + logs.size() + " logs");
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sms_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        try {
            SmsLog log = logs.get(position);

            // Sender
            holder.tvSender.setText("📱 " + log.sender);

            // SIM Slot with color coding
            holder.tvSimSlot.setText("SIM: " + log.simSlot);
            if ("SIM1".equals(log.simSlot)) {
                holder.tvSimSlot.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else if ("SIM2".equals(log.simSlot)) {
                holder.tvSimSlot.setTextColor(Color.parseColor("#2196F3")); // Blue
            }

            // Message (truncated)
            String message = log.messageBody;
            if (message != null && message.length() > 100) {
                message = message.substring(0, 97) + "...";
            }
            holder.tvMessage.setText(message);

            // Date/Time
            holder.tvDate.setText("🕐 " + log.formattedDate);

            // Rule matched
            if (log.matchedRuleName != null) {
                holder.tvRule.setVisibility(View.VISIBLE);
                holder.tvRule.setText("📋 Rule: " + log.matchedRuleName);
            } else {
                holder.tvRule.setVisibility(View.GONE);
            }

            // Actions executed
            String actions = log.getActionsSummary();
            if (!"No actions".equals(actions)) {
                holder.tvActions.setVisibility(View.VISIBLE);
                holder.tvActions.setText("🚀 Actions: " + actions);
            } else {
                holder.tvActions.setVisibility(View.GONE);
            }

            // Transformation
            if (log.wasTransformed) {
                holder.tvTransform.setVisibility(View.VISIBLE);
                holder.tvTransform.setText("🔄 Transformed (" + log.transformType + ")");
            } else {
                holder.tvTransform.setVisibility(View.GONE);
            }

            // Credits
            holder.tvCredits.setText("💳 Credits: " + log.creditsUsed);
            if (log.creditsUsed > 0) {
                holder.tvCredits.setTextColor(Color.parseColor("#FF9800")); // Orange
            }

            // Status
            if (log.hasError) {
                holder.tvStatus.setText("❌ Error");
                holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
                holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Light red
            } else {
                holder.tvStatus.setText("✅ Success");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                holder.cardView.setCardBackgroundColor(Color.WHITE);
            }

            // Click listener
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLogClick(log, position);
                }
            });

            Log.d(TAG, "Bound log " + position + ": " + log.sender);

        } catch (Exception e) {
            Log.e(TAG, "Error binding log at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        int count = logs != null ? logs.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    /**
     * Update adapter data
     */
    public void updateLogs(List<SmsLog> newLogs) {
        Log.d(TAG, "Updating logs. New size: " + newLogs.size());
        this.logs.clear();
        this.logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
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
            cardView = (CardView) itemView;
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