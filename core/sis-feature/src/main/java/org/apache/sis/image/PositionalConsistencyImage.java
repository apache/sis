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

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.util.Workaround;


/**
 * Estimation of positional error for each pixel in an image computed by {@link ResampledImage}.
 * This is the implementation of {@link ResampledImage#POSITIONAL_CONSISTENCY_KEY} property value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class PositionalConsistencyImage extends ComputedImage {
    /**
     * The color model for this image.
     * Arbitrarily configured for an error range from 0 to 1.
     */
    private final ColorModel colorModel;

    /**
     * Domain of pixel coordinates in this image.
     */
    private final int minX, minY, width, height;

    /**
     * A copy of {@link ResampledImage#toSourceSupport} with the support translation removed.
     * The result may be different than {@link ResampledImage#toSource} if the transform has
     * been replaced by {@link ResamplingGrid}.
     */
    private final MathTransform toSource;

    /**
     * The inverse of {@link ResampledImage#toSource}. Should not be concatenated with {@link #toSource}
     * because optimizations applied by Apache SIS during concatenations may hide the errors that we want
     * to visualize.
     */
    private final MathTransform toTarget;

    /**
     * Creates a new instance for the given image.
     */
    PositionalConsistencyImage(final ResampledImage image, final MathTransform toSource) throws TransformException {
        super(createSampleModel(image.getSampleModel()), image);
        this.toSource = toSource;
        this.toTarget = image.toSource.inverse();
        this.minX     = image.getMinX();
        this.minY     = image.getMinY();
        this.width    = image.getWidth();
        this.height   = image.getHeight();
        colorModel    = ColorModelFactory.createGrayScale(DataBuffer.TYPE_FLOAT, 1, 0, 0, 1);
    }

    /**
     * Creates the sample model. This is a workaround for RFE #4093999
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Workaround(library="JDK", version="1.8")
    private static SampleModel createSampleModel(final SampleModel origin) {
        final int width = origin.getWidth();
        return new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, origin.getHeight(), 1, width, new int[1]);
    }

    /**
     * Returns an arbitrary color model for this image.
     */
    @Override
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Gets a property from this image. Current default implementation supports the following keys
     * (more properties may be added to this list in future Apache SIS versions):
     *
     * <ul>
     *   <li>{@value #SAMPLE_RESOLUTIONS_KEY}</li>
     * </ul>
     */
    @Override
    public Object getProperty(final String key) {
        if (SAMPLE_RESOLUTIONS_KEY.equals(key)) {
            /*
             * Division by 8 is an arbitrary value for having one more digit
             * and keep a number having an exact representation in base 2.
             */
            return new float[] {(float) (ResamplingGrid.TOLERANCE / 8)};
        } else {
            return super.getProperty(key);
        }
    }

    /**
     * Returns the names of all recognized properties.
     *
     * @return names of all recognized properties.
     */
    @Override
    public String[] getPropertyNames() {
        return new String[] {
            SAMPLE_RESOLUTIONS_KEY
        };
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
     * Invoked when a tile need to be computed or updated.
     *
     * @param  tileX  the column index of the tile to compute.
     * @param  tileY  the row index of the tile to compute.
     * @param  tile   if the tile already exists but needs to be updated, the tile to update. Otherwise {@code null}.
     * @return computed tile for the given indices.
     * @throws TransformException if an error occurred while computing pixel coordinates.
     */
    @Override
    protected Raster computeTile(final int tileX, final int tileY, WritableRaster tile) throws TransformException {
        if (tile == null) {
            tile = createTile(tileX, tileY);
        }
        final int scanline = tile.getWidth();
        final int tileMinX = tile.getMinX();
        final int tileMinY = tile.getMinY();
        final int tileMaxX = Math.addExact(tileMinX, scanline);
        final int tileMaxY = Math.addExact(tileMinY, tile.getHeight());
        final double[] buffer = new double[scanline * Math.max(ResampledImage.BIDIMENSIONAL, toSource.getSourceDimensions())];
        for (int y=tileMinY; y<tileMaxY; y++) {
            for (int i=0, x = tileMinX; x < tileMaxX; x++) {
                buffer[i++] = x;
                buffer[i++] = y;
            }
            toSource.transform(buffer, 0, buffer, 0, scanline);
            toTarget.transform(buffer, 0, buffer, 0, scanline);
            for (int t=0,i=0, x = tileMinX; x < tileMaxX; x++) {
                buffer[t++] = Math.hypot(buffer[i++] - x,
                                         buffer[i++] - y);
            }
            tile.setSamples(tileMinX, y, scanline, 1, 0, buffer);
        }
        return tile;
    }
}
