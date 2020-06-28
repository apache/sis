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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * Mask of missing values.
 * This is the implementation of {@link ResampledImage#MASK_KEY} property value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class MaskImage extends SourceAlignedImage {
    /**
     * Properties inherited from the source image. Must be consistent with the <cite>switch case</cite>
     * statement delegating to the source image in {@link #getProperty(String)}.
     *
     * @see #getPropertyNames()
     */
    private static final Set<String> INHERITED_PROPERTIES = JDK9.setOf(
            GRID_GEOMETRY_KEY, POSITIONAL_ACCURACY_KEY, ResampledImage.POSITIONAL_CONSISTENCY_KEY);

    /**
     * Creates a new instance for the given image.
     */
    MaskImage(final ResampledImage image) {
        super(image, ColorModelFactory.createIndexColorModel(new int[] {0, -1},
                1, ImageUtilities.getVisibleBand(image), 0));
    }

    /**
     * Gets a property from this image.
     */
    @Override
    public Object getProperty(final String key) {
        return INHERITED_PROPERTIES.contains(key) ? getSource().getProperty(key) : super.getProperty(key);
    }

    /**
     * Returns the names of all recognized properties.
     *
     * @return names of all recognized properties, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        return getPropertyNames(INHERITED_PROPERTIES, null);
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
        final int tileMinX = tile.getMinX();
        final int tileMinY = tile.getMinY();
        final int tileMaxX = Math.addExact(tileMinX, tile.getWidth());
        final int tileMaxY = Math.addExact(tileMinY, tile.getHeight());
        float[] values = null;
        /*
         * Following algorithm is inefficient; it would be much faster to read or write directly in the arrays.
         * But it may not be worth to optimize it for now.
         */
        for (int y=tileMinY; y<tileMaxY; y++) {
            for (int x=tileMinX; x<tileMaxX; x++) {
                values = source.getPixel(x, y, values);
                for (int i=0; i<values.length; i++) {
                    if (Float.isNaN(values[i])) {
                        tile.setSample(x, y, 0, 1);
                        break;
                    }
                }
                // Otherwise leave the value to 0.
            }
        }
        return tile;
    }
}
