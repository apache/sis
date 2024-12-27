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

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.RasterFormatException;
import static java.lang.Math.floorDiv;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Messages;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.image.privy.ImageUtilities;
import static org.apache.sis.pending.jdk.JDK18.ceilDiv;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.grid.SequenceType;


/**
 * An iterator over sample values in a raster or an image.  This iterator makes easier to read and write efficiently
 * pixel or sample values. The iterator {@linkplain RenderedImage#getTile(int,int) acquires tiles} and releases them
 * automatically. Unless otherwise specified, iterators are free to use an {@linkplain #getIterationOrder() iteration
 * order} that minimize the "acquire / release tile" operations (in other words, iterations are not necessarily from
 * left to right). Iteration can be performed on a complete image or only a sub-region of it. Some optimized iterator
 * implementations exist for a few commonly used {@linkplain java.awt.image.SampleModel sample models}.
 *
 * <h2>Example</h2>
 * {@snippet lang="java" :
 *     PixelIterator it = PixelIterator.create(image);
 *     double[] samples = null;
 *     while (it.next()) {
 *         samples = it.getPixel(samples);      // Get values in all bands.
 *         // Perform computation here...
 *     }
 *     }
 *
 * <h2>Default implementation</h2>
 * This base class uses the {@link Raster} API for traversing the pixels in each tile.
 * Calls to {@link #next()} move the current position by increasing the following values, in order:
 *
 * <ol>
 *   <li>Column index in a single tile (from left to right)</li>
 *   <li>Row index in a single tile (from top to bottom).</li>
 *   <li>Then, {@code tileX} index from left to right.</li>
 *   <li>Then, {@code tileY} index from top to bottom.</li>
 * </ol>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.2
 * @since   1.0
 */
public class PixelIterator {
    /**
     * The image in which iteration is occurring, or {@code null} if none.
     * If {@code null}, then {@link #currentRaster} must be non-null.
     */
    final RenderedImage image;

    /**
     * The current raster in which iteration is occurring. This may change when the iterator
     * reaches a new {@link #image} tile. May be {@code null} if not yet determined.
     *
     * @see RenderedImage#getTile(int, int)
     */
    private Raster currentRaster;

    /**
     * Number of bands in all tiles in the {@linkplain #image}.
     * The {@link #currentRaster} shall always have this number of bands.
     */
    final int numBands;

    /**
     * The domain, in pixel coordinates, of the region traversed by this pixel iterator.
     * This may be smaller than the image or raster bounds, but not greater.
     * The lower values are inclusive and the upper values are exclusive.
     *
     * @see #getDomain()
     */
    final int lowerX, lowerY, upperX, upperY;

    /**
     * Size of all tiles in the {@link #image}.
     */
    private final int tileWidth, tileHeight;

    /**
     * The X and Y coordinate of the upper-left pixel of tile (0,0).
     * Note that tile (0,0) may not actually exist.
     */
    private final int tileGridXOffset, tileGridYOffset;

    /**
     * The domain, in tile coordinates, of the region traversed by this pixel iterator.
     * The tile index ranges may be smaller than the ranges of valid indices in image,
     * but not greater. The lower values are inclusive and the upper values exclusive.
     */
    private final int tileLowerX, tileLowerY, tileUpperX, tileUpperY;

    /**
     * Size of the window to use in {@link #createWindow(TransferType)} method, or {@code 0} if none.
     */
    private final int windowWidth, windowHeight;

    /**
     * Tile coordinate of {@link #currentRaster}.
     * The {@code tileY >= tileUpperY} condition is used for detecting when we reached iteration end.
     */
    int tileX, tileY;

    /**
     * Current (column, row) index in current raster.
     * The {@code x >= lowerX} condition is used for detecting if iteration started.
     */
    int x, y;

    /**
     * Bounds of the region traversed by the iterator in {@linkplain #currentRaster current raster}.
     * When iteration reaches the upper coordinates, the iterator needs to move to next tile.
     * This is the raster bounds clipped to the area of interest.
     */
    private int currentLowerX, currentUpperX, currentUpperY;

    /**
     * Maximal {@linkplain #x} and {@linkplain #y} coordinates (exclusive) that {@link Window} can use for
     * fetching values in current tile. If some (x,y) coordinates inside the window are equal or greater,
     * then the window will need to fetch some values on neighbor tiles (i.e. the window is overlapping
     * two or more tiles).
     *
     * <p>This is initialized by {@link #fetchTile()} to the same values as {@link #currentUpperX} and
     * {@link #currentUpperY} but without clipping to the area of interest. We want to keep the flexibility
     * to overwrite with other coordinate system in future versions, if useful for {@link Window} performance.
     * Consequently, those values should not be used in other context than {@link #fetchValues(Window, Object)}.</p>
     */
    private int windowLimitX, windowLimitY;

    /**
     * {@code true} for default iteration order, or {@code false} for {@link SequenceType#LINEAR}.
     * Note that the order is equivalent to linear order when there is only one tile.
     */
    private final boolean isDefaultOrder;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  data     the raster which contains the sample values on which to iterate.
     * @param  subArea  the raster region where to perform the iteration, or {@code null}
     *                  for iterating over all the raster domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     * @param  order    {@code null} or {@link SequenceType#LINEAR}. Other values may be added in future versions.
     */
    PixelIterator(final Raster data, final Rectangle subArea, final Dimension window, final SequenceType order) {
        final Rectangle bounds;
        image           = null;
        currentRaster   = data;
        numBands        = data.getNumBands();
        tileWidth       = data.getWidth();
        tileHeight      = data.getHeight();
        tileGridXOffset = data.getMinX();
        tileGridYOffset = data.getMinY();
        bounds          = intersection(tileGridXOffset, tileGridYOffset, tileWidth, tileHeight, subArea, window);
        tileLowerX      = 0;                    // In this case only one raster: tile index is fixed to 0.
        tileLowerY      = 0;
        tileUpperX      = (bounds.width  == 0) ? 0 : 1;
        tileUpperY      = (bounds.height == 0) ? 0 : 1;
        lowerX          = bounds.x;
        lowerY          = bounds.y;
        upperX          = Math.addExact(lowerX, bounds.width);
        upperY          = Math.addExact(lowerY, bounds.height);
        windowWidth     = (window != null) ? window.width  : 0;
        windowHeight    = (window != null) ? window.height : 0;
        currentLowerX   = lowerX;
        currentUpperX   = upperX;
        currentUpperY   = upperY;
        windowLimitX    = Math.addExact(tileGridXOffset, tileWidth);    // Initialized here because `fetchTile()` will not be invoked.
        windowLimitY    = Math.addExact(tileGridYOffset, tileHeight);
        x               = Math.decrementExact(lowerX);                  // Set to the position before first pixel.
        y               = lowerY;
        isDefaultOrder  = true;
    }

    /**
     * Creates an iterator for the given region in the given image.
     *
     * @param  data     the image which contains the sample values on which to iterate.
     * @param  subArea  the image region where to perform the iteration, or {@code null}
     *                  for iterating over all the image domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     * @param  order    {@code null} or {@link SequenceType#LINEAR}. Other values may be added in future versions.
     */
    PixelIterator(final RenderedImage data, final Rectangle subArea, final Dimension window, final SequenceType order) {
        final Rectangle bounds;
        image           = data;
        numBands        = data.getSampleModel().getNumBands();
        tileWidth       = data.getTileWidth();
        tileHeight      = data.getTileHeight();
        tileGridXOffset = data.getTileGridXOffset();
        tileGridYOffset = data.getTileGridYOffset();
        bounds          = intersection(data.getMinX(), data.getMinY(), data.getWidth(), data.getHeight(), subArea, window);
        lowerX          = bounds.x;
        lowerY          = bounds.y;
        upperX          = Math.addExact(lowerX, bounds.width);
        upperY          = Math.addExact(lowerY, bounds.height);
        tileLowerX      = floorDiv(Math.subtractExact(lowerX, tileGridXOffset), tileWidth);
        tileLowerY      = floorDiv(Math.subtractExact(lowerY, tileGridYOffset), tileHeight);
        tileUpperX      =  ceilDiv(Math.subtractExact(upperX, tileGridXOffset), tileWidth);
        tileUpperY      =  ceilDiv(Math.subtractExact(upperY, tileGridYOffset), tileHeight);
        windowWidth     = (window != null) ? window.width  : 0;
        windowHeight    = (window != null) ? window.height : 0;
        tileX           = Math.decrementExact(tileLowerX);
        tileY           = tileLowerY;
        currentLowerX   = lowerX;
        currentUpperX   = lowerX;               // Really `lower`, so the position is the tile before the first tile.
        currentUpperY   = lowerY;
        x               = Math.decrementExact(lowerX);          // Set to the position before first pixel.
        y               = lowerY;
        isDefaultOrder  = (order == null) || (tileUpperX - tileLowerX) <= 1;
        /*
         * We need to ensure that `tileUpperY+1 > tileUpperY` will alway be true because `tileY` may be equal
         * to `tileUpperY` when the `if (++tileY >= tileUpperY)` statement is excuted in the `next()` method.
         * This is because `tileY` is used as a sentinel value for detecting when we reached iteration end.
         */
        if (tileUpperY == Integer.MAX_VALUE) {
            throw new ArithmeticException(Errors.format(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
        }
    }

    /**
     * Computes the intersection between the given bounds and {@code subArea} if {@code subArea} is non-null.
     * If the result is empty, then the width and/or height are set to zero (not negative).
     */
    private static Rectangle intersection(int x, int y, int width, int height, Rectangle subArea, Dimension window) {
        if (window != null) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.EmptyImage));
            }
            ArgumentChecks.ensureBetween("window.width",  1, width,  window.width);
            ArgumentChecks.ensureBetween("window.height", 1, height, window.height);
            width  -= (window.width  - 1);
            height -= (window.height - 1);
        }
        Rectangle bounds = new Rectangle(x, y, width, height);
        if (subArea != null) {
            bounds = bounds.intersection(subArea);
            if (bounds.width  < 0) {bounds.x = x; bounds.width  = 0;}
            if (bounds.height < 0) {bounds.y = y; bounds.height = 0;}
        }
        return bounds;
    }

    /**
     * Builds pixel iterators for specified region of interest, window size or iteration order.
     * By default, the builder creates iterators for all pixels in the given raster or image,
     * with unspecified iteration order. Users can invoke setter methods for specifying
     * desired behavior for the iterators to create.
     *
     * <h2>Example</h2>
     * {@snippet lang="java" :
     *     PixelIterator iterator = new PixelIterator.Builder()
     *             .setRegionOfInterest(new Rectangle(10, 10, 5, 5)
     *             .create(image);
     *     }
     */
    public static class Builder {
        /**
         * The region where to perform the iteration, or {@code null} for iterating over all the domain.
         */
        private Rectangle subArea;

        /**
         * Size of the window to use in {@link PixelIterator#createWindow(TransferType)} method,
         * or {@code null} if none.
         */
        private Dimension window;

        /**
         * The desired iteration order, or {@code null} for a default order.
         */
        private SequenceType order;

        /**
         * Creates a new iterator builder with no region of interest, no window size and default iterator order.
         */
        public Builder() {
        }

        /**
         * Sets the region (in pixel coordinates) where to perform the iteration.
         * If this method is not invoked, then  by default iterators will traverse all pixels in the image or raster.
         * If a sub-area is specified, then the traversed area is the {@linkplain Rectangle#intersection(Rectangle)
         * intersection} of {@code subArea} with the image or {@linkplain Raster#getBounds() raster bounds}.
         *
         * @param  subArea  region to intersect, or {@code null} for iterating over all image domain.
         * @return {@code this} for method call chaining.
         * @throws IllegalArgumentException if the given rectangle is empty.
         */
        public Builder setRegionOfInterest(final Rectangle subArea) {
            if (subArea != null && subArea.isEmpty()) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.EmptyTileOrImageRegion));
            }
            this.subArea = subArea;
            return this;
        }

        /**
         * Sets the size of the window to use in {@link PixelIterator#createWindow(TransferType)} method.
         * By default, iterators do not create windows.
         *
         * @param  window  the window size, or {@code null} if no window will be created.
         * @return {@code this} for method call chaining.
         */
        public Builder setWindowSize(final Dimension window) {
            this.window = window;
            return this;
        }

        /**
         * Sets the desired iteration order.
         * The {@code order} argument can have the following values:
         *
         * <table class="sis">
         *   <caption>Supported iteration order</caption>
         *   <tr><th>Value</th>                         <th>Iteration order</th></tr>
         *   <tr><td>{@code null}</td>                  <td>Most efficient iteration order.</td></tr>
         *   <tr><td>{@link SequenceType#LINEAR}</td>   <td>From left to right, then from top to bottom.</td></tr>
         * </table>
         *
         * Any other {@code order} value will cause an {@link IllegalArgumentException} to be thrown.
         * More iteration orders may be supported in future Apache SIS versions.
         *
         * @param  order  the desired iteration order, or {@code null} for a default order.
         * @return {@code this} for method call chaining.
         */
        public Builder setIteratorOrder(final SequenceType order) {
            if (order == null || order.equals(SequenceType.LINEAR)) {
                this.order = order;
            } else {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, order));
            }
            return this;
        }

        /**
         * If the given image is a wrapper doing nothing else than computing a property value,
         * unwraps it since the iterators are not interested in properties. This unwrapping is
         * also necessary for allowing the builder to recognize the {@link BufferedImage} case.
         */
        private static RenderedImage unwrap(RenderedImage image) {
            while (image instanceof ImageAdapter) {
                image = ((ImageAdapter) image).source;
            }
            return image;
        }

        /**
         * If the given sample model is compatible with {@link BandedIterator}, returns the model scanline stride.
         * Otherwise returns 0. A {@code BandedIterator} can be used only if the returned value is greater than 0.
         */
        static int getScanlineStride(final SampleModel sm) {
            if (sm instanceof ComponentSampleModel) {
                final ComponentSampleModel csm = (ComponentSampleModel) sm;
                if (csm.getPixelStride() == 1) {
                    for (final int offset : csm.getBandOffsets()) {
                        if (offset != 0) return 0;
                    }
                    if (ArraysExt.isRange(0, csm.getBankIndices())) {
                        return csm.getScanlineStride();
                    }
                }
            } else if (sm instanceof SinglePixelPackedSampleModel) {
                final SinglePixelPackedSampleModel csm = (SinglePixelPackedSampleModel) sm;
                final int[] offsets = csm.getBitOffsets();
                if (offsets.length == 1 && offsets[0] == 0) {
                    return csm.getScanlineStride();
                }
            } else if (sm instanceof MultiPixelPackedSampleModel) {
                final MultiPixelPackedSampleModel csm = (MultiPixelPackedSampleModel) sm;
                if (csm.getDataBitOffset() == 0 && csm.getPixelBitStride() == DataBuffer.getDataTypeSize(csm.getDataType())) {
                    return csm.getScanlineStride();
                }
            }
            return 0;
        }

        /**
         * Creates a read-only iterator for the given raster.
         *
         * @param  data  the raster which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given raster.
         */
        public PixelIterator create(final Raster data) {
            final int scanlineStride = getScanlineStride(data.getSampleModel());
            if (scanlineStride > 0) {
                return new BandedIterator(data, null, subArea, window, order, scanlineStride);
            } else {
                return new PixelIterator(data, subArea, window, order);
            }
        }

        /**
         * Creates a read-only iterator for the given image.
         *
         * @param  data  the image which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given image.
         */
        public PixelIterator create(RenderedImage data) {
            data = unwrap(Objects.requireNonNull(data));
            /*
             * Note: Before Java 16, `BufferedImage.getTileGridXOffset()` and `getTileGridYOffset()` had a bug.
             * They should return `BufferedImage.getMinX()` (which is always 0) because the image contains only
             * one tile at index (0,0).  But they return `raster.getSampleModelTranslateX()` instead, which may
             * be non-zero if the image is a sub-region of another image.  Delegating to `create(Raster)` avoid
             * this problem in addition of being a slight optimization.
             *
             * Issue tracker: https://bugs.openjdk.java.net/browse/JDK-8166038
             */
            if (data instanceof BufferedImage) {
                return create(((BufferedImage) data).getRaster());
            }
            final int scanlineStride = getScanlineStride(data.getSampleModel());
            if (scanlineStride > 0) {
                return new BandedIterator(data, null, subArea, window, order, scanlineStride);
            } else {
                return new PixelIterator(data, subArea, window, order);
            }
        }

        /**
         * Creates a read/write iterator for the given raster.
         *
         * @param  data  the raster which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given raster.
         */
        public WritablePixelIterator createWritable(final WritableRaster data) {
            return createWritable(Objects.requireNonNull(data), data);
        }

        /**
         * Creates a read/write iterator for the given image.
         *
         * @param  data  the image which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given image.
         */
        public WritablePixelIterator createWritable(final WritableRenderedImage data) {
            if (Objects.requireNonNull(data) instanceof BufferedImage) {
                return createWritable(((BufferedImage) data).getRaster());
            }
            return createWritable(data, data);
        }

        /**
         * Creates an iterator which will read and write in two different rasters.
         * The two rasters must use the same sample model and have the same bounds.
         *
         * @param  input    the raster which contains the sample values to read.
         * @param  output   the raster where to write the sample values. Can be the same as {@code input}.
         * @return a new writable iterator.
         */
        public WritablePixelIterator createWritable(final Raster input, final WritableRaster output) {
            ArgumentChecks.ensureNonNull("input",  input);
            ArgumentChecks.ensureNonNull("output", output);
            final int scanlineStride = getScanlineStride(input.getSampleModel());
            if (scanlineStride > 0) {
                return new BandedIterator(input, output, subArea, window, order, scanlineStride);
            } else {
                return new WritablePixelIterator(input, output, subArea, window, order);
            }
        }

        /**
         * Creates an iterator which will read and write in two different images.
         * The two images must use the same sample model and have the same bounds.
         *
         * @param  input    the image which contains the sample values to read.
         * @param  output   the image where to write the sample values. Can be the same as {@code input}.
         * @return a new writable iterator.
         */
        public WritablePixelIterator createWritable(RenderedImage input, final WritableRenderedImage output) {
            ArgumentChecks.ensureNonNull("input",  input);
            ArgumentChecks.ensureNonNull("output", output);
            input = unwrap(input);
            final int scanlineStride = getScanlineStride(input.getSampleModel());
            if (scanlineStride > 0) {
                return new BandedIterator(input, output, subArea, window, order, scanlineStride);
            } else {
                return new WritablePixelIterator(input, output, subArea, window, order);
            }
        }
    }

    /**
     * Creates an iterator for all pixels in the given image.
     * This is a convenience method for {@code new Builder().create(data)}.
     *
     * @param  data  the image which contains the sample values on which to iterate.
     * @return a new iterator traversing all pixels in the given image, in arbitrary order.
     */
    public static PixelIterator create(final RenderedImage data) {
        return new Builder().create(data);
    }

    /**
     * Returns {@code true} if this iterator can write pixel values (after cast to {@code WritablePixelIterator}).
     * This method should be used instead of {@code instanceof} check because, for some implementations, being
     * an instance of {@code WritablePixelIterator} is not a sufficient condition.
     *
     * @return {@code true} if this iterator can safely be casted to {@link WritablePixelIterator} and used for
     *         writing pixel values.
     */
    public boolean isWritable() {
        return false;
    }

    /**
     * Returns the type used for storing data in the raster buffer.
     * The data type identifies the {@link DataBuffer} subclass used for storage.
     *
     * @return the type used for storing data in the raster buffer.
     *
     * @see SampleModel#getDataType()
     *
     * @since 1.2
     */
    public DataType getDataType() {
        return DataType.forDataBufferType(getSampleModel().getDataType());
    }

    /**
     * Returns the most efficient type ({@code int}, {@code float} or {@code double}) for transferring data between the
     * underlying rasters and this iterator. The transfer type is not necessarily the storage type used by the rasters.
     * For example, {@code int} values will be used for transferring data even if the underlying rasters store all sample
     * values as {@code byte}s.
     *
     * <p>The transfer type is only a hint since all iterator methods work for any type (conversions are applied as needed).
     * However if this method returns {@link TransferType#INT}, then {@link #getSample(int)} and {@link #getPixel(int[])}
     * will be slightly more efficient than equivalent methods for other types. Conversely if this method returns
     * {@link TransferType#DOUBLE}, then {@link #getSampleDouble(int)} will be both more efficient and avoid accuracy lost.</p>
     *
     * @return the most efficient data type for transferring data.
     *
     * @see SampleModel#getTransferType()
     */
    public TransferType<?> getTransferType() {
        return TransferType.valueOf(getSampleModel().getTransferType());
    }

    /**
     * Returns the sample model of the image or raster.
     */
    private SampleModel getSampleModel() {
        return (image != null) ? image.getSampleModel() : currentRaster.getSampleModel();
    }

    /**
     * Returns the range of sample values that can be stored in each band of the rendered image or raster.
     * The ranges depend on the data type (byte, integer, <i>etc.</i>) and the number of bits per sample.
     * If the samples are stored as floating point values, then the ranges are infinite (unbounded).
     *
     * <p>Usually, the range is the same for all bands. A situation where the ranges may differ is when an
     * image uses {@link java.awt.image.SinglePixelPackedSampleModel}, in which case the number of bits per
     * pixel may vary for different bands.</p>
     *
     * @return the ranges of valid sample values for each band. Ranges may be {@linkplain NumberRange#isBounded() unbounded}.
     */
    public NumberRange<?>[] getSampleRanges() {
        /*
         * Take the sample model of current tile if possible. This details should be irrelevant (so we do not mention
         * it in above javadoc) because the sample model shall be the same for all tiles. But if inconsistency happens,
         * stay consistent at least with the fact that all getter methods in PixelIterator return information relative
         * to current iterator position.
         */
        final SampleModel model = getSampleModel();
        final NumberRange<?>[] ranges = new NumberRange<?>[model.getNumBands()];
        if (ranges.length != 0) {
            final int dataType = model.getDataType();
            if (ImageUtilities.isIntegerType(dataType)) {
                int bandToDefine = 0, lastDefinedBand;
                do {
                    final int size = model.getSampleSize(bandToDefine);
                    long minimum = 0;
                    long maximum = Numerics.bitmask(size) - 1;
                    if (!ImageUtilities.isUnsignedType(model)) {
                        maximum >>>= 1;                                 // Convert unsigned range to signed range.
                        minimum = ~maximum;
                    }
                    final NumberRange<?> range = (dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_SHORT)
                                               ? NumberRange.create((short) minimum, true, (short) maximum, true)
                                               : NumberRange.create((int)   minimum, true, (int)   maximum, true);
                    ranges[bandToDefine] = range;
                    /*
                     * Usually all bands have the same number of bits, and consequently the same range of values.
                     * For handling this common case, loop below shares the same `NumberRange` instance with all
                     * bands having same characteristic. If at least one band has a different number of bits, the
                     * `bandToDefine` index will point to the first occurrence and the computation is repeated.
                     */
                    lastDefinedBand = bandToDefine;
                    for (int band = ranges.length; --band > lastDefinedBand;) {
                        if (ranges[band] == null) {
                            if (model.getSampleSize(band) == size) {
                                ranges[band] = range;
                            } else {
                                bandToDefine = band;
                            }
                        }
                    }
                } while (bandToDefine > lastDefinedBand);
            } else {
                Arrays.fill(ranges, (dataType == DataBuffer.TYPE_FLOAT)
                        ? NumberRange.create(Float. NEGATIVE_INFINITY, false, Float. POSITIVE_INFINITY, false)
                        : NumberRange.create(Double.NEGATIVE_INFINITY, false, Double.POSITIVE_INFINITY, false));
            }
        }
        return ranges;
    }

    /**
     * Returns the order in which pixels are traversed. {@link SequenceType#LINEAR} means that pixels on the first
     * row are traversed from left to right, then pixels on the second row from left to right, <i>etc.</i>
     * An empty value means that the iteration order is unspecified.
     *
     * @return order in which pixels are traversed.
     */
    public Optional<SequenceType> getIterationOrder() {
        if (isDefaultOrder && (tileUpperX - tileLowerX) > 1) {
            return Optional.empty();                                // Undefined iteration order.
        } else {
            return Optional.of(SequenceType.LINEAR);
        }
    }

    /**
     * Returns the number of bands (samples per pixel) in the image or raster.
     *
     * @return number of bands.
     */
    public int getNumBands() {
        return numBands;
    }

    /**
     * Returns the pixel coordinates of the region where this iterator is doing the iteration.
     * If no region was specified at construction time, then this method returns the image or raster bounds.
     *
     * @return pixel coordinates of the iteration region.
     */
    public Rectangle getDomain() {
        return new Rectangle(lowerX, lowerY, upperX - lowerX, upperY - lowerY);
    }

    /**
     * Returns the column (x) and row (y) indices of the current pixel.
     * The {@link #next()} or {@link #moveTo(int,int)} method must have been invoked before this method.
     * Indices of the first pixel are not necessarily zero; they can even be negative.
     *
     * @return column and row indices of current iterator position.
     * @throws IllegalStateException if this method is invoked before the first call to {@link #next()}
     *         or {@link #moveTo(int,int)}, or after {@code next()} returned {@code false}.
     */
    public Point getPosition() {
        final short message;
        if (x < lowerX) {
            message = Resources.Keys.IterationNotStarted;
        } else if (tileY >= tileUpperY) {
            message = Resources.Keys.IterationIsFinished;
        } else {
            return new Point(x,y);
        }
        throw new IllegalStateException(Resources.format(message));
    }

    /**
     * Moves the pixel iterator to the given column (x) and row (y) indices. After this method invocation,
     * the iterator state is as if the {@link #next()} method has been invoked just before to reach the
     * specified position.
     *
     * <h4>Usage example</h4>
     * {@snippet lang="java" :
     *     iterator.moveTo(x, y);
     *     do {
     *         int sample = iterator.getSample(band);
     *         // Use sample value here...
     *     } while (iterator.next());
     *     }
     *
     * @param  px  the column index of the pixel to make current.
     * @param  py  the row index of the pixel to make current.
     * @throws IndexOutOfBoundsException if the given indices are outside the iteration domain.
     */
    public void moveTo(final int px, final int py) {
        if (px < lowerX || px >= upperX  ||  py < lowerY || py >= upperY) {
            throw new IndexOutOfBoundsException(Resources.format(Resources.Keys.OutOfIteratorDomain_2, px, py));
        }
        if (image != null) {
            final int tx = Math.floorDiv(px - tileGridXOffset, tileWidth);
            final int ty = Math.floorDiv(py - tileGridYOffset, tileHeight);
            if (tx != tileX || ty != tileY) {
                releaseTile();                                      // Release current writable raster, if any.
                tileX = tx;
                tileY = ty;
                int currentLowerY = fetchTile();                    // Also update `currentLowerX` field value.
                if (currentLowerY > py || py >= currentUpperY ||
                    currentLowerX > px || px >= currentUpperX)
                {
                    throw incompatibleTile();
                }
            }
        }
        x = px;
        y = py;
    }

    /**
     * Moves the iterator to the next pixel. A pixel iterator is initially positioned before the first pixel.
     * The first call to {@code next()} makes the first pixel the current one; the second call makes the second
     * pixel the current one, <i>etc.</i> The second pixel is not necessarily on the same row as the first one;
     * iteration order is implementation dependent.
     *
     * <p>When a call to {@code next()} returns {@code false}, the iterator is positioned after the last pixel.
     * Any invocation of a {@code getSample(int)} method will result in a {@link NoSuchElementException} to be
     * thrown.</p>
     *
     * @return {@code true} if the current pixel is valid, or {@code false} if there are no more pixels.
     * @throws IllegalStateException if this iterator already reached end of iteration in a previous call
     *         to {@code next()}, and {@link #rewind()} or {@link #moveTo(int,int)} have not been invoked.
     */
    public boolean next() {
        /*
         * Current implementation supports two iteration orders: default and SequenceType.LINEAR.
         * They are the two most frequent orders and have in common an iteration over x values of
         * current tile before to make any decision. It is reasonably cheap to implement them in
         * the same method because the cost of checking for iteration order happens only once per
         * tile row (instead of at every pixel). Providing the two implementations here makes
         * easier for subclasses such as `BandedIterator` to support those two iteration orders.
         *
         * All other iteration orders (Cantor, Morton, Hilbert, etc.) should have a dedicated
         * class overriding this method. We do not intent to support all iteration order here.
         */
        if (++x >= currentUpperX) {
            if (isDefaultOrder) {
                if (++y >= currentUpperY) {             // Strict equality (==) would work, but use >= as a safety.
                    releaseTile();                      // Release current writable raster, if any.
                    if (++tileX >= tileUpperX) {        // Strict equality (==) would work, but use >= as a safety.
                        if (++tileY >= tileUpperY) {
                            endOfIteration();
                            return false;
                        }
                        tileX = tileLowerX;
                    }
                    y = fetchTile();
                }
                x = currentLowerX;
            } else {
                /*
                 * SequenceType.LINEAR iteration order: before to move to next row, verify if there is
                 * more tiles to traverse on the right side.
                 */
                releaseTile();                          // Must be invoked before (tileX, tileY) change.
                if (x < upperX) {
                    tileX++;
                } else {
                    if (++y >= currentUpperY) {         // Move to next line only after full image row.
                        if (++tileY >= tileUpperY) {
                            endOfIteration();
                            return false;
                        }
                    }
                    tileX = tileLowerX;
                    x = lowerX;                         // Beginning of next row.
                }
                /*
                 * At this point the (x,y) pixel coordinates have been updated and are inside the domain of validity.
                 * We need to change tile, either because we moved to the tile on the right or because we started a
                 * new row (in which case we need to move to the leftmost tile).
                 */
                if (fetchTile() > y) {
                    throw incompatibleTile();
                }
            }
            changedRowOrTile();
        }
        return true;
    }

    /**
     * Invoked by the default {@link #next()} implementation when the iterator moved to a new row or a new tile.
     * Subclasses can override for updating some <var>y</var>-dependent cached values.
     *
     * <p>Note that this method is not invoked by {@link #moveTo(int, int)} for performance reason.
     * Subclasses can get equivalent functionality by overriding {@code moveTo(…)} and checking
     * {@code isSameRowAndTile(px, py)}.</p>
     */
    void changedRowOrTile() {
    }

    /**
     * Returns whether given position is on the same row and same tile as current (x,y) position.
     * This method is provided as a complement to {@link #changedRowOrTile()}.
     */
    final boolean isSameRowAndTile(final int px, final int py) {
        return (py == y) && px >= currentLowerX && px < currentUpperX;
    }

    /**
     * Returns the exception to throw when the tile at index ({@link #tileX}, {@link #tileY})
     * uses an incompatible sample model.
     */
    private RasterFormatException incompatibleTile() {
        String message = Resources.format(Resources.Keys.IncompatibleTile_2, tileX, tileY);
        if (image instanceof PlanarImage) {
            final String inconsistency = ((PlanarImage) image).verify();
            if (inconsistency != null) {
                message = message + ' ' + Messages.format(Messages.Keys.PossibleInconsistency_1, inconsistency);
            }
        }
        return new RasterFormatException(message);
    }

    /**
     * Verifies if the width or height of a tile fetched by {@link #fetchTile()} is valid.
     * The tile size must be strictly equal to the expected size, except if the tile is the
     * last one in a row or column in which case the tile is allowed to be smaller.
     *
     * @param  actual    the width or height of fetched tile.
     * @param  expected  the expected tile size, either {@link #tileWidth} or {@link #tileHeight}.
     * @param  isLast    whether the tile is in the last column (if checking width) or last row.
     * @return whether the tile has a valid width or height.
     */
    private static boolean isValidTileSize(final int actual, final int expected, final boolean isLast) {
        return isLast ? (actual >= 0 && actual <= expected) : actual == expected;
    }

    /**
     * Fetches from the image a tile for the current {@link #tileX} and {@link #tileY} coordinates.
     * All fields prefixed by {@code current} are updated by this method. The caller is responsible
     * for updating the {@link #x} and {@link #y} fields.
     *
     * <p>Note 1: {@link #releaseTile()} is always invoked before this method.
     * Consequently, {@link #currentRaster} is already {@code null}.</p>
     *
     * <p>Note 2: there is no {@code currentLowerY} field in this {@code PixelIterator} class.
     * Instead, that value is returned by this method.</p>
     *
     * @return the {@link #y} value of the first row of new tile.
     */
    private int fetchTile() {
        Raster tile = fetchWritableTile();
        if (tile == null) {
            tile = image.getTile(tileX, tileY);
        }
        final Rectangle bounds = tile.getBounds();
        if (!(tile.getNumBands() == numBands
                && isValidTileSize(bounds.width,  tileWidth,  tileX == tileUpperX - 1)
                && isValidTileSize(bounds.height, tileHeight, tileY == tileUpperY - 1)))
        {
            throw incompatibleTile();
        }
        windowLimitX   = Math.addExact(bounds.x, bounds.width);
        windowLimitY   = Math.addExact(bounds.y, bounds.height);
        currentUpperX  = Math.min(upperX, windowLimitX);
        currentUpperY  = Math.min(upperY, windowLimitY);
        currentLowerX  = Math.max(lowerX, bounds.x);
        currentRaster  = tile;
        acquiredTile(tile);
        return Math.max(lowerY, bounds.y);
    }

    /**
     * Fetches from the writable image a tile for the current {@link #tileX} and {@link #tileY} coordinates.
     * If the writable tile is the same tile as the one used for read operation, then that tile should be
     * returned. This method is for {@link #fetchTile()} internal usage only and should be implemented by
     * {@link WritablePixelIterator} only.
     *
     * @return a tile that can be used for <em>read</em> operation, or {@code null} if none.
     *         This value shall be non-null only if the tile to write is the same as the tile to read.
     */
    Raster fetchWritableTile() {
        return null;
    }

    /**
     * Invoked when the iterator fetched a new tile. This is a hook for subclasses.
     * The default implementation does nothing. This is overridden when a subclass
     * needs to store additional raster properties, for example its buffer for more
     * direct access to sample values.
     *
     * @param  tile  the new tile from which to read sample values.
     */
    void acquiredTile(Raster tile) {
    }

    /**
     * Releases the tiles acquired by this iterator, if any.
     */
    void releaseTile() {
        if (image != null) {
            currentRaster = null;
        }
    }

    /**
     * Invoked when a call to {@link #next()} moved to the end of iteration. This method sets fields to values
     * that will allow {@link #moveTo(int,int)} and {@link #next()} to detect that we already finished iteration.
     *
     * <p>Note: {@link #releaseTile()} is always invoked before this method.
     * Consequently, {@link #currentRaster} is already {@code null}.</p>
     */
    private void endOfIteration() {
        /*
         * The `tileY` value is used for checking if next() is invoked again, in order to avoid a
         * common misuse pattern. In principle `tileY` needs to be compared only to `tileUpperY`,
         * but we also compare to `tileLowerY + 1` for handling the empty iterator case.
         */
        final boolean error = tileY > Math.max(tileUpperY, tileLowerY + 1);
        /*
         * Paranoiac safety: keep the x, y and tileX variables before their limits
         * in order to avoid overflow in the `if (++foo >= limit)` statements.
         */
        x =  currentUpperX - 1;
        y =  currentUpperY - 1;
        tileX = tileUpperX - 1;
        tileY = tileUpperY;             // Sentinel value for detecting following error condition.
        if (error) {
            throw new IllegalStateException(Resources.format(Resources.Keys.IterationIsFinished));
        }
    }

    /**
     * Returns the sample value in the specified band of current pixel, rounded toward zero.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getSample(int)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  band  the band for which to get the sample value.
     * @return sample value in specified band of current pixel.
     *
     * @see Raster#getSample(int, int, int)
     */
    public int getSample(final int band) {
        return currentRaster.getSample(x, y, band);
    }

    /**
     * Returns the sample value in the specified band of current pixel as a single-precision floating point number.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getSampleFloat(int)} method is invoked. If above condition is
     * not met, then this method behavior is undefined: it may throw any runtime exception or return a meaningless
     * value (there is no explicit bounds check for performance reasons).
     *
     * @param  band  the band for which to get the sample value.
     * @return sample value in specified band of current pixel.
     *
     * @see Raster#getSampleFloat(int, int, int)
     */
    public float getSampleFloat(final int band) {
        return currentRaster.getSampleFloat(x, y, band);
    }

    /**
     * Returns the sample value in the specified band of current pixel, without precision lost.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getSampleDouble(int)} method is invoked. If above condition is
     * not met, then this method behavior is undefined: it may throw any runtime exception or return a meaningless
     * value (there is no explicit bounds check for performance reasons).
     *
     * @param  band  the band for which to get the sample value.
     * @return sample value in specified band of current pixel.
     *
     * @see Raster#getSampleDouble(int, int, int)
     */
    public double getSampleDouble(final int band) {
        return currentRaster.getSampleDouble(x, y, band);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  dest  a pre-allocated array where to store the sample values, or {@code null} if none.
     * @return the sample values for current pixel.
     *
     * @see Raster#getPixel(int, int, int[])
     */
    public int[] getPixel(final int[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  dest  a pre-allocated array where to store the sample values, or {@code null} if none.
     * @return the sample values for current pixel.
     *
     * @see Raster#getPixel(int, int, float[])
     */
    public float[] getPixel(final float[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  dest  a pre-allocated array where to store the sample values, or {@code null} if none.
     * @return the sample values for current pixel.
     *
     * @see Raster#getPixel(int, int, double[])
     */
    public double[] getPixel(final double[] dest) {
        return currentRaster.getPixel(x, y, dest);
    }

    /**
     * Returns the data elements (not necessarily band values) of current pixel.
     * The {@code Object} argument and return value is a relatively opaque format (it may be {@code int[]},
     * {@code byte[]}, <i>etc.</i>): it is used for transferring values in a packed format between compatible
     * Java2D sample or color models. That {@code Object} should generally not be used directly by the caller.
     *
     * <p>Data elements are useful for copying values in another image using the same sample model,
     * or for getting colors with a call to {@link java.awt.image.ColorModel#getRGB(Object)}.</p>
     *
     * <h4>Example</h4>
     * If an image has Red, Green, Blue and Alpha bands, then the {@link #getPixel(int[])} methods will return
     * arrays of length 4 containing the individual values for each band, no matter how those bands are stored
     * in the image. By contrast this {@code getDataElements(…)} method may return an array of length 1 with
     * all sample values packed as a single ARGB value.
     *
     * @param  dest  a pre-allocated array where to store the data elements, or {@code null} if none.
     * @return the data elements for current pixel.
     *
     * @see Raster#getDataElements(int, int, Object)
     *
     * @since 1.1
     */
    public Object getDataElements(final Object dest) {
        return currentRaster.getDataElements(x, y, dest);
    }

    /**
     * Returns a moving window over the sample values in a rectangular region starting at iterator position.
     * The <var>window size</var> must have been specified at {@code PixelIterator} construction time.
     * The current iterator position is the window corner having the smallest <var>x</var> and <var>y</var> coordinates.
     * This is typically, but not necessarily (depending on axis orientations) the window upper-left corner.
     * Sample values are stored in a sequence of length
     * <var>(number of bands)</var> × <var>(window width)</var> × <var>(window height)</var>.
     * Values are always stored with band index varying fastest, then column index, then row index.
     * Columns are traversed from left to right and rows are traversed from top to bottom
     * ({@link SequenceType#LINEAR} iteration order).
     * That order is the same regardless the {@linkplain #getIterationOrder() iteration order} of this iterator.
     *
     * <div class="note"><b>Example:</b>
     * for an RGB image, the 3 first values are the red, green and blue components of the pixel at
     * {@linkplain #getPosition() current iterator position}. The 3 next values are the red, green
     * and blue components of the pixel at the right of current iterator position (not necessarily
     * the position where a call to {@link #next()} would have go), <i>etc.</i></div>
     *
     * Calls to {@link #next()} or {@link #moveTo(int,int)} followed by {@link Window#update()}
     * replaces the window content with values starting at the new iterator position.
     * Before the first {@link Window#update()} invocation, the window is filled with zero values.
     *
     * <p>If this iterator is used for
     * {@linkplain WritablePixelIterator#setPixel(int[]) writing pixel values at current position},
     * those write operations may change the content of windows at {@linkplain #next() next positions}
     * unless the iteration order of this iterator is {@link SequenceType#LINEAR}.</p>
     *
     * <h4>Usage example</h4>
     * following code creates an iterator over the full area of given image, then a window of 5×5 pixels.
     * The window is moved over all the image area in iteration order. Inside the window, data are copied
     * in {@linkplain SequenceType#LINEAR linear order} regardless the iteration order.
     *
     * {@snippet lang="java" :
     *     PixelIterator it = create(image, null, new Dimension(5, 5), null);     // Windows size will be 5×5 pixels.
     *     PixelIterator<FloatBuffer> window = it.createWindow(TransferType.FLOAT);
     *     FloatBuffer values = window.values;
     *     while (it.next()) {
     *         window.update();
     *         while (buffer.hasRemaining()) {
     *             float sample = buffer.get();
     *             // use the sample value here.
     *         }
     *     }
     *     }
     *
     * @param  <T>   the type of the data buffer to use for transferring data.
     * @param  type  the desired type of values ({@code int}, {@code float} or {@code double}).
     *               Use {@link #getTransferType()} if the most efficient type is desired.
     * @return a window over the sample values in the underlying image or raster.
     *
     * @see Raster#getPixels(int, int, int, int, double[])
     */
    @SuppressWarnings("unchecked")
    public <T extends Buffer> Window<T> createWindow(final TransferType<T> type) {
        // Implicit null value check below.
        final int length = numBands * windowWidth * windowHeight;
        // `transfer` array needs one row or one column less than `data`.
        final int transferLength = length - numBands * Math.min(windowWidth, windowHeight);
        final Window<?> window;
        switch (type.dataBufferType) {
            case DataBuffer.TYPE_INT:    window = new IntWindow(new int   [length], new int   [transferLength]); break;
            case DataBuffer.TYPE_FLOAT:  window =  createWindow(new float [length], new float [transferLength]); break;
            case DataBuffer.TYPE_DOUBLE: window =  createWindow(new double[length], new double[transferLength]); break;
            default: throw new AssertionError(type);  // Should never happen unless we updated TransferType and forgot to update this method.
        }
        return (Window<T>) window;
    }

    /**
     * Creates a window for floating point values using the given arrays. This is a hook for allowing subclasses
     * to specify alternative implementations. We provide hooks only for floating point types, not for integers,
     * because the {@code int} type is already optimized by Java2D with specialized {@code Raster.getPixels(…)}
     * method implementations. By contrast the {@code float} and {@code double} types in Java2D use generic and
     * slower code paths.
     */
    Window<FloatBuffer>  createWindow( float[] data,  float[] transfer) {return new  FloatWindow(data, transfer);}
    Window<DoubleBuffer> createWindow(double[] data, double[] transfer) {return new DoubleWindow(data, transfer);}

    /**
     * Contains the sample values in a moving window over the image. Windows are created by calls to
     * {@link PixelIterator#createWindow(TransferType)} and sample values are stored in {@link Buffer}s.
     * The buffer content is replaced ever time {@link #update()} is invoked.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     *
     * @param  <T>  the type of buffer which can be used for transferring data.
     *
     * @since 0.8
     */
    public abstract static class Window<T extends Buffer> {
        /**
         * Enumeration values for the last argument in {@link #getPixels(Raster, int, int, int, int, int)}.
         * <ul>
         *   <li>{@code DIRECT}: store sample values directly in the final destination array.</li>
         *   <li>{@code TRANSFER}: store sample values in a temporary buffer (copied to destination by caller).</li>
         *   <li>{@code TRANSFER_FROM_OTHER}: same as {@code TRANSFER}, but also notify that the given raster is not
         *       {@link PixelIterator#currentRaster}.</li>
         * </ul>
         */
        static final int DIRECT = 0, TRANSFER = 1, TRANSFER_FROM_OTHER = 2;

        /**
         * A buffer containing all sample values fetched by the last call to {@link #update()}. The buffer
         * capacity is <var>(number of bands)</var> × <var>(window width)</var> × <var>(window height)</var>.
         * Values are always stored with band index varying fastest, then column index, then row index.
         * Columns are traversed from left to right and rows are traversed from top to bottom
         * ({@link SequenceType#LINEAR} iteration order).
         * That order is the same regardless the {@linkplain PixelIterator#getIterationOrder() iteration order}
         * of enclosing iterator.
         *
         * <p>Every time that {@link #update()} is invoked, the buffer content is replaced by sample values
         * starting at the {@linkplain PixelIterator#getPosition() current iterator position}.
         * Before the first {@code update()} invocation, the buffer is filled with zero values.</p>
         */
        public final T values;

        /**
         * Creates a new window which will store the sample values in the given buffer.
         */
        Window(final T buffer) {
            values = buffer;
        }

        /**
         * Returns the iterator that created this window.
         */
        abstract PixelIterator owner();

        /**
         * Returns the width and height of this window in pixels.
         *
         * @return the window size in pixels.
         *
         * @since 1.1
         */
        public final Dimension getSize() {
            final PixelIterator it = owner();
            return new Dimension(it.windowWidth, it.windowHeight);
        }

        /**
         * Updates this window with the sample values in the region starting at current iterator position.
         * The buffer position, limit and mark are {@linkplain Buffer#clear() cleared}.
         *
         * <p>The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
         * been invoked successfully, before this {@code update()} method is invoked. If above condition is not met,
         * then this method behavior is undefined: it may throw any runtime exception or return meaningless values
         * (there is no explicit bounds check for performance reasons).</p>
         */
        public abstract void update();

        /**
         * Returns an array containing all samples for a rectangle of pixels in the given raster, one sample
         * per array element. Subclasses should delegate to one of the {@code Raster#getPixels(…)} methods
         * depending on the buffer data type.
         *
         * <h4>Constraints</h4>
         * {@code subWidth} and {@code subHeight} shall always be greater than zero.
         *
         * @param  raster     the raster from which to get the pixel values.
         * @param  subX       the X coordinate of the upper-left pixel location.
         * @param  subY       the Y coordinate of the upper-left pixel location.
         * @param  subWidth   width of the pixel rectangle.
         * @param  subHeight  height of the pixel rectangle.
         * @param  mode       one of {@link #DIRECT}, {@link #TRANSFER} or {@link #TRANSFER_FROM_OTHER}.
         * @return the array in which sample values have been stored.
         */
        abstract Object getPixels(Raster raster, int subX, int subY, int subWidth, int subHeight, int mode);
    }

    /**
     * {@link Window} implementation backed by an array of {@code int[]}. This is the most efficient
     * {@code Window} because Java2D has many optimizations for images backed by integer values:
     *
     * <ul>
     *   <li>{@link Raster#getPixels(int, int, int, int, int[])} overridden in private subclasses.</li>
     *   <li>{@link SampleModel#getPixels(int, int, int, int, int[], DataBuffer)} overridden in public subclasses.</li>
     * </ul>
     *
     * In particular we should not try to get the backing {@code int[]} array ourselves
     * because it may cause Java2D to disable GPU accelerations on that raster.
     */
    private final class IntWindow extends Window<IntBuffer> {
        /**
         * Sample values in the window ({@code data}) and a temporary array ({@code transfer}).
         * Those arrays are overwritten when {@link #update()} is invoked.
         */
        private final int[] data, transfer;

        /**
         * Creates a new window which will store the sample values in the given {@code data} array.
         */
        IntWindow(final int[] data, final int[] transfer) {
            super(IntBuffer.wrap(data).asReadOnlyBuffer());
            this.data = data;
            this.transfer = transfer;
        }

        /**
         * Returns the iterator that created this window.
         */
        @Override
        final PixelIterator owner() {
            return PixelIterator.this;
        }

        /**
         * Performs the transfer between the underlying raster and this window.
         */
        @Override
        Object getPixels(Raster raster, int subX, int subY, int subWidth, int subHeight, int mode) {
            return raster.getPixels(subX, subY, subWidth, subHeight, (mode == DIRECT) ? data : transfer);
        }

        /**
         * Updates this window with the sample values in the region starting at current iterator position.
         * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
         */
        @Override
        public void update() {
            values.clear();
            fetchValues(this, data);
        }
    }

    /**
     * {@link Window} implementation backed by an array of {@code float[]}.
     * This implementation is provided for completeness but is rarely used.
     * We do not attempt performance optimization for this case.
     */
    private final class FloatWindow extends Window<FloatBuffer> {
        /**
         * Sample values in the window ({@code data}) and a temporary array ({@code transfer}).
         * Those arrays are overwritten when {@link #update()} is invoked.
         */
        private final float[] data, transfer;

        /**
         * Creates a new window which will store the sample values in the given {@code data} array.
         */
        FloatWindow(final float[] data, final float[] transfer) {
            super(FloatBuffer.wrap(data).asReadOnlyBuffer());
            this.data = data;
            this.transfer = transfer;
        }

        /**
         * Returns the iterator that created this window.
         */
        @Override
        final PixelIterator owner() {
            return PixelIterator.this;
        }

        /**
         * Performs the transfer between the underlying raster and this window.
         */
        @Override
        Object getPixels(Raster raster, int subX, int subY, int subWidth, int subHeight, int mode) {
            return raster.getPixels(subX, subY, subWidth, subHeight, (mode == DIRECT) ? data : transfer);
        }

        /**
         * Updates this window with the sample values in the region starting at current iterator position.
         * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
         */
        @Override
        public void update() {
            values.clear();
            fetchValues(this, data);
        }
    }

    /**
     * {@link Window} implementation backed by an array of {@code double[]}.
     * This is the implementation used by Apache SIS for most computations.
     *
     * <h2>Performance note</h2>
     * Java2D has numerous optimizations for the integer cases, with no equivalent for the floating point cases.
     * Consequently, if the data buffer is known to use some integer type, it is faster to get integer values and
     * convert them to {@code double} values instead of to request directly floating-point values. However, the
     * improvement is not as much as using {@link BandedIterator} as least for small windows. For that reason,
     * we do not provide the "integers converted to doubles" performance workaround for now. Even if we provided
     * it, this {@code DoubleWindow} would still be necessary for the general case (non-integer data buffers).
     */
    private final class DoubleWindow extends Window<DoubleBuffer> {
        /**
         * Sample values in the window ({@code data}) and a temporary array ({@code transfer}).
         * Those arrays are overwritten when {@link #update()} is invoked.
         */
        private final double[] data, transfer;

        /**
         * Creates a new window which will store the sample values in the given {@code data} array.
         */
        DoubleWindow(final double[] data, final double[] transfer) {
            super(DoubleBuffer.wrap(data).asReadOnlyBuffer());
            this.data = data;
            this.transfer = transfer;
        }

        /**
         * Returns the iterator that created this window.
         */
        @Override
        final PixelIterator owner() {
            return PixelIterator.this;
        }

        /**
         * Performs the transfer between the underlying raster and this window.
         */
        @Override
        Object getPixels(Raster raster, int subX, int subY, int subWidth, int subHeight, int mode) {
            return raster.getPixels(subX, subY, subWidth, subHeight, (mode == DIRECT) ? data : transfer);
        }

        /**
         * Updates this window with the sample values in the region starting at current iterator position.
         * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
         */
        @Override
        public void update() {
            values.clear();
            fetchValues(this, data);
        }
    }

    /**
     * Updates the content of given window with the sample values in the region starting at current iterator position.
     *
     * <h4>Performance note</h4>
     * We could store the position of last update in the {@code Window} object and invoke {@code getPixels(…)}
     * only for window area that changed. Sample values that are still inside the window could be moved with
     * {@code System.arraycopy(…)}. We tried that approach, but performance at least on small windows was worst
     * than current naive implementation.
     *
     * @param  window  the window to update.
     * @param  data    the array of primitive type where sample values are stored.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    final void fetchValues(final Window<?> window, final Object data) {
        int     subEndX   = windowLimitX - x;
        int     subEndY   = windowLimitY - y;
        int     subWidth  = Math.min(windowWidth,  subEndX);
        int     subHeight = Math.min(windowHeight, subEndY);
        boolean fullWidth = (subWidth == windowWidth);
        if (fullWidth && subHeight == windowHeight) {
            /*
             * Optimization for the case where the full window is inside current raster.
             * This is the vast majority of cases, so we perform this check soon before
             * to compute more local variables.
             */
            final Object transfer = window.getPixels(currentRaster, x, y, subWidth, subHeight, Window.DIRECT);
            assert transfer == data;
            return;
        }
        /*
         * At this point, we determined that the window is overlapping two or more tiles.
         * We will need more variables for iterating over the tiles around `currentRaster`.
         */
        Raster raster    = currentRaster;
        int mode         = Window.TRANSFER;
        int destOffset   = 0;                       // Index in `window` array where to copy the sample values.
        int subX         = 0;                       // Upper-left corner of a sub-window inside the window.
        int subY         = 0;
        int tileSubX     = tileX;                   // The tile where is located the (subX, subY) coordinate.
        int tileSubY     = tileY;
        final int stride = windowWidth * numBands;  // Number of samples between two rows in the `windows` array.
        final int rewind = subEndX;
        for (;;) {
            if (subWidth > 0 && subHeight > 0) {
                final Object transfer = window.getPixels(raster, x + subX, y + subY, subWidth, subHeight, mode);
                if (fullWidth) {
                    System.arraycopy(transfer, 0, data, destOffset, stride * subHeight);
                } else {
                    final int  rowLength = numBands  * subWidth;
                    final int fullLength = rowLength * subHeight;
                    for (int srcOffset=0; srcOffset < fullLength; srcOffset += rowLength) {
                        System.arraycopy(transfer, srcOffset, data, destOffset, rowLength);
                        destOffset += stride;
                    }
                }
            }
            /*
             * At this point, we copied all sample values that we could obtain from the current tile.
             * Move to the next tile on current row, or if we reached the end of row move to the next row.
             */
            if (subEndX < windowWidth) {
                subX     = subEndX;
                subEndX += tileWidth;                       // Next tile on the same row.
                tileSubX++;
            } else {
                if (subEndY >= windowHeight) {
                    return;                                 // Completed last row of tiles.
                }
                subY     = subEndY;
                subEndY += tileHeight;                      // Tile on the next row.
                tileSubY++;
                tileSubX = tileX;
                subEndX  = rewind;
                subX     = 0;                               // Move x position back to the window left border.
            }
            mode       = Window.TRANSFER_FROM_OTHER;
            raster     = image.getTile(tileSubX, tileSubY);
            destOffset = (subY * windowWidth + subX) * numBands;
            subWidth   = Math.min(windowWidth,  subEndX) - subX;
            subHeight  = Math.min(windowHeight, subEndY) - subY;
            fullWidth  = (subWidth == windowWidth);
        }
    }

    /**
     * Restores the iterator to the start position. After this method has been invoked,
     * the iterator is in the same state as after construction.
     */
    public void rewind() {
        releaseTile();                  // Release current writable raster, if any.
        if (image == null) {
            tileX = 0;
            tileY = 0;
        } else {
            tileX = tileLowerX - 1;     // Note: no need for decrementExact(…) because already checked by constructor.
            tileY = tileLowerY;
            currentLowerX = lowerX;
            currentUpperX = lowerX;     // Really `lower`, so the position is the tile before the first tile.
            currentUpperY = lowerY;
        }
        x = lowerX - 1;                 // Set to the position before first pixel.
        y = lowerY;
        changedRowOrTile();
    }
}
