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
package org.apache.sis.metadata.iso.identification;

import javax.xml.bind.JAXBException;
import org.opengis.util.NameFactory;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.internal.geoapi.evolution.UnsupportedCodeList;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.xml.NilReason;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singleton;


/**
 * Tests {@link DefaultServiceIdentification}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    DefaultCoupledResourceTest.class,
    org.apache.sis.metadata.iso.identification.DefaultDataIdentificationTest.class
})
public final strictfp class DefaultServiceIdentificationTest extends XMLTestCase {
    /**
     * An XML file in this package containing a service identification.
     */
    private static final String XML_FILE = "ServiceIdentification.xml";

    /**
     * Creates the service identification to use for testing purpose.
     */
    private static DefaultServiceIdentification create() {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final DefaultCoupledResource resource = DefaultCoupledResourceTest.create();
        final DefaultServiceIdentification id = new DefaultServiceIdentification(
                factory.createGenericName(null, "Web Map Server"),      // serviceType
                NilReason.MISSING.createNilObject(Citation.class),      // citation
                "A dummy service for testing purpose.");                // abstract
        id.setServiceTypeVersions(singleton("1.0"));
        id.setCoupledResources(singleton(resource));
        id.setCouplingType(UnsupportedCodeList.valueOf("LOOSE"));
        id.setContainsOperations(singleton(resource.getOperation()));
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
        assertTrue(create().equals(unmarshalFile(DefaultServiceIdentification.class, XML_FILE), ComparisonMode.DEBUG));
    }
}
