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


/**
 * Base class of unit converters.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
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
}
