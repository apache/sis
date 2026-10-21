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
package org.apache.sis.geometries.operation.spatialanalysis2d;

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.curve.LinearRing;
import org.apache.sis.maths.Maths;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link StraightSkeleton}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class StraightSkeletonTest {

    /**
     * A rectangle gives two hips at each end joined by a ridge.
     * <pre>
     *   4 ■-------------------■
     *     |  ⟍             ⟋  |
     *   2 |    ■---------■    |
     *     |  ⟋             ⟍  |
     *   0 ■-------------------■
     *     0    2         8    10
     * </pre>
     */
    @Test
    public void testRectangle() {
        assertSkeleton(skeleton(new double[] {0,0, 10,0, 10,4, 0,4, 0,0}), 6, 5, 2, 2,2, 8,2);
    }

    /**
     * A square gives a pyramid, all hips meeting at the center.
     */
    @Test
    public void testSquare() {
        assertSkeleton(skeleton(new double[] {0,0, 4,0, 4,4, 0,4, 0,0}), 5, 4, 2, 2,2);
    }

    /**
     * A L shape, the reflex vertex creates a valley up to the junction of the two ridges.
     * <pre>
     *  10 ■---■
     *     |   |
     *   4 |   ■-----------■
     *     |               |
     *   0 ■---------------■
     *     0   4           10
     * </pre>
     */
    @Test
    public void testLShape() {
        assertSkeleton(skeleton(new double[] {0,0, 10,0, 10,4, 4,4, 4,10, 0,10, 0,0}), 9, 8, 2, 2,2, 8,2, 2,8);
    }

    /**
     * A square with an off-center hole, the widest part is on the right of the hole.
     * <pre>
     *  10 ■-------------------■
     *     |                   |
     *   6 |   ■-----■         |
     *     |   |     |         |
     *   3 |   ■-----■         |
     *   0 ■-------------------■
     *     0   2     5         10
     * </pre>
     */
    @Test
    public void testHole() {
        final StraightSkeleton skeleton = skeleton(new double[] {0,0, 10,0, 10,10, 0,10, 0,0},
                                                   new double[] {2,3, 5,3, 5,6, 2,6, 2,3});
        assertSkeleton(skeleton, -1, -1, 2.5);
        final double[] nodes = skeleton.getNodes();
        for (int i = 0; i < nodes.length; i += 3) {
            final double x = nodes[i], y = nodes[i+1];
            if (nodes[i+2] > 0) {
                assertTrue(x > 0 && x < 10 && y > 0 && y < 10, "Node outside the exterior ring.");
                assertFalse(x >= 2 && x <= 5 && y >= 3 && y <= 6, "Node inside the hole.");
            }
        }
    }

    /**
     * Verifies the number of nodes and arcs (ignored if negative), the maximum time, the nodes at the
     * maximum time given as (x,y) pairs, and that no two arcs cross each other.
     */
    private static void assertSkeleton(final StraightSkeleton skeleton, final int nbNodes, final int nbArcs,
                                       final double maxTime, final double... topNodes)
    {
        final double[] n = skeleton.getNodes();
        final int[] arcs = skeleton.getArcs();
        if (nbNodes >= 0) assertEquals(nbNodes, n.length / 3);
        if (nbArcs  >= 0) assertEquals(nbArcs, arcs.length / 2);
        assertEquals(maxTime, skeleton.getMaximumTime(), 1E-9);
next:   for (int k = 0; k < topNodes.length; k += 2) {
            for (int i = 0; i < n.length; i += 3) {
                if (Math.abs(n[i] - topNodes[k]) < 1E-9 && Math.abs(n[i+1] - topNodes[k+1]) < 1E-9) {
                    assertEquals(maxTime, n[i+2], 1E-9);
                    continue next;
                }
            }
            fail("Missing node at " + topNodes[k] + " " + topNodes[k+1]);
        }
        for (int i = 0; i < arcs.length; i += 2) {
            for (int j = i + 2; j < arcs.length; j += 2) {
                final int a = arcs[i]*3, b = arcs[i+1]*3, c = arcs[j]*3, d = arcs[j+1]*3;
                if (a == c || a == d || b == c || b == d) continue;
                final boolean cross = Maths.lineSide(n[c], n[c+1], n[d], n[d+1], n[a], n[a+1])
                                    * Maths.lineSide(n[c], n[c+1], n[d], n[d+1], n[b], n[b+1]) < 0
                                   && Maths.lineSide(n[a], n[a+1], n[b], n[b+1], n[c], n[c+1])
                                    * Maths.lineSide(n[a], n[a+1], n[b], n[b+1], n[d], n[d+1]) < 0;
                assertFalse(cross, "Crossing arcs.");
            }
        }
    }

    private static StraightSkeleton skeleton(final double[] exterior, final double[]... holes) {
        final List<LinearRing> interiors = new ArrayList<>();
        for (double[] hole : holes) {
            interiors.add(ring(hole));
        }
        return new StraightSkeleton(GeometryFactory.DEFAULT.createPolygon(ring(exterior), interiors));
    }

    private static LinearRing ring(final double[] coordinates) {
        return GeometryFactory.DEFAULT.createLinearRing(GeometryFactory.DEFAULT.createDataPoints(Geometries.getUndefinedCRS(2), coordinates));
    }
}
