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
package org.apache.sis.io.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.RasterFormatException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.pending.jdk.JDK18;


/**
 * Helper methods for writing a rectangular area, a cube or a hyper-cube in a channel.
 * A rectangular area is usually a tile, and consequently should be relatively small.
 * The same instance can be reused when the shape of source arrays and the shape of
 * the region to write do not change, which is typically the case when writing tiles.
 * This class is thread-safe if writing in different output channels.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class HyperRectangleWriter {
    /**
     * Index of the first value to use in the array given to write methods.
     */
    private final int startAt;

    /**
     * Number of elements that can be written in a single I/O operation.
     */
    final int contiguousDataLength;

    /**
     * Number of elements to write in each dimension after the contiguous dimensions, in reverse order.
     * For an image, it may be an array of length 2 with the height and width of the destination region,
     * in that order. However it may be an array of length 1 with only the height, or an empty array
     * if some chunks of data can be written in a single I/O operation.
     *
     * <p>Values in this array are decremented by 1.</p>
     */
    private final int[] remaining;

    /**
     * Value by which to increment the flat array index for moving to the next value.
     * The array length and the element order is the same as in {@link #remaining}.
     */
    private final int[] strides;

    /**
     * Creates a new writer for data of a shape specified by the given region.
     * The region also specifies the subset to write.
     *
     * @param  output  where to write data.
     * @param  region  size of the source hyper-rectangle and region to write.
     * @throws ArithmeticException if the region is too large.
     */
    public HyperRectangleWriter(final Region region) {
        startAt              = Math.toIntExact(region.startAt);
        int cdd              = region.contiguousDataDimension();
        contiguousDataLength = region.targetLength(cdd);
        final int d          = region.getDimension() - cdd;
        remaining            = new int[d];
        strides              = new int[d];
        for (int i=d; --i>=0; cdd++) {
            if ((remaining[i] = region.getTargetSize(cdd) - 1) < 0 ||
                  (strides[i] = Math.toIntExact(region.getSkip(cdd) + contiguousDataLength)) == 0)
            {
                /*
                 * Should have been verified as of Region constructor contract.
                 * Check again as a safety against never-ending loops.
                 */
                throw new AssertionError(region);
            }
        }
    }

    /**
     * A builder for {@code HyperRectangleWriter} created from a {@code Raster} or a {@code SampleModel}.
     * Each builder shall be used only once. For creating more {@code HyperRectangleWriter} instances,
     * new builders shall be created.
     *
     * <p>All getter methods contain information that are valid
     * only after a {@code create(…)} method has been invoked.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    public static final class Builder {
        /**
         * Number of elements (not necessarily bytes) contained in this hyper-rectangle.
         * The number of bytes to write will be this length multiplied by element size.
         *
         * @see #length()
         */
        private long length;

        /**
         * The number of bits in a pixel, or 0 if the sample model is not multi-pixel packed.
         */
        private int pixelBitStride;

        /**
         * Number of elements (not necessarily bytes) between a pixel and the next pixel.
         *
         * @see #pixelStride()
         */
        private int pixelStride;

        /**
         * Number of elements (not necessarily bytes) between a row and the next row.
         *
         * @see #scanlineStride()
         */
        private int scanlineStride;

        /**
         * The indices of all banks to write with {@code HyperRectangleWriter}.
         * This is not necessarily the bank indices of all bands, because the writer may be
         * able to write all bands contiguously in a single call to a {@code write(…)} method.
         * A length greater than one means that the {@link HyperRectangleWriter} instance
         * created by this builder will need to be invoked repetitively for each bank.
         *
         * @see #bankIndex(int)
         */
        private int[] bankIndices;

        /**
         * The offset to add to each bank to write with {@code HyperRectangleWriter}.
         * This is in addition of offsets declared in {@link DataBuffer#getOffsets()}.
         *
         * @see #bankOffset(int, int)
         */
        private int[] bankOffsets;

        /**
         * Whether writing data requires the use of {@link ByteOrder#BIG_ENDIAN} in the destination buffer.
         * This is sometime needed when {@code short} or {@code int} values are used for packing many bytes.
         * This flag can be ignored if, for example, ARGB values are really stored as integers in native
         * byte order rather than 4 bytes. If this flag is {@code false}, then byte order does not matter
         * (i.e., {@code false} does mean that {@link ByteOrder#LITTLE_ENDIAN} is required).
         *
         * @see #byteOrder(ByteOrder)
         */
        private boolean requiresBigEndian;

        /**
         * Subregion to write, or {@code null} for writing the whole raster.
         * This field serves two purposes:
         *
         * <h4>Before a call to a {@code create(…)}</h4>
         * The rectangle is in the coordinate system of the object specified to the {@code create(…)} method:
         * {@link Raster} coordinates if the {@link #create(Raster)} method is invoked, or
         * {@link SampleModel} coordinates if the {@link #create(SampleModel)} method is invoked.
         *
         * <h4>After a call to a {@code create(…)}</h4>
         * If the field was null, it is set to a non-null value containing {@link Raster} or {@link SampleModel}
         * coordinates as described above. If the field was already non-null, the previous instance is unmodified.
         */
        private Rectangle region;

        /**
         * The translation from the coordinate system of the {@link SampleModel} to that of the {@link Raster}.
         * To convert a pixel's coordinate from the {@link Raster} coordinate system to the {@link SampleModel}
         * coordinate system, this value must be subtracted.
         *
         * @see Raster#getSampleModelTranslateX()
         * @see Raster#getSampleModelTranslateY()
         */
        private int sampleModelTranslateX, sampleModelTranslateY;

        /**
         * Creates a new builder.
         */
        public Builder() {
        }

        /**
         * Returns whether the given sample model is supported. If this method returns {@code true},
         * then invoking a {@code create(…)} method should not throw {@link RasterFormatException}.
         *
         * @param  sm  the sample model of the rasters to write, or {@code null}.
         * @return whether the given sample model is non-null and supported.
         *
         * @see #create(SampleModel)
         */
        public static boolean isSupported(final SampleModel sm) {
            if (sm instanceof ComponentSampleModel)         return true;
            if (sm instanceof SinglePixelPackedSampleModel) return isSupported(null, (SinglePixelPackedSampleModel) sm);
            if (sm instanceof MultiPixelPackedSampleModel)  return isSupported(null, (MultiPixelPackedSampleModel)  sm);
            return false;
        }

        /**
         * Creates a new writer for raster data described by the given sample model and strides.
         * The {@link #pixelStride} and {@link #scanlineStride} fields must be set before this method is invoked.
         *
         * @param  sm           the sample model of the rasters to write.
         * @param  bandOffsets  bands to read, or {@code null} for all of them in same order.
         * @return writer for rasters using the specified sample model (never {@code null}).
         */
        private HyperRectangleWriter create(final SampleModel sm, final int[] bandOffsets) {
            final int width, height;
            ArgumentChecks.ensureStrictlyPositive("width",  width  = sm.getWidth());
            ArgumentChecks.ensureStrictlyPositive("height", height = sm.getHeight());
            ArgumentChecks.ensureStrictlyPositive("scanlineStride", scanlineStride);    // May be less than `width`.
            ArgumentChecks.ensureBetween("pixelStride", 1, scanlineStride, pixelStride);
            final long[] sourceSize  = {
                scanlineStride,
                height
            };
            if (region == null) {
                region = new Rectangle(width, height);
            }
            final long[] regionLower = {
                region.x - (long) sampleModelTranslateX,
                region.y - (long) sampleModelTranslateY
            };
            final long[] regionUpper = {
                regionLower[0] + region.width,
                regionLower[1] + region.height
            };
            regionLower[0] *= pixelStride;      // Should not overflow for reasonable values of pixel stride.
            regionUpper[0] *= pixelStride;
            if (pixelBitStride != 0) {
                final int dataSize = DataBuffer.getDataTypeSize(sm.getDataType());
                ArgumentChecks.ensureBetween("pixelBitStride", 1, dataSize, pixelBitStride);
                regionLower[0] = Math.floorDiv(regionLower[0] * pixelBitStride, dataSize);
                regionUpper[0] = JDK18.ceilDiv(regionUpper[0] * pixelBitStride, dataSize);
            }
            final var subset = new Region(sourceSize, regionLower, regionUpper, new long[] {1,1});
            length = subset.length;
            if (bandOffsets != null) {
                final int numBands = bandOffsets.length;
                if (numBands != pixelStride || !ArraysExt.isRange(0, bandOffsets)) {
                    if (numBands != 1) {
                        ArgumentChecks.ensureBetween("pixelStride", numBands, scanlineStride, pixelStride);
                    }
                    return new SubsampledRectangleWriter(subset, bandOffsets, pixelStride);
                }
            }
            return new HyperRectangleWriter(subset);
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * The returned writer will need to be applied repetitively for each bank
         * if {@link #numBanks()} returns a value greater than one.
         *
         * <h4>Banded versus interleaved</h4>
         * If the given sample model is an instance of {@link BandedSampleModel},
         * then this method will unconditionally handle each band as if it was stored in a separated bank,
         * even if it would be more efficient to handle the data as a {@link PixelInterleavedSampleModel}.
         * For example, the GeoTIFF writer does its own analysis of the sample model for deciding whether
         * to declare in <abbr>TIFF</abbr> tags that the data are in  "planar" or "chunky" configuration.
         * The {@code HyperRectangleWriter} must follow that configuration.
         *
         * <p>If the given sample model is the generic {@link ComponentSampleModel}, then this method decides
         * automatically whether the banded or pixel interleaved sample model is the best match for the data.</p>
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer for rasters using the specified sample model (never {@code null}).
         */
        public HyperRectangleWriter create(final ComponentSampleModel sm) {
            int[] bandOffsets;
            pixelStride    = sm.getPixelStride();
            scanlineStride = sm.getScanlineStride();
            bankIndices    = sm.getBankIndices();
            bandOffsets    = sm.getBandOffsets();
            boolean isInterleaved = (sm instanceof PixelInterleavedSampleModel);
            if (!(isInterleaved  ||  sm instanceof BandedSampleModel)) {
                /*
                 * Generic `ComponentSampleModel`. Detect whether it can be handled as interleaved.
                 * The sample model shall use only one bank (actually, that condition is true even
                 * for `PixelInterleavedSampleModel`). Furthermore, the index of each sample value
                 * (relative to the base index) should be inside a pixel stride.
                 */
                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;    // (max - min) defaut to 1 if `bandOffsets` is empty.
                for (int b : bandOffsets) {
                    if (b < min) min = b;
                    if (b > max) max = b;
                }
                isInterleaved = (max - min) < pixelStride;
            }
            if (isInterleaved && ArraysExt.allEquals(bankIndices, bankIndices[0])) {
                /*
                 * PixelInterleavedSampleModel (at least conceptually, no matter the actual type).
                 * The returned `HyperRectangleWriter` instance may write all sample values in a
                 * single call to a `write(…)` method, even if there is many bands.
                 */
                bankIndices = ArraysExt.resize(bankIndices, 1);
                bankOffsets = new int[1];
            } else {
                /*
                 * BandedSampleModel (at least conceptually, no matter the actual type).
                 * The returned `HyperRectangleWriter` instance will need to be used
                 * repetitively by the caller.
                 */
                bankOffsets = bandOffsets;
                bandOffsets = null;
            }
            return create(sm, bandOffsets);
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * {@code SinglePixelPackedSampleModel} packs all sample values of a pixel
         * in a single data array element, one element per pixel.
         *
         * <h4>Limitations</h4>
         * This method currently supports only the writing of a single band using all bits.
         * This constraint is a safety for the GeoTIFF writer in case it didn't detected correctly
         * that a raster need to be reformatted before encoding. One reason for this constraint is
         * because if (for example) an {@code int[]} array is interpreted as an {@code byte[]} array
         * with 4 components per integer, we have a byte order issue.
         *
         * This constraint will probably be relaxed in a future Apache SIS version.
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer for rasters using the specified sample model (never {@code null}).
         * @throws RasterFormatException if the given sample model is not supported.
         */
        public HyperRectangleWriter create(final SinglePixelPackedSampleModel sm) {
            bankIndices    = new int[1];   // Length is NOT the number of bands.
            bankOffsets    = bankIndices;
            pixelStride    = 1;
            scanlineStride = sm.getScanlineStride();
            if (isSupported(this, sm)) {
                return create(sm, null);
            } else {
                throw new RasterFormatException(sm.toString());
            }
        }

        /**
         * Tests whether the given sample model is supported by this builder.
         *
         * @param  builder  the builder in which to store information, or {@code null} if none.
         * @param  sm       the sample model of the rasters to test for support.
         * @return whether the given sample model is supported.
         */
        private static boolean isSupported(final Builder builder, final SinglePixelPackedSampleModel sm) {
            /*
             * If there is only one band, it is okay to store the values using the current data type.
             * We require that all bits are used because otherwise, there is a risk to write garbage
             * and we are not sure that the destination format can encode the mask.
             */
            final int dataSize = DataBuffer.getDataTypeSize(sm.getDataType());
            final int[] d  = sm.getBitMasks();
            if (d.length == 1) {
                int mask = (int) ((1L << dataSize) - 1);
                if ((d[0] & mask) == mask) {
                    return true;
                }
            }
            /*
             * If the sample model is packing many sample values in a single element,
             * many formats such as GeoTIFF will interpret those values as bytes.
             */
            if ((dataSize % (d.length * Byte.SIZE)) != 0) {
                return false;
            }
            int mask = 0xFF;
            for (int i = d.length; --i >= 0;) {
                if (d[i] != mask) {
                    return false;
                }
                mask <<= Byte.SIZE;
            }
            if (builder != null) {
                builder.requiresBigEndian = true;
            }
            return true;
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * {@code MultiPixelPackedSampleModel} represents one-banded images and can
         * pack multiple one-sample pixels into one data element.
         *
         * <h4>Limitations</h4>
         * As a matter of principle, this method verifies that the following conditions are true.
         * They are parts of {@link MultiPixelPackedSampleModel} definition (sometime indirectly),
         * so they should always be true:
         *
         * <ul>
         *   <li>The number of bands is 1.</li>
         *   <li>The pixel bit stride is the number of bits per sample.</li>
         *   <li>All bits are used. It implies that the number of bits is 1, 2, 4, 8, 16 or 32.</li>
         * </ul>
         *
         * This method sets {@link #requiresBigEndian} to {@code true} if a multi-bytes data type is
         * used for storing sample values that could have been stored separately with a smaller type.
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer for rasters using the specified sample model (never {@code null}).
         * @throws RasterFormatException if one of above-cited condition is false (should be very rare).
         */
        public HyperRectangleWriter create(final MultiPixelPackedSampleModel sm) {
            bankIndices    = new int[1];   // Length is NOT the number of bands.
            bankOffsets    = bankIndices;
            pixelStride    = 1;
            pixelBitStride = sm.getPixelBitStride();
            scanlineStride = sm.getScanlineStride();
            if (isSupported(this, sm)) {
                return create(sm, null);
            } else {
                throw new RasterFormatException(sm.toString());
            }
        }

        /**
         * Tests whether the given sample model is supported by this builder.
         *
         * @param  builder  the builder in which to store information, or {@code null} if none.
         * @param  sm       the sample model of the rasters to test for support.
         * @return whether the given sample model is supported.
         */
        private static boolean isSupported(final Builder builder, final MultiPixelPackedSampleModel sm) {
            final int[] d  = sm.getSampleSize();
            if (d.length == 1) {
                final int sampleSize = d[0];
                if (sm.getPixelBitStride() == sampleSize) {
                    final int dataSize = DataBuffer.getDataTypeSize(sm.getDataType());
                    if (dataSize % sampleSize == 0) {   // Check that all bits are used.
                        if (builder != null) {
                            builder.requiresBigEndian = Math.max(sampleSize, Byte.SIZE) != dataSize;
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * The returned writer will need to be applied repetitively for each bank
         * if {@link #numBanks()} returns a value greater than one.
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer for rasters using the specified sample model (never {@code null}).
         * @throws RasterFormatException if the given sample model is not supported.
         *
         * @see #isSupported(SampleModel)
         */
        public HyperRectangleWriter create(final SampleModel sm) {
            if (sm instanceof ComponentSampleModel)         return create((ComponentSampleModel)         sm);
            if (sm instanceof SinglePixelPackedSampleModel) return create((SinglePixelPackedSampleModel) sm);
            if (sm instanceof MultiPixelPackedSampleModel)  return create((MultiPixelPackedSampleModel)  sm);
            throw new RasterFormatException(sm.toString());
        }

        /**
         * Creates a new writer for data of the specified raster.
         * The returned writer will need to be applied repetitively for each bank
         * if {@link #numBanks()} returns a value greater than one.
         *
         * <h4>Tile size</h4>
         * Many formats such as GeoTIFF require that all tiles have the same size,
         * including the size in the last row and last column of the tile matrix.
         * In such case, {@code width} and {@code height} should be set to the size of the tiles to write.
         * This size shall not be greater than the {@link SampleModel} size.
         *
         * @param  tile    the raster to write.
         * @param  width   the tile width,  or -1 for the raster width.
         * @param  height  the tile height, or -1 for the raster height.
         * @return writer for rasters using the specified sample model (never {@code null}).
         * @throws RasterFormatException if the given sample model is not supported.
         */
        public HyperRectangleWriter create(final Raster tile, final int width, final int height) {
            region = tile.getBounds();
            if (width  >= 0) region.width  = width;
            if (height >= 0) region.height = height;
            sampleModelTranslateX = tile.getSampleModelTranslateX();
            sampleModelTranslateY = tile.getSampleModelTranslateY();
            return create(tile.getSampleModel());
        }

        /**
         * {@return the number of banks to write}.
         * This is not necessarily the number of bands, because the writer may be able
         * to write all bands contiguously in a single call to a {@code write(…)} method.
         */
        public int numBanks() {
            return bankIndices.length;
        }

        /**
         * {@return the total number of elements contained in the hyper-rectangle}.
         * The number of bytes to write will be this length multiplied by element size.
         * This information is valid only after a {@code create(…)} method has been invoked.
         */
        public long length() {
            return length;
        }

        /**
         * {@return the number of elements (not necessarily bytes) between a pixel and the next pixel}.
         * This information is valid only after a {@code create(…)} method has been invoked.
         */
        public int pixelStride() {
            return pixelStride;
        }

        /**
         * {@return the number of elements (not necessarily bytes) between a row and the next row}.
         * This information is valid only after a {@code create(…)} method has been invoked.
         */
        public int scanlineStride() {
            return scanlineStride;
        }

        /**
         * Returns the index of a bank to write with {@code HyperRectangleWriter}.
         * This information is valid only after a {@code create(…)} method has been invoked.
         *
         * @param  i  index from 0 inclusive to {@link #numBanks()} exclusive.
         * @return the index of a bank to write with {@code HyperRectangleWriter}.
         */
        public int bankIndex(int i) {
            return bankIndices[i];
        }

        /**
         * Returns the offset to add to a bank to write with {@code HyperRectangleWriter}.
         * This is in addition of offsets declared in {@link DataBuffer#getOffsets()}.
         *
         * @param  i             index from 0 inclusive to {@link #numBanks()} exclusive.
         * @param  bufferOffset  the value of <code>{@link DataBuffer#getOffsets()}[bankIndex(i)]</code>.
         * @return offset of a banks to write with {@code HyperRectangleWriter}.
         */
        public int bankOffset(int i, int bufferOffset) {
            return Math.addExact(bankOffsets[i], bufferOffset);
        }

        /**
         * Returns the byte order to use for writing data, or {@code null} if no change is needed.
         * A specific byte order is sometime needed when {@code short} or {@code int} values are
         * used for packing many bytes. This order can be ignored if, for example, ARGB values
         * are really stored as integers in native byte order rather than 4 bytes.
         */
        public ByteOrder byteOrder(ByteOrder current) {
            return requiresBigEndian && (current != ByteOrder.BIG_ENDIAN) ? ByteOrder.BIG_ENDIAN : null;
        }
    }

    /**
     * Returns the offset of the first element to use in user supplied array.
     *
     * @param  offset  offset supplied by the useR.
     * @return offset to use.
     */
    final int startAt(final int offset) {
        return Math.addExact(startAt, offset);
    }

    /**
     * Returns an array which will be decremented for counting the number of contiguous write operations to apply.
     *
     * @return number of I/O operations to apply.
     */
    final int[] count() {
        return remaining.clone();
    }

    /**
     * Updates the array index to the next value to use after a contiguous write operation.
     *
     * @param  count  array of counters to update in-place.
     * @return next offset, or -1 if the iteration is finished.
     */
    final int next(int offset, final int[] count) {
        for (int i = count.length; --i >= 0;) {
            if (--count[i] >= 0) {
                return offset + strides[i];
            }
            count[i] = remaining[i];
        }
        return -1;
    }

    /**
     * Returns a suggested value for the {@code direct} argument of {@code write(…, byte[], …)}.
     * The suggestion is based on whether the output buffer is direct or not.
     *
     * @param  output  the output which will be given in argument to the {@code write(…)} methods.
     * @return suggested value for the {@code direct} boolean argument.
     */
    public boolean suggestDirect(final ChannelDataOutput output) {
        // Really ! because we want to use the direct buffer if it exists.
        return !output.buffer.isDirect() && output.buffer.capacity() <= contiguousDataLength;
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     * If the {@code direct} argument is {@code true}, then this method writes
     * directly in the {@link ChannelDataOutput#channel} without copying bytes
     * to the {@link ChannelDataOutput#buffer}.
     *
     * <p>Note that the direct mode is not necessarily faster.
     * If the destination is a NIO channel, Java may perform internally a copy to a direct buffer.
     * If the {@code output} buffer is already {@linkplain ByteBuffer#isDirect() direct}, it may be
     * more efficient to set the {@code direct} argument to {@code false} for using that buffer.
     * See {@link #suggestDirect(ChannelDataOutput)} for a hint about this argument value.</p>
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @param  direct  whether to write directly to the channel if possible.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final byte[] data, int offset, final boolean direct) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        if (direct) {
            final ByteBuffer buffer = ByteBuffer.wrap(data);
            output.flush();
            do {
                buffer.limit(offset + contiguousDataLength).position(offset);
                do {
                    final int n = output.channel.write(buffer);
                    output.moveBufferForward(n);
                    if (n == 0) {
                        output.onEmptyTransfer();
                    }
                } while (buffer.hasRemaining());
            } while ((offset = next(offset, count)) >= 0);
        } else {
            do output.write(data, offset, contiguousDataLength);
            while ((offset = next(offset, count)) >= 0);
        }
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final short[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeShorts(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final int[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeInts(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final long[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeLongs(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final float[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeFloats(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final double[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeDoubles(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }
}
