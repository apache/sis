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
import org.opengis.test.dataset.TestData;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link MetadataCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
@DependsOn(CommandRunnerTest.class)
public final strictfp class MetadataCommandTest extends TestCase {
    /**
     * Tests the sub-command on a netCDF file.
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    public void testNetCDF() throws Exception {
        final URL url = TestData.NETCDF_2D_GEOGRAPHIC.location();
        final MetadataCommand test = new MetadataCommand(0, CommandRunner.TEST, url.toString());
        test.run();
        verifyNetCDF("Metadata", test.outputBuffer.toString());
    }

    /**
     * Verifies the netCDF metadata. The given string can be either a text format or XML format.
     * This method will check only for some keyword - this is not an extensive check of the result.
     */
    private static void verifyNetCDF(final String expectedHeader, final String result) {
        assertTrue(expectedHeader,                           result.startsWith(expectedHeader));
        assertTrue("Sea Surface Temperature Analysis Model", result.contains("Sea Surface Temperature Analysis Model"));
        assertTrue("GCMD Science Keywords",                  result.contains("GCMD Science Keywords"));
        assertTrue("NOAA/NWS/NCEP",                          result.contains("NOAA/NWS/NCEP"));
    }

    /**
     * Tests with the same file than {@link #testNetCDF()}, but producing a XML output.
     *
     * @throws Exception if an error occurred while creating the command.
     */
    @Test
    @DependsOnMethod("testNetCDF")
    public void testFormatXML() throws Exception {
        final URL url = TestData.NETCDF_2D_GEOGRAPHIC.location();
        final MetadataCommand test = new MetadataCommand(0, CommandRunner.TEST, url.toString(), "--format", "XML");
        test.run();
        verifyNetCDF("<?xml", test.outputBuffer.toString());
    }
}
