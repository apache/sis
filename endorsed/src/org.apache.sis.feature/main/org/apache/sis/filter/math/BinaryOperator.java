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
package org.apache.sis.filter.math;

import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.io.ObjectStreamException;
import org.opengis.util.ScopedName;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.base.BinaryFunction;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.feature.internal.shared.FeatureProjectionBuilder;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * An operation upon two numerical operands. Inputs are {@link Number} instances
 * which will be converted to {@link Double}. Output are {@link Double}.
 *
 * <h2>Terminology</h2>
 * "Binary operator" is a specialization of "binary function" in that it restricts
 * the inputs and the output to the same set, which is the set of double-precision
 * floating point numbers.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
final class BinaryOperator<R> extends BinaryFunction<R, Number, Number>
        implements FeatureExpression<R, Double>, Optimization.OnExpression<R, Double>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8021641013005967925L;

    /**
     * Description of the function to apply.
     */
    private final Function function;

    /**
     * The {@link Function#binary} value, guaranteed non-null.
     */
    private final transient DoubleBinaryOperator math;

    /**
     * Creates a new function.
     */
    BinaryOperator(final Function function,
            final Expression<R, ? extends Number> expression1,
            final Expression<R, ? extends Number> expression2)
    {
        super(expression1, expression2);
        this.function = function;
        math = Objects.requireNonNull(function.binary);
    }

    /**
     * Invoked at deserialization time for setting the {@link #math} field.
     */
    private Object readResolve() throws ObjectStreamException {
        return new BinaryOperator<>(function, expression1, expression2);
    }

    /**
     * Returns the name of the function to be called.
     */
    @Override
    public ScopedName getFunctionName() {
        return function.getFunctionName();
    }

    /**
     * Returns the type of values computed by this expression.
     */
    @Override
    public final Class<Double> getResultClass() {
        return Double.class;
    }

    /**
     * Provides the type of results computed by this expression. That type depends only
     * on the {@code ArithmeticFunction} subclass and is given by {@link #expectedType()}.
     */
    @Override
    public final FeatureProjectionBuilder.Item expectedType(FeatureProjectionBuilder addTo) {
        return addTo.addSourceProperty(function.getResultType(), false);
    }

    /**
     * Evaluates the expression.
     */
    @Override
    public final Double apply(final R feature) {
        final Number left  = expression1.apply(feature);
        if (left != null) {
            final Number right = expression2.apply(feature);
            if (right != null) {
                return math.applyAsDouble(left.doubleValue(), right.doubleValue());
            }
        }
        return null;
    }
}
