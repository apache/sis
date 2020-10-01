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
package org.apache.sis.internal.referencing;

import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ComparisonMode;


/**
 * A {@link WraparoundTransform} where the number of cycles added or removed does not exceed a given limit.
 * The bound is determined by whether the coordinate to transform is before or after a median point.
 * If the coordinate is before the median, this class puts a limit on the number of cycles added.
 * If the coordinate is after  the median, this class puts a limit on the number of cycles removed.
 * The intent is to avoid that the lower bound of an envelope is shifted by a greater number of cycles
 * than the upper bound, which may result in lower bound becoming greater than upper bound.
 *
 * The final result is that envelopes transformed using {@code WraparoundInEnvelope} may be larger
 * than envelopes transformed using {@link WraparoundTransform} but should never be smaller.
 *
 * <h2>Mutability</h2>
 * <b>This class is mutable.</b> This class records the translations that {@link #shift(double)} wanted to apply
 * but could not because of the {@linkplain #limit} documented in above paragraph. When such missed translations
 * are detected, caller should {@linkplain #translate() translate the limit} and transform same envelope again.
 * We do that because each envelope transformation should use a consistent {@linkplain #limit} for all corners.
 * Since this strategy breaks usual {@link org.apache.sis.referencing.operation.transform.AbstractMathTransform}
 * contract about immutability, this class should be used only for temporary transforms to apply on an envelope.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class WraparoundInEnvelope extends WraparoundTransform {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1590996680159048327L;

    /**
     * The median source coordinate where the minimum or maximum number of cycles is determined.
     * This <em>source</em> coordinate is not necessarily the same as the median <em>target</em> coordinate
     * (which is set to zero by application of normalization matrix) because those medians were determined
     * from source and target envelopes, which do not need to be the same.
     */
    private final double sourceMedian;

    /**
     * Number of cycles at the {@linkplain #sourceMedian} position. This is the minimum or maximum number
     * of cycles to remove to a coordinate, depending if that coordinate is before or after the median.
     */
    private double limit;

    /**
     * The minimum and maximum number of {@linkplain #period period}s that {@link #shift(double)} wanted
     * to add to the coordinate before to be constrained to the {@link #limit}.
     */
    private double minCycles, maxCycles;

    /**
     * Whether {@link #minCycles} or {@link #maxCycles} has been updated.
     */
    private boolean minChanged, maxChanged;

    /**
     * Creates a new transform with a wraparound behavior in the given dimension.
     * Input and output values in the wraparound dimension shall be normalized in
     * the [−p/2 … +p/2] range where <var>p</var> is the period (e.g. 360°).
     */
    WraparoundInEnvelope(final int dimension, final int wraparoundDimension,
                         final double period, final double sourceMedian)
    {
        super(dimension, wraparoundDimension, period);
        this.sourceMedian = sourceMedian;
        reset();
    }

    /**
     * Copy constructor for {@link #redim(int)}.
     */
    private WraparoundInEnvelope(final WraparoundInEnvelope source, final int dimension) {
        super(dimension, source.wraparoundDimension, source.period);
        sourceMedian = source.sourceMedian;
        synchronized (source) {
            minCycles = maxCycles = limit = source.limit;
        }
    }

    /**
     * Returns an instance with the specified number of dimensions while keeping
     * {@link #wraparoundDimension} and {@link #period} unchanged.
     */
    @Override
    WraparoundTransform redim(final int n) {
        return new WraparoundInEnvelope(this, n);
    }

    /**
     * Applies the wraparound on the given value. This implementation ensures that coordinates smaller than
     * {@link #sourceMedian} before wraparound are still smaller than the (possibly shifted) median after wraparound,
     * and conversely for coordinates greater than the median.
     *
     * The final result is that envelopes transformed using {@code WraparoundInEnvelope} may be larger
     * than envelopes transformed using {@link WraparoundTransform} but should never be smaller.
     *
     * @param  x  the value on which to apply wraparound.
     * @return the value after wraparound.
     */
    @Override
    final synchronized double shift(final double x) {
        double n = Math.rint(x / period);
        if (x < sourceMedian) {
            if (n < limit) {
                if (n < minCycles) {
                    minCycles = n;
                    minChanged = true;
                }
                n = limit;
            }
        } else {
            if (n > limit) {
                if (n > maxCycles) {
                    maxCycles = n;
                    maxChanged = true;
                }
                n = limit;
            }
        }
        return x - n * period;
    }

    /**
     * Modifies this transform with a translation for enabling the wraparound that could not be applied in previous
     * {@link #shift(double)} executions. If this method returns {@code true}, then this transform computes different
     * output coordinates for the same input coordinates.
     *
     * <h4>Usage</h4>
     * This method can be invoked after transforming an envelope. If this method returns {@code true}, then
     * the same envelope should be transformed again and the new result added to the previous result (union).
     *
     * @return {@code true} if this transform has been modified.
     */
    final synchronized boolean translate() {
        if (minChanged) {
            minChanged = false;
            limit = minCycles;
            return true;
        }
        if (maxChanged) {
            maxChanged = false;
            limit = maxCycles;
            return true;
        }
        return false;
    }

    /**
     * Resets this transform to its initial state.
     */
    final synchronized void reset() {
        minCycles = maxCycles = limit = Math.rint(sourceMedian / period);
    }

    /**
     * Compares this transform with the given object for equality.
     *
     * @param  object  the object to compare with this transform.
     * @param  mode    ignored, can be {@code null}.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final WraparoundInEnvelope other = (WraparoundInEnvelope) object;
            return Numerics.equals(sourceMedian, other.sourceMedian) &&
                   Numerics.equals(limit, other.limit);
        }
        return false;
    }

    /**
     * Computes a hash code value for this transform.
     */
    @Override
    protected int computeHashCode() {
        return super.computeHashCode() + 7*Double.hashCode(limit);
    }
}
