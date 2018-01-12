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
import org.opengis.metadata.identification.RepresentativeFraction;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.util.Version;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultRepresentativeFraction}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.4
 * @module
 */
public final strictfp class DefaultRepresentativeFractionTest extends XMLTestCase {
    /**
     * {@code false} if testing ISO 19115-3 document, or {@code true} if testing ISO 19139:2007 document.
     */
    private boolean legacy;

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
            version  = LegacyNamespaces.ISO_19139;
        } else {
            version  = LegacyNamespaces.ISO_19115_3;
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
        final DefaultRepresentativeFraction fraction = new DefaultRepresentativeFraction();
        assertEquals("Initial value", 0L, fraction.getDenominator());
        assertTrue("Initial value", Double.isNaN(fraction.doubleValue()));

        fraction.setScale(0.25);
        assertEquals("getDenominator()", 4L, fraction.getDenominator());
        assertEquals("doubleValue()", 0.25, fraction.doubleValue(), 0.0);
    }

    /**
     * Tests XML marshalling using ISO 19115-3 schema.
     * This XML fragment contains an identifier.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final DefaultRepresentativeFraction fraction = new DefaultRepresentativeFraction(8);
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
    @DependsOnMethod("testMarshalling")
    public void testMarshallingLegacy() throws JAXBException {
        legacy = true;
        testMarshalling();
    }

    /**
     * Tests indirectly {@link DefaultRepresentativeFraction#freeze()}.
     * This method verifies that a call to {@link DefaultResolution#freeze()}
     * implies a call to {@link DefaultRepresentativeFraction#freeze()}.
     *
     * @since 0.7
     */
    @Test
    public void testFreeze() {
        final DefaultRepresentativeFraction fraction = new DefaultRepresentativeFraction(1000);
        final DefaultResolution resolution = new DefaultResolution(fraction);
        resolution.freeze();
        final DefaultRepresentativeFraction clone = (DefaultRepresentativeFraction) resolution.getEquivalentScale();
        assertEquals ("Fraction should have the same value.",      fraction, clone);
        assertNotSame("Should have copied the fraction instance.", fraction, clone);
        try {
            clone.setDenominator(10);
            fail("Shall not be allowed to modify an unmodifiable fraction.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
        }
    }
}
