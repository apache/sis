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
package org.apache.sis.gui.coverage;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javafx.concurrent.Task;
import org.apache.sis.internal.gui.BackgroundThreads;


/**
 * A {@link Raster} for a {@link RenderedImage} file, potentially to be loaded in a background thread.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class GridTile {
    /**
     * The tile coordinates.
     */
    final int tileX, tileY;

    /**
     * Hash code value computed from tile indices.
     */
    private final int hashCode;

    /**
     * The tile, or {@code null} if not yet loaded.
     */
    Raster tile;

    /**
     * Non-null if an error occurred while reading the tile.
     */
    Throwable error;

    /**
     * Whether a tile loading is in progress. Used for avoiding to create many threads requesting the same tile.
     * If loading is in progress, other requests for that tile will return {@code null} immediately.
     */
    private boolean loading;

    /**
     * Creates a new tile for the given tile coordinates.
     */
    GridTile(final int tileX, final int tileY, final int numXTiles) {
        this.tileX = tileX;
        this.tileY = tileY;
        hashCode = tileX + tileY * numXTiles;
    }

    /**
     * Loads tile from the given image in a background thread and informs the specified view
     * when the tile become available.
     *
     * @param  view  the view for which to load a tile.
     */
    final void load(final GridView view) {
        if (!loading && error == null) {
            loading = true;
            final RenderedImage image = view.getImage();
            BackgroundThreads.execute(new Task<Raster>() {
                /** Invoked in background thread for fetching the tile. */
                @Override protected Raster call() {
                    return image.getTile(tileX, tileY);
                }

                /** Invoked in JavaFX thread on success. */
                @Override protected void succeeded() {
                    super.succeeded();
                    tile    = getValue();
                    error   = null;
                    loading = false;
                    view.contentChanged(false);
                }

                /** Invoked in JavaFX thread on failure. */
                @Override protected void failed() {
                    super.failed();
                    tile    = null;
                    error   = getException();
                    loading = false;
                }

                /** Invoked in JavaFX thread on cancellation. */
                @Override protected void cancelled() {
                    super.cancelled();
                    tile    = null;
                    error   = null;
                    loading = false;
                }
            });
        }
    }

    /**
     * Returns a hash code value for this tile.
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Compares the indices of this tile with the given object for equality.
     * Only indices are compared; the raster is ignored.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof GridTile) {
            final GridTile that = (GridTile) other;
            return tileX == that.tileX && tileY == that.tileY;
        }
        return false;
    }

    /**
     * Returns a string representation for debugging purpose only.
     */
    @Override
    public String toString() {
        return getClass().getCanonicalName() + '[' + tileX + ", " + tileY + ']';
    }
}
