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
package org.apache.sis.storage.netcdf;

import static java.util.Map.entry;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.netcdf.base.Decoder;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.DataStoreMock;
import org.apache.sis.storage.netcdf.base.TestCase;
import org.apache.sis.storage.netcdf.classic.ChannelDecoderTest;

// Specific to the main branch:
import org.apache.sis.storage.netcdf.base.TestData;
import org.apache.sis.test.ContentVerifier;


/**
 * Tests {@link MetadataReader}. This tests uses the SIS embedded implementation and the UCAR library
 * for reading netCDF attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MetadataReaderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MetadataReaderTest() {
    }

    /**
     * Tests {@link MetadataReader#split(String)}.
     */
    @Test
    public void testSplit() {
        assertArrayEquals(new String[] {"John Doe", "Foo \" Bar", "Jane Lee", "L J Smith, Jr."},
                MetadataReader.split("John Doe, \"Foo \" Bar\" ,Jane Lee,\"L J Smith, Jr.\"").toArray());
    }

    /**
     * Reads the metadata using the netCDF decoder embedded with SIS,
     * and compares its string representation with the expected one.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testEmbedded() throws IOException, DataStoreException {
        final Decoder input = ChannelDecoderTest.createChannelDecoder(TestData.NETCDF_2D_GEOGRAPHIC);
        final Metadata metadata = new MetadataReader(input).read();
        input.close(new DataStoreMock("lock"));
        compareToExpected(metadata, false).assertMetadataEquals();
    }

    /**
     * Reads the metadata using the UCAR library and compares
     * its string representation with the expected one.
     *
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testUCAR() throws IOException, DataStoreException {
        final Decoder input = createDecoder(TestData.NETCDF_2D_GEOGRAPHIC);
        final Metadata metadata = new MetadataReader(input).read();
        input.close(new DataStoreMock("lock"));
        final ContentVerifier verifier = compareToExpected(metadata, true);
        verifier.addExpectedValue("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.alternateTitle[1]", "NetCDF-3/CDM");
        verifier.assertMetadataEquals();
    }

    /**
     * Converts the given object to the actual type stored in the metadata.
     *
     * @param  expected  the expected date to convert.
     * @param  ucar      whether the UCAR wrapper is used.
     * @return the given date converted to the expected type.
     */
    private static Temporal actual(final LocalDateTime expected, final boolean ucar) {
        return ucar ? expected.toInstant(ZoneOffset.UTC) : expected;
    }

    /**
     * Creates comparator for the string representation of the given metadata object with the expected one.
     * The given metadata shall have been created from the {@link TestData#NETCDF_2D_GEOGRAPHIC} dataset.
     *
     * @param  actual    the metadata which have been read.
     * @param  ucar      whether the UCAR wrapper is used.
     */
    static ContentVerifier compareToExpected(final Metadata actual, final boolean ucar) {
        final var verifier = new ContentVerifier();
        verifier.addPropertyToIgnore(Metadata.class, "metadataStandard");
        verifier.addPropertyToIgnore(Metadata.class, "referenceSystemInfo");
        verifier.addPropertyToIgnore(Citation.class, "otherCitationDetails");   // "Read by Foo version XYZ" in format citation.
        verifier.addPropertyToIgnore(TemporalExtent.class, "extent");
        verifier.addPropertyToIgnore((path) -> path.equals("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.identifier[0].authority"));
        verifier.addMetadataToVerify(actual);
        verifier.addExpectedValues(
            // Hard-coded
            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.alternateTitle[0]", "NetCDF"),
            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.title", "NetCDF Classic and 64-bit Offset Format"),
            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.citedResponsibleParty[0].party[0].name", "Open Geospatial Consortium"),
            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.citedResponsibleParty[0].role", Role.PRINCIPAL_INVESTIGATOR),

            // Read from the file
            entry("dateInfo[0].date",                                                        actual(LocalDateTime.of(2018, 5, 15, 13, 1), ucar)),
            entry("dateInfo[0].dateType",                                                    DateType.REVISION),
            entry("metadataScope[0].resourceScope",                                          ScopeCode.DATASET),
            entry("identificationInfo[0].abstract",                                          "Global, two-dimensional model data"),
            entry("identificationInfo[0].purpose",                                           "GeoAPI conformance tests"),
            entry("identificationInfo[0].supplementalInformation",                           "For testing purpose only."),
            entry("identificationInfo[0].citation.title",                                    "Test data from Sea Surface Temperature Analysis Model"),
            entry("identificationInfo[0].descriptiveKeywords[0].keyword[0]",                 "EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature"),
            entry("identificationInfo[0].descriptiveKeywords[0].thesaurusName.title",        "GCMD Science Keywords"),
            entry("identificationInfo[0].descriptiveKeywords[0].type",                       KeywordType.THEME),
            entry("identificationInfo[0].pointOfContact[0].role",                            Role.POINT_OF_CONTACT),
            entry("identificationInfo[0].pointOfContact[0].party[0].name",                   "NOAA/NWS/NCEP"),
            entry("identificationInfo[0].citation.citedResponsibleParty[0].role",            Role.ORIGINATOR),
            entry("identificationInfo[0].citation.citedResponsibleParty[0].party[0].name",   "NOAA/NWS/NCEP"),
            entry("identificationInfo[0].citation.date[0].date",                             actual(LocalDateTime.of(2005, 9, 22,  0, 0), ucar)),
            entry("identificationInfo[0].citation.date[1].date",                             actual(LocalDateTime.of(2018, 5, 15, 13, 0), ucar)),
            entry("identificationInfo[0].citation.date[0].dateType",                         DateType.CREATION),
            entry("identificationInfo[0].citation.date[1].dateType",                         DateType.REVISION),
            entry("identificationInfo[0].citation.identifier[0].code",                       "NCEP/SST/Global_5x2p5deg/SST_Global_5x2p5deg_20050922_0000.nc"),
            entry("identificationInfo[0].citation.identifier[0].authority.title",            "edu.ucar.unidata"),
            entry("identificationInfo[0].resourceConstraints[0].useLimitation[0]",           "Freely available"),
            entry("identificationInfo[0].extent[0].geographicElement[0].extentTypeCode",     Boolean.TRUE),
            entry("identificationInfo[0].extent[0].geographicElement[0].westBoundLongitude", -180.0),
            entry("identificationInfo[0].extent[0].geographicElement[0].eastBoundLongitude",  180.0),
            entry("identificationInfo[0].extent[0].geographicElement[0].southBoundLatitude",  -90.0),
            entry("identificationInfo[0].extent[0].geographicElement[0].northBoundLatitude",   90.0),
            entry("identificationInfo[0].extent[0].verticalElement[0].maximumValue",            0.0),
            entry("identificationInfo[0].extent[0].verticalElement[0].minimumValue",            0.0),
            entry("identificationInfo[0].spatialRepresentationType[0]",                      SpatialRepresentationType.GRID),
            entry("spatialRepresentationInfo[0].cellGeometry",                               CellGeometry.AREA),
            entry("spatialRepresentationInfo[0].numberOfDimensions",                         2),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionName",   DimensionNameType.COLUMN),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionName",   DimensionNameType.ROW),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionSize",   73),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionSize",   73),
            entry("spatialRepresentationInfo[0].transformationParameterAvailability",        false),

            // Variable descriptions (only one in this test).
            entry("contentInfo[0].attributeGroup[0].attribute[0].sequenceIdentifier",        "SST"),
            entry("contentInfo[0].attributeGroup[0].attribute[0].description",               "Sea temperature"),
            entry("contentInfo[0].attributeGroup[0].attribute[0].name[0].code",              "sea_water_temperature"),
            entry("contentInfo[0].attributeGroup[0].attribute[0].transferFunctionType",      TransferFunctionType.LINEAR),
            entry("contentInfo[0].attributeGroup[0].attribute[0].scaleFactor",               0.0011),
            entry("contentInfo[0].attributeGroup[0].attribute[0].offset",                    -1.85),
            entry("contentInfo[0].attributeGroup[0].attribute[0].units",                     "°C"),

            entry("resourceLineage[0].statement", "Decimated and modified by GeoAPI for inclusion in conformance test suite."));

        return verifier;
    }
}
