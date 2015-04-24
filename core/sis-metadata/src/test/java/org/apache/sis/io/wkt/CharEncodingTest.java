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
package org.apache.sis.io.wkt;

import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.test.mock.CoordinateSystemAxisMock;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link CharEncoding} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class CharEncodingTest extends TestCase {
    /**
     * Tests {@link CharEncoding#getAbbreviation(CoordinateSystem, CoordinateSystemAxis)}.
     */
    @Test
    public void testGetAbbreviation() {
        assertAbbreviationEquals("B", new Geographic("Geodetic latitude",    "φ"));
        assertAbbreviationEquals("U", new Geocentric("Geocentric latitude",  "φ′"));
        assertAbbreviationEquals("L", new Geographic("Geodetic longitude",   "λ"));
        assertAbbreviationEquals("V", new Geocentric("Geocentric longitude", "θ"));
    }

    /**
     * Asserts that the abbreviation of the given axis, after replacement of Greek letters,
     * is equals to the expected string.
     */
    private static void assertAbbreviationEquals(final String expected, final CoordinateSystemAxisMock axis) {
        assertEquals("abbreviation", expected, CharEncoding.DEFAULT.getAbbreviation(axis, axis));
    }

    /**
     * A single axis which is part of a geographic CRS.
     */
    @SuppressWarnings("serial")
    private static final class Geographic extends CoordinateSystemAxisMock implements EllipsoidalCS {
        Geographic(final String name, final String abbreviation) {
            super(name, abbreviation);
        }
    }

    /**
     * A single axis which is part of a geocentric CRS.
     */
    @SuppressWarnings("serial")
    private static final class Geocentric extends CoordinateSystemAxisMock implements SphericalCS {
        Geocentric(final String name, final String abbreviation) {
            super(name, abbreviation);
        }
    }
}
