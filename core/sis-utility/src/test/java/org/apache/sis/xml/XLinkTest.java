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
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link XLink}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class XLinkTest extends TestCase {
    /**
     * Tests the automatic {@link XLink#getType()} detection.
     *
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testGetType() throws URISyntaxException {
        final XLink link = new XLink();
        int hashCode = link.hashCode();
        assertFalse(hashCode == 0);
        assertNull(link.getType());

        link.setType(XLink.Type.AUTO);
        assertEquals(XLink.Type.TITLE, link.getType());
        assertEquals("XLink[type=\"title\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);

        link.setRole(new URI("org:apache:sis:role"));
        assertEquals(XLink.Type.EXTENDED, link.getType());
        assertEquals("XLink[type=\"extended\", role=\"org:apache:sis:role\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);

        link.setTitle(new SimpleInternationalString("Some title"));
        assertEquals(XLink.Type.EXTENDED, link.getType());
        assertEquals("XLink[type=\"extended\", role=\"org:apache:sis:role\", title=\"Some title\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);

        link.setLabel("SomeLabel");
        assertEquals(XLink.Type.RESOURCE, link.getType());
        assertEquals("XLink[type=\"resource\", role=\"org:apache:sis:role\", title=\"Some title\", label=\"SomeLabel\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);

        link.setHRef(new URI("org:apache:sis:href"));
        assertEquals(XLink.Type.LOCATOR, link.getType());
        assertEquals("XLink[type=\"locator\", href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", label=\"SomeLabel\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);

        link.setShow(XLink.Show.NEW);
        assertNull("Can't be Type.SIMPLE if a label is defined.", link.getType());
        assertEquals("XLink[href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", show=\"new\", label=\"SomeLabel\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);

        link.setLabel(null);
        assertEquals(XLink.Type.SIMPLE, link.getType());
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", show=\"new\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);

        link.setActuate(XLink.Actuate.ON_LOAD);
        assertEquals(XLink.Type.SIMPLE, link.getType());
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\", role=\"org:apache:sis:role\", title=\"Some title\", show=\"new\", actuate=\"onLoad\"]", link.toString());
        assertFalse("Hash code should have changed.", hashCode == (hashCode = link.hashCode()));
        assertFalse("Hash code can not be zero.", hashCode == 0);
        /*
         * Now freezes the XLink and ensures that it is really immutable.
         */
        link.freeze();
        assertEquals("hashCode", hashCode, link.hashCode());
        try {
            link.setType(null);
            fail("The XLink should be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("XLink"));
        }
    }

    /**
     * Tests write operation, which should not be allowed for some type of link.
     *
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testWrite() throws URISyntaxException {
        final XLink link = new XLink();
        link.setType(XLink.Type.SIMPLE);
        link.setHRef(new URI("org:apache:sis:href"));
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\"]", link.toString());
        try {
            link.setLabel("SomeLabel");
            fail("Should not be allowed to set the label.");
        } catch (IllegalStateException e) {
            // This is the expected exception. The message should contains the type name.
            assertTrue(e.getMessage().contains("label"));
            assertTrue(e.getMessage().contains("simple"));
        }
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\"]", link.toString());
        try {
            link.setType(XLink.Type.EXTENDED);
            fail("Should not be allowed to set a type that does not include HREF.");
        } catch (IllegalStateException e) {
            // This is the expected exception. The message should contains the type name.
            assertTrue(e.getMessage().contains("extended"));
        }
        assertEquals("XLink[type=\"simple\", href=\"org:apache:sis:href\"]", link.toString());
        /*
         * The Locator type contains the HREF attribute, so the following operation should be
         * allowed.
         */
        link.setType(XLink.Type.LOCATOR);
        assertEquals("XLink[type=\"locator\", href=\"org:apache:sis:href\"]", link.toString());
        /*
         * Now freezes the XLink and ensures that it is really immutable.
         */
        link.freeze();
        try {
            link.setHRef(null);
            fail("The XLink should be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("XLink"));
        }
    }

    /**
     * Tests equality.
     *
     * @throws URISyntaxException Should never happen.
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
        assertFalse(link.hashCode() == other.hashCode());

        other.setType(XLink.Type.AUTO);
        assertFalse(link.equals(other));
        assertFalse(link.hashCode() == other.hashCode());

        other.setRole(new URI("org:apache:sis:role"));
        assertFalse(link.equals(other));
        assertFalse(link.hashCode() == other.hashCode());

        other.setTitle(new SimpleInternationalString("Some title"));
        assertEquals(link, other);
        assertEquals(link.hashCode(), other.hashCode());

        other.freeze();
        assertEquals(link, other);
        assertEquals(link.hashCode(), other.hashCode());
    }
}
