package com.appdev.techlogic;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CardAdapter adapter;
    List<CardItem> list;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        db = new DatabaseHelper(this); // Initialize SQLite helper

        list = new ArrayList<>();
        list.add(new CardItem("", true)); // Add button always first

        // Load saved cards from database
        List<CardItem> savedCards = db.getAllCards();
        list.addAll(savedCards);

        adapter = new CardAdapter(list,
                () -> { // Add button click
                    String newTitle = generateUniqueTitle();

                    // Save to SQLite
                    db.insertCard(newTitle);

                    // Add to RecyclerView
                    list.add(1, new CardItem(newTitle, false));
                    adapter.notifyItemInserted(1);
                    recyclerView.scrollToPosition(0);
                },
                position -> { // Card click
                    if (position < 0 || position >= list.size()) return;

                    CardItem clicked = list.get(position);
                    if (clicked.isAddButton) return;

                    Intent intent = new Intent(MainActivity.this, DiagramActivity.class);
                    intent.putExtra("card_title", clicked.title);
                    startActivity(intent);
                }
        );

        // Add Long Click to Rename
        adapter.setOnCardLongClickListener(position -> {
            if (position < 0 || position >= list.size()) return;
            CardItem clicked = list.get(position);
            if (clicked.isAddButton) return;

            showRenameDialog(clicked.title, position);
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void showRenameDialog(String oldTitle, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Project");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText input = viewInflated.findViewById(R.id.input);
        input.setText(oldTitle);
        input.setSelectAllOnFocus(true);
        builder.setView(viewInflated);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (newTitle.equals(oldTitle)) {
                dialog.dismiss();
            } else if (isTitleExists(newTitle)) {
                Toast.makeText(this, "Title already exists", Toast.LENGTH_SHORT).show();
            } else {
                db.updateCardTitle(oldTitle, newTitle);
                refreshList();
                Toast.makeText(this, "Renamed to " + newTitle, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private boolean isTitleExists(String title) {
        for (CardItem item : list) {
            if (!item.isAddButton && item.title.equalsIgnoreCase(title)) {
                return true;
            }
        }
        return false;
    }

    private String generateUniqueTitle() {
        Set<String> existingTitles = new HashSet<>();
        for (CardItem item : list) {
            if (!item.isAddButton) {
                existingTitles.add(item.title);
            }
        }

        int i = 1;
        while (true) {
            String candidate = "Card " + i;
            if (!existingTitles.contains(candidate)) {
                return candidate;
            }
            i++;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        list.clear();
        list.add(new CardItem("", true));
        List<CardItem> savedCards = db.getAllCards();
        list.addAll(savedCards);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}