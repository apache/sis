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
package org.apache.sis.metadata.iso;

import java.util.logging.LogRecord;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests XML (un)marshalling of various metadata objects.
 * For every metadata objects tested by this class, the expected XML representation
 * is provided by {@code *.xml} files in the following directory:
 *
 * <ul>
 *   <li>{@code "core/sis-metadata/src/test/resources/org/apache/sis/metadata/iso"}</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.4
 * @module
 */
public final strictfp class DefaultMetadataTest extends XMLTestCase implements WarningListener<Object> {
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
     * For internal {@code DefaultMetadata} usage.
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
    private DefaultMetadata unmarshal(final String xml) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        unmarshaller.setProperty(XML.WARNING_LISTENER, this);
        final Object c = unmarshal(unmarshaller, xml);
        pool.recycle(unmarshaller);
        return (DefaultMetadata) c;
    }

    /**
     * Tests unmarshalling of a metadata having a collection that contains no element.
     * This was used to cause a {@code NullPointerException} prior SIS-139 fix.
     *
     * @throws JAXBException If an error occurred during the during unmarshalling processes.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-139">SIS-139</a>
     */
    @Test
    public void testEmptyCollection() throws JAXBException {
        final DefaultMetadata metadata = unmarshal(
                "<gmd:MD_Metadata xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:contact/>\n" +
                "</gmd:MD_Metadata>");
        /*
         * Verify metadata property.
         */
        assertTrue(metadata.getContacts().isEmpty());
        /*
         * Verify warning message emitted during unmarshalling.
         */
        assertEquals("warning", "NullCollectionElement_1", resourceKey);
        assertArrayEquals("warning", new String[] {"CheckedArrayList<ResponsibleParty>"}, parameters);
    }
}
