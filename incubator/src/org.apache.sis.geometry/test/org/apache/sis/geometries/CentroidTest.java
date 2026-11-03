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
package org.apache.sis.geometries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.curve.Arc;
import org.apache.sis.geometries.curve.Circle;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.curve.OffsetCurve;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.surface.CurvePolygon;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vector;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link Geometry#getCentroid()} and {@link Geometry#getRepresentativePoint()} on all two-dimensional
 * geometry types, including those computed on a linear approximation and the mesh primitives.
 *
 * @author Johann Sorel (Geomatys)
 */
public class CentroidTest {

    private static final double TOLERANCE = 1E-9;

    /**
     * Tolerance for the results computed on a linear approximation of a curve.
     */
    private static final double CURVE_TOLERANCE = 1E-5;

    private static final GeometryFactory F = GeometryFactory.DEFAULT;

    private static final CoordinateReferenceSystem CRS2 = Geometries.getUndefinedCRS(2);

    private static DataPoints points(double... c) {
        return F.createDataPoints(CRS2, c);
    }

    private static Array array(double... c) {
        return NDArrays.of(CRS2, c);
    }

    private static LineString line(double... c) {
        return F.createLineString(points(c));
    }

    private static LinearRing ring(double... c) {
        return F.createLinearRing(points(c));
    }

    private static Polygon polygon(LinearRing exterior, LinearRing... holes) {
        return F.createPolygon(exterior, List.of(holes));
    }

    private static Circle circle(double cx, double cy, double r) {
        return F.createCircle(points(cx+r,cy, cx,cy+r, cx-r,cy, cx,cy-r, cx+r,cy),
                              array(cx,cy, cx,cy, cx,cy, cx,cy), List.of());
    }

    private static <M extends MeshPrimitive> M mesh(M mesh, double... c) {
        mesh.setPositions(array(c));
        return mesh;
    }

    private static void assertPoint(double x, double y, Point actual, double tolerance) {
        assertNotNull(actual);
        final Vector<?> p = actual.getPosition();
        assertEquals(x, p.get(0), tolerance, "x");
        assertEquals(y, p.get(1), tolerance, "y");
    }

    /**
     * Verifies that the given point is inside the rectangle.
     */
    private static void assertInside(double xmin, double ymin, double xmax, double ymax, Point actual) {
        assertNotNull(actual);
        final Vector<?> p = actual.getPosition();
        assertTrue(p.get(0) > xmin && p.get(0) < xmax && p.get(1) > ymin && p.get(1) < ymax, "Point outside: " + p);
    }

    /**
     * Verifies that the given point is one of the given positions.
     */
    private static void assertOneOf(Point actual, double... c) {
        assertNotNull(actual);
        final Vector<?> p = actual.getPosition();
        for (int i = 0; i < c.length; i += 2) {
            if (p.get(0) == c[i] && p.get(1) == c[i+1]) return;
        }
        fail("Point is not one of the positions: " + p);
    }

    /**
     * Tests points, line strings, polygons and their collections.
     */
    @Test
    public void testSimpleFeatures() {
        Geometry g = F.createPoint(CRS2, 1, 2);
        assertPoint(1, 2, g.getCentroid(), TOLERANCE);
        assertPoint(1, 2, g.getRepresentativePoint(), TOLERANCE);

        g = F.createMultiPoint(points(0,0, 2,1, 1,3));
        assertPoint(1, 4.0/3, g.getCentroid(), TOLERANCE);
        assertOneOf(g.getRepresentativePoint(), 0,0, 2,1, 1,3);

        g = line(0,0, 2,1, 3,3);
        assertPoint(1.75, 1.25, g.getCentroid(), TOLERANCE);
        assertPoint(2, 1, g.getRepresentativePoint(), TOLERANCE);

        // Segments of lengths 2, 2 and 2√2 with middles (1,0), (2,1) and (1,1).
        g = ring(0,0, 2,0, 2,2, 0,0);
        final double s2 = Math.sqrt(2);
        assertPoint((6 + 2*s2) / (4 + 2*s2), (2 + 2*s2) / (4 + 2*s2), g.getCentroid(), TOLERANCE);

        // Lines of lengths 2 and √5 with middles (1,0) and (1,1.5).
        g = F.createMultiLineString(CRS2, line(0,0, 2,0), line(0,1, 2,2));
        final double s5 = Math.sqrt(5);
        assertPoint(1, 1.5*s5 / (2 + s5), g.getCentroid(), TOLERANCE);
        assertPoint(1, 0, g.getRepresentativePoint(), TOLERANCE);

        // A square of area 100 with a hole of area 4 centered on (2,2).
        g = polygon(ring(0,0, 10,0, 10,10, 0,10, 0,0), ring(1,1, 1,3, 3,3, 3,1, 1,1));
        assertPoint(5.125, 5.125, g.getCentroid(), TOLERANCE);
        final Vector<?> p = g.getRepresentativePoint().getPosition();
        assertInside(0, 0, 10, 10, g.getRepresentativePoint());
        assertFalse(p.get(0) > 1 && p.get(0) < 3 && p.get(1) > 1 && p.get(1) < 3, "Point in the hole");

        g = F.createMultiPolygon(CRS2, polygon(ring(0,0, 1,0, 1,1, 0,1, 0,0)), polygon(ring(2,0, 3,0, 3,1, 2,1, 2,0)));
        assertPoint(1.5, 0.5, g.getCentroid(), TOLERANCE);
        assertInside(0, 0, 1, 1, g.getRepresentativePoint());

        g = F.createTriangle(ring(0,0, 2,0, 0,2, 0,0));
        assertPoint(2.0/3, 2.0/3, g.getCentroid(), TOLERANCE);

        // Elements of lower dimensions are ignored.
        g = F.createGeometryCollection(CRS2, F.createPoint(CRS2, 0, 0), line(1,0, 3,2));
        assertPoint(2, 1, g.getCentroid(), TOLERANCE);
        assertPoint(2, 1, g.getRepresentativePoint(), TOLERANCE);
    }

    /**
     * Tests curves which are not line strings, computed on a linear approximation.
     */
    @Test
    public void testCurves() {
        // Quarter of the unit circle, centroid at 2/π from the center on both axes.
        final Arc arc = F.createArc(points(1,0, 0,1), array(0,0), List.of(), false);
        assertPoint(2/Math.PI, 2/Math.PI, arc.getCentroid(), CURVE_TOLERANCE);
        assertPoint(Math.sqrt(0.5), Math.sqrt(0.5), arc.getRepresentativePoint(), CURVE_TOLERANCE);

        final Circle circle = circle(0, 0, 1);
        assertPoint(0, 0, circle.getCentroid(), CURVE_TOLERANCE);
        assertPoint(-1, 0, circle.getRepresentativePoint(), CURVE_TOLERANCE);

        // Upper half of the circle of radius 1 centered on (1,0).
        final Curve half = F.createCircularString(points(0,0, 1,1, 2,0));
        assertPoint(1, 2/Math.PI, half.getCentroid(), CURVE_TOLERANCE);
        assertPoint(1, 1, half.getRepresentativePoint(), CURVE_TOLERANCE);

        // Lines of lengths 2 and 1 with middles (0,0) and (1,0.5).
        final Curve compound = F.createCompoundCurve(line(-1,0, 1,0), line(1,0, 1,1));
        assertPoint(1.0/3, 1.0/6, compound.getCentroid(), CURVE_TOLERANCE);
        assertPoint(0.5, 0, compound.getRepresentativePoint(), CURVE_TOLERANCE);

        // Symmetric quadratic Bézier curve culminating at (1,1).
        final Curve bezier = F.createBezier(points(0,0, 2,0), array(0,0, 1,2, 2,0), new double[] {0, 1}, 2,
                                            null, null, null, null);
        final Point centroid = bezier.getCentroid();
        assertEquals(1, centroid.getPosition().get(0), CURVE_TOLERANCE);
        assertInside(0, 0, 2, 1, centroid);
        assertPoint(1, 1, bezier.getRepresentativePoint(), CURVE_TOLERANCE);

        final OffsetCurve offset = F.createOffsetCurve(line(0,0, 2,1, 3,3), Quantities.create(0.5, Units.UNITY), null);
        final LineString linear = offset.asLine(null, null);
        assertEquals(linear.getCentroid().getPosition(), offset.getCentroid().getPosition());
        assertEquals(linear.getRepresentativePoint().getPosition(), offset.getRepresentativePoint().getPosition());
    }

    /**
     * Tests surfaces which are not polygons.
     */
    @Test
    public void testSurfaces() {
        final CurvePolygon disk = F.createCurvePolygon(circle(0, 0, 2), List.of());
        assertPoint(0, 0, disk.getCentroid(), CURVE_TOLERANCE);
        assertTrue(disk.getRepresentativePoint().getPosition().length() < 2);
        assertEquals(4 * Math.PI, disk.getArea().getValue().doubleValue(), 1E-4);

        final CurvePolygon square = F.createCurvePolygon(ring(0,0, 2,0, 2,2, 0,2, 0,0), List.of());
        assertPoint(1, 1, square.getCentroid(), TOLERANCE);
        assertInside(0, 0, 2, 2, square.getRepresentativePoint());

        Geometry g = F.createPolyhedralSurface(CRS2, new Polygon[] {
                polygon(ring(0,0, 1,0, 1,1, 0,1, 0,0)), polygon(ring(1,0, 2,0, 2,1, 1,1, 1,0))});
        assertPoint(1, 0.5, g.getCentroid(), TOLERANCE);
        assertInside(0, 0, 2, 1, g.getRepresentativePoint());

        g = F.createTIN(CRS2, new Triangle[] {
                F.createTriangle(ring(0,0, 2,0, 0,2, 0,0)), F.createTriangle(ring(2,0, 2,2, 0,2, 2,0))});
        assertPoint(1, 1, g.getCentroid(), TOLERANCE);
        assertInside(0, 0, 2, 2, g.getRepresentativePoint());

        // A curve polygon of area 4 centered on (1,1) and a polygon of area 1 centered on (4.5,0.5).
        g = F.createGeometryCollection(CRS2, square, polygon(ring(4,0, 5,0, 5,1, 4,1, 4,0)));
        assertPoint(1.7, 0.9, g.getCentroid(), TOLERANCE);
    }

    /**
     * Tests the mesh primitives, which are viewed as collections, line strings or triangulated surfaces.
     */
    @Test
    public void testMeshes() {
        Geometry g = mesh(new MeshPrimitive.Points(), 0,0, 2,0, 1,3);
        assertPoint(1, 1, g.getCentroid(), TOLERANCE);
        assertOneOf(g.getRepresentativePoint(), 0,0, 2,0, 1,3);

        g = mesh(new MeshPrimitive.Lines(), 0,0, 2,0, 0,1, 2,2);
        final double s5 = Math.sqrt(5);
        assertPoint(1, 1.5*s5 / (2 + s5), g.getCentroid(), TOLERANCE);

        g = mesh(new MeshPrimitive.LineStrip(), 0,0, 2,1, 3,3);
        assertPoint(1.75, 1.25, g.getCentroid(), TOLERANCE);
        assertPoint(2, 1, g.getRepresentativePoint(), TOLERANCE);

        g = mesh(new MeshPrimitive.LineLoop(), 0,0, 2,0, 2,2);
        assertEquals(ring(0,0, 2,0, 2,2, 0,0).getCentroid().getPosition(), g.getCentroid().getPosition());

        for (final MeshPrimitive m : new MeshPrimitive[] {
                mesh(new MeshPrimitive.Triangles(), 0,0, 2,0, 0,2, 2,0, 2,2, 0,2),
                mesh(new MeshPrimitive.TriangleStrip(), 0,0, 0,2, 2,0, 2,2),
                mesh(new MeshPrimitive.TriangleFan(), 0,0, 2,0, 2,2, 0,2)})
        {
            assertPoint(1, 1, m.getCentroid(), TOLERANCE);
            assertInside(0, 0, 2, 2, m.getRepresentativePoint());
        }
    }

    /**
     * Name of an attribute holding {@code x + 10*y} at each position. Any interpolation of a linear
     * function of the position gives the value of that function at the interpolated position.
     */
    private static final String ATT_TEST = "TEST";

    /**
     * Creates data points with a {@value #ATT_TEST} attribute.
     */
    private static DataPoints attributed(double... c) {
        final double[] values = new double[c.length / 2];
        for (int i = 0; i < values.length; i++) {
            values[i] = c[2*i] + 10 * c[2*i + 1];
        }
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, array(c));
        attributes.put(ATT_TEST, NDArrays.of(1, values));
        return F.createDataPoints(attributes);
    }

    private static LinearRing attributedRing(double... c) {
        return F.createLinearRing(attributed(c));
    }

    /**
     * Verifies that the {@value #ATT_TEST} attribute of the given point is {@code x + 10*y}.
     */
    private static void assertAttribute(Point point) {
        assertNotNull(point);
        final Vector<?> p = point.getPosition();
        final Vector<?> value = point.getAttribute(ATT_TEST);
        assertNotNull(value, "Missing attribute");
        assertEquals(p.get(0) + 10 * p.get(1), value.get(0), TOLERANCE);
    }

    /**
     * Tests that all attributes are interpolated at the centroid and at the representative point.
     */
    @Test
    public void testAttributes() {
        Geometry g = F.createLineString(attributed(0,0, 2,1, 3,3));
        assertAttribute(g.getCentroid());
        assertAttribute(g.getRepresentativePoint());

        final Triangle triangle = F.createTriangle(attributedRing(0,0, 2,0, 0,2, 0,0));
        assertPoint(2.0/3, 2.0/3, triangle.getCentroid(), TOLERANCE);
        assertAttribute(triangle.getCentroid());

        final Polygon holed = polygon(attributedRing(0,0, 10,0, 10,10, 0,10, 0,0), attributedRing(1,1, 1,3, 3,3, 3,1, 1,1));
        assertPoint(5.125, 5.125, holed.getCentroid(), TOLERANCE);
        assertAttribute(holed.getCentroid());
        assertAttribute(holed.getRepresentativePoint());

        g = F.createMultiPolygon(CRS2, holed, polygon(attributedRing(20,0, 21,0, 21,1, 20,1, 20,0)));
        assertAttribute(g.getCentroid());

        g = F.createTIN(CRS2, new Triangle[] {
                F.createTriangle(attributedRing(0,0, 2,0, 0,2, 0,0)), F.createTriangle(attributedRing(2,0, 2,2, 0,2, 2,0))});
        assertPoint(1, 1, g.getCentroid(), TOLERANCE);
        assertAttribute(g.getCentroid());
        assertAttribute(g.getRepresentativePoint());

        final MeshPrimitive.Triangles mesh = mesh(new MeshPrimitive.Triangles(), 0,0, 2,0, 0,2, 2,0, 2,2, 0,2);
        mesh.setAttribute(ATT_TEST, NDArrays.of(1, new double[] {0, 2, 20, 2, 22, 20}));
        assertAttribute(mesh.getCentroid());
        assertAttribute(mesh.getRepresentativePoint());
    }

    /**
     * Tests that normals are normalized after interpolation.
     */
    @Test
    public void testNormalizedAttribute() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, array(0,0, 2,0, 0,2, 0,0));
        attributes.put(DataPointsType.ATT_NORMAL, NDArrays.of(3, new double[] {0,0,1, 0,1,0, 1,0,0, 0,0,1}));
        final Triangle triangle = F.createTriangle(F.createLinearRing(F.createDataPoints(attributes)));
        final double c = 1 / Math.sqrt(3);
        assertArrayEquals(new double[] {c, c, c}, triangle.getCentroid().getAttribute(DataPointsType.ATT_NORMAL).toArrayDouble(), TOLERANCE);
    }
}
