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
package org.apache.sis.referencing.datum;

import java.lang.reflect.Field;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.internal.referencing.VerticalDatumTypes;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.apache.sis.referencing.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.referencing.GeodeticObjectVerifier.*;


/**
 * Tests the {@link DefaultVerticalDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.internal.referencing.VerticalDatumTypesTest.class)
public final strictfp class DefaultVerticalDatumTest extends XMLTestCase {
    /**
     * An XML file in this package containing a vertical datum definition.
     */
    private static final String XML_FILE = "Mean Sea Level.xml";

    /**
     * Tests the {@link DefaultVerticalDatum#getVerticalDatumType()} method in a state
     * simulating unmarshalling of GML 3.2 document.
     *
     * @throws NoSuchFieldException   Should never happen.
     * @throws IllegalAccessException Should never happen.
     */
    @Test
    public void testAfterUnmarshal() throws NoSuchFieldException, IllegalAccessException {
        final Field typeField = DefaultVerticalDatum.class.getDeclaredField("type");
        typeField.setAccessible(true);
        assertEquals(VerticalDatumType .GEOIDAL,       typeForName(typeField, "Geoidal height"));
        assertEquals(VerticalDatumType .DEPTH,         typeForName(typeField, "Some depth measurement"));
        assertEquals(VerticalDatumTypes.ELLIPSOIDAL,   typeForName(typeField, "Ellipsoidal height"));
        assertEquals(VerticalDatumType .OTHER_SURFACE, typeForName(typeField, "NotADepth"));
    }

    /**
     * Returns the vertical datum type inferred by {@link DefaultVerticalDatum} for the given name.
     */
    private static VerticalDatumType typeForName(final Field typeField, final String name) throws IllegalAccessException {
        final DefaultVerticalDatum datum = new DefaultVerticalDatum(
                singletonMap(DefaultVerticalDatum.NAME_KEY, name), VerticalDatumType.OTHER_SURFACE);
        typeField.set(datum, null);
        return datum.getVerticalDatumType();
    }

    /**
     * Tests {@link DefaultVerticalDatum#toWKT()}.
     */
    @Test
    public void testToWKT() {
        DefaultVerticalDatum datum;
        datum = new DefaultVerticalDatum(singletonMap(DefaultVerticalDatum.NAME_KEY, "Geoidal"), VerticalDatumType.GEOIDAL);
        assertWktEquals(datum, "VERT_DATUM[“Geoidal”, 2005]");

        datum = new DefaultVerticalDatum(singletonMap(DefaultVerticalDatum.NAME_KEY, "Ellipsoidal"), VerticalDatumTypes.ELLIPSOIDAL);
        assertWktEquals(datum, "VERT_DATUM[“Ellipsoidal”, 2002]");
    }

    /**
     * Tests unmarshalling.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final DefaultVerticalDatum datum = unmarshalFile(DefaultVerticalDatum.class, XML_FILE);
        assertIsMeanSeaLevel(datum);
        assertIsWorld((GeographicBoundingBox) getSingleton(datum.getDomainOfValidity().getGeographicElements()));
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        assertEquals("remarks",          "Approximates geoid.",             datum.getRemarks().toString());
        assertEquals("scope",            "Hydrography.",                    datum.getScope().toString());
        assertEquals("anchorDefinition", "Averaged over a 19-year period.", datum.getAnchorPoint().toString());
    }
}
