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
package org.apache.sis.metadata.iso.quality;

import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.maintenance.ScopeCode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;

// Specific to the main branch:
import org.opengis.metadata.quality.Scope;


/**
 * Tests the marshalling of elements containing {@link ScopeCode}.
 * When formatting legacy XML, {@code MD_Scope} should be renamed {@code DQ_Scope}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-508">SIS-508 on issue tracker</a>
 */
@SuppressWarnings("exports")
public final class ScopeCodeTest extends TestCase {
    /**
     * The XML fragment used for testing.
     */
    private static final String XML =
            "<gmd:DQ_DataQuality xmlns:gmd=\"http://www.isotc211.org/2005/gmd\">\n" +
            "  <gmd:scope>\n" +
            "    <gmd:DQ_Scope>\n" +
            "      <gmd:level>\n" +
            "        <gmd:MD_ScopeCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_ScopeCode\""
                              + " codeListValue=\"dataset\" codeSpace=\"eng\">Dataset</gmd:MD_ScopeCode>\n" +
            "      </gmd:level>\n" +
            "    </gmd:DQ_Scope>\n" +
            "  </gmd:scope>\n" +
            "</gmd:DQ_DataQuality>\n";

    /**
     * Creates a new test case.
     */
    public ScopeCodeTest() {
    }

    /**
     * Tests marshalling a small metadata containing a {@link ScopeCode}.
     *
     * @throws JAXBException if an error occurred during XML marshalling.
     */
    @Test
    public void testMarshallingLegacy() throws JAXBException {
        final String actual = marshal(new DefaultDataQuality(ScopeCode.DATASET), VERSION_2007);
        assertXmlEquals(XML, actual, "xmlns:*");
    }

    /**
     * Tests unmarshalling a small metadata containing a {@link ScopeCode}.
     *
     * @throws JAXBException if an error occurred during XML unmarshalling.
     */
    @Test
    public void testUnmarshallingLegacy() throws JAXBException {
        final DefaultDataQuality metadata = unmarshal(DefaultDataQuality.class, XML);
        final Scope scope = metadata.getScope();
        assertNotNull(scope, "scope");
        assertEquals(ScopeCode.DATASET, scope.getLevel());
    }
}
