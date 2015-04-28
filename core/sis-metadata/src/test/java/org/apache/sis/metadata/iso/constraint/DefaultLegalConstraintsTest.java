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

import java.util.logging.LogRecord;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.constraint.Restriction;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link DefaultLegalConstraints}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
public final strictfp class DefaultLegalConstraintsTest extends XMLTestCase implements WarningListener<Object> {
    /**
     * The resource key for the message of the warning that occurred while unmarshalling a XML fragment,
     * or {@code null} if none.
     */
    private Object resourceKey;

    /**
     * The parameter of the warning that occurred while unmarshalling a XML fragment, or {@code null} if none.
     */
    private Object[] parameters;

    /**
     * For internal {@code DefaultLegalConstraints} usage.
     *
     * @return {@code Object.class}.
     */
    @Override
    public Class<Object> getSourceClass() {
        return Object.class;
    }

    /**
     * Invoked when a warning occurred while unmarshalling a test XML fragment. This method ensures that no other
     * warning occurred before this method call (i.e. each test is allowed to cause at most one warning), then
     * remember the warning parameters for verification by the test method.
     *
     * @param source  Ignored.
     * @param warning The warning.
     */
    @Override
    public void warningOccured(final Object source, final LogRecord warning) {
        assertNull(resourceKey);
        assertNull(parameters);
        assertNotNull(resourceKey = warning.getMessage());
        assertNotNull(parameters  = warning.getParameters());
    }

    /**
     * Unmarshalls the given XML fragment.
     */
    private DefaultLegalConstraints unmarshal(final String xml) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        unmarshaller.setProperty(XML.WARNING_LISTENER, this);
        final Object c = unmarshal(unmarshaller, xml);
        pool.recycle(unmarshaller);
        return (DefaultLegalConstraints) c;
    }

    /**
     * Tests unmarshalling of an element containing an empty {@code codeListValue} attribute.
     * This was used to cause a {@code NullPointerException} prior SIS-157 fix.
     *
     * @throws JAXBException If an error occurred during the during unmarshalling processes.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-157">SIS-157</a>
     */
    @Test
    public void testUnmarshallEmptyCodeListValue() throws JAXBException {
        final DefaultLegalConstraints c = unmarshal(
                "<gmd:MD_LegalConstraints xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:accessConstraints>\n" +
                "    <gmd:MD_RestrictionCode codeListValue=\"intellectualPropertyRights\" codeList=\"http://www.isotc211.org/2005/resources/codeList.xml#MD_RestrictionCode\"/>\n" +
                "  </gmd:accessConstraints>\n" +
                "  <gmd:useConstraints>\n" + // Below is an intentionally empty code list value (SIS-157)
                "    <gmd:MD_RestrictionCode codeListValue=\"\" codeList=\"http://www.isotc211.org/2005/resources/codeList.xml#MD_RestrictionCode\"/>\n" +
                "  </gmd:useConstraints>\n" +
                "</gmd:MD_LegalConstraints>");
        /*
         * Verify metadata property.
         */
        assertEquals("accessConstraints", Restriction.INTELLECTUAL_PROPERTY_RIGHTS, getSingleton(c.getAccessConstraints()));
        assertTrue("useConstraints", c.getUseConstraints().isEmpty());
        /*
         * Verify warning message emitted during unmarshalling.
         */
        assertEquals("warning", "NullCollectionElement_1", resourceKey);
        assertArrayEquals("warning", new String[] {"CodeListSet<Restriction>"}, parameters);
    }

    /**
     * Tests (un)marshalling of a XML fragment containing the {@link Restriction#LICENSE} code.
     * The spelling changed between ISO 19115:2003 and 19115:2014, from "license" to "licence".
     * We need to ensure that XML marshalling use the old spelling, until the XML schema is updated.
     *
     * @throws JAXBException If an error occurred during the during unmarshalling processes.
     */
    @Test
    public void testLicenceCode() throws JAXBException {
        final String xml =
                "<gmd:MD_LegalConstraints xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:useConstraints>\n" +
                "    <gmd:MD_RestrictionCode"
                        + " codeList=\"http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#MD_RestrictionCode\""
                        + " codeListValue=\"license\">License</gmd:MD_RestrictionCode>\n" +
                "  </gmd:useConstraints>\n" +
                "</gmd:MD_LegalConstraints>\n";

        final DefaultLegalConstraints c = new DefaultLegalConstraints();
        c.setUseConstraints(singleton(Restriction.LICENSE));
        assertXmlEquals(xml, marshal(c), "xmlns:*");
        /*
         * Unmarshall and ensure that we got back the original LICENCE code, not a new "LICENSE" code.
         */
        final DefaultLegalConstraints actual = unmarshal(xml);
        assertSame(Restriction.LICENSE, getSingleton(actual.getUseConstraints()));
        assertEquals(c, actual);
    }
}
