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
package org.apache.sis.test.integration;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;

import org.opengis.metadata.*;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.constraint.*;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.maintenance.*;
import org.opengis.metadata.spatial.GeometricObjectType;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.datum.VerticalDatumType;

import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.*;
import org.apache.sis.metadata.iso.citation.*;
import org.apache.sis.metadata.iso.constraint.*;
import org.apache.sis.metadata.iso.content.*;
import org.apache.sis.metadata.iso.distribution.*;
import org.apache.sis.metadata.iso.extent.*;
import org.apache.sis.metadata.iso.identification.*;
import org.apache.sis.metadata.iso.spatial.*;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.datum.DefaultVerticalDatum;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.internal.jaxb.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.jaxb.gcx.Anchor;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.IdentifierSpace;
import org.apache.sis.xml.NilReason;
import org.apache.sis.xml.XML;

// Test dependencies
import org.apache.sis.test.LoggingWatcher;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.xml.DocumentComparator;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests XML (un)marshalling of a metadata object containing various elements
 * in addition to Coordinate Reference System (CRS) elements.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see org.apache.sis.metadata.iso.DefaultMetadataTest
 *
 * @since 0.5
 */
@DependsOn({
    MetadataVerticalTest.class
})
public final class MetadataTest extends TestCase {
    /**
     * The resource file which contains an XML representation of a {@link Metadata} object.
     */
    private static final String XML_FILE = "Metadata.xml";

    /**
     * A JUnit {@link Rule} for listening to log events. This field is public because JUnit requires us to
     * do so, but should be considered as an implementation details (it should have been a private field).
     */
    @Rule
    public final LoggingWatcher loggings = new LoggingWatcher(Loggers.XML);

    /**
     * Verifies that no unexpected warning has been emitted in any test defined in this class.
     */
    @After
    public void assertNoUnexpectedLog() {
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Programmatically creates the metadata to marshal, or to compare against the unmarshalled metadata.
     *
     * @return the hard-coded representation of {@value #XML_FILE} content.
     */
    private DefaultMetadata createHardCoded() {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataIdentifier(new DefaultIdentifier("test/Metadata.xml"));
        metadata.setLocalesAndCharsets(Map.of(Locale.ENGLISH, StandardCharsets.UTF_8));
        metadata.setMetadataScopes(Set.of(new DefaultMetadataScope(ScopeCode.DATASET, "Pseudo Common Data Index record")));
        metadata.setDateInfo(Set.of(new DefaultCitationDate(TestUtilities.date("2009-01-01 04:00:00"), DateType.CREATION)));
        /*
         * Contact information for the author. The same party will be used for custodian and distributor,
         * with only the role changed. Note that we need to create an instance of the deprecated class,
         * because this is what will be unmarshalled from the XML document.
         */
        @SuppressWarnings("deprecation")
        final DefaultResponsibility author = new DefaultResponsibleParty(Role.AUTHOR);
        final Anchor country = new Anchor(URI.create("SDN:C320:2:FR"), "France"); // Non-public SIS class.
        {
            final DefaultOnlineResource online = new DefaultOnlineResource(URI.create("http://www.ifremer.fr/sismer/"));
            online.setProtocol(Constants.HTTP);
            final DefaultContact contact = new DefaultContact(online);
            contact.getIdentifierMap().putSpecialized(IdentifierSpace.ID, "IFREMER");
            contact.setPhones(List.of(
                    new DefaultTelephone("+33 (0)2 xx.xx.xx.x6", TelephoneType.VOICE),
                    new DefaultTelephone("+33 (0)2 xx.xx.xx.x4", TelephoneType.FACSIMILE)
            ));
            final DefaultAddress address = new DefaultAddress();
            address.setDeliveryPoints(Set.of(new SimpleInternationalString("Brest institute")));
            address.setCity(new SimpleInternationalString("Plouzane"));
            address.setPostalCode("29280");
            address.setCountry(country);
            address.setElectronicMailAddresses(Set.of("xx@xx.fr"));
            contact.setAddresses(Set.of(address));
            author.setParties(Set.of(new DefaultOrganisation("Some marine institute", null, null, contact)));
            metadata.setContacts(Set.of(author));
        }
        /*
         * Data indentification.
         */
        {
            final DefaultCitation citation = new DefaultCitation("Some set of points");
            citation.setAlternateTitles(Set.of(new SimpleInternationalString("Code XYZ")));
            citation.setDates(List.of(
                    new DefaultCitationDate(TestUtilities.date("1990-06-04 22:00:00"), DateType.REVISION),
                    new DefaultCitationDate(TestUtilities.date("1979-08-02 22:00:00"), DateType.CREATION)));
            {
                @SuppressWarnings("deprecation")
                final DefaultResponsibility originator = new DefaultResponsibleParty(Role.ORIGINATOR);
                final DefaultOnlineResource online = new DefaultOnlineResource(URI.create("http://www.com.univ-mrs.fr/LOB/"));
                online.setProtocol(Constants.HTTP);
                final DefaultContact contact = new DefaultContact(online);
                contact.setPhones(List.of(
                        new DefaultTelephone("+33 (0)4 xx.xx.xx.x5", TelephoneType.VOICE),
                        new DefaultTelephone("+33 (0)4 xx.xx.xx.x8", TelephoneType.FACSIMILE)
                ));
                final DefaultAddress address = new DefaultAddress();
                address.setDeliveryPoints(Set.of(new SimpleInternationalString("Oceanology institute")));
                address.setCity(new SimpleInternationalString("Marseille"));
                address.setPostalCode("13288");
                address.setCountry(country);
                contact.setAddresses(Set.of(address));
                originator.setParties(Set.of(new DefaultOrganisation("Oceanology laboratory", null, null, contact)));
                citation.setCitedResponsibleParties(Set.of(originator));
            }
            final DefaultDataIdentification identification = new DefaultDataIdentification(
                    citation,                                                   // Citation
                    "Description of pseudo data for testing purpose only.",     // Abstract
                    Locale.ENGLISH,                                             // Language,
                    TopicCategory.OCEANS);                                      // Topic category
            {
                @SuppressWarnings("deprecation")
                final DefaultResponsibility custodian = new DefaultResponsibleParty(author);
                custodian.setRole(Role.CUSTODIAN);
                identification.setPointOfContacts(Set.of(custodian));
            }
            /*
             * Data indentification / Keywords.
             */
            {
                final DefaultKeywords keyword = new DefaultKeywords(
                        new Anchor(URI.create("SDN:P021:35:ATTN"), "Transmittance and attenuance of the water column"));
                keyword.setType(KeywordType.THEME);
                final DefaultCitation thesaurus = new DefaultCitation("BODC Parameter Discovery Vocabulary");
                thesaurus.setAlternateTitles(Set.of(new SimpleInternationalString("P021")));
                thesaurus.setDates(Set.of(new DefaultCitationDate(TestUtilities.date("2008-11-25 23:00:00"), DateType.REVISION)));
                thesaurus.setEdition(new Anchor(URI.create("SDN:C371:1:35"), "35"));
                thesaurus.setIdentifiers(Set.of(new ImmutableIdentifier(null, null, "http://www.seadatanet.org/urnurl/")));
                keyword.setThesaurusName(thesaurus);
                identification.setDescriptiveKeywords(Set.of(keyword));
            }
            /*
             * Data indentification / Browse graphic.
             */
            {
                final DefaultBrowseGraphic g = new DefaultBrowseGraphic(URI.create("file:///thumbnail.png"));
                g.setFileDescription(new SimpleInternationalString("Arbitrary thumbnail for this test only."));
                identification.setGraphicOverviews(Set.of(g));
            }
            /*
             * Data indentification / Resource constraint.
             */
            {
                final DefaultLegalConstraints constraint = new DefaultLegalConstraints();
                constraint.setAccessConstraints(Set.of(Restriction.LICENCE));
                identification.setResourceConstraints(Set.of(constraint));
            }
            /*
             * Data indentification / Aggregate information.
             */
            {
                @SuppressWarnings("deprecation")
                final DefaultAssociatedResource aggregateInfo = new DefaultAggregateInformation();
                final DefaultCitation name = new DefaultCitation("Some oceanographic campaign");
                name.setAlternateTitles(Set.of(new SimpleInternationalString("Pseudo group of data")));
                name.setDates(Set.of(new DefaultCitationDate(TestUtilities.date("1990-06-04 22:00:00"), DateType.REVISION)));
                aggregateInfo.setName(name);
                aggregateInfo.setInitiativeType(InitiativeType.CAMPAIGN);
                aggregateInfo.setAssociationType(AssociationType.LARGER_WORK_CITATION);
                identification.setAssociatedResources(Set.of(aggregateInfo));
            }
            /*
             * Data indentification / Extent.
             */
            {
                final DefaultCoordinateSystemAxis axis = new DefaultCoordinateSystemAxis(
                        nameAndIdentifier("depth", "Depth", null), "D", AxisDirection.DOWN, Units.METRE);

                final DefaultVerticalCS cs = new DefaultVerticalCS(
                        nameAndIdentifier("depth", "Depth", null), axis);

                final DefaultVerticalDatum datum = new DefaultVerticalDatum(
                        nameAndIdentifier("D28", "Depth below D28", "For testing purpose"), VerticalDatumType.OTHER_SURFACE);

                final DefaultVerticalCRS vcrs = new DefaultVerticalCRS(
                        nameAndIdentifier("D28", "Depth below D28", "CRS for testing purpose"), datum, cs);

                final DefaultTemporalExtent temporal = new DefaultTemporalExtent();
                temporal.setBounds(TestUtilities.date("1990-06-05 00:00:00"), TestUtilities.date("1990-07-02 00:00:00"));
                identification.setExtents(Set.of(new DefaultExtent(
                        null,
                        new DefaultGeographicBoundingBox(1.1666, 1.1667, 36.4, 36.6),
                        new DefaultVerticalExtent(10, 25, vcrs),
                        temporal)));
            }
            /*
             * Data identification / Environmental description and Supplemental information.
             */
            {
                identification.setEnvironmentDescription (new SimpleInternationalString("Possibly cloudy."));
                identification.setSupplementalInformation(new SimpleInternationalString("This metadata has been modified with dummy values."));
            }
            metadata.setIdentificationInfo(Set.of(identification));
        }
        /*
         * Information about spatial representation.
         */
        {
            final DefaultVectorSpatialRepresentation rep = new DefaultVectorSpatialRepresentation();
            final DefaultGeometricObjects geoObj = new DefaultGeometricObjects(GeometricObjectType.POINT);
            rep.setGeometricObjects(Set.of(geoObj));
            metadata.setSpatialRepresentationInfo(Set.of(rep));
        }
        /*
         * Information about Coordinate Reference System.
         */
        {
            final DefaultCitation citation = new DefaultCitation("World Geodetic System 84");
            citation.setAlternateTitles(Set.of(new SimpleInternationalString("L101")));
            citation.setIdentifiers(Set.of(new ImmutableIdentifier(null, null, "SDN:L101:2:4326")));
            citation.setEdition(new Anchor(URI.create("SDN:C371:1:2"), "2"));
            metadata.setReferenceSystemInfo(Set.of(
                    new ReferenceSystemMetadata(new ImmutableIdentifier(citation, "L101", "4326"))));
        }
        /*
         * Information about content.
         */
        {
            final DefaultImageDescription contentInfo = new DefaultImageDescription();
            contentInfo.setCloudCoverPercentage(50.0);
            metadata.setContentInfo(Set.of(contentInfo));
        }
        /*
         * Extension to metadata.
         */
        {
            final DefaultMetadataExtensionInformation extensionInfo = new DefaultMetadataExtensionInformation();
            extensionInfo.setExtendedElementInformation(Set.of(new DefaultExtendedElementInformation(
                    "SDN:EDMO",                                                     // Name
                    "European Directory of Marine Organisations",                   // Definition
                    null,                                                           // Condition
                    Datatype.CODE_LIST,                                             // Data type
                    "SeaDataNet",                                                   // Parent entity
                    "For testing only",                                             // Rule
                    NilReason.MISSING.createNilObject(Responsibility.class))));     // Source
            metadata.setMetadataExtensionInfo(Set.of(extensionInfo));
        }
        /*
         * Distribution information.
         */
        {
            @SuppressWarnings("deprecation")
            final DefaultResponsibility distributor = new DefaultResponsibleParty(author);
            final DefaultDistribution distributionInfo = new DefaultDistribution();
            distributor.setRole(Role.DISTRIBUTOR);
            distributionInfo.setDistributors(Set.of(new DefaultDistributor(distributor)));

            final DefaultFormat format = new DefaultFormat();
            final DefaultCitation specification = new DefaultCitation();
            specification.setAlternateTitles(Set.of(new Anchor(URI.create("SDN:L241:1:MEDATLAS"), "MEDATLAS ASCII")));
            specification.setEdition(new SimpleInternationalString("1.0"));
            format.setFormatSpecificationCitation(specification);
            distributionInfo.setDistributionFormats(Set.of(format));

            final DefaultDigitalTransferOptions transfer = new DefaultDigitalTransferOptions();
            transfer.setTransferSize(2.431640625);
            final DefaultOnlineResource onlines = new DefaultOnlineResource(URI.create("ftp://www.ifremer.fr/data/something"));
            onlines.setDescription(new SimpleInternationalString("Dummy download link"));
            onlines.setFunction(OnLineFunction.DOWNLOAD);
            onlines.setProtocol("ftp");
            transfer.setOnLines(Set.of(onlines));
            distributionInfo.setTransferOptions(Set.of(transfer));
            metadata.setDistributionInfo(Set.of(distributionInfo));
        }
        return metadata;
    }

    /**
     * Returns a property map with a name and identifier. This is used for creating CRS components.
     */
    private static Map<String,?> nameAndIdentifier(final String identifier, final String name, final String scope) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(DefaultVerticalDatum.NAME_KEY, new NamedIdentifier(null, name));
        properties.put(DefaultVerticalDatum.IDENTIFIERS_KEY, new NamedIdentifier(null, "test", identifier, null, null));
        if (scope != null) {
            properties.put(DefaultVerticalDatum.SCOPE_KEY, scope);
        }
        return properties;
    }

    /**
     * Returns the URL to the {@value #XML_FILE} file to use for this test.
     *
     * @return the URL to {@value #XML_FILE} test file.
     */
    private URL getResource() {
        return MetadataTest.class.getResource(XML_FILE);
    }

    /**
     * Tests marshalling of a XML document.
     *
     * @throws Exception if an error occurred during marshalling.
     */
    @Test
    public void testMarshalling() throws Exception {
        final MarshallerPool pool   = getMarshallerPool();
        final Marshaller     ms     = pool.acquireMarshaller();
        final StringWriter   writer = new StringWriter(25000);
        ms.setProperty(XML.METADATA_VERSION, VERSION_2007);
        ms.marshal(createHardCoded(), writer);
        pool.recycle(ms);
        /*
         * Apache SIS can marshal CharSequence as Anchor only if the property type is InternationalString.
         * But the 'Metadata.hierarchyLevelName' and 'Identifier.code' properties are String, which we can
         * not subclass. Consequently, SIS currently marshals them as plain string. Replace those strings
         * by the anchor version so we can compare the XML with the "Metadata.xml" file content.
         */
        final StringBuffer xml = writer.getBuffer();
        replace(xml, "<gcol:CharacterString>Pseudo Common Data Index record</gcol:CharacterString>",
                     "<gmx:Anchor xlink:href=\"SDN:L231:3:CDI\">Pseudo Common Data Index record</gmx:Anchor>");
        replace(xml, "<gcol:CharacterString>4326</gcol:CharacterString>",
                     "<gmx:Anchor xlink:href=\"SDN:L101:2:4326\">4326</gmx:Anchor>");
        /*
         * The <gmd:EX_TemporalExtent> block cannot be marshalled es expected yet (need a "sis-temporal" module).
         * We need to instruct the XML comparator to ignore this block during the comparison. We also ignore for
         * now the "gml:id" attribute since SIS generates different values than the ones in our test XML file,
         * and those values may change in future SIS version.
         */
        final DocumentComparator comparator = new DocumentComparator(getResource(), xml.toString());
        comparator.ignoredNodes.add(LegacyNamespaces.GMD + ":temporalElement");
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add(Namespaces.XSI + ":schemaLocation");
        comparator.ignoredAttributes.add(Namespaces.GML + ":id");
        comparator.ignoreComments = true;
        comparator.compare();
    }

    /**
     * Replaces the first occurrence of the given string by another one.
     *
     * @param  buffer     the buffer in which to perform the replacement.
     * @param  toSearch   the string to search.
     * @param  replaceBy  the value to use as a replacement.
     */
    private static void replace(final StringBuffer buffer, final String toSearch, final String replaceBy) {
        final int i = buffer.indexOf(toSearch);
        assertTrue("String to replace not found.", i >= 0);
        buffer.replace(i, i + toSearch.length(), replaceBy);
    }

    /**
     * Tests unmarshalling of a XML document.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        /*
         * Note: if this MetadataTest class is made final, then all following lines
         * until pool.recycle(…) can be replaced by a call to unmarshallFile(XML_FILE).
         */
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final DefaultMetadata metadata = (DefaultMetadata) unmarshaller.unmarshal(getResource());
        pool.recycle(unmarshaller);
        final DefaultMetadata expected = createHardCoded();
        assertTrue(metadata.equals(expected, ComparisonMode.DEBUG));
    }
}
