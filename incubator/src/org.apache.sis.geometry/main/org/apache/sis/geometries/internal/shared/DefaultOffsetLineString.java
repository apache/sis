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
package org.apache.sis.geometries.internal.shared;

import java.util.HashMap;
import java.util.Map;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.Unit;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.hypot;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Cursor;
import org.apache.sis.maths.DataType;
import org.apache.sis.measure.Units;


/**
 * A curve at a constant distance from a base line string, in the plane of the first two axes.
 *
 * <p>Each segment of the base line string is translated along its normal, then consecutive
 * translated segments are joined at the intersection of their supporting lines. The offset
 * curve has the same number of points as the base line string, a closed base line string
 * gives a closed offset curve. Only the first two coordinates of each position are modified,
 * other coordinates and attributes are copied unchanged.</p>
 *
 * <p>With a negative distance, which is the right side (generated line string with ' character) :</p>
 * <pre>
 *                    B___________ C
 *                    |  _______  \
 *                    | |B'   C'\  \
 *                    | |        \  \
 *                    | |        D'  D
 *                    A A'
 * </pre>
 *
 * <p>With a positive distance, which is the left side :</p>
 * <pre>
 *                 B'________________C'
 *                 |  B___________ C \
 *                 |  |           \   \
 *                 |  |            \   \
 *                 |  |             \   \
 *                 |  |              D   D'
 *                 A' A
 * </pre>
 *
 * @author Remi Marechal (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public final class DefaultOffsetLineString extends DefaultOffsetCurve {

    /**
     * Accepted tolerance.
     */
    private static final double TOLERANCE = 1E-9;

    /**
     * Offset data points, computed when first requested.
     */
    private DataPoints dataPoints;

    /**
     * Creates a curve offset from the given line string.
     *
     * @param  baseCurve  line string from which this curve is offset.
     * @param  distance   constant offset distance. A positive distance designates the left side
     *                    of the base curve with respect to its direction, and a negative distance
     *                    its right side. A dimensionless distance is in the unit of the CRS axes.
     */
    public DefaultOffsetLineString(final LineString baseCurve, final Quantity<?> distance) {
        super(baseCurve, distance, null);
    }

    @Override
    public LineString getBaseCurve() {
        return (LineString) baseCurve;
    }

    /**
     * Returns the offset positions.
     *
     * @throws OperationException if the offset cannot be computed.
     */
    @Override
    public synchronized DataPoints getDataPoints() {
        if (dataPoints == null) {
            dataPoints = compute(getBaseCurve().getDataPoints(), getAxisDistance());
        }
        return dataPoints;
    }

    @Override
    public Array getControlPoints() {
        return getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION);
    }

    @Override
    public synchronized void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws IllegalArgumentException {
        super.setCoordinateReferenceSystem(crs);
        dataPoints = null;
    }

    /**
     * Returns the envelope of the offset positions.
     */
    @Override
    public Envelope getEnvelope() {
        if (isEmpty()) return null;
        return getDataPoints().getEnvelope();
    }

    /**
     * Returns the offset positions as a line string, or a linear ring if the base curve is a linear ring.
     * The arguments are ignored since this curve is already linear.
     */
    @Override
    public LineString asLine(final Quantity<?> spacing, final Quantity<?> offset) {
        if (baseCurve instanceof LinearRing) {
            return GeometryFactory.DEFAULT.createLinearRing(getDataPoints());
        }
        return GeometryFactory.DEFAULT.createLineString(getDataPoints());
    }

    /**
     * Returns the offset distance in the unit of the first CRS axis.
     *
     * @throws UnsupportedOperationException if the distance unit cannot be converted to the axis unit.
     */
    private double getAxisDistance() {
        final double value = distance.getValue().doubleValue();
        final Unit<?> axisUnit = baseCurve.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getUnit();
        if (axisUnit == null || Units.UNITY.equals(distance.getUnit())) {
            return value;
        }
        try {
            return distance.getUnit().getConverterToAny(axisUnit).convert(value);
        } catch (IncommensurableException e) {
            throw new UnsupportedOperationException("The offset distance in " + distance.getUnit()
                    + " cannot be converted to the unit of axis 0 (" + axisUnit + ").", e);
        }
    }

    /**
     * Computes the offset positions.
     *
     * @param base data points of the base line string.
     * @param distance offset distance, positive for the left side and negative for the right side.
     * @return copy of the base data points with offset positions.
     * @throws OperationException if a translation is impossible.
     */
    private static DataPoints compute(DataPoints base, double distance) throws OperationException {
        final ArrayDataPoints ps = copy(base);
        final Array positions = ps.getAttribute(DataPointsType.ATT_POSITION);
        final int nb = Math.toIntExact(positions.getLength());
        if (distance == 0 || nb < 2) return ps;

        //the algorithm translates on the right side for positive values
        distance = -distance;

        final double[] xs = new double[nb];
        final double[] ys = new double[nb];
        Cursor cursor = positions.cursor();
        for (int i = 0; cursor.next(); i++) {
            xs[i] = cursor.getSample(0);
            ys[i] = cursor.getSample(1);
        }

        final double[] dxs = new double[nb];
        final double[] dys = new double[nb];
        //previous translated segment
        final double[] seg0 = new double[4];
        //current translated segment
        final double[] seg1 = new double[4];
        final double[] inter = new double[2];

        translateSegment(xs[0], ys[0], xs[1], ys[1], seg0, distance);
        dxs[0] = seg0[0];
        dys[0] = seg0[1];
        if (nb == 2) {
            dxs[1] = seg0[2];
            dys[1] = seg0[3];
        } else {
            final boolean closed = xs[0] == xs[nb - 1] && ys[0] == ys[nb - 1];
            final int segmentNumber = nb - 1;
            for (int c = 1; c < segmentNumber; c++) {
                translateSegment(xs[c], ys[c], xs[c+1], ys[c+1], seg1, distance);

                //intersection between current translated segment and previously generated segment
                intersectionRightLine(seg0[0], seg0[1], seg0[2], seg0[3], seg1[0], seg1[1], seg1[2], seg1[3], inter);
                dxs[c] = inter[0];
                dys[c] = inter[1];
                System.arraycopy(seg1, 0, seg0, 0, 4);

                if (c == segmentNumber - 1) {
                    if (closed) {
                        //close the translated line string
                        intersectionRightLine(seg0[0], seg0[1], seg0[2], seg0[3], dxs[0], dys[0], dxs[1], dys[1], inter);
                        dxs[0] = inter[0];
                        dys[0] = inter[1];
                        dxs[c+1] = inter[0];
                        dys[c+1] = inter[1];
                    } else {
                        dxs[c+1] = seg1[2];
                        dys[c+1] = seg1[3];
                    }
                }
            }
        }

        cursor = positions.cursor();
        for (int i = 0; cursor.next(); i++) {
            cursor.setSample(0, dxs[i]);
            cursor.setSample(1, dys[i]);
        }
        return ps;
    }

    /**
     * Copy all attributes, positions are converted to doubles to store the offset coordinates.
     */
    private static ArrayDataPoints copy(DataPoints ps) {
        final Map<String,Array> attributes = new HashMap<>();
        for (String name : ps.getType().getAttributeNames()) {
            final Array array = ps.getAttributeArray(name);
            attributes.put(name, DataPointsType.ATT_POSITION.equals(name) ? array.retype(DataType.DOUBLE) : array.copy());
        }
        return new ArrayDataPoints(attributes);
    }

    /**
     * Translate a single segment along its normal.
     *
     * @param x0 segment start x
     * @param y0 segment start y
     * @param x1 segment end x
     * @param y1 segment end y
     * @param dst translated segment as x0,y0,x1,y1
     * @param distance offset distance, positive for the right side and negative for the left side.
     */
    private static void translateSegment(double x0, double y0, double x1, double y1, double[] dst, double distance) {
        final double vx = x1 - x0;
        final double vy = y1 - y0;

        //orthogonal vector
        double ox = vy;
        double oy = -vx;
        final double normO = hypot(ox, oy);
        ox = ox / normO * distance;
        oy = oy / normO * distance;

        dst[0] = x0 + ox;
        dst[1] = y0 + oy;
        dst[2] = x1 + ox;
        dst[3] = y1 + oy;
    }

    /**
     * Find the intersection point between the two lines (AB) and (CD).
     *
     * @param out receives the intersection point x,y.
     * @throws OperationException if the two lines do not intersect.
     */
    private static void intersectionRightLine(double ax, double ay, double bx, double by,
                                              double cx, double cy, double dx, double dy,
                                              double[] out) throws OperationException {

        //director vector of (AB) line
        final double ux = bx - ax;
        final double uy = by - ay;

        //director vector of (CD) line
        final double vx = dx - cx;
        final double vy = dy - cy;

        //B = C
        if (abs(bx - cx) < TOLERANCE && abs(by - cy) < TOLERANCE) {
            set(out, bx, by);
            return;
        }

        //A = D, should not happen if the line string direction is respected
        if (abs(ax - dx) < TOLERANCE && abs(ay - dy) < TOLERANCE) {
            set(out, ax, ay);
            return;
        }

        //A = B, should not happen
        if (abs(ax - bx) < TOLERANCE && abs(ay - by) < TOLERANCE) {
            //CA vector, if colinear with CD then AB is on CD
            final double wx = ax - cx;
            final double wy = ay - cy;
            if (abs(wx * vy - wy * vx) < TOLERANCE) {
                set(out, ax, ay);
                return;
            }
            throw noIntersection();
        }

        //C = D, should not happen
        if (abs(cx - dx) < TOLERANCE && abs(cy - dy) < TOLERANCE) {
            //AC vector, if colinear with AB then CD is on AB
            final double wx = cx - ax;
            final double wy = cy - ay;
            if (abs(ux * wy - uy * wx) < TOLERANCE) {
                set(out, cx, cy);
                return;
            }
            throw noIntersection();
        }

        if (abs(vy) < TOLERANCE && abs(ux) < TOLERANCE) {
            /*
             *     B
             *     |
             * C __|___ D
             *     |
             *     A
             */
            set(out, ax, cy);
            return;
        }

        if (abs(vx) < TOLERANCE && abs(uy) < TOLERANCE) {
            /*
             *     D
             *     |
             * A __|___ B
             *     |
             *     C
             */
            set(out, cx, ay);
            return;
        }

        if (abs(ux) < TOLERANCE && abs(vx) < TOLERANCE) {
            /*
             * B
             * |  D
             * A  |
             *    C
             */
            if (abs(ax - cx) > TOLERANCE) throw noIntersection();
            set(out, ax, getIntersectionOnOneDimension(ay, by, cy, dy));
            return;
        }

        if (abs(uy) < TOLERANCE && abs(vy) < TOLERANCE) {
            /*
             * A_____B
             *    C_____D
             */
            if (abs(ay - cy) > TOLERANCE) throw noIntersection();
            set(out, getIntersectionOnOneDimension(ax, bx, cx, dx), ay);
            return;
        }

        if (abs(ux) <= TOLERANCE) {
            /*
             *     B  D
             *     | /
             *     |/
             *    /|
             *   / A
             *  C
             */
            set(out, ax, (ax - cx) * (vy / vx) + cy);
            return;
        }

        if (abs(uy) <= TOLERANCE) {
            /*
             *       D
             *      /
             *  A__/____B
             *    /
             *   C
             */
            set(out, (ay - cy) * (vx / vy) + cx, ay);
            return;
        }

        if (abs(vx) <= TOLERANCE) {
            /*
             *     D  B
             *     | /
             *     |/
             *    /|
             *   / C
             *  A
             */
            set(out, cx, (cx - ax) * (uy / ux) + ay);
            return;
        }

        if (abs(vy) <= TOLERANCE) {
            /*
             *       B
             *      /
             *  C__/____D
             *    /
             *   A
             */
            set(out, (cy - ay) * (ux / uy) + ax, cy);
            return;
        }

        final double vx_vy = vx / vy;

        /*
         * (AB) parametric equation.
         * {X(t) = t * ux + A.x
         * {Y(t) = t * uy + A.y
         */
        final double t = ((ay - cy) * vx_vy + cx - ax) / (ux - uy * vx_vy);
        final double x = t * ux + ax;
        final double y = t * uy + ay;
        assert abs(y - ((x - cx) / vx_vy + cy)) <= TOLERANCE : "Error computing y from lines should be equal";
        set(out, x, y);
    }

    /**
     * Returns the middle of the intersection between spans [a,b] and [c,d].
     *
     * @throws OperationException if the spans do not intersect.
     */
    private static double getIntersectionOnOneDimension(double a, double b, double c, double d) throws OperationException {
        final double imin = max(a, c);
        final double imax = min(b, d);
        if (imax + TOLERANCE < imin - TOLERANCE) {
            /*
             * A_____B    C____D
             */
            throw noIntersection();
        }
        return (imin + imax) / 2.0;
    }

    private static void set(double[] out, double x, double y) {
        out[0] = x;
        out[1] = y;
    }

    private static OperationException noIntersection() {
        return new OperationException("No intersection between lines.");
    }
}
