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

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.image.internal.shared.ImageUtilities;


/**
 * An image which is the result of averaging 4 pixels of the image at higher resolution.
 * It can be seen as a special case of {@link ResampledImage} with bilinear interpolation
 * at the exact center of a block of 4 pixels.
 *
 * @todo Add an auxiliary image with contains the rest of the division by 4 (when sample values are integers)
 *       or the number of averaged sample values (when sample values are floating points).
 *       Use that information when computing overview of overviews, for better accuracy.
 *
 * @author  Estelle Idée (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OverviewImage extends ComputedImage {
    /**
     * The image origin.
     */
    private final int minX, minY;

    /**
     * The image size in pixels.
     */
    private final int width, height;

    /**
     * The offset to add after conversion from target to source pixel coordinates.
     * This is either 0 or 1.
     */
    private final byte offsetX, offsetY;

    /**
     * Creates a new image which will create an overview of the given image.
     *
     * @param  source  the image at higher resolution.
     */
    OverviewImage(final RenderedImage source) {
        this(targetBounds(ImageUtilities.getBounds(source)), source);
    }

    /** Workaround for RFE #4093999 ("Relax constraint on placement of this()/super() call in constructors"). */
    private static Rectangle targetBounds(final Rectangle bounds) {
        bounds.x     >>= 1;     // Round toward negative infinity.
        bounds.y     >>= 1;
        bounds.width  /= 2;     // Round toward 0.
        bounds.height /= 2;
        return bounds;
    }

    /** Workaround for RFE #4093999 ("Relax constraint on placement of this()/super() call in constructors"). */
    private OverviewImage(final Rectangle bounds, final RenderedImage source) {
        super(ImageLayout.DEFAULT.createCompatibleSampleModel(source, bounds), source);
        offsetX = (byte) (source.getMinX() & 1);    // TODO: move before `targetBounds(…)` after RFE #4093999.
        offsetY = (byte) (source.getMinY() & 1);
        minX    = bounds.x;
        minY    = bounds.y;
        width   = bounds.width;
        height  = bounds.height;
    }

    /**
     * Returns the color model of this resampled image.
     * Default implementation assumes that this image has the same color model as the source image.
     *
     * @return the color model, or {@code null} if unspecified.
     */
    @Override
    public ColorModel getColorModel() {
        return getSource().getColorModel();
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction.
     */
    @Override
    public final int getMinTileX() {
        return getSource().getMinTileX() / 2;   // Round toward zero.
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction.
     */
    @Override
    public final int getMinTileY() {
        return getSource().getMinTileY() / 2;
    }

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     */
    @Override
    public final int getMinX() {
        return minX;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     */
    @Override
    public final int getMinY() {
        return minY;
    }

    /**
     * Returns the number of columns in this image.
     */
    @Override
    public final int getWidth() {
        return width;
    }

    /**
     * Returns the number of rows in this image.
     */
    @Override
    public final int getHeight() {
        return height;
    }

    /**
     * Invoked when a tile needs to be computed or updated.
     *
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @param  tile   if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) {
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        Rectangle bounds = tile.getBounds();
        bounds.width  <<= 1;
        bounds.height <<= 1;
        bounds.x      <<= 1;
        bounds.y      <<= 1;
        bounds.x += offsetX;
        bounds.y += offsetY;
        final PixelIterator it = new PixelIterator.Builder()
                .setIteratorOrder(SequenceType.LINEAR)
                .setRegionOfInterest(bounds)
                .create(getSource());
        /*
         * The iterator may have intersected the given bounds with the source image bounds.
         * Therefore, we derive the limits from these bounds instead of from tile bounds.
         * It should cover the whole valid area of the tile.
         */
        bounds = it.getDomain();
        if (((bounds.width | bounds.height) & 1) != 0) {
            throw new ImagingOpException(Resources.format(Resources.Keys.IncompatibleTile_2, tileX, tileY));
        }
        bounds.x      >>= 1;    // Round toward negative infinity.
        bounds.y      >>= 1;
        bounds.width  >>= 1;
        bounds.height >>= 1;
        final int numBands = tile.getNumBands();
        final var buffer   = new double[Math.multiplyExact(bounds.width, numBands)];
        final var counts   = new byte[buffer.length];
        double[]  left     = null;
        double[]  right    = null;
        final int ymax = bounds.y + bounds.height;
        for (int y = bounds.y; y < ymax; y++) {
            int x = bounds.x;
            /*
             * Memorize the sum of two consecutive pixels for all pixels in the current source row.
             * The `counts` array contains the number of valid values, which will by 2, 3 or 4 on
             * the assumption that the next row will not contain NaN value (verified in next loop).
             */
            for (int i=0; i < buffer.length;) {
                if (it.next()) {
                    left = it.getPixel(left);
                    if (it.next()) {
                        right = it.getPixel(right);
                        for (int b=0; b<numBands; b++) {
                            byte count = 4;
                            double sum = left[b] + right[b];
                            if (Double.isNaN(sum)) {
                                // Give precedence to the left side if both sides are NaN.
                                count = (Double.isNaN(sum = right[b]) &&
                                         Double.isNaN(sum =  left[b])) ? (byte) 2 : (byte) 3;
                            }
                            buffer[i] = sum;
                            counts[i++] = count;
                        }
                        continue;
                    }
                }
                throw new ImagingOpException(Resources.format(Resources.Keys.OutOfIteratorDomain_2, i/numBands + x, y));
            }
            /*
             * Read the next row and compute the average with the previous row which was memorized by above loop.
             * If some values are NaN, the number of valid values gien by `counts` is adjusted.
             */
            for (int i=0; i < buffer.length; x++) {
                if (it.next()) {
                    left = it.getPixel(left);
                    if (it.next()) {
                        right = it.getPixel(right);
                        for (int b=0; b<numBands; b++, i++) {
                            int  count = counts[i];
                            double add = left[b] + right[b];
                            double sum = add + buffer[i];
                            // Test `isNaN(sum)` first because it will be false in the vast majority of cases.
                            if (Double.isNaN(sum) && Double.isNaN(sum = add)) {
                                sum = buffer[i];
                                if (Double.isNaN(add = right[b]) && Double.isNaN(add = left[b])) {
                                    count -= 2;     // The two values of the current row are invalid.
                                } else {
                                    count--;        // Exactly one value of the current row is valid.
                                    sum = Double.isNaN(sum) ? add : sum + add;
                                }
                                if (count <= 1) {
                                    // Avoid a division by 0 in order to preserve the NaN bits pattern.
                                    left[b] = sum;
                                    continue;
                                }
                            }
                            left[b] = sum / count;
                        }
                        tile.setPixel(x, y, left);
                        continue;
                    }
                }
                throw new ImagingOpException(Resources.format(Resources.Keys.OutOfIteratorDomain_2, x, y));
            }
        }
        return tile;
    }

    /**
     * Notifies the source image that tiles will be computed soon in the given region.
     * If the source image is an instance of {@link ComputedImage}, then this method
     * forwards the notification to it.
     */
    @Override
    protected Disposable prefetch(final Rectangle tiles) {
        final RenderedImage source = getSource();
        if (source instanceof PlanarImage) {
            final long xmin = 2L * tiles.x + offsetX;
            final long ymin = 2L * tiles.y + offsetY;
            final long xmax = 2L * tiles.width  + xmin;
            final long ymax = 2L * tiles.height + ymin;
            final int x = Numerics.clamp(xmin);
            final int y = Numerics.clamp(ymin);
            return ((PlanarImage) source).prefetch(
                    new Rectangle(x, y, Numerics.clamp(xmax - x),
                                        Numerics.clamp(ymax - y)));
        }
        return super.prefetch(tiles);
    }

    /**
     * Compares the given object with this image for equality.
     *
     * @param  object  the object to compare with this image.
     * @return {@code true} if the given object is an image performing the same overview as this image.
     */
    @Override
    public boolean equals(final Object object) {
        if (equalsBase(object)) {
            final var other = (OverviewImage) object;
            return minX    == other.minX    &&
                   minY    == other.minY    &&
                   width   == other.width   &&
                   height  == other.height  &&
                   offsetX == other.offsetX &&
                   offsetY == other.offsetY;
        }
        return false;
    }

    /**
     * Returns a hash code value for this image. The {@link #minX}, {@link #minY}, {@link #width} and {@link #height}
     * fields are included in the hash computation as a matter of principle, but this is actually not very important
     * because they are derived information.
     */
    @Override
    public int hashCode() {
        return hashCodeBase() + (minX + 31*(minY + 31*(width + 31*height)));
    }
}
