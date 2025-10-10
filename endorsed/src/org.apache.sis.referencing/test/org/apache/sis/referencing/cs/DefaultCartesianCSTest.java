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

import java.util.Map;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.apache.sis.referencing.GeodeticObjectVerifier;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.referencing.Assertions.assertAxisEquals;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgIdentifierEquals;


/**
 * Tests the {@link DefaultCartesianCS} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultCartesianCSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultCartesianCSTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a Cartesian coordinate system definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultCartesianCSTest.class.getResourceAsStream("CartesianCS.xml");
    }

    /**
     * Tests the creation of a Cartesian CS with legal axes.
     */
    @Test
    public void testConstructor() {
        final Map<String,?> properties = Map.of(DefaultCartesianCS.NAME_KEY, "Test");
        DefaultCartesianCS cs;
        /*
         * (E,N) : legal axes for the usual projected CRS.
         */
        cs = new DefaultCartesianCS(properties,
                HardCodedAxes.EASTING,
                HardCodedAxes.NORTHING);
        Validators.validate(cs);
        /*
         * (NE,SE) : same CS rotated by 45°
         */
        cs = new DefaultCartesianCS(properties,
                HardCodedAxes.NORTH_EAST,
                HardCodedAxes.SOUTH_EAST);
        Validators.validate(cs);
        /*
         * (NE,h) : considered perpendicular.
         */
        cs = new DefaultCartesianCS(properties,
                HardCodedAxes.NORTH_EAST,
                HardCodedAxes.ALTITUDE);
        Validators.validate(cs);
    }

    /**
     * Tests the creation of a Cartesian CS with illegal axes.
     */
    @Test
    public void testConstructorArgumentChecks() {
        final Map<String,?> properties = Map.of(DefaultCartesianCS.NAME_KEY, "Test");

        forIllegalAxes(properties,
                       HardCodedAxes.GEODETIC_LONGITUDE,
                       HardCodedAxes.GEODETIC_LATITUDE,
                       "Angular units should not be accepted.");

        forIllegalAxes(properties,
                       HardCodedAxes.SOUTHING,
                       HardCodedAxes.NORTHING,
                       "Colinear units should not be accepted.");

        forIllegalAxes(properties,
                       HardCodedAxes.NORTH_EAST,
                       HardCodedAxes.EASTING,
                       "Non-perpendicular axis should not be accepted.");
    }

    /**
     * Tests the construction of a coordinate system having illegal axes.
     */
    private static void forIllegalAxes(Map<String,?> properties,
            CoordinateSystemAxis a1, CoordinateSystemAxis a2, String message)
    {
        var e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultCartesianCS(properties, a1, a2),
                message);
        assertMessageContains(e);
    }

    /**
     * Creates an axis for the specified direction.
     *
     * @param direction The name of an {@link AxisDirection} value.
     */
    private static DefaultCoordinateSystemAxis createAxis(final String direction) {
        final AxisDirection c = CoordinateSystems.parseAxisDirection(direction);
        if (c.equals(AxisDirection.NORTH)) return HardCodedAxes.NORTHING;
        if (c.equals(AxisDirection.EAST))  return HardCodedAxes.EASTING;
        if (c.equals(AxisDirection.SOUTH)) return HardCodedAxes.SOUTHING;
        if (c.equals(AxisDirection.WEST))  return HardCodedAxes.WESTING;
        return new DefaultCoordinateSystemAxis(Map.of(NAME_KEY, c.name()), "?", c, Units.METRE);
    }

    /**
     * Creates a coordinate system with the specified axis directions.
     *
     * @param x The name of an {@link AxisDirection} value for the x axis.
     * @param y The name of an {@link AxisDirection} value for the y axis.
     */
    private static DefaultCartesianCS createCS(final String x, final String y) {
        final DefaultCoordinateSystemAxis xAxis = createAxis(x);
        final DefaultCoordinateSystemAxis yAxis = createAxis(y);
        final String name = xAxis.getName().getCode() + ", " + yAxis.getName().getCode();
        return new DefaultCartesianCS(Map.of(NAME_KEY, name), xAxis, yAxis);
    }

    /**
     * Creates a Cartesian CS using the provided test axis, invokes {@link AbstractCS#forConvention(AxesConvention)}
     * on it and compares with the expected axes.
     *
     * @param expectedX The name of the expected {@link AxisDirection} of x axis after normalization.
     * @param expectedY The name of the expected {@link AxisDirection} of y axis after normalization.
     * @param toTestX   The name of the {@link AxisDirection} value for the x axis of the CS to normalize.
     * @param toTestY   The name of the {@link AxisDirection} value for the y axis of the CS to normalize.
     */
    private static void assertConventionallyOrientedEquals(
            final String expectedX, final String expectedY,
            final String toTestX,   final String toTestY)
    {
        DefaultCartesianCS cs = createCS(toTestX, toTestY);
        cs = cs.forConvention(AxesConvention.DISPLAY_ORIENTED);
        assertEqualsIgnoreMetadata(createCS(expectedX, expectedY), cs);
    }

    /**
     * Asserts that the coordinate system made of the given axes has conventional orientation.
     * Then ensures that swapping the axes and applying conventional orientation gives back the original CS.
     */
    private static void testConventionalOrientation(final String x, final String y) {
        assertConventionallyOrientedEquals(x, y, x, y);         // Expect no-op.
        assertConventionallyOrientedEquals(x, y, y, x);         // Expect normalization.
    }

    /**
     * Tests {@link DefaultCartesianCS#forConvention(AxesConvention)} with
     * {@link AxesConvention#DISPLAY_ORIENTED}.
     */
    @Test
    public void testConventionalOrientation() {
        // -------------------------------- Axes to test ------ Expected axes --
        assertConventionallyOrientedEquals("East", "North",    "East", "North");
        assertConventionallyOrientedEquals("East", "North",    "North", "East");
        assertConventionallyOrientedEquals("East", "North",    "South", "East");
        assertConventionallyOrientedEquals("East", "North",    "South", "West");

        testConventionalOrientation("East",                       "North");
        testConventionalOrientation("South-East",                 "North-East");
        testConventionalOrientation("North along  90 deg East",   "North along   0 deg");
        testConventionalOrientation("North along  90 deg East",   "North along   0 deg");
        testConventionalOrientation("North along  75 deg West",   "North along 165 deg West");
        testConventionalOrientation("South along  90 deg West",   "South along   0 deg");
        testConventionalOrientation("South along 180 deg",        "South along  90 deg West");
        testConventionalOrientation("North along 130 deg West",   "North along 140 deg East");
    }

    /**
     * Tests (un)marshalling of a Cartesian coordinate system.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultCartesianCS cs = unmarshalFile(DefaultCartesianCS.class, openTestFile());
        Validators.validate(cs);
        GeodeticObjectVerifier.assertIsProjected2D(cs);
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        final CoordinateSystemAxis E = cs.getAxis(0);
        final CoordinateSystemAxis N = cs.getAxis(1);
        assertEquals("Easting, northing (E,N)", cs.getName().getCode());
        assertRemarksEquals("Used in ProjectedCRS.", cs, null);
        assertEpsgIdentifierEquals("4400", assertSingleton(cs.getIdentifiers()));
        assertEpsgIdentifierEquals("1",    assertSingleton(E.getIdentifiers()));
        assertEpsgIdentifierEquals("2",    assertSingleton(N.getIdentifiers()));
        assertAxisEquals("Easting",  "E", AxisDirection.EAST,  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Units.METRE, null, E);
        assertAxisEquals("Northing", "N", AxisDirection.NORTH, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Units.METRE, null, N);
        /*
         * Marshal and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), cs, "xmlns:*", "xsi:schemaLocation");
    }
}
