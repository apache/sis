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

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.lang.ref.Reference;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.pending.jdk.JDK16;


/**
 * A cache of tiles computed by {@link ComputedImage}. A common cache is shared by all images.
 * Tiles are kept by strong references until a memory usage limit is reached, in which case
 * the references of oldest tiles become soft references.
 *
 * <p>The same {@link Raster} may be shared by many images. Removing the tiles of an image
 * does not impact other images even if they share the same rasters.</p>
 *
 * <h2>Design note</h2>
 * The use of a common cache for all images makes easier to set an application-wide limit
 * (for example 25% of available memory). The use of soft reference does not cause as much
 * memory retention as it may seem because those references are hold only as long as the
 * image exists. When an image is garbage collected, the corresponding soft references are
 * {@linkplain Key#dispose() cleaned}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class TileCache extends Cache<TileCache.Key, Raster> {
    /**
     * The instance shared by all image operations. Current version does not allow to specify
     * a custom cache for some images, but a future version may allow that.
     */
    static final TileCache GLOBAL = new TileCache();

    /**
     * Creates a new tile cache. We put an arbitrary limit of 25% of available memory.
     * If more tiles are created, some strong references will become soft references.
     * Because strong references may be kept by the JVM, the amount of memory actually
     * used may be greater than this limit. However, those references are cleaned when the
     * image owning those tiles is {@linkplain ComputedTiles#dispose() garbage collected}.
     */
    private TileCache() {
        super(100, Runtime.getRuntime().maxMemory() / 4, true);
    }

    /**
     * Returns an estimation of the memory consumption of the given tile.
     *
     * @param  tile  the tile for which to estimate memory usage.
     * @return memory used by the given tile, in bytes.
     */
    @Override
    protected int cost(final Raster tile) {
        long numBits = Math.multiplyFull(tile.getWidth(), tile.getHeight()) * tile.getNumBands();
        final DataBuffer buffer = tile.getDataBuffer();
        if (buffer != null) try {
            numBits *= DataBuffer.getDataTypeSize(buffer.getDataType());
        } catch (IllegalArgumentException e) {
            numBits *= Integer.SIZE;                // Conservatively assume 32 bits values.
        }
        return Numerics.clamp(numBits / Byte.SIZE);
    }

    /**
     * Forces the removal of all garbage collected tiles.
     * This method should not need to be invoked.
     * It is provided as a debugging tools when suspecting a memory leak.
     *
     * @return {@code true} if some entries have been removed as a result of this method call.
     */
    @Override
    public boolean flush() {
        boolean changed = keySet().removeIf(Key::isEmpty);
        changed |= super.flush();
        return changed;
    }

    /**
     * A compound key identifying a tile of a {@link ComputedImage}.
     */
    static final class Key {
        /**
         * The image which own the tile as a weak reference. All {@code TileCache.Key} instances
         * for the same image will share the same reference.  Consequently, it is okay to compare
         * {@code image} fields directly instead of {@code image.get()}.
         */
        private final ComputedTiles image;

        /**
         * Index of the tile owned by the image.
         */
        private final int tileX, tileY;

        /**
         * Creates a new key identifying a tile or a cached image.
         *
         * @param  image  the image which own the tile.
         * @param  tileX  the column index of the cached tile.
         * @param  tileY  the row index of the cached tile.
         */
        Key(final ComputedTiles image, final int tileX, final int tileY) {
            this.image = image;
            this.tileX = tileX;
            this.tileY = tileY;
        }

        /**
         * Returns the tile indices.
         */
        final Point indices() {
            return new Point(tileX, tileY);
        }

        /**
         * Returns the error message when this tile cannot be computed.
         *
         * @param  key  {@link Resources.Keys#CanNotComputeTile_2} or {@link Resources.Keys#TileErrorFlagSet_2}.
         */
        final String error(final short key) {
            return Resources.format(key, tileX, tileY);
        }

        /**
         * Removes the raster associated to this key. This method is invoked for all tiles in an image being disposed.
         * The disposal may happen either by an explicit call to {@link ComputedImage#dispose()}, or because the image
         * has been {@linkplain ComputedTiles#dispose() garbage collected}.
         */
        final void dispose() {
            GLOBAL.remove(this);
        }

        /**
         * Returns {@code true} if the reference to the image has been cleared.
         * The {@link #dispose()} should have been invoked in such cases.
         */
        final boolean isEmpty() {
            return JDK16.refersTo(image, null);
        }

        /**
         * Returns a hash code value for this key. Note that this is okay to use {@link #image} directly
         * in hash code computation instead of {@link Reference#get()} because we maintain a one-to-one
         * relationship between {@link ComputedImage} and its {@link Reference}.
         */
        @Override
        public int hashCode() {
            /*
             * Dispatch tileX and tileY on approximately two halves of 32 bits integer.
             * 65563 is a prime number close to 65536, the capacity of 16 bits integers.
             */
            return System.identityHashCode(image) + tileX + 65563 * tileY;
        }

        /**
         * Compares this key with the given object for equality. See {@link #hashCode()} for a note about
         * direct comparison of {@link #image} references.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof TileCache.Key) {
                final TileCache.Key k = (TileCache.Key) obj;
                return image == k.image && tileX == k.tileX && tileY == k.tileY;
            }
            return false;
        }

        /**
         * Returns a string representation of this key for debugging purposes.
         */
        @Override
        public String toString() {
            return getClass().getSimpleName() + '[' + tileX + ", " + tileY + ']';
        }
    }
}
