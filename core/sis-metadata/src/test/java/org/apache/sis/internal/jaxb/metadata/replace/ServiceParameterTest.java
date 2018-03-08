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
package org.apache.sis.internal.jaxb.metadata.replace;

import javax.xml.bind.JAXBException;
import org.opengis.util.MemberName;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.iso.Names;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link ServiceParameter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.5
 * @module
 */
public final strictfp class ServiceParameterTest extends XMLTestCase {
    /**
     * Creates the parameter to use for testing purpose.
     *
     * @return the test parameter.
     */
    public static ServiceParameter create() {
        final MemberName paramName = Names.createMemberName(null, null, "Version", String.class);
        final ServiceParameter param = new ServiceParameter();
        param.memberName    = paramName;
        param.optionality   = true;
        param.repeatability = false;
        return param;
    }

    /**
     * Tests {@link ServiceParameter#getName()}.
     */
    @Test
    public void testGetName() {
        final ServiceParameter param = create();
        assertEquals("name", "Version", String.valueOf(param.getName()));
        assertEquals("valueClass", String.class, param.getValueClass());
    }

    /**
     * Tests {@link ServiceParameter#getValueType()} and {@link ServiceParameter#getValueClass()}.
     */
    @Test
    public void testGetValueType() {
        final ServiceParameter param = create();
        assertEquals("valueType", "OGC:CharacterString", param.getValueType().toFullyQualifiedName().toString());
        assertEquals("valueClass", String.class, param.getValueClass());
    }

    /**
     * Tests {@link ServiceParameter#getOptionalityLabel()} and {@link ServiceParameter#setOptionalityLabel(String)}.
     */
    @Test
    public void testOptionalityLabel() {
        final ServiceParameter param = create();
        assertEquals("Optional", param.getOptionalityLabel());

        param.optionality = false;
        assertEquals("Mandatory", param.getOptionalityLabel());

        param.setOptionalityLabel("Optional");
        assertTrue(param.optionality);

        param.setOptionalityLabel("Mandatory");
        assertFalse(param.optionality);
    }

    /**
     * Tests marshalling of an almost empty parameter (except for default mandatory values).
     * The main purpose is to ensure that the XML does not contains spurious elements like
     * empty enumeration wrapper. For testing a complete marshalling,
     * see {@link org.apache.sis.metadata.iso.identification.DefaultServiceIdentificationTest}.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshalEmpty() throws JAXBException {
        final String xml = marshal(new ServiceParameter());
        assertXmlEquals(
                "<srv:SV_Parameter xmlns:srv=\"" + Namespaces.SRV + '"' +
                                 " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <srv:optionality>\n" +
                "    <gco:Boolean>false</gco:Boolean>\n" +
                "  </srv:optionality>\n" +
                "  <srv:repeatability>\n" +
                "    <gco:Boolean>false</gco:Boolean>\n" +
                "  </srv:repeatability>\n" +
                "</srv:SV_Parameter>\n", xml, "xmlns:*");
    }
}
