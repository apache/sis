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
package org.apache.sis.geometries.curve;

import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link Bezier}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BezierTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    /**
     * Two cubic pieces joined at (10 0): an arch above the x axis from (0 0), then its point
     * reflection through (10 0) below the axis up to (20 0). Each piece is symmetric, so its
     * middle is at parameter ½, and both have the same length, so the curve middle is the junction.
     */
    @Test
    public void testSubCurve_Length() {
        final Bezier curve = GeometryFactory.DEFAULT.createBezier(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0, 20,0),
                NDArrays.of(CRS_2D, new double[] {0,0, 0,10, 10,10, 10,0, 10,-10, 20,-10, 20,0}),
                new double[] {0, 1}, 3, null, null, null, null);
        assertPositionEquals( 0,  0,   curve.subCurve(Quantities.create(  0, Units.PERCENT)));
        assertPositionEquals( 5,  7.5, curve.subCurve(Quantities.create( 25, Units.PERCENT)));
        assertPositionEquals(10,  0,   curve.subCurve(Quantities.create( 50, Units.PERCENT)));
        assertPositionEquals(15, -7.5, curve.subCurve(Quantities.create( 75, Units.PERCENT)));
        assertPositionEquals(20,  0,   curve.subCurve(Quantities.create(100, Units.PERCENT)));
        assertPositionEquals(10,  0,   curve.getRepresentativePoint());
    }

    /**
     * The control points shall make a whole number of pieces of the curve degree.
     */
    @Test
    public void testSubCurve_LengthInvalid() {
        final Bezier curve = GeometryFactory.DEFAULT.createBezier(
                GeometryFactory.DEFAULT.createDataPoints(CRS_2D, 0,0, 10,0),
                NDArrays.of(CRS_2D, new double[] {0,0, 0,10, 10,10, 10,5, 10,0}),
                new double[] {0, 1}, 3, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> curve.subCurve(Quantities.create(50, Units.PERCENT)));
    }
}
