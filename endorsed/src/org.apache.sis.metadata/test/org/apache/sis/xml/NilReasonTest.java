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

import java.net.URI;
import java.net.URISyntaxException;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArraysExt;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.opengis.metadata.citation.ResponsibleParty;


/**
 * Tests {@link NilReason}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class NilReasonTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NilReasonTest() {
    }

    /**
     * Verifies {@link NilReason#ordinal()} values.
     */
    @Test
    public void verifyOrdinal() {
        assertEquals(3, NilReason.TEMPLATE    .ordinal());
        assertEquals(2, NilReason.MISSING     .ordinal());
        assertEquals(1, NilReason.INAPPLICABLE.ordinal());
        assertEquals(4, NilReason.UNKNOWN     .ordinal());
        assertEquals(5, NilReason.WITHHELD    .ordinal());
    }

    /**
     * Tests the {@link NilReason#valueOf(String)} method on constants.
     *
     * @throws URISyntaxException if a test URI cannot be parsed (should not happen).
     */
    @Test
    public void testValueOfConstant() throws URISyntaxException {
        assertSame(NilReason.TEMPLATE, NilReason.valueOf("template"));
        assertSame(NilReason.MISSING,  NilReason.valueOf("missing"));
        assertSame(NilReason.TEMPLATE, NilReason.valueOf("TEMPLATE"));
        assertSame(NilReason.MISSING,  NilReason.valueOf("  missing "));

        final NilReason[] reasons = NilReason.values();
        assertTrue(ArraysExt.contains(reasons, NilReason.TEMPLATE));
        assertTrue(ArraysExt.contains(reasons, NilReason.MISSING));
    }

    /**
     * Tests the {@link NilReason#valueOf(String)} method on "other".
     *
     * @throws URISyntaxException if a test URI cannot be parsed (should not happen).
     */
    @Test
    public void testValueOfOther() throws URISyntaxException {
        assertSame(NilReason.OTHER, NilReason.valueOf("other"));
        final NilReason other = NilReason.valueOf("other:myReason");
        assertSame(other, NilReason.valueOf("  OTHER : myReason "));
        assertNotSame  (NilReason.OTHER, other, "Expected a new instance.");
        assertNotEquals(NilReason.OTHER, other);
        assertEquals("myReason", other.getOtherExplanation());
        assertNull(other.getURI(), "NilReason.getURI()");

        final NilReason[] reasons = NilReason.values();
        assertTrue(ArraysExt.contains(reasons, NilReason.TEMPLATE));
        assertTrue(ArraysExt.contains(reasons, NilReason.MISSING));
        assertTrue(ArraysExt.contains(reasons, other));
    }

    /**
     * Tests the {@link NilReason#valueOf(String)} method on a URI.
     *
     * @throws URISyntaxException if a test URI cannot be parsed (should not happen).
     */
    @Test
    public void testValueOfURI() throws URISyntaxException {
        final NilReason other = NilReason.valueOf("http://www.nilreasons.org");
        assertSame(other, NilReason.valueOf("  http://www.nilreasons.org  "));
        assertNull(other.getOtherExplanation());
        assertEquals("http://www.nilreasons.org", String.valueOf(other.getURI()));

        final NilReason[] reasons = NilReason.values();
        assertTrue(ArraysExt.contains(reasons, NilReason.TEMPLATE));
        assertTrue(ArraysExt.contains(reasons, NilReason.MISSING));
        assertTrue(ArraysExt.contains(reasons, other));
    }

    /**
     * Tests {@link NilReason#createNilObject(Class)} for a float type.
     * Opportunistically tests {@link NilReason#forObject(Object)} with the created object.
     */
    @Test
    public void testCreateNilFloat() {
        final Float nan  = Float.NaN;
        final Float value = NilReason.MISSING.createNilObject(Float.class);
        assertEquals (nan, value);
        assertNotSame(nan, value);
        assertSame(NilReason.MISSING, NilReason.forObject(value));
        assertNull(NilReason.forObject(nan));
        assertNull(NilReason.forObject(0f));
        assertSame(value, NilReason.MISSING.createNilObject(Float.class), "Expected cached value.");
    }

    /**
     * Tests {@link NilReason#createNilObject(Class)} for a double type.
     * Opportunistically tests {@link NilReason#forObject(Object)} with the created object.
     */
    @Test
    public void testCreateNilDouble() {
        final Double nan  = Double.NaN;
        final Double value = NilReason.TEMPLATE.createNilObject(Double.class);
        assertEquals (nan, value);
        assertNotSame(nan, value);
        assertSame(NilReason.TEMPLATE, NilReason.forObject(value));
        assertNull(NilReason.forObject(nan));
        assertNull(NilReason.forObject(0.0));
        assertSame(value, NilReason.TEMPLATE.createNilObject(Double.class), "Expected cached value.");
    }

    /**
     * Tests {@link NilReason#createNilObject(Class)} for a string type.
     * Opportunistically tests {@link NilReason#forObject(Object)} with the created object.
     */
    @Test
    public void testCreateNilString() {
        final String value = NilReason.MISSING.createNilObject(String.class);
        assertEquals ("", value);
        assertNotSame("", value);
        assertSame(NilReason.MISSING, NilReason.forObject(value));
        assertNull(NilReason.forObject(""));
        assertNull(NilReason.forObject("null"));
        assertSame(value, NilReason.MISSING.createNilObject(String.class), "Expected cached value.");
    }

    /**
     * Tests {@link NilReason#createNilObject(Class)} for an international string type.
     * Opportunistically tests {@link NilReason#forObject(Object)} with the created object.
     */
    @Test
    public void testCreateNilInternationalString() {
        final InternationalString value = NilReason.MISSING.createNilObject(InternationalString.class);
        assertEquals("", value.toString());
        assertInstanceOf(NilObject.class, value);
        assertSame(NilReason.MISSING, NilReason.forObject(value));
        assertSame(value, NilReason.MISSING.createNilObject(InternationalString.class), "Expected cached value.");
    }

    /**
     * Tests {@link NilReason#createNilObject(Class)} for an URI.
     */
    @Test
    public void testCreateNilURI() {
        final URI value = NilReason.MISSING.createNilObject(URI.class);
        assertEquals("", value.toString());
        assertSame(NilReason.MISSING, NilReason.forObject(value));
        assertSame(value, NilReason.MISSING.createNilObject(URI.class), "Expected cached value.");
    }

    /**
     * Tests the creation of {@link NilObject} instances.
     */
    @Test
    public void testCreateNilObject() {
        final Citation citation = NilReason.TEMPLATE.createNilObject(Citation.class);
        assertInstanceOf(NilObject.class, citation);
        assertNull(citation.getTitle());
        assertTrue(citation.getDates().isEmpty());
        assertEquals("Citation[template]", citation.toString());
        assertSame(NilReason.TEMPLATE, NilReason.forObject(citation));
        assertSame(citation, NilReason.TEMPLATE.createNilObject(Citation.class), "Expected cached value.");
    }

    /**
     * Tests the comparison of {@link NilObject} instances.
     */
    @Test
    public void testNilObjectComparison() {
        final Citation e1 = NilReason.TEMPLATE.createNilObject(Citation.class);
        final Citation e2 = NilReason.MISSING .createNilObject(Citation.class);
        final Citation e3 = NilReason.TEMPLATE.createNilObject(Citation.class);
        assertEquals(e1.hashCode(), e3.hashCode());
        assertFalse (e1.hashCode() == e2.hashCode());
        assertEquals(e1, e3);
        assertFalse (e1.equals(e2));

        assertInstanceOf(LenientComparable.class, e1);
        final LenientComparable c = (LenientComparable) e1;
        assertTrue (c.equals(e3, ComparisonMode.STRICT));
        assertFalse(c.equals(e2, ComparisonMode.STRICT));
        assertFalse(c.equals(e2, ComparisonMode.BY_CONTRACT));
        assertTrue (c.equals(e2, ComparisonMode.IGNORE_METADATA));
        assertTrue (c.equals(e2, ComparisonMode.APPROXIMATE));
        assertTrue (c.equals(e2, ComparisonMode.DEBUG));

        // Following object should alway be different because it does not implement the same interface.
        final ResponsibleParty r1 = NilReason.TEMPLATE.createNilObject(ResponsibleParty.class);
        assertFalse(c.equals(r1, ComparisonMode.STRICT));
        assertFalse(c.equals(r1, ComparisonMode.BY_CONTRACT));
        assertFalse(c.equals(r1, ComparisonMode.IGNORE_METADATA));
        assertFalse(c.equals(r1, ComparisonMode.APPROXIMATE));
        assertFalse(c.equals(r1, ComparisonMode.DEBUG));
    }
}
