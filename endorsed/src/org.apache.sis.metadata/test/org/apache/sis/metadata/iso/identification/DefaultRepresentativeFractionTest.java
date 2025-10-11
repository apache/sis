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

import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.identification.RepresentativeFraction;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.util.Version;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;


/**
 * Tests {@link DefaultRepresentativeFraction}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@SuppressWarnings("exports")
public final class DefaultRepresentativeFractionTest extends TestCase {
    /**
     * {@code false} if testing ISO 19115-3 document, or {@code true} if testing ISO 19139:2007 document.
     */
    private boolean legacy;

    /**
     * Creates a new test case.
     */
    public DefaultRepresentativeFractionTest() {
    }

    /**
     * Verifies that marshalling the given metadata produces the expected XML document,
     * then verifies that unmarshalling that document gives back the original metadata object.
     * If {@link #legacy} is {@code true}, then this method will use ISO 19139:2007 schema.
     */
    private void roundtrip(final RepresentativeFraction browse, String expected) throws JAXBException {
        final String  actual;
        final Version version;
        if (legacy) {
            expected = toLegacyXML(expected);
            version  = VERSION_2007;
        } else {
            version  = VERSION_2014;
        }
        actual = marshal(browse, version);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(browse, unmarshal(RepresentativeFraction.class, actual));
    }

    /**
     * Test {@link DefaultRepresentativeFraction#setScale(double)}.
     */
    @Test
    public void testSetScale() {
        final var fraction = new DefaultRepresentativeFraction();
        assertEquals(0L, fraction.getDenominator());
        assertTrue(Double.isNaN(fraction.doubleValue()));

        fraction.setScale(0.25);
        assertEquals(4L, fraction.getDenominator());
        assertEquals(0.25, fraction.doubleValue());
    }

    /**
     * Tests XML marshalling using ISO 19115-3 schema.
     * This XML fragment contains an identifier.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final var fraction = new DefaultRepresentativeFraction(8);
        fraction.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "scale");
        roundtrip(fraction,
                "<mri:MD_RepresentativeFraction xmlns:mri=\"" + Namespaces.MRI + '"' +
                                              " xmlns:gco=\"" + Namespaces.GCO + '"' +
                                              " id=\"scale\">\n" +
                "  <mri:denominator>\n" +
                "    <gco:Integer>8</gco:Integer>\n" +
                "  </mri:denominator>\n" +
                "</mri:MD_RepresentativeFraction>");
    }

    /**
     * Tests XML marshalling using ISO 19139:2007 schema.
     * This XML fragment contains an identifier.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshallingLegacy() throws JAXBException {
        legacy = true;
        testMarshalling();
    }

    /**
     * Tests indirectly {@link DefaultRepresentativeFraction#freeze()}.
     * This method verifies that a call to {@code DefaultResolution.transitionTo(FINAL)}
     * implies a call to {@link DefaultRepresentativeFraction#freeze()}.
     */
    @Test
    public void testFreeze() {
        final var fraction = new DefaultRepresentativeFraction(1000);
        final var resolution = new DefaultResolution(fraction);
        resolution.transitionTo(DefaultResolution.State.FINAL);
        assertSame(fraction, resolution.getEquivalentScale());

        var e = assertThrows(UnsupportedOperationException.class, () -> fraction.setDenominator(10),
                             "Shall not be allowed to modify an unmodifiable fraction.");
        assertNotNull(e);
    }
}
