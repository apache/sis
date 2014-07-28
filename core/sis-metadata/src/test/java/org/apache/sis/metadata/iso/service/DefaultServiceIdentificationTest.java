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
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.service.CouplingType;
import org.opengis.metadata.service.DistributedComputingPlatform;
import org.opengis.metadata.service.ParameterDirection;
import org.apache.sis.xml.NilReason;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singleton;
import static org.apache.sis.internal.system.DefaultFactories.SIS_NAMES;


/**
 * Tests {@link DefaultServiceIdentification}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(org.apache.sis.metadata.iso.identification.DefaultDataIdentificationTest.class)
public final strictfp class DefaultServiceIdentificationTest extends XMLTestCase {
    /**
     * An XML file in this package containing a service identification.
     */
    private static final String XML_FILE = "ServiceIdentification.xml";

    /**
     * Creates the service identification to use for testing purpose.
     */
    private static DefaultServiceIdentification create() {
        final TypeName   valueType = SIS_NAMES.createTypeName(null, "CharacterString");
        final MemberName paramName = SIS_NAMES.createMemberName(null, "Version", valueType);
        final DefaultParameter param = new DefaultParameter(paramName, "Optional", false);
        assertSame("valueType", valueType, param.getValueType());
        param.setDirection(ParameterDirection.IN);

        final DefaultOperationMetadata md = new DefaultOperationMetadata("Get Map",
                DistributedComputingPlatform.WEB_SERVICES, null);
        md.setParameters(singleton(param));
        md.setConnectPoints(singleton(NilReason.MISSING.createNilObject(OnlineResource.class)));

        final DefaultServiceIdentification id = new DefaultServiceIdentification(
                SIS_NAMES.createGenericName(null, "Web Map Server"),    // serviceType
                NilReason.MISSING.createNilObject(Citation.class),      // citation
                "A dummy service for testing purpose.");                // abstract
        id.setServiceTypeVersions(singleton("1.0"));
        id.setCouplingType(CouplingType.LOOSE);
        id.setContainsOperations(singleton(md));
        return id;
    }

    /**
     * Tests the marshalling of a service metadata.
     *
     * @throws JAXBException If an error occurred during the during marshalling process.
     */
    @Test
    public void testMarshal() throws JAXBException {
        assertMarshalEqualsFile(XML_FILE, create(), "xlmns:*", "xsi:schemaLocation");
    }

    /**
     * Tests the unmarshalling of a service metadata.
     *
     * <p><b>XML test file:</b>
     * {@code "core/sis-metadata/src/test/resources/org/apache/sis/metadata/iso/service/ServiceIdentification.xml"}</p>
     *
     * @throws JAXBException If an error occurred during the during unmarshalling process.
     */
    @Test
    public void testUnmarshal() throws JAXBException {
        assertEquals(create(), unmarshalFile(DefaultServiceIdentification.class, XML_FILE));
    }
}
