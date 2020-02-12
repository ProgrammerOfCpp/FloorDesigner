package com.daniils.floordesigner;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.daniils.floordesigner.activity.EditorActivity;
import com.daniils.floordesigner.data.PolygonData;
import com.daniils.floordesigner.data.WindowData;
import com.daniils.floordesigner.util.Binsearch;
import com.daniils.floordesigner.util.Maths;
import com.daniils.floordesigner.util.Util;
import com.daniils.floordesigner.view.AssetsManager;
import com.daniils.floordesigner.view.DrawingView;
import com.daniils.floordesigner.windows.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static android.view.View.VISIBLE;

public class Polygon extends Selectable {
    public static final int wallsColor = Color.rgb(231, 228, 154);
    public static final int fillColor = Color.rgb(196, 191, 189);
    public Vertex firstVertex;
    public final DrawingView drawingView;
    public LinkedList<Vertex> vertices = new LinkedList<>();
    private double square = 0;
    public String label = "";
    public final int LABEL_THICKNESS = 40;
    private double rotation = 0, scale = 0.5;
    private Rect aabb = new Rect();
    public boolean locked = false;

    public Polygon(DrawingView drawingView, ArrayList<Point> path) {
        this.drawingView = drawingView;
        if (path.size() < 3)
            remove();
        recreateFromPath(path);
        // check whether it goes clockwise
        if (!canExist()) {
            Collections.reverse(path);
            recreateFromPath(path);
        }
        // update outline
        for (Vertex v : vertices) {
            v.updateBisector();
        }
        for (Vertex v : vertices) {
            v.updateOutline();
        }
        updateSquare();
    }

    public Polygon(DrawingView drawingView, PolygonData data) {
        this(drawingView, data.path);
        rotation = data.rotation;
        scale = data.scale;
        label = data.label;
        locked = data.locked;
    }

    private void recreateFromPath(ArrayList<Point> path) {
        // build polygon
        firstVertex = new Vertex(this, path.get(0), true);
        Vertex prevInner = firstVertex;
        for (int i = 1; i < path.size(); i++) {
            Vertex nextInner = new Vertex(this, path.get(i), false);
            prevInner.next = nextInner;
            nextInner.prev = prevInner;
            prevInner = nextInner;
        }
        prevInner.next = firstVertex;
        firstVertex.prev = prevInner;
        updateVerticesList();
    }

    public boolean canExist() {
        double sum = 0;
        for (Vertex v : vertices) {
            sum += v.getAngle();
        }
        double rightSum = Math.PI * (vertices.size() - 2);
        if (Math.abs(sum - rightSum) > Maths.E)
            return false;
        return true;
    }

    public void drawOuterLine(Canvas g, Paint paint) {
        boolean first = true;
        Path path = new Path();
        for (Vertex v : vertices) {
            Point a = v.outlineA;
            Point b = v.outlineB;
            Point c = v.next.outlineA;
            /*paint.setStyle(Paint.Style.FILL);
            int r = (int)(paint.getStrokeWidth() / 2);
            g.drawRect((int)a.x - r, (int)a.y - r, (int)a.x + r, (int)a.y + r, paint);
            g.drawRect((int)b.x - r, (int)b.y - r, (int)b.x + r, (int)b.y + r, paint);
            paint.setStyle(Paint.Style.STROKE);*/
            if (first)
                path.moveTo((float) a.x, (float) a.y);
            path.lineTo((float) b.x, (float) b.y);
            path.lineTo((float) c.x, (float) c.y);
            first = false;
        }
        paint.setColor(wallsColor);
        paint.setStyle(Paint.Style.FILL);
        g.drawPath(path, paint);
    }

    public void drawInnerLine(Canvas g, Paint paint) {
        boolean first = true;
        Path path = new Path();

        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        int id = 0;
        for (Vertex v : vertices) {
            if (v.selected && v.next.selected) {
                g.drawLine((int)v.p.x, (int)v.p.y, (int)v.next.p.x, (int)v.next.p.y, paint);
            }
            if (first)
                path.moveTo((float) v.p.x, (float) v.p.y);
            path.lineTo((float)v.next.p.x, (float)v.next.p.y);
            first = false;
        }
        paint.setColor(fillColor);
        paint.setStyle(Paint.Style.FILL);
        g.drawPath(path, paint);

        for (Vertex v : vertices) {
            v.drawWindows(g);
        }
    }

    public void drawInfo(Canvas g, Paint paint) {
        for (Vertex v : vertices) {
            v.drawInfo(g, paint);
        }
    }

    public void drawUI(Canvas g, Paint paint) {
        AssetsManager assetsManager = ((EditorActivity)drawingView.getContext()).assetsManager;
        for (Vertex v : vertices) {
            Bitmap im1 = assetsManager.moveVertexIcon;
            Matrix mat = new Matrix();



            if (selected || v.selected) {
                mat.setTranslate((float) v.p.x - im1.getWidth() / 2f,
                        (float) v.p.y - im1.getHeight() / 2f);
                g.drawBitmap(im1, mat, paint);
            }


            if (selected || (v.selected && v.next.selected)) {
                Bitmap im2 = assetsManager.moveLineIcon;
                Point c = v.next.p.add(v.p).scale(0.5f);
                c.x -= im2.getWidth() / 2f;
                c.y -= im2.getHeight() / 2f;
                mat = new Matrix();
                double theta = Maths.theta(v.p, v.next.p);
                mat.postRotate((float) Math.toDegrees(theta) + 90,
                        (float) im2.getWidth() / 2f,
                        (float) im2.getHeight() / 2f);
                mat.postTranslate((float) c.x, (float) c.y);
                g.drawBitmap(im2, mat, paint);
            }

            for (Window window : v.windows) {
                if (window.selected) {
                    Point[] ps = window.getAbsoluteCoordinates();

                    mat = new Matrix();
                    mat.setTranslate((float) ps[0].x - im1.getWidth() / 2f,
                            (float) ps[0].y - im1.getHeight() / 2f);
                    g.drawBitmap(im1, mat, paint);

                    mat = new Matrix();
                    mat.setTranslate((float) ps[1].x - im1.getWidth() / 2f,
                            (float) ps[1].y - im1.getHeight() / 2f);
                    g.drawBitmap(im1, mat, paint);
                }
            }
        }

        String text = Util.setPrecision(square, 2) + " ft^2\n" + label;
        paint.setColor(Color.BLACK);
        paint.setTextSize(LABEL_THICKNESS);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setTypeface(Typeface.MONOSPACE);
        Util.drawMultiline(g, paint, getCentroid(), text);
    }

    public void remove() {
        for (Vertex v : vertices)
            v.setSelected(drawingView.selection, false);
        if (!drawingView.polygonsToRemove.contains(this))
            drawingView.polygonsToRemove.add(this);
    }

    public void updateVerticesList() {
        vertices = new LinkedList<>();
        Vertex v = firstVertex;
        do {
            vertices.add(v);
            v = v.next;
        } while(!v.first);
    }

    public boolean contains(Point p) {
        com.snatik.polygon.Polygon poly = getPolygon();
        return poly.contains(new com.snatik.polygon.Point(p.x, p.y));
    }

    @Override
    public boolean processMovement(Point point) {
        super.processMovement(point);
        if (locked) return true;


        final Point delta = point.sub(prevPoint);
        if (delta.length() > DrawingView.DRAG_MOVEMENT_STEP)
            System.out.println("Problem 2");


        translatePolygonMathematically(delta);
        boolean collision = false;
        if (getIntersection() != null) {
            collision = true;
            translatePolygonMathematically(delta.scale(-1));
            double len = Binsearch.perform(0, delta.length(), 1, x -> {
                Point v = delta.setLength(x);
                translatePolygonMathematically(v);
                boolean g = getIntersection() == null;
                translatePolygonMathematically(v.scale(-1));
                return g;
            });
            translatePolygonMathematically(delta.setLength(len));
        }
        drawingView.invalidate();

        return !collision;
    }

    @Override
    public boolean touchDown(Point point) {
        return super.touchDown(point);
    }

    @Override
    public void touchUp(Point point) {
        super.touchUp(point);
    }

    public Point getIntersection() {
        for (Vertex v : vertices) {
            Point c = v.getIntersection();
            if (c != null)
                return c;
        }


        com.snatik.polygon.Polygon polygon = getPolygon();
        for (Polygon poly : drawingView.polygons) {
            if (poly == this) continue;
            if (!poly.aabb.intersect(aabb)) continue;
            for (Vertex v : poly.vertices) {
                if (polygon.contains(new com.snatik.polygon.Point(v.p.x, v.p.y))) {
                    return v.p;
                }
            }
        }

        return null;
    }

    public PolygonData getData() {
        PolygonData data = new PolygonData();

        int vertexId = 0;
        for (Vertex v : vertices) {
            data.path.add(new Point(v.p));
            for (Window window : v.windows) {
                WindowData windowData = new WindowData();
                windowData.left = window.left;
                windowData.right = window.right;
                windowData.vertexId = vertexId;
                windowData.classId = window.getClassId();
                data.windows.add(windowData);
            }
            vertexId++;
        }
        data.scale = scale;
        data.rotation = rotation;
        data.label = label;
        data.locked = locked;
        return data;
    }

    public void updateSquare() {
        double sum = 0.0;
        for (Vertex v : vertices) {
            Point a = v.outlineA;
            Point b = v.outlineB;
            Point c = v.next.outlineA;
            sum +=  (a.x * b.y) - (a.y * b.x);
            sum +=  (b.x * c.y) - (b.y * c.x);
        }
        sum /= 2;
        square = Util.setPrecision(sum * Maths.M_TO_INCH * Maths.M_TO_INCH, 2);
        square = Math.abs(square);
    }

    public void updateAABB() {
        List<Double> vals = new ArrayList<>();
        for (Vertex v : vertices) {
            vals.add(v.p.x);
        }
        Collections.sort(vals);
        aabb.left = vals.get(0).intValue();
        aabb.right = vals.get(vertices.size() - 1).intValue();
        vals.clear();
        for (Vertex v : vertices) {
            vals.add(v.p.y);
        }
        Collections.sort(vals);
        aabb.bottom = vals.get(0).intValue();
        aabb.top = vals.get(vertices.size() - 1).intValue();
    }

    public Point getCentroid() {
        double sx = 0, sy = 0;
        for (Vertex v : vertices) {
            sx += v.p.x;
            sy += v.p.y;
        }
        return new Point(
                sx / vertices.size(),
                sy / vertices.size()
        );
    }

    private com.snatik.polygon.Polygon getPolygon() {
        com.snatik.polygon.Polygon.Builder builder = new com.snatik.polygon.Polygon.Builder();
        for (Vertex v : vertices) {
            builder.addVertex(new com.snatik.polygon.Point(v.p.x, v.p.y));
        }
        return builder.build();
    }

    public void showMenu() {
        Activity activity = (Activity)drawingView.getContext();
        activity.findViewById(R.id.room_panel).setVisibility(VISIBLE);
        ((EditText)activity.findViewById(R.id.labelText)).setText(label);

        SeekBar scaleBar = activity.findViewById(R.id.scale_edit);
        scaleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setScale((double)seekBar.getProgress() / seekBar.getMax());
                seekBar.setProgress((int)(0.5 + seekBar.getMax() * getScale()));
                drawingView.updateLengthFrame();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        scaleBar.setProgress((int)(getScale() * scaleBar.getMax()));

        SeekBar rotBar = activity.findViewById(R.id.rotation_edit);
        rotBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setRotation((double)seekBar.getProgress() / seekBar.getMax());
                seekBar.setProgress((int)(0.5 + seekBar.getMax() * getRotation()));
                drawingView.updateLengthFrame();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        rotBar.setProgress((int)(0.5 + getRotation() * rotBar.getMax()));

        TextView lockState = activity.findViewById(R.id.lock);
        if (locked)
            lockState.setText(R.string.unlock);
        else
            lockState.setText(R.string.lock);
    }

    public double getScale() {
        return scale;
    }

    public double getRotation() {
        return rotation;
    }

    private void scalePolygonMathematically(double scale) {
        Point center = getCentroid();
        for (Vertex v : vertices) {
            v.p = (v.p.sub(center)).
                    scale(scale / this.scale).
                    add(center);
        }
        this.scale = scale;
    }

    public void setScale(double scale) {
        scale = Math.max(0.1, scale);

        double minScale = this.scale;
        scalePolygonMathematically(scale);
        if (getIntersection() != null) {
            double fs;
            fs = Binsearch.perform(minScale, scale, 0.01, x -> {
                scalePolygonMathematically(x);
                return getIntersection() == null;
            });
            scalePolygonMathematically(fs);
        }
        drawingView.invalidate();
        updateOutline();
    }

    public void rotatePolygonMathematically(double rotation) {
        double alpha = Math.PI * (rotation - this.rotation);
        Point center = getCentroid();
        for (Vertex v : vertices) {
            v.p = Maths.rotate(v.p.sub(center), alpha).add(center);
        }
        this.rotation = rotation;
    }

    public void setRotation(double rotation) {

        final double delta = rotation - this.rotation;
        final double ampl = Math.abs(delta);
        if (ampl == 0)
            return;
        final double sign = delta / ampl;
        final double r0 = this.rotation;


        rotatePolygonMathematically(r0 + ampl * sign);
        if (getIntersection() != null) {
            //blockFurtherMovementDueToCollision();
            final double step = Math.PI / 10;
            class Kostyl {
                boolean stop = false;
            }
            final Kostyl kostyl = new Kostyl();
            double finalAmplitude = ampl;
            double lastAmplitude = 0;
            for (int i = 0; i * step < ampl && !kostyl.stop; i++) {
                finalAmplitude = Binsearch.perform(step * i, Math.min(ampl, step * (i + 1)), 0.01, x -> {
                    double theta = r0 + x * sign;
                    rotatePolygonMathematically(theta);
                    if (getIntersection() != null) {
                        kostyl.stop = true;
                        return false;
                    } else {
                        return true;
                    }
                });
                rotatePolygonMathematically(r0 + finalAmplitude * sign);
                if (getIntersection() != null) {
                    finalAmplitude = lastAmplitude;
                } else {
                    lastAmplitude = finalAmplitude;
                }
            }
            rotatePolygonMathematically(r0 + finalAmplitude * sign);
        }
        updateOutline();
        drawingView.invalidate();
    }

    public void translatePolygonMathematically(Point delta) {
        for (Vertex v : vertices) {
            v.p = v.p.add(delta);
            v.outlineA = v.outlineA.add(delta);
            v.outlineB = v.outlineB.add(delta);
        }
    }

    public void updateOutline() {
        for (Vertex v : vertices) {
            v.updateBisector();
            v.updateOutline();
        }
        updateSquare();
        updateAABB();
    }
}
