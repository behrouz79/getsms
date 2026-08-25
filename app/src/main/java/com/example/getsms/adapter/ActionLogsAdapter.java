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

        // Strip background (green rail = success, red rail = error)
        holder.itemView.setBackgroundResource(
                log.success ? R.drawable.strip_success : R.drawable.strip_error);

        // Action type badge — amber, ALLCAPS
        holder.tvActionType.setText(log.actionType != null ? log.actionType.toUpperCase(Locale.ROOT) : "");

        // Destination (monospace, ellipsized)
        holder.tvDestination.setText(log.actionDestination != null ? log.actionDestination : "");

        // Timestamp — HH:mm
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(log.executionTime)));

        // Meta line: "142 ms · 200 OK" or "5.0 s · timeout"
        String meta = buildMetaLine(log);
        holder.tvDuration.setText(meta);
        int metaColor = log.success
                ? context.getResources().getColor(R.color.text_secondary)
                : context.getResources().getColor(R.color.error);
        holder.tvDuration.setTextColor(metaColor);

        // Retry badge
        if (log.isRetry && log.attemptNumber > 1) {
            holder.tvRetryBadge.setVisibility(View.VISIBLE);
            holder.tvRetryBadge.setText("retry " + log.attemptNumber);
        } else {
            holder.tvRetryBadge.setVisibility(View.GONE);
        }

        // Backup badge
        holder.tvBackupBadge.setVisibility(log.isBackupAction ? View.VISIBLE : View.GONE);

        // From / rule name
        if (log.ruleName != null && !log.ruleName.isEmpty()) {
            holder.tvFrom.setVisibility(View.VISIBLE);
            holder.tvFrom.setText("Rule: " + log.ruleName);
        } else {
            holder.tvFrom.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLogClick(log, position);
        });
    }

    private String buildMetaLine(ActionLog log) {
        if (log.success) {
            String duration = log.durationMs >= 1000
                    ? String.format(Locale.ROOT, "%.1f s", log.durationMs / 1000.0)
                    : log.durationMs + " ms";
            String code = log.statusCode > 0 ? " · " + log.statusCode : "";
            return duration + code;
        } else {
            String duration = log.durationMs >= 1000
                    ? String.format(Locale.ROOT, "%.1f s", log.durationMs / 1000.0)
                    : log.durationMs + " ms";
            String err = log.errorType != null ? log.errorType.toLowerCase(Locale.ROOT) : "error";
            return duration + " · " + err;
        }
    }

    @Override
    public int getItemCount() {
        return logs != null ? logs.size() : 0;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvActionType;
        TextView tvDestination;
        TextView tvTime;
        TextView tvDuration;
        TextView tvRetryBadge;
        TextView tvBackupBadge;
        TextView tvFrom;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActionType  = itemView.findViewById(R.id.tvActionType);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvDuration    = itemView.findViewById(R.id.tvDuration);
            tvRetryBadge  = itemView.findViewById(R.id.tvRetryBadge);
            tvBackupBadge = itemView.findViewById(R.id.tvBackupBadge);
            tvFrom        = itemView.findViewById(R.id.tvFrom);
        }
    }
}
