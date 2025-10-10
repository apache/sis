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

import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.GeodeticObjectVerifier;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.referencing.Assertions.assertAxisEquals;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgIdentifierEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests the {@link DefaultEllipsoidalCS} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultEllipsoidalCSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultEllipsoidalCSTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing an ellipsoidal coordinate system definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultEllipsoidalCSTest.class.getResourceAsStream("EllipsoidalCS.xml");
    }

    /**
     * Tests the {@link DefaultEllipsoidalCS#forConvention(AxesConvention)} method.
     */
    @Test
    public void testShiftLongitudeRange() {
        final DefaultEllipsoidalCS cs = HardCodedCS.GEODETIC_3D;
        CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals(-180, axis.getMinimumValue(), "longitude.minimumValue");
        assertEquals(+180, axis.getMaximumValue(), "longitude.maximumValue");

        assertSame(cs, cs.forConvention(AxesConvention.NORMALIZED), "Expected a no-op.");
        final DefaultEllipsoidalCS shifted = cs.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame(cs, shifted);
        Validators.validate(shifted);

        axis = shifted.getAxis(0);
        assertEquals(  0, axis.getMinimumValue(), "longitude.minimumValue");
        assertEquals(360, axis.getMaximumValue(), "longitude.maximumValue");
        assertSame(shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE), "Expected a no-op.");
        assertSame(shifted, cs     .forConvention(AxesConvention.POSITIVE_RANGE), "Expected cached instance.");
    }

    /**
     * Tests the {@link DefaultEllipsoidalCS#forConvention(AxesConvention)} method with grads units.
     */
    @Test
    public void testShiftLongitudeRangeGrads() {
        final DefaultEllipsoidalCS cs = HardCodedCS.ELLIPSOIDAL_gon;
        CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals(-200, axis.getMinimumValue(), "longitude.minimumValue");
        assertEquals(+200, axis.getMaximumValue(), "longitude.maximumValue");

        assertSame(cs, cs.forConvention(AxesConvention.RIGHT_HANDED), "Expected a no-op.");
        final DefaultEllipsoidalCS shifted = cs.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame(cs, shifted);
        Validators.validate(shifted);

        axis = shifted.getAxis(0);
        assertEquals(  0, axis.getMinimumValue(), "longitude.minimumValue");
        assertEquals(400, axis.getMaximumValue(), "longitude.maximumValue");
        assertSame(shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE), "Expected a no-op.");
        assertSame(shifted, cs     .forConvention(AxesConvention.POSITIVE_RANGE), "Expected cached instance.");
    }

    /**
     * Tests the {@link DefaultEllipsoidalCS#forConvention(AxesConvention)} involving unit conversions.
     */
    @Test
    public void testUnitConversion() {
        final DefaultEllipsoidalCS cs = HardCodedCS.ELLIPSOIDAL_gon;
        CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals(Units.GRAD, axis.getUnit());
        assertEquals(-200, axis.getMinimumValue(), "longitude.minimumValue");
        assertEquals(+200, axis.getMaximumValue(), "longitude.maximumValue");

        final DefaultEllipsoidalCS converted = cs.forConvention(AxesConvention.NORMALIZED);
        assertNotSame(cs, converted);
        Validators.validate(converted);

        axis = converted.getAxis(0);
        assertEquals(Units.DEGREE, axis.getUnit());
        assertEquals(-180, axis.getMinimumValue(), "longitude.minimumValue");
        assertEquals(+180, axis.getMaximumValue(), "longitude.maximumValue");
    }

    /**
     * Tests (un)marshalling of an ellipsoidal coordinate system.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultEllipsoidalCS cs = unmarshalFile(DefaultEllipsoidalCS.class, openTestFile());
        Validators.validate(cs);
        GeodeticObjectVerifier.assertIsGeodetic2D(cs, true);
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        final CoordinateSystemAxis φ = cs.getAxis(0);
        final CoordinateSystemAxis λ = cs.getAxis(1);
        assertEquals("Latitude (north), Longitude (east)",     cs.getName().getCode());
        assertRemarksEquals("Used in two-dimensional GeographicCRS.", cs, null);
        assertAxisEquals("Geodetic latitude",  "φ", AxisDirection.NORTH, -90,  +90, Units.DEGREE, RangeMeaning.EXACT, φ);
        assertAxisEquals("Geodetic longitude", "λ", AxisDirection.EAST, -180, +180, Units.DEGREE, RangeMeaning.WRAPAROUND, λ);
        assertEpsgIdentifierEquals("6422", assertSingleton(cs.getIdentifiers()));
        assertEpsgIdentifierEquals("106",  assertSingleton(φ.getIdentifiers()));
        assertEpsgIdentifierEquals("107",  assertSingleton(λ.getIdentifiers()));
        /*
         * Marshal and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(), cs, "xmlns:*", "xsi:schemaLocation");
    }
}
