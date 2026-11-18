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


/**
 * Base classes of images computed in a monolithic way.
 * All tiles are computed immediately when a tile is requested.
 * This behavior is of course not desired, which is why this base class is not public.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class MonolithicImage extends PlanarImage {
    /**
     * The common listener registered on all source images, or {@code null} if none.
     * This listener use weak reference, therefore it should not cause retention of
     * large amount of memory.
     */
    private TileObserver sourceListener;

    /**
     * Whether a listener to writable images has been registered.
     * This is done only when first needed and uses weak reference.
     */
    private boolean hasAddedListeners;

    /**
     * Creates a new monolithic image.
     */
    MonolithicImage() {
    }

    /**
     * Returns whether this image may notify its observers about tile updates.
     * A value of {@code false} means that this image will not be notified about changes in its source,
     * therefore this image will not forward tile update events, therefore observers registered on this
     * class can be ignored in order to reduce the risk of memory leaks (listeners can be other images).
     * In case of doubt (implementations that we do not control), conservatively returns {@code true}.
     *
     * @return whether tile update events may occur.
     */
    @Override
    final boolean mayFireTileUpdates() {
        registerSourceListeners();      // Does nothing if already registered.
        return sourceListener != null;
    }

    /**
     * Returns all source images as an array.
     *
     * @return source images, or an empty array if none.
     */
    abstract RenderedImage[] getSourceImages();

    /**
     * Adds a listener by weak reference to all images that are writable or planar.
     * If a listener has already been added, then this method does nothing.
     * This method should be invoked in a synchronized block.
     */
    final void registerSourceListeners() {
        assert Thread.holdsLock(this);
        if (hasAddedListeners) {
            return;
        }
        hasAddedListeners = true;       // Set first for making only one attempt in case of failure.
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
        assert Thread.holdsLock(this);
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
    private static final class Listener extends WeakReference<MonolithicImage> implements Observer {
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
}
