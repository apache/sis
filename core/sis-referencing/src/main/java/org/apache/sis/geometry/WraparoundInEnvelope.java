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
package org.apache.sis.geometry;

import java.util.function.UnaryOperator;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.WraparoundTransform;
import org.apache.sis.util.ArraysExt;


/**
 * A {@link WraparoundTransform} where the number of cycles added or removed does not exceed a given limit.
 * The bound is determined by whether the coordinate to transform is before or after a median point.
 * If the coordinate is before the median, this class puts a limit on the number of cycles added.
 * If the coordinate is after  the median, this class puts a limit on the number of cycles removed.
 * The intent is to avoid that the lower bound of an envelope is shifted by a greater number of cycles
 * than the upper bound, which may result in lower bound becoming greater than upper bound.
 *
 * <p>The median point is {@link #sourceMedian}.
 * It is used as the source coordinate where the minimum or maximum number of cycles is determined.
 * This <em>source</em> coordinate is not necessarily the same as the median <em>target</em> coordinate
 * (which is fixed to zero by application of normalization matrix) because those medians were determined
 * from source and target envelopes, which do not need to be the same.</p>
 *
 * <p>The final result is that envelopes transformed using {@code WraparoundInEnvelope} may be larger
 * than envelopes transformed using {@link WraparoundTransform} but should never be smaller.
 * For example when transforming the following envelope with wraparound on the dashed line:</p>
 *
 * {@preformat text
 *     ┌─┆───────────────┆───┐           ┆              Envelope to transform.
 *     │ ┆               ┆   │           ┆
 *     └─┆───────────────┆───┘           ┆
 *       ┆               ┆   ┌─────────┐ ┆              Result we would got without `WraparoundInEnvelope`.
 *       ┆               ┆   │         │ ┆
 *       ┆               ┆   └─────────┘ ┆
 *       ┆             ┌─┆───┐         ┌─┆───┐          Better result (union to be done by caller).
 *       ┆             │ ┆   │         │ ┆   │
 *       ┆             └─┆───┘         └─┆───┘
 * }
 *
 * <h2>Mutability</h2>
 * <b>This class is mutable.</b> This class records the translations that {@link #shift(double)} wanted to apply
 * but could not because of the {@linkplain #limit} documented in above paragraph. When such missed translations
 * are detected, caller should {@linkplain #translate() translate the limit} and transform same envelope again.
 * We do that because each envelope transformation should use a consistent {@linkplain #limit} for all corners.
 * Since this strategy breaks usual {@link org.apache.sis.referencing.operation.transform.AbstractMathTransform}
 * contract about immutability, this class should be used only for temporary transforms to apply on an envelope.
 *
 * <h2>Thread-safety</h2>
 * This class is <strong>not</strong> thread-safe. Each instance shall be used by only one thread.
 * Temporary instances should be created by the thread doing transformations and discarded immediately after.
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
    private static final long serialVersionUID = 4017870982753327584L;

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
     * Creates a new transform initialized to a copy of given instance.
     * Input and output values in the wraparound dimension shall be normalized
     * in the [−p/2 … +p/2] range where <var>p</var> is the period (e.g. 360°).
     */
    private WraparoundInEnvelope(final WraparoundTransform other) {
        super(other);
        minCycles = maxCycles = limit = Math.rint(sourceMedian / period);
    }

    /**
     * Applies the wraparound on the given coordinate value. This implementation ensures that coordinates smaller than
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
    protected final double shift(final double x) {
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
     * {@link #shift(double)} executions. If this method returns {@code true}, then this transform will now compute
     * different output coordinates for the same input coordinates.
     *
     * <h4>Usage</h4>
     * This method can be invoked after transforming an envelope. If this method returns {@code true}, then
     * the same envelope should be transformed again and the new result added to the previous result (union).
     *
     * @return {@code true} if this transform has been modified.
     */
    private boolean translate() {
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
     * Helper class for transforming an envelope with special checks for wraparounds.
     * This class provides a translatable {@linkplain #transform} for enabling wraparounds that could not be applied
     * in previous {@link #shift(double)} executions. The translation is applied by calls to the {@link #translate()}
     * method, which should be invoked repetitively until it returns {@code false}.
     */
    static final class Controller implements UnaryOperator<WraparoundTransform> {
        /**
         * The potentially mutable transform to use for transforming envelope corners.
         * This transform is derived from the transform specified at construction time
         * and should not live longer than the time needed for transforming an envelope.
         */
        final MathTransform transform;

        /**
         * All wraparound steps found, or {@code null} if none.
         */
        private WraparoundInEnvelope[] wraparounds;

        /**
         * Creates a new instance using the given transform. If the given transform contains wraparound steps,
         * then the transform stored in the {@link #transform} will be a different transform chains instance.
         */
        @SuppressWarnings("ThisEscapedInObjectConstruction")
        Controller(final MathTransform transform) {
            this.transform = replace(transform, this);
        }

        /**
         * Callback method for replacing {@link WraparoundTransform} instances by {@link WraparoundInEnvelope}
         * instances in {@link #transform}. This method is public as an implementation side-effect and should
         * not be invoked directly (it is invoked by {@link WraparoundTransform#replace(MathTransform, Function)}).
         *
         * @param  transform  the {@code WraparoundTransform} instance to replace by a translatable instance.
         * @return same wraparound operation but with a control on translations applied on corner coordinates.
         */
        @Override
        public WraparoundTransform apply(final WraparoundTransform transform) {
            if (!Double.isFinite(transform.sourceMedian)) {
                return transform;
            }
            final WraparoundInEnvelope w = new WraparoundInEnvelope(transform);
            if (wraparounds == null) {
                wraparounds = new WraparoundInEnvelope[] {w};
            } else {
                wraparounds = ArraysExt.append(wraparounds, w);
            }
            return w;
        }

        /**
         * Modifies the {@linkplain #transform} with a translation for enabling wraparounds that could not be applied
         * in previous {@link #shift(double)} executions. If this method returns {@code true}, then the transform will
         * compute different output coordinates for the same input coordinates.
         *
         * <h4>Usage</h4>
         * This method can be invoked after transforming an envelope. If this method returns {@code true}, then
         * the same envelope should be transformed again and the new result added to the previous result (union).
         *
         * @return {@code true} if the {@linkplain #transform} has been modified.
         */
        final boolean translate() {
            boolean modified = false;
            if (wraparounds != null) {
                for (final WraparoundInEnvelope tr : wraparounds) {
                    modified |= tr.translate();
                }
            }
            return modified;
        }
    }
}
