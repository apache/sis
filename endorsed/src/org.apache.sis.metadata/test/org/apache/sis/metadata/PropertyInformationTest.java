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
import org.opengis.metadata.Identifier;
import org.opengis.metadata.ExtendedElementInformation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.acquisition.EnvironmentalRecord;
import org.apache.sis.metadata.iso.acquisition.DefaultEnvironmentalRecord;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.measure.Range;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.apache.sis.test.Assertions.assertSingleton;

// Specific to the geoapi-4.0 branch:
import org.opengis.annotation.Obligation;


/**
 * Tests the {@link PropertyInformation} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class PropertyInformationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PropertyInformationTest() {
    }

    /**
     * Creates a property information instance for the given method of the {@link Citation} interface.
     *
     * @param  elementType  the type of elements returned by the given method.
     * @param  method       the name of the method.
     * @param  property     the ISO 19115 name of the property.
     * @throws NoSuchMethodException if the {@code method} name is invalid.
     */
    private static <T> PropertyInformation<T> create(final Class<T> elementType, final String method,
            final String property) throws NoSuchMethodException
    {
        return new PropertyInformation<>(HardCodedCitations.ISO_19115, property,
                Citation.class.getMethod(method), elementType, null);
    }

    /**
     * Asserts that the {@linkplain ExtendedElementInformation#getParentEntity() parent entity}
     * of the given element is {@link Citation}.
     */
    private static void assertParentIsCitation(final ExtendedElementInformation information) {
        assertInstanceOf(Identifier.class, information);    // Specific to SIS implementation.
        assertTitleEquals("ISO 19115", ((Identifier) information).getAuthority(), "authority");
        assertEquals("CI_Citation", assertSingleton(information.getParentEntity()));
    }

    /**
     * Tests the properties of {@link Citation#getTitle()}.
     * The element type is an {@link InternationalString} singleton, which is mandatory.
     *
     * @throws NoSuchMethodException if the {@code getTitle()} method has not been found.
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
        assertParentIsCitation(information);
        assertEquals("title", information.getName());
        final InternationalString definition = information.getDefinition();
        assertEquals("Name by which the cited resource is known.", definition.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertInstanceOf(CheckedContainer.class, information);      // Specific to SIS implementation.
        assertEquals(InternationalString.class, ((CheckedContainer<?>) information).getElementType());
        assertEquals(Datatype.CHARACTER_STRING, information.getDataType());
        assertEquals(Obligation.MANDATORY, information.getObligation());
        assertEquals(Integer.valueOf(1), information.getMaximumOccurrence());
        assertNull(information.getDomainValue());
    }

    /**
     * Tests the properties of {@link Citation#getPresentationForms()}.
     * The element type is {@link PresentationForm} in a collection.
     *
     * @throws NoSuchMethodException if the {@code getPresentationForms()} method has not been found.
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
        assertParentIsCitation(information);
        assertEquals("presentationForm", information.getName());
        final InternationalString definition = information.getDefinition();
        assertEquals("Mode in which the resource is represented.", definition.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertInstanceOf(CheckedContainer.class, information);       // Specific to SIS implementation.
        assertEquals(PresentationForm.class, ((CheckedContainer<?>) information).getElementType());
        assertEquals(Datatype.CODE_LIST, information.getDataType());
        assertEquals(Obligation.OPTIONAL, information.getObligation());
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), information.getMaximumOccurrence());
        assertNull(information.getDomainValue());
    }

    /**
     * Tests {@link PropertyInformation#getDomainValue()} with a non-null range.
     *
     * @throws NoSuchMethodException if the {@code getMaxRelativeHumidity()} or other method has not been found.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testGetDomainValue() throws NoSuchMethodException {
        final ExtendedElementInformation information = new PropertyInformation<>(HardCodedCitations.ISO_19115,
                "maxRelativeHumidity", EnvironmentalRecord.class.getMethod("getMaxRelativeHumidity"), Double.class,
                DefaultEnvironmentalRecord.class.getMethod("getMaxRelativeHumidity").getAnnotation(ValueRange.class));

        final InternationalString domainValue = information.getDomainValue();
        assertNotNull(domainValue);
        assertEquals("[0.0 … 100.0]", domainValue.toString());
        assertEquals("[0 … 100]", domainValue.toString(Locale.ENGLISH));
        assertEquals("[0 … 100]", domainValue.toString(Locale.FRENCH));
        var range = assertInstanceOf(Range.class, domainValue);      // Specific to SIS implementation.
        assertEquals(Double.valueOf(  0), range.getMinValue());
        assertEquals(Double.valueOf(100), range.getMaxValue());
    }

    /**
     * Tests the {@link PropertyInformation#toString()} method.
     * All information in the expected strings have been validated by previous tests in this class.
     *
     * @throws NoSuchMethodException if the {@code getTitle()} or other method has not been found.
     */
    @Test
    public void testToString() throws NoSuchMethodException {
        assertEquals("PropertyInformation[“CI_Citation:title” : Character string, mandatory, maxOccurs=1]",
                create(InternationalString.class, "getTitle", "title").toString());

        assertEquals("PropertyInformation[“CI_Citation:presentationForm” : Codelist, optional, maxOccurs=∞]",
                create(PresentationForm.class, "getPresentationForms", "presentationForm").toString());
    }

    /**
     * Tests serialization.
     *
     * @throws NoSuchMethodException if the {@code getTitle()} method has not been found.
     */
    @Test
    public void testSerialization() throws NoSuchMethodException {
        assertSerializedEquals(create(InternationalString.class, "getTitle", "title"));
    }
}
