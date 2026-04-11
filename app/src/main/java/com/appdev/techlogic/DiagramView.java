package com.appdev.techlogic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DiagramView extends View {
    private List<GateInstance> gates = new ArrayList<>();
    private Paint gridPaint;
    private int gridSize = 60; // Size of each grid square

    public DiagramView(Context context) {
        super(context);
        init();
    }

    public DiagramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);
    }

    public void addGate(int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        // Add a new gate at the center of the screen
        gates.add(new GateInstance(bitmap, getWidth() / 2, getHeight() / 2));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
        for (GateInstance gate : gates) {
            // Draw the logic gate image at its coordinates
            canvas.drawBitmap(gate.bitmap, gate.x - 100, gate.y - 100, null);
        }
    }

    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        // Vertical lines
        for (int x = 0; x <= width; x += gridSize) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }

        // Horizontal lines
        for (int y = 0; y <= height; y += gridSize) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }
    }

    private static class GateInstance {
        Bitmap bitmap;
        float x, y;

        GateInstance(Bitmap b, float x, float y) {
            this.bitmap = b;
            this.x = x;
            this.y = y;
        }
    }
}
