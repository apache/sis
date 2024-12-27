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
import java.awt.Rectangle;
import java.util.Objects;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.image.privy.ImageUtilities;
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
 */
abstract class MultiSourceImage extends WritableComputedImage {
    /**
     * Color model of this image.
     * A null value is allowed but not recommended.
     *
     * @see #getColorModel()
     */
    private final ColorModel colorModel;

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
    final boolean parallel;

    /**
     * Creates a new multi-sources image.
     *
     * @param  sources      sources of this image.
     * @param  bounds       range of pixel coordinates of this image.
     * @param  minTile      indices of the first tile in this image.
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  colorModel   the color model of the image, or {@code null} if none.
     * @param  parallel     whether parallel computation is allowed.
     * @throws IllegalArgumentException if the color model is incompatible with the sample model.
     */
    MultiSourceImage(final RenderedImage[] sources, final Rectangle bounds, final Point minTile,
                     final SampleModel sampleModel, final ColorModel colorModel,
                     final boolean parallel)
    {
        super(sampleModel, sources);
        this.colorModel = colorModel;
        this.minX       = bounds.x;
        this.minY       = bounds.y;
        this.width      = bounds.width;
        this.height     = bounds.height;
        this.minTileX   = minTile.x;
        this.minTileY   = minTile.y;
        this.parallel   = parallel;
        ensureCompatible(sampleModel, colorModel);
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
        return new MultiSourcePrefetch(getSourceArray(), aoi).run(parallel);
    }

    /**
     * Returns a hash code value for this image.
     */
    @Override
    public int hashCode() {
        return hashCodeBase() + 37 * Objects.hashCode(colorModel);
    }

    /**
     * Compares the given object with this image for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (equalsBase(object)) {
            final var other = (MultiSourceImage) object;
            return parallel == other.parallel &&
                   minTileX == other.minTileX &&
                   minTileY == other.minTileY &&
                   getBounds().equals(other.getBounds()) &&
                   Objects.equals(colorModel, other.colorModel);
        }
        return false;
    }
}
