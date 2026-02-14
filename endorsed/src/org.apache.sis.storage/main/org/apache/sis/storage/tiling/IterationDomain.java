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
package org.apache.sis.storage.tiling;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.awt.image.RenderedImage;


/**
 * A request for tiles from a rendered image.
 * Contrarily to {@link TiledGridCoverage.AOI}, this request works with arbitrary {@link RenderedImage} and does
 * not manage a tile cache. Tile caching is assumed to be managed by the {@code RenderedImage} implementation.
 * This class is designed for use with {@link Spliterator} with parallelism.
 *
 * <h2>Iteration order</h2>
 * Iteration order is unspecified. Current implementation iterates from left to right, then top to bottom.
 * But future implementations may support Hilbert order. If the iteration is split, the iteration order is
 * interleaved: for example with two threads, the first tile is given to the first thread, the second tile
 * is given to the second thread, the third tile is given to the first thread, <i>etc.</i> The intend is to
 * stay relatively close to a reading of tiles in sequential order when all threads are executed in parallel.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <T>  type of tiles on which the {@link Spliterator} will iterate.
 */
abstract class IterationDomain<T> {
    /**
     * Minimal column index of the region on which to iterate.
     */
    private final int xmin;

    /**
     * Minimal row index of the region on which to iterate.
     */
    private final int ymin;

    /**
     * Number of columns on which to iterate.
     * This is {@code (xmax + 1) - xmin} where {@code xmax} is inclusive.
     */
    private final long width;

    /**
     * Maximum value (exclusive) of the indexes of the tiles on which to iterate.
     * Index is computed using the row-major convention.
     */
    private final long limit;

    /**
     * Creates a new request for tile iterators.
     *
     * @param xmin  first column index of tiles, inclusive.
     * @param xmin  first row index of tiles, inclusive.
     * @param xmax  last column index of tiles, inclusive.
     * @param ymax  last row index of tiles, inclusive.
     */
    protected IterationDomain(final int xmin, final int ymin, final int xmax, final int ymax) {
        this.xmin  = xmin;
        this.ymin  = ymin;
        this.width = (xmax + 1L) - xmin;
        this.limit = Math.multiplyExact(width, (ymax + 1L) - ymin);
    }

    /**
     * Creates a new item for the tile at the given indexes. If this method returns {@code null},
     * then the tile is assumed missing and this iterator searches for the next tile.
     *
     * @param  tileX  column index of the tile.
     * @param  tileY  row index of the tile.
     * @return item for the tile at the given indexes, or {@code null} if missing.
     */
    protected abstract T createTile(int tileX, int tileY);

    /**
     * Creates the first tile, or returns {@code null} if the tile is missing.
     */
    final T createFirstTile() {
        return createTile(xmin, ymin);
    }

    /**
     * Returns a new iterator over the tiles.
     *
     * @return a new iterator.
     */
    public final Spliterator<T> iterator() {
        return new Iterator();
    }

    /**
     * Iterator over the tiles.
     */
    private final class Iterator implements Spliterator<T> {
        /**
         * Row-major index of the next tile to return.
         */
        private long index;

        /**
         * Increment to apply on {@link #index} between each iteration.
         */
        private int increment;

        /**
         * Creates a new iterator which will initially traverse all tiles.
         */
        Iterator() {
            index = Math.multiplyExact(width, ymin);
            increment = 1;
        }

        /**
         * Creates a new iterator which will iterate over half of the tiles covered by the given iterator.
         * This constructor modifies the supplied iterator for covering the other half.
         *
         * @param  parent  the iterator to split. It will be modified by this method call.
         */
        private Iterator(final Iterator parent) {
            index = parent.index;
            parent.index += parent.increment;
            increment = parent.increment *= 2;
        }

        /**
         * Returns, if possible, an iterator covering half of the tiles covered by this iterator.
         * On return, this iterator will cover the other half.
         *
         * @return an iterator covering half of the tiles, or {@code null} if this iterator cannot be partitioned.
         */
        @Override
        public Spliterator<T> trySplit() {
            return (limit - index) > increment ? new Iterator(this) : null;
        }

        /**
         * Returns the number of remaining tiles in the iteration.
         */
        @Override
        public long estimateSize() {
            return (limit - index) / increment;
        }

        /**
         * In the context of this iterator, this is synonymous of {@link #estimateSize()}.
         */
        @Override
        public long getExactSizeIfKnown() {
            return estimateSize();
        }

        /**
         * Returns the characteristics of the iteration. The number of tiles is known in advance ({@link #SIZED}),
         * null values are not allowed ({@link #NONNULL}), there will be no tiles at same indexes ({@link #DISTINCT}),
         * and the tile matrix will not change ({@link #IMMUTABLE}). This iterator makes no guaranteed about iteration
         * order (i.e., not {@link #ORDERED}).
         *
         * <p>Note that {@link #IMMUTABLE} is not a promise that the pixel values will not change,
         * as the image may be writable. But this iterator is concerned only about the set of tiles,
         * not the content of those tiles.</p>
         */
        @Override
        public int characteristics() {
            return SIZED | SUBSIZED | NONNULL | DISTINCT | IMMUTABLE;
        }

        /**
         * If a remaining tile exists, performs the given action on it.
         *
         * @param  action  the action to execute on the next tile.
         * @return whether the action has been performed.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            while (index < limit) {
                final int tileY = Math.toIntExact(index / width);
                final int tileX = Math.toIntExact(index % width + xmin);
                final T   tile  = createTile(tileX, tileY);
                index += increment;
                if (tile != null) {
                    action.accept(tile);
                    return true;
                }
            }
            return false;
        }

        /**
         * Performs the given action on all remaining tiles.
         *
         * @param  action  the action to execute on all remaining tiles.
         */
        @Override
        public void forEachRemaining(final Consumer<? super T> action) {
            while (index < limit) {
                final int tileY = Math.toIntExact(index / width);
                final int tileX = Math.toIntExact(index % width + xmin);
                final T   tile  = createTile(tileX, tileY);
                index += increment;
                if (tile != null) {
                    action.accept(tile);
                }
            }
        }
    }
}
