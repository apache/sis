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
package org.apache.sis.geometry.wrapper.jts;

import java.util.function.DoubleFunction;
import static java.lang.Double.NaN;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.CoordinateSequence;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link PackedCoordinateSequence}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PackedCoordinateSequenceTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PackedCoordinateSequenceTest() {
    }

    /**
     * Tests the creation from coordinates without <var>z</var> or <var>M</var> values.
     * The absence of values should be detected even when using a specialized class such
     * as {@link CoordinateXYZM}.
     */
    @Test
    public void testXY() {
        createAndCompare(2, NaN, NaN, (x) -> new Coordinate    (x, x+1, NaN));
        createAndCompare(2, NaN, NaN, (x) -> new CoordinateXY  (x, x+1));
        createAndCompare(2, NaN, NaN, (x) -> new CoordinateXYM (x, x+1, NaN));
        createAndCompare(2, NaN, NaN, (x) -> new CoordinateXYZM(x, x+1, NaN, NaN));
    }

    /**
     * Tests the creation from coordinates without <var>M</var> values.
     */
    @Test
    public void testXYZ() {
        createAndCompare(3, 3, NaN, (x) -> new Coordinate    (x, x+1, x+3));
        createAndCompare(3, 3, NaN, (x) -> new CoordinateXYZM(x, x+1, x+3, NaN));
    }

    /**
     * Tests the creation from coordinates without <var>z</var> values.
     */
    @Test
    public void testXYM() {
        createAndCompare(3, NaN, -5, (x) -> new CoordinateXYM (x, x+1,      x-5));
        createAndCompare(3, NaN, -5, (x) -> new CoordinateXYZM(x, x+1, NaN, x-5));
    }

    /**
     * Tests the creation from coordinates with all dimensions.
     */
    @Test
    public void testXYZM() {
        createAndCompare(4, 7, -3, (x) -> new CoordinateXYZM(x, x+1, x+7, x-3));
    }

    /**
     * Implementation of {@link #test2D()} creating points using the given function.
     * The given function shall create point with <var>y</var> = <var>x</var>+1.
     */
    private void createAndCompare(final int dim, final double z, final double m,
                                  final DoubleFunction<Coordinate> creator)
    {
        final var points = new Coordinate[] {
            creator.apply( 4),
            creator.apply(-3),
            creator.apply( 7)
        };
        boolean doublePrecision = true;
        do {
            final CoordinateSequence cs = new PackedCoordinateSequenceFactory(doublePrecision).create(points);
            assertEquals(dim,  cs.getDimension());
            assertEquals( 3,   cs.size());
            assertEquals( 4,   cs.getX(0));
            assertEquals( 4+1, cs.getY(0));
            assertEquals( 4+z, cs.getZ(0));
            assertEquals( 4+m, cs.getM(0));
            assertEquals(-3,   cs.getX(1));
            assertEquals(-3+1, cs.getY(1));
            assertEquals(-3+z, cs.getZ(1));
            assertEquals(-3+m, cs.getM(1));
            assertEquals( 7,   cs.getX(2));
            assertEquals( 7+1, cs.getY(2));
            assertEquals( 7+z, cs.getZ(2));
            assertEquals( 7+m, cs.getM(2));
        } while ((doublePrecision = !doublePrecision) == false);
    }
}
