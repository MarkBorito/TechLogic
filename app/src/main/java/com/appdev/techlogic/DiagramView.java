package com.appdev.techlogic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DiagramView extends View {
    private List<GateInstance> gates = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();
    private Paint gridPaint;
    private Paint linePaint;
    private Paint selectedPaint;
    private Paint arrowPaint;
    private int gridSize = 60; // Size of each grid square

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    private float mScaleFactor = 1.f;
    private float mPosX = 0;
    private float mPosY = 0;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mDownX;
    private float mDownY;
    private float mCurrentX;
    private float mCurrentY;
    private int mActivePointerId = -1;

    private GateInstance mSelectedGate = null;
    private GateInstance connectionStartGate = null;

    public DiagramView(Context context) {
        super(context);
        init(context);
    }

    public DiagramView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public interface OnGateLongClickListener {
        void onLongClick(GateInstance gate);
    }
    private OnGateLongClickListener longClickListener;
    public void setOnGateLongClickListener(OnGateLongClickListener listener) {
        this.longClickListener = listener;
    }

    private void init(Context context) {
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        selectedPaint = new Paint();
        selectedPaint.setColor(Color.YELLOW);
        selectedPaint.setStrokeWidth(5f);
        selectedPaint.setStyle(Paint.Style.STROKE);

        arrowPaint = new Paint();
        arrowPaint.setColor(Color.BLACK);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                float diagramX = (e.getX() - mPosX) / mScaleFactor;
                float diagramY = (e.getY() - mPosY) / mScaleFactor;

                // Check for gate first
                GateInstance gate = findGateAt(diagramX, diagramY);
                if (gate != null && longClickListener != null) {
                    longClickListener.onLongClick(gate);
                }

                // Then check for connections
                Connection conn = findConnectionAt(diagramX, diagramY);
                if (conn != null) {
                    connections.remove(conn);
                    invalidate();
                }
            }
        });
    }

    // Correct method signature in DiagramView.java
    public void removeGate(GateInstance gate) {
        gates.remove(gate);
        Iterator<Connection> it = connections.iterator();
        while (it.hasNext()) {
            Connection conn = it.next();
            if (conn.start == gate || conn.end == gate) {
                it.remove();
            }
        }
        if (connectionStartGate == gate) connectionStartGate = null;
        invalidate();
    }

    private Connection findConnectionAt(float x, float y) {
        float threshold = 30 / mScaleFactor;
        for (Connection conn : connections) {
            // Update these to match the scaled edges used in onDraw
            float x1 = conn.start.x - 100 + (conn.start.bitmap.getWidth() * conn.start.scale);
            float y1 = conn.start.y - 100 + (conn.start.bitmap.getHeight() * conn.start.scale / 2f);

            float x2 = conn.end.x - 100;
            float y2 = conn.end.y - 100 + (conn.end.bitmap.getHeight() * conn.end.scale / 2f);

            float midX = (x1 + x2) / 2;

            if (distToSegment(x, y, x1, y1, midX, y1) < threshold ||
                    distToSegment(x, y, midX, y1, midX, y2) < threshold ||
                    distToSegment(x, y, midX, y2, x2, y2) < threshold) {
                return conn;
            }
        }
        return null;
    }

    private double distToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float l2 = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
        if (l2 == 0.0) return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        float t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));
        return Math.sqrt((px - (x1 + t * (x2 - x1))) * (px - (x1 + t * (x2 - x1))) +
                         (py - (y1 + t * (y2 - y1))) * (py - (y1 + t * (y2 - y1))));
    }

    public void addGate(int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        float centerX = (getWidth() / 2f - mPosX) / mScaleFactor;
        float centerY = (getHeight() / 2f - mPosY) / mScaleFactor;
        gates.add(new GateInstance(bitmap, centerX, centerY, resId));
        invalidate();
    }

    public GateInstance createGateFromLoad(int resId, float x, float y, float scale) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        GateInstance gate = new GateInstance(bitmap, x, y, resId);
        gate.scale = scale; // Set the saved scale
        return gate;
    }

    public void setLoadedData(List<GateInstance> gates, List<Connection> connections) {
        this.gates = gates;
        this.connections = connections;
        invalidate();
    }

    public List<GateInstance> getGates() { return gates; }
    public List<Connection> getConnections() { return connections; }

    public float[] getBoundingBox() {
        if (gates.isEmpty()) return null;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (GateInstance gate : gates) {
            float left = gate.x - 100;
            float top = gate.y - 100;
            float right = left + (gate.bitmap.getWidth() * gate.scale);
            float bottom = top + (gate.bitmap.getHeight() * gate.scale);

            if (left < minX) minX = left;
            if (top < minY) minY = top;
            if (right > maxX) maxX = right;
            if (bottom > maxY) maxY = bottom;
        }

        // Add padding
        float padding = 50;
        return new float[]{minX - padding, minY - padding, maxX + padding, maxY + padding};
    }

    public void drawDiagram(Canvas canvas, float offsetX, float offsetY) {
        canvas.save();
        canvas.translate(-offsetX, -offsetY);

        // Draw connections
        for (Connection conn : connections) {
            float x1 = conn.start.x - 100 + (conn.start.bitmap.getWidth() * conn.start.scale);
            float y1 = conn.start.y - 100 + (conn.start.bitmap.getHeight() * conn.start.scale / 2f);
            float x2 = conn.end.x - 100;
            float y2 = conn.end.y - 100 + (conn.end.bitmap.getHeight() * conn.end.scale / 2f);
            drawOrthogonalLine(canvas, x1, y1, x2, y2, linePaint);
        }

        // Draw gates
        for (GateInstance gate : gates) {
            float scaledW = gate.bitmap.getWidth() * gate.scale;
            float scaledH = gate.bitmap.getHeight() * gate.scale;
            android.graphics.RectF destRect = new android.graphics.RectF(
                    gate.x - 100, gate.y - 100,
                    gate.x - 100 + scaledW, gate.y - 100 + scaledH);
            canvas.drawBitmap(gate.bitmap, null, destRect, null);
        }
        canvas.restore();
    }

    public Bitmap exportToBitmap() {
        float[] bbox = getBoundingBox();
        if (bbox == null) return null;

        int width = (int) (bbox[2] - bbox[0]);
        int height = (int) (bbox[3] - bbox[1]);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        drawDiagram(canvas, bbox[0], bbox[1]);

        return bitmap;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mScaleDetector.onTouchEvent(ev);
        mGestureDetector.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mLastTouchX = x;
                mLastTouchY = y;
                mDownX = x;
                mDownY = y;
                mActivePointerId = ev.getPointerId(0);
                float diagramX = (x - mPosX) / mScaleFactor;
                float diagramY = (y - mPosY) / mScaleFactor;
                mCurrentX = diagramX;
                mCurrentY = diagramY;
                mSelectedGate = findGateAt(diagramX, diagramY);
                invalidate();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex != -1) {
                    final float x = ev.getX(pointerIndex);
                    final float y = ev.getY(pointerIndex);
                    mCurrentX = (x - mPosX) / mScaleFactor;
                    mCurrentY = (y - mPosY) / mScaleFactor;
                    if (!mScaleDetector.isInProgress()) {
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;
                        if (mSelectedGate != null) {
                            mSelectedGate.x += dx / mScaleFactor;
                            mSelectedGate.y += dy / mScaleFactor;
                        } else {
                            mPosX += dx;
                            mPosY += dy;
                        }
                    }
                    mLastTouchX = x;
                    mLastTouchY = y;
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                final float x = ev.getX();
                final float y = ev.getY();
                if (Math.abs(x - mDownX) < 10 && Math.abs(y - mDownY) < 10) {
                    float diagramX = (x - mPosX) / mScaleFactor;
                    float diagramY = (y - mPosY) / mScaleFactor;
                    GateInstance tappedGate = findGateAt(diagramX, diagramY);
                    if (tappedGate != null) {
                        if (connectionStartGate == null) {
                            connectionStartGate = tappedGate;
                        } else if (connectionStartGate != tappedGate) {
                            connections.add(new Connection(connectionStartGate, tappedGate));
                            connectionStartGate = null;
                        } else {
                            connectionStartGate = null;
                        }
                    } else {
                        connectionStartGate = null;
                    }
                }
                mActivePointerId = -1;
                mSelectedGate = null;
                performClick();
                invalidate();
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = -1;
                mSelectedGate = null;
                invalidate();
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
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
        for (int i = gates.size() - 1; i >= 0; i--) {
            GateInstance gate = gates.get(i);
            float left = gate.x - 100;
            float top = gate.y - 100;

            // Use the scale here too!
            float right = left + (gate.bitmap.getWidth() * gate.scale);
            float bottom = top + (gate.bitmap.getHeight() * gate.scale);

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
        for (Connection conn : connections) {
            // Calculate the actual width/height based on scale
            float startW = conn.start.bitmap.getWidth() * conn.start.scale;
            float startH = conn.start.bitmap.getHeight() * conn.start.scale;

            float endW = conn.end.bitmap.getWidth() * conn.end.scale;
            float endH = conn.end.bitmap.getHeight() * conn.end.scale;

            // START POINT: The Right Edge of the start gate, vertically centered
            float x1 = (conn.start.x - 100) + startW;
            float y1 = (conn.start.y - 100) + (startH / 2f);

            // END POINT: The Left Edge of the end gate, vertically centered
            float x2 = (conn.end.x - 100);
            float y2 = (conn.end.y - 100) + (endH / 2f);

            drawOrthogonalLine(canvas, x1, y1, x2, y2, linePaint);
        }
        if (connectionStartGate != null && mSelectedGate == null) {
            // Start at the scaled right edge
            float x1 = connectionStartGate.x - 100 + (connectionStartGate.bitmap.getWidth() * connectionStartGate.scale);
            float y1 = connectionStartGate.y - 100 + (connectionStartGate.bitmap.getHeight() * connectionStartGate.scale / 2f);

            // Draw to current finger position (mCurrentX, mCurrentY)
            drawOrthogonalLine(canvas, x1, y1, mCurrentX, mCurrentY, linePaint);
        }
        // Inside DiagramView.java -> onDraw()
        for (GateInstance gate : gates) {
            // 1. Calculate the scaled width and height
            float scaledW = gate.bitmap.getWidth() * gate.scale;
            float scaledH = gate.bitmap.getHeight() * gate.scale;

            // 2. Create the destination rectangle (where the image will be stretched to)
            // We subtract 100 because your code seems to use a -100 offset for centering
            android.graphics.RectF destRect = new android.graphics.RectF(
                    gate.x - 100,
                    gate.y - 100,
                    gate.x - 100 + scaledW,
                    gate.y - 100 + scaledH
            );

            // 3. Draw the bitmap INTO that rectangle
            // The 'null' for the second parameter means "use the whole source image"
            canvas.drawBitmap(gate.bitmap, null, destRect, null);

            // Optional: Update your selection box logic if you have one
            if (gate == connectionStartGate) {
                canvas.drawRect(destRect, selectedPaint);
            }
        }
        canvas.restore();
    }

    private void drawOrthogonalLine(Canvas canvas, float x1, float y1, float x2, float y2, Paint paint) {
        Path path = new Path();
        path.moveTo(x1, y1);
        float midX = (x1 + x2) / 2;
        path.lineTo(midX, y1);
        path.lineTo(midX, y2);
        path.lineTo(x2, y2);
        canvas.drawPath(path, paint);
        float arrowSize = 15;
        Path arrowPath = new Path();
        arrowPath.moveTo(x2, y2);
        if (x2 > x1) {
            arrowPath.lineTo(x2 - arrowSize, y2 - arrowSize / 2);
            arrowPath.lineTo(x2 - arrowSize, y2 + arrowSize / 2);
        } else {
            arrowPath.lineTo(x2 + arrowSize, y2 - arrowSize / 2);
            arrowPath.lineTo(x2 + arrowSize, y2 + arrowSize / 2);
        }
        arrowPath.close();
        canvas.drawPath(arrowPath, arrowPaint);
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

    public static class GateInstance {
        Bitmap bitmap;
        float x, y;
        int resId;
        float scale = 1.0f;

        GateInstance(Bitmap b, float x, float y, int resId) {
            this.bitmap = b;
            this.x = x;
            this.y = y;
            this.resId = resId;
            this.scale = 1.0f;
        }
    }

    public static class Connection {
        GateInstance start;
        GateInstance end;

        Connection(GateInstance start, GateInstance end) {
            this.start = start;
            this.end = end;
        }
    }
}