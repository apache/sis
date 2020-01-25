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

import java.awt.Rectangle;
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
     * Hash code value computed from tile indices only. Other fields must be ignored.
     *
     * @see #hashCode()
     */
    private final int hashCode;

    /**
     * The tile, or {@code null} if not yet loaded.
     *
     * @see #tile()
     */
    private Raster tile;

    /**
     * Non-null if a loading is in progress or if an error occurred while fetching the tile.
     * The loading status is used for avoiding to create many threads requesting the same tile.
     * If loading is in progress, requests for that tile will return {@code null} immediately.
     */
    private Error status;

    /**
     * Creates a new tile for the given tile coordinates.
     */
    GridTile(final int tileX, final int tileY, final int numXTiles) {
        this.tileX = tileX;
        this.tileY = tileY;
        hashCode = tileX + tileY * numXTiles;
    }

    /**
     * Returns a hash code value for this tile. This hash code value must be based only on tile indices;
     * the {@link #tile} and the {@link #status} must be ignored, because we will use {@link GridTile}
     * instances also as keys for locating tiles in a hash map.
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Compares the indices of this tile with the given object for equality.
     * Only indices are compared; the raster is ignored. See {@link #hashCode()} for more information.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof GridTile) {
            final GridTile that = (GridTile) other;
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
     * Loads tile from the given image in a background thread and informs the specified view
     * when the tile become available. If we already failed to load that tile in a previous
     * attempt, then this method may set the {@link GridViewSkin#error} field.
     *
     * @param  view  the view for which to load a tile.
     */
    final void load(final GridView view) {
        if (status == null) {
            status = Error.LOADING;                                         // Pseudo-error.
            final RenderedImage image = view.getImage();
            BackgroundThreads.execute(new Task<Raster>() {
                /** Invoked in background thread for fetching the tile. */
                @Override protected Raster call() {
                    return image.getTile(tileX, tileY);
                }

                /**
                 * Invoked in JavaFX thread on success. If {@link GridView} is still showing
                 * the same image, it will be informed that the tile is available. Otherwise
                 * (if the image has changed) we ignore the result.
                 */
                @Override protected void succeeded() {
                    super.succeeded();
                    tile   = null;
                    status = null;
                    if (view.getImage() == image) {
                        tile = getValue();
                        view.contentChanged(false);
                    }
                }

                /**
                 * Invoked in JavaFX thread on failure. Discards everything and sets the error message
                 * if {@link GridView} is still showing the image for which we failed to load a tile.
                 */
                @Override protected void failed() {
                    super.failed();
                    tile   = null;
                    status = null;
                    if (view.getImage() == image) {
                        status = new Error(view.getTileBounds(tileX, tileY), getException());
                        view.contentChanged(false);     // For rendering the error message.
                    }
                }

                /**
                 * Invoked in JavaFX thread on cancellation. Just discard everything.
                 * Ideally we should interrupt the {@link RenderedImage#getTile(int, int)}
                 * process, but we currently have no API for that.
                 */
                @Override protected void cancelled() {
                    super.cancelled();
                    tile   = null;
                    status = null;
                }
            });
        } else if (status != Error.LOADING) {
            /*
             * A previous attempt failed to load that tile. We may have an error message to report.
             * If more than one tile failed, take the one with largest visible area.
             */
            ((GridViewSkin) view.getSkin()).errorOccurred(status);
        }
    }

    /**
     * The status of a tile request, either {@link #LOADING} or any other instance in case of error.
     * If not {@link #LOADING}, this class contains the reason why a tile request failed, together
     * with some information that depends on the viewing context. In particular {@link #visibleArea}
     * needs to be recomputed every time the viewed area in the {@link GridView} changed.
     */
    static final class Error {
        /**
         * A pseudo-error for saying that the tile is being fetched. Its effect is similar to an error
         * in the sense that {@link #load(GridView)} returns {@code null} immediately, except that no
         * error message is recorded.
         */
        private static final Error LOADING = new Error(null, null);

        /**
         * If we failed to load the tile, the reason for the failure.
         */
        final Throwable exception;

        /**
         * If we failed to load the tile, the zero-based row and column indices of the tile.
         * This is computed by {@link GridView#getTileBounds(int, int)} and should be constant.
         */
        private final Rectangle region;

        /**
         * Intersection of {@link #region} with the area currently shown in the view.
         * May vary with scrolling and is empty if the tile in error is outside visible area.
         *
         * @see GridError#getVisibleArea()
         */
        Rectangle visibleArea;

        /**
         * The {@link GridError#updateCount} value last time that {@link #visibleArea} was computed.
         * Used for detecting if we should recompute the visible area.
         */
        private int updateCount;

        /**
         * Creates an error status with the given cause.
         */
        private Error(final Rectangle region, final Throwable exception) {
            this.region    = region;
            this.exception = exception;
        }

        /**
         * Returns the area inside {@link #visibleArea}.
         */
        private long area() {
            return visibleArea.width * (long) visibleArea.height;
        }

        /**
         * Recomputes the {@link #visibleArea} value if needed, then returns {@code true}
         * if the visible area in this {@code Error} if wider than the one in {@code other}.
         *
         * @param  stamp     value of {@link GridError#updateCount}.
         * @param  viewArea  value of {@link GridError#viewArea}.
         * @param  other     the previous error, or {@code null}.
         * @return whether this status should replace {@code other}.
         */
        final boolean updateAndCompare(final int stamp, final Rectangle viewArea, final Error other) {
            if (updateCount != stamp) {
                visibleArea = viewArea.intersection(region);
                updateCount = stamp;
            }
            return (other == null) || area() > other.area();
        }
    }
}
