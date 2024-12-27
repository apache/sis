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
package org.apache.sis.image.privy;

import java.util.Map;
import java.awt.Image;
import java.awt.image.Raster;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


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
 */
public class TiledImage extends PlanarImage {
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
     * Image properties, or an empty map if none.
     */
    private final Map<String,Object> properties;

    /**
     * Creates a new tiled image. The first tile in the given array must be the
     * one located at the minimal tile indices. All tiles must have the same size
     * and the same sample model and must be sorted in row-major fashion
     * (this is not verified in current version, but may be in the future).
     *
     * @param properties  image properties, or {@code null} if none.
     * @param colorModel  the color model, or {@code null} if none.
     * @param width       number of pixels along X axis in the whole rendered image.
     * @param height      number of pixels along Y axis in the whole rendered image.
     * @param minTileX    minimum tile index in the X direction.
     * @param minTileY    minimum tile index in the Y direction.
     * @param tiles       the tiles. Must contains at least one element. This array is not cloned.
     */
    public TiledImage(final Map<String,Object> properties, final ColorModel colorModel,
                      final int width, final int height, final int minTileX, final int minTileY,
                      final Raster... tiles)
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
        this.properties = (properties != null) ? Map.copyOf(properties) : Map.of();
    }

    /**
     * Verifies whether image layout information and tile coordinates are consistent.
     * This method verifies the size and minimum pixel coordinates of all tiles.
     * If okay, then this method completes the check with all verifications
     * {@linkplain PlanarImage#verify() documented in parent class}
     *
     * @return {@code null} if image layout information are consistent,
     *         or the name of inconsistent attribute if a problem is found.
     */
    @Override
    public String verify() {
        final int minX       = getMinX();
        final int minY       = getMinY();
        final int numXTiles  = getNumXTiles();
        final int tileWidth  = getTileWidth();
        final int tileHeight = getTileHeight();
        final SampleModel sm = getSampleModel();
        for (int i=0; i < tiles.length; i++) {
            final Raster tile = tiles[i];
            final int tx = i % numXTiles;
            final int ty = i / numXTiles;
            final int ox = minX + tx * tileWidth;
            final int oy = minY + ty * tileHeight;
            if (tile.getMinX() != ox) return property(tx, ty, "x");
            if (tile.getMinY() != oy) return property(tx, ty, "y");
            /*
             * We accept two conventions for the raster size: either it is exactly equal to the tile size
             * (possibly extending belong the image size), or either it is exactly equal to the tile size
             * clipped to image size.
             */
            final int tw = tile.getWidth();
            final int th = tile.getHeight();
            if (tw != tileWidth || th != tileHeight) {
                if (tw != Math.min(tileWidth,  width  - ox)) return property(tx, ty, "width");
                if (th != Math.min(tileHeight, height - oy)) return property(tx, ty, "height");
            }
            if (!sm.equals(tile.getSampleModel())) return property(tx, ty, "sampleModel");
        }
        return super.verify();      // "width" and "height" properties should be checked last.
    }

    /**
     * Label returned by {@link #verify()} for identifying an error in a specified tile.
     */
    private static String property(final int tx, final int ty, final String name) {
        return "tiles[" + tx + ", " + ty + "]." + name;
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
     * Gets a property from this image.
     *
     * @param  key  the name of the property to get.
     * @return the property value, or {@link Image#UndefinedProperty} if none.
     */
    @Override
    public Object getProperty(final String key) {
        Object value = properties.getOrDefault(key, Image.UndefinedProperty);
        if (value instanceof DeferredProperty) {
            value = ((DeferredProperty) value).compute(this);
        }
        return value;
    }

    /**
     * Returns the names of all recognized properties,
     * or {@code null} if this image has no properties.
     *
     * @return names of all recognized properties, or {@code null} if none.
     */
    @Override
    public String[] getPropertyNames() {
        final int n = properties.size();
        return (n == 0) ? null : properties.keySet().toArray(new String[n]);
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
        tileX = verifyTileIndex("tileX", tileX, minTileX, numXTiles);
        tileY = verifyTileIndex("tileY", tileY, minTileY, numYTiles);
        return tiles[tileX + tileY * numXTiles];
    }

    /**
     * Verifies that the given tile index is inside expected bounds, then returns is zero-based value.
     */
    private static int verifyTileIndex(final String name, final int value, final int min, final int count) {
        final int r = value - min;
        if (r >= 0 && r < count) {
            return r;
        }
        throw new IndexOutOfBoundsException(Errors.format(
                Errors.Keys.ValueOutOfRange_4, name, min, min + count - 1, value));
    }
}
