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
package org.apache.sis.internal.jaxb.referencing;

import java.util.Collections;
import javax.measure.unit.SI;
import javax.xml.bind.JAXBException;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link SecondDefiningParameter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class SecondDefiningParameterTest extends XMLTestCase {
    /**
     * The XML to be used for testing purpose.
     */
    private static final String XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<gml:SecondDefiningParameter xmlns:gml=\"http://www.opengis.net/gml/3.2\">\n" +
            "  <gml:semiMinorAxis uom=\"urn:ogc:def:uom:EPSG::9001\">6371000.0</gml:semiMinorAxis>\n" +
            "</gml:SecondDefiningParameter>";

    /**
     * Generates a XML tree using the annotations on the {@link SecondDefiningParameter} class.
     *
     * @throws JAXBException If an error occurred during the marshalling process.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final DefaultEllipsoid ellipsoid = DefaultEllipsoid.createEllipsoid(Collections.singletonMap(
                DefaultEllipsoid.NAME_KEY, "Sphere"), 6371000, 6371000, SI.METRE);
        final SecondDefiningParameter sdp = new SecondDefiningParameter(ellipsoid, false);
        assertXmlEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<gml:SecondDefiningParameter xmlns:gml=\"http://www.opengis.net/gml/3.2\">\n" +
                "  <gml:semiMinorAxis uom=\"urn:ogc:def:uom:EPSG::9001\">6371000.0</gml:semiMinorAxis>\n" +
                "</gml:SecondDefiningParameter>",
                marshal(sdp), "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Creates a {@link SecondDefiningParameter} from a XML tree.
     *
     * @throws JAXBException If an error occurred during the unmarshalling process.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final SecondDefiningParameter sdp = unmarshal(SecondDefiningParameter.class, XML);
        assertEquals(6371000.0, sdp.measure.value, 0);
        assertEquals(SI.METRE,  sdp.measure.unit);
    }
}
