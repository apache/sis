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
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.NilReason;

// Test dependencies
import org.junit.Test;
import org.apache.sis.xml.test.TestCase;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link DefaultConformanceResult}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultConformanceResultTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultConformanceResultTest() {
    }

    /**
     * Tests marshalling and unmarshalling of the given result.
     *
     * @param  result  the result to test.
     * @param  xml     the expected XML
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    private void testXML(final DefaultConformanceResult result, final String xml) throws JAXBException {
        result.setExplanation(new SimpleInternationalString("A result"));
        assertMarshalEquals(xml, result);
        assertEquals(result, unmarshal(DefaultConformanceResult.class, xml));
    }

    /**
     * Tests (un)marshalling of an XML document with a result.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testXML() throws JAXBException {
        final var result = new DefaultConformanceResult();
        result.setPass(true);
        assertNull(result.nilReasons().get("pass"));
        testXML(result,
                "<mdq:DQ_ConformanceResult xmlns:mdq=\"" + Namespaces.MDQ + '"'
                                       + " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mdq:explanation>\n" +
                "    <gco:CharacterString>A result</gco:CharacterString>\n" +
                "  </mdq:explanation>\n" +
                "  <mdq:pass>\n" +
                "    <gco:Boolean>true</gco:Boolean>\n" +
                "  </mdq:pass>\n" +
                "</mdq:DQ_ConformanceResult>");
    }

    /**
     * Tests (un)marshalling of an XML document with a result missing for an unknown reason.
     * At marshalling time, the nil reason of mandatory properties should default to {@link NilReason#UNKNOWN}.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testUnknownReason() throws JAXBException {
        final var result = new DefaultConformanceResult();
        assertNull(result.nilReasons().get("pass"));
        testXML(result,
                "<mdq:DQ_ConformanceResult xmlns:mdq=\"" + Namespaces.MDQ + '"'
                                       + " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mdq:explanation>\n" +
                "    <gco:CharacterString>A result</gco:CharacterString>\n" +
                "  </mdq:explanation>\n" +
                "  <mdq:pass gco:nilReason=\"unknown\"/>\n" +
                "</mdq:DQ_ConformanceResult>");
    }

    /**
     * Tests (un)marshalling of an XML document with a result missing because the XML is a template.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testTemplateReason() throws JAXBException {
        final var result = new DefaultConformanceResult();
        result.setPass(true);
        result.nilReasons().put("pass", NilReason.TEMPLATE);
        assertNull(result.pass());
        testXML(result,
                "<mdq:DQ_ConformanceResult xmlns:mdq=\"" + Namespaces.MDQ + '"'
                                       + " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mdq:explanation>\n" +
                "    <gco:CharacterString>A result</gco:CharacterString>\n" +
                "  </mdq:explanation>\n" +
                "  <mdq:pass gco:nilReason=\"template\"/>\n" +
                "</mdq:DQ_ConformanceResult>");
    }
}
