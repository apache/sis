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
import org.opengis.util.InternationalString;
import org.apache.sis.xml.FreeTextMarshallingTest;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.opengis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link AbstractPositionalAccuracy}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@DependsOn(FreeTextMarshallingTest.class)
public final strictfp class AbstractPositionalAccuracyTest extends XMLTestCase {
    /**
     * An XML file in this package containing a positional accuracy definition.
     */
    private static final String XML_FILE = "PositionalAccuracy.xml";

    /**
     * Tests the (un)marshalling of a text group with a default {@code <gco:CharacterString>} element.
     * This test is somewhat a duplicate of {@link FreeTextMarshallingTest}, but the context is more
     * elaborated.
     *
     * <p><b>XML test file:</b>
     * {@code "core/sis-metadata/src/test/resources/org/apache/sis/metadata/iso/quality/PositionalAccuracy.xml"}</p>
     *
     * @throws JAXBException If an error occurred during the during marshalling / unmarshalling processes.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-107">GEOTK-107</a>
     * @see FreeTextMarshallingTest
     */
    @Test
    public void testXML() throws JAXBException {
        final AbstractElement metadata = unmarshalFile(AbstractElement.class, XML_FILE);
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
        assertInstanceOf("Wrong value for <gmd:result>", DefaultConformanceResult.class,
                getSingleton(metadata.getResults()));
        /*
         * Marshalling: ensure that we didn't lost any information.
         */
        assertMarshalEqualsFile(XML_FILE, metadata, "xmlns:*", "xsi:schemaLocation", "xsi:type");
    }
}
