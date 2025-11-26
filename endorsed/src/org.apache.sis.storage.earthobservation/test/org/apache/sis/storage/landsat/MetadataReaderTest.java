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
package org.apache.sis.storage.landsat;

import java.util.regex.Matcher;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static java.util.Map.entry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.acquisition.Context;
import org.opengis.metadata.acquisition.OperationType;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.content.CoverageContentType;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.metadata.identification.Progress;
import org.opengis.metadata.identification.TopicCategory;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.FactoryException;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;
import org.opengis.test.dataset.ContentVerifier;


/**
 * Tests {@link MetadataReader}.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MetadataReaderTest extends TestCase {
    /**
     * Helper class for verifying metadata content.
     */
    private ContentVerifier verifier;

    /**
     * A buffer for building paths to expected properties.
     */
    private StringBuilder buffer;

    /**
     * Creates a new test case.
     */
    public MetadataReaderTest() {
    }

    /**
     * Tests the regular expression used for detecting the
     * “Image courtesy of the U.S. Geological Survey” credit.
     */
    @Test
    public void testCreditPattern() {
        final Matcher m = MetadataReader.CREDIT.matcher("Image courtesy of the U.S. Geological Survey");
        assertTrue(m.find());
        assertEquals(22, m.end());
    }

    /**
     * Tests {@link MetadataReader#read(BufferedReader)}.
     *
     * <p><b>Note for maintainer:</b> if the result of this test changes, consider updating
     * <a href="./doc-files/MetadataMapping.html">./doc-files/MetadataMapping.html</a> accordingly.</p>
     *
     * @throws IOException if an error occurred while reading the test file.
     * @throws DataStoreException if a property value cannot be parsed as a number or a date.
     * @throws FactoryException if an error occurred while creating the Coordinate Reference System.
     */
    @Test
    public void testRead() throws IOException, DataStoreException, FactoryException {
        final Metadata actual;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                MetadataReaderTest.class.getResourceAsStream("LandsatTest.txt"), "UTF-8")))
        {
            final MetadataReader reader = new MetadataReader(null, "LandsatTest.txt", createListeners());
            reader.read(in);
            actual = reader.getMetadata();
        }
        verifier = new ContentVerifier();
        verifier.addPropertyToIgnore(Metadata.class, "metadataStandard");           // Because hard-coded in SIS.
        verifier.addPropertyToIgnore(Metadata.class, "referenceSystemInfo");        // Very verbose and depends on EPSG connection.
        verifier.addPropertyToIgnore(TemporalExtent.class, "extent");               // Because currently time-zone sensitive.
        verifier.addMetadataToVerify(actual);
        verifier.addExpectedValues(
            entry("defaultLocale+otherLocale[0]",   "en"),
            entry("metadataIdentifier.code",        "LandsatTest"),
            entry("metadataScope[0].resourceScope", ScopeCode.COVERAGE),
            entry("dateInfo[0].date",               OffsetDateTime.of(2016, 6, 27, 16, 48, 12, 0, ZoneOffset.UTC)),
            entry("dateInfo[0].dateType",           DateType.CREATION),

            entry("identificationInfo[0].topicCategory[0]",          TopicCategory.GEOSCIENTIFIC_INFORMATION),
            entry("identificationInfo[0].citation.date[0].date",     OffsetDateTime.of(2016, 6, 27, 16, 48, 12, 0, ZoneOffset.UTC)),
            entry("identificationInfo[0].citation.date[0].dateType", DateType.CREATION),
            entry("identificationInfo[0].citation.title",            "LandsatTest"),
            entry("identificationInfo[0].credit[0]", "Derived from U.S. Geological Survey data"),

            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.title", "GeoTIFF Coverage Encoding Profile"),
            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.alternateTitle[0]", "GeoTIFF"),
            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.citedResponsibleParty[0].party[0].name", "Open Geospatial Consortium"),
            entry("identificationInfo[0].resourceFormat[0].formatSpecificationCitation.citedResponsibleParty[0].role", Role.PRINCIPAL_INVESTIGATOR),

            entry("identificationInfo[0].extent[0].geographicElement[0].extentTypeCode",     true),
            entry("identificationInfo[0].extent[0].geographicElement[0].westBoundLongitude", 108.34),
            entry("identificationInfo[0].extent[0].geographicElement[0].eastBoundLongitude", 110.44),
            entry("identificationInfo[0].extent[0].geographicElement[0].southBoundLatitude",  10.50),
            entry("identificationInfo[0].extent[0].geographicElement[0].northBoundLatitude",  12.62),
            entry("identificationInfo[0].spatialResolution[0].distance", 15.0),
            entry("identificationInfo[0].spatialResolution[1].distance", 30.0),

            entry("acquisitionInformation[0].platform[0].identifier.code",               "Pseudo LANDSAT"),
            entry("acquisitionInformation[0].platform[0].instrument[0].identifier.code", "Pseudo TIRS"),
            entry("acquisitionInformation[0].acquisitionRequirement[0].identifier.code", "Software unit tests"),
            entry("acquisitionInformation[0].operation[0].significantEvent[0].context",  Context.ACQUISITION),
            entry("acquisitionInformation[0].operation[0].significantEvent[0].time",     OffsetDateTime.of(2016, 6, 26, 3, 2, 1, 90_000_000, ZoneOffset.UTC)),
            entry("acquisitionInformation[0].operation[0].status", Progress.COMPLETED),
            entry("acquisitionInformation[0].operation[0].type",   OperationType.REAL),

            entry("contentInfo[0].processingLevelCode.authority.title", "Landsat"),
            entry("contentInfo[0].processingLevelCode.codeSpace",       "Landsat"),
            entry("contentInfo[0].processingLevelCode.code",            "Pseudo LT1"),

            entry("contentInfo[0].cloudCoverPercentage",        8.3),
            entry("contentInfo[0].illuminationAzimuthAngle",  116.9),
            entry("contentInfo[0].illuminationElevationAngle", 58.8),

            entry("spatialRepresentationInfo[0].numberOfDimensions", 2),
            entry("spatialRepresentationInfo[1].numberOfDimensions", 2),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionName", DimensionNameType.SAMPLE),
            entry("spatialRepresentationInfo[1].axisDimensionProperties[0].dimensionName", DimensionNameType.SAMPLE),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionName", DimensionNameType.LINE),
            entry("spatialRepresentationInfo[1].axisDimensionProperties[1].dimensionName", DimensionNameType.LINE),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[0].dimensionSize",  7600),
            entry("spatialRepresentationInfo[0].axisDimensionProperties[1].dimensionSize",  7800),
            entry("spatialRepresentationInfo[1].axisDimensionProperties[0].dimensionSize", 15000),
            entry("spatialRepresentationInfo[1].axisDimensionProperties[1].dimensionSize", 15500),
            entry("spatialRepresentationInfo[0].transformationParameterAvailability", false),
            entry("spatialRepresentationInfo[1].transformationParameterAvailability", false),
            entry("spatialRepresentationInfo[0].checkPointAvailability", false),
            entry("spatialRepresentationInfo[1].checkPointAvailability", false),

            entry("resourceLineage[0].source[0].description", "Pseudo GLS"));

        /*
         * The expected values in "contentInfo[0].attributeGroup[…].attribute[…].*" have a lot of redundancy.
         * Therefore, we set those expected values by loop instead of repeating tens of long property paths.
         */
        final String[] descriptions = {
            "Coastal Aerosol",
            "Blue",
            "Green",
            "Red",
            "Near-Infrared",
            "Short Wavelength Infrared (SWIR) 1",
            "Short Wavelength Infrared (SWIR) 2",
            "Cirrus",
            "Panchromatic",
            "Thermal Infrared Sensor (TIRS) 1",
            "Thermal Infrared Sensor (TIRS) 2"
        };
        final short[] peakResponses = {433, 482, 562, 655, 865, 1610, 2200, 1375, 590, 10800, 12000};
        int band = 0;

        buffer = new StringBuilder(80).append("contentInfo[0].attributeGroup[");
        final int groupBase = buffer.length();
        final int[] numAttributes = {8, 1, 2};
        for (int group = 0; group < numAttributes.length; group++) {
            final boolean mainGroups = (group != 2);
            /*
             * contentInfo[0].attributeGroup[0…2].contentType[0]
             */
            buffer.setLength(groupBase);
            buffer.append(group).append("].");
            addExpectedValue("contentType[0]", CoverageContentType.PHYSICAL_MEASUREMENT);
            /*
             * contentInfo[0].attributeGroup[0…2].attribute[…].minValue
             * contentInfo[0].attributeGroup[0…2].attribute[…].maxValue
             * ... etc ...
             */
            final int attributeBase = buffer.append("attribute[").length();
            for (int attribute = 0; attribute < numAttributes[group]; attribute++) {
                buffer.setLength(attributeBase);
                buffer.append(attribute).append("].");
                addExpectedValue("minValue", 1.0);
                addExpectedValue("maxValue", 65535.0);
                addExpectedValue("description", descriptions[band]);
                addExpectedValue("peakResponse", (double) peakResponses[band++]);
                addExpectedValue("boundUnits", "nm");
                addExpectedValue("transferFunctionType", TransferFunctionType.LINEAR);
                addExpectedValue("scaleFactor", mainGroups ? 2.0E-5 : 0.000334);
                addExpectedValue("offset", mainGroups ? -0.1 : 0.1);
                if (mainGroups) {
                    addExpectedValue("units", "");
                }
            }
        }
        assertEquals(descriptions.length, band);
        assertEquals(peakResponses.length, band);
        verifier.assertMetadataEquals();
    }

    /**
     * Adds an expected value for the given property. The path to that property is the
     * current content of {@link #buffer}, including a trailing {@code '.'} separator.
     * The buffer is reset to its original length after this method call.
     */
    private void addExpectedValue(final String tip, Object value) {
        final int length = buffer.length();
        verifier.addExpectedValue(buffer.append(tip).toString(), value);
        buffer.setLength(length);
    }

    /**
     * Creates a dummy set of store listeners.
     * Used only for constructors that require a non-null {@link StoreListeners} instance.
     *
     * @return a dummy set of listeners.
     */
    private static StoreListeners createListeners() {
        final class DummyResource extends AbstractResource {
            /** Creates a dummy resource without parent. */
            DummyResource() {
                super(null);
            }

            /** Makes listeners accessible to this package. */
            StoreListeners listeners() {
                return listeners;
            }
        }
        return new DummyResource().listeners();
    }
}
