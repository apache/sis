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
import java.awt.image.BandedSampleModel;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.util.Workaround;


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
 * Subclasses may relax those restrictions at the cost of more complex {@link #computeTile(int, int)}
 * implementation. Those restrictions may also be relaxed in future versions of this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public abstract class BandedSampleConverter extends ComputedImage {
    /**
     * The transfer functions to apply on each band of the source image.
     */
    private final MathTransform1D[] converters;

    /**
     * Creates a new image of the given data type which will compute values using the given converters.
     *
     * @param  source      the image for which to convert sample values.
     * @param  layout      object to use for computing tile size, or {@code null} for the default.
     * @param  targetType  the type of this image resulting from conversion of given image.
     * @param  converters  the transfer functions to apply on each band of the source image.
     */
    public BandedSampleConverter(final RenderedImage source, final ImageLayout layout,
                                 final int targetType, final MathTransform1D... converters)
    {
        super(createSampleModel(targetType, converters.length, layout, source), source);
        this.converters = converters;
    }

    /**
     * Returns the sample model to use for this image. This is a workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static BandedSampleModel createSampleModel(final int targetType,
            final int numBands, ImageLayout layout, final RenderedImage source)
    {
        if (layout == null) {
            layout = ImageLayout.DEFAULT;
        }
        final Dimension tile = layout.suggestTileSize(source);
        return new BandedSampleModel(targetType, tile.width, tile.height, numBands);
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
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @return computed tile for the given indices (can not be null).
     * @throws TransformException if an error occurred while converting a sample value.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY) throws TransformException {
        final Raster         source = getSource(0).getTile(tileX, tileY);
        final WritableRaster target = createTile(tileX, tileY);
        final Transferer   transfer = Transferer.suggest(source, target);
        transfer.compute(converters);
        return target;
    }
}
