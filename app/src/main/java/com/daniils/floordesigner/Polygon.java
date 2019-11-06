package com.daniils.floordesigner;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.daniils.floordesigner.activity.EditorActivity;
import com.daniils.floordesigner.activity.MainActivity;
import com.daniils.floordesigner.data.PolygonData;
import com.daniils.floordesigner.view.AssetsManager;
import com.daniils.floordesigner.view.DrawingView;

import java.util.ArrayList;
import java.util.LinkedList;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class Polygon extends Selectable {
    public Vertex firstVertex;
    public final DrawingView drawingView;
    public LinkedList<Vertex> vertices = new LinkedList<>();
    private boolean clockwise = true;
    public Point prevTouchPoint = null;
    private double square = 0;
    public String label = "";
    public final int LABEL_THICKNESS = 40;
    private double rotation = 0, scale = 0.5;
    public boolean locked = false;

    public Polygon(DrawingView drawingView, ArrayList<Point> path) {
        this.drawingView = drawingView;
        if (path.size() < 3)
            remove();
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
        // check whether it goes clockwise
        if (!canExist())
            clockwise = false;
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
        paint.setColor(Color.rgb(231, 228, 154));
        paint.setStyle(Paint.Style.FILL);
        g.drawPath(path, paint);
    }

    public void drawInnerLine(Canvas g, Paint paint) {
        boolean first = true;
        Path path = new Path();

        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        for (Vertex v : vertices) {
            if (v.selected && v.next.selected) {
                g.drawLine((int)v.p.x, (int)v.p.y, (int)v.next.p.x, (int)v.next.p.y, paint);
            }
            if (first)
                path.moveTo((float) v.p.x, (float) v.p.y);
            path.lineTo((float)v.next.p.x, (float)v.next.p.y);
            first = false;
        }
        paint.setColor(Color.rgb(196, 191, 189));
        paint.setStyle(Paint.Style.FILL);
        g.drawPath(path, paint);
    }

    public void drawInfo(Canvas g, Paint paint) {
        for (Vertex v : vertices) {
            v.drawInfo(g, paint);
        }
    }

    public void drawUI(Canvas g, Paint paint) {
        AssetsManager assetsManager = ((EditorActivity)drawingView.getContext()).assetsManager;
        for (Vertex v : vertices) {
            if (!selected && !v.selected)
                continue;
            Bitmap im1 = assetsManager.moveVertexIcon;
            Matrix mat = new Matrix();
            mat.setTranslate((float) v.p.x - im1.getWidth() / 2f,
                    (float) v.p.y - im1.getHeight() / 2f);
            g.drawBitmap(im1, mat, paint);


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
    public void touchDragged(Point point) {
        super.touchDragged(point);
        if (locked) return;
        if (prevTouchPoint != null)  {
            Point delta = point.sub(prevTouchPoint);
            for (Vertex v : vertices) {
                v.p = v.p.add(delta);
                v.outlineA = v.outlineA.add(delta);
                v.outlineB = v.outlineB.add(delta);
            }
        }
        prevTouchPoint = new Point(point);
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public Point getIntersection() {
        for (Vertex v : vertices) {
            Point c = v.getIntersection();
            if (c != null)
                return c;
        }
        return null;
    }

    public PolygonData getData() {
        PolygonData data = new PolygonData();
        for (Vertex v : vertices) {
            data.path.add(new Point(v.p));
        }
        data.scale = scale;
        data.rotation = rotation;
        data.label = label;
        data.locked = locked;
        return data;
    }

    public void updateSquare() {
        double k =  0.0000660066;
        double mToInch = 3.28083989501;
        double sum = 0.0;
        for (Vertex v : vertices) {
            sum +=  mToInch * ((v.p.x * v.next.p.y) - (v.p.y * v.next.p.x));
        }
        square = Util.setPrecision(sum * k, 2);
        square = Math.abs(square);
    }

    private Point getCentroid() {
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
        scaleBar.setProgress((int)(getScale() * scaleBar.getMax()));
        scaleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setScale((double)seekBar.getProgress() / seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar rotBar = activity.findViewById(R.id.rotation_edit);
        rotBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setRotation((double)seekBar.getProgress() / seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        rotBar.setProgress((int)(getRotation() * rotBar.getMax()));

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

    public void setScale(double scale) {
        scale = Math.max(0.1, scale);
        Point center = getCentroid();
        for (Vertex v : vertices) {
            v.p = (v.p.sub(center)).
                    scale(1 / this.scale).
                    scale(scale).
                    add(center);
        }
        updateOutline();
        drawingView.invalidate();
        this.scale = scale;
    }

    public void setRotation(double rotation) {
        double alpha = Math.PI * (rotation - this.rotation);
        System.out.println("ROT " + Math.toDegrees(alpha));
        Point center = getCentroid();
        for (Vertex v : vertices) {
            v.p = Maths.rotate(v.p.sub(center), alpha).add(center);
        }
        updateOutline();
        drawingView.invalidate();
        this.rotation = rotation;
    }

    public void updateOutline() {
        for (Vertex v : vertices) {
            v.updateBisector();
            v.updateOutline();
        }
        updateSquare();
    }
}
