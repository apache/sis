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

import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import org.apache.sis.image.DataType;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.HyperRectangleReader;
import org.apache.sis.io.stream.Region;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.ByteRanges;


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
    UncompressedImage(final CoverageBuilder builder, final ByteRanges.Reader locator, final String name)
            throws DataStoreException
    {
        super(builder, locator, name);
        sampleModel = builder.sampleModel();
        dataType    = builder.dataType();     // Shall be after `sampleModel()`.
    }

    /**
     * Computes the range of bytes that will be needed for reading a single tile of this image.
     *
     * @param  context  where to store the ranges of bytes.
     * @throws DataStoreException if an error occurred while computing the range of bytes.
     */
    @Override
    protected Reader computeByteRanges(final ImageResource.Coverage.ReadContext context) throws DataStoreException {
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
        final long tileIndex = addExact(multiplyExact(context.subTileY, numXTiles), context.subTileX);
        final long offset    = addExact(multiplyExact(tileIndex, tileSize), skipBytes);
        locator.resolve(offset, tileSize - skipBytes, context);
        return (final ChannelDataInput input) -> {
            /*
             * Now read all banks and store the values in the image buffer.
             * If there is many banks (`InterleavingMode.COMPONENT`), these
             * banks are assumed consecutive.
             */
            input.buffer.order(byteOrder);
            final var hr = new HyperRectangleReader(ImageUtilities.toNumberEnum(dataType), input);
            hr.setOrigin(context.offset() - skipBytes);
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
        };
    }
}
