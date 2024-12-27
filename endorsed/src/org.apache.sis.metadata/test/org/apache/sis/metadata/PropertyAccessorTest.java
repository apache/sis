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

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.time.temporal.TemporalAmount;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.identification.*;                       // Really using almost everything.
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.util.InternationalString;
import org.opengis.util.GenericName;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import static org.apache.sis.metadata.PropertyAccessor.APPEND;
import static org.apache.sis.metadata.PropertyAccessor.RETURN_NULL;
import static org.apache.sis.metadata.PropertyAccessor.RETURN_PREVIOUS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.mock.GeographicCRSMock;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;

// Specific to the main and geoapi-3.1 branches:
import java.util.Date;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.referencing.ReferenceIdentifier;

// Specific to the main branch:
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultAssociatedResource;


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
 */
@SuppressWarnings("OverlyStrongTypeCast")
public final class PropertyAccessorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PropertyAccessorTest() {
    }

    /**
     * Creates a new property accessor for the {@link DefaultCitation} class.
     */
    private static PropertyAccessor createPropertyAccessor() {
        return new PropertyAccessor(Citation.class, DefaultCitation.class, DefaultCitation.class);
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
     * @param  accessor  the accessor to test.
     * @param  expected  the expected names and types as described above.
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
            assertEquals(methodName,    accessor.name(index, KeyNamePolicy.METHOD_NAME));
            assertEquals(propertyName,  accessor.name(index, KeyNamePolicy.JAVABEANS_PROPERTY));
            assertEquals(umlIdentifier, accessor.name(index, KeyNamePolicy.UML_IDENTIFIER));
            assertEquals(sentence,      accessor.name(index, KeyNamePolicy.SENTENCE));
            assertEquals(declaringType, accessor.type(index, TypeValuePolicy.DECLARING_INTERFACE));
            assertEquals(index,         accessor.indexOf(methodName,    false), methodName);
            assertEquals(index,         accessor.indexOf(propertyName,  false), propertyName);
            assertEquals(index,         accessor.indexOf(umlIdentifier, false), umlIdentifier);
            assertEquals(index,         accessor.indexOf(propertyName .toLowerCase(Locale.ROOT), false), propertyName);
            assertEquals(index,         accessor.indexOf(umlIdentifier.toLowerCase(Locale.ROOT), false), umlIdentifier);
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
            } else if (propertyType == Map.class) {
                elementType = Map.Entry.class;
            }
            assertEquals(propertyType, accessor.type(index, TypeValuePolicy.PROPERTY_TYPE), propertyName);
            assertEquals(elementType,  accessor.type(index, TypeValuePolicy.ELEMENT_TYPE), umlIdentifier);
        }
        assertEquals(i/6, accessor.count(), "Count of getter methods.");
    }

    /**
     * Tests the constructor with the {@link DefaultCitation} implementation.
     * The order of properties shall be the order declared in the {@code XmlType.propOrder} annotation.
     * This test may need to be updated if a future GeoAPI release modifies the {@link Citation} interface.
     * Other tests that depends on {@link Citation} property order are {@link NameMapTest#testEntrySet()},
     * {@link TypeMapTest#testEntrySet()} and most tests in {@link ValueMapTest}.
     *
     * @see NameMapTest#testEntrySet()
     * @see TypeMapTest#testEntrySet()
     * @see ValueMapTest
     */
    @Test
    public void testConstructor() {
        assertMappingEquals(createPropertyAccessor(),
        //……Declaring type………Method………………………………………………………………JavaBeans………………………………………………UML identifier……………………………Sentence………………………………………………………Type………………………………………………………………
            Citation.class, "getTitle",                   "title",                   "title",                 "Title",                      InternationalString.class,
            Citation.class, "getAlternateTitles",         "alternateTitles",         "alternateTitle",        "Alternate titles",           InternationalString[].class,
            Citation.class, "getDates",                   "dates",                   "date",                  "Dates",                      CitationDate[].class,
            Citation.class, "getEdition",                 "edition",                 "edition",               "Edition",                    InternationalString.class,
            Citation.class, "getEditionDate",             "editionDate",             "editionDate",           "Edition date",               Date.class,
            Citation.class, "getIdentifiers",             "identifiers",             "identifier",            "Identifiers",                Identifier[].class,
            Citation.class, "getCitedResponsibleParties", "citedResponsibleParties", "citedResponsibleParty", "Cited responsible parties",  ResponsibleParty[].class,
            Citation.class, "getPresentationForms",       "presentationForms",       "presentationForm",      "Presentation forms",         PresentationForm[].class,
            Citation.class, "getSeries",                  "series",                  "series",                "Series",                     Series.class,
            Citation.class, "getOtherCitationDetails",    "otherCitationDetails",    "otherCitationDetails",  "Other citation details",     InternationalString.class,
//          Citation.class, "getCollectiveTitle",         "collectiveTitle",         "collectiveTitle",       "Collective title",           InternationalString.class,   -- deprecated as of ISO 19115:2014
            Citation.class, "getISBN",                    "ISBN",                    "ISBN",                  "ISBN",                       String.class,
            Citation.class, "getISSN",                    "ISSN",                    "ISSN",                  "ISSN",                       String.class,
     DefaultCitation.class, "getOnlineResources",         "onlineResources",         "onlineResource",        "Online resources",           OnlineResource[].class,
     DefaultCitation.class, "getGraphics",                "graphics",                "graphic",               "Graphics",                   BrowseGraphic[].class);
    }

    /**
     * Tests the constructor with the {@link DefaultDataIdentification} implementation.
     * The purpose of this test is to ensure that the properties defined in the parent
     * class are sorted first.
     *
     * Note that if there is any element not declared as JAXB elements,
     * those ones will be last in alphabetical order.
     * Such situation is usually temporary until the JAXB annotations are completed.
     */
    @Test
    public void testConstructorWithInheritance() {
        assertMappingEquals(new PropertyAccessor(DataIdentification.class, DefaultDataIdentification.class, DefaultDataIdentification.class),
        //……Declaring type………………………Method………………………………………………………………………JavaBeans………………………………………………………UML identifier………………………………………Sentence……………………………………………………………Type………………………………………………………………
            Identification.class, "getCitation",                   "citation",                   "citation",                  "Citation",                     Citation.class,
            Identification.class, "getAbstract",                   "abstract",                   "abstract",                  "Abstract",                     InternationalString.class,
            Identification.class, "getPurpose",                    "purpose",                    "purpose",                   "Purpose",                      InternationalString.class,
            Identification.class, "getCredits",                    "credits",                    "credit",                    "Credits",                      String[].class,
            Identification.class, "getStatus",                     "status",                     "status",                    "Status",                       Progress[].class,
            Identification.class, "getPointOfContacts",            "pointOfContacts",            "pointOfContact",            "Point of contacts",            ResponsibleParty[].class,
        DataIdentification.class, "getSpatialRepresentationTypes", "spatialRepresentationTypes", "spatialRepresentationType", "Spatial representation types", SpatialRepresentationType[].class,
        DataIdentification.class, "getSpatialResolutions",         "spatialResolutions",         "spatialResolution",         "Spatial resolutions",          Resolution[].class,
    AbstractIdentification.class, "getTemporalResolutions",        "temporalResolutions",        "temporalResolution",        "Temporal resolutions",         TemporalAmount[].class,
        DataIdentification.class, "getTopicCategories",            "topicCategories",            "topicCategory",             "Topic categories",             TopicCategory[].class,
        DataIdentification.class, "getExtents",                    "extents",                    "extent",                    "Extents",                      Extent[].class,
    AbstractIdentification.class, "getAdditionalDocumentations",   "additionalDocumentations",   "additionalDocumentation",   "Additional documentations",    Citation[].class,
    AbstractIdentification.class, "getProcessingLevel",            "processingLevel",            "processingLevel",           "Processing level",             Identifier.class,
            Identification.class, "getResourceMaintenances",       "resourceMaintenances",       "resourceMaintenance",       "Resource maintenances",        MaintenanceInformation[].class,
            Identification.class, "getGraphicOverviews",           "graphicOverviews",           "graphicOverview",           "Graphic overviews",            BrowseGraphic[].class,
            Identification.class, "getResourceFormats",            "resourceFormats",            "resourceFormat",            "Resource formats",             Format[].class,
            Identification.class, "getDescriptiveKeywords",        "descriptiveKeywords",        "descriptiveKeywords",       "Descriptive keywords",         Keywords[].class,
            Identification.class, "getResourceSpecificUsages",     "resourceSpecificUsages",     "resourceSpecificUsage",     "Resource specific usages",     Usage[].class,
            Identification.class, "getResourceConstraints",        "resourceConstraints",        "resourceConstraints",       "Resource constraints",         Constraints[].class,
    AbstractIdentification.class, "getAssociatedResources",        "associatedResources",        "associatedResource",        "Associated resources",         DefaultAssociatedResource[].class,
        DataIdentification.class, "getEnvironmentDescription",     "environmentDescription",     "environmentDescription",    "Environment description",      InternationalString.class,
        DataIdentification.class, "getSupplementalInformation",    "supplementalInformation",    "supplementalInformation",   "Supplemental information",     InternationalString.class,
 DefaultDataIdentification.class, "getLocalesAndCharsets",         "localesAndCharsets",         "defaultLocale+otherLocale", "Locales and charsets",         Map.class);
    }

    /**
     * Tests the constructor with a method which override another method with covariant return type.
     * This test may need to be updated if a future GeoAPI release modifies the {@link GeographicCRS} interface.
     */
    @Test
    public void testConstructorWithCovariantReturnType() {
        assertMappingEquals(new PropertyAccessor(GeographicCRS.class, GeographicCRSMock.class, GeographicCRSMock.class),
        //……Declaring type……………………………Method……………………………………………………JavaBeans……………………………………UML identifier………………………Sentence…………………………………………Type…………………………………………………………
            GeographicCRS.class,    "getCoordinateSystem",    "coordinateSystem",    "coordinateSystem",    "Coordinate system",     EllipsoidalCS.class,       // Covariant return type
            GeodeticCRS.class,      "getDatum",               "datum",               "datum",               "Datum",                 GeodeticDatum.class,       // Covariant return type
            IdentifiedObject.class, "getName",                "name",                "name",                "Name",                  ReferenceIdentifier.class,
            IdentifiedObject.class, "getAlias",               "alias",               "alias",               "Alias",                 GenericName[].class,
            IdentifiedObject.class, "getIdentifiers",         "identifiers",         "identifier",          "Identifiers",           ReferenceIdentifier[].class,
            ReferenceSystem.class,  "getScope",               "scope",               "SC_CRS.scope",        "Scope",                 InternationalString.class,
            ReferenceSystem.class,  "getDomainOfValidity",    "domainOfValidity",    "domainOfValidity",    "Domain of validity",    Extent.class,
            IdentifiedObject.class, "getRemarks",             "remarks",             "remarks",             "Remarks",               InternationalString.class);
    }

    /**
     * Tests the {@link PropertyAccessor#information(Citation, int)} method.
     * This method delegates to some {@link PropertyInformationTest} methods.
     */
    @Test
    public void testInformation() {
        final PropertyAccessor accessor = createPropertyAccessor();
        PropertyInformationTest.validateTitle           (accessor.information(HardCodedCitations.ISO_19115, accessor.indexOf("title",            true)));
        PropertyInformationTest.validatePresentationForm(accessor.information(HardCodedCitations.ISO_19115, accessor.indexOf("presentationForm", true)));
    }

    /**
     * Tests the {@link PropertyAccessor#get(int, Object)} method on the {@link HardCodedCitations#ISO_19111} constant.
     * The metadata object read by this test is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     ├─Title…………………………………… International Organization for Standardization
     *     ├─Alternate title………… ISO 19111
     *     ├─Identifier
     *     │   ├─Code…………………………… 19111
     *     │   └─Code space…………… ISO
     *     └─Presentation form…… Document digital</pre>
     */
    @Test
    public void testGet() {
        final DefaultCitation  instance = HardCodedCitations.ISO_19111;
        final PropertyAccessor accessor = createPropertyAccessor();

        // Singleton value (not a collection)
        final Object title = accessor.get(accessor.indexOf("title", true), instance);
        assertInstanceOf(InternationalString.class, title);
        assertEquals("Spatial referencing by coordinates", title.toString());

        // Collection of InternationalStrings
        final Object alternateTitles = accessor.get(accessor.indexOf("alternateTitles", true), instance);
        assertInstanceOf(Collection.class, alternateTitles);
        assertEquals("ISO 19111", getSingleton((Collection<?>) alternateTitles).toString());

        // Collection of Identifiers
        final Object identifiers = accessor.get(accessor.indexOf("identifiers", true), instance);
        assertEquals("19111", getSingletonCode(identifiers));
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * with a value to be stored <em>as-is</em> (without conversion).
     * The metadata object created by this test is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     ├─Title………………………… Some title
     *     ├─Identifier
     *     │   ├─Code………………… Some ISBN code
     *     │   └─Authority
     *     │       └─Title…… ISBN
     *     └─ISBN…………………………… Some ISBN code</pre>
     */
    @Test
    public void testSet() {
        final DefaultCitation  instance = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        Object newValue;
        int index;

        newValue = new SimpleInternationalString("Some title");
        index = accessor.indexOf("title", true);
        assertNull(accessor.set(index, instance, newValue, RETURN_PREVIOUS));
        assertSame(newValue, accessor.get(index, instance));
        assertSame(newValue, instance.getTitle());

        newValue = "Some ISBN code";
        index = accessor.indexOf("ISBN", true);
        assertNull(accessor.set(index, instance, newValue, RETURN_PREVIOUS));
        assertSame(newValue, accessor.get(index, instance));
        assertSame(newValue, instance.getISBN());
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method with a {@code null} value.
     * Setting a property to {@code null} is equivalent to removing that property value.
     * The metadata object used by this test (before removal) is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     └─Title………………………… Some title</pre>
     */
    @Test
    public void testSetNull() {
        final DefaultCitation  instance = new DefaultCitation("Some title");
        final PropertyAccessor accessor = createPropertyAccessor();
        final InternationalString title = instance.getTitle();
        final int index = accessor.indexOf("title", true);

        assertEquals("Some title", title.toString()); // Sanity check before to continue.
        assertNull(accessor.set(index, instance, null, RETURN_NULL));
        assertNull(instance.getTitle());

        instance.setTitle(title);
        assertSame(title, accessor.set(index, instance, null, RETURN_PREVIOUS));
        assertNull(instance.getTitle());
    }

    /**
     * Tests setting a deprecated properties. This properties should not be visible in the map,
     * but still be accepted by the map views.
     */
    @Test
    public void testSetDeprecated() {
        final PropertyAccessor accessor = new PropertyAccessor(CoverageDescription.class,
                    DefaultCoverageDescription.class, DefaultCoverageDescription.class);
        final int indexOfDeprecated  = accessor.indexOf("contentType", true);
        final int indexOfReplacement = accessor.indexOf("attributeGroup", true);
        assertTrue(indexOfDeprecated > indexOfReplacement,
                "Deprecated elements shall be sorted after non-deprecated ones.");
        /*
         * Writes a value using the deprecated property.
         */
        final DefaultCoverageDescription instance = new DefaultCoverageDescription();
        assertNull(accessor.set(indexOfDeprecated, instance, CoverageContentType.IMAGE, PropertyAccessor.RETURN_PREVIOUS),
                   "Shall be initially empty.");
        assertEquals(CoverageContentType.IMAGE, accessor.get(indexOfDeprecated, instance));
        /*
         * Compares with the non-deprecated property.
         */
        final Collection<DefaultAttributeGroup> groups = instance.getAttributeGroups();
        assertSame(groups, accessor.get(indexOfReplacement, instance));
        assertEquals(CoverageContentType.IMAGE, getSingleton(getSingleton(groups).getContentTypes()));
        /*
         * While we can read/write the value through two properties,
         * only one should be visible.
         */
        assertEquals(1, accessor.count(instance, ValueExistencePolicy.NON_EMPTY, PropertyAccessor.COUNT_SHALLOW),
                     "Deprecated property shall not be visible.");
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * with a value that will need to be converted. The conversion will be from
     * {@link String} to {@link InternationalString}. The created metadata object is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     └─Title……………… Some title</pre>
     */
    @Test
    public void testSetWithConversion() {
        final String           expected = "Some title";
        final DefaultCitation  instance = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        final int              index    = accessor.indexOf("title", true);
        final Object           oldValue = accessor.set(index, instance, expected, RETURN_PREVIOUS);
        final Object           value    = accessor.get(index, instance);

        assertNull      (oldValue);
        assertInstanceOf(InternationalString.class, value);
        assertSame      (expected, value.toString());
        assertSame      (value, instance.getTitle());
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method when the value
     * is a collection. The new collection shall replace the previous one (no merge expected).
     * The metadata object created by this test after the replacement is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     ├─Title……………………………………………………… Ignored title
     *     ├─Alternate title (1 of 2)…… New title 1
     *     └─Alternate title (2 of 2)…… New title 2</pre>
     *
     * @see #testSetInAppendMode()
     */
    @Test
    public void testSetCollection() {
        final DefaultCitation instance = new DefaultCitation("Ignored title");
        final List<InternationalString> oldTitles = List.of(
                new SimpleInternationalString("Old title 1"),
                new SimpleInternationalString("Old title 2"));
        final List<InternationalString> newTitles = List.of(
                new SimpleInternationalString("New title 1"),
                new SimpleInternationalString("New title 2"));

        // Set the alternate titles.
        instance.setAlternateTitles(oldTitles);
        final PropertyAccessor accessor = createPropertyAccessor();
        final int              index    = accessor.indexOf("alternateTitles", true);
        final Object           oldValue = accessor.set(index, instance, newTitles, RETURN_PREVIOUS);
        final Object           newValue = accessor.get(index, instance);

        // Verify the values.
        assertEquals(oldTitles, oldValue, "set(…, RETURN_PREVIOUS)");
        assertEquals(newTitles, newValue, "get(…)");
        assertSame  (newValue, instance.getAlternateTitles());
        assertTitleEquals("Ignored title", instance, "citation");
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * when adding elements in a collection, without conversion of type.
     * The metadata object created by this test is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     ├─Title……………………………………………………… Ignored title
     *     ├─Alternate title (1 of 2)…… Another title
     *     └─Alternate title (2 of 2)…… Yet another title</pre>
     */
    @Test
    public void testSetIntoCollection() {
        testSetIntoCollection(false);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * when adding elements in a collection, with conversion of type.
     * The metadata object created by this test is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     ├─Title……………………………………………………… Ignored title
     *     ├─Alternate title (1 of 2)…… Another title
     *     └─Alternate title (2 of 2)…… Yet another title</pre>
     */
    @Test
    public void testSetIntoCollectionWithConversion() {
        testSetIntoCollection(true);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * when adding elements in a collection, with or without conversion of type.
     */
    private static void testSetIntoCollection(final boolean conversion) {
        final String              text1    = "Another title";
        final String              text2    = "Yet another title";
        final InternationalString title1   = new SimpleInternationalString(text1);
        final InternationalString title2   = new SimpleInternationalString(text2);
        final DefaultCitation     instance = new DefaultCitation("Ignored title");
        final PropertyAccessor    accessor = createPropertyAccessor();
        final int                 index    = accessor.indexOf("alternateTitles", true);

        // Insert the first value. Old collection shall be empty.
        Object oldValue = accessor.set(index, instance, conversion ? text1 : title1, RETURN_PREVIOUS);
        assertTrue(assertInstanceOf(Collection.class, oldValue).isEmpty());

        // Insert the second value. Old collection shall contain the first value.
        oldValue = accessor.set(index, instance, conversion ? text2 : title2, RETURN_PREVIOUS);
        assertInstanceOf(Collection.class, oldValue);
        oldValue = getSingleton((Collection<?>) oldValue);
        assertSame(text1, oldValue.toString());
        if (!conversion) {
            assertSame(title1, oldValue, "InternationalString should have been stored as-is.");
        }

        // Check final collection content.
        final List<InternationalString> expected = List.of(title1, title2);
        assertEquals(expected, accessor.get(index, instance));
        assertTitleEquals("Ignored title", instance, "citation");
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method in
     * {@link PropertyAccessor#APPEND} mode. In this mode, new collections
     * are added into existing collections instead of replacing them.
     * The metadata object created by this test after the merge is:
     *
     * <pre class="text">
     *   DefaultCitation
     *     ├─Title……………………………………………………… Added title
     *     ├─Alternate title (1 of 4)…… Old title 1
     *     ├─Alternate title (2 of 4)…… Old title 2
     *     ├─Alternate title (3 of 4)…… New title 1
     *     └─Alternate title (4 of 4)…… New title 2</pre>
     *
     * @see #testSetCollection()
     */
    public void testSetInAppendMode() {
        final DefaultCitation instance = new DefaultCitation();
        final List<InternationalString> oldTitles = List.of(
                new SimpleInternationalString("Old title 1"),
                new SimpleInternationalString("Old title 2"));
        final List<InternationalString> newTitles = List.of(
                new SimpleInternationalString("New title 1"),
                new SimpleInternationalString("New title 2"));
        final List<InternationalString> merged = new ArrayList<>(oldTitles);
        assertTrue(merged.addAll(newTitles));

        // Set the title.
        instance.setAlternateTitles(oldTitles);
        final PropertyAccessor accessor = createPropertyAccessor();
        final int titleIndex = accessor.indexOf("title", true);
        Object titleChanged = accessor.set(titleIndex, instance, "Added title", APPEND);

        // Set the alternate titles.
        final int    index    = accessor.indexOf("alternateTitles", true);
        final Object changed  = accessor.set(index, instance, newTitles, APPEND);
        final Object newValue = accessor.get(index, instance);

        // Verify the values.
        assertEquals(Boolean.TRUE, titleChanged, "set(…, APPEND)");
        assertEquals(Boolean.TRUE, changed, "set(…, APPEND)");
        assertEquals(merged, newValue, "get(…)");
        assertSame  (newValue, instance.getAlternateTitles());
        assertTitleEquals("Added title", instance, "citation");

        // Test setting again the title to the same value.
        titleChanged = accessor.set(titleIndex, instance, "Added title", APPEND);
        assertEquals(Boolean.FALSE, titleChanged, "set(…, APPEND)");
        assertTitleEquals("Added title", instance, "citation");

        // Test setting the title to a different value.
        titleChanged = accessor.set(titleIndex, instance, "Different title", APPEND);
        assertNull(titleChanged, "set(…, APPEND)");     // Operation shall be refused.
        assertTitleEquals("Added title", instance, "citation");
    }

    /**
     * Tests the equals methods.
     */
    @Test
    public void testEquals() {
        DefaultCitation citation = HardCodedCitations.EPSG;
        final PropertyAccessor accessor = createPropertyAccessor();
        assertFalse(accessor.equals(citation, HardCodedCitations.SIS,  ComparisonMode.STRICT));
        assertTrue (accessor.equals(citation, HardCodedCitations.EPSG, ComparisonMode.STRICT));

        // Same test as above, but on a copy of the EPSG constant.
        citation = new DefaultCitation(HardCodedCitations.EPSG);
        assertFalse(accessor.equals(citation, HardCodedCitations.SIS,  ComparisonMode.STRICT));
        assertTrue (accessor.equals(citation, HardCodedCitations.EPSG, ComparisonMode.STRICT));

        // Identifiers shall be stored in different collection instances with equal content.
        final int    index  = accessor.indexOf("identifiers", true);
        final Object source = accessor.get(index, HardCodedCitations.EPSG);
        final Object target = accessor.get(index, citation);
        assertInstanceOf(Collection.class, source);
        assertInstanceOf(Collection.class, target);
        assertNotSame(source, target, "Distinct objects shall have distinct collections.");
        assertEquals (source, target, "The two collections shall have the same content.");
        assertEquals ("EPSG", getSingletonCode(target));

        // Set the identifiers to null, which should clear the collection.
        assertEquals(source, accessor.set(index, citation, null, RETURN_PREVIOUS), "Expected the previous value.");
        final Object value = accessor.get(index, citation);
        assertNotNull(value, "Should have replaced null by an empty collection.");
        assertTrue(((Collection<?>) value).isEmpty(), "Should have replaced null by an empty collection.");
    }

    /**
     * Tests {@link PropertyAccessor#toString()}. The {@code toString()}
     * method is only for debugging purpose, but we test it anyway.
     */
    @Test
    public void testToString() {
        final PropertyAccessor accessor = createPropertyAccessor();
        assertEquals("PropertyAccessor[14 getters (+1 ext.) & 15 setters in DefaultCitation:Citation]", accessor.toString());
    }

    /**
     * Returns the code of the singleton identifier found in the given collection.
     * This method verifies that the object is of the expected type.
     *
     * @param  identifiers  a singleton {@code Collection<Identifier>}.
     * @return {@link Identifier#getCode()}.
     */
    static String getSingletonCode(final Object identifiers) {
        assertInstanceOf(Collection.class, identifiers);
        Object identifier = getSingleton((Collection<?>) identifiers);
        return assertInstanceOf(Identifier.class, identifier).getCode();
    }
}
