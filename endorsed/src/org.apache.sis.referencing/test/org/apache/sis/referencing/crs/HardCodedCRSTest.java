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
package org.apache.sis.referencing.crs;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.ValidatorContainer;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.crs.HardCodedCRS.*;


/**
 * Validates the {@link HardCodedCRS} definitions.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class HardCodedCRSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public HardCodedCRSTest() {
    }

    /**
     * Validates constants.
     *
     * <p>Note: ISO specification does not allow ellipsoidal height, so we have to relax
     * the check for the {@code DefaultVerticalCRS.ELLIPSOIDAL_HEIGHT} constant.</p>
     */
    @Test
    public void validate() {
        final ValidatorContainer validators = new ValidatorContainer();
        validators.validate(WGS84);
        validators.validate(WGS84_3D);
        validators.validate(ELLIPSOIDAL_HEIGHT);
        validators.validate(GRAVITY_RELATED_HEIGHT);
        validators.validate(TIME);
        validators.validate(SPHERICAL);
        validators.validate(GEOCENTRIC);
        validators.validate(CARTESIAN_2D);
        validators.validate(CARTESIAN_3D);
    }

    /**
     * Tests dimension of constants.
     */
    @Test
    public void testDimensions() {
        assertEquals(1, TIME                .getCoordinateSystem().getDimension());
        assertEquals(1, DEPTH               .getCoordinateSystem().getDimension());
        assertEquals(2, WGS84               .getCoordinateSystem().getDimension());
        assertEquals(2, WGS84_LATITUDE_FIRST.getCoordinateSystem().getDimension());
        assertEquals(3, WGS84_3D            .getCoordinateSystem().getDimension());
        assertEquals(2, CARTESIAN_2D        .getCoordinateSystem().getDimension());
        assertEquals(3, CARTESIAN_3D        .getCoordinateSystem().getDimension());
        assertEquals(3, GEOCENTRIC          .getCoordinateSystem().getDimension());
        assertEquals(3, SPHERICAL           .getCoordinateSystem().getDimension());
        assertEquals(4, GEOID_4D            .getCoordinateSystem().getDimension());
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(WGS84);
        assertSerializedEquals(WGS84_3D);
        assertSerializedEquals(GEOID_4D);
    }
}
