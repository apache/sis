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
import java.util.Collection;
import java.util.Collections;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.filter.Node;

// Branch-dependent imports
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;


/**
 * Base class for filters performing operations on one value.
 * The nature of the operation is dependent on the subclass.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class UnaryFunction extends Node implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4441264252138631694L;

    /**
     * The expression to be used by this operator.
     *
     * @see #getExpression()
     */
    protected final Expression expression;

    /**
     * Creates a new unary operator.
     */
    UnaryFunction(final Expression expression) {
        ArgumentChecks.ensureNonNull("expression", expression);
        this.expression = expression;
    }

    /**
     * Returns the expressions to be used by this operator.
     */
    public final Expression getExpression() {
        return expression;
    }

    /**
     * Returns the singleton expression tested by this operator.
     */
    @Override
    protected final Collection<?> getChildren() {
        return Collections.singleton(expression);
    }

    /**
     * Returns a hash code value for this operator.
     */
    @Override
    public final int hashCode() {
        // We use the symbol as a way to differentiate the subclasses.
        return expression.hashCode() ^ symbol();
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
            return expression.equals(((UnaryFunction) obj).expression);
        }
        return false;
    }


    /**
     * Filter operator that checks if an expression's value is {@code null}.  A {@code null}
     * is equivalent to no value present. The value 0 is a valid value and is not considered
     * {@code null}.
     */
    static final class IsNull extends UnaryFunction implements org.opengis.filter.PropertyIsNull {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = 5538743632585679484L;

        /** Creates a new operator. */
        IsNull(Expression expression) {
            super(expression);
        }

        /** Identification of this operation. */
        @Override public String getName() {return NAME;}
        @Override protected char symbol() {return '∅';}

        /** Returns {@code true} if the given value evaluates to {@code null}. */
        @Override public boolean evaluate(final Object object) {
            return expression.evaluate(object) == null;
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The negation filter (¬).
     */
    static final class Not extends Node implements org.opengis.filter.Not {
        /** For cross-version compatibility. */
        private static final long serialVersionUID = -1296823195138427781L;

        /** The filter to negate. */
        private final Filter filter;

        /** Creates a new filter. */
        Not(final Filter filter) {
            ArgumentChecks.ensureNonNull("filter", filter);
            this.filter = filter;
        }

        /** Identification of this operation. */
        @Override public String getName() {return "Not";}
        @Override protected char symbol() {return '¬';}

        /** Returns the singleton filter used by this operation. */
        @Override protected Collection<Filter> getChildren() {
            return Collections.singletonList(filter);
        }

        /** Returns */
        @Override public Filter getFilter() {
            return filter;
        }

        /** Evaluate this filter on the given object. */
        @Override public boolean evaluate(final Object object) {
            return !filter.evaluate(object);
        }

        /** Implementation of the visitor pattern. */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }
}
