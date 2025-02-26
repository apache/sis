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
import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import org.apache.sis.image.DataType;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.io.stream.HyperRectangleReader;
import org.apache.sis.io.stream.Region;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.ByteReader;


/**
 * A single image ({@code 'unci'} item type) from the HEIF file.
 * An image may be used as a tile in a larger image ({@code 'grid'} item type).
 * The image may be implicitly tiled if {@link #numXTiles} is greater than 1.
 *
 * <h4>Requirement</h4>
 * This class requires that {@link CoverageBuilder#sampleModel()} can build a sample model.
 * In other words, the boxes such as {@code UncompressedFrameConfig} must have been found.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class UncompressedImage extends Image {
    /**
     * The type of sample values in the raster.
     */
    private final DataType dataType;

    /**
     * The sample model of each tile. The model size is the tile size.
     *
     * @see #getSampleModel(int[])
     */
    private final SampleModel sampleModel;

    /**
     * Creates a new tile.
     *
     * @param  builder  helper class for building the grid geometry and sample dimensions.
     * @param  locator  the provider of bytes to read from the <abbr>ISOBMFF</abbr> box.
     * @param  name     a name that identifies this image, for debugging purpose.
     * @throws RasterFormatException if the sample model cannot be created.
     */
    UncompressedImage(final CoverageBuilder builder, final ByteReader locator, final String name) {
        super(builder, locator, name);
        sampleModel = builder.sampleModel();
        dataType    = builder.dataType();     // Shall be after `sampleModel()`.
    }

    /**
     * Reads a single tile. The default implementation assumes an uncompressed tile.
     *
     * @param  store    the data store reading a tile.
     * @param  tileX    0-based column index of the tile to read, starting from image left.
     * @param  tileY    0-based column index of the tile to read, starting from image top.
     * @param  context  contains the target raster or the image reader to use.
     * @return tile filled with the pixel values read by this method.
     */
    @Override
    protected Raster readTile(final GeoHeifStore store, final long tileX, final long tileY,
            final ImageResource.Coverage.ReadContext context) throws IOException, DataStoreException
    {
        final long[] sourceSize = {
            // Note: the following ignores `MultiPixelPackedSampleModel`, but that model does not seem used by HEIF.
            sampleModel.getWidth() * (long) sampleModel.getNumDataElements(),
            sampleModel.getHeight()
        };
        /*
         * In the current implementation, we read the whole tile. If a future implementation allows
         * to read a sub-region of the tile, we would need to make `HyperRectangleReader` accepts a
         * `Buffer` with an arbitrary position in argument.
         */
        final var  region    = new Region(sourceSize, new long[2], sourceSize, new long[] {1, 1});
        final int  dataSize  = dataType.bytes();
        final long tileSize  = multiplyExact(region.length, dataSize);
        final long skipBytes = region.getStartByteOffset(dataSize);
        final var  request   = new ByteReader.FileRegion();
        request.input  = store.ensureOpen();
        request.offset = multiplyExact(addExact(multiplyExact(tileY, numXTiles), tileX), tileSize);
        request.length = tileSize;
        request.skip(skipBytes);
        locator.resolve(request);
        /*
         * Now read all banks and store the values in the image buffer.
         * If there is many banks (`InterleavingMode.COMPONENT`), these
         * banks are assumed consecutive.
         */
        request.input.buffer.order(byteOrder);
        final var hr = new HyperRectangleReader(ImageUtilities.toNumberEnum(dataType), request.input);
        hr.setOrigin(request.offset - skipBytes);
        final WritableRaster raster = context.createRaster();
        final DataBuffer data = raster.getDataBuffer();
        final int numBanks = data.getNumBanks();
        for (int b=0; b<numBanks; b++) {
            if (b != 0) {
                hr.setOrigin(addExact(hr.getOrigin(), tileSize));
            }
            hr.setDestination(RasterFactory.wrapAsBuffer(data, b));
            hr.readAsBuffer(region, 0);
        }
        return raster;
    }
}
