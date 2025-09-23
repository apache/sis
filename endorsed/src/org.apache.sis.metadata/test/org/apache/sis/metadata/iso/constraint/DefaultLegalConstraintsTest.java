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
package org.apache.sis.metadata.iso.constraint;

import java.util.Set;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.constraint.Restriction;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.ISO_NAMESPACE;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link DefaultLegalConstraints}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
public final class DefaultLegalConstraintsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultLegalConstraintsTest() {
    }

    /**
     * Tests unmarshalling of an element containing an empty {@code codeListValue} attribute.
     * This was used to cause a {@code NullPointerException} prior SIS-157 fix.
     *
     * @throws JAXBException if an error occurred during the during unmarshalling processes.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-157">SIS-157</a>
     */
    @Test
    public void testUnmarshallEmptyCodeListValue() throws JAXBException {
        final DefaultLegalConstraints c = unmarshal(DefaultLegalConstraints.class,
                "<mco:MD_LegalConstraints xmlns:mco=\"" + Namespaces.MCO + "\">\n" +
                "  <mco:accessConstraints>\n" +
                "    <mco:MD_RestrictionCode codeListValue=\"intellectualPropertyRights\" codeList=\"" + ISO_NAMESPACE + "19115/resources/Codelist/cat/codelists.xml#MD_RestrictionCode\"/>\n" +
                "  </mco:accessConstraints>\n" +
                "  <mco:useConstraints>\n" +            // Below is an intentionally empty code list value (SIS-157)
                "    <mco:MD_RestrictionCode codeListValue=\"\" codeList=\"" + ISO_NAMESPACE + "19115/resources/Codelist/cat/codelists.xml#MD_RestrictionCode\"/>\n" +
                "  </mco:useConstraints>\n" +
                "</mco:MD_LegalConstraints>");
        /*
         * Verify metadata property.
         */
        assertEquals(Restriction.INTELLECTUAL_PROPERTY_RIGHTS, getSingleton(c.getAccessConstraints()));
        assertTrue(c.getUseConstraints().isEmpty());
    }

    /**
     * Tests (un)marshalling of a XML fragment containing the {@link Restriction#LICENSE} code.
     * The spelling changed between ISO 19115:2003 and 19115:2014, from "license" to "licence".
     * We need to ensure that XML marshalling use the old spelling, until the XML schema is updated.
     *
     * @throws JAXBException if an error occurred during the during unmarshalling processes.
     */
    @Test
    public void testLicenceCode() throws JAXBException {
        String xml =
                "<mco:MD_LegalConstraints xmlns:mco=\"" + Namespaces.MCO + "\">\n" +
                "  <mco:useConstraints>\n" +
                "    <mco:MD_RestrictionCode"
                        + " codeList=\"" + ISO_NAMESPACE + "19115/resources/Codelist/cat/codelists.xml#MD_RestrictionCode\""
                        + " codeListValue=\"licence\">License</mco:MD_RestrictionCode>\n" +
                "  </mco:useConstraints>\n" +
                "</mco:MD_LegalConstraints>\n";

        final DefaultLegalConstraints c = new DefaultLegalConstraints();
        c.setUseConstraints(Set.of(Restriction.LICENSE));
        assertXmlEquals(xml, marshal(c), "xmlns:*");
        DefaultLegalConstraints actual = unmarshal(DefaultLegalConstraints.class, xml);
        assertSame(Restriction.LICENSE, getSingleton(actual.getUseConstraints()));
        assertEquals(c, actual);
        /*
         * Above code tested ISO 19115-3 (un)marshalling. Code below test legacy ISO 19139:2007 (un)marshalling.
         * This is where the spelling difference appears. At unmarshalling, verify that we got back the original
         * LICENCE code, not a new "LICENSE" code.
         */
        xml  =  "<gmd:MD_LegalConstraints xmlns:gmd=\"" + LegacyNamespaces.GMD + "\">\n" +
                "  <gmd:useConstraints>\n" +
                "    <gmd:MD_RestrictionCode"
                        + " codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_RestrictionCode\""
                        + " codeListValue=\"license\">License</gmd:MD_RestrictionCode>\n" +
                "  </gmd:useConstraints>\n" +
                "</gmd:MD_LegalConstraints>\n";

        assertXmlEquals(xml, marshal(c, VERSION_2007), "xmlns:*");
        actual = unmarshal(DefaultLegalConstraints.class, xml);
        assertSame(Restriction.LICENSE, getSingleton(actual.getUseConstraints()));
        assertEquals(c, actual);
    }
}
