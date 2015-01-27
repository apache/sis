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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static javax.measure.unit.Unit.ONE;
import static javax.measure.unit.SI.CELSIUS;
import static javax.measure.unit.SI.KELVIN;
import static javax.measure.unit.SI.METRE;
import static javax.measure.unit.SI.METRES_PER_SECOND;
import static javax.measure.unit.SI.SQUARE_METRE;
import static javax.measure.unit.SI.KILOMETRE;
import static javax.measure.unit.SI.KILOGRAM;
import static javax.measure.unit.SI.JOULE;
import static javax.measure.unit.SI.PASCAL;
import static javax.measure.unit.SI.SECOND;
import static javax.measure.unit.SI.HERTZ;
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
import static javax.measure.unit.NonSI.PERCENT;
import static org.apache.sis.measure.SexagesimalConverter.*;
import static org.apache.sis.measure.Units.*;
import static org.apache.sis.test.Assert.*;


/**
 * Test conversions using the units declared in {@link Units}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn({
    SexagesimalConverterTest.class,
    org.apache.sis.internal.util.DefinitionURITest.class,
    org.apache.sis.internal.util.XPathsTest.class
})
public final strictfp class UnitsTest extends TestCase {
    /**
     * Sanity check of {@link UnitsMap}. This test fail if at least one code in the
     * {@link UnitsMap#EPSG_CODES} static initializer is invalid.
     */
    @Test
    public void testUnitsMap() {
        assertFalse(UnitsMap.EPSG_CODES.containsKey(null));
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
        assertEquals(DMS,      assertSerializedEquals(DMS));
        assertEquals(DMS_SCALED, assertSerializedEquals(DMS_SCALED));
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
        assertFalse(isTemporal(DMS));
        assertFalse(isTemporal(DMS_SCALED));
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
        assertFalse(isLinear(DMS));
        assertFalse(isLinear(DMS_SCALED));
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
        assertTrue (isAngular(DMS));
        assertTrue (isAngular(DMS_SCALED));
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
        assertFalse(isScale(DMS));
        assertFalse(isScale(DMS_SCALED));
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
     * Tests {@link Units#valueOf(String)} with units most commonly found in geospatial data.
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
        assertSame(DEGREE_ANGLE, valueOf("degrees_north"));
        assertSame(DEGREE_ANGLE, valueOf("degrées_north"));
        assertSame(DEGREE_ANGLE, valueOf("decimal_degree"));
        assertSame(SECOND_ANGLE, valueOf("arcsec"));
        assertSame(RADIAN,       valueOf("rad"));
        assertSame(RADIAN,       valueOf("radian"));
        assertSame(RADIAN,       valueOf("radians"));
        assertSame(SECOND,       valueOf("s"));
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
        assertSame(KELVIN,       valueOf("K"));
        assertSame(KELVIN,       valueOf("degK"));
        assertSame(CELSIUS,      valueOf("Celsius"));
        assertSame(CELSIUS,      valueOf("degree Celsius"));
        assertSame(CELSIUS,      valueOf("degree_Celcius"));
        assertSame(PASCAL,       valueOf("Pa"));
    }

    /**
     * Tests {@link Units#valueOf(String)} with more advanced units.
     * Those units are found in NetCDF files among others.
     */
    @Test
    public void testAdvancedValueOf() {
        assertSame  (Units.MILLISECOND,             valueOf("ms"));
        assertEquals(METRES_PER_SECOND,             valueOf("m/s"));
        assertEquals(METRES_PER_SECOND,             valueOf("m.s-1"));
        assertEquals(SQUARE_METRE.divide(SECOND),   valueOf("m2.s-1"));
        assertEquals(KILOGRAM.divide(SQUARE_METRE), valueOf("kg.m-2"));
        assertEquals(JOULE.divide(KILOGRAM),        valueOf("J/kg"));
        assertEquals(PASCAL.divide(SECOND),         valueOf("Pa/s"));
        assertSame  (HERTZ,                         valueOf("1/s"));
        assertSame  (HERTZ,                         valueOf("s-1"));
        assertSame  (PERCENT,                       valueOf("%"));
        assertSame  (Unit.ONE,                      valueOf("kg/kg"));
        assertSame  (Unit.ONE,                      valueOf("kg.kg-1"));
        assertSame  (Units.PPM,                     valueOf("ppm")); // Parts per million
        assertSame  (Units.PSU,                     valueOf("psu")); // Pratical Salinity Unit
        assertSame  (Units.SIGMA,                   valueOf("sigma"));

        // Potential vorticity surface
        assertEquals(KELVIN.times(SQUARE_METRE).divide(KILOGRAM.times(SECOND)), valueOf("K.m2.kg-1.s-1"));
    }

    /**
     * Tests {@link Units#valueOfEPSG(int)} and {@link Units#valueOf(String)} with a {@code "EPSG:####"} syntax.
     */
    @Test
    public void testValueOfEPSG() {
        assertSame(METRE,        valueOfEPSG(9001));
        assertSame(DEGREE_ANGLE, valueOfEPSG(9102)); // Used in prime meridian and operation parameters.
        assertSame(DEGREE_ANGLE, valueOfEPSG(9122)); // Used in coordinate system axes.
        assertSame(METRE,        valueOf("EPSG:9001"));
        assertSame(DEGREE_ANGLE, valueOf(" epsg : 9102"));
        assertSame(DEGREE_ANGLE, valueOf("urn:ogc:def:uom:EPSG::9102"));
        assertSame(METRE,        valueOf("http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertSame(METRE,        valueOf("gmxUom.xml#m"));
    }

    /**
     * Tests {@link Units#getEpsgCode(Unit, boolean)}.
     */
    @Test
    public void testGetEpsgCode() {
        assertEquals(Integer.valueOf(9001), getEpsgCode(METRE, false));
        assertEquals(Integer.valueOf(9102), getEpsgCode(DEGREE_ANGLE, false));
        assertEquals(Integer.valueOf(9122), getEpsgCode(DEGREE_ANGLE, true));
        assertEquals(Integer.valueOf(9110), getEpsgCode(DMS, false));
        assertEquals(Integer.valueOf(9110), getEpsgCode(DMS, true));
    }
}
