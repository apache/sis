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
package org.apache.sis.internal.netcdf;

import java.util.Date;
import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.storage.netcdf.AttributeNames.*;


/**
 * Tests the {@link Decoder} implementation. The default implementation tests
 * {@link org.apache.sis.internal.netcdf.ucar.DecoderWrapper} since the UCAR
 * library is our reference implementation. However subclasses can override the
 * {@link #createDecoder(String)} method in order to test a different implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public strictfp class DecoderTest extends TestCase {
    /**
     * Tests {@link Decoder#stringValue(String)} with global attributes.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testStringValue() throws IOException, DataStoreException {
        selectDataset(NCEP);
        assertAttributeEquals("Sea Surface Temperature Analysis Model",      TITLE);
        assertAttributeEquals("NCEP SST Global 5.0 x 2.5 degree model data", SUMMARY);
        assertAttributeEquals("NCEP SST Global 5.0 x 2.5 degree model data", "SUMMARY"); // test case-insensitive search
        assertAttributeEquals("NOAA/NWS/NCEP",                               CREATOR.NAME);
        assertAttributeEquals(/* Empty string in file   */ (String) null,    CREATOR.EMAIL);
        assertAttributeEquals(/* Non-existent attribute */ (String) null,    CONTRIBUTOR.NAME);

        if (isSupplementalFormatSupported("HDF5")) {
            selectDataset(CIP);
            assertAttributeEquals(/* Only control character */ (String) null,   TITLE);
            assertAttributeEquals("UCAR",                                       CREATOR.INSTITUTION);
            assertAttributeEquals("U.S. National Weather Service - NCEP (WMC)", HISTORY);
        }
    }

    /**
     * Tests {@link Decoder#numericValue(String)} with global attributes.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testNumericValue() throws IOException, DataStoreException {
        selectDataset(NCEP);
        assertAttributeEquals(Double.valueOf( -90), LATITUDE .MINIMUM);
        assertAttributeEquals(Double.valueOf( +90), LATITUDE .MAXIMUM);
        assertAttributeEquals(Double.valueOf(-180), LONGITUDE.MINIMUM);
        assertAttributeEquals(Double.valueOf(+180), LONGITUDE.MAXIMUM);
        assertAttributeEquals((Double) null,        LATITUDE .RESOLUTION);
        assertAttributeEquals((Double) null,        LONGITUDE.RESOLUTION);
        assertAttributeEquals(Double.valueOf(1),    "version"); // Specific to the NCEP file.
    }

    /**
     * Tests {@link Decoder#dateValue(String)} with global attributes.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testDateValue() throws IOException, DataStoreException {
        selectDataset(NCEP);
        assertAttributeEquals(date("2005-09-22 00:00:00"), DATE_CREATED);
        assertAttributeEquals((Date) null,                 DATE_MODIFIED);
    }

    /**
     * Tests {@link Decoder#numberToDate(String, Number[])}.
     *
     * @throws IOException if an I/O error occurred while opening the file.
     * @throws DataStoreException if a logical error occurred.
     */
    @Test
    public void testNumberToDate() throws IOException, DataStoreException {
        final Decoder decoder = selectDataset(NCEP);
        assertArrayEquals(new Date[] {
            date("2005-09-22 00:00:00")
        }, decoder.numberToDate("hours since 1992-1-1", 120312));

        assertArrayEquals(new Date[] {
            date("1970-01-09 18:00:00"),
            date("1969-12-29 06:00:00"),
            date("1993-04-10 00:00:00")
        }, decoder.numberToDate("days since 1970-01-01T00:00:00Z", 8.75, -2.75, 8500));
    }
}
