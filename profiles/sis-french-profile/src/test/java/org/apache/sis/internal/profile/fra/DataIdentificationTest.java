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
package org.apache.sis.internal.profile.fra;

import javax.xml.bind.JAXBException;
import org.apache.sis.xml.XML;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests (un)marshalling of French profile of data identification.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.4
 * @since   0.4
 * @module
 */
public final strictfp class DataIdentificationTest extends TestCase {
    /**
     * Tests marshalling and unmarshalling of a XML fragment.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final String xml =
                "<fra:FRA_DataIdentification xmlns:gmd=\"http://www.isotc211.org/2005/gmd\"" +
                                           " xmlns:gco=\"http://www.isotc211.org/2005/gco\"" +
                                           " xmlns:fra=\"http://www.cnig.gouv.fr/2005/fra\">\n" +
                "  <gmd:citation>\n" +
                "    <gmd:CI_Citation>\n" +
                "      <gmd:title>\n" +
                "        <gco:CharacterString>Main documentation.</gco:CharacterString>\n" +
                "      </gmd:title>\n" +
                "    </gmd:CI_Citation>\n" +
                "  </gmd:citation>\n" +
                "  <fra:relatedCitation>\n" +
                "    <gmd:CI_Citation>\n" +
                "      <gmd:title>\n" +
                "        <gco:CharacterString>Related documentation.</gco:CharacterString>\n" +
                "      </gmd:title>\n" +
                "    </gmd:CI_Citation>\n" +
                "  </fra:relatedCitation>\n" +
                "</fra:FRA_DataIdentification>";

        final Object id = XML.unmarshal(xml);
        assertInstanceOf("Expected an AFNOR instance.", DataIdentification.class, id);
        assertTitleEquals("citation", "Main documentation.", ((DataIdentification) id).getCitation());
        assertTitleEquals("relatedCitations", "Related documentation.", getSingleton(((DataIdentification) id).getRelatedCitations()));

        final String actual = XML.marshal(id);
        assertXmlEquals(xml, actual, "xmlns:*");
    }
}
