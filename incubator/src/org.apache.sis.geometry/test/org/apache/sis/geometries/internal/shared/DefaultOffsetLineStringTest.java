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

import static java.lang.StrictMath.sqrt;
import javax.measure.Quantity;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.OffsetCurve;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DefaultOffsetLineString}.
 *
 * @author Remi Marechal (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class DefaultOffsetLineStringTest {

    private static final double TOLERANCE = 1E-9;

    private static final double SQRT_2 = sqrt(2.0);

    private static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.normalizedGeographic();

    private static final CoordinateReferenceSystem CRS_3D = CommonCRS.WGS84.geographic3D();

    /**
     * Create a line string from coordinates ordered as x0, y0, ... xn, yn.
     */
    private static LineString line(double ... coords) {
        return GeometryFactory.DEFAULT.createLineString(new ArrayDataPoints(NDArrays.of(CRS_2D, coords)));
    }

    private static OffsetCurve offset(LineString line, Quantity<?> distance) {
        return GeometryFactory.DEFAULT.createOffsetCurve(line, distance, null);
    }

    private static OffsetCurve offset(LineString line, double distance) {
        return offset(line, Quantities.create(distance, Units.UNITY));
    }

    private static double[] positions(Geometry geom) {
        return geom.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble();
    }

    /**
     * Verifies the positions of the offset curve.
     *
     * @param distance offset distance, positive for the left side and negative for the right side.
     */
    private static void assertOffset(LineString line, double distance, double ... expected) {
        final OffsetCurve result = offset(line, distance);
        assertInstanceOf(DefaultOffsetLineString.class, result);
        assertEquals(CRS_2D, result.getCoordinateReferenceSystem());
        assertArrayEquals(expected, positions(result), TOLERANCE);
    }

    /**
     * Tests that the factory creates a {@link DefaultOffsetLineString} only for line strings in 2D.
     */
    @Test
    public void testFactory() {
        final LineString line = line(2, 1, 2, 4);
        final OffsetCurve result = offset(line, 3);
        assertInstanceOf(DefaultOffsetLineString.class, result);
        assertSame(line, result.getBaseCurve());
        assertEquals(GeometryType.OFFSETCURVE, result.getGeometryType());
        assertNull(result.getRefDirection());
    }

    /**
     * Tests line strings composed of a single segment, in all directions.
     */
    @Test
    public void testSingleSegment() {
        /*
         * B
         * |
         * A
         */
        LineString line = line(2, 1, 2, 4);
        assertOffset(line, -3,  5, 1,  5, 4);
        assertOffset(line,  5, -3, 1, -3, 4);

        /*
         *    B
         *   /
         *  A
         */
        line = line(2, 1, 4, 3);
        assertOffset(line, -3 * SQRT_2,  5, -2,  7, 0);
        assertOffset(line,  5 * SQRT_2, -3,  6, -1, 8);

        /*
         *  A ____ B
         */
        line = line(2, 1, 4, 1);
        assertOffset(line, -3, 2, -2, 4, -2);
        assertOffset(line,  7, 2,  8, 4,  8);

        /*
         *  A
         *   \
         *    \
         *     B
         */
        line = line(-1, 3, 6, -4);
        assertOffset(line, -7 * SQRT_2, -8, -4, -1, -11);
        assertOffset(line,  7 * SQRT_2,  6, 10, 13,   3);

        /*
         * A
         * |
         * B
         */
        line = line(-1, 3, -1, -4);
        assertOffset(line, -2, -3, 3, -3, -4);
        assertOffset(line,  5,  4, 3,  4, -4);

        /*
         *       A
         *      /
         *     /
         *    B
         */
        line = line(1, 3, -4, -2);
        assertOffset(line, -5 * SQRT_2, -4,  8, -9,  3);
        assertOffset(line,  5 * SQRT_2,  6, -2,  1, -7);

        /*
         *  B ____ A
         */
        line = line(4, 3, -10, 3);
        assertOffset(line, -2, 4,  5, -10,  5);
        assertOffset(line,  5, 4, -2, -10, -2);

        /*
         *  B
         *   \
         *    \
         *     A
         */
        line = line(7, -3, -6, 10);
        assertOffset(line, -9 * SQRT_2, 16,  6,   3, 19);
        assertOffset(line,  5 * SQRT_2,  2, -8, -11,  5);
    }

    /**
     * Tests line strings composed of several segments, closed and not closed.
     */
    @Test
    public void testSeveralSegments() {
        /*
         * B_______C
         * |       |
         * |       |
         * |       |
         * A_______D
         *
         * Clockwise
         */
        LineString line = line(-2, -2, -2, 3, 5, 3, 5, -2, -2, -2);
        //inside
        assertOffset(line, -1.5,
                -0.5, -0.5,  -0.5, 1.5,  3.5, 1.5,  3.5, -0.5,  -0.5, -0.5);
        //outside
        assertOffset(line, 7.2,
                -9.2, -9.2,  -9.2, 10.2,  12.2, 10.2,  12.2, -9.2,  -9.2, -9.2);

        /*
         *       D
         *      / \
         *     /   \
         *  A /     \
         *    \     / C
         *     \   /
         *      \ /
         *       B
         *
         * Counterclockwise
         */
        line = line(-1, -1, 3, -5, 7, -1, 3, 3, -1, -1);
        //inside
        assertOffset(line, SQRT_2,
                1, -1,  3, -3,  5, -1,  3, 1,  1, -1);
        //outside
        assertOffset(line, -3.3 * SQRT_2,
                -7.6, -1,  3, -11.6,  13.6, -1,  3, 9.6,  -7.6, -1);

        /*
         * B____C_____D     F          K
         * |          \    /|         /
         * |           \  / |        /
         * |            \/  G       J
         * A             E  |      /
         *                  |_____/
         *                  H     I
         *
         * Not closed.
         */
        line = line(-2, -3, -2, 5, 1, 5, 4, 5, 6, 1, 9, 5, 9, 2, 9, -1, 12, -1, 16, 0, 20, 1);
        assertOffset(line, -1,
                -1, -3,
                -1, 4,
                1, 4,
                3.381966011250105, 4,
                5.829179606750063, -0.8944271909999157,
                8, 2,
                8, 2,
                8, -2,
                12.12310562561766, -2,
                16.242535625036332, -0.9701425001453319,
                20.242535625036332, 0.029857499854668124);
        assertOffset(line, 2,
                -4, -3,
                -4, 7,
                1, 7,
                5.23606797749979, 7,
                6.341640786499873, 4.7888543819998315,
                11, 11,
                11, 2,
                11, 1,
                11.753788748764679, 1,
                15.514928749927334, 1.9402850002906638,
                19.514928749927336, 2.9402850002906638);
    }

    /**
     * Tests that a linear ring gives a linear ring when linearized.
     */
    @Test
    public void testLinearRing() {
        final LinearRing ring = GeometryFactory.DEFAULT.createLinearRing(new ArrayDataPoints(NDArrays.of(CRS_2D,
                -2.0, -2.0, -2.0, 3.0, 5.0, 3.0, 5.0, -2.0, -2.0, -2.0)));
        final LineString result = offset(ring, -1.5).asLine(null, null);
        assertInstanceOf(LinearRing.class, result);
        assertArrayEquals(new double[]{-0.5, -0.5,  -0.5, 1.5,  3.5, 1.5,  3.5, -0.5,  -0.5, -0.5},
                positions(result), TOLERANCE);
    }

    /**
     * Tests that coordinates after the first two are preserved.
     */
    @Test
    public void testThirdDimensionPreserved() {
        final LineString line = GeometryFactory.DEFAULT.createLineString(new ArrayDataPoints(NDArrays.of(CRS_3D,
                2.0, 1.0, 10.0,
                2.0, 4.0, 20.0)));
        final OffsetCurve result = offset(line, -3);
        assertEquals(CRS_3D, result.getCoordinateReferenceSystem());
        assertArrayEquals(new double[]{5, 1, 10,  5, 4, 20}, positions(result), TOLERANCE);
    }

    /**
     * Tests that integer positions are not truncated.
     */
    @Test
    public void testIntegerPositions() {
        final LineString line = GeometryFactory.DEFAULT.createLineString(new ArrayDataPoints(NDArrays.of(CRS_2D,
                2, 1,
                2, 4)));
        assertArrayEquals(new double[]{3.5, 1,  3.5, 4}, positions(offset(line, -1.5)), TOLERANCE);
    }

    /**
     * Tests that a distance in a unit of measurement is converted to the unit of the CRS axes.
     */
    @Test
    public void testDistanceUnit() {
        final LineString line = line(2, 1, 2, 4);
        final OffsetCurve result = offset(line, Quantities.create(-3 * 60, Units.ARC_MINUTE));
        assertArrayEquals(new double[]{5, 1,  5, 4}, positions(result), TOLERANCE);
    }

    /**
     * Tests that the envelope is the one of the offset positions.
     */
    @Test
    public void testEnvelope() {
        final Envelope env = offset(line(2, 1, 2, 4), -3).getEnvelope();
        assertArrayEquals(new double[]{5, 1}, env.getLowerCorner().getCoordinates(), TOLERANCE);
        assertArrayEquals(new double[]{5, 4}, env.getUpperCorner().getCoordinates(), TOLERANCE);
    }
}
