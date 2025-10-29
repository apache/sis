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
package org.apache.sis.filter.function.math;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import java.io.ObjectStreamException;
import org.opengis.util.ScopedName;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.function.UnaryFunction;
import org.apache.sis.feature.internal.shared.FeatureExpression;
import org.apache.sis.feature.internal.shared.FeatureProjectionBuilder;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Expression;


/**
 * An operation on a single operand.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
final class UnaryOperator<R> extends UnaryFunction<R, Number>
        implements FeatureExpression<R, Double>, Optimization.OnExpression<R, Double>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -6215509464490587978L;

    /**
     * The function to apply.
     */
    private final Function function;

    /**
     * The {@link Function#binary} value, guaranteed non-null.
     */
    private final transient DoubleUnaryOperator math;

    /**
     * Creates a new function.
     */
    UnaryOperator(final Function function, final Expression<R, ? extends Number> expression) {
        super(expression);
        this.function = function;
        math = Objects.requireNonNull(function.unary);
    }

    /**
     * Invoked at deserialization time for setting the {@link #math} field.
     */
    private Object readResolve() throws ObjectStreamException {
        return new UnaryOperator<>(function, expression);
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
        final Number value  = expression.apply(feature);
        if (value != null) {
            return math.applyAsDouble(value.doubleValue());
        }
        return null;
    }
}
