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
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.internal.jaxb.gmi.LE_ProcessStep;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.util.Version;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests {@link DefaultProcessStep}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 */
public final class DefaultProcessStepTest extends TestUsingFile {
    /**
     * An XML file containing process step information.
     */
    private static final String FILENAME = "ProcessStep.xml";

    /**
     * Tests the (un)marshalling of a metadata mixing elements from ISO 19115 and ISO 19115-2 standards.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testXML() throws JAXBException {
        roundtrip(XML2016+FILENAME, VERSION_2014);
    }

    /**
     * Tests the (un)marshalling of a metadata in legacy ISO 19139:2007 document.
     * This test uses the same metadata than {@link #testXML()}.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testLegacyXML() throws JAXBException {
        roundtrip(XML2007+FILENAME, VERSION_2007);
    }

    /**
     * Tests (un)marshalling in the given version.
     */
    private void roundtrip(final String filename, final Version version) throws JAXBException {
        final DefaultProcessing  processing  = new DefaultProcessing();
        final DefaultProcessStep processStep = new DefaultProcessStep("Some process step.");
        processing.setProcedureDescription(new SimpleInternationalString("Some procedure."));
        processing.setIdentifier(new DefaultIdentifier("P4"));
        processStep.setProcessingInformation(processing);
        /*
         * XML marshalling, and compare with the content of "ProcessStep.xml" file.
         */
        assertMarshalEqualsFile(filename, processStep, version, "xmlns:*", "xsi:schemaLocation");
        /*
         * XML unmarshalling: ensure that we didn't lost any information.
         * Note that since the XML uses the <gmi:…> namespace, we got an instance of LE_ProcessStep, which
         * in SIS implementation does not carry any useful information; it is just a consequence of the way
         * namespaces are managed. We will convert to the parent DefaultProcessStep type before comparison.
         */
        DefaultProcessStep step = unmarshalFile(DefaultProcessStep.class, filename);
        assertInstanceOf("The unmarshalled object is expected to be in GMI namespace.", LE_ProcessStep.class, step);
        step = new DefaultProcessStep(step);
        assertEquals(processStep, step);
    }
}
