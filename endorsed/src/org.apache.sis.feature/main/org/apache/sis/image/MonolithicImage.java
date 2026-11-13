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
     * Creates a new monolithic image.
     */
    MonolithicImage() {
    }

    /**
     * The listener, or {@code null} if none.
     */
    private TileObserver listener;

    /**
     * Removes the listener from all images that are writable.
     * This method should be invoked in a synchronized block.
     *
     * @param  sources  the source images for which to remove the listener.
     */
    final void removeListeners(final RenderedImage[] sources) {
        if (listener != null) {
            for (final RenderedImage source : sources) {
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
     *
     * @param  sources  the source images for which to add a listener.
     */
    final void addListeners(final RenderedImage[] sources) {
        for (final RenderedImage source : sources) {
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
         */
        @Override
        public void tileUpdate(WritableRenderedImage source, int tileX, int tileY, boolean willBeWritable) {
            if (!willBeWritable) {
                final MonolithicImage target = get();
                if (target != null) {
                    target.recompute();
                } else {
                    source.removeTileObserver(this);
                }
            }
        }
    }

    /**
     * Invoked when the image needs to be recomputed.
     */
    protected abstract void recompute();
}
