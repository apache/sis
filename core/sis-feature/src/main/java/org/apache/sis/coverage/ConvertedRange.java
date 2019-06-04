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
package org.apache.sis.coverage;

import javax.measure.Unit;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;


/**
 * Range of real values computed from the range of the sample values.
 * The {@link Category#toConverse} conversion is used by the caller for computing the inclusive and exclusive
 * minimum and maximum values of this range. We compute both the inclusive and exclusive values because we can not
 * rely on the default implementation, which looks for the nearest representable number. For example if the range
 * of sample values is 0 to 10 exclusive (or 0 to 9 inclusive) and the scale is 2, then the range of real values
 * is 0 to 20 exclusive or 0 to 18 inclusive, not 0 to 19.9999… The numbers between 18 and 20 is a "gray area"
 * where we don't know for sure what the user intents to do.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class ConvertedRange extends MeasurementRange<Double> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1416908614729956928L;

    /**
     * The minimal value to be returned by {@link #getMinDouble(boolean)} when
     * the {@code inclusive} flag is the opposite of {@link #isMinIncluded()}.
     */
    private final double altMinimum;

    /**
     * The maximal value to be returned by {@link #getMaxDouble(boolean)} when
     * the {@code inclusive} flag is the opposite of {@link #isMaxIncluded()}.
     */
    private final double altMaximum;

    /**
     * Constructs a range of {@code double} values.
     */
    ConvertedRange(final double[] extremums, final boolean isMinIncluded, final boolean isMaxIncluded, final Unit<?> unit) {
        super(Double.class, extremums[0], isMinIncluded, extremums[1], isMaxIncluded, unit);
        altMinimum = extremums[2];
        altMaximum = extremums[3];
    }

    /**
     * Completes the union computed by {@link Range#union(Range)} with the unions of alternative extremum.
     */
    private ConvertedRange(final NumberRange<Double> union, final ConvertedRange r1, final NumberRange<Double> r2) {
        super(union, r1.unit());
        boolean f;
        altMinimum = Math.min(r1.getMinDouble(f = !isMinIncluded()), r2.getMinDouble(f));
        altMaximum = Math.max(r1.getMaxDouble(f = !isMaxIncluded()), r2.getMaxDouble(f));
    }

    /**
     * Returns the union of this range with the given range.
     */
    @Override
    public Range<Double> union(final Range<Double> range) {
        Range<Double> union = super.union(range);
        if (union != this && union != range) {
            if (union instanceof NumberRange<?> && range instanceof NumberRange<?>) {
                union = new ConvertedRange((NumberRange<Double>) union, this, (NumberRange<Double>) range);
            }
        }
        return union;
    }

    /**
     * Returns the minimum value with the specified inclusive or exclusive state.
     */
    @Override
    public double getMinDouble(final boolean inclusive) {
        return (inclusive == isMinIncluded()) ? getMinDouble() : altMinimum;
    }

    /**
     * Returns the maximum value with the specified inclusive or exclusive state.
     */
    @Override
    public double getMaxDouble(final boolean inclusive) {
        return (inclusive == isMaxIncluded()) ? getMaxDouble() : altMaximum;
    }
}
