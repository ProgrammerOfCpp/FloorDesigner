package com.daniils.floordesigner.windows;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.daniils.floordesigner.Point;
import com.daniils.floordesigner.Selectable;
import com.daniils.floordesigner.Vertex;
import com.daniils.floordesigner.util.Maths;
import com.daniils.floordesigner.util.Util;
import com.daniils.floordesigner.view.DrawingView;


public class Window extends Selectable implements Comparable<Window> {
    public Vertex v;
    public double left, right;
    public int selectedBorder = 0;
    private double originalScale;

    public Window(Point p, Vertex v) {
        this.v = v;
        originalScale = v.polygon.getScale();
        Point mid = Maths.projectTo(p, v.p, v.next.p);
        double dist = Maths.dist(mid, v.p);
        double len = Maths.dist(v.p, v.next.p);
        left = (dist - getWidth() / 2) / len;
        right = (dist + getWidth() / 2) / len;
    }

    public Window(double left, double right, Vertex v) {
        this.v = v;
        originalScale = v.polygon.getScale();
        this.left = left;
        this.right = right;
    }

    public void draw(Canvas c) {
        c.save();
        Point[] ps = getAbsoluteCoordinates();
        c.translate((float)ps[0].x, (float)ps[0].y);
        double theta = Maths.theta(v.p, v.next.p);
        c.rotate((float)(theta * 180 / Math.PI));
        Paint paint = Util.getPaint(Color.BLUE);
        c.scale((float)(v.polygon.getScale() / originalScale), (float)(v.polygon.getScale() / originalScale));
        drawDetails(c, paint);
        c.restore();
    }

    protected void drawDetails(Canvas c, Paint paint) {
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth((float)Vertex.THICKNESS);
        float width = getLength();
        c.drawLine(0, 0, width, 0, paint);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);
        float tr = (float)Vertex.THICKNESS / 3;
        for (double y = -Vertex.THICKNESS / 6; y <= Vertex.THICKNESS / 6; y += Vertex.THICKNESS / 6) {
            c.drawLine(tr, (float)y, width - tr, (float)y, paint);
        }
        c.drawRect(0, (float)Vertex.THICKNESS / 2, width, -(float)Vertex.THICKNESS / 2, paint);
        c.drawRect(0, (float)Vertex.THICKNESS / 6, tr, -(float)Vertex.THICKNESS / 6, paint);
        c.drawRect(width - tr, (float)Vertex.THICKNESS / 6, width, -(float)Vertex.THICKNESS / 6, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
    }

    public double getWidth() {
        return 150;
    }

    public int getClassId() {
        return 0;
    }

    @Override
    public int compareTo(Window o) {
        if (left < o.left)
            return -1;
        if (left > o.left)
            return 1;
        return 0;
    }

    @Override
    public boolean processMovement(Point point) {
        super.processMovement(point);
        if (selectedBorder == 0)
            return true;
        double len = Maths.dist(v.p, v.next.p);
        Point rel = Maths.getRelativeCoords(v.p, v.next.p, point).scale(1 / len);
        Window right  = v.windows.higher(this);
        Window left = v.windows.lower(this);

        double l = (left == null ? 0 : left.right);
        double r = (right == null ? 1 : right.left);
        if (selectedBorder == -1) {
            this.left = Maths.clamp(rel.x, l, this.right - 30 / len);
        }
        if (selectedBorder == 1) {
            this.right = Maths.clamp(rel.x, this.left + 30 / len, r);
        }
        return true;
    }

    @Override
    public boolean touchDown(Point point) {
        super.touchDown(point);
        Point[] ps = getAbsoluteCoordinates();
        double d1 = Maths.dist(ps[0], point);
        double d2 = Maths.dist(ps[1], point);
        double dist2PointMin = 2 * DrawingView.VERTEX_BUTTON_RADIUS / v.polygon.drawingView.scaleFactor;
        if (Math.min(d1, d2) > dist2PointMin) {
            selectedBorder = 0;
            return false;
        }
        if (Math.min(d1, d2) < dist2PointMin / 2) {
            if (d1 < d2)
                selectedBorder = -1;
            else
                selectedBorder = 1;
        } else {
            selectedBorder = 0;
        }
        return true;
    }

    @Override
    public void touchUp(Point point) {
        super.touchUp(point);
        selectedBorder = 0;
    }

    public float getLength() {
        if (v == null)
            return 0;
        Point vec = v.next.p.sub(v.p);
        double len = vec.length();
        return (float)(Math.abs(right - left) * len);
    }

    public Point[] getAbsoluteCoordinates() {
        if (v == null)
            return null;
        Point vec = v.next.p.sub(v.p);
        double len = vec.length();
        Point a = v.p.add(vec.scale(left));
        Point b = v.p.add(vec.scale(right));

        double y = v.p.x - v.next.p.x ;
        double x =  v.next.p.y - v.p.y;
        Point offset = new Point(x, y).scale(0.5 * Vertex.THICKNESS / len);
        return new Point[] { a.add(offset), b.add(offset) };
    }
}
