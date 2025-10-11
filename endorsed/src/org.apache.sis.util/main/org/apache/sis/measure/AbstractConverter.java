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
import java.io.Serializable;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.apache.sis.math.DecimalFunctions;


/**
 * Skeletal implementation of the {@code UnitConverter} interface for reducing implementation effort.
 * This class makes easier to define a non-linear conversion between two units of measurement.
 * Note that for linear conversions, the standard {@link Unit#shift(Number)}, {@link Unit#multiply(Number)}
 * and {@link Unit#divide(Number)} methods should be used instead.
 *
 * <p>After a non-linear conversion has been created, a new unit of measurement using that conversion
 * can be defined by a call to {@link Unit#transform(UnitConverter)}.
 * See {@link Units#logarithm(Unit)} for an example.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see Units#converter(Number, Number)
 * @see Units#logarithm(Unit)
 *
 * @since 1.5
 */
public abstract class AbstractConverter implements UnitConverter, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8480235641759297444L;

    /**
     * Creates a new converter.
     */
    protected AbstractConverter() {
    }

    /**
     * Returns {@code true} if {@link #convert(double)} returns given values unchanged.
     * The default implementation returns {@code false} for convenience of non-linear conversions.
     * Subclasses should override if their conversions may be identity.
     *
     * @return whether this converter performs no operation.
     */
    @Override
    public boolean isIdentity() {
        return false;
    }

    /**
     * Indicates if this converter is linear in JSR-385 sense (not the usual mathematical sense).
     * This is {@code true} if this converter contains a scale <em>but no offset</em>.
     *
     * @return whether this converter contains an offset.
     *
     * @deprecated This method is badly named, but we can't change since it is defined by JSR-385.
     */
    @Override
    @Deprecated
    public boolean isLinear() {
        return isIdentity();
    }

    /**
     * If the conversion can be represented by a polynomial equation, returns the coefficients of that equation.
     * Otherwise returns {@code null}. This is the implementation of {@link Units#coefficients(UnitConverter)}.
     */
    Number[] coefficients() {
        return isIdentity() ? new Number[0] : null;
    }

    /**
     * Performs a unit conversion on the given number. The default implementation delegates to the version working
     * on {@code double} primitive type, so it may not provide the accuracy normally required by this method contract.
     * Linear conversions should override this method.
     *
     * @param  value  the value to convert.
     * @return the converted value.
     */
    @Override
    public Number convert(final Number value) {
        return convert(value.doubleValue());
    }

    /**
     * Returns the derivative of the conversion function at the given value, or {@code NaN} if unknown.
     * The given argument is ignored (can be {@link Double#NaN}) if the conversion is linear.
     *
     * @param  value  the point at which to compute the derivative.
     * @return the derivative at the value.
     */
    public abstract double derivative(double value);

    /**
     * Delegates to {@link #derivative(double)} if the given converter is an Apache SIS implementation,
     * or use a fallback otherwise.
     */
    static double derivative(final UnitConverter converter, final double value) {
        if (converter != null) {
            if (converter instanceof AbstractConverter) {
                return ((AbstractConverter) converter).derivative(value);
            } else if (converter.isLinear()) {
                return converter.convert(1) - converter.convert(0);
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the scale factor of the given converter if the conversion is linear, or NaN otherwise.
     */
    static double scale(final UnitConverter converter) {
        if (converter != null && converter.isLinear() && converter.convert(0) == 0) {
            /*
             * Above check for `converter(0)` is a paranoiac check because
             * JSR-385 said that a "linear" converter has no offset.
             */
            return converter.convert(1);
        }
        return Double.NaN;
    }

    /**
     * Returns the value of the given number, with special handling for {@link Float} value on the assumption
     * that the original value was written in base 10. This is usually the case for unit conversion factors.
     */
    static double doubleValue(final Number n) {
        return (n instanceof Float) ? DecimalFunctions.floatToDouble(n.floatValue()) : n.doubleValue();
    }

    /**
     * Returns {@code true} if the given floating point numbers are considered equal.
     * The tolerance factor used in this method is arbitrary and may change in any future version.
     */
    static boolean epsilonEquals(final double expected, final double actual) {
        return Math.abs(expected - actual) <= Math.scalb(Math.ulp(expected), 4);
    }

    /**
     * Concatenates this converter with another converter. The resulting converter is equivalent to first converting
     * by the specified converter (right converter), and then converting by this converter (left converter).
     *
     * <p>The default implementation is okay, but subclasses should override if they can detect optimizations.</p>
     *
     * @param  before  the converter to concatenate before this converter.
     * @return a conversion which applies {@code before} first, then {@code this}.
     */
    @Override
    public UnitConverter concatenate(final UnitConverter before) {
        if (equals(before.inverse())) {
            return IdentityConverter.INSTANCE;
        }
        return new ConcatenatedConverter(before, this);
    }

    /**
     * Returns the steps of fundamental converters making up this converter. The default implementation returns
     * only {@code this} on the assumption that this conversion is not a concatenation of other converters.
     * Subclasses should override if this assumption does not hold.
     *
     * @return list of steps in the unit conversion represented by this instance.
     */
    @Override
    public List<UnitConverter> getConversionSteps() {
        return List.of(this);
    }
}
