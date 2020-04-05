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
package org.apache.sis.internal.coverage.j2d;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.TileObserver;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.measure.NumberRange;


/**
 * An image where each sample value is computed independently of other sample values and independently
 * of neighbor points. Values are computed by a separated {@link MathTransform1D} for each band
 * (by contrast, an {@code InterleavedSampleConverter} would handle all sample values as a coordinate tuple).
 * Current implementation makes the following simplifications:
 *
 * <ul>
 *   <li>The image has exactly one source.</li>
 *   <li>Image layout (minimum coordinates, image size, tile grid) is the same than source image layout,
 *     unless the source has too large tiles in which case {@link ImageLayout} automatically subdivides
 *     the tile grid in smaller tiles.</li>
 *   <li>Image is computed and stored on a band-by-band basis using a {@link BandedSampleModel}.</li>
 *   <li>Calculation is performed on {@code float} or {@code double} numbers.</li>
 * </ul>
 *
 * If the given source is writable and the transform are invertible, then the {@code BandedSampleConverter}
 * returned by the {@link #create create(…)} method will implement {@link WritableRenderedImage} interface.
 * In such case, writing converted values will cause the corresponding source values to be updated too.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class BandedSampleConverter extends ComputedImage {
    /**
     * The transfer functions to apply on each band of the source image.
     */
    private final MathTransform1D[] converters;

    /**
     * The color model for the expected range of values. May be {@code null}.
     */
    private final ColorModel colorModel;

    /**
     * The sample resolutions, or {@code null} if unknown.
     */
    private final double[] sampleResolutions;

    /**
     * Creates a new image which will compute values using the given converters.
     *
     * @param  source       the image for which to convert sample values.
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  colorModel   the color model for the expected range of values, or {@code null}.
     * @param  ranges       the expected range of values for each band, or {@code null} if unknown.
     * @param  converters   the transfer functions to apply on each band of the source image.
     */
    private BandedSampleConverter(final RenderedImage source,  final BandedSampleModel sampleModel,
                                  final ColorModel colorModel, final NumberRange<?>[] ranges,
                                  final MathTransform1D[] converters)
    {
        super(sampleModel, source);
        this.colorModel = colorModel;
        this.converters = converters;
        /*
         * Get an estimation of the resolution, arbitrarily looking in the middle of the range of values.
         * If the converters are linear (which is the most common case), the middle value does not matter
         * except if it falls on a "no data" value.
         */
        boolean hasResolutions = false;
        final double[] resolutions = new double[converters.length];
        for (int i=0; i<resolutions.length; i++) {
            /*
             * Get the sample value in the middle of the range of valid values for the current band.
             * If no range was explicitly given, use the approximate average of all possible values.
             */
            double middle = Double.NaN;
            if (ranges != null) {
                final NumberRange<?> range = ranges[i];
                if (range != null) {
                    middle = (range.getMinDouble() + range.getMaxDouble()) / 2;
                }
            }
            if (!Double.isFinite(middle)) {
                switch (ImageUtilities.getDataType(source)) {
                    default:                     middle = 0;      break;
                    case DataBuffer.TYPE_BYTE:   middle = 0x80;   break;
                    case DataBuffer.TYPE_USHORT: middle = 0x8000; break;
                }
            }
            /*
             * Get the derivative in the middle value, which is constant everywhere
             * in the common case of a linear transform.
             */
            final MathTransform1D c = converters[i];
            double r;
            try {
                r = c.derivative(middle);
                if (!Double.isFinite(r)) {
                    r = c.derivative(1);                // Second attempt if the middle value didn't work.
                }
            } catch (TransformException e) {
                r = Double.NaN;
            }
            resolutions[i] = r;
            hasResolutions |= Double.isFinite(r);
        }
        sampleResolutions = hasResolutions ? resolutions : null;
    }

    /**
     * Creates a new image of the given data type which will compute values using the given converters.
     * The number of bands is the length of the {@code converters} array, which must be greater than 0
     * and not greater than the number of bands in the source image.
     *
     * @param  source      the image for which to convert sample values.
     * @param  layout      object to use for computing tile size, or {@code null} for the default.
     * @param  targetType  the type of this image resulting from conversion of given image.
     * @param  colorModel  the color model for the expected range of values, or {@code null}.
     * @param  ranges      the expected range of values for each band, or {@code null} if unknown.
     * @param  converters  the transfer functions to apply on each band of the source image.
     * @return the image which compute converted values from the given source.
     */
    public static BandedSampleConverter create(final RenderedImage source, ImageLayout layout,
            final int targetType, final ColorModel colorModel, final NumberRange<?>[] ranges,
            final MathTransform1D... converters)
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("converters", converters);
        final int numBands = converters.length;
        ArgumentChecks.ensureSizeBetween("converters", 1, source.getSampleModel().getNumBands(), numBands);
        if (layout == null) {
            layout = ImageLayout.DEFAULT;
        }
        final Dimension tile = layout.suggestTileSize(source, null);
        final BandedSampleModel sampleModel = RasterFactory.unique(
                new BandedSampleModel(targetType, tile.width, tile.height, numBands));
        /*
         * If the source image is writable, then changes in the converted image may be retro-propagated
         * to that source image. If we fail to compute the required inverse transforms, log a notice at
         * a low level because this is not a serious problem; writable BandedSampleConverter is a plus
         * but not a requirement.
         */
        if (source instanceof WritableRenderedImage) try {
            final MathTransform1D[] inverses = new MathTransform1D[numBands];
            for (int i=0; i<numBands; i++) {
                inverses[i] = converters[i].inverse();
            }
            return new Writable((WritableRenderedImage) source, sampleModel, colorModel, ranges, converters, inverses);
        } catch (NoninvertibleTransformException e) {
            Logging.recoverableException(Logging.getLogger(Modules.RASTER), BandedSampleConverter.class, "create", e);
        }
        return new BandedSampleConverter(source, sampleModel, colorModel, ranges, converters);
    }

    /**
     * Gets a property from this image. Current implementation recognizes:
     * {@value #SAMPLE_RESOLUTIONS_KEY}.
     */
    @Override
    public Object getProperty(final String key) {
        if (SAMPLE_RESOLUTIONS_KEY.equals(key) && sampleResolutions != null) {
            return sampleResolutions.clone();
        }
        return super.getProperty(key);
    }

    /**
     * Returns the names of all recognized properties, or {@code null} if this image has no properties.
     */
    @Override
    public String[] getPropertyNames() {
        if (sampleResolutions != null) {
            return new String[] {
                SAMPLE_RESOLUTIONS_KEY
            };
        }
        return super.getPropertyNames();
    }

    /**
     * Returns the color model associated with all rasters of this image.
     * If the sample values of this image are floating point numbers, then
     * a gray scale color model is computed from the expected range of values.
     *
     * @return the color model of this image, or {@code null} if none.
     */
    @Override
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Returns the width (in pixels) of this image.
     * This is the the same value than the source image (not necessarily zero).
     *
     * @return the width (number of columns) of this image.
     */
    @Override
    public int getWidth() {
        return getSource(0).getWidth();
    }

    /**
     * Returns the height (in pixels) of this image.
     * This is the the same value than the source image (not necessarily zero).
     *
     * @return the height (number of rows) of this image.
     */
    @Override
    public int getHeight() {
        return getSource(0).getHeight();
    }

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     * This is the the same value than the source image (not necessarily zero).
     *
     * @return the minimum <var>x</var> coordinate (column) of this image.
     */
    @Override
    public int getMinX() {
        return getSource(0).getMinX();
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     * This is the the same value than the source image (not necessarily zero).
     *
     * @return the minimum <var>y</var> coordinate (row) of this image.
     */
    @Override
    public int getMinY() {
        return getSource(0).getMinY();
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction.
     * This is the the same value than the source image (not necessarily zero).
     *
     * @return the minimum tile index in the <var>x</var> direction.
     */
    @Override
    public int getMinTileX() {
        return getSource(0).getMinTileX();
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction.
     * This is the the same value than the source image (not necessarily zero).
     *
     * @return the minimum tile index in the <var>y</var> direction.
     */
    @Override
    public int getMinTileY() {
        return getSource(0).getMinTileY();
    }

    /**
     * Computes the tile at specified indices.
     *
     * @param  tileX   the column index of the tile to compute.
     * @param  tileY   the row index of the tile to compute.
     * @param  target  if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices (can not be null).
     * @throws TransformException if an error occurred while converting a sample value.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster target) throws TransformException {
        if (target == null) {
            target = createTile(tileX, tileY);
        }
        Transferer.create(getSource(0), target).compute(converters);
        return target;
    }

    /**
     * A {@code BandedSampleConverter} capable to retro-propagate the changes to the source coverage.
     * This class contains the inverse of all {@link MathTransform1D} given to the parent class.
     */
    private static final class Writable extends BandedSampleConverter implements WritableRenderedImage {
        /**
         * The converters for computing the source values from a converted value.
         */
        private final MathTransform1D[] inverses;

        /**
         * The observers, or {@code null} if none. This is a copy-on-write array:
         * values are never modified after construction (new arrays are created).
         *
         * This field is declared volatile because it is read without synchronization by
         * {@link #markTileWritable(int, int, boolean)}. Since this is a copy-on-write array,
         * it is okay to omit synchronization for that method but we still need the memory effect.
         */
        @SuppressWarnings("VolatileArrayField")
        private volatile TileObserver[] observers;

        /**
         * Creates a new writable image which will compute values using the given converters.
         */
        Writable(final WritableRenderedImage source,  final BandedSampleModel sampleModel,
                 final ColorModel colorModel, final NumberRange<?>[] ranges,
                 final MathTransform1D[] converters, final MathTransform1D[] inverses)
        {
            super(source, sampleModel, colorModel, ranges, converters);
            this.inverses = inverses;
        }

        /**
         * Adds an observer to be notified when a tile is checked out for writing.
         * If the observer is already present, it will receive multiple notifications.
         *
         * @param  observer  the observer to notify.
         */
        @Override
        public synchronized void addTileObserver(final TileObserver observer) {
            observers = WriteSupport.addTileObserver(observers, observer);
        }

        /**
         * Removes an observer from the list of observers notified when a tile is checked out for writing.
         * If the observer was not registered, nothing happens. If the observer was registered for multiple
         * notifications, it will now be registered for one fewer.
         *
         * @param  observer  the observer to stop notifying.
         */
        @Override
        public synchronized void removeTileObserver(final TileObserver observer) {
            observers = WriteSupport.removeTileObserver(observers, observer);
        }

        /**
         * Sets or clears whether a tile is checked out for writing and notifies the listener if needed.
         *
         * @param  tileX    the <var>x</var> index of the tile to acquire or release.
         * @param  tileY    the <var>y</var> index of the tile to acquire or release.
         * @param  writing  {@code true} for acquiring the tile, or {@code false} for releasing it.
         */
        @Override
        protected boolean markTileWritable(final int tileX, final int tileY, final boolean writing) {
            final boolean notify = super.markTileWritable(tileX, tileY, writing);
            if (notify) {
                WriteSupport.fireTileUpdate(observers, this, tileX, tileY, writing);
            }
            return notify;
        }

        /**
         * Checks out a tile for writing.
         *
         * @param  tileX  the <var>x</var> index of the tile.
         * @param  tileY  the <var>y</var> index of the tile.
         * @return the specified tile as a writable tile.
         */
        @Override
        public WritableRaster getWritableTile(final int tileX, final int tileY) {
            final WritableRaster tile = (WritableRaster) getTile(tileX, tileY);
            markTileWritable(tileX, tileY, true);
            return tile;
        }

        /**
         * Relinquishes the right to write to a tile. If the tile goes from having one writer to
         * having no writers, the values are inverse converted and written in the original image.
         * If the caller continues to write to the tile, the results are undefined.
         *
         * @param  tileX  the <var>x</var> index of the tile.
         * @param  tileY  the <var>y</var> index of the tile.
         */
        @Override
        public void releaseWritableTile(final int tileX, final int tileY) {
            if (markTileWritable(tileX, tileY, false)) {
                setData(getTile(tileX, tileY));
            }
        }

        /**
         * Sets a region of the image to the contents of the given raster.
         * The raster is assumed to be in the same coordinate space as this image.
         * The operation is clipped to the bounds of this image.
         *
         * @param  data  the values to write in this image.
         */
        @Override
        public void setData(final Raster data) {
            final Rectangle bounds = data.getBounds();
            final WritableRenderedImage target = (WritableRenderedImage) getSource(0);
            ImageUtilities.clipBounds(target, bounds);
            final TileOpExecutor executor = new TileOpExecutor(target, bounds) {
                @Override protected void writeTo(final WritableRaster target) throws TransformException {
                    final Rectangle aoi = target.getBounds().intersection(bounds);
                    Transferer.create(data, target, aoi).compute(inverses);
                }
            };
            executor.writeTo(target);
            /*
             * Request to recompute the tiles of this `BandedSampleConverter` because if the values
             * in the source image are integers, then converting back to floating point values may
             * produce slightly different results.
             */
            markDirtyTiles(executor.getTileIndices());
        }
    }
}
