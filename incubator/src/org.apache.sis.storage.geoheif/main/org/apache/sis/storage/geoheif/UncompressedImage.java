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
import java.awt.image.BandedSampleModel;
import org.apache.sis.image.DataType;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.image.internal.shared.RasterFactory;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.HyperRectangleReader;
import org.apache.sis.io.stream.Region;
import org.apache.sis.io.stream.inflater.ComputedByteChannel;
import org.apache.sis.io.stream.inflater.Deflate;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.UnsupportedEncodingException;
import org.apache.sis.storage.isobmff.ByteRanges;
import org.apache.sis.storage.isobmff.mpeg.CompressedUnitsItemInfo;
import org.apache.sis.storage.isobmff.mpeg.CompressionConfiguration;


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
class UncompressedImage extends Image {
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
     * The type of compression, or 0 if the image is uncompressed.
     * Should be one of the {@code CompressionConfiguration.COMPRESSION_*} constants.
     */
    private final int compressionType;

    /**
     * The compression units which contains all image data, or {@code null} if none.
     * A compressed image may have no compression unit if the location of data are
     * specified by another type of box.
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
        sampleModel         = builder.sampleModel();
        dataType            = builder.imageModel().dataType;
        compressionType     = builder.compressionType();
        compressedImageUnit = builder.compressedImageUnit();
    }

    /**
     * Returns a channel which will decompress data on-the-fly.
     * Callers shall invoke {@link ComputedByteChannel#setInputRegion(long, long)} on the returned object.
     *
     * @param  input  the channel of compressed data.
     * @return the channel of uncompressed data, or {@code null} if the data are uncompressed.
     * @throws UnsupportedEncodingException if the compression type is not supported.
     */
    private ComputedByteChannel inflater(final ChannelDataInput input) throws UnsupportedEncodingException {
        switch (compressionType) {
            case 0: return null;
            case CompressionConfiguration.COMPRESSION_ZLIB:    return new Deflate(input, listeners, false);
            case CompressionConfiguration.COMPRESSION_DEFLATE: return new Deflate(input, listeners, true);
            default: throw new UnsupportedEncodingException("The \"" +
                    CompressionConfiguration.formatFourCC(compressionType) + "\" compression is not supported.");

        }
    }

    /**
     * Returns [(width × number of samples per pixel), (height), (number of banks)], in that order.
     * The banks are assumed to be consecutive.
     *
     * <p><b>Limitation:</b> current implementation ignores {@link java.awt.image.MultiPixelPackedSampleModel},
     * but that model does not seem to be used by <abbr>HEIF</abbr>.</p>
     *
     * @param  sampleModel  the sample model for which to get the size.
     * @return the scanline stride, raster height and number of banks, in that order.
     */
    private static long[] size(final SampleModel sampleModel) {
        long width    = sampleModel.getWidth();
        long numBanks = sampleModel.getNumDataElements();
        if (!(sampleModel instanceof BandedSampleModel)) {
            width *= numBanks;
            numBanks = 1;
        }
        return new long[] {width, sampleModel.getHeight(), numBanks};
    }

    /**
     * Computes the range of bytes that will be needed for reading the tile at the specified index.
     * The given region is converted to offsets relatives to the beginning of the <abbr>HEIF</abbr>
     * file and the result is stored in the given {@code addTo} argument.
     *
     * @param  tileIndex  index of the tile for which to compute the range of bytes.
     * @param  region     relative indexes of the uncompressed data to read in each tile.
     * @param  addTo      where to store the offsets relatives to the beginning of the file.
     * @throws DataStoreException if an error occurred with the data in the boxes.
     * @throws ArithmeticException if an integer overflow occurred.
     */
    protected void computeByteRanges(final long tileIndex, final Region region, final ByteRanges addTo)
            throws DataStoreException
    {
        final long tileSize = multiplyExact(region.length, dataType.bytes());
        locator.resolve(multiplyExact(tileIndex, tileSize), tileSize, addTo);
    }

    /**
     * Returns the compression units which contains tile data.
     * The {@code Unit.offset} value shall be relative to the offset
     * computed by {@link #computeByteRanges(long, long, ByteRanges)}.
     *
     * @param  tileIndex  index of the tile for which to get the compression unit.
     * @return the compression unit for the tile at the given index.
     * @throws DataStoreException if the compression unit cannot be obtained.
     */
    protected CompressedUnitsItemInfo.Unit compressedImageUnit(final long tileIndex) throws DataStoreException {
        if (compressedImageUnit == null) {
            throw new DataStoreContentException("Missing compressed unit.");
        }
        return compressedImageUnit;
    }

    /**
     * Computes the range of bytes that will be needed for reading a single tile of this image.
     * This method is invoked in a code synchronized on {@link ImageResource#getSynchronizationLock()}.
     *
     * @param  context  where to store the ranges of bytes.
     * @return the function to invoke later for reading the tile.
     * @throws DataStoreException if an error occurred while computing the range of bytes.
     * @throws ArithmeticException if an offset or index overflows the capacity of 32-bits integers.
     */
    @Override
    protected final Reader computeByteRanges(final ImageResource.Coverage.ReadContext context) throws DataStoreException {
        final long[] sourceSize = size(sampleModel);
        final var    region     = new Region(sourceSize, null, null, null);
        final long   tileIndex  = addExact(multiplyExact(context.subTileY, numXTiles), context.subTileX);
        computeByteRanges(tileIndex, region, context);
        return (ChannelDataInput input) -> {
            long origin = context.offset();
            ComputedByteChannel inflater = context.reuseInflater();
            if (inflater == null || inflater.compressedInput() != input) {
                inflater = inflater(input);
            }
            if (inflater != null) {
                final CompressedUnitsItemInfo.Unit unit = compressedImageUnit(tileIndex);
                inflater.setInputRegion(addExact(origin, unit.offset), unit.size);
                // The following (int) cast is okay even if inexact because it is only a hint.
                input = inflater.createDataInput((int) sourceSize[0]);
                origin = 0;
            }
            /*
             * Now read all banks and store the values in the image buffer.
             * If there is many banks (`InterleavingMode.COMPONENT`), these
             * banks are assumed consecutive.
             */
            input.buffer.order(byteOrder);
            final var hr = new HyperRectangleReader(ImageUtilities.toNumberEnum(dataType), input);
            hr.setOrigin(origin);
            Region bank = region;
            final WritableRaster raster   = context.createRaster();
            final long[]         upper    = size(raster.getSampleModel());
            final DataBuffer     target   = raster.getDataBuffer();
            final int            numBanks = target.getNumBanks();
            for (int b=0; b<numBanks; b++) {
                upper[2] = b + 1;
                if (b != 0 || !Arrays.equals(sourceSize, upper)) {
                    final long[] lower = new long[upper.length];
                    lower[2] = b;
                    bank = new Region(sourceSize, lower, upper, null);
                }
                hr.setDestination(RasterFactory.wrapAsBuffer(target, b));
                hr.readAsBuffer(bank, 0);
            }
            if (inflater != null) {
                context.saveForReuse(inflater);
            }
            return raster;
        };
    }
}
