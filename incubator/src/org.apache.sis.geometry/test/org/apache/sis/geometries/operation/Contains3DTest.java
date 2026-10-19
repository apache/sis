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

import java.util.Arrays;
import java.util.List;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.solid.polyhedron.Icosahedron;
import org.apache.sis.geometries.surface.Polygon;
import org.apache.sis.maths.Array;
import org.apache.sis.maths.Maths;
import org.apache.sis.maths.NDArrays;
import org.apache.sis.maths.Vectors;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests the three-dimensional and planar tests of {@link Contains}: winding number of a surface,
 * polyhedron containing a point, and ring containing a point in any plane.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Contains3DTest {

    private static final double EPS = 1E-9;

    private static final CoordinateReferenceSystem CRS3D = Geometries.getUndefinedCRS(3);

    /**
     * Indices of the corners of the twelve triangles of a cube, oriented outward. The corner of index
     * <var>x</var> + 2<var>y</var> + 4<var>z</var> is at the minimum (0) or maximum (1) of each axis.
     */
    private static final int[] CUBE = {
        0,2,3, 0,3,1,       // z = 0
        4,5,7, 4,7,6,       // z = 1
        0,1,5, 0,5,4,       // y = 0
        2,6,7, 2,7,3,       // y = 1
        0,4,6, 0,6,2,       // x = 0
        1,3,7, 1,7,5        // x = 1
    };

    /**
     * Returns the coordinates of the eight corners of the cube between the given minimum and maximum on all axes.
     */
    private static double[] corners(final double min, final double max) {
        final double[] xyz = new double[8 * 3];
        for (int i = 0; i < 8; i++) {
            xyz[i*3  ] = ((i & 1) == 0) ? min : max;
            xyz[i*3+1] = ((i & 2) == 0) ? min : max;
            xyz[i*3+2] = ((i & 4) == 0) ? min : max;
        }
        return xyz;
    }

    /**
     * Returns the triangles of the cube, reversed so that they are oriented inward, with indices shifted by the given offset.
     */
    private static int[] inward(final int offset) {
        final int[] triangles = new int[CUBE.length];
        for (int i = 0; i < CUBE.length; i += 3) {
            triangles[i  ] = CUBE[i  ] + offset;
            triangles[i+1] = CUBE[i+2] + offset;
            triangles[i+2] = CUBE[i+1] + offset;
        }
        return triangles;
    }

    /**
     * The winding number is 1 inside a closed surface oriented outward, −1 if oriented inward, 0 outside,
     * and ½ on a face.
     */
    @Test
    public void testWindingNumber() {
        final double[] xyz = corners(0, 1);
        assertEquals( 1,   Contains.windingNumber(xyz, CUBE,      new double[] {0.5, 0.5, 0.5}), EPS);
        assertEquals( 1,   Contains.windingNumber(xyz, CUBE,      new double[] {0.9, 0.1, 0.3}), EPS);
        assertEquals( 0,   Contains.windingNumber(xyz, CUBE,      new double[] {2.0, 0.5, 0.5}), EPS);
        assertEquals( 0,   Contains.windingNumber(xyz, CUBE,      new double[] {-3,  7,   0.2}), EPS);
        assertEquals(-1,   Contains.windingNumber(xyz, inward(0), new double[] {0.5, 0.5, 0.5}), EPS);
        assertEquals( 0.5, Contains.windingNumber(xyz, CUBE,      new double[] {0.3, 0.6, 0.0}), EPS, "On a face.");
    }

    /**
     * The winding numbers of an outer surface and of the inward surface of its void add,
     * giving 0 inside the void.
     */
    @Test
    public void testWindingNumberWithVoid() {
        final double[] outer = corners(0, 4), inner = corners(1, 3);
        final double[] xyz = Arrays.copyOf(outer, outer.length + inner.length);
        System.arraycopy(inner, 0, xyz, outer.length, inner.length);
        final int[] triangles = Arrays.copyOf(CUBE, CUBE.length * 2);
        System.arraycopy(inward(8), 0, triangles, CUBE.length, CUBE.length);
        assertEquals(1, Contains.windingNumber(xyz, triangles, new double[] {0.5, 0.5, 0.5}), EPS, "In the solid.");
        assertEquals(0, Contains.windingNumber(xyz, triangles, new double[] {2,   2,   2  }), EPS, "In the void.");
        assertEquals(0, Contains.windingNumber(xyz, triangles, new double[] {5,   2,   2  }), EPS, "Outside.");
    }

    /**
     * A polyhedron contains the points inside its exterior shell and outside its interior shells.
     * The interior shell is oriented as the exterior one, which shall not matter.
     */
    @Test
    public void testPolyhedron() {
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polyhedron cube = f.createPolyhedron(cube(0, 4), List.of());
        assertTrue (Contains.contains(cube, f.createPoint(CRS3D, 2, 2, 2)));
        assertTrue (Contains.contains(cube, f.createPoint(CRS3D, 0.1, 3.9, 0.2)));
        assertFalse(Contains.contains(cube, f.createPoint(CRS3D, 5, 2, 2)));

        final Polyhedron hollow = f.createPolyhedron(cube(0, 4), List.of(cube(1, 3)));
        assertTrue (Contains.contains(hollow, f.createPoint(CRS3D, 0.5, 2, 2)), "In the solid.");
        assertFalse(Contains.contains(hollow, f.createPoint(CRS3D, 2, 2, 2)),   "In the void.");
        assertFalse(Contains.contains(hollow, f.createPoint(CRS3D, 5, 2, 2)),   "Outside.");

        final Polyhedron icosahedron = new Icosahedron();
        assertTrue (Contains.contains(icosahedron, f.createPoint(icosahedron.getCoordinateReferenceSystem(), 0.1, -0.2, 0.3)));
        assertFalse(Contains.contains(icosahedron, f.createPoint(icosahedron.getCoordinateReferenceSystem(), 0.9, 0.9, 0)));

        assertThrows(OperationException.class, () -> Contains.contains(cube, f.createPoint(CommonCRS.WGS84.geographic3D(), 2, 2, 2)));
    }

    /**
     * A ring in a vertical plane contains a point when tested in the plane of its normal.
     */
    @Test
    public void testRingInPlane() {
        final Array ring = NDArrays.of(3, new double[] {0,2,0, 4,2,0, 4,2,4, 0,2,4, 0,2,0});
        final int[] axes = Maths.projectionAxes(Maths.calculatePolygonNormal(ring));
        assertTrue (Contains.contains(ring, Vectors.createDouble(3).set(new double[] {1, 2, 3}), axes));
        assertFalse(Contains.contains(ring, Vectors.createDouble(3).set(new double[] {5, 2, 3}), axes));
        assertFalse(Contains.contains(ring, Vectors.createDouble(3).set(new double[] {1, 2, 5}), axes));
    }

    /**
     * Returns the six faces of the cube between the given minimum and maximum on all axes, oriented outward.
     */
    private static GeometryCollection<Polygon> cube(final double min, final double max) {
        final double[] xyz = corners(min, max);
        final GeometryFactory f = GeometryFactory.DEFAULT;
        final Polygon[] faces = new Polygon[6];
        for (int i = 0; i < 6; i++) {
            //the two triangles of a face are (a,b,c) and (a,c,d), so the ring of the face is a, b, c, d.
            final int a = CUBE[i*6], b = CUBE[i*6+1], c = CUBE[i*6+2], d = CUBE[i*6+5];
            final double[] coordinates = new double[5 * 3];
            int k = 0;
            for (final int corner : new int[] {a, b, c, d, a}) {
                System.arraycopy(xyz, corner * 3, coordinates, k, 3);
                k += 3;
            }
            faces[i] = f.createPolygon(ring(coordinates), List.of());
        }
        return f.createMultiPolygon(CRS3D, faces);
    }

    private static LinearRing ring(final double... coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(NDArrays.of(CRS3D, coordinates)));
    }
}
