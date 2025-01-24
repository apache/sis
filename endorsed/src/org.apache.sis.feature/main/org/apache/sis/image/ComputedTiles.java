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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.lang.ref.WeakReference;
import java.awt.Point;
import java.awt.image.TileObserver;
import java.awt.image.ImagingOpException;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.system.ReferenceQueueConsumer;
import org.apache.sis.util.Disposable;


/**
 * Weak reference to a {@link ComputedImage} image together with information about tile status.
 * This class also contains necessary information for releasing resources when image is disposed.
 * This class shall not contain any strong reference to the {@link ComputedImage}.
 *
 * <p>Despite the {@code ComputedTiles} class name, this class does not contain any reference
 * to the tiles. Instead, it contains keys for getting the tiles from {@link TileCache#GLOBAL}.
 * Consequently, this class "contains" the tiles only indirectly.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ComputedTiles extends WeakReference<ComputedImage> implements Disposable, TileObserver {
    /**
     * Whether a tile in the cache is ready for use or needs to be recomputed because one if its sources
     * changed its data. Those values are stored in {@link #cachedTiles} map.
     *
     * <ul>
     *   <li>{@code VALID} means that the tile, if presents, is ready for use. A tile may be non-existent in the cache
     *       despite being marked {@code VALID} if the tile has been garbage-collected after it has been marked.</li>
     *   <li>{@code DIRTY} means that the tile needs to be recomputed. If the tile is present, its data should be
     *       discarded but its storage space will be reused.</li>
     *   <li>{@code ERROR} means that the previous attempt to compute this tile failed.</li>
     *   <li>All other values means that the tile has been checkout out for a write operation.
     *       That value is incremented/decremented when the writable tile is acquired/released.
     *       Write operation status have precedence over the dirty state.</li>
     *   <li>{@code COMPUTING} is a special case of above point when calculation just started.</li>
     * </ul>
     */
    private static final int VALID = 0, DIRTY = -1, ERROR = -2, COMPUTING = 1;

    /**
     * Indices of all cached tiles. Used for removing tiles from the cache when the image is disposed.
     * Values can be {@link #ERROR}, {@link #DIRTY}, {@link #VALID} or counts of writers as unsigned
     * integers (including {@link #COMPUTING} and {@link #VALID} as special cases).
     *
     * All accesses to this collection must be synchronized.
     */
    private final Map<TileCache.Key, Integer> cachedTiles;

    /**
     * All {@link ComputedImage#sources} that are writable, or {@code null} if none.
     * This is used for removing tile observers when the {@link ComputedImage} is garbage-collected.
     */
    private WritableRenderedImage[] sources;

    /**
     * Creates a new weak reference to the given image and registers this {@link ComputedTiles}
     * as a listener of all given sources. The listeners will be automatically removed when the
     * {@link ComputedImage} is garbage collected.
     *
     * @param  image  the image for which to release tiles on garbage-collection.
     * @param  ws     sources to observe for changes, or {@code null} if none.
     */
    ComputedTiles(final ComputedImage image, final WritableRenderedImage[] ws) {
        super(image, ReferenceQueueConsumer.QUEUE);
        cachedTiles = new HashMap<>();
        sources = ws;
        if (ws != null) {
            int i = 0;
            try {
                while (i < ws.length) {
                    WritableRenderedImage source = ws[i++];     // `i++` must be before `addTileObserver(…)` call.
                    source.addTileObserver(this);
                }
            } catch (RuntimeException e) {
                unregister(ws, i, e);                           // `unregister(…)` will rethrow the given exception.
            }
        }
    }

    /**
     * Returns {@code true} if the given value is {@link #COMPUTING} or a greater unsigned value.
     * Returns {@code false} if the value is null, {@link #VALID}, {@link #DIRTY} or {@link #ERROR}.
     */
    private static boolean isWritable(final Integer value) {
        if (value == null) return false;
        final int n = value;                        // Negative if we have more than Integer.MAX_VALUE writers.
        return (n >= COMPUTING) || (n < ERROR);
    }

    /**
     * Returns {@code true} if the specified tile is checked out for a write operation.
     *
     * @param  key  indices of the tile to check.
     * @return whether the specified tile is checked out for a write operation.
     */
    final boolean isTileWritable(final TileCache.Key key) {
        final Integer value;
        synchronized (cachedTiles) {
            value = cachedTiles.get(key);
        }
        return isWritable(value);
    }

    /**
     * Returns {@code true} if the specified tile needs to be recomputed. An absent tile is considered as dirty.
     * If previous attempt to compute the tile failed, then an {@link ImagingOpException} is thrown again.
     *
     * @param  key  indices of the tile to check.
     * @return whether the specified tile needs to be recomputed.
     * @throws ImagingOpException if we already tried and failed to compute the specified tile.
     */
    final boolean isTileDirty(final TileCache.Key key) {
        final Integer value;
        synchronized (cachedTiles) {
            value = cachedTiles.get(key);
        }
        if (value != null) {
            switch (value) {
                case DIRTY: break;
                case ERROR: throw new ImagingOpException(key.error(Resources.Keys.TileErrorFlagSet_2));
                default:    return false;
            }
        }
        return true;
    }

    /**
     * If the specified tile is absent or {@link #DIRTY}, sets its status to {@link #COMPUTING} and
     * returns {@code true}. Otherwise if there are no errors, does nothing and returns {@code false}.
     *
     * @param  key  indices of the tile to compute if dirty.
     * @return whether the specified tile was absent or dirty.
     * @throws ImagingOpException if we already tried and failed to compute the specified tile.
     */
    final boolean trySetComputing(final TileCache.Key key) {
        final Integer value;
        synchronized (cachedTiles) {
            value = cachedTiles.putIfAbsent(key, COMPUTING);
            if (value == null || cachedTiles.replace(key, DIRTY, COMPUTING)) {
                return true;
            }
        }
        if (value == ERROR) {
            throw new ImagingOpException(key.error(Resources.Keys.TileErrorFlagSet_2));
        }
        return false;
    }

    /**
     * Increments the count of writers for the specified tile.
     * If the specified tile was marked dirty or in error, that previous status is discarded.
     *
     * @param  key  indices of the tile to mark writable.
     * @return {@code true} if the tile goes from having no writers to having one writer.
     * @throws ArithmeticException if too many writers.
     */
    final boolean startWrite(final TileCache.Key key) {
        Integer value = COMPUTING;                          // Do the boxing outside synchronized block.
        synchronized (cachedTiles) {
            value = cachedTiles.merge(key, value, ComputedTiles::increment);
        }
        return value == COMPUTING;
    }

    /**
     * Decrements the count of writers for the specified tile.
     *
     * @param  key      indices of the tile which was marked writable.
     * @param  success  whether the operation should be considered successful.
     * @return {@code true} if the tile goes from having one writer to having no writers.
     */
    final boolean endWrite(final TileCache.Key key, final boolean success) {
        final int status = success ? VALID : ERROR;
        Integer value = status;                             // Do the boxing outside synchronized block.
        synchronized (cachedTiles) {
            value = cachedTiles.merge(key, value, ComputedTiles::decrement);
        }
        return value == status;
    }

    /**
     * If the value is {@link #VALID}, {@link #DIRTY} or {@link #ERROR}, sets it to {@link #COMPUTING}.
     * Otherwise increments that value.
     *
     * @param  value      the value to increment.
     * @param  computing  must be {@link #COMPUTING}.
     * @return the incremented value.
     */
    private static Integer increment(final Integer value, final Integer computing) {
        final int n = value;
        switch (n) {
            case VALID:
            case DIRTY:
            case ERROR:     return computing;
            case ERROR - 1: throw new ArithmeticException();        // Unsigned integer overflow
            default:        return n + 1;                           // case COMPUTING or greater
        }
    }

    /**
     * If the value is {@link #VALID}, {@link #DIRTY}, {@link #ERROR} or {@link #COMPUTING},
     * sets that value to {@link #VALID} or {@link #ERROR}. Otherwise decrements that value.
     *
     * @param  value   the value to decrement.
     * @param  status  {@link #VALID} or {@link #ERROR}.
     * @return the decremented value.
     */
    private static Integer decrement(final Integer value, final Integer status) {
        final int n = value;
        if (n >= ERROR && n <= COMPUTING) {     // Do not use the ternary operator here.
            return status;
        } else {
            return n - 1;
        }
    }

    /**
     * Adds in the given list the indices of all tiles which are checked out for writing.
     * If the given list is {@code null}, then this method stops the search at the first
     * writable tile.
     *
     * @param  indices  the list where to add indices, or {@code null} if none.
     * @return whether at least one tile is checked out for writing.
     */
    final boolean getWritableTileIndices(final List<Point> indices) {
        synchronized (cachedTiles) {
            for (final Map.Entry<TileCache.Key, Integer> entry : cachedTiles.entrySet()) {
                if (isWritable(entry.getValue())) {
                    if (indices == null) return true;
                    indices.add(entry.getKey().indices());
                }
            }
        }
        return (indices != null) && !indices.isEmpty();
    }

    /**
     * Marks all tiles in the given range of indices as in need of being recomputed.
     * This method is invoked when some tiles of at least one source image changed.
     * All arguments, including maximum values, are inclusive.
     *
     * @param  error  {@code false} for marking valid tiles as dirty, or {@code true} for marking tiles in error.
     * @return {@code true} if at least one tile got its status updated.
     *
     * @see ComputedImage#markDirtyTiles(Rectangle)
     */
    final boolean markDirtyTiles(final int minTileX, final int minTileY, final int maxTileX, final int maxTileY, final boolean error) {
        final Integer search = error ? ERROR : VALID;
        final Integer dirty  = DIRTY;
        boolean updated = false;
        synchronized (cachedTiles) {
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                    final TileCache.Key key = new TileCache.Key(this, tileX, tileY);
                    updated |= cachedTiles.replace(key, search, dirty);
                }
            }
        }
        return updated;
    }

    /**
     * Invoked when a source is changing the content of one of its tile.
     * This method is interested only in events fired after the change is done.
     * The tiles that depend on the modified tile are marked in need to be recomputed.
     *
     * @param source          the image that own the tile which is about to be updated.
     * @param tileX           the <var>x</var> index of the tile that is being updated.
     * @param tileY           the <var>y</var> index of the tile that is being updated.
     * @param willBeWritable  if {@code true}, the tile is grabbed for writing; otherwise it is being released.
     */
    @Override
    public void tileUpdate(final WritableRenderedImage source, int tileX, int tileY, final boolean willBeWritable) {
        if (!willBeWritable) {
            final ComputedImage target = get();
            if (target != null) {
                target.sourceTileChanged(source, tileX, tileY);
            } else {
                /*
                 * Should not happen, unless maybe the source invoked this method before `dispose()` has done
                 * its work. Or maybe we have a bug in our code and this `ComputedTiles` is still alive when
                 * it should not. In any cases there is no point to continue observing the source.
                 */
                source.removeTileObserver(this);
            }
        }
    }

    /**
     * Invoked when the {@link ComputedImage} has been garbage-collected. This method removes all cached
     * tiles that were owned by the image and stops observing all sources. If the same {@link Raster} was
     * shared by many images, other images are not impacted.
     *
     * <p>This method should not perform other cleaning work because it is not guaranteed to be invoked.
     * In some case, there is nothing preventing this weak reference to be garbage collected before this
     * {@code dispose()} method is invoked. The case is: if {@code ComputedTiles} is not registered as a
     * {@link TileObserver} and if {@link TileCache#GLOBAL} does not contain any tile associated to this
     * {@link ComputedImage} in its key.</p>
     *
     * @see ComputedImage#dispose()
     */
    @Override
    public void dispose() {
        synchronized (cachedTiles) {
            cachedTiles.keySet().forEach(TileCache.Key::dispose);
            cachedTiles.clear();
        }
        final WritableRenderedImage[] ws = sources;
        if (ws != null) {
            unregister(ws, ws.length, null);
        }
    }

    /**
     * Stops observing writable sources for modifications. This method is invoked when the {@link ComputedImage}
     * is garbage collected. It may also be invoked for rolling back observer registrations if an error occurred
     * during {@link ComputedTiles} construction. This method clears the {@link #sources} field immediately for
     * allowing the garbage collector to release the sources in the event where this {@code ComputedTiles} would
     * live longer than expected.
     *
     * @param  ws       a copy of {@link #sources}. Cannot be null.
     * @param  i        index after the last source to stop observing.
     * @param  failure  if this method is invoked because an exception occurred, that exception.
     */
    private void unregister(final WritableRenderedImage[] ws, int i, RuntimeException failure) {
        sources = null;                     // Let GC to its work in case of error in this method.
        while (--i >= 0) try {
            ws[i].removeTileObserver(this);
        } catch (RuntimeException e) {
            if (failure == null) failure = e;
            else failure.addSuppressed(e);
        }
        if (failure != null) {
            throw failure;
        }
    }
}
