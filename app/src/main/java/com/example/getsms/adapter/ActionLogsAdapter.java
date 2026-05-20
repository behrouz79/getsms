package com.example.getsms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.ActionLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        int success = context.getResources().getColor(R.color.claude_success);
        int error = context.getResources().getColor(R.color.claude_error);

        if (log.success) {
            holder.viewStatusStrip.setBackgroundColor(success);
            holder.tvStatus.setText("✓");
            holder.tvStatus.setTextColor(success);
        } else {
            holder.viewStatusStrip.setBackgroundColor(error);
            holder.tvStatus.setText("✕");
            holder.tvStatus.setTextColor(error);
        }

        holder.tvActionType.setText(log.actionType);
        holder.tvDestination.setText(truncate(log.actionDestination, 40));

        if (log.success) {
            holder.tvDuration.setText(log.durationMs + "ms  ·  " + log.statusCode);
            holder.tvDuration.setTextColor(context.getResources().getColor(R.color.claude_text_hint));
        } else {
            holder.tvDuration.setText(log.durationMs + "ms  ·  " + (log.errorType != null ? log.errorType : "ERROR"));
            holder.tvDuration.setTextColor(error);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(log.executionTime)));

        if (log.isRetry) {
            holder.tvRetryBadge.setVisibility(View.VISIBLE);
            holder.tvRetryBadge.setText("Retry #" + log.attemptNumber);
        } else {
            holder.tvRetryBadge.setVisibility(View.GONE);
        }

        if (log.isBackupAction) {
            holder.tvBackupBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvBackupBadge.setVisibility(View.GONE);
        }

        if (!log.success && log.errorMessage != null) {
            holder.tvErrorMessage.setVisibility(View.VISIBLE);
            holder.tvErrorMessage.setText(truncate(log.errorMessage, 80));
        } else {
            holder.tvErrorMessage.setVisibility(View.GONE);
        }

        if (log.success && log.responseBody != null && !log.responseBody.isEmpty()) {
            holder.tvResponse.setVisibility(View.VISIBLE);
            holder.tvResponse.setText(truncate(log.responseBody, 60));
        } else {
            holder.tvResponse.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLogClick(log, position);
        });
    }

    @Override
    public int getItemCount() {
        return logs != null ? logs.size() : 0;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        View viewStatusStrip;
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
            viewStatusStrip = itemView.findViewById(R.id.viewStatusStrip);
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
