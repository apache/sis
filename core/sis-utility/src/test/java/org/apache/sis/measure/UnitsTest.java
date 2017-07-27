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

import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Area;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Length;
import javax.measure.quantity.LuminousFlux;
import javax.measure.quantity.LuminousIntensity;
import javax.measure.quantity.Mass;
import javax.measure.quantity.SolidAngle;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Temperature;
import javax.measure.quantity.Time;
import javax.measure.quantity.Volume;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.measure.SexagesimalConverter.*;
import static org.apache.sis.measure.Units.*;
import static org.apache.sis.test.Assert.*;


/**
 * Test conversions using the units declared in {@link Units}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.3
 * @module
 */
@DependsOn({
    UnitFormatTest.class,
    SexagesimalConverterTest.class,
    org.apache.sis.internal.util.DefinitionURITest.class,
    org.apache.sis.internal.util.XPathsTest.class
})
public final strictfp class UnitsTest extends TestCase {
    /**
     * Verifies that the {@link Units#initialized} flag has been set.
     */
    @Test
    public void testInitialized() {
        assertTrue(Units.initialized);
    }

    /**
     * Tests serialization of units.
     */
    @Test
    public void testSerialization() {
        assertSame  (DEGREE,     assertSerializedEquals(DEGREE));
        assertEquals(DMS,        assertSerializedEquals(DMS));
        assertEquals(DMS_SCALED, assertSerializedEquals(DMS_SCALED));
        assertSame  (PPM,        assertSerializedEquals(PPM));
    }

    /**
     * Tests {@link Units#isTemporal(Unit)}.
     */
    @Test
    public void testIsTemporal() {
        // Standard units
        assertFalse(isTemporal(null));
        assertFalse(isTemporal(UNITY));
        assertFalse(isTemporal(METRE));
        assertFalse(isTemporal(RADIAN));
        assertFalse(isTemporal(DEGREE));
        assertFalse(isTemporal(ARC_MINUTE));
        assertFalse(isTemporal(ARC_SECOND));
        assertFalse(isTemporal(GRAD));
        assertTrue (isTemporal(DAY));
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
        assertFalse(isLinear(UNITY));
        assertTrue (isLinear(METRE));
        assertFalse(isLinear(RADIAN));
        assertFalse(isLinear(DEGREE));
        assertFalse(isLinear(ARC_MINUTE));
        assertFalse(isLinear(ARC_SECOND));
        assertFalse(isLinear(GRAD));
        assertFalse(isLinear(DAY));
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
        assertFalse(isAngular(UNITY));
        assertFalse(isAngular(METRE));
        assertTrue (isAngular(RADIAN));
        assertTrue (isAngular(DEGREE));
        assertTrue (isAngular(ARC_MINUTE));
        assertTrue (isAngular(ARC_SECOND));
        assertTrue (isAngular(GRAD));
        assertFalse(isAngular(DAY));
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
        assertTrue (isScale(UNITY));
        assertFalse(isScale(METRE));
        assertFalse(isScale(RADIAN));
        assertFalse(isScale(DEGREE));
        assertFalse(isScale(ARC_MINUTE));
        assertFalse(isScale(ARC_SECOND));
        assertFalse(isScale(GRAD));
        assertFalse(isScale(DAY));
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
    }

    /**
     * Tests {@link Units#toStandardUnit(Unit)}.
     */
    @Test
    public void testToStandardUnit() {
        assertEquals(1000.0,               toStandardUnit(KILOMETRE),    1E-15);
        assertEquals(0.017453292519943295, toStandardUnit(DEGREE), 1E-15);
    }

    /**
     * Verifies some conversion factors.
     */
    @Test
    public void testConversionFactors() {
        assertEquals(1000, KILOMETRE        .getConverterTo(METRE)              .convert(1), STRICT);
        assertEquals( 3.6, METRES_PER_SECOND.getConverterTo(KILOMETRES_PER_HOUR).convert(1), STRICT);
    }

    /**
     * Tests getting a unit for a given quantity type.
     */
    @Test
    public void testGetForQuantity() {
        assertSame("Length",            Units.METRE,             Units.get(Length.class));
        assertSame("Mass",              Units.KILOGRAM,          Units.get(Mass.class));
        assertSame("Time",              Units.SECOND,            Units.get(Time.class));
        assertSame("Temperature",       Units.KELVIN,            Units.get(Temperature.class));
        assertSame("Area",              Units.SQUARE_METRE,      Units.get(Area.class));
        assertSame("Volume",            Units.CUBIC_METRE,       Units.get(Volume.class));
        assertSame("Speed",             Units.METRES_PER_SECOND, Units.get(Speed.class));
        assertSame("LuminousIntensity", Units.CANDELA,           Units.get(LuminousIntensity.class));
        assertSame("LuminousFlux",      Units.LUMEN,             Units.get(LuminousFlux.class));
        assertSame("SolidAngle",        Units.STERADIAN,         Units.get(SolidAngle.class));
        assertSame("Angle",             Units.RADIAN,            Units.get(Angle.class));
        assertSame("Dimensionless",     Units.UNITY,             Units.get(Dimensionless.class));
    }

    /**
     * Tests getting a unit for a given dimension.
     */
    @Test
    public void testGetForDimension() {
        assertSame("Length",            Units.METRE,             Units.get(Units.METRE            .getDimension()));
        assertSame("Mass",              Units.KILOGRAM,          Units.get(Units.KILOGRAM         .getDimension()));
        assertSame("Time",              Units.SECOND,            Units.get(Units.SECOND           .getDimension()));
        assertSame("Temperature",       Units.KELVIN,            Units.get(Units.KELVIN           .getDimension()));
        assertSame("Area",              Units.SQUARE_METRE,      Units.get(Units.SQUARE_METRE     .getDimension()));
        assertSame("Volume",            Units.CUBIC_METRE,       Units.get(Units.CUBIC_METRE      .getDimension()));
        assertSame("Speed",             Units.METRES_PER_SECOND, Units.get(Units.METRES_PER_SECOND.getDimension()));
        assertSame("LuminousIntensity", Units.CANDELA,           Units.get(Units.CANDELA          .getDimension()));
        assertSame("LuminousFlux",      Units.CANDELA,           Units.get(Units.LUMEN            .getDimension()));    // Because lumen is candela divided by a dimensionless unit.
        assertSame("SolidAngle",        Units.UNITY,             Units.get(Units.STERADIAN        .getDimension()));
        assertSame("Angle",             Units.UNITY,             Units.get(Units.RADIAN           .getDimension()));
        assertSame("Dimensionless",     Units.UNITY,             Units.get(Units.UNITY            .getDimension()));
    }

    /**
     * Tests {@link Units#valueOf(String)} with units most commonly found in geospatial data.
     */
    @Test
    public void testValueOf() {
        assertSame(DEGREE,       valueOf("°"));
        assertSame(DEGREE,       valueOf("deg"));
        assertSame(DEGREE,       valueOf("degree"));
        assertSame(DEGREE,       valueOf("degrees"));
        assertSame(DEGREE,       valueOf("degrées"));
        assertSame(DEGREE,       valueOf("DEGREES"));
        assertSame(DEGREE,       valueOf("DEGRÉES"));
        assertSame(DEGREE,       valueOf("degrees_east"));
        assertSame(DEGREE,       valueOf("degrees_north"));
        assertSame(DEGREE,       valueOf("degrées_north"));
        assertSame(DEGREE,       valueOf("decimal_degree"));
        assertSame(ARC_SECOND,   valueOf("arcsec"));
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
        assertSame  (MILLISECOND,                   valueOf("ms"));
        assertEquals(METRES_PER_SECOND,             valueOf("m/s"));
        assertEquals(METRES_PER_SECOND,             valueOf("m.s-1"));
        assertEquals(SQUARE_METRE.divide(SECOND),   valueOf("m2.s-1"));
        assertEquals(KILOGRAM.divide(SQUARE_METRE), valueOf("kg.m-2"));
        assertEquals(JOULE.divide(KILOGRAM),        valueOf("J/kg"));
        assertEquals(PASCAL.divide(SECOND),         valueOf("Pa/s"));
        assertSame  (HERTZ,                         valueOf("1/s"));
        assertSame  (HERTZ,                         valueOf("s-1"));
        assertSame  (PERCENT,                       valueOf("%"));
        assertSame  (UNITY,                         valueOf("kg/kg"));
        assertSame  (UNITY,                         valueOf("kg.kg-1"));
        assertSame  (PPM,                           valueOf("ppm"));            // Parts per million
        assertSame  (PSU,                           valueOf("psu"));            // Pratical Salinity Unit
        assertSame  (SIGMA,                         valueOf("sigma"));

        // Potential vorticity surface
        assertEquals(KELVIN.multiply(SQUARE_METRE).divide(KILOGRAM.multiply(SECOND)), valueOf("K.m2.kg-1.s-1"));
    }

    /**
     * Tests {@link Units#valueOfEPSG(int)} and {@link Units#valueOf(String)} with a {@code "EPSG:####"} syntax.
     */
    @Test
    public void testValueOfEPSG() {
        assertSame(METRE,  valueOfEPSG(9001));
        assertSame(DEGREE, valueOfEPSG(9102));              // Used in prime meridian and operation parameters.
        assertSame(DEGREE, valueOfEPSG(9122));              // Used in coordinate system axes.
        assertSame(METRE,  valueOf("EPSG:9001"));
        assertSame(DEGREE, valueOf(" epsg : 9102"));
        assertSame(DEGREE, valueOf("urn:ogc:def:uom:EPSG::9102"));
        assertSame(METRE,  valueOf("http://schemas.opengis.net/iso/19139/20070417/resources/uom/gmxUom.xml#xpointer(//*[@gml:id='m'])"));
        assertSame(METRE,  valueOf("gmxUom.xml#m"));

        assertSame(TROPICAL_YEAR,                   valueOfEPSG(1029));
        assertSame(SECOND,                          valueOfEPSG(1040));
        assertSame(FOOT,                            valueOfEPSG(9002));
        assertSame(US_SURVEY_FOOT,                  valueOfEPSG(9003));
        assertSame(NAUTICAL_MILE,                   valueOfEPSG(9030));
        assertSame(KILOMETRE,                       valueOfEPSG(9036));
        assertSame(RADIAN,                          valueOfEPSG(9101));
        assertSame(ARC_MINUTE,                      valueOfEPSG(9103));
        assertSame(ARC_SECOND,                      valueOfEPSG(9104));
        assertSame(GRAD,                            valueOfEPSG(9105));
        assertSame(MICRORADIAN,                     valueOfEPSG(9109));
        assertSame(SexagesimalConverter.DMS_SCALED, valueOfEPSG(9107));
        assertSame(SexagesimalConverter.DMS_SCALED, valueOfEPSG(9108));
        assertSame(SexagesimalConverter.DMS,        valueOfEPSG(9110));
        assertSame(SexagesimalConverter.DM,         valueOfEPSG(9111));
        assertSame(UNITY,                           valueOfEPSG(9203));
        assertSame(UNITY,                           valueOfEPSG(9201));
        assertSame(PPM,                             valueOfEPSG(9202));
    }

    /**
     * Tests {@link Units#getEpsgCode(Unit, boolean)}.
     */
    @Test
    public void testGetEpsgCode() {
        assertEquals(Integer.valueOf(9001), getEpsgCode(METRE,          false));
        assertEquals(Integer.valueOf(9102), getEpsgCode(DEGREE,         false));
        assertEquals(Integer.valueOf(9122), getEpsgCode(DEGREE,         true));
        assertEquals(Integer.valueOf(9110), getEpsgCode(DMS,            false));
        assertEquals(Integer.valueOf(9110), getEpsgCode(DMS,            true));
        assertEquals(Integer.valueOf(9107), getEpsgCode(DMS_SCALED,     false));
        assertEquals(Integer.valueOf(9111), getEpsgCode(DM,             false));
        assertEquals(Integer.valueOf(1029), getEpsgCode(TROPICAL_YEAR,  false));
        assertEquals(Integer.valueOf(1040), getEpsgCode(SECOND,         false));
        assertEquals(Integer.valueOf(9002), getEpsgCode(FOOT,           false));
        assertEquals(Integer.valueOf(9003), getEpsgCode(US_SURVEY_FOOT, false));
        assertEquals(Integer.valueOf(9030), getEpsgCode(NAUTICAL_MILE,  false));
        assertEquals(Integer.valueOf(9036), getEpsgCode(KILOMETRE,      false));
        assertEquals(Integer.valueOf(9101), getEpsgCode(RADIAN,         false));
        assertEquals(Integer.valueOf(9103), getEpsgCode(ARC_MINUTE,     false));
        assertEquals(Integer.valueOf(9104), getEpsgCode(ARC_SECOND,     false));
        assertEquals(Integer.valueOf(9105), getEpsgCode(GRAD,           false));
        assertEquals(Integer.valueOf(9109), getEpsgCode(MICRORADIAN,    false));
        assertEquals(Integer.valueOf(9201), getEpsgCode(UNITY,          false));
        assertEquals(Integer.valueOf(9202), getEpsgCode(PPM,            false));
    }
}
