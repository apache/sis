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

import java.nio.ByteOrder;
import java.io.IOException;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import javax.imageio.ImageTypeSpecifier;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.isobmff.ByteRanges;


/**
 * A single image ({@code 'unci'} item type) from the HEIF file.
 * An image may be used as a tile in a larger image ({@code 'grid'} item type).
 * In the uncompressed case, the image may be implicitly tiled if {@link #numXTiles} is greater than 1.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
abstract class Image {
    /**
     * A name that identifies this image, for debugging purposes.
     */
    private final String name;

    /**
     * Number of columns and rows in the tile matrix.
     * This is usually not supported for compressed tiles.
     */
    protected final int numXTiles, numYTiles;

    /**
     * The provider of bytes to read from the <abbr>ISOBMFF</abbr> box.
     * The bytes are read from the {@link ChannelDataInput} at a position specified by the box.
     */
    protected final ByteRanges.Reader locator;

    /**
     * The byte order to use for reading the sample values of the image.
     */
    protected final ByteOrder byteOrder;

    /**
     * Creates a new tile.
     *
     * @param  builder  helper class for building the grid geometry and sample dimensions.
     * @param  locator  the provider of bytes to read from the <abbr>ISOBMFF</abbr> box.
     * @param  name     a name that identifies this image, for debugging purpose.
     * @throws RasterFormatException if the sample model cannot be created.
     */
    protected Image(final CoverageBuilder builder, final ByteRanges.Reader locator, final String name) {
        this.locator = locator;
        this.name    = name;
        byteOrder    = builder.byteOrder();
        numXTiles    = builder.numTiles(0);
        numYTiles    = builder.numTiles(1);
        // Do NOT invoke `builder.sampleModel()`, because that information is not available for all types.
    }

    /**
     * A supplier of image, used for deferred computation.
     */
    @FunctionalInterface
    interface Supplier {
        Image get() throws DataStoreException;
    }

    /**
     * Returns the sample model and color model of this image.
     * For uncompressed image, this information is not available and this method always throws an exception.
     * For some other profiles such as JPEG, the sample and color models are encoded in this image payload.
     * The size of the sample model shall be the tile size.
     *
     * @param  store   the store that opened the <abbr>HEIF</abbr> file.
     * @throws DataStoreContentException if this image does not include information about the sample/color models.
     * @throws DataStoreException if another problem occurred with the content of the <abbr>HEIF</abbr> file.
     * @throws IOException if an I/O error occurred.
     */
    protected ImageTypeSpecifier getImageType(final GeoHeifStore store) throws DataStoreException, IOException {
        throw new DataStoreContentException("Cannot determine the sample model.");
    }

    /**
     * Computes the range of bytes that will be needed for reading a single tile of this image.
     * There will be usually a single range of bytes per tile, but this method allows the bytes
     * to be spread over more than one extent.
     *
     * <p>This method does not read the bytes immediately.
     * Instead, it returns a function which will be executed later for finishing the read operation.</p>
     *
     * @param  context  where to store the ranges of bytes.
     * @return the function to invoke later for reading the tile.
     * @throws DataStoreException if an error occurred while computing the range of bytes.
     */
    protected abstract Reader computeByteRanges(ImageResource.Coverage.ReadContext context) throws DataStoreException;

    /**
     * Reads a single tile from a sequence of bytes.
     * Instances are prepared and returned by {@link #computeByteRanges computeByteRanges(…)}.
     */
    @FunctionalInterface
    protected interface Reader {
        /**
         * Reads a single tile from a sequence of bytes in the given input.
         * The implementation is responsible for setting the stream position before to start reading bytes.
         * The given {@code input} will view all bytes after the initial position as if they were stored in
         * one single large extent.
         * Implementations should ignore the fact that the sequence of bytes may be spread in many extents.
         *
         * @param  input  a view of the byte sequences as if they were stored in one single large extent.
         * @return tile filled with the pixel values read by this method.
         */
        Raster readTile(ChannelDataInput input) throws Exception;
    }

    /**
     * Returns the name of this image, for debugging purposes.
     *
     * @return the name of this image.
     */
    @Override
    public String toString() {
        return name;
    }
}
