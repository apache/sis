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

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometry;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.Point;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.curve.LineString;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.geometries.mesh.MeshPrimitive;
import org.apache.sis.geometries.mesh.MultiMeshPrimitive;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Array;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ToPrimitiveTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    @Test
    public void testPoint() {
        final Point point = GeometryFactory.DEFAULT.createPoint(CRS2D, 1, 2);
        final Geometry result = new GeometryProcessor().toPrimitive(point);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.POINTS, primitive.getType());
    }

    @Test
    public void testLineString() {
        final Array positions = NDArrays.of(CRS2D, new double[]{0,1,2,3});
        final DataPoints points = GeometryFactory.DEFAULT.createDataPoints(positions);
        final LineString line = GeometryFactory.DEFAULT.createLineString(points);
        final Geometry result = new GeometryProcessor().toPrimitive(line);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive.getType());
    }

    @Test
    public void testPolygon() {
        final LinearRing exterior = ring(0,0, 2,0, 2,2, 0,2, 0,0);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, null);
        final Geometry result = new GeometryProcessor().toPrimitive(polygon);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.TRIANGLES, primitive.getType());
        assertEquals(6, primitive.getPositions().getLength(), "Expected 2 triangles.");
        assertEquals(4, trianglesArea(primitive), 1E-12);
    }

    /**
     * Tests the triangulation of a polygon with a hole, which requires to cut the exterior
     * ring toward the hole. The triangles shall cover the polygon area without the hole.
     */
    @Test
    public void testPolygonWithHole() {
        final LinearRing exterior = ring(0,0, 10,0, 10,10, 0,10, 0,0);
        final LinearRing hole     = ring(2,2, 2,4, 4,4, 4,2, 2,2);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, java.util.List.of(hole));
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon);
        assertEquals(MeshPrimitive.Type.TRIANGLES, primitive.getType());
        assertEquals(96, trianglesArea(primitive), 1E-9);
    }

    /**
     * Same test as {@link #testPolygonWithHole()}, with the hole ring starting at a point which is
     * not its leftmost one. The cut toward the hole shall still start from the leftmost point.
     */
    @Test
    public void testPolygonWithHoleStartingAnywhere() {
        final LinearRing exterior = ring(0,0, 10,0, 10,10, 0,10, 0,0);
        final LinearRing hole     = ring(4,4, 4,2, 2,2, 2,4, 4,4);
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, java.util.List.of(hole));
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon);
        assertEquals(96, trianglesArea(primitive), 1E-9);
    }

    /**
     * Tests the triangulation of a three-dimensional polygon, which shall keep the z coordinates.
     */
    @Test
    public void testPolygon3D() {
        final LinearRing exterior = GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(
                NDArrays.of(CommonCRS.WGS84.geographic3D(), 0,0,5, 2,0,5, 2,2,7, 0,2,7, 0,0,5)));
        final Polygon polygon = GeometryFactory.DEFAULT.createPolygon(exterior, null);
        final MeshPrimitive primitive = (MeshPrimitive) new GeometryProcessor().toPrimitive(polygon);
        assertEquals(MeshPrimitive.Type.TRIANGLES, primitive.getType());
        assertEquals(3, primitive.getPositions().getDimension());
        assertEquals(6, primitive.getPositions().getLength(), "Expected 2 triangles.");
        final double[] c = primitive.getPositions().toArrayDouble();
        for (int i = 0; i < c.length; i += 3) {
            assertEquals(c[i+1] == 0 ? 5 : 7, c[i+2], "z shall follow the polygon plane.");
        }
    }

    /**
     * Creates a ring in the two-dimensional test CRS.
     */
    private static LinearRing ring(final double... coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(
                GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(CRS2D, coordinates)));
    }

    /**
     * Returns the sum of the areas of the triangles of the given non-indexed mesh.
     */
    private static double trianglesArea(final MeshPrimitive primitive) {
        final double[] c = primitive.getPositions().toArrayDouble();
        double area = 0;
        for (int i = 0; i + 5 < c.length; i += 6) {
            area += Math.abs((c[i+2] - c[i]) * (c[i+5] - c[i+1]) - (c[i+4] - c[i]) * (c[i+3] - c[i+1])) / 2;
        }
        return area;
    }

    @Test
    public void testMultiLineString() {
        { //one line has 3 points, so we must obtain a MultiPrimitive
            final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
            final DataPoints points1 = GeometryFactory.DEFAULT.createDataPoints(positions1);
            final LineString line1 = GeometryFactory.DEFAULT.createLineString(points1);

            final Array positions2 = NDArrays.of(CRS2D, new double[]{3,4,5,6,7,8});
            final DataPoints points2 = GeometryFactory.DEFAULT.createDataPoints(positions2);
            final LineString line2 = GeometryFactory.DEFAULT.createLineString(points2);

            final GeometryCollection<LineString> mlines = GeometryFactory.DEFAULT.createMultiLineString(line1, line2);

            final Geometry result = new GeometryProcessor().toPrimitive(mlines);
            assertTrue(result instanceof MultiMeshPrimitive);
            final MultiMeshPrimitive mp = (MultiMeshPrimitive) result;
            assertEquals(2, mp.getNumGeometries());
            assertTrue(mp.getGeometryN(0) instanceof MeshPrimitive);
            assertTrue(mp.getGeometryN(1) instanceof MeshPrimitive);

            final MeshPrimitive primitive1 = (MeshPrimitive) mp.getGeometryN(0);
            assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive1.getType());

            final MeshPrimitive primitive2 = (MeshPrimitive) mp.getGeometryN(1);
            assertEquals(MeshPrimitive.Type.LINE_STRIP, primitive2.getType());
        }
        { //all linestrings are lines, we must obtain a Primitive.Lines
            final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
            final DataPoints points1 = GeometryFactory.DEFAULT.createDataPoints(positions1);
            final LineString line1 = GeometryFactory.DEFAULT.createLineString(points1);

            final Array positions2 = NDArrays.of(CRS2D, new double[]{3,4,5,6});
            final DataPoints points2 = GeometryFactory.DEFAULT.createDataPoints(positions2);
            final LineString line2 = GeometryFactory.DEFAULT.createLineString(points2);

            final GeometryCollection<LineString> mlines = GeometryFactory.DEFAULT.createMultiLineString(line1, line2);

            final Geometry result = new GeometryProcessor().toPrimitive(mlines);
            assertTrue(result instanceof MeshPrimitive.Lines);
            final MeshPrimitive.Lines mp = (MeshPrimitive.Lines) result;
            assertEquals(2, mp.getNumGeometries());
            LineString l1 = mp.getGeometryN(0);
            LineString l2 = mp.getGeometryN(1);
            assertArrayEquals(new double[]{0,1,2,3}, l1.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble(), 0.0);
            assertArrayEquals(new double[]{3,4,5,6}, l2.getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble(), 0.0);
        }
    }

    @Test
    public void testMultiPoint() {
        final Array positions1 = NDArrays.of(CRS2D, new double[]{0,1,2,3});
        final DataPoints points = GeometryFactory.DEFAULT.createDataPoints(positions1);
        final GeometryCollection<Point> mpoints = GeometryFactory.DEFAULT.createMultiPoint(points);

        final Geometry result = new GeometryProcessor().toPrimitive(mpoints);
        assertTrue(result instanceof MeshPrimitive);
        final MeshPrimitive primitive1 = (MeshPrimitive) result;
        assertEquals(MeshPrimitive.Type.POINTS, primitive1.getType());
    }
}
