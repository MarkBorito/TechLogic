package com.appdev.techlogic;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

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

// 🔷 Shape list
        shapeList = new ArrayList<>();
        shapeList.add(new ShapeItem("RECT"));
        shapeList.add(new ShapeItem("CIRCLE"));
        shapeList.add(new ShapeItem("AND"));
        shapeList.add(new ShapeItem("OR"));

// 🔷 Adapter
        shapeAdapter = new ShapeAdapter(shapeList, type -> {
            diagramView.addShape(type); // 🔥 send shape to canvas
        });

// 🔷 RecyclerView setup
        toolRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        toolRecycler.setAdapter(shapeAdapter);

// 🔽 Expand / Collapse
        btnExpand.setOnClickListener(v -> {
            if (isExpanded) {
                bottomPanel.getLayoutParams().height = 120;
                toolRecycler.setVisibility(View.GONE);
            } else {
                bottomPanel.getLayoutParams().height = 800;
                toolRecycler.setVisibility(View.VISIBLE);
            }
            bottomPanel.requestLayout();
            isExpanded = !isExpanded;
        });
    }
}