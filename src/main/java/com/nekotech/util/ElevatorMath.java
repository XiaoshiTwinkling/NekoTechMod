package com.nekotech.util;

public final class ElevatorMath {
    private ElevatorMath() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    /**
     * 先加速后减速。
     */
    public static double easeInOut(double t) {
        t = clamp(t, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }
}