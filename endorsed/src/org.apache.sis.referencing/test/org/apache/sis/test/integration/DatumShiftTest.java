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
import org.apache.sis.system.DataDirectory;
import org.apache.sis.referencing.operation.gridded.GridFile;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.operation.provider.NTv2Test;
import org.apache.sis.referencing.operation.provider.NADCONTest;
import org.apache.sis.referencing.operation.provider.FranceGeocentricInterpolationTest;
import org.apache.sis.test.TestCase;


/**
 * Tests datum shifts using the official grid files rather than the small extracts distributed in the SIS
 * {@code test/resources} directories. The grid files need to be stored in the {@code $SIS_DATA/DatumChanges}
 * directory.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DatumShiftTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DatumShiftTest() {
    }

    /**
     * Tests loading an official {@code "ntf_r93.gsb"} datum shift grid file
     * and interpolating the sample point tested by {@link FranceGeocentricInterpolationTest}.
     *
     * @throws Exception if an error occurred while loading or computing the grid, or while testing transformations.
     */
    @Test
    public void testRGF93() throws Exception {
        final URI file = assumeDataExists(DataDirectory.DATUM_CHANGES, "ntf_r93.gsb").toUri();
        NTv2Test.testRGF93(new GridFile(file));
    }

    /**
     * Tests loading the official {@code "conus.las"} and {@code "conus.los"} datum shift grid files
     * and interpolating a sample point tested by {@link NADCONTest}.
     *
     * @throws Exception if an error occurred while loading or computing the grid, or while testing transformations.
     */
    @Test
    public void testNADCON() throws Exception {
        final URI latitudeShifts  = assumeDataExists(DataDirectory.DATUM_CHANGES, "conus.las").toUri();
        final URI longitudeShifts = assumeDataExists(DataDirectory.DATUM_CHANGES, "conus.los").toUri();
        NADCONTest.testNADCON(new GridFile(latitudeShifts), new GridFile(longitudeShifts));
    }
}
