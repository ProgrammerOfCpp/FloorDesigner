

package com.daniils.floordesigner;

import java.util.Arrays;
import java.util.LinkedList;

class MovementCorrector {
    private class CorrectionVariant {
        Point[][] lines;
        Point p;
        private double priority;

        CorrectionVariant(Point p, Point[][] lines, double priority) {
            this.p = p;
            this.lines = lines;
            this.priority = priority;
        }
    }

    private final double CORRECTION_DIST = 50;
    private final Vertex v;
    private final LinkedList<Point[]> directionalLines;
    private LinkedList<CorrectionVariant> correctionVariants = new LinkedList<>();

    MovementCorrector(Vertex v, LinkedList<Point[]> directionalLines) {
        this.v = v;
        if (directionalLines == null)
            this.directionalLines = new LinkedList<>();
        else
            this.directionalLines = directionalLines;
    }

    void performMovement(Point dest) {
        Point delta = dest.sub(v.p);
        double bestLen = delta.length();
        boolean intersects = moveBy(delta, true);
        if (intersects) {
            bestLen = 0;
            double l = 0, r = delta.length();
            double prevLen = Double.MAX_VALUE;
            while (true) {
                double len = l + (r - l) / 2;
                delta = delta.setLength(len);
                if (Math.abs(len - prevLen) < 1)
                    break;
                prevLen = len;

                boolean intersection = moveBy(delta, true);
                if (intersection) {
                    r = len;
                } else {
                    bestLen = len;
                    l = len;
                }
            }
        }
        delta = delta.setLength(bestLen);
        moveBy(delta,false);

        v.prev.updateBisector();
        v.updateBisector();
        v.next.updateBisector();
        v.next.next.updateBisector();

        v.prev.updateOutline();
        v.updateOutline();
        v.next.updateOutline();
        v.next.next.updateOutline();
        v.polygon.updateOutline();
    }

    private boolean moveBy(Point delta, boolean testOnly) {
        Point pOld = new Point(v.p);
        Point pNextOld = new Point(v.next.p);
        Point to = v.p.add(delta);

        Point offset = null;
        if (v.next.selected) {
            offset = Maths.calculateSegmentOffset(v.p, v.next.p, to);
            v.p = v.p.add(offset);
            v.next.p = v.next.p.add(offset);
        } else {
            v.p = to;
        }
        boolean intersection =
                !v.polygon.canExist() ||
                v.next.next.getIntersection() != null ||
                v.next.getIntersection() != null ||
                v.getIntersection() != null ||
                v.prev.getIntersection() != null;
        if (intersection || testOnly) {
            v.p = pOld;
            v.next.p = pNextOld;
        }
        if (!testOnly) {
            pOld = new Point(v.p);
            pNextOld = new Point(v.next.p);
            if (v.next.selected) {
                alignSegment(offset);
            } else {
                alignPoint(to);
            }
            intersection =
                    !v.polygon.canExist() ||
                    v.next.next.getIntersection() != null ||
                    v.next.getIntersection() != null ||
                    v.getIntersection() != null ||
                    v.prev.getIntersection() != null;
            if (intersection) {
                v.p = pOld;
                v.next.p = pNextOld;
                directionalLines.clear();
            }
        }
        return intersection;
    }

    private void alignSegment(Point offset) {
        Point delta = v.next.p.sub(v.p);
        updateVariantsForSegmentMovement(v.p, offset);
        Point pAligned = align(v.p);
        if (pAligned != null) {
            v.p = pAligned;
            v.next.p = v.p.add(delta);
            return;
        }
        updateVariantsForSegmentMovement(v.next.p.add(offset), offset);
        Point pNextAligned = align(v.next.p.add(offset));
        if (pNextAligned != null) {
            v.next.p = pNextAligned;
            v.p = v.next.p.sub(delta);
        }
    }

    private void alignPoint(Point to) {
        updateVariantsForPointMovement(to);

        Point pAligned = align(to);
        if (pAligned != null) {
            v.p = pAligned;
            return;
        }
        v.p = to;
    }

    private void updateVariantsForPointMovement(Point point) {
        Point[] dir1 = getClosestDirectionForPointMovement(point, v.prev, false);
        Point[] dir2 = getClosestDirectionForPointMovement(point, v.next, true);
        correctionVariants.clear();
        if (dir1 != null && dir2 != null) {
            Point c = Maths.intersection(dir1[0], dir1[1], dir2[0], dir2[1], true);
            correctionVariants.add(new CorrectionVariant(c, new Point[][] {{ v.prev.p, c },{ c, v.next.p }}, 0.5));
        }
        if (dir1 != null) {
            Point c = Maths.projectTo(point, dir1[0], dir1[1]);
            correctionVariants.add(new CorrectionVariant(c, new Point[][] {{ c, v.prev.p}, { v.prev.p, v.prev.prev.p }}, 2));
        }
        if (dir2 != null) {
            Point c = Maths.projectTo(point, dir2[0], dir2[1]);
            correctionVariants.add(new CorrectionVariant(c, new Point[][] {{ c, v.next.p}, { v.next.p, v.next.next.p }}, 2));
        }
    }

    private Point[] getClosestDirectionForPointMovement(Point point, Vertex v, boolean forward) {
        Point a = v.p;
        Point b = (forward ? v.next.p : v.prev.p);
        double startAngle = Maths.theta(a, b);
        /*double[] angles = new double[]{
                0, Math.PI/6, Math.PI / 4, Math.PI / 3,
                Math.PI / 2, Math.PI * 2/3, Math.PI *3/4, Math.PI * 5/6 };*/
        double[] angles = new double[] {
                0, Math.PI * 1 / 8, Math.PI * 1 / 4, Math.PI * 2 / 4, Math.PI * 3 / 4
        };
        Point[] best = null;
        double bestDist = CORRECTION_DIST;
        for (double angle : angles) {
            double theta = angle + startAngle;
            a = v.p;
            b = Maths.getRotatedPoint(theta, 1).add(a);
            double dist = Maths.dist(a, b, point);
            if (dist < bestDist) {
                bestDist = dist;
                best = new Point[]{ a, b };
            }
        }
        return best;
    }

    private void updateVariantsForSegmentMovement(Point point, Point offset) {
        Point[] dir = getClosestDirectionForSegmentMovement(point);
        correctionVariants.clear();
        if (dir != null) {
            Point c = Maths.intersection(dir[0], dir[1], point, point.add(offset));
            correctionVariants.add(new CorrectionVariant(c, new Point[][] { dir }, 1));
        }
    }

    private Point[] getClosestDirectionForSegmentMovement(Point point) {
        Point[] best = null;
        double bestDist = CORRECTION_DIST;

        for (Polygon poly : v.polygon.drawingView.polygons) {
            if (poly == v.polygon) ///????????????????????????
                continue;
            for (Vertex u : poly.vertices) {
                Point a = u.p;
                Point b = u.next.p;
                Point rel = Maths.getRelativeCoords(a, b, point);
                if (rel.x >= 0 && rel.x <= Maths.dist(a, b) && Math.abs(rel.y) < bestDist) {
                    bestDist = Math.abs(rel.y);
                    best = new Point[] { a, b };
                }
            }
        }
        return best;
    }

    private Point align(Point point) {
        Point out = null;
        double bestDist = CORRECTION_DIST;
        for (int i = 0; i < correctionVariants.size(); i++) {
            CorrectionVariant variant = correctionVariants.get(i);
            if (variant.p == null)
                continue;
            double dist = Maths.dist(point, variant.p) * variant.priority;
            if (dist < bestDist) {
                bestDist = dist;
                out = variant.p;
                directionalLines.clear();
                directionalLines.addAll(Arrays.asList(variant.lines));
            }
        }
        return out;
    }
}
