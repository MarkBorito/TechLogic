package com.appdev.techlogic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;

import java.util.List;

public class DiagramActivity extends AppCompatActivity {

    RecyclerView toolRecycler;
    ShapeAdapter shapeAdapter;
    List<ShapeItem> shapeList;
    DiagramView diagramView;
    LinearLayout bottomPanel;
    ImageView btnExpand;
    ImageView btnMenu;
    TextView txtTitle;
    DatabaseHelper dbHelper;
    String currentCardTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagram);

        dbHelper = new DatabaseHelper(this);
        currentCardTitle = getIntent().getStringExtra("card_title");

        toolRecycler = findViewById(R.id.toolRecycler);
        bottomPanel = findViewById(R.id.bottomPanel);
        btnExpand = findViewById(R.id.btnExpand);
        diagramView = findViewById(R.id.diagramView);
        btnMenu = findViewById(R.id.btnMenu);
        txtTitle = findViewById(R.id.txtTitle);

        // Set the header title to match the card name
        if (currentCardTitle != null) {
            txtTitle.setText(currentCardTitle);
            dbHelper.loadDiagram(currentCardTitle, diagramView);
        }

        // Add long press listener to rename from header
        txtTitle.setOnLongClickListener(v -> {
            showRenameDialog();
            return true;
        });

        // Set up the menu button
        btnMenu.setOnClickListener(v -> showPopupMenu(v));

// 🔷 Logic Gate list
        shapeList = new ArrayList<>();
        shapeList.add(new ShapeItem("AND", R.drawable.and));
        shapeList.add(new ShapeItem("OR", R.drawable.or));
        shapeList.add(new ShapeItem("NAND", R.drawable.nand));
        shapeList.add(new ShapeItem("NOR", R.drawable.nor));

// 🔷 Adapter logic
        shapeAdapter = new ShapeAdapter(shapeList, item -> {
            diagramView.addGate(item.getImageResId());
        });

// 🔷 RecyclerView setup
        toolRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        toolRecycler.setAdapter(shapeAdapter);

        // Initialize the BottonSheetBehavior
        BottomSheetBehavior<LinearLayout> behavior = BottomSheetBehavior.from(bottomPanel);

        // Set the click listener to toggle states
        btnExpand.setOnClickListener(v -> {
            if (behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    btnExpand.setImageResource(android.R.drawable.arrow_down_float);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    btnExpand.setImageResource(android.R.drawable.arrow_up_float);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    private void showRenameDialog() {
        if (currentCardTitle == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Project");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText input = viewInflated.findViewById(R.id.input);
        input.setText(currentCardTitle);
        input.setSelectAllOnFocus(true);
        builder.setView(viewInflated);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (newTitle.equals(currentCardTitle)) {
                dialog.dismiss();
            } else {
                dbHelper.updateCardTitle(currentCardTitle, newTitle);
                currentCardTitle = newTitle;
                txtTitle.setText(newTitle);
                Toast.makeText(this, "Renamed to " + newTitle, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.diagram_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_save) {
                saveCurrentDiagram();
                return true;
            } else if (id == R.id.action_delete) {
                confirmDelete();
                return true;
            } else if (id == R.id.action_back) {
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void saveCurrentDiagram() {
        if (currentCardTitle != null) {
            dbHelper.saveDiagram(currentCardTitle, diagramView.getGates(), diagramView.getConnections());
            Toast.makeText(this, "Diagram saved successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error: No project title found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete this project? This will remove all gates and connections.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentCardTitle != null) {
                        dbHelper.deleteCard(currentCardTitle);
                        Toast.makeText(this, "Project deleted.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}