package com.example.getsms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.Action;

import java.util.List;

public class ActionsAdapter extends RecyclerView.Adapter<ActionsAdapter.ActionViewHolder> {

    private final Context context;
    private final List<Action> actions;
    private final ActionClickListener listener;

    public interface ActionClickListener {
        void onEditClick(Action action, int position);
        void onDeleteClick(Action action, int position);
    }

    public ActionsAdapter(Context context, List<Action> actions, ActionClickListener listener) {
        this.context = context;
        this.actions = actions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_action, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        Action action = actions.get(position);

        // Display action type with retry/backup indicators
        StringBuilder typeText = new StringBuilder(action.type.toString());

        if (action.enableRetry) {
            typeText.append(" 🔄");
        }
        if (action.enableBackup && action.hasValidBackup()) {
            typeText.append(" 💾");
        }

        holder.tvActionType.setText(typeText.toString());

        // Display destination based on type
        StringBuilder destinationText = new StringBuilder();
        switch (action.type) {
            case WEBHOOK:
                destinationText.append("URL: ").append(action.destination);
                break;
            case SMS:
                destinationText.append("Phone: ").append(action.destination);
                break;
            case TELEGRAM:
                destinationText.append("Chat ID: ").append(action.chatId);
                break;
            case WHATSAPP:
                destinationText.append("Phone: ").append(action.destination);
                break;
        }

        // Add retry/backup info
        if (action.enableRetry) {
            destinationText.append(" • Retry: ").append(action.maxRetries).append("x");
        }
        if (action.enableBackup && action.hasValidBackup()) {
            destinationText.append(" • Backup: ").append(action.backupType);
        }

        holder.tvDestination.setText(destinationText.toString());

        // Display template (truncated if too long)
        String template = action.template != null ? action.template : "{message}";
        if (template.length() > 50) {
            template = template.substring(0, 47) + "...";
        }
        holder.tvTemplate.setText("Template: " + template);

        // Display transformation info if enabled
        if (action.enableTransform && action.transformType != null) {
            holder.tvTemplate.setText(holder.tvTemplate.getText() + " 🔄 " + action.transformType);
        }

        // Set enabled/disabled appearance
        float alpha = action.enabled ? 1.0f : 0.5f;
        holder.tvActionType.setAlpha(alpha);
        holder.tvDestination.setAlpha(alpha);
        holder.tvTemplate.setAlpha(alpha);

        // Click listeners
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(action, position));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(action, position));
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        TextView tvActionType;
        TextView tvDestination;
        TextView tvTemplate;
        Button btnEdit;
        Button btnDelete;

        ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActionType = itemView.findViewById(R.id.tvActionType);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvTemplate = itemView.findViewById(R.id.tvTemplate);
            btnEdit = itemView.findViewById(R.id.btnEditAction);
            btnDelete = itemView.findViewById(R.id.btnDeleteAction);
        }
    }
}