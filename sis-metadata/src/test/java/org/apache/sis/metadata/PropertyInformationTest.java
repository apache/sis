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
import org.opengis.metadata.Datatype;
import org.opengis.metadata.Obligation;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.ExtendedElementInformation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link PropertyInformation}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class PropertyInformationTest extends TestCase {
    /**
     * Creates a property information instance for the given method of the {@link Citation} interface.
     *
     * @param  elementType The type of elements returned by the given method.
     * @param  method      The name of the method.
     * @param  property    The ISO 19115 name of the property.
     * @throws NoSuchMethodException If the {@code method} name is invalid.
     */
    private static <T> PropertyInformation<T> create(final Class<T> elementType, final String method,
            final String property) throws NoSuchMethodException
    {
        return new PropertyInformation<>(HardCodedCitations.ISO_19115, property,
                Citation.class.getMethod(method), elementType, null);
    }

    /**
     * Tests the properties of {@link Citation#getTitle()}.
     * The element type is an {@link InternationalString} singleton, which is mandatory.
     *
     * @throws NoSuchMethodException Should never happen.
     */
    @Test
    public void testTitle() throws NoSuchMethodException {
        validateTitle(create(InternationalString.class, "getTitle", "title"));
    }

    /**
     * Validates a property information for {@link Citation#getTitle()}.
     * This is validation code to be shared with {@link PropertyAccessorTest#testInformation()}.
     */
    static void validateTitle(final ExtendedElementInformation information) {
        assertInstanceOf("SIS-specific", Identifier.class, information);
        assertEquals("ISO 19115",   ((Identifier) information).getAuthority().getTitle().toString());
        assertEquals("CI_Citation", getSingleton(information.getParentEntity()));
        assertEquals("title",       information.getName());
        final InternationalString definition = information.getDefinition();
        assertEquals("Name by which the cited resource is known.", definition.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertInstanceOf("SIS-specific", CheckedContainer.class, information);
        assertEquals(InternationalString.class, ((CheckedContainer<?>) information).getElementType());
        assertEquals(Datatype.CHARACTER_STRING, information.getDataType());
        assertEquals(Obligation.MANDATORY, information.getObligation());
        assertEquals(Integer.valueOf(1), information.getMaximumOccurrence());
    }

    /**
     * Tests the properties of {@link Citation#getPresentationForms()}.
     * The element type is {@link PresentationForm} in a collection.
     *
     * @throws NoSuchMethodException Should never happen.
     */
    @Test
    public void testPresentationForm() throws NoSuchMethodException {
        validatePresentationForm(create(PresentationForm.class, "getPresentationForms", "presentationForm"));
    }

    /**
     * Validates information for {@link Citation#getPresentationForms()}.
     * This is validation code to be shared with {@link PropertyAccessorTest#testInformation()}.
     */
    static void validatePresentationForm(final ExtendedElementInformation information) {
        assertInstanceOf("SIS-specific", Identifier.class, information);
        assertEquals("ISO 19115",        ((Identifier) information).getAuthority().getTitle().toString());
        assertEquals("CI_Citation",      getSingleton(information.getParentEntity()));
        assertEquals("presentationForm", information.getName());
        final InternationalString definition = information.getDefinition();
        assertEquals("Mode in which the resource is represented.", definition.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertInstanceOf("SIS-specific", CheckedContainer.class, information);
        assertEquals(PresentationForm.class, ((CheckedContainer<?>) information).getElementType());
        assertEquals(Datatype.CODE_LIST, information.getDataType());
        assertEquals(Obligation.OPTIONAL, information.getObligation());
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), information.getMaximumOccurrence());
    }

    /**
     * Tests serialization.
     *
     * @throws NoSuchMethodException Should never happen.
     */
    @Test
    public void testSerialization() throws NoSuchMethodException {
        assertSerializedEquals(create(InternationalString.class, "getTitle", "title"));
    }
}
