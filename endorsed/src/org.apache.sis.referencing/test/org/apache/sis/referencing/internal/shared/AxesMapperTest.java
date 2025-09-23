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
package org.apache.sis.referencing.internal.shared;

import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;


/**
 * Tests the {@link AxesMapper} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AxesMapperTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AxesMapperTest() {
    }

    /**
     * Tests with axes having same direction in same order.
     */
    @Test
    public void testSameDirections() {
        assertArrayEquals(new int[] {0, 1}, AxesMapper.indices(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.GEODETIC_2D));
    }

    /**
     * Tests with axes having same direction but different order.
     */
    @Test
    public void testAxisOrderChange() {
        assertArrayEquals(new int[] {1, 0}, AxesMapper.indices(
                HardCodedCS.GEODETIC_φλ,
                HardCodedCS.GEODETIC_2D));

        assertArrayEquals(new int[] {1, 0}, AxesMapper.indices(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.GEODETIC_φλ));
    }

    /**
     * Tests selection of vertical axis, including opposite direction.
     */
    @Test
    public void testVerticalAxis() {
        assertArrayEquals(new int[] {2}, AxesMapper.indices(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.ELLIPSOIDAL_HEIGHT));

        assertArrayEquals(new int[] {2}, AxesMapper.indices(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.DEPTH));
    }

    /**
     * Tests the search of axes that do not exist in source coordinate system.
     */
    @Test
    public void testNotFound() {
        assertNull(AxesMapper.indices(
                HardCodedCS.GEODETIC_3D,
                HardCodedCS.DAYS));

        assertNull(AxesMapper.indices(
                HardCodedCS.GEODETIC_2D,
                HardCodedCS.GEODETIC_3D));
    }

    /**
     * Tests with directions such as "South along 90° East".
     */
    @Test
    public void testDirectionsFromPole() {
        assertArrayEquals(new int[] {1,0}, AxesMapper.indices(
                HardCodedCS.GEODETIC_φλ,
                CommonCRS.WGS84.universal(90, 0).getCoordinateSystem()));
    }
}
