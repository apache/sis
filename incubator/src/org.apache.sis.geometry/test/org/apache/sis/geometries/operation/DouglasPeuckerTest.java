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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.GeometryType;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.curve.CompoundCurve;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.internal.shared.ArrayDataPoints;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.surface.Triangle;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link DouglasPeucker}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DouglasPeuckerTest {

    private static final double TOLERANCE = 1E-9;

    private static final CoordinateReferenceSystem CRS_2D = CommonCRS.WGS84.normalizedGeographic();

    private static final CoordinateReferenceSystem CRS_3D = CommonCRS.WGS84.geographic3D();

    /**
     * Creates the data points of the given coordinates ordered as x0, y0, … xn, yn.
     */
    private static ArrayDataPoints points(double... coords) {
        return new ArrayDataPoints(NDArrays.of(CRS_2D, coords));
    }

    /**
     * Creates a line string from coordinates ordered as x0, y0, … xn, yn.
     */
    private static LineString line(double... coords) {
        return GeometryFactory.DEFAULT.createLineString(points(coords));
    }

    /**
     * Creates a linear ring from coordinates ordered as x0, y0, … xn, yn.
     */
    private static LinearRing ring(double... coords) {
        return GeometryFactory.DEFAULT.createLinearRing(points(coords));
    }

    private static double[] positions(Geometry geom) {
        return geom.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble();
    }

    /**
     * Tests the simplification of a line string.
     * The point farthest from the segment between the end points is kept, the other points are removed.
     */
    @Test
    public void testLineString() {
        final LineString line = line(0, 0,  1, 0.1,  2, 3,  3, 0.1,  4, 0);
        final LineString result = DouglasPeucker.simplify(line, 1);
        assertEquals(CRS_2D, result.getCoordinateReferenceSystem());
        assertArrayEquals(new double[] {0, 0,  2, 3,  4, 0}, positions(result), TOLERANCE);
        /*
         * With a smaller tolerance, all points are kept: the points (1, 0.1) and (3, 0.1) are at
         * a distance of 0.78 from the segments (0,0)–(2,3) and (2,3)–(4,0) respectively.
         */
        assertSame(line, DouglasPeucker.simplify(line, 0.5));
        /*
         * With a large tolerance, only the end points are kept.
         */
        assertArrayEquals(new double[] {0, 0,  4, 0}, positions(DouglasPeucker.simplify(line, 10)), TOLERANCE);
    }

    /**
     * Tests that the geometry is returned unchanged when the tolerance is not strictly positive.
     */
    @Test
    public void testInvalidTolerance() {
        final LineString line = line(0, 0,  1, 0.1,  2, 0);
        assertSame(line, DouglasPeucker.simplify(line, 0));
        assertSame(line, DouglasPeucker.simplify(line, -1));
        assertSame(line, DouglasPeucker.simplify(line, Double.NaN));
        assertSame(line, DouglasPeucker.simplify((Geometry) line, 0));
    }

    /**
     * Tests that distances are measured to the segment and not to the infinite line.
     * A point going back before the start point is aligned with the end points but far from the segment.
     */
    @Test
    public void testSegmentDistance() {
        final LineString line = line(0, 0,  -5, 0,  10, 0);
        assertSame(line, DouglasPeucker.simplify(line, 1));
    }

    /**
     * Tests that all attributes of the kept points are preserved.
     */
    @Test
    public void testAttributes() {
        final Map<String,Array> attributes = new LinkedHashMap<>();
        attributes.put(DataPointsType.ATT_POSITION, NDArrays.of(CRS_2D, 0, 0,  1, 0.1,  2, 3,  3, 0.1,  4, 0));
        attributes.put("value", NDArrays.of(1, 10.0, 11.0, 12.0, 13.0, 14.0));
        final LineString line = GeometryFactory.DEFAULT.createLineString(new ArrayDataPoints(attributes));
        final LineString result = DouglasPeucker.simplify(line, 1);
        assertArrayEquals(new double[] {0, 0,  2, 3,  4, 0}, positions(result), TOLERANCE);
        assertArrayEquals(new double[] {10, 12, 14},
                result.getDataPoints().getAttributeArray("value").toArrayDouble(), TOLERANCE);
    }

    /**
     * Tests that all dimensions are used in distance calculation.
     */
    @Test
    public void test3D() {
        final LineString high = GeometryFactory.DEFAULT.createLineString(new ArrayDataPoints(
                NDArrays.of(CRS_3D, 0.0, 0.0, 0.0,  1.0, 0.0, 5.0,  2.0, 0.0, 0.0)));
        assertSame(high, DouglasPeucker.simplify(high, 1));

        final LineString low = GeometryFactory.DEFAULT.createLineString(new ArrayDataPoints(
                NDArrays.of(CRS_3D, 0, 0, 0,  1, 0, 0.5,  2, 0, 0)));
        final LineString result = DouglasPeucker.simplify(low, 1);
        assertEquals(CRS_3D, result.getCoordinateReferenceSystem());
        assertArrayEquals(new double[] {0, 0, 0,  2, 0, 0}, positions(result), TOLERANCE);
    }

    /**
     * Tests that points with non-finite coordinates are kept
     * and that the sections between them are simplified independently.
     */
    @Test
    public void testNonFinite() {
        final double nan = Double.NaN;
        final LineString line = line(0, 0,  1, 0.1,  2, 0,  nan, nan,  3, 0,  4, 0.1,  5, 0);
        final LineString result = DouglasPeucker.simplify(line, 0.5);
        assertArrayEquals(new double[] {0, 0,  2, 0,  nan, nan,  3, 0,  5, 0}, positions(result), TOLERANCE);
    }

    /**
     * Tests the simplification of a linear ring.
     */
    @Test
    public void testLinearRing() {
        final LinearRing ring = ring(0, 0,  5, 0.1,  10, 0,  10, 10,  0, 10,  0, 0);
        final LinearRing result = DouglasPeucker.simplify(ring, 1);
        assertArrayEquals(new double[] {0, 0,  10, 0,  10, 10,  0, 10,  0, 0}, positions(result), TOLERANCE);
        /*
         * Same test through the generic method, which shall preserve the ring type.
         */
        final Geometry generic = DouglasPeucker.simplify((Geometry) ring, 1);
        assertInstanceOf(LinearRing.class, generic);
        assertArrayEquals(positions(result), positions(generic), TOLERANCE);
    }

    /**
     * Tests that a ring smaller than the tolerance does not collapse below a triangle.
     * The kept points are the first point, the point farthest from it,
     * and the point farthest from the segment between those two points.
     */
    @Test
    public void testRingCollapse() {
        final LinearRing ring = ring(0, 0,  1, 0,  2, 0.1,  1, 0.2,  0, 0.1,  0, 0);
        final LinearRing result = DouglasPeucker.simplify(ring, 5);
        assertArrayEquals(new double[] {0, 0,  2, 0.1,  1, 0.2,  0, 0}, positions(result), TOLERANCE);
        /*
         * A ring which is already a triangle cannot be simplified.
         */
        assertSame(result, DouglasPeucker.simplify(result, 5));
    }

    /**
     * Tests the simplification of the exterior and interior rings of a polygon.
     */
    @Test
    public void testPolygon() {
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(
                ring(0, 0,  5, 0.1,  10, 0,  10, 10,  0, 10,  0, 0),
                List.of(ring(2, 2,  5, 2.1,  8, 2,  8, 8,  2, 8,  2, 2)));
        final Polygon result = DouglasPeucker.simplify(polygon, 1);
        assertEquals(1, result.getNumInteriorRing());
        assertArrayEquals(new double[] {0, 0,  10, 0,  10, 10,  0, 10,  0, 0},
                positions(result.getExteriorRing()), TOLERANCE);
        assertArrayEquals(new double[] {2, 2,  8, 2,  8, 8,  2, 8,  2, 2},
                positions(result.getInteriorRingN(0)), TOLERANCE);
    }

    /**
     * Tests that geometries without removable points are returned unchanged.
     */
    @Test
    public void testUnchanged() {
        final Point point = GeometryFactory.DEFAULT.createPoint(CRS_2D, 1, 2);
        assertSame(point, DouglasPeucker.simplify(point, 10));

        final Triangle triangle = GeometryFactory.DEFAULT.createTriangle(ring(0, 0,  10, 0,  0, 10,  0, 0));
        assertSame(triangle, DouglasPeucker.simplify(triangle, 100));
        assertSame(triangle, DouglasPeucker.simplify((Geometry) triangle, 100));

        final GeometryCollection<Point> points = GeometryFactory.DEFAULT.createMultiPoint(CRS_2D,
                GeometryFactory.DEFAULT.createPoint(CRS_2D, 1, 2),
                GeometryFactory.DEFAULT.createPoint(CRS_2D, 3, 4));
        assertSame(points, DouglasPeucker.simplify(points, 10));
    }

    /**
     * Tests the simplification of each element of a collection, through {@link GeometryProcessor}.
     */
    @Test
    public void testCollection() {
        final LineString unchanged = line(0, 0,  1, 5,  2, 0);
        final GeometryCollection<LineString> lines = GeometryFactory.DEFAULT.createMultiLineString(CRS_2D,
                line(0, 0,  1, 0.1,  2, 0),
                unchanged);
        final Geometry result = new GeometryProcessor().simplify(lines, 1);
        final GeometryCollection<?> col = assertInstanceOf(GeometryCollection.class, result);
        assertEquals(GeometryType.LINESTRING, col.getElementType());
        assertEquals(CRS_2D, col.getCoordinateReferenceSystem());
        assertEquals(2, col.getNumGeometries());
        assertArrayEquals(new double[] {0, 0,  2, 0}, positions(col.getGeometryN(0)), TOLERANCE);
        assertSame(unchanged, col.getGeometryN(1));
    }

    /**
     * Tests that each component of a compound curve is simplified with its end points kept.
     */
    @Test
    public void testCompoundCurve() {
        final CompoundCurve curve = GeometryFactory.DEFAULT.createCompoundCurve(CRS_2D,
                line(0, 0,  1, 0.1,  2, 0),
                line(2, 0,  3, 0.1,  4, 0));
        final CompoundCurve result = assertInstanceOf(CompoundCurve.class, DouglasPeucker.simplify(curve, 0.5));
        assertEquals(2, result.getNumCurves());
        assertArrayEquals(new double[] {0, 0,  2, 0}, positions(result.getCurveN(0)), TOLERANCE);
        assertArrayEquals(new double[] {2, 0,  4, 0}, positions(result.getCurveN(1)), TOLERANCE);
    }

    /**
     * Tests that mesh line strips and line loops keep their primitive type.
     */
    @Test
    public void testMeshPrimitive() {
        final LineString strip = DouglasPeucker.simplify(new MeshPrimitive.LineStrip(points(0, 0,  1, 0.1,  2, 0)), 0.5);
        assertInstanceOf(MeshPrimitive.LineStrip.class, strip);
        assertArrayEquals(new double[] {0, 0,  2, 0}, positions(strip), TOLERANCE);
        /*
         * The data points of a line loop repeat the first point at the end.
         */
        final LineString loop = DouglasPeucker.simplify(new MeshPrimitive.LineLoop(points(0, 0,  5, 0.1,  10, 0,  10, 10,  0, 10)), 1);
        assertInstanceOf(MeshPrimitive.LineLoop.class, loop);
        assertArrayEquals(new double[] {0, 0,  10, 0,  10, 10,  0, 10,  0, 0}, positions(loop), TOLERANCE);
    }
}
