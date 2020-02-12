package com.daniils.floordesigner;

public class Shapes {

    public static class Shape {
        public String name;
        public Point[] points;

        Shape(String name, Point[] points) {
            this.name = name;
            this.points = points;
        }
    }

    public static final Shape[] shapes = {
        new Shape("Square", new Point[] {
                new Point(0, 0),
                new Point(1, 0),
                new Point(1, 1),
                new Point(0, 1)
        }), new Shape("L Shape", new Point[] {
            new Point(0, 0),
            new Point(0.33, 0),
            new Point(0.33, 0.66),
            new Point(1, 0.66),
            new Point(1, 1),
            new Point(0, 1)
        }), new Shape("T Shape", new Point[] {
            new Point(0, 0),
            new Point(1, 0),
            new Point(1, 0.33),
            new Point(0.66, 0.33),
            new Point(0.66, 1),
            new Point(0.33, 1),
            new Point(0.33, 0.33),
            new Point(0, 0.33)
        }), new Shape("U Shape", new Point[] {
            new Point(0, 0),
            new Point(0.33, 0),
            new Point(0.33, 0.66),
            new Point(0.66, 0.66),
            new Point(0.66, 0),
            new Point(1, 0),
            new Point(1, 1),
            new Point(0, 1)
        })
    };
}
