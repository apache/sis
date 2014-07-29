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
package org.apache.sis.metadata.iso.service;

import javax.xml.bind.JAXBException;
import org.opengis.util.TypeName;
import org.opengis.util.MemberName;
import org.opengis.metadata.service.ParameterDirection;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.Namespaces;
import org.junit.Test;

import static org.apache.sis.internal.system.DefaultFactories.SIS_NAMES;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultParameter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class DefaultParameterTest extends XMLTestCase {
    /**
     * Creates the parameter to use for testing purpose.
     */
    static DefaultParameter create() {
        final TypeName   valueType = SIS_NAMES.createTypeName(null, "CharacterString");
        final MemberName paramName = SIS_NAMES.createMemberName(null, "Version", valueType);
        final DefaultParameter param = new DefaultParameter(paramName, true, false);
        assertSame("valueType", valueType, param.getValueType());
        param.setDirection(ParameterDirection.IN);
        return param;
    }

    /**
     * Tests {@link DefaultParameter#getOptionalityLabel()} and {@link DefaultParameter#setOptionalityLabel(String)}.
     */
    @Test
    public void testOptionalityLabel() {
        final DefaultParameter param = create();
        assertEquals("Optional", param.getOptionalityLabel());

        param.setOptionality(false);
        assertEquals("Mandatory", param.getOptionalityLabel());

        param.setOptionality(null);
        assertNull(param.getOptionalityLabel());

        param.setOptionalityLabel("Optional");
        assertTrue(param.getOptionality());

        param.setOptionalityLabel("Mandatory");
        assertFalse(param.getOptionality());
    }

    /**
     * Tests marshalling of an empty parameter. The main purpose is to ensure that
     * the XML does not contains spurious elements like empty enumeration wrapper.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshalEmpty() throws JAXBException {
        final String xml = marshal(new DefaultParameter());
        assertXmlEquals("<srv:SV_Parameter xmlns:srv=\"" + Namespaces.SRV + "\"/>", xml, "xlmns:*");
    }
}
