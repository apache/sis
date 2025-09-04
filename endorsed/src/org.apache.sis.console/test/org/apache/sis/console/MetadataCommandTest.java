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
package org.apache.sis.console;

import java.net.URL;
import org.apache.sis.system.Loggers;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCaseWithLogs;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.dataset.TestData;


/**
 * Tests the {@link MetadataCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MetadataCommandTest extends TestCaseWithLogs {
    /**
     * Creates a new test case.
     */
    public MetadataCommandTest() {
        super(Loggers.CRS_FACTORY);
    }

    /**
     * Tests the sub-command on a netCDF file.
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    public void testNetCDF() throws Exception {
        final URL url = TestData.NETCDF_2D_GEOGRAPHIC.location();
        var test = new MetadataCommand(0, new String[] {CommandRunner.TEST, url.toString()});
        test.run();
        verifyNetCDF("Metadata", test.outputBuffer.toString());
        loggings.skipNextLogIfContains("EPSG:6019");    // Warning about deprecated EPSG code for datum.
        loggings.skipNextLogIfContains("EPSG:4019");    // Warning about deprecated EPSG code for CRS.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies the netCDF metadata. The given string can be either a text format or XML format.
     * This method will check only for some keyword - this is not an extensive check of the result.
     */
    private static void verifyNetCDF(final String expectedHeader, final String result) {
        assertTrue(result.startsWith(expectedHeader));
        assertTrue(result.contains("Sea Surface Temperature Analysis Model"));
        assertTrue(result.contains("GCMD Science Keywords"));
        assertTrue(result.contains("NOAA/NWS/NCEP"));
    }

    /**
     * Tests with the same file as {@link #testNetCDF()}, but producing a XML output.
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    public void testFormatXML() throws Exception {
        final URL url = TestData.NETCDF_2D_GEOGRAPHIC.location();
        var test = new MetadataCommand(0, new String[] {CommandRunner.TEST, url.toString(), "--format", "XML"});
        test.run();
        verifyNetCDF("<?xml", test.outputBuffer.toString());
        loggings.skipNextLogIfContains("EPSG:6019");    // Warning about deprecated EPSG code for datum.
        loggings.skipNextLogIfContains("EPSG:4019");    // Warning about deprecated EPSG code for CRS.
        loggings.assertNoUnexpectedLog();
    }
}
