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
package org.apache.sis.storage.netcdf.base;

import java.time.Instant;
import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import static org.apache.sis.storage.netcdf.AttributeNames.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link Decoder} implementation. The default implementation tests
 * {@link org.apache.sis.storage.netcdf.ucar.DecoderWrapper} since the UCAR
 * library is our reference implementation. However, subclasses can override the
 * {@link #createDecoder(TestData)} method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class DecoderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DecoderTest() {
    }

    /**
     * Tests {@link Decoder#stringValue(String)} with global attributes.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testStringValue() throws IOException, DataStoreException {
        selectDataset(TestData.NETCDF_2D_GEOGRAPHIC);
        assertAttributeEquals("Test data from Sea Surface Temperature Analysis Model", TITLE);
        assertAttributeEquals("Global, two-dimensional model data",                    SUMMARY);
        assertAttributeEquals("Global, two-dimensional model data",                    "SUMMARY");    // test case-insensitive search
        assertAttributeEquals("NOAA/NWS/NCEP",                                         CREATOR.NAME);
        assertAttributeEquals((String) null,                                           CREATOR.EMAIL);
        assertAttributeEquals((String) null,                                           CONTRIBUTOR.NAME);
    }

    /**
     * Tests {@link Decoder#numericValue(String)} with global attributes.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testNumericValue() throws IOException, DataStoreException {
        selectDataset(TestData.NETCDF_2D_GEOGRAPHIC);
        assertAttributeEquals(Double.valueOf( -90), LATITUDE .MINIMUM);
        assertAttributeEquals(Double.valueOf( +90), LATITUDE .MAXIMUM);
        assertAttributeEquals(Double.valueOf(-180), LONGITUDE.MINIMUM);
        assertAttributeEquals(Double.valueOf(+180), LONGITUDE.MAXIMUM);
        assertAttributeEquals((Double) null,        LATITUDE .RESOLUTION);
        assertAttributeEquals((Double) null,        LONGITUDE.RESOLUTION);
    }

    /**
     * Tests {@link Decoder#dateValue(String)} with global attributes.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testDateValue() throws IOException, DataStoreException {
        selectDataset(TestData.NETCDF_2D_GEOGRAPHIC);
        assertAttributeEquals(Instant.parse("2005-09-22T00:00:00Z"), DATE_CREATED);
        assertAttributeEquals(Instant.parse("2018-05-15T13:00:00Z"), DATE_MODIFIED);
        assertAttributeEquals((Instant) null, DATE_ISSUED);
    }

    /**
     * Tests {@link Decoder#numberToDate(String, Number[])}.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testNumberToDate() throws IOException, DataStoreException {
        final Decoder decoder = selectDataset(TestData.NETCDF_2D_GEOGRAPHIC);
        assertArrayEquals(new Instant[] {
            Instant.parse("2005-09-22T00:00:00Z")
        }, decoder.numberToDate("hours since 1992-1-1", 120312));

        assertArrayEquals(new Instant[] {
            Instant.parse("1970-01-09T18:00:00Z"),
            Instant.parse("1969-12-29T06:00:00Z"),
            Instant.parse("1993-04-10T00:00:00Z")
        }, decoder.numberToDate("days since 1970-01-01T00:00:00Z", 8.75, -2.75, 8500));
    }

    /**
     * Tests {@link Decoder#getTitle()} and {@link Decoder#getId()}.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testGetTitleAndID() throws IOException, DataStoreException {
        final Decoder decoder = selectDataset(TestData.NETCDF_2D_GEOGRAPHIC);
        /*
         * Actually we really want a null value, even if the NCEP file contains 'title' and 'id' attributes,
         * because the decoder methods are supposed to check only for the "_Title" and "_Id" attributes as a
         * last resort fallback when MetadataReader failed to find the title and identifier by itself.
         */
        assertNull(decoder.getTitle());
        assertNull(decoder.getId());
    }
}
