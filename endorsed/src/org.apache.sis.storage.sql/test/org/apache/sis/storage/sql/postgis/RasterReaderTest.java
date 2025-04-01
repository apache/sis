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
package org.apache.sis.storage.sql.postgis;

import java.util.Arrays;
import java.awt.image.RenderedImage;
import java.awt.image.DataBufferUShort;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.io.stream.ChannelDataInput;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link RasterReader}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RasterReaderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public RasterReaderTest() {
    }

    /**
     * Tests reading a raster in unsigned short format.
     * This method reads the {@code "raster-ushort.wkb"} file
     * and compares the result with the expected raster.
     *
     * @throws Exception if an error occurred while reading or decoding the test file.
     */
    @Test
    public void testUShort() throws Exception {
        RasterReaderTest.compareReadResult(TestRaster.USHORT);
    }

    /**
     * Reads the file for the given test enumeration and compares with the expected raster.
     */
    private static void compareReadResult(final TestRaster test) throws Exception {
        RasterReaderTest.compareReadResult(test, new RasterReader(null), test.input());
    }

    /**
     * Reads the file for the given test enumeration and compares with the expected raster.
     * The given reader and input are used for reading the raster. The input will be closed.
     */
    @SuppressWarnings("ConvertToTryWithResources")  // Because testing on a byte array, closing is not very important.
    static void compareReadResult(final TestRaster test, final RasterReader reader, final ChannelDataInput input) throws Exception {
        final GridCoverage coverage = reader.readAsCoverage(input);
        input.channel.close();
        assertEquals(TestRaster.SRID, reader.getSRID());
        assertEquals(TestRaster.getGridToCRS(), reader.getGridToCRS());
        compareReadResult(test, coverage);
    }

    /**
     * Compares the given image with the expected raster.
     */
    static void compareReadResult(final TestRaster test, final GridCoverage coverage) {
        final RenderedImage image = coverage.render(null);
        final var expected = (DataBufferUShort) test.createRaster().getDataBuffer();
        final var actual   = (DataBufferUShort) image.getTile(0, 0).getDataBuffer();
        assertTrue(Arrays.deepEquals(expected.getBankData(), actual.getBankData()));
    }
}
