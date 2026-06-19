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
package org.apache.sis.storage.geoheif;

import java.io.IOException;
import java.awt.image.RasterFormatException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.Region;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.ByteRanges;
import org.apache.sis.storage.isobmff.base.ItemLocation;
import org.apache.sis.storage.isobmff.mpeg.CompressedUnitsItemInfo;
import org.apache.sis.storage.isobmff.image.TiledImageConfiguration;


/**
 * A tiled image ({@code 'tili'} item type) from the <abbr>HEIF</abbr> file.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class TiledImage extends UncompressedImage {
    /**
     * Positions relative to the beginning of the box where to find the tile data.
     */
    private final long[] tileOffsets;

    /**
     * Number of bytes of the compressed data of each tile, or {@code null} if not stored.
     * The length of this array shall be equal to the {@link #tileOffsets} array length.
     * If {@code null}, then the lengths must be computed from the offsets.
     */
    private final long[] tileSizes;

    /**
     * Creates a new tile.
     *
     * @param  builder  helper class for building the grid geometry and sample dimensions.
     * @param  locator  the provider of bytes to read from the <abbr>ISOBMFF</abbr> box.
     * @param  name     a name that identifies this image, for debugging purpose.
     * @throws RasterFormatException if the sample model cannot be created.
     * @throws IOException if an error occurred while reading bytes from the input stream.
     */
    TiledImage(final CoverageBuilder builder, final ByteRanges.Reader locator, final String name)
            throws DataStoreException, IOException
    {
        super(builder, locator, name);
        final int numTiles = Math.multiplyExact(numXTiles, numYTiles);
        final TiledImageConfiguration tiling = builder.tiling();
        final int offsetFieldLength = tiling.offsetFieldLength();
        final int sizeFieldLength   = tiling.sizeFieldLength();
        final ChannelDataInput input = builder.store().ensureOpen();
        input.seek(((ItemLocation.Item) locator).baseOffset);
        tileOffsets = new long[numTiles];
        tileSizes = (sizeFieldLength != 0) ? new long[numTiles] : null;
        for (int i = 0; i < numTiles; i++) {
            tileOffsets[i] = input.readBits(offsetFieldLength);
            if (tileSizes != null) {
                tileSizes[i] = input.readBits(sizeFieldLength);
            }
        }
    }

    /**
     * Computes the range of bytes that will be needed for reading the tile at the specified index.
     * The given region is converted to offsets relatives to the beginning of the <abbr>HEIF</abbr>
     * file and the result is stored in the given {@code addTo} argument.
     *
     * @param  tileIndex  index of the tile for which to compute the range of bytes.
     * @param  region     ignored (replaced by the tile size which is stored or computed from the offset).
     * @param  addTo      where to store the offsets relatives to the beginning of the file.
     * @throws DataStoreException if an error occurred with the data in the boxes.
     * @throws ArithmeticException if an integer overflow occurred.
     */
    @Override
    protected void computeByteRanges(final long tileIndex, Region region, final ByteRanges addTo)
            throws DataStoreException
    {
        int i = Math.toIntExact(tileIndex);
        final long offset = tileOffsets[i];
        final long tileSize;
        if (tileSizes != null) {
            tileSize = tileSizes[i];
        } else if (i < tileOffsets.length - 1) {
            tileSize = tileOffsets[i+1] - offset;
        } else {
            tileSize = -1;      // Means to read all remaining bytes in the box.
        }
        locator.resolve(offset, tileSize, addTo);
    }

    /**
     * Returns the compression units which contains tile data.
     * The {@code Unit.offset} value is relative to the offset
     * computed by {@link #computeByteRanges(long, long, ByteRanges)}.
     *
     * @param  tileIndex  index of the tile for which to get the compression unit.
     * @return the compression unit for the tile at the given index.
     */
    @Override
    protected CompressedUnitsItemInfo.Unit compressedImageUnit(final long tileIndex) {
        // Set the offset to 0 because it has already been added by `computeByteRanges(…)`
        return new CompressedUnitsItemInfo.Unit(0, tileSizes[Math.toIntExact(tileIndex)]);
    }
}
