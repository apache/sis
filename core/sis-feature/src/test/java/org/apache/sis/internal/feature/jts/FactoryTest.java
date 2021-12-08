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
package org.apache.sis.internal.feature.jts;

import org.apache.sis.internal.feature.GeometriesTestCase;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.MultiLineString;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Factory} implementation for JTS geometries.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.0
 * @module
 */
public final strictfp class FactoryTest extends GeometriesTestCase {
    /**
     * Creates a new test case.
     */
    public FactoryTest() {
        super(Factory.INSTANCE);
    }

    /**
     * Tests {@link Factory#createPolyline(boolean, int, Vector...)}.
     */
    @Test
    @Override
    public void testCreatePolyline() {
        super.testCreatePolyline();
        final MultiLineString mp = (MultiLineString) geometry;
        assertEquals("numGeometries", 2, mp.getNumGeometries());
        verifyTwoFirstGeometries(mp);
    }

    /**
     * Verifies the coordinates of the two first geometries of the given multi line string.
     * If there is more than 2 geometries, it is caller responsibility to verify the other ones.
     */
    private static void verifyTwoFirstGeometries(final MultiLineString mp) {
        assertArrayEquals(new CoordinateXY[] {
                new CoordinateXY(4, 5),
                new CoordinateXY(7, 9),
                new CoordinateXY(9, 3),
                new CoordinateXY(4, 5)}, mp.getGeometryN(0).getCoordinates());

        assertArrayEquals(new CoordinateXY[] {
                new CoordinateXY(-3, -2),
                new CoordinateXY(-2, -5),
                new CoordinateXY(-1, -6)}, mp.getGeometryN(1).getCoordinates());
    }

    /**
     * Tests {@link Factory#mergePolylines(Iterator)} (or actually tests its strategy).
     */
    @Test
    @Override
    public void testMergePolylines() {
        super.testMergePolylines();
        final MultiLineString mp = (MultiLineString) geometry;
        assertEquals("numGeometries", 3, mp.getNumGeometries());
        verifyTwoFirstGeometries(mp);
        assertArrayEquals(new CoordinateXY[] {
                new CoordinateXY(13, 11),
                new CoordinateXY(14, 12),
                new CoordinateXY(15, 11),
                new CoordinateXY(13, 10)}, mp.getGeometryN(2).getCoordinates());
    }
}
