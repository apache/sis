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
package org.apache.sis.maths;

import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.GeneralEnvelope;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class VectorsTest {

    @Test
    public void octEncodingTest(){

        Vector3D.Float normal = new Vector3D.Float(1,0,0);
        byte[] bytes = Vectors.toOctByte(normal);
        Vector3D.Float res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,1,0);
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,0,1);
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0.5f,0,0.5f);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(-1,0,0);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,-1,0);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,0,-1);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .01f);

        normal = new Vector3D.Float(0,-0.5f,0.5f);
        normal.normalize();
        bytes = Vectors.toOctByte(normal);
        res = Vectors.octToNormal(bytes[0], bytes[1]);
        assertArrayEquals(normal.toArrayFloat(), res.toArrayFloat(), .02f);
    }

    @Test
    public void quantizeTransformTupleTest() throws MismatchedDimensionException, TransformException {

        final GeneralEnvelope quantizeBox = new GeneralEnvelope(3);
        quantizeBox.setRange(0, 0, 100);
        quantizeBox.setRange(1, 100, 300);
        quantizeBox.setRange(2, -300, -100);

        final Vector3D.Double coord1 = new Vector3D.Double(0,100,-300);
        final Vector3D.Double coord2 = new Vector3D.Double(100,300,-100);

        final Tuple dp1 = Vectors.toQuantizedEncoding(coord1, quantizeBox, 32767, null);
        final Tuple dp2 = Vectors.toQuantizedEncoding(coord2, quantizeBox, 32767, null);

        assertEquals(new Vector3D.Double(0, 0, 0), dp1);
        assertEquals(new Vector3D.Double(32767, 32767, 32767), dp2);
        assertEquals(DataType.USHORT, dp1.getDataType());
        assertEquals(DataType.USHORT, dp2.getDataType());

    }

    private static final double DELTA = 1e-8;

    @Test
    public void quantizeTransformTest() throws MismatchedDimensionException, TransformException {

        final GeneralEnvelope quantizeBox = new GeneralEnvelope(3);
        quantizeBox.setRange(0, 0, 100);
        quantizeBox.setRange(1, 100, 300);
        quantizeBox.setRange(2, -300, -100);

        MathTransform trs = Vectors.quantizedTransform(quantizeBox, 32767);

        final Vector3D.Double coord1 = new Vector3D.Double(0,100,-300);
        final Vector3D.Double coord2 = new Vector3D.Double(100,300,-100);

        coord1.transform(trs);
        coord2.transform(trs);

        assertArrayEquals(new double[] {0, 0, 0}, coord1.toArrayDouble(), DELTA);
        assertArrayEquals(new double[] {32767, 32767, 32767}, coord2.toArrayDouble(), DELTA);

    }

    private static final double SLERP_TOLERANCE = 1e-12;

    /**
     * Spherical interpolation must sweep the angle evenly, and must keep the length of
     * the vectors, which is what sets it apart from a linear interpolation.
     */
    @Test
    public void slerpTest() {
        final double[] start = {1, 0, 0};
        final double[] end   = {0, 1, 0};

        //the ends are returned as they are, and ratios outside the range are clamped
        assertArrayEquals(start, Vectors.slerp(start, end, 0), SLERP_TOLERANCE);
        assertArrayEquals(end,   Vectors.slerp(start, end, 1), SLERP_TOLERANCE);
        assertArrayEquals(start, Vectors.slerp(start, end, -1), SLERP_TOLERANCE);
        assertArrayEquals(end,   Vectors.slerp(start, end, 2), SLERP_TOLERANCE);

        //the middle of a quarter circle is at 45 degrees, not at the middle of the chord
        final double h = Math.sqrt(0.5);
        assertArrayEquals(new double[] {h, h, 0}, Vectors.slerp(start, end, 0.5), SLERP_TOLERANCE);
        //where a linear interpolation would fall short, inside the circle
        assertEquals(Math.sqrt(0.5), Vectors.length(Vectors.lerp(start, end, 0.5)), SLERP_TOLERANCE);

        //a third of the way is a third of the angle, so 30 degrees
        assertArrayEquals(new double[] {Math.cos(Math.PI/6), Math.sin(Math.PI/6), 0},
                          Vectors.slerp(start, end, 1.0/3), SLERP_TOLERANCE);

        //the length is kept, and the angle grows proportionally to the ratio
        for (int i = 0; i <= 10; i++) {
            final double ratio = i / 10.0;
            final double[] point = Vectors.slerp(start, end, ratio);
            assertEquals(1, Vectors.length(point), SLERP_TOLERANCE, "Length must be kept");
            assertEquals(ratio * Math.PI/2, Math.acos(Vectors.dot(point, start)), SLERP_TOLERANCE,
                         "Angle must grow proportionally to the ratio");
        }
    }

    /**
     * Spherical interpolation must work in any dimension, and on vectors which are not
     * unit vectors.
     */
    @Test
    public void slerpOtherLengthsAndDimensionsTest() {
        //2D vectors of length 3
        final double[] start2D = {3, 0};
        final double[] end2D   = {0, 3};
        final double[] mid2D = Vectors.slerp(start2D, end2D, 0.5);
        assertEquals(3, Vectors.length(mid2D), SLERP_TOLERANCE);
        assertArrayEquals(new double[] {3*Math.sqrt(0.5), 3*Math.sqrt(0.5)}, mid2D, SLERP_TOLERANCE);

        //4D vectors
        final double[] start4D = {1, 0, 0, 0};
        final double[] end4D   = {0, 0, 0, 1};
        final double[] mid4D = Vectors.slerp(start4D, end4D, 0.5);
        assertEquals(1, Vectors.length(mid4D), SLERP_TOLERANCE);
        assertArrayEquals(new double[] {Math.sqrt(0.5), 0, 0, Math.sqrt(0.5)}, mid4D, SLERP_TOLERANCE);

        //vectors of different lengths still sweep the angle evenly
        final double[] shortV = {1, 0, 0};
        final double[] longV  = {0, 4, 0};
        final double[] mid = Vectors.slerp(shortV, longV, 0.5);
        assertEquals(Math.PI/4, Math.acos(Vectors.dot(mid, shortV) / Vectors.length(mid)), SLERP_TOLERANCE);
    }

    /**
     * Interpolating between aligned vectors : parallel vectors have no arc to sweep so a
     * linear interpolation is used, and opposite vectors define no unique arc at all so
     * they must be rejected rather than silently produce NaN.
     */
    @Test
    public void slerpAlignedTest() {
        final double[] a = {1, 0, 0};

        //parallel vectors of the same length
        assertArrayEquals(a, Vectors.slerp(a, a.clone(), 0.5), SLERP_TOLERANCE);
        //parallel vectors of different lengths interpolate linearly
        assertArrayEquals(new double[] {2, 0, 0}, Vectors.slerp(a, new double[] {3, 0, 0}, 0.5), SLERP_TOLERANCE);
        //a vector of length zero has no direction, the interpolation stays linear
        assertArrayEquals(new double[] {0.5, 0, 0}, Vectors.slerp(a, new double[] {0, 0, 0}, 0.5), SLERP_TOLERANCE);

        //opposite vectors
        final double[] opposite = {-1, 0, 0};
        assertThrows(IllegalArgumentException.class, () -> Vectors.slerp(a, opposite, 0.5));
        //but the ends themselves are still well defined
        assertArrayEquals(a, Vectors.slerp(a, opposite, 0), SLERP_TOLERANCE);
        assertArrayEquals(opposite, Vectors.slerp(a, opposite, 1), SLERP_TOLERANCE);
    }

    /**
     * Vectors of different sizes cannot be interpolated, and a buffer must match.
     */
    @Test
    public void slerpInvalidTest() {
        final double[] v3 = {1, 0, 0};
        final double[] v2 = {0, 1};
        assertThrows(IllegalArgumentException.class, () -> Vectors.slerp(v3, v2, 0.5));
        assertThrows(IllegalArgumentException.class, () -> Vectors.slerp(v3, v3.clone(), 0.5, new double[2]));
    }

    /**
     * The interpolation must also be reachable from the vector itself, mutating it.
     */
    @Test
    public void slerpOnVectorTest() {
        final Vector3D.Double start = new Vector3D.Double(1, 0, 0);
        final Vector3D.Double end   = new Vector3D.Double(0, 1, 0);
        final Vector3D.Double result = start.slerp(end, 0.5);
        //the vector is mutated in place and returned
        assertSame(start, result);
        final double h = Math.sqrt(0.5);
        assertArrayEquals(new double[] {h, h, 0}, result.toArrayDouble(), SLERP_TOLERANCE);
        //the other vector is left alone
        assertArrayEquals(new double[] {0, 1, 0}, end.toArrayDouble(), SLERP_TOLERANCE);
    }
}
