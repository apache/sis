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
package org.apache.sis.internal.referencing;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Formulas}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class FormulasTest extends TestCase {
    /**
     * Verifies the {@link Formulas#JULIAN_YEAR_LENGTH} constant.
     */
    @Test
    public void testConstants() {
        assertEquals(StrictMath.round(365.25 * 24 * 60 * 60 * 1000), Formulas.JULIAN_YEAR_LENGTH);
    }

    /**
     * Tests {@link Formulas#isPoleToPole(double, double)}.
     */
    @Test
    public void testIsPoleToPole() {
        assertTrue (Formulas.isPoleToPole(-90, 90));
        assertFalse(Formulas.isPoleToPole(-89, 90));
        assertFalse(Formulas.isPoleToPole(-90, 89));
    }

    /**
     * Tests {@link Formulas#getAuthalicRadius(double, double)} using the parameters of <cite>GRS 1980</cite>
     * ellipsoid (EPSG:7019).
     *
     * <ul>
     *   <li>Semi-major axis length: 6378137 metres</li>
     *   <li>Inverse flattening: 298.257222101</li>
     * </ul>
     *
     * Expected result is the radius of <cite>GRS 1980 Authalic Sphere</cite> (EPSG:7048).
     */
    @Test
    public void testGetAuthalicRadius() {
        assertEquals(6371007, Formulas.getAuthalicRadius(6378137, 6356752), 0.5);
    }
}
