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
import org.apache.sis.gui.internal.BackgroundThreads;


/**
 * A single tile potentially fetched (loaded or computed) from an image in a background thread.
 * The source is a {@link RenderedImage} and the cached object is {@link Raster}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridTile {
    /**
     * The tile coordinates.
     */
    final int tileX, tileY;

    /**
     * The tile, or {@code null} if not yet fetched.
     *
     * @see #tile()
     */
    private Raster tile;

    /**
     * Non-null if an error occurred while fetching the tile.
     */
    private GridError error;

    /**
     * Whether a fetching is under progress. Used for avoiding to create many threads requesting the same
     * tile at the same time: if {@code true} then {@link #load(GridView)} returns {@code null} immediately.
     */
    private boolean loading;

    /**
     * Creates a new tile for the given tile coordinates.
     */
    GridTile(final int tileX, final int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    /**
     * Returns a hash code value for this tile. This hash code value must be based only on tile indices.
     * The {@link #tile} and the {@link #error} must be ignored, because we will use {@link GridTile}
     * instances also as keys for locating tiles in a hash map.
     */
    @Override
    public int hashCode() {
        return tileX ^ Integer.reverse(tileY);
    }

    /**
     * Compares the indices of this tile with the given object for equality.
     * Only indices are compared, the raster is ignored.
     * See {@link #hashCode()} for more information.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof GridTile) {
            final var that = (GridTile) other;
            return tileX == that.tileX && tileY == that.tileY;
            // Intentionally no other comparisons.
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

    /**
     * Returns the cached tile if available, or {@code null} otherwise.
     * If null, then the caller should invoke {@link #load(GridView)}.
     */
    final Raster tile() {
        return tile;
    }

    /**
     * Clears the cached {@link #tile}. That tile will be recomputed again if needed.
     * Note that in many cases the tile will not be really recomputed since a second,
     * more sophisticated caching mechanism may be done (at least in Apache SIS case)
     * by {@link RenderedImage#getTile(int, int)} implementation.
     *
     * @return {@code true} if there is no error, in which case the whole {@link GridTile} can be discarded.
     */
    final boolean clearTile() {
        if (loading) {
            return false;
        }
        tile = null;
        return error == null;
    }

    /**
     * Clears the tile, error and loading flags. This is invoked after a the background thread fetching a
     * tile completed, either successfully or with a failure, before to set the new values. This is also
     * invoked if we want to retry loading a tile after a failure.
     */
    final void clear() {
        tile    = null;
        error   = null;
        loading = false;
    }

    /**
     * Fetches (load or compute) tile from the image in a background thread and informs the specified view
     * when the tile become available. If we already failed to fetch that tile in a previous attempt, then
     * this method returns {@code null}.
     *
     * @param  view  the view for which to fetch a tile.
     */
    final void load(final GridView view) {
        if (!loading && error == null) {
            loading = true;
            final RenderedImage image = view.getImage();
            BackgroundThreads.execute(new Task<Raster>() {
                /**
                 * Invoked in background thread for fetching the tile.
                 */
                @Override
                protected Raster call() {
                    return image.getTile(tileX, tileY);
                }

                /**
                 * Invoked in JavaFX thread on success. If {@link GridView} is still showing
                 * the same image, it will be informed that the tile is available. Otherwise
                 * (if the image has changed) we ignore the result.
                 */
                @Override
                protected void succeeded() {
                    clear();
                    if (view.getImage() == image) {
                        tile = getValue();
                        view.updateCellValues();
                    }
                }

                /**
                 * Invoked in JavaFX thread on failure. Discards everything and sets the error message
                 * if {@link GridView} is still showing the image for which we failed to load a tile.
                 */
                @Override
                protected void failed() {
                    clear();
                    if (view.getImage() == image) {
                        error = new GridError(view, GridTile.this, getException());
                        ((GridViewSkin) view.getSkin()).errorOccurred(error);
                    }
                }

                /**
                 * Invoked in JavaFX thread on cancellation. Just discard everything.
                 * Ideally we should interrupt the {@link RenderedImage#getTile(int, int)}
                 * process, but we currently have no API for that.
                 */
                @Override
                protected void cancelled() {
                    clear();
                }
            });
        }
    }
}
