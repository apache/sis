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

import java.lang.ref.WeakReference;
import java.awt.image.TileObserver;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.image.internal.shared.WritableUntiledImage;


/**
 * Base classes of images computed in a monolithic way.
 * All tiles are computed immediately when a tile is requested.
 * This behavior is of course not desired, which is why this base class is not public.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class MonolithicImage extends PlanarImage {
    /**
     * The observers, or {@code null} if none. This is a copy-on-write array:
     * values are never modified after construction (new arrays are created).
     *
     * This field is declared volatile because it is read without synchronization by
     * {@link #markTileWritable(int, int, boolean)}. Since this is a copy-on-write array,
     * it is okay to omit synchronization for that method but we still need the memory effect.
     *
     * @see #addTileObserver(TileObserver)
     * @see #removeTileObserver(TileObserver)
     */
    @SuppressWarnings("VolatileArrayField")
    private volatile TileObserver[] observers;

    /**
     * The common listener registered on all source images, or {@code null} if none.
     * This listener use weak reference, therefore it should not cause retention of
     * large amount of memory.
     */
    private TileObserver sourceListener;

    /**
     * Creates a new monolithic image.
     */
    MonolithicImage() {
    }

    /**
     * Returns all source images as an array.
     *
     * @return source images, or an empty array if none.
     */
    abstract RenderedImage[] getSourceImages();

    /**
     * Adds a listener by weak reference to all images that are writable or planar.
     * This method should be invoked in a synchronized block.
     */
    final void registerSourceListeners() {
        boolean registered = false;
        TileObserver listener = sourceListener;
        try {
            for (final RenderedImage source : getSourceImages()) {
                if (listener == null) {
                    listener = new Listener(this);
                }
                registered |= addTileObserver(source, listener);
            }
        } finally {
            if (registered) {
                sourceListener = listener;
            }
        }
    }

    /**
     * Removes the listener from all images that are writable or planar.
     * This method should be invoked in a synchronized block.
     */
    final void unregisterSourceListeners() {
        if (sourceListener != null) {
            for (final RenderedImage source : getSourceImages()) {
                removeTileObserver(source, sourceListener);
            }
            sourceListener = null;
        }
    }

    /**
     * A listener using weak reference for notifying the enclosed image that a source changed.
     */
    private static final class Listener extends WeakReference<MonolithicImage> implements PlanarImage.Observer {
        /**
         * Creates a new listener which will notify the given image.
         *
         * @param  target  the image to notify when a source changed.
         */
        Listener(final MonolithicImage target) {
            super(target);
        }

        /**
         * Invoked when a source image changed.
         * The enclosed image is notified only after the tile calculation finished.
         *
         * @param source   the image that owns the updated tile.
         * @param tileX    the <var>x</var> index of the tile that is being updated.
         * @param tileY    the <var>y</var> index of the tile that is being updated.
         * @param writing  {@code true} before writing, {@code false} after update.
         */
        @Override
        public void tileUpdate(final RenderedImage source, final int tileX, final int tileY, final boolean writing) {
            final MonolithicImage target = get();
            if (target == null) {
                removeTileObserver(source, this);
            } else if (writing) {
                /*
                 * Wait for completion before to mark source tile as updated. We should also ensure
                 * that the tile is not used before completion, but this is not yet implemented.
                 */
            } else {
                target.sourceTileChanged(source, tileX, tileY);
            }
        }
    }

    /**
     * Invoked when a tile of a source image has been updated.
     *
     * @param source  the image that own the tile which has been updated.
     * @param tileX   the <var>x</var> index of the tile that has been updated.
     * @param tileY   the <var>y</var> index of the tile that has been updated.
     *
     * @see ComputedImage#sourceTileChanged(RenderedImage, int, int)
     */
    protected abstract void sourceTileChanged(RenderedImage source, int tileX, int tileY);

    /**
     * Adds an observer to be notified when a tile is about to be modified.
     * A tile can be modified indirectly if one of the source images changed.
     * The given listener should be an instance of {@link Observer} in order
     * to be notified even if this image is not a {@link WritableRenderedImage}.
     *
     * @param  observer  the observer to notify, ignored if {@code null}.
     *         If already present, it will receive multiple notifications.
     */
    @Override
    public synchronized void addTileObserver(final TileObserver observer) {
        if (observer instanceof Observer || this instanceof WritableRenderedImage) {
            observers = WritableUntiledImage.addTileObserver(observers, observer);
        }
    }

    /**
     * Removes an observer from the list of observers notified when a tile is modified.
     * If the observer was not registered, nothing happens.
     * If the observer was registered for multiple notifications, it will now be registered for one fewer.
     *
     * @param  observer  the observer to stop notifying.
     */
    @Override
    public synchronized void removeTileObserver(final TileObserver observer) {
        observers = WritableUntiledImage.removeTileObserver(observers, observer);
    }

    /**
     * Notifies listeners that a tile is about to be modified or has been modified.
     *
     * @param  tileX    the <var>x</var> index of the tile to acquire or release.
     * @param  tileY    the <var>y</var> index of the tile to acquire or release.
     * @param  writing  {@code true} if the tile will be modified, or {@code false} if the modification is done.
     */
    final void fireTileUpdate(final int tileX, final int tileY, final boolean writing) {
        fireTileUpdate(observers, this, tileX, tileY, writing);
    }
}
