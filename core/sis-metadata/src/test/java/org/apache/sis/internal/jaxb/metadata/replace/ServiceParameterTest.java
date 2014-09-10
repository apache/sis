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
import org.opengis.util.TypeName;
import org.opengis.util.MemberName;
import org.opengis.parameter.ParameterDirection;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.Namespaces;
import org.junit.Test;

import static org.apache.sis.internal.system.DefaultFactories.SIS_NAMES;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link ServiceParameter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class ServiceParameterTest extends XMLTestCase {
    /**
     * Creates the parameter to use for testing purpose.
     *
     * @return The test parameter.
     */
    public static ServiceParameter<?> create() {
        final TypeName   valueType = SIS_NAMES.createTypeName(null, "CharacterString");
        final MemberName paramName = SIS_NAMES.createMemberName(null, "Version", valueType);
        final ServiceParameter<?> param = new ServiceParameter();
        param.name          = paramName;
        param.optionality   = true;
        param.repeatability = false;
        param.direction     = ParameterDirection.IN;
        assertSame("valueType", valueType, param.getValueType());
        return param;
    }

    /**
     * Tests {@link ServiceParameter#getOptionality()} and {@link ServiceParameter#setOptionality(String)}.
     */
    @Test
    public void testOptionalityLabel() {
        final ServiceParameter<?> param = create();
        assertEquals("Optional", param.getOptionality());

        param.optionality = false;
        assertEquals("Mandatory", param.getOptionality());

        param.setOptionality("Optional");
        assertTrue(param.optionality);

        param.setOptionality("Mandatory");
        assertFalse(param.optionality);
    }

    /**
     * Tests marshalling of an almost empty parameter (except for default mandatory values).
     * The main purpose is to ensure that the XML does not contains spurious elements like
     * empty enumeration wrapper.
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
                "    <gco:CharacterString>Mandatory</gco:CharacterString>\n" +
                "  </srv:optionality>\n" +
                "  <srv:repeatability>\n" +
                "    <gco:Boolean>false</gco:Boolean>\n" +
                "  </srv:repeatability>\n" +
                "</srv:SV_Parameter>\n", xml, "xlmns:*");
    }
}
