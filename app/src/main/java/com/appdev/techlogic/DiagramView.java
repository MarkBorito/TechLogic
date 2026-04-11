package com.appdev.techlogic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DiagramView extends View {
    private List<GateInstance> gates = new ArrayList<>();
    private Paint gridPaint;
    private int gridSize = 60; // Size of each grid square

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private float mPosX = 0;
    private float mPosY = 0;
    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId = -1;

    private GateInstance mSelectedGate = null;

    public DiagramView(Context context) {
        super(context);
        init(context);
    }

    public DiagramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void addGate(int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        // Add a new gate at the center of the current view, accounting for scale and pan
        float centerX = (getWidth() / 2f - mPosX) / mScaleFactor;
        float centerY = (getHeight() / 2f - mPosY) / mScaleFactor;
        gates.add(new GateInstance(bitmap, centerX, centerY));
        invalidate();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);

                // Check if we touched a gate
                float diagramX = (x - mPosX) / mScaleFactor;
                float diagramY = (y - mPosY) / mScaleFactor;
                mSelectedGate = findGateAt(diagramX, diagramY);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex != -1) {
                    final float x = ev.getX(pointerIndex);
                    final float y = ev.getY(pointerIndex);

                    // Only move if the ScaleGestureDetector isn't processing a gesture.
                    if (!mScaleDetector.isInProgress()) {
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;

                        if (mSelectedGate != null) {
                            // Drag the gate (adjust by scale factor)
                            mSelectedGate.x += dx / mScaleFactor;
                            mSelectedGate.y += dy / mScaleFactor;
                        } else {
                            // Pan the whole view
                            mPosX += dx;
                            mPosY += dy;
                        }

                        invalidate();
                    }

                    mLastTouchX = x;
                    mLastTouchY = y;
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = -1;
                mSelectedGate = null;
                performClick();
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = -1;
                mSelectedGate = null;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    private GateInstance findGateAt(float x, float y) {
        // Iterate backwards to select the topmost gate
        for (int i = gates.size() - 1; i >= 0; i--) {
            GateInstance gate = gates.get(i);
            // Gates are drawn at gate.x - 100, gate.y - 100
            // Assuming default size is roughly 200x200 based on the offset
            float halfWidth = gate.bitmap.getWidth() / 2f;
            float halfHeight = gate.bitmap.getHeight() / 2f;
            
            // Adjusting hit detection based on how they are drawn:
            // canvas.drawBitmap(gate.bitmap, gate.x - 100, gate.y - 100, null);
            // So the center is (gate.x - 100 + halfWidth, gate.y - 100 + halfHeight) ? 
            // Wait, the previous code used: canvas.drawBitmap(gate.bitmap, gate.x - 100, gate.y - 100, null);
            // This usually means the user intended gate.x/y to be the center, offset by 100.
            
            float left = gate.x - 100;
            float top = gate.y - 100;
            float right = left + gate.bitmap.getWidth();
            float bottom = top + gate.bitmap.getHeight();

            if (x >= left && x <= right && y >= top && y <= bottom) {
                return gate;
            }
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(mPosX, mPosY);
        canvas.scale(mScaleFactor, mScaleFactor);

        drawGrid(canvas);
        for (GateInstance gate : gates) {
            // Draw the logic gate image at its coordinates
            canvas.drawBitmap(gate.bitmap, gate.x - 100, gate.y - 100, null);
        }

        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        int width = 5000; 
        int height = 5000;

        for (int x = -width; x <= width; x += gridSize) {
            canvas.drawLine(x, -height, x, height, gridPaint);
        }

        for (int y = -height; y <= height; y += gridSize) {
            canvas.drawLine(-width, y, width, y, gridPaint);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float prevScale = mScaleFactor;
            mScaleFactor *= scaleFactor;

            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            mPosX -= (focusX - mPosX) * (mScaleFactor / prevScale - 1);
            mPosY -= (focusY - mPosY) * (mScaleFactor / prevScale - 1);

            invalidate();
            return true;
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
