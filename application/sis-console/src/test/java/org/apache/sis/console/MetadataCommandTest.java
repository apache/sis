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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link MetadataCommand} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@DependsOn(CommandRunnerTest.class)
public final strictfp class MetadataCommandTest extends TestCase {
    /**
     * Verifies the {@link MetadataCommand#MAX_AUTHORITY_LENGTH} value.
     */
    @Test
    public void verifyMaxAuthorityLength() {
        int length = 0;
        for (final String authority : MetadataCommand.AUTHORITIES) {
            final int c = authority.length();
            if (c > length) length = c;
        }
        assertEquals("MAX_AUTHORITY_LENGTH", length, MetadataCommand.MAX_AUTHORITY_LENGTH);
    }

    /**
     * Tests the sub-command on a NetCDF file.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @Ignore("Requires GeoAPI 3.1")
    public void testNetCDF() throws Exception {
        final URL url = MetadataCommandTest.class.getResource("NCEP-SST.nc");
        assertNotNull("NCEP-SST.nc", url);
        final MetadataCommand test = new MetadataCommand(0, CommandRunner.TEST, url.toString());
        test.run();
        verifyNetCDF("Metadata", test.outputBuffer.toString());
    }

    /**
     * Verifies the NetCDF metadata. The given string can be either a text format or XML format.
     * This method will check only for some keyword - this is not an extensive check of the result.
     */
    private static void verifyNetCDF(final String expectedHeader, final String result) {
        assertTrue(expectedHeader,                           result.startsWith(expectedHeader));
        assertTrue("ISO 19115",                              result.contains("ISO 19115"));
        assertTrue("Sea Surface Temperature Analysis Model", result.contains("Sea Surface Temperature Analysis Model"));
        assertTrue("GCMD Science Keywords",                  result.contains("GCMD Science Keywords"));
        assertTrue("NOAA/NWS/NCEP",                          result.contains("NOAA/NWS/NCEP"));
    }

    /**
     * Tests with the same file than {@link #testNetCDF()}, but producing a XML output.
     *
     * @throws Exception Should never happen.
     */
    @Test
    @Ignore("Requires GeoAPI 3.1")
    @DependsOnMethod("testNetCDF")
    public void testFormatXML() throws Exception {
        final URL url = MetadataCommandTest.class.getResource("NCEP-SST.nc");
        final MetadataCommand test = new MetadataCommand(0, CommandRunner.TEST, url.toString(), "--format", "XML");
        test.run();
        verifyNetCDF("<?xml", test.outputBuffer.toString());
    }
}
