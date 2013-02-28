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
package org.apache.sis.metadata;

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.internal.simple.SimpleCitation;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link PropertyDescriptor}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class PropertyDescriptorTest extends TestCase {
    /**
     * Creates a property descriptor instance for the given method of the {@link Citation} interface.
     *
     * @param  elementType The type of elements returned by the given method.
     * @param  method      The name of the method.
     * @param  property    The ISO 19115 name of the property.
     * @throws NoSuchMethodException If the {@code method} name is invalid.
     */
    private static <T> PropertyDescriptor<T> create(final Class<T> elementType, final String method,
            final String property) throws NoSuchMethodException
    {
        return new PropertyDescriptor<>(elementType, new SimpleCitation("ISO 19115"), property,
                Citation.class.getMethod(method));
    }

    /**
     * Tests the properties of {@link Citation#getTitle()}.
     * The element type is an {@link InternationalString} singleton, which is mandatory.
     *
     * @throws NoSuchMethodException Should never happen.
     */
    @Test
    public void testTitle() throws NoSuchMethodException {
        final PropertyDescriptor<?> descriptor = create(InternationalString.class, "getTitle", "title");
        assertEquals("ISO 19115",   descriptor.getName().getAuthority().getTitle().toString());
        assertEquals("CI_Citation", descriptor.getCodeSpace());
        assertEquals("title",       descriptor.getCode());
        final InternationalString remarks = descriptor.getRemarks();
        assertEquals("Name by which the cited resource is known.", remarks.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertEquals(InternationalString.class, descriptor.getValueClass());
        assertEquals(1, descriptor.getMinimumOccurs());
        assertEquals(1, descriptor.getMaximumOccurs());
    }

    /**
     * Tests the properties of {@link Citation#getPresentationForms()}.
     * The element type is {@link PresentationForm} in a collection.
     *
     * @throws NoSuchMethodException Should never happen.
     */
    @Test
    public void testPresentationForm() throws NoSuchMethodException {
        final PropertyDescriptor<?> descriptor = create(PresentationForm.class, "getPresentationForms", "presentationForm");
        assertEquals("ISO 19115",        descriptor.getName().getAuthority().getTitle().toString());
        assertEquals("CI_Citation",      descriptor.getCodeSpace());
        assertEquals("presentationForm", descriptor.getCode());
        final InternationalString remarks = descriptor.getRemarks();
        assertEquals("Mode in which the resource is represented.", remarks.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertEquals(PresentationForm.class, descriptor.getValueClass());
        assertEquals(0, descriptor.getMinimumOccurs());
        assertEquals(Integer.MAX_VALUE, descriptor.getMaximumOccurs());
    }
}
