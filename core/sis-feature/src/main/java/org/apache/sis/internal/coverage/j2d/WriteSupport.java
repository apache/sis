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
package org.apache.sis.internal.coverage.j2d;

import java.util.Arrays;
import java.awt.image.TileObserver;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.util.ArraysExt;


/**
 * Helper methods for {@link WritableRenderedImage} implementations.
 *
 * <p>A future version of this class may extends {@code PlanarImage} or {@code ComputedImage}.
 * We have not yet decided which case would be useful.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final class WriteSupport {
    /**
     * Do not allow (for now) instantiation of this class.
     */
    private WriteSupport() {
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
     * Notifies all listeners that the specified tile has been checked out for writing.
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
}
