package com.appdev.techlogic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // Added this
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ShapeAdapter extends RecyclerView.Adapter<ShapeAdapter.ViewHolder> {

    List<ShapeItem> list; // Your variable is named 'list'
    OnShapeClickListener listener;

    public interface OnShapeClickListener {
        void onShapeClick(ShapeItem item); // Changed String to ShapeItem to pass the whole object
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
    public void onBindViewHolder(@NonNull ShapeAdapter.ViewHolder holder, int position) {
        // 1. Fixed: Changed 'shapeList' to 'list'
        ShapeItem item = list.get(position);

        // 2. Fixed: Use the names defined in your ViewHolder below
        holder.imgTool.setImageResource(item.getImageResId());
        holder.txtTool.setText(item.getName());

        // 3. Fixed: Changed 'onItemClick' to 'onShapeClick' to match your interface
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShapeClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTool;
        ImageView imgTool; // Added this to hold the logic gate image

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs (txtTool and imgTool) exist in your shape_tool.xml
            txtTool = itemView.findViewById(R.id.txtTool);
            imgTool = itemView.findViewById(R.id.imgTool);
        }
    }
    public void updateList(List<ShapeItem> newList) {
        this.list = newList;
        notifyDataSetChanged(); // This refreshes the UI with the new items
    }
}