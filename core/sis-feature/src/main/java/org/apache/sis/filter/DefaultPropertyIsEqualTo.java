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

import java.util.Objects;
import org.apache.sis.util.Numbers;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.MatchAction;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;


/**
 * Filter operator that compares that its two sub-expressions are equal to each other.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class DefaultPropertyIsEqualTo extends AbstractComparisonOperator implements PropertyIsEqualTo {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5783347523815670017L;

    /**
     * Creates a new comparison operator.
     * It is caller responsibility to ensure that no argument is null.
     */
    DefaultPropertyIsEqualTo(Expression expression1, Expression expression2, boolean matchCase, MatchAction matchAction) {
        super(expression1, expression2, matchCase, matchAction);
    }

    /**
     * Returns the mathematical symbol for this comparison operator.
     */
    @Override
    protected char symbol() {
        return '=';
    }

    /**
     * Determines if the test represented by this filter passed.
     *
     * @todo Use locale-sensitive {@link java.text.Collator} for string comparisons.
     */
    @Override
    public boolean evaluate(Object object) {
        final Object r1 = expression1.evaluate(object);
        final Object r2 = expression2.evaluate(object);
        if (Objects.equals(r1, r2)) {
            return true;
        } else if (r1 instanceof Number && r2 instanceof Number) {
            @SuppressWarnings("unchecked") final Class<? extends Number> c1 = (Class<? extends Number>) r1.getClass();
            @SuppressWarnings("unchecked") final Class<? extends Number> c2 = (Class<? extends Number>) r2.getClass();
            if (c1 != c2) {
                final Class<? extends Number> c = Numbers.widestClass(c1, c2);
                return Numbers.cast((Number) r1, c).equals(
                       Numbers.cast((Number) r2, c));
            }
        } else if (r1 instanceof CharSequence && r2 instanceof CharSequence) {
            final String s1 = r1.toString();
            final String s2 = r2.toString();
            if (!matchCase) {
                return s1.equalsIgnoreCase(s2);
            } else if (r1 != s1 || r2 != s2) {
                return s1.equals(s2);
            }
        }
        return false;
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }
}
