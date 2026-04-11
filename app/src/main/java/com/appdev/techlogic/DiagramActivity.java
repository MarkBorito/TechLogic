package com.appdev.techlogic;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
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
    boolean isExpanded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagram);

        toolRecycler = findViewById(R.id.toolRecycler);
        bottomPanel = findViewById(R.id.bottomPanel);
        btnExpand = findViewById(R.id.btnExpand);
        diagramView = findViewById(R.id.diagramView);

        String title = getIntent().getStringExtra("card_title");

// 🔷 Logic Gate list
        shapeList = new ArrayList<>();
        shapeList.add(new ShapeItem("AND", R.drawable.and));
        shapeList.add(new ShapeItem("OR", R.drawable.or));
        shapeList.add(new ShapeItem("NAND", R.drawable.nand));
        shapeList.add(new ShapeItem("NOR", R.drawable.nor));

// 🔷 Adapter logic (No change needed here if you update addShape)
        shapeAdapter = new ShapeAdapter(shapeList, item -> {
            diagramView.addGate(item.getImageResId()); // Send the image ID to the canvas
        });

// 🔷 RecyclerView setup
        toolRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        toolRecycler.setAdapter(shapeAdapter);

        // Initialize the BottonSheetBehavior
        BottomSheetBehavior<LinearLayout> behavior = BottomSheetBehavior.from(bottomPanel);

        // 2. Set the click listener to toggle states
        btnExpand.setOnClickListener(v -> {
            if (behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        // 3. (Optional) Change the arrow icon when sliding
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    btnExpand.setImageResource(android.R.drawable.arrow_down_float); // Point down when open
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    btnExpand.setImageResource(android.R.drawable.arrow_up_float);   // Point up when closed
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // You can animate things here while the user is dragging
            }
        });
    }
}