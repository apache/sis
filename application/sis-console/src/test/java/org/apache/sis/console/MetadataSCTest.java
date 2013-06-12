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
import org.opengis.wrapper.netcdf.IOTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link MetadataSC} sub-command.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(SubCommandTest.class)
public final strictfp class MetadataSCTest extends TestCase {
    /**
     * Tests the sub-command on a NetCDF file.
     *
     * @throws Exception Should never happen.
     */
    @Test
    public void testNetCDF() throws Exception {
        final URL url = IOTestCase.class.getResource(IOTestCase.NCEP);
        assertNotNull(IOTestCase.NCEP, url);
        final MetadataSC test = new MetadataSC(0, SubCommand.TEST, url.toString());
        test.run();
        final String result = test.outputBuffer.toString();
        assertTrue("DefaultMetadata",                        result.startsWith("DefaultMetadata"));
        assertTrue("ISO 19115-2",                            result.contains  ("ISO 19115-2"));
        assertTrue("Sea Surface Temperature Analysis Model", result.contains  ("Sea Surface Temperature Analysis Model"));
        assertTrue("GCMD Science Keywords",                  result.contains  ("GCMD Science Keywords"));
        assertTrue("NOAA/NWS/NCEP",                          result.contains  ("NOAA/NWS/NCEP"));
    }
}
