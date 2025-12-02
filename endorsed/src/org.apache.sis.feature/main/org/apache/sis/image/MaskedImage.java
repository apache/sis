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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.ByteOrder;
import java.awt.Rectangle;
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
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.image.internal.shared.FillValues;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.image.internal.shared.TilePlaceholder;
import static org.apache.sis.util.internal.shared.Numerics.LONG_SHIFT;
import static org.apache.sis.pending.jdk.JDK18.ceilDiv;


/**
 * An image which is the result of clearing all pixels inside or outside a geometry.
 * The geometry shall be expressed in pixel coordinates.
 *
 * @author  Martin Desruisseaux (Geomatys)
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
     * Bounds of the {@linkplain #clip} in pixels coordinates and in tile coordinates.
     * The latter provides a fast way to determine if a tile intersects the mask.
     * The bounds are computed together when first needed.
     *
     * @see #getMaskTiles()
     */
    private transient volatile Rectangle maskBounds, maskTiles;

    /**
     * The clip after rasterization. Each element contains 8 pixel values.
     * Index of pixel value at coordinate (x,y) can be obtained as below:
     *
     * {@snippet lang="java" :
     *     int xm      = x - maskBounds.x;
     *     int xy      = y - maskBounds.y;
     *     int element = mask[ym*scanlineStride + xm/Byte.SIZE];
     *     int shift   = (Byte.SIZE-1) - (xm & (Byte.SIZE-1));
     *     int pixel   = (element >>> shift) & 1;
     *     }
     *
     * @see #getMask()
     */
    private transient SoftReference<ByteBuffer> maskRef;

    /**
     * Number of pixels (bits) to skip for reaching the same column of next line (scanline stride).
     * This is the image width rounded to the next multiple of 8 (integer number of bytes).
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
     * Returns the bounds of the {@linkplain #clip} in tile coordinates.
     * It provides a fast way to determine if a tile intersects the mask.
     */
    private Rectangle getMaskTiles() {
        Rectangle bt = maskTiles;
        if (bt == null) {
            synchronized (this) {
                bt = maskTiles;
                if (bt == null) {
                    final RenderedImage source = getSource();
                    final Rectangle bp = clip.getBounds();
                    ImageUtilities.clipBounds(source, bp);
                    bt = new Rectangle();
                    if (!bp.isEmpty()) {
                        final int xmax = ImageUtilities.pixelToTileX(source, bp.x + bp.width  - 1) + 1;
                        final int ymax = ImageUtilities.pixelToTileY(source, bp.y + bp.height - 1) + 1;
                        bt.width  = xmax - (bt.x = ImageUtilities.pixelToTileX(source, bp.x));
                        bt.height = ymax - (bt.y = ImageUtilities.pixelToTileY(source, bp.y));
                    }
                    maskBounds = bp;
                    maskTiles  = bt;
                }
            }
        }
        return bt;
    }

    /**
     * Returns pixel values of the mask in a multi pixel packed array.
     * After conversion to {@link LongBuffer}, index of pixel value at
     * coordinate (x,y) can be obtained as below:
     *
     * {@snippet lang="java" :
     *     int xm      = x - maskBounds.x;
     *     int xy      = y - maskBounds.y;
     *     int element = mask[ym*scanlineStride + xm/Long.SIZE];
     *     int shift   = (Long.SIZE-1) - (xm & (Long.SIZE-1));
     *     int pixel   = (element >>> shift) & 1;
     *     }
     *
     * <h4>Preconditions</h4>
     * The {@link #getMaskTiles()} method must have been invoked at least once before this method.
     */
    private synchronized ByteBuffer getMask() {
        ByteBuffer mask;
        if (maskRef == null || (mask = maskRef.get()) == null) {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            final Rectangle maskBounds = this.maskBounds;
            int size = ceilDiv(maskBounds.width, Byte.SIZE) * maskBounds.height;
            final int r = size & (Long.BYTES - 1);
            if (r != 0) size += Long.BYTES - r;                         // Round to a multiple of 8 bytes.
            final DataBufferByte buffer = new DataBufferByte(size);
            /*
             * Create a 1-bit image with an `IndexColorModel` with two colors: {0, 0, 0} and {255, 255, 255}.
             * Java2D has specialized code for TYPE_BYTE_BINARY; we reproduce something equivalent but with
             * the array size rounded to an integer multiple of `long` size.
             */
            final byte[] gray = {0, -1};
            final IndexColorModel cm = new IndexColorModel(1, gray.length, gray, gray, gray);
            final WritableRaster raster = Raster.createPackedRaster(buffer, maskBounds.width, maskBounds.height, 1, null);
            final Graphics2D g = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null).createGraphics();
            try {
                g.translate(-maskBounds.x, -maskBounds.y);
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
            mask = mask.order(ByteOrder.BIG_ENDIAN).asReadOnlyBuffer();
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
        /*
         * Before to compute the tile, check if the tile is outside the mask.
         * If this is the case, we can return a tile (source or empty) as-is.
         */
        final RenderedImage source = getSource();
        final int xmin = ImageUtilities.tileToPixelX(source, tileX);
        final int ymin = ImageUtilities.tileToPixelY(source, tileY);
        if (!getMaskTiles().contains(tileX, tileY)) {
            if (maskInside) {
                return source.getTile(tileX, tileY);
            } else {
                return createEmptyTile(xmin, ymin);
            }
        }
        /*
         * Tile may intersect the mask. Computation is necessary, but we may discover at
         * the end of this method that the result is still an empty tile or source tile.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Rectangle maskBounds = this.maskBounds;
        final LongBuffer mask = getMask().asLongBuffer();
        final int xmax   = xmin + source.getTileWidth();
        final int ymax   = ymin + source.getTileHeight();
        final int xEnd   = Math.min(xmax, maskBounds.x + maskBounds.width);
        final int yEnd   = Math.min(ymax, maskBounds.y + maskBounds.height);
        final int xStart = Math.max(xmin, maskBounds.x);
        final int yStart = Math.max(ymin, maskBounds.y);
        final int imax   = xEnd   - maskBounds.x;                   // Maximum x index in mask, exclusive.
        final int xoff   = xStart - maskBounds.x;
        Raster    data   = null;
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
        for (int y=yStart; y<yEnd; y++) {
            int index = (y - maskBounds.y) * maskScanlineStride;    // Index in unit of bits for now (converted later).
            final int emax  = (index +  imax) >>> LONG_SHIFT;       // Last index in unit of long elements, inclusive.
            final int shift = (index += xoff) & (Long.SIZE-1);      // First bit to read in the long, 0 = highest bit.
            index >>>= LONG_SHIFT;                                  // Convert from bit (pixel) index to long[] index.
            /*
             * We want a value such as `base + index*Long.SIZE + lower` is equal to `xStart`
             * when all variables point to the first potentially masked pixel of the tile:
             *
             *   - `index` has not yet been incremented
             *   - `lower = shift`                          (number of leading zeros)
             *
             * `remaining` is the number of bits to use in the last element (at index = emax).
             */
            final int base = xStart - (index*Long.SIZE + shift);
            final int remaining = xEnd - (base + emax*Long.SIZE);
            assert remaining >= 0 && remaining < Long.SIZE : remaining;
            /*
             * Read the bit mask for the first pixels (up to 64) of current row. Some leading bits of
             * the first element may be the rightmost pixels of the tile on the left side of current tile.
             * We need to clear those bits for allowing the loop to skip them.
             */
            long element = mask.get(index);
            {   // For keeping variable local.
                long m = Numerics.bitmask(Long.SIZE - shift) - 1;           // All bits set if shift = 0.
                if (index == emax && remaining != 0) {
                    m &= -(1L << (Long.SIZE - remaining));                  // ~(x-1) simplified as -x
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
                        if (data == null) {
                            /*
                             * First time that we copy pixels. Get the rasters only at this point.
                             * This delay allows to avoid computing the source tile when fully masked.
                             */
                            data = source.getTile(tileX, tileY);
                            assert data.getMinX() == xmin && data.getMinY() == ymin;
                            boolean clean = needCreate(tile, data);
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
                    transfer = data.getDataElements(x, y, count, 1, transfer);
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
         * The tile is fetched only if at least one pixel needs to be copied from the source tile.
         * If the source tile is still null at this point, it means that masked region is fully empty.
         * Note that the `target` variable may be non-null because it was an argument to this method.
         */
        final boolean isFullTile = (xStart == xmin && yStart == ymin && xEnd == xmax && yEnd == ymax);
        if (data == null) {
            if (isFullTile) {
                return createEmptyTile(xmin, ymin);
            }
            data = source.getTile(tileX, tileY);
            boolean clean = needCreate(tile, data);
            if (clean) {
                tile = createTile(tileX, tileY);
                clean = fillValues.isFullyZero;
            }
            if (!clean) {
                fillValues.fill(tile);
            }
        }
        /*
         * If no bit from the `present` mask have been cleared, then it means that all pixels
         * have been copied. In such case the source tile can be returned directly.
         */
        assert data.getMinX() == xmin && data.getMinY() == ymin;
        if (present == -1 && (isFullTile | maskInside)) {
            return data;
        }
        /*
         * The tile is partially masked. If the tile is not fully included in `maskBounds`,
         * there is some pixels that we need to copy here.
         */
        if (maskInside) {
            final int width  = xmax - xmin;
            final int height = yEnd - yStart;
complete:   for (int border = 0; ; border++) {
                final int start, span;
                switch (border) {
                    case 0:  span = yStart - (start = ymin); break;     // Top    (horizontal, lower y)
                    case 1:  span = ymax   - (start = yEnd); break;     // Bottom (horizontal, upper y)
                    case 2:  span = xStart - (start = xmin); break;     // Left   (vertical,   lower x)
                    case 3:  span = xmax   - (start = xEnd); break;     // Right  (vertical,   upper x)
                    default: break complete;
                }
                final boolean horizontal = (border & 2) == 0;
                final int area = span * (horizontal ? width : height);
                if (area > 0) {
                    if (area > transferSize) {
                        transferSize = area;
                        transfer = null;
                    }
                    if (horizontal) {
                        transfer = data.getDataElements(xmin, start, width, span, transfer);
                        tile.setDataElements(xmin, start, width, span, transfer);
                    } else {
                        transfer = data.getDataElements(start, yStart, span, height, transfer);
                        tile.setDataElements(start, yStart, span, height, transfer);
                    }
                }
            }
        }
        return tile;
    }

    /**
     * Returns an empty tile starting at the given pixel coordinates. Each empty tile will share its
     * data buffer with other empty tiles, including from other images using the same sample model.
     */
    private Raster createEmptyTile(final int xmin, final int ymin) {
        TilePlaceholder p = emptyTiles;
        if (p == null) {
            // Not a problem if invoked concurrently by two threads.
            emptyTiles = p = TilePlaceholder.filled(sampleModel, fillValues);
        }
        return p.create(new Point(xmin, ymin));
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
     * if the given object is non-null, is an instance of the exact same class as this image,
     * has equal sources, clip and fill values.
     *
     * @param  object  the object to compare with this image.
     * @return {@code true} if the given object is an image performing the same clipping as this image.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final var other = (MaskedImage) object;
            return clip.equals(other.clip) && fillValues.equals(other.fillValues);
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
        return super.hashCode() + clip.hashCode() + fillValues.hashCode();
    }
}
