package com.example.getsms.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.getsms.R;
import com.example.getsms.modul.Response;

import java.io.File;
import java.util.List;

public class AdapterRequRec extends RecyclerView.Adapter<AdapterRequRec.MyHolder> {

    private Context cxt;
    private List<Response> data;

    public AdapterRequRec(Context cxt, List<Response> data) {
        this.cxt = cxt;
        this.data = data;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vv = LayoutInflater.from(cxt).inflate(R.layout.item_rec_request, parent, false);

        return new MyHolder(vv);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.name.setText(data.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {

        private TextView name;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.tvName);
        }
    }
}
