package com.appdev.techlogic;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

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
                    // Add this check to prevent IndexOutOfBounds
                    if (position < 0 || position >= list.size()) return;

                    CardItem clicked = list.get(position);

                    if (clicked.isAddButton) return;

                    // This Toast confirms the code is reaching this point
                    Toast.makeText(this, "Opening: " + clicked.title, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this, DiagramActivity.class);
                    intent.putExtra("card_title", clicked.title);
                    startActivity(intent);
                }
        );

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
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
        // Refresh the list when coming back from DiagramActivity (in case of delete)
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