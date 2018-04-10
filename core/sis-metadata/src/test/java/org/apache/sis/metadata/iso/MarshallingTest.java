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
import org.apache.sis.internal.jaxb.gcx.Anchor;
import org.apache.sis.internal.jaxb.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.test.XMLTestCase;
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
public final class MarshallingTest extends XMLTestCase implements WarningListener<Object> {
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
    private DefaultMetadata metadata() throws URISyntaxException {
        /*
         * Metadata
         *   ├─Metadata identifier…… dummy-metadata
         *   │   └─Code space………………… sis.test
         *   ├─Parent metadata……………… A parent metadata
         *   │   └─Identifier………………… dummy-parent-metadata
         *   │       └─Code space……… sis.test
         *   ├─Language (1 de 2)………… English
         *   ├─Language (2 de 2)………… French (Canada)
         *   ├─Character set…………………… ISO-8859-1
         *   └─Metadata scope
         *       ├─Resource scope……… Dataset
         *       └─Name………………………………… Metadata for an (imaginary) data set
         *
         * Some code are indented for readability and more local variable scopes.
         */
        final DefaultMetadata md = new DefaultMetadata();
        {
            // Metadata identifier
            final DefaultIdentifier id = new DefaultIdentifier("dummy-metadata");
            id.setCodeSpace("sis.test");
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
            final DefaultIdentifier parentId = new DefaultIdentifier("dummy-parent-metadata");
            parentId.setCodeSpace("sis.test");
            parent.getIdentifiers().add(parentId);
            md.setParentMetadata(parent);
        }
        // mdb:metadataScope (hierarchyLevel and hierarchyLevelName in legacy ISO 19115:2003 model)
        md.getMetadataScopes().add(new DefaultMetadataScope(ScopeCode.DATASET, "Metadata for an (imaginary) data set"));
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
             *   │   │   ├─Delivery point…………………………… At the desk
             *   │   │   ├─City……………………………………………………… Metropolis city
             *   │   │   ├─Administrative area……………… Utopia area
             *   │   │   ├─Postal code…………………………………… A1A 2C2
             *   │   │   ├─Country……………………………………………… Atlantis island
             *   │   │   └─Electronic mail address…… test@example.com
             *   │   ├─Online resource
             *   │   │   ├─Linkage……………………………………………… http://example.com
             *   │   │   ├─Protocol…………………………………………… Hyper-text
             *   │   │   ├─Application profile……………… Test only
             *   │   │   ├─Name……………………………………………………… Timaeus & Critias
             *   │   │   ├─Description…………………………………… A dialog between philosophers.
             *   │   │   └─Function…………………………………………… Search
             *   │   ├─Hours of service………………………………… Weekdays 9:00 AM - 5:00 PM
             *   │   ├─Contact instructions……………………… Knock at the door
             *   │   └─Contact type…………………………………………… Imaginary
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
                    address.setDeliveryPoints(Collections.singleton(new SimpleInternationalString("At the desk")));
                    address.getElectronicMailAddresses().add("test@example.com");
                    address.setCity(new SimpleInternationalString("Metropolis city"));
                    address.setAdministrativeArea(new SimpleInternationalString("Utopia area"));
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
                onlineResource.setApplicationProfile("Test only");
                onlineResource.setFunction(OnLineFunction.SEARCH);
                contact.getOnlineResources().add(onlineResource);
                contact.setHoursOfService(Collections.singleton(new SimpleInternationalString("Weekdays 9:00 AM - 5:00 PM")));
                contact.setContactInstructions(new SimpleInternationalString("Knock at the door"));
                contact.setContactType(new SimpleInternationalString("Imaginary"));
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
             * Spatial Representation Info
             *
             * Georectified
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
            final DefaultDimension dim1 = new DefaultDimension(DimensionNameType.ROW,    7777);
            final DefaultDimension dim2 = new DefaultDimension(DimensionNameType.COLUMN, 2233);
            dim1.setResolution(10.0);
            dim2.setResolution( 5.0);
            georectified.setAxisDimensionProperties(Arrays.asList(dim1, dim2));
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
            id.setCodeSpace("sis.test");
            id.setVersion("1.0");
            id.setDescription(new SimpleInternationalString("An imaginary reference system."));
            refSystem.setName(id);
            md.getReferenceSystemInfo().add(refSystem);
        }
        {
            // Metadata extension information.
            final DefaultMetadataExtensionInformation extension = new DefaultMetadataExtensionInformation();
            extension.setExtensionOnLineResource(onlineResource);
            final DefaultExtendedElementInformation elementInfo = new DefaultExtendedElementInformation();
            elementInfo.setName("extendedElementInfoName");
            elementInfo.setDefinition(new SimpleInternationalString("definition"));
            elementInfo.setObligation(Obligation.MANDATORY);
            elementInfo.setCondition(new SimpleInternationalString("condition"));
            elementInfo.setDataType(Datatype.META_CLASS);
            elementInfo.setMaximumOccurrence(1);
            elementInfo.setDomainValue(new SimpleInternationalString("domainValue"));
            elementInfo.setShortName("shortName");
            elementInfo.setDomainCode(1234);
            elementInfo.setParentEntity(Collections.singleton("parentEntity"));
            elementInfo.setRule(new SimpleInternationalString("rule"));
            elementInfo.setRationale(new SimpleInternationalString("rationale"));
            extension.getExtendedElementInformation().add(elementInfo);
            md.getMetadataExtensionInfo().add(extension);
        }
        // Data identification info
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
            // Extents
            final DefaultExtent extent = new DefaultExtent();
            extent.setDescription(new SimpleInternationalString("Azores"));
            {
                // Bounding box
                final DefaultGeographicBoundingBox boundingBox = new DefaultGeographicBoundingBox();
                boundingBox.setInclusion(true);
                boundingBox.setNorthBoundLatitude( 39);
                boundingBox.setEastBoundLongitude(-28);
                boundingBox.setSouthBoundLatitude( 35);
                boundingBox.setWestBoundLongitude(-22);
                extent.getGeographicElements().add(boundingBox);
            }
            final DefaultTemporalExtent tempExtent = new DefaultTemporalExtent();
            extent.getTemporalElements().add(tempExtent);
            extents = Collections.singleton(extent);
            dataId.setExtents(extents);
        }
        // Resource constraints
        final Collection<Constraints> resourceConstraints;
        final Collection<Citation> emptyCitations = Collections.singleton(new DefaultCitation());
        {
            DefaultConstraints constraint = new DefaultConstraints();
            constraint.getResponsibleParties().add(new DefaultResponsibility());
            constraint.setReferences(emptyCitations);
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
            resourceConstraints = Collections.singleton(constraint);
            dataId.setResourceConstraints(resourceConstraints);
        }
        // Points of contact
        final Collection<Responsibility> pocs = Collections.singleton(new DefaultResponsibility());
        dataId.setPointOfContacts(pocs);
        dataId.getSpatialRepresentationTypes().add(SpatialRepresentationType.GRID);
        {
            // Spatial resolution
            final DefaultResolution resolution = new DefaultResolution();
            resolution.setDistance(56777.0);
            dataId.getSpatialResolutions().add(resolution);
        }
        dataId.setTopicCategories(Arrays.asList(TopicCategory.OCEANS, TopicCategory.SOCIETY));
        dataId.getStatus().add(Progress.HISTORICAL_ARCHIVE);

        // Citation
        final DefaultCitation cit = new DefaultCitation();
        cit.setTitle(new SimpleInternationalString("A lost island"));
        cit.setEdition(new SimpleInternationalString("First edition"));
        cit.setEditionDate(new Date(1523311200000L));
        cit.setCollectiveTitle(new SimpleInternationalString("Popular legends"));
        cit.setAlternateTitles(Arrays.asList(new SimpleInternationalString("Island lost again"),
                                             new Anchor(new URI("http://map-example.com"), "Map example")));
        cit.getDates().add(new DefaultCitationDate(new Date(1523224800000L), DateType.CREATION));
        dataId.setCitation(cit);
        dataId.setTemporalResolutions(Collections.emptySet());              // TODO: depends on sis-temporal
        final Collection<MaintenanceInformation> resourceMaintenances;
        {
            // Resource maintenance
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
            maintenanceInfo.getMaintenanceScopes().add(maintenanceScope);
            resourceMaintenances = Collections.singleton(maintenanceInfo);
            dataId.setResourceMaintenances(resourceMaintenances);
        }
        {
            // Resource format (MD_Format)
            final DefaultFormat resourceFormat = new DefaultFormat();
            resourceFormat.setName(new SimpleInternationalString("name"));
            resourceFormat.setAmendmentNumber(new SimpleInternationalString("amendmentNumber"));
            resourceFormat.setVersion(new SimpleInternationalString("version"));
            resourceFormat.setSpecification(new SimpleInternationalString("specification"));
            resourceFormat.setFileDecompressionTechnique(new SimpleInternationalString("decompressionTechnique"));
            dataId.getResourceFormats().add(resourceFormat);
        }
        final Collection<Keywords> descriptiveKeywords;
        {
            // Descriptive keywords (MD_Keywords)
            final DefaultKeywords keywords = new DefaultKeywords();
            keywords.setType(KeywordType.THEME);
            keywords.setThesaurusName(new DefaultCitation());
            final DefaultKeywordClass keywordClass = new DefaultKeywordClass();
            keywordClass.setClassName(new SimpleInternationalString("name"));
            keywords.setKeywordClass(keywordClass);
            keywords.setKeywords(Arrays.asList(new SimpleInternationalString("keyword1"),
                                               new SimpleInternationalString("keyword2")));
            descriptiveKeywords = Collections.singleton(keywords);
            dataId.setDescriptiveKeywords(descriptiveKeywords);
        }
        {
            // Resource specific usage
            final DefaultUsage usage = new DefaultUsage();
            usage.setSpecificUsage(new SimpleInternationalString("specificUsage"));
            usage.setUsageDate(new Date());
            usage.setResponses(Collections.singleton(new SimpleInternationalString("response")));
            usage.setAdditionalDocumentation(emptyCitations);
            usage.setIdentifiedIssues(emptyCitations);
            usage.setUserDeterminedLimitations(new SimpleInternationalString("userDeterminedLimitations"));
            final DefaultResponsibility resp = new DefaultResponsibility();
            usage.getUserContactInfo().add(resp);
            dataId.getResourceSpecificUsages().add(usage);
        }
        final Collection<AssociatedResource> associatedResources;
        {
            // Associated resources (AggregationInfo in 19139)
            final DefaultAssociatedResource associatedResource = new DefaultAssociatedResource();
            DefaultCitation associatedResourceCitation = new DefaultCitation();
            associatedResource.setName(associatedResourceCitation);
            associatedResource.setAssociationType(AssociationType.DEPENDENCY);
            associatedResource.setInitiativeType(InitiativeType.EXPERIMENT);
            associatedResources = Collections.singleton(associatedResource);
            dataId.setAssociatedResources(associatedResources);
        }
        dataId.setLanguages(languages);     // Locales (ISO 19115:2014) a.k.a Languages and CharacterSets (ISO 19115:2003)
        dataId.setCharacterSets(charSets);
        dataId.setEnvironmentDescription (new SimpleInternationalString("environmentDescription"));
        dataId.setSupplementalInformation(new SimpleInternationalString("supplementalInformation"));
        {
            // Service identification info
            final DefaultServiceIdentification serviceId = new DefaultServiceIdentification();
            serviceId.setCitation(cit);
            serviceId.setAbstract(new SimpleInternationalString("abstract"));
            serviceId.setPointOfContacts(pocs);
            serviceId.setExtents(extents);
            serviceId.setResourceMaintenances(resourceMaintenances);
            serviceId.setDescriptiveKeywords(descriptiveKeywords);
            serviceId.setResourceConstraints(resourceConstraints);
            serviceId.setAssociatedResources(associatedResources);
            serviceId.setServiceTypeVersions(Collections.singleton("serviceTypeVersion"));
            // TODO: Coupled resources
            final DefaultCoupledResource coupledResource = new DefaultCoupledResource();
            serviceId.getCoupledResources().add(coupledResource);
            serviceId.setCouplingType(CouplingType.TIGHT);
            final DefaultOperationMetadata operationMetadata = new DefaultOperationMetadata();
            {
                operationMetadata.setOperationName("operationName");
                operationMetadata.setOperationDescription(new SimpleInternationalString("operationDescription"));
                operationMetadata.setInvocationName(new SimpleInternationalString("invocationName"));
                operationMetadata.getDistributedComputingPlatforms().add(DistributedComputingPlatform.JAVA);
                operationMetadata.getConnectPoints().add(new DefaultOnlineResource());
                // Parameters are unchanged according to crosswalk. Don't need to do this one.
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
                final DefaultRecordSchema schema = new DefaultRecordSchema(null, null, "MySchema");
                final Map<CharSequence,Class<?>> members = new LinkedHashMap<>();
                members.put("city",      String.class);
                members.put("latitude",  Double.class);
                members.put("longitude", Double.class);
                final RecordType recordType = schema.createRecordType("MyRecordType", members);
                coverageDescription.setAttributeDescription(recordType);
                {
                    // Attribute group
                    final DefaultAttributeGroup attributeGroup = new DefaultAttributeGroup();
                    attributeGroup.getContentTypes().add(CoverageContentType.AUXILLARY_INFORMATION);
                    // Attributes
                    final DefaultRangeDimension rangeDimension = new DefaultRangeDimension();
                    rangeDimension.setDescription(new SimpleInternationalString("descriptor"));
                    // TODO: Sequence identifier
                    /*DefaultMemberName memberName = DefaultNameFactory.createMemberName(NameSpace, CharSequence, TypeName);
                    rangeDimension.setSequenceIdentifier(memberName);*/
                    // Names
                    rangeDimension.getNames().add(new DefaultIdentifier());
                    final DefaultSampleDimension sampleDimension = new DefaultSampleDimension();
                    sampleDimension.setDescription(new SimpleInternationalString("descriptor"));
                    sampleDimension.setMinValue(11.11);
                    sampleDimension.setMaxValue(22.22);
                    sampleDimension.setUnits(Units.FAHRENHEIT);
                    sampleDimension.setScaleFactor(1.0);
                    final Collection<RangeDimension> rangeDimensions = Arrays.asList(rangeDimension, sampleDimension);
                    attributeGroup.setAttributes(rangeDimensions);
                    coverageDescription.setDimensions(rangeDimensions);
                    // coverageDescription.getAttributeGroups().add(attributeGroup);
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
     * Invoked when a warning occurred while marshalling a test XML fragment.
     * The only expected warning message is "Can't find resource for bundle
     * {@code java.util.PropertyResourceBundle}, key {@code MD_DimensionNameTypeCode.row}".
     *
     * @param source  ignored.
     * @param warning the warning.
     */
    @Override
    public void warningOccured(final Object source, final LogRecord warning) {
        final String message = warning.getMessage();
        assertNotNull(message, message.contains("MD_DimensionNameTypeCode.row"));
        assertNull("Expected a warning message without parameters.", warning.getParameters());
    }
}
