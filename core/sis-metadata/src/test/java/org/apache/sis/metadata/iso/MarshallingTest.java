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
package org.apache.sis.metadata.iso;

import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.logging.LogRecord;
import java.util.MissingResourceException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import org.opengis.annotation.Obligation;
import org.opengis.util.RecordType;
import org.opengis.metadata.Datatype;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.constraint.*;
import org.opengis.metadata.content.*;
import org.opengis.metadata.extent.*;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.maintenance.*;
import org.opengis.metadata.spatial.*;
import org.apache.sis.metadata.iso.citation.*;
import org.apache.sis.metadata.iso.constraint.*;
import org.apache.sis.metadata.iso.content.*;
import org.apache.sis.metadata.iso.distribution.*;
import org.apache.sis.metadata.iso.extent.*;
import org.apache.sis.metadata.iso.identification.*;
import org.apache.sis.metadata.iso.maintenance.*;
import org.apache.sis.metadata.iso.spatial.*;
import org.apache.sis.util.iso.DefaultRecordSchema;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.measure.Units;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.internal.jaxb.gcx.Anchor;
import org.apache.sis.internal.jaxb.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.util.iso.Names;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Simple test cases for marshalling a {@link DefaultMetadata} object to an XML file.
 * This class is used to test the ISO 19115-3 metadata standard implementation.
 *
 * @author  Cullen Rombach (Image Matters)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-400">SIS-400</a>
 *
 * @since 1.0
 * @module
 */
public final class MarshallingTest extends TestUsingFile implements WarningListener<Object> {
    /**
     * An XML file containing a metadata.
     * This is mostly an anti-regression test.
     */
    private static final String FILENAME = "Metadata.xml";

    /**
     * The marshaller used to handle marshalling the created DefaultMetadata object.
     */
    private final Marshaller marshaller;

    /**
     * The pool from which the marshaller is pulled.
     */
    private final MarshallerPool pool;

    /**
     * The output to which the metadata object will be marshaled.
     */
    private final StringWriter output;

    /**
     * {@code true} if marshalling legacy XML instead than latest schema.
     */
    private boolean legacyXML;

    /**
     * Initializes a new test case.
     *
     * @throws JAXBException if an error occurred while preparing the marshaller.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public MarshallingTest() throws JAXBException {
        output     = new StringWriter();
        pool       = getMarshallerPool();
        marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.WARNING_LISTENER, this);
    }

    /**
     * Creates a metadata object to marshal.
     */
    @SuppressWarnings("deprecation")
    private static DefaultMetadata metadata() throws URISyntaxException {
        /*
         * Metadata
         *   ├─Metadata identifier…… a-metadata-identifier
         *   │   └─Code space………………… md.id.ns
         *   ├─Parent metadata……………… A parent metadata
         *   │   └─Identifier………………… a-parent-identifier
         *   │       └─Code space……… pmd.id.ns
         *   ├─Language (1 de 2)………… English
         *   ├─Language (2 de 2)………… French (Canada)
         *   ├─Character set…………………… ISO-8859-1
         *   └─Metadata scope
         *       ├─Resource scope……… Dataset
         *       └─Name………………………………… Metadata for an imaginary data set
         *
         * Some code are indented for readability and more local variable scopes.
         */
        final DefaultMetadata md = new DefaultMetadata();
        {
            // Metadata identifier
            final DefaultIdentifier id = new DefaultIdentifier("a-metadata-identifier");
            id.setCodeSpace("md.id.ns");
            md.setMetadataIdentifier(id);
        }
        // Languages — one language only, and one (country, language) tupple.
        final Collection<Locale> languages = Arrays.asList(Locale.ENGLISH, Locale.CANADA_FRENCH);
        md.setLanguages(languages);

        // Character Sets (character encoding)
        final Collection<Charset> charSets = Collections.singleton(StandardCharsets.ISO_8859_1);
        md.setCharacterSets(charSets);
        {
            // Parent metadata
            final DefaultCitation parent = new DefaultCitation("A parent metadata");
            final DefaultIdentifier parentId = new DefaultIdentifier("a-parent-identifier");
            parentId.setCodeSpace("pmd.id.ns");
            parent.getIdentifiers().add(parentId);
            md.setParentMetadata(parent);
        }
        // mdb:metadataScope (hierarchyLevel and hierarchyLevelName in legacy ISO 19115:2003 model)
        md.getMetadataScopes().add(new DefaultMetadataScope(ScopeCode.DATASET, "Metadata for an imaginary data set"));
        final DefaultOnlineResource onlineResource;
        {
            /*
             * Contact information for the parties.
             *
             * Organisation………………………………………………………………… Plato Republic
             *   ├─Contact info
             *   │   ├─Phone (1 de 2)
             *   │   │   ├─Number………………………………………………… 555-444-3333
             *   │   │   └─Number type…………………………………… Voice
             *   │   ├─Phone (2 de 2)
             *   │   │   ├─Number………………………………………………… 555-555-5555
             *   │   │   └─Number type…………………………………… Facsimile
             *   │   ├─Address
             *   │   │   ├─Delivery point…………………………… 123 Main Street
             *   │   │   ├─City……………………………………………………… Metropolis city
             *   │   │   ├─Administrative area……………… Utopia province
             *   │   │   ├─Postal code…………………………………… A1A 2C2
             *   │   │   ├─Country……………………………………………… Atlantis island
             *   │   │   └─Electronic mail address…… test@example.com
             *   │   ├─Online resource
             *   │   │   ├─Linkage……………………………………………… http://example.com
             *   │   │   ├─Protocol…………………………………………… Submarine HTTP
             *   │   │   ├─Application profile……………… Imaginary work
             *   │   │   ├─Name……………………………………………………… Timaeus & Critias
             *   │   │   ├─Description…………………………………… A dialog between philosophers.
             *   │   │   └─Function…………………………………………… Search
             *   │   ├─Hours of service………………………………… Weekdays 9:00 AM - 5:00 PM
             *   │   ├─Contact instructions……………………… Through thought
             *   │   └─Contact type…………………………………………… Virtual
             *   └─Individual…………………………………………………………… Socrates
             *       └─Position name………………………………………… Philosopher
             */
            final DefaultContact contact = new DefaultContact();
            contact.setPhones(Arrays.asList(new DefaultTelephone("555-444-3333", TelephoneType.VOICE),
                                            new DefaultTelephone("555-555-5555", TelephoneType.FACSIMILE)));
            {
                {
                    // Address information
                    final DefaultAddress address = new DefaultAddress();
                    address.setDeliveryPoints(Collections.singleton(new SimpleInternationalString("123 Main Street")));
                    address.getElectronicMailAddresses().add("test@example.com");
                    address.setCity(new SimpleInternationalString("Metropolis city"));
                    address.setAdministrativeArea(new SimpleInternationalString("Utopia province"));
                    address.setPostalCode("A1A 2C2");
                    address.setCountry(new SimpleInternationalString("Atlantis island"));
                    contact.getAddresses().add(address);
                }
                // Online resources
                final DefaultInternationalString description = new DefaultInternationalString();
                description.add(Locale.ENGLISH, "A dialog between philosophers.");
                description.add(Locale.FRENCH,  "Un dialogue entre philosophes.");
                onlineResource = new DefaultOnlineResource(new URI("http://example.com"));
                onlineResource.setName(new SimpleInternationalString("Timaeus & Critias"));
                onlineResource.setDescription(description);
                onlineResource.setProtocol("Submarine HTTP");
                onlineResource.setApplicationProfile("Imaginary work");
                onlineResource.setFunction(OnLineFunction.SEARCH);
                onlineResource.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "timaeus");    // For enabling references
                contact.getOnlineResources().add(onlineResource);
                contact.setHoursOfService(Collections.singleton(new SimpleInternationalString("Weekdays 9:00 AM - 5:00 PM")));
                contact.setContactInstructions(new SimpleInternationalString("Through thought"));
                contact.setContactType(new SimpleInternationalString("Virtual"));
                contact.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "thought");           // For enabling references
            }
            // Create some individuals
            final DefaultIndividual individual  = new DefaultIndividual("Socrates", "Philosopher", null);
            final DefaultIndividual individual2 = new DefaultIndividual("Hermocrates", "Politician", contact);
            final DefaultOrganisation org = new DefaultOrganisation("Plato Republic", null, individual, contact);
            md.setContacts(Arrays.asList(new DefaultResponsibility(Role.POINT_OF_CONTACT, null, org),
                                         new DefaultResponsibility(Role.POINT_OF_CONTACT, null, individual2)));
        }
        // Date info (date stamp in legacy ISO 19115:2003 model)
        final Collection<CitationDate> dateInfo = Collections.singleton(new DefaultCitationDate(new Date(1260961229580L), DateType.CREATION));
        md.setDateInfo(dateInfo);
        {
            // Metadata standard
            final DefaultCitation standard = new DefaultCitation("ISO 19115-1");
            standard.setEdition(new SimpleInternationalString("2014"));
            md.getMetadataStandards().add(standard);
        }
        {
            /*
             * Spatial representation info : Georectified
             *   ├─Number of dimensions………………………………………………… 2
             *   ├─Axis dimension properties (1 de 2)…………… Row
             *   │   ├─Dimension size……………………………………………………… 7 777
             *   │   └─Resolution………………………………………………………………… 10
             *   ├─Axis dimension properties (2 de 2)…………… Column
             *   │   ├─Dimension size……………………………………………………… 2 233
             *   │   └─Resolution………………………………………………………………… 5
             *   ├─Cell geometry…………………………………………………………………… Area
             *   ├─Transformation parameter availability…… false
             *   ├─Check point availability……………………………………… false
             *   └─Point in pixel………………………………………………………………… Upper right
             */
            final DefaultGeorectified georectified = new DefaultGeorectified();
            georectified.setNumberOfDimensions(2);
            final DefaultDimension rows = new DefaultDimension(DimensionNameType.ROW,    7777);
            final DefaultDimension cols = new DefaultDimension(DimensionNameType.COLUMN, 2233);
            rows.setResolution(10.0);
            cols.setResolution( 5.0);
            georectified.setAxisDimensionProperties(Arrays.asList(rows, cols));
            georectified.setCellGeometry(CellGeometry.AREA);
            georectified.setPointInPixel(PixelOrientation.UPPER_RIGHT);
            md.getSpatialRepresentationInfo().add(georectified);
        }
        {
            // Reference System Information
            final ReferenceSystemMetadata refSystem = new ReferenceSystemMetadata();
            final DefaultCitation cit = new DefaultCitation("Atlantis grid");
            cit.setDates(dateInfo);
            {
                //  Responsibilities
                final DefaultOrganisation org = new DefaultOrganisation();
                org.setName(new SimpleInternationalString("Atlantis national mapping agency"));
                cit.getCitedResponsibleParties().add(new DefaultResponsibility(Role.PUBLISHER, null, org));
            }
            // Identifier
            final DefaultIdentifier id = new DefaultIdentifier("AG9000");
            id.setAuthority(cit);
            id.setCodeSpace("rs.id.ns");
            id.setVersion("1.0");
            id.setDescription(new SimpleInternationalString("An imaginary reference system."));
            refSystem.setName(id);
            md.getReferenceSystemInfo().add(refSystem);
        }
        {
            /*
             * Extended element information…… ExtendedElementName
             *   ├─Parent entity………………………………… VirtualObject
             *   ├─Definition………………………………………… An extended element not included in the standard.
             *   ├─Obligation………………………………………… Conditional
             *   ├─Condition…………………………………………… Presents in “Imaginary work” profile.
             *   ├─Data type…………………………………………… Meta class
             *   ├─Maximum occurrence…………………… 3
             *   ├─Domain value…………………………………… Alpha, beta or gamma.
             *   ├─Rule………………………………………………………… Element exists in cited resource.
             *   └─Rationale…………………………………………… For testing extended elements.
             */
            final DefaultMetadataExtensionInformation extension = new DefaultMetadataExtensionInformation();
            extension.setExtensionOnLineResource(onlineResource);
            final DefaultExtendedElementInformation elementInfo = new DefaultExtendedElementInformation();
            elementInfo.setName("ExtendedElementName");
            elementInfo.setDefinition(new SimpleInternationalString("An extended element not included in the standard."));
            elementInfo.setObligation(Obligation.CONDITIONAL);
            elementInfo.setCondition(new SimpleInternationalString("Presents in “Imaginary work” profile."));
            elementInfo.setDataType(Datatype.META_CLASS);
            elementInfo.setMaximumOccurrence(3);
            elementInfo.setDomainValue(new SimpleInternationalString("Alpha, beta or gamma."));
            elementInfo.setShortName("ExtEltName");
            elementInfo.setDomainCode(1234);
            elementInfo.setParentEntity(Collections.singleton("VirtualObject"));
            elementInfo.setRule(new SimpleInternationalString("Element exists in cited resource."));
            elementInfo.setRationale(new SimpleInternationalString("For testing extended elements."));
            extension.getExtendedElementInformation().add(elementInfo);
            md.getMetadataExtensionInfo().add(extension);
        }
        /*
         * Data identification info
         *   ├─Abstract………………… Méta-données pour une carte imaginaire.
         *   └─Purpose…………………… For XML (un)marshalling tests.
         */
        final DefaultDataIdentification dataId = new DefaultDataIdentification();
        {
            final DefaultInternationalString description = new DefaultInternationalString();
            description.add(Locale.ENGLISH, "Metadata for an imaginary map.");
            description.add(Locale.FRENCH,  "Méta-données pour une carte imaginaire.");
            dataId.setAbstract(description);
            dataId.setPurpose(new SimpleInternationalString("For XML (un)marshalling tests."));
        }
        final Collection<Extent> extents;
        {
            /*
             * Extent……………………………………………………………… Azores
             *   ├─Geographic element
             *   │   ├─West bound longitude…… 24°30′W
             *   │   ├─East bound longitude…… 32°W
             *   │   ├─South bound latitude…… 36°45′N
             *   │   ├─North bound latitude…… 40°N
             *   │   └─Extent type code……………… true
             *   └─Temporal element
             */
            final DefaultExtent extent = new DefaultExtent();
            extent.setDescription(new SimpleInternationalString("Azores"));
            {
                final DefaultGeographicBoundingBox bbox = new DefaultGeographicBoundingBox();
                bbox.setInclusion(true);
                bbox.setNorthBoundLatitude( 40.00);
                bbox.setEastBoundLongitude(-32.00);
                bbox.setSouthBoundLatitude( 36.75);
                bbox.setWestBoundLongitude(-24.50);
                extent.getGeographicElements().add(bbox);
            }
            final DefaultTemporalExtent temporal = new DefaultTemporalExtent();
            extent.getTemporalElements().add(temporal);
            extent.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "azores");     // For enabling references
            extents = Collections.singleton(extent);
            dataId.setExtents(extents);
        }
        final Collection<Constraints> resourceConstraints;
        {
            /*
             * Constraints
             *   ├─Use limitation…………………………………… Not for navigation.
             *   ├─Constraint application scope
             *   │   └─Level………………………………………………… Document
             *   ├─Graphic
             *   │   ├─File name……………………………………… ocean.png
             *   │   ├─File description…………………… Somewhere in the Atlantic ocean
             *   │   ├─File type……………………………………… PNG image
             *   │   ├─Linkage
             *   │   └─Image constraints
             *   └─Releasability
             *       └─Statement……………………………………… Public domain
             */
            final DefaultConstraints constraint = new DefaultConstraints();
            final DefaultBrowseGraphic graphic = new DefaultBrowseGraphic(new URI("ocean.png"));
            graphic.setFileDescription(new SimpleInternationalString("Somewhere in the Atlantic ocean"));
            graphic.setFileType("PNG image");
            graphic.getImageConstraints().add(new DefaultConstraints());
            graphic.getLinkages().add(new DefaultOnlineResource());
            constraint.getGraphics().add(graphic);
            constraint.setUseLimitations(Collections.singleton(new SimpleInternationalString("Not for navigation.")));

            // Releasability
            final DefaultReleasability releasability = new DefaultReleasability();
            releasability.setStatement(new SimpleInternationalString("Public domain"));
            constraint.setReleasability(releasability);
            constraint.setConstraintApplicationScope(new DefaultScope(ScopeCode.DOCUMENT));
            constraint.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "public");         // For enabling references
            resourceConstraints = Collections.singleton(constraint);
            dataId.setResourceConstraints(resourceConstraints);
        }
        dataId.getSpatialRepresentationTypes().add(SpatialRepresentationType.GRID);
        {
            // Spatial resolution
            final DefaultResolution resolution = new DefaultResolution();
            resolution.setDistance(56777.0);
            dataId.getSpatialResolutions().add(resolution);
        }
        dataId.setTopicCategories(Arrays.asList(TopicCategory.OCEANS, TopicCategory.SOCIETY));
        dataId.getStatus().add(Progress.HISTORICAL_ARCHIVE);
        /*
         * Citation………………………………………………………… A lost island
         *   ├─Alternate title (1 de 2)…… Island lost again
         *   ├─Alternate title (2 de 2)…… Map example
         *   ├─Date………………………………………………………… 2018-04-09 00:00:00
         *   │   └─Date type………………………………… Création
         *   ├─Edition………………………………………………… First edition
         *   └─Edition date…………………………………… 2018-04-10 00:00:00
         */
        final DefaultCitation cit = new DefaultCitation();
        cit.setTitle(new SimpleInternationalString("A lost island"));
        cit.setEdition(new SimpleInternationalString("First edition"));
        cit.setEditionDate(new Date(1523311200000L));
        cit.setCollectiveTitle(new SimpleInternationalString("Popular legends"));
        cit.setAlternateTitles(Arrays.asList(new SimpleInternationalString("Island lost again"),
                                             new Anchor(new URI("http://map-example.com"), "Map example")));
        cit.getDates().add(new DefaultCitationDate(new Date(1523224800000L), DateType.CREATION));
        cit.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "lost-island");
        dataId.setCitation(cit);
        dataId.setTemporalResolutions(Collections.emptySet());              // TODO: depends on sis-temporal
        final Collection<MaintenanceInformation> resourceMaintenances;
        {
            /*
             * Maintenance information
             *   ├─Maintenance and update frequency…… Not planned
             *   ├─Maintenance date……………………………………………… 3000-01-01 00:00:00
             *   │   └─Date type……………………………………………………… Révision
             *   └─Maintenance scope
             *       ├─Level………………………………………………………………… Model
             *       └─Level description
             *           └─Dataset………………………………………………… Imaginary map
             */
            DefaultMaintenanceInformation maintenanceInfo = new DefaultMaintenanceInformation();
            maintenanceInfo.setMaintenanceAndUpdateFrequency(MaintenanceFrequency.NOT_PLANNED);
            maintenanceInfo.getMaintenanceDates().add(new DefaultCitationDate(new Date(32503676400000L), DateType.REVISION));
            final DefaultScope maintenanceScope = new DefaultScope();
            maintenanceScope.setLevel(ScopeCode.MODEL);
            {
                // Scope level descriptions
                final DefaultScopeDescription scopeDescription = new DefaultScopeDescription();
                scopeDescription.setDataset("Imaginary map");
                maintenanceScope.getLevelDescription().add(scopeDescription);
            }
            maintenanceInfo.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "not-planned");
            maintenanceInfo.getMaintenanceScopes().add(maintenanceScope);
            resourceMaintenances = Collections.singleton(maintenanceInfo);
            dataId.setResourceMaintenances(resourceMaintenances);
        }
        {
            /*
             * Format
             *   ├─Format specification citation…… Portable Network Graphics
             *   │   ├─Alternate title……………………………… PNG
             *   │   └─Edition…………………………………………………… November 2003
             *   ├─Amendment number……………………………………… Second edition
             *   └─File decompression technique……… L77 / Huffman coding
             */
            final DefaultFormat resourceFormat = new DefaultFormat();
            resourceFormat.setName(new SimpleInternationalString("PNG"));
            resourceFormat.setSpecification(new SimpleInternationalString("Portable Network Graphics"));
            resourceFormat.setAmendmentNumber(new SimpleInternationalString("Second edition"));
            resourceFormat.setVersion(new SimpleInternationalString("November 2003"));
            resourceFormat.setFileDecompressionTechnique(new SimpleInternationalString("L77 / Huffman coding"));
            dataId.getResourceFormats().add(resourceFormat);
        }
        final Collection<Keywords> descriptiveKeywords;
        {
            /*
             * Keywords
             *   ├─Thesaurus name………… Plato's dialogues
             *   ├─Keyword class…………… Greek elements
             *   ├─Keyword (1 de 2)…… Water
             *   ├─Keyword (2 de 2)…… Aether
             *   └─Type…………………………………… Theme
             */
            final DefaultKeywords keywords = new DefaultKeywords();
            keywords.setType(KeywordType.THEME);
            keywords.setThesaurusName(new DefaultCitation("Plato's dialogues"));
            final DefaultKeywordClass keywordClass = new DefaultKeywordClass();
            keywordClass.setClassName(new SimpleInternationalString("Greek elements"));
            keywords.setKeywordClass(keywordClass);
            keywords.setKeywords(Arrays.asList(new SimpleInternationalString("Water"),
                                               new SimpleInternationalString("Aether")));
            keywords.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "greek-elements");
            descriptiveKeywords = Collections.singleton(keywords);
            dataId.setDescriptiveKeywords(descriptiveKeywords);
        }
        {
            /*
             * Usage………………………………………………………………………… For testing purpose only.
             *   ├─Usage date time…………………………………… 2018-04-10 14:00:00
             *   ├─User determined limitations…… Not to be used outside MarshallingTest.java test file.
             *   └─Response……………………………………………………… Random elements
             */
            final DefaultUsage usage = new DefaultUsage();
            usage.setSpecificUsage(new SimpleInternationalString("For testing purpose only."));
            usage.setUsageDate(new Date(1523361600000L));
            usage.setResponses(Collections.singleton(new SimpleInternationalString("Random elements")));
            usage.setUserDeterminedLimitations(new SimpleInternationalString("Not to be used outside MarshallingTest.java test file."));
            dataId.getResourceSpecificUsages().add(usage);
        }
        final Collection<AssociatedResource> associatedResources;
        {
            // Associated resources (AggregationInfo in 19139)
            final DefaultAssociatedResource associatedResource = new DefaultAssociatedResource();
            associatedResource.setAssociationType(AssociationType.DEPENDENCY);
            associatedResource.setInitiativeType(InitiativeType.EXPERIMENT);
            associatedResource.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "dependency");
            associatedResources = Collections.singleton(associatedResource);
            dataId.setAssociatedResources(associatedResources);
        }
        dataId.setLanguages(languages);     // Locales (ISO 19115:2014) a.k.a Languages and CharacterSets (ISO 19115:2003)
        dataId.setCharacterSets(charSets);
        dataId.setEnvironmentDescription (new SimpleInternationalString("High humidity."));
        dataId.setSupplementalInformation(new SimpleInternationalString("High water pressure."));
        {
            // Service identification info
            final DefaultServiceIdentification serviceId = new DefaultServiceIdentification();
            serviceId.setCitation(cit);
            serviceId.setAbstract(new SimpleInternationalString("An inspiration for story tellers."));
            serviceId.setExtents(extents);
            serviceId.setResourceMaintenances(resourceMaintenances);
            serviceId.setDescriptiveKeywords(descriptiveKeywords);
            serviceId.setResourceConstraints(resourceConstraints);
            serviceId.setAssociatedResources(associatedResources);
            serviceId.setServiceTypeVersions(Collections.singleton("Version 1000+"));
            // TODO: Coupled resources
            final DefaultCoupledResource coupledResource = new DefaultCoupledResource();
            serviceId.getCoupledResources().add(coupledResource);
            serviceId.setCouplingType(CouplingType.LOOSE);
            final DefaultOperationMetadata operationMetadata = new DefaultOperationMetadata();
            {
                operationMetadata.setOperationName("Authoring");
                operationMetadata.setOperationDescription(new SimpleInternationalString("Write a book."));
                operationMetadata.setInvocationName(new SimpleInternationalString("someMethodName"));
                operationMetadata.getDistributedComputingPlatforms().add(DistributedComputingPlatform.JAVA);
            }
            serviceId.getContainsOperations().add(operationMetadata);
            serviceId.getOperatesOn().add(dataId);
            md.setIdentificationInfo(Arrays.asList(dataId, serviceId));
        }
        {
            // Content info
            final DefaultCoverageDescription coverageDescription;
            {
                coverageDescription = new DefaultCoverageDescription();
                // Attribute description
                final DefaultRecordSchema schema = new DefaultRecordSchema(null, null, "IslandFeatures");
                final Map<CharSequence,Class<?>> members = new LinkedHashMap<>();
                members.put("city",      String.class);
                members.put("latitude",  Double.class);
                members.put("longitude", Double.class);
                final RecordType recordType = schema.createRecordType("SettledArea", members);
                coverageDescription.setAttributeDescription(recordType);
                {
                    /*
                     * Attribute group
                     *   ├─Content type…………………… Auxilliary information
                     *   ├─Attribute (1 de 2)…… 42
                     *   │   ├─Description…………… Population density
                     *   │   └─Name
                     *   └─Attribute (2 de 2)
                     *       ├─Description…………… Temperature
                     *       ├─Max value………………… 22,22
                     *       ├─Min value………………… 11,11
                     *       ├─Units…………………………… °C
                     *       └─Scale factor………… 1,5
                     */
                    final DefaultAttributeGroup attributeGroup = new DefaultAttributeGroup();
                    attributeGroup.getContentTypes().add(CoverageContentType.AUXILLARY_INFORMATION);
                    // Attributes
                    final DefaultRangeDimension rangeDimension = new DefaultRangeDimension();
                    rangeDimension.setDescription(new SimpleInternationalString("Population density"));
                    rangeDimension.setSequenceIdentifier(Names.createMemberName(null, null, "42", Integer.class));
                    rangeDimension.getNames().add(new DefaultIdentifier());
                    final DefaultSampleDimension sampleDimension = new DefaultSampleDimension();
                    sampleDimension.setDescription(new SimpleInternationalString("Temperature"));
                    sampleDimension.setMinValue(11.11);
                    sampleDimension.setMaxValue(22.22);
                    sampleDimension.setUnits(Units.CELSIUS);
                    sampleDimension.setScaleFactor(1.5);
                    attributeGroup.setAttributes(Arrays.asList(rangeDimension, sampleDimension));
                    coverageDescription.getAttributeGroups().add(attributeGroup);
                }
            }
            // Feature Catalogue Description
            final DefaultFeatureCatalogueDescription featureCatalogueDescription = new DefaultFeatureCatalogueDescription();
            featureCatalogueDescription.setIncludedWithDataset(true);
            featureCatalogueDescription.setCompliant(true);
            md.setContentInfo(Arrays.asList(coverageDescription, featureCatalogueDescription));
        }
        return md;
    }

    /**
     * Tests marshalling of an ISO 19139:2007 document (based on ISO 19115:2003 model).
     * Current implementation merely tests that marshalling does not produce exception.
     *
     * @throws URISyntaxException if an error occurred while creating the metadata object.
     * @throws JAXBException if an error occurred while marshalling the document.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-400">SIS-400</a>
     */
    @Test
    public void testLegacySchema() throws URISyntaxException, JAXBException {
        legacyXML = true;
        final DefaultMetadata md = metadata();
        marshaller.setProperty(XML.METADATA_VERSION, VERSION_2007);
        marshaller.marshal(md, output);
        recycle();
    }

    /**
     * Tests marshalling of an ISO 19115-3 document (based on ISO 19115:2014 model).
     * Current implementation merely tests that marshalling does not produce exception.
     *
     * @throws URISyntaxException if an error occurred while creating the metadata object.
     * @throws JAXBException if an error occurred while marshalling the document.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-400">SIS-400</a>
     */
    @Test
    public void testCurrentSchema() throws JAXBException, URISyntaxException {
        final DefaultMetadata md = metadata();
        marshaller.setProperty(XML.METADATA_VERSION, VERSION_2014);
        marshaller.marshal(md, output);
        recycle();
    }

    /**
     * Invoked only on success, for recycling the marshaller.
     */
    private void recycle() {
        pool.recycle(marshaller);
    }

    /**
     * For internal {@code DefaultMetadata} usage.
     *
     * @return {@code Object.class}.
     */
    @Override
    public Class<Object> getSourceClass() {
        return Object.class;
    }

    /**
     * Invoked when a warning occurred while marshalling a test XML fragment. Expected warnings are
     * "Can't find resource for bundle {@code java.util.PropertyResourceBundle}, key <cite>Foo</cite>".
     * When marshalling legacy XML only, additional warnings may occur.
     *
     * @param source  ignored.
     * @param warning the warning.
     */
    @Override
    public void warningOccured(final Object source, final LogRecord warning) {
        if (warning.getThrown() instanceof MissingResourceException) {
            assertNull("Expected a warning message without parameters.", warning.getParameters());
            return;
        }
        final String message = warning.getMessage();
        if (legacyXML) {
            assertEquals("IgnoredPropertiesAfterFirst_1", message);
            assertArrayEquals(new String[] {"RangeDimension"}, warning.getParameters());
        } else {
            fail("Unexpected logging message: " + message);
        }
    }
}
