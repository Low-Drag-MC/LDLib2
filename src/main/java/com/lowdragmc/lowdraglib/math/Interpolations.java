package com.lowdragmc.lowdraglib.math;

/**
 * @author KilaBash
 * @date 2022/6/17
 * @implNote Interpolations
 */
public class Interpolations {
    
    public static double CatmullRom(double t, double  p0, double  p1, double  p2, double  p3) {
        double v0 = ( p2 - p0 ) * 0.5;
        double v1 = ( p3 - p1 ) * 0.5;
        double t2 = t * t;
        double t3 = t * t2;
        return ( 2 * p1 - 2 * p2 + v0 + v1 ) * t3 + ( - 3 * p1 + 3 * p2 - 2 * v0 - v1 ) * t2 + v0 * t + p1;
    }

    public static double QuadraticBezierP0(double t, double p) {
	    double k = 1 - t;
        return k * k * p;

    }

    public static double QuadraticBezierP1(double t, double p) {
        return 2 * ( 1 - t ) * t * p;
    }

    public static double QuadraticBezierP2(double t, double p) {
        return t * t * p;
    }

    public static double QuadraticBezier(double t, double p0, double p1, double p2) {
        return QuadraticBezierP0(t, p0) + QuadraticBezierP1(t, p1) + QuadraticBezierP2(t, p2);
    }

    public static double CubicBezierP0(double t, double p) {
        double k = 1 - t;
        return k * k * k * p;
    }

    public static double CubicBezierP1(double t, double p) {
        double k = 1 - t;
        return 3 * k * k * t * p;
    }

    public static double CubicBezierP2(double t, double p) {
        return 3 * ( 1 - t ) * t * t * p;
    }

    public static double CubicBezierP3(double t, double p) {
        return t * t * t * p;
    }

    public static double CubicBezier(double t, double p0, double p1, double p2, double p3) {
        return CubicBezierP0(t, p0) + CubicBezierP1(t, p1) + CubicBezierP2(t, p2) + CubicBezierP3(t, p3);
    }
}
