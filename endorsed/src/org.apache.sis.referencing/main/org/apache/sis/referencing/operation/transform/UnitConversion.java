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
package org.apache.sis.referencing.operation.transform;

import java.io.Serializable;
import javax.measure.UnitConverter;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.internal.Resources;
import org.apache.sis.measure.Units;


/**
 * Bridge between Unit API and referencing API.
 * This is used only when the converter is non-linear or is not a recognized implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class UnitConversion extends AbstractMathTransform1D implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7344042406568682405L;

    /**
     * The unit converter to wrap.
     */
    @SuppressWarnings("serial")                 // Apache SIS implementation is serializable.
    private final UnitConverter converter;

    /**
     * The inverse conversion, computed when first needed.
     */
    private UnitConversion inverse;

    /**
     * Creates a new wrapper.
     *
     * @param converter the unit converter to wrap.
     */
    private UnitConversion(final UnitConverter converter) {
        this.converter = converter;
    }

    /**
     * Converts the given unit converter to a math transform.
     */
    @SuppressWarnings("fallthrough")
    static MathTransform1D create(final UnitConverter converter) {
        Number[] coefficients = Units.coefficients(converter);
        if (coefficients != null) {
            Number scale = 1, offset = 0;
            switch (coefficients.length) {
                case 2: scale  = coefficients[1];     // Fall through
                case 1: offset = coefficients[0];     // Fall through
                case 0: return LinearTransform1D.create(scale, offset);
            }
        }
        return new UnitConversion(converter);
    }

    /**
     * Tests whether this transform changes any value.
     */
    @Override
    public boolean isIdentity() {
        return converter.isIdentity();
    }

    /**
     * Converts the given value.
     *
     * @param  value  the value to convert.
     * @return the converted value.
     */
    @Override
    public double transform(double value) {
        return converter.convert(value);
    }

    /**
     * Computes the derivative at the given value.
     *
     * @param  value  the value for which to compute derivative.
     * @return the derivative for the given value.
     * @throws TransformException if the derivative cannot be computed.
     */
    @Override
    public double derivative(double value) throws TransformException {
        final double derivative = Units.derivative(converter, value);
        if (Double.isNaN(derivative) && !Double.isNaN(value)) {
            throw new TransformException(Resources.format(Resources.Keys.CanNotComputeDerivative));
        }
        return derivative;
    }

    /**
     * Returns the inverse transform of this object.
     */
    @Override
    public synchronized MathTransform1D inverse() {
        if (inverse == null) {
            inverse = new UnitConversion(converter.inverse());
            inverse.inverse = this;
        }
        return inverse;
    }

    /**
     * Concatenates or pre-concatenates in an optimized way this math transform with the given one, if possible.
     */
    @Override
    protected void tryConcatenate(final TransformJoiner context) throws FactoryException {
        int relativeIndex = +1;
        do {
            final MathTransform other = context.getTransform(relativeIndex).orElse(null);
            if (other instanceof UnitConversion) {
                final var that = (UnitConversion) other;
                if (context.replace(relativeIndex, create(relativeIndex < 0
                        ? that.converter.concatenate(this.converter)
                        : this.converter.concatenate(that.converter))))
                {
                    return;
                }
            }
        } while ((relativeIndex = -relativeIndex) < 0);
        super.tryConcatenate(context);
    }
}
