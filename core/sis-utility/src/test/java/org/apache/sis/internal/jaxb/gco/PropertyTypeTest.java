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
import org.apache.sis.xml.ReferenceResolverMock;
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
     * Asserts that the given property has the given UUID value, which may be null.
     * All other properties are expected to be null for this test.
     */
    private static void assertReferenceEqualsUUID(final String uuidref, final PropertyType<?,?> property) {
        assertNull  ("nilReason",          property.getNilReason());
        assertEquals("uuidref",   uuidref, property.getUUIDREF());
        assertNull  ("href",               property.getHRef());
        assertNull  ("role",               property.getRole());
        assertNull  ("arcrole",            property.getArcRole());
        assertNull  ("title",              property.getTitle());
        assertNull  ("show",               property.getShow());
        assertNull  ("actuate",            property.getActuate());
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
        assertReferenceEqualsUUID(null, property);
    }

    /**
     * Tests the construction of an object containing {@code UUID} reference,
     * but in a context where the user didn't gave us the authorization to use it.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @DependsOnMethod("testWithUUID")
    public void testWithDiscardedUUID() throws Exception {
        testWithUUID(false);
    }

    /**
     * Tests the construction of an object containing a {@link UUID}.
     * The {@code UUID} is allowed to replace the object definition in the XML to be marshalled.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @DependsOnMethod("testWithNoReference")
    public void testWithUUID() throws Exception {
        testWithUUID(true);
    }

    /**
     * Implementation of the public {@code test*UUID()} methods.
     *
     * <ul>
     *   <li>If {@code useReferenceResolverMock} is {@code false}, then this test behaves like
     *       {@link #testWithNoReference()} because the default SIS behavior is to not replace
     *       XML elements by their {@code uuidref} attributes, since SIS does not know if the
     *       client who is going to read the XML file will be able to find object definitions
     *       from their UUID.</li>
     *
     *   <li>If {@code useReferenceResolverMock} is {@code true}, then the metadata object shall be replaced
     *       by the UUID because the {@link ReferenceResolverMock#canSubstituteByReference(MarshalContext,
     *       Class, Object, UUID)} method returns {@code true}.</li>
     * </ul>
     */
    private void testWithUUID(final boolean useReferenceResolverMock) throws Exception {
        final UUID uuid = UUID.randomUUID();
        metadata.getIdentifierMap().putSpecialized(IdentifierSpace.UUID, uuid);
        if (useReferenceResolverMock) {
            context = ReferenceResolverMock.begin(true);
            // XMLTestCase.clearContext() will dispose the context.
        }
        final PropertyTypeMock property = marshal();
        if (!useReferenceResolverMock) {
            assertSame(metadata, property.metadata);
            assertReferenceEqualsUUID(null, property);
        } else {
            assertNull("metadata", property.metadata);
            assertReferenceEqualsUUID(uuid.toString(), property);
        }
    }

    /**
     * Tests the construction of an object containing a {@link XLink} while keeping the metadata.
     * The {@code XLink} is provided for information purpose.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @DependsOnMethod("testWithXLink")
    public void testWithInformativeXLink() throws Exception {
        testWithXLink(false);
    }

    /**
     * Tests the construction of an object containing a {@link XLink} replacing the metadata.
     * The {@code XLink} is allowed to replace the object definition in the XML to be marshalled.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @DependsOnMethod("testWithNoReference")
    public void testWithXLink() throws Exception {
        testWithXLink(true);
    }

    /**
     * Implementation of the public {@code test*XLink()} methods.
     *
     * <ul>
     *   <li>If {@code useReferenceResolverMock} is {@code false}, then this test uses the default SIS behavior,
     *       which is to not omit the metadata and still write the XLink for informative purpose.</li>
     *
     *   <li>If {@code useReferenceResolverMock} is {@code true}, then the metadata object shall be replaced
     *       by the XLink because the {@link ReferenceResolverMock#canSubstituteByReference(MarshalContext,
     *       Class, Object, Xlink)} method returns {@code true}.</li>
     * </ul>
     */
    private void testWithXLink(final boolean useReferenceResolverMock) {
        final XLink link = new XLink();
        link.setShow(XLink.Show.REPLACE);
        link.setActuate(XLink.Actuate.ON_LOAD);
        link.setTitle(new SimpleInternationalString("myLinkTitle"));
        metadata.getIdentifierMap().putSpecialized(IdentifierSpace.XLINK, link);
        if (useReferenceResolverMock) {
            context = ReferenceResolverMock.begin(true);
            // XMLTestCase.clearContext() will dispose the context.
        }
        final PropertyTypeMock property = marshal();
        if (!useReferenceResolverMock) {
            assertSame(metadata, property.metadata);
        } else {
            assertNull("metadata", property.metadata);
        }
        assertNull  ("nilReason",                      property.getNilReason());
        assertNull  ("uuidref",                        property.getUUIDREF());
        assertNull  ("href",                           property.getHRef());
        assertNull  ("role",                           property.getRole());
        assertNull  ("arcrole",                        property.getArcRole());
        assertEquals("title",   "myLinkTitle",         property.getTitle());
        assertEquals("show",    XLink.Show.REPLACE,    property.getShow());
        assertEquals("actuate", XLink.Actuate.ON_LOAD, property.getActuate());
    }
}
