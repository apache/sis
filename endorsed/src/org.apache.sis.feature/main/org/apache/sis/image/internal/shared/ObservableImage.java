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

import java.util.Arrays;
import java.util.Hashtable;
import java.awt.Point;
import java.awt.image.TileObserver;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.ArraysExt;


/**
 * A buffered image which can notify tile observers when tile are acquired for write operations.
 * Provides also helper methods for {@link WritableRenderedImage} implementations.
 *
 * <p>This class should be used in preference to {@link BufferedImage} when the image may be the
 * source of {@link org.apache.sis.image.ImageProcessor} operations. It is the case In particular
 * when this image is given to {@link org.apache.sis.coverage.grid.GridCoverage2D} constructor.
 * We cannot prevent {@link BufferedImage} to implement {@link WritableRenderedImage}, but we
 * can give a change to Apache SIS to be notified about modifications to pixel data.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ObservableImage extends BufferedImage {
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
     * Number of times that the tile has been acquired for writing and not yet released.
     * Write operations on this field should be in synchronized blocks.
     */
    private volatile int writeCount;

    /**
     * Creates an image of the specified type.
     *
     * @param width   image width.
     * @param height  image height.
     * @param type    one of {@code TYPE_*} constants.
     */
    public ObservableImage(int width, int height, int type) {
        super(width, height, type);
    }

    /**
     * Creates an image using the specified raster.
     *
     * @param colors  color model of the new image.
     * @param raster  the singleton raster for the image data.
     * @param isRasterPremultiplied   whether data in the raster has been premultiplied with alpha.
     * @param properties  image properties as ({@code String}, {@code Object}) entries.
     */
    public ObservableImage(ColorModel colors, WritableRaster raster, boolean isRasterPremultiplied, Hashtable<?,?> properties) {
        super(colors, raster, isRasterPremultiplied, properties);
    }

    /**
     * Returns a new array with the specified observer added to the array of observers.
     * If the observer is already present, it will receive multiple notifications.
     *
     * @param  observers  the array where to add the observer, or {@code null}.
     * @param  observer   the observer to add. Null values are ignored.
     * @return the updated array of observers.
     */
    public static TileObserver[] addTileObserver(TileObserver[] observers, final TileObserver observer) {
        if (observer != null) {
            if (observers == null) {
                return new TileObserver[] {observer};
            }
            final int n = observers.length;
            observers = Arrays.copyOf(observers, n+1);
            observers[n] = observer;
        }
        return observers;
    }

    /**
     * Returns a new array with the specified observer removed from the specified array of observers.
     * If the observer was not registered, nothing happens and the given array is returned as-is.
     * If the observer was registered for multiple notifications, it will now be registered for one fewer.
     *
     * @param  observers  the array where to remove the observer, or {@code null}.
     * @param  observer   the observer to remove.
     * @return the updated array of observers.
     */
    public static TileObserver[] removeTileObserver(final TileObserver[] observers, final TileObserver observer) {
        if (observers != null) {
            for (int i=observers.length; --i >= 0;) {
                if (observers[i] == observer) {
                    return ArraysExt.remove(observers, i, 1);
                }
            }
        }
        return observers;
    }

    /**
     * Notifies all listeners that the specified tile has been checked out for writing or has been released.
     *
     * @param observers       the observers to notify, or {@code null} if none.
     * @param image           the image that owns the tile.
     * @param tileX           the <var>x</var> index of the tile that is being updated.
     * @param tileY           the <var>y</var> index of the tile that is being updated.
     * @param willBeWritable  if {@code true}, the tile will be grabbed for writing; otherwise it is being released.
     */
    public static void fireTileUpdate(final TileObserver[] observers, final WritableRenderedImage image,
                                      final int tileX, final int tileY, final boolean willBeWritable)
    {
        if (observers != null) {
            for (final TileObserver observer : observers) {
                observer.tileUpdate(image, tileX, tileY, willBeWritable);
            }
        }
    }

    /**
     * Adds an observer to be notified when a tile is checked out for writing.
     * If the observer is already present, it will receive multiple notifications.
     *
     * @param  observer  the observer to notify.
     */
    @Override
    public synchronized void addTileObserver(final TileObserver observer) {
        observers = addTileObserver(observers, observer);
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
        observers = removeTileObserver(observers, observer);
    }

    /**
     * Notifies all listeners that the specified tile has been checked out for writing or has been released.
     * The notifications are sent only if the given {@code count} is zero.
     *
     * @param count           value of {@link #writeCount} before increment or after decrement.
     * @param willBeWritable  if {@code true}, the tile will be grabbed for writing; otherwise it is being released.
     */
    private void fireTileUpdate(final int count, final boolean willBeWritable) {
        if (count == 0) {
            fireTileUpdate(observers, this, 0, 0, willBeWritable);
        }
    }

    /**
     * Checks out a tile for writing. If the same tile is checked out many times
     * before to be released, only the first checkout is notified to listeners.
     *
     * @param  tileX  the <var>x</var> index of the tile.
     * @param  tileY  the <var>y</var> index of the tile.
     * @return the specified tile as a writable tile.
     * @throws IndexOutOfBoundsException if a given tile index is out of bounds.
     */
    @Override
    public WritableRaster getWritableTile(final int tileX, final int tileY) {
        if ((tileX | tileY) != 0) {
            throw new IndexOutOfBoundsException();
        }
        final WritableRaster tile = super.getWritableTile(tileX, tileY);
        final int count;
        synchronized (this) {
            count = writeCount++;
        }
        // Should be outside the synchronized block.
        fireTileUpdate(count, true);
        return tile;
    }

    /**
     * Relinquishes the right to write to a tile. If the tile goes from having
     * one writer to having no writers, then the listeners are notified.
     *
     * @param  tileX  the <var>x</var> index of the tile.
     * @param  tileY  the <var>y</var> index of the tile.
     * @throws IndexOutOfBoundsException if a given tile index is out of bounds.
     */
    @Override
    public void releaseWritableTile(final int tileX, final int tileY) {
        if ((tileX | tileY) != 0) {
            throw new IndexOutOfBoundsException();
        }
        final int count;
        synchronized (this) {
            count = --writeCount;
            if (count < 0) writeCount = 0;
        }
        if (count < 0) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.TileNotWritable_2, tileX, tileY));
        }
        // Should be outside the synchronized block.
        fireTileUpdate(count, false);
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
        return (tileX | tileY) == 0 && writeCount != 0;
    }

    /**
     * Returns the indices of all tiles checked out for writing.
     * Returns {@code null} if none are checked out.
     *
     * @return indices of tiles that are checked out for writing, or {@code null} if none.
     */
    @Override
    public Point[] getWritableTileIndices() {
        return writeCount == 0 ? null : new Point[] {new Point()};
    }

    /**
     * Returns whether any tile is checked out for writing.
     *
     * @return {@code true} if any tiles are checked out for writing, or {@code false} otherwise.
     */
    @Override
    public boolean hasTileWriters() {
        return writeCount != 0;
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
        int count;
        synchronized (this) {
            count = writeCount++;
        }
        fireTileUpdate(count, true);
        try {
            // Do not use super.setData(…) because it does not handle correctly the float and double types.
            getRaster().setRect(data);
        } finally {
            synchronized (this) {
                // Similar to `releaseWritableTile(…)` but without throwing exception.
                writeCount = count = Math.max(0, writeCount - 1);
            }
            fireTileUpdate(count, false);
        }
    }
}
