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
import java.util.LinkedHashMap;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.feature.internal.Resources;


/**
 * A writable version of {@link TiledImage}.
 * The array of tiles given to the constructor can contain null elements,
 * in which case initially empty tiles will be created when first requested.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class WritableTiledImage extends TiledImage implements WritableRenderedImage {
    /**
     * Indices of tiles taken for a write operation.
     * Values are counter of calls to {@link #getWritableTile(int, int)}.
     * All accesses to this map shall be synchronized on the map instance.
     */
    private final Map<Point, Integer> writables;

    /**
     * Creates a new tiled image.
     * The first tile in the given array must be the one located at the minimal tile indices.
     * The caller must ensure that origin of {@code bounds} is the origin of the first tile.
     * All tiles must have the same size and the same sample model and must be sorted in row-major fashion.
     *
     * @param properties  image properties, or {@code null} if none.
     * @param colorModel  the color model, or {@code null} if none.
     * @param sampleModel the sample model used by all tiles.
     * @param bounds      minimum (<var>x</var>, <var>y</var>) coordinates together with the image size.
     * @param minTile     minimum tile indexes along <var>x</var> and <var>y</var> axes, or {@code null} for (0,0).
     * @param tiles       the tiles. Must contains at least one element. This array is not cloned.
     */
    public WritableTiledImage(final Map<String, ?> properties,
                              final ColorModel     colorModel,
                              final SampleModel    sampleModel,
                              final Rectangle      bounds,
                              final Point          minTile,
                              final WritableRaster... tiles)
    {
        super(properties, colorModel, sampleModel, bounds, minTile, tiles);
        writables = new LinkedHashMap<>();
    }

    /**
     * Verifies whether image layout information and tile coordinates are consistent.
     *
     * @hidden
     */
    @Override
    public String verify() {
        synchronized (tiles) {
            return super.verify();
        }
    }

    /**
     * Returns the sample model specified or inferred at construction time.
     */
    @Override
    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * Returns the tile at the given location in tile coordinates.
     * If the tile is null, an initially empty tile is created.
     */
    @Override
    public final Raster getTile(final int tileX, final int tileY) {
        final int numXTiles = getNumXTiles();
        final int numYTiles = getNumYTiles();
        checkTileIndex("tileX", minTileX, numXTiles, tileX);
        checkTileIndex("tileY", minTileY, numYTiles, tileY);
        final int index = (tileX - minTileX) + (tileY - minTileY) * numXTiles;
        synchronized (tiles) {
            Raster tile = tiles[index];
            if (tile == null) {
                final int x = ImageUtilities.tileToPixelX(this, tileX);
                final int y = ImageUtilities.tileToPixelY(this, tileY);
                tiles[index] = tile = createTile(new Point(x, y));
            }
            return tile;
        }
    }

    /**
     * Creates an initially empty tile. This method is invoked when {@link #getTile(int, int)}
     * is requested for a tile that does not exist yet. Subclasses can override this method in
     * order, for example, to fill the new tile with a fill value.
     *
     * @param  location  the upper-left corner of the tile.
     * @return a new raster for a tile at the given location.
     */
    protected WritableRaster createTile(final Point location) {
        return WritableRaster.createWritableRaster(sampleModel, location);
    }

    /**
     * Checks out a tile for writing. If the same tile is checked out many times
     * before to be released, only the first checkout is notified to listeners.
     *
     * @param  tileX  the <var>x</var> index of the tile.
     * @param  tileY  the <var>y</var> index of the tile.
     * @return the specified tile as a writable tile.
     */
    @Override
    public WritableRaster getWritableTile(final int tileX, final int tileY) {
        final var tile = (WritableRaster) getTile(tileX, tileY);
        final var key = new Point(tileX, tileY);
        final Integer count;
        synchronized (writables) {
            count = writables.merge(key, 1, (old, one) -> old + 1);
        }
        if (count <= 1) {
            fireTileUpdate(tileX, tileY, true);
        }
        return tile;
    }

    /**
     * Relinquishes the right to write to a tile. If the tile goes from having one writer to
     * having no writers, the values are inverse converted and written in the original image.
     * If the caller continues to write to the tile, the results are undefined.
     *
     * @param  tileX  the <var>x</var> index of the tile.
     * @param  tileY  the <var>y</var> index of the tile.
     */
    @Override
    public void releaseWritableTile(final int tileX, final int tileY) {
        final var key = new Point(tileX, tileY);
        final Integer count;
        final boolean close;
        synchronized (writables) {
            count = writables.computeIfPresent(key, (k, old) -> old - 1);
            close = (count != null && count <= 0);
            if (close) {
                writables.remove(key);
            }
        }
        if (count == null) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.TileNotWritable_2, tileX, tileY));
        }
        if (close) {
            fireTileUpdate(tileX, tileY, false);
        }
    }

    /**
     * Returns whether a tile is currently checked out for writing.
     *
     * @param  tileX  the <var>x</var> index of the tile.
     * @param  tileY  the <var>y</var> index of the tile.
     * @return {@code true} if specified tile is checked out for writing, {@code false} otherwise.
     */
    @Override
    public boolean isTileWritable(final int tileX, final int tileY) {
        final var key = new Point(tileX, tileY);
        synchronized (writables) {
            return writables.containsKey(key);
        }
    }

    /**
     * Returns the indices of all tiles checked out for writing.
     * Returns null if none are checked out.
     *
     * @return indices of tiles that are checked out for writing, or {@code null} if none.
     */
    @Override
    public Point[] getWritableTileIndices() {
        final Point[] indices;
        synchronized (writables) {
            final int n = writables.size();
            if (n == 0) return null;
            indices = writables.keySet().toArray(new Point[n]);
        }
        for (int i=0; i<indices.length; i++) {
            indices[i] = new Point(indices[i]);
        }
        return indices;
    }

    /**
     * Returns whether any tile is checked out for writing.
     *
     * @return {@code true} if any tiles are checked out for writing, or {@code false} otherwise.
     */
    @Override
    public boolean hasTileWriters() {
        synchronized (writables) {
            return !writables.isEmpty();
        }
    }

    /**
     * Sets a region of the image to the contents of the given raster.
     * The raster is assumed to be in the same coordinate space as this image.
     * The operation is clipped to the bounds of this image.
     *
     * @param  data  the values to write in this image.
     */
    @Override
    public void setData(final Raster data) {
        final Rectangle bounds = data.getBounds();
        ImageUtilities.clipBounds(this, bounds);
        if (!bounds.isEmpty()) {
            final var op = new TileOpExecutor(this, bounds) {
                @Override protected void writeTo(final WritableRaster target) {
                    target.setRect(data);
                }
            };
            op.writeTo(this);
        }
    }
}
