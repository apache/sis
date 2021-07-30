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
package org.apache.sis.filter;

import java.util.List;
import java.util.Arrays;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.GeometryWrapper;
import org.apache.sis.internal.feature.SpatialOperationContext;
import org.apache.sis.internal.filter.Node;
import org.apache.sis.util.ArgumentChecks;

// Branch-dependent imports
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;
import org.opengis.filter.SpatialOperator;
import org.opengis.filter.InvalidFilterValueException;


/**
 * Base class for filters having two expressions evaluating to geometries.
 * In addition of 2 geometries, the filter can have additional non-geometric arguments.
 * The nature of the operation depends on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <G>  the implementation type of geometry objects.
 *
 * @since 1.1
 * @module
 */
abstract class BinaryGeometryFilter<R,G> extends Node implements SpatialOperator<R>, Optimization.OnFilter<R> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7205680763469213064L;

    /**
     * The first of the two expressions to be used by this function.
     *
     * @see BinarySpatialOperator#getOperand1()
     */
    protected final Expression<? super R, GeometryWrapper<G>> expression1;

    /**
     * The second of the two expressions to be used by this function.
     *
     * @see BinarySpatialOperator#getOperand2()
     */
    protected final Expression<? super R, GeometryWrapper<G>> expression2;

    /**
     * The preferred CRS and other context to use if geometry transformations are needed.
     */
    protected final SpatialOperationContext context;

    /**
     * Creates a new binary function.
     *
     * @param  geometry1   the first of the two expressions to be used by this function.
     * @param  geometry2   the second of the two expressions to be used by this function.
     * @param  systemUnit  if the CRS needs to be in some units of measurement, the {@link Unit#getSystemUnit()} value.
     */
    protected BinaryGeometryFilter(final Geometries<G> library,
                                   final Expression<? super R, ?> geometry1,
                                   final Expression<? super R, ?> geometry2,
                                   final Unit<?> systemUnit)
    {
        ArgumentChecks.ensureNonNull("expression1", geometry1);
        ArgumentChecks.ensureNonNull("expression2", geometry2);
        Expression<? super R, GeometryWrapper<G>> expression1, expression2;
        expression1 = toGeometryWrapper(library, geometry1);
        expression2 = toGeometryWrapper(library, geometry2);
        /*
         * Check if any expression is a literal. If an expression is a literal, we use
         * its Coordinate Reference System as the CRS in which to perform the operation.
         * Otherwise the CRS will be selected on a case-by-case basis at evaluation time.
         */
        final int index;
        final Literal<? super R, ?> literal;
        final GeometryWrapper<G> value;
        if (geometry2 instanceof Literal<?,?>) {
            literal = (Literal<? super R, ?>) geometry2;
            value = expression2.apply(null);
            index   = 1;
        } else if (geometry1 instanceof Literal<?,?>) {
            literal = (Literal<? super R, ?>) geometry1;
            value = expression1.apply(null);
            index = 0;
        } else {
            literal = null;
            value = null;
            index   = -1;
        }

        try {
            context = new SpatialOperationContext(null, value, systemUnit, index);
            if (value != null) {
                final GeometryWrapper<G> gt = context.transform(value);
                if (gt != value) {
                    final Expression<? super R, GeometryWrapper<G>> tr = new LeafExpression.Transformed<>(gt, literal);
                    switch (index) {
                        case 0:  expression1 = tr; break;
                        case 1:  expression2 = tr; break;
                        default: throw new AssertionError(index);
                    }
                }
            }
        } catch (FactoryException | TransformException | IncommensurableException e) {
            throw new InvalidFilterValueException(e);
        }
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    /**
     * Recreates a new filter of the same type and with the same parameters, but using the given expressions.
     * This method is invoked when it is possible to simplify or optimize at least one of the expressions that
     * were given in the original call to the constructor.
     */
    protected abstract BinaryGeometryFilter<R,G> recreate(final Expression<? super R, ?> geometry1,
                                                          final Expression<? super R, ?> geometry2);

    /**
     * Returns the original expression specified by the user.
     *
     * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
     * @param  <G>  the geometry implementation type.
     * @param  expression  the expression to unwrap.
     * @return the unwrapped expression.
     */
    protected static <R,G> Expression<? super R, ?> original(final Expression<R, ? extends GeometryWrapper<G>> expression) {
        Expression<? super R, ?> unwrapped = unwrap(expression);
        if (unwrapped instanceof LeafExpression.Transformed<?, ?>) {
            unwrapped = ((LeafExpression.Transformed<R, ?>) unwrapped).original;
        }
        return unwrapped;
    }

    /**
     * Returns the two expressions used as parameters by this filter.
     */
    @Override
    public List<Expression<? super R, ?>> getExpressions() {
        return Arrays.asList(original(expression1), original(expression2));     // TODO: use List.of(…) with JDK9.
    }

    /**
     * Tries to optimize this filter. This method checks if any expression is a literal.
     * If both expressions are literal, we can evaluate immediately. If any expression
     * is a literal and returns {@code null}, then the result is known in advance too.
     */
    @Override
    public final Filter<? super R> optimize(final Optimization optimization) {
        final Expression<? super R, ?> geometry1  = unwrap(expression1);
        final Expression<? super R, ?> geometry2  = unwrap(expression2);
        final Expression<? super R, ?> effective1 = optimization.apply(geometry1);
        final Expression<? super R, ?> effective2 = optimization.apply(geometry2);
        final Literal<? super R, ?> literal;
        final boolean immediate;                // true if the filter should be evaluated immediately.
        final boolean literalIsNull;            // true if one of the literal value is null.
        if (effective2 instanceof Literal<?,?>) {
            literal   = (Literal<? super R, ?>) effective2;
            immediate = (effective1 instanceof Literal<?,?>);
        } else if (effective1 instanceof Literal<?,?>) {
            literal   = (Literal<? super R, ?>) effective1;
            immediate = false;
        } else {
            return this;
        }
        literalIsNull = (literal.getValue() == null);
        final boolean result;
        if (literalIsNull) {
            // If the literal has no value, then the filter will always evaluate to a negative result.
            result = negativeResult();
        } else {
            Filter<? super R> filter = this;
            if ((effective1 != geometry1) || (effective2 != geometry2)) {
                filter = recreate(effective1, effective2);
            }
            if (!immediate) {
                return filter;
            }
            // If all operands are literal, we can evaluate the expression immediately.
            result = filter.test(null);
        }
        return result ? Filter.include() : Filter.exclude();
    }

    /**
     * Returns the value to return when a test can not be applied.
     */
    protected abstract boolean negativeResult();
}
