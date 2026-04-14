package com.appdev.techlogic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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

        EditText etSearch = findViewById(R.id.etSearch);


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
        adapter.setOnMenuClickListener((position, view) -> {
            CardItem clicked = list.get(position);

            PopupMenu popup = new PopupMenu(MainActivity.this, view);
            popup.getMenuInflater().inflate(R.menu.card_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_rename) {
                    showRenameDialog(clicked.title, position);
                    return true;
                } else if (id == R.id.menu_export) {
                    // We will implement export later, for now just a toast
                    exportProject(clicked.title);
                    return true;
                } else if (id == R.id.menu_delete) {
                    showDeleteConfirmDialog(clicked.title, position);
                    return true;
                }
                return false;
            });
            popup.show();
        });
        // Add Long Click to Rename
        adapter.setOnCardLongClickListener(position -> {
            if (position < 0 || position >= list.size()) return;
            CardItem clicked = list.get(position);
            if (clicked.isAddButton) return;

            showRenameDialog(clicked.title, position);
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // FIXED: Changed 'cardAdapter' to 'adapter'
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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
    private void showDeleteConfirmDialog(String title, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete '" + title + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        // 1. Delete from Database
                        db.deleteCard(title);

                        // 2. IMPORTANT: You must update the adapter's data
                        // Calling your existing refresh method
                        refreshList();

                        Toast.makeText(this, "Project deleted", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error deleting project", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void exportProject(String title) {
        // 1. Create a temporary DiagramView
        DiagramView tempView = new DiagramView(this);

        // 2. Load the data from DB into this view
        db.loadDiagram(title, tempView);

        // 3. FORCE MEASURE: This is the missing piece.
        // We give it a large enough space to "exist" so it can calculate its bounding box.
        tempView.measure(
                View.MeasureSpec.makeMeasureSpec(2500, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(2500, View.MeasureSpec.AT_MOST)
        );
        tempView.layout(0, 0, tempView.getMeasuredWidth(), tempView.getMeasuredHeight());

        // 4. Use the export method
        Bitmap bitmap = tempView.exportToBitmap();

        if (bitmap != null) {
            saveBitmapToDisk(bitmap, title);
        } else {
            Toast.makeText(this, "Diagram is empty or could not be rendered!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBitmapToDisk(Bitmap bitmap, String title) {
        android.content.ContentValues values = new android.content.ContentValues();    values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, title + ".png");
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/TechLogic");

        android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w")) {
                java.io.FileOutputStream out = new java.io.FileOutputStream(pfd.getFileDescriptor());
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Toast.makeText(this, "Exported to Pictures/TechLogic", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}