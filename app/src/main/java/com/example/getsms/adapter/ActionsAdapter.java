package com.example.getsms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.model.Action;

import java.util.List;
import java.util.Locale;

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

        holder.itemView.setAlpha(action.enabled ? 1.0f : 0.65f);

        // Action type badge — WEBHOOK / SMS / TELEGRAM etc.
        StringBuilder typeLine = new StringBuilder(action.type.toString().toUpperCase(Locale.ROOT));
        if (action.enableRetry) typeLine.append(" · RETRY");
        if (action.enableBackup && action.hasValidBackup()) typeLine.append(" · BACKUP");
        holder.tvActionType.setText(typeLine.toString());

        // Destination (monospace, ellipsized)
        String dest;
        switch (action.type) {
            case WEBHOOK:  dest = action.destination != null ? action.destination : ""; break;
            case SMS:      dest = action.destination != null ? action.destination : ""; break;
            case TELEGRAM: dest = action.chatId != null ? action.chatId : ""; break;
            case WHATSAPP: dest = action.destination != null ? action.destination : ""; break;
            default:       dest = "";
        }
        holder.tvDestination.setText(dest);

        // Template preview (caption monospace)
        String tpl = action.template != null ? action.template : "{message}";
        holder.tvTemplate.setText(tpl);

        holder.itemView.setOnClickListener(v -> listener.onEditClick(action, position));
        holder.tvDeleteAction.setOnClickListener(v -> listener.onDeleteClick(action, position));
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDragHandle;
        TextView tvActionType;
        TextView tvDestination;
        TextView tvTemplate;
        TextView tvDeleteAction;

        ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDragHandle   = itemView.findViewById(R.id.tvDragHandle);
            tvActionType   = itemView.findViewById(R.id.tvActionType);
            tvDestination  = itemView.findViewById(R.id.tvDestination);
            tvTemplate     = itemView.findViewById(R.id.tvTemplate);
            tvDeleteAction = itemView.findViewById(R.id.tvDeleteAction);
        }
    }
}
