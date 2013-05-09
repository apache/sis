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

import java.net.URISyntaxException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.test.TestCase;
import org.junit.*;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link NilReason}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 */
public final strictfp class NilReasonTest extends TestCase {
    /**
     * Tests the {@link NilReason#valueOf(String)} method on constants.
     *
     * @throws URISyntaxException Should never happen.
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
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testValueOfOther() throws URISyntaxException {
        assertSame(NilReason.OTHER, NilReason.valueOf("other"));
        final NilReason other = NilReason.valueOf("other:myReason");
        assertSame(other, NilReason.valueOf("  OTHER : myReason "));
        assertNotSame("Expected a new instance.", NilReason.OTHER, other);
        assertFalse  ("NilReason.equals(Object)", NilReason.OTHER.equals(other));
        assertEquals ("NilReason.getOtherExplanation()", "myReason", other.getOtherExplanation());
        assertNull   ("NilReason.getURI()", other.getURI());

        final NilReason[] reasons = NilReason.values();
        assertTrue(ArraysExt.contains(reasons, NilReason.TEMPLATE));
        assertTrue(ArraysExt.contains(reasons, NilReason.MISSING));
        assertTrue(ArraysExt.contains(reasons, other));
    }

    /**
     * Tests the {@link NilReason#valueOf(String)} method on a URI.
     *
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testValueOfURI() throws URISyntaxException {
        final NilReason other = NilReason.valueOf("http://www.nilreasons.org");
        assertSame(other, NilReason.valueOf("  http://www.nilreasons.org  "));
        assertNull  ("NilReason.getOtherExplanation()", other.getOtherExplanation());
        assertEquals("NilReason.getURI()", "http://www.nilreasons.org", String.valueOf(other.getURI()));

        final NilReason[] reasons = NilReason.values();
        assertTrue(ArraysExt.contains(reasons, NilReason.TEMPLATE));
        assertTrue(ArraysExt.contains(reasons, NilReason.MISSING));
        assertTrue(ArraysExt.contains(reasons, other));
    }

    /**
     * Tests the creation of {@link NilObject} instances.
     */
    @Test
    public void testCreateNilObject() {
        final Citation citation = NilReason.TEMPLATE.createNilObject(Citation.class);
        assertInstanceOf("Unexpected proxy.", NilObject.class, citation);
        assertNull(citation.getTitle());
        assertTrue(citation.getDates().isEmpty());
        assertEquals("NilObject.toString()", "Citation[template]", citation.toString());
    }

    /**
     * Tests the comparison of {@link NilObject} instances.
     */
    @Test
    public void testNilObjectComparison() {
        final Citation e1 = NilReason.TEMPLATE.createNilObject(Citation.class);
        final Citation e2 = NilReason.MISSING .createNilObject(Citation.class);
        final Citation e3 = NilReason.TEMPLATE.createNilObject(Citation.class);
        assertEquals("NilObject.hashCode()", e1.hashCode(), e3.hashCode());
        assertFalse ("NilObject.hashCode()", e1.hashCode() == e2.hashCode());
        assertEquals("NilObject.equals(Object)", e1, e3);
        assertFalse ("NilObject.equals(Object)", e1.equals(e2));

        assertInstanceOf("e1", LenientComparable.class, e1);
        final LenientComparable c = (LenientComparable) e1;
        assertTrue (c.equals(e3, ComparisonMode.STRICT));
        assertFalse(c.equals(e2, ComparisonMode.STRICT));
        assertFalse(c.equals(e2, ComparisonMode.BY_CONTRACT));
        assertTrue (c.equals(e2, ComparisonMode.IGNORE_METADATA));
        assertTrue (c.equals(e2, ComparisonMode.APPROXIMATIVE));
        assertTrue (c.equals(e2, ComparisonMode.DEBUG));

        // Following object should alway be different because it does not implement the same interface.
        final ResponsibleParty r1 = NilReason.TEMPLATE.createNilObject(ResponsibleParty.class);
        assertFalse(c.equals(r1, ComparisonMode.STRICT));
        assertFalse(c.equals(r1, ComparisonMode.BY_CONTRACT));
        assertFalse(c.equals(r1, ComparisonMode.IGNORE_METADATA));
        assertFalse(c.equals(r1, ComparisonMode.APPROXIMATIVE));
        assertFalse(c.equals(r1, ComparisonMode.DEBUG));
    }
}
