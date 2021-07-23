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
package org.apache.sis.storage.geotiff;

import java.awt.Point;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.Buffer;
import org.apache.sis.internal.storage.TiledGridResource;
import org.apache.sis.internal.coverage.j2d.RasterFactory;
import org.apache.sis.internal.geotiff.Uncompressed;
import org.apache.sis.internal.geotiff.Inflater;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.image.DataType;

import static java.lang.Math.toIntExact;
import static org.apache.sis.internal.jdk9.JDK9.multiplyFull;


/**
 * Raster data obtained from a compressed GeoTIFF file in the domain requested by user.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CompressedSubset extends DataSubset {
    /**
     * Number of sample values to skip for moving to the next row of a tile.
     */
    private final long scanlineStride;

    /**
     * Creates a new data subset. All parameters should have been validated
     * by {@link ImageFileDirectory#validateMandatoryTags()} before this call.
     * This constructor should be invoked inside a synchronized block.
     *
     * @param  source   the resource which contain this {@code DataSubset}.
     * @param  subset   description of the {@code owner} subset to cover.
     * @param  rasters  potentially shared cache of rasters read by this {@code DataSubset}.
     * @throws ArithmeticException if the number of tiles overflows 32 bits integer arithmetic.
     */
    CompressedSubset(final DataCube source, final TiledGridResource.Subset subset) throws DataStoreException {
        super(source, subset);
        scanlineStride = multiplyFull(getTileSize(0), numInterleaved);
    }

    /**
     * Computes the number of pixels to read in dimension <var>i</var>
     * The arguments given to this method are the ones given to the {@code readSlice(…)} method.
     */
    private static int pixelCount(final long[] lower, final long[] upper, final int[] subsampling, final int i) {
        final int n = toIntExact((upper[i] - lower[i] - 1) / subsampling[i] + 1);
        assert (n > 0) : n;
        return n;
    }

    /**
     * Reads a two-dimensional slice of the data cube from the given input channel.
     *
     * @param  offsets      position in the channel where tile data begins, one value per bank.
     * @param  byteCounts   number of bytes for the compressed tile data, one value per bank.
     * @param  lower        (<var>x</var>, <var>y</var>) coordinates of the first pixel to read relative to the tile.
     * @param  upper        (<var>x</var>, <var>y</var>) coordinates after the last pixel to read relative to the tile.
     * @param  subsampling  (<var>sx</var>, <var>sy</var>) subsampling factors.
     * @param  location     pixel coordinates in the upper-left corner of the tile to return.
     * @return image decoded from the GeoTIFF file.
     */
    @Override
    WritableRaster readSlice(final long[] offsets, final long[] byteCounts, final long[] lower, final long[] upper,
                             final int[] subsampling, final Point location) throws IOException, DataStoreException
    {
        final DataType type   = getDataType();
        final int      width  = pixelCount(lower, upper, subsampling, 0);
        final int      height = pixelCount(lower, upper, subsampling, 1);
        final int      skipY  = subsampling[1] - 1;
        final int      skipX  = numInterleaved * (subsampling[0] - 1);
        final long     head   = numInterleaved * lower[0];
        final long     tail   = numInterleaved * (getTileSize(0) - (width*subsampling[0] + lower[0])) + skipX;
        final Buffer[] banks  = new Buffer[numBanks];
        for (int b=0; b<numBanks; b++) {
            final Buffer   bank = RasterFactory.createBuffer(type, capacity);
            final Inflater algo = Uncompressed.create(reader().input, offsets[b], width, numInterleaved, skipX, bank);
            for (long y = lower[1]; --y >= 0;) {
                algo.skip(scanlineStride);
            }
            for (int y = height; --y > 0;) {        // (height - 1) iterations.
                algo.skip(head);
                algo.uncompressRow();
                algo.skip(tail);
                for (int j=skipY; --j>=0;) {
                    algo.skip(scanlineStride);
                }
            }
            algo.skip(head);                        // Last iteration without last `skip(…)` calls.
            algo.uncompressRow();
            fillRemainingRows(bank.flip());
            banks[b] = bank;
        }
        return WritableRaster.createWritableRaster(model, RasterFactory.wrap(type, banks), location);
    }
}
