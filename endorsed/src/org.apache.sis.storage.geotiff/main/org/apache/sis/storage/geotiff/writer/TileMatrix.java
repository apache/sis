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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferDouble;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import org.apache.sis.image.DataType;
import org.apache.sis.util.internal.Numerics;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.HyperRectangleWriter;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.geotiff.base.Compression;
import org.apache.sis.storage.geotiff.base.Predictor;
import org.apache.sis.storage.geotiff.base.Resources;


/**
 * Handler for writing offsets and lengths of tiles.
 * Tile size should be multiples of 16 according TIFF specification, but this is not enforced here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TileMatrix {
    /**
     * Offset in {@link ChannelDataOutput} where the IFD starts.
     */
    public final long offsetIFD;

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
     * @param offsetIFD         offset in {@link ChannelDataOutput} where the IFD starts.
     * @param compression       the compression method to apply.
     * @param compressionLevel  compression level (0-9), or -1 for the default.
     * @param predictor         the predictor to apply before to compress data.
     */
    public TileMatrix(final RenderedImage image, final int numPlanes, final int[] bitsPerSample,
                      final long offsetIFD, final Compression compression, final int compressionLevel,
                      final Predictor predictor)
    {
        final int pixelSize, numArrays;
        this.offsetIFD        = offsetIFD;
        this.numPlanes        = numPlanes;
        this.image            = image;
        this.compression      = compression;
        this.compressionLevel = compressionLevel;
        this.predictor        = predictor;

        type       = DataType.forBands(image);
        tileWidth  = image.getTileWidth();
        tileHeight = image.getTileHeight();
        pixelSize  = (bitsPerSample != null) ? Numerics.ceilDiv(Arrays.stream(bitsPerSample).sum(), Byte.SIZE) : 1;
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
     * Creates the data output stream to use for writing compressed data.
     *
     * @param  output  where to write compressed bytes.
     * @throws DataStoreException if the compression method is unsupported.
     * @throws IOException if an error occurred while creating the data channel.
     * @return the data output for compressing data, or {@code output} if uncompressed.
     */
    private ChannelDataOutput createCompressionChannel(final ChannelDataOutput output,
            final int pixelStride, final int scanlineStride)
            throws DataStoreException, IOException
    {
        if (compressionLevel == 0) {
            return output;
        }
        PixelChannel channel;
        boolean isDirect = false;           // `true` if using a native library which accepts NIO buffers.
        switch (compression) {
            case NONE:    return output;
            case DEFLATE: channel = new ZIP(output, compressionLevel); isDirect = true; break;
            default: throw unsupported(Resources.Keys.UnsupportedCompressionMethod_1, compression);
        }
        switch (predictor) {
            case NONE: break;
            case HORIZONTAL_DIFFERENCING: {
                channel = HorizontalPredictor.create(channel, type, pixelStride, scanlineStride);
                break;
            }
            default: throw unsupported(Resources.Keys.UnsupportedPredictor_1, predictor);
        }
        final int capacity = CompressionChannel.BUFFER_SIZE;
        ByteBuffer buffer = isDirect ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
        return new ChannelDataOutput(output.filename, channel, buffer.order(output.buffer.order()));
    }

    /**
     * Creates an exception for an unsupported configuration.
     */
    private static DataStoreException unsupported(final short key, final Enum<?> value) {
        return new DataStoreException(Resources.forLocale(null).getString(key, value));
    }

    /**
     * Writes all tiles of the image.
     * Caller shall invoke {@link #writeOffsetsAndLengths(ChannelDataOutput)} after this method.
     * This invocation is not done by this method for allowing the caller to control when to write data.
     *
     * @param  output  where to write the tiles data.
     * @throws DataStoreException if the compression method is unsupported.
     * @throws IOException if an error occurred while writing to the given output.
     */
    public void writeRasters(final ChannelDataOutput output) throws DataStoreException, IOException {
        ChannelDataOutput compress = null;
        PixelChannel      cc       = null;
        SampleModel       sm       = null;
        int[] bankIndices          = null;
        HyperRectangleWriter rect  = null;
        final int minTileX = image.getMinTileX();
        final int minTileY = image.getMinTileY();
        int planeIndex = 0;
        while (planeIndex < offsets.length) {
            /*
             * In current implementation, we iterate from left to right then top to bottom.
             * But a future version could use Hilbert iterator (for example).
             */
            final int tileIndex = planeIndex / numPlanes;
            int tileX = tileIndex % numXTiles;
            int tileY = tileIndex / numXTiles;
            tileX += minTileX;
            tileY += minTileY;
            final Raster tile = image.getTile(tileX, tileY);
            if (sm != (sm = tile.getSampleModel())) {
                rect = null;
                final var builder = new HyperRectangleWriter.Builder().region(new Rectangle(tileWidth, tileHeight));
                if (sm instanceof ComponentSampleModel) {
                    final var csm = (ComponentSampleModel) sm;
                    rect = builder.create(csm);
                    bankIndices = csm.getBankIndices();
                } else if (sm instanceof SinglePixelPackedSampleModel) {
                    final var csm = (SinglePixelPackedSampleModel) sm;
                    rect = builder.create(csm);
                    bankIndices = new int[1];
                } else if (sm instanceof MultiPixelPackedSampleModel) {
                    final var csm = (MultiPixelPackedSampleModel) sm;
                    rect = builder.create(csm);
                    bankIndices = new int[1];
                }
                if (compress == null) {
                    compress = createCompressionChannel(output, builder.pixelStride(), builder.scanlineStride());
                    if (compress != output) cc = (PixelChannel) compress.channel;
                }
            }
            if (rect == null) {
                throw new UnsupportedOperationException();      // TODO: reformat using a recycled Raster.
            }
            final DataBuffer buffer = tile.getDataBuffer();
            final int[] bufferOffsets = buffer.getOffsets();
            for (int j=0; j<numPlanes; j++) {
                final int  b        = bankIndices[j];
                final int  offset   = bufferOffsets[b];
                final long position = output.getStreamPosition();
                switch (type) {
                    default:     throw new AssertionError(type);
                    case BYTE:   rect.write(compress, ((DataBufferByte)   buffer).getData(b), offset); break;
                    case USHORT: rect.write(compress, ((DataBufferUShort) buffer).getData(b), offset); break;
                    case SHORT:  rect.write(compress, ((DataBufferShort)  buffer).getData(b), offset); break;
                    case INT:    rect.write(compress, ((DataBufferInt)    buffer).getData(b), offset); break;
                    case FLOAT:  rect.write(compress, ((DataBufferFloat)  buffer).getData(b), offset); break;
                    case DOUBLE: rect.write(compress, ((DataBufferDouble) buffer).getData(b), offset); break;
                }
                if (cc != null) {
                    cc.finish(compress);
                }
                offsets[planeIndex] = position;
                lengths[planeIndex] = Math.toIntExact(Math.subtractExact(output.getStreamPosition(), position));
                planeIndex++;
            }
        }
        if (cc != null) cc.close();
        if (planeIndex != offsets.length) {
            throw new AssertionError();                 // Should never happen.
        }
    }
}
