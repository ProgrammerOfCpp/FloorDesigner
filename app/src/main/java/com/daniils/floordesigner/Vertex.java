package com.daniils.floordesigner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Vertex extends Selectable {
    private final double THICKNESS = 30;
    public Vertex next, prev;
    public final boolean first;
    public Point p;
    public Point outlineA, outlineB;
    public final Polygon polygon;
    private LinkedList<Point[]> directionalLines = new LinkedList<>();
    private Point bisector;
    private double bisectorLength;

    public Vertex(Polygon polygon, Point p, boolean first) {
        this.polygon = polygon;
        this.p = p;
        this.first = first;
    }

    @Override
    public void touchDragged(Point point) {
        super.touchDragged(point);
        if (polygon.locked) return;
        if (selected) {
            new MovementCorrector(this, directionalLines)
                    .performMovement(point);
        }
    }

    public void updateBisector() {
        double angle = getAngle();
        if (angle < Math.PI / 180 && false) {
            bisectorLength = THICKNESS;
        } else {
            double outer = Math.PI * 2 - angle;
            double alpha = outer / 2 - Math.PI / 2;
            double cosine = Math.cos(alpha);
            bisectorLength = THICKNESS / cosine;
        }
        double theta2 = Maths.theta(this.p, next.p);
        double alpha = angle / 2;
        double theta;
        if (polygon.isClockwise())
            theta = theta2 + alpha;
        else
            theta = theta2 - alpha;
        bisector = Maths.getRotatedPoint(theta, bisectorLength);
    }

    public void updateOutline() {
        double maxX = THICKNESS * 2;
        double x = getBisectorLength();
        if (Math.abs(x) > maxX) {
            x = x / Math.abs(x) * maxX;
            Point bis = getBisector(-x);
            outlineA = Maths.projectTo(bis, prev.getBisector(true), getBisector(true));
            outlineB = Maths.projectTo(bis, getBisector(true), next.getBisector(true));
        } else {
            Point bis = getBisector(-x);
            outlineA = new Point(bis);
            outlineB = new Point(bis);
        }
    }

    public double getBisectorLength() {
        return bisectorLength;
    }

    @Override
    public void setSelected(ArrayList<Selectable> selection, boolean selected) {
        if (this.selected && !selected && next.selected) {
            next.setSelected(null, false);
        }
        super.setSelected(selection, selected);
    }

    public double getAngle() {
        double theta1 = Maths.theta(prev.p, this.p);
        double theta2 = Maths.theta(this.p, next.p);
        double delta = theta2 - theta1;
        double theta;
        if (polygon.isClockwise())
            theta = Math.PI * 3 - delta;
        else
            theta = Math.PI * 3 + delta;
        return theta % (Math.PI*2);
    }

    public Point getBisector(double length) {
        return bisector.setLength(length).add(this.p);
    }

    public Point getBisector(boolean outer) {
        return bisector.scale(outer ? -1 : 1).add(this.p);
    }

    public void drawInfo(Canvas g, Paint paint) {
        // dirs
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        paint.setColor(Color.GREEN);
        paint.setAlpha(128);
        for (Point[] line : directionalLines) {
            final double len = 10000;
            double dx = line[1].x - line[0].x;
            double dy = line[1].y - line[0].y;
            double x1 = line[0].x - dx * len;
            double y1 = line[0].y - dy * len;
            double x2 = line[1].x + dx * len;
            double y2 = line[1].y + dy * len;
            g.drawLine((float) x1, (float) y1, (float) x2, (float) y2, paint);
        }
        directionalLines.clear();
        paint.setAlpha(255);
        paint.setStrokeWidth(4);
        // some beautiful points
        if (selected) {
            paint.setStyle(Paint.Style.FILL);
            final int RAD = 7;
            paint.setColor(Color.RED);
            g.drawCircle((int) p.x, (int) p.y, RAD, paint);
            paint.setColor(Color.BLUE);
            g.drawCircle((int) prev.p.x, (int) prev.p.y, RAD, paint);
            paint.setColor(Color.GREEN);
            g.drawCircle((int) next.p.x, (int) next.p.y, RAD, paint);
        }
        // angle
        if (selected || next.selected || prev.selected) {
            double a = Maths.dist(prev.p, p);
            double b = Maths.dist(p, next.p);
            int RAD = (int)Math.min(THICKNESS *2, Math.min(a, b));
            RectF rect = new RectF((int)p.x - RAD, (int)p.y - RAD, (int)p.x + RAD, (int)p.y + RAD);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.GREEN);
            double startAngle;
            if (polygon.isClockwise()) {
                startAngle = Maths.theta(p, next.p);
            } else {
                startAngle = Maths.theta(p, prev.p);
            }
            double angle = Math.toDegrees(getAngle());
            g.drawArc(rect, (float)Math.toDegrees(startAngle), (float)angle, false, paint);
            String text = ((int)(angle + 0.5) % 360) + "°";
            int hw = (int)(paint.measureText(text) / 2);
            int hh = (int)(paint.getTextSize() / 2);
            Point bys = getBisector(THICKNESS);
            int x = (int)(bys.x - hw);
            int y = (int)(bys.y + hh);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextSize((int) THICKNESS);
            paint.setTypeface(Typeface.MONOSPACE);
            g.drawText(text, x, y, paint);
        }
    }

    public Point getIntersection() {
        double theta = Maths.theta(this.p, next.p, this.p, prev.p);
        if (Math.min(theta, Math.PI * 2 - theta) <= Math.toRadians(1))
            return new Point(this.p);

        for (Polygon poly : polygon.drawingView.polygons) {
            for (Vertex v : poly.vertices) {
                if (v == this)
                    continue;
                Point c = Maths.intersection(this.p, next.p, v.p, v.next.p, false);
                if (c != null) {
                    if (c.equals(this.p) || c.equals(next.p))
                        continue;
                    return c;
                }
            }
        }
        return null;
    }
}
