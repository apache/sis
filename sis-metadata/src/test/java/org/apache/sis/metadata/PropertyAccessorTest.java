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

import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Date;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.util.InternationalString;
import org.opengis.util.GenericName;

import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Ignore;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.opengis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link PropertyAccessor} class. Every tests in this class instantiates directly a
 * {@link PropertyAccessor} object by invoking the {@link #createPropertyAccessor()} method.
 * This class shall not test accessors created indirectly (e.g. the accessors created
 * by {@link MetadataStandard}).
 *
 * <p>This test case uses the {@link Citation} and {@link GeographicCRS} types. If those types
 * are modified in a future GeoAPI version, then some hard-coded values in this test may need
 * to be updated.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@DependsOn(PropertyDescriptorTest.class)
public final strictfp class PropertyAccessorTest extends TestCase {
    /**
     * Creates a new property accessor for the {@link DefaultCitation} class.
     */
    private static PropertyAccessor createPropertyAccessor() {
        return new PropertyAccessor(HardCodedCitations.ISO_19115, Citation.class, DefaultCitation.class);
    }

    /**
     * Asserts that the properties found by the given {@code accessor} have the given names and types.
     * The {@code expected} array shall be a sequence of tuples having the following components:
     *
     * <ul>
     *   <li>The interface that declare the method.</li>
     *   <li>Name of the getter method as specified by {@link KeyNamePolicy#METHOD_NAME}.</li>
     *   <li>Name of the JavaBeans property as specified by {@link KeyNamePolicy#JAVABEANS_PROPERTY}.</li>
     *   <li>ISO 19115 UML identifier as specified by {@link KeyNamePolicy#UML_IDENTIFIER}.</li>
     *   <li>A sentence as specified by {@link KeyNamePolicy#SENTENCE}.</li>
     *   <li>The type of elements. By convention, an array type stands for {@link Collection}
     *       (we have to do this replacement because of parameterized types erasure).</li>
     * </ul>
     *
     * The tuples shall be ordered according the {@link PropertyComparator}.
     *
     * @param accessor The accessor to test.
     * @param expected The expected names and types as described above.
     *
     * @see PropertyAccessor#mapping
     */
    private static void assertMappingEquals(final PropertyAccessor accessor, final Object... expected) {
        int i = 0;
        while (i < expected.length) {
            final int      index         = i / 6;
            final Class<?> declaringType = (Class<?>) expected[i++];
            final String   methodName    = (String)   expected[i++];
            final String   propertyName  = (String)   expected[i++];
            final String   umlIdentifier = (String)   expected[i++];
            final String   sentence      = (String)   expected[i++];
            assertEquals("declaringType", declaringType, accessor.type(index, TypeValuePolicy.DECLARING_INTERFACE));
            assertEquals("methodName",    methodName,    accessor.name(index, KeyNamePolicy.METHOD_NAME));
            assertEquals("propertyName",  propertyName,  accessor.name(index, KeyNamePolicy.JAVABEANS_PROPERTY));
            assertEquals("umlIdentifier", umlIdentifier, accessor.name(index, KeyNamePolicy.UML_IDENTIFIER));
            assertEquals("sentence",      sentence,      accessor.name(index, KeyNamePolicy.SENTENCE));
            assertEquals(methodName,      index,         accessor.indexOf(methodName,    false));
            assertEquals(propertyName,    index,         accessor.indexOf(propertyName,  false));
            assertEquals(umlIdentifier,   index,         accessor.indexOf(umlIdentifier, false));
            assertEquals(propertyName,    index,         accessor.indexOf(propertyName .toLowerCase(Locale.ROOT), false));
            assertEquals(umlIdentifier,   index,         accessor.indexOf(umlIdentifier.toLowerCase(Locale.ROOT), false));
            /*
             * Verifies the type of values. This need special handling for collections.
             */
            Class<?> propertyType = (Class<?>) expected[i++];
            Class<?> elementType  = propertyType;
            if (propertyType.isArray()) {
                elementType  = propertyType.getComponentType();
                propertyType = Collection.class;
                if (IdentifiedObject.class.isAssignableFrom(accessor.type)) {
                    // Special cases
                    if (propertyName.equals("identifiers")) {
                        propertyType = Set.class;
                    }
                }
            }
            assertEquals(propertyName,  propertyType, accessor.type(index, TypeValuePolicy.PROPERTY_TYPE));
            assertEquals(umlIdentifier, elementType,  accessor.type(index, TypeValuePolicy.ELEMENT_TYPE));
        }
        assertEquals("Count of 'get' methods.", i/6, accessor.count());
    }

    /**
     * Tests the constructor. This test may need to be updated if a future GeoAPI release
     * modifies the {@link Citation} interface.
     */
    @Test
    public void testConstructor() {
        assertMappingEquals(createPropertyAccessor(),
        //……Declaring type………Method………………………………………………………………JavaBeans………………………………………………UML identifier……………………………Sentence………………………………………………………Type………………………………………………………………
/*Required*/Citation.class, "getDates",                   "dates",                   "date",                  "Dates",                      CitationDate[].class,
            Citation.class, "getTitle",                   "title",                   "title",                 "Title",                      InternationalString.class,
/*Optional*/Citation.class, "getAlternateTitles",         "alternateTitles",         "alternateTitle",        "Alternate titles",           InternationalString[].class,
            Citation.class, "getCitedResponsibleParties", "citedResponsibleParties", "citedResponsibleParty", "Cited responsible parties",  ResponsibleParty[].class,
            Citation.class, "getCollectiveTitle",         "collectiveTitle",         "collectiveTitle",       "Collective title",           InternationalString.class,
            Citation.class, "getEdition",                 "edition",                 "edition",               "Edition",                    InternationalString.class,
            Citation.class, "getEditionDate",             "editionDate",             "editionDate",           "Edition date",               Date.class,
            Citation.class, "getIdentifiers",             "identifiers",             "identifier",            "Identifiers",                Identifier[].class,
            Citation.class, "getISBN",                    "ISBN",                    "ISBN",                  "ISBN",                       String.class,
            Citation.class, "getISSN",                    "ISSN",                    "ISSN",                  "ISSN",                       String.class,
            Citation.class, "getOtherCitationDetails",    "otherCitationDetails",    "otherCitationDetails",  "Other citation details",     InternationalString.class,
            Citation.class, "getPresentationForms",       "presentationForms",       "presentationForm",      "Presentation forms",         PresentationForm[].class,
            Citation.class, "getSeries",                  "series",                  "series",                "Series",                     Series.class);
    }

    /**
     * Tests the constructor with a method which override an other method with covariant
     * return type. This test may need to be updated if a future GeoAPI release modifies
     * the {@link GeographicCRS} interface.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-205">GEOTK-205</a>
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testConstructorWithCovariantReturnType() {
        final Class<?> type = GeographicCRS.class;
        assertMappingEquals(new PropertyAccessor(HardCodedCitations.ISO, type, type),
        //……Declaring type……………………………Method……………………………………………JavaBeans……………………………UML identifier………………Sentence…………………………………Type…………………………………………………………
/*Required*/GeographicCRS.class,    "getCoordinateSystem", "coordinateSystem", "coordinateSystem", "Coordinate system",  EllipsoidalCS.class,       // Covariant return type
            GeodeticCRS.class,      "getDatum",            "datum",            "datum",            "Datum",              GeodeticDatum.class,       // Covariant return type
            IdentifiedObject.class, "getName",             "name",             "name",             "Name",               ReferenceIdentifier.class,
/*Optional*/IdentifiedObject.class, "getAlias",            "alias",            "alias",            "Alias",              GenericName[].class,
            ReferenceSystem.class,  "getDomainOfValidity", "domainOfValidity", "domainOfValidity", "Domain of validity", Extent.class,
            IdentifiedObject.class, "getIdentifiers",      "identifiers",      "identifier",       "Identifiers",        ReferenceIdentifier[].class,
            IdentifiedObject.class, "getRemarks",          "remarks",          "remarks",          "Remarks",            InternationalString.class,
            ReferenceSystem.class,  "getScope",            "scope",            "SC_CRS.scope",     "Scope",              InternationalString.class);
    }

    /**
     * Tests the {@link PropertyAccessor#descriptor(int)} method.
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testDescriptor() {
        final PropertyAccessor accessor = createPropertyAccessor();
        PropertyDescriptorTest.validateTitle           (accessor.descriptor(accessor.indexOf("title",            true)));
        PropertyDescriptorTest.validatePresentationForm(accessor.descriptor(accessor.indexOf("presentationForm", true)));
    }

    /**
     * Tests the {@link PropertyAccessor#get(int, Object)} method.
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testGet() {
        final Citation         citation = HardCodedCitations.ISO;
        final PropertyAccessor accessor = createPropertyAccessor();

        // Singleton value (not a collection)
        final Object title = accessor.get(accessor.indexOf("title", true), citation);
        assertInstanceOf("title", InternationalString.class, title);
        assertEquals("International Organization for Standardization", title.toString());

        // Collection of InternationalStrings
        final Object alternateTitles = accessor.get(accessor.indexOf("alternateTitles", true), citation);
        assertInstanceOf("alternateTitles", Collection.class, alternateTitles);
        assertEquals("ISO", getSingleton((Collection<?>) alternateTitles).toString());

        // Collection of Identifiers
        final Object identifiers = accessor.get(accessor.indexOf("identifiers", true), citation);
        assertInstanceOf("identifiers", Collection.class, identifiers);
        HardCodedCitations.assertIdentifiersForEPSG((Collection<?>) identifiers);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, boolean)} method
     * with a value to be stored <cite>as-is</cite>.
     */
    @Test
    @DependsOnMethod("testGet")
    public void testSet() {
        final Citation         citation = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        {
            final Object newValue = new SimpleInternationalString("Some title");
            final int    index    = accessor.indexOf("title", true);
            final Object oldValue = accessor.set(index, citation, newValue, true);
            assertNull(oldValue);
            assertSame(newValue, accessor.get(index, citation));
            assertSame(newValue, citation.getTitle());
        }
        if (false) { // TODO
            final Object newValue = "Some ISBN code";
            final int    index    = accessor.indexOf("ISBN", true);
            final Object oldValue = accessor.set(index, citation, newValue, true);
            assertNull(oldValue);
            assertSame(newValue, accessor.get(index, citation));
            assertSame(newValue, citation.getISBN());
        }
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, boolean)} method
     * with a value that will need to be converted. The conversion will be from
     * {@link String} to {@link InternationalString}.
     */
    @Test
    @DependsOnMethod("testSet")
    @Ignore("Needs ObjectConverters.find(…) implementation.")
    public void testSetWithConversion() {
        final String           expected = "Some title";
        final Citation         citation = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        final int              index    = accessor.indexOf("title", true);
        final Object           oldValue = accessor.set(index, citation, expected, true);
        final Object           value    = accessor.get(index, citation);

        assertNull(oldValue);
        assertInstanceOf("title", InternationalString.class, value);
        assertSame(expected, value.toString());
        assertSame(value, citation.getTitle());
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, boolean)} method
     * when adding elements in a collection, without conversion of type.
     */
    @Test
    @DependsOnMethod("testSet")
    public void testSetInCollection() {
        testSetInCollection(false);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, boolean)} method
     * when adding elements in a collection, with conversion of type.
     */
    @Test
    @DependsOnMethod("testSet")
    @Ignore("Needs ObjectConverters.find(…) implementation.")
    public void testSetInCollectionWithConversion() {
        testSetInCollection(true);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, boolean)} method
     * when adding elements in a collection, with or without conversion of type.
     */
    private void testSetInCollection(final boolean conversion) {
        final String              text1    = "An other title";
        final String              text2    = "Yet an other title";
        final InternationalString title1   = new SimpleInternationalString(text1);
        final InternationalString title2   = new SimpleInternationalString(text2);
        final Citation            citation = new DefaultCitation();
        final PropertyAccessor    accessor = createPropertyAccessor();
        final int                 index    = accessor.indexOf("alternateTitle", true);

        // Insert the first value. Old collection shall be empty.
        Object oldValue = accessor.set(index, citation, conversion ? text1 : title1, true);
        assertInstanceOf("alternateTitle", Collection.class, oldValue);
        assertTrue(((Collection<?>) oldValue).isEmpty());

        // Insert the second value. Old collection shall contains the first value.
        oldValue = accessor.set(index, citation, conversion ? text2 : title2, true);
        assertInstanceOf("alternateTitle", Collection.class, oldValue);
        oldValue = getSingleton((Collection<?>) oldValue);
        assertSame(text1, oldValue.toString());
        if (!conversion) {
            assertSame("InternationalString should have been stored as-is.", title1, oldValue);
        }

        // Check final collection content.
        final List<InternationalString> expected = Arrays.asList(title1, title2);
        assertEquals(expected, accessor.get(index, citation));
    }

    /**
     * Tests the {@link PropertyAccessor#shallowCopy(Object, Object)} method.
     */
    @Test
    public void testShallowCopy() {
        final Citation original = HardCodedCitations.ISO;
        final Citation copy = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        assertTrue("The copy should have modified the destination.", accessor.shallowCopy(original, copy));
        assertEquals("International Organization for Standardization", copy.getTitle().toString());

        Collection<?> values = copy.getAlternateTitles();
        assertNotSame("Collections shall have been copied.", original.getAlternateTitles(), values);
        assertEquals("ISO", getSingleton(values).toString());

        values = copy.getPresentationForms();
        assertNotSame("Collections shall have been copied.", original.getPresentationForms(), values);
        assertEquals(PresentationForm.DOCUMENT_DIGITAL, getSingleton(values));
    }

    /**
     * Tests the equals methods.
     */
    @Test
    @DependsOnMethod("testShallowCopy")
    public void testEquals() {
        Citation citation = HardCodedCitations.EPSG;
        final PropertyAccessor accessor = createPropertyAccessor();
        assertFalse(accessor.equals(citation, HardCodedCitations.GEOTIFF, ComparisonMode.STRICT));
        assertTrue (accessor.equals(citation, HardCodedCitations.EPSG,    ComparisonMode.STRICT));
        /*
         * Same test than above, but on a copy of the EPSG constant.
         */
        citation = new DefaultCitation();
        assertTrue (accessor.shallowCopy(HardCodedCitations.EPSG, citation));
        assertFalse(accessor.equals(citation, HardCodedCitations.GEOTIFF, ComparisonMode.STRICT));
        assertTrue (accessor.equals(citation, HardCodedCitations.EPSG,    ComparisonMode.STRICT));
        /*
         * Identifiers shall be stored in different collection instances with equal content.
         */
        final int    index  = accessor.indexOf("identifiers", true);
        final Object source = accessor.get(index, HardCodedCitations.EPSG);
        final Object target = accessor.get(index, citation);
        assertInstanceOf("identifiers", Collection.class, source);
        assertInstanceOf("identifiers", Collection.class, target);
//      assertNotSame(source, target);  // TODO: require non-empty collection.
        assertEquals (source, target);
        HardCodedCitations.assertIdentifiersForEPSG((Collection<?>) source);
        HardCodedCitations.assertIdentifiersForEPSG((Collection<?>) target);
        /*
         * Set the identifiers to null, which should clear the collection.
         */
// TODO assertEquals("Expected the previous value.", source, accessor.set(index, citation, null, true));
        final Object value = accessor.get(index, citation);
        assertNotNull("Should have replaced null by an empty collection.", value);
        assertTrue("Should have replaced null by an empty collection.", ((Collection<?>) value).isEmpty());
    }

    /**
     * Tests {@link PropertyAccessor#hashCode(Object)}.
     */
    @Test
    public void testHashCode() {
        final DefaultCitation citation = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        int hashCode = accessor.hashCode(citation);
        assertEquals("Empty metadata.", 0, hashCode);

        final InternationalString title = new SimpleInternationalString("Some title");
        citation.setTitle(title);
        hashCode = accessor.hashCode(citation);
        assertEquals("Metadata with a single value.", title.hashCode(), hashCode);

        final InternationalString alternateTitle = new SimpleInternationalString("An other title");
        citation.setAlternateTitles(singleton(alternateTitle));
        hashCode = accessor.hashCode(citation);
        assertEquals("Metadata with two values.", title.hashCode() + Arrays.asList(alternateTitle).hashCode(), hashCode);
    }
}
