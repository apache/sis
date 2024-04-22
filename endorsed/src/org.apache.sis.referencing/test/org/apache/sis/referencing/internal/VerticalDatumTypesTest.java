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
package org.apache.sis.referencing.internal;

import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.ArraysExt;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link VerticalDatumTypes} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class VerticalDatumTypesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public VerticalDatumTypesTest() {
    }

    /**
     * Verifies name constraint with values defined in {@link org.apache.sis.referencing.CommonCRS.Vertical}.
     * Some enumeration values must have the same names as the constants defined in {@link VerticalDatumTypes},
     * because the realization method is obtained by a call to {@link VerticalDatumType#valueOf(String)}.
     */
    @Test
    public void verifyNameConstraint() {
        assertEquals(VerticalDatumTypes.ELLIPSOIDAL, CommonCRS.Vertical.ELLIPSOIDAL.name());
        assertEquals(VerticalDatumTypes.BAROMETRIC,  CommonCRS.Vertical.BAROMETRIC.name());
    }

    /**
     * Tests the {@link VerticalDatumTypes#fromLegacy(int)} method.
     */
    @Test
    public void testFromLegacy() {
        assertEquals(VerticalDatumTypes.ellipsoidal(), VerticalDatumTypes.fromLegacy(2002));
        assertEquals(VerticalDatumType .GEOIDAL,       VerticalDatumTypes.fromLegacy(2005));
        assertEquals(VerticalDatumType .DEPTH,         VerticalDatumTypes.fromLegacy(2006));
    }

    /**
     * Tests the {@link VerticalDatumTypes#toLegacy(VerticalDatumType)} method.
     */
    @Test
    public void testToLegacy() {
        assertEquals(2002, VerticalDatumTypes.toLegacy(VerticalDatumType.valueOf("ELLIPSOIDAL")));
        assertEquals(2005, VerticalDatumTypes.toLegacy(VerticalDatumType.GEOIDAL));
        assertEquals(2006, VerticalDatumTypes.toLegacy(VerticalDatumType.DEPTH));
    }

    /**
     * Verifies the list of realization methods.
     */
    @Test
    public void verifyCodeList() {
        final VerticalDatumType expected = VerticalDatumTypes.ellipsoidal();    // Must be first.
        final VerticalDatumType[] types = VerticalDatumType.values();
        assertEquals(VerticalDatumType.OTHER_SURFACE, types[0]);
        assertTrue(ArraysExt.contains(types, expected));
    }
}
