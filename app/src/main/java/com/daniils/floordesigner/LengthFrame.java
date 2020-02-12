package com.daniils.floordesigner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.daniils.floordesigner.util.Maths;
import com.daniils.floordesigner.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

public class LengthFrame {
    private ArrayList<Double>
            left = new ArrayList<>(),
            right = new ArrayList<>(),
            top = new ArrayList<>(),
            bottom = new ArrayList<>();
    private double leftBound, rightBound, topBound, bottomBound;
    private int pointsCount = 0;
    private final int OFFSET = 90;
    private final int TEXT_OFFSET = 30;

    private void reset() {
        left = new ArrayList<>();
        right = new ArrayList<>();
        top = new ArrayList<>();
        bottom = new ArrayList<>();

        leftBound = Double.MAX_VALUE;
        rightBound = -Double.MAX_VALUE;
        bottomBound = -Double.MAX_VALUE;
        topBound = Double.MAX_VALUE;
        pointsCount = 0;
    }
    public void update(LinkedList<Polygon> polygons) {
        reset();
        for (Polygon poly : polygons) {
            for (Vertex v : poly.vertices) {
                if (Maths.equals(v.getAngle(), Math.PI)) continue;
                addPoint(v.outlineA);
                addPoint(v.outlineB);
            }
        }
        left.add(topBound);
        left.add(bottomBound);
        right.add(topBound);
        right.add(bottomBound);
        top.add(leftBound);
        top.add(rightBound);
        bottom.add(leftBound);
        bottom.add(rightBound);

        Collections.sort(left);
        Collections.sort(right);
        Collections.sort(top);
        Collections.sort(bottom);
    }

    private void addPoint(Point p) {
        leftBound = Math.min(leftBound, p.x);
        rightBound = Math.max(rightBound, p.x);
        topBound = Math.min(topBound, p.y);
        bottomBound = Math.max(bottomBound, p.y);



        boolean l = p.x < ((leftBound + rightBound) / 2);
        boolean t = p.y < ((topBound + bottomBound) / 2);
        if (l && t) {
            left.add(p.y);
            top.add(p.x);
        }
        if (l && !t) {
            left.add(p.y);
            bottom.add(p.x);
        }
        if (!l && t) {
            right.add(p.y);
            top.add(p.x);
        }
        if (!l && !t) {
            right.add(p.y);
            bottom.add(p.x);
        }

        pointsCount++;
    }
    public void draw(Canvas canvas) {
        if (pointsCount < 2) return;
        drawLine(left, leftBound - OFFSET, false, canvas);
        drawLine(right, rightBound + OFFSET, false, canvas);
        drawLine(top, topBound - OFFSET, true, canvas);
        drawLine(bottom, bottomBound + OFFSET, true, canvas);
    }

    private void drawLine(ArrayList<Double> coords, double baseCoordinate, boolean horizontal, Canvas canvas) {
        Iterator<Double> it = coords.iterator();
        double last = it.next();
        while (it.hasNext()) {
            double cur = it.next();
            if (it.hasNext() && Math.abs(last - cur) < 10) continue;
            double x1, x2, y1, y2;
            if (horizontal) {
                x1 = last;
                x2 = cur;
                y1 = y2 = baseCoordinate;
            } else {
                y1 = last;
                y2 = cur;
                x1 = x2 = baseCoordinate;
            }
            drawLineSegment(x1, x2, y1, y2, horizontal, canvas);
            last = cur;

        }
    }

    private void drawLineSegment(double x1, double x2, double y1, double y2, boolean horizontal, Canvas canvas) {
        final int DASH = 10;
        Paint paint = Util.getPaint(Color.rgb(0, 255 * 2 / 3, 0));
        paint.setStrokeWidth(10);
        canvas.drawLine((float)x1, (float)y1, (float)x2, (float)y2, paint);

        paint.setStrokeWidth(3);
        paint.setTextSize(30);
        double dist = new Point(x1, y1).sub(new Point(x2, y2)).length();
        String text = Double.toString(Util.setPrecision(dist * Maths.M_TO_INCH, 0));
        float width = paint.measureText(text);
        double textX, textY;
        if (horizontal) {
            canvas.drawLine((float)x1, (float)y1 - DASH, (float)x1, (float)y2 + DASH, paint);
            canvas.drawLine((float)x2, (float)y1 - DASH, (float)x2, (float)y2 + DASH, paint);
            textX = (x1 + x2) / 2;
            textY = y1 - TEXT_OFFSET;
        } else {
            canvas.drawLine((float)x1 - DASH, (float)y1, (float)x2 + DASH, (float)y1, paint);
            canvas.drawLine((float)x1 - DASH, (float)y2, (float)x2 + DASH, (float)y2, paint);
            textX = x1 - TEXT_OFFSET;
            textY = (y1 + y2) / 2;
        }
        if (text.equals("0.0")) return;
        canvas.save();
        canvas.translate((float)textX, (float)textY);
        if (!horizontal)
            canvas.rotate(-90);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.MONOSPACE);
        canvas.drawText(text, -width / 2, 0, paint);
        canvas.restore();
    }
}
