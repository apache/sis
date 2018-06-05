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
package org.apache.sis.storage.earthobservation;

import java.util.Locale;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.Context;
import org.opengis.metadata.acquisition.OperationType;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.FactoryException;
import org.opengis.test.dataset.ContentVerifier;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.EmptyWarningListeners;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.storage.earthobservation.LandsatReader.DIM;


/**
 * Tests {@link LandsatReader}.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public class LandsatReaderTest extends TestCase {
    /**
     * Tests the regular expression used for detecting the
     * “Image courtesy of the U.S. Geological Survey” credit.
     */
    @Test
    public void testCreditPattern() {
        final Matcher m = LandsatReader.CREDIT.matcher("Image courtesy of the U.S. Geological Survey");
        assertTrue("matches", m.find());
        assertEquals("end", 22, m.end());
    }

    /**
     * Verifies the value of the {@link LandsatReader#BAND_GROUPS} mask.
     */
    @Test
    public void verifyBandGroupsMask() {
        final int[] PANCHROMATIC = {8};
        final int[] REFLECTIVE   = {1, 2, 3, 4, 5, 6, 7, 9};
        final int[] THERMAL      = {10, 11};
        long mask = 0;
        for (int i=0; i < PANCHROMATIC.length; i++) mask |= (LandsatReader.PANCHROMATIC/DIM << 2*(PANCHROMATIC[i] - 1));
        for (int i=0; i <   REFLECTIVE.length; i++) mask |= (LandsatReader.REFLECTIVE/DIM   <<   2*(REFLECTIVE[i] - 1));
        for (int i=0; i <      THERMAL.length; i++) mask |= (LandsatReader.THERMAL/DIM      <<      2*(THERMAL[i] - 1));
        assertEquals("BAND_GROUPS", mask, LandsatReader.BAND_GROUPS);
    }

    /**
     * Tests {@link LandsatReader#read(BufferedReader)}.
     *
     * <p><b>NOTE FOR MAINTAINER:</b> if the result of this test changes, consider updating
     * <a href="./doc-files/LandsatMetadata.html">./doc-files/LandsatMetadata.html</a> accordingly.</p>
     *
     * @throws IOException if an error occurred while reading the test file.
     * @throws DataStoreException if a property value can not be parsed as a number or a date.
     * @throws FactoryException if an error occurred while creating the Coordinate Reference System.
     */
    @Test
    public void testRead() throws IOException, DataStoreException, FactoryException {
        final Metadata actual;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                LandsatReaderTest.class.getResourceAsStream("LandsatTest.txt"), "UTF-8")))
        {
            final LandsatReader reader = new LandsatReader("LandsatTest.txt",
                    new EmptyWarningListeners<>(Locale.ENGLISH, Modules.EARTH_OBSERVATION));
            reader.read(in);
            actual = reader.getMetadata();
        }
        final ContentVerifier verifier = new ContentVerifier();
        verifier.addPropertyToIgnore(Metadata.class, "metadataStandard");           // Because hard-coded in SIS.
        verifier.addPropertyToIgnore(Metadata.class, "referenceSystemInfo");        // Very verbose and depends on EPSG connection.
        verifier.addMetadataToVerify(actual);
        verifier.assertMetadataEquals(
            "language[0]",                                                                           "en",
            "metadataIdentifier.code",                                                               "LandsatTest",
            "metadataScope[0].resourceScope",                                                        ScopeCode.COVERAGE,
            "dateInfo[0].date",                                                                      date("2016-06-27 16:48:12"),
            "dateInfo[0].dateType",                                                                  DateType.CREATION,
            "identificationInfo[0].citation.date[0].date",                                           date("2016-06-27 16:48:12"),
            "identificationInfo[0].citation.date[0].dateType",                                       DateType.CREATION,
            "identificationInfo[0].citation.title",                                                  "LandsatTest",
            "identificationInfo[0].credit[0]",                                                       "Derived from U.S. Geological Survey data",
            "identificationInfo[0].resourceFormat[0].formatSpecificationCitation.title",             "GeoTIFF Coverage Encoding Profile",
            "identificationInfo[0].resourceFormat[0].formatSpecificationCitation.alternateTitle[0]", "GeoTIFF",
            "identificationInfo[0].extent[0].geographicElement[0].extentTypeCode",                   true,
            "identificationInfo[0].extent[0].geographicElement[0].westBoundLongitude",               108.34,
            "identificationInfo[0].extent[0].geographicElement[0].eastBoundLongitude",               110.44,
            "identificationInfo[0].extent[0].geographicElement[0].southBoundLatitude",                10.50,
            "identificationInfo[0].extent[0].geographicElement[0].northBoundLatitude",                12.62,
            "identificationInfo[0].spatialResolution[0].distance",                                    15.0,
            "identificationInfo[0].spatialResolution[1].distance",                                    30.0,

            "acquisitionInformation[0].platform[0].identifier.code",               "Pseudo LANDSAT",
            "acquisitionInformation[0].platform[0].instrument[0].identifier.code", "Pseudo TIRS",
            "acquisitionInformation[0].acquisitionRequirement[0].identifier.code", "Software unit tests",
            "acquisitionInformation[0].operation[0].significantEvent[0].context",  Context.ACQUISITION,
            "acquisitionInformation[0].operation[0].significantEvent[0].time",     date("2016-06-26 03:02:01.090"),
            "acquisitionInformation[0].operation[0].status",                       Progress.COMPLETED,
            "acquisitionInformation[0].operation[0].type",                         OperationType.REAL,

            "contentInfo[0].attributeGroup[0].attribute[0].name[0].code",  "TestImage_B1.TIF",
            "contentInfo[0].attributeGroup[0].attribute[1].name[0].code",  "TestImage_B2.TIF",
            "contentInfo[0].attributeGroup[0].attribute[2].name[0].code",  "TestImage_B3.TIF",
            "contentInfo[0].attributeGroup[0].attribute[3].name[0].code",  "TestImage_B4.TIF",
            "contentInfo[0].attributeGroup[0].attribute[4].name[0].code",  "TestImage_B5.TIF",
            "contentInfo[0].attributeGroup[0].attribute[5].name[0].code",  "TestImage_B6.TIF",
            "contentInfo[0].attributeGroup[0].attribute[6].name[0].code",  "TestImage_B7.TIF",
            "contentInfo[0].attributeGroup[0].attribute[7].name[0].code",  "TestImage_B9.TIF",
            "contentInfo[0].attributeGroup[1].attribute[0].name[0].code",  "TestImage_B8.TIF",
            "contentInfo[0].attributeGroup[2].attribute[0].name[0].code",  "TestImage_B10.TIF",
            "contentInfo[0].attributeGroup[2].attribute[1].name[0].code",  "TestImage_B11.TIF",

            "contentInfo[0].attributeGroup[0].attribute[0].description",   "Coastal Aerosol",
            "contentInfo[0].attributeGroup[0].attribute[1].description",   "Blue",
            "contentInfo[0].attributeGroup[0].attribute[2].description",   "Green",
            "contentInfo[0].attributeGroup[0].attribute[3].description",   "Red",
            "contentInfo[0].attributeGroup[0].attribute[4].description",   "Near-Infrared",
            "contentInfo[0].attributeGroup[0].attribute[5].description",   "Short Wavelength Infrared (SWIR) 1",
            "contentInfo[0].attributeGroup[0].attribute[6].description",   "Short Wavelength Infrared (SWIR) 2",
            "contentInfo[0].attributeGroup[0].attribute[7].description",   "Cirrus",
            "contentInfo[0].attributeGroup[1].attribute[0].description",   "Panchromatic",
            "contentInfo[0].attributeGroup[2].attribute[0].description",   "Thermal Infrared Sensor (TIRS) 1",
            "contentInfo[0].attributeGroup[2].attribute[1].description",   "Thermal Infrared Sensor (TIRS) 2",

            "contentInfo[0].attributeGroup[0].attribute[0].minValue",      1.0,
            "contentInfo[0].attributeGroup[0].attribute[1].minValue",      1.0,
            "contentInfo[0].attributeGroup[0].attribute[2].minValue",      1.0,
            "contentInfo[0].attributeGroup[0].attribute[3].minValue",      1.0,
            "contentInfo[0].attributeGroup[0].attribute[4].minValue",      1.0,
            "contentInfo[0].attributeGroup[0].attribute[5].minValue",      1.0,
            "contentInfo[0].attributeGroup[0].attribute[6].minValue",      1.0,
            "contentInfo[0].attributeGroup[0].attribute[7].minValue",      1.0,
            "contentInfo[0].attributeGroup[1].attribute[0].minValue",      1.0,
            "contentInfo[0].attributeGroup[2].attribute[0].minValue",      1.0,
            "contentInfo[0].attributeGroup[2].attribute[1].minValue",      1.0,

            "contentInfo[0].attributeGroup[0].attribute[0].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[0].attribute[1].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[0].attribute[2].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[0].attribute[3].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[0].attribute[4].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[0].attribute[5].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[0].attribute[6].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[0].attribute[7].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[1].attribute[0].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[2].attribute[0].maxValue",      65535.0,
            "contentInfo[0].attributeGroup[2].attribute[1].maxValue",      65535.0,

            "contentInfo[0].attributeGroup[0].attribute[0].peakResponse",    433.0,
            "contentInfo[0].attributeGroup[0].attribute[1].peakResponse",    482.0,
            "contentInfo[0].attributeGroup[0].attribute[2].peakResponse",    562.0,
            "contentInfo[0].attributeGroup[0].attribute[3].peakResponse",    655.0,
            "contentInfo[0].attributeGroup[0].attribute[4].peakResponse",    865.0,
            "contentInfo[0].attributeGroup[0].attribute[5].peakResponse",   1610.0,
            "contentInfo[0].attributeGroup[0].attribute[6].peakResponse",   2200.0,
            "contentInfo[0].attributeGroup[0].attribute[7].peakResponse",   1375.0,
            "contentInfo[0].attributeGroup[1].attribute[0].peakResponse",    590.0,
            "contentInfo[0].attributeGroup[2].attribute[0].peakResponse",  10800.0,
            "contentInfo[0].attributeGroup[2].attribute[1].peakResponse",  12000.0,

            "contentInfo[0].attributeGroup[0].attribute[0].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[1].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[2].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[3].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[4].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[5].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[6].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[0].attribute[7].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[1].attribute[0].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[2].attribute[0].transferFunctionType",  TransferFunctionType.LINEAR,
            "contentInfo[0].attributeGroup[2].attribute[1].transferFunctionType",  TransferFunctionType.LINEAR,

            "contentInfo[0].attributeGroup[0].attribute[0].scaleFactor",  0.0127,
            "contentInfo[0].attributeGroup[0].attribute[1].scaleFactor",  0.013,
            "contentInfo[0].attributeGroup[0].attribute[2].scaleFactor",  0.012,
            "contentInfo[0].attributeGroup[0].attribute[3].scaleFactor",  0.0101,
            "contentInfo[0].attributeGroup[0].attribute[4].scaleFactor",  0.00619,
            "contentInfo[0].attributeGroup[0].attribute[5].scaleFactor",  0.00154,
            "contentInfo[0].attributeGroup[0].attribute[6].scaleFactor",  0.000519,
            "contentInfo[0].attributeGroup[0].attribute[7].scaleFactor",  0.00242,
            "contentInfo[0].attributeGroup[1].attribute[0].scaleFactor",  0.0115,
            "contentInfo[0].attributeGroup[2].attribute[0].scaleFactor",  0.000334,
            "contentInfo[0].attributeGroup[2].attribute[1].scaleFactor",  0.000334,

            "contentInfo[0].attributeGroup[0].attribute[0].offset",       -63.6,
            "contentInfo[0].attributeGroup[0].attribute[1].offset",       -65.1,
            "contentInfo[0].attributeGroup[0].attribute[2].offset",       -60.0,
            "contentInfo[0].attributeGroup[0].attribute[3].offset",       -50.6,
            "contentInfo[0].attributeGroup[0].attribute[4].offset",       -31.0,
            "contentInfo[0].attributeGroup[0].attribute[5].offset",       -7.7,
            "contentInfo[0].attributeGroup[0].attribute[6].offset",       -2.6,
            "contentInfo[0].attributeGroup[0].attribute[7].offset",       -12.1,
            "contentInfo[0].attributeGroup[1].attribute[0].offset",       -57.3,
            "contentInfo[0].attributeGroup[2].attribute[0].offset",       0.1,
            "contentInfo[0].attributeGroup[2].attribute[1].offset",       0.1,

            "contentInfo[0].attributeGroup[0].attribute[0].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[0].attribute[1].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[0].attribute[2].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[0].attribute[3].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[0].attribute[4].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[0].attribute[5].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[0].attribute[6].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[0].attribute[7].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[1].attribute[0].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[2].attribute[0].boundUnits",   "nm",
            "contentInfo[0].attributeGroup[2].attribute[1].boundUnits",   "nm",

            "contentInfo[0].attributeGroup[0].contentType[0]", CoverageContentType.PHYSICAL_MEASUREMENT,
            "contentInfo[0].attributeGroup[1].contentType[0]", CoverageContentType.PHYSICAL_MEASUREMENT,
            "contentInfo[0].attributeGroup[2].contentType[0]", CoverageContentType.PHYSICAL_MEASUREMENT,

            "contentInfo[0].cloudCoverPercentage",         8.3,
            "contentInfo[0].illuminationAzimuthAngle",   116.9,
            "contentInfo[0].illuminationElevationAngle",  58.8,

            "spatialRepresentationInfo[0].numberOfDimensions",                       2,
            "spatialRepresentationInfo[1].numberOfDimensions",                       2,
            "spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionName", DimensionNameType.SAMPLE,
            "spatialRepresentationInfo[1].axisDimensionProperties[0].dimensionName", DimensionNameType.SAMPLE,
            "spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionName", DimensionNameType.LINE,
            "spatialRepresentationInfo[1].axisDimensionProperties[1].dimensionName", DimensionNameType.LINE,
            "spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionSize", 15000,
            "spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionSize", 15500,
            "spatialRepresentationInfo[1].axisDimensionProperties[0].dimensionSize", 7600,
            "spatialRepresentationInfo[1].axisDimensionProperties[1].dimensionSize", 7800,
            "spatialRepresentationInfo[0].transformationParameterAvailability",      false,
            "spatialRepresentationInfo[1].transformationParameterAvailability",      false,
            "spatialRepresentationInfo[0].checkPointAvailability",                   false,
            "spatialRepresentationInfo[1].checkPointAvailability",                   false);
    }
}
