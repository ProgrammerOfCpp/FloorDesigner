package com.daniils.floordesigner.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.daniils.floordesigner.Point;
import com.daniils.floordesigner.Polygon;
import com.daniils.floordesigner.Vertex;

import java.util.ArrayList;
import java.util.LinkedList;

public class Util {
    public static ArrayList<Point> createRoundPath(int n, double radius, Point center) {
        double a = 0;
        ArrayList<Point> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = center.x + radius * Math.cos(a);
            double y = center.y + radius * Math.sin(a);
            a += 2 * Math.PI / n;
            out.add(new Point(x, y));
        }
        return out;
    }

    public static Paint getPaint(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        return paint;
    }

    public static double setPrecision(double v, int digits) {
        double scale = Math.pow(10, digits);
        return (int)((v * scale) + 0.5) * 1.0 / scale;
    }

    public static void drawMultiline(Canvas g, Paint paint, Point center, String text) {
        String[] lines = text.split("\n");
        int y = (int)center.y - (int)(lines.length * paint.getTextSize()) / 2;
        for (String s : lines) {
            int x = (int)center.x - (int)paint.measureText(s) / 2;
            g.drawText(s, x, y, paint);
            y += paint.getTextSize();
        }
    }

    public static Vertex getClosestSegment(LinkedList<Polygon> polygons, Point point, double minDist) {
        Vertex out = null;
        for (Polygon poly : polygons) {
            for (Vertex v : poly.vertices) {
                Point p = Maths.getRelativeCoords(v.p, v.next.p, point);
                double maxX = v.next.p.sub(v.p).length();
                if (p.x > 0 && p.x < maxX && Math.abs(p.y) <= minDist) {
                    minDist = Math.abs(p.y);
                    out = v;
                }
            }
        }
        return out;
    }
}
