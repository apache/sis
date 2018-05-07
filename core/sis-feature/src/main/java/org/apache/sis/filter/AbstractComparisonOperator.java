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

import java.io.Serializable;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.MatchAction;
import org.opengis.filter.expression.Expression;


/**
 * Base class for filters that compare exactly two values against each other.
 * The nature of the comparison is dependent on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractComparisonOperator implements BinaryComparisonOperator, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1401452229232869720L;

    /**
     * The first of the two expressions to be compared by this operator.
     *
     * @see #getExpression1()
     */
    protected final Expression expression1;

    /**
     * The second of the two expressions to be compared by this operator.
     *
     * @see #getExpression2()
     */
    protected final Expression expression2;

    /**
     * Whether comparisons are case sensitive.
     *
     * @see #isMatchingCase()
     */
    protected final boolean matchCase;

    /**
     * Specifies how the comparison predicate shall be evaluated for a collection of values.
     *
     * @see #getMatchAction()
     */
    protected final MatchAction matchAction;

    /**
     * Creates a new binary comparison operator.
     * It is caller responsibility to ensure that no argument is null.
     */
    AbstractComparisonOperator(final Expression expression1, final Expression expression2,
                               final boolean matchCase, final MatchAction matchAction)
    {
        this.expression1 = expression1;
        this.expression2 = expression2;
        this.matchCase   = matchCase;
        this.matchAction = matchAction;
    }

    /**
     * Returns the mathematical symbol for this comparison operator.
     * The symbol should be one of the following: {@literal < > ≤ ≥ = ≠}.
     */
    protected abstract char symbol();

    /**
     * Returns the first of the two expressions to be compared by this operator.
     */
    @Override
    public final Expression getExpression1() {
        return expression1;
    }

    /**
     * Returns the second of the two expressions to be compared by this operator.
     */
    @Override
    public final Expression getExpression2() {
        return expression2;
    }

    /**
     * Specifies whether comparisons are case sensitive.
     *
     * @return {@code true} if the comparisons are case sensitive, otherwise {@code false}.
     */
    @Override
    public final boolean isMatchingCase() {
        return matchCase;
    }

    /**
     * Specifies how the comparison predicate shall be evaluated for a collection of values.
     * Values can be {@link MatchAction#ALL ALL} if all values in the collection shall satisfy the predicate,
     * {@link MatchAction#ANY ANY} if any of the value in the collection can satisfy the predicate, or
     * {@link MatchAction#ONE ONE} if only one of the values in the collection shall satisfy the predicate.
     *
     * @return how the comparison predicate shall be evaluated for a collection of values.
     */
    @Override
    public final MatchAction getMatchAction() {
        return matchAction;
    }

    /**
     * Returns a hash code value for this comparison operator.
     */
    @Override
    public final int hashCode() {
        int hash = (31 * expression1.hashCode() + expression2.hashCode()) * 37 + matchAction.hashCode();
        if (matchCase) hash = ~hash;
        return hash ^ symbol();             // Use the symbol as a way to differentiate the subclasses.
    }

    /**
     * Compares this operator with the given object for equality.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && obj.getClass() == getClass()) {
            final AbstractComparisonOperator other = (AbstractComparisonOperator) obj;
            return matchCase   ==     other.matchCase &&
                   expression1.equals(other.expression1) &&
                   expression2.equals(other.expression2) &&
                   matchAction.equals(other.matchAction);
        }
        return false;
    }

    /**
     * Returns a string representation of this comparison operator.
     */
    @Override
    public final String toString() {
        return new StringBuilder(30).append(expression1).append(' ').append(symbol()).append(' ')
                                    .append(expression2).toString();
    }
}
