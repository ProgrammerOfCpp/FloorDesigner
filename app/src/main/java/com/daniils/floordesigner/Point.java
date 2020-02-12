package com.daniils.floordesigner;

import com.daniils.floordesigner.util.Maths;

import java.io.Serializable;

public class Point implements Serializable {
    public static final Point zero = new Point(0, 0);
    public double x, y;
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point point) {
        this.x = point.x;
        this.y = point.y;
    }

    public Point add(Point point) {
        return new Point(
                this.x + point.x,
                this.y + point.y);
    }

    public Point sub(Point point) {
        return new Point(
                this.x - point.x,
                this.y - point.y);
    }

    public Point scale(double scale) {
        return new Point(this.x * scale, this.y * scale);
    }

    public double length() {
        return Maths.dist(Point.zero, this);
    }

    public Point normalize() {
        double k = 1 / length();
        return this.scale(k);
    }

    public Point setLength(double length) {
        return this.normalize().scale(length);
    }

    public boolean isNan() {
        return Double.isNaN(x) || Double.isNaN(y);
    }

    public boolean equals(Point point) {
        return Maths.equals(this.x, point.x) && Maths.equals(this.y, point.y);
    }

    @Override
    public String toString() {
        return "(" + (int)(this.x + 0.5) + "," + (int)(this.y + 0.5) + ")";
    }
}
