package com.daniils.floordesigner.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.daniils.floordesigner.BuildingHelper;
import com.daniils.floordesigner.Maths;
import com.daniils.floordesigner.Point;
import com.daniils.floordesigner.Polygon;
import com.daniils.floordesigner.R;
import com.daniils.floordesigner.Selectable;
import com.daniils.floordesigner.Vertex;
import com.daniils.floordesigner.Util;
import com.daniils.floordesigner.data.PolygonData;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class DrawingView extends View {
    public static final double VERTEX_BUTTON_RADIUS = 50;
    public static final double SEGM_BUTTON_RADIUS = 30;
    private boolean drawing = false;
    private boolean placingSquare = false;
    private LinkedList<Point> path = new LinkedList<>();
    public ArrayList<Selectable> selection = new ArrayList<>();
    public LinkedList<Polygon> polygons = new LinkedList<>();
    public LinkedList<Polygon> polygonsToRemove = new LinkedList<>();
    private Point translation = new Point(0, 0);
    public double scaleFactor = 0.8f;
    private Point touchStart = new Point(0, 0);
    private Point[] placedSquare = null;

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


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean out;
        if (drawing)
            out = onTouchDrawing(event);
        else if (placingSquare)
            out = onTouchPlacingSquare(event);
        else
            out = onTouchNoDrawing(event);
        return out;
    }

    private void clearSelection() {
        for (Selectable v : selection) {
            v.setSelected(selection, false);
        }
    }

    private boolean onTouchPlacingSquare(MotionEvent event) {
        int x = (int)((event.getX() + 0.5f - translation.x) / scaleFactor);
        int y = (int)((event.getY() + 0.5f - translation.y) / scaleFactor);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                placedSquare = new Point[] { new Point(x, y),
                                            new Point(x, y) };
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                ArrayList<Point> pts = new ArrayList<>();
                pts.add(new Point(placedSquare[0].x, placedSquare[0].y));
                pts.add(new Point(placedSquare[1].x, placedSquare[0].y));
                pts.add(new Point(placedSquare[1].x, placedSquare[1].y));
                pts.add(new Point(placedSquare[0].x, placedSquare[1].y));
                Polygon poly = new Polygon(this, pts);
                polygons.add(poly);
                placedSquare = null;
                setPlacingSquare(false);
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
        int x = (int)((event.getX() + 0.5f - translation.x) / scaleFactor);
        int y = (int)((event.getY() + 0.5f - translation.y) / scaleFactor);

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
                setDrawing(false);
                invalidate();
                break;
            default:
                return super.onTouchEvent(event);
        }
        return true;
    }

    private boolean onTouchNoDrawing(MotionEvent event) {
        int x = (int)((event.getX() + 0.5f - translation.x) / scaleFactor);
        int y = (int)((event.getY() + 0.5f - translation.y) / scaleFactor);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clearSelection();
                touchStart = new Point(event.getX(), event.getY());

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
                if (bestPoint != null) {
                    bestPoint.setSelected(selection, true);
                    bestPoint.polygon.showMenu();
                } else if (bestSegm != null) {
                    bestSegm.setSelected(selection, true);
                    bestSegm.next.setSelected(null, true);
                    bestSegm.polygon.showMenu();
                } else if (bestPoly != null) {
                    bestPoly.setSelected(selection, true);
                    bestPoly.showMenu();
                } else {
                    ((Activity)getContext()).findViewById(R.id.room_panel).setVisibility(GONE);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                for (Selectable v : selection)
                    v.touchDragged(new Point(x, y));
                if (selection.isEmpty()) {
                    p = new Point(event.getX(), event.getY());
                    Point delta = p.sub(touchStart);
                    translation = translation.add(delta);
                    touchStart = new Point(p);
                }
                invalidate();
                break;
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
        this.drawing = drawing;
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
            break;
        }
        selection.clear();
        invalidate();
    }

    public void setPlacingSquare(boolean b) {
        this.placingSquare = b;
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
}
