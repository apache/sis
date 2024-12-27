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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.image.privy.ColorModelFactory;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.util.logging.Logging;
import static org.apache.sis.image.privy.ImageUtilities.LOGGER;


/**
 * Mask of missing values.
 * This is the implementation of {@value ResampledImage#MASK_KEY} property value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see ResampledImage#getProperty(String)
 * @see ResampledImage#MASK_KEY
 */
final class MaskImage extends SourceAlignedImage {
    /**
     * Convert integer values to floating point values, or {@code null} if none.
     * This is needed since we use {@link Float#isNaN(float)} for identifying values to mask.
     */
    private final MathTransform converter;

    /**
     * Creates a new instance for the given image.
     */
    MaskImage(final ResampledImage image) {
        super(image, ColorModelFactory.createIndexColorModel(
                1, Math.max(0, ImageUtilities.getVisibleBand(image)), new int[] {0, -1}, true, 0));

        MathTransform converter = null;
        if (image.interpolation instanceof Visualization.InterpConvert) try {
            converter = ((Visualization.InterpConvert) image.interpolation).converter.inverse();
        } catch (NoninvertibleTransformException e) {
            // ResampledImage.getProperty("org.apache.sis.Mask") is the public caller of this constructor.
            Logging.unexpectedException(LOGGER, ResampledImage.class, "getProperty", e);
        }
        this.converter = converter;
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
        final Raster source = getSource().getTile(tileX, tileY);
        /*
         * Create a new tile unconditionally, without checking if a we can recycle a previous tile,
         * because we need a tile will all sample values initialized to zero. It should not happen
         * often that there is a tile to recycle anyway.
         */
        tile = createTile(tileX, tileY);
        final int numBands  = tile.getNumBands();
        final int tileMinX  = tile.getMinX();
        final int tileMinY  = tile.getMinY();
        final int tileMaxY  = Math.addExact(tileMinY, tile.getHeight());
        final int tileWidth = tile.getWidth();
        final float[] row   = new float[Math.multiplyExact(tileWidth, numBands)];
        for (int y=tileMinY; y<tileMaxY; y++) {
            source.getPixels(tileMinX, y, tileWidth, 1, row);
            if (converter != null) {
                converter.transform(row, 0, row, 0, tileWidth);
            }
            for (int i=0; i<row.length; i++) {
                if (Float.isNaN(row[i])) {
                    final int x = i / numBands + tileMinX;
                    tile.setSample(x, y, 0, 1);
                }
                // Otherwise leave the value to 0.
            }
        }
        return tile;
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + 97 * Objects.hashCode(converter);
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            final MaskImage other = (MaskImage) object;
            return Objects.equals(converter, other.converter);
        }
        return false;
    }
}
