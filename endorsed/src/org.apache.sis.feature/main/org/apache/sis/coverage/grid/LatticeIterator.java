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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.apache.sis.util.logging.Logging;


/**
 * Iterator over all grid coordinates included inside the grid extent, low and high values included.
 * Iteration is ordered from first dimension varying the fastest to last dimension varying the slowest.
 *
 * @param  <T>  the type of elements returned by this iterator.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see GridExtent#latticePointStream(boolean)
 */
abstract class LatticeIterator<T> implements Spliterator<T> {
    /**
     * The low coordinates followed by the high coordinates, inclusive.
     * This is either a copy of the {@link GridExtent#coordinates} array,
     * or a subregion if the iterator has been split.
     */
    private final long[] extent;

    /**
     * The current position.
     */
    final long[] current;

    /**
     * Whether the iteration is finished.
     */
    boolean done;

    /**
     * Creates a new iterator with the given low and high grid coordinate values.
     * The given {@code extent} array is initially a <strong>clone</strong> of the
     * {@link GridExtent#coordinates} array and may be modified in-place if the iterator is split.
     *
     * @param  extent  the low coordinates followed by the high coordinates (inclusive) in a modifiable array.
     */
    LatticeIterator(final long[] extent) {
        this.extent  = extent;
        this.current = Arrays.copyOf(extent, dimension());
    }

    /**
     * Creates an iterator which will iterate over a prefix of this iterator.
     * The extent of the prefix (in grid coordinates) is given in argument.
     *
     * @param  extent  extent of the prefix in a modifiable array.
     * @return a new iterator over a prefix of this iterator.
     */
    protected abstract LatticeIterator<T> createPrefixIterator(final long[] extent);

    /**
     * Returns the characteristics of this iterator: ordered, size known, without duplicated value.
     */
    @Override
    public final int characteristics() {
        return ORDERED | DISTINCT | SIZED | NONNULL | IMMUTABLE | SUBSIZED;
    }

    /**
     * Returns the exact number of points inside the extent if that number does not overflow.
     * In case of size exceeding the capacity of 64-bits integers, returns {@link Long#MAX_VALUE}.
     */
    @Override
    public final long estimateSize() {
        long count = 1;
        final int dimension = extent.length >>> 1;
        for (int i=0; i<dimension; i++) {
            try {
                long span = Math.subtractExact(extent[dimension + i], extent[i]);
                count = Math.multiplyExact(count, Math.incrementExact(span));
            } catch (ArithmeticException e) {
                Logging.ignorableException(GridExtent.LOGGER, GridExtent.class, "latticePointStream", e);
                return Long.MAX_VALUE;
            }
        }
        return count;
    }

    /**
     * Tries to return an iterator covering a prefix of the points covered by this iterator.
     * If successful, then after this method call this iterator covers the remaining suffix.
     */
    @Override
    public Spliterator<T> trySplit() {
        final int dimension = extent.length >>> 1;
        for (int i = dimension; --i >= 0;) {
            long span = Math.subtractExact(extent[dimension + i], extent[i]);
            if (span != 0) {
                final long[] prefix = extent.clone();
                final long separation = prefix[i] + (span >>> 1);
                prefix[i + dimension] = separation;
                current[i] = extent[i] = separation + 1;
                return createPrefixIterator(prefix);
            }
        }
        return null;
    }

    /**
     * Moves to the next point and checks whether there are more lattice points to iterate over.
     * Newly created iterators are positioned on their first point, which is guaranteed to exist.
     * This method is invoked <em>after</em> each point, in preparation for the next call to the
     * {@code tryAdvance(…)} method.
     *
     * @param  dimension  value of {@link #dimension()} (because often known by the caller).
     * @return whether there are more lattice points after this method call.
     */
    final boolean next(final int dimension) {
        for (int i=0; i<dimension; i++) {
            if (current[i] < extent[i + dimension]) {
                current[i]++;
                return true;
            }
            current[i] = extent[i];
        }
        return false;
    }

    /**
     * Returns the number of dimensions of grid points.
     * This is the value of {@link GridExtent#getDimension()}.
     *
     * @return number of dimensions of grid points.
     */
    protected final int dimension() {
        return extent.length >>> 1;
    }

    /**
     * Iterator over all grid coordinates included inside the grid extent, low and high values included.
     * Iteration is ordered from first dimension varying the fastest to last dimension varying the slowest.
     * The grid coordinates are returned directly (untransformed) as 64-bits integers.
     *
     * @see GridExtent#latticePointStream(boolean)
     */
    static final class Grid extends LatticeIterator<long[]> {
        /**
         * Creates a new iterator with the given low and high grid coordinate values.
         * The given array is initially a <strong>clone</strong> of {@link GridExtent#coordinates}.
         * That array may be modified in-place if the iterator is split.
         *
         * @param  extent  the low coordinates followed by the high coordinates (inclusive) in a modifiable array.
         */
        Grid(final long[] extent) {
            super(extent);
        }

        /**
         * Creates an iterator which will iterate over a prefix of this iterator.
         * The extent of the prefix (in grid coordinates) is given in argument.
         */
        @Override
        protected LatticeIterator<long[]> createPrefixIterator(final long[] extent) {
            return new Grid(extent);
        }

        /**
         * Preforms the given action on the next lattice point if it exists.
         *
         * @param  action  the action to execute.
         * @return whether a remaining point existed.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super long[]> action) {
            if (done) return false;
            action.accept(current.clone());
            done = !next(dimension());
            return true;
        }

        /**
         * Preforms the given action on all remaining lattice points.
         *
         * @param  action  the action to execute.
         */
        @Override
        public void forEachRemaining(final Consumer<? super long[]> action) {
            final int dimension = dimension();
            if (!done) {
                do action.accept(current.clone());
                while (next(dimension));
                done = true;
            }
        }
    }
}
