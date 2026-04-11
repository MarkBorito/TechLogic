package com.appdev.techlogic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DiagramView extends View {
    private List<GateInstance> gates = new ArrayList<>();
    Paint paint;

    // This one is used when creating the view from code
    public DiagramView(Context context) {
        super(context);
//        init();
    }    // THIS IS THE MISSING ONE! Used when inflating from XML
    public DiagramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
//        init();
    }
    // Call this from Activity
    public void addGate(int resId) {
        // Convert the drawable ID into a Bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);

        // Add a new gate at the center of the screen
        gates.add(new GateInstance(bitmap, getWidth() / 2, getHeight() / 2));

        invalidate(); // Refresh the screen
    }

//    private void init() {
//        paint = new Paint();
//        paint.setColor(0xFF000000);
//        paint.setStrokeWidth(5f);
//        paint.setStyle(Paint.Style.STROKE); // Good practice to set style6888888888
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (GateInstance gate : gates) {
            // Draw the logic gate image at its coordinates
            canvas.drawBitmap(gate.bitmap, gate.x - 100, gate.y - 100, null);
        }    }
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