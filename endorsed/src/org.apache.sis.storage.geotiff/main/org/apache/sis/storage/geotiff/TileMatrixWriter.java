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

import java.util.Arrays;
import java.io.IOException;
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


/**
 * Handler for writing offsets and lengths of tiles.
 * Tile size should be multiples of 16 according TIFF specification, but this is not enforced here.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TileMatrixWriter {
    /**
     * Offset in {@link ChannelDataOutput} where the IFD starts.
     */
    final long offsetIFD;

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
    final int tileWidth, tileHeight;

    /**
     * Uncompressed size of tiles in number of bytes, as an unsigned integer.
     */
    private final int tileSize;

    /**
     * Compressed size of each tile in number of bytes, as unsigned integers.
     */
    final int[] lengths;

    /**
     * Offsets to each tiles. Not necessarily in increasing order (it depends on tile order).
     */
    final long[] offsets;

    /**
     * Tags where are stored offsets and lengths.
     */
    TagValueWriter offsetsTag, lengthsTag;

    /**
     * Creates a new set of information about tiles to write.
     *
     * @param image          the image to write.
     * @param numBands       the number of bands.
     * @param bitsPerSample  number of bits per sample to write.
     * @param isPlanar       whether the planar configuration is to store bands in separated planes.
     * @param offsetIFD      offset in {@link ChannelDataOutput} where the IFD starts.
     */
    TileMatrixWriter(final RenderedImage image, final int numPlanes, final int[] bitsPerSample,
                     final long offsetIFD)
    {
        final int pixelSize, numArrays;
        this.offsetIFD = offsetIFD;
        this.numPlanes = numPlanes;
        this.image     = image;
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
    final void writeOffsetsAndLengths(final ChannelDataOutput output) throws IOException {
        offsetsTag.rewrite(output);
        for (int value : lengths) {
            if (value != tileSize) {
                lengthsTag.rewrite(output);
                break;
            }
        }
    }

    /**
     * Writes all tiles of the image.
     * Caller shall invoke {@link #writeOffsetsAndLengths(ChannelDataOutput)} after this method.
     * This invocation is not done by this method for allowing the caller to control when to write data.
     *
     * @param  output  where to write the tiles data.
     * @throws IOException if an error occurred while writing to the given output.
     */
    final void writeRasters(final ChannelDataOutput output) throws IOException {
        SampleModel sm = null;
        int[] bankIndices = null;
        HyperRectangleWriter rect = null;
        final int minTileX = image.getMinTileX();
        final int minTileY = image.getMinTileY();
        int planeIndex = 0;
        for (int i=0; i < offsets.length; i++) {
            /*
             * In current implementation, we iterate from left to right then top to bottom.
             * But a future version could use Hilbert iterator (for example).
             */
            int tileX = i % numXTiles;
            int tileY = i / numXTiles;
            tileX += minTileX;
            tileY += minTileY;
            final Raster tile = image.getTile(tileX, tileY);
            if (sm != (sm = tile.getSampleModel())) {
                rect = null;
                final var region = new Rectangle(tileWidth, tileHeight);
                if (sm instanceof ComponentSampleModel) {
                    final var csm = (ComponentSampleModel) sm;
                    rect = HyperRectangleWriter.of(csm, region);
                    bankIndices = csm.getBankIndices();
                } else if (sm instanceof SinglePixelPackedSampleModel) {
                    final var csm = (SinglePixelPackedSampleModel) sm;
                    rect = HyperRectangleWriter.of(csm, region);
                    bankIndices = new int[1];
                } else if (sm instanceof MultiPixelPackedSampleModel) {
                    final var csm = (MultiPixelPackedSampleModel) sm;
                    rect = HyperRectangleWriter.of(csm, region);
                    bankIndices = new int[1];
                }
            }
            if (rect == null) {
                throw new UnsupportedOperationException();      // TODO: reformat using a recycled Raster.
            }
            final long position = output.getStreamPosition();
            final DataBuffer buffer = tile.getDataBuffer();
            final int[] bufferOffsets = buffer.getOffsets();
            for (int j=0; j < numPlanes; j++) {
                final int b = bankIndices[j];
                final int offset = bufferOffsets[b];
                switch (type) {
                    case BYTE:   rect.write(output, ((DataBufferByte)   buffer).getData(b), offset); break;
                    case USHORT: rect.write(output, ((DataBufferUShort) buffer).getData(b), offset); break;
                    case SHORT:  rect.write(output, ((DataBufferShort)  buffer).getData(b), offset); break;
                    case INT:    rect.write(output, ((DataBufferInt)    buffer).getData(b), offset); break;
                    case FLOAT:  rect.write(output, ((DataBufferFloat)  buffer).getData(b), offset); break;
                    case DOUBLE: rect.write(output, ((DataBufferDouble) buffer).getData(b), offset); break;
                }
            }
            offsets[planeIndex] = position;
            lengths[planeIndex] = Math.toIntExact(Math.subtractExact(output.getStreamPosition(), position));
            planeIndex++;
        }
        if (planeIndex != offsets.length) {
            throw new AssertionError();                 // Should never happen.
        }
    }
}
