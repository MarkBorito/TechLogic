package com.appdev.techlogic;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.shapes.Shape;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DiagramActivity extends AppCompatActivity {

    RecyclerView toolRecycler;
    ShapeAdapter shapeAdapter;
    DiagramView diagramView;
    LinearLayout bottomPanel;
    ImageView btnExpand,  btnMenu, btnBackToCategories;
    TextView txtTitle, panelTitle;
    DatabaseHelper dbHelper;
    String currentCardTitle;
    List<ShapeItem> categoryList;
    List<ShapeItem> gateList;
    List<ShapeItem> resistorList;
    List<ShapeItem> diodeList;
    List<ShapeItem> labelList;

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
        btnBackToCategories = findViewById(R.id.btnBackToCategories);
        panelTitle = findViewById(R.id.panelTitle);
        ImageView btnUndo = findViewById(R.id.btnUndo);
        ImageView btnRedo = findViewById(R.id.btnRedo);

        if (currentCardTitle != null) {
            txtTitle.setText(currentCardTitle);
            dbHelper.loadDiagram(currentCardTitle, diagramView);
        }
        btnUndo.setOnClickListener(v -> diagramView.undo());
        btnRedo.setOnClickListener(v -> diagramView.redo());

        setupLibrary();

        diagramView.setOnGateLongClickListener(gate -> {
            String lockOption = gate.isLocked ? "Unlock Component" : "Lock Component";
            String[] options = {"Resize", "Copy", "Duplicate", "Group/Ungroup", lockOption, "Delete"};

            new AlertDialog.Builder(this)
                    .setTitle("Gate Options")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: showResizeDialog(gate); break;
                            case 1:
                                diagramView.copyGate(gate);
                                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                                break;
                            case 2: diagramView.duplicateGate(gate); break;
                            case 3: diagramView.toggleGroup(gate); break;
                            case 4:
                                gate.isLocked = !gate.isLocked;
                                String msg = gate.isLocked ? "Component Locked" : "Component Unlocked";
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                                diagramView.invalidate();
                                break;
                            case 5: diagramView.removeGate(gate); break;
                        }
                        dialog.dismiss();
                    })
                    .show();
        });
        diagramView.setOnTextDoubleClickListener(textItem -> {
            showEditLabelDialog(textItem);
        });
        // Inside onCreate, update the LongClickListener logic
        diagramView.setOnTextLongClickListener(textItem -> {
            String[] options = {"Edit Text", "Resize", "Delete"};
            new AlertDialog.Builder(this)
                    .setTitle("Text Options")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showEditLabelDialog(textItem);
                        } else if (which == 1) {
                            showTextResizeDialog(textItem);
                        } else if (which == 2) {
                            diagramView.removeText(textItem);
                        }
                    })
                    .show();
        });

        btnMenu.setOnClickListener(v -> showPopupMenu(v));
        txtTitle.setOnLongClickListener(v -> {
            showRenameDialog();
            return true;
        });
        setupBottomSheet();
    }

    private void showEditLabelDialog(DiagramView.TextInstance textItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Text");

        final EditText input = new EditText(this);
        input.setText(textItem.text);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            textItem.text = input.getText().toString();
            diagramView.invalidate();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void showTextResizeDialog(DiagramView.TextInstance textItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resize Text");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 40, 50, 40);
        final android.widget.SeekBar seekBar = new android.widget.SeekBar(this);

        // Text scale usually starts at 1.0. Let's allow 0.5x to 5.0x
        seekBar.setMax(500);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            seekBar.setMin(50);
        }
        seekBar.setProgress((int)(textItem.scale * 100));

        layout.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
        builder.setView(layout);

        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                textItem.scale = progress / 100.0f;
                diagramView.invalidate();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        });

        builder.setPositiveButton("Done", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showResizeDialog(DiagramView.GateInstance gate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resize Component");

        // Create a simple SeekBar for scaling
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 40, 50, 40);
        final android.widget.SeekBar seekBar = new android.widget.SeekBar(this);

        seekBar.setMax(250);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            seekBar.setMin(10);
        }
        seekBar.setProgress((int)(gate.scale * 1000));

        layout.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
        builder.setView(layout);

        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                gate.scale = progress / 1000.0f;
                diagramView.invalidate();
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        });

        builder.setPositiveButton("Done", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

        private void initializeComponentData() {
        // 1. Create the Main Categories
            categoryList = new ArrayList<>();
            categoryList.add(new ShapeItem("Logic Gates", R.drawable.gate_category, true));
            categoryList.add(new ShapeItem("Resistors", R.drawable.resistors_category, true));
            categoryList.add(new ShapeItem("Diodes", R.drawable.diode_category, true));
            categoryList.add(new ShapeItem("Labels", R.drawable.text, true));
            // 2. Create the Sub-components (Logic Gates)
            gateList = new ArrayList<>();
            gateList.add(new ShapeItem("AND", R.drawable.gate_and, false));
            gateList.add(new ShapeItem("OR", R.drawable.gate_or, false));
            gateList.add(new ShapeItem("NAND", R.drawable.gate_nand, false));
            gateList.add(new ShapeItem("NOR", R.drawable.gate_nor, false));
            gateList.add(new ShapeItem("XOR", R.drawable.gate_xor, false));
            gateList.add(new ShapeItem("NOT", R.drawable.gate_not, false));
            gateList.add(new ShapeItem("XNOR", R.drawable.gate_xnor, false));
            //add all the gates here

            resistorList = new ArrayList<>();
            resistorList.add(new ShapeItem("220 Resistor", R.drawable.resistors_category, false));
            resistorList.add(new ShapeItem("330 Resistor", R.drawable.resistors_category, false));
            resistorList.add(new ShapeItem("470 Resistor", R.drawable.resistors_category, false));
            resistorList.add(new ShapeItem("1000 Resistor", R.drawable.resistors_category, false));
            resistorList.add(new ShapeItem("2200 Resistor", R.drawable.resistors_category, false));
            resistorList.add(new ShapeItem("4700 Resistor", R.drawable.resistors_category, false));
            resistorList.add(new ShapeItem("10000 Resistor", R.drawable.resistors_category, false));

            diodeList = new ArrayList<>();
            diodeList.add(new ShapeItem("Standard Diode", R.drawable.diode_category, false));
            diodeList.add(new ShapeItem("Fast/Schottky Diode", R.drawable.diode_fast, false));
            diodeList.add(new ShapeItem("Zener Diode", R.drawable.diode_zener, false));
            diodeList.add(new ShapeItem("Light Emitting Diode", R.drawable.diode_led, false));

            labelList = new ArrayList<>();
            labelList.add(new ShapeItem("Add Label", R.drawable.text, false));

        }
        private void setupLibrary() {
            initializeComponentData();
            toolRecycler.setLayoutManager(new GridLayoutManager(this, 2));

            shapeAdapter = new ShapeAdapter(categoryList, item -> {
                if (item.isCategory()) {

                    if (item.getName().equals("Logic Gates")) {
                        shapeAdapter.updateList(gateList);
                    } else if (item.getName().equals("Resistors")) {
                        shapeAdapter.updateList(resistorList);
                    } else if (item.getName().equals("Diodes")) {
                        shapeAdapter.updateList(diodeList);
                    } else if (item.getName().equals("Labels")) {
                        shapeAdapter.updateList(labelList);
                    }
                    btnBackToCategories.setVisibility(View.VISIBLE);
                    panelTitle.setText(item.getName());


                    btnBackToCategories.setVisibility(View.VISIBLE);
                    panelTitle.setText(item.getName());
                } else {
                    if (item.getName().equals("Add Label")) {
                        diagramView.addText("Text");
                    } else {
                        diagramView.addGate(item.getImageResId());
                    }
                }
            });
            toolRecycler.setAdapter(shapeAdapter);
            btnBackToCategories.setOnClickListener(v -> {
                shapeAdapter.updateList(categoryList);
                btnBackToCategories.setVisibility(View.GONE);
                panelTitle.setText("Categories");
            });
        }

private void setupBottomSheet() {
    BottomSheetBehavior<LinearLayout> behavior = BottomSheetBehavior.from(bottomPanel);

    behavior.setFitToContents(false);

    behavior.setHalfExpandedRatio(0.5f);

    behavior.setHideable(false);
    btnExpand.setOnClickListener(v -> {

        if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        } else if (behavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {
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
            } else if (!newTitle.equals(currentCardTitle)) {
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
            } else if (id == R.id.action_export) {
                showExportOptions();
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

    private void showExportOptions() {
        String[] options = {"PNG Image", "PDF Document"};
        new AlertDialog.Builder(this)
                .setTitle("Export As")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) exportDiagramAsPng();
                    else if (which == 1) exportDiagramAsPdf();
                })
                .show();
    }

    private void exportDiagramAsPng() {
        Bitmap bitmap = diagramView.exportToBitmap();
        if (bitmap == null) {
            Toast.makeText(this, "Diagram is empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        String filename = (currentCardTitle != null ? currentCardTitle : "diagram") + "_" + System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TechLogic");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(this, "Exported to Pictures/TechLogic", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportDiagramAsPdf() {
        float[] bbox = diagramView.getBoundingBox();
        if (bbox == null) {
            Toast.makeText(this, "Diagram is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        int width = (int) (bbox[2] - bbox[0]);
        int height = (int) (bbox[3] - bbox[1]);

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        canvas.drawColor(android.graphics.Color.WHITE);
        diagramView.drawDiagram(canvas, bbox[0], bbox[1]);

        document.finishPage(page);

        String filename = (currentCardTitle != null ? currentCardTitle : "diagram") + "_" + System.currentTimeMillis() + ".pdf";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TechLogic");

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                document.writeTo(out);
                Toast.makeText(this, "Exported to Downloads/TechLogic", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
        document.close();
    }

    private void saveCurrentDiagram() {
        if (currentCardTitle != null) {
            Bitmap preview = diagramView.getThumbnail();
            dbHelper.saveDiagram(currentCardTitle, diagramView.getGates(), diagramView.getConnections(), diagramView.getTexts(), preview);
            Toast.makeText(this, "Diagram saved successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete this project?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentCardTitle != null) {
                        dbHelper.deleteCard(currentCardTitle);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}