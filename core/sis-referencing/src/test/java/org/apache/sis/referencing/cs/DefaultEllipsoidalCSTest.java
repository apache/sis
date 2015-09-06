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

import javax.xml.bind.JAXBException;
import javax.measure.unit.NonSI;
import org.opengis.test.Validators;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.referencing.GeodeticObjectVerifier;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link DefaultEllipsoidalCS} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(AbstractCSTest.class)
public final strictfp class DefaultEllipsoidalCSTest extends XMLTestCase {
    /**
     * An XML file in this package containing an ellipsoidal coordinate system definition.
     */
    private static final String XML_FILE = "EllipsoidalCS.xml";

    /**
     * Tests the {@link DefaultEllipsoidalCS#forConvention(AxesConvention)} method.
     */
    @Test
    public void testShiftLongitudeRange() {
        final DefaultEllipsoidalCS cs = HardCodedCS.GEODETIC_3D;
        CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals("longitude.minimumValue", -180, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", +180, axis.getMaximumValue(), STRICT);

        assertSame("Expected a no-op.", cs,  cs.forConvention(AxesConvention.NORMALIZED));
        final DefaultEllipsoidalCS shifted = cs.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame("Expected a new CS.", cs, shifted);
        Validators.validate(shifted);

        axis = shifted.getAxis(0);
        assertEquals("longitude.minimumValue",   0, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", 360, axis.getMaximumValue(), STRICT);
        assertSame("Expected a no-op.",         shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE));
        assertSame("Expected cached instance.", shifted, cs     .forConvention(AxesConvention.POSITIVE_RANGE));
    }

    /**
     * Tests the {@link DefaultEllipsoidalCS#forConvention(AxesConvention)} method with gradians units.
     */
    @Test
    @DependsOnMethod("testShiftLongitudeRange")
    public void testShiftLongitudeRangeGradians() {
        final DefaultEllipsoidalCS cs = HardCodedCS.ELLIPSOIDAL_gon;
        CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals("longitude.minimumValue", -200, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", +200, axis.getMaximumValue(), STRICT);

        assertSame("Expected a no-op.", cs,  cs.forConvention(AxesConvention.RIGHT_HANDED));
        final DefaultEllipsoidalCS shifted = cs.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame("Expected a new CS.", cs, shifted);
        Validators.validate(shifted);

        axis = shifted.getAxis(0);
        assertEquals("longitude.minimumValue",   0, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", 400, axis.getMaximumValue(), STRICT);
        assertSame("Expected a no-op.",         shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE));
        assertSame("Expected cached instance.", shifted, cs     .forConvention(AxesConvention.POSITIVE_RANGE));
    }

    /**
     * Tests the {@link DefaultEllipsoidalCS#forConvention(AxesConvention)} involving unit conversions.
     */
    @Test
    public void testUnitConversion() {
        final DefaultEllipsoidalCS cs = HardCodedCS.ELLIPSOIDAL_gon;
        CoordinateSystemAxis axis = cs.getAxis(0);
        assertEquals("unit", NonSI.GRADE, axis.getUnit());
        assertEquals("longitude.minimumValue", -200, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", +200, axis.getMaximumValue(), STRICT);

        final DefaultEllipsoidalCS converted = cs.forConvention(AxesConvention.NORMALIZED);
        assertNotSame("Expected a new CS.", cs, converted);
        Validators.validate(converted);

        axis = converted.getAxis(0);
        assertEquals("unit", NonSI.DEGREE_ANGLE, axis.getUnit());
        assertEquals("longitude.minimumValue", -180, axis.getMinimumValue(), STRICT);
        assertEquals("longitude.maximumValue", +180, axis.getMaximumValue(), STRICT);
    }

    /**
     * Tests (un)marshalling of an ellipsoidal coordinate system.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultEllipsoidalCS cs = unmarshalFile(DefaultEllipsoidalCS.class, XML_FILE);
        Validators.validate(cs);
        GeodeticObjectVerifier.assertIsGeodetic2D(cs, true);
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        final CoordinateSystemAxis φ = cs.getAxis(0);
        final CoordinateSystemAxis λ = cs.getAxis(1);
        assertEquals("name",    "Latitude (north), Longitude (east)",     cs.getName().getCode());
        assertEquals("remarks", "Used in two-dimensional GeographicCRS.", cs.getRemarks().toString());
        assertAxisEquals("Geodetic latitude",  "φ", AxisDirection.NORTH, -90,  +90, NonSI.DEGREE_ANGLE, RangeMeaning.EXACT, φ);
        assertAxisEquals("Geodetic longitude", "λ", AxisDirection.EAST, -180, +180, NonSI.DEGREE_ANGLE, RangeMeaning.WRAPAROUND, λ);
        assertEpsgIdentifierEquals("6422", getSingleton(cs.getIdentifiers()));
        assertEpsgIdentifierEquals("106",  getSingleton(φ.getIdentifiers()));
        assertEpsgIdentifierEquals("107",  getSingleton(λ.getIdentifiers()));
        /*
         * Marshal and compare with the original file.
         */
        assertMarshalEqualsFile(XML_FILE, cs, "xmlns:*", "xsi:schemaLocation");
    }
}
