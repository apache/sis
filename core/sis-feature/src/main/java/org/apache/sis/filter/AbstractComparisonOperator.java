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
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AbstractComparisonOperator extends AbstractBinaryOperator implements BinaryComparisonOperator, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4709016194087609721L;

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
        super(expression1, expression2);
        this.matchCase   = matchCase;
        this.matchAction = matchAction;
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
        int hash = super.hashCode() * 37 + matchAction.hashCode();
        if (matchCase) hash = ~hash;
        return hash;
    }

    /**
     * Compares this operator with the given object for equality.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            final AbstractComparisonOperator other = (AbstractComparisonOperator) obj;
            return matchCase == other.matchCase && matchAction.equals(other.matchAction);
        }
        return false;
    }
}
