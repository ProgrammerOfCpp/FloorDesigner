package com.daniils.floordesigner;

import java.util.LinkedList;

public class MovementFilter {
    Point sqrSum;
    LinkedList<Point> lst;
    int bufferSize;

    public MovementFilter(int bufferSize)
    {
        reset();
        this.bufferSize = bufferSize;
    }

    public void reset() {
        sqrSum = new Point(0, 0);
        lst = new LinkedList<>();
    }

    public Point update(Point p) {
        lst.add(new Point(p));
        sqrSum.x += p.x * p.x;
        sqrSum.y += p.y * p.y;
        if (lst.size() > bufferSize) {
            Point l = lst.removeFirst();
            sqrSum.x -= l.x * l.x;
            sqrSum.y -= l.y * l.y;
        }
        return getPosition();
    }

    public Point getPosition() {
        double x = Math.sqrt(sqrSum.x / lst.size());
        double y = Math.sqrt(sqrSum.y / lst.size());
        return new Point(x, y);
    }
}
