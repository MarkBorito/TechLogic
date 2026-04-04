package com.appdev.techlogic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class DiagramView extends View {

    Paint paint;

    public DiagramView(Context context) {
        super(context);

        paint = new Paint();
        paint.setColor(0xFF000000);
        paint.setStrokeWidth(5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(100, 100, 300, 300, paint);
    }

    // 🔥 this is needed for your adapter
    public void addShape(String type) {
        // temporary
        invalidate();
    }
}