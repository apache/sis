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

import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.lineage.Source;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.DefaultIdentifier;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.xml.test.TestCase;


/**
 * Tests {@link DefaultLineage}. This include testing the XML marshalling of objects in the
 * {@code "gmi"} namespace that GeoAPI merged with the object of same name in the {@code "gmd"} namespace.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultLineageTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultLineageTest() {
    }

    /**
     * Create a lineage to marshal. If {@code extension} is {@code false}, then this method uses
     * only properties defined in ISO 19115-1. If {@code extension} is {@code true}, then this
     * method adds an ISO 19115-2 property.
     */
    private static DefaultLineage create(final boolean extension) {
        final DefaultLineage lineage = new DefaultLineage();
        final DefaultSource source = new DefaultSource();
        source.setDescription(new SimpleInternationalString("Description of source data level."));
        lineage.getSources().add(source);
        if (extension) {
            source.setProcessedLevel(new DefaultIdentifier("DummyLevel"));
        }
        return lineage;
    }

    /**
     * Verifies the unmarshalling result.
     */
    private static void verify(final DefaultLineage lineage) {
        final Source source = assertSingleton(lineage.getSources());
        assertEquals("Description of source data level.", String.valueOf(source.getDescription()));
    }

    /**
     * Tests the marshalling of a {@code "mrl:LI_Source"} element.
     * If this case, the test uses only ISO 19115-1 elements (no ISO 19115-2).
     * Consequently, the XML name shall be {@code "mrl:LI_Source"}.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testSource() throws JAXBException {
        String actual = marshal(create(false));
        assertXmlEquals(
            "<mrl:LI_Lineage xmlns:mrl=\"" + Namespaces.MRL + '"' +
                           " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <mrl:source>\n" +
            "    <mrl:LI_Source>\n" +
            "      <mrl:description>\n" +
            "        <gco:CharacterString>Description of source data level.</gco:CharacterString>\n" +
            "      </mrl:description>\n" +
            "    </mrl:LI_Source>\n" +
            "  </mrl:source>\n" +
            "</mrl:LI_Lineage>", actual, "xmlns:*");

        verify(unmarshal(DefaultLineage.class, actual));
    }

    /**
     * Tests the marshalling of a legacy {@code "gmd:LI_Source"} element.
     * If this case, the test uses only ISO 19115-1 elements (no ISO 19115-2).
     * Consequently, the legacy XML name shall be {@code "gmd:LI_Source"}.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testLegacySource() throws JAXBException {
        String actual = marshal(create(false), VERSION_2007);
        assertXmlEquals(
            "<gmd:LI_Lineage xmlns:gmd=\"" + LegacyNamespaces.GMD + '"' +
                           " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
            "  <gmd:source>\n" +
            "    <gmd:LI_Source>\n" +
            "      <gmd:description>\n" +
            "        <gco:CharacterString>Description of source data level.</gco:CharacterString>\n" +
            "      </gmd:description>\n" +
            "    </gmd:LI_Source>\n" +
            "  </gmd:source>\n" +
            "</gmd:LI_Lineage>", actual, "xmlns:*");

        verify(unmarshal(DefaultLineage.class, actual));
    }

    /**
     * Tests the marshalling of a {@code "mrl:LE_Source"} element.
     * This test starts with the same metadata as {@link #testSource()} and adds an
     * ISO 19115-2 specific property. Consequently, the XML name, which was originally
     * {@code "mrl:LI_Source"}, shall become {@code "mrl:LE_Source"}.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testSourceImagery() throws JAXBException {
        String actual = marshal(create(true));
        assertXmlEquals(
            "<mrl:LI_Lineage xmlns:mrl=\"" + Namespaces.MRL + '"' +
                           " xmlns:mcc=\"" + Namespaces.MCC + '"' +
                           " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <mrl:source>\n" +
            "    <mrl:LE_Source>\n" +
            "      <mrl:description>\n" +
            "        <gco:CharacterString>Description of source data level.</gco:CharacterString>\n" +
            "      </mrl:description>\n" +
            "      <mrl:processedLevel>\n" +
            "        <mcc:MD_Identifier>\n" +
            "          <mcc:code>\n" +
            "            <gco:CharacterString>DummyLevel</gco:CharacterString>\n" +
            "          </mcc:code>\n" +
            "        </mcc:MD_Identifier>\n" +
            "      </mrl:processedLevel>\n" +
            "    </mrl:LE_Source>\n" +
            "  </mrl:source>\n" +
            "</mrl:LI_Lineage>", actual, "xmlns:*");

        verify(unmarshal(DefaultLineage.class, actual));
    }

    /**
     * Tests the marshalling of a legacy {@code "gmi:LE_Source"} element.
     * This test starts with the same metadata as {@link #testLegacySource()} and adds
     * an ISO 19115-2 specific property. Consequently, the XML name, which was originally
     * {@code "gmd:LI_Source"}, shall become {@code "gmi:LE_Source"}.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testLegacySourceImagery() throws JAXBException {
        String actual = marshal(create(true), VERSION_2007);
        assertXmlEquals(
            "<gmd:LI_Lineage xmlns:gmd=\"" + LegacyNamespaces.GMD + '"' +
                           " xmlns:gmi=\"" + LegacyNamespaces.GMI + '"' +
                           " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
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

        verify(unmarshal(DefaultLineage.class, actual));
    }
}
