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
package org.apache.sis.style.sld1;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for {@link StyledLayerDescriptor}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class StyledLayerDescriptorTest extends SLDTestCase {
    /**
     * Creates a new test case.
     */
    public StyledLayerDescriptorTest() {
    }

    /**
     * Test of {@code version} property.
     */
    @Test
    public void testVersion() {
        final var cdt = new StyledLayerDescriptor();
        assertEquals(StyledLayerDescriptor.VERSION, cdt.getVersion());
        assertEquals("1.1.0", cdt.getVersion());
    }

    /**
     * Test of {@code Name} property.
     */
    @Test
    public void testName() {
        final var cdt = new StyledLayerDescriptor();

        // Check defaults
        assertEmpty(cdt.getName());

        // Check get/set
        String value = "A random name";
        cdt.setName(value);
        assertOptionalEquals(value, cdt.getName());
    }

    /**
     * Test of {@code Description} property.
     */
    @Test
    public void testDescription() {
        final var cdt = new StyledLayerDescriptor();

        // Check defaults
        assertEmpty(cdt.getDescription());

        // Check get/set
        var desc = anyDescription();
        cdt.setDescription(desc);
        assertOptionalEquals(desc, cdt.getDescription());
    }

    /**
     * Test of {@code UseSLDLibrary} property.
     */
    @Test
    public void testLibraries() {
        final var cdt = new StyledLayerDescriptor();

        // Check defaults
        assertTrue(cdt.libraries().isEmpty());

        // Check get/set
        var value = new UseSLDLibrary();
        value.setOnlineResource(anyOnlineResource());
        cdt.libraries().add(value);
        assertEquals(1, cdt.libraries().size());
        assertSame(value, cdt.libraries().get(0));
    }

    /**
     * Test of {@code NamedLayer} and {@code UserLayer} properties.
     */
    @Test
    public void testLayers() {
        final var cdt = new StyledLayerDescriptor();

        // Check defaults
        assertTrue(cdt.layers().isEmpty());

        // Check get/set
        var named = new NamedLayer();
        var user  = new UserLayer();
        cdt.layers().add(named);
        cdt.layers().add(user);
        assertEquals(2, cdt.layers().size());
        assertSame(named, cdt.layers().get(0));
        assertSame(user,  cdt.layers().get(1));
    }

    /**
     * Tests {@code equals}, {@code hashCode} and deep {@code clone} on a filled descriptor.
     */
    @Test
    public void testEqualsAndClone() {
        final var cdt = new StyledLayerDescriptor();
        cdt.setName("sld");
        cdt.setDescription(anyDescription());
        final var library = new UseSLDLibrary();
        library.setOnlineResource(anyOnlineResource());
        cdt.libraries().add(library);

        final var named = new NamedLayer();
        named.setName("named_layer");
        final var namedStyle = new NamedStyle();
        namedStyle.setName("named_style");
        named.styles().add(namedStyle);
        named.styles().add(anyUserStyle("user_style"));
        cdt.layers().add(named);

        final var user = new UserLayer();
        user.setName("user_layer");
        user.setSource(new RemoteOWS());
        user.styles().add(anyUserStyle("user_style"));
        cdt.layers().add(user);

        // Copy constructor is shallow.
        final var copy = new StyledLayerDescriptor(cdt);
        assertEquals(cdt, copy);
        assertSame(cdt.layers().get(0), copy.layers().get(0));

        // Clone is deep.
        final var clone = cdt.clone();
        assertCloneEquals(cdt, clone);
        assertNotSame(cdt.libraries(), clone.libraries());
        assertNotSame(cdt.layers(),    clone.layers());
        assertNotSame(cdt.getDescription().orElseThrow(), clone.getDescription().orElseThrow());
        assertNotSame(cdt.libraries().get(0), clone.libraries().get(0));
        assertNotSame(cdt.layers().get(0), clone.layers().get(0));
        assertNotSame(cdt.layers().get(1), clone.layers().get(1));

        final var clonedNamed = (NamedLayer) clone.layers().get(0);
        assertNotSame(named.styles().get(0), clonedNamed.styles().get(0));
        assertNotSame(named.styles().get(1), clonedNamed.styles().get(1));
        assertEquals (named.styles().get(1), clonedNamed.styles().get(1));

        // Changes in the clone do not impact the original.
        clone.setName("other");
        clone.layers().clear();
        assertNotEquals(cdt, clone);
        assertOptionalEquals("sld", cdt.getName());
        assertEquals(2, cdt.layers().size());
    }
}
