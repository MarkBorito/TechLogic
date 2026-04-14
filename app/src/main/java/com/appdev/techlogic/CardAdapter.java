package com.appdev.techlogic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<CardItem> list;
    private OnAddClickListener addListener;
    private OnCardClickListener cardListener;
    private OnCardLongClickListener longClickListener;
    private List<CardItem> fullList;

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
        this.fullList = new ArrayList<>(list);
    }
    public void filter(String text) {
        list.clear();
        if (text.isEmpty()) {
            list.addAll(fullList);
        } else {
            text = text.toLowerCase();
            for (CardItem item : fullList) {
                // Keep the "Add Card" button (+) and match titles
                if (item.isAddButton || item.title.toLowerCase().contains(text)) {
                    list.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    // When you update the list from the database, update fullList too
    public void updateData(List<CardItem> newList) {
        this.fullList = new ArrayList<>(newList);
        this.list.clear();
        this.list.addAll(newList);
        notifyDataSetChanged();
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
            return new CardViewHolder(view, cardListener, longClickListener, menuListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CardItem item = list.get(position); // MOVED: Put this at the top so it's accessible

        if (holder instanceof AddViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (addListener != null) addListener.onAddClick();
            });
        } else if (holder instanceof CardViewHolder) {
            CardViewHolder cardHolder = (CardViewHolder) holder; // Variable declared here
            cardHolder.txtTitle.setText(item.title);

            // FIX: Keep the thumbnail logic INSIDE the if-else-if block
            if (item.image != null && item.image.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(item.image, 0, item.image.length);
                cardHolder.imgThumbnail.setImageBitmap(bitmap);
                cardHolder.imgThumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                // If no image, show your logo or a default preview
                cardHolder.imgThumbnail.setImageResource(R.drawable.logo);
            }
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
        ImageView imgThumbnail; // ADDED: The ImageView reference

        public CardViewHolder(@NonNull View itemView, OnCardClickListener clickListener, OnCardLongClickListener longListener, OnMenuClickListener menuListener) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail); // ADDED: Find it in XML
            ImageButton btnMenu = itemView.findViewById(R.id.btnMenu);
            btnMenu.setOnClickListener(v -> {
                if (menuListener != null) menuListener.onMenuClick(getAdapterPosition(), v);
            });
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
    public interface OnMenuClickListener {
        void onMenuClick(int position, View view);
    }
    private OnMenuClickListener menuListener;
    public void setOnMenuClickListener(OnMenuClickListener listener) {
        this.menuListener = listener;
    }
}