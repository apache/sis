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


/**
 * Base classes of images computed in a monolithic way.
 * All tiles are computed immediately when a tile is requested.
 * This behavior is of course not desired, which is why this base class is not public.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class MonolithicImage extends PlanarImage {
    /**
     * The listener, or {@code null} if none.
     */
    private TileObserver listener;

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
     * Removes the listener from all images that are writable.
     * This method should be invoked in a synchronized block.
     */
    final void unregisterSourceListeners() {
        if (listener != null) {
            for (final RenderedImage source : getSourceImages()) {
                if (source instanceof WritableRenderedImage) {
                    ((WritableRenderedImage) source).removeTileObserver(listener);
                }
            }
            listener = null;
        }
    }

    /**
     * Adds a listener by weak reference to all images that are writable.
     * This method should be invoked in a synchronized block.
     */
    final void registerSourceListeners() {
        for (final RenderedImage source : getSourceImages()) {
            if (source instanceof WritableRenderedImage) {
                if (listener == null) {
                    listener = new Listener(this);
                }
                ((WritableRenderedImage) source).addTileObserver(listener);
            }
        }
    }

    /**
     * A listener using weak reference for notifying the enclosed image that a source changed.
     */
    private static final class Listener extends WeakReference<MonolithicImage> implements TileObserver {
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
         * @param source  the image that owns the updated tile.
         * @param tileX   the <var>x</var> index of the tile that is being updated.
         * @param tileY   the <var>y</var> index of the tile that is being updated.
         * @param willBeWritable  {@code true} before writing, {@code false} after released.
         */
        @Override
        public void tileUpdate(final WritableRenderedImage source, final int tileX, final int tileY,
                               final boolean willBeWritable)
        {
            final MonolithicImage target = get();
            if (target == null) {
                source.removeTileObserver(this);
            } else if (willBeWritable) {
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
