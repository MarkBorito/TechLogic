package com.appdev.techlogic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DiagramView extends View {

    Paint paint;

    // This one is used when creating the view from code
    public DiagramView(Context context) {
        super(context);
        init();
    }    // THIS IS THE MISSING ONE! Used when inflating from XML
    public DiagramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFF000000);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE); // Good practice to set style6888888888
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(100, 100, 300, 300, paint);
    }

    public void addShape(String type) {
        invalidate();
    }
}