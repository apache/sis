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
import ucar.nc2.NetcdfFile;
import org.opengis.wrapper.netcdf.IOTestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link Decoder} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final strictfp class DecoderTest extends IOTestCase {
    /**
     * The decoder to test.
     */
    private Decoder decoder;

    /**
     * Tests the {@link DecoderUCAR} implementation.
     *
     * @throws IOException If an error occurred while reading the NetCDF file.
     */
    @Test
    public void testUCAR() throws IOException {
        final NetcdfFile file = open(IOTestCase.NCEP);
        try {
            decoder = new DecoderUCAR(null, file);
            runAllTests();
        } finally {
            file.close();
        }
    }

    /**
     * Runs all the tests defined below this method.
     */
    private void runAllTests() throws IOException {
        decoder.setSearchPath(new String[1]);
        testStringValue();
    }

    /**
     * Tests {@link Decoder#stringValue(String)}.
     */
    private void testStringValue() throws IOException {
        assertEquals("Sea Surface Temperature Analysis Model", decoder.stringValue(AttributeNames.TITLE));
    }
}
