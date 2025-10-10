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
package org.apache.sis.measure;

import java.util.List;
import java.util.ArrayList;
import javax.measure.UnitConverter;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.Utilities;


/**
 * The concatenation of two unit converters where at least one of them is not linear.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ConcatenatedConverter extends AbstractConverter implements LenientComparable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6506147355157815065L;

    /**
     * The first unit converter to apply.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final UnitConverter c1;

    /**
     * The second unit converter to apply, after {@code c1}.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final UnitConverter c2;

    /**
     * The inverse of this unit converter. Computed when first needed and stored in
     * order to avoid rounding error if the user asks for the inverse of the inverse.
     */
    private transient ConcatenatedConverter inverse;

    /**
     * Creates a new concatenation of the given unit converters.
     * Values will be converted according {@code c1} first, then {@code c2}.
     */
    ConcatenatedConverter(final UnitConverter c1, final UnitConverter c2) {
        this.c1 = c1;
        this.c2 = c2;
    }

    /**
     * Returns {@code true} if the two unit converters are identity converters.
     * Should always be {@code false}, otherwise we would not have created a {@code ConcatenatedConverter}.
     */
    @Override
    public boolean isIdentity() {
        return c1.isIdentity() && c2.isIdentity();
    }

    /**
     * Returns {@code true} if the two unit converters are linear converters.
     * Should always be {@code false}, otherwise we would not have created a {@code ConcatenatedConverter}.
     *
     * @deprecated This method is badly named, but we can't change since it is defined by JSR-385.
     */
    @Override
    @Deprecated
    public boolean isLinear() {
        return c1.isLinear() && c2.isLinear();
    }

    /**
     * Returns the inverse of this unit converter.
     */
    @Override
    public synchronized UnitConverter inverse() {
        if (inverse == null) {
            inverse = new ConcatenatedConverter(c2.inverse(), c1.inverse());
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * Applies the linear conversion on the given IEEE 754 floating-point value.
     */
    @Override
    public double convert(final double value) {
        return c2.convert(c1.convert(value));
    }

    /**
     * Applies the linear conversion on the given value.
     */
    @Override
    public Number convert(final Number value) {
        return c2.convert(c1.convert(value));
    }

    /**
     * Returns the derivative of the conversion function at the given value, or {@code NaN} if unknown.
     */
    @Override
    public double derivative(final double value) {
        return derivative(c1, value) * derivative(c2, c1.convert(value));
    }

    /**
     * Concatenates this converter with another converter. The resulting converter is equivalent to first converting
     * by the specified converter (right converter), and then converting by this converter (left converter).
     */
    @Override
    public UnitConverter concatenate(final UnitConverter converter) {
        if (equals(converter.inverse())) {
            return IdentityConverter.INSTANCE;
        }
        // Delegate to c1 and c2 because they may provide more intelligent 'concatenate' implementations.
        return c2.concatenate(c1.concatenate(converter));
    }

    /**
     * Returns the steps of fundamental converters making up this converter.
     * For example, {@code c1.getConversionSteps()} returns {@code c1} while
     * {@code c1.concatenate(c2).getConversionSteps()} returns {@code c1, c2}.
     */
    @Override
    public List<UnitConverter> getConversionSteps() {
        final List<UnitConverter> converters = new ArrayList<>();
        converters.addAll(c1.getConversionSteps());
        converters.addAll(c2.getConversionSteps());
        return converters;
    }

    /**
     * Returns a hash code value for this unit converter.
     */
    @Override
    public int hashCode() {
        return (c1.hashCode() + 31 * c2.hashCode()) ^ (int) serialVersionUID;
    }

    /**
     * Compares this converter with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ConcatenatedConverter) {
            final ConcatenatedConverter o = (ConcatenatedConverter) other;
            return c1.equals(o.c1) && c2.equals(o.c2);
        }
        return false;
    }

    /**
     * Compares this converter with the given object for equality, optionally ignoring rounding errors.
     */
    @Override
    public boolean equals(final Object other, final ComparisonMode mode) {
        if (other instanceof ConcatenatedConverter) {
            final ConcatenatedConverter o = (ConcatenatedConverter) other;
            return Utilities.deepEquals(c1, o.c1, mode) &&
                   Utilities.deepEquals(c2, o.c2, mode);
        }
        return false;
    }
}
