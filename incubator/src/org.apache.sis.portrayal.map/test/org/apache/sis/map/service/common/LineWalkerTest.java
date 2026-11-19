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
package org.apache.sis.map.service.common;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link LineWalker}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LineWalkerTest {
    /**
     * Tolerance on coordinates and angles.
     */
    private static final double EPS = 1E-12;

    /**
     * Creates a new test case.
     */
    public LineWalkerTest() {
    }

    /**
     * Returns a walker along an L-shaped line: 10 pixels to the right, then 10 pixels downward,
     * with a duplicated point and a point with non-finite coordinates.
     */
    private static LineWalker corner() {
        final GeometryFactory factory = GeometryFactory.DEFAULT;
        return new LineWalker(factory.createLineString(factory.createDataPoints(
                CommonCRS.WGS84.normalizedGeographic(), 0, 0, 10, 0, 10, 0, Double.NaN, 5, 10, 10)));
    }

    /**
     * Asserts that the given placement has the expected position and rotation.
     */
    private static void assertPlacement(final double x, final double y, final double rotation, final GraphicPlacement actual) {
        assertEquals(x,        actual.x(),        EPS, "x");
        assertEquals(y,        actual.y(),        EPS, "y");
        assertEquals(rotation, actual.rotation(), EPS, "rotation");
    }

    /**
     * Tests the positions along a line, with clamping outside the line.
     */
    @Test
    public void testPosition() {
        final LineWalker walker = corner();
        assertFalse(walker.isEmpty());
        assertEquals(20, walker.length(), EPS);
        assertPlacement( 4,  0, 0,           walker.position(4));
        assertPlacement(10,  3, Math.PI / 2, walker.position(13));
        assertPlacement( 0,  0, 0,           walker.position(-5));
        assertPlacement(10, 10, Math.PI / 2, walker.position(50));
    }

    /**
     * Tests the positions at regular distances.
     */
    @Test
    public void testWalk() {
        final List<GraphicPlacement> positions = corner().walk(-3, 8);
        assertEquals(2, positions.size(), "Positions at −3 and 21 are outside the line.");
        assertPlacement( 5, 0, 0,           positions.get(0));
        assertPlacement(10, 3, Math.PI / 2, positions.get(1));
        assertEquals(3, corner().walk(4, 8).size(), "Positions at 4, 12 and 20.");
        assertThrows(IllegalArgumentException.class, () -> corner().walk(0, 0));
    }

    /**
     * Tests the reversed line and the upright directions.
     */
    @Test
    public void testReverseAndUpright() {
        final LineWalker walker   = corner();
        final LineWalker reversed = walker.reverse();
        assertEquals(walker.length(), reversed.length(), EPS);
        assertPlacement(10, 7, -Math.PI / 2, reversed.position(3));

        assertFalse(walker.isUpsideDown(0, 8),  "Toward the right.");
        assertTrue (walker.isUpsideDown(12, 18), "Straight downward.");
        assertFalse(reversed.isUpsideDown(2, 8), "Straight upward.");
        assertTrue (reversed.isUpsideDown(12, 18), "Toward the left.");
        assertEquals(-Math.PI / 2, walker.uprightDirection(12, 18), EPS, "Downward text shall read upward.");
        assertEquals(0, reversed.uprightDirection(12, 18), EPS, "Leftward text shall read rightward.");
        assertEquals(Math.PI / 4, walker.uprightDirection(5, 15), EPS);
    }

    /**
     * Tests the walkers along the sub-paths of a shape.
     */
    @Test
    public void testShape() {
        final Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(5, 0);
        path.moveTo(20, 20);            // Single point after removal of duplicates: non-empty but without length.
        path.lineTo(20, 20);
        path.append(new Rectangle2D.Double(30, 30, 10, 5), false);
        final List<LineWalker> walkers = LineWalker.of(path);
        assertEquals(3, walkers.size());
        assertEquals(5,  walkers.get(0).length(), EPS);
        assertEquals(0,  walkers.get(1).length(), EPS);
        assertEquals(30, walkers.get(2).length(), EPS, "Closed rectangle.");
        assertTrue(walkers.get(1).walk(0, 1).isEmpty());
        assertPlacement(20, 20, 0, walkers.get(1).position(3));
        assertEquals(31, walkers.get(2).walk(0, 1).size());
    }
}
