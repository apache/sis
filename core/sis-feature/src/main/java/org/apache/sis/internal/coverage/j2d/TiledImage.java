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

import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.ArgumentChecks;


/**
 * A rendered image which can contain an arbitrary number of tiles. Tiles are stored in memory.
 * This class may become public in a future version, but not yet because managing large tiled
 * images would require a more sophisticated class than current implementation.
 *
 * <p>This class should not perform any computation; all tiles are given at construction time.
 * This requirement makes this class thread-safe and concurrent without the need for synchronization.</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class TiledImage extends PlanarImage {
    /**
     * The color model, or {@code null} if none.
     */
    private final ColorModel colorModel;

    /**
     * Number of pixels along X or Y axis in the whole rendered image.
     */
    private final int width, height;

    /**
     * Index of the first tile in the image.
     */
    private final int minTileX, minTileY;

    /**
     * The tiles. They must all use the same sample model.
     */
    private final Raster[] tiles;

    /**
     * Creates a new tiled image. The first tile in the given array must be the
     * one located at the minimal tile indices. All tiles must have the same size
     * and the same sample model and must be sorted in row-major fashion
     * (this is not verified in current version, but may be in the future).
     *
     * @param colorModel  the color model, or {@code null} if none.
     * @param width       number of pixels along X axis in the whole rendered image.
     * @param height      number of pixels along Y axis in the whole rendered image.
     * @param minTileX    minimum tile index in the X direction.
     * @param minTileY    minimum tile index in the Y direction.
     * @param tiles       the tiles. Must contains at least one element.
     */
    public TiledImage(final ColorModel colorModel, final int width, final int height,
                      final int minTileX, final int minTileY, final Raster... tiles)
    {
        ArgumentChecks.ensureStrictlyPositive("width",  width);
        ArgumentChecks.ensureStrictlyPositive("height", height);
        ArgumentChecks.ensureNonEmpty        ("tiles",  tiles);
        this.colorModel = colorModel;
        this.width      = width;
        this.height     = height;
        this.minTileX   = minTileX;
        this.minTileY   = minTileY;
        this.tiles      = tiles;
    }

    /**
     * Returns the color model, or {@code null} if none.
     */
    @Override
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Returns the sample model.
     */
    @Override
    public SampleModel getSampleModel() {
        return tiles[0].getSampleModel();
    }


    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     */
    @Override
    public int getMinX() {
        return tiles[0].getMinX();
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     */
    @Override
    public int getMinY() {
        return tiles[0].getMinY();
    }

    /**
     * Returns the number of pixels along X axis in the whole rendered image.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Returns the number of pixels along Y axis in the whole rendered image.
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Returns the tile width in pixels. All tiles must have the same width.
     */
    @Override
    public int getTileWidth() {
        return tiles[0].getWidth();
    }

    /**
     * Returns the tile height in pixels. All tiles must have the same height.
     */
    @Override
    public int getTileHeight() {
        return tiles[0].getHeight();
    }

    /**
     * Returns the minimum tile index in the X direction.
     */
    @Override
    public int getMinTileX() {
        return minTileX;
    }

    /**
     * Returns the minimum tile index in the Y direction.
     */
    @Override
    public int getMinTileY() {
        return minTileY;
    }

    /**
     * Returns the tile at the given location in tile coordinates.
     */
    @Override
    public Raster getTile(int tileX, int tileY) {
        final int numXTiles = getNumXTiles();
        final int numYTiles = getNumYTiles();
        if ((tileX -= minTileX) < 0 || tileX >= numXTiles ||
            (tileY -= minTileY) < 0 || tileY >= numYTiles)
        {
            throw new IndexOutOfBoundsException();
        }
        return tiles[tileX + tileY * numXTiles];
    }
}
