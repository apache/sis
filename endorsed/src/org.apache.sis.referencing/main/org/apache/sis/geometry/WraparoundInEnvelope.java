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

import java.util.List;
import java.util.function.UnaryOperator;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.operation.provider.Wraparound;
import org.apache.sis.referencing.operation.transform.WraparoundTransform;


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
 * For example, when transforming the following envelope with wraparound on the dashed line:</p>
 *
 * <pre class="text">
 *     ┌─┆───────────────┆───┐           ┆              Envelope to transform.
 *     │ ┆               ┆   │           ┆
 *     └─┆───────────────┆───┘           ┆
 *       ┆               ┆   ┌─────────┐ ┆              Result we would got without `WraparoundInEnvelope`.
 *       ┆               ┆   │         │ ┆
 *       ┆               ┆   └─────────┘ ┆
 *       ┆             ┌─┆───┐         ┌─┆───┐          Better result (union to be done by caller).
 *       ┆             │ ┆   │         │ ┆   │
 *       ┆             └─┆───┘         └─┆───┘</pre>
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
 */
final class WraparoundInEnvelope extends WraparoundTransform {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4017870982753327584L;

    /**
     * Number of cycles at the {@linkplain #sourceMedian} position. This is the minimum or maximum number
     * of cycles to remove to a coordinate, depending if that coordinate is before or after the median.
     *
     * <p>This value is an integer, but stored as a {@code double} for avoiding type conversions.
     * This is initialized at construction time, then changed when {@link #translate()} is invoked.</p>
     */
    private double limit;

    /**
     * The minimum and maximum number of {@linkplain #period period}s that {@link #shift(double)} wanted
     * to add to the coordinate before to be constrained to the {@link #limit}.
     * This value is an integer, but stored as a {@code double} for avoiding type conversions.
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
            final var w = new WraparoundInEnvelope(transform);
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

        /**
         * Returns a snapshot of the state of the wraparound transform.
         * This state can change when {@link #translate()} is invoked.
         * It may be used as a key in a map.
         *
         * @return a snapshot of the state of the wraparound transform.
         */
        final Parameters state() {
            State state = null;
            if (wraparounds != null) {
                for (final WraparoundInEnvelope tr : wraparounds) {
                    state = new State(tr, state);
                }
            }
            return state;
        }
    }

    /**
     * A semi-opaque object that describes the state of the wraparound transform.
     * The parameters published by this object are not committed API and may change in any version.
     * The current implementation is a linked list, but this list usually contains only one element.
     *
     * <p>The purpose of this class is to be used as keys in a hash map. Therefore, the only important
     * methods are {@link #hashCode()} and {@link #equals(Object)}. The other methods are defined for
     * compliance with the {@link Parameters} contract, but should not be used.</p>
     */
    private static final class State extends Parameters {
        /** The group of parameters published by the public methods. */
        private static volatile ParameterDescriptorGroup parameters;

        /** Copy of a value from the enclosing wraparound transform. */
        private final int wraparoundDimension;

        /** Copies of values from the enclosing wraparound transform. */
        private final double period, limit;

        /** State of the wraparound transform executed before this one. */
        private final State previous;

        /** Creates a snapshot of the state of the given transforms. */
        State(final WraparoundInEnvelope tr, final State previous) {
            wraparoundDimension = tr.wraparoundDimension;
            this.period   = tr.period;
            this.limit    = tr.limit;
            this.previous = previous;
        }

        /**
         * Returns a hash code value for this state.
         * It includes the hash code of previous {@code State} instances in the linked list.
         */
        @Override
        public int hashCode() {
            int hash = wraparoundDimension;
            State s1 = this;
            do hash = 37*hash + (31*Double.hashCode(s1.limit) + Double.hashCode(s1.period));
            while ((s1 = s1.previous) != null);
            return hash;
        }

        /**
         * Compares this state with the given object for equality.
         * The previous elements of the linked list are also compared.
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof State) {
                State s1 = this;
                State s2 = (State) obj;
                while (Numerics.equals(s1.limit,  s2.limit)  &&
                       Numerics.equals(s1.period, s2.period) &&
                       s1.wraparoundDimension == s2.wraparoundDimension)
                {
                    s1 = s1.previous;
                    s2 = s2.previous;
                    if (s1 == s2) return true;  // We are mostly interrested the case when both are null.
                    if (s1 == null || s2 == null) break;
                }
            }
            return false;
        }

        /**
         * Returns the group of parameters published by the object.
         * Provided for compliance with the interface contract, but
         * not useful for the purpose of the {@code State} object.
         */
        @Override
        public ParameterDescriptorGroup getDescriptor() {
            ParameterDescriptorGroup p = parameters;
            if (p == null) {
                final var builder = new ParameterBuilder().setCodeSpace(Citations.SIS, "SIS");
                final ParameterDescriptor<Double> shift = builder.addName("shift").create(Double.NaN, null);
                parameters = p = builder.addName("Wraparound state").createGroup(Wraparound.WRAPAROUND_DIMENSION, shift);
            }
            return p;
        }

        /**
         * Returns the values of all parameters defined by {@link #getDescriptor()}.
         * Provided for compliance with the interface contract, but* not useful for
         * the purpose of the {@code State} object.
         */
        @Override
        public List<GeneralParameterValue> values() {
            return List.of(parameter("wraparound_dim"), parameter("shift"));
        }

        /**
         * Returns the value of the parameter of the given name.
         * Provided for compliance with the interface contract,
         * but* not useful for the purpose of {@code State}.
         */
        @Override
        public ParameterValue<?> parameter(String name) {
            switch (name) {
                case "wraparound_dim": {
                    ParameterValue<Integer> p = Wraparound.WRAPAROUND_DIMENSION.createValue();
                    p.setValue(wraparoundDimension);
                    return p;
                }
                case "shift": {
                    var p = (ParameterValue<?>) getDescriptor().descriptors().get(1).createValue();
                    p.setValue(period * limit);
                    return p;
                }
                default: throw new ParameterNotFoundException(null, name);
            }
        }

        /**
         * Unsupported operation. Actually, we could return the parameters of a previous element
         * of the linked list. But this is not implemented yet because we have no known usage.
         */
        @Override
        public List<ParameterValueGroup> groups(String name) {
            throw new ParameterNotFoundException(null, name);
        }

        /**
         * Unsupported operation as this parameter group is unmodifiable.
         */
        @Override
        public ParameterValueGroup addGroup(String name) {
            throw new ParameterNotFoundException(null, name);
        }
    }
}
