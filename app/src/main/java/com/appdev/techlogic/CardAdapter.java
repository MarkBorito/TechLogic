package com.appdev.techlogic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<CardItem> list;
    private OnAddClickListener addListener;
    private OnCardClickListener cardListener;

    // Interface for Add button
    public interface OnAddClickListener {
        void onAddClick();
    }

    // Interface for Card clicks
    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    // Constructor with both listeners
    public CardAdapter(List<CardItem> list, OnAddClickListener addListener, OnCardClickListener cardListener) {
        this.list = list;
        this.addListener = addListener;
        this.cardListener = cardListener;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isAddButton ? 0 : 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 0) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_card, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_card, parent, false);
            return new CardViewHolder(view, cardListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof AddViewHolder) {
            holder.itemView.setOnClickListener(v -> addListener.onAddClick());
        } else {
            CardItem item = list.get(position);
            ((CardViewHolder) holder).txtTitle.setText(item.title);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Add Card ViewHolder
    static class AddViewHolder extends RecyclerView.ViewHolder {
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // Normal Card ViewHolder with click listener
    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;

        public CardViewHolder(@NonNull View itemView, OnCardClickListener listener) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);

            // Make the whole card clickable
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCardClick(getAdapterPosition());
                }
            });
        }
    }
}