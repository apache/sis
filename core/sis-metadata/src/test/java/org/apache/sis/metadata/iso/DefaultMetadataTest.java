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

import javax.xml.bind.JAXBException;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests XML (un)marshalling of various metadata objects.
 * For every metadata objects tested by this class, the expected XML representation
 * is provided by {@code *.xml} files <a href="{@scmUrl metadata}/">here</a>.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.4
 * @module
 */
public final strictfp class DefaultMetadataTest extends XMLTestCase {
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
        final String xml =
                "<gmd:MD_Metadata xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:contact/>\n" +
                "</gmd:MD_Metadata>";
        final DefaultMetadata metadata = (DefaultMetadata) XML.unmarshal(xml);
        assertTrue(metadata.getContacts().isEmpty());
    }
}
