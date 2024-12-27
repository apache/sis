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

import java.util.Set;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.privy.ColorModelFactory;


/**
 * Estimation of positional error for each pixel in an image computed by {@link ResampledImage}.
 * This is the implementation of {@link ResampledImage#POSITIONAL_CONSISTENCY_KEY} property value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class PositionalConsistencyImage extends SourceAlignedImage {
    /**
     * Properties inherited from the source image. Must be consistent with the <i>switch case</i>
     * statement delegating to the source image in {@link #getProperty(String)}.
     *
     * @see #getPropertyNames()
     */
    private static final Set<String> INHERITED_PROPERTIES = Set.of(
            GRID_GEOMETRY_KEY, POSITIONAL_ACCURACY_KEY, MASK_KEY);

    /**
     * Properties added by this image, no matter if present in source image or not. Must be consistent with
     * the <i>switch case</i> statement doing its own calculation in {@link #getProperty(String)}.
     *
     * @see #getPropertyNames()
     */
    private static final String[] ADDED_PROPERTIES = {SAMPLE_RESOLUTIONS_KEY};

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
        super(image, ColorModelFactory.createGrayScale(DataBuffer.TYPE_FLOAT, 1, 0, 0, 1));
        this.toSource = toSource;
        this.toTarget = image.toSource.inverse();
    }

    /**
     * Gets a property from this image.
     */
    @Override
    public Object getProperty(final String key) {
        switch (key) {
            case SAMPLE_RESOLUTIONS_KEY: {
                return new double[] {ResamplingGrid.TOLERANCE / 8};
            }
            case POSITIONAL_ACCURACY_KEY:
            case GRID_GEOMETRY_KEY:
            case MASK_KEY: {
                return getSource().getProperty(key);
            }
            default: {
                return super.getProperty(key);
            }
        }
    }

    /**
     * Returns the names of all recognized properties.
     *
     * @return names of all recognized properties.
     */
    @Override
    public String[] getPropertyNames() {
        return filterPropertyNames(getSource().getPropertyNames(), INHERITED_PROPERTIES, ADDED_PROPERTIES);
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

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 67 * toSource.hashCode()
                                + 97 * toTarget.hashCode();
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final PositionalConsistencyImage other = (PositionalConsistencyImage) object;
            return toSource.equals(other.toSource) && toTarget.equals(other.toTarget);
        }
        return false;
    }
}
