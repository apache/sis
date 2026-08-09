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
import java.util.function.Function;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.TileObserver;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.image.PlanarImage;


/**
 * The buffered image used by Apache <abbr>SIS</abbr> for untiled images.
 * This class is preferred to {@link TiledImage} in the untiled case because
 * Java2D has performance optimizations for instances of {@link BufferedImage}.
 * This class is also preferred to instances of the exact {@link BufferedImage}
 * class for the following reasons:
 *
 * <ul class="verbose">
 *   <li>This class can notify tile observers when tiles are acquired for write operations.
 *     We cannot prevent {@link BufferedImage} to implement {@link WritableRenderedImage}, but we can
 *     increase the chances that Apache <abbr>SIS</abbr> is notified about pixel data modifications.
 *     For example, images given to {@link org.apache.sis.coverage.grid.GridCoverage2D} constructor
 *     are often used as sources of {@link org.apache.sis.image.ImageProcessor} operations,
 *     which listen to tile changes in order to flush the cache of invalidated tiles.</li>
 *   <li>This class can compute the {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY}
 *     property when first needed. We use this class even when the property value is known in advance
 *     because it has the desired side-effect of not letting {@link #getSubimage(int, int, int, int)}
 *     inherit that property.</li>
 *   <li>This class implements {@link #getData(Rectangle)} by delegating to more efficient Java2D methods
 *     and with a tolerance required by Apache <abbr>SIS</abbr> regarding intersections.</li>
 * </ul>
 *
 * <p>This class provides also static helper methods for {@link WritableRenderedImage} implementations.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WritableUntiledImage extends BufferedImage {
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
     * Write operations on this field should be done inside synchronized blocks.
     */
    private volatile int writeCount;

    /**
     * The value associated to the {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY} key.
     * May be a {@code Function<RenderedImage, GridGeometry} if the grid geometry is computed when
     * first requested. This is {@code null} if there is no such property.
     *
     * This property is stored as a {@code WritableUntiledImage} field rather than a {@code Hashtable}
     * entry for preventing inheritance by {@link #getSubimage(int, int, int, int)}.
     */
    private Object gridGeometry;

    /**
     * Creates an image of the specified type.
     *
     * @param width   image width.
     * @param height  image height.
     * @param type    one of {@code TYPE_*} constants.
     */
    public WritableUntiledImage(int width, int height, int type) {
        super(width, height, type);
    }

    /**
     * Creates an image using the specified raster.
     *
     * @param colors         color model of the new image.
     * @param raster         the singleton raster for the image data.
     * @param premultiplied  whether data in the raster has been premultiplied with alpha.
     * @param properties     image properties as ({@code String}, {@code Object}) entries.
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public WritableUntiledImage(final ColorModel colors,
                                final WritableRaster raster,
                                final boolean premultiplied,
                                final Hashtable<?,?> properties)
    {
        super(colors, raster, premultiplied, properties);
    }

    /**
     * Sets the value associated to the {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY} key.
     * If the grid geometry is known in advance, it will be used. Otherwise the grid geometry will be
     * computed when first requested using the given supplier.
     *
     * @param  ifKnown   the grid geometry, or {@code null} if no known in advance.
     * @param  supplier  the function to execute when first needed, or {@code null} if none.
     * @return {@code this} for method call chaining.
     */
    public WritableUntiledImage setGridGeometry(final GridGeometry ifKnown, final Function<RenderedImage, GridGeometry> supplier) {
        gridGeometry = (ifKnown != null) ? ifKnown : supplier;
        return this;
    }

    /**
     * Returns the names of properties that this image can provide.
     */
    @Override
    public String[] getPropertyNames() {
        String[] names = super.getPropertyNames();  // May be null.
        if (gridGeometry != null) {
            if (names == null) {
                names = new String[] {PlanarImage.GRID_GEOMETRY_KEY};
            } else {
                names = ArraysExt.append(names, PlanarImage.GRID_GEOMETRY_KEY);
            }
        }
        return names;
    }

    /**
     * Returns the property associated to the given key.
     * If the key is {@value org.apache.sis.image.PlanarImage#GRID_GEOMETRY_KEY},
     * then the {@link GridGeometry} will be computed when first needed.
     *
     * @param  name  name of the property to get.
     * @return property value associated to the given name, or {@link #UndefinedProperty} if none.
     * @throws ImagingOpException if the property value cannot be computed.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getProperty(final String name) {
        if (PlanarImage.GRID_GEOMETRY_KEY.equals(name)) {
            synchronized (this) {
                if (gridGeometry != null) {
                    if (gridGeometry instanceof GridGeometry) {
                        return (GridGeometry) gridGeometry;
                    }
                    // `ClassCastException` should never occur here.
                    return gridGeometry = ((Function<RenderedImage, GridGeometry>) gridGeometry).apply(this);
                }
            }
        }
        return super.getProperty(name);
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
     * Returns a copy of this image as one large tile.
     * The returned raster will not be updated if this image is changed.
     *
     * <p>Note: the implementation in Java 25 allocates a whole new tile if the raster is a subtile.
     * By contrast, the implementation in this class allocates only the space required by the subtile.</p>
     *
     * @return a copy of this image as one large tile.
     *
     * @see PlanarImage#getData()
     */
    @Override
    public Raster getData() {
        return copyData(null);
    }

    /**
     * Returns a copy of an arbitrary region of this image.
     * The returned raster will not be updated if this image is changed.
     *
     * <h4>Handling of regions outside the image bounds</h4>
     * The given Area Of Interest (<abbr>AOI</abbr>) shall intersect the image bounds,
     * but does not need to be fully contained inside those bounds.
     * If {@code aoi} is partially outside the image bounds,
     * only the pixels inside the intersection are copied and the other pixels are set to 0.
     * This is useful when re-tiling with a tile size which is not divisor of the image size.
     * Note that different {@link RenderedImage} implementations may have different policies.
     *
     * @param  aoi  the region of this image to copy.
     * @return a copy of this image in the given area of interest.
     *
     * @see PlanarImage#getData(Rectangle)
     * @throws IllegalArgumentException if the given rectangle is empty or does not intersect this image bounds.
     */
    @Override
    public Raster getData(final Rectangle aoi) {
        if (aoi.isEmpty()) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "aoi"));
        }
        return copyData(RasterFactory.createWritableRaster(getSampleModel(), aoi));
    }

    /**
     * Copies an arbitrary rectangular region of this image to the supplied writable raster.
     * The region to be copied is determined from the bounds of the supplied target raster.
     *
     * <h4>Handling of regions outside the image bounds</h4>
     * The bounds of the {@code target} raster shall intersect the bounds of this image.
     * Only the pixels inside the intersection are copied and the other pixels are unchanged.
     * This tolerance is useful when using tile sizes that are not divisor of the image size.
     * Note that different {@link RenderedImage} implementations may have different policies.
     *
     * @param  target  the raster to hold a copy of this image, or {@code null}.
     * @return the given raster if it was not null, or a new raster otherwise.
     *
     * @see PlanarImage#copyData(WritableRaster)
     */
    @Override
    public WritableRaster copyData(WritableRaster target) {
        Raster source = getRaster();
        Rectangle aoi = source.getBounds();
        if (target == null) {
            target = RasterFactory.createWritableRaster(source.getSampleModel(), aoi);
        } else if (!aoi.equals(aoi = aoi.intersection(target.getBounds()))) {
            if (aoi.isEmpty()) {
                // Note: this is stricter than `PlanarImage.copy(target)`, but useful for debugging.
                throw new IllegalArgumentException(Errors.format(Errors.Keys.OutsideDomainOfValidity));
            }
            source = source.createChild(aoi.x, aoi.y, aoi.width, aoi.height, aoi.x, aoi.y, null);
        }
        target.setRect(source);
        return target;
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
