package com.daniils.floordesigner;

import com.daniils.floordesigner.util.Binsearch;
import com.daniils.floordesigner.util.Maths;

import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BuildingHelper {
    private static final double MIN_DIST = 50;
    private static final double MIN_DIST_LAST = 80;
    private static final double MIN_THETA = Math.toRadians(30);

    public static ArrayList<Point> buildPolyFromPath(List<Point> path, double scale) {
        if (path.size() < 3)
            return null;

        ArrayList<Point> out = new ArrayList<>();

        Point c = path.get(0);
        out.add(c);

        Iterator<Point> itA = path.iterator();
        Iterator<Point> itB = path.iterator(); itB.next();
        while(itB.hasNext()) {
            Point a = itA.next();
            Point b = itB.next();
            double theta = Maths.theta(c, a, a, b);
            double dist = Math.min(
                    Math.min(
                            Maths.dist(c, a),
                            Maths.dist(c, b)),
                    Maths.dist(path.get(0), b)
            );
            if (dist > MIN_DIST / scale && theta > MIN_THETA) {
                out.add(b);
                c = a;
            }
            if (out.size() > 1) {
                if (Maths.dist(path.get(0), out.get(out.size() - 1)) < MIN_DIST_LAST / scale) {
                    out.remove(out.size() - 1);
                }
            }
        }

        if (out.size() < 3)
            return null;
        processCorners(out);
        return out;
    }

    private static void processCorners(ArrayList<Point> points) {
        for (int i = 0; i < points.size(); i++) {
            Point a = points.get((i) % points.size());
            Point b = points.get((i + 1) % points.size());
            Point c = points.get((i + 2) % points.size());
            Point[] dir = MovementCorrector.getBestDirection(new Point [] { a, b }, c);
            if (i < points.size() - 1) {
                points.set((i + 2) % points.size(), Maths.projectTo(c, dir[0], dir[1]));
            } else {
                Point d = points.get((i + 3) % points.size());
                points.set((i + 2) % points.size(), Maths.intersection(c, d, dir[0], dir[1]));
            }
        }
    }

    private static Point[] getBestDirection(final Point a, final Point b, final Point c) {
        double[] vals = MovementCorrector.angles;
        Point[] bestDirection = null;
        double bestDist = Double.MAX_VALUE;
        for (double theta : vals) {
            Point A = b, B = Maths.rotate(a.sub(b), theta).add(A);
            Point p = Maths.projectTo(c, A, B);
            double dist = p.sub(c).length();
            if (dist < bestDist) {
                bestDist = dist;
                bestDirection = new Point[] { A, B };
            }
        }
        return bestDirection;
    }

        /*
        Math.PI / 4, Math.PI / 2, Math.PI * 3 / 4, Math.PI
        double[] vals = { 0.125 * Math.PI, 0.185 * Math.PI, 0.5 * Math.PI / 4, Math.PI / 2 };
        double bestTheta = vals[0];
        double theta = Maths.theta(b, c) - Maths.theta(b, a);
        for (int i = 0; i < 2; i++) {
            double a0 = 0;
            for (int j = 0; j < 4; j++, a0 += Math.PI / 2) {
                double scale = 1 - 2 * i;
                for (double val : vals) {
                    if (Math.abs(Math.PI  * 2 - (val + a0)) < Maths.E)
                        continue;// 2п is not good
                    val = (val + a0) * scale;
                    if (Math.abs(val - theta) < Math.abs(bestTheta - theta)) {
                        bestTheta = val;
                    }
                }
            }
        }*/
        /*final Point o = Maths.getCircleCenter(a, b, c);
        final Point ob = b.sub(o);
        final double r0 = ob.length();
        final double thetaTarget = Math.PI / 2 + 0 * best;
        double r = Binsearch.perform(0, r0 * 10, Maths.E, new Binsearch.Comparator() {
            @Override
            public boolean less(Double x) {
                Point v = ob.scale(x / r0);
                Point t = o.add(v);
                double theta = Maths.theta(t, a, t, c);
                return theta > thetaTarget;
            }
        });
        return o.add(ob.scale(r / r0));*/
}
