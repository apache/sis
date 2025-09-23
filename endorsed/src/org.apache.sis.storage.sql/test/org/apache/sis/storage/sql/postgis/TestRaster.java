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

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;


/**
 * Enumeration of WKB rasters available for testing purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum TestRaster {
    /**
     * A raster with pixel values stored as unsigned short integers.
     */
    USHORT(DataBuffer.TYPE_USHORT, "raster-ushort.wkb", 115);

    /**
     * Arbitrary size (in pixels) and number of bands of test rasters.
     */
    private static final int WIDTH = 3, HEIGHT = 4, NUM_BANDS = 2;

    /**
     * The spatial reference identifier used for the test rasters.
     *
     * @see #getGridToCRS()
     */
    static final int SRID = 4326;

    /**
     * The raster type as a {@link DataBuffer} constant.
     */
    private final int dataType;

    /**
     * Name of the file where the WKB raster is stored.
     */
    final String filename;

    /**
     * Expected file size in bytes.
     */
    final int length;

    /**
     * Creates a new enumeration value for a test raster.
     */
    private TestRaster(final int dataType, final String filename, final int length) {
        this.dataType = dataType;
        this.filename = filename;
        this.length   = length;
    }

    /**
     * Creates a raster filled with arbitrary pixel values.
     */
    final Raster createRaster() {
        final WritableRaster raster = WritableRaster.createBandedRaster(dataType, WIDTH, HEIGHT, NUM_BANDS, null);
        final int[] samples = new int[NUM_BANDS];
        for (int y=0; y<HEIGHT; y++) {
            int value = (y+1) * 100;
            for (int x=0; x<WIDTH; x++) {
                value += 10;
                for (int b=0; b<NUM_BANDS; b++) {
                    samples[b] = value + (b+1);
                }
                raster.setPixel(x, y, samples);
            }
        }
        return raster;
    }

    /**
     * Returns the sequence of bytes for the test raster encoded in WKB.
     */
    final byte[] getEncoded() throws IOException {
        try (InputStream in = TestRaster.class.getResourceAsStream(filename)) {
            final byte[] buffer = new byte[length];
            assertEquals(length, in.read(buffer));
            assertEquals(-1, in.read());
            return buffer;
        }
    }

    /**
     * Returns the input sequence of bytes as a channel.
     */
    final ChannelDataInput input() throws IOException {
        final ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(getEncoded()));
        return new ChannelDataInput(filename, channel, ByteBuffer.allocate(60), false);
    }

    /**
     * Returns a channel where to write the output sequence of bytes.
     *
     * @param  output  the final destination where to write bytes.
     */
    final ChannelDataOutput output(final ByteArrayOutputStream dest) throws IOException {
        return new ChannelDataOutput(filename, Channels.newChannel(dest), ByteBuffer.allocate(60));
    }

    /**
     * Returns the "grid to CRS" transform used for the tests.
     */
    static AffineTransform2D getGridToCRS() {
        return new AffineTransform2D(1.25, 0, 0, 2.5, -80, -60);
    }

    /**
     * Returns the grid geometry which contains the "grid to CRS" transform together with {@link #SRID}.
     *
     * @see #SRID
     */
    static GridGeometry getGridGeometry() {
        return new GridGeometry(null, RasterFormat.ANCHOR, getGridToCRS(), CommonCRS.WGS84.geographic());
    }
}
