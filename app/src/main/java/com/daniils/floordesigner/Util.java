package com.daniils.floordesigner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.ArrayList;

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
            System.out.println("ROT " + s);
            int x = (int)center.x - (int)paint.measureText(s) / 2;
            g.drawText(s, x, y, paint);
            y += paint.getTextSize();
        }
    }
}
