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
import org.opengis.test.Validators;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.referencing.GeodeticObjectVerifier;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link DefaultEllipsoidalCS} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
@DependsOn(DefaultCoordinateSystemAxisTest.class)
public final strictfp class DefaultEllipsoidalCSTest extends XMLTestCase {
    /**
     * An XML file in this package containing an ellipsoidal coordinate system definition.
     */
    private static final String XML_FILE = "EllipsoidalCS.xml";

    /**
     * Tests unmarshalling of an ellipsoidal coordinate system.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final DefaultEllipsoidalCS cs = unmarshalFile(DefaultEllipsoidalCS.class, XML_FILE);
        Validators.validate(cs);
        GeodeticObjectVerifier.assertIsGeodetic2D(cs);
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        assertEquals("name",    "Latitude (north), Longitude (east)",     cs.getName().getCode());
        assertEquals("remarks", "Used in two-dimensional GeographicCRS.", cs.getRemarks().toString());
        assertIdentifierEquals("identifier", "OGP", "EPSG", null, "6422", getSingleton(cs.getIdentifiers()));

        final CoordinateSystemAxis latitude  = cs.getAxis(0);
        final CoordinateSystemAxis longitude = cs.getAxis(1);
        assertIdentifierEquals("axis[0].identifier", "OGP", "EPSG", null, "106", getSingleton(latitude.getIdentifiers()));
        assertIdentifierEquals("axis[1].identifier", "OGP", "EPSG", null, "107", getSingleton(longitude.getIdentifiers()));
        assertEquals("axis[0].abbreviation", "Lat",                   latitude .getAbbreviation());
        assertEquals("axis[1].abbreviation", "Long",                  longitude.getAbbreviation());
        assertEquals("axis[0].rangeMeaning", RangeMeaning.EXACT,      latitude .getRangeMeaning());
        assertEquals("axis[1].abbreviation", RangeMeaning.WRAPAROUND, longitude.getRangeMeaning());
        /*
         * Minimum and maximum values have been verified by GeodeticObjectVerifier.assertIsGeodetic2D(cs)
         * if range meanings were not null.
         */
    }
}
