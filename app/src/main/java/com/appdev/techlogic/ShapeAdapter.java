package com.appdev.techlogic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ShapeAdapter extends RecyclerView.Adapter<ShapeAdapter.ViewHolder> {

    List<ShapeItem> list;
    OnShapeClickListener listener;

    public interface OnShapeClickListener {
        void onShapeClick(String type);
    }

    public ShapeAdapter(List<ShapeItem> list, OnShapeClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shape_tool, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShapeItem item = list.get(position);
        holder.txtTool.setText(item.type);

        holder.itemView.setOnClickListener(v -> {
            listener.onShapeClick(item.type);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTool;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTool = itemView.findViewById(R.id.txtTool);
        }
    }
}