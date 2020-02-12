package com.daniils.floordesigner.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.daniils.floordesigner.BuildingHelper;
import com.daniils.floordesigner.LengthFrame;
import com.daniils.floordesigner.MovementFilter;
import com.daniils.floordesigner.Shapes;
import com.daniils.floordesigner.WallElements;
import com.daniils.floordesigner.data.WindowData;
import com.daniils.floordesigner.util.Maths;
import com.daniils.floordesigner.Point;
import com.daniils.floordesigner.Polygon;
import com.daniils.floordesigner.R;
import com.daniils.floordesigner.Selectable;
import com.daniils.floordesigner.Vertex;
import com.daniils.floordesigner.util.Util;
import com.daniils.floordesigner.data.PolygonData;
import com.daniils.floordesigner.windows.Window;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

@SuppressLint("ViewConstructor")
public class DrawingView extends View {

    enum State  { DEFAULT, DRAWING, PLACING_WINDOW, PLACING_SHAPE };
    public static final double VERTEX_BUTTON_RADIUS = 50;
    public static final double SEGM_BUTTON_RADIUS = 30;
    public static final double DRAG_MOVEMENT_STEP = 50;

    private State state = State.DEFAULT;
    private LinkedList<Point> path = new LinkedList<>();
    public ArrayList<Selectable> selection = new ArrayList<>();
    public LinkedList<Polygon> polygons = new LinkedList<>();
    public LinkedList<Polygon> polygonsToRemove = new LinkedList<>();
    private Point translation = new Point(0, 0);
    public double scaleFactor = 0.8f;
    private Point touchStart = new Point(0, 0);
    private Point[] placedSquare = null;
    private LengthFrame lengthFrame = new LengthFrame();
    int shapeIndex = 0;
    int windowIndex = 0;
    MovementFilter filter = new MovementFilter(10);

    public DrawingView(Context context, String filename) {
        super(context);
        try {
            load(filename);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean out = false;
        switch (state) {
            case DRAWING:
                out = onTouchDrawing(event);
                break;
            case PLACING_SHAPE:
                out = onTouchPlacingSquare(event);
                break;
            case PLACING_WINDOW:
                out = onTouchPlacingWindow(event);
                break;
            case DEFAULT:
                out = onTouchNoDrawing(event);
        }
        return out;
    }

    private void clearSelection() {
        if (!selection.isEmpty()) {
            selection.get(0).setSelected(selection, false);
        }
    }

    private boolean onTouchPlacingWindow(MotionEvent event) {
        Point t = new Point(event.getX(), event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            filter.reset();
        t = filter.update(t);
        int x = (int)((t.x + 0.5f - translation.x) / scaleFactor);
        int y = (int)((t.y + 0.5f - translation.y) / scaleFactor);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                try {
                    Point p = new Point(x, y);
                    Class<?> aClass = WallElements.wallElements[windowIndex].aClass;
                    Constructor<?> constructor = aClass.getConstructor(Point.class, Vertex.class);
                    Vertex v = Util.getClosestSegment(polygons, p, 90);
                    if (v != null) {
                        Window window = (Window) constructor.newInstance(p, v);
                        v.addWindow(window);
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                setPlacingWindow(false);
                invalidate();
                break;
        }
        return true;
    }

    private boolean onTouchPlacingSquare(MotionEvent event) {
        Point t = new Point(event.getX(), event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            filter.reset();
        t = filter.update(t);
        int x = (int)((t.x + 0.5f - translation.x) / scaleFactor);
        int y = (int)((t.y + 0.5f - translation.y) / scaleFactor);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                placedSquare = new Point[] { new Point(x, y),
                                            new Point(x, y) };
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                ArrayList<Point> pts = new ArrayList<>();
                double x0 = placedSquare[0].x, y0 = placedSquare[0].y;
                double w = placedSquare[1].x - x0, h = placedSquare[1].y - y0;
                for (Point p : Shapes.shapes[shapeIndex].points) {
                    pts.add(new Point(x0 + p.x * w, y0 + p.y * h));
                }
                Polygon poly = new Polygon(this, pts);
                polygons.add(poly);
                placedSquare = null;
                setPlacingShape(false);
                updateLengthFrame();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                double dx = Math.abs(x - placedSquare[0].x);
                boolean upper = y <= placedSquare[0].y;
                placedSquare[1].x = x;
                placedSquare[1].y = placedSquare[0].y + dx * (upper ? -1 : 1);
                invalidate();
                break;
        }
        return true;
    }

    private boolean onTouchDrawing(MotionEvent event) {
        Point t = new Point(event.getX(), event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            filter.reset();
        t = filter.update(t);
        int x = (int)((t.x + 0.5f - translation.x) / scaleFactor);
        int y = (int)((t.y + 0.5f - translation.y) / scaleFactor);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.add(new Point(x, y));
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                path.add(new Point(x, y));
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                path.add(new Point(x, y));
                ArrayList<Point> pts = BuildingHelper.buildPolyFromPath(path, scaleFactor);
                if (pts != null) {
                    Polygon poly = new Polygon(this, pts);
                    if (poly.getIntersection() == null) {
                        polygons.add(poly);
                    }
                }
                path.clear();
                updateLengthFrame();
                invalidate();
                break;
            default:
                return super.onTouchEvent(event);
        }
        return true;
    }

    private boolean onTouchNoDrawing(MotionEvent event) {
        // t - touch in pixel/screen coords
        Point t = new Point(event.getX(), event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            filter.reset();
        t = filter.update(t);
        int x = (int)((t.x + 0.5f - translation.x) / scaleFactor);
        int y = (int)((t.y + 0.5f - translation.y) / scaleFactor);
        // c - touch in world coords
        Point c = new Point(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Transmit action to selectables
                boolean preventDefault = false;
                for (Selectable v : selection) {
                    if (v.touchDown(new Point(c)))
                        preventDefault = true;
                }
                if (preventDefault)
                    break;

                // Update touch start
                touchStart = new Point(t.x, t.y);

                // Select best point & best segment
                Point p = new Point(x, y);
                double dist2PointMin = VERTEX_BUTTON_RADIUS / scaleFactor;
                double dist2SegmMin = SEGM_BUTTON_RADIUS / scaleFactor;
                Vertex bestPoint = null;
                Vertex bestSegm = null;
                Polygon bestPoly = null;
                for (Polygon poly : polygons) {
                    if (bestPoint == null && bestSegm == null && bestPoly == null) {
                        if (poly.contains(p)) {
                            bestPoly = poly;
                        }
                    }
                    for (Vertex v : poly.vertices) {
                        double dist2Point = Maths.dist(v.p, p);
                        if (dist2Point < dist2PointMin) {
                            dist2PointMin = dist2Point;
                            bestPoint = v;
                        }
                        Point rel = Maths.getRelativeCoords(v.p, v.next.p, p);
                        boolean overlaps = (rel.x >= 0 && rel.x <= Maths.dist(v.p, v.next.p));
                        double dist2Segm = Math.abs(rel.y);
                        if (dist2Segm < dist2SegmMin && overlaps) {
                            dist2SegmMin = dist2Segm;
                            bestSegm = v;
                        }
                    }
                }

                // Choice priority: 1) point 2)segment 3)polygon 4)total deselection
                if (bestPoint != null) {
                    clearSelection();
                    bestPoint.setSelected(selection, true);
                    bestPoint.touchDown(c);
                    bestPoint.polygon.showMenu();
                } else if (bestSegm != null) {
                    if (bestSegm.selected) break;
                    if (!bestSegm.trySelectWindow(selection, p)) {
                        clearSelection();
                        bestSegm.setSelected(selection, true);
                        bestSegm.touchDown(c);
                        bestSegm.next.setSelected(null, true);
                        bestSegm.next.touchDown(c);
                    }
                    bestSegm.polygon.showMenu();
                } else if (bestPoly != null) {
                    clearSelection();
                    bestPoly.setSelected(selection, true);
                    bestPoly.touchDown(c);
                    bestPoly.showMenu();
                } else {
                    clearSelection();
                    ((Activity)getContext()).findViewById(R.id.room_panel).setVisibility(GONE);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:
                // Transmit action to selectables, dozing distance step by step
                for (Selectable v : selection) {
                    v.touchMove(c);
                }
                // Update translation
                if (selection.isEmpty()) {
                    p = new Point(t.x, t.y);
                    translation = translation.add(p.sub(touchStart));
                    touchStart = new Point(p);
                }

                updateLengthFrame();
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                // Transmit action to selectables
                for (Selectable v : selection)
                    v.touchUp(new Point(c));

                updateLengthFrame();
                invalidate();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint mPaint = Util.getPaint(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);

        canvas.translate((float) translation.x, (float) translation.y);
        canvas.scale((float)scaleFactor, (float)scaleFactor);
        for (Polygon poly : polygonsToRemove) {
            polygons.remove(poly);
        }
        polygonsToRemove.clear();
        drawPolygons(canvas);
        lengthFrame.draw(canvas);
        drawPenLine(canvas);
        drawPlacedRect(canvas);
    }

    private void drawPolygons(Canvas canvas) {
        Paint paint = Util.getPaint(Color.BLACK);
        // Draw outer
        for (Polygon poly : polygons) {
            poly.drawOuterLine(canvas, paint);
        }
        // Draw inner
        for (Polygon poly : polygons) {
            poly.drawInnerLine(canvas, paint);
        }
        // Draw info
        for (Polygon poly : polygons) {
            poly.drawInfo(canvas, paint);
        }
        // Draw UI
        for (Polygon poly : polygons) {
            poly.drawUI(canvas, paint);
        }
    }

    private void drawPenLine(Canvas canvas) {
        Paint mPaint = Util.getPaint(Color.BLACK);
        if (path.size() > 1) {
            Iterator<Point> it = path.iterator();
            Point a = it.next();
            while (it.hasNext()) {
                Point b = it.next();
                canvas.drawLine((float)a.x, (float)a.y, (float)b.x, (float)b.y, mPaint);
                a = b;
            }
        }
    }

    private void drawPlacedRect(Canvas canvas) {
        Paint mPaint = Util.getPaint(Color.RED);
        if (placedSquare != null) {
            Rect rect = new Rect(
                    (int)Math.min(placedSquare[0].x, placedSquare[1].x),
                    (int)Math.min(placedSquare[0].y, placedSquare[1].y),
                    (int)Math.max(placedSquare[0].x, placedSquare[1].x),
                    (int)Math.max(placedSquare[0].y, placedSquare[1].y));
            canvas.drawRect(rect, mPaint);
        }
    }

    public void setDrawing(boolean drawing) {
        this.state = (drawing ? State.DRAWING : State.DEFAULT);
    }

    public void save(String filename) throws IOException {
        LinkedList<PolygonData> data = new LinkedList<>();
        for (Polygon poly : polygons) {
            data.add(poly.getData());
        }
        FileOutputStream file = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(file);
        out.writeObject(data);
        out.close();
        file.close();
    }

    public void load(String filename) throws IOException, ClassNotFoundException {
        FileInputStream file = new FileInputStream(filename);
        ObjectInputStream in = new ObjectInputStream(file);

        LinkedList<PolygonData> data = (LinkedList<PolygonData>)in.readObject();
        for (PolygonData polygonData : data) {
            Polygon poly = new Polygon(this, polygonData);
            polygons.add(poly);
            int vertexId = 0;
            Iterator<Vertex> it = poly.vertices.iterator();
            Vertex vertex = it.next();
            for (WindowData windowData : polygonData.windows) {
                while (windowData.vertexId > vertexId) {
                    vertex = it.next();
                    vertexId++;
                }
                try {
                    Class<?> aClass = WallElements.wallElements[windowData.classId].aClass;
                    Constructor<?> constructor = aClass.getConstructor(double.class, double.class, Vertex.class);
                    Window window = (Window)constructor.newInstance(windowData.left, windowData.right, vertex);
                    vertex.addWindow(window);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        in.close();
        file.close();
    }

    public void deleteSelected() {
        LinkedList<Selectable> selection = new LinkedList<>(this.selection);
        for (Selectable v : selection) {
            if (v instanceof Polygon) {
                ((Polygon) v).remove();
                break;
            }
            if (v instanceof Vertex) {
                ((Vertex) v).polygon.remove();
            }
            if (v instanceof Window) {
                ((Window) v).v.removeWindow((Window)v);
            }
            break;
        }
        selection.clear();
        updateLengthFrame();
        invalidate();
    }

    public void setPlacingShape(boolean b) {
        this.state = (b ? State.PLACING_SHAPE : State.DEFAULT);
    }

    public void setPlacingWindow(boolean b) {
        this.state = (b ? State.PLACING_WINDOW : State.DEFAULT);
    }

    public void lockSelected() {
        LinkedList<Selectable> selection = new LinkedList<>(this.selection);
        for (Selectable v : selection) {
            Polygon poly = null;
            if (v instanceof Polygon) {
                poly = (Polygon)v;
            }
            if (v instanceof Vertex) {
                poly = ((Vertex) v).polygon;
            }
            if (poly != null) {
                poly.locked = !poly.locked;
                poly.showMenu();
            }
            break;
        }
    }

    public void updateLengthFrame() {
        lengthFrame.update(polygons);
    }

    public String changeShape() {
        shapeIndex = (shapeIndex + 1) % Shapes.shapes.length;
        return Shapes.shapes[shapeIndex].name;
    }

    public String changeWindow() {
        windowIndex = (windowIndex + 1) % WallElements.wallElements.length;
        return WallElements.wallElements[windowIndex].name;
    }
}
