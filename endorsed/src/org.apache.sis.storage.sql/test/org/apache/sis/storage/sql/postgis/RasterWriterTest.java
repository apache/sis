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

import java.awt.image.Raster;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import org.apache.sis.io.stream.ChannelDataOutput;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link WKBRasterReader}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class RasterWriterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public RasterWriterTest() {
    }

    /**
     * Tests writing a raster in unsigned short format.
     * This method writes the raster in an array and compares the result with
     * the expected sequence of bytes provided by {@code "raster-ushort.wkb"} file.
     *
     * @throws IOException if an error occurred while writing the test file.
     * @throws Exception if an error occurred during the search for SRID code.
     */
    @Test
    public void testUShort() throws Exception {
        compareWriteResult(TestRaster.USHORT);
    }

    /**
     * Writes the raster for the given test enumeration
     * and compares with the expected sequence of bytes.
     */
    private static void compareWriteResult(final TestRaster test) throws Exception {
        final Raster raster = test.createRaster();
        final var writer = new RasterWriter(null);
        final var buffer = new ByteArrayOutputStream(test.length);
        final ChannelDataOutput output = test.output(buffer);
        writer.setGridToCRS(TestRaster.getGridGeometry());
        writer.write(raster, output);
        output.flush();
        assertArrayEquals(test.getEncoded(), buffer.toByteArray());
    }
}
