package com.daniils.floordesigner;

import com.daniils.floordesigner.view.DrawingView;

import java.util.ArrayList;

public class Selectable {
    public boolean selected = false;
    protected Point prevPoint = null;
    public void setSelected(ArrayList<Selectable> selection, boolean selected) {
        if (selection != null) {
            if (selected && !this.selected)
                selection.add(this);
            if (!selected && this.selected)
                selection.remove(this);
        }
        this.selected = selected;
    }

    public boolean touchDown(Point point) {
        prevPoint = new Point(point);
        return false;
    }

    public void touchMove(Point point) {
        if (prevPoint == null) return;
        Point delta = point.sub(prevPoint);
        double remainingDistance = delta.length();
        System.out.println(remainingDistance);
        Point to = new Point(prevPoint);
        while (remainingDistance > 0) {
            double step = Math.min(remainingDistance, DrawingView.DRAG_MOVEMENT_STEP);
            to = to.add(delta.setLength(step));
            remainingDistance -= step;

            boolean stop = !processMovement(to);

            prevPoint = new Point(to);

            if (stop) {
                break;
            }
        }
    }

    public boolean processMovement(Point point) {
        return true;
    }

    public void touchUp(Point point) {

    }
}
