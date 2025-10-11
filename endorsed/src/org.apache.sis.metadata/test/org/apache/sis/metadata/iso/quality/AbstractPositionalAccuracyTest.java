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
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.util.InternationalString;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.xml.bind.lan.FreeTextMarshallingTest;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link AbstractPositionalAccuracy}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@SuppressWarnings("exports")
public final class AbstractPositionalAccuracyTest extends TestUsingFile {
    /**
     * Creates a new test case.
     */
    public AbstractPositionalAccuracyTest() {
    }

    /**
     * Opens the stream to the XML file containing quality information.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final Format format) {
        return format.openTestFile("PositionalAccuracy.xml");
    }

    /**
     * Tests the (un)marshalling of a text group with a default {@code <gco:CharacterString>} element.
     * This test is somewhat a duplicate of {@link FreeTextMarshallingTest}, but the context is more
     * elaborated.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     *
     * @see FreeTextMarshallingTest
     */
    @Test
    public void testXML() throws JAXBException {
        roundtrip(Format.XML2016);
    }

    /**
     * Tests the (un)marshalling of a text group from/to legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during the during marshalling / unmarshalling processes.
     */
    @Test
    public void testLegacyXML() throws JAXBException {
        roundtrip(Format.XML2007);
    }

    /**
     * Unmarshals the given file and verify the content.
     * Then marshals the object and verify that we get equivalent XML.
     */
    @SuppressWarnings("deprecation")
    private void roundtrip(final Format format) throws JAXBException {
        final AbstractElement metadata = unmarshalFile(AbstractElement.class, openTestFile(format));
        final InternationalString nameOfMeasure = assertSingleton(metadata.getNamesOfMeasure());
        /*
         * Programmatic verification of the text group.
         */
        assertEquals("Name of a measure used for testing accuracy", nameOfMeasure.toString(Locale.ENGLISH));
        assertEquals("Nom d'une mesure utilisée pour tester la précision", nameOfMeasure.toString(Locale.FRENCH));
        assertEquals("An identifier", metadata.getMeasureIdentification().getCode());
        /*
         * Opportunist test. While it was not the purpose of this test, the above metadata
         * needs to contain a "result" element in order to pass XML validation test.
         */
        final Result result = assertSingleton(metadata.getResults());
        assertInstanceOf(DefaultConformanceResult.class, result, "Wrong value for <gmd:result>");
        assertEquals(Boolean.TRUE, ((ConformanceResult) result).pass(), "result.pass");
        /*
         * Marshalling: ensure that we didn't lost any information.
         */
        assertMarshalEqualsFile(openTestFile(format), metadata, format.schemaVersion,
                                "xmlns:*", "xsi:schemaLocation", "xsi:type");
    }
}
