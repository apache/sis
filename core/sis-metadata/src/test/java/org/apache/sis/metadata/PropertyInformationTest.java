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
import org.opengis.metadata.acquisition.EnvironmentalRecord;
import org.apache.sis.metadata.iso.acquisition.DefaultEnvironmentalRecord;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.measure.Range;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link PropertyInformation} class.
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
        return new PropertyInformation<T>(HardCodedCitations.ISO_19115, property,
                Citation.class.getMethod(method), elementType, null);
    }

    /**
     * Asserts that the {@linkplain ExtendedElementInformation#getParentEntity() parent entity}
     * of the given element is {@link Citation}.
     */
    private static void assertParentIsCitation(final ExtendedElementInformation information) {
        assertInstanceOf("Specific to SIS implementation.", Identifier.class, information);
        assertTitleEquals("authority", "ISO 19115", ((Identifier) information).getAuthority());
        assertEquals("CI_Citation", getSingleton(information.getParentEntity()));
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
        assertParentIsCitation(information);
        assertEquals("title", information.getName());
        final InternationalString definition = information.getDefinition();
        assertEquals("Name by which the cited resource is known.", definition.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertInstanceOf("Specific to SIS implementation.", CheckedContainer.class, information);
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
        assertParentIsCitation(information);
        assertEquals("presentationForm", information.getName());
        final InternationalString definition = information.getDefinition();
        assertEquals("Mode in which the resource is represented.", definition.toString(Locale.ENGLISH));
        // Test other locale here, if any.

        assertInstanceOf("Specific to SIS implementation.", CheckedContainer.class, information);
        assertEquals(PresentationForm.class, ((CheckedContainer<?>) information).getElementType());
        assertEquals(Datatype.CODE_LIST, information.getDataType());
        assertEquals(Obligation.OPTIONAL, information.getObligation());
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), information.getMaximumOccurrence());
        assertNull(information.getDomainValue());
    }

    /**
     * Tests {@link PropertyInformation#getDomainValue()} with a non-null range.
     *
     * @throws NoSuchMethodException Should never happen.
     */
    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testGetDomainValue() throws NoSuchMethodException {
        final ExtendedElementInformation information = new PropertyInformation<Double>(HardCodedCitations.ISO_19115,
                "maxRelativeHumidity", EnvironmentalRecord.class.getMethod("getMaxRelativeHumidity"), Double.class,
                DefaultEnvironmentalRecord.class.getMethod("getMaxRelativeHumidity").getAnnotation(ValueRange.class));

        final InternationalString domainValue = information.getDomainValue();
        assertNotNull(domainValue);
        assertEquals("[0.0 … 100.0]", domainValue.toString());
        assertEquals("[0 … 100]", domainValue.toString(Locale.ENGLISH));
        assertEquals("[0 … 100]", domainValue.toString(Locale.FRENCH));
        assertInstanceOf("Specific to SIS implementation.", Range.class, domainValue);
        assertEquals("getMinValue()", Double.valueOf(  0), ((Range) domainValue).getMinValue());
        assertEquals("getMaxValue()", Double.valueOf(100), ((Range) domainValue).getMaxValue());
    }

    /**
     * Tests the {@link PropertyInformation#toString()} method.
     * All information in the expected strings have been validated by previous tests in this class.
     *
     * @throws NoSuchMethodException Should never happen.
     */
    @Test
    @DependsOnMethod({"testTitle", "testPresentationForm"})
    public void testToString() throws NoSuchMethodException {
        assertEquals("PropertyInformation[“CI_Citation:title” : Character string, mandatory, maxOccurs=1]",
                create(InternationalString.class, "getTitle", "title").toString());

        assertEquals("PropertyInformation[“CI_Citation:presentationForm” : Codelist, optional, maxOccurs=∞]",
                create(PresentationForm.class, "getPresentationForms", "presentationForm").toString());
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
