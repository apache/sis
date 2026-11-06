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

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import static org.apache.sis.referencing.internal.shared.CoordinateOperations.MAXIMUM_BUFFER_SIZE;
import org.apache.sis.util.collection.BackingStoreException;


/**
 * Iterator over all grid coordinates included inside the grid extent, transformed to <abbr>CRS</abbr> coordinates.
 * If a {@link TransformException} is thrown during traversal, it will be wrapped in a {@link BackingStoreException}.
 *
 * @param  <T>  the type of elements returned by this iterator: {@code double[]} or {@link Double}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class GridPointIterator<T> extends LatticeIterator<T> {
    /**
     * The transform to apply on grid coordinates for obtaining target coordinates.
     */
    private final MathTransform gridToCRS;

    /**
     * Number of dimensions of the coordinate tuples to return.
     */
    private final int crsDimension;

    /**
     * A buffer for transforming a group of many points in one transform call.
     */
    private final double[] buffer;

    /**
     * Index after the last valid coordinate in the {@linkplain #buffer}.
     */
    private int limit;

    /**
     * Index of the next coordinate in the {@linkplain #buffer} to return.
     */
    private int currentPosition;

    /**
     * Creates a new iterator with the given low and high grid coordinate values and the given transform.
     * The given array is initially a <strong>clone</strong> of {@link GridExtent#coordinates}.
     *
     * @param  extent     the low coordinates followed by the high coordinates (inclusive) in a modifiable array.
     * @param  gridToCRS  the transform to apply on each grid coordinate values.
     */
    GridPointIterator(final long[] extent, final MathTransform gridToCRS) {
        super(extent);
        this.gridToCRS = gridToCRS;
        crsDimension = gridToCRS.getTargetDimensions();
        final int dimension = Math.max(crsDimension, gridToCRS.getSourceDimensions());
        final int capacity  = (int) Math.min(estimateSize(), Math.max(MAXIMUM_BUFFER_SIZE / dimension, 1));
        buffer = new double[capacity * dimension];
    }

    /**
     * Creates a new iterator which will iterate over a prefix of the given iterator.
     *
     * @param  suffix  the iterator which will become a suffix later.
     * @param  prefix  extent of the prefix of the given iterator.
     */
    GridPointIterator(final GridPointIterator<T> suffix, final long[] prefix) {
        this(prefix, suffix.gridToCRS);
    }

    /**
     * Tests whether there are more points to return and move the cursor to the next point.
     * Newly created iterator are positioned (from the point of view of this method) before the first point.
     * This method is invoked before each point to return, for example in a {@code while (next()) {…}} loop.
     *
     * @return whether the next point exists and can be returned.
     */
    protected final boolean next() {
        if (currentPosition >= limit) {
            if (done) {
                return false;
            }
            limit = 0;
            currentPosition = 0;
            final int dimension = dimension();
            int j = 0;
            do {
                for (int i=0; i<dimension; i++) {
                    buffer[j++] = current[i];
                }
                if (!next(dimension)) {
                    done = true;
                    break;
                }
            } while (j < buffer.length);
            try {
                gridToCRS.transform(buffer, 0, buffer, 0, j / dimension);
            } catch (TransformException e) {
                throw new BackingStoreException(e);
            }
            limit = j;
        }
        return true;
    }

    /**
     * Returns the multi-dimensional coordinate values at the current position.
     * This method or the {@link #coordinate()} method must be invoked
     * exactly once after {@link #next()} returned {@code true}.
     *
     * @return the coordinate values.
     */
    protected final double[] coordinates() {
        final double[] coordinates = new double[crsDimension];
        System.arraycopy(buffer, currentPosition, coordinates, 0, crsDimension);
        currentPosition += crsDimension;
        return coordinates;
    }

    /**
     * Returns the one-dimensional coordinate value at the current position.
     * This method or the {@link #coordinates()} method must be invoked
     * exactly once after {@link #next()} returned {@code true}.
     *
     * @return the coordinate value.
     */
    protected final double coordinate() {
        return buffer[currentPosition++];
    }

    /**
     * Iterator over coordinates in a <abbr>CRS</abbr> having an arbitrary number of dimensions.
     */
    static final class MultiDimensional extends GridPointIterator<double[]> {
        /**
         * Creates a new iterator with the given low and high grid coordinate values and the given transform.
         * The given array is initially a <strong>clone</strong> of {@link GridExtent#coordinates}.
         *
         * @param  extent     the low coordinates followed by the high coordinates (inclusive) in a modifiable array.
         * @param  gridToCRS  the transform to apply on each grid coordinate values.
         */
        MultiDimensional(final long[] extent, final MathTransform gridToCRS) {
            super(extent, gridToCRS);
        }

        /**
         * Creates a new iterator which will iterate over a prefix of the given iterator.
         *
         * @param  suffix  the iterator which will become a suffix later.
         * @param  prefix  extent of the prefix of the given iterator.
         */
        MultiDimensional(final MultiDimensional suffix, final long[] prefix) {
            super(suffix, prefix);
        }

        /**
         * Creates an iterator which will iterate over a prefix of this iterator.
         * The extent of the prefix (in grid coordinates) is given in argument.
         *
         * @return a new iterator over a prefix of this iterator.
         */
        @Override
        protected LatticeIterator<double[]> createPrefixIterator(final long[] extent) {
            return new MultiDimensional(this, extent);
        }

        /**
         * Preforms the given action on the next point if it exists.
         * The coordinates are transformed in batch for performance reasons.
         *
         * @param  action  the action to execute.
         * @return whether a remaining point existed.
         */
        @Override
        public boolean tryAdvance(final Consumer<? super double[]> action) {
            if (next()) {
                action.accept(coordinates());
                return true;
            }
            return false;
        }

        /**
         * Preforms the given action on all remaining lattice points.
         *
         * @param  action  the action to execute.
         */
        @Override
        public void forEachRemaining(final Consumer<? super double[]> action) {
            while (next()) {
                action.accept(coordinates());
            }
        }
    }

    /**
     * Iterator over coordinates in a <abbr>CRS</abbr> having exactly one dimension.
     */
    static final class OneDimensional extends GridPointIterator<Double> implements Spliterator.OfDouble {
        /**
         * Creates a new iterator with the given low and high grid coordinate values and the given transform.
         * The given array is initially a <strong>clone</strong> of {@link GridExtent#coordinates}.
         *
         * @param  extent     the low coordinates followed by the high coordinates (inclusive) in a modifiable array.
         * @param  gridToCRS  the transform to apply on each grid coordinate values.
         */
        OneDimensional(final long[] extent, final MathTransform gridToCRS) {
            super(extent, gridToCRS);
        }

        /**
         * Creates a new iterator which will iterate over a prefix of the given iterator.
         *
         * @param  suffix  the iterator which will become a suffix later.
         * @param  prefix  extent of the prefix of the given iterator.
         */
        OneDimensional(final OneDimensional suffix, final long[] prefix) {
            super(suffix, prefix);
        }

        /**
         * Creates an iterator which will iterate over a prefix of this iterator.
         * The extent of the prefix (in grid coordinates) is given in argument.
         *
         * @return a new iterator over a prefix of this iterator.
         */
        @Override
        protected LatticeIterator<Double> createPrefixIterator(final long[] extent) {
            return new OneDimensional(this, extent);
        }

        /**
         * Tries to return an iterator covering a prefix of the points covered by this iterator.
         * If successful, then after this method call this iterator covers the remaining suffix.
         */
        @Override
        public Spliterator.OfDouble trySplit() {
            return (Spliterator.OfDouble) super.trySplit();
        }

        /**
         * Preforms the given action on the next point if it exists.
         * The coordinates are transformed in batch for performance reasons.
         *
         * @param  action  the action to execute.
         * @return whether a remaining point existed.
         */
        @Override
        public boolean tryAdvance(final DoubleConsumer action) {
            if (next()) {
                action.accept(coordinate());
                return true;
            }
            return false;
        }

        /**
         * Preforms the given action on all remaining lattice points.
         *
         * @param  action  the action to execute.
         */
        @Override
        public void forEachRemaining(final DoubleConsumer action) {
            while (next()) {
                action.accept(coordinate());
            }
        }
    }
}
