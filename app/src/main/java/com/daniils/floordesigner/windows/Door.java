package com.daniils.floordesigner.windows;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.daniils.floordesigner.Point;
import com.daniils.floordesigner.Polygon;
import com.daniils.floordesigner.Vertex;

public class Door extends Window {

    public Door(Point p, Vertex v) {
        super(p, v);
    }

    public Door(double left, double right, Vertex v) {
        super(left, right, v);
    }

    @Override
    public double getWidth() {
        return 200;
    }

    public int getClassId() {
        return 1;
    }

    @Override
    protected void drawDetails(Canvas c, Paint paint) {
        paint.setColor(Polygon.fillColor);
        paint.setStrokeWidth((float)Vertex.THICKNESS);
        float width = getLength();
        c.drawLine(0, 0, width, 0, paint);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);
        float vt = (float)Vertex.THICKNESS;
        float hh = vt * 0.1875f;
        float ww = hh;
        c.drawLine(0, -hh, width, -hh, paint);
        c.drawRect(0, hh, ww, -hh, paint);
        c.drawRect(width - ww, hh, width, -hh, paint);
        c.drawRect(ww, -width, ww * 2, -hh, paint);
        float radius = width - 3 * ww;
        c.drawArc(ww * 2 - radius, -hh - radius - hh * 2, ww * 2 + radius, -hh + radius + hh * 2, 0, -90, true, paint);
    }
}
