package com.daniils.floordesigner.util;

public class Binsearch {
    @FunctionalInterface
    public interface Comparator {
        boolean less(Double x);
    }
    public static double perform(double min, double max, double eps, Comparator comparator) {
        while (max - min > eps) {
            double x = min + (max - min) / 2;
            if (comparator.less(x)) {
                min = x;
            } else {
                max = x;
            }
        }
        return min;
    }
}
