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
package org.apache.sis.xml;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.net.URL;
import java.io.IOException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.iso.lineage.DefaultProcessing;
import org.apache.sis.metadata.iso.lineage.DefaultProcessStep;
import org.apache.sis.metadata.iso.quality.AbstractElement;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests XML (un)marshalling of various metadata objects.
 * For every metadata objects tested by this class, the expected XML representation
 * is provided by {@code *.xml} files in the <a href="{@scmUrl gmd-data}/">here</a>.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
@DependsOn(FreeTextMarshallingTest.class)
public final strictfp class MetadataMarshallingTest extends XMLTestCase {
    /**
     * A poll of configured {@link Marshaller} and {@link Unmarshaller}, created when first needed.
     */
    private static MarshallerPool pool;

    /**
     * Creates the XML (un)marshaller pool to be shared by all test methods.
     * The (un)marshallers locale and timezone will be set to fixed values.
     *
     * @throws JAXBException If an error occurred while creating the pool.
     *
     * @see #disposeMarshallerPool()
     */
    @BeforeClass
    public static void createMarshallerPool() throws JAXBException {
        final Map<String,Object> properties = new HashMap<String,Object>(4);
        assertNull(properties.put(XML.LOCALE, Locale.ENGLISH));
        assertNull(properties.put(XML.TIMEZONE, "CET"));
        pool = new MarshallerPool(properties);
    }

    /**
     * Invoked by JUnit after the execution of every tests in order to dispose
     * the {@link MarshallerPool} instance used internally by this class.
     */
    @AfterClass
    public static void disposeMarshallerPool() {
        pool = null;
    }

    /**
     * Returns the URL to the XML file of the given name.
     *
     * @param  filename The name of the XML file.
     * @return The URL to the given XML file.
     */
    private URL getResource(final String filename) {
        final URL resource = MetadataMarshallingTest.class.getResource(filename);
        assertNotNull(filename, resource);
        return resource;
    }

    /**
     * Tests the (un)marshalling of a text group with a default {@code <gco:CharacterString>} element.
     * This test is somewhat a duplicate of {@link FreeTextMarshallingTest}, but the context is more
     * elaborated.
     *
     * <p><b>XML test file:</b>
     * <a href="{@scmUrl gmd-data}/PositionalAccuracy.xml">PositionalAccuracy.xml</a></p>
     *
     * @throws IOException   If an error occurred while reading the XML file.
     * @throws JAXBException If an error occurred during the during marshalling / unmarshalling processes.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-107">GEOTK-107</a>
     * @see FreeTextMarshallingTest
     */
    @Test
    public void testPositionalAccuracy() throws IOException, JAXBException {
        final Marshaller   marshaller   = pool.acquireMarshaller();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final URL          resource     = getResource("PositionalAccuracy.xml");
        final Object       metadata     = XML.unmarshal(resource);
        assertInstanceOf("PositionalAccuracy.xml", AbstractElement.class, metadata);
        final InternationalString nameOfMeasure = getSingleton(((AbstractElement) metadata).getNamesOfMeasure());
        /*
         * Programmatic verification of the text group.
         */
        assertEquals("Quantitative quality measure focusing on the effective class percent "
                + "regarded to the total surface size", nameOfMeasure.toString(Locale.ENGLISH));
        assertEquals("Mesure qualité quantitative de type pourcentage de représentation de la "
                + "classe par rapport à la surface totale", nameOfMeasure.toString(Locale.FRENCH));
        /*
         * Opportunist test. While it was not the purpose of this test, the above metadata
         * needs to contain a "result" element in order to pass XML validation test.
         */
        assertInstanceOf("Wrong value for <gmd:result>", DefaultConformanceResult.class,
                getSingleton(((AbstractElement) metadata).getResults()));
        /*
         * Final comparison: ensure that we didn't lost any information.
         */
        assertXmlEquals(resource, marshal(marshaller, metadata), "xmlns:*", "xsi:schemaLocation", "xsi:type");
        pool.recycle(unmarshaller);
        pool.recycle(marshaller);
    }

    /**
     * Tests the (un)marshalling of a metadata mixing elements from ISO 19115 and ISO 19115-2 standards.
     *
     * <p><b>XML test file:</b>
     * <a href="{@scmUrl gmd-data}/ProcessStep.xml">ProcessStep.xml</a></p>
     *
     * @throws IOException   If an error occurred while reading the XML file.
     * @throws JAXBException If an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testProcessStep() throws IOException, JAXBException {
        final Marshaller         marshaller   = pool.acquireMarshaller();
        final Unmarshaller       unmarshaller = pool.acquireUnmarshaller();
        final DefaultProcessing  processing   = new DefaultProcessing();
        final DefaultProcessStep processStep  = new DefaultProcessStep("Some process step.");
        processing.setProcedureDescription(new SimpleInternationalString("Some procedure."));
        processStep.setProcessingInformation(processing);
        /*
         * XML marshalling, and compare with the content of "ProcessStep.xml" file.
         */
        final String xml = marshal(marshaller, processStep);
        assertTrue(xml.startsWith("<?xml"));
        assertXmlEquals(getResource("ProcessStep.xml"), xml, "xmlns:*", "xsi:schemaLocation");
        /*
         * Final comparison: ensure that we didn't lost any information.
         */
        assertEquals(processStep, unmarshal(unmarshaller, xml));
        pool.recycle(unmarshaller);
        pool.recycle(marshaller);
    }
}
