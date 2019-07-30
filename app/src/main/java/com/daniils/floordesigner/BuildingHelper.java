package com.daniils.floordesigner;

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
        return out;
    }
}
