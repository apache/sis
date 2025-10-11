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
package org.apache.sis.xml.bind.metadata.replace;

import jakarta.xml.bind.JAXBException;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.Names;
import org.apache.sis.metadata.iso.quality.DefaultMeasureDescription;
import org.apache.sis.xml.Namespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;
import org.opengis.referencing.operation.Matrix;
import org.opengis.metadata.quality.ValueStructure;


/**
 * Tests {@link QualityParameter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class QualityParameterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public QualityParameterTest() {
    }

    /**
     * Creates the parameter to use for testing purpose.
     *
     * @return the test parameter.
     */
    public static QualityParameter create() {
        final QualityParameter param = new QualityParameter();
        param.code           = "some parameter";
        param.definition     = new SimpleInternationalString("a definition");
        param.description    = new DefaultMeasureDescription("a description");
        param.valueStructure = ValueStructure.MATRIX;
        param.valueType      = Names.createTypeName(Integer.class);
        return param;
    }

    /**
     * Tests {@link QualityParameter#getName()}.
     */
    @Test
    public void testGetName() {
        final QualityParameter param = create();
        final Identifier name = param.getName();
        assertNull  (name.getCodeSpace());
        assertEquals("some parameter", name.getCode());
        assertEquals("a definition",   name.getDescription().toString());
        assertEquals("a description",  param.getDescription().orElseThrow().toString());
    }

    /**
     * Tests {@link QualityParameter#getValueType()} and {@link QualityParameter#getValueClass()}.
     */
    @Test
    public void testGetValueType() {
        final QualityParameter param = create();
        assertEquals(Matrix.class, param.getValueClass());
        assertEquals("OGC:Integer", param.getValueType().toFullyQualifiedName().toString());
    }

    /**
     * Tests marshalling of a parameter.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshal() throws JAXBException {
        final String xml = marshal(create());
        assertXmlEquals(
                "<dqm:DQM_Parameter xmlns:dqm=\"" + Namespaces.DQM + '"' +
                                 " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <dqm:name>\n" +
                "    <gco:CharacterString>some parameter</gco:CharacterString>\n" +
                "  </dqm:name>\n" +
                "  <dqm:definition>\n" +
                "    <gco:CharacterString>a definition</gco:CharacterString>\n" +
                "  </dqm:definition>\n" +
                "  <dqm:description>\n" +
                "    <dqm:DQM_Description>\n" +
                "      <dqm:textDescription>\n" +
                "        <gco:CharacterString>a description</gco:CharacterString>\n" +
                "      </dqm:textDescription>\n" +
                "    </dqm:DQM_Description>\n" +
                "  </dqm:description>\n" +
                "  <dqm:valueType>\n" +
                "    <gco:TypeName>\n" +
                "      <gco:aName>\n" +
                "        <gco:CharacterString>Integer</gco:CharacterString>\n" +
                "      </gco:aName>\n" +
                "    </gco:TypeName>\n" +
                "  </dqm:valueType>\n" +
                "  <dqm:valueStructure>\n" +
                "    <dqm:DQM_ValueStructure codeList=\"http://standards.iso.org/iso/19115/resources/Codelist/cat/codelists.xml#DQM_ValueStructure\""
                                         + " codeListValue=\"matrix\">Matrix</dqm:DQM_ValueStructure>\n" +
                "  </dqm:valueStructure>\n" +
                "</dqm:DQM_Parameter>\n", xml, "xmlns:*");
    }
}
