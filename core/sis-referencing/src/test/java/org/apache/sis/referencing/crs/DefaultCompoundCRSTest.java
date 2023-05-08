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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import javax.xml.bind.JAXBException;
import org.opengis.test.Validators;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.cs.DefaultCompoundCS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.xml.TestCase;
import org.junit.Test;

import static org.opengis.referencing.crs.CompoundCRS.NAME_KEY;
import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.opengis.test.Assert.assertAxisDirectionsEqual;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgNameAndIdentifierEqual;


/**
 * Tests the {@link DefaultCompoundCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.4
 */
@DependsOn({
    SubTypesTest.class,
    DefaultGeographicCRSTest.class,
    DefaultVerticalCRSTest.class
})
public final class DefaultCompoundCRSTest extends TestCase {
    /**
     * The vertical CRS arbitrarily chosen in this class for the tests.
     */
    private static final DefaultVerticalCRS HEIGHT = HardCodedCRS.GRAVITY_RELATED_HEIGHT;

    /**
     * The temporal CRS arbitrarily chosen in this class for the tests.
     */
    private static final DefaultTemporalCRS TIME = HardCodedCRS.TIME;

    /**
     * An XML file in this package containing a projected CRS definition.
     */
    private static final String XML_FILE = "CompoundCRS.xml";

    /**
     * Verifies that we do not allow construction with a duplicated horizontal or vertical component.
     *
     * @since 0.8
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testDuplicatedComponent() {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultCompoundCRS.LOCALE_KEY, Locale.ENGLISH));
        assertNull(properties.put(DefaultCompoundCRS.NAME_KEY,   "3D + illegal"));
        try {
            new DefaultCompoundCRS(properties, HardCodedCRS.WGS84, HEIGHT, HardCodedCRS.SPHERE);
            fail("Should not allow construction with two horizontal components.");
        } catch (IllegalArgumentException e) {
            assertEquals("Compound coordinate reference systems cannot contain two horizontal components.", e.getMessage());
        }
        /*
         * Try again with duplicated vertical components, opportunistically
         * testing localization in a different language.
         */
        properties.put(DefaultCompoundCRS.LOCALE_KEY, Locale.FRENCH);
        try {
            new DefaultCompoundCRS(properties, HardCodedCRS.WGS84, HEIGHT, HardCodedCRS.ELLIPSOIDAL_HEIGHT);
            fail("Should not allow construction with two vertical components.");
        } catch (IllegalArgumentException e) {
            assertEquals("Un système de référence des coordonnées ne peut pas contenir deux composantes verticales.", e.getMessage());
        }
    }

    /**
     * Verifies that horizontal CRS + ellipsoidal height is disallowed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-303">SIS-303</a>
     *
     * @since 0.8
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testEllipsoidalHeight() {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(DefaultCompoundCRS.LOCALE_KEY, Locale.ENGLISH));
        assertNull(properties.put(DefaultCompoundCRS.NAME_KEY,   "3D"));
        try {
            new DefaultCompoundCRS(properties, HardCodedCRS.WGS84, HardCodedCRS.ELLIPSOIDAL_HEIGHT);
            fail("Should not allow construction with ellipsoidal height.");
        } catch (IllegalArgumentException e) {
            assertEquals("Compound coordinate reference systems should not contain ellipsoidal height. "
                    + "Use a three-dimensional geographic system instead.", e.getMessage());
        }
        /*
         * We allow an ellipsoidal height if there is no horizontal CRS.
         * This is a departure from ISO 19111.
         */
        final DefaultCompoundCRS crs = new DefaultCompoundCRS(properties, HardCodedCRS.ELLIPSOIDAL_HEIGHT, TIME);
        assertAxisDirectionsEqual("CompoundCRS", crs.getCoordinateSystem(), AxisDirection.UP, AxisDirection.FUTURE);
    }

    /**
     * Tests construction and serialization of a {@link DefaultCompoundCRS}.
     */
    @Test
    public void testConstructionAndSerialization() {
        final DefaultGeographicCRS crs2 = HardCodedCRS.WGS84;
        final DefaultCompoundCRS   crs3 = new DefaultCompoundCRS(Map.of(NAME_KEY, "3D"), crs2, HEIGHT);
        final DefaultCompoundCRS   crs4 = new DefaultCompoundCRS(Map.of(NAME_KEY, "4D"), crs3, TIME);
        Validators.validate(crs4);
        /*
         * Verifies the coordinate system axes.
         */
        final CoordinateSystem cs = crs4.getCoordinateSystem();
        assertInstanceOf("coordinateSystem", DefaultCompoundCS.class, cs);
        assertEquals("dimension", 4, cs.getDimension());
        assertSame(HardCodedAxes.GEODETIC_LONGITUDE,     cs.getAxis(0));
        assertSame(HardCodedAxes.GEODETIC_LATITUDE,      cs.getAxis(1));
        assertSame(HardCodedAxes.GRAVITY_RELATED_HEIGHT, cs.getAxis(2));
        assertSame(HardCodedAxes.TIME,                   cs.getAxis(3));
        /*
         * Verifies the list of components, including after serialization
         * since readObject(ObjectInputStream) is expected to recreate it.
         */
        verifyComponents(crs2, crs3, crs4);
        verifyComponents(crs2, crs3, assertSerializedEquals(crs4));
    }

    /**
     * Verifies the components of the CRS created by {@link #testConstructionAndSerialization()}.
     *
     * @param  crs2  the expected two-dimensional component (for the 2 first axes).
     * @param  crs3  the expected three-dimensional component.
     * @param  crs4  the four-dimensional compound CRS to test.
     */
    private static void verifyComponents(final DefaultGeographicCRS crs2,
                                         final DefaultCompoundCRS   crs3,
                                         final DefaultCompoundCRS   crs4)
    {
        assertArrayEquals(new AbstractCRS[] {crs3, TIME},         crs4.getComponents().toArray());
        assertArrayEquals(new AbstractCRS[] {crs2, HEIGHT, TIME}, crs4.getSingleComponents().toArray());
    }

    /**
     * Tests {@link DefaultCompoundCRS#forConvention(AxesConvention)} with {@link AxesConvention#RIGHT_HANDED}.
     */
    @Test
    public void testNormalization() {
        final DefaultGeographicCRS crs2 = HardCodedCRS.WGS84_LATITUDE_FIRST;
        final DefaultGeographicCRS rh2  = crs2.forConvention(AxesConvention.RIGHT_HANDED);
        final DefaultCompoundCRS   crs3 = new DefaultCompoundCRS(Map.of(NAME_KEY, "3D"), crs2, HEIGHT);
        final DefaultCompoundCRS   crs4 = new DefaultCompoundCRS(Map.of(NAME_KEY, "4D"), crs3, TIME);
        final DefaultCompoundCRS   rh4  = crs4.forConvention(AxesConvention.RIGHT_HANDED);
        assertNotSame(crs4, rh4);
        Validators.validate(rh4);
        verifyComponents(crs2, crs3, crs4);
        verifyComponents(rh2, new DefaultCompoundCRS(Map.of(NAME_KEY, "3D"), rh2, HEIGHT), rh4);
    }

    /**
     * Tests {@link DefaultCompoundCRS#forConvention(AxesConvention)} with {@link AxesConvention#POSITIVE_RANGE}.
     */
    @Test
    public void testShiftLongitudeRange() {
        final DefaultGeographicCRS crs3 = HardCodedCRS.WGS84_3D;
        final DefaultCompoundCRS   crs4 = new DefaultCompoundCRS(Map.of(NAME_KEY, "4D"), crs3, TIME);
        CoordinateSystemAxis axis = crs4.getCoordinateSystem().getAxis(0);
        assertEquals("longitude.minimumValue", -180.0, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", +180.0, axis.getMaximumValue(), STRICT);

        assertSame("Expected a no-op.", crs4, crs4.forConvention(AxesConvention.RIGHT_HANDED));
        final DefaultCompoundCRS shifted =   crs4.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame("Expected a new CRS.", crs4, shifted);
        Validators.validate(shifted);

        axis = shifted.getCoordinateSystem().getAxis(0);
        assertEquals("longitude.minimumValue",      0.0, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue",    360.0, axis.getMaximumValue(), STRICT);
        assertSame("Expected a no-op.",         shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE));
        assertSame("Expected cached instance.", shifted, crs4   .forConvention(AxesConvention.POSITIVE_RANGE));
    }

    /**
     * Tests {@link DefaultCompoundCRS#isStandardCompliant(List)}.
     *
     * @since 0.6
     */
    @Test
    public void testIsStandardCompliant() {
        final DefaultCompoundCRS crs3 = new DefaultCompoundCRS(Map.of(NAME_KEY, "3D"), HardCodedCRS.WGS84,  HEIGHT);
        final DefaultCompoundCRS crs4 = new DefaultCompoundCRS(Map.of(NAME_KEY, "4D"), HardCodedCRS.WGS84_3D, TIME);
        assertTrue (isStandardCompliant(crs3));
        assertTrue (isStandardCompliant(crs4));
        assertTrue (isStandardCompliant(new DefaultCompoundCRS(Map.of(NAME_KEY, "4D"), crs3, TIME)));
        assertFalse(isStandardCompliant(new DefaultCompoundCRS(Map.of(NAME_KEY, "5D"), crs4, TIME)));
        assertFalse(isStandardCompliant(new DefaultCompoundCRS(Map.of(NAME_KEY, "4D"), TIME, crs3)));
    }

    /**
     * Returns {@code true} if the given CRS is compliant with ISO 19162 restrictions.
     */
    private static boolean isStandardCompliant(final DefaultCompoundCRS crs) {
        return DefaultCompoundCRS.isStandardCompliant(crs.getSingleComponents());
    }

    /**
     * Tests WKT 1 formatting.
     */
    @Test
    public void testWKT1() {
        assertWktEquals(Convention.WKT1,
                "COMPD_CS[“WGS 84 + height + time”,\n" +
                "  GEOGCS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "      PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  VERT_CS[“MSL height”,\n" +
                "    VERT_DATUM[“Mean Sea Level”, 2005],\n" +
                "    UNIT[“metre”, 1],\n" +
                "    AXIS[“Gravity-related height”, UP],\n" +
                "    AUTHORITY[“EPSG”, “5714”]],\n" +   // SIS includes Identifier for component of CompoundCRS.
                "  TIMECRS[“Time”,\n" +
                "    TDATUM[“Modified Julian”, TIMEORIGIN[1858-11-17]],\n" +
                "    TIMEUNIT[“day”, 86400],\n" +
                "    AXIS[“Time”, FUTURE]]]",
                HardCodedCRS.GEOID_4D);
    }

    /**
     * Tests WKT 2 formatting.
     */
    @Test
    @DependsOnMethod("testWKT1")
    public void testWKT2() {
        assertWktEquals(Convention.WKT2,
                "COMPOUNDCRS[“WGS 84 + height + time”,\n" +
                "  GEODCRS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      ELLIPSOID[“WGS84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]],\n" +
                "      PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    CS[ellipsoidal, 2],\n" +
                "      AXIS[“Longitude (L)”, east, ORDER[1]],\n" +
                "      AXIS[“Latitude (B)”, north, ORDER[2]],\n" +
                "      ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "  VERTCRS[“MSL height”,\n" +
                "    VDATUM[“Mean Sea Level”],\n" +
                "    CS[vertical, 1],\n" +
                "      AXIS[“Gravity-related height (H)”, up, ORDER[1]],\n" +
                "      LENGTHUNIT[“metre”, 1],\n" +
                "    ID[“EPSG”, 5714]],\n" +            // SIS includes Identifier for component of CompoundCRS.
                "  TIMECRS[“Time”,\n" +
                "    TDATUM[“Modified Julian”, TIMEORIGIN[1858-11-17]],\n" +
                "    CS[temporal, 1],\n" +
                "      AXIS[“Time (t)”, future, ORDER[1]],\n" +
                "      TIMEUNIT[“day”, 86400]],\n" +
                "  AREA[“World”],\n" +
                "  BBOX[-90.00, -180.00, 90.00, 180.00]]",
                HardCodedCRS.GEOID_4D);
    }

    /**
     * Tests WKT 2 "simplified" formatting.
     */
    @Test
    @DependsOnMethod("testWKT2")
    public void testWKT2_Simplified() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "CompoundCRS[“WGS 84 + height + time”,\n" +
                "  GeodeticCRS[“WGS 84”,\n" +
                "    Datum[“World Geodetic System 1984”,\n" +
                "      Ellipsoid[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "    CS[ellipsoidal, 2],\n" +
                "      Axis[“Longitude (L)”, east],\n" +
                "      Axis[“Latitude (B)”, north],\n" +
                "      Unit[“degree”, 0.017453292519943295]],\n" +
                "  VerticalCRS[“MSL height”,\n" +
                "    VerticalDatum[“Mean Sea Level”],\n" +
                "    CS[vertical, 1],\n" +
                "      Axis[“Gravity-related height (H)”, up],\n" +
                "      Unit[“metre”, 1],\n" +
                "    Id[“EPSG”, 5714]],\n" +            // SIS includes Identifier for component of CompoundCRS.
                "  TimeCRS[“Time”,\n" +
                "    TimeDatum[“Modified Julian”, TimeOrigin[1858-11-17]],\n" +
                "    CS[temporal, 1],\n" +
                "      Axis[“Time (t)”, future],\n" +
                "      TimeUnit[“day”, 86400]],\n" +
                "  Area[“World”],\n" +
                "  BBox[-90.00, -180.00, 90.00, 180.00]]",
                HardCodedCRS.GEOID_4D);
    }

    /**
     * Tests (un)marshalling of a derived coordinate reference system.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     *
     * @since 0.7
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultCompoundCRS crs = unmarshalFile(DefaultCompoundCRS.class, XML_FILE);
        Validators.validate(crs);
        assertEpsgNameAndIdentifierEqual("JGD2011 + JGD2011 (vertical) height", 6697, crs);
        assertAxisDirectionsEqual("coordinateSystem", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);
        /*
         * Shallow verification of the components.
         */
        final List<CoordinateReferenceSystem> components = crs.getComponents();
        assertSame("singleComponents", components, crs.getSingleComponents());
        assertEquals("components.size", 2, components.size());
        assertEpsgNameAndIdentifierEqual("JGD2011",                   6668, components.get(0));
        assertEpsgNameAndIdentifierEqual("JGD2011 (vertical) height", 6695, components.get(1));
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(XML_FILE, crs, "xmlns:*", "xsi:schemaLocation", "gml:id");
    }
}
