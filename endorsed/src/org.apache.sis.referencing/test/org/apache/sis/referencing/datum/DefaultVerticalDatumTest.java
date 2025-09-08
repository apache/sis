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

import java.util.Map;
import java.io.InputStream;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.referencing.internal.VerticalDatumTypes;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.privy.LegacyNamespaces;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.Version;
import static org.apache.sis.referencing.GeodeticObjectVerifier.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.TestUtilities.getScope;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;

// Specific to the main branch:
import java.lang.reflect.Field;
import org.opengis.referencing.datum.VerticalDatumType;


/**
 * Tests the {@link DefaultVerticalDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultVerticalDatumTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultVerticalDatumTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a vertical datum definition.
     *
     * @param  legacy  {@code true} for GML 3.1 or {@code false} for GML 3.2.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final boolean legacy) {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultVerticalDatumTest.class.getResourceAsStream(
                legacy ? "VerticalDatum (GML 3.1).xml"
                       : "VerticalDatum.xml");
    }

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
        assertEquals(VerticalDatumTypes.ellipsoidal(), typeForName(typeField, "Ellipsoidal height"));
        assertEquals(VerticalDatumType .OTHER_SURFACE, typeForName(typeField, "NotADepth"));
    }

    /**
     * Returns the vertical datum type inferred by {@link DefaultVerticalDatum} for the given name.
     */
    private static VerticalDatumType typeForName(final Field typeField, final String name) throws IllegalAccessException {
        final var datum = new DefaultVerticalDatum(
                Map.of(DefaultVerticalDatum.NAME_KEY, name),
                VerticalDatumType.OTHER_SURFACE);
        typeField.set(datum, null);
        return datum.getVerticalDatumType();
    }

    /**
     * Tests {@link DefaultVerticalDatum#toWKT()}.
     */
    @Test
    public void testToWKT() {
        DefaultVerticalDatum datum;
        datum = new DefaultVerticalDatum(Map.of(DefaultVerticalDatum.NAME_KEY, "Geoidal"), VerticalDatumType.GEOIDAL);
        assertWktEquals(Convention.WKT1, "VERT_DATUM[“Geoidal”, 2005]", datum);
        assertWktEquals(Convention.WKT2_2015, "VDATUM[“Geoidal”]", datum);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "VerticalDatum[“Geoidal”]", datum);

        datum = new DefaultVerticalDatum(Map.of(DefaultVerticalDatum.NAME_KEY, "Ellipsoidal"), VerticalDatumTypes.ellipsoidal());
        assertWktEquals(Convention.WKT1, "VERT_DATUM[“Ellipsoidal”, 2002]", datum);
        assertWktEquals(Convention.WKT2_2015, "VDATUM[“Ellipsoidal”]", datum);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "VerticalDatum[“Ellipsoidal”]", datum);
    }

    /**
     * Tests XML (un)marshalling.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultVerticalDatum datum = unmarshalFile(DefaultVerticalDatum.class, openTestFile(false));
        assertIsMeanSeaLevel(datum, true);
        /*
         * Following attribute does not exist in GML 3.2, so it has been inferred.
         * Our datum name is "Mean Sea Level", which is mapped to the geoidal type.
         */
        assertEquals(VerticalDatumType.GEOIDAL, datum.getVerticalDatumType());
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        assertRemarksEquals("Approximates geoid.",      datum, null);
        assertEquals("Hydrography.",                    getScope(datum));
        assertEquals("Averaged over a 19-year period.", datum.getAnchorDefinition().get().toString());
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(openTestFile(false), datum, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests (un)marshalling of an older version, GML 3.1.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-160">SIS-160: Need XSLT between GML 3.1 and 3.2</a>
     */
    @Test
    public void testGML31() throws JAXBException {
        final Version version = new Version("3.1");
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        unmarshaller.setProperty(XML.GML_VERSION, version);
        final var datum = (DefaultVerticalDatum) unmarshaller.unmarshal(openTestFile(true));
        pool.recycle(unmarshaller);
        /*
         * Following attribute exists in GML 3.1 only.
         */
        assertEquals(VerticalDatumType.GEOIDAL, datum.getVerticalDatumType());
        /*
         * The name, anchor definition and domain of validity are lost because
         * those property does not have the same XML element name (SIS-160).
         * Below is all we have.
         */
        assertRemarksEquals("Approximates geoid.", datum, null);
        assertEquals("Hydrography.", getScope(datum));
        /*
         * Test marshalling. We cannot yet compare with the original XML file
         * because of all the information lost. This may be fixed in a future
         * SIS version (SIS-160).
         */
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.GML_VERSION, version);
        final String xml = marshal(marshaller, datum);
        pool.recycle(marshaller);
        assertXmlEquals(
                "<gml:VerticalDatum xmlns:gml=\"" + LegacyNamespaces.GML + "\">\n" +
                "  <gml:remarks>Approximates geoid.</gml:remarks>\n" +
                "  <gml:scope>Hydrography.</gml:scope>\n" +
                "  <gml:verticalDatumType>geoidal</gml:verticalDatumType>\n" +
                "</gml:VerticalDatum>", xml, "xmlns:*");
    }
}
