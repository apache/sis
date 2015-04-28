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
package org.apache.sis.metadata.iso.lineage;

import java.util.Arrays;
import javax.xml.bind.JAXBException;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultLineage}. This include testing the XML marshalling of objects in the
 * {@code "gmi"} namespace that GeoAPI merged with the object of same name in the {@code "gmd"} namespace.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final strictfp class DefaultLineageTest extends XMLTestCase {
    /**
     * Tests the marshalling of an {@code "gmd:LI_Source"} element, which shall become
     * {@code "gmi:LE_Source"} when some ISO 19115-2 properties are defined.
     *
     * @throws JAXBException If an error occurred while marshalling the XML.
     */
    @Test
    public void testSource() throws JAXBException {
        final DefaultLineage lineage = new DefaultLineage();
        final DefaultSource source = new DefaultSource();
        source.setDescription(new SimpleInternationalString("Description of source data level."));
        lineage.setSources(Arrays.asList(source));
        /*
         * If this simpler case, only ISO 19115 elements are defined (no ISO 19115-2).
         * Consequently the XML name shall be "gmd:LI_Source".
         */
        String actual = XML.marshal(lineage);
        assertXmlEquals(
            "<gmd:LI_Lineage xmlns:gmd=\"" + Namespaces.GMD + '"' +
                           " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <gmd:source>\n" +
            "    <gmd:LI_Source>\n" +
            "      <gmd:description>\n" +
            "        <gco:CharacterString>Description of source data level.</gco:CharacterString>\n" +
            "      </gmd:description>\n" +
            "    </gmd:LI_Source>\n" +
            "  </gmd:source>\n" +
            "</gmd:LI_Lineage>", actual, "xmlns:*");
        /*
         * Now add a ISO 19115-2 specific property. The XML name shall become "gmi:LE_Source".
         */
        source.setProcessedLevel(new DefaultIdentifier("DummyLevel"));
        actual = XML.marshal(lineage);
        assertXmlEquals(
            "<gmd:LI_Lineage xmlns:gmd=\"" + Namespaces.GMD + '"' +
                           " xmlns:gmi=\"" + Namespaces.GMI + '"' +
                           " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <gmd:source>\n" +
            "    <gmi:LE_Source>\n" +
            "      <gmd:description>\n" +
            "        <gco:CharacterString>Description of source data level.</gco:CharacterString>\n" +
            "      </gmd:description>\n" +
            "      <gmi:processedLevel>\n" +
            "        <gmd:MD_Identifier>\n" +
            "          <gmd:code>\n" +
            "            <gco:CharacterString>DummyLevel</gco:CharacterString>\n" +
            "          </gmd:code>\n" +
            "        </gmd:MD_Identifier>\n" +
            "      </gmi:processedLevel>\n" +
            "    </gmi:LE_Source>\n" +
            "  </gmd:source>\n" +
            "</gmd:LI_Lineage>", actual, "xmlns:*");
    }
}
