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
package org.apache.sis.geometries.operation.triangulate;

import java.util.ArrayList;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.DataPoints;
import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.operation.OperationException;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vectors;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link EarClipping}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class EarClippingTest {

    private static final CoordinateReferenceSystem CRS2D = CommonCRS.WGS84.normalizedGeographic();

    private static final CoordinateReferenceSystem CRS3D = Geometries.getUndefinedCRS(3);

    /**
     * Each face of a box, with its exterior ring in both directions, gives two triangles which turn in the
     * direction of the ring. The faces perpendicular to every axis are tested, in particular those of normal
     * along y which were projected on a plane reversing their orientation.
     */
    @Test
    public void testFaceOrientation() {
        final double x0 = 0, y0 = 0, z0 = 0, x1 = 2, y1 = 3, z1 = 4;
        final double[][] faces = {
            {x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, x0,y0,z0},
            {x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1, x1,y0,z0},
            {x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1, x0,y0,z0},
            {x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0},
            {x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0, x0,y0,z0},
            {x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0,y0,z1}
        };
        final double[] areas = {12, 12, 8, 8, 6, 6};
        for (int i = 0; i < faces.length; i++) {
            assertOriented(polygon(CRS3D, faces[i]), areas[i], 2);
            assertOriented(polygon(CRS3D, reverse(faces[i], 3)), areas[i], 2);
        }
    }

    /**
     * A polygon with a hole gives triangles which turn in the direction of the exterior ring,
     * in a vertical plane of normal along y and in an oblique plane, with the ring in both directions.
     */
    @Test
    public void testHoleOrientation() {
        final double[] exterior = {0,0, 6,0, 6,4, 0,4, 0,0};
        final double[] hole     = {2,1, 2,3, 4,3, 4,1, 2,1};
        final double area = 24 - 4;
        //the plane y = 1, and an oblique plane made of two orthonormal vectors.
        final double[][][] planes = {
            {{0, 1, 0}, {1, 0, 0}, {0, 0, 1}},
            {{5, -2, 3}, {1/3., 2/3., 2/3.}, {2/3., 1/3., -2/3.}}
        };
        for (final double[][] plane : planes) {
            for (final boolean reversed : new boolean[] {false, true}) {
                final double[] e = toPlane(reversed ? reverse(exterior, 2) : exterior, plane);
                final double[] h = toPlane(reversed ? reverse(hole, 2) : hole, plane);
                assertOriented(polygon(CRS3D, e, h), area, 8);
            }
        }
    }

    /**
     * In two dimensions, the triangles turn in the direction of the exterior ring, whether it is counterclockwise
     * or clockwise. The polygon is concave, and has aligned positions.
     */
    @Test
    public void testOrientation2D() {
        final double[] shape = {0,0, 2,0, 4,0, 4,1, 1,1, 1,4, 0,4, 0,0};
        for (final boolean reversed : new boolean[] {false, true}) {
            final Polygon polygon = polygon(CRS2D, reversed ? reverse(shape, 2) : shape);
            final EarClipping.Triangulation triangulation = new EarClipping().triangulateIndexed(polygon);
            final DataPoints vertices = triangulation.vertices();
            final int[] triangles = triangulation.triangles();
            double area = 0;
            for (int i = 0; i < triangles.length; i += 3) {
                final double side = Maths.lineSide(vertices.getPosition(triangles[i]),
                                                   vertices.getPosition(triangles[i+1]),
                                                   vertices.getPosition(triangles[i+2]));
                assertTrue(reversed ? side < 0 : side > 0, "The triangle shall turn in the direction of the ring.");
                area += Math.abs(side) / 2;
            }
            assertEquals(7, area, 1E-12);
        }
    }

    /**
     * A ring which intersects itself cannot be triangulated, which shall be reported instead of returning
     * the triangles found before the failure.
     */
    @Test
    public void testSelfIntersectingRing() {
        final Polygon bowTie = polygon(CRS2D, new double[] {0,0, 2,2, 2,0, 0,2, 0,0});
        assertThrows(OperationException.class, () -> new EarClipping().triangulateIndexed(bowTie));
    }

    /**
     * Triangulates the given polygon and verifies that each triangle turns in the direction of the exterior ring,
     * which is that its normal has the direction of the normal of the ring. Also verifies the area and the number
     * of triangles.
     */
    private static void assertOriented(final Polygon polygon, final double area, final int count) {
        final double[] normal = Maths.calculatePolygonNormal(
                polygon.getExteriorRing().getDataPoints().getAttributeArray(DataPointsType.ATT_POSITION));
        final EarClipping.Triangulation triangulation = new EarClipping().triangulateIndexed(polygon);
        final DataPoints vertices = triangulation.vertices();
        final int[] triangles = triangulation.triangles();
        assertEquals(count * 3, triangles.length, "Number of triangles.");
        double sum = 0;
        for (int i = 0; i < triangles.length; i += 3) {
            final double[] a = vertices.getPosition(triangles[i  ]).toArrayDouble();
            final double[] b = vertices.getPosition(triangles[i+1]).toArrayDouble();
            final double[] c = vertices.getPosition(triangles[i+2]).toArrayDouble();
            final double[] n = Vectors.cross(Vectors.subtract(b, a), Vectors.subtract(c, a));
            assertTrue(Vectors.dot(n, normal) > 0, "The triangle shall turn in the direction of the exterior ring.");
            sum += Vectors.length(n) / 2;
        }
        assertEquals(area, sum, 1E-9);
    }

    /**
     * Creates a polygon from the coordinates of its exterior ring followed by those of its holes.
     */
    private static Polygon polygon(final CoordinateReferenceSystem crs, final double[] exterior, final double[]... holes) {
        final List<LinearRing> interiors = new ArrayList<>();
        for (final double[] hole : holes) {
            interiors.add(ring(crs, hole));
        }
        return GeometryFactory.DEFAULT.createPolygon(ring(crs, exterior), interiors);
    }

    private static LinearRing ring(final CoordinateReferenceSystem crs, final double... coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(crs, coordinates)));
    }

    /**
     * Returns the positions in reverse order, each position having the given number of coordinates.
     */
    private static double[] reverse(final double[] coordinates, final int dimension) {
        final double[] reversed = new double[coordinates.length];
        for (int i = 0; i < coordinates.length; i += dimension) {
            System.arraycopy(coordinates, i, reversed, coordinates.length - dimension - i, dimension);
        }
        return reversed;
    }

    /**
     * Converts (u,v) coordinates to three-dimensional coordinates in the plane made of an origin and two vectors.
     */
    private static double[] toPlane(final double[] uv, final double[][] plane) {
        final double[] origin = plane[0], u = plane[1], v = plane[2];
        final double[] xyz = new double[uv.length / 2 * 3];
        for (int i = 0, j = 0; i < uv.length; i += 2) {
            for (int k = 0; k < 3; k++) {
                xyz[j++] = origin[k] + uv[i] * u[k] + uv[i+1] * v[k];
            }
        }
        return xyz;
    }
}
