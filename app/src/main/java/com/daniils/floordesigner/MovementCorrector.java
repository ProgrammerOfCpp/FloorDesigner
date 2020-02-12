

package com.daniils.floordesigner;

import com.daniils.floordesigner.util.Binsearch;
import com.daniils.floordesigner.util.Maths;

import java.util.Arrays;
import java.util.LinkedList;

class MovementCorrector {

    public static final double[] angles = {
            0, 0.125 * Math.PI, 0.185 * Math.PI, Math.PI * 0.250, Math.PI * 0.375 , 0.500 * Math.PI, 0.750 * Math.PI, 1.000 * Math.PI,
            1.125 * Math.PI, 1.185 * Math.PI, Math.PI * 1.250, Math.PI * 1.375 , 1.500 * Math.PI, 1.750 * Math.PI
    };
    private final double BINSEARCH_E = 0.1;

    private final Vertex v;
    private final LinkedList<Point[]> directionalLines;
    private Point crossingPoint;

    private Point tmpVP, tmpVNP;

    MovementCorrector(Vertex v, LinkedList<Point[]> directionalLines) {
        this.v = v;
        if (directionalLines == null)
            this.directionalLines = new LinkedList<>();
        else
            this.directionalLines = directionalLines;

        this.directionalLines.clear();

        recalculateCrossingPoint();
    }

    public boolean performMovement(Point dest) {
        boolean fixedOnEdge = moveToDestination(dest);

        refreshOutlineAndBisectors();

        return fixedOnEdge;
    }

    private boolean moveToDestination(Point dest) {
        final Point offset = v.next.selected ? Maths.calculateSegmentOffset(v.p, v.next.p, dest) : dest.sub(v.p);
        moveByOffset(offset);
        if (hasIntersection()) {
            loadState();
            double len = Binsearch.perform(0, offset.length(), BINSEARCH_E, x -> {
                moveByOffset(offset.setLength(x));
                boolean its = hasIntersection();
                loadState();
                return !its;
            });
            moveByOffset(offset.setLength(len));
            return true;
        }
        return false;
    }

    private void saveState() {
        tmpVP = new Point(v.p);
        tmpVNP = new Point(v.next.p);
    }

    private void loadState() {
        v.p = new Point(tmpVP);
        v.next.p = new Point(tmpVNP);
    }

    public void moveByOffset(Point offset) {
        saveState();
        v.p = v.p.add(offset);
        if (v.next.selected) {
            v.next.p = v.next.p.add(offset);
        }
    }

    private void refreshOutlineAndBisectors() {
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

    private boolean hasIntersection() {
        return !v.polygon.canExist() ||
                v.next.getIntersection() != null ||
                v.getIntersection() != null ||
                v.prev.getIntersection() != null;
    }

    public void recalculateCrossingPoint() {
        if (v.next.selected) return;
        Point[] firstEdge = { v.prev.p, v.prev.prev.p };
        Point[] secondEdge = { v.next.p, v.next.next.p };
        crossingPoint = getBestDirectionsCrossing(firstEdge, secondEdge, v.p);
    }

    public void snap() {
        if (v.next.selected) return;
        performMovement(crossingPoint);
    }

    public static Point[][] getAllDirections(final Point[] edge) {
        double[] vals = MovementCorrector.angles;
        Point[][] directions = new Point[vals.length][];
        for (int i = 0; i < directions.length; i++) {
            double theta = vals[i];
            Point A = edge[0], B = Maths.rotate(edge[0].sub(edge[1]), theta).add(edge[0]);
            directions[i] = new Point[] { A, B };
        }
        return directions;
    }

    public Point getBestDirectionsCrossing(final Point[] firstEdge, final Point[] secondEdge, final Point a) {
        Point[][] dirsFirst = getAllDirections(firstEdge);
        Point[][] dirsSecond = getAllDirections(secondEdge);

        Point bestPoint = null;
        double bestDist = Double.MAX_VALUE;
        for (Point[] dir1 : dirsFirst) {
            for (Point[] dir2 : dirsSecond) {
                Point p = Maths.intersection(dir1[0], dir1[1], dir2[0], dir2[1]);
                if (p == null) continue;
                double dist = Maths.dist(a, p);
                if (dist < bestDist) {
                    directionalLines.clear();
                    directionalLines.add(dir1);
                    directionalLines.add(dir2);
                    bestDist = dist;
                    bestPoint = p;
                }
            }
        }
        return bestPoint;
    }

    public static Point[] getBestDirection(final Point[] edge, final Point a) {
        Point[] bestDirection = null;
        double bestDist = Double.MAX_VALUE;

        Point[][] directions = getAllDirections(edge);
        for (Point[] dir : directions) {
            Point p = Maths.projectTo(a, dir[0], dir[1]);
            double dist = Maths.dist(a, p);
            if (dist < bestDist) {
                bestDist = dist;
                bestDirection = dir;
            }
        }
        return bestDirection;
    }


/*
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
        //<1>
        if (v.next.selected) {
            alignSegment(offset);
        } else {
            alignPoint(to);
        }
        //</1>
        boolean intersection = hasIntersection();
        if (intersection || testOnly) {
            v.p = pOld;
            v.next.p = pNextOld;
        }
        /*if (!testOnly) {
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
            correctionVariants.add(new CorrectionVariant(c, new Point[][] {{ v.prev.p, c },{ c, v.next.p }}, 0.1));
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
        double len = b.sub(a).length();
        Point[] best = null;
        double bestDist = CORRECTION_DIST;
        double startAngle = Maths.theta(a, b);
        for (double angle : angles) {
            double theta = angle + startAngle;
            a = v.p;
            b = Maths.getRotatedPoint(theta, len).add(a);
            double dist = Maths.dist(a, b, point);
            if (dist < bestDist) {
                bestDist = dist;
                best = new Point[]{a, b};
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
    } */
}
