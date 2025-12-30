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
package org.apache.sis.filter.base;

import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.apache.sis.filter.Optimization;
import org.apache.sis.xml.NilReason;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.NilOperator;
import org.opengis.filter.NullOperator;


/**
 * Base class for expressions, comparators or filters performing operations on one expressions.
 * The nature of the operation depends on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <V>  the type of value computed by the expression.
 */
public class UnaryFunction<R,V> extends Node {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2020526901451551162L;

    /**
     * The expression to be used by this operator.
     *
     * @see #getExpression()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final Expression<R, ? extends V> expression;

    /**
     * Creates a new unary operator.
     *
     * @param  expression  the expression to be used by this operator.
     */
    protected UnaryFunction(final Expression<R, ? extends V> expression) {
        this.expression = Objects.requireNonNull(expression);
    }

    /**
     * Returns the class of resources expected by this filter.
     * Defined for {@link Filter#getResourceClass()} implementations.
     *
     * @return type of resources accepted by this filter, or {@code null} if inconsistent.
     */
    public final Class<? super R> getResourceClass() {
        return expression.getResourceClass();
    }

    /**
     * Returns the expression used as parameter by this function.
     * Defined for {@link Expression#getParameters()} implementations.
     *
     * @return the expression used as parameter by this function.
     */
    public final List<Expression<R,?>> getParameters() {
        return getExpressions();
    }

    /**
     * Returns the expression used as parameter by this filter.
     * Defined for {@link Filter#getExpressions()} implementations.
     *
     * @return a list of size 1 containing the singleton expression.
     */
    public final List<Expression<R,?>> getExpressions() {
        return List.of(expression);
    }

    /**
     * Returns the expression used by this operator possibly completed in subclasses with other parameters.
     * This is used for {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected Collection<?> getChildren() {
        return getExpressions();
    }


    /**
     * Filter operator that checks if an expression's value is {@code null}.  A {@code null}
     * is equivalent to no value present. The value 0 is a valid value and is not considered
     * {@code null}.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    public static final class IsNull<R> extends UnaryFunction<R,Object>
            implements NullOperator<R>, Optimization.OnFilter<R>
    {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 2960285515924533419L;

        /** Creates a new operator. */
        public IsNull(final Expression<R,?> expression) {
            super(expression);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
            return new IsNull<>(effective[0]);
        }

        /** Identification of the operation. */
        @Override protected char symbol() {
            return '∅';
        }

        /** Evaluate this filter on the given object. */
        @Override public boolean test(final R object) {
            return expression.apply(object) == null;
        }
    }


    /**
     * Filter operator that checks if an expression's value is nil.
     * The difference with {@link IsNull} is that a value should exist but
     * cannot be provided for the reason given by {@link #getNilReason()}.
     *
     * @param  <R>  the type of resources used as inputs.
     */
    public static final class IsNil<R> extends UnaryFunction<R,Object>
            implements NilOperator<R>, Optimization.OnFilter<R>
    {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -7540765433296725888L;

        /** The reason why the value is nil, or {@code null} for accepting any reason. */
        private final String nilReason;

        /** Creates a new operator. */
        public IsNil(final Expression<R,?> expression, final String nilReason) {
            super(expression);
            this.nilReason = nilReason;
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<R> recreate(final Expression<R,?>[] effective) {
            return new IsNil<>(effective[0], nilReason);
        }

        /** Returns the reason why the value is nil. */
        @Override public Optional<String> getNilReason() {
            return Optional.ofNullable(nilReason);
        }

        /** Evaluate this filter on the given object. */
        @Override public boolean test(final R object) {
            final NilReason value = NilReason.forObject(expression.apply(object));
            if (value     == null) return false;
            if (nilReason == null) return true;
            final String explanation = value.equals(NilReason.OTHER) ? value.getOtherExplanation() : value.toString();
            return nilReason.equalsIgnoreCase(explanation);
        }
    }
}
