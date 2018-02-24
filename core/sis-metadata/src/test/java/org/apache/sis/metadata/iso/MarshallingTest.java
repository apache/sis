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
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.measure.Units;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.internal.jaxb.gmx.Anchor;
import org.apache.sis.internal.jaxb.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Simple test cases for marshalling a {@link DefaultMetadata} object to an XML file.
 * This class is used to test the ISO 19115-3 metadata standard implementation.
 *
 * @author  Cullen Rombach (Image Matters)
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
         * Some code are indented for readability and more local variable scopes.
         */
        final DefaultMetadata md = new DefaultMetadata();
        {
            // Metadata identifier
            final DefaultIdentifier id = new DefaultIdentifier("fileIdentifier");
            id.setCodeSpace("fileIdentifierNamespace");
            md.setMetadataIdentifier(id);
        }
        // Languages
        final Collection<Locale> languages = Arrays.asList(Locale.US, Locale.GERMANY);
        md.setLanguages(languages);

        // Character Sets (character encoding)
        final Collection<Charset> charSets = Collections.singleton(StandardCharsets.UTF_8);
        md.setCharacterSets(charSets);
        {
            // Parent metadata
            final DefaultCitation parent = new DefaultCitation("parentMetadata");
            final DefaultIdentifier parentId = new DefaultIdentifier("parentMetadata");
            parentId.setCodeSpace("parentMetadataCodeSpace");
            parent.getIdentifiers().add(parentId);
            md.setParentMetadata(parent);
        }
        // mdb:metadataScope (hierarchyLevel and hierarchyLevelName in legacy ISO 19115:2003 model)
        md.getMetadataScopes().add(new DefaultMetadataScope(ScopeCode.DATASET, "hierarchyLevelName"));
        final DefaultOnlineResource onlineResource;
        {
            // Contact information for the parties.
            final DefaultContact contact = new DefaultContact();
            contact.setPhones(Arrays.asList(new DefaultTelephone("555-867-5309", TelephoneType.VOICE),
                                            new DefaultTelephone("555-555-5555", TelephoneType.FACSIMILE)));
            {
                {
                    // Address information
                    final DefaultAddress address = new DefaultAddress();
                    address.setDeliveryPoints(Collections.singleton(new SimpleInternationalString("deliveryPoint")));
                    address.getElectronicMailAddresses().add("test@example.com");
                    address.setCity(new SimpleInternationalString("city"));
                    address.setAdministrativeArea(new SimpleInternationalString("administrativeArea"));
                    address.setPostalCode("postalCode");
                    address.setCountry(new SimpleInternationalString("country"));
                    contact.getAddresses().add(address);
                }
                // Online resources
                onlineResource = new DefaultOnlineResource();
                onlineResource.setLinkage(new URI("http://example.com"));
                onlineResource.setProtocol("protocol");
                onlineResource.setApplicationProfile("applicationProfile");
                onlineResource.setName(new SimpleInternationalString("name"));
                onlineResource.setDescription(new SimpleInternationalString("description"));
                onlineResource.setFunction(OnLineFunction.DOWNLOAD);
                contact.getOnlineResources().add(onlineResource);
                contact.setHoursOfService(Collections.singleton(new SimpleInternationalString("Weekdays 9:00 AM - 5:00 PM")));
                contact.setContactInstructions(new SimpleInternationalString("contactInstructions"));
                contact.setContactType(new SimpleInternationalString("contactType"));
            }
            // Create some DefaultIndividuals
            final DefaultIndividual individual  = new DefaultIndividual("individualName", "positionName", null);
            final DefaultIndividual individual2 = new DefaultIndividual("individualName2", "positionName2", contact);
            final DefaultOrganisation org = new DefaultOrganisation("organisationName", null, individual, contact);
            md.setContacts(Arrays.asList(new DefaultResponsibility(Role.POINT_OF_CONTACT, null, org),
                                         new DefaultResponsibility(Role.POINT_OF_CONTACT, null, individual2)));
        }
        // Date info (date stamp in legacy ISO 19115:2003 model)
        final Collection<CitationDate> dateInfo = Collections.singleton(new DefaultCitationDate(new Date(), DateType.CREATION));
        md.setDateInfo(dateInfo);
        {
            // Metadata standard
            final DefaultCitation standard = new DefaultCitation("metadataStandardName");
            standard.setEdition(new SimpleInternationalString("metadataStandardVersion"));
            md.getMetadataStandards().add(standard);
        }
        {
            // Spatial Representation Info
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
            final DefaultCitation cit = new DefaultCitation("refSystemCitationTitle");
            cit.setDates(dateInfo);
            {
                //  Responsibilities
                final DefaultOrganisation org = new DefaultOrganisation();
                org.setName(new SimpleInternationalString("orgName"));
                cit.getCitedResponsibleParties().add(new DefaultResponsibility(Role.PUBLISHER, null, org));
            }
            // Identifier
            final DefaultIdentifier id = new DefaultIdentifier("refSystemCode");
            id.setAuthority(cit);
            id.setCodeSpace("refSystemCodeSpace");
            id.setVersion("1.0");
            id.setDescription(new SimpleInternationalString("refSystemDescription"));
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
        dataId.setAbstract(new SimpleInternationalString("abstract"));
        dataId.setPurpose(new SimpleInternationalString("purpose"));
        final Collection<Extent> extents;
        {
            // Extents
            final DefaultExtent extent = new DefaultExtent();
            extent.setDescription(new SimpleInternationalString("description"));
            {
                // Bounding box
                final DefaultGeographicBoundingBox boundingBox = new DefaultGeographicBoundingBox();
                boundingBox.setInclusion(true);
                boundingBox.setNorthBoundLatitude(11.11);
                boundingBox.setEastBoundLongitude(11.11);
                boundingBox.setSouthBoundLatitude(11.11);
                boundingBox.setWestBoundLongitude(11.11);
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
            final DefaultBrowseGraphic graphic = new DefaultBrowseGraphic(new URI("filename.png"));
            graphic.setFileDescription(new SimpleInternationalString("description"));
            graphic.setFileType("fileType");
            graphic.getImageConstraints().add(new DefaultConstraints());
            graphic.getLinkages().add(new DefaultOnlineResource());
            constraint.getGraphics().add(graphic);
            constraint.setUseLimitations(Collections.singleton(new SimpleInternationalString("useLimitation")));

            // Releasability
            final DefaultReleasability releasability = new DefaultReleasability();
            releasability.setStatement(new SimpleInternationalString("statement"));
            constraint.setReleasability(releasability);
            constraint.setConstraintApplicationScope(new DefaultScope(ScopeCode.APPLICATION));
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
        dataId.setTopicCategories(Arrays.asList(TopicCategory.OCEANS, TopicCategory.FARMING));
        dataId.getStatus().add(Progress.ACCEPTED);

        // Citation
        final DefaultCitation cit = new DefaultCitation();
        cit.setTitle(new SimpleInternationalString("citationTitle"));
        cit.setEdition(new SimpleInternationalString("edition"));
        cit.setEditionDate(new Date());
        cit.setCollectiveTitle(new SimpleInternationalString("collectiveTitle"));
        cit.setAlternateTitles(Arrays.asList(new SimpleInternationalString("alternateTitle"),
                                             new Anchor(new URI("http://example.com"), "alternateTitle")));
        cit.getDates().add(new DefaultCitationDate(new Date(), DateType.CREATION));
        dataId.setCitation(cit);
        dataId.setTemporalResolutions(Collections.emptySet());              // TODO: depends on sis-temporal
        final Collection<MaintenanceInformation> resourceMaintenances;
        {
            // Resource maintenance
            DefaultMaintenanceInformation maintenanceInfo = new DefaultMaintenanceInformation();
            maintenanceInfo.setMaintenanceAndUpdateFrequency(MaintenanceFrequency.ANNUALLY);
            maintenanceInfo.getMaintenanceDates().add(new DefaultCitationDate(new Date(), DateType.NEXT_UPDATE));
            final DefaultScope maintenanceScope = new DefaultScope();
            maintenanceScope.setLevel(ScopeCode.APPLICATION);
            {
                // Scope level descriptions
                final DefaultScopeDescription scopeDescription = new DefaultScopeDescription();
                scopeDescription.setDataset("dataset");
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
