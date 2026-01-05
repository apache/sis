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

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;


/**
 * Base class for expressions, comparators or filters performing operations on two expressions.
 * The nature of the operation depends on the subclass. If operands are numerical values, they
 * may be converted to a common type before the operation is performed. That operation is not
 * necessarily an arithmetic operation, it may also be a comparison for example.
 *
 * <h2>Terminology</h2>
 * "Binary function" takes two inputs from possibly different sets. This is more general than
 * "binary operator", which takes inputs and produce an output of the same set.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <R>   the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 * @param  <A1>  the type of value computed by the first expression (left operand).
 * @param  <A2>  the type of value computed by the second expression (right operand).
 */
public abstract class BinaryFunction<R, A1, A2> extends Node {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8632475810190545852L;

    /**
     * The first of the two expressions to be used by this function.
     *
     * @see #getExpression1()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final Expression<R, ? extends A1> expression1;

    /**
     * The second of the two expressions to be used by this function.
     *
     * @see #getExpression2()
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    protected final Expression<R, ? extends A2> expression2;

    /**
     * Creates a new binary function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    protected BinaryFunction(final Expression<R, ? extends A1> expression1,
                             final Expression<R, ? extends A2> expression2)
    {
        this.expression1 = Objects.requireNonNull(expression1);
        this.expression2 = Objects.requireNonNull(expression2);
    }

    /**
     * Returns the class of resources expected by this filter.
     * Defined for {@link Filter#getResourceClass()} and {@link Expression#getResourceClass()} implementations.
     *
     * @return type of resources accepted by this filter, or {@code null} if inconsistent.
     */
    public final Class<? super R> getResourceClass() {
        return specializedClass(expression1.getResourceClass(),
                                expression2.getResourceClass());
    }

    /**
     * Returns the expressions used as parameters by this function.
     * Defined for {@link Expression#getParameters()} implementations.
     *
     * @return the expression used as parameter by this function.
     */
    public final List<Expression<R,?>> getParameters() {
        return getExpressions();
    }

    /**
     * Returns the two expressions used as parameters by this filter.
     * Defined for {@link Filter#getExpressions()} implementations.
     *
     * @return a list of size 2 containing the two expressions.
     */
    public final List<Expression<R,?>> getExpressions() {
        return List.of(expression1, expression2);
    }

    /**
     * Returns the two expressions in a list of size 2.
     * This is used for {@link #toString()}, {@link #hashCode()} and {@link #equals(Object)} implementations.
     */
    @Override
    protected final Collection<?> getChildren() {
        return getExpressions();
    }
}
