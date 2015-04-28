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
package org.apache.sis.metadata.iso.lineage;

import javax.xml.bind.JAXBException;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefaultProcessStep}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final strictfp class DefaultProcessStepTest extends XMLTestCase {
    /**
     * An XML file in this package containing a process step definition.
     */
    private static final String XML_FILE = "ProcessStep.xml";

    /**
     * Tests the (un)marshalling of a metadata mixing elements from ISO 19115 and ISO 19115-2 standards.
     *
     * <p><b>XML test file:</b>
     * {@code "core/sis-metadata/src/test/resources/org/apache/sis/metadata/iso/lineage/ProcessStep.xml"}</p>
     *
     * @throws JAXBException If an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultProcessing  processing  = new DefaultProcessing();
        final DefaultProcessStep processStep = new DefaultProcessStep("Some process step.");
        processing.setProcedureDescription(new SimpleInternationalString("Some procedure."));
        processStep.setProcessingInformation(processing);
        /*
         * XML marshalling, and compare with the content of "ProcessStep.xml" file.
         */
        assertMarshalEqualsFile(XML_FILE, processStep, "xlmns:*", "xsi:schemaLocation");
        /*
         * XML unmarshalling: ensure that we didn't lost any information.
         */
        assertEquals(processStep, unmarshalFile(DefaultProcessStep.class, XML_FILE));
    }
}
