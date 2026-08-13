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
package org.apache.sis.gml;

import java.util.Arrays;
import org.apache.sis.geometries.GeometryFactory;
import org.apache.sis.geometries.PointSequence;
import org.apache.sis.geometries.math.DataType;
import org.apache.sis.geometries.math.NDArrays;
import org.apache.sis.geometries.math.SampleSystem;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Accumulator of coordinate tuples read from a GML document, and factory of the
 * {@link PointSequence} that the Apache SIS geometry model is built upon.
 *
 * <p>Coordinates are accumulated as a single flat {@code double[]} of {@code size() × dimension()}
 * ordinates, which is the layout that {@link NDArrays#of(SampleSystem, double...)} expects. The
 * tuple width is <em>not</em> known in advance: it is fixed by the first tuple appended and then
 * enforced for every subsequent one, because an Apache SIS
 * {@link org.apache.sis.geometries.math.Array} is strictly rectangular — there is no per-tuple
 * width, and no convention by which a missing ordinate could be marked absent. A GML coordinate
 * list mixing 2-D and 3-D tuples is therefore rejected rather than padded: padding with {@code 0}
 * would invent coordinates, and padding with {@link Double#NaN} would silently poison every
 * downstream computation.</p>
 *
 * <p>The coordinate reference system is supplied only at {@linkplain #build build} time, since the
 * CRS and the tuple width must agree and the width is known only once parsing of the element is
 * finished. See {@link GMLCRS#forDimension GMLCRS.forDimension(…)} for how the two are reconciled.</p>
 *
 * <p>This class is not thread-safe and is meant to be used once per GML geometry element.</p>
 *
 * @author  Johann Sorel (Geomatys)
 */
final class PositionListBuilder {
    /**
     * The accumulated ordinates, in row-major order ({@code x₀ y₀ … x₁ y₁ …}).
     * Only the first {@link #count} elements are meaningful.
     */
    private double[] values;

    /**
     * Number of meaningful ordinates in {@link #values}. Always a multiple of {@link #dimension}.
     */
    private int count;

    /**
     * Number of ordinates per tuple, or 0 if no tuple has been appended yet.
     */
    private int dimension;

    /**
     * Creates an empty builder.
     */
    PositionListBuilder() {
        values = new double[12];        // 4 tuples of 3 ordinates.
    }

    /**
     * Returns the number of ordinates per tuple, or 0 if no tuple has been appended yet.
     */
    final int dimension() {
        return dimension;
    }

    /**
     * Returns the number of tuples appended so far.
     */
    final int size() {
        return (dimension != 0) ? count / dimension : 0;
    }

    /**
     * Returns {@code true} if no tuple has been appended.
     */
    final boolean isEmpty() {
        return count == 0;
    }

    /**
     * Returns a trimmed copy of the accumulated ordinates, in row-major order.
     * The returned array has {@code size() × dimension()} elements.
     */
    final double[] toFlatArray() {
        return Arrays.copyOf(values, count);
    }

    /**
     * Appends one coordinate tuple.
     *
     * @param  tuple  the ordinates of the tuple to append. Must not be empty, and must have the
     *                same length as every tuple appended before it.
     * @throws DataStoreContentException if the tuple width differs from the width already
     *         established by a previous call.
     */
    final void add(final double... tuple) throws DataStoreContentException {
        ensureWidth(tuple.length);
        if (count + tuple.length > values.length) {
            values = Arrays.copyOf(values, Math.max(count + tuple.length, values.length * 2));
        }
        System.arraycopy(tuple, 0, values, count, tuple.length);
        count += tuple.length;
    }

    /**
     * Appends several coordinate tuples given as a flat sequence of ordinates.
     *
     * @param  ordinates  the ordinates of all the tuples, in row-major order.
     * @param  width      the number of ordinates per tuple. Must divide {@code ordinates.length}.
     * @throws DataStoreContentException if {@code width} differs from the width already established
     *         by a previous call, or does not divide {@code ordinates.length}.
     */
    final void addFlat(final double[] ordinates, final int width) throws DataStoreContentException {
        if (width <= 0 || ordinates.length % width != 0) {
            throw new DataStoreContentException("A GML coordinate list of " + ordinates.length
                    + " ordinates is not a whole number of " + width + "-dimensional tuples.");
        }
        ensureWidth(width);
        if (count + ordinates.length > values.length) {
            values = Arrays.copyOf(values, Math.max(count + ordinates.length, values.length * 2));
        }
        System.arraycopy(ordinates, 0, values, count, ordinates.length);
        count += ordinates.length;
    }

    /**
     * Establishes the tuple width on the first call, and verifies it on subsequent calls.
     */
    private void ensureWidth(final int width) throws DataStoreContentException {
        if (width == 0) {
            throw new DataStoreContentException("A GML coordinate tuple cannot be empty.");
        }
        if (dimension == 0) {
            dimension = width;
        } else if (dimension != width) {
            throw new DataStoreContentException("Inconsistent coordinate tuple width in a single GML"
                    + " coordinate list: got " + width + " ordinates after " + dimension + ".");
        }
    }

    /**
     * Builds the point sequence for the accumulated tuples.
     *
     * <p>The coordinate reference system of the result is derived from {@code inScope} and from the
     * accumulated tuple width by {@link GMLCRS#forDimension GMLCRS.forDimension(…)}: the two must
     * agree, because an Apache SIS point sequence reports one dimension only.</p>
     *
     * @param  inScope  the CRS resolved for the enclosing GML element, or {@code null} if the
     *                  document declared none at any enclosing level.
     * @return the point sequence, possibly empty.
     * @throws DataStoreContentException if the accumulated tuple width contradicts {@code inScope}.
     * @throws DataStoreReferencingException if the CRS needed to reconcile the two cannot be created.
     */
    final PointSequence build(final CoordinateReferenceSystem inScope)
            throws DataStoreContentException, DataStoreReferencingException
    {
        /*
         * An empty sequence still needs a CRS, since `ArraySequence` rejects a null one. Two
         * dimensions is the GML default, and is what `srsDimension` defaults to as well.
         */
        if (count == 0) {
            final SampleSystem ss = SampleSystem.of(GMLCRS.forDimension(inScope, (dimension != 0) ? dimension : 2));
            return GeometryFactory.createSequence(NDArrays.of(ss, DataType.DOUBLE, 0));
        }
        final SampleSystem ss = SampleSystem.of(GMLCRS.forDimension(inScope, dimension));
        return GeometryFactory.createSequence(NDArrays.of(ss, toFlatArray()));
    }
}
