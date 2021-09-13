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
package org.apache.sis.math;

import java.io.Serializable;
import java.util.function.IntSupplier;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A vector whose values are the repetitions of the values given in a base vector.
 * This vector can be created as a result of {@link Vector#compress(double)}.
 * The intent is to handle the cases found in netCDF files where localization grids
 * (e.g. the {@code "longitude"} variable storing longitude values of all points in a grid)
 * contains a lot of repetitions.
 *
 * <p>{@link #cycleLength} is usually the length of the {@linkplain #base} vector, but not necessarily.
 * If {@link #occurrences} = 1 and {@code cycleLength} = 4 for example, then this class handles repetitions like below:</p>
 *
 * {@preformat text
 *    10 12 15 20
 *    10 12 15 20    ← new cycle
 *    10 12 15 20    ← new cycle
 *    10 12 15 20    ← new cycle
 *    …etc…
 * }
 *
 * If {@link #occurrences} &gt; 1, then this class handles repetitions in a different way
 * (in this example, {@link #cycleLength} is still 4):
 *
 * {@preformat text
 *    10 10 10 10
 *    12 12 12 12
 *    15 15 15 15
 *    20 20 20 20
 *    10 10 10 10    ← new cycle
 *    12 12 12 12
 *    …etc…
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
final class RepeatedVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3607036775685492552L;

    /**
     * The vector on which this vector is derived from.
     */
    private final Vector base;

    /**
     * Number of times that each {@linkplain #base} element appears in a row before to move
     * to the next {@code base} element. See class javadoc for more information.
     */
    private final int occurrences;

    /**
     * Length of the sequence of values to repeat, after conversion to base vector indices.
     * Usually equals to the length of the {@linkplain #base} vector but can also be smaller.
     * Shall not be greater than {@code base.size()}. See class javadoc for more information.
     */
    private final int cycleLength;

    /**
     * The size of this vector.
     * This is often {@link #cycleLength} × {@link #occurrences}, but not necessarily.
     */
    private final int size;

    /**
     * Creates a new vector of repeated data.
     *
     * @param base         the vector on which this vector is derived from.
     * @param occurrences  number of time that each element is repeated.
     * @param cycleLength  length of the sequence of values to repeat.
     * @param size         this vector size, usually {@code base.size() * repetition}.
     */
    RepeatedVector(final Vector base, final int occurrences, final int cycleLength, final int size) {
        this.base        = base;
        this.occurrences = occurrences;
        this.cycleLength = cycleLength;
        this.size        = size;
        assert cycleLength <= base.size() : cycleLength;
    }

    /**
     * Creates a vector of repeated data from the result of a call to {@link Vector#repetitions(int...)}.
     *
     * @param base         the vector on which this vector is derived from.
     * @param repetitions  results of {@link Vector#repetitions(int...)}. Must be non-empty.
     * @param tolerance    tolerance factor for compression of the base vector.
     */
    RepeatedVector(final Vector base, final int[] repetitions, final double tolerance) {
        size        = base.size();
        occurrences = repetitions[0];
        cycleLength = (repetitions.length >= 2) ? repetitions[1] : size / occurrences;
        this.base   = base.subSampling(0, occurrences, cycleLength).compress(tolerance);
    }

    /**
     * Converts the given index from this vector domain to an index in the {@linkplain #base} vector.
     */
    private int toBase(final int index) {
        ArgumentChecks.ensureValidIndex(size, index);
        return (index / occurrences) % cycleLength;
    }

    /**
     * Returns the type of values, which is inherited from the {@linkplain #base} vector.
     */
    @Override
    public final Class<? extends Number> getElementType() {
        return base.getElementType();
    }

    /** Forwards to the base vector. */
    @Override public final boolean isEmptyOrNaN()      {return base.isEmptyOrNaN();}
    @Override public final boolean isSinglePrecision() {return base.isSinglePrecision();}
    @Override public final boolean isInteger()         {return base.isInteger();}
    @Override public final boolean isUnsigned()        {return base.isUnsigned();}
    @Override public final int     size()              {return size;}
    @Override public final boolean isNaN      (int i)  {return base.isNaN      (toBase(i));}
    @Override public final double  doubleValue(int i)  {return base.doubleValue(toBase(i));}
    @Override public final float   floatValue (int i)  {return base.floatValue (toBase(i));}
    @Override public final long    longValue  (int i)  {return base.longValue  (toBase(i));}
    @Override public final int     intValue   (int i)  {return base.intValue   (toBase(i));}
    @Override public final short   shortValue (int i)  {return base.shortValue (toBase(i));}
    @Override public final byte    byteValue  (int i)  {return base.byteValue  (toBase(i));}
    @Override public final String  stringValue(int i)  {return base.stringValue(toBase(i));}
    @Override public final Number  get        (int i)  {return base.get        (toBase(i));}

    /**
     * The range of values in this vector is the range of values in the {@linkplain #base} vector
     * if we use all its data.
     */
    @Override
    public final NumberRange<?> range() {
        return (cycleLength == base.size()) ? base.range() : super.range();
    }

    /**
     * Overridden for efficiency in case {@link #base} itself overrides that method.
     * Overriding that method is optional; the default implementation would have worked.
     */
    @Override
    final NumberRange<?> range(final IntSupplier indices, final int count) {
        return base.range(() -> toBase(indices.getAsInt()), count);
    }

    /**
     * Do not allow setting values.
     */
    @Override
    public final Number set(int index, Number value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.CanNotStoreInVector_1, value));
    }

    /**
     * Returns the parameters used by this {@code RepeatedVector} instance on the assumption
     * that they are the result of a previous invocation to {@link Vector#repetitions(int...)}.
     */
    @Override
    public int[] repetitions(final int... candidates) {
        if (cycleLength * occurrences >= size) {
            return new int[] {occurrences};
        } else {
            return new int[] {occurrences, cycleLength};
        }
    }

    /**
     * Returns a vector whose value is the content of this vector repeated <var>count</var> times.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector repeat(final boolean eachValue, final int count) {
opti:   if (count > 1 && cycleLength * occurrences >= size) {
            final int n;
            if (eachValue) {
                if (cycleLength < base.size()) break opti;
                n = occurrences;
            } else {
                if (occurrences != 1) break opti;
                n = cycleLength;
            }
            return base.repeat(eachValue, Math.multiplyExact(n, count));
        }
        return super.repeat(eachValue, count);
    }

    /**
     * Returns {@code null} since the repetition of a sequence of numbers implies that there is no regular increment.
     * An exception to this rule would be if the {@linkplain #base} vector contains a constant value or if the repetition
     * is exactly 1, but we should not have created a {@code RepeatedVector} in such cases.
     */
    @Override
    public final Number increment(final double tolerance) {
        return null;
    }

    /**
     * Returns {@code this} since this vector is considered already compressed. Actually it may be possible to compress more
     * if the {@linkplain #base} vector has been modified after {@code RepeatedVector} construction. But it should not happen
     * since this vector is read-only and {@link Vector#compress(double)} recommends to not keep reference to the original vector.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Vector compress(final double tolerance) {
        return this;
    }

    /**
     * Informs {@link #pick(int...)} that this vector is backed by another vector.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Vector backingVector() {
        return base;
    }

    /**
     * Converts an array of indexes used by this vector to the indexes used by the backing vector.
     * This method must also check index validity.
     */
    @Override
    final int[] toBacking(int[] indices) {
        indices = indices.clone();
        for (int i=0; i<indices.length; i++) {
            indices[i] = toBase(indices[i]);
        }
        return indices;
    }

    /**
     * Implementation of {@link #subSampling(int,int,int)}.
     * Arguments validity has been verified by the caller.
     *
     * @param  first   index of the first value to be included in the returned view.
     * @param  step    the index increment in this vector between two consecutive values
     *                 in the returned vector. Can be positive, zero or negative.
     * @param  length  the length of the vector to be returned. Can not be greater than
     *                 the length of this vector, except if the {@code step} is zero.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    Vector createSubSampling(final int first, final int step, final int length) {
        /*
         * If the sub-range is such that there is no more repetition,
         * return the base vector (or a sub-range of it) directly.
         */
        if ((step % occurrences) == 0) {
            final int bs    = step  / occurrences;                      // Step in the base vector.
            final int lower = first / occurrences;                      // First index in the base vector (inclusive).
            final int upper = lower + (length-1) * bs;                  // Last index in the base vector (inclusive).
            if (lower >= 0 && lower <= upper && upper < base.size()
                    && (lower / cycleLength) == (upper / cycleLength))  // Lower et upper must be member of the same cycle.
            {
                return base.subSampling(lower, bs, length);
            }
        }
        /*
         * We still have repetitions. Return another RepeatedVector if possible.
         * Fallback on SubSampling wrapper only in last resort.
         */
        if (first < occurrences && (occurrences % step) == 0) {
            return new RepeatedVector(base, occurrences / step, cycleLength, length);
        }
        return super.createSubSampling(first, step, length);
    }
}
