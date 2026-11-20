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
package org.apache.sis.geometries.operation;

import javax.measure.Quantity;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.Prism;
import org.apache.sis.geometries.Representation;
import org.apache.sis.geometries.curve.CircularString;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.DataType;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link GeometryProcessor#discretize(Geometry, Quantity, Quantity)}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DiscretizeTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    private static final GeometryFactory F = GeometryFactory.DEFAULT;

    /**
     * Maximal distance between a curve and its linear approximation used in the tests.
     */
    private static final Quantity<?> OFFSET = Quantities.create(0.001, Units.DEGREE);

    /**
     * Creates a half circle of radius 5 centered on (5,0).
     */
    private static CircularString arc() {
        return F.createCircularString(F.createDataPoints(CRS2D, 0,0, 5,5, 10,0));
    }

    /**
     * Asserts that all the positions are on the circle of radius 5 centered on (5,0).
     */
    private static void assertOnCircle(final DataPoints points) {
        for (int i = 0; i < points.size(); i++) {
            final double[] p = points.getPosition(i).toArrayDouble();
            assertEquals(5, Math.hypot(p[0] - 5, p[1]), 1E-9, "Point not on the circle.");
        }
    }

    /**
     * A curve becomes a line string whose points are on the curve.
     */
    @Test
    public void testCurve() {
        final Geometry result = arc().discretize(null, OFFSET);
        assertInstanceOf(LineString.class, result);
        assertEquals(Representation.DISCRETE, result.getRepresentation());
        assertTrue(result.getDataPoints().size() > 3, "The arc shall be densified.");
        assertOnCircle(result.getDataPoints());
    }

    /**
     * A curve polygon becomes a polygon whose rings are linear approximations of the curves.
     */
    @Test
    public void testCurvePolygon() {
        final CircularString circle = F.createCircularString(F.createDataPoints(CRS2D, 0,0, 5,5, 10,0, 5,-5, 0,0));
        final CurvePolygon disk = F.createCurvePolygon(circle, null);
        final Geometry result = disk.discretize(null, OFFSET);
        assertInstanceOf(Polygon.class, result);
        assertEquals(Representation.DISCRETE, result.getRepresentation());
        assertOnCircle(((Polygon) result).getExteriorRing().getDataPoints());
    }

    /**
     * The prism of a polygon becomes a polyhedron.
     */
    @Test
    public void testPrism() {
        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        final Prism prism = F.createPrism(square, NumberRange.create(0., true, 3., true), null);
        final Geometry result = prism.discretize(null, null);
        assertInstanceOf(Polyhedron.class, result);
        assertEquals(Representation.DISCRETE, result.getRepresentation());
    }

    /**
     * A geometry which is already discrete is returned unchanged.
     */
    @Test
    public void testDiscrete() {
        final Polygon square = F.createPolygon(F.createLinearRing(F.createDataPoints(CRS2D, 0,0, 2,0, 2,2, 0,2, 0,0)), null);
        assertSame(square, square.discretize(null, OFFSET));
        final Point point = F.createPoint(CRS2D, 1, 2);
        assertSame(point, point.discretize(null, OFFSET));
    }

    /**
     * The elements of a collection are discretized one by one, and the element type is kept when possible.
     */
    @Test
    public void testCollection() {
        final Point point = F.createPoint(CRS2D, 1, 2);
        final GeometryCollection<Geometry> mixed = F.createGeometryCollection(CRS2D, arc(), point);
        final GeometryCollection<?> result = assertInstanceOf(GeometryCollection.class, mixed.discretize(null, OFFSET));
        assertEquals(2, result.getNumGeometries());
        assertInstanceOf(LineString.class, result.getGeometryN(0));
        assertSame(point, result.getGeometryN(1));
        assertEquals(Representation.DISCRETE, result.getRepresentation());

        final GeometryCollection<CircularString> curves = F.createMultiCurve(CRS2D, arc(), arc());
        final GeometryCollection<?> lines = assertInstanceOf(GeometryCollection.class, curves.discretize(null, OFFSET));
        assertEquals(GeometryType.CURVE, lines.getElementType());
    }

    /**
     * A geometry without discrete form causes an exception.
     */
    @Test
    public void testUnsupported() {
        final Vector<?> origin = Vectors.create(new double[] {0, 0}, DataType.DOUBLE);
        final Vector<?> direction = Vectors.create(new double[] {1, 0}, DataType.DOUBLE);
        final Geometry ray = F.createRay(CRS2D, origin, direction);
        assertThrows(UnsupportedOperationException.class, () -> ray.discretize(null, OFFSET));
    }
}
