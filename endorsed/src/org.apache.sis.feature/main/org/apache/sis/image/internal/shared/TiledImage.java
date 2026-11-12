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
package org.apache.sis.image.internal.shared;

import java.util.Map;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Image;
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
 * <p>This class assumes that all tiles are provided at construction time with no deferred calculation.
 * This assumption makes this class thread-safe and concurrent without the need for synchronization.
 * If this assumption is not true, then subclasses should override the {@link #verify()} and
 * {@link #getTile(int, int)} methods with synchronization.</p>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class TiledImage extends PlanarImage {
    /**
     * The color model, or {@code null} if none.
     */
    protected final ColorModel colorModel;

    /**
     * The sample model, either specified at construction time or inferred from the tiles.
     * May be {@code null} if no sample model was found at construction time.
     */
    protected final SampleModel sampleModel;

    /**
     * Number of pixels along <var>x</var> or <var>y</var> axis in the whole rendered image.
     */
    protected final int width, height;

    /**
     * Index of the first pixel in the image. Must be equals to the origin of the first tile.
     * That first tile may be {@code null} if a subclass implements deferred computation.
     */
    protected final int minX, minY;

    /**
     * Index of the first tile in the image.
     */
    protected final int minTileX, minTileY;

    /**
     * The tiles in row-major fashion. All tiles must all use the same sample model.
     */
    protected final Raster[] tiles;

    /**
     * Image properties in an immutable map, or an empty map if none.
     */
    protected final Map<String, Object> properties;

    /**
     * Creates a new tiled image.
     * The first tile in the given array must be the one located at the minimal tile indices.
     * The caller must ensure that origin of {@code bounds} is the origin of the first tile.
     * All tiles must have the same size and the same sample model and must be sorted in row-major fashion
     * (this is not verified in current version, but may be in the future).
     *
     * @param properties  image properties, or {@code null} if none.
     * @param colorModel  the color model, or {@code null} if none.
     * @param sampleModel the sample model used by all tiles.
     * @param bounds      minimum (<var>x</var>, <var>y</var>) coordinates together with the image size.
     * @param minTile     minimum tile indexes along <var>x</var> and <var>y</var> axes, or {@code null} for (0,0).
     * @param tiles       the tiles. Must contains at least one element. This array is not cloned.
     */
    public TiledImage(final Map<String, ?> properties,
                      final ColorModel     colorModel,
                      final SampleModel    sampleModel,
                      final Rectangle      bounds,
                      final Point          minTile,
                      final Raster...      tiles)
    {
        ArgumentChecks.ensureNonNull("sampleModel", sampleModel);
        ArgumentChecks.ensureStrictlyPositive("width",  width  = bounds.width);
        ArgumentChecks.ensureStrictlyPositive("height", height = bounds.height);
        ArgumentChecks.ensureNonEmpty        ("tiles",  tiles);
        this.tiles       = tiles;
        this.minX        = bounds.x;
        this.minY        = bounds.y;
        this.colorModel  = colorModel;
        this.sampleModel = sampleModel;
        this.properties  = (properties != null) ? Map.copyOf(properties) : Map.of();
        if (minTile != null) {
            minTileX = minTile.x;
            minTileY = minTile.y;
        } else {
            minTileX = 0;
            minTileY = 0;
        }
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
        final int numXTiles  = getNumXTiles();
        final int tileWidth  = getTileWidth();
        final int tileHeight = getTileHeight();
        for (int i=0; i < tiles.length; i++) {
            final Raster tile = tiles[i];
            if (tile == null) continue;
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
            if (!sampleModel.equals(tile.getSampleModel())) return property(tx, ty, "sampleModel");
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
        return sampleModel;
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
            value = ((DeferredProperty) value).getOrCompute(this);
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
        return minX;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     */
    @Override
    public int getMinY() {
        return minY;
    }

    /**
     * Returns the number of pixels along <var>x</var> axis in the whole rendered image.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Returns the number of pixels along <var>y</var> axis in the whole rendered image.
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
        return sampleModel.getWidth();
    }

    /**
     * Returns the tile height in pixels. All tiles must have the same height.
     */
    @Override
    public int getTileHeight() {
        return sampleModel.getHeight();
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction.
     */
    @Override
    public final int getMinTileX() {
        return minTileX;
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction.
     */
    @Override
    public final int getMinTileY() {
        return minTileY;
    }

    /**
     * Returns the tile at the given location in tile coordinates.
     */
    @Override
    public Raster getTile(final int tileX, final int tileY) {
        final int numXTiles = getNumXTiles();
        final int numYTiles = getNumYTiles();
        checkTileIndex("tileX", minTileX, numXTiles, tileX);
        checkTileIndex("tileY", minTileY, numYTiles, tileY);
        return tiles[(tileX - minTileX) + (tileY - minTileY) * numXTiles];
    }
}
