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
package org.apache.sis.storage.base;

import java.util.Map;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import org.apache.sis.image.privy.BatchComputedImage;


/**
 * A rendered image where tiles are loaded only when first needed.
 * Used for {@link org.apache.sis.storage.RasterLoadingStrategy#AT_GET_TILE_TIME}.
 * Other loading strategies should not instantiate this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TiledDeferredImage extends BatchComputedImage {
    /**
     * Number of pixels along X or Y axis in the whole rendered image.
     */
    private final int width, height;

    /**
     * Index of the first tile in the image.
     */
    private final int minTileX, minTileY;

    /**
     * Iterator over tiles. The iterator position should not be modified;
     * instead subsets of this iterator will be created when needed.
     */
    private final TiledGridCoverage.TileIterator iterator;

    /**
     * Creates a new tiled image.
     *
     * @param imageSize   full image size, after subsampling.
     * @param tileLower   indices of first tile to read, inclusive.
     * @param properties  image properties, or {@code null} if none.
     */
    TiledDeferredImage(final int[] imageSize, final int[] tileLower,
                       final Map<String,Object> properties, final TiledGridCoverage.TileIterator iterator)
    {
        super(iterator.getCoverage().model, properties);
        this.width    = imageSize[TiledGridCoverage.X_DIMENSION];
        this.height   = imageSize[TiledGridCoverage.Y_DIMENSION];
        this.minTileX = tileLower[TiledGridCoverage.X_DIMENSION];
        this.minTileY = tileLower[TiledGridCoverage.Y_DIMENSION];
        this.iterator = iterator;
    }

    /** Returns the color model, or {@code null} if none. */
    @Override public ColorModel getColorModel() {
        return iterator.getCoverage().colors;
    }

    /** Returns the minimum <var>x</var> coordinate (inclusive) of this image. */
    @Override public final int getMinX() {
        return iterator.getTileOrigin(TiledGridCoverage.X_DIMENSION);
    }

    /** Returns the minimum <var>y</var> coordinate (inclusive) of this image. */
    @Override public final int getMinY() {
        return iterator.getTileOrigin(TiledGridCoverage.Y_DIMENSION);
    }

    /** Returns the number of pixels along X axis in the whole rendered image. */
    @Override public final int getWidth() {return width;}

    /** Returns the number of pixels along Y axis in the whole rendered image. */
    @Override public final int getHeight() {return height;}

    /** Returns the minimum tile index in the X direction. */
    @Override public final int getMinTileX() {return minTileX;}

    /** Returns the minimum tile index in the Y direction. */
    @Override public final int getMinTileY() {return minTileY;}

    /**
     * Loads immediately and returns all tiles in the given ranges of tile indices.
     *
     * @param  tiles  range of tile indices for which to load tiles.
     * @return loaded tiles for the given indices, in row-major fashion.
     */
    @Override
    protected Raster[] computeTiles(final Rectangle tiles) throws Exception {
        final TiledGridCoverage.TileIterator aoi = iterator.subset(
                new int[] {
                    tiles.x,
                    tiles.y
                },
                new int[] {
                    Math.addExact(tiles.x, tiles.width),
                    Math.addExact(tiles.y, tiles.height)
                });
        return aoi.getCoverage().readTiles(aoi);
    }
}
