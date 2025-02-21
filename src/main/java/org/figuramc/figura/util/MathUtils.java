package org.figuramc.figura.util;

public class MathUtils {

    public static boolean isInteger(double d) {
        return isInteger(d, 1e-5); // Reasonable default epsilon
    }
    public static boolean isInteger(double d, double epsilon) {
        return Math.abs(d - Math.round(d)) <= epsilon;
    }


}
