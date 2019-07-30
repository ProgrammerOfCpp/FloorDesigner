package com.daniils.floordesigner;

import java.util.ArrayList;

public class Selectable {
    public boolean selected = false;

    public void setSelected(ArrayList<Selectable> selection, boolean selected) {
        if (selection != null) {
            if (selected && !this.selected)
                selection.add(this);
            if (!selected && this.selected)
                selection.remove(this);
        }
        this.selected = selected;
    }

    public void touchDragged(Point point) {

    }
}
