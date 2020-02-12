package com.daniils.floordesigner.util;

import com.daniils.floordesigner.Point;

import java.util.Arrays;

public class Maths {
    public static final double E = 0.000000000001;
    public static final double M_TO_INCH = 0.01471587849;

    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < E;
    }

    public static double clamp(double v, double a, double b) {
        if (b < a) {
            double c = a;
            a = b;
            b = c;
        }
        if (v < a)  v = a;
        if (v > b)  v = b;
        return v;
    }

    public static double dist(Point A, Point B) {
        double dx = B.x - A.x;
        double dy = B.y - A.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public static double dist(Point A, Point B, Point C) {
        double a = dist(A, B);
        double b = dist(B, C);
        double c = dist(C, A);
        double p = (a + b + c) / 2;
        double S = Math.sqrt(p*(p-a)*(p-b)*(p-c));
        return 2 * S / a;
    }

    public static Point getRelativeCoords(Point A, Point B, Point C) {
        double theta = theta(A, B, A, C);
        double len = dist(A, C);
        int x = (int)(Math.cos(theta) * len + 0.5);
        int y = (int)(Math.sin(theta) * len + 0.5);
        return new Point(x, y);
    }

    public static double theta(Point A, Point B, Point C, Point D) {
        double a = dist(A, B);
        double b = dist(C, D);
        if (a == 0 || b == 0)
            return Math.PI;
        double x1 = B.x - A.x;
        double y1 = B.y - A.y;
        double x2 = D.x - C.x;
        double y2 = D.y - C.y;
        double mul = x1*x2 + y1*y2;
        double cos = mul / (a * b);
        double out = Math.acos(cos);
        if (Double.isNaN(out))
            out = 0;
        return out;
    }

    public static double theta(Point A, Point B) {
        double x = B.x - A.x;
        double y = B.y - A.y;
        return Math.atan2(y, x);
    }

    public static Point rotate(Point A, double alpha) {
        double x = A.x * Math.cos(alpha) - A.y * Math.sin(alpha);
        double y = A.x * Math.sin(alpha) + A.y * Math.cos(alpha);
        return new Point(x, y);
    }

    public static Point intersection(Point A, Point B, Point C, Point D) {
        double x2 = D.x - C.x;
        if (x2 == 0) {
            Point tmp = new Point(A);
            A = new Point(C);
            C = new Point(tmp);
            tmp = new Point(B);
            B = new Point(D);
            D = new Point(tmp);
            x2 = D.x - C.x;
        }
        double x1 = B.x - A.x;
        double y1 = B.y - A.y;
        double k1 = y1 / x1;
        double b1 = A.y - k1 * A.x;
        double y2 = D.y - C.y;
        double k2 = y2 / x2;
        double b2 = C.y - k2 * C.x;
        if (k1 == k2 || (y1 == y2 && y2 == 0) || (x1 == x2 && x2 == 0))
            return null;
        double x, y;
        if (x1 == 0) {
            if (y2 == 0) {
                x = A.x;
                y = C.y;
            } else {
                x = A.x;
                y = k2 * x + b2;
            }
        } else {
            if (y2 == 0) {
                y = C.y;
                x = (y - b1) / k1;
            } else {
                x = (b2 - b1) / (k1 - k2);
                y = k1 * x + b1;
            }
        }
        return new Point(x, y);
    }

    public static boolean fitsRect(Point A, Point B, Point C) {
        double l = Math.min(A.x, B.x);
        double t = Math.min(A.y, B.y);
        double r = Math.max(A.x, B.x);
        double b = Math.max(A.y, B.y);
        return C.x >= l && C.x <= r && C.y >= t && C.y <= b;
    }

    public static Point intersection(Point A, Point B, Point C, Point D, boolean infinite) {
        Point p = intersection(A, B, C, D);
        if (!infinite && p != null) {
            if (!fitsRect(A, B, p) || !fitsRect(C, D, p))
                return null;
        }
        return p;
    }

    public static Point getCircleCenter(Point A, Point B, Point C) {
        A = A.sub(C);
        B = B.sub(C);
        double a1 = 2 * A.x, b1 = 2 * A.y, c1 = A.x * A.x + A.y * A.y;
        double a2 = 2 * B.x, b2 = 2 * B.y, c2 = B.x * B.x + B.y * B.y;
        if (a1 == 0 && b2 == 0) {
            return getCircleCenter(B.add(C), A.add(C), C);
        }
        double E = 0;
        if (a1 * b2 == a2 * b1) {
            E = Maths.E;
        }
        double y = (a1 * c2 - a2 * c1) / (a1 * b2 - a2 * b1 + E);
        double x = (c1 - b1 * y) / a1;
        return new Point(x, y).add(C);
    }

    public static Point projectTo(Point point, Point a, Point b) {
        Point out = new Point(point);
        if (a.x == b.x) {
            out.x = a.x;
        } else if (a.y == b.y) {
            out.y = a.y;
        } else {
            double theta = theta(a, point, a, b);
            double len = dist(a, point) * Math.cos(theta);
            out = b.sub(a).setLength(len).add(a);
        }
        return out;
    }

    public static Point calculateSegmentOffset(Point A, Point B, Point to) {
        double dist = dist(A, B, to);
        double len = dist(A, B);
        double x = (B.x - A.x) / len * dist;
        double y = (B.y - A.y) / len * dist;
        Point o1 = new Point(-y, x);
        Point o2 = new Point(y, -x);
        Point p1 = new Point(A.x + o1.x, A.y + o1.y);
        Point p2 = new Point(A.x + o2.x, A.y + o2.y);
        double d1 = dist(p1, to);
        double d2 = dist(p2, to);
        if (d1 < d2)
            return o1;
        else
            return o2;
    }

    public static Point getRotatedPoint(double angle, double len) {
        return new Point(Math.cos(angle) * len, Math.sin(angle) * len);
    }
}
