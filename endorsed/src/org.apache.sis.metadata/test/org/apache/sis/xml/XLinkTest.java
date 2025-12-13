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
import org.apache.sis.util.SimpleInternationalString;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests {@link XLink}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class XLinkTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public XLinkTest() {
    }

    /**
     * Verifies that the hash code is different than the given value.
     *
     * @param  hashCode  the old hash code.
     * @param  link      the XLink to hash.
     * @return the new hash code.
     */
    private static int assertHashCodeChanged(int hashCode, final XLink link) {
        final int newHash = link.hashCode();
        assertNotEquals(hashCode, newHash, "Hash code should have changed.");
        assertNotEquals(0, hashCode, "Hash code cannot be zero.");
        return newHash;
    }

    /**
     * Tests the automatic {@link XLink#getType()} detection.
     *
     * @throws URISyntaxException if a test URI cannot be parsed (should not happen).
     */
    @Test
    public void testGetType() throws URISyntaxException {
        final XLink link = new XLink();
        int hashCode = link.hashCode();
        assertNotEquals(0, hashCode);
        assertNull(link.getType());

        link.setType(XLink.Type.AUTO);
        assertEquals(XLink.Type.TITLE, link.getType());
        assertEquals("XLink[type=\"title\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);

        link.setRole(new URI("org:apache:sis:role"));
        assertEquals(XLink.Type.EXTENDED, link.getType());
        assertEquals("XLink[type=\"extended\", role=\"org:apache:sis:role\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);

        link.setTitle(new SimpleInternationalString("Some title"));
        assertEquals(XLink.Type.EXTENDED, link.getType());
        assertEquals("XLink[type=\"extended\", role=\"org:apache:sis:role\", title=\"Some title\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);

        link.setLabel("SomeLabel");
        assertEquals(XLink.Type.RESOURCE, link.getType());
        assertEquals("XLink[type=\"resource\", role=\"org:apache:sis:role\", title=\"Some title\", label=\"SomeLabel\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);

        link.setHRef(new URI("org:apache:sis:href"));
        assertEquals(XLink.Type.LOCATOR, link.getType());
        assertEquals("XLink[type=\"locator\", href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", label=\"SomeLabel\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);

        link.setShow(XLink.Show.NEW);
        assertNull(link.getType(), "Cannot be Type.SIMPLE if a label is defined.");
        assertEquals("XLink[href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", show=\"new\", label=\"SomeLabel\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);

        link.setLabel(null);
        assertEquals(XLink.Type.SIMPLE, link.getType());
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", show=\"new\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);

        link.setActuate(XLink.Actuate.ON_LOAD);
        assertEquals(XLink.Type.SIMPLE, link.getType());
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", show=\"new\", actuate=\"onLoad\"]", link.toString());
        hashCode = assertHashCodeChanged(hashCode, link);
        /*
         * Now freezes the XLink and ensures that it is really immutable.
         */
        link.freeze();
        assertEquals(hashCode, link.hashCode(), "hashCode");

        var exception = assertThrows(UnsupportedOperationException.class, () -> link.setType(null), "The XLink should be unmodifiable.");
        assertMessageContains(exception, "XLink");
    }

    /**
     * Tests write operation, which should not be allowed for some type of link.
     *
     * @throws URISyntaxException if a test URI cannot be parsed (should not happen).
     */
    @Test
    public void testWrite() throws URISyntaxException {
        final XLink link = new XLink();
        link.setType(XLink.Type.SIMPLE);
        link.setHRef(new URI("org:apache:sis:href"));
        /*
         * Should not be allowed to set the label.
         */
        IllegalStateException exception;
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\"]", link.toString());
        exception = assertThrows(IllegalStateException.class, () -> link.setLabel("SomeLabel"));
        assertMessageContains(exception, "label", "simple");
        /*
         * Should not be allowed to set a type that does not include HREF.
         */
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\"]", link.toString());
        exception = assertThrows(IllegalStateException.class, () -> link.setType(XLink.Type.EXTENDED));
        assertMessageContains(exception, "extended");
        /*
         * The Locator type contains the HREF attribute, so the following operation should be allowed.
         */
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\"]", link.toString());
        link.setType(XLink.Type.LOCATOR);
        assertEquals("XLink[type=\"locator\", href=\"org:apache:sis:href\"]", link.toString());
        /*
         * Now freezes the XLink and ensures that it is really immutable.
         */
        link.freeze();
        var e = assertThrows(UnsupportedOperationException.class, () -> link.setHRef(null));
        assertMessageContains(e, "XLink");
    }

    /**
     * Tests equality.
     *
     * @throws URISyntaxException if a test URI cannot be parsed (should not happen).
     */
    @Test
    public void testEquals() throws URISyntaxException {
        final XLink link = new XLink();
        link.setType(XLink.Type.AUTO);
        link.setRole(new URI("org:apache:sis:role"));
        link.setTitle(new SimpleInternationalString("Some title"));
        link.freeze();

        final XLink other = new XLink();
        assertFalse(link.equals(other));
        assertNotEquals(link.hashCode(), other.hashCode());

        other.setType(XLink.Type.AUTO);
        assertFalse(link.equals(other));
        assertNotEquals(link.hashCode(), other.hashCode());

        other.setRole(new URI("org:apache:sis:role"));
        assertFalse(link.equals(other));
        assertNotEquals(link.hashCode(), other.hashCode());

        other.setTitle(new SimpleInternationalString("Some title"));
        assertTrue(link.equals(other));
        assertEquals(link.hashCode(), other.hashCode());

        other.freeze();
        assertTrue(link.equals(other));
        assertEquals(link.hashCode(), other.hashCode());
    }
}
