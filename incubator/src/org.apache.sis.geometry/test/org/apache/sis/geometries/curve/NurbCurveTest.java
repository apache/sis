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

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.sis.geometries.AttributeInterpolation;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.apache.sis.geometries.GeometryTest;


/**
 * Tests {@link NurbCurve}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class NurbCurveTest extends GeometryTest {
    @Test
    @Disabled("Not implemented yet.")
    public void testGetGeometryType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetInterpolation() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testSetCoordinateReferenceSystem() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetAttributesType() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsEmpty() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetCurveForm() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetKnotSpec() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testIsRational() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetNumArc() {
    }

    @Test
    @Disabled("Not implemented yet.")
    public void testGetSegment() {
    }

    /**
     * Creates the quarter of the circle of radius 10 centered on (0 0), from (10 0) to (0 10),
     * as a rational quadratic curve. Its length is 5π.
     */
    private static NurbCurve createQuarterCircle(final DataPoints points) {
        return GeometryFactory.DEFAULT.createNurbCurve(points,
                new double[] {1, Math.sqrt(0.5), 1}, new double[] {0,0,0, 1,1,1}, 2);
    }

    @Test
    public void testGetLength() {
        final NurbCurve arc = createQuarterCircle(GeometryFactory.DEFAULT.createDataPoints(CRS_2D, new double[] {10,0, 10,10, 0,10}));
        assertEquals(5 * Math.PI, arc.getLength().getValue().doubleValue(), 1E-9);
    }

    /**
     * The point at a distance along the quarter circle is at the angle of that distance divided by the radius.
     * The rational parameter is not proportional to the angle, so this verifies the arc length parameterization.
     */
    @Test
    public void testSubCurve_Length() {
        final NurbCurve arc = createQuarterCircle(GeometryFactory.DEFAULT.createDataPoints(CRS_2D, new double[] {10,0, 10,10, 0,10}));
        final double h = 10 * Math.sqrt(0.5);
        assertPositionEquals(10, 0, arc.subCurve(Quantities.create(0, Units.PERCENT)));
        assertPositionEquals( h, h, arc.subCurve(Quantities.create(50, Units.PERCENT)));
        assertPositionEquals( 0, 10, arc.subCurve(Quantities.create(100, Units.PERCENT)));
        assertPositionEquals(10 * Math.cos(Math.PI / 6), 5, arc.subCurve(Quantities.create(5 * Math.PI / 3, Units.DEGREE)));
        assertPositionEquals(5, 10 * Math.sin(Math.PI / 3), arc.subCurve(Quantities.create(10 * Math.PI / 3, Units.DEGREE)));
        assertPositionEquals( h, h, arc.getRepresentativePoint());
    }

    /**
     * Attributes interpolated linearly are blended with the rational basis functions,
     * while the others take the value of the control point of largest weight.
     */
    @Test
    public void testSubCurve_LengthWithAttributes() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(CRS_2D, new double[] {10,0, 10,10, 0,10}));
        attributes.put("nearest", NDArrays.of(1, new double[] {0, 0, 30}));
        attributes.put("linear",  NDArrays.of(1, new double[] {0, 0, 30}));
        final NurbCurve arc = createQuarterCircle(withInterpolations(GeometryFactory.DEFAULT.createDataPoints(attributes),
                Map.of("linear", AttributeInterpolation.LINEAR)));
        final Point point = arc.subCurve(Quantities.create(50, Units.PERCENT));
        assertEquals(0, point.getAttribute("nearest").get(0), TOLERANCE, "Value of the control point of largest weight.");
        assertEquals(30 * (1 - Math.sqrt(0.5)), point.getAttribute("linear").get(0), TOLERANCE, "Blended by the basis functions.");
    }
}
