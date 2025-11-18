package com.example.getsms.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.ActionLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying detailed action logs
 */
public class ActionLogsAdapter extends RecyclerView.Adapter<ActionLogsAdapter.LogViewHolder> {

    private final Context context;
    private final List<ActionLog> logs;
    private final OnLogClickListener listener;

    public interface OnLogClickListener {
        void onLogClick(ActionLog log, int position);
    }

    public ActionLogsAdapter(Context context, List<ActionLog> logs, OnLogClickListener listener) {
        this.context = context;
        this.logs = logs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_action_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        ActionLog log = logs.get(position);

        // Status and Action Type
        if (log.success) {
            holder.tvStatus.setText("✅");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        } else {
            holder.tvStatus.setText("❌");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
        }

        // Action type with emoji
        String actionEmoji = getActionEmoji(log.actionType);
        holder.tvActionType.setText(actionEmoji + " " + log.actionType);

        // Destination
        holder.tvDestination.setText("📍 " + truncate(log.actionDestination, 30));

        // Duration and status code
        if (log.success) {
            holder.tvDuration.setText(String.format("⏱️ %dms | Status: %d",
                    log.durationMs, log.statusCode));
            holder.tvDuration.setTextColor(Color.parseColor("#666666"));
        } else {
            holder.tvDuration.setText(String.format("⏱️ %dms | %s %s",
                    log.durationMs, log.getErrorTypeEmoji(), log.errorType));
            holder.tvDuration.setTextColor(Color.parseColor("#F44336"));
        }

        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        holder.tvTime.setText("🕐 " + sdf.format(new Date(log.executionTime)));

        // Retry badge
        if (log.isRetry) {
            holder.tvRetryBadge.setVisibility(View.VISIBLE);
            holder.tvRetryBadge.setText("🔄 Retry #" + log.attemptNumber);
        } else {
            holder.tvRetryBadge.setVisibility(View.GONE);
        }

        // Backup badge
        if (log.isBackupAction) {
            holder.tvBackupBadge.setVisibility(View.VISIBLE);
            holder.tvBackupBadge.setText("🔀 Backup");
        } else {
            holder.tvBackupBadge.setVisibility(View.GONE);
        }

        // Error message (if failed)
        if (!log.success && log.errorMessage != null) {
            holder.tvErrorMessage.setVisibility(View.VISIBLE);
            holder.tvErrorMessage.setText("💬 " + truncate(log.errorMessage, 80));
        } else {
            holder.tvErrorMessage.setVisibility(View.GONE);
        }

        // Response body (if success and available)
        if (log.success && log.responseBody != null && !log.responseBody.isEmpty()) {
            holder.tvResponse.setVisibility(View.VISIBLE);
            holder.tvResponse.setText("📄 " + truncate(log.responseBody, 50));
        } else {
            holder.tvResponse.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLogClick(log, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return logs != null ? logs.size() : 0;
    }

    /**
     * Get emoji for action type
     */
    private String getActionEmoji(String actionType) {
        switch (actionType) {
            case "WEBHOOK": return "🌐";
            case "SMS": return "📱";
            case "TELEGRAM": return "✈️";
            case "WHATSAPP": return "💚";
            default: return "📋";
        }
    }

    /**
     * Truncate string
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvStatus;
        TextView tvActionType;
        TextView tvDestination;
        TextView tvDuration;
        TextView tvTime;
        TextView tvRetryBadge;
        TextView tvBackupBadge;
        TextView tvErrorMessage;
        TextView tvResponse;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvActionType = itemView.findViewById(R.id.tvActionType);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRetryBadge = itemView.findViewById(R.id.tvRetryBadge);
            tvBackupBadge = itemView.findViewById(R.id.tvBackupBadge);
            tvErrorMessage = itemView.findViewById(R.id.tvErrorMessage);
            tvResponse = itemView.findViewById(R.id.tvResponse);
        }
    }
}