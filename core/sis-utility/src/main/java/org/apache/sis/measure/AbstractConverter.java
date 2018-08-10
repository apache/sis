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

import java.io.Serializable;
import javax.measure.UnitConverter;
import org.apache.sis.math.DecimalFunctions;


/**
 * Base class of unit converters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
abstract class AbstractConverter implements UnitConverter, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8480235641759297444L;

    /**
     * Creates a new converter.
     */
    AbstractConverter() {
    }

    /**
     * If the conversion can be represented by a polynomial equation, returns the coefficients of that equation.
     * Otherwise returns {@code null}.
     */
    Number[] coefficients() {
        return isIdentity() ? new Number[0] : null;
    }

    /**
     * Returns the derivative of the conversion function at the given value, or {@code NaN} if unknown.
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
            // Above check for converter(0) is a paranoiac check since
            // JSR-363 said that a "linear" converter has no offset.
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
}
