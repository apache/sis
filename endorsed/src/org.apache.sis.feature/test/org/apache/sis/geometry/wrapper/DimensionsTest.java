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
package org.apache.sis.geometry.wrapper;

import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Dimensions}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DimensionsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DimensionsTest() {
    }

    /**
     * Tests {@link Dimensions#forCRS(CoordinateReferenceSystem)}.
     */
    @Test
    public void testForCRS() {
        assertSame(Dimensions.XY,   Dimensions.forCRS(HardCodedCRS.WGS84));
        assertSame(Dimensions.XYZ,  Dimensions.forCRS(HardCodedCRS.WGS84_3D));
        assertSame(Dimensions.XYZM, Dimensions.forCRS(HardCodedCRS.WGS84_4D));
        assertSame(Dimensions.XYZ,  Dimensions.forCRS(HardCodedCRS.GEOID_3D));
        assertSame(Dimensions.XYZM, Dimensions.forCRS(HardCodedCRS.GEOID_4D));
        assertSame(Dimensions.XYM,  Dimensions.forCRS(HardCodedCRS.WGS84_WITH_TIME));
    }

    /**
     * Tests {@link Dimensions#forZorM(boolean, boolean)}.
     */
    @Test
    public void testForZorM() {
        assertSame(Dimensions.XY,   Dimensions.forZorM(false, false));
        assertSame(Dimensions.XYZ,  Dimensions.forZorM(true,  false));
        assertSame(Dimensions.XYM,  Dimensions.forZorM(false, true));
        assertSame(Dimensions.XYZM, Dimensions.forZorM(true,  true));
    }

    /**
     * Tests {@link Dimensions#forCount(boolean, boolean)}.
     */
    @Test
    public void testForCount() {
        assertSame(Dimensions.XY,   Dimensions.forCount(2, false));
        assertSame(Dimensions.XYZ,  Dimensions.forCount(3, false));
        assertSame(Dimensions.XYM,  Dimensions.forCount(3, true));
        assertSame(Dimensions.XYZM, Dimensions.forCount(4, true));
        assertSame(Dimensions.XYZM, Dimensions.forCount(4, false));
        assertNotNull(assertThrows(IllegalArgumentException.class, () -> Dimensions.forCount(1, false)).getMessage());
    }

    /**
     * Tests {@link Dimensions#adjust adjust(…)}.
     *
     * @throws FactoryException if an error occurred while querying the authority factory or creating a <abbr>CRS</abbr>.
     */
    @Test
    public void testAdjust() throws FactoryException {
        assertSame(HardCodedCRS.WGS84,    Dimensions.XY .adjust(HardCodedCRS.WGS84,    null, null, null));
        assertSame(HardCodedCRS.WGS84,    Dimensions.XY .adjust(HardCodedCRS.GEOID_3D, null, null, null));
        assertSame(HardCodedCRS.WGS84,    Dimensions.XY .adjust(HardCodedCRS.GEOID_4D, null, null, null));
        assertSame(HardCodedCRS.GEOID_3D, Dimensions.XYZ.adjust(HardCodedCRS.GEOID_3D, null, null, null));

        CRS.equivalent(HardCodedCRS.WGS84,    Dimensions.XY .adjust(HardCodedCRS.WGS84_3D, null, null, null));
        CRS.equivalent(HardCodedCRS.WGS84_3D, Dimensions.XYZ.adjust(HardCodedCRS.WGS84,    null, null, null));
        CRS.equivalent(HardCodedCRS.GEOID_3D, Dimensions.XYZ.adjust(HardCodedCRS.GEOID_4D, null, null, null));
    }
}
