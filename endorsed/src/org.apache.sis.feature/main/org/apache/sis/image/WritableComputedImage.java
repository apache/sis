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
package org.apache.sis.image;

import java.awt.image.SampleModel;
import java.awt.image.TileObserver;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.image.privy.ObservableImage;


/**
 * Parent classes for computed images that are <em>potentially</em> writable.
 * This class implements some {@link WritableRenderedImage} methods such as
 * the methods for adding or removing tile listeners. However this class does
 * <em>not</em> implement the {@link WritableRenderedImage} interface itself.
 * It is up to subclasses to implement that interface explicitly
 * when they have determined that the image is effectively writable.
 *
 * <h2>Usage pattern</h2>
 * Create a package-private read-only image by extending this class as if
 * {@link ComputedImage} was extended directly. Ignore all public methods
 * defined in this class. Do not make the class public for preventing users
 * users to access those public methods.
 *
 * <p>Create a package-private writable image as a subclass of above read-only image.
 * Override {@link #setData(Raster)}, {@link #equals(Object)} and {@link #hashCode()}.
 * The latter two methods need to be overridden for restoring the identity behavior
 * for writable image, because it may have listeners attached to this specific instance.
 * Example:</p>
 *
 * {@snippet lang="java" :
 * class MyOperation extends WritableComputedImage {
 *
 *     // Constructors omitted for brevity.
 *
 *     static final class Writable extends MyOperation implements WritableRenderedImage {
 *         @Override
 *         public void setData(Raster data) {
 *             // Write data back to original images here.
 *         }
 *
 *         @Override
 *         public boolean equals(final Object object) {
 *             return object == this;
 *         }
 *
 *         @Override
 *         public int hashCode() {
 *             return System.identityHashCode(this);
 *         }
 *     }
 * }
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class WritableComputedImage extends ComputedImage {
    /**
     * The observers, or {@code null} if none. This is a copy-on-write array:
     * values are never modified after construction (new arrays are created).
     *
     * This field is declared volatile because it is read without synchronization by
     * {@link #markTileWritable(int, int, boolean)}. Since this is a copy-on-write array,
     * it is okay to omit synchronization for that method but we still need the memory effect.
     */
    @SuppressWarnings("VolatileArrayField")
    private volatile TileObserver[] observers;

    /**
     * Creates an initially empty image with the given sample model.
     * The source images are not necessarily {@link WritableRenderedImage}
     * because this {@code WritableComputedImage} instance may be effectively read-only.
     * However if this {@code WritableComputedImage} instance is effectively writable,
     * then the given sources should be writable too.
     *
     * @param  sampleModel  the sample model shared by all tiles in this image.
     * @param  sources      sources of this image (may be an empty array), or a null array if unknown.
     */
    protected WritableComputedImage(SampleModel sampleModel, RenderedImage... sources) {
        super(sampleModel, sources);
    }

    /**
     * Adds an observer to be notified when a tile is checked out for writing.
     * If the observer is already present, it will receive multiple notifications.
     *
     * @param  observer  the observer to notify.
     */
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
    public synchronized void removeTileObserver(final TileObserver observer) {
        observers = ObservableImage.removeTileObserver(observers, observer);
    }

    /**
     * Sets or clears whether a tile is checked out for writing and notifies the listener if needed.
     *
     * @param  tileX    the <var>x</var> index of the tile to acquire or release.
     * @param  tileY    the <var>y</var> index of the tile to acquire or release.
     * @param  writing  {@code true} for acquiring the tile, or {@code false} for releasing it.
     */
    @Override
    protected boolean markTileWritable(final int tileX, final int tileY, final boolean writing) {
        final boolean notify = super.markTileWritable(tileX, tileY, writing);
        if (notify && this instanceof WritableRenderedImage) {
            ObservableImage.fireTileUpdate(observers, (WritableRenderedImage) this, tileX, tileY, writing);
        }
        return notify;
    }

    /**
     * Checks out a tile for writing.
     *
     * @param  tileX  the <var>x</var> index of the tile.
     * @param  tileY  the <var>y</var> index of the tile.
     * @return the specified tile as a writable tile.
     */
    public WritableRaster getWritableTile(final int tileX, final int tileY) {
        final var tile = (WritableRaster) getTile(tileX, tileY);
        markTileWritable(tileX, tileY, true);
        return tile;
    }

    /**
     * Relinquishes the right to write to a tile.
     * If the tile goes from having one writer to having no writers,
     * then the values are written to the original images by a call to {@link #setData(Raster)}.
     * If the caller continues to write to the tile, the results are undefined.
     *
     * @param  tileX  the <var>x</var> index of the tile.
     * @param  tileY  the <var>y</var> index of the tile.
     */
    public void releaseWritableTile(final int tileX, final int tileY) {
        if (markTileWritable(tileX, tileY, false)) {
            setData(getTile(tileX, tileY));
        }
    }

    /**
     * Sets a region of the image to the contents of the given raster.
     * The raster is assumed to be in the same coordinate space as this image.
     * The operation is clipped to the bounds of this image.
     *
     * @param  data  the values to write in this image.
     */
    protected void setData(final Raster data) {
        throw new UnsupportedOperationException();
    }
}
