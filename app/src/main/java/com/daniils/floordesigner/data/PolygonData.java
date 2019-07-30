package com.daniils.floordesigner.data;

import com.daniils.floordesigner.Point;

import java.io.Serializable;
import java.util.ArrayList;

public class PolygonData implements Serializable {
    public ArrayList<Point> path = new ArrayList<>();
    public String label = "";
    public double rotation = 0, scale = 1;
}