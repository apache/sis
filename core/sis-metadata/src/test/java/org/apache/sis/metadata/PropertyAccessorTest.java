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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Date;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.identification.*; // Really using almost everything.
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
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
import org.apache.sis.metadata.iso.content.DefaultAttributeGroup;
import org.apache.sis.metadata.iso.content.DefaultCoverageDescription;
import org.apache.sis.metadata.iso.identification.AbstractIdentification;
import org.apache.sis.metadata.iso.identification.DefaultAssociatedResource;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.metadata.PropertyAccessor.APPEND;
import static org.apache.sis.metadata.PropertyAccessor.RETURN_NULL;
import static org.apache.sis.metadata.PropertyAccessor.RETURN_PREVIOUS;


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
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(PropertyInformationTest.class)
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
            assertEquals("methodName",    methodName,    accessor.name(index, KeyNamePolicy.METHOD_NAME));
            assertEquals("propertyName",  propertyName,  accessor.name(index, KeyNamePolicy.JAVABEANS_PROPERTY));
            assertEquals("umlIdentifier", umlIdentifier, accessor.name(index, KeyNamePolicy.UML_IDENTIFIER));
            assertEquals("sentence",      sentence,      accessor.name(index, KeyNamePolicy.SENTENCE));
            assertEquals("declaringType", declaringType, accessor.type(index, TypeValuePolicy.DECLARING_INTERFACE));
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
     DefaultCitation.class, "getGraphics",                "graphics",                "graphic",               "Graphics",                   BrowseGraphic[].class,
     DefaultCitation.class, "getOnlineResources",         "onlineResources",         "onlineResource",        "Online resources",           OnlineResource[].class);
    }

    /**
     * Tests the constructor with the {@link DefaultDataIdentification} implementation.
     * The purpose of this test is to ensure that the properties defined in the parent
     * class are sorted first.
     *
     * <div class="note"><b>Note:</b> if there is any element not declared as JAXB elements,
     * those ones will be last in alphabetical order. Such situation is usually temporary
     * until the JAXB annotations are completed.</div>
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testConstructorWithInheritance() {
        assertMappingEquals(new PropertyAccessor(HardCodedCitations.ISO_19115, DataIdentification.class, DefaultDataIdentification.class),
        //……Declaring type………………………Method………………………………………………………………………JavaBeans………………………………………………………UML identifier………………………………………Sentence……………………………………………………………Type………………………………………………………………
            Identification.class, "getCitation",                   "citation",                   "citation",                  "Citation",                     Citation.class,
            Identification.class, "getAbstract",                   "abstract",                   "abstract",                  "Abstract",                     InternationalString.class,
            Identification.class, "getPurpose",                    "purpose",                    "purpose",                   "Purpose",                      InternationalString.class,
            Identification.class, "getCredits",                    "credits",                    "credit",                    "Credits",                      String[].class,
            Identification.class, "getStatus",                     "status",                     "status",                    "Status",                       Progress[].class,
            Identification.class, "getPointOfContacts",            "pointOfContacts",            "pointOfContact",            "Point of contacts",            ResponsibleParty[].class,
            Identification.class, "getResourceMaintenances",       "resourceMaintenances",       "resourceMaintenance",       "Resource maintenances",        MaintenanceInformation[].class,
            Identification.class, "getGraphicOverviews",           "graphicOverviews",           "graphicOverview",           "Graphic overviews",            BrowseGraphic[].class,
            Identification.class, "getResourceFormats",            "resourceFormats",            "resourceFormat",            "Resource formats",             Format[].class,
            Identification.class, "getDescriptiveKeywords",        "descriptiveKeywords",        "descriptiveKeywords",       "Descriptive keywords",         Keywords[].class,
            Identification.class, "getResourceSpecificUsages",     "resourceSpecificUsages",     "resourceSpecificUsage",     "Resource specific usages",     Usage[].class,
            Identification.class, "getResourceConstraints",        "resourceConstraints",        "resourceConstraints",       "Resource constraints",         Constraints[].class,
        DataIdentification.class, "getSpatialRepresentationTypes", "spatialRepresentationTypes", "spatialRepresentationType", "Spatial representation types", SpatialRepresentationType[].class,
        DataIdentification.class, "getSpatialResolutions",         "spatialResolutions",         "spatialResolution",         "Spatial resolutions",          Resolution[].class,
        DataIdentification.class, "getLanguages",                  "languages",                  "language",                  "Languages",                    Locale[].class,
        DataIdentification.class, "getCharacterSets",              "characterSets",              "characterSet",              "Character sets",               CharacterSet[].class,
        DataIdentification.class, "getTopicCategories",            "topicCategories",            "topicCategory",             "Topic categories",             TopicCategory[].class,
        DataIdentification.class, "getEnvironmentDescription",     "environmentDescription",     "environmentDescription",    "Environment description",      InternationalString.class,
        DataIdentification.class, "getExtents",                    "extents",                    "extent",                    "Extents",                      Extent[].class,
        DataIdentification.class, "getSupplementalInformation",    "supplementalInformation",    "supplementalInformation",   "Supplemental information",     InternationalString.class,
    AbstractIdentification.class, "getAdditionalDocumentations",   "additionalDocumentations",   "additionalDocumentation",   "Additional documentations",    Citation[].class,
    AbstractIdentification.class, "getAssociatedResources",        "associatedResources",        "associatedResource",        "Associated resources",         DefaultAssociatedResource[].class,
    AbstractIdentification.class, "getProcessingLevel",            "processingLevel",            "processingLevel",           "Processing level",             Identifier.class);
    }

    /**
     * Tests the constructor with a method which override an other method with covariant
     * return type. This test may need to be updated if a future GeoAPI release modifies
     * the {@link GeographicCRS} interface.
     *
     * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-205">GEOTK-205</a>
     */
    @Test
    @DependsOnMethod("testConstructorWithInheritance")
    public void testConstructorWithCovariantReturnType() {
        final Class<?> type = GeographicCRS.class;
        assertMappingEquals(new PropertyAccessor(HardCodedCitations.ISO_19111, type, type),
        //……Declaring type……………………………Method……………………………………………JavaBeans……………………………UML identifier………………Sentence…………………………………Type…………………………………………………………
            GeographicCRS.class,    "getCoordinateSystem", "coordinateSystem", "coordinateSystem", "Coordinate system",  EllipsoidalCS.class,       // Covariant return type
            GeodeticCRS.class,      "getDatum",            "datum",            "datum",            "Datum",              GeodeticDatum.class,       // Covariant return type
            IdentifiedObject.class, "getName",             "name",             "name",             "Name",               ReferenceIdentifier.class,
            IdentifiedObject.class, "getAlias",            "alias",            "alias",            "Alias",              GenericName[].class,
            ReferenceSystem.class,  "getDomainOfValidity", "domainOfValidity", "domainOfValidity", "Domain of validity", Extent.class,
            IdentifiedObject.class, "getIdentifiers",      "identifiers",      "identifier",       "Identifiers",        ReferenceIdentifier[].class,
            IdentifiedObject.class, "getRemarks",          "remarks",          "remarks",          "Remarks",            InternationalString.class,
            ReferenceSystem.class,  "getScope",            "scope",            "SC_CRS.scope",     "Scope",              InternationalString.class);
    }

    /**
     * Tests the {@link PropertyAccessor#information(int)} method.
     * This method delegates to some {@link PropertyInformationTest} methods.
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testInformation() {
        final PropertyAccessor accessor = createPropertyAccessor();
        PropertyInformationTest.validateTitle           (accessor.information(accessor.indexOf("title",            true)));
        PropertyInformationTest.validatePresentationForm(accessor.information(accessor.indexOf("presentationForm", true)));
    }

    /**
     * Tests the {@link PropertyAccessor#get(int, Object)} method on the {@link HardCodedCitations#ISO}
     * constant. The metadata object read by this test is:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title…………………………………… International Organization for Standardization
     *     ├─Alternate title………… ISO 19111
     *     ├─Identifier
     *     │   ├─Code…………………………… 19111
     *     │   └─Code space…………… ISO
     *     └─Presentation form…… Document digital
     * }
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testGet() {
        final DefaultCitation  instance = HardCodedCitations.ISO_19111;
        final PropertyAccessor accessor = createPropertyAccessor();

        // Singleton value (not a collection)
        final Object title = accessor.get(accessor.indexOf("title", true), instance);
        assertInstanceOf("title", InternationalString.class, title);
        assertEquals("title", "Spatial referencing by coordinates", title.toString());

        // Collection of InternationalStrings
        final Object alternateTitles = accessor.get(accessor.indexOf("alternateTitles", true), instance);
        assertInstanceOf("alternateTitles", Collection.class, alternateTitles);
        assertEquals("alternateTitles", "ISO 19111", getSingleton((Collection<?>) alternateTitles).toString());

        // Collection of Identifiers
        final Object identifiers = accessor.get(accessor.indexOf("identifiers", true), instance);
        assertEquals("19111", getSingletonCode(identifiers));
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * with a value to be stored <cite>as-is</cite> (without conversion).
     * The metadata object created by this test is:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title………………………… Some title
     *     ├─Identifier
     *     │   ├─Code………………… Some ISBN code
     *     │   └─Authority
     *     │       └─Title…… ISBN
     *     └─ISBN…………………………… Some ISBN code
     * }
     */
    @Test
    @DependsOnMethod("testGet")
    public void testSet() {
        final DefaultCitation  instance = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        Object newValue;
        int index;

        newValue = new SimpleInternationalString("Some title");
        index = accessor.indexOf("title", true);
        assertNull("title", accessor.set(index, instance, newValue, RETURN_PREVIOUS));
        assertSame("title", newValue, accessor.get(index, instance));
        assertSame("title", newValue, instance.getTitle());

        newValue = "Some ISBN code";
        index = accessor.indexOf("ISBN", true);
        assertNull("ISBN", accessor.set(index, instance, newValue, RETURN_PREVIOUS));
        assertSame("ISBN", newValue, accessor.get(index, instance));
        assertSame("ISBN", newValue, instance.getISBN());
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method with a {@code null} value.
     * Setting a property to {@code null} is equivalent to removing that property value.
     * The metadata object used by this test (before removal) is:
     *
     * {@preformat text
     *   DefaultCitation
     *     └─Title………………………… Some title
     * }
     */
    @Test
    @DependsOnMethod("testSet")
    public void testSetNull() {
        final DefaultCitation  instance = new DefaultCitation("Some title");
        final PropertyAccessor accessor = createPropertyAccessor();
        final InternationalString title = instance.getTitle();
        final int index = accessor.indexOf("title", true);

        assertEquals("Some title", title.toString()); // Sanity check before to continue.
        assertNull("title", accessor.set(index, instance, null, RETURN_NULL));
        assertNull("title", instance.getTitle());

        instance.setTitle(title);
        assertSame("title", title, accessor.set(index, instance, null, RETURN_PREVIOUS));
        assertNull("title", instance.getTitle());
    }

    /**
     * Tests setting a deprecated properties. This properties should not be visible in the map,
     * but still be accepted by the map views.
     */
    @Test
    @DependsOnMethod("testSet")
    public void testSetDeprecated() {
        final PropertyAccessor accessor = new PropertyAccessor(HardCodedCitations.ISO_19115,
                CoverageDescription.class, DefaultCoverageDescription.class);
        final int indexOfDeprecated  = accessor.indexOf("contentType", true);
        final int indexOfReplacement = accessor.indexOf("attributeGroup", true);
        assertTrue("Deprecated elements shall be sorted after non-deprecated ones.",
                indexOfDeprecated > indexOfReplacement);
        /*
         * Writes a value using the deprecated property.
         */
        final DefaultCoverageDescription instance = new DefaultCoverageDescription();
        assertNull("Shall be initially empty.", accessor.set(indexOfDeprecated, instance,
                CoverageContentType.IMAGE, PropertyAccessor.RETURN_PREVIOUS));
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
        assertEquals("Deprecated property shall not be visible.", 1, accessor.count(
                instance, ValueExistencePolicy.NON_EMPTY, PropertyAccessor.COUNT_SHALLOW));
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * with a value that will need to be converted. The conversion will be from
     * {@link String} to {@link InternationalString}. The created metadata object is:
     *
     * {@preformat text
     *   DefaultCitation
     *     └─Title……………… Some title
     * }
     */
    @Test
    @DependsOnMethod("testSet")
    public void testSetWithConversion() {
        final String           expected = "Some title";
        final DefaultCitation  instance = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        final int              index    = accessor.indexOf("title", true);
        final Object           oldValue = accessor.set(index, instance, expected, RETURN_PREVIOUS);
        final Object           value    = accessor.get(index, instance);

        assertNull      ("title", oldValue);
        assertInstanceOf("title", InternationalString.class, value);
        assertSame      ("title", expected, value.toString());
        assertSame      ("title", value, instance.getTitle());
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method when the value
     * is a collection. The new collection shall replace the previous one (no merge expected).
     * The metadata object created by this test after the replacement is:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title……………………………………………………… Ignored title
     *     ├─Alternate title (1 of 2)…… New title 1
     *     └─Alternate title (2 of 2)…… New title 2
     * }
     *
     * @see #testSetInAppendMode()
     */
    @Test
    @DependsOnMethod("testSet")
    public void testSetCollection() {
        final DefaultCitation instance = new DefaultCitation("Ignored title");
        final List<InternationalString> oldTitles = Arrays.<InternationalString>asList(
                new SimpleInternationalString("Old title 1"),
                new SimpleInternationalString("Old title 2"));
        final List<InternationalString> newTitles = Arrays.<InternationalString>asList(
                new SimpleInternationalString("New title 1"),
                new SimpleInternationalString("New title 2"));

        // Set the alternate titles.
        instance.setAlternateTitles(oldTitles);
        final PropertyAccessor accessor = createPropertyAccessor();
        final int              index    = accessor.indexOf("alternateTitles", true);
        final Object           oldValue = accessor.set(index, instance, newTitles, RETURN_PREVIOUS);
        final Object           newValue = accessor.get(index, instance);

        // Verify the values.
        assertEquals("set(…, RETURN_PREVIOUS)", oldTitles, oldValue);
        assertEquals("get(…)",                  newTitles, newValue);
        assertSame  ("alternateTitles",         newValue, instance.getAlternateTitles());
        assertTitleEquals("title", "Ignored title", instance);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * when adding elements in a collection, without conversion of type.
     * The metadata object created by this test is:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title……………………………………………………… Ignored title
     *     ├─Alternate title (1 of 2)…… An other title
     *     └─Alternate title (2 of 2)…… Yet an other title
     * }
     */
    @Test
    @DependsOnMethod("testSet")
    public void testSetIntoCollection() {
        testSetIntoCollection(false);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * when adding elements in a collection, with conversion of type.
     * The metadata object created by this test is:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title……………………………………………………… Ignored title
     *     ├─Alternate title (1 of 2)…… An other title
     *     └─Alternate title (2 of 2)…… Yet an other title
     * }
     */
    @Test
    @DependsOnMethod("testSetIntoCollection")
    public void testSetIntoCollectionWithConversion() {
        testSetIntoCollection(true);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method
     * when adding elements in a collection, with or without conversion of type.
     */
    private static void testSetIntoCollection(final boolean conversion) {
        final String              text1    = "An other title";
        final String              text2    = "Yet an other title";
        final InternationalString title1   = new SimpleInternationalString(text1);
        final InternationalString title2   = new SimpleInternationalString(text2);
        final DefaultCitation     instance = new DefaultCitation("Ignored title");
        final PropertyAccessor    accessor = createPropertyAccessor();
        final int                 index    = accessor.indexOf("alternateTitles", true);

        // Insert the first value. Old collection shall be empty.
        Object oldValue = accessor.set(index, instance, conversion ? text1 : title1, RETURN_PREVIOUS);
        assertInstanceOf("alternateTitles", Collection.class, oldValue);
        assertTrue("alternateTitles", ((Collection<?>) oldValue).isEmpty());

        // Insert the second value. Old collection shall contain the first value.
        oldValue = accessor.set(index, instance, conversion ? text2 : title2, RETURN_PREVIOUS);
        assertInstanceOf("alternateTitles", Collection.class, oldValue);
        oldValue = getSingleton((Collection<?>) oldValue);
        assertSame("alternateTitles", text1, oldValue.toString());
        if (!conversion) {
            assertSame("InternationalString should have been stored as-is.", title1, oldValue);
        }

        // Check final collection content.
        final List<InternationalString> expected = Arrays.asList(title1, title2);
        assertEquals("alternateTitles", expected, accessor.get(index, instance));
        assertTitleEquals("title", "Ignored title", instance);
    }

    /**
     * Tests the {@link PropertyAccessor#set(int, Object, Object, int)} method in
     * {@link PropertyAccessor#APPEND} mode. In this mode, new collections
     * are added into existing collections instead than replacing them.
     * The metadata object created by this test after the merge is:
     *
     * {@preformat text
     *   DefaultCitation
     *     ├─Title……………………………………………………… Added title
     *     ├─Alternate title (1 of 4)…… Old title 1
     *     ├─Alternate title (2 of 4)…… Old title 2
     *     ├─Alternate title (3 of 4)…… New title 1
     *     └─Alternate title (4 of 4)…… New title 2
     * }
     *
     * @see #testSetCollection()
     */
    public void testSetInAppendMode() {
        final DefaultCitation instance = new DefaultCitation();
        final List<InternationalString> oldTitles = Arrays.<InternationalString>asList(
                new SimpleInternationalString("Old title 1"),
                new SimpleInternationalString("Old title 2"));
        final List<InternationalString> newTitles = Arrays.<InternationalString>asList(
                new SimpleInternationalString("New title 1"),
                new SimpleInternationalString("New title 2"));
        final List<InternationalString> merged = new ArrayList<InternationalString>(oldTitles);
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
        assertEquals("set(…, APPEND)",  Boolean.TRUE, titleChanged);
        assertEquals("set(…, APPEND)",  Boolean.TRUE, changed);
        assertEquals("get(…)",          merged, newValue);
        assertSame  ("alternateTitles", newValue, instance.getAlternateTitles());
        assertTitleEquals("title", "Added title", instance);

        // Test setting again the title to the same value.
        titleChanged = accessor.set(titleIndex, instance, "Added title", APPEND);
        assertEquals("set(…, APPEND)", Boolean.FALSE, titleChanged);
        assertTitleEquals("title", "Added title", instance);

        // Test setting the title to a different value.
        titleChanged = accessor.set(titleIndex, instance, "Different title", APPEND);
        assertNull("set(…, APPEND)", titleChanged); // Operation shall be refused.
        assertTitleEquals("title", "Added title", instance);
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

        // Same test than above, but on a copy of the EPSG constant.
        citation = new DefaultCitation(HardCodedCitations.EPSG);
        assertFalse(accessor.equals(citation, HardCodedCitations.SIS,  ComparisonMode.STRICT));
        assertTrue (accessor.equals(citation, HardCodedCitations.EPSG, ComparisonMode.STRICT));

        // Identifiers shall be stored in different collection instances with equal content.
        final int    index  = accessor.indexOf("identifiers", true);
        final Object source = accessor.get(index, HardCodedCitations.EPSG);
        final Object target = accessor.get(index, citation);
        assertInstanceOf("identifiers", Collection.class, source);
        assertInstanceOf("identifiers", Collection.class, target);
        assertNotSame("Distinct objects shall have distinct collections.", source, target);
        assertEquals ("The two collections shall have the same content.",  source, target);
        assertEquals ("EPSG", getSingletonCode(target));

        // Set the identifiers to null, which should clear the collection.
        assertEquals("Expected the previous value.", source, accessor.set(index, citation, null, RETURN_PREVIOUS));
        final Object value = accessor.get(index, citation);
        assertNotNull("Should have replaced null by an empty collection.", value);
        assertTrue("Should have replaced null by an empty collection.", ((Collection<?>) value).isEmpty());
    }

    /**
     * Tests {@link PropertyAccessor#hashCode(Object)}.
     */
    @Test
    public void testHashCode() {
        final DefaultCitation  instance = new DefaultCitation();
        final PropertyAccessor accessor = createPropertyAccessor();
        final int              baseCode = Citation.class.hashCode();
        int hashCode = accessor.hashCode(instance);
        assertEquals("Empty metadata.", baseCode, hashCode);

        final InternationalString title = new SimpleInternationalString("Some title");
        instance.setTitle(title);
        hashCode = accessor.hashCode(instance);
        assertEquals("Metadata with a single value.", baseCode + title.hashCode(), hashCode);

        final InternationalString alternateTitle = new SimpleInternationalString("An other title");
        instance.setAlternateTitles(singleton(alternateTitle));
        hashCode = accessor.hashCode(instance);
        assertEquals("Metadata with two values.", baseCode + title.hashCode() + Arrays.asList(alternateTitle).hashCode(), hashCode);
    }

    /**
     * Tests {@link PropertyAccessor#toString()}. The {@code toString()}
     * method is only for debugging purpose, but we test it anyway.
     */
    @Test
    public void testToString() {
        final PropertyAccessor accessor = createPropertyAccessor();
        assertEquals("PropertyAccessor[14 getters (+1 ext.) & 15 setters in DefaultCitation:Citation from “ISO 19115”]", accessor.toString());
    }

    /**
     * Returns the code of the singleton identifier found in the given collection.
     * This method verifies that the object is of the expected type.
     *
     * @param identifiers A singleton {@code Collection<Identifier>}.
     * @return {@link Identifier#getCode()}.
     */
    static String getSingletonCode(final Object identifiers) {
        assertInstanceOf("identifiers", Collection.class, identifiers);
        final Object identifier = getSingleton((Collection<?>) identifiers);
        assertInstanceOf("identifier", Identifier.class, identifier);
        return ((Identifier) identifier).getCode();
    }
}
