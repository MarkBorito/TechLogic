package com.appdev.techlogic;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<CardItem> list;
    private OnAddClickListener addListener;
    private OnCardClickListener cardListener;
    private OnCardLongClickListener longClickListener;

    public interface OnAddClickListener {
        void onAddClick();
    }

    public interface OnCardClickListener {
        void onCardClick(int position);
    }

    public interface OnCardLongClickListener {
        void onCardLongClick(int position);
    }

    public CardAdapter(List<CardItem> list, OnAddClickListener addListener, OnCardClickListener cardListener) {
        this.list = list;
        this.addListener = addListener;
        this.cardListener = cardListener;
    }

    public void setOnCardLongClickListener(OnCardLongClickListener listener) {
        this.longClickListener = listener;
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
            return new CardViewHolder(view, cardListener, longClickListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (addListener != null) addListener.onAddClick();
            });
        } else if (holder instanceof CardViewHolder) {
            CardItem item = list.get(position);
            CardViewHolder cardHolder = (CardViewHolder) holder;
            cardHolder.txtTitle.setText(item.title);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class AddViewHolder extends RecyclerView.ViewHolder {
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;

        public CardViewHolder(@NonNull View itemView, OnCardClickListener clickListener, OnCardLongClickListener longListener) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCardClick(getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longListener != null) {
                    longListener.onCardLongClick(getAdapterPosition());
                    return true;
                }
                return false;
            });
        }
    }
}