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
import java.awt.image.Raster;
import java.awt.image.TileObserver;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.feature.internal.Resources;


/**
 * A writable version of {@link TiledImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class WritableTiledImage extends TiledImage implements WritableRenderedImage {
    /**
     * The observers, or {@code null} if none. This is a copy-on-write array:
     * values are never modified after construction (new arrays are created).
     *
     * This field is declared volatile because it is read without synchronization by
     * {@link #getWritableTile(int, int)} and {@link #releaseWritableTile(int, int)}.
     * Since this is a copy-on-write array, it is okay to omit synchronization for
     * those methods but we still need the memory effect.
     */
    @SuppressWarnings("VolatileArrayField")
    private volatile TileObserver[] observers;

    /**
     * Indices of tiles taken for a write operation.
     * Values are counter of calls to {@link #getWritableTile(int, int)}.
     * All accesses to this map shall be synchronized on the map instance.
     */
    private final Map<Point,Integer> writables;

    /**
     * Creates a new tiled image. The first tile in the given array must be the one located at the minimal tile
     * indices. All tiles must have the same size and the same sample model and must be sorted in row-major fashion.
     *
     * @param properties  image properties, or {@code null} if none.
     * @param colorModel  the color model, or {@code null} if none.
     * @param width       number of pixels along X axis in the whole rendered image.
     * @param height      number of pixels along Y axis in the whole rendered image.
     * @param minTileX    minimum tile index in the X direction.
     * @param minTileY    minimum tile index in the Y direction.
     * @param tiles       the tiles. Must contains at least one element.
     *                    This array is not cloned.
     */
    public WritableTiledImage(final Map<String,Object> properties, final ColorModel colorModel,
                              final int width, final int height, final int minTileX, final int minTileY,
                              final WritableRaster... tiles)
    {
        super(properties, colorModel, width, height, minTileX, minTileY, tiles);
        writables = new LinkedHashMap<>();
    }

    /**
     * Adds an observer to be notified when a tile is checked out for writing.
     * If the observer is already present, it will receive multiple notifications.
     *
     * @param  observer  the observer to notify.
     */
    @Override
    public synchronized void addTileObserver(final TileObserver observer) {
        observers = ObservableImage.addTileObserver(observers, observer);
    }

    /**
     * Removes an observer from the list of observers notified when a tile is checked out for writing.
     * If the observer was not registered, nothing happens. If the observer was registered for multiple
     * notifications, it will now be registered for one fewer.
     *
     * @param  observer  the observer to stop notifying.
     */
    @Override
    public synchronized void removeTileObserver(final TileObserver observer) {
        observers = ObservableImage.removeTileObserver(observers, observer);
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
        final WritableRaster tile = (WritableRaster) super.getTile(tileX, tileY);
        final Point key = new Point(tileX, tileY);
        final Integer count;
        synchronized (writables) {
            count = writables.merge(key, 1, (old, one) -> old + 1);
        }
        if (count <= 1) {
            ObservableImage.fireTileUpdate(observers, this, tileX, tileY, true);
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
        final Point key = new Point(tileX, tileY);
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
            ObservableImage.fireTileUpdate(observers, this, tileX, tileY, false);
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
        final Point key = new Point(tileX, tileY);
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
            final TileOpExecutor op = new TileOpExecutor(this, bounds) {
                @Override protected void writeTo(final WritableRaster target) {
                    target.setRect(data);
                }
            };
            op.writeTo(this);
        }
    }
}
