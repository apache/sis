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
package org.apache.sis.internal.jaxb.gco;

import java.util.UUID;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.xml.XLink;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.test.mock.ReferenceResolverMock;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Test {@link PropertyType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(org.apache.sis.internal.jaxb.IdentifierMapAdapterTest.class)
public final strictfp class PropertyTypeTest extends XMLTestCase {
    /**
     * The pseudo-metadata object to wrap for testing purpose.
     */
    private final PropertyTypeMock.Value metadata = new PropertyTypeMock.Value();

    /**
     * Simulates the marshalling of the {@link #metadata} value.
     */
    private PropertyTypeMock marshal() {
        /*
         * The 'new PropertyTypeMock()' below is for creating the adapter, not the wrapper. The wrapper is created
         * by the 'marshal' call. This is because our PropertyType implementations are both adapters and wrappers.
         */
        return new PropertyTypeMock().marshal(metadata);
    }

    /**
     * Creates a dummy XLink.
     */
    private static XLink createXLink() {
        final XLink link = new XLink();
        link.setShow(XLink.Show.REPLACE);
        link.setActuate(XLink.Actuate.ON_LOAD);
        link.setTitle(new SimpleInternationalString("myResult"));
        return link;
    }

    /**
     * Tests the construction of a plain property (no identifier).
     *
     * @throws Exception Should never happen.
     */
    @Test
    public void testWithNoReference() throws Exception {
        final PropertyTypeMock property = marshal();
        assertSame(metadata, property.metadata);
        assertNoReference(property);
    }

    /**
     * Asserts that the given property has no UUID, XLink or other references.
     */
    private static void assertNoReference(final PropertyType<?,?> property) {
        assertNull ("nilReason", property.getNilReason());
        assertNull ("uuidref",   property.getUUIDREF());
        assertNull ("href",      property.getHRef());
        assertNull ("role",      property.getRole());
        assertNull ("arcrole",   property.getArcRole());
        assertNull ("title",     property.getTitle());
        assertNull ("show",      property.getShow());
        assertNull ("actuate",   property.getActuate());
        assertFalse("skip",      property.skip());
    }

    /**
     * Tests the construction of an object containing {@code UUID} and {@code XLink} references,
     * but in a context where the user didn't gave us the authorization to use them.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @DependsOnMethod({"testWithUUID", "testWithXLink"})
    public void testWithDiscardedReferences() throws Exception {
        final UUID  uuid = UUID.randomUUID();
        final XLink link = createXLink();
        metadata.getIdentifierMap().putSpecialized(IdentifierSpace.UUID,  uuid);
        metadata.getIdentifierMap().putSpecialized(IdentifierSpace.XLINK, link);
        final PropertyTypeMock property = marshal();
        assertSame(metadata, property.metadata);
        assertNoReference(property);
    }

    /**
     * Tests the construction of an object containing a {@link UUID}.
     * The {@code XLink} is allowed to replace the object definition in the XML to be marshalled.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @DependsOnMethod("testWithNoReference")
    public void testWithUUID() throws Exception {
        final UUID uuid = UUID.randomUUID();
        metadata.getIdentifierMap().putSpecialized(IdentifierSpace.UUID, uuid);
        context = ReferenceResolverMock.begin(true);
        final PropertyTypeMock property = marshal();
        assertSame  (metadata,        property.metadata);
        assertNull  ("nilReason",     property.getNilReason());
        assertEquals(uuid.toString(), property.getUUIDREF());
        assertNull  ("href",          property.getHRef());
        assertNull  ("role",          property.getRole());
        assertNull  ("arcrole",       property.getArcRole());
        assertNull  ("title",         property.getTitle());
        assertNull  ("show",          property.getShow());
        assertNull  ("actuate",       property.getActuate());
        assertTrue  ("skip",          property.skip());
    }

    /**
     * Tests the construction of an object containing a {@link XLink}.
     * The {@code XLink} is allowed to replace the object definition in the XML to be marshalled.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @DependsOnMethod("testWithNoReference")
    public void testWithXLink() throws Exception {
        final XLink link = createXLink();
        metadata.getIdentifierMap().putSpecialized(IdentifierSpace.XLINK, link);
        context = ReferenceResolverMock.begin(true);
        final PropertyTypeMock property = marshal();
        assertSame  (metadata,              property.metadata);
        assertNull  ("nilReason",           property.getNilReason());
        assertNull  ("uuidref",             property.getUUIDREF());
        assertNull  ("href",                property.getHRef());
        assertNull  ("role",                property.getRole());
        assertNull  ("arcrole",             property.getArcRole());
        assertEquals("myResult",            property.getTitle());
        assertEquals(XLink.Show.REPLACE,    property.getShow());
        assertEquals(XLink.Actuate.ON_LOAD, property.getActuate());
        assertTrue  ("skip",                property.skip());
    }
}
