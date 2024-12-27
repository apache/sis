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
package org.apache.sis.storage.geotiff.writer;

import java.util.Arrays;
import java.util.Objects;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import org.apache.sis.image.DataType;
import org.apache.sis.io.stream.UpdatableWrite;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.HyperRectangleWriter;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.storage.geotiff.base.Compression;
import org.apache.sis.storage.geotiff.base.Predictor;
import org.apache.sis.storage.geotiff.base.Resources;
import org.apache.sis.pending.jdk.JDK18;


/**
 * Handler for writing offsets and lengths of tiles.
 * Tile size should be multiples of 16 according TIFF specification, but this is not enforced here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TileMatrix {
    /**
     * Where the next image will be written.
     * This information is saved for the caller but not used by this class.
     */
    public UpdatableWrite<?> nextIFD;

    /**
     * The images to write.
     */
    private final RenderedImage image;

    /**
     * The type of sample values.
     */
    private final DataType type;

    /**
     * Number of planes to write. Different then 1 only for planar images.
     */
    private final int numPlanes;

    /**
     * Number of tiles along each axis and in total.
     */
    private final int numXTiles, numYTiles, numTiles;

    /**
     * Size of each tile.
     */
    public final int tileWidth, tileHeight;

    /**
     * Uncompressed size of tiles in number of bytes, as an unsigned integer.
     */
    private final int tileSize;

    /**
     * Compressed size of each tile in number of bytes, as unsigned integers.
     */
    public final int[] lengths;

    /**
     * Offsets to each tiles. Not necessarily in increasing order (it depends on tile order).
     */
    public final long[] offsets;

    /**
     * Tags where are stored offsets and lengths.
     */
    public TagValue offsetsTag, lengthsTag;

    /**
     * The compression to use for writing tiles.
     */
    private final Compression compression;

    /**
     * The compression level, or -1 for default.
     */
    private final int compressionLevel;

    /**
     * The predictor to apply before to compress data.
     */
    private final Predictor predictor;

    /**
     * Creates a new set of information about tiles to write.
     *
     * @param image             the image to write.
     * @param numPlanes         the number of banks (plane in TIFF terminology).
     * @param bitsPerSample     number of bits per sample to write.
     * @param compression       the compression method to apply.
     * @param compressionLevel  compression level (0-9), or -1 for the default.
     * @param predictor         the predictor to apply before to compress data.
     */
    public TileMatrix(final RenderedImage image, final int numPlanes, final int[] bitsPerSample,
                      final Compression compression, final int compressionLevel, final Predictor predictor)
    {
        final int pixelSize, numArrays;
        this.numPlanes        = numPlanes;
        this.image            = image;
        this.compression      = compression;
        this.compressionLevel = compressionLevel;
        this.predictor        = predictor;

        type       = DataType.forBands(image);
        tileWidth  = image.getTileWidth();
        tileHeight = image.getTileHeight();
        pixelSize  = (bitsPerSample != null) ? JDK18.ceilDiv(Arrays.stream(bitsPerSample).sum(), Byte.SIZE) : 1;
        tileSize   = tileWidth * tileHeight * pixelSize;        // Overflow is not really a problem for our usage.
        numXTiles  = image.getNumXTiles();
        numYTiles  = image.getNumYTiles();
        numTiles   = Math.multiplyExact(numXTiles, numYTiles);
        numArrays  = Math.multiplyExact(numPlanes, numTiles);
        offsets    = new long[numArrays];
        lengths    = new int [numArrays];
        Arrays.fill(lengths, tileSize);
    }

    /**
     * {@return whether to use strips instead of tiles}.
     * This is {@code true} if image rows are not separated in tiles.
     * The purpose of using strips is to avoid the restriction that tile size must be multiple of 16 bytes.
     */
    public boolean useStrips() {
        return numXTiles == 1;
    }

    /**
     * Rewrites the offsets and lengths arrays in the IFD.
     * This method shall be invoked after all tiles have been written.
     *
     * @throws IOException if an error occurred while writing to the output stream.
     */
    public void writeOffsetsAndLengths(final ChannelDataOutput output) throws IOException {
        offsetsTag.rewrite(output);
        for (int value : lengths) {
            if (value != tileSize) {
                lengthsTag.rewrite(output);
                break;
            }
        }
    }

    /**
     * Creates an exception for an unsupported configuration.
     */
    private static DataStoreException unsupported(final short key, final Enum<?> value) {
        return new DataStoreException(Resources.forLocale(null).getString(key, value));
    }

    /**
     * Writes the (eventually compressed) sample values of all tiles of the image.
     * Caller shall invoke {@link #writeOffsetsAndLengths(ChannelDataOutput)} after this method.
     * This invocation is not done by this method for allowing the caller to control when to write data.
     *
     * @param  output  where to write the tiles data.
     * @throws RasterFormatException if the raster uses an unsupported sample model.
     * @throws ArithmeticException if an integer overflow occurs.
     * @throws DataStoreException if the compression method is unsupported.
     * @throws IOException if an error occurred while writing to the given output.
     */
    public void writeRasters(final ChannelDataOutput output) throws DataStoreException, IOException {
        ChannelDataOutput compOutput    = null;
        PixelChannel      compressor    = null;
        SampleModel       sampleModel   = null;
        boolean           direct        = false;
        ByteOrder         dataByteOrder = null;
        final ByteOrder   fileByteOrder = output.buffer.order();
        final int minTileX = image.getMinTileX();
        final int minTileY = image.getMinTileY();
        for (int tileIndex = 0; tileIndex < numTiles; tileIndex++) {
            /*
             * In current implementation, we iterate from left to right then top to bottom.
             * But a future version could use Hilbert iterator (for example).
             */
            int tileX = tileIndex % numXTiles;
            int tileY = tileIndex / numXTiles;
            tileX += minTileX;
            tileY += minTileY;
            final Raster tile = image.getTile(tileX, tileY);
            /*
             * Creates the `rect` object which will be used for writing a subset of the raster data.
             * This object depends not only on the sample model, but also on the raster coordinates.
             * Therefore, a new instance needs to be created for each tile.
             */
            final var builder = new HyperRectangleWriter.Builder();
            final HyperRectangleWriter rect = builder.create(tile, tileWidth, tileHeight);
            if (builder.numBanks() != numPlanes) {
                // This exception would be a bug in our analysis of the sample model.
                throw new InternalDataStoreException(tile.getSampleModel().toString());
            }
            /*
             * The compressor depends on properties that change only with the sample model,
             * so `compressor` is usually created only once and shared by all tiles.
             */
            if (!Objects.equals(sampleModel, sampleModel = tile.getSampleModel())) {
                direct = type.equals(DataType.BYTE) && rect.suggestDirect(output);
                if (compressor != null) {
                    compressor.close();
                    compressor = null;
                }
                dataByteOrder = builder.byteOrder(fileByteOrder);
                direct &= (dataByteOrder == null);      // Disable direct mode if a change of byte order is needed.
                /*
                 * Creates the data output to use for writing compressed data. The compressor will need an
                 * intermediate buffer, unless the `direct` flag is true, in which case we will bypass the
                 * buffer and send data directly from the raster to the `compressor` channel. Such direct
                 * mode allows to send large blocks of data without being constrained by the buffer size.
                 * It is possible only for bytes (not integers of floats) and only if the compressor does
                 * not modify the data (i.e. no predictor is used).
                 */
                if (compressionLevel != 0) {
                    final long length = Math.multiplyExact(builder.length(), type.bytes());
                    switch (compression) {
                        case DEFLATE: compressor = new ZIP(output, length, compressionLevel); break;
                        default: throw unsupported(Resources.Keys.UnsupportedCompressionMethod_1, compression);
                    }
                    switch (predictor) {
                        default: throw unsupported(Resources.Keys.UnsupportedPredictor_1, predictor);
                        case NONE: break;
                        case HORIZONTAL_DIFFERENCING: {
                            compressor = HorizontalPredictor.create(compressor, type, builder.pixelStride(), builder.scanlineStride());
                            direct = false;     // Because the predictor will write in the buffer, so it must be a copy of the data.
                            break;
                        }
                    }
                    ByteBuffer buffer = direct ? ByteBuffer.allocate(0) : compressor.createBuffer();
                    compOutput = new ChannelDataOutput(output.filename, compressor, buffer.order(fileByteOrder));
                } else {
                    compOutput = output;
                    assert predictor == Predictor.NONE : predictor;     // Assumption documented in `Compression` class.
                }
            }
            /*
             * Compress and write sample values.
             */
            final DataBuffer buffer = tile.getDataBuffer();
            final int[] bufferOffsets = buffer.getOffsets();
            for (int j=0; j<numPlanes; j++) {
                final int  b        = builder.bankIndex(j);
                final int  offset   = builder.bankOffset(j, bufferOffsets[b]);
                final long position = output.getStreamPosition();
                try {
                    if (dataByteOrder != null) {
                        compOutput.buffer.order(dataByteOrder);
                    }
                    switch (type) {
                        default:     throw new AssertionError(type);
                        case BYTE:   rect.write(compOutput, ((DataBufferByte)   buffer).getData(b), offset, direct); break;
                        case USHORT: rect.write(compOutput, ((DataBufferUShort) buffer).getData(b), offset); break;
                        case SHORT:  rect.write(compOutput, ((DataBufferShort)  buffer).getData(b), offset); break;
                        case UINT:   // Fall through
                        case INT:    rect.write(compOutput, ((DataBufferInt)    buffer).getData(b), offset); break;
                        case FLOAT:  rect.write(compOutput, ((DataBufferFloat)  buffer).getData(b), offset); break;
                        case DOUBLE: rect.write(compOutput, ((DataBufferDouble) buffer).getData(b), offset); break;
                    }
                    if (compressor != null) {
                        compressor.finish(compOutput);
                    }
                } finally {
                    if (dataByteOrder != null) {    // Avoid touching byte order if it wasn't needed.
                        compOutput.buffer.order(fileByteOrder);
                    }
                }
                final int planeIndex = tileIndex + j*numTiles;
                offsets[planeIndex] = position;
                lengths[planeIndex] = Math.toIntExact(Math.subtractExact(output.getStreamPosition(), position));
            }
        }
        if (compressor != null) {
            compressor.close();
        }
    }
}
