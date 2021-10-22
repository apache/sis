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
package org.apache.sis.image;

import java.util.Objects;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.lang.ref.SoftReference;
import java.nio.ByteOrder;
import org.apache.sis.internal.coverage.j2d.FillValues;
import org.apache.sis.internal.coverage.j2d.TilePlaceholder;

import static org.apache.sis.internal.util.Numerics.ceilDiv;


/**
 * An image which is the result of clearing all pixels inside or outside a geometry.
 * The geometry shall be expressed in pixel coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class MaskedImage extends SourceAlignedImage {
    /**
     * The clip in pixel coordinates.
     */
    private final Shape clip;

    /**
     * {@code true} for masking pixels inside the shape, or
     * {@code false} for masking pixels outside the shape.
     */
    private final boolean maskInside;

    /**
     * The clip after rasterization. Each element contains 8 pixel values.
     * Index of pixel value at coordinate (x,y) can be obtained as below:
     *
     * {@preformat java
     *     int element = mask[y*scanlineStride + x/Byte.SIZE];
     *     int shift   = (Byte.SIZE-1) - (x & (Byte.SIZE-1));
     *     int pixel   = (element >>> shift) & 1;
     * }
     *
     * @see #getMask()
     */
    private transient SoftReference<ByteBuffer> maskRef;

    /**
     * Number of pixels (bits) to skip for reaching the same column of next line (scanline stride).
     * This is the image width rounded to the next multiple of 8 (integer amount of bytes).
     */
    private transient int maskScanlineStride;

    /**
     * Values to assign to the pixels to mask.
     */
    private final FillValues fillValues;

    /**
     * A provider of empty tiles.
     * All empty tiles will share the same data buffer for saving memory.
     */
    private transient volatile TilePlaceholder emptyTiles;

    /**
     * Creates a new image with the given source image and clip.
     *
     * @param source      the image from which to copy pixel values.
     * @param clip        the clip or mask in pixel coordinates.
     * @param maskInside  {@code true} for masking pixels inside the shape, or {@code false} for masking outside.
     * @param fillValues  values to assign to pixels outside clip area, or {@code null} for default values.
     */
    MaskedImage(final RenderedImage source, final Shape clip, final boolean maskInside, final Number[] fillValues) {
        super(source);
        this.clip = clip;
        this.maskInside = maskInside;
        this.fillValues = new FillValues(sampleModel, fillValues, true);
    }

    /**
     * Gets a property from this image.
     */
    @Override
    public Object getProperty(final String key) {
        return POSITIONAL_PROPERTIES.contains(key) ? getSource().getProperty(key) : super.getProperty(key);
    }

    /**
     * Returns the names of all recognized properties.
     *
     * @return names of all recognized properties, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        return filterPropertyNames(getSource().getPropertyNames(), POSITIONAL_PROPERTIES, null);
    }

    /**
     * Returns pixel values of the mask in a multi pixel packed array.
     * After conversion to {@link LongBuffer}, index of pixel value at
     * coordinate (x,y) can be obtained as below:
     *
     * {@preformat java
     *     int element = mask[y*scanlineStride + x/Long.SIZE];
     *     int shift   = (Long.SIZE-1) - (x & (Long.SIZE-1));
     *     int pixel   = (element >>> shift) & 1;
     * }
     */
    private synchronized ByteBuffer getMask() {
        ByteBuffer mask;
        if (maskRef == null || (mask = maskRef.get()) == null) {
            /*
             * Create a 1-bit image with an `IndexColorModel` with two colors: {0, 0, 0} and {255, 255, 255}.
             * Java2D has specialized code for TYPE_BYTE_BINARY; we reproduce something equivalent but we the
             * array size rounded to an integer multiple of {@code long} size.
             */
            final int width  = getWidth();
            final int height = getHeight();
            int size = ceilDiv(width, Byte.SIZE) * height;
            final int r = size & (Long.BYTES - 1);
            if (r != 0) size += Long.BYTES - r;                         // Round to a multiple of 8 bytes.
            final DataBufferByte buffer = new DataBufferByte(size);

            final byte[] gray = {0, -1};
            final IndexColorModel cm = new IndexColorModel(1, gray.length, gray, gray, gray);
            final WritableRaster raster = Raster.createPackedRaster(buffer, width, height, 1, null);
            final Graphics2D g = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null).createGraphics();
            try {
                g.translate(-getMinX(), -getMinY());
                g.setColor(Color.WHITE);
                g.fill(clip);
            } finally {
                g.dispose();
            }
            /*
             * Backing array should be obtained only after drawing,
             * otherwise Java2D may disable hardware acceleration.
             */
            mask = ByteBuffer.wrap(buffer.getData());
            if (maskInside) {
                final LongBuffer b = mask.order(ByteOrder.nativeOrder()).asLongBuffer();
                while (b.hasRemaining()) {
                    b.put(b.position(), ~b.get());          // Inverse all bits.
                }
            }
            mask.order(ByteOrder.BIG_ENDIAN).asReadOnlyBuffer();
            final MultiPixelPackedSampleModel sm = (MultiPixelPackedSampleModel) raster.getSampleModel();
            assert sm.getNumDataElements() == 1 && sm.getPixelBitStride() == 1 && sm.getDataBitOffset() == 0;
            maskScanlineStride = sm.getScanlineStride() * Byte.SIZE;
            maskRef = new SoftReference<>(mask);
        }
        return mask;
    }

    /**
     * Invoked when a tile need to be computed or updated.
     * May be invoked concurrently in different threads.
     *
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @param  tile   if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) {
        final Raster source = getSource().getTile(tileX, tileY);
        final LongBuffer mask = getMask().asLongBuffer();
        final int xImage = getMinX();
        final int yImage = getMinY();
        final int xmin   = source.getMinX();
        final int ymin   = source.getMinY();
        final int xmax   = Math.min(xmin + source.getWidth(),  xImage + getWidth());    // Exclusive.
        final int ymax   = Math.min(ymin + source.getHeight(), yImage + getHeight());
        final int imax   = xmax - xImage - 1;                       // Inclusive.
        final int xoff   = xmin - xImage;
        final int stride = maskScanlineStride;                      // Must be after call to `getMask()`.
        Object  transfer = null;
        int transferSize = 0;
        long present     = -1;                                      // Bits will be set to 0 if some pixels are masked.
        /*
         * Code below is complicated because we use bit twiddling for processing data by chuncks of 64 pixels.
         * A single `(element == 0)` check tells us almost instantaneously that we can skip the next 64 pixels.
         * Otherwise a few bit twiddling tell us efficiently that, for example, there is 40 consecutive values
         * to copy. It allows us to use the Java2D API for transferring blocks of data, which is more efficient
         * than looping over individual pixels.
         */
        for (int y=ymin; y<ymax; y++) {
            int index = (y - yImage) * stride;                      // Index in unit of bits for now (converted later).
            final int emax  = (index +  imax) /  Long.SIZE;         // Last index in unit of long elements, inclusive.
            final int shift = (index += xoff) & (Long.SIZE-1);      // First bit to read in the long, 0 = highest bit.
            index /= Long.SIZE;                                     // Convert from bit (pixel) index to long[] index.
            /*
             * We want a value such as `base + index*Long.SIZE + lower` is equal to `xmin`
             * when all variables point to the first pixel in the current row of the tile:
             *
             *   - `index` has not yet been incremented
             *   - `lower = shift`                          (number of leading zeros)
             *
             * `remaining` is the number of bits to use in the last element (at index = emax).
             */
            final int base = xmin - (index*Long.SIZE + shift);
            final int remaining = xmax - (base + emax*Long.SIZE);
            assert remaining >= 0 && remaining < Long.SIZE : remaining;
            /*
             * Read the bit mask for the first pixels (up to 64) of current row. Some leading bits of
             * the first element may be the rightmost pixels of the tile on the left side of current tile.
             * We need to clear those bits for allowing the loop to skip them.
             */
            long element = mask.get(index);
            if (shift != 0) {
                long m = (1L << (Long.SIZE - shift)) - 1;
                if (index == emax && remaining != 0) {
                    m &= (1L << (Long.SIZE - remaining)) - 1;
                }
                present &= (element | ~m);
                element &= m;
            }
            for (;;) {
                /*
                 * The `element` is a mask for a group of 64 pixels in a row. We try to process pixels
                 * by chunks as much as possible because transfering groups of pixels is faster than
                 * transfering individual pixels. The block below finds ranges of consecutive 1 bits:
                 * all bits from `lower` inclusive to `upper` exclusive are 1. So the corresponding
                 * pixels can be copied in a single transfer.
                 */
                while (element != 0) {
                    final int lower = Long.numberOfLeadingZeros(element);
                    final int upper = Long.numberOfLeadingZeros(~element & ((1L << (Long.SIZE-1 - lower)) - 1));
                    final int x     = base + index*Long.SIZE + lower;
                    final int count = upper - lower;
                    assert count > 0 && count <= Long.SIZE : count;
                    if (count > transferSize) {
                        if (transferSize == 0) {
                            // First time that we copy pixels.
                            boolean clean = needCreate(tile, source);
                            if (clean) {
                                tile = createTile(tileX, tileY);
                                clean = fillValues.isFullyZero;
                            }
                            if (!clean) {
                                fillValues.fill(tile);
                            }
                        }
                        transferSize = count;
                        transfer = null;
                    }
                    transfer = source.getDataElements(x, y, count, 1, transfer);
                    tile.setDataElements(x, y, count, 1, transfer);
                    element &= (1L << (Long.SIZE - upper)) - 1;
                }
                /*
                 * Finished to process 64 pixels (or maybe less if we were at the beginning or end of row).
                 * Move to the next group of 64 pixels, with a special case for the last element of a row.
                 */
                if (++index < emax) {
                    element = mask.get(index);
                    present &= element;
                } else if (index == emax) {
                    /*
                     * The last element in a row may contain pixel values for the tile on the right side
                     * of current tile. We need to clear those pixels for allowing the loop to skip them.
                     */
                    if (remaining == 0) break;
                    final long m = (1L << (Long.SIZE - remaining)) - 1;
                    element = mask.get(index);
                    present &= (element | m);
                    element &= ~m;
                } else {
                    break;
                }
            }
        }
        /*
         * The tile has been created only if at least one pixel needs to be copied from the source tile.
         * If the tile is still null at this point, it means that it is fully empty.
         */
        if (tile == null) {
            TilePlaceholder p = emptyTiles;
            if (p == null) {
                // Not a problem if invoked concurrently by two threads.
                emptyTiles = p = TilePlaceholder.filled(sampleModel, fillValues);
            }
            return p.create(new Point(source.getMinX(), source.getMinY()));
        }
        /*
         * If no bit from the `present` mask have been cleared, then it means that all pixels
         * have been copied. In such case the source tile can be returned directly.
         */
        return (present == -1) ? source : tile;
    }

    /**
     * Returns {@code true} if the given tile should be discarded and a new tile created
     * before to write pixel values.
     *
     * @param  tile    the tile to potentially reuse. Can be {@code null}.
     * @param  source  the corresponding tile from source image.
     * @return {@code true} if given tile is null or should not be modified.
     */
    private boolean needCreate(final WritableRaster tile, final Raster source) {
        if (tile == null || tile.getDataBuffer() == source.getDataBuffer()) {
            return true;
        }
        final TilePlaceholder p = emptyTiles;
        return (p != null) && p.isCreatorOf(tile);
    }

    /**
     * Compares the given object with this image for equality. This method returns {@code true}
     * if the given object is non-null, is an instance of the exact same class than this image,
     * has equal sources, clip and fill values.
     *
     * @param  object  the object to compare with this image.
     * @return {@code true} if the given object is an image performing the same clipping than this image.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass().equals(getClass())) {
            final MaskedImage other = (MaskedImage) object;
            return clip.equals(other.clip) &&
                   Objects.deepEquals(fillValues, other.fillValues) &&
                   getSources().equals(other.getSources());
        }
        return false;
    }

    /**
     * Returns a hash code value for this image.
     *
     * @return a hash code value based on a description of the operation performed by this image.
     */
    @Override
    public int hashCode() {
        return clip.hashCode() + Objects.hashCode(fillValues) + getSources().hashCode();
    }
}
