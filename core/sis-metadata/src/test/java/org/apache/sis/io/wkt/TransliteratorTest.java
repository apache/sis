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

import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.apache.sis.internal.metadata.AxisNames;

// Test imports
import org.apache.sis.test.mock.CoordinateSystemAxisMock;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Transliterator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class TransliteratorTest extends TestCase {
    /**
     * Verify the value of the {@link Transliterator#SPACES} constant.
     */
    @Test
    public void testSpacesConstant() {
        int code = 0;
        for (char c=0; c<32; c++) {
            if (Character.isWhitespace(c)) {
                code |= (1 << c);
            }
        }
        assertEquals(Transliterator.SPACES, code);
    }

    /**
     * Tests {@link Transliterator#filter(String)}.
     */
    @Test
    @DependsOnMethod("testSpacesConstant")
    public void testFilter() {
        final Transliterator t = Transliterator.DEFAULT;
        assertEquals("Nouvelle triangulation francaise", t.filter("Nouvelle\r\ntriangulation\nfrançaise"));
        assertEquals("ABC D E", t.filter("AB\bC\rD\tE"));
    }

    /**
     * Tests {@link Transliterator#toLongAxisName(String, AxisDirection, String)}.
     */
    @Test
    public void testToLongAxisName() {
        final Transliterator t = Transliterator.DEFAULT;
        assertEquals("Geodetic latitude",   t.toLongAxisName("ellipsoidal", AxisDirection.NORTH, "lat"));
        assertEquals("Geodetic longitude",  t.toLongAxisName("ellipsoidal", AxisDirection.EAST,  "long"));
        assertEquals("Geodetic latitude",   t.toLongAxisName("ellipsoidal", AxisDirection.NORTH, "latitude"));
        assertEquals("Geodetic longitude",  t.toLongAxisName("ellipsoidal", AxisDirection.EAST,  "longitude"));
        assertEquals("Spherical latitude",  t.toLongAxisName("spherical",   AxisDirection.NORTH, "lat"));
        assertEquals("Spherical longitude", t.toLongAxisName("spherical",   AxisDirection.EAST,  "long"));
        assertEquals("Ellipsoidal height",  t.toLongAxisName("ellipsoidal", AxisDirection.UP,    "ellipsoidal_height"));
        assertEquals("Unknown name",        t.toLongAxisName("ellipsoidal", AxisDirection.UP,    "Unknown name"));
    }

    /**
     * Tests {@link Transliterator#toShortAxisName(CoordinateSystem, AxisDirection, String)}.
     */
    @Test
    public void testToShortAxisName() {
        assertShortAxisNameEquals("Latitude",            new Geographic(AxisNames.GEODETIC_LATITUDE,   "φ"));
        assertShortAxisNameEquals("Spherical latitude",  new Geocentric(AxisNames.SPHERICAL_LATITUDE,  "φ′"));
        assertShortAxisNameEquals("Longitude",           new Geographic(AxisNames.GEODETIC_LONGITUDE,  "λ"));
        assertShortAxisNameEquals("Spherical longitude", new Geocentric(AxisNames.SPHERICAL_LONGITUDE, "θ"));
    }

    /**
     * Tests {@link Transliterator#toUnicodeAbbreviation(String, AxisDirection, String)}.
     */
    @Test
    public void testToUnicodeAbbreviation() {
        final Transliterator t = Transliterator.DEFAULT;
        assertEquals("P", "φ",  t.toUnicodeAbbreviation("ellipsoidal", AxisDirection.NORTH, "P"));
        assertEquals("B", "φ",  t.toUnicodeAbbreviation("ellipsoidal", AxisDirection.NORTH, "B"));
        assertEquals("L", "λ",  t.toUnicodeAbbreviation("ellipsoidal", AxisDirection.EAST,  "L"));
        assertEquals("U", "θ",  t.toUnicodeAbbreviation("polar",       AxisDirection.OTHER, "U"));
        assertEquals("U", "φ′", t.toUnicodeAbbreviation("spherical",   AxisDirection.NORTH, "U"));
        assertEquals("V", "θ",  t.toUnicodeAbbreviation("spherical",   AxisDirection.EAST,  "V"));
    }

    /**
     * Tests {@link Transliterator#toLatinAbbreviation(CoordinateSystem, AxisDirection, String)}.
     */
    @Test
    public void testToLatinAbbreviation() {
        assertAbbreviationEquals("B", new Geographic(AxisNames.GEODETIC_LATITUDE,   "φ"));
        assertAbbreviationEquals("U", new Geocentric(AxisNames.SPHERICAL_LATITUDE,  "φ′"));
        assertAbbreviationEquals("L", new Geographic(AxisNames.GEODETIC_LONGITUDE,  "λ"));
        assertAbbreviationEquals("V", new Geocentric(AxisNames.SPHERICAL_LONGITUDE, "θ"));
    }

    /**
     * Asserts that the name of the given axis, after replacement by a short name,
     * is equals to the expected string.
     */
    private static void assertShortAxisNameEquals(final String expected, final CoordinateSystemAxisMock axis) {
        assertEquals("name", expected, Transliterator.DEFAULT.toShortAxisName(axis,
                axis.getDirection(), axis.getName().getCode()));
    }

    /**
     * Asserts that the abbreviation of the given axis, after replacement of Greek letters,
     * is equals to the expected string.
     */
    private static void assertAbbreviationEquals(final String expected, final CoordinateSystemAxisMock axis) {
        assertEquals("abbreviation", expected, Transliterator.DEFAULT.toLatinAbbreviation(axis,
                axis.getDirection(), axis.getAbbreviation()));
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
