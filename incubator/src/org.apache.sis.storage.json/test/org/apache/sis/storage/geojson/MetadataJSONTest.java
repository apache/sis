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
package org.apache.sis.storage.geojson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.DefaultMetadataScope;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.citation.DefaultContact;
import org.apache.sis.metadata.iso.citation.DefaultIndividual;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;
import org.apache.sis.metadata.iso.citation.DefaultOrganisation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibility;
import org.apache.sis.metadata.iso.citation.DefaultTelephone;
import org.apache.sis.metadata.iso.constraint.DefaultConstraints;
import org.apache.sis.metadata.iso.constraint.DefaultLegalConstraints;
import org.apache.sis.metadata.iso.constraint.DefaultSecurityConstraints;
import org.apache.sis.metadata.iso.distribution.DefaultDigitalTransferOptions;
import org.apache.sis.metadata.iso.distribution.DefaultDistribution;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultGeographicDescription;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.metadata.iso.identification.DefaultKeywords;
import org.apache.sis.metadata.iso.identification.DefaultResolution;
import org.apache.sis.metadata.iso.identification.DefaultUsage;
import org.apache.sis.metadata.iso.lineage.DefaultLineage;
import org.apache.sis.metadata.iso.lineage.DefaultSource;
import org.apache.sis.metadata.iso.maintenance.DefaultMaintenanceInformation;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.metadata.iso.quality.DefaultCompletenessCommission;
import org.apache.sis.metadata.iso.quality.DefaultConfidence;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.quality.DefaultDescriptiveResult;
import org.apache.sis.metadata.iso.quality.DefaultEvaluationReportInformation;
import org.apache.sis.metadata.iso.quality.DefaultFullInspection;
import org.apache.sis.metadata.iso.quality.DefaultMeasureReference;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.referencing.AbstractReferenceSystem;
import org.apache.sis.storage.geojson.binding.GeoJSONFeature;
import org.apache.sis.storage.geojson.binding.GeoJSONFeatureCollection;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultRecord;
import org.apache.sis.util.iso.DefaultRecordType;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Telephone;
import org.opengis.metadata.citation.TelephoneType;
import org.opengis.metadata.constraint.Classification;
import org.opengis.metadata.constraint.Constraints;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.identification.Identification;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.identification.Keywords;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.identification.Usage;
import org.opengis.metadata.maintenance.MaintenanceFrequency;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.DescriptiveResult;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.opengis.metadata.quality.FullInspection;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.ThematicClassificationCorrectness;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.temporal.Period;
import org.opengis.util.Record;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MetadataJSON}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MetadataJSONTest {

    private static final String FIXTURE = "metadata-collection.json";
    private static final String IDENTIFIER = "1b6f1c4e-0000-4000-8000-000000000001";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Reads the test document, a feature collection using the constructs of the reference example.
     */
    @Test
    public void testReadCollection() throws IOException {
        final DefaultMetadata metadata;
        try (InputStream in = MetadataJSONTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(in, FIXTURE);
            metadata = MetadataJSON.read(in);
        }
        assertEquals("6f0c4b1e-3a6d-4f5e-9d2b-000000000042", metadata.getMetadataIdentifier().getCode());
        assertEquals("urn:uuid:", metadata.getMetadataIdentifier().getCodeSpace());
        assertEquals(Map.of(Locale.FRANCE, StandardCharsets.UTF_8), metadata.getLocalesAndCharsets());

        // Contact with an organisation, two telephones and an individual without "type" member.
        final Responsibility contact = single(metadata.getContacts());
        assertEquals(Role.POINT_OF_CONTACT, contact.getRole());
        final DefaultOrganisation organisation = assertInstanceOf(DefaultOrganisation.class, single(contact.getParties()));
        assertEquals("Example Mapping Agency", organisation.getName().toString());
        final Collection<? extends Telephone> phones = single(organisation.getContactInfo()).getPhones();
        assertEquals(2, phones.size());
        assertEquals(TelephoneType.VOICE, phones.iterator().next().getNumberType());
        assertEquals("Data manager", single(organisation.getIndividual()).getPositionName().toString());

        // Dates keyed by date type.
        final List<CitationDate> dates = new ArrayList<>(metadata.getDateInfo());
        assertEquals(2, dates.size());
        assertEquals(DateType.REVISION, dates.get(0).getDateType());
        assertEquals(OffsetDateTime.of(2024, 4, 20, 6, 19, 51, 0, ZoneOffset.UTC), dates.get(0).getReferenceDate());
        assertEquals(DateType.CREATION, dates.get(1).getDateType());
        assertEquals(LocalDate.of(2020, 2, 4), dates.get(1).getReferenceDate());

        final ReferenceSystem crs = single(metadata.getReferenceSystemInfo());
        assertEquals("EPSG::2154", crs.getName().getCode());

        // Identification.
        final DefaultDataIdentification identification = assertInstanceOf(DefaultDataIdentification.class,
                single(metadata.getIdentificationInfo()));
        final Citation citation = identification.getCitation();
        assertEquals("Synthetic land cover dataset", citation.getTitle().toString());
        assertEquals(OffsetDateTime.of(2021, 3, 31, 0, 0, 0, 0, ZoneOffset.ofHours(11)),
                     single(citation.getDates()).getReferenceDate());
        assertEquals(Year.of(2021), citation.getEditionDate());
        assertEquals("9780000000002", citation.getISBN());
        assertEquals("2021", citation.getSeries().getIssueIdentification().toString());
        assertEquals(Double.valueOf(250), single(identification.getSpatialResolutions()).getDistance());
        assertEquals(java.time.Period.ofDays(1), single(identification.getTemporalResolutions()));
        assertEquals(List.of(TopicCategory.IMAGERY_BASE_MAPS_EARTH_COVER), new ArrayList<>(identification.getTopicCategories()));
        assertEquals(List.of(SpatialRepresentationType.VECTOR), new ArrayList<>(identification.getSpatialRepresentationTypes()));
        assertEquals("Doe, J.", single(single(identification.getPointOfContacts()).getParties()).getName().toString());

        // Extent.
        final Extent extent = single(identification.getExtents());
        assertEquals("Metropolitan France", extent.getDescription().toString());
        final List<GeographicExtent> elements = new ArrayList<>(extent.getGeographicElements());
        assertEquals(2, elements.size());
        assertInstanceOf(DefaultGeographicDescription.class, elements.get(0));
        final GeographicBoundingBox box = assertInstanceOf(GeographicBoundingBox.class, elements.get(1));
        assertEquals(-5, box.getWestBoundLongitude());
        assertEquals(10, box.getEastBoundLongitude());
        assertEquals(41, box.getSouthBoundLatitude());
        assertEquals(52, box.getNorthBoundLatitude());
        final DefaultTemporalExtent time = assertInstanceOf(DefaultTemporalExtent.class, single(extent.getTemporalElements()));
        assertEquals(LocalDate.of(2020, 1, 1), time.getBeginning().orElseThrow());
        assertFalse(time.getEnding().isPresent());

        // Maintenance, formats and keywords.
        assertEquals(MaintenanceFrequency.AS_NEEDED, single(identification.getResourceMaintenances()).getMaintenanceAndUpdateFrequency());
        assertEquals(java.time.Period.of(1, 2, 0), single(identification.getResourceMaintenances()).getUserDefinedMaintenanceFrequency());
        assertEquals("UNZIP", single(identification.getResourceFormats()).getFileDecompressionTechnique().toString());
        final List<Keywords> keywords = new ArrayList<>(identification.getDescriptiveKeywords());
        assertEquals(2, keywords.size());
        assertEquals(KeywordType.THEME, keywords.get(0).getType());
        assertEquals("Countries", keywords.get(1).getThesaurusName().getTitle().toString());

        // Constraints, the last one has no "type" member.
        final List<Constraints> constraints = new ArrayList<>(identification.getResourceConstraints());
        assertEquals(3, constraints.size());
        final DefaultLegalConstraints legal = assertInstanceOf(DefaultLegalConstraints.class, constraints.get(0));
        assertEquals(List.of(Restriction.LICENSE), new ArrayList<>(legal.getAccessConstraints()));
        assertEquals("CC-BY", single(single(legal.getReferences()).getAlternateTitles()).toString());
        final DefaultSecurityConstraints security = assertInstanceOf(DefaultSecurityConstraints.class, constraints.get(1));
        assertEquals(Classification.UNCLASSIFIED, security.getClassification());
        assertEquals(DefaultConstraints.class, constraints.get(2).getClass());
        assertEquals("Not for navigation", single(constraints.get(2).getUseLimitations()).toString());

        // Distribution.
        final DigitalTransferOptions options = single(single(metadata.getDistributionInfo()).getTransferOptions());
        assertEquals(Double.valueOf(12.5), options.getTransferSize());
        assertEquals(URI.create("https://example.org/data/landcover.zip"), single(options.getOnLines()).getLinkage());
        assertEquals(OnLineFunction.DOWNLOAD, single(options.getOnLines()).getFunction());

        // Data quality.
        final DataQuality quality = single(metadata.getDataQualityInfo());
        assertEquals(ScopeCode.DATASET, quality.getScope().getLevel());
        assertEquals("Quality evaluation report", quality.getStandaloneQualityReport().getReportReference().getTitle().toString());
        final List<Element> reports = new ArrayList<>(quality.getReports());
        assertEquals(2, reports.size());
        final DefaultCompletenessCommission commission = assertInstanceOf(DefaultCompletenessCommission.class, reports.get(0));
        assertEquals("2", commission.getMeasureReference().getMeasureIdentification().getCode());
        final FullInspection inspection = assertInstanceOf(FullInspection.class, commission.getEvaluationMethod());
        assertEquals(EvaluationMethodType.DIRECT_EXTERNAL, inspection.getEvaluationMethodType());
        final QuantitativeResult quantitative = assertInstanceOf(QuantitativeResult.class, single(commission.getResults()));
        assertEquals(2.0, single(single(quantitative.getValues()).getFields().values()));
        assertEquals(Units.METRE, quantitative.getValueUnit());
        assertEquals(List.of("road"), toStrings(single(quantitative.getResultScope().getLevelDescription()).getFeatures()));

        final DefaultConfidence confidence = assertInstanceOf(DefaultConfidence.class, reports.get(1));
        final List<Result> results = new ArrayList<>(confidence.getResults());
        assertEquals(2, results.size());
        assertEquals(Boolean.FALSE, assertInstanceOf(ConformanceResult.class, results.get(0)).pass());
        assertEquals("Evaluated on the whole dataset.", assertInstanceOf(DescriptiveResult.class, results.get(1)).getStatement().toString());
        assertInstanceOf(ThematicClassificationCorrectness.class, single(confidence.getDerivedElements()));

        // Lineage and scopes.
        assertEquals("Derived from satellite imagery.", single(metadata.getResourceLineages()).getStatement().toString());
        assertEquals("Synthetic source.", single(single(metadata.getResourceLineages()).getSources()).getDescription().toString());
        assertEquals(ScopeCode.DATASET, single(metadata.getMetadataScopes()).getResourceScope());
        assertEquals(Classification.UNCLASSIFIED,
                     assertInstanceOf(DefaultSecurityConstraints.class, single(metadata.getMetadataConstraints())).getClassification());
    }

    /**
     * Writes a metadata and reads it back.
     */
    @Test
    public void testRoundTrip() throws IOException {
        final DefaultMetadata original = createMetadata();
        final DefaultMetadata metadata = MetadataJSON.read(new ByteArrayInputStream(write(original)));

        assertEquals(IDENTIFIER, metadata.getMetadataIdentifier().getCode());
        assertEquals("urn:uuid:", metadata.getMetadataIdentifier().getCodeSpace());
        assertEquals(Map.of(Locale.ENGLISH, StandardCharsets.UTF_8), metadata.getLocalesAndCharsets());
        assertEquals(original.getDateInfo(), metadata.getDateInfo());
        assertEquals(original.getMetadataScopes(), metadata.getMetadataScopes());
        assertEquals(original.getContacts(), metadata.getContacts());
        assertEquals("3112", single(metadata.getReferenceSystemInfo()).getName().getCode());

        final Identification expected = single(original.getIdentificationInfo());
        final Identification actual   = single(metadata.getIdentificationInfo());
        assertEquals(expected.getCitation(), actual.getCitation());
        assertEquals(expected.getAbstract(), actual.getAbstract());
        assertEquals(expected.getTopicCategories(), actual.getTopicCategories());
        assertEquals(expected.getSpatialResolutions(), actual.getSpatialResolutions());
        assertEquals(new ArrayList<>(expected.getTemporalResolutions()), new ArrayList<>(actual.getTemporalResolutions()));
        assertEquals(expected.getDescriptiveKeywords(), actual.getDescriptiveKeywords());
        assertEquals(expected.getResourceConstraints(), actual.getResourceConstraints());
        assertEquals(expected.getResourceFormats(), actual.getResourceFormats());
        assertEquals(expected.getResourceMaintenances(), actual.getResourceMaintenances());

        final Extent extent = single(actual.getExtents());
        assertEquals(single(expected.getExtents()).getGeographicElements(), extent.getGeographicElements());
        final TemporalExtent time = single(extent.getTemporalElements());
        final Period period = assertInstanceOf(Period.class, time.getExtent());
        assertEquals(LocalDate.of(2009, 1, 1),   period.getBeginning().getPosition());
        assertEquals(LocalDate.of(2009, 12, 31), period.getEnding().getPosition());
        final VerticalExtent vertical = single(extent.getVerticalElements());
        assertEquals(Double.valueOf(-10), vertical.getMinimumValue());
        assertEquals(Double.valueOf(100), vertical.getMaximumValue());

        final Usage usage = single(actual.getResourceSpecificUsages());
        assertEquals("Testing", usage.getSpecificUsage().toString());
        assertInstanceOf(Period.class, single(usage.getUsageDates()));

        assertEquals(original.getDistributionInfo(), metadata.getDistributionInfo());
        assertEquals(original.getResourceLineages(), metadata.getResourceLineages());
        assertEquals(original.getMetadataConstraints(), metadata.getMetadataConstraints());

        final DataQuality quality = single(metadata.getDataQualityInfo());
        final DataQuality expectedQuality = single(original.getDataQualityInfo());
        assertEquals(expectedQuality.getScope(), quality.getScope());
        assertEquals(expectedQuality.getStandaloneQualityReport(), quality.getStandaloneQualityReport());
        final List<Element> reports = new ArrayList<>(quality.getReports());
        assertEquals(2, reports.size());
        final Element commission = reports.get(0);
        assertInstanceOf(DefaultCompletenessCommission.class, commission);
        final Element expectedCommission = new ArrayList<>(expectedQuality.getReports()).get(0);
        assertEquals(expectedCommission.getMeasureReference(), commission.getMeasureReference());
        assertEquals(expectedCommission.getEvaluationMethod(), commission.getEvaluationMethod());
        final List<Result> results = new ArrayList<>(commission.getResults());
        assertEquals(2, results.size());
        final Record record = single(assertInstanceOf(QuantitativeResult.class, results.get(0)).getValues());
        assertEquals(2.0, single(record.getFields().values()));
        assertEquals(Boolean.TRUE, assertInstanceOf(ConformanceResult.class, results.get(1)).pass());
        final DefaultConfidence confidence = assertInstanceOf(DefaultConfidence.class, reports.get(1));
        assertInstanceOf(DefaultCompletenessCommission.class, single(confidence.getDerivedElements()));
    }

    /**
     * Verifies the JSON structure written for a metadata.
     */
    @Test
    public void testWriteStructure() throws IOException {
        final JsonNode root = mapper.readTree(write(createMetadata()));
        assertEquals("Feature", root.path("type").asText());
        assertEquals(IDENTIFIER, root.path("id").asText());
        final List<String> conformsTo = new ArrayList<>();
        root.path("conformsTo").forEach((JsonNode e) -> conformsTo.add(e.asText()));
        assertEquals(List.of(MetadataJSON.CONFORMANCE_CORE, MetadataJSON.CONFORMANCE_DATA_QUALITY,
                             MetadataJSON.CONFORMANCE_NO_REFERENCE), conformsTo);

        final JsonNode geometry = root.path("geometry");
        assertEquals("Polygon", geometry.path("type").asText());
        assertEquals(mapper.readTree("[[[112.0,-44.0],[156.0,-44.0],[156.0,-10.0],[112.0,-10.0],[112.0,-44.0]]]"),
                     geometry.path("coordinates"));
        assertEquals(mapper.readTree("[112.0,-44.0,156.0,-10.0]"), root.path("bbox"));

        final JsonNode properties = root.path("properties");
        assertEquals("eng",  properties.path("defaultLocale").path("language").asText());
        assertEquals("utf8", properties.path("defaultLocale").path("characterEncoding").asText());
        assertTrue(properties.path("dateInfo").isObject());
        assertEquals("2010-02-04", properties.path("dateInfo").path("creation").asText());
        assertEquals("2018-04-20T06:19:51Z", properties.path("dateInfo").path("revision").asText());

        final JsonNode contact = properties.path("contact").path(0);
        assertEquals("pointOfContact", contact.path("role").asText());
        final JsonNode party = contact.path("party").path(0);
        assertEquals("CI_Organisation", party.path("type").asText());
        assertEquals("CI_Individual", party.path("individual").path(0).path("type").asText());
        assertEquals("+33 1 00 00 00 00", party.path("contactInfo").path(0).path("phone").path("voice").asText());

        final JsonNode identification = properties.path("identificationInfo");
        assertTrue(identification.isObject());
        assertEquals("geoscientificInformation", identification.path("topicCategory").path(0).asText());
        assertEquals("9780000000002", identification.path("citation").path("ISBN").asText());
        assertTrue(identification.path("citation").path("identifier").isMissingNode());
        assertEquals("EX_GeographicDescription", identification.path("extent").path(0).path("geographicElement").path(0).path("type").asText());
        assertEquals("EX_GeographicBoundingBox", identification.path("extent").path(0).path("geographicElement").path(1).path("type").asText());
        assertEquals("MD_LegalConstraints",    identification.path("resourceConstraints").path(0).path("type").asText());
        assertEquals("MD_SecurityConstraints", identification.path("resourceConstraints").path(1).path("type").asText());
        assertEquals("PT6H", identification.path("temporalResolution").path(0).asText());

        final JsonNode report = properties.path("dataQualityInfo").path(0).path("report");
        assertEquals("Commission", report.path(0).path("type").asText());
        assertEquals("FullInspection", report.path(0).path("evaluationMethod").path(0).path("type").asText());
        assertEquals("QuantitativeResult", report.path(0).path("result").path(0).path("type").asText());
        assertEquals(2.0, report.path(0).path("result").path(0).path("value").path(0).asDouble());
        assertEquals("Confidence", report.path(1).path("type").asText());
        assertEquals("Commission", report.path(1).path("derivedElement").path(0).path("type").asText());
    }

    /**
     * Verifies that a bounding box crossing the anti-meridian is split in two polygons.
     */
    @Test
    public void testAntiMeridian() throws IOException {
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        identification.setCitation(new DefaultCitation("Pacific"));
        identification.setExtents(List.of(new DefaultExtent(null, new DefaultGeographicBoundingBox(170, -170, -20, 10), null, null)));
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setIdentificationInfo(List.of(identification));

        final JsonNode root = mapper.readTree(write(metadata));
        assertEquals("MultiPolygon", root.path("geometry").path("type").asText());
        assertEquals(2, root.path("geometry").path("coordinates").size());
        assertEquals(mapper.readTree("[170.0,-20.0,-170.0,10.0]"), root.path("bbox"));
        assertTrue(root.path("id").isMissingNode() || root.path("id").isNull());
    }

    /**
     * Verifies that the feature identifier is used when the metadata identifier is absent,
     * and that a metadata without geographic extent has a null geometry.
     */
    @Test
    public void testFeatureIdentifier() throws IOException {
        final String json = "{\"type\": \"Feature\", \"id\": \"abc\", \"geometry\": null, \"properties\": {}}";
        final DefaultMetadata metadata = MetadataJSON.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals("abc", metadata.getMetadataIdentifier().getCode());

        final JsonNode root = mapper.readTree(write(metadata));
        assertTrue(root.path("geometry").isNull());
        assertEquals("abc", root.path("properties").path("metadataIdentifier").path("code").asText());
    }

    /**
     * Verifies the reading of collections with several records and of invalid documents.
     */
    @Test
    public void testReadAll() throws IOException {
        final String json = "{\"type\": \"FeatureCollection\", \"features\": ["
                + "{\"type\": \"Feature\", \"id\": \"a\", \"geometry\": null, \"properties\": {}},"
                + "{\"type\": \"Feature\", \"id\": \"b\", \"geometry\": null, \"properties\": {}}]}";
        final List<DefaultMetadata> records = MetadataJSON.readAll(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, records.size());
        assertEquals("b", records.get(1).getMetadataIdentifier().getCode());
        assertThrows(IOException.class, () -> MetadataJSON.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
        assertThrows(IOException.class, () -> MetadataJSON.read(new ByteArrayInputStream(
                "{\"type\": \"Point\", \"coordinates\": [0, 0]}".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IOException.class, () -> MetadataJSON.readAll(new ByteArrayInputStream(
                "{\"type\": \"FeatureCollection\", \"features\": [{\"type\": \"Point\", \"coordinates\": [0, 0]}]}"
                .getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * Converts a metadata to a GeoJSON feature and back,
     * then reads the feature nested in a feature collection.
     */
    @Test
    public void testFeatureConversion() throws IOException {
        final DefaultMetadata original = createMetadata();
        final GeoJSONFeature feature = MetadataJSON.toFeature(original, null);
        assertEquals(IDENTIFIER, feature.getId());
        assertTrue(feature.getConformsTo().isEmpty(), "A nested feature shall not declare conformance classes.");
        assertNotNull(feature.getGeometry());
        assertEquals(4, feature.getBbox().size());

        final DefaultMetadata metadata = MetadataJSON.fromFeature(feature);
        assertEquals(IDENTIFIER, metadata.getMetadataIdentifier().getCode());
        assertEquals(single(original.getIdentificationInfo()).getCitation(),
                     single(metadata.getIdentificationInfo()).getCitation());

        final GeoJSONFeatureCollection collection = new GeoJSONFeatureCollection();
        collection.setFeatures(List.of(feature));
        final DefaultMetadata nested = MetadataJSON.read(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(collection)));
        assertEquals(IDENTIFIER, nested.getMetadataIdentifier().getCode());

        // The feature identifier, here a number, is used when the metadata identifier is absent.
        final GeoJSONFeature numbered = new GeoJSONFeature();
        numbered.setId(12);
        numbered.setProperties(Map.of());
        assertEquals("12", MetadataJSON.fromFeature(numbered).getMetadataIdentifier().getCode());
    }

    private static byte[] write(final DefaultMetadata metadata) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        MetadataJSON.write(metadata, out);
        return out.toByteArray();
    }

    private static <E> E single(final Collection<? extends E> values) {
        assertEquals(1, values.size(), "Expected a single value.");
        return values.iterator().next();
    }

    private static List<String> toStrings(final Collection<? extends CharSequence> values) {
        final List<String> result = new ArrayList<>();
        for (final CharSequence value : values) {
            result.add(value.toString());
        }
        return result;
    }

    /**
     * Creates a metadata covering the properties encoded by this module.
     */
    private static DefaultMetadata createMetadata() {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataIdentifier(new DefaultIdentifier("urn:uuid:", IDENTIFIER, null));
        metadata.setLocalesAndCharsets(Map.of(Locale.ENGLISH, StandardCharsets.UTF_8));

        final DefaultAddress address = new DefaultAddress();
        address.setCity(new SimpleInternationalString("Montpellier"));
        address.setElectronicMailAddresses(List.of("contact@example.org"));
        final DefaultContact contactInfo = new DefaultContact();
        contactInfo.setPhones(List.of(new DefaultTelephone("+33 1 00 00 00 00", TelephoneType.VOICE)));
        contactInfo.setAddresses(List.of(address));
        final DefaultIndividual individual = new DefaultIndividual();
        individual.setName(new SimpleInternationalString("Doe, J."));
        individual.setPositionName(new SimpleInternationalString("Data manager"));
        final DefaultOrganisation organisation = new DefaultOrganisation();
        organisation.setName(new SimpleInternationalString("Example Mapping Agency"));
        organisation.setContactInfo(List.of(contactInfo));
        organisation.setIndividual(List.of(individual));
        final DefaultResponsibility contact = new DefaultResponsibility(Role.POINT_OF_CONTACT, null, organisation);
        metadata.setContacts(List.of(contact));
        metadata.setDateInfo(List.of(
                new DefaultCitationDate(OffsetDateTime.of(2018, 4, 20, 6, 19, 51, 0, ZoneOffset.UTC), DateType.REVISION),
                new DefaultCitationDate(LocalDate.of(2010, 2, 4), DateType.CREATION)));
        metadata.setMetadataScopes(List.of(new DefaultMetadataScope(ScopeCode.DATASET, "dataset")));
        metadata.setReferenceSystemInfo(List.of(new AbstractReferenceSystem(
                Map.of(ReferenceSystem.NAME_KEY, new DefaultIdentifier("EPSG", "3112", null)))));

        final DefaultCitation citation = new DefaultCitation("Test dataset");
        citation.setDates(List.of(new DefaultCitationDate(LocalDate.of(2010, 1, 1), DateType.PUBLICATION)));
        citation.setISBN("9780000000002");
        final DefaultResolution resolution = new DefaultResolution();
        resolution.setDistance(250.0);
        final DefaultExtent extent = new DefaultExtent("Australia",
                new DefaultGeographicBoundingBox(112, 156, -44, -10),
                new DefaultVerticalExtent(-10, 100, null),
                new DefaultTemporalExtent(LocalDate.of(2009, 1, 1), LocalDate.of(2009, 12, 31)));
        extent.setGeographicElements(List.of(
                new DefaultGeographicDescription(new DefaultIdentifier("AU")),
                new DefaultGeographicBoundingBox(112, 156, -44, -10)));
        final DefaultKeywords keywords = new DefaultKeywords("soils", "mapping");
        keywords.setType(KeywordType.THEME);
        final DefaultLegalConstraints legal = new DefaultLegalConstraints();
        legal.setAccessConstraints(List.of(Restriction.LICENSE));
        legal.setReferences(List.of(new DefaultCitation("Creative Commons Attribution 4.0")));
        final DefaultFormat format = new DefaultFormat();
        format.setFormatSpecificationCitation(new DefaultCitation("GeoPackage"));
        format.setFileDecompressionTechnique(new SimpleInternationalString("UNZIP"));
        final DefaultUsage usage = new DefaultUsage();
        usage.setSpecificUsage(new SimpleInternationalString("Testing"));
        usage.addUsageDates(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 6, 30));

        final DefaultDataIdentification identification = new DefaultDataIdentification();
        identification.setCitation(citation);
        identification.setAbstract(new SimpleInternationalString("Synthetic dataset."));
        identification.setTopicCategories(List.of(TopicCategory.GEOSCIENTIFIC_INFORMATION));
        identification.setSpatialRepresentationTypes(List.of(SpatialRepresentationType.VECTOR));
        identification.setSpatialResolutions(List.of(resolution));
        identification.setTemporalResolutions(List.of(Duration.ofHours(6)));
        identification.setExtents(List.of(extent));
        identification.setDescriptiveKeywords(List.of(keywords));
        identification.setResourceConstraints(List.of(legal, new DefaultSecurityConstraints(Classification.UNCLASSIFIED)));
        identification.setResourceFormats(List.of(format));
        identification.setResourceMaintenances(List.of(new DefaultMaintenanceInformation(MaintenanceFrequency.AS_NEEDED)));
        identification.setResourceSpecificUsages(List.of(usage));
        metadata.setIdentificationInfo(List.of(identification));

        final DefaultOnlineResource online = new DefaultOnlineResource(URI.create("https://example.org/data.zip"));
        online.setFunction(OnLineFunction.DOWNLOAD);
        online.setProtocol("WWW:LINK-1.0-http--link");
        final DefaultDigitalTransferOptions options = new DefaultDigitalTransferOptions();
        options.setOnLines(List.of(online));
        options.setTransferSize(12.5);
        final DefaultDistribution distribution = new DefaultDistribution();
        distribution.setTransferOptions(List.of(options));
        metadata.setDistributionInfo(List.of(distribution));

        final DefaultMeasureReference measure = new DefaultMeasureReference("number of excess items");
        measure.setMeasureIdentification(new DefaultIdentifier("2"));
        final DefaultFullInspection inspection = new DefaultFullInspection();
        inspection.setEvaluationMethodType(EvaluationMethodType.DIRECT_EXTERNAL);
        inspection.setEvaluationMethodDescription(new SimpleInternationalString("Count of all items."));
        final DefaultRecord record = new DefaultRecord(DefaultRecordType.SINGLE_REAL);
        record.setAll(2.0);
        final DefaultQuantitativeResult quantitative = new DefaultQuantitativeResult();
        quantitative.setValues(List.of(record));
        final DefaultConformanceResult conformance = new DefaultConformanceResult();
        conformance.setSpecification(new DefaultCitation("Product specification"));
        conformance.setPass(Boolean.TRUE);
        final DefaultCompletenessCommission commission = new DefaultCompletenessCommission();
        commission.setMeasureReference(measure);
        commission.setEvaluationMethod(inspection);
        commission.setResults(List.of(quantitative, conformance));
        final DefaultConfidence confidence = new DefaultConfidence();
        confidence.setMeasureReference(new DefaultMeasureReference("safety factor"));
        confidence.setResults(List.of(new DefaultDescriptiveResult("Trustful evaluation.")));
        confidence.setDerivedElements(List.of(commission));
        final DefaultEvaluationReportInformation report = new DefaultEvaluationReportInformation();
        report.setReportReference(new DefaultCitation("Quality evaluation report"));
        report.setAbstract(new SimpleInternationalString("Details of the evaluation."));
        final DefaultDataQuality quality = new DefaultDataQuality(ScopeCode.DATASET);
        quality.setReports(List.of(commission, confidence));
        quality.setStandaloneQualityReport(report);
        metadata.setDataQualityInfo(List.of(quality));

        final DefaultLineage lineage = new DefaultLineage();
        lineage.setStatement(new SimpleInternationalString("Derived from field surveys."));
        lineage.setScope(new DefaultScope(ScopeCode.DATASET));
        lineage.setSources(List.of(new DefaultSource("Field survey data.")));
        metadata.setResourceLineages(List.of(lineage));
        metadata.setMetadataConstraints(List.of(new DefaultSecurityConstraints(Classification.UNCLASSIFIED)));
        return metadata;
    }
}
