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

import java.util.Locale;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.quality.Result;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.jaxb.lan.FreeTextMarshallingTest;
import org.apache.sis.util.Version;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.test.DependsOn;
import org.junit.Ignore;
import org.junit.Test;

import static org.opengis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link AbstractPositionalAccuracy}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.3
 * @module
 */
@DependsOn(FreeTextMarshallingTest.class)
public final strictfp class AbstractPositionalAccuracyTest extends TestUsingFile {
    /**
     * An XML file containing quality information.
     */
    private static final String FILENAME = "PositionalAccuracy.xml";

    /**
     * Tests the (un)marshalling of a text group with a default {@code <gco:CharacterString>} element.
     * This test is somewhat a duplicate of {@link FreeTextMarshallingTest}, but the context is more
     * elaborated.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-394">Issue SIS-394</a>
     * @see FreeTextMarshallingTest
     */
    @Test
    @Ignore("Depends on SIS-394")
    public void testXML() throws JAXBException {
        roundtrip(XML2016+FILENAME, VERSION_2014);
    }

    /**
     * Tests the (un)marshalling of a text group from/to legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testLegacyXML() throws JAXBException {
        roundtrip(XML2007+FILENAME, VERSION_2007);
    }

    /**
     * Unmarshals the given file and verify the content.
     * Then marshals the object and verify that we get equivalent XML.
     */
    private void roundtrip(final String filename, final Version version) throws JAXBException {
        final AbstractElement metadata = unmarshalFile(AbstractElement.class, filename);
        final InternationalString nameOfMeasure = getSingleton(metadata.getNamesOfMeasure());
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
        final Result result = getSingleton(metadata.getResults());
        assertInstanceOf("Wrong value for <gmd:result>", DefaultConformanceResult.class, result);
        assertEquals("result.pass", Boolean.TRUE, ((DefaultConformanceResult) result).pass());
        /*
         * Marshalling: ensure that we didn't lost any information.
         */
        assertMarshalEqualsFile(filename, metadata, version, "xmlns:*", "xsi:schemaLocation", "xsi:type");
    }
}
