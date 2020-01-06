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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.TileObserver;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;


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
     * Creates a new image which will compute values using the given converters.
     *
     * @param  source       the image for which to convert sample values.
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  colorModel   the color model for from the expected range of values, or {@code null}.
     * @param  converters   the transfer functions to apply on each band of the source image.
     */
    BandedSampleConverter(final RenderedImage source,  final BandedSampleModel sampleModel,
                          final ColorModel colorModel, final MathTransform1D[] converters)
    {
        super(sampleModel, source);
        this.colorModel = colorModel;
        this.converters = converters;
    }

    /**
     * Creates a new image of the given data type which will compute values using the given converters.
     * The number of bands is the length of the {@code converters} array, which must be greater than 0
     * and not greater than the number of bands in the source image.
     *
     * @param  source      the image for which to convert sample values.
     * @param  layout      object to use for computing tile size, or {@code null} for the default.
     * @param  targetType  the type of this image resulting from conversion of given image.
     * @param  colorModel  the color model for from the expected range of values, or {@code null}.
     * @param  converters  the transfer functions to apply on each band of the source image.
     * @return the image which compute converted values from the given source.
     */
    public static BandedSampleConverter create(final RenderedImage source, ImageLayout layout,
                                               final int targetType, final ColorModel colorModel,
                                               final MathTransform1D... converters)
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("converters", converters);
        final int numBands = converters.length;
        ArgumentChecks.ensureSizeBetween("converters", 1, source.getSampleModel().getNumBands(), numBands);
        if (layout == null) {
            layout = ImageLayout.DEFAULT;
        }
        final Dimension tile = layout.suggestTileSize(source);
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
            return new Writable((WritableRenderedImage) source, sampleModel, colorModel, converters, inverses);
        } catch (NoninvertibleTransformException e) {
            Logging.recoverableException(Logging.getLogger(Modules.RASTER), BandedSampleConverter.class, "create", e);
        }
        return new BandedSampleConverter(source, sampleModel, colorModel, converters);
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
     */
    private static final class Writable extends BandedSampleConverter implements WritableRenderedImage {
        /**
         * The converters for computing the source values from a converted value.
         */
        private final MathTransform1D[] inverses;

        /**
         * Creates a new writable image which will compute values using the given converters.
         */
        Writable(final WritableRenderedImage source,  final BandedSampleModel sampleModel,
                 final ColorModel colorModel, final MathTransform1D[] converters, final MathTransform1D[] inverses)
        {
            super(source, sampleModel, colorModel, converters);
            this.inverses = inverses;
        }

        @Override
        public void addTileObserver(final TileObserver observer) {
        }

        @Override
        public void removeTileObserver(final TileObserver observer) {
        }

        @Override
        public WritableRaster getWritableTile(final int tileX, final int tileY) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void releaseWritableTile(final int tileX, final int tileY) {
        }

        @Override
        public void setData(final Raster data) {
            throw new UnsupportedOperationException();
        }
    }
}
