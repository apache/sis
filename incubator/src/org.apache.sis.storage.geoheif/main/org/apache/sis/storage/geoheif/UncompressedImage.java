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

import java.util.Arrays;
import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.image.internal.shared.RasterFactory;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.HyperRectangleReader;
import org.apache.sis.io.stream.Region;
import org.apache.sis.io.stream.inflater.ComputedByteChannel;
import org.apache.sis.io.stream.inflater.Deflate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.isobmff.ByteRanges;
import org.apache.sis.storage.isobmff.mpeg.CompressedUnitsItemInfo;


/**
 * A single image ({@code 'unci'} item type) from the <abbr>HEIF</abbr> file.
 * An image may be used as a tile in a larger image ({@code 'grid'} item type).
 * The image may be implicitly tiled if {@link #numXTiles} is greater than 1.
 *
 * <p>This class is named "uncompressed" image because the <abbr>HEIF</abbr> format uses that name.
 * Nevertheless, the data may be composed of compressed units. A compressed unit may be the whole
 * image or only part of it (tile, row or pixel).</p>
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
     * The compression units which contains all image data, or {@code null} if the image is uncompressed.
     */
    private final CompressedUnitsItemInfo.Unit compressedImageUnit;

    /**
     * Creates a new tile.
     *
     * @param  builder  helper class for building the grid geometry and sample dimensions.
     * @param  locator  the provider of bytes to read from the <abbr>ISOBMFF</abbr> box.
     * @param  name     a name that identifies this image, for debugging purpose.
     * @throws DataStoreContentException if the "grid to <abbr>CRS</abbr>" transform or the sample dimensions cannot be created.
     */
    UncompressedImage(final CoverageBuilder builder, final ByteRanges.Reader locator, final String name)
            throws DataStoreException
    {
        super(builder, locator, name);
        sampleModel = builder.sampleModel();
        dataType    = builder.imageModel().dataType;
        compressedImageUnit = builder.getCompressedImageUnit();
    }

    /**
     * Returns (width × number of samples per pixel) and the height, in that order.
     *
     * <p><b>Limitation:</b> current implementation ignores {@link java.awt.image.MultiPixelPackedSampleModel},
     * but that model does not seem to be used by <abbr>HEIF</abbr>.</p>
     *
     * @param  sampleModel  the sample model for which to get the size.
     * @return the scanline stride and raster height, in that order.
     */
    private static long[] size(final SampleModel sampleModel) {
        return new long[] {
            sampleModel.getWidth() * (long) sampleModel.getNumDataElements(),
            sampleModel.getHeight()
        };
    }

    /**
     * Creates a two-dimensional region without subsampling.
     *
     * @param  sourceSize   the number of elements along each dimension.
     * @param  regionUpper  indices after the last value to read along each dimension.
     */
    private static Region region(final long[] sourceSize, final long[] regionUpper) {
        return new Region(sourceSize, new long[2], regionUpper, new long[] {1, 1});
    }

    /**
     * Computes the range of bytes that will be needed for reading a single tile of this image.
     *
     * @param  context  where to store the ranges of bytes.
     * @return the function to invoke later for reading the tile.
     * @throws DataStoreException if an error occurred while computing the range of bytes.
     */
    @Override
    protected Reader computeByteRanges(final ImageResource.Coverage.ReadContext context) throws DataStoreException {
        final long[] sourceSize = size(sampleModel);
        /*
         * In the current implementation, we read the whole tile. If a future implementation allows
         * to read a sub-region of the tile, we would need to make `HyperRectangleReader` accepts a
         * `Buffer` with an arbitrary position in argument.
         */
        final var  region    = region(sourceSize, sourceSize);
        final int  dataSize  = dataType.bytes();
        final long tileSize  = multiplyExact(region.length, dataSize);
        final long tileIndex = addExact(multiplyExact(context.subTileY, numXTiles), context.subTileX);
        final long offset    = multiplyExact(tileIndex, tileSize);
        locator.resolve(offset, tileSize, context);
        return (ChannelDataInput input) -> {
            long origin = context.offset();
            final ComputedByteChannel inflater;
            final CompressedUnitsItemInfo.Unit unit = compressedImageUnit;
            if (unit != null) {
                inflater = new Deflate(input, listeners);
                inflater.setInputRegion(addExact(origin, unit.offset), unit.size);
                input = inflater.createDataInput(context.reuseBuffer(), (int) sourceSize[0]);    // (int) cast okay even if inexact.
                origin = 0;
            } else {
                inflater = null;
            }
            /*
             * Now read all banks and store the values in the image buffer.
             * If there is many banks (`InterleavingMode.COMPONENT`), these
             * banks are assumed consecutive.
             */
            input.buffer.order(byteOrder);
            final var hr = new HyperRectangleReader(ImageUtilities.toNumberEnum(dataType), input);
            hr.setOrigin(origin);
            final WritableRaster raster       = context.createRaster();
            final long[]         rasterSize   = size(raster.getSampleModel());
            final Region         rasterRegion = Arrays.equals(sourceSize, rasterSize) ? region : region(sourceSize, rasterSize);
            final DataBuffer     data         = raster.getDataBuffer();
            final int            numBanks     = data.getNumBanks();
            for (int b=0; b<numBanks; b++) {
                if (b != 0) {
                    hr.setOrigin(addExact(hr.getOrigin(), tileSize));
                }
                hr.setDestination(RasterFactory.wrapAsBuffer(data, b));
                hr.readAsBuffer(rasterRegion, 0);
            }
            if (inflater != null) {
                context.saveForReuse(inflater.compressedInput().buffer);
            }
            return raster;
        };
    }
}
