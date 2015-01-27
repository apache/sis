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
package org.apache.sis.referencing.cs;

import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.referencing.cs.HardCodedCS.*;


/**
 * Validates the {@link HardCodedCS} definitions.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({
    DefaultCartesianCSTest.class,
    DefaultEllipsoidalCSTest.class
})
public final strictfp class HardCodedCSTest extends TestCase {
    /**
     * Validates constants.
     */
    @Test
    public void validate() {
        Validators.validate(PROJECTED);
        Validators.validate(GEOCENTRIC);
        Validators.validate(CARTESIAN_2D);
        Validators.validate(CARTESIAN_3D);
        Validators.validate(GRID);
        Validators.validate(DISPLAY);
        Validators.validate(GEODETIC_2D);
        Validators.validate(GEODETIC_3D);
        Validators.validate(ELLIPSOIDAL_HEIGHT);
        Validators.validate(GRAVITY_RELATED_HEIGHT);
        Validators.validate(DEPTH);
        Validators.validate(DAYS);
        Validators.validate(SECONDS);
        Validators.validate(MILLISECONDS);
        Validators.validate(SPHERICAL);
    }

    /**
     * Tests the dimensions of some hard-coded coordinate systems.
     */
    @Test
    public void testDimensions() {
        assertEquals("Cartesian 2D",   2, PROJECTED  .getDimension());
        assertEquals("Cartesian 3D",   3, GEOCENTRIC .getDimension());
        assertEquals("Ellipsoidal 2D", 2, GEODETIC_2D.getDimension());
        assertEquals("Ellipsoidal 3D", 3, GEODETIC_3D.getDimension());
        assertEquals("Vertical",       1, DEPTH      .getDimension());
        assertEquals("Temporal",       1, DAYS       .getDimension());
    }

    /**
     * Tests serialization of various objects.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(PROJECTED);
        assertSerializedEquals(GEOCENTRIC);
        assertSerializedEquals(GEODETIC_2D);
        assertSerializedEquals(GEODETIC_3D);
    }

    /**
     * Verifies that some definitions are already normalized.
     */
    @Test
    public void testNormalized() {
        AbstractCS cs;
        cs = GRID;               assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = GEOCENTRIC;         assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = CARTESIAN_2D;       assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = CARTESIAN_3D;       assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = PROJECTED;          assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = GEODETIC_2D;        assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = GEODETIC_3D;        assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = DAYS;               assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
        cs = ELLIPSOIDAL_HEIGHT; assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED));
    }
}
