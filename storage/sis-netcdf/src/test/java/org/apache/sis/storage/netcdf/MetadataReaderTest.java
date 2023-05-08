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

import java.io.IOException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.identification.KeywordType;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.test.dataset.ContentVerifier;
import org.opengis.test.dataset.TestData;
import org.apache.sis.internal.netcdf.TestCase;
import org.apache.sis.internal.netcdf.Decoder;
import org.apache.sis.internal.netcdf.impl.ChannelDecoderTest;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreMock;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.date;


/**
 * Tests {@link MetadataReader}. This tests uses the SIS embedded implementation and the UCAR library
 * for reading netCDF attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   0.3
 */
@DependsOn({
    ChannelDecoderTest.class,
    org.apache.sis.internal.netcdf.impl.VariableInfoTest.class
})
public final class MetadataReaderTest extends TestCase {
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
        compareToExpected(metadata).assertMetadataEquals();
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
        final ContentVerifier verifier = compareToExpected(metadata);
        verifier.addExpectedValue("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.alternateTitle[1]", "NetCDF-3/CDM");
        verifier.assertMetadataEquals();
    }

    /**
     * Creates comparator for the string representation of the given metadata object with the expected one.
     * The given metadata shall have been created from the {@link TestData#NETCDF_2D_GEOGRAPHIC} dataset.
     */
    static ContentVerifier compareToExpected(final Metadata actual) {
        final ContentVerifier verifier = new ContentVerifier();
        verifier.addPropertyToIgnore(Metadata.class, "metadataStandard");
        verifier.addPropertyToIgnore(Metadata.class, "referenceSystemInfo");
        verifier.addPropertyToIgnore(TemporalExtent.class, "extent");
        verifier.addMetadataToVerify(actual);
        verifier.addExpectedValues(
            // Hard-coded
            "identificationInfo[0].resourceFormat[0].formatSpecificationCitation.alternateTitle[0]", "NetCDF",
            "identificationInfo[0].resourceFormat[0].formatSpecificationCitation.title", "NetCDF Classic and 64-bit Offset Format",
            "identificationInfo[0].resourceFormat[0].formatSpecificationCitation.citedResponsibleParty[0].party[0].name", "Open Geospatial Consortium",
            "identificationInfo[0].resourceFormat[0].formatSpecificationCitation.citedResponsibleParty[0].role", Role.PRINCIPAL_INVESTIGATOR,

            // Read from the file
            "dateInfo[0].date",                                                        date("2018-05-15 13:01:00"),
            "dateInfo[0].dateType",                                                    DateType.REVISION,
            "metadataScope[0].resourceScope",                                          ScopeCode.DATASET,
            "identificationInfo[0].abstract",                                          "Global, two-dimensional model data",
            "identificationInfo[0].purpose",                                           "GeoAPI conformance tests",
            "identificationInfo[0].supplementalInformation",                           "For testing purpose only.",
            "identificationInfo[0].citation.title",                                    "Test data from Sea Surface Temperature Analysis Model",
            "identificationInfo[0].descriptiveKeywords[0].keyword[0]",                 "EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature",
            "identificationInfo[0].descriptiveKeywords[0].thesaurusName.title",        "GCMD Science Keywords",
            "identificationInfo[0].descriptiveKeywords[0].type",                       KeywordType.THEME,
            "identificationInfo[0].pointOfContact[0].role",                            Role.POINT_OF_CONTACT,
            "identificationInfo[0].pointOfContact[0].party[0].name",                   "NOAA/NWS/NCEP",
            "identificationInfo[0].citation.citedResponsibleParty[0].role",            Role.ORIGINATOR,
            "identificationInfo[0].citation.citedResponsibleParty[0].party[0].name",   "NOAA/NWS/NCEP",
            "identificationInfo[0].citation.date[0].date",                             date("2005-09-22 00:00:00"),
            "identificationInfo[0].citation.date[1].date",                             date("2018-05-15 13:00:00"),
            "identificationInfo[0].citation.date[0].dateType",                         DateType.CREATION,
            "identificationInfo[0].citation.date[1].dateType",                         DateType.REVISION,
            "identificationInfo[0].citation.identifier[0].code",                       "NCEP/SST/Global_5x2p5deg/SST_Global_5x2p5deg_20050922_0000.nc",
            "identificationInfo[0].citation.identifier[0].authority.title",            "edu.ucar.unidata",
            "identificationInfo[0].resourceConstraints[0].useLimitation[0]",           "Freely available",
            "identificationInfo[0].extent[0].geographicElement[0].extentTypeCode",     Boolean.TRUE,
            "identificationInfo[0].extent[0].geographicElement[0].westBoundLongitude", -180.0,
            "identificationInfo[0].extent[0].geographicElement[0].eastBoundLongitude",  180.0,
            "identificationInfo[0].extent[0].geographicElement[0].southBoundLatitude",  -90.0,
            "identificationInfo[0].extent[0].geographicElement[0].northBoundLatitude",   90.0,
            "identificationInfo[0].extent[0].verticalElement[0].maximumValue",            0.0,
            "identificationInfo[0].extent[0].verticalElement[0].minimumValue",            0.0,
            "identificationInfo[0].spatialRepresentationType[0]",                      SpatialRepresentationType.GRID,
            "spatialRepresentationInfo[0].cellGeometry",                               CellGeometry.AREA,
            "spatialRepresentationInfo[0].numberOfDimensions",                         2,
            "spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionName",   DimensionNameType.COLUMN,
            "spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionName",   DimensionNameType.ROW,
            "spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionSize",   73,
            "spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionSize",   73,
            "spatialRepresentationInfo[0].transformationParameterAvailability",        false,

            // Variable descriptions (only one in this test).
            "contentInfo[0].attributeGroup[0].attribute[0].sequenceIdentifier",        "SST",
            "contentInfo[0].attributeGroup[0].attribute[0].description",               "Sea temperature",
            "contentInfo[0].attributeGroup[0].attribute[0].name[0].code",              "sea_water_temperature",
            "contentInfo[0].attributeGroup[0].attribute[0].transferFunctionType",      TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[0].scaleFactor",               0.0011,
            "contentInfo[0].attributeGroup[0].attribute[0].offset",                    -1.85,
            "contentInfo[0].attributeGroup[0].attribute[0].units",                     "°C",

            "resourceLineage[0].statement", "Decimated and modified by GeoAPI for inclusion in conformance test suite.");

        return verifier;
    }
}
