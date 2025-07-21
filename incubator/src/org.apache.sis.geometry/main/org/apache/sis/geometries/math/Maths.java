/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.geometries.math;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.apache.sis.geometries.math.Vectors.*;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.util.Static;
import org.apache.sis.util.privy.Numerics;


/**
 * Origin : Adapted from Unlicense-Lib
 *
 * Math utilities.
 */
public final class Maths extends Static {

    /**
     * Calculate normal of triangle made of given 3 points.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @return triangle normal
     */
    public static float[] calculateNormal(float[] a, float[] b, float[] c){
        final float[] ab = Vectors.subtract(b,a);
        final float[] ac = Vectors.subtract(c,a);
        final float[] res = cross(ab, ac);
        Vectors.normalize(res, res);
        return res;
    }

    /**
     * Calculate normal of triangle made of given 3 points.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @return triangle normal
     */
    public static double[] calculateNormal(double[] a, double[] b, double[] c){
        final double[] ab = Vectors.subtract(b,a);
        final double[] ac = Vectors.subtract(c,a);
        final double[] res = Vectors.cross(ab, ac);
        Vectors.normalize(res, res);
        return res;
    }

    public static double[] calculateNormalD(float[] a, float[] b, float[] c){
        final double[] ab = subtract(b, a);
        final double[] ac = subtract(c, a);
        final double[] res = cross(ab, ac);
        normalize(res);
        return res;
    }

    public static Vector calculateNormal(Tuple a, Tuple b, Tuple c){
        Vector ab = Vectors.createDouble(a.getDimension());
        ab.add(b);
        ab.subtract(a);
        Vector ac = Vectors.createDouble(a.getDimension());
        ac.add(c);
        ac.subtract(a);
        Vector res = ab.cross(ac);
        res.normalize();
        return res;
    }

    public static double[] subtract(float[] A, float[] B){
        return new double[] {
            A[0] - (double) B[0],
            A[1] - (double) B[1],
            A[2] - (double) B[2]
        };
    }

    public static double[] cross(double[] vector, double[] other){
        final double newX = (vector[1] * other[2]) - (vector[2] * other[1]);
        final double newY = (vector[2] * other[0]) - (vector[0] * other[2]);
        final double newZ = (vector[0] * other[1]) - (vector[1] * other[0]);
        return new double[]{newX,newY,newZ};
    }

    public static float[] cross(float[] vector, float[] other){
        final float newX = (float) ((vector[1] * (double) other[2]) - (vector[2] * (double) other[1]));
        final float newY = (float) ((vector[2] * (double) other[0]) - (vector[0] * (double) other[2]));
        final float newZ = (float) ((vector[0] * (double) other[1]) - (vector[1] * (double) other[0]));
        return new float[]{newX,newY,newZ};
    }

    public static void normalize(float[] vector){
        final float nlength = 1f/(float)length(vector);
        vector[0] *= nlength;
        vector[1] *= nlength;
        vector[2] *= nlength;
    }

    public static void normalize(double[] vector){
        final double nlength = 1d/length(vector);
        vector[0] *= nlength;
        vector[1] *= nlength;
        vector[2] *= nlength;
    }

    public static double length(float[] vector){
        double t;
        final double length = (t = vector[0]) * t
                            + (t = vector[1]) * t
                            + (t = vector[2]) * t;
        return Math.sqrt(length);
    }

    public static double length(double[] vector){
        final double length = vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2];
        return Math.sqrt(length);
    }

    /**
     * Test if the Point p is on the line porting the edge
     *
     * @param a line origin
     * @param b line end
     * @param p Point to test
     * @return true/false
     */
    public static boolean isOnLine(Tuple a, Tuple b, Tuple p) {
//        final double d = Math.abs(distanceSquare(a, b, p));
//        return d < TOLERANCE && d > -TOLERANCE;

        if (lineSide(a,b,p) != 0) return false;
        final double[] ab = {b.get(0) - a.get(0), b.get(1) - a.get(1)};
        final double[] ac = {p.get(0) - a.get(0), p.get(1) - a.get(1)};
        final double e = dot2D(ac, ab);
        // cases where point is outside segment
        if (e <= 0.0f) return false;
        final double f = dot2D(ab, ab);
        if (e >= f) return false;
        return true;
    }

    /**
     * Test if the Point p is on the line porting the edge
     *
     * @param a line origin
     * @param b line end
     * @param p Point to test
     * @return true/false
     */
    public static boolean isOnLine(float[] a, float[] b, float[] p) {
//        final double d = Math.abs(distanceSquare(a, b, p));
//        return d < TOLERANCE && d > -TOLERANCE;

        if (lineSide(a,b,p) != 0) return false;
        final double[] ab = subtract(b,a);
        final double[] ac = subtract(p,a);
        final double e = dot2D(ac, ab);
        // cases where point is outside segment
        if (e <= 0.0f) return false;
        final double f = dot2D(ab, ab);
        if (e >= f) return false;
        return true;
    }

    /**
     * Test the side of a point compare to a line.
     * Only X,Y ordinates are used.
     *
     * @param a line start
     * @param b line end
     * @param c to test
     * @return greater than 0 if point is on the left side
     *          equal 0 if point is on the line
     *          inferior than 0 if point is on the right side
     */
    public static double lineSide(Tuple a, Tuple b, Tuple c) {
        return lineSide(a.get(0), a.get(1), b.get(0), b.get(1), c.get(0), c.get(1));
    }

    /**
     * Test the side of a point compare to a line.
     * Only X,Y ordinates are used.
     *
     * @param a line start
     * @param b line end
     * @param c to test
     * @return greater than 0 if point is on the left side
     *          equal 0 if point is on the line
     *          inferior than 0 if point is on the right side
     */
    public static double lineSide(float[] a, float[] b, float[] c) {
        return lineSide(a[0], a[1], b[0], b[1], c[0], c[1]);
    }

    /**
     * Test the side of a point compare to a line.Only X,Y ordinates are used.
     *
     * @param x1 line start X
     * @param y1 line start Y
     * @param x2 line end X
     * @param y2 line end Y
     * @param x point X
     * @param y point Y
     * @return greater than 0 if point is on the left side
     *          equal 0 if point is on the line
     *          inferior than 0 if point is on the right side
     */
    public static double lineSide(double x1, double y1, double x2, double y2, double x, double y) {
        return (x2 - x1) * (y - y1) - (x - x1) * (y2 - y1);
    }

    /**
     * Test if point is inside triangle using barycentric weights.
     * Only X,Y ordinates are used.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @param p test point
     * @return true if point is inside triangle
     */
    public static boolean isPointInTriangle_BaryAlgo(Tuple a, Tuple b, Tuple c, Tuple p){
        return isPointInTriangle_BaryAlgo(a.get(0), a.get(1), b.get(0), b.get(1), c.get(0), c.get(1), p.get(0), p.get(1));
    }

    /**
     * Test if point is inside triangle using barycentric weights.
     * Only X,Y ordinates are used.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @param p test point
     * @return true if point is inside triangle
     */
    public static boolean isPointInTriangle_BaryAlgo(float[] a, float[] b, float[] c, float[] p){
        return isPointInTriangle_BaryAlgo(a[0], a[1], b[0], b[1], c[0], c[1], p[0], p[1]);
    }

    /**
     * Test if point is inside triangle using barycentric weights.
     *
     * @param x1 triangle first point X
     * @param y1 triangle first point Y
     * @param x2 triangle second point X
     * @param y2 triangle second point Y
     * @param x3 triangle third point X
     * @param y3 triangle third point Y
     * @param x point X
     * @param y point Y
     * @return true if point is inside triangle
     */
    public static boolean isPointInTriangle_BaryAlgo(double x1, double y1, double x2, double y2, double x3, double y3, double x, double y){
        final double[] bary = getBarycentricValue2D(x1, y1, x2, y2, x3, y3, x, y);
        return bary[1] >= 0.0 && bary[2] >= 0.0 && (bary[1] + bary[2]) <= 1.0;
    }
    /**
     * @see Maths#isPointInTriangle_SideAlgo(double, double, double, double, double, double, double, double)
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @param p test point
     * @return true if point is inside triangle
     */
    public static boolean isPointInTriangle_SideAlgo(Tuple a, Tuple b, Tuple c, Tuple p) {
        final double x = p.get(0);
        final double y = p.get(1);
        final double x1 = a.get(0);
        final double y1 = a.get(1);
        final double x2 = b.get(0);
        final double y2 = b.get(1);
        final double x3 = c.get(0);
        final double y3 = c.get(1);
        return isPointInTriangle_SideAlgo(x1,y1, x2,y2, x3,y3, x,y);
    }

    /**
     * Test if point is inside triangle using line side tests (also called dot product).
     * Triangle points are expected to be ordered counterclockwise.
     *
     * @param x1 triangle first point X
     * @param y1 triangle first point Y
     * @param x2 triangle second point X
     * @param y2 triangle second point Y
     * @param x3 triangle third point X
     * @param y3 triangle third point Y
     * @param x point X
     * @param y point Y
     * @return true if point is inside triangle
     */
    public static boolean isPointInTriangle_SideAlgo(double x1, double y1, double x2, double y2, double x3, double y3, double x, double y) {
        return (lineSide(x1, y1, x2, y2, x, y) >= 0)
            && (lineSide(x2, y2, x3, y3, x, y) >= 0)
            && (lineSide(x3, y3, x1, y1, x, y) >= 0);
    }

    /**
     * Test if point is inside triangle using only triangle bounding box.
     *
     * @param x1 triangle first point X
     * @param y1 triangle first point Y
     * @param x2 triangle second point X
     * @param y2 triangle second point Y
     * @param x3 triangle third point X
     * @param y3 triangle third point Y
     * @param x point X
     * @param y point Y
     * @param epsilon bounding box margin
     * @return true if point is inside triangle
     */
    public static boolean isPointInTriangle_BoundingBox(double x1, double y1, double x2, double y2, double x3, double y3, double x, double y, double epsilon) {
        double xMin = Math.min(x1, Math.min(x2, x3)) - epsilon;
        double xMax = Math.max(x1, Math.max(x2, x3)) + epsilon;
        double yMin = Math.min(y1, Math.min(y2, y3)) - epsilon;
        double yMax = Math.max(y1, Math.max(y2, y3)) + epsilon;
        return !(x < xMin || xMax < x || y < yMin || yMax < y);
    }

    /**
     * Test if point is inside triangle.
     * This method first test a bounding box intersection then using side algorithm
     * and finally segment distant using given tolerance.
     *
     * Explications provided here :
     * https://totologic.blogspot.com/2014/01/accurate-point-in-triangle-test.html
     *
     * @param x1 triangle first point X
     * @param y1 triangle first point Y
     * @param x2 triangle second point X
     * @param y2 triangle second point Y
     * @param x3 triangle third point X
     * @param y3 triangle third point Y
     * @param x point X
     * @param y point Y
     * @param epsilon bounding box margin
     * @return true if point is inside triangle
     */
    public static boolean isPointInTriangle_Accurate(double x1, double y1, double x2, double y2, double x3, double y3, double x, double y, double epsilon) {
        if (!Maths.isPointInTriangle_BoundingBox(x1, y1, x2, y2, x3, y3, x, y, epsilon)) {
            return false;
        }
        final double epsilonSquare = epsilon * epsilon;
        return Maths.isPointInTriangle_SideAlgo(x1, y1, x2, y2, x3, y3, x, y)
            || Maths.distanceSquare(x1, y1, x2, y2, x, y) <= epsilonSquare
            || Maths.distanceSquare(x2, y2, x3, y3, x, y) <= epsilonSquare
            || Maths.distanceSquare(x3, y3, x1, y1, x, y) <= epsilonSquare;
    }

    /**
     * Only X,Y ordinates are used.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     */
    public static boolean isCounterClockwise(Tuple a, Tuple b, Tuple c) {
        return lineSide(a, b, c) > 0;
    }

    /**
     * Only X,Y ordinates are used.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     */
    public static boolean isCounterClockwise(float[] a, float[] b, float[] c) {
        return lineSide(a, b, c) > 0;
    }

    /**
     * Compute barycentric value of a point in a triangle.
     * Only X,Y ordinates are used.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     */
    public static double[] getBarycentricValue2D(Tuple a, Tuple b, Tuple c, Tuple p){
        return Maths.getBarycentricValue2D(a.get(0), a.get(1), b.get(0), b.get(1), c.get(0), c.get(1), p.get(0), p.get(1));
    }

    /**
     * Compute barycentric value of a point in a triangle.
     *
     * @param x1 triangle first point X
     * @param y1 triangle first point Y
     * @param x2 triangle second point X
     * @param y2 triangle second point Y
     * @param x3 triangle third point X
     * @param y3 triangle third point Y
     * @param x point X
     * @param y point Y
     * @return [a,b,c] weights
     */
    public static double[] getBarycentricValue2D(double x1, double y1, double x2, double y2, double x3, double y3, double x, double y){
        final double v0x = x2-x1;
        final double v0y = y2-y1;
        final double v1x = x3-x1;
        final double v1y = y3-y1;
        final double v2x = x-x1;
        final double v2y = y-y1;
        final double d00 = v0x * v0x + v0y * v0y;
        final double d01 = v0x * v1x + v0y * v1y;
        final double d11 = v1x * v1x + v1y * v1y;
        final double d20 = v2x * v0x + v2y * v0y;
        final double d21 = v2x * v1x + v2y * v1y;
        final double denom = d00 * d11 - d01 * d01;
        final double v = (d11 * d20 - d01 * d21) / denom;
        final double w = (d00 * d21 - d01 * d20) / denom;
        final double u = 1.0 - v - w;
        return new double[]{u, v, w};
    }

    public static double[] getBarycentricValue2D(final float[] a, final float[] b, final float[] c, final float[] p){
        final double[] v0 = subtract(b, a);
        final double[] v1 = subtract(c, a);
        final double[] v2 = subtract(p, a);
        final double d00 = dot2D(v0,v0);
        final double d01 = dot2D(v0,v1);
        final double d11 = dot2D(v1,v1);
        final double d20 = dot2D(v2,v0);
        final double d21 = dot2D(v2,v1);
        final double denom = d00 * d11 - d01 * d01;
        final double v = (d11 * d20 - d01 * d21) / denom;
        final double w = (d00 * d21 - d01 * d20) / denom;
        final double u = 1.0f - v - w;
        return new double[]{u, v, w};
    }

    public static boolean inCircle(Tuple a, Tuple b, Tuple c, Tuple d) {
        return inCircle(a.get(0), a.get(1), b.get(0), b.get(1), c.get(0), c.get(1), d.get(0), d.get(1));
    }

    public static boolean inCircle(float[] a, float[] b, float[] c, float[] d) {
        double t;
        double a2 = (t = a[0]) * t + (t = a[1]) * t;
        double b2 = (t = b[0]) * t + (t = b[1]) * t;
        double c2 = (t = c[0]) * t + (t = c[1]) * t;
        double d2 = (t = d[0]) * t + (t = d[1]) * t;

        double det44 = 0;
        det44 += d2 * det33(a[0], a[1], 1, b[0], b[1], 1, c[0], c[1], 1);
        det44 -= d[0] * det33(a2, a[1], 1, b2, b[1], 1, c2, c[1], 1);
        det44 += d[1] * det33(a2, a[0], 1, b2, b[0], 1, c2, c[0], 1);
        det44 -= 1 * det33(a2, a[0], a[1], b2, b[0], b[1], c2, c[0], c[1]);

        if (det44 < 0) {
            return true;
        }
        return false;
    }

    public static boolean inCircle(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        final double a2 = ax*ax + ay*ay;
        final double b2 = bx*bx + by*by;
        final double c2 = cx*cx + cy*cy;
        final double d2 = dx*dx + dy*dy;

        double det44 = (d2 * det33(ax, ay,  1, bx, by,  1, cx, cy,  1))
                     - (dx * det33(a2, ay,  1, b2, by,  1, c2, cy,  1))
                     + (dy * det33(a2, ax,  1, b2, bx,  1, c2, cx,  1))
                     - ( 1 * det33(a2, ax, ay, b2, bx, by, c2, cx, cy));
        return det44 < 0;
    }

    private static double det33(double... m) {
        return (m[0] * (m[4] * m[8] - m[5] * m[7]))
             - (m[1] * (m[3] * m[8] - m[5] * m[6]))
             + (m[2] * (m[3] * m[7] - m[4] * m[6]));
    }

    public static double dot2D(final float[] vector, final float[] other){
        return (double) vector[0] * (double) other[0]
             + (double) vector[1] * (double) other[1];
    }

    public static double dot2D(final double[] vector, final double[] other){
        return vector[0] * other[0]
             + vector[1] * other[1];
    }

    public static double dot2D(final double x1, double y1, double x2, double y2){
        return x1 * x2 + y1 * y2;
    }

    public static double distance(Tuple a, Vector planNormal, double planD){
        return planNormal.dot(a) - planD;
    }

    public static double distance(Vector3D.Double a, Vector3D.Double planNormal, double planD){
        return planNormal.dot(a) - planD;
    }

    /**
     * Compute squre distance from segment to line.
     *
     * @param segmentStart segment start
     * @param segmentEnd segment end
     * @param point test point
     * @return point distance to segment
     */
    public static double distanceSquare(final float[] segmentStart, final float[] segmentEnd, final float[] point){
        final double[] ab = subtract(segmentEnd, segmentStart);
        final double[] ac = subtract(point,segmentStart);
        final double[] bc = subtract(point,segmentEnd);
        final double e = dot2D(ac, ab);
        // cases where point is outside segment
        if (e <= 0.0) return dot2D(ac, ac);
        final double f = dot2D(ab, ab);
        if (e >= f) return dot2D(bc, bc);
        // cases where point projects onto segment
        return dot2D(ac, ac)- e*e /f;
    }

    /**
     * Compute squre distance from segment to line.
     *
     * @param x1 segment start X
     * @param y1 segment start Y
     * @param x2 segment end X
     * @param y2 segment end Y
     * @param x point X
     * @param y point Y
     * @return point distance to segment
     */
    public static double distanceSquare(double x1, double y1, double x2, double y2, double x, double y) {
        if (y1 == y2 && y2 == y) {
            //point on the same vertical line
            if (x2 < x1) {
                double t = x1;
                x1 = x2;
                x2 = t;
            }
            return (x < x1) ? x1 - x
                 : (x > x2) ? x - x2
                 : 0.0;
        } else if (x1 == x2 && x2 == x) {
            //point on the same horizontal line
            if (y2 < y1) {
                double t = y1;
                y1 = y2;
                y2 = t;
            }
            return (y < y1) ? y1 - y
                 : (y > y2) ? y - y2
                 : 0.0;
        }

        final double segLength2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        final double dotprod = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / segLength2;
        if (dotprod < 0) {
            return (x - x1) * (x - x1) + (y - y1) * (y - y1);
        } else if (dotprod <= 1) {
            double tarLength2 = (x1 - x) * (x1 - x) + (y1 - y) * (y1 - y);
            return tarLength2 - dotprod * dotprod * segLength2;
        } else {
            return (x - x2) * (x - x2) + (y - y2) * (y - y2);
        }
    }

    /**
     * Calculate normal of triangle made of given 3 points.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @param buffer to store normal values
     * @return triangle normal
     */
    public static float[] calculateNormal(float[] a, float[] b, float[] c, float[] buffer){
        final float[] ab = Vectors.subtract(b,a);
        final float[] ac = Vectors.subtract(c,a);
        buffer = Vectors.cross(ab, ac, buffer);
        return Vectors.normalize(buffer, buffer);
    }

    public static double[] calculateCircleCenter(double[] a, double[] b, double[] c) {
        final double as = (b[1]-a[1]) / (b[0]-a[0]);
        final double bs = (c[1]-b[1]) / (c[0]-b[0]);
        final double[] center = new double[2];
        center[0] = (as * bs * (a[1]-c[1]) + bs * (a[0]+b[0]) - as * (b[0]+c[0])) / (2 * (bs-as));
        center[1] = -1.0 * (center[0] - (a[0]+b[0])/2.0) / as + (a[1]+b[1])/2.0;
        return center;
    }

    /**
     *
     * @param c symmetry center
     * @param p point to reflect
     * @return reflected point
     */
    public static double[] calculatePointSymmetry(double[] c, double[] p){
        final double[] r = new double[c.length];
        for(int i=0;i<r.length;i++) r[i] = (2*c[i])-p[i];
        return r;
    }

    /**
     * Test if a point is inside given triangle.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @param p point to test
     * @return true if point is inside triangle
     */
    public static boolean inTriangle(double[] a, double[] b, double[] c, double[] p){
        final double[] bary = getBarycentricValue(a, b, c, p);
        return bary[1] >= 0.0 && bary[2] >= 0.0 && (bary[1] + bary[2]) <= 1.0;
    }

    /**
     * Calculate the barycentric value in triangle for given point.
     *
     * @param a first triangle point
     * @param b second triangle point
     * @param c third triangle point
     * @param p point to test
     * @return Vector barycentric values
     */
    public static double[] getBarycentricValue(final double[] a, final double[] b, final double[] c, final double[] p){
        final double[] v0 = Vectors.subtract(b, a);
        final double[] v1 = Vectors.subtract(c, a);
        final double[] v2 = Vectors.subtract(p, a);
        final double d00 = dot(v0, v0);
        final double d01 = dot(v0,v1);
        final double d11 = dot(v1,v1);
        final double d20 = dot(v2,v0);
        final double d21 = dot(v2,v1);
        final double denom = d00 * d11 - d01 * d01;
        final double v = (d11 * d20 - d01 * d21) / denom;
        final double w = (d00 * d21 - d01 * d20) / denom;
        final double u = 1.0f - v - w;
        return new double[]{u, v, w};
    }

    /**
     * Calculate constant D of a plan.
     * Same as normal.dot(point).
     *
     * @param normal plan normal
     * @param pointOnPlan a point in the plan
     * @return plan D constant value
     */
    public static double calculatePlanD(double[] normal, double[] pointOnPlan){
        return Vectors.dot(normal, pointOnPlan);
    }

    /**
     * Calculate projection of a point on a plan.
     *
     * @param point point to project
     * @param planNormal plan normal
     * @param planD plan D constant
     * @return projected point
     */
    public static double[] projectPointOnPlan(double[] point, double[] planNormal, double planD){
        double[] va = Vectors.subtract(point, Vectors.scale(planNormal,planD) );
        double d = Vectors.dot(planNormal, va);
        return Vectors.subtract(va, Vectors.scale(planNormal, d));
    }

    /**
     * Test if given sequence of tuple is in clockwise direction.
     * This method expect the coordinates to be a closed line.
     *
     * @param coordinates line coordinates
     * @return true if clockwise
     */
    public static boolean isClockWise(List<Tuple> coordinates){
        final double area = calculateArea(coordinates);
        return area > 0;
    }

    /**
     * Test if given sequence of tuple is in clockwise direction.
     * This method expect the coordinates to be a closed line.
     *
     * @param coordinates polygon outer line
     * @return area
     */
    public static double calculateArea(List<Tuple> coordinates){
        double area = 0;
        final int numPoints = coordinates.size();
        for(int i=0;i<numPoints-1;i++){
            final Tuple start = coordinates.get(i);
            final Tuple end = coordinates.get(i+1);
            area += (start.get(0)+end.get(0)) * (start.get(1)-end.get(1));
        }
        return area/2.0;
    }

    /**
     * Create an Affine that transform the given bbox to be centerd into target bbox.
     * The source bbox is scaled with given scale.
     *
     * @param source
     * @param target
     * @param scale wanted scale.
     * @return
     */
    public static MatrixSIS centeredScaled(GeneralEnvelope source, GeneralEnvelope target, double scale){

        final double[] sourceCenter = source.getMedian().getCoordinates();
        final double[] targetCenter = target.getMedian().getCoordinates();
        Vectors.scale(sourceCenter, scale, sourceCenter);
        final double[] trs = Vectors.subtract(targetCenter, sourceCenter);

        final MatrixSIS mt = new Matrix4();
        for (int i=0;i<trs.length;i++){
            mt.setElement(i, i, scale);
            mt.setElement(i, trs.length, trs[i]);
        }

        return mt;
    }

    /**
     * Create an Affine that transform the given bbox to fit into target bbox.
     * Dimensions ratio are preserved and will be centered in target bbox.
     *
     * @param source
     * @param target
     * @return
     */
    public static MatrixSIS scaled(GeneralEnvelope source, GeneralEnvelope target){
        //find min scale
        final int dim = source.getDimension();
        double scale = target.getSpan(0) / source.getSpan(0);
        for (int i=1;i<dim;i++){
            scale = Math.min( target.getSpan(i) / source.getSpan(i), scale);
        }
        return centeredScaled(source, target, scale);
    }

    /**
     * JavaScript can not read binary data such as float if they are not byte aligned.
     * Float require 4 bytes alignment and Double 8.
     * In the B3DM specification the gltf must be 8 bytes aligned.
     *
     * @param data source array
     * @param isJson is true padding will be spaces
     * @param previousDataLength length of data before data array which must be included in padding
     * @param padding wanted padding
     *
     * @return padded byte array
     */
    public static byte[] pad(byte[] data, boolean isJson, int previousDataLength, int padding) {
        if (data==null) return null;

        final int remaining = (previousDataLength+data.length) % padding;
        if (remaining == 0) return data;

        final byte[] array = new byte[data.length + (padding-remaining)];
        Arrays.fill(array, isJson ? (byte)' ' : 0x00);
        System.arraycopy(data, 0, array, 0, data.length);
        return array;
    }

    /**
     * Compute distance ratio of point on the segment.
     *
     * @param x1 segment start X
     * @param y1 segment start Y
     * @param x2 segment end X
     * @param y2 segment end Y
     * @param x point X
     * @param y point Y
     * @return ratio betwen 0 and 1 if point is on segment,
     *         negative if before start,
     *         greater then one if after end
     */
    public static double projectionRatio(double x1, double y1, double x2, double y2, double x, double y) {
        //compute weights based on distance
        final double abx = x2 - x1;
        final double aby = y2 - y1;
        final double acx = x - x1;
        final double acy = y - y1;
        final double e = Maths.dot2D(acx, acy, abx, aby);
        final double f = Maths.dot2D(abx, aby, abx, aby);
        return e / f;
    }

    /**
     * Normalized byte to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = max(i / 127.0, -1.0)}</pre>
     *
     * @return double in range [-1..1]
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static double normalizedByte(byte value) {
        return Math.max((double) value / 127.0, -1.0);
    }

    /**
     * Normalized unsigned byte to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = i / 255.0}</pre>
     *
     * @return double in range [0..1]
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static double normalizedUByte(byte value) {
        return ((double) (value & 0xFF)) / 255.0;
    }

    /**
     * Normalized short to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = max(i / 32767.0, -1.0)}</pre>
     *
     * @return double in range [-1..1]
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static double normalizedShort(short value) {
        return Math.max((double) value / 32767.0, -1.0);
    }

    /**
     * Normalized unsigned short to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = i / 65535.0}</pre>
     *
     * @return double in range [0..1]
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static double normalizedUShort(short value) {
        return ((double) (value & 0xFFFF)) / 65535.0;
    }

    /**
     * Normalized byte to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = max(i / 2147483647.0, -1.0)}</pre>
     *
     * @return double in range [-1..1]
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static double normalizedInt(int value) {
        return Math.max((double) value / 2147483647.0, -1.0);
    }

    /**
     * Normalized unsigned int to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = i / 4294967295.0}</pre>
     *
     * @return double in range [0..1]
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static double normalizedUInt(int value) {
        return ((double) (value & 0xFFFFFFFFL)) / 4294967295.0;
    }

    /**
     * Normalized long to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = max(i / 9223372036854775807.0, -1.0)}</pre>
     *
     * @return double in range [-1..1]
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static double normalizedLong(long value) {
        return Math.max((double) value / 9223372036854775807.0, -1.0);
    }

    /**
     * Normalized unsigned long to double.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code f = i / 18446744073709551615.0}</pre>
     *
     * @return double in range [0..1]
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static double normalizedULong(long value) {
        return Numerics.toUnsignedDouble(value) / 18446744073709551615.0;
    }

    /**
     * Double to Normalized byte.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 127.0)}</pre>
     *
     * @param value value must be in range[-1..1], value is not verified.
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static byte toNormalizedByte(double value) {
        return (byte) Math.round(value * 127.0);
    }

    /**
     * Double to Normalized unsigned byte.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 255.0)}</pre>
     *
     * @param value value must be in range[0..1], value is not verified.
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static byte toNormalizedUByte(double value) {
        return (byte) Math.round(value * 255.0);
    }

    /**
     * Double to Normalized short.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 32767.0)}</pre>
     * As consequence : -1.0 will produce : -32767
     *
     * @param value value must be in range[-1..1], value is not verified.
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static short toNormalizedShort(double value) {
        return (short) Math.round(value * 32767.0);
    }

    /**
     * Double to Normalized unsigned short.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 65535.0)}</pre>
     *
     * @param value value must be in range[0..1], value is not verified.
     * @see https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_mesh_quantization/README.md#encoding-quantized-data
     */
    public static short toNormalizedUShort(double value) {
        return (short) Math.round(value * 65535.0);
    }

    /**
     * Double to Normalized int.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 2147483647.0)}</pre>
     *
     * @param value value must be in range[-1..1], value is not verified.
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static int toNormalizedInt(double value) {
        return (int) Math.round(value * 2147483647.0);
    }

    /**
     * Double to Normalized unsigned int.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 4294967295.0)}</pre>
     *
     * @param value value must be in range[0..1], value is not verified.
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static int toNormalizedUInt(double value) {
        return (int) Math.round(value * 4294967295.0);
    }

    /**
     * Double to Normalized long.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 9223372036854775807.0)}</pre>
     *
     * @param value value must be in range[-1..1], value is not verified.
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static long toNormalizedLong(double value) {
        return Math.round(value * 9223372036854775807.0);
    }

    /**
     * Double to Normalized unsigned long.
     *
     * Formula follows recommandation of Khronos KHR_mesh_quantization and 3D-Tiles Metadata :
     * <pre>{@code i = round(f * 18446744073709551615.0)}</pre>
     * Since java do not have unsigned long, BigDecimal and BigInteger are used instead.
     *
     * @param value value must be in range[0..1], value is not verified.
     * @see https://github.com/CesiumGS/3d-tiles/tree/main/specification/Metadata#normalized-values
     */
    public static long toNormalizedULong(double value) {
        byte[] array = new BigDecimal(value).multiply(new BigDecimal(18446744073709551615.0)).toBigInteger().toByteArray();
        if (array.length > 8 && array[0] != 0) return 0xFFFFFFFFFFFFFFFFL;
        long n = 0l;
        for (byte b : array) {
            n = (n << 8) + (b & 255);
        }
        return n;
    }

    /**
     * Clamps a value between min value and max value.
     *
     * @param val the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static int clamp(int val, int min, int max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Clamps each value of an array between min value and max value.
     *
     * @param val the array of values to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static int[] clamp(int[] val, int min, int max) {
        final int[] ret = new int[val.length];
        for (int i=0; i<val.length; i++) {
            ret[i] = clamp(val[i], min, max);
        }
        return ret;
    }

    /**
     * Clamps a value between min value and max value.
     *
     * @param val the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static long clamp(long val, long min, long max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Clamps each value of an array between min value and max value.
     *
     * @param val the array of values to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static long[] clamp(long[] val, long min, long max) {
        final long[] ret = new long[val.length];
        for (int i=0; i<val.length; i++) {
            ret[i] = clamp(val[i], min, max);
        }
        return ret;
    }

    /**
     * Clamps a value between min value and max value.
     *
     * @param val the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Clamps each value of an array between min value and max value.
     *
     * @param val the array of values to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static float[] clamp(float[] val, float min, float max) {
        final float[] ret = new float[val.length];
        for (int i=0; i<val.length; i++) {
            ret[i] = clamp(val[i], min, max);
        }
        return ret;
    }

    /**
     * Clamps a value between min value and max value.
     *
     * @param val the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static double clamp(double val, double min, double max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Clamps each value of an array between min value and max value.
     *
     * @param val the array of values to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return val clamped between min and max
     */
    public static double[] clamp(double[] val, double min, double max) {
        final double[] ret = new double[val.length];
        for (int i=0; i<val.length; i++) {
            ret[i] = clamp(val[i], min, max);
        }
        return ret;
    }

    /**
     * Clamps each value of an array between min value and max value.
     *
     * @param val the array of values to clamp, values are modified
     * @param min the minimum value
     * @param max the maximum value
     */
    public static void applyClamp(double[] val, double min, double max) {
        switch (val.length) {
            case 4 :
                val[3] = clamp(val[3], min, max);
            case 3 :
                val[2] = clamp(val[2], min, max);
            case 2 :
                val[1] = clamp(val[1], min, max);
            case 1 :
                val[0] = clamp(val[0], min, max);
                break;
            default :
                for (int i=0; i<val.length; i++) {
                    val[i] = clamp(val[i], min, max);
                }
        }
    }
}
