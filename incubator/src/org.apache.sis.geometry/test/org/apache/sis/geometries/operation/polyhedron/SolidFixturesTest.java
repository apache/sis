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
package org.apache.sis.geometries.operation.polyhedron;

import org.apache.sis.geometries.DataPointsType;
import org.apache.sis.geometries.GeometryCollection;
import org.apache.sis.geometries.operation.triangulate.EarClipping;
import org.apache.sis.geometries.solid.Polyhedron;
import org.apache.sis.geometries.surface.Polygon;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Verifies the volumes and the orientations of the {@link SolidFixtures}.
 * The orientation of a shell is verified by the sign of the volume it encloses,
 * positive if its faces are oriented outward of the volume.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SolidFixturesTest {

    private static final double EPS = 1E-9;

    @Test
    public void testBox() {
        final Polyhedron box = SolidFixtures.box(0, 0, 0, 2, 3, 4);
        assertEquals(SolidFixtures.CRS, box.getCoordinateReferenceSystem());
        assertVolume(24, box);
        assertTrue(box.getInteriorShells().isEmpty());
    }

    /**
     * The faces of the void are oriented toward the inside of the void, so the void encloses a negative volume.
     */
    @Test
    public void testHollowCube() {
        final Polyhedron hollow = SolidFixtures.hollowCube(4, 1);
        assertVolume(64 - 8, hollow);
        assertEquals(1, hollow.getInteriorShells().size());
        assertEquals(-8, signedVolume(hollow.getInteriorShells().get(0)), EPS);
    }

    @Test
    public void testRotatedBox() {
        final Polyhedron box = SolidFixtures.rotatedBox();
        assertVolume(6, box);
        final double x = box.getExteriorShell().getGeometryN(0).getExteriorRing().getDataPoints().getPosition(1).get(0);
        assertNotEquals(Math.rint(x), x, "The coordinates shall not be integers.");
    }

    @Test
    public void testIcosahedron() {
        final double edge = 4 / Math.sqrt(10 + 2 * Math.sqrt(5));
        assertVolume(5 * (3 + Math.sqrt(5)) * edge * edge * edge / 12, SolidFixtures.icosahedron());
    }

    @Test
    public void testPrismWithHole() {
        final Polyhedron prism = SolidFixtures.prismWithHole();
        assertEquals(SolidFixtures.CRS, prism.getCoordinateReferenceSystem());
        assertVolume((16 - 4) * 2, prism);
    }

    /**
     * The pairs of cubes have the expected volumes, and touch where documented.
     */
    @Test
    public void testPairs() {
        Polyhedron[] pair = SolidFixtures.sharingFace();
        assertVolume(1, pair[0]);
        assertVolume(1, pair[1]);
        assertEquals(pair[0].getEnvelope().getMaximum(0), pair[1].getEnvelope().getMinimum(0));

        pair = SolidFixtures.sharingEdge();
        assertVolume(1, pair[1]);
        assertEquals(pair[0].getEnvelope().getMaximum(1), pair[1].getEnvelope().getMinimum(1));

        pair = SolidFixtures.sharingVertex();
        assertVolume(1, pair[1]);
        assertEquals(pair[0].getEnvelope().getMaximum(2), pair[1].getEnvelope().getMinimum(2));

        pair = SolidFixtures.overlapping();
        assertVolume(8, pair[0]);
        assertVolume(8, pair[1]);

        pair = SolidFixtures.offsetAlongX();
        assertVolume(8, pair[1]);
        assertEquals(pair[0].getEnvelope().getMinimum(1), pair[1].getEnvelope().getMinimum(1));
    }

    /**
     * Verifies the volume of the given polyhedron, and that its exterior shell is oriented outward.
     */
    private static void assertVolume(final double expected, final Polyhedron solid) {
        assertEquals(expected, solid.getVolume().getValue().doubleValue(), EPS, "Volume.");
        assertTrue(signedVolume(solid.getExteriorShell()) > 0, "The exterior shell shall be oriented outward.");
    }

    /**
     * Returns the volume enclosed by the given shell, positive if its faces are oriented outward of that volume.
     * Each face is split in triangles, and the volumes of the tetrahedra joining the origin to the triangles
     * are added.
     */
    private static double signedVolume(final GeometryCollection<Polygon> shell) {
        final EarClipping triangulator = new EarClipping();
        double volume = 0;
        for (int i = 0, n = shell.getNumGeometries(); i < n; i++) {
            final EarClipping.Triangulation face = triangulator.triangulateIndexed(shell.getGeometryN(i));
            final double[] c = face.vertices().getAttributeArray(DataPointsType.ATT_POSITION).toArrayDouble();
            final int[] triangles = face.triangles();
            for (int k = 0; k < triangles.length; k += 3) {
                final int a = triangles[k] * 3, b = triangles[k+1] * 3, d = triangles[k+2] * 3;
                volume += c[a]   * (c[b+1] * c[d+2] - c[b+2] * c[d+1])
                        + c[a+1] * (c[b+2] * c[d]   - c[b]   * c[d+2])
                        + c[a+2] * (c[b]   * c[d+1] - c[b+1] * c[d]);
            }
        }
        return volume / 6;
    }
}
