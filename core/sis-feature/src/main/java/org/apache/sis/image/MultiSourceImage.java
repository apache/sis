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

import java.awt.Point;
import java.util.Arrays;
import java.util.Objects;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;
import org.apache.sis.util.Disposable;


/**
 * An image which is the result of a computation involving more than one source.
 * All sources shall use the same pixel coordinate system. However the sources
 * do not need to have the same bounds or use the same tile matrix.
 *
 * <p>This implementation is for images that are <em>potentially</em> writable.
 * Whether the image is effectively writable depends on whether all sources are
 * instances of {@link WritableRenderedImage}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
abstract class MultiSourceImage extends WritableComputedImage {
    /**
     * The source images, potentially with a preprocessing applied.
     * Those sources may be different than {@link #getSources()} for example with the
     * application of a "band select" operation for retaining only the bands needed.
     */
    protected final RenderedImage[] filteredSources;

    /**
     * Color model of this image.
     *
     * @see #getColorModel()
     */
    protected final ColorModel colorModel;

    /**
     * Domain of pixel coordinates. All images shall share the same pixel coordinate space,
     * meaning that a pixel at coordinates (<var>x</var>, <var>y</var>) in this image will
     * contain the sample values of all source images at the same coordinates.
     * It does <em>not</em> mean that all source images shall have the same bounds.
     */
    private final int minX, minY, width, height;

    /**
     * Index of the first tile. Contrarily to pixel coordinates,
     * the tile coordinate space does not need to be the same for all images.
     */
    private final int minTileX, minTileY;

    /**
     * Whether parallel computation is allowed.
     */
    private final boolean parallel;

    /**
     * Creates a new multi-sources image.
     *
     * @param  layout     pixel and tile coordinate spaces of this image, together with sample model.
     * @param  colorizer  provider of color model to use for this image, or {@code null} for automatic.
     * @param  parallel   whether parallel computation is allowed.
     */
    MultiSourceImage(final MultiSourceLayout layout, final Colorizer colorizer, final boolean parallel) {
        super(layout.sampleModel, layout.sources);
        final Rectangle r = layout.domain;
        minX            = r.x;
        minY            = r.y;
        width           = r.width;
        height          = r.height;
        minTileX        = layout.minTileX;
        minTileY        = layout.minTileY;
        filteredSources = layout.filteredSources;
        colorModel      = layout.createColorModel(colorizer);
        ensureCompatible(colorModel);
        this.parallel = parallel;
    }

    /** Returns the information inferred at construction time. */
    @Override public final ColorModel getColorModel() {return colorModel;}
    @Override public final int        getWidth()      {return width;}
    @Override public final int        getHeight()     {return height;}
    @Override public final int        getMinX()       {return minX;}
    @Override public final int        getMinY()       {return minY;}
    @Override public final int        getMinTileX()   {return minTileX;}
    @Override public final int        getMinTileY()   {return minTileY;}

    /**
     * Converts a tile (column, row) indices to smallest (<var>x</var>, <var>y</var>) pixel coordinates
     * inside the tile. The returned value is a coordinate of the pixel in upper-left corner.
     *
     * @param  tileX  the tile index for which to get pixel coordinate.
     * @param  tileY  the tile index for which to get pixel coordinate.
     * @return smallest (<var>x</var>, <var>y</var>) pixel coordinates inside the tile.
     */
    final Point tileToPixel(final int tileX, final int tileY) {
        return new Point(Math.toIntExact((((long) tileX) - minTileX) * getTileWidth()  + minX),
                         Math.toIntExact((((long) tileY) - minTileY) * getTileHeight() + minY));
    }

    /**
     * Notifies the source images that tiles will be computed soon in the given region.
     * This method forwards the notification to all images that are instances of {@link PlanarImage}.
     */
    @Override
    protected Disposable prefetch(final Rectangle tiles) {
        /*
         * Convert tile indices to pixel indices. The latter will be converted back to
         * tile indices for each source because the tile numbering may not be the same.
         */
        final Rectangle aoi = ImageUtilities.tilesToPixels(this, tiles);
        return new MultiSourcePrefetch(filteredSources, aoi).run(parallel);
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return hashCodeBase() + 37 * (Arrays.hashCode(filteredSources) + 31 * Objects.hashCode(colorModel));
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (equalsBase(object)) {
            final MultiSourceImage other = (MultiSourceImage) object;
            return parallel == other.parallel &&
                   minTileX == other.minTileX &&
                   minTileY == other.minTileY &&
                   getBounds().equals(other.getBounds()) &&
                   Objects.equals(colorModel, other.colorModel) &&
                   Arrays.equals(filteredSources, other.filteredSources);
        }
        return false;
    }
}
