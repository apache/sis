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
package org.apache.sis.measure;

import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Field;
import javax.measure.Unit;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link UnitFormat} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(UnitDimensionTest.class)
public final strictfp class UnitFormatTest extends TestCase {
    /**
     * Verifies all constants defined in {@link Units} class. This method verifies:
     *
     * <ul>
     *   <li>The string representation of the dimension, which indirectly tests {@link UnitFormat}
     *       since {@link UnitDimension} delegates to to {@code UnitFormat}.</li>
     *   <li>The unit symbol as given by {@link UnitFormat#format(Object)} using the system-wide instance.</li>
     * </ul>
     */
    @Test
    public void verifyUnitConstants() {
        final Set<String> declared = new HashSet<>(64);
        for (final Field f : Units.class.getFields()) {
            if (Unit.class.isAssignableFrom(f.getType())) {
                declared.add(f.getName());
            }
        }
        verify(declared, "NANOMETRE",           "L",        "nm",    Units.NANOMETRE);
        verify(declared, "MILLIMETRE",          "L",        "mm",    Units.MILLIMETRE);
        verify(declared, "CENTIMETRE",          "L",        "cm",    Units.CENTIMETRE);
        verify(declared, "METRE",               "L",        "m",     Units.METRE);
        verify(declared, "KILOMETRE",           "L",        "km",    Units.KILOMETRE);
        verify(declared, "NAUTICAL_MILE",       "L",        "M",     Units.NAUTICAL_MILE);
        verify(declared, "STATUTE_MILE",        "L",        "mi",    Units.STATUTE_MILE);
        verify(declared, "US_SURVEY_FOOT",      "L",        "ft_US", Units.US_SURVEY_FOOT);
        verify(declared, "FOOT",                "L",        "ft",    Units.FOOT);
        verify(declared, "INCH",                "L",        "in",    Units.INCH);
        verify(declared, "POINT",               "L",        "pt",    Units.POINT);
        verify(declared, "RADIAN",              "",         "rad",   Units.RADIAN);
        verify(declared, "GRAD",                "",         "grad",  Units.GRAD);
        verify(declared, "DEGREE",              "",         "°",     Units.DEGREE);
        verify(declared, "ARC_MINUTE",          "",         "′",     Units.ARC_MINUTE);
        verify(declared, "ARC_SECOND",          "",         "″",     Units.ARC_SECOND);
        verify(declared, "MICRORADIAN",         "",         "µrad",  Units.MICRORADIAN);
        verify(declared, "MILLISECOND",         "T",        "ms",    Units.MILLISECOND);
        verify(declared, "SECOND",              "T",        "s",     Units.SECOND);
        verify(declared, "MINUTE",              "T",        "min",   Units.MINUTE);
        verify(declared, "HOUR",                "T",        "h",     Units.HOUR);
        verify(declared, "DAY",                 "T",        "d",     Units.DAY);
        verify(declared, "WEEK",                "T",        "wk",    Units.WEEK);
        verify(declared, "TROPICAL_YEAR",       "T",        "a",     Units.TROPICAL_YEAR);
        verify(declared, "PASCAL",              "M⋅L⁻¹⋅T⁻²", "Pa",    Units.PASCAL);
        verify(declared, "HECTOPASCAL",         "M⋅L⁻¹⋅T⁻²", "hPa",   Units.HECTOPASCAL);
        verify(declared, "SQUARE_METRE",        "L²",       "m²",    Units.SQUARE_METRE);
        verify(declared, "CUBIC_METRE",         "L³",       "m³",    Units.CUBIC_METRE);
        verify(declared, "METRES_PER_SECOND",   "L∕T",      "m∕s",   Units.METRES_PER_SECOND);
        verify(declared, "KILOMETRES_PER_HOUR", "L∕T",      "km∕h",  Units.KILOMETRES_PER_HOUR);
        verify(declared, "KILOGRAM",            "M",        "kg",    Units.KILOGRAM);
        verify(declared, "NEWTON",              "M⋅L∕T²",   "N",     Units.NEWTON);
        verify(declared, "JOULE",               "M⋅L²∕T²",  "J",     Units.JOULE);
        verify(declared, "WATT",                "M⋅L²∕T³",  "W",     Units.WATT);
        verify(declared, "KELVIN",              "Θ",        "K",     Units.KELVIN);
        verify(declared, "CELSIUS",             "Θ",        "℃",     Units.CELSIUS);
        verify(declared, "HERTZ",               "T⁻¹",      "Hz",    Units.HERTZ);
        verify(declared, "UNITY",               "",         "",      Units.UNITY);
        verify(declared, "PERCENT",             "",         "%",     Units.PERCENT);
        verify(declared, "PPM",                 "",         "ppm",   Units.PPM);
        verify(declared, "PSU",                 "",         "psu",   Units.PSU);
        verify(declared, "PIXEL",               "",         "px",    Units.PIXEL);
        assertTrue("Missing units in test:" + declared, declared.isEmpty());
    }

    /**
     * Verifies one of the constants declared in the {@link Unit} class.
     *
     * @param declared   a map from which to remove the {@code field} value, for verifying that we didn't forgot an element.
     * @param field      the name of the constant to be verified.
     * @param dimension  the expected string representation of the unit dimension.
     * @param symbol     the expected string representation of the unit.
     * @param unit       the unit to verify.
     */
    private static void verify(final Set<String> declared, final String field, final String dimension, final String symbol, final Unit<?> unit) {
        assertEquals(field, dimension, String.valueOf(unit.getDimension()));
        assertEquals(field, symbol,    UnitFormat.INSTANCE.format(unit));
        declared.remove(field);
    }

    /**
     * Tests the formatting of a dimension having rational powers.
     */
    @Test
    @DependsOnMethod("verifyUnitConstants")
    public void testRationalPower() {
        assertEquals("T^(5⁄2)⋅M⁻¹⋅L⁻¹", UnitDimensionTest.specificDetectivity().toString());
    }
}
