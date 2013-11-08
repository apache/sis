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

import javax.measure.unit.Unit;
import javax.measure.quantity.Quantity;
import javax.measure.converter.UnitConverter;
import org.apache.sis.test.TestCase;
import org.junit.*;

import static javax.measure.unit.Unit.ONE;
import static javax.measure.unit.SI.CELSIUS;
import static javax.measure.unit.SI.METRE;
import static javax.measure.unit.SI.KILOMETRE;
import static javax.measure.unit.SI.RADIAN;
import static javax.measure.unit.NonSI.CENTIRADIAN;
import static javax.measure.unit.NonSI.DEGREE_ANGLE;
import static javax.measure.unit.NonSI.MINUTE_ANGLE;
import static javax.measure.unit.NonSI.SECOND_ANGLE;
import static javax.measure.unit.NonSI.GRADE;
import static javax.measure.unit.NonSI.DAY;
import static javax.measure.unit.NonSI.SPHERE;
import static javax.measure.unit.NonSI.ATMOSPHERE;
import static javax.measure.unit.NonSI.NAUTICAL_MILE;
import static org.apache.sis.measure.Units.*;
import static org.apache.sis.test.Assert.*;


/**
 * Test conversions using the units declared in {@link Units}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
public final strictfp class UnitsTest extends TestCase {
    /**
     * Compares two values for equality.
     */
    private static <Q extends Quantity> void checkConversion(
            final double expected, final Unit<Q> unitExpected,
            final double actual,   final Unit<Q> unitActual)
    {
        UnitConverter converter = unitActual.getConverterTo(unitExpected);
        assertEquals(expected, converter.convert(actual), 1E-6);
        converter = converter.inverse();
        assertEquals(actual, converter.convert(expected), 1E-6);
    }

    /**
     * Checks the conversions using {@link Units#SEXAGESIMAL_DMS}.
     */
    @Test
    public void testSexagesimal() {
        checkConversion(10.00, DEGREE_ANGLE, 10.0000, SEXAGESIMAL_DMS);
        checkConversion(10.01, DEGREE_ANGLE, 10.0036, SEXAGESIMAL_DMS);
        checkConversion(10.50, DEGREE_ANGLE, 10.3000, SEXAGESIMAL_DMS);
        checkConversion(10.99, DEGREE_ANGLE, 10.5924, SEXAGESIMAL_DMS);
    }

    /**
     * Tests serialization of units.
     *
     * @todo The {@code assertEquals} in this method should actually be {@code assertSame},
     *       but JSR-275 0.9.3 does not resolve units on deserialization.
     */
    @Test
    public void testSerialization() {
        assertEquals(DEGREE_ANGLE,         assertSerializedEquals(DEGREE_ANGLE));
        assertEquals(SEXAGESIMAL_DMS,      assertSerializedEquals(SEXAGESIMAL_DMS));
        assertEquals(DEGREE_MINUTE_SECOND, assertSerializedEquals(DEGREE_MINUTE_SECOND));
        assertEquals(PPM,                  assertSerializedEquals(PPM));
    }

    /**
     * Tests {@link Units#isTemporal(Unit)}.
     */
    @Test
    public void testIsTemporal() {
        // Standard units
        assertFalse(isTemporal(null));
        assertFalse(isTemporal(ONE));
        assertFalse(isTemporal(METRE));
        assertFalse(isTemporal(RADIAN));
        assertFalse(isTemporal(CENTIRADIAN));
        assertFalse(isTemporal(DEGREE_ANGLE));
        assertFalse(isTemporal(MINUTE_ANGLE));
        assertFalse(isTemporal(SECOND_ANGLE));
        assertFalse(isTemporal(GRADE));
        assertTrue (isTemporal(DAY));
        assertFalse(isTemporal(SPHERE));
        assertFalse(isTemporal(ATMOSPHERE));
        assertFalse(isTemporal(NAUTICAL_MILE));

        // Additional units
        assertFalse(isTemporal(PPM));
        assertTrue (isTemporal(MILLISECOND));
        assertFalse(isTemporal(SEXAGESIMAL_DMS));
        assertFalse(isTemporal(DEGREE_MINUTE_SECOND));
    }

    /**
     * Tests {@link Units#isLinear(Unit)}.
     */
    @Test
    public void testIsLinear() {
        // Standard units
        assertFalse(isLinear(null));
        assertFalse(isLinear(ONE));
        assertTrue (isLinear(METRE));
        assertFalse(isLinear(RADIAN));
        assertFalse(isLinear(CENTIRADIAN));
        assertFalse(isLinear(DEGREE_ANGLE));
        assertFalse(isLinear(MINUTE_ANGLE));
        assertFalse(isLinear(SECOND_ANGLE));
        assertFalse(isLinear(GRADE));
        assertFalse(isLinear(DAY));
        assertFalse(isLinear(SPHERE));
        assertFalse(isLinear(ATMOSPHERE));
        assertTrue (isLinear(NAUTICAL_MILE));

        // Additional units
        assertFalse(isLinear(PPM));
        assertFalse(isLinear(MILLISECOND));
        assertFalse(isLinear(SEXAGESIMAL_DMS));
        assertFalse(isLinear(DEGREE_MINUTE_SECOND));
    }

    /**
     * Tests {@link Units#isAngular(Unit)}.
     */
    @Test
    public void testIsAngular() {
        // Standard units
        assertFalse(isAngular(null));
        assertFalse(isAngular(ONE));
        assertFalse(isAngular(METRE));
        assertTrue (isAngular(RADIAN));
        assertTrue (isAngular(CENTIRADIAN));
        assertTrue (isAngular(DEGREE_ANGLE));
        assertTrue (isAngular(MINUTE_ANGLE));
        assertTrue (isAngular(SECOND_ANGLE));
        assertTrue (isAngular(GRADE));
        assertFalse(isAngular(DAY));
        assertFalse(isAngular(SPHERE));
        assertFalse(isAngular(ATMOSPHERE));
        assertFalse(isAngular(NAUTICAL_MILE));

        // Additional units
        assertFalse(isAngular(PPM));
        assertFalse(isAngular(MILLISECOND));
        assertTrue (isAngular(SEXAGESIMAL_DMS));
        assertTrue (isAngular(DEGREE_MINUTE_SECOND));
    }

    /**
     * Tests {@link Units#isScale(Unit)}.
     */
    @Test
    public void testIsScale() {
        // Standard units
        assertFalse(isScale(null));
        assertTrue (isScale(ONE));
        assertFalse(isScale(METRE));
        assertFalse(isScale(RADIAN));
        assertFalse(isScale(CENTIRADIAN));
        assertFalse(isScale(DEGREE_ANGLE));
        assertFalse(isScale(MINUTE_ANGLE));
        assertFalse(isScale(SECOND_ANGLE));
        assertFalse(isScale(GRADE));
        assertFalse(isScale(DAY));
        assertFalse(isScale(SPHERE));
        assertFalse(isScale(ATMOSPHERE));
        assertFalse(isScale(NAUTICAL_MILE));

        // Additional units
        assertTrue (isScale(PPM));
        assertFalse(isScale(MILLISECOND));
        assertFalse(isScale(SEXAGESIMAL_DMS));
        assertFalse(isScale(DEGREE_MINUTE_SECOND));
    }

    /**
     * Tests {@link Units#isPressure(Unit)}.
     */
    @Test
    public void testIsPressure() {
        assertFalse(isPressure(null));
        assertFalse(isPressure(METRE));
        assertTrue (isPressure(ATMOSPHERE));
    }

    /**
     * Tests {@link Units#toStandardUnit(Unit)}.
     */
    @Test
    public void testToStandardUnit() {
        assertEquals(1000.0,               toStandardUnit(KILOMETRE),    1E-15);
        assertEquals(0.017453292519943295, toStandardUnit(DEGREE_ANGLE), 1E-15);
    }

    /**
     * Tests {@link Units#multiply(Unit, double)}.
     */
    @Test
    public void testMultiply() {
        assertSame(KILOMETRE,    multiply(METRE,  1000));
        assertSame(DEGREE_ANGLE, multiply(RADIAN, 0.017453292519943295));
    }

    /**
     * Tests {@link Units#valueOf(String)}.
     */
    @Test
    public void testValueOf() {
        assertSame(DEGREE_ANGLE, valueOf("°"));
        assertSame(DEGREE_ANGLE, valueOf("deg"));
        assertSame(DEGREE_ANGLE, valueOf("degree"));
        assertSame(DEGREE_ANGLE, valueOf("degrees"));
        assertSame(DEGREE_ANGLE, valueOf("degrées"));
        assertSame(DEGREE_ANGLE, valueOf("DEGREES"));
        assertSame(DEGREE_ANGLE, valueOf("DEGRÉES"));
        assertSame(DEGREE_ANGLE, valueOf("degrees_east"));
        assertSame(DEGREE_ANGLE, valueOf("degrées_north"));
        assertSame(DEGREE_ANGLE, valueOf("decimal_degree"));
        assertSame(SECOND_ANGLE, valueOf("arcsec"));
        assertSame(RADIAN,       valueOf("rad"));
        assertSame(RADIAN,       valueOf("radian"));
        assertSame(RADIAN,       valueOf("radians"));
        assertSame(METRE,        valueOf("m"));
        assertSame(METRE,        valueOf("metre"));
        assertSame(METRE,        valueOf("meter"));
        assertSame(METRE,        valueOf("metres"));
        assertSame(METRE,        valueOf("mètres"));
        assertSame(METRE,        valueOf("meters"));
        assertSame(KILOMETRE,    valueOf("km"));
        assertSame(KILOMETRE,    valueOf("kilometre"));
        assertSame(KILOMETRE,    valueOf("kilometer"));
        assertSame(KILOMETRE,    valueOf("kilometres"));
        assertSame(KILOMETRE,    valueOf("kilomètres"));
        assertSame(KILOMETRE,    valueOf("kilometers"));
        assertSame(CELSIUS,      valueOf("Celsius"));
        assertSame(CELSIUS,      valueOf("degree Celsius"));
        assertSame(CELSIUS,      valueOf("degree_Celcius"));
    }

    /**
     * Tests {@link Units#valueOfEPSG(int)}.
     */
    @Test
    public void testValueOfEPSG() {
        assertSame(METRE,        valueOfEPSG(9001));
        assertSame(DEGREE_ANGLE, valueOfEPSG(9102));
        assertSame(METRE,        valueOf("EPSG:9001"));
        assertSame(DEGREE_ANGLE, valueOf(" epsg : 9102"));
    }
}
