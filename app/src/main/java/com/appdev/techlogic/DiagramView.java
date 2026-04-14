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
import java.util.Stack;

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
    private List<TextInstance> texts = new ArrayList<>();
    private TextInstance mSelectedText = null;
    private OnTextLongClickListener textLongClickListener;
    private OnTextDoubleClickListener textDoubleClickListener;
    private Stack<DiagramState> undoStack = new Stack<>();
    private Stack<DiagramState> redoStack = new Stack<>();
    private GateInstance clipboardGate = null;
    private List<List<GateInstance>> groups = new ArrayList<>();
    private List<GateInstance> multiSelectBuffer = new ArrayList<>();

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
                saveState();
                float diagramX = (e.getX() - mPosX) / mScaleFactor;
                float diagramY = (e.getY() - mPosY) / mScaleFactor;
                TextInstance clickedText = findTextAt(diagramX, diagramY);
                if (clickedText != null) {
                    if (textLongClickListener != null) {
                        textLongClickListener.onTextLongClick(clickedText);
                        return;
                    }
                }


                // Check for gate first
                GateInstance gate = findGateAt(diagramX, diagramY);
                if (gate != null && longClickListener != null) {
                    longClickListener.onLongClick(gate);
                    return;
                }

                // Then check for connections
                Connection conn = findConnectionAt(diagramX, diagramY);
                if (conn != null) {
                    saveState();
                    connections.remove(conn);
                    invalidate();
                    return;
                }
                if (gate == null && clickedText == null) {
                    showPasteMenu();
                }
            }

            public boolean onDoubleTap(MotionEvent e) {
                float diagramX = (e.getX() - mPosX) / mScaleFactor;
                float diagramY = (e.getY() - mPosY) / mScaleFactor;

                for (TextInstance t : texts) {
                    if (diagramX >= t.x && diagramX <= t.x + 200 && diagramY >= t.y - 50 && diagramY <= t.y) {
                        if (textDoubleClickListener != null) {
                            textDoubleClickListener.onTextDoubleClick(t);
                            return true;
                        }
                    }
                }
                return false;
            }
        });

    }

    public void saveState() {
        undoStack.push(new DiagramState(gates, connections, texts));
        redoStack.clear(); // Clear redo history when a new action is performed
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(new DiagramState(gates, connections, texts));
            DiagramState previous = undoStack.pop();
            this.gates = previous.gates;
            this.connections = previous.connections;
            this.texts = previous.texts;
            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new DiagramState(gates, connections, texts));
            DiagramState next = redoStack.pop();
            this.gates = next.gates;
            this.connections = next.connections;
            this.texts = next.texts;
            invalidate();
        }
    }

    // Correct method signature in DiagramView.java
    public void removeGate(GateInstance gate) {
        saveState();
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
        saveState();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        float centerX = (getWidth() / 2f - mPosX) / mScaleFactor;
        float centerY = (getHeight() / 2f - mPosY) / mScaleFactor;
        GateInstance newGate = new GateInstance(bitmap, centerX, centerY, resId);
        newGate.scale = 0.0625f;
        gates.add(newGate);
        invalidate();
    }

    public GateInstance createGateFromLoad(int resId, float x, float y, float scale) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        GateInstance gate = new GateInstance(bitmap, x, y, resId);
        gate.scale = scale; // Set the saved scale
        return gate;
    }

    public void setLoadedData(List<GateInstance> gates, List<Connection> connections, List<TextInstance> loadedTexts) {
        this.gates = gates;
        this.connections = connections;
        this.texts = loadedTexts;
        invalidate();
    }

    public List<GateInstance> getGates() { return gates; }
    public List<Connection> getConnections() { return connections; }
    public List<TextInstance> getTexts() { return texts; }

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
        for (TextInstance t : texts) {
            if (t.x < minX) minX = t.x;
            if (t.y - 50 < minY) minY = t.y - 50;
            if (t.x + 200 > maxX) maxX = t.x + 200;
            if (t.y > maxY) maxY = t.y;
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

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);
        for (TextInstance t : texts) {
            textPaint.setTextSize(50f * t.scale);
            canvas.drawText(t.text, t.x, t.y, textPaint);
        }
        canvas.restore();
    }

    public Bitmap exportToBitmap() {
        float[] bbox = getBoundingBox();
        if (bbox == null) return null;

        int width = (int) (bbox[2] - bbox[0]) + 100;
        int height = (int) (bbox[3] - bbox[1]) + 100;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        // This now draws gates, connections, AND text correctly offset!
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
                mSelectedText = findTextAt(diagramX, diagramY);
                if (mSelectedText == null) {
                    mSelectedGate = findGateAt(diagramX, diagramY);
                }
                if (mSelectedGate == null) {
                    for (TextInstance t : texts) {
                        if (diagramX >= t.x && diagramX <= t.x + 200 && diagramY >= t.y - 50 && diagramY <= t.y) {
                            mSelectedText = t; // You'll need to declare 'private TextInstance mSelectedText' at the top
                            break;
                        }
                    }
                }
                invalidate();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);if (pointerIndex != -1) {
                    final float x = ev.getX(pointerIndex);
                    final float y = ev.getY(pointerIndex);

                    // Calculate how much the finger moved since the last frame
                    float dx = (x - mLastTouchX) / mScaleFactor;
                    float dy = (y - mLastTouchY) / mScaleFactor;

                    // Current diagram coordinates for drawing the "in-progress" connection line
                    mCurrentX = (x - mPosX) / mScaleFactor;
                    mCurrentY = (y - mPosY) / mScaleFactor;

                    if (mSelectedGate != null && !mScaleDetector.isInProgress()) {
                        // Check if the gate belongs to a group
                        List<GateInstance> group = findGroupFor(mSelectedGate);
                        if (group != null) {
                            // Move every gate in the group smoothly
                            for (GateInstance g : group) {
                                if (!g.isLocked) {
                                    g.x += dx;
                                    g.y += dy;
                                }
                            }
                        } else {
                            // Move single gate smoothly
                            if (!mSelectedGate.isLocked) {
                                mSelectedGate.x += dx;
                                mSelectedGate.y += dy;
                            }
                        }

                        // GUIDES REMOVED: We no longer set guideLineX/Y or loop through neighbors here

                    } else if (mSelectedText != null) {
                        // Move text smoothly
                        mSelectedText.x += dx;
                        mSelectedText.y += dy;
                    } else if (!mScaleDetector.isInProgress()) {
                        // Panning logic (moving the whole canvas)
                        mPosX += x - mLastTouchX;
                        mPosY += y - mLastTouchY;
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
                float moveDist = (float) Math.sqrt(Math.pow(ev.getX() - mDownX, 2) + Math.pow(ev.getY() - mDownY, 2));
                if (Math.abs(x - mDownX) < 10 && Math.abs(y - mDownY) < 10) {
                    float diagramX = (x - mPosX) / mScaleFactor;
                    float diagramY = (y - mPosY) / mScaleFactor;
                    GateInstance tappedGate = findGateAt(diagramX, diagramY);
                    if (tappedGate != null) {
                        if (connectionStartGate == null) {
                            connectionStartGate = tappedGate;
                        } else if (connectionStartGate != tappedGate) {
                            saveState();
                            connections.add(new Connection(connectionStartGate, tappedGate));
                            connectionStartGate = null;
                        } else {
                            connectionStartGate = null;
                        }
                    } else {
                        connectionStartGate = null;
                    }
                }
                if (moveDist >= 10) {
                    // It was a drag, so reset connection state
                    connectionStartGate = null;
                }
                mActivePointerId = -1;
                mSelectedGate = null;
                mSelectedText = null;
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
        // Inside onDraw, before drawing the gates
        Paint groupPaint = new Paint();
        groupPaint.setColor(Color.BLUE);
        groupPaint.setStyle(Paint.Style.STROKE);
        groupPaint.setStrokeWidth(2f);
        groupPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 10}, 0));

        for (List<GateInstance> group : groups) {
            if (group.isEmpty()) continue;
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

            for (GateInstance g : group) {
                float left = g.x - 100;
                float top = g.y - 100;
                float right = left + (g.bitmap.getWidth() * g.scale);
                float bottom = top + (g.bitmap.getHeight() * g.scale);
                if (left < minX) minX = left;
                if (top < minY) minY = top;
                if (right > maxX) maxX = right;
                if (bottom > maxY) maxY = bottom;
            }
            // Draw a rectangle with a little padding around the group
            canvas.drawRect(minX - 10, minY - 10, maxX + 10, maxY + 10, groupPaint);
        }
        // Inside DiagramView.java -> onDraw()
        for (GateInstance gate : gates) {
            // 1. Calculate the scaled width and height
            float scaledW = gate.bitmap.getWidth() * gate.scale;
            float scaledH = gate.bitmap.getHeight() * gate.scale;
            if (gate.isLocked) {
                Paint lockPaint = new Paint();
                lockPaint.setColor(Color.RED);
                lockPaint.setTextSize(30f);
                // Draw a small "L" or a red dot at the corner
                canvas.drawText("🔒", gate.x - 90, gate.y - 70, lockPaint);
            }
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
        for (TextInstance t : texts) {
            t.paint.setTextSize(50f * t.scale);
            canvas.drawText(t.text, t.x, t.y, t.paint);
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
        float scale;
        public boolean isLocked = false;

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
    public static class TextInstance {
        public String text;
        public float x, y;
        public float scale = 1.0f;
        public Paint paint;

        public TextInstance(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.paint = new Paint();
            this.paint.setColor(Color.BLACK);
            this.paint.setTextSize(50f); // Base size
            this.paint.setAntiAlias(true);
        }
    }
    public interface OnTextDoubleClickListener {
        void onTextDoubleClick(TextInstance text);
    }
    public interface OnTextLongClickListener {
        void onTextLongClick(TextInstance text);
    }
    public void setOnTextDoubleClickListener(OnTextDoubleClickListener listener) {
        this.textDoubleClickListener = listener;
    }
    public void setOnTextLongClickListener(OnTextLongClickListener listener) {
        this.textLongClickListener = listener;
    }
    public void addText(String content) {
        saveState();
        float centerX = (getWidth() / 2f - mPosX) / mScaleFactor;
        float centerY = (getHeight() / 2f - mPosY) / mScaleFactor;
        texts.add(new TextInstance(content, centerX, centerY));
        invalidate();
    }
    public void removeText(TextInstance text) {
        saveState();
        texts.remove(text);
        invalidate();
    }
    private TextInstance findTextAt(float x, float y) {
        for (TextInstance t : texts) {
            // We create a bounding box:
            // Left: t.x, Right: t.x + 300 (approx), Top: t.y - 60, Bottom: t.y + 20
            if (x >= t.x && x <= t.x + 300 && y >= t.y - 60 && y <= t.y + 20) {
                return t;
            }
        }
        return null;
    }
    private static class DiagramState {
        List<GateInstance> gates;
        List<Connection> connections;
        List<TextInstance> texts;

        DiagramState(List<GateInstance> g, List<Connection> c, List<TextInstance> t) {
            // Deep copy gates
            this.gates = new ArrayList<>();
            for (GateInstance gate : g) {
                GateInstance copy = new GateInstance(gate.bitmap, gate.x, gate.y, gate.resId);
                copy.scale = gate.scale;
                this.gates.add(copy);
            }
            // Deep copy texts
            this.texts = new ArrayList<>();
            for (TextInstance text : t) {
                TextInstance copy = new TextInstance(text.text, text.x, text.y);
                copy.scale = text.scale;
                this.texts.add(copy);
            }
            // Copy connections (references to the NEW gate copies)
            this.connections = new ArrayList<>();
            for (Connection conn : c) {
                // Find the index of the original gates to link the new copies correctly
                int startIndex = g.indexOf(conn.start);
                int endIndex = g.indexOf(conn.end);
                if (startIndex != -1 && endIndex != -1) {
                    this.connections.add(new Connection(this.gates.get(startIndex), this.gates.get(endIndex)));
                }
            }
        }
    }
    public void copyGate(GateInstance gate) {
        this.clipboardGate = gate;
    }
    public void duplicateGate(GateInstance gate) {
        saveState();
        // Create a new instance offset slightly from the original
        GateInstance duplicated = new GateInstance(gate.bitmap, gate.x + 50, gate.y + 50, gate.resId);
        duplicated.scale = gate.scale;
        gates.add(duplicated);
        invalidate();
    }
    public void pasteGate() {
        if (clipboardGate != null) {
            saveState();
            float centerX = (getWidth() / 2f - mPosX) / mScaleFactor;
            float centerY = (getHeight() / 2f - mPosY) / mScaleFactor;
            GateInstance pasted = new GateInstance(clipboardGate.bitmap, centerX, centerY, clipboardGate.resId);
            pasted.scale = clipboardGate.scale;
            gates.add(pasted);
            invalidate();
        }
    }
    public void toggleGroup(GateInstance gate) {
        saveState();

        // 1. If the gate is already in a group, REMOVE it from that group (Ungrouping one item)
        List<GateInstance> existingGroup = findGroupFor(gate);
        if (existingGroup != null) {
            existingGroup.remove(gate);
            // If only 1 item left in group, dissolve the group entirely
            if (existingGroup.size() < 2) {
                groups.remove(existingGroup);
            }
            invalidate();
            return;
        }

        // 2. If we are already dragging or have a "Current" group, add to it
        // Search if there's an active group nearby or just use the last one created
        if (!groups.isEmpty()) {
            // Add to the most recently modified group
            groups.get(groups.size() - 1).add(gate);
        } else {
            // No groups exist yet, start a new one with a buffer
            if (!multiSelectBuffer.contains(gate)) {
                multiSelectBuffer.add(gate);
            }

            if (multiSelectBuffer.size() >= 2) {
                groups.add(new ArrayList<>(multiSelectBuffer));
                multiSelectBuffer.clear();
            }
        }
        invalidate();
    }
    private List<GateInstance> findGroupFor(GateInstance gate) {
        for (List<GateInstance> group : groups) {
            if (group.contains(gate)) return group;
        }
        return null;
    }
    private void showPasteMenu() {
        if (clipboardGate == null) return;

        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), this);
        popup.getMenu().add("Paste Component");
        popup.setOnMenuItemClickListener(item -> {
            pasteGate();
            return true;
        });
        // Use the last touch coordinates to show the menu at the finger location
        popup.show();
    }
    public Bitmap getThumbnail() {
        // 1. Get the area where the components actually are
        float[] bbox = getBoundingBox();
        if (bbox == null) return null; // Empty diagram

        float diagramWidth = bbox[2] - bbox[0];
        float diagramHeight = bbox[3] - bbox[1];

        // 2. Create a square bitmap for the card (e.g., 300x300)
        int thumbSize = 300;
        Bitmap thumb = Bitmap.createBitmap(thumbSize, thumbSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(thumb);
        canvas.drawColor(Color.WHITE); // Background

        // 3. Calculate scale to fit the circuit into the 300px box without distorting
        float scale = Math.min(thumbSize / diagramWidth, thumbSize / diagramHeight);

        // 4. Center the circuit in the thumbnail
        float xOffset = (thumbSize - (diagramWidth * scale)) / 2f;
        float yOffset = (thumbSize - (diagramHeight * scale)) / 2f;

        canvas.translate(xOffset, yOffset);
        canvas.scale(scale, scale);

        // 5. Use your EXISTING export logic to draw gates, connections, and text
        drawDiagram(canvas, bbox[0], bbox[1]);

        return thumb;
    }
}